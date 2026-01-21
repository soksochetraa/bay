package com.example.bay.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bay.HomeActivity;
import com.example.bay.databinding.FragmentSettingBinding;
import com.example.bay.repository.UserRepository;
import com.example.bay.model.User;

public class SettingFragment extends Fragment {

    private static final String TAG = "SettingFragment";

    private FragmentSettingBinding binding;
    private UserRepository userRepository;
    private String userId;

    public SettingFragment() {
        // Required empty public constructor
    }

    // Remove the newInstance method if you're not using arguments anymore
    // Or keep it for backward compatibility if needed

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository();
        // Don't get activity here - it might not be attached yet
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSettingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get userId from HomeActivity here, when fragment is attached
        if (getActivity() instanceof HomeActivity) {
            HomeActivity homeActivity = (HomeActivity) getActivity();
            userId = homeActivity.getCurrentUserId();
        }

        // Add null check for userRepository as well
        if (userId == null || userRepository == null) {
            Log.w(TAG, "User ID or UserRepository is null");
            return;
        }

        // Set up back button click listener immediately (no need to wait for user data)
        binding.btnBack.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                requireActivity().onBackPressed();
            }
        });

        userRepository.getUserById(userId, new UserRepository.UserCallback<User>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(User user) {
                // Check if fragment is still attached to activity
                if (!isAdded() || getActivity() == null || binding == null) {
                    Log.d(TAG, "Fragment not attached, skipping UI update");
                    return;
                }

                // Ensure UI updates run on main thread
                requireActivity().runOnUiThread(() -> {
                    if (binding == null || user == null) {
                        Log.d(TAG, "Binding or user is null");
                        return;
                    }

                    String fullName = user.getFirst_name() + " " + user.getLast_name();
                    binding.btnBack.setText(fullName);
                    Log.d(TAG, "User name set to: " + fullName);
                });
            }

            @Override
            public void onError(String errorMsg) {
                // Check if fragment is still attached to activity
                if (!isAdded() || getActivity() == null) {
                    return;
                }

                // Handle error appropriately
                Log.e(TAG, "Error getting user: " + errorMsg);
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.btnBack.setText("Back");
                    }
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Optional: Refresh user data when fragment becomes visible again
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        // Consider cancelling any ongoing operations here if possible
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up repository if needed
        userRepository = null;
    }
}