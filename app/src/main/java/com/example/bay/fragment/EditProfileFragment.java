package com.example.bay.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.PhoneNumberVerifyFragment;
import com.example.bay.databinding.FragmentEditProfileBinding;
import com.example.bay.model.User;
import com.example.bay.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class EditProfileFragment extends Fragment {

    private String userId;
    private FragmentEditProfileBinding binding;
    private UserRepository userRepository;
    private User currentUser;
    private Uri selectedImageUri;

    public static EditProfileFragment newInstance(String userId) {
        EditProfileFragment fragment = new EditProfileFragment();
        Bundle b = new Bundle();
        b.putString("user_id", userId);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository();
        userId = getArguments().getString("user_id");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        loadUser();
        binding.profileImage.setOnClickListener(v -> openGallery());
        binding.tvChangePhoto.setOnClickListener(v -> openGallery());
        binding.btnSave.setOnClickListener(v -> saveProfile());
        binding.btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void loadUser() {
        ((HomeActivity) requireActivity()).showLoading();
        userRepository.getUserById(userId, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User u) {
                currentUser = u;
                binding.etFirstName.setText(u.getFirst_name());
                binding.etLastName.setText(u.getLast_name());
                binding.tvEmailDisplay.setText(u.getEmail());
                binding.tvPhoneDisplay.setText(u.getPhone());
                binding.tvLocation.setText(u.getLocation());
                binding.etBio.setText(u.getBio());
                binding.tvBioCounter.setText((u.getBio() == null ? 0 : u.getBio().length()) + " / 100");
                if (!TextUtils.isEmpty(u.getProfileImageUrl())) {
                    Glide.with(EditProfileFragment.this).load(u.getProfileImageUrl()).into(binding.profileImage);
                }
                ((HomeActivity) requireActivity()).hideLoading();
            }

            @Override
            public void onError(String e) {
                ((HomeActivity) requireActivity()).hideLoading();
                Toast.makeText(getContext(), e, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String first = binding.etFirstName.getText().toString().trim();
        String last = binding.etLastName.getText().toString().trim();
        String bio = binding.etBio.getText().toString().trim();
        String email = binding.tvEmailDisplay.getText().toString().trim();
        String phone = binding.tvPhoneDisplay.getText().toString().trim();

        if (TextUtils.isEmpty(first) || TextUtils.isEmpty(last)) return;

        boolean emailChanged = !TextUtils.equals(email, currentUser.getEmail());
        boolean phoneChanged = !TextUtils.equals(phone, currentUser.getPhone());

        currentUser.setFirst_name(first);
        currentUser.setLast_name(last);
        currentUser.setBio(bio);
        currentUser.setLocation(binding.tvLocation.getText().toString());

        ((HomeActivity) requireActivity()).showLoading();

        if (emailChanged) {
            userRepository.checkEmailExists(email, userId, new UserRepository.BoolCallback() {
                @Override
                public void onResult(boolean exists) {
                    if (exists) {
                        ((HomeActivity) requireActivity()).hideLoading();
                        Toast.makeText(getContext(), "Email already used", Toast.LENGTH_SHORT).show();
                    } else {
                        verifyEmail(email);
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    ((HomeActivity) requireActivity()).hideLoading();
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        if (phoneChanged) {
            userRepository.checkPhoneExists(phone, userId, new UserRepository.BoolCallback() {
                @Override
                public void onResult(boolean exists) {
                    if (exists) {
                        ((HomeActivity) requireActivity()).hideLoading();
                        Toast.makeText(getContext(), "Phone already used", Toast.LENGTH_SHORT).show();
                    } else {
                        Bundle b = new Bundle();
                        b.putString("phone", phone);
                        b.putString("userId", userId);
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(android.R.id.content, PhoneNumberVerifyFragment.class, b)
                                .addToBackStack(null)
                                .commit();
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    ((HomeActivity) requireActivity()).hideLoading();
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        if (selectedImageUri != null) uploadImage(); else updateUser();
    }

    private void verifyEmail(String email) {
        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        fu.updateEmail(email).addOnSuccessListener(v ->
                fu.sendEmailVerification().addOnSuccessListener(v2 -> {
                    ((HomeActivity) requireActivity()).hideLoading();
                    Toast.makeText(getContext(), "Verify email sent", Toast.LENGTH_LONG).show();
                })
        ).addOnFailureListener(e -> {
            ((HomeActivity) requireActivity()).hideLoading();
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadImage() {
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("profile_images/" + userId + "_" + UUID.randomUUID() + ".jpg");

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(t ->
                        ref.getDownloadUrl().addOnSuccessListener(u -> {
                            currentUser.setProfileImageUrl(u.toString());
                            updateUser();
                        })
                )
                .addOnFailureListener(e -> {
                    ((HomeActivity) requireActivity()).hideLoading();
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUser() {
        userRepository.updateUser(userId, currentUser, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User r) {
                ((HomeActivity) requireActivity()).hideLoading();
                requireActivity().onBackPressed();
            }

            @Override
            public void onError(String e) {
                ((HomeActivity) requireActivity()).hideLoading();
                Toast.makeText(getContext(), e, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePicker.launch(i);
    }

    private final ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null) {
                    selectedImageUri = r.getData().getData();
                    binding.profileImage.setImageURI(selectedImageUri);
                }
            });

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
