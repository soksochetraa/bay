package com.example.bay.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.databinding.FragmentEditProfileBinding;
import com.example.bay.model.User;
import com.example.bay.repository.UserRepository;
import com.example.bay.viewmodel.SharedUserViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;

public class EditProfileFragment extends Fragment {
    private String userId;
    private FragmentEditProfileBinding binding;
    private UserRepository userRepository;
    private SharedUserViewModel sharedUserViewModel;
    private User currentUser;
    private Uri selectedImageUri;
    private String pendingAddress;
    private boolean userLoaded = false;
    private static final long NAME_CHANGE_COOLDOWN = 60L * 24 * 60 * 60 * 1000;

    private String originalBio = "";
    private String originalLocation = "";
    private boolean hasNewImage = false;

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
        sharedUserViewModel = new ViewModelProvider(requireActivity()).get(SharedUserViewModel.class);
        userId = getArguments().getString("user_id");

        getParentFragmentManager().setFragmentResultListener(
                "map_picker_result",
                this,
                (requestKey, result) -> {
                    pendingAddress = result.getString("address", "ទីតាំងបានជ្រើសរើស");
                    if (binding != null) {
                        binding.tvLocation.setText(pendingAddress);
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBackNavigation();
                    }
                }
        );

        if (pendingAddress != null) {
            binding.tvLocation.setText(pendingAddress);
        }

        ((HomeActivity) requireActivity()).showLoading();
        sharedUserViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            ((HomeActivity) requireActivity()).hideLoading();
            if (user != null) {
                currentUser = user;
                if (!userLoaded) {
                    userLoaded = true;
                    restoreUiFromCurrentUser();
                }
            }
        });

        binding.btnFullName.setOnClickListener(v -> {
            if (TextUtils.isEmpty(userId)) {
                Toast.makeText(requireContext(), "Invalid user session", Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            ChangeNameFragment fragment = new ChangeNameFragment();
            fragment.setArguments(bundle);
            ((HomeActivity) requireActivity()).LoadFragment(fragment);
        });

        binding.profileImage.setOnClickListener(v -> openGallery());
        binding.tvChangePhoto.setOnClickListener(v -> openGallery());

        binding.btnAddPhoneNumber.setOnClickListener(v -> {
            ((HomeActivity) requireActivity()).LoadFragment(new AddPhoneNumberFragment());
        });

        binding.btnChangeEmail.setOnClickListener(v -> {
            ((HomeActivity) requireActivity()).LoadFragment(new ChangeOrVerifyEmailFragment());
        });

        binding.btnChnagePhoneNumber.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("phone", currentUser != null ? currentUser.getPhone() : "");

            ChangePhoneNumberFragment fragment = new ChangePhoneNumberFragment();
            fragment.setArguments(bundle);

            ((HomeActivity) requireActivity()).LoadFragment(fragment);
        });

        binding.btnAddEmail.setOnClickListener(v -> {
            ((HomeActivity) requireActivity()).LoadFragment(new ChangeOrVerifyEmailFragment());
        });

        binding.tvLocation.setOnClickListener(v -> {
            HomeActivity home = (HomeActivity) getActivity();
            if (home != null) home.LoadFragment(new MapPickerFragment());
        });

        binding.etBio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvBioCounter.setText(s.length() + " / 100");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollContent, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            int bottomPadding = Math.max(imeInsets.bottom, navInsets.bottom);
            binding.scrollContent.setPadding(
                    binding.scrollContent.getPaddingLeft(),
                    binding.scrollContent.getPaddingTop(),
                    binding.scrollContent.getPaddingRight(),
                    bottomPadding
            );

            if (imeInsets.bottom > 0) {
                binding.scrollContent.postDelayed(() -> {
                    View focused = binding.scrollContent.findFocus();
                    if (focused != null && binding != null) {
                        binding.scrollContent.smoothScrollTo(0, focused.getBottom());
                    }
                }, 100);
            }

            return insets;
        });

        binding.btnSave.setOnClickListener(v -> saveProfile());

        binding.btnBack.setOnClickListener(v -> handleBackNavigation());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            checkEmailVerificationStatus();
        }
    }

    private void restoreUiFromCurrentUser() {
        if (currentUser == null || binding == null) return;

        User u = currentUser;
        Long lastChangedAtObj = u.getLastNameChangedAt();
        long lastChangedAt = lastChangedAtObj != null ? lastChangedAtObj : 0L;

        if (!canChangeName(lastChangedAt)) {
            binding.btnFullName.setEnabled(false);
            binding.btnFullName.setAlpha(0.5f);
            long daysLeft = getRemainingDays(lastChangedAt);
            binding.tvNameChangeNotice.setVisibility(View.VISIBLE);
            binding.tvNameChangeNotice.setText("អ្នកអាចប្ដូរឈ្មោះម្ដងទៀតក្រោយ " + daysLeft + " ថ្ងៃ");
        } else {
            binding.btnFullName.setEnabled(true);
            binding.btnFullName.setAlpha(1f);
            binding.tvNameChangeNotice.setVisibility(View.GONE);
        }

        if (u.getEmail().isEmpty()) {
            binding.etEmail.setVisibility(View.GONE);
            binding.btnChangeEmail.setVisibility(View.GONE);
            binding.verifiedEmail.setVisibility(View.GONE);
            binding.btnAddEmail.setVisibility(View.VISIBLE);
        } else {
            if (u.isEmailVerified()) {
                binding.etEmail.setVisibility(View.GONE);
                binding.btnChangeEmail.setVisibility(View.GONE);
                binding.verifiedEmail.setText(u.getEmail());
                binding.verifiedEmail.setVisibility(View.VISIBLE);
                binding.btnAddEmail.setVisibility(View.GONE);
            } else {
                binding.etEmail.setText(u.getEmail());
                binding.etEmail.setVisibility(View.VISIBLE);
                binding.btnChangeEmail.setVisibility(View.VISIBLE);
                binding.verifiedEmail.setVisibility(View.GONE);
                binding.btnAddEmail.setVisibility(View.GONE);
            }
        }

        if (u.getPhone().isEmpty()) {
            binding.etPhoneNumber.setVisibility(View.GONE);
            binding.btnChnagePhoneNumber.setVisibility(View.GONE);
            binding.btnAddPhoneNumber.setVisibility(View.VISIBLE);
            binding.verifiedPhoneNumber.setVisibility(View.GONE);
        } else {
            if (u.isPhoneVerified()) {
                binding.etPhoneNumber.setVisibility(View.GONE);
                binding.btnChnagePhoneNumber.setVisibility(View.GONE);
                binding.verifiedPhoneNumber.setText(u.getPhone());
                binding.verifiedPhoneNumber.setVisibility(View.VISIBLE);
                binding.btnAddPhoneNumber.setVisibility(View.GONE);
            } else {
                binding.etPhoneNumber.setText(u.getPhone());
                binding.etPhoneNumber.setVisibility(View.VISIBLE);
                binding.btnChnagePhoneNumber.setVisibility(View.VISIBLE);
                binding.verifiedPhoneNumber.setVisibility(View.GONE);
                binding.btnAddPhoneNumber.setVisibility(View.GONE);
            }
        }

        binding.btnFullName.setText(u.getFirstName() + " " + u.getLastName());

        if (pendingAddress != null) {
            binding.tvLocation.setText(pendingAddress);
        } else {
            binding.tvLocation.setText(u.getLocation());
        }

        binding.etBio.setText(u.getBio());
        binding.tvBioCounter.setText((u.getBio() == null ? 0 : u.getBio().length()) + " / 100");
        if (!TextUtils.isEmpty(u.getProfileImageUrl())) {
            Glide.with(EditProfileFragment.this).load(u.getProfileImageUrl()).into(binding.profileImage);
        }
        originalBio = u.getBio() != null ? u.getBio() : "";
        originalLocation = pendingAddress != null ? pendingAddress : (u.getLocation() != null ? u.getLocation() : "");
        hasNewImage = false;
    }

    private void saveProfile() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String newBio = binding.etBio.getText().toString().trim();
        currentUser.setBio(newBio);

        String newLocation = binding.tvLocation.getText().toString().trim();
        if (!TextUtils.isEmpty(newLocation)) {
            currentUser.setLocation(newLocation);
        }

        ((HomeActivity) requireActivity()).showLoading();

        if (selectedImageUri != null) {
            uploadImage();
        } else {
            updateUser();
        }
    }

    private void checkEmailVerificationStatus() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            firebaseUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    boolean isVerified = firebaseUser.isEmailVerified();
                    if (currentUser != null && currentUser.isEmailVerified() != isVerified) {
                        currentUser.setEmailVerified(isVerified);
                        userRepository.updateUser(userId, currentUser, new UserRepository.UserCallback<User>() {
                            @Override
                            public void onSuccess(User user) {
                                if (isVerified) {
                                    binding.etEmail.setVisibility(View.GONE);
                                    binding.btnChangeEmail.setVisibility(View.GONE);
                                    binding.verifiedEmail.setText(user.getEmail());
                                    binding.verifiedEmail.setVisibility(View.VISIBLE);
                                    binding.btnAddEmail.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), "✓ Email verified successfully!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onError(String errorMsg) {}
                        });
                    }
                }
            });
        }
    }

    private boolean canChangeName(long lastChangedAt) {
        return System.currentTimeMillis() - lastChangedAt >= NAME_CHANGE_COOLDOWN;
    }

    private long getRemainingDays(long lastChangedAt) {
        long remaining = NAME_CHANGE_COOLDOWN - (System.currentTimeMillis() - lastChangedAt);
        return Math.max(0, remaining / (24L * 60 * 60 * 1000));
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
                Toast.makeText(getContext(), "រក្សាទុកបានជោគជ័យ!", Toast.LENGTH_SHORT).show();
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
                    hasNewImage = true;
                    binding.profileImage.setImageURI(selectedImageUri);
                }
            });

    private boolean hasUnsavedChanges() {
        if (binding == null) return false;

        String currentBio = binding.etBio.getText().toString().trim();
        String currentLocation = binding.tvLocation.getText().toString().trim();

        boolean bioChanged = !currentBio.equals(originalBio);
        boolean locationChanged = !currentLocation.equals(originalLocation);

        return bioChanged || locationChanged || hasNewImage;
    }

    private void handleBackNavigation() {
        if (hasUnsavedChanges()) {
            ((HomeActivity) requireActivity()).showDialog(
                    "អ្នកមានការផ្លាស់ប្ដូរដែលមិនទាន់រក្សាទុក\nតើអ្នកចង់រក្សាទុកមុនពេលចាកចេញ?",
                    "រក្សាទុក",
                    "បោះបង់",
                    () -> saveProfile(),
                    () -> ((HomeActivity) requireActivity()).navigateToMyProfile(),
                    false
            );
        } else {
            ((HomeActivity) requireActivity()).navigateToMyProfile();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}