package com.example.bay;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.bay.databinding.ActivityCompleteProfileBinding;
import com.example.bay.model.User;
import com.example.bay.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class CompleteProfileActivity extends AppCompatActivity {

    private static final String TAG = "CompleteProfile";
    private static final int PICK_LOCATION_REQUEST = 200;

    private ActivityCompleteProfileBinding binding;
    private FirebaseAuth mAuth;
    private Uri imageUri;
    private String openFrom = "";
    private String phoneNumber = "";
    private String verifiedCode = "";
    private String firstName = "";
    private String lastName = "";
    private String email = "";
    private String password = "";
    private String deviceToken = "";
    private boolean hasGoogleProvider = false;

    private UserRepository userRepository;
    private User createdUser;
    private boolean hasPermission = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                hasPermission = isGranted;
                if (isGranted) {
                    openGalleryIntent();
                } else {
                    showPermissionDeniedDialog();
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            if (data.getData() != null) {
                                imageUri = data.getData();
                                prepareAndStartCrop(imageUri);
                            }
                        }
                    });

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
            hasGoogleProvider = getIntent().getBooleanExtra("hasGoogleProvider",false);

           Log.d(TAG, "openFrom: " + openFrom);
           if (hasGoogleProvider){
               Log.d(TAG, "Has Google Provider: ");
           }
        }
    }

    private void setupUI() {
        if ("openFromPhoneNumber".equals(openFrom)) {
            binding.etPhoneNumber.setVisibility(View.GONE);
            binding.etEmail.setVisibility(View.VISIBLE);
            binding.etFirstName.setVisibility(View.VISIBLE);
            binding.etLastName.setVisibility(View.VISIBLE);

        } else if ("openFromGoogle".equals(openFrom) && hasGoogleProvider) {
            binding.etPhoneNumber.setVisibility(View.VISIBLE);
            binding.etEmail.setVisibility(View.GONE);
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

        if (!TextUtils.isEmpty(firstName)) {
            binding.etFirstName.setText(firstName);
        }

        if (!TextUtils.isEmpty(lastName)) {
            binding.etLastName.setText(lastName);
        }

        if (!TextUtils.isEmpty(email)) {
            binding.etEmail.setText(email);
        }
    }

    private void setupClickListeners() {
        binding.imageButton.setOnClickListener(v -> {
            if (imageUri != null) {
                showImageChangeDialog();
            } else {
                checkAndRequestGalleryPermission();
            }
        });
        binding.materialButton2.setOnClickListener(v -> validateAndSaveUser());

        binding.imagePreview.setOnClickListener(v -> {
            if (imageUri != null) {
                showImageChangeDialog();
            }
        });
    }

    private void showImageChangeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profile Picture");
        builder.setMessage("Do you want to change your profile picture?");
        builder.setPositiveButton("Change", (dialog, which) -> {
            checkAndRequestGalleryPermission();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.setNeutralButton("Remove", (dialog, which) -> {
            removeImage();
        });
        builder.show();
    }

    private void removeImage() {
        imageUri = null;
        binding.imagePreview.setImageDrawable(null);
        binding.imagePreview.setVisibility(View.GONE);
        binding.imageButton.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
    }

    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("Gallery permission is required to select a profile picture. Please enable it in app settings.");
        builder.setPositiveButton("Open Settings", (dialog, which) -> {
            openAppSettings();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(this, "You can continue without a profile picture", Toast.LENGTH_LONG).show();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void checkAndRequestGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true;
                openGalleryIntent();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES)) {
                    showPermissionRationaleDialog();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true;
                openGalleryIntent();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showPermissionRationaleDialog();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }
        }
    }

    private void showPermissionRationaleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Needed");
        builder.setMessage("This app needs gallery access to let you select a profile picture. Would you like to grant permission?");
        builder.setPositiveButton("Grant", (dialog, which) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });
        builder.setNegativeButton("Not Now", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(this, "You can continue without a profile picture", Toast.LENGTH_LONG).show();
        });
        builder.show();
    }

    private void openGalleryIntent() {
        if (hasPermission) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        } else {
            Toast.makeText(this, "Permission not granted. Please enable gallery permission in settings.", Toast.LENGTH_SHORT).show();
        }
    }

    private void prepareAndStartCrop(Uri sourceUri) {
        showLoading("Preparing image...");

        new Thread(() -> {
            try {
                Uri jpegUri = convertImageToJpeg(sourceUri);

                runOnUiThread(() -> {
                    hideLoading();
                    if (jpegUri != null) {
                        startUCrop(jpegUri);
                    } else {
                        Toast.makeText(this, "Unable to process image format", Toast.LENGTH_SHORT).show();
                        if (sourceUri != null) {
                            displayImage(sourceUri);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error preparing image: " + e.getMessage());
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (sourceUri != null) {
                        displayImage(sourceUri);
                    }
                });
            }
        }).start();
    }

    private Uri convertImageToJpeg(Uri sourceUri) {
        try {
            ContentResolver resolver = getContentResolver();
            String mimeType = resolver.getType(sourceUri);
            Log.d(TAG, "Original MIME type: " + mimeType);

            File tempJpegFile = new File(getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inDither = true;

            Bitmap bitmap;
            try (InputStream inputStream = resolver.openInputStream(sourceUri)) {
                bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            }

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap");
                return null;
            }

            int maxSize = 2048;
            if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                float scale = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                bitmap.recycle();
                bitmap = scaledBitmap;
            }

            try (FileOutputStream out = new FileOutputStream(tempJpegFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                out.flush();
            }

            bitmap.recycle();

            return FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", tempJpegFile);

        } catch (Exception e) {
            Log.e(TAG, "Error converting image: " + e.getMessage());
            return null;
        }
    }

    private void startUCrop(Uri sourceUri) {
        try {
            String destinationFileName = "cropped_image_" + System.currentTimeMillis() + ".jpg";
            File destinationFile = new File(getCacheDir(), destinationFileName);
            Uri destinationUri = Uri.fromFile(destinationFile);

            UCrop uCrop = UCrop.of(sourceUri, destinationUri);

            UCrop.Options options = new UCrop.Options();
            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(95);
            options.setHideBottomControls(false);
            options.setFreeStyleCropEnabled(false);
            options.setCircleDimmedLayer(true);
            options.setShowCropFrame(true);
            options.setShowCropGrid(true);
            options.setMaxBitmapSize(2048);
            options.setMaxScaleMultiplier(5);
            options.setImageToCropBoundsAnimDuration(500);
            options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
            options.setToolbarColor(ContextCompat.getColor(this, androidx.appcompat.R.color.primary_dark_material_dark));
            options.setStatusBarColor(ContextCompat.getColor(this, androidx.appcompat.R.color.primary_dark_material_dark));
            options.setActiveControlsWidgetColor(ContextCompat.getColor(this, androidx.appcompat.R.color.primary_material_dark));
            options.setToolbarWidgetColor(ContextCompat.getColor(this, androidx.appcompat.R.color.material_grey_50));

            uCrop.withOptions(options);
            uCrop.withAspectRatio(1, 1);
            uCrop.withMaxResultSize(1024, 1024);

            uCrop.start(this);
        } catch (Exception e) {
            Log.e(TAG, "UCrop failed to start: " + e.getMessage());
            Toast.makeText(this, "Unable to crop image, using original", Toast.LENGTH_SHORT).show();
            displayImage(sourceUri);
        }
    }

    private void displayImage(Uri uri) {
        try {
            binding.imagePreview.setImageURI(uri);
            binding.imageButton.setVisibility(View.GONE);
            binding.imagePreview.setVisibility(View.VISIBLE);
            imageUri = uri;
        } catch (Exception e) {
            Log.e(TAG, "Error displaying image: " + e.getMessage());
            Toast.makeText(this, "Unable to display selected image", Toast.LENGTH_SHORT).show();
            imageUri = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                imageUri = resultUri;
                displayImage(imageUri);
                Toast.makeText(this, "Image cropped successfully", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            Log.e(TAG, "UCrop error: " + cropError.getMessage());
            Toast.makeText(this, "Crop failed, using original image", Toast.LENGTH_SHORT).show();
            if (imageUri != null) {
                displayImage(imageUri);
            }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Crop cancelled", Toast.LENGTH_SHORT).show();
            if (imageUri != null) {
                displayImage(imageUri);
            }
        } else if (requestCode == PICK_LOCATION_REQUEST && resultCode == RESULT_OK && data != null) {
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
        if ("openFromPhoneNumber".equals(openFrom) || "openFromGoogle".equals(openFrom) || "openFromRegister".equals(openFrom)) {
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (TextUtils.isEmpty(firstName)) {
                Toast.makeText(this, "First name is required", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (TextUtils.isEmpty(lastName)) {
                Toast.makeText(this, "Last name is required", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            if (!TextUtils.isEmpty(phone) && !isValidPhoneNumber(phone)) {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if ("Select Role".equals(role)) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isValidPhoneNumber(String phone) {
        String phoneRegex = "^[+]?[0-9]{10,13}$";
        return phone.matches(phoneRegex);
    }

    private void uploadImageToStorage(String firstName, String lastName, String email, String phone, String role) {
        showLoading("Uploading image...");
        StorageReference storageRef =
                FirebaseStorage.getInstance().getReference("profile_images/" + UUID.randomUUID() + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri ->
                                saveUserData(firstName, lastName, email, phone, role, uri.toString())
                        ).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                            saveUserData(firstName, lastName, email, phone, role, null);
                        })
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed: " + e.getMessage());
                    Toast.makeText(this, "Image upload failed, continuing without image", Toast.LENGTH_SHORT).show();
                    saveUserData(firstName, lastName, email, phone, role, null);
                });
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
                    } else {
                        hideLoading();
                        Toast.makeText(this, "Failed to create user", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "Failed to create account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Email auth failed: " + e.getMessage());
                });
    }

    private void createUserWithPhone(String firstName, String lastName, String email, String phone, String role, String imageUrl) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        String uid = firebaseUser != null ? firebaseUser.getUid() : UUID.randomUUID().toString();
        createdUser = createUser(uid, firstName, lastName, email, phone, role, imageUrl);
        sendUserToRepository(createdUser);
    }

    private User createUser(String userId, String firstName, String lastName, String email, String phone, String role, String imageUrl) {
        String finalPhone = TextUtils.isEmpty(phone) ? "" : phone;
        User user = new User(userId, firstName, lastName, email, finalPhone, role, "Phnom Penh", imageUrl, deviceToken, true);
        return user;
    }

    private void sendUserToRepository(User user) {
        userRepository.createUser(user, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User result) {
                hideLoading();
                createdUser = result;
                Toast.makeText(CompleteProfileActivity.this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                autoLoginAndGoToMap();
            }

            @Override
            public void onError(String errorMsg) {
                hideLoading();
                Toast.makeText(CompleteProfileActivity.this, "Failed to save profile: " + errorMsg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "User creation failed: " + errorMsg);
            }
        });
    }

    private void autoLoginAndGoToMap() {
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Intent intent = new Intent(CompleteProfileActivity.this, MapPickerActivity.class);
                        startActivityForResult(intent, PICK_LOCATION_REQUEST);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Auto-login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        navigateToHome();
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
            public void onError(String errorMsg) {
                Toast.makeText(CompleteProfileActivity.this, "Location update failed, but profile saved", Toast.LENGTH_SHORT).show();
                navigateToHome();
            }
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
        binding.imageButton.setEnabled(false);
    }

    private void hideLoading() {
        binding.loading.setVisibility(View.GONE);
        binding.materialButton2.setEnabled(true);
        binding.imageButton.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}