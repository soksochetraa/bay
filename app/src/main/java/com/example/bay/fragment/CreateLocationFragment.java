package com.example.bay.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.MapPickerFragment;
import com.example.bay.databinding.FragmentCreateLocationBinding;
import com.example.bay.databinding.ItemEditPostImageBinding;
import com.example.bay.model.Location;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateLocationFragment extends Fragment {

    private FragmentCreateLocationBinding binding;
    private final List<Uri> imageUris = new ArrayList<>();
    private Uri logoUri;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseStorage storage;

    private ActivityResultLauncher<Intent> imagePicker;
    private ActivityResultLauncher<Intent> logoPicker;

    private Double lat;
    private Double lng;

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
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        imagePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onImagesPicked
        );

        logoPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onLogoPicked
        );

        binding.btnAddImage.setOnClickListener(v -> openImagePicker());
        binding.ivLogo.setOnClickListener(v -> openLogoPicker());

        binding.etLocationLink.setOnClickListener(v ->
                home.LoadFragment(new MapPickerFragment())
        );

        getParentFragmentManager().setFragmentResultListener(
                "map_picker_result",
                getViewLifecycleOwner(),
                (k, r) -> {
                    lat = r.getDouble("latitude");
                    lng = r.getDouble("longitude");
                    binding.etLocationLink.setText("Selected");
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePicker.launch(intent);
    }

    private void openLogoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        logoPicker.launch(intent);
    }

    private void onLogoPicked(ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
        logoUri = result.getData().getData();
        Glide.with(this).load(logoUri).into(binding.ivLogo);
    }

    private void onImagesPicked(ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;

        imageUris.clear();
        binding.layoutSelectedImages.removeAllViews();

        if (result.getData().getClipData() != null) {
            int count = Math.min(5, result.getData().getClipData().getItemCount());
            for (int i = 0; i < count; i++) {
                imageUris.add(result.getData().getClipData().getItemAt(i).getUri());
            }
        } else if (result.getData().getData() != null) {
            imageUris.add(result.getData().getData());
        }

        for (int i = 0; i < imageUris.size(); i++) {
            ItemEditPostImageBinding item =
                    ItemEditPostImageBinding.inflate(getLayoutInflater(), binding.layoutSelectedImages, false);

            int index = i;
            Glide.with(this).load(imageUris.get(i)).into(item.ivPostImage);

            item.btnDeleteImage.setOnClickListener(v -> {
                imageUris.remove(index);
                binding.layoutSelectedImages.removeView(item.getRoot());
            });

            binding.layoutSelectedImages.addView(item.getRoot());
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        home = (HomeActivity) context;
        home.hideBottomNavigation();
    }
}
