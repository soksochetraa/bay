package com.example.bay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bay.databinding.ActivityCompleteProfileBinding;
import com.example.bay.model.User;
import com.example.bay.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.UUID;

public class CompleteProfileActivity extends AppCompatActivity {

    private static final String TAG = "CompleteProfile";
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int PICK_LOCATION_REQUEST = 200;

    private ActivityCompleteProfileBinding binding;
    private FirebaseAuth mAuth;
    private Uri imageUri;
    private String imageUrl;
    private String openFrom = "";
    private String phoneNumber = "";
    private String verifiedCode = "";
    private String firstName = "";
    private String lastName = "";
    private String email = "";
    private String password = "";
    private UserRepository userRepository;
    private User createdUser;

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
        userRepository = new UserRepository();
        Log.d(TAG, "Firebase initialized");
    }

    private void initializeFromIntent() {
        if (getIntent() != null) {
            phoneNumber = getIntent().getStringExtra("phone_number");
            verifiedCode = getIntent().getStringExtra("verified_code");
            openFrom = getIntent().getStringExtra("openFrom");
            firstName = getIntent().getStringExtra("firstName");
            lastName = getIntent().getStringExtra("lastName");
            email = getIntent().getStringExtra("email");
            password = getIntent().getStringExtra("password");
            Log.d(TAG, "Intent data: " + openFrom + ", "+ firstName + ", " + lastName + ", " + email + ", "+ password + ", " + phoneNumber);
        }
    }

    private void setupUI() {
        binding.imagePreview.setVisibility(View.GONE);

        if ("openFromPhoneNumber".equals(openFrom)) {
            binding.etPhoneNumber.setVisibility(View.GONE);
            binding.etEmail.setVisibility(View.VISIBLE);
            binding.etFirstName.setVisibility(View.VISIBLE);
            binding.etLastName.setVisibility(View.VISIBLE);
        } else {
            binding.etFirstName.setVisibility(View.GONE);
            binding.etLastName.setVisibility(View.GONE);
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
            Log.d(TAG, "Image selected: " + imageUri);
        }

        if (requestCode == PICK_LOCATION_REQUEST && resultCode == RESULT_OK && data != null) {
            String province = data.getStringExtra("province");
            if (createdUser != null && province != null) {
                createdUser.setLocation(province);
                updateUserLocation(createdUser);
            }
        }
    }

    private void validateAndSaveUser() {
        String inputFirstName = binding.etFirstName.getText().toString().trim();
        String inputLastName = binding.etLastName.getText().toString().trim();
        String inputEmail = binding.etEmail.getText().toString().trim();
        String inputPhone = binding.etPhoneNumber.getText().toString().trim();
        String role = binding.spinnerCategory.getSelectedItem().toString();

        Log.d(TAG, "Button clicked: " + inputFirstName + ", " + inputLastName + ", " + inputEmail + ", " + inputPhone);
        Toast.makeText(this, "Processing...", Toast.LENGTH_SHORT).show();

        if (!isValidInput(inputFirstName, inputLastName, inputEmail, inputPhone, role)) {
            Log.d(TAG, "Validation failed");
            return;
        }
        Log.d(TAG, "Validation passed");

        String finalFirstName = inputFirstName.isEmpty() ? firstName : inputFirstName;
        String finalLastName = inputLastName.isEmpty() ? lastName : inputLastName;
        String finalEmail = inputEmail.isEmpty() ? email : inputEmail;
        String finalPhone = inputPhone.isEmpty() ? phoneNumber : inputPhone;

        showLoading("Saving profile...");
        Log.d(TAG, "Saving user data...");

        if (imageUri != null) {
            uploadImageToStorage(finalFirstName, finalLastName, finalEmail, finalPhone, role);
        } else {
            saveUserData(finalFirstName, finalLastName, finalEmail, finalPhone, role, null);
        }
    }

    private boolean isValidInput(String firstName, String lastName, String email, String phone, String role) {
        if ("openFromPhoneNumber".equals(openFrom)) {
            if (TextUtils.isEmpty(email) && TextUtils.isEmpty(this.email)) {
                binding.etEmail.setError("Email is required");
                return false;
            }
            if (!isValidEmail(email.isEmpty() ? this.email : email)) {
                binding.etEmail.setError("Please enter a valid email");
                return false;
            }
            if (TextUtils.isEmpty(firstName)) {
                binding.etFirstName.setError("First name is required");
                return false;
            }
            if (TextUtils.isEmpty(lastName)) {
                binding.etLastName.setError("Last name is required");
                return false;
            }
        } else {
            if (TextUtils.isEmpty(phone) && TextUtils.isEmpty(this.phoneNumber)) {
                binding.etPhoneNumber.setError("Phone number is required");
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

    private void uploadImageToStorage(String firstName, String lastName, String email, String phone, String role) {
        Log.d(TAG, "Uploading image...");
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile_images/" + UUID.randomUUID() + ".jpg");
        UploadTask uploadTask = storageRef.putFile(imageUri);

        uploadTask.addOnSuccessListener(taskSnapshot ->
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    imageUrl = uri.toString();
                    Log.d(TAG, "Image uploaded: " + imageUrl);
                    saveUserData(firstName, lastName, email, phone, role, imageUrl);
                }).addOnFailureListener(e -> {
                    hideLoading();
                    Log.e(TAG, "Get download URL failed: " + e.getMessage());
                    Toast.makeText(this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
        ).addOnFailureListener(e -> {
            hideLoading();
            Log.e(TAG, "Image upload failed: " + e.getMessage());
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveUserData(String firstName, String lastName, String email, String phone, String role, String uploadedImageUrl) {
        String finalImageUrl = uploadedImageUrl != null ? uploadedImageUrl : "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";
        Log.d(TAG, "Creating user with: " + email + " / " + phone);

        if (isEmailRegistration(email, password)) {
            createUserWithEmail(firstName, lastName, email, phone, role, finalImageUrl);
        } else if (isPhoneRegistration(phone)) {
            createUserWithPhone(firstName, lastName, email, phone, role, finalImageUrl);
        } else {
            hideLoading();
            Log.e(TAG, "Invalid registration data");
            Toast.makeText(this, "Invalid registration data", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isEmailRegistration(String email, String password) {
        return !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password);
    }

    private boolean isPhoneRegistration(String phone) {
        return !TextUtils.isEmpty(phone);
    }

    private void createUserWithEmail(String firstName, String lastName, String email, String phone, String role, String imageUrl) {
        Log.d(TAG, "Registering new user with email: " + email);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        createdUser = createUser(firebaseUser.getUid(), firstName, lastName, email, phone, role, imageUrl);
                        Log.d(TAG, "Firebase user created: " + firebaseUser.getUid());
                        sendUserToRepository(createdUser);
                    } else {
                        hideLoading();
                        Log.e(TAG, "FirebaseUser is null");
                        Toast.makeText(this, "User creation failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Log.e(TAG, "createUserWithEmail failed: " + e.getMessage());
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createUserWithPhone(String firstName, String lastName, String email, String phone, String role, String imageUrl) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        String firebaseUid = (firebaseUser != null) ? firebaseUser.getUid() : UUID.randomUUID().toString();
        Log.d(TAG, "Creating user (phone): " + firebaseUid);
        createdUser = createUser(firebaseUid, firstName, lastName, email, phone, role, imageUrl);
        sendUserToRepository(createdUser);
    }

    private User createUser(String userId, String firstName, String lastName, String email, String phone, String role, String imageUrl) {
        Log.d(TAG, "createUser object built");
        return new User(userId, firstName, lastName, email, phone, role, "Phnom Penh", imageUrl);
    }

    private void sendUserToRepository(User user) {
        Log.d(TAG, "Sending user to Firebase via Retrofit...");
        userRepository.createUser(user, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User result) {
                hideLoading();
                createdUser = result;
                Log.d(TAG, "✅ User successfully saved to Firebase!");
                autoLoginAndGoToMap();
            }

            @Override
            public void onError(String errorMsg) {
                hideLoading();
                Log.e(TAG, "❌ Failed to save user: " + errorMsg);
                Toast.makeText(CompleteProfileActivity.this, "❌ Failed to save user: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void autoLoginAndGoToMap() {
        Log.d(TAG, "Auto logging in...");
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Log.d(TAG, "Login success → open MapPickerActivity");
                        Intent intent = new Intent(this, MapPickerActivity.class);
                        startActivityForResult(intent, PICK_LOCATION_REQUEST);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Auto login failed: " + e.getMessage()));
        } else {
            Log.d(TAG, "Skipping login → open MapPickerActivity directly");
            Intent intent = new Intent(this, MapPickerActivity.class);
            startActivityForResult(intent, PICK_LOCATION_REQUEST);
        }
    }

    private void updateUserLocation(User user) {
        Log.d(TAG, "Updating user location: " + user.getLocation());
        userRepository.updateUser(user.getUserId(), user, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User result) {
                Toast.makeText(CompleteProfileActivity.this, "✅ Location updated: " + user.getLocation(), Toast.LENGTH_SHORT).show();
                navigateToHome();
            }

            @Override
            public void onError(String errorMsg) {
                Toast.makeText(CompleteProfileActivity.this, "❌ Failed to update location: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHome() {
        Log.d(TAG, "Navigating to HomeActivity");
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(String message) {
        binding.loading.setVisibility(View.VISIBLE);
        binding.loadingText.setText(message);
        binding.materialButton2.setEnabled(false);
    }

    private void hideLoading() {
        binding.loading.setVisibility(View.GONE);
        binding.materialButton2.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
