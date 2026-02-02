package com.example.bay.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bay.AboutAppFragment;
import com.example.bay.ContactUsFragment;
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

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository();
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

        if (getActivity() instanceof HomeActivity) {
            HomeActivity homeActivity = (HomeActivity) getActivity();
            userId = homeActivity.getCurrentUserId();
        }

        if (userId == null || userRepository == null) {
            Log.w(TAG, "User ID or UserRepository is null");
            return;
        }

        binding.btnLanguage.setOnClickListener(v->{
            Toast.makeText(getActivity(), "សូមអធ្យាស្រ័យពួកយើងមិនទាន់ធ្វើហើយទេ!", Toast.LENGTH_SHORT).show();
        });

        binding.buttonAboutApp.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getActivity();
                homeActivity.LoadFragment(new AboutAppFragment());
            }
        });

        binding.btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getActivity();
                homeActivity.signOut();
            }
        });

        binding.buttonContactUs.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getActivity();
                homeActivity.LoadFragment(new ContactUsFragment());
            }
        });


        binding.btnBack.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                requireActivity().onBackPressed();
            }
        });

        userRepository.getUserById(userId, new UserRepository.UserCallback<User>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(User user) {
                if (!isAdded() || getActivity() == null || binding == null) {
                    Log.d(TAG, "Fragment not attached, skipping UI update");
                    return;
                }

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
                if (!isAdded() || getActivity() == null) {
                    return;
                }

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        userRepository = null;
    }
}