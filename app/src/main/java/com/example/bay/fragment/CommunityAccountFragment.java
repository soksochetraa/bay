package com.example.bay.fragment;

import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.PostCardUserAdapter;
import com.example.bay.databinding.FragmentCommunityAccountBinding;
import com.example.bay.model.Chat;
import com.example.bay.repository.ChatRepository;
import com.example.bay.repository.UserRepository;

import java.util.Objects;

public class CommunityAccountFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private String userId;

    private FragmentCommunityAccountBinding binding;
    private UserRepository userRepository;
    private ChatRepository chatRepository;
    private HomeActivity homeActivity;

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
        chatRepository = new ChatRepository();
        homeActivity = (HomeActivity) requireActivity();
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommunityAccountBinding.inflate(inflater, container, false);

        String currentUserId = homeActivity.getCurrentUserId();

        if (currentUserId == null) {
            return binding.getRoot();
        }

        if (currentUserId.equals(userId)) {
            binding.btnSetting.setVisibility(VISIBLE);
            binding.btnEditProfile.setVisibility(VISIBLE);
            binding.btnBack.setVisibility(View.GONE);
            binding.constraintLayout6.setVisibility(View.GONE);
            homeActivity.showBottomNavigation();
        } else {
            binding.btnSetting.setVisibility(View.GONE);
            binding.btnEditProfile.setVisibility(View.GONE);
            binding.btnBack.setVisibility(VISIBLE);
            binding.constraintLayout6.setVisibility(VISIBLE);
            binding.tvActivityHeader.setText("ការផ្សព្វផ្សាយរបស់គាត់");
            homeActivity.hideBottomNavigation();
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (userId == null) return;

        HomeActivity activity = (HomeActivity) getActivity();
        if (activity == null) return;

        String currentUserId = activity.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "You need to be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId.equals(userId)) {
            binding.tvAbout.setText("អំពីអ្នក");
            binding.message.setVisibility(View.GONE);
            binding.ViewSellProfile.setVisibility(View.GONE);
        } else {
            binding.tvAbout.setText("អំពីគាត់");
            binding.message.setVisibility(View.VISIBLE);
        }

        binding.message.setOnClickListener(v -> {
            if (currentUserId.equals(userId)) {
                activity.navigateTo(R.id.nav_message, new MessageFragment());
            } else {
                startChatWithUser(currentUserId, userId);
            }
        });

        binding.ViewSellProfile.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Viewing sell profile", Toast.LENGTH_SHORT).show();
        });

        binding.btnSetting.setOnClickListener(v -> {
            activity.LoadFragment(new SettingFragment());
            activity.hideBottomNavigation();
        });

        binding.btnBack.setOnClickListener(v -> {
            activity.onBackPressed();
        });

        binding.btnEditProfile.setOnClickListener(v -> {
            activity.LoadFragment(EditProfileFragment.newInstance(userId));
            activity.hideBottomNavigation();
        });

        userRepository.getUserById(userId, new UserRepository.UserCallback<com.example.bay.model.User>() {
            @Override
            public void onSuccess(com.example.bay.model.User user) {
                if (binding == null || user == null) return;

                String bio = user.getBio() != null ? user.getBio() : "no bio yet!!";
                String role = user.getRole();

                if (role == null) {
                    return;
                }

                switch (role) {
                    case "សិស្ស":
                        binding.ivRole.setImageResource(R.drawable.ic_graduation_cap);
                        break;
                    case "កសិករ":
                        binding.ivRole.setImageResource(R.drawable.ic_tractor);
                        break;
                    case "ឈ្មួយ":
                        binding.ivRole.setImageResource(R.drawable.ic_briefcase_business);
                        break;
                    case "Admin":
                        binding.ivRole.setImageResource(R.drawable.ic_crown);
                        break;
                    default:
                        break;
                }

                if (user.isUserVerified()) {
                    binding.verified.setVisibility(VISIBLE);
                } else {
                    binding.verified.setVisibility(View.GONE);
                }

                binding.tvName.setText(user.getFirst_name() + " " + user.getLast_name());
                binding.tvBio.setText(bio);
                binding.tvLocation.setText(user.getLocation());
                binding.tvRole.setText(user.getRole());

                Glide.with(requireContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.img)
                        .into(binding.profileImage);
            }

            @Override
            public void onError(String errorMsg) {
                Log.e("CommunityAccount", "Error loading user: " + errorMsg);
            }
        });

        PostCardUserAdapter adapter = new PostCardUserAdapter(requireContext(), userId);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnDataChangedListener(new PostCardUserAdapter.OnDataChangedListener() {
            @Override
            public void onDataChanged(boolean isEmpty) {
                if (binding == null) return;

                if (isEmpty) {
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.tvActivityHeader.setVisibility(View.GONE);
                    binding.emptyState.setVisibility(VISIBLE);
                } else {
                    binding.recyclerView.setVisibility(VISIBLE);
                    binding.tvActivityHeader.setVisibility(VISIBLE);
                    binding.emptyState.setVisibility(View.GONE);
                }
            }
        });
    }

    private void startChatWithUser(String currentUserId, String otherUserId) {
        HomeActivity activity = (HomeActivity) getActivity();
        if (activity == null) return;

        activity.showLoading();
        binding.message.setEnabled(false);

        chatRepository.getOrCreateChat(currentUserId, otherUserId, new ChatRepository.ChatCallback<Chat>() {
            @Override
            public void onSuccess(Chat chat) {
                activity.hideLoading();
                binding.message.setEnabled(true);

                PersonalMessageFragment fragment = PersonalMessageFragment.newInstance(chat.getChatId(), otherUserId);
                activity.LoadFragment(fragment);
                activity.hideBottomNavigation();
            }

            @Override
            public void onError(String error) {
                activity.hideLoading();
                binding.message.setEnabled(true);

                Log.e("CommunityAccount", "Error creating chat: " + error);
                Toast.makeText(requireContext(), "Failed to start chat: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}