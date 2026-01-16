package com.example.bay.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.model.User;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.UserViewHolder> {

    private static final String TAG = "SearchResultAdapter";

    private List<User> users = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private String currentUserId;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onUserClick(User user);
        void onMessageClick(User user);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        Log.d(TAG, "setUsers called with " + users.size() + " users");
        filterCurrentUser();
        notifyDataSetChanged();
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        Log.d(TAG, "setCurrentUserId called: " + userId);
        filterCurrentUser();
        notifyDataSetChanged();
    }

    public void clearUsers() {
        users.clear();
        filteredUsers.clear();
        notifyDataSetChanged();
    }

    private void filterCurrentUser() {
        filteredUsers.clear();

        Log.d(TAG, "filterCurrentUser called. CurrentUserId: " + currentUserId + ", Total users: " + users.size());

        if (currentUserId == null || currentUserId.isEmpty()) {
            // If no current user, show all users
            filteredUsers.addAll(users);
            Log.d(TAG, "No current user ID, showing all " + users.size() + " users");
            return;
        }

        int filteredOutCount = 0;

        // Filter out current user
        for (User user : users) {
            String userId = user.getUserId();
            Log.d(TAG, "Checking user: " + user.getFirst_name() + " " + user.getLast_name() +
                    ", UserId: " + userId + ", CurrentUserId: " + currentUserId);

            if (userId == null || !userId.equals(currentUserId)) {
                filteredUsers.add(user);
            } else {
                filteredOutCount++;
                Log.d(TAG, "FILTERED OUT - Current user found: " + user.getFirst_name() + " " + user.getLast_name());
            }
        }

        Log.d(TAG, "Filtered out " + filteredOutCount + " users. Showing " + filteredUsers.size() + " users");
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_person, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        if (position < 0 || position >= filteredUsers.size()) return;

        User user = filteredUsers.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return filteredUsers.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView profileImage;
        private final TextView tvUsername;
        private final TextView tvCategory;
        private final ImageView btnMessage;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.btnProfile);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            btnMessage = itemView.findViewById(R.id.btnMessage);
        }

        public void bind(User user, OnItemClickListener listener) {
            String fullName = user.getFirst_name() + " " + user.getLast_name();
            tvUsername.setText(fullName);

            if (user.isUserVerified()) {
                tvUsername.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ico_user_verified, 0);
                tvUsername.setCompoundDrawablePadding(8);
            } else {
                tvUsername.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }

            if (user.getBio() != null && !user.getBio().isEmpty()) {
                tvCategory.setText(user.getBio());
            } else if (user.getRole() != null) {
                tvCategory.setText(user.getRole());
            } else {
                tvCategory.setText("");
            }

            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.img)
                        .circleCrop()
                        .into(profileImage);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });

            if (btnMessage != null) {
                btnMessage.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMessageClick(user);
                    }
                });
            }
        }
    }
}