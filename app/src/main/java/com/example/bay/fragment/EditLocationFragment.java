package com.example.bay.fragment;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.MapPickerFragment;
import com.example.bay.R;
import com.example.bay.databinding.FragmentCreateLocationBinding;
import com.example.bay.model.Location;
import com.example.bay.repository.LocationRepository;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class EditLocationFragment extends Fragment {

    private static final String ARG_ID = "location_id";

    private FragmentCreateLocationBinding binding;
    private LocationRepository repository;
    private FirebaseUser currentUser;
    private FirebaseStorage storage;

    private HomeActivity home;

    private String locationId;
    private Location loadedLocation;

    private Double latitude;
    private Double longitude;
    private String locationAddress;

    private String selectedCategory = "Market";
    private final List<String> growingList = new ArrayList<>();

    private final List<String> existingPhotoUrls = new ArrayList<>();
    private final List<Uri> newPhotoUris = new ArrayList<>();

    private String existingProfileUrl = "";
    private Uri newProfileUri = null;

    private ActivityResultLauncher<Intent> imagePicker;
    private ActivityResultLauncher<Intent> profilePicker;

    public static EditLocationFragment newInstance(String locationId) {
        EditLocationFragment f = new EditLocationFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ID, locationId);
        f.setArguments(b);
        return f;
    }

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

        repository = new LocationRepository();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        storage = FirebaseStorage.getInstance();

        locationId = getArguments() != null ? getArguments().getString(ARG_ID) : null;

        setupCategoryButtons();
        setupPickers();
        setupListeners();
        loadLocation();
    }

    private void setupPickers() {
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleImagesResult
        );

        profilePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
                    newProfileUri = result.getData().getData();
                    if (newProfileUri != null) {
                        Glide.with(requireContext())
                                .load(newProfileUri)
                                .placeholder(R.drawable.ic_tractor)
                                .centerCrop()
                                .into(binding.imgLogo);
                    }
                }
        );
    }

    private void setupCategoryButtons() {
        setCategory("Market");
        binding.btnFarm.setOnClickListener(v -> setCategory("Farm"));
        binding.btnMarket.setOnClickListener(v -> setCategory("Market"));
    }

    private void setCategory(String category) {
        selectedCategory = category;

        boolean isFarm = "Farm".equals(category);

        binding.btnFarm.setEnabled(!isFarm);
        binding.btnMarket.setEnabled(isFarm);

        if (isFarm) {
            binding.btnFarm.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));
            binding.btnFarm.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            binding.btnFarm.setStrokeWidth(0);

            binding.btnMarket.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.white));
            binding.btnMarket.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            binding.btnMarket.setStrokeWidth(dpToPx(1));
            binding.btnMarket.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.primary));
        } else {
            binding.btnMarket.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));
            binding.btnMarket.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            binding.btnMarket.setStrokeWidth(0);

            binding.btnFarm.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.white));
            binding.btnFarm.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            binding.btnFarm.setStrokeWidth(dpToPx(1));
            binding.btnFarm.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.primary));
        }
    }

    private void setupListeners() {
        binding.btnCancelCreate.setOnClickListener(v -> {
            if (home != null) home.onBackPressed();
        });

        binding.btnSaveFarm.setText("កែប្រែ");
        binding.btnSaveFarm.setOnClickListener(v -> validateAndUpdate());

        binding.btnAddGrowing.setOnClickListener(v -> addGrowingItem());
        binding.etGrowingInput.setOnEditorActionListener((v, actionId, event) -> {
            addGrowingItem();
            return true;
        });

        binding.etLocationLink.setOnClickListener(v -> {
            if (home != null) home.LoadFragment(new MapPickerFragment());
        });

        binding.btnAddImage.setOnClickListener(v -> openImagesPicker());
        binding.imgLogo.setOnClickListener(v -> openProfilePicker());

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

    private void loadLocation() {
        if (TextUtils.isEmpty(locationId)) {
            Toast.makeText(requireContext(), "Location ID មិនត្រឹមត្រូវ", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.getLocationById(locationId, new LocationRepository.LocationCallback<Location>() {
            @Override
            public void onSuccess(Location result) {
                if (result == null) {
                    Toast.makeText(requireContext(), "រកមិនឃើញទិន្នន័យ", Toast.LENGTH_SHORT).show();
                    return;
                }

                loadedLocation = result;

                String uid = FirebaseAuth.getInstance().getUid();
                if (uid == null || loadedLocation.owner == null || !uid.equals(loadedLocation.owner.uuid)) {
                    Toast.makeText(requireContext(), "អ្នកមិនមានសិទ្ធិកែប្រែទីតាំងនេះទេ", Toast.LENGTH_SHORT).show();
                    if (home != null) home.onBackPressed();
                    return;
                }

                selectedCategory = !TextUtils.isEmpty(loadedLocation.category) ? loadedLocation.category : "Market";
                setCategory(selectedCategory);

                binding.etFarmName.setText(nonNull(loadedLocation.name));
                binding.etPhone.setText(loadedLocation.contact != null ? nonNull(loadedLocation.contact.phoneNumber) : "");
                binding.etLocationLink.setText(loadedLocation.contact != null ? nonNull(loadedLocation.contact.locationLink) : "");
                binding.etAbout.setText(loadedLocation.detail != null ? nonNull(loadedLocation.detail.about) : "");

                latitude = loadedLocation.latitude;
                longitude = loadedLocation.longitude;
                locationAddress = loadedLocation.contact != null ? loadedLocation.contact.locationLink : "";

                growingList.clear();
                if (loadedLocation.detail != null && loadedLocation.detail.growing != null) {
                    growingList.addAll(loadedLocation.detail.growing);
                }
                refreshGrowingChips();

                existingPhotoUrls.clear();
                if (loadedLocation.photos != null) existingPhotoUrls.addAll(loadedLocation.photos);

                existingProfileUrl = nonNull(loadedLocation.profileUrl);
                if (!TextUtils.isEmpty(existingProfileUrl)) {
                    Glide.with(requireContext())
                            .load(existingProfileUrl)
                            .placeholder(R.drawable.ic_tractor)
                            .centerCrop()
                            .into(binding.imgLogo);
                }

                showCurrentImages();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(requireContext(), "បញ្ហាក្នុងការទាញទិន្នន័យ: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagesPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        imagePicker.launch(Intent.createChooser(intent, "ជ្រើសរើសរូបភាព"));
    }

    private void openProfilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        profilePicker.launch(Intent.createChooser(intent, "ជ្រើសរើសរូបភាពតំណាង"));
    }

    private void handleImagesResult(ActivityResult result) {
        if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;

        Intent data = result.getData();

        List<Uri> picked = new ArrayList<>();

        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            int count = clipData.getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null) picked.add(uri);
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            if (uri != null) picked.add(uri);
        }

        for (Uri u : picked) {
            if (u == null) continue;
            if (!newPhotoUris.contains(u)) newPhotoUris.add(u);
        }

        trimToFiveTotal();
        showCurrentImages();
    }

    private void trimToFiveTotal() {
        int total = existingPhotoUrls.size() + newPhotoUris.size();
        if (total <= 5) return;

        int overflow = total - 5;

        while (overflow > 0 && newPhotoUris.size() > 0) {
            newPhotoUris.remove(newPhotoUris.size() - 1);
            overflow--;
        }

        while (overflow > 0 && existingPhotoUrls.size() > 0) {
            existingPhotoUrls.remove(existingPhotoUrls.size() - 1);
            overflow--;
        }

        Toast.makeText(requireContext(), "រូបភាពអតិបរមា ៥ រូប", Toast.LENGTH_SHORT).show();
    }

    private void showCurrentImages() {
        LinearLayout container = binding.layoutSelectedImages;
        container.removeAllViews();

        int total = existingPhotoUrls.size() + newPhotoUris.size();
        if (total == 0) {
            binding.horizontalImageScroll.setVisibility(View.GONE);
            return;
        } else {
            binding.horizontalImageScroll.setVisibility(View.VISIBLE);
        }

        int imageSize = dpToPx(100);
        int marginEnd = dpToPx(8);
        int removeSize = dpToPx(22);

        List<Object> items = new ArrayList<>();
        items.addAll(existingPhotoUrls);
        items.addAll(newPhotoUris);

        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);

            FrameLayout frameLayout = new FrameLayout(requireContext());
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(imageSize, imageSize);
            if (i < items.size() - 1) frameParams.setMarginEnd(marginEnd);
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
                    .load(item)
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
            btnRemove.setOnClickListener(v -> removePhotoAt(position));

            frameLayout.addView(imageView);
            frameLayout.addView(btnRemove);
            container.addView(frameLayout);
        }
    }

    private void removePhotoAt(int position) {
        int existingCount = existingPhotoUrls.size();
        if (position < existingCount) {
            existingPhotoUrls.remove(position);
        } else {
            int idx = position - existingCount;
            if (idx >= 0 && idx < newPhotoUris.size()) newPhotoUris.remove(idx);
        }
        showCurrentImages();
    }

    private void addGrowingItem() {
        String item = binding.etGrowingInput.getText().toString().trim();
        if (TextUtils.isEmpty(item)) {
            binding.etGrowingInput.setError("សូមបញ្ចូលដំណាំ");
            binding.etGrowingInput.requestFocus();
            return;
        }

        String normalized = item.replaceAll("\\s+", " ").trim();

        for (String existing : growingList) {
            if (existing.equalsIgnoreCase(normalized)) {
                Toast.makeText(requireContext(), "បានបន្ថែមរួចហើយ", Toast.LENGTH_SHORT).show();
                binding.etGrowingInput.setText("");
                return;
            }
        }

        growingList.add(normalized);
        binding.etGrowingInput.setText("");
        refreshGrowingChips();
    }

    private void refreshGrowingChips() {
        binding.chipGroupGrowing.removeAllViews();

        for (int i = 0; i < growingList.size(); i++) {
            String text = growingList.get(i);

            Chip chip = new Chip(requireContext());
            chip.setText(text);
            chip.setCloseIconVisible(true);
            chip.setCheckable(false);
            chip.setChipBackgroundColorResource(R.color.white);
            chip.setChipStrokeWidth(dpToPx(1));
            chip.setChipStrokeColorResource(R.color.primary);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            chip.setCloseIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
            chip.setEnsureMinTouchTargetSize(false);
            chip.setMinHeight(dpToPx(36));
            chip.setChipMinHeight(dpToPx(36));
            chip.setPadding(dpToPx(2), 0, dpToPx(2), 0);

            final int pos = i;
            chip.setOnCloseIconClickListener(v -> {
                if (pos >= 0 && pos < growingList.size()) {
                    growingList.remove(pos);
                    refreshGrowingChips();
                }
            });

            binding.chipGroupGrowing.addView(chip);
        }
    }

    private void validateAndUpdate() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "សូមចូលទៅក្នុងគណនីរបស់អ្នកជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        if (loadedLocation == null) {
            Toast.makeText(requireContext(), "ទិន្នន័យមិនទាន់ load", Toast.LENGTH_SHORT).show();
            return;
        }

        String farmName = binding.etFarmName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String link = binding.etLocationLink.getText().toString().trim();
        String about = binding.etAbout.getText().toString().trim();

        if (TextUtils.isEmpty(farmName)) {
            binding.etFarmName.setError("សូមបញ្ចូលឈ្មោះកសិដ្ឋាន");
            binding.etFarmName.requestFocus();
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

        if (TextUtils.isEmpty(link)) {
            binding.etLocationLink.setError("សូមជ្រើសរើសទីតាំង");
            binding.etLocationLink.requestFocus();
            return;
        }

        int totalPhotos = existingPhotoUrls.size() + newPhotoUris.size();
        if (totalPhotos != 5) {
            Toast.makeText(requireContext(), "សូមរក្សាទុករូបភាពឲ្យបាន ៥ រូប", Toast.LENGTH_SHORT).show();
            return;
        }

        setSavingState(true);

        String folderId = extractLocationFolderId();
        if (TextUtils.isEmpty(folderId)) folderId = locationId;

        uploadIfNeeded(folderId, new UploadAllCallback() {
            @Override
            public void onSuccess(String finalProfileUrl, List<String> finalPhotos) {
                doUpdate(farmName, phone, link, about, finalProfileUrl, finalPhotos);
            }

            @Override
            public void onFailure(String error) {
                setSavingState(false);
                Toast.makeText(requireContext(), "បរាជ័យ: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadIfNeeded(String folderId, UploadAllCallback callback) {
        if (newProfileUri == null && newPhotoUris.isEmpty()) {
            List<String> finalPhotos = new ArrayList<>(existingPhotoUrls);
            callback.onSuccess(existingProfileUrl, finalPhotos);
            return;
        }

        if (newProfileUri != null) {
            uploadProfile(folderId, newProfileUri, new UploadCallback() {
                @Override
                public void onSuccess(String profileUrl) {
                    uploadPhotos(folderId, new ImagesUploadCallback() {
                        @Override
                        public void onSuccess(List<String> newUrls) {
                            List<String> finalPhotos = new ArrayList<>(existingPhotoUrls);
                            finalPhotos.addAll(newUrls);
                            if (finalPhotos.size() != 5) {
                                callback.onFailure("ចំនួនរូបភាពមិនត្រឹមត្រូវ");
                                return;
                            }
                            callback.onSuccess(profileUrl, finalPhotos);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            callback.onFailure(errorMessage);
                        }
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    callback.onFailure(errorMessage);
                }
            });
        } else {
            uploadPhotos(folderId, new ImagesUploadCallback() {
                @Override
                public void onSuccess(List<String> newUrls) {
                    List<String> finalPhotos = new ArrayList<>(existingPhotoUrls);
                    finalPhotos.addAll(newUrls);
                    if (finalPhotos.size() != 5) {
                        callback.onFailure("ចំនួនរូបភាពមិនត្រឹមត្រូវ");
                        return;
                    }
                    callback.onSuccess(existingProfileUrl, finalPhotos);
                }

                @Override
                public void onFailure(String errorMessage) {
                    callback.onFailure(errorMessage);
                }
            });
        }
    }

    private void uploadProfile(String folderId, Uri uri, UploadCallback callback) {
        String filename = "profile_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference()
                .child("locations")
                .child(folderId)
                .child(filename);

        UploadTask uploadTask = ref.putFile(uri);
        uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                }).addOnSuccessListener(downloadUri -> callback.onSuccess(downloadUri.toString()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void uploadPhotos(String folderId, ImagesUploadCallback callback) {
        if (newPhotoUris.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<String> urls = new ArrayList<>();
        for (Uri uri : newPhotoUris) {
            String filename = UUID.randomUUID().toString() + ".jpg";
            StorageReference ref = storage.getReference()
                    .child("locations")
                    .child(folderId)
                    .child(filename);

            UploadTask uploadTask = ref.putFile(uri);
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return ref.getDownloadUrl();
            }).addOnSuccessListener(downloadUri -> {
                urls.add(downloadUri.toString());
                if (urls.size() == newPhotoUris.size()) callback.onSuccess(urls);
            }).addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        }
    }

    private void doUpdate(String farmName, String phone, String link, String about,
                          String profileUrl, List<String> photos) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        Location.Owner owner = new Location.Owner(currentUser.getUid());
        Location.Contact contact = new Location.Contact(phone, link);
        Location.Detail detail = new Location.Detail(new ArrayList<>(growingList), about);
        Location.Visibility visibility = loadedLocation.visibility != null ? loadedLocation.visibility : new Location.Visibility(true);

        Location updated = new Location(
                owner,
                farmName,
                selectedCategory,
                loadedLocation.status != null ? loadedLocation.status : "active",
                latitude,
                longitude,
                profileUrl,
                photos,
                contact,
                detail,
                visibility,
                timestamp
        );

        repository.updateLocation(locationId, updated, new LocationRepository.LocationCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                setSavingState(false);
                Toast.makeText(requireContext(), "កែប្រែបានជោគជ័យ", Toast.LENGTH_SHORT).show();
                if (home != null) home.onBackPressed();
            }

            @Override
            public void onFailure(String error) {
                setSavingState(false);
                Toast.makeText(requireContext(), "កែប្រែមិនបាន: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extractLocationFolderId() {
        String url = null;

        if (!TextUtils.isEmpty(existingProfileUrl)) url = existingProfileUrl;
        if (url == null && !existingPhotoUrls.isEmpty()) url = existingPhotoUrls.get(0);

        if (TextUtils.isEmpty(url)) return "";

        String key = "locations%2F";
        int start = url.indexOf(key);
        if (start == -1) return "";

        start += key.length();
        int end = url.indexOf("%2F", start);
        if (end == -1) return "";

        return url.substring(start, end);
    }

    private void setSavingState(boolean isSaving) {
        binding.btnSaveFarm.setEnabled(!isSaving);
        binding.btnFarm.setEnabled(!isSaving);
        binding.btnMarket.setEnabled(!isSaving);
        binding.btnAddGrowing.setEnabled(!isSaving);
        binding.etGrowingInput.setEnabled(!isSaving);
        binding.etFarmName.setEnabled(!isSaving);
        binding.etPhone.setEnabled(!isSaving);
        binding.etAbout.setEnabled(!isSaving);
        binding.etLocationLink.setEnabled(!isSaving);
        binding.btnAddImage.setEnabled(!isSaving);
        binding.imgLogo.setEnabled(!isSaving);

        binding.btnSaveFarm.setText(isSaving ? "កំពុងកែប្រែ..." : "កែប្រែ");
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private String nonNull(String s) {
        return s == null ? "" : s;
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
        if (home != null) home.showBottomNavigation();
    }

    interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String errorMessage);
    }

    interface ImagesUploadCallback {
        void onSuccess(List<String> downloadUrls);
        void onFailure(String errorMessage);
    }

    interface UploadAllCallback {
        void onSuccess(String finalProfileUrl, List<String> finalPhotos);
        void onFailure(String error);
    }
}
