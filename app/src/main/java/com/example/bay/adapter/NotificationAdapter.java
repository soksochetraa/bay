package com.example.bay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.databinding.ItemNotificationBinding;
import com.example.bay.model.Notification;
import com.example.bay.repository.UserRepository;
import com.example.bay.util.TimeUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notificationList = new ArrayList<>();
    private List<Notification> filteredList = new ArrayList<>();

    public void setData(List<Notification> list) {
        this.notificationList = list;
        filterLatestChatMessages();
        notifyDataSetChanged();
    }

    private void filterLatestChatMessages() {
        Map<String, Notification> latestChatMap = new HashMap<>();
        List<Notification> otherNotifications = new ArrayList<>();

        for (Notification notification : notificationList) {
            if (notification.getType() != null && notification.getType().equals("chat")) {
                String senderId = notification.getSender();

                // Check if we already have a chat from this sender
                if (latestChatMap.containsKey(senderId)) {
                    Notification existing = latestChatMap.get(senderId);
                    // Compare dates and keep the latest one
                    if (isLaterDate(notification.getTimestamp(), existing.getTimestamp())) {
                        latestChatMap.put(senderId, notification);
                    }
                } else {
                    latestChatMap.put(senderId, notification);
                }
            } else {
                // Keep non-chat notifications as they are
                otherNotifications.add(notification);
            }
        }

        // Combine latest chat messages with other notifications
        filteredList = new ArrayList<>();
        filteredList.addAll(latestChatMap.values());
        filteredList.addAll(otherNotifications);

        // Sort all notifications by date (newest first)
        sortByDateDescending();
    }

    private boolean isLaterDate(String timestamp1, String timestamp2) {
        if (timestamp1 == null && timestamp2 == null) return false;
        if (timestamp1 == null) return false;
        if (timestamp2 == null) return true;

        try {
            long time1 = parseTimestampToMillis(timestamp1);
            long time2 = parseTimestampToMillis(timestamp2);
            return time1 > time2;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private long parseTimestampToMillis(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return 0;

        try {
            // Try parsing as long (milliseconds)
            if (timestamp.matches("\\d+")) {
                return Long.parseLong(timestamp);
            }

            // Try ISO 8601 format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            Date date = sdf.parse(timestamp);
            if (date != null) return date.getTime();

            // Try other common formats
            SimpleDateFormat[] formats = {
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            };

            for (SimpleDateFormat format : formats) {
                try {
                    date = format.parse(timestamp);
                    if (date != null) return date.getTime();
                } catch (ParseException ignored) {}
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void sortByDateDescending() {
        Collections.sort(filteredList, new Comparator<Notification>() {
            @Override
            public int compare(Notification n1, Notification n2) {
                long time1 = parseTimestampToMillis(n1.getTimestamp());
                long time2 = parseTimestampToMillis(n2.getTimestamp());

                // Sort in descending order (newest first)
                return Long.compare(time2, time1);
            }
        });
    }

    // Optional: Method to get only chat notifications filtered
    public List<Notification> getLatestChatMessages() {
        Map<String, Notification> latestChatMap = new HashMap<>();
        for (Notification notification : notificationList) {
            if (notification.getType() != null && notification.getType().equals("chat")) {
                String senderId = notification.getSender();
                if (latestChatMap.containsKey(senderId)) {
                    Notification existing = latestChatMap.get(senderId);
                    if (isLaterDate(notification.getTimestamp(), existing.getTimestamp())) {
                        latestChatMap.put(senderId, notification);
                    }
                } else {
                    latestChatMap.put(senderId, notification);
                }
            }
        }
        return new ArrayList<>(latestChatMap.values());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = filteredList.get(position);

        holder.binding.tvTitle.setText(notification.getTitle());
        holder.binding.tvMessage.setText(notification.getBody());

        // Set time ago if timestamp exists
        if (notification.getTimestamp() != null && !notification.getTimestamp().isEmpty()) {
            String timeAgo = TimeUtils.getRelativeTime(notification.getTimestamp());
            holder.binding.tvDate.setText(timeAgo);
        } else {
            holder.binding.tvDate.setText("មិនទាន់មាន");
        }

        if (notification.getType() != null && notification.getType().equals("chat")) {
            UserRepository userRepository = new UserRepository();

            String senderId = notification.getSender();

            userRepository.getFullName(senderId, new UserRepository.UserCallback<>() {
                @Override
                public void onSuccess(Map<String, String> result) {
                    String firstName = result.get("firstName") != null ? result.get("firstName") : "";
                    String lastName = result.get("lastName") != null ? result.get("lastName") : "";

                    String fullName = firstName + " " + lastName;
                    if (!fullName.trim().isEmpty()) {
                        holder.binding.tvTitle.setText(fullName);
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    holder.binding.tvTitle.setText("Unknown User");
                }
            });

            userRepository.getProfileImageUrl(senderId, new UserRepository.UserCallback<>() {
                @Override
                public void onSuccess(String profileImageUrl) {
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        holder.binding.btnProfile.setVisibility(View.VISIBLE);
                        holder.binding.iconLayout.setVisibility(View.GONE);

                        Glide.with(holder.itemView.getContext())
                                .load(profileImageUrl)
                                .circleCrop()
                                .placeholder(android.R.drawable.ic_menu_camera)
                                .error(android.R.drawable.ic_menu_report_image)
                                .into(holder.binding.btnProfile);
                    } else {
                        holder.binding.btnProfile.setVisibility(View.GONE);
                        holder.binding.iconLayout.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    holder.binding.btnProfile.setVisibility(View.GONE);
                    holder.binding.iconLayout.setVisibility(View.VISIBLE);
                }
            });
        } else {
            holder.binding.btnProfile.setVisibility(View.GONE);
            holder.binding.iconLayout.setVisibility(View.VISIBLE);
        }

        // Set click listener for notification item
        holder.itemView.setOnClickListener(v -> {
            if (onNotificationClickListener != null) {
                onNotificationClickListener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // Interface for notification click handling
    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private OnNotificationClickListener onNotificationClickListener;

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.onNotificationClickListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemNotificationBinding binding;

        public ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}