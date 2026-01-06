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
import com.google.firebase.messaging.FirebaseMessaging;
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
    private String deviceToken = "";

    private UserRepository userRepository;
    private User createdUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCompleteProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        fetchDeviceToken();
        initializeFromIntent();
        setupUI();
        setupClickListeners();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
    }

    private void fetchDeviceToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    deviceToken = token;
                    Log.d(TAG, "FCM Token: " + token);
                })
                .addOnFailureListener(e -> {
                    deviceToken = "";
                    Log.e(TAG, "Failed to get FCM token: " + e.getMessage());
                });
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

        if (!isValidInput(inputFirstName, inputLastName, inputEmail, inputPhone, role)) return;

        String finalFirstName = inputFirstName.isEmpty() ? firstName : inputFirstName;
        String finalLastName = inputLastName.isEmpty() ? lastName : inputLastName;
        String finalEmail = inputEmail.isEmpty() ? email : inputEmail;
        String finalPhone = inputPhone.isEmpty() ? phoneNumber : inputPhone;

        showLoading("Saving profile...");

        if (imageUri != null) {
            uploadImageToStorage(finalFirstName, finalLastName, finalEmail, finalPhone, role);
        } else {
            saveUserData(finalFirstName, finalLastName, finalEmail, finalPhone, role, null);
        }
    }

    private boolean isValidInput(String firstName, String lastName, String email, String phone, String role) {
        if ("openFromPhoneNumber".equals(openFrom)) {
            if (TextUtils.isEmpty(email)) return false;
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return false;
            if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) return false;
        } else {
            if (TextUtils.isEmpty(phone)) return false;
        }
        return !"Select Role".equals(role);
    }

    private void uploadImageToStorage(String firstName, String lastName, String email, String phone, String role) {
        StorageReference storageRef =
                FirebaseStorage.getInstance().getReference("profile_images/" + UUID.randomUUID() + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri ->
                                saveUserData(firstName, lastName, email, phone, role, uri.toString())
                        )
                )
                .addOnFailureListener(e -> hideLoading());
    }

    private void saveUserData(String firstName, String lastName, String email, String phone, String role, String uploadedImageUrl) {
        String finalImageUrl = uploadedImageUrl != null
                ? uploadedImageUrl
                : "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            createUserWithEmail(firstName, lastName, email, phone, role, finalImageUrl);
        } else {
            createUserWithPhone(firstName, lastName, email, phone, role, finalImageUrl);
        }
    }

    private void createUserWithEmail(String firstName, String lastName, String email, String phone, String role, String imageUrl) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        createdUser = createUser(firebaseUser.getUid(), firstName, lastName, email, phone, role, imageUrl);
                        sendUserToRepository(createdUser);
                    } else hideLoading();
                })
                .addOnFailureListener(e -> hideLoading());
    }

    private void createUserWithPhone(String firstName, String lastName, String email, String phone, String role, String imageUrl) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        String uid = firebaseUser != null ? firebaseUser.getUid() : UUID.randomUUID().toString();
        createdUser = createUser(uid, firstName, lastName, email, phone, role, imageUrl);
        sendUserToRepository(createdUser);
    }

    private User createUser(String userId, String firstName, String lastName, String email, String phone, String role, String imageUrl) {
        return new User(userId, firstName, lastName, email, phone, role, "Phnom Penh", imageUrl, deviceToken);
    }

    private void sendUserToRepository(User user) {
        userRepository.createUser(user, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User result) {
                hideLoading();
                createdUser = result;
                autoLoginAndGoToMap();
            }

            @Override
            public void onError(String errorMsg) {
                hideLoading();
            }
        });
    }

    private void autoLoginAndGoToMap() {
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Intent intent = new Intent(this, MapPickerActivity.class);
                        startActivityForResult(intent, PICK_LOCATION_REQUEST);
                    });
        } else {
            Intent intent = new Intent(this, MapPickerActivity.class);
            startActivityForResult(intent, PICK_LOCATION_REQUEST);
        }
    }

    private void updateUserLocation(User user) {
        userRepository.updateUser(user.getUserId(), user, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User result) {
                navigateToHome();
            }

            @Override
            public void onError(String errorMsg) {}
        });
    }

    private void navigateToHome() {
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
