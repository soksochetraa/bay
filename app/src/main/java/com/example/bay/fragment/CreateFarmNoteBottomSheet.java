package com.example.bay.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.databinding.ItemModalCreateFarmNoteBinding;
import com.example.bay.model.Location;
import com.example.bay.repository.FarmMapRepository;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateFarmNoteBottomSheet extends BottomSheetDialogFragment {

    private ItemModalCreateFarmNoteBinding binding;

    private final List<Uri> selectedImages = new ArrayList<>();
    private int currentSlotIndex = -1;

    private FarmMapRepository repository;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private FirebaseStorage storage;


    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repository = new FarmMapRepository();
        mAuth = FirebaseAuth.getInstance();

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleImagePickerResult
        );

        for (int i = 0; i < 5; i++) {
            selectedImages.add(null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = ItemModalCreateFarmNoteBinding.inflate(inflater, container, false);
        setupImageSlots();
        setupButtons();
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null && getActivity() != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int height = metrics.heightPixels;
                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                params.height = height;
                bottomSheet.setLayoutParams(params);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(height);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    private void setupImageSlots() {
        View.OnClickListener listener = v -> {
            if (v == binding.imgSlot1) currentSlotIndex = 0;
            else if (v == binding.imgSlot2) currentSlotIndex = 1;
            else if (v == binding.imgSlot3) currentSlotIndex = 2;
            else if (v == binding.imgSlot4) currentSlotIndex = 3;
            else if (v == binding.imgSlot5) currentSlotIndex = 4;
            openImagePicker();
        };

        binding.imgSlot1.setOnClickListener(listener);
        binding.imgSlot2.setOnClickListener(listener);
        binding.imgSlot3.setOnClickListener(listener);
        binding.imgSlot4.setOnClickListener(listener);
        binding.imgSlot5.setOnClickListener(listener);
    }

    private void openImagePicker() {
        if (currentSlotIndex < 0 || currentSlotIndex > 4) return;
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void handleImagePickerResult(ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
        Uri uri = result.getData().getData();
        if (uri == null) return;

        selectedImages.set(currentSlotIndex, uri);

        ImageView target;
        if (currentSlotIndex == 0) target = (ImageView) binding.imgSlot1.getChildAt(0);
        else if (currentSlotIndex == 1) target = (ImageView) binding.imgSlot2.getChildAt(0);
        else if (currentSlotIndex == 2) target = (ImageView) binding.imgSlot3.getChildAt(0);
        else if (currentSlotIndex == 3) target = (ImageView) binding.imgSlot4.getChildAt(0);
        else target = (ImageView) binding.imgSlot5.getChildAt(0);

        Glide.with(this)
                .load(uri)
                .centerCrop()
                .placeholder(R.drawable.img)
                .error(R.drawable.img)
                .into(target);
    }

    private void setupButtons() {
        binding.btnCloseCreate.setOnClickListener(v -> dismiss());
        binding.btnSaveFarm.setOnClickListener(v -> {
            if (!validateForm()) return;
            saveFarmNote();
        });
    }

    private boolean validateForm() {
        if (TextUtils.isEmpty(binding.etFarmName.getText())) return false;
        int count = 0;
        for (Uri uri : selectedImages) if (uri != null) count++;
        return count >= 5;
    }

    private void saveFarmNote() {
        String locationId = database.getReference("locations").push().getKey();
        if (locationId == null) return;

        List<String> uploadedUrls = new ArrayList<>();
        uploadNextImage(locationId, 0, uploadedUrls);
    }

    private void uploadNextImage(String locationId, int index, List<String> urls) {
        if (index >= selectedImages.size()) {
            saveLocationData(locationId, urls);
            return;
        }

        Uri uri = selectedImages.get(index);
        if (uri == null) {
            uploadNextImage(locationId, index + 1, urls);
            return;
        }

        StorageReference ref = storage.getReference()
                .child("locations")
                .child(locationId)
                .child("photo_" + index + ".jpg");

        ref.putFile(uri)
                .continueWithTask(task -> ref.getDownloadUrl())
                .addOnSuccessListener(downloadUri -> {
                    urls.add(downloadUri.toString());
                    uploadNextImage(locationId, index + 1, urls);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Upload failed", Toast.LENGTH_SHORT).show()
                );
    }

    private void saveLocationData(String id, List<String> photoUrls) {
        Location loc = new Location();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) loc.owner = new Location.Owner(user.getUid());

        loc.name = binding.etFarmName.getText().toString().trim();
        loc.category = binding.spinnerCategory.getSelectedItem().toString();
        loc.status = "active";
        loc.latitude = Double.NaN;
        loc.longitude = Double.NaN;

        loc.contact = new Location.Contact(
                binding.etPhone.getText().toString().trim(),
                binding.etLocationLink.getText().toString().trim(),
                binding.etFacebook.getText().toString().trim(),
                binding.etTelegram.getText().toString().trim(),
                binding.etTiktok.getText().toString().trim()
        );

        Location.Detail detail = new Location.Detail();
        detail.about = binding.etAbout.getText().toString().trim();
        loc.detail = detail;

        loc.photos = photoUrls;
        loc.visibility = new Location.Visibility(true);
        loc.createdAt = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.getDefault()
        ).format(new Date());

        database.getReference("locations")
                .child(id)
                .setValue(loc)
                .addOnSuccessListener(v -> {
                    Toast.makeText(getContext(), "Saved successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Save failed", Toast.LENGTH_SHORT).show()
                );
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
