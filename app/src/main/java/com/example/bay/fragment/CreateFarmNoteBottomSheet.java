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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.model.Location;
import com.example.bay.repository.FarmMapRepository;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateFarmNoteBottomSheet extends BottomSheetDialogFragment {

    private EditText etFarmName, etPhone, etLocationLink, etFacebook, etTelegram, etTiktok;
    private EditText etGrowing, etCertificates, etAbout;
    private Spinner spinnerCategory;
    private ShapeableImageView imgSlot1, imgSlot2, imgSlot3, imgSlot4, imgSlot5;
    private MaterialButton btnSaveFarm;
    private ImageButton btnCloseCreate;

    private final List<Uri> selectedImages = new ArrayList<>();
    private int currentSlotIndex = -1;

    private FarmMapRepository repository;
    private FirebaseAuth mAuth;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new FarmMapRepository();
        mAuth = FirebaseAuth.getInstance();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        handleImagePickerResult(result);
                    }
                }
        );

        // prepare list size = 5
        for (int i = 0; i < 5; i++) {
            selectedImages.add(null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_modal_create_farm_note, container, false);

        initViews(view);
        setupImageSlotClickListeners();
        setupButtons();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null && getActivity() != null) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenHeight = displayMetrics.heightPixels;

                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                params.height = screenHeight;
                bottomSheet.setLayoutParams(params);

                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(screenHeight);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    private void initViews(View view) {
        etFarmName = view.findViewById(R.id.etFarmName);
        etPhone = view.findViewById(R.id.etPhone);
        etLocationLink = view.findViewById(R.id.etLocationLink);
        etFacebook = view.findViewById(R.id.etFacebook);
        etTelegram = view.findViewById(R.id.etTelegram);
        etTiktok = view.findViewById(R.id.etTiktok);
        etGrowing = view.findViewById(R.id.etGrowing);
        etCertificates = view.findViewById(R.id.etCertificates);
        etAbout = view.findViewById(R.id.etAbout);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);

        imgSlot1 = view.findViewById(R.id.imgSlot1);
        imgSlot2 = view.findViewById(R.id.imgSlot2);
        imgSlot3 = view.findViewById(R.id.imgSlot3);
        imgSlot4 = view.findViewById(R.id.imgSlot4);
        imgSlot5 = view.findViewById(R.id.imgSlot5);

        btnSaveFarm = view.findViewById(R.id.btnSaveFarm);
        btnCloseCreate = view.findViewById(R.id.btnCloseCreate);
    }

    private void setupImageSlotClickListeners() {
        View.OnClickListener imageClickListener = v -> {
            if (v.getId() == R.id.imgSlot1) currentSlotIndex = 0;
            else if (v.getId() == R.id.imgSlot2) currentSlotIndex = 1;
            else if (v.getId() == R.id.imgSlot3) currentSlotIndex = 2;
            else if (v.getId() == R.id.imgSlot4) currentSlotIndex = 3;
            else if (v.getId() == R.id.imgSlot5) currentSlotIndex = 4;

            openImagePicker();
        };

        imgSlot1.setOnClickListener(imageClickListener);
        imgSlot2.setOnClickListener(imageClickListener);
        imgSlot3.setOnClickListener(imageClickListener);
        imgSlot4.setOnClickListener(imageClickListener);
        imgSlot5.setOnClickListener(imageClickListener);
    }

    private void openImagePicker() {
        if (getContext() == null) return;
        if (currentSlotIndex < 0 || currentSlotIndex > 4) return;

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void handleImagePickerResult(ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;

        Uri imageUri = result.getData().getData();
        if (imageUri == null || getContext() == null) return;
        if (currentSlotIndex < 0 || currentSlotIndex > 4) return;

        selectedImages.set(currentSlotIndex, imageUri);

        ShapeableImageView target;
        switch (currentSlotIndex) {
            case 0:
                target = imgSlot1;
                break;
            case 1:
                target = imgSlot2;
                break;
            case 2:
                target = imgSlot3;
                break;
            case 3:
                target = imgSlot4;
                break;
            case 4:
            default:
                target = imgSlot5;
                break;
        }

        Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .placeholder(R.drawable.img)
                .error(R.drawable.img)
                .into(target);
    }

    private void setupButtons() {
        btnCloseCreate.setOnClickListener(v -> dismiss());

        btnSaveFarm.setOnClickListener(v -> {
            if (!validateForm()) return;
            saveFarmNote();
        });
    }

    private boolean validateForm() {
        if (getContext() == null) return false;

        String name = etFarmName.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString()
                : "";

        if (TextUtils.isEmpty(name)) {
            etFarmName.setError("សូមបញ្ចូលឈ្មោះកសិដ្ឋាន");
            etFarmName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(category)) {
            Toast.makeText(getContext(),
                    "សូមជ្រើសប្រភេទកសិដ្ឋាន",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        int validCount = 0;
        for (Uri uri : selectedImages) {
            if (uri != null) validCount++;
        }
        if (validCount < 5) {
            Toast.makeText(getContext(),
                    "សូមបន្ថែមរូបភាពយ៉ាងហោចណាស់ ៥ រូប!",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveFarmNote() {
        if (getContext() == null) return;

        String name = etFarmName.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString()
                : "";

        String phone = etPhone.getText().toString().trim();
        String locationLink = etLocationLink.getText().toString().trim();
        String facebook = etFacebook.getText().toString().trim();
        String telegram = etTelegram.getText().toString().trim();
        String tiktok = etTiktok.getText().toString().trim();
        String growingRaw = etGrowing.getText().toString().trim();
        String certRaw = etCertificates.getText().toString().trim();
        String about = etAbout.getText().toString().trim();

        Location newLocation = new Location();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            newLocation.owner = new Location.Owner(currentUser.getUid());
        }

        newLocation.name = name;
        newLocation.category = category;
        newLocation.status = "active";

        newLocation.latitude = Double.NaN;
        newLocation.longitude = Double.NaN;

        Location.Contact contact = new Location.Contact(
                phone,
                locationLink,
                facebook,
                telegram,
                tiktok
        );
        newLocation.contact = contact;

        Location.Detail detail = new Location.Detail();
        if (!TextUtils.isEmpty(growingRaw)) {
            String[] parts = growingRaw.split(",");
            List<String> growingList = new ArrayList<>();
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    growingList.add(trimmed);
                }
            }
            detail.growing = growingList;
        }

        if (!TextUtils.isEmpty(certRaw)) {
            String[] parts = certRaw.split(",");
            List<Map<String, String>> certList = new ArrayList<>();
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    Map<String, String> map = new HashMap<>();
                    map.put("name", trimmed);
                    map.put("status", "Pending");
                    certList.add(map);
                }
            }
            detail.certificate = certList;
        }

        detail.about = about;
        newLocation.detail = detail;

        List<String> photoStrings = new ArrayList<>();
        for (Uri uri : selectedImages) {
            if (uri != null) {
                photoStrings.add(uri.toString());
            }
        }
        newLocation.photos = photoStrings;

        newLocation.visibility = new Location.Visibility(true);

        String createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                .format(new Date());
        newLocation.createdAt = createdAt;

        repository.createLocation(newLocation, new FarmMapRepository.LocationCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getContext() == null) return;
                Toast.makeText(getContext(),
                        "រក្សាទុកកំណត់ត្រាកសិដ្ឋានបានជោគជ័យ!",
                        Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;
                Toast.makeText(getContext(),
                        "បរាជ័យក្នុងការរក្សាទុក: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
