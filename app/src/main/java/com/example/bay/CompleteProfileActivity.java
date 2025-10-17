package com.example.bay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bay.databinding.ActivityCompleteProfileBinding;
import com.example.bay.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class CompleteProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;

    private ActivityCompleteProfileBinding binding;
    private FirebaseAuth mAuth;
    private Uri imageUri;
    private String imageUrl;

    private String openFrom = "";
    private String phoneNumber = "";
    private String verifiedCode = "";
    private String username = "";
    private String email = "";
    private String password = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityCompleteProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        initializeFromIntent();
        setupUI();
        setupClickListeners();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeFromIntent() {
        if (getIntent() != null) {
            phoneNumber = getIntent().getStringExtra("phone_number");
            verifiedCode = getIntent().getStringExtra("verified_code");
            openFrom = getIntent().getStringExtra("openFrom");
            username = getIntent().getStringExtra("username");
            email = getIntent().getStringExtra("email");
            password = getIntent().getStringExtra("password");
        }
    }

    private void setupUI() {
        binding.imagePreview.setVisibility(View.GONE);

        if ("openFromPhoneNumber".equals(openFrom)) {
            binding.etPhoneNumber.setVisibility(View.GONE);
            binding.etEmail.setVisibility(View.VISIBLE);
        } else {
            binding.etUsername.setVisibility(View.GONE);
            binding.etPhoneNumber.setVisibility(View.VISIBLE);
            binding.etEmail.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(phoneNumber)) {
            binding.etPhoneNumber.setText(phoneNumber);
        }
    }

    private void setupClickListeners() {
        binding.imageButton.setOnClickListener(v -> openGallery());
        binding.materialButton2.setOnClickListener(v -> validateAndSaveUser());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            binding.imagePreview.setImageURI(imageUri);
            binding.imageButton.setVisibility(View.GONE);
            binding.imagePreview.setVisibility(View.VISIBLE);
        }
    }

    private void validateAndSaveUser() {
        String inputUsername = binding.etUsername.getText().toString().trim();
        String inputEmail = binding.etEmail.getText().toString().trim();
        String inputPhone = binding.etPhoneNumber.getText().toString().trim();
        String role = binding.spinnerCategory.getSelectedItem().toString();

        // Validate required fields
        if (!isValidInput(inputUsername, inputEmail, inputPhone, role)) {
            return;
        }

        String finalUsername = inputUsername.isEmpty() ? username : inputUsername;
        String finalEmail = inputEmail.isEmpty() ? email : inputEmail;
        String finalPhone = inputPhone.isEmpty() ? phoneNumber : inputPhone;

        showLoading("Saving profile...");

        if (imageUri != null) {
            uploadImageToStorage(finalUsername, finalEmail, finalPhone, role);
        } else {
            saveUserData(finalUsername, finalEmail, finalPhone, role, null);
        }
    }

    private boolean isValidInput(String username, String email, String phone, String role) {
        if ("openFromPhoneNumber".equals(openFrom)) {
            // Email registration validation
            if (TextUtils.isEmpty(email) && TextUtils.isEmpty(this.email)) {
                binding.etEmail.setError("Email is required");
                return false;
            }
            if (!isValidEmail(email.isEmpty() ? this.email : email)) {
                binding.etEmail.setError("Please enter a valid email");
                return false;
            }
        } else {
            // Phone registration validation
            if (TextUtils.isEmpty(username) && TextUtils.isEmpty(this.username)) {
                binding.etUsername.setError("Username is required");
                return false;
            }
        }

        if ("Select Role".equals(role)) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void uploadImageToStorage(String username, String email, String phone, String role) {
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("profile_images/" + UUID.randomUUID() + ".jpg");

        UploadTask uploadTask = storageRef.putFile(imageUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                imageUrl = uri.toString();
                saveUserData(username, email, phone, role, imageUrl);
            }).addOnFailureListener(e -> {
                hideLoading();
                Toast.makeText(CompleteProfileActivity.this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            hideLoading();
            Toast.makeText(CompleteProfileActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveUserData(String username, String email, String phone, String role, String uploadedImageUrl) {
        String finalImageUrl = uploadedImageUrl != null ? uploadedImageUrl : "https://example.com/default-profile.png";

        if (isEmailRegistration(email, password)) {
            createUserWithEmail(username, email, phone, role, finalImageUrl);
        } else if (isPhoneRegistration(phone)) {
            createUserWithPhone(username, email, phone, role, finalImageUrl);
        } else {
            hideLoading();
            Toast.makeText(this, "Invalid registration data", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isEmailRegistration(String email, String password) {
        return !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password);
    }

    private boolean isPhoneRegistration(String phone) {
        return !TextUtils.isEmpty(phone);
    }

    private void createUserWithEmail(String username, String email, String phone, String role, String imageUrl) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        String userId = firebaseUser.getUid();
                        User user = createUser(userId, username, email, phone, role, imageUrl);
                        saveUserToDatabase(user, userId);
                    } else {
                        hideLoading();
                        Toast.makeText(this, "User creation failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createUserWithPhone(String username, String email, String phone, String role, String imageUrl) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : UUID.randomUUID().toString();
        User user = createUser(userId, username, email, phone, role, imageUrl);
        saveUserToDatabase(user, userId);
    }

    private User createUser(String userId, String username, String email, String phone, String role, String imageUrl) {
        User user = new User(
                userId,
                username,
                email,
                phone,
                role,
                "Phnom Penh",
                imageUrl
        );
        user.password = password;
        return user;
    }

    private void saveUserToDatabase(User user, String userId) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    hideLoading();
                    Toast.makeText(this, "✅ បង្កើតអ្នកប្រើប្រាស់ជោគជ័យ", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, AuthenticationLogInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(String message) {
        binding.materialButton2.setEnabled(false);
        binding.imageButton.setEnabled(false);
        binding.loading.setVisibility(View.VISIBLE);
        if (binding.loadingText != null) {
            binding.loadingText.setText(message);
        }
    }

    private void hideLoading() {
        binding.materialButton2.setEnabled(true);
        binding.imageButton.setEnabled(true);
        binding.loading.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}