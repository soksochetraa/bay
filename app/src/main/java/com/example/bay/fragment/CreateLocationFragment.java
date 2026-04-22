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
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.databinding.FragmentCreateLocationBinding;
import com.example.bay.model.Location;
import com.example.bay.repository.LocationRepository;
import com.example.bay.viewmodel.CreateLocationViewModel;
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

public class CreateLocationFragment extends Fragment {

    private FragmentCreateLocationBinding binding;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseStorage storage;

    private ActivityResultLauncher<Intent> imagePicker;
    private ActivityResultLauncher<Intent> profilePicker;

    private HomeActivity home;

    private LocationRepository locationRepository;
    private CreateLocationViewModel vm;

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

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "សូមចូលទៅក្នុងគណនីរបស់អ្នកជាមុន", Toast.LENGTH_SHORT).show();
            if (home != null) home.onBackPressed();
            return;
        }

        String phone = currentUser.getPhoneNumber();

        storage = FirebaseStorage.getInstance();
        locationRepository = new LocationRepository();

        vm = new ViewModelProvider(requireActivity()).get(CreateLocationViewModel.class);
        
        if (TextUtils.isEmpty(vm.phone.getValue())) {
            vm.phone.setValue(phone);
        }

        setupCategoryButtons();
        setupClickListeners();
        setupImagePickers();
        bindState();
        renderAll();
    }

    private void bindState() {
        vm.imageUris.observe(getViewLifecycleOwner(), uris -> showCurrentImages(uris));
        vm.growingList.observe(getViewLifecycleOwner(), list -> refreshGrowingChips(list));
        vm.profileUri.observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                Glide.with(requireContext()).load(uri).placeholder(R.drawable.ic_tractor).centerCrop().into(binding.imgLogo);
            } else {
                binding.imgLogo.setImageResource(R.drawable.ic_tractor);
            }
        });
        vm.locationAddress.observe(getViewLifecycleOwner(), addr -> {
            if (!TextUtils.isEmpty(addr)) binding.etLocationLink.setText(addr);
        });
        vm.selectedCategory.observe(getViewLifecycleOwner(), this::setCategory);

        binding.etFarmName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) vm.farmName.setValue(binding.etFarmName.getText().toString());
        });
        binding.etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) vm.phone.setValue(binding.etPhone.getText().toString());
        });
        binding.etAbout.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) vm.about.setValue(binding.etAbout.getText().toString());
        });
    }

    private void renderAll() {
        binding.etFarmName.setText(nonNull(vm.farmName.getValue()));
        binding.etPhone.setText(nonNull(vm.phone.getValue()));
        binding.etAbout.setText(nonNull(vm.about.getValue()));
        binding.etLocationLink.setText(nonNull(vm.locationAddress.getValue()));
        setCategory(nonNull(vm.selectedCategory.getValue(), "Market"));
        showCurrentImages(nonNullList(vm.imageUris.getValue()));
        refreshGrowingChips(nonNullList(vm.growingList.getValue()));
        Uri p = vm.profileUri.getValue();
        if (p != null) Glide.with(requireContext()).load(p).placeholder(R.drawable.ic_tractor).centerCrop().into(binding.imgLogo);
    }

    private void setupCategoryButtons() {
        setCategory(nonNull(vm != null ? vm.selectedCategory.getValue() : "Market", "Market"));
        binding.btnFarm.setOnClickListener(v -> vm.selectedCategory.setValue("Farm"));
        binding.btnMarket.setOnClickListener(v -> vm.selectedCategory.setValue("Market"));
    }

    private void setCategory(String category) {
        String c = TextUtils.isEmpty(category) ? "Market" : category;
        boolean isFarm = "Farm".equals(c);

        binding.btnFarm.setEnabled(!isFarm);
        binding.btnMarket.setEnabled(isFarm);

        if (isFarm) {
            binding.tvGrowingLabel.setVisibility(View.VISIBLE);
            binding.layoutGrowingInput.setVisibility(View.VISIBLE);
            binding.chipGroupGrowing.setVisibility(View.VISIBLE);

            binding.btnFarm.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));
            binding.btnFarm.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            binding.btnFarm.setStrokeWidth(0);

            binding.btnMarket.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.white));
            binding.btnMarket.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            binding.btnMarket.setStrokeWidth(dpToPx(1));
            binding.btnMarket.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.primary));
        } else {
            binding.tvGrowingLabel.setVisibility(View.GONE);
            binding.layoutGrowingInput.setVisibility(View.GONE);
            binding.chipGroupGrowing.setVisibility(View.GONE);

            binding.btnMarket.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));
            binding.btnMarket.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            binding.btnMarket.setStrokeWidth(0);

            binding.btnFarm.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.white));
            binding.btnFarm.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            binding.btnFarm.setStrokeWidth(dpToPx(1));
            binding.btnFarm.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.primary));
        }
    }

    private void setupClickListeners() {
        binding.btnCancelCreate.setOnClickListener(v -> {
            if (home != null) home.onBackPressed();
        });

        binding.btnAddImage.setOnClickListener(v -> openImagePicker());
        binding.imgLogo.setOnClickListener(v -> openProfilePicker());

        binding.etLocationLink.setOnClickListener(v -> {
            saveDraftToVm();
            if (home != null) home.LoadFragment(new MapPickerFragment());
        });

        binding.btnSaveFarm.setOnClickListener(v -> validateAndSaveLocation());
        binding.btnAddGrowing.setOnClickListener(v -> addGrowingItem());

        binding.etGrowingInput.setOnEditorActionListener((v, actionId, event) -> {
            addGrowingItem();
            return true;
        });

        getParentFragmentManager().setFragmentResultListener(
                "map_picker_result",
                getViewLifecycleOwner(),
                (k, r) -> {
                    vm.latitude.setValue(r.getDouble("latitude"));
                    vm.longitude.setValue(r.getDouble("longitude"));
                    vm.locationAddress.setValue(r.getString("address", "ទីតាំងបានជ្រើសរើស"));
                }
        );
    }

    private void saveDraftToVm() {
        vm.farmName.setValue(binding.etFarmName.getText().toString());
        vm.phone.setValue(binding.etPhone.getText().toString());
        vm.about.setValue(binding.etAbout.getText().toString());
        vm.locationAddress.setValue(binding.etLocationLink.getText().toString());
    }

    private void addGrowingItem() {
        String item = binding.etGrowingInput.getText().toString().trim();
        if (TextUtils.isEmpty(item)) {
            binding.etGrowingInput.setError("សូមបញ្ចូលដំណាំ");
            binding.etGrowingInput.requestFocus();
            return;
        }

        String normalized = item.replaceAll("\\s+", " ").trim();
        ArrayList<String> current = nonNullList(vm.growingList.getValue());
        for (String existing : current) {
            if (existing.equalsIgnoreCase(normalized)) {
                Toast.makeText(requireContext(), "បានបន្ថែមរួចហើយ", Toast.LENGTH_SHORT).show();
                binding.etGrowingInput.setText("");
                return;
            }
        }

        current.add(normalized);
        vm.growingList.setValue(current);
        binding.etGrowingInput.setText("");
    }

    private void refreshGrowingChips(List<String> list) {
        binding.chipGroupGrowing.removeAllViews();

        for (int i = 0; i < list.size(); i++) {
            String text = list.get(i);

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
                ArrayList<String> cur = nonNullList(vm.growingList.getValue());
                if (pos >= 0 && pos < cur.size()) {
                    cur.remove(pos);
                    vm.growingList.setValue(cur);
                }
            });

            binding.chipGroupGrowing.addView(chip);
        }
    }

    private void setupImagePickers() {
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleImagePickerResult
        );

        profilePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
                    Uri uri = result.getData().getData();
                    vm.profileUri.setValue(uri);
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
        profilePicker.launch(Intent.createChooser(intent, "ជ្រើសរើសរូបភាពតំណាង"));
    }

    private void handleImagePickerResult(ActivityResult result) {
        if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;

        Intent data = result.getData();
        ArrayList<Uri> current = nonNullList(vm.imageUris.getValue());

        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            int count = clipData.getItemCount();
            for (int i = 0; i < Math.min(count, 5); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null && !current.contains(uri)) current.add(uri);
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            if (uri != null && !current.contains(uri)) current.add(uri);
        }

        if (current.size() > 5) {
            current.subList(5, current.size()).clear();
            Toast.makeText(requireContext(), "ត្រូវជ្រើសរូបភាពយ៉ាងច្រើនបំផុត ៥ រូប", Toast.LENGTH_SHORT).show();
        }

        vm.imageUris.setValue(current);
    }

    private void showCurrentImages(List<Uri> uris) {
        LinearLayout container = binding.layoutSelectedImages;
        container.removeAllViews();

        if (uris.isEmpty()) {
            binding.horizontalImageScroll.setVisibility(View.GONE);
            return;
        } else {
            binding.horizontalImageScroll.setVisibility(View.VISIBLE);
        }

        int imageSize = dpToPx(100);
        int marginEnd = dpToPx(8);
        int removeSize = dpToPx(22);

        for (int i = 0; i < uris.size(); i++) {
            Uri uri = uris.get(i);

            FrameLayout frameLayout = new FrameLayout(requireContext());
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(imageSize, imageSize);
            if (i < uris.size() - 1) frameParams.setMarginEnd(marginEnd);
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
                ArrayList<Uri> cur = nonNullList(vm.imageUris.getValue());
                if (position >= 0 && position < cur.size()) {
                    cur.remove(position);
                    vm.imageUris.setValue(cur);
                }
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
        saveDraftToVm();

        String farmName = nonNull(vm.farmName.getValue());
        String category = nonNull(vm.selectedCategory.getValue(), "Market");
        String phone = nonNull(vm.phone.getValue());
        String locationLink = nonNull(vm.locationAddress.getValue());
        String about = nonNull(vm.about.getValue());

        ArrayList<Uri> images = nonNullList(vm.imageUris.getValue());
        Uri pUri = vm.profileUri.getValue();
        Double lat = vm.latitude.getValue();
        Double lng = vm.longitude.getValue();
        ArrayList<String> grow = nonNullList(vm.growingList.getValue());

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "សូមចូលទៅក្នុងគណនីរបស់អ្នកជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(farmName)) {
            binding.etFarmName.setError("សូមបញ្ចូលឈ្មោះកសិដ្ឋាន");
            binding.etFarmName.requestFocus();
            return;
        }

        if (images.size() < 5) {
            Toast.makeText(requireContext(), "សូមបន្ថែមរូបភាពយ៉ាងហោចណាស់ ៥ រូប", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pUri == null) {
            Toast.makeText(requireContext(), "សូមជ្រើសរើសរូបភាពតំណាង", Toast.LENGTH_SHORT).show();
            return;
        }

        if (lat == null || lng == null) {
            Toast.makeText(requireContext(), "សូមជ្រើសរើសទីតាំងលើផែនទី", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            binding.etPhone.setError("សូមបញ្ចូលលេខទូរស័ព្ទ");
            binding.etPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(locationLink)) {
            binding.etLocationLink.setError("សូមជ្រើសរើសទីតាំង");
            binding.etLocationLink.requestFocus();
            return;
        }

        String locationFolderId = UUID.randomUUID().toString();

        setSavingState(true);

        uploadProfileImage(locationFolderId, pUri, new UploadCallback() {
            @Override
            public void onSuccess(String profileUrl) {
                uploadFarmPhotos(locationFolderId, images, new ImagesUploadCallback() {
                    @Override
                    public void onSuccess(List<String> photoUrls) {
                        saveLocationViaRepository(
                                farmName,
                                category,
                                profileUrl,
                                photoUrls,
                                phone,
                                locationLink,
                                grow,
                                about,
                                lat,
                                lng
                        );
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        setSavingState(false);
                        Toast.makeText(requireContext(), "មិនអាចផ្ទុករូបភាពបាន: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                setSavingState(false);
                Toast.makeText(requireContext(), "មិនអាចផ្ទុករូបភាពតំណាងបាន: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadProfileImage(String locationFolderId, Uri profileUri, UploadCallback callback) {
        String filename = "profile_" + UUID.randomUUID() + ".jpg";
        StorageReference profileRef = storage.getReference()
                .child("locations")
                .child(locationFolderId)
                .child(filename);

        UploadTask uploadTask = profileRef.putFile(profileUri);

        uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return profileRef.getDownloadUrl();
                }).addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void uploadFarmPhotos(String locationFolderId, List<Uri> imageUris, ImagesUploadCallback callback) {
        List<String> downloadUrls = new ArrayList<>();

        for (Uri uri : imageUris) {
            String filename = UUID.randomUUID() + ".jpg";
            StorageReference photoRef = storage.getReference()
                    .child("locations")
                    .child(locationFolderId)
                    .child(filename);

            UploadTask uploadTask = photoRef.putFile(uri);

            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return photoRef.getDownloadUrl();
            }).addOnSuccessListener(downloadUri -> {
                downloadUrls.add(downloadUri.toString());
                if (downloadUrls.size() == imageUris.size()) callback.onSuccess(downloadUrls);
            }).addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        }
    }

    private void saveLocationViaRepository(String farmName,
                                           String category,
                                           String profileUrl,
                                           List<String> photoUrls,
                                           String phone,
                                           String locationLink,
                                           List<String> growingList,
                                           String about,
                                           double lat,
                                           double lng) {

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "សូមចូលទៅក្នុងគណនីរបស់អ្នកជាមុន", Toast.LENGTH_SHORT).show();
            setSavingState(false);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        Location.Owner owner = new Location.Owner(currentUser.getUid());
        Location.Contact contact = new Location.Contact(phone, locationLink);
        Location.Detail detail = new Location.Detail(growingList, about);
        Location.Visibility visibility = new Location.Visibility(true);

        Location location = new Location(
                owner,
                farmName,
                category,
                "active",
                lat,
                lng,
                profileUrl,
                photoUrls,
                contact,
                detail,
                visibility,
                timestamp
        );

        locationRepository.createLocation(location, new LocationRepository.LocationCallback<String>() {
            @Override
            public void onSuccess(String result) {
                setSavingState(false);
                Toast.makeText(requireContext(), "បានបង្កើតកសិដ្ឋានដោយជោគជ័យ", Toast.LENGTH_SHORT).show();
                if (home != null) home.onBackPressed();
            }

            @Override
            public void onFailure(String error) {
                setSavingState(false);
                Toast.makeText(requireContext(), "មិនអាចរក្សាទុកបាន: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSavingState(boolean isSaving) {
        binding.btnSaveFarm.setEnabled(!isSaving);
        binding.btnAddImage.setEnabled(!isSaving);
        binding.imgLogo.setEnabled(!isSaving);
        binding.btnFarm.setEnabled(!isSaving);
        binding.btnMarket.setEnabled(!isSaving);
        binding.btnAddGrowing.setEnabled(!isSaving);
        binding.etGrowingInput.setEnabled(!isSaving);

        if (isSaving) binding.btnSaveFarm.setText("កំពុងរក្សាទុក...");
        else binding.btnSaveFarm.setText("រក្សាទុកកំណត់ត្រា");
    }

    private String nonNull(String v) {
        return v == null ? "" : v;
    }

    private String nonNull(String v, String def) {
        return TextUtils.isEmpty(v) ? def : v;
    }

    private <T> ArrayList<T> nonNullList(ArrayList<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    private <T> ArrayList<T> nonNullList(List<T> list) {
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeActivity) {
            home = (HomeActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
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
