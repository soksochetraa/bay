package com.example.bay.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.bay.databinding.FragmentChangeOrVerifyEmailBinding;
import com.example.bay.model.User;
import com.example.bay.repository.UserRepository;
import com.example.bay.viewmodel.SharedUserViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import com.example.bay.HomeActivity;

public class ChangeOrVerifyEmailFragment extends Fragment {
    private FragmentChangeOrVerifyEmailBinding binding;
    private UserRepository userRepository;
    private String userId;
    private User currentUser;

    public ChangeOrVerifyEmailFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChangeOrVerifyEmailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userRepository = new UserRepository();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        SharedUserViewModel sharedUserViewModel = new ViewModelProvider(requireActivity()).get(SharedUserViewModel.class);
        
        ((HomeActivity) requireActivity()).showLoading();
        sharedUserViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            ((HomeActivity) requireActivity()).hideLoading();
            if (user != null) {
                currentUser = user;
                if (!TextUtils.isEmpty(user.getEmail())) {
                    binding.etEmail.setText(user.getEmail());
                }
            }
        });

        binding.button.setOnClickListener(v -> requireActivity().onBackPressed());

        binding.btnNext.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(getContext(), "Please enter email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
                return;
            }
            checkAndProcessEmail(email);
        });
    }


    private void checkAndProcessEmail(String newEmail) {
        ((HomeActivity) requireActivity()).showLoading();
        userRepository.checkEmailExists(newEmail, userId, new UserRepository.BoolCallback() {
            @Override
            public void onResult(boolean exists) {
                if (exists) {
                    ((HomeActivity) requireActivity()).hideLoading();
                    Toast.makeText(getContext(), "Email already used by another account", Toast.LENGTH_SHORT).show();
                } else {
                    sendVerificationEmail(newEmail);
                }
            }

            @Override
            public void onError(String errorMsg) {
                ((HomeActivity) requireActivity()).hideLoading();
                Toast.makeText(getContext(), "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendVerificationEmail(String newEmail) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            ((HomeActivity) requireActivity()).hideLoading();
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseUser.updateEmail(newEmail)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        firebaseUser.sendEmailVerification()
                                .addOnCompleteListener(verificationTask -> {
                                    ((HomeActivity) requireActivity()).hideLoading();

                                    if (verificationTask.isSuccessful()) {
                                        updateUserEmailInDatabase(newEmail);
                                        Toast.makeText(getContext(),
                                                "Verification email sent to " + newEmail,
                                                Toast.LENGTH_LONG).show();
                                        ((HomeActivity) requireActivity()).LoadFragment(new SentEmailFragment());
                                    } else {
                                        rollbackEmailChange(firebaseUser, currentUser.getEmail());
                                        Toast.makeText(getContext(),
                                                "Failed to send verification",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        ((HomeActivity) requireActivity()).hideLoading();
                        Toast.makeText(getContext(),
                                "Failed to update email: " + (task.getException() != null ?
                                        task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserEmailInDatabase(String newEmail) {
        if (currentUser == null) return;

        currentUser.setEmail(newEmail);
        currentUser.setEmailVerified(false);

        userRepository.updateUser(userId, currentUser, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User user) {
                Toast.makeText(getContext(),
                        "Email updated. Please verify your new email.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMsg) {
                rollbackEmailChange(FirebaseAuth.getInstance().getCurrentUser(), currentUser.getEmail());
                Toast.makeText(getContext(),
                        "Failed to update profile. Changes reverted.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rollbackEmailChange(FirebaseUser user, String oldEmail) {
        if (user == null || TextUtils.isEmpty(oldEmail)) return;
        user.updateEmail(oldEmail);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}