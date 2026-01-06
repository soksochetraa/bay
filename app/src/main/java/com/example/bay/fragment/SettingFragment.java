package com.example.bay.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bay.databinding.FragmentSettingBinding;
import com.example.bay.repository.UserRepository;

public class SettingFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";

    private FragmentSettingBinding binding;
    private UserRepository userRepository;
    private String userId;

    public SettingFragment() {
        // Required empty public constructor
    }

    public static SettingFragment newInstance(String userId) {
        SettingFragment fragment = new SettingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository();

        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }
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

        if (userId == null) return;

        userRepository.getUserById(userId, new UserRepository.UserCallback<>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(com.example.bay.model.User user) {
                if (binding == null || user == null) return;
                binding.btnBack.setText(user.getFirst_name() + " " + user.getLast_name());
            }

            @Override
            public void onError(String errorMsg) {
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
