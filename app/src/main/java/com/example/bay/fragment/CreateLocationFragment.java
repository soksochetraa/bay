package com.example.bay.fragment;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.MapPickerFragment;
import com.example.bay.R;
import com.example.bay.databinding.FragmentCreateLocationBinding;
import com.example.bay.model.Location;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CreateLocationFragment extends Fragment {

    private FragmentCreateLocationBinding binding;
    private final List<Uri> imageUris = new ArrayList<>();
    private Uri profileUri;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference locationsRef;
    private FirebaseStorage storage;

    private ActivityResultLauncher<Intent> imagePicker;
    private ActivityResultLauncher<Intent> profilePicker;

    private Double latitude;
    private Double longitude;
    private String locationAddress;

    private HomeActivity home;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateLocationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        locationsRef = FirebaseDatabase.getInstance().getReference("locations");
        storage = FirebaseStorage.getInstance();

        setupClickListeners();
        setupImagePickers();
        showCurrentImages();
    }

    private void setupClickListeners() {
        // Close button
        binding.btnCloseCreate.setOnClickListener(v -> {
            if (home != null) {
                home.onBackPressed();
            }
        });

        // Add images button
        binding.btnAddImage.setOnClickListener(v -> openImagePicker());

        // Profile image picker
        binding.imgLogo.setOnClickListener(v -> openProfilePicker());

        // Location link
        binding.etLocationLink.setOnClickListener(v -> {
            if (home != null) {
                home.LoadFragment(new MapPickerFragment());
            }
        });

        // Save button
        binding.btnSaveFarm.setOnClickListener(v -> validateAndSaveLocation());

        // Listen for map picker result
        getParentFragmentManager().setFragmentResultListener(
                "map_picker_result",
                getViewLifecycleOwner(),
                (k, r) -> {
                    latitude = r.getDouble("latitude");
                    longitude = r.getDouble("longitude");
                    locationAddress = r.getString("address", "ទីតាំងបានជ្រើសរើស");
                    binding.etLocationLink.setText(locationAddress);
                }
        );
    }

    private void setupImagePickers() {
        // Image picker for multiple farm photos
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleImagePickerResult
        );

        // Profile picker for single profile image
        profilePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
                    profileUri = result.getData().getData();
                    Glide.with(requireContext())
                            .load(profileUri)
                            .placeholder(R.drawable.ic_add_photo)
                            .centerCrop()
                            .into(binding.ivLogo);
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        imagePicker.launch(Intent.createChooser(intent, "ជ្រើសរើសរូបភាពកសិដ្ឋាន"));
    }

    private void openProfilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        profilePicker.launch(Intent.createChooser(intent, "ជ្រើសរើសរូបភាពប្រវត្តិកសិដ្ឋាន"));
    }

    private void handleImagePickerResult(ActivityResult result) {
        if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) {
            return;
        }

        Intent data = result.getData();

        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            int count = clipData.getItemCount();
            // Limit to 5 images maximum as per your XML hint
            for (int i = 0; i < Math.min(count, 5); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null && !imageUris.contains(uri)) {
                    imageUris.add(uri);
                }
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            if (uri != null && !imageUris.contains(uri)) {
                imageUris.add(uri);
            }
        }

        // Limit to maximum 5 images
        if (imageUris.size() > 5) {
            imageUris.subList(5, imageUris.size()).clear();
            Toast.makeText(requireContext(), "ត្រូវជ្រើសរូបភាពយ៉ាងច្រើនបំផុត ៥ រូប", Toast.LENGTH_SHORT).show();
        }

        showCurrentImages();
    }

    private void showCurrentImages() {
        LinearLayout container = binding.layoutSelectedImages;
        container.removeAllViews();

        if (imageUris.isEmpty()) {
            binding.tvNoImageHint.setVisibility(View.VISIBLE);
            binding.horizontalImageScroll.setVisibility(View.GONE);
            return;
        } else {
            binding.tvNoImageHint.setVisibility(View.GONE);
            binding.horizontalImageScroll.setVisibility(View.VISIBLE);
        }

        int imageSize = dpToPx(100);
        int marginEnd = dpToPx(8);
        int removeSize = dpToPx(22);

        for (int i = 0; i < imageUris.size(); i++) {
            Uri uri = imageUris.get(i);

            FrameLayout frameLayout = new FrameLayout(requireContext());
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(imageSize, imageSize);
            if (i < imageUris.size() - 1) {
                frameParams.setMarginEnd(marginEnd);
            }
            frameLayout.setLayoutParams(frameParams);

            ImageView imageView = new ImageView(requireContext());
            FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(R.drawable.bg_image_placeholder);

            Glide.with(requireContext())
                    .load(uri)
                    .placeholder(R.drawable.image_border)
                    .error(R.drawable.image_border)
                    .centerCrop()
                    .into(imageView);

            ImageView btnRemove = new ImageView(requireContext());
            FrameLayout.LayoutParams removeParams = new FrameLayout.LayoutParams(removeSize, removeSize);
            removeParams.gravity = Gravity.TOP | Gravity.END;
            removeParams.setMargins(0, dpToPx(4), dpToPx(4), 0);
            btnRemove.setLayoutParams(removeParams);
            btnRemove.setImageResource(R.drawable.ic_close);
            btnRemove.setBackgroundResource(R.drawable.rounded_close_bg);
            btnRemove.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            btnRemove.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

            final int position = i;
            btnRemove.setOnClickListener(v -> {
                imageUris.remove(position);
                showCurrentImages();
            });

            frameLayout.addView(imageView);
            frameLayout.addView(btnRemove);
            container.addView(frameLayout);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void validateAndSaveLocation() {
        // Get all input values
        String farmName = binding.etFarmName.getText().toString().trim();
        String category = binding.spinnerCategory.getSelectedItem().toString();
        String phone = binding.etPhone.getText().toString().trim();
        String locationLink = binding.etLocationLink.getText().toString().trim();
        String facebook = binding.etFacebook.getText().toString().trim();
        String telegram = binding.etTelegram.getText().toString().trim();
        String tiktok = binding.etTiktok.getText().toString().trim();
        String growingText = binding.etGrowing.getText().toString().trim();
        String certificatesText = binding.etCertificates.getText().toString().trim();
        String about = binding.etAbout.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(farmName)) {
            binding.etFarmName.setError("សូមបញ្ចូលឈ្មោះកសិដ្ឋាន");
            binding.etFarmName.requestFocus();
            return;
        }

        if (imageUris.size() < 5) {
            Toast.makeText(requireContext(), "សូមបន្ថែមរូបភាពយ៉ាងហោចណាស់ ៥ រូប", Toast.LENGTH_SHORT).show();
            return;
        }

        if (profileUri == null) {
            Toast.makeText(requireContext(), "សូមជ្រើសរើសរូបភាពប្រវត្តិ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (latitude == null || longitude == null) {
            Toast.makeText(requireContext(), "សូមជ្រើសរើសទីតាំងលើផែនទី", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            binding.etPhone.setError("សូមបញ្ចូលលេខទូរស័ព្ទ");
            binding.etPhone.requestFocus();
            return;
        }

        // Parse growing list
        List<String> growingList = new ArrayList<>();
        if (!TextUtils.isEmpty(growingText)) {
            String[] items = growingText.split(",");
            for (String item : items) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    growingList.add(trimmed);
                }
            }
        }

        // Parse certificates (assuming simple text for now)
        List<Map<String, String>> certificateList = new ArrayList<>();
        if (!TextUtils.isEmpty(certificatesText)) {
            String[] items = certificatesText.split(",");
            for (String item : items) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    Map<String, String> cert = new HashMap<>();
                    cert.put("name", trimmed);
                    cert.put("description", "");
                    certificateList.add(cert);
                }
            }
        }

        // Disable save button and show loading
        setSavingState(true);

        // Upload profile image first
        uploadProfileImage(profileUri, new UploadCallback() {
            @Override
            public void onSuccess(String profileUrl) {
                // Upload farm photos
                uploadFarmPhotos(imageUris, new ImagesUploadCallback() {
                    @Override
                    public void onSuccess(List<String> photoUrls) {
                        // Create and save location object
                        saveLocationToDatabase(
                                farmName, category, profileUrl, photoUrls, phone,
                                locationLink, facebook, telegram, tiktok,
                                growingList, certificateList, about
                        );
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        setSavingState(false);
                        Toast.makeText(requireContext(),
                                "មិនអាចផ្ទុករូបភាពបាន: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                setSavingState(false);
                Toast.makeText(requireContext(),
                        "មិនអាចផ្ទុករូបភាពប្រវត្តិបាន: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadProfileImage(Uri profileUri, UploadCallback callback) {
        String filename = "profile_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference profileRef = storage.getReference()
                .child("location_profiles")
                .child(filename);

        UploadTask uploadTask = profileRef.putFile(profileUri);

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return profileRef.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            callback.onSuccess(uri.toString());
        }).addOnFailureListener(e -> {
            callback.onFailure(e.getMessage());
        });
    }

    private void uploadFarmPhotos(List<Uri> imageUris, ImagesUploadCallback callback) {
        List<String> downloadUrls = new ArrayList<>();
        List<UploadTask> uploadTasks = new ArrayList<>();

        for (Uri uri : imageUris) {
            String filename = UUID.randomUUID().toString() + ".jpg";
            StorageReference photoRef = storage.getReference()
                    .child("location_photos")
                    .child(filename);

            UploadTask uploadTask = photoRef.putFile(uri);
            uploadTasks.add(uploadTask);

            // Get download URL after upload
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return photoRef.getDownloadUrl();
            }).addOnSuccessListener(downloadUri -> {
                downloadUrls.add(downloadUri.toString());

                // When all photos are uploaded
                if (downloadUrls.size() == imageUris.size()) {
                    callback.onSuccess(downloadUrls);
                }
            }).addOnFailureListener(e -> {
                callback.onFailure(e.getMessage());
            });
        }
    }

    private void saveLocationToDatabase(String farmName, String category, String profileUrl,
                                        List<String> photoUrls, String phone, String locationLink,
                                        String facebook, String telegram, String tiktok,
                                        List<String> growingList, List<Map<String, String>> certificateList,
                                        String about) {

        if (currentUser == null) {
            Toast.makeText(requireContext(), "សូមចូលទៅក្នុងគណនីរបស់អ្នកជាមុន", Toast.LENGTH_SHORT).show();
            setSavingState(false);
            return;
        }

        String locationId = locationsRef.push().getKey();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        // Create Location object according to your model
        Location.Owner owner = new Location.Owner(currentUser.getUid());

        Location.Contact contact = new Location.Contact(
                phone,
                locationLink,
                facebook,
                telegram,
                tiktok
        );

        Location.Detail detail = new Location.Detail(
                growingList,
                certificateList,
                about
        );

        Location.Visibility visibility = new Location.Visibility(true);

        Location location = new Location(
                owner,
                farmName,
                category,
                "active",  // Default status
                latitude,
                longitude,
                profileUrl,
                photoUrls,
                contact,
                detail,
                visibility,
                timestamp
        );

        if (locationId != null) {
            locationsRef.child(locationId).setValue(location)
                    .addOnSuccessListener(aVoid -> {
                        setSavingState(false);
                        Toast.makeText(requireContext(),
                                "បានបង្កើតកសិដ្ឋានដោយជោគជ័យ",
                                Toast.LENGTH_SHORT).show();

                        if (home != null) {
                            home.onBackPressed();
                        }
                    })
                    .addOnFailureListener(e -> {
                        setSavingState(false);
                        Toast.makeText(requireContext(),
                                "មិនអាចរក្សាទុកបាន: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setSavingState(boolean isSaving) {
        binding.btnSaveFarm.setEnabled(!isSaving);
        binding.btnAddImage.setEnabled(!isSaving);
        binding.imgLogo.setEnabled(!isSaving);

        if (isSaving) {
            binding.btnSaveFarm.setText("កំពុងរក្សាទុក...");
        } else {
            binding.btnSaveFarm.setText("រក្សាទុកកំណត់ត្រា");
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeActivity) {
            home = (HomeActivity) context;
            home.hideBottomNavigation();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (home != null) {
            home.showBottomNavigation();
        }
    }

    interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String errorMessage);
    }

    interface ImagesUploadCallback {
        void onSuccess(List<String> downloadUrls);
        void onFailure(String errorMessage);
    }
}