package com.example.bay.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.PostCardUserAdapter;
import com.example.bay.databinding.FragmentCommunityAccountBinding;
import com.example.bay.repository.UserRepository;

import java.util.Objects;

public class CommunityAccountFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private String userId;

    private FragmentCommunityAccountBinding binding;
    private UserRepository userRepository;

    public CommunityAccountFragment() {
    }

    public static CommunityAccountFragment newInstance(String userId) {
        CommunityAccountFragment fragment = new CommunityAccountFragment();
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommunityAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (userId == null) return;

        HomeActivity activity = (HomeActivity) getActivity();

        if (activity == null) return;
        if (!activity.getCurrentUserId().equals(userId)){
            binding.btnSetting.setVisibility(View.GONE);
            binding.btnEditProfile.setVisibility(View.GONE);
        }

        binding.btnSetting.setOnClickListener(v -> {
            activity.LoadFragment(new SettingFragment());
        });


        binding.btnEditProfile.setOnClickListener(v -> {
            activity.LoadFragment(EditProfileFragment.newInstance(userId));
            activity.hideBottomNavigation();
        });

        userRepository.getUserById(userId, new UserRepository.UserCallback<>() {
            @Override
            public void onSuccess(com.example.bay.model.User user) {
                if (binding == null || user == null) return;

                String bio = user.getBio() != null ? user.getBio() : "no bio yet!!";
                Log.d("Test", "bio: " + bio);
                Log.d("Test", "user: ");

                binding.tvName.setText(user.getFirst_name() + " " + user.getLast_name());
                binding.tvBio.setText(bio);
                binding.tvLocation.setText(user.getLocation());

                Glide.with(requireContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.img)
                        .into(binding.profileImage);
            }

            @Override
            public void onError(String errorMsg) {
            }
        });

        PostCardUserAdapter adapter = new PostCardUserAdapter(requireContext(), userId);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
