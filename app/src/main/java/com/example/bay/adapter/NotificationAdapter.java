package com.example.bay.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.bay.R;
import com.example.bay.model.Notification;
import com.example.bay.repository.UserRepository;
import com.example.bay.util.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
        void onNotificationLongClick(Notification notification);
        void onMarkAsReadClick(Notification notification);
    }

    private List<Notification> notificationList;
    private OnNotificationClickListener listener;
    private UserRepository userRepository;
    private Context context;

    public NotificationAdapter(List<Notification> notificationList,
                               OnNotificationClickListener listener,
                               Context context) {
        this.notificationList = notificationList != null ? new ArrayList<>(notificationList) : new ArrayList<>();
        this.listener = listener;
        this.userRepository = new UserRepository();
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        Notification notification = notificationList.get(position);
        return notification.isRead() ? 0 : 1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (viewType == 0) ? R.layout.item_notification_read : R.layout.item_notification_unread;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Notification notification = notificationList.get(position);

        // Set notification data
        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());

        // Fix timestamp display - check if TimeUtils.getRelativeTime accepts long
        if (holder.tvTime != null) {
            try {
                // Try to use TimeUtils
                holder.tvTime.setText(TimeUtils.getRelativeTime(String.valueOf(notification.getTimestamp())));
            } catch (Exception e) {
                // Fallback
                holder.tvTime.setText(formatTime(notification.getTimestamp()));
            }
        }

        // Load sender profile image
        userRepository.getUserById(notification.getSenderId(), new UserRepository.UserCallback<com.example.bay.model.User>() {
            @Override
            public void onSuccess(com.example.bay.model.User user) {
                if (holder.imgProfile != null) {
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        Glide.with(context)
                                .load(user.getProfileImageUrl())
                                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                .placeholder(R.drawable.img)
                                .error(R.drawable.img)
                                .into(holder.imgProfile);
                    } else {
                        holder.imgProfile.setImageResource(R.drawable.img);
                    }
                }
            }

            @Override
            public void onError(String errorMsg) {
                if (holder.imgProfile != null) {
                    holder.imgProfile.setImageResource(R.drawable.img);
                }
            }
        });

        // Set notification type icon - add null check
        if (holder.imgType != null) {
            int iconResId = getNotificationIcon(notification.getType());
            holder.imgType.setImageResource(iconResId);
        }

        // Show/hide unread indicator - with null safety check
        if (holder.unreadIndicator != null) {
            boolean isUnread = (getItemViewType(position) == 1);
            holder.unreadIndicator.setVisibility(isUnread ? View.VISIBLE : View.GONE);
        }

        // Set click listeners with null checks
        if (holder.itemView != null) {
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationLongClick(notification);
                    return true;
                }
                return false;
            });
        }

        // Set mark as read button click listener with null check
        if (holder.btnMarkAsRead != null) {
            holder.btnMarkAsRead.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMarkAsReadClick(notification);
                }
            });
        }
    }

    // Helper method to format time
    private String formatTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) {
            return "មុននេះបន្តិច";
        } else if (diff < 3600000) {
            long minutes = diff / 60000;
            return minutes + " នាទីមុន";
        } else if (diff < 86400000) {
            long hours = diff / 3600000;
            return hours + " ម៉ោងមុន";
        } else if (diff < 604800000) {
            long days = diff / 86400000;
            return days + " ថ្ងៃមុន";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    // Helper method to get notification icon
    private int getNotificationIcon(String type) {
        switch (type) {
            case "chat_message":
                return R.drawable.ic_message;
            case "system":
                return R.drawable.ic_account;
            case "admin":
                return R.drawable.ic_book;
            default:
                return R.drawable.ic_bell;
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public void updateData(List<Notification> newNotificationList) {
        this.notificationList.clear();
        this.notificationList.addAll(newNotificationList != null ? newNotificationList : new ArrayList<>());
        notifyDataSetChanged();
    }

    public void addNotification(Notification notification) {
        this.notificationList.add(0, notification);
        notifyItemInserted(0);
    }

    public void removeNotification(String notificationId) {
        for (int i = 0; i < notificationList.size(); i++) {
            if (notificationList.get(i).getNotificationId().equals(notificationId)) {
                notificationList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void updateNotification(Notification updatedNotification) {
        for (int i = 0; i < notificationList.size(); i++) {
            if (notificationList.get(i).getNotificationId().equals(updatedNotification.getNotificationId())) {
                notificationList.set(i, updatedNotification);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void clear() {
        notificationList.clear();
        notifyDataSetChanged();
    }

    // Updated ViewHolder with null safety in constructor
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        ImageView imgType;
        TextView tvTitle;
        TextView tvMessage;
        TextView tvTime;
        View unreadIndicator;
        ImageView btnMarkAsRead;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views with null checks
            imgProfile = itemView.findViewById(R.id.imgProfile);
            imgType = itemView.findViewById(R.id.imgType);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            btnMarkAsRead = itemView.findViewById(R.id.btnMarkAsRead);

            // Log missing views for debugging
            if (imgProfile == null) Log.d("NotificationAdapter", "imgProfile is null");
            if (imgType == null) Log.d("NotificationAdapter", "imgType is null");
            if (tvTitle == null) Log.d("NotificationAdapter", "tvTitle is null");
            if (tvMessage == null) Log.d("NotificationAdapter", "tvMessage is null");
            if (tvTime == null) Log.d("NotificationAdapter", "tvTime is null");
            if (unreadIndicator == null) Log.d("NotificationAdapter", "unreadIndicator is null");
            if (btnMarkAsRead == null) Log.d("NotificationAdapter", "btnMarkAsRead is null");
        }
    }
}