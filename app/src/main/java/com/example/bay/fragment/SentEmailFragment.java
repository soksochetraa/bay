package com.example.bay.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.bay.HomeActivity;
import com.example.bay.databinding.FragmentSentEmailBinding;

public class SentEmailFragment extends Fragment {

    private FragmentSentEmailBinding binding;
    private HomeActivity homeActivity;

    public SentEmailFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentSentEmailBinding.inflate(inflater, container, false);

        binding.backButton.setOnClickListener(v -> {
            homeActivity = (HomeActivity) getActivity();
            if (homeActivity != null) {
                String userId = homeActivity.getCurrentUserId();
                homeActivity.LoadFragment(EditProfileFragment.newInstance(userId));
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}