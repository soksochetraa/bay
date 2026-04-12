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

import com.example.bay.HomeActivity;
import com.example.bay.databinding.FragmentSettingBinding;
import com.example.bay.viewmodel.SharedUserViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.bay.model.User;

public class SettingFragment extends Fragment {

    private static final String TAG = "SettingFragment";

    private FragmentSettingBinding binding;
    private SharedUserViewModel sharedUserViewModel;
    private String userId;

    public SettingFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedUserViewModel = new ViewModelProvider(requireActivity()).get(SharedUserViewModel.class);
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

        if (userId == null || sharedUserViewModel == null) {
            Log.w(TAG, "User ID or SharedUserViewModel is null");
            return;
        }

        binding.btnLanguage.setOnClickListener(v->{
            Toast.makeText(getActivity(), "សូមអធ្យាស្រ័យពួកយើងមិនទាន់ធ្វើហើយទេ!", Toast.LENGTH_SHORT).show();
        });

        binding.btnSavedPosts.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getActivity();
                homeActivity.LoadFragment(new SavedCommunityPostsFragment());
            }
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

        sharedUserViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
             if (!isAdded() || getActivity() == null || binding == null) {
                 return;
             }
             if (user != null) {
                 String fullName = user.getFirstName() + " " + user.getLastName();
                 binding.btnBack.setText(fullName);
             } else {
                 binding.btnBack.setText("Back");
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
    }
}