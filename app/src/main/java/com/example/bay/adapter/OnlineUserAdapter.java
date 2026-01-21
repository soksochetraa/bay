package com.example.bay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.bay.R;
import com.example.bay.model.User;
import com.example.bay.model.Message;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OnlineUserAdapter extends RecyclerView.Adapter<OnlineUserAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private List<User> filteredUserList;
    private List<User> originalUserList;
    private List<Message> messageList;
    private OnUserClickListener listener;
    private Context context;
    private String currentUserId;
    private User currentUser;

    public OnlineUserAdapter(List<User> userList, List<Message> messageList,
                             String currentUserId, User currentUser,
                             OnUserClickListener listener, Context context) {
        this.originalUserList = userList != null ? new ArrayList<>(userList) : new ArrayList<>();
        this.messageList = messageList != null ? messageList : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.currentUser = currentUser;
        this.listener = listener;
        this.context = context;
        filterAndSortUsers();
    }

    // Method to filter and sort users based on criteria
    private void filterAndSortUsers() {
        filteredUserList = new ArrayList<>();

        if (currentUser != null) {
            // Add current user first
            filteredUserList.add(currentUser);
        }

        // Get all users who have chat history with current user
        Set<String> usersWithChatHistory = getUsersWithChatHistory();

        // Add online users who have chat history
        for (User user : originalUserList) {
            // Skip current user (already added)
            if (currentUserId != null && currentUserId.equals(user.getUserId())) {
                continue;
            }

            // Check if user is online AND has chat history with current user
            if (user.isOnline() && usersWithChatHistory.contains(user.getUserId())) {
                filteredUserList.add(user);
            }
        }
    }

    // Method to get user IDs who have chat history with current user
    private Set<String> getUsersWithChatHistory() {
        Set<String> userIds = new HashSet<>();

        if (messageList == null || currentUserId == null) {
            return userIds;
        }

        for (Message message : messageList) {
            String senderId = message.getSenderId();
            String receiverId = message.getReceiverId();

            if (currentUserId.equals(senderId) && receiverId != null) {
                userIds.add(receiverId);
            } else if (currentUserId.equals(receiverId) && senderId != null) {
                userIds.add(senderId);
            }
        }

        return userIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = filteredUserList.get(position);
        boolean isCurrentUser = currentUserId != null && currentUserId.equals(user.getUserId());

        // Set profile image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .into(holder.btnProfile);
        } else {
            holder.btnProfile.setImageResource(R.drawable.img);
        }

        // Set user name (combine first_name + last_name)
        if (holder.tvUserName != null) {
            String displayName = getUserDisplayName(user, isCurrentUser);
            holder.tvUserName.setText(displayName);
        }

        // Show/hide online indicator
        if (user.isOnline()) {
            holder.onlineIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.onlineIndicator.setVisibility(View.GONE);
        }

        // Show/hide current user indicator
        if (holder.currentUserIndicator != null) {
            if (isCurrentUser) {
                holder.currentUserIndicator.setVisibility(View.VISIBLE);
            } else {
                holder.currentUserIndicator.setVisibility(View.GONE);
            }
        }

        // Set click listener (only for non-current users)
        holder.btnProfile.setOnClickListener(v -> {
            if (listener != null && !isCurrentUser) {
                listener.onUserClick(user);
            }
        });
    }

    // Helper method to get display name
    private String getUserDisplayName(User user, boolean isCurrentUser) {
        StringBuilder nameBuilder = new StringBuilder();

        // Combine first name and last name
        if (user.getFirst_name() != null && !user.getFirst_name().trim().isEmpty()) {
            nameBuilder.append(user.getFirst_name().trim());
        }

        if (user.getLast_name() != null && !user.getLast_name().trim().isEmpty()) {
            if (nameBuilder.length() > 0) {
                nameBuilder.append(" ");
            }
            nameBuilder.append(user.getLast_name().trim());
        }

        // If no name is available, use default
        if (nameBuilder.length() == 0) {
            nameBuilder.append("User");
        }

        // Add "(You)" indicator for current user
        if (isCurrentUser) {
            nameBuilder.append(" (You)");
        }

        return nameBuilder.toString();
    }

    @Override
    public int getItemCount() {
        return filteredUserList != null ? filteredUserList.size() : 0;
    }

    // Update method to refresh data
    public void updateData(List<User> userList, List<Message> messages, User currentUser) {
        this.originalUserList = userList != null ? new ArrayList<>(userList) : new ArrayList<>();
        this.messageList = messages != null ? messages : new ArrayList<>();
        this.currentUser = currentUser;
        filterAndSortUsers();
        notifyDataSetChanged();
    }

    // Update user list only
    public void updateUserList(List<User> userList) {
        this.originalUserList = userList != null ? new ArrayList<>(userList) : new ArrayList<>();
        filterAndSortUsers();
        notifyDataSetChanged();
    }

    // Update messages only
    public void updateMessageList(List<Message> messages) {
        this.messageList = messages != null ? messages : new ArrayList<>();
        filterAndSortUsers();
        notifyDataSetChanged();
    }

    // Method to update user's online status
    public void updateUserOnlineStatus(String userId, boolean isOnline) {
        for (int i = 0; i < filteredUserList.size(); i++) {
            User user = filteredUserList.get(i);
            if (userId.equals(user.getUserId())) {
                user.setOnline(isOnline);
                notifyItemChanged(i);
                break;
            }
        }
    }

    // Get specific user from the filtered list
    public User getUserAtPosition(int position) {
        if (position >= 0 && position < filteredUserList.size()) {
            return filteredUserList.get(position);
        }
        return null;
    }

    public int findUserPosition(String userId) {
        for (int i = 0; i < filteredUserList.size(); i++) {
            if (userId.equals(filteredUserList.get(i).getUserId())) {
                return i;
            }
        }
        return -1;
    }

    public void clear() {
        filteredUserList.clear();
        originalUserList.clear();
        messageList.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView btnProfile;
        View onlineIndicator;
        View currentUserIndicator;
        TextView tvUserName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            btnProfile = itemView.findViewById(R.id.btnProfile);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            currentUserIndicator = itemView.findViewById(R.id.currentUserIndicator);
            tvUserName = itemView.findViewById(R.id.tvUserName);
        }
    }
}