package com.example.bay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bay.databinding.ItemNotificationBinding;
import com.example.bay.model.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notificationList = new ArrayList<>();

    public void setData(List<Notification> list) {
        notificationList = list;
        notifyDataSetChanged();
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
        Notification notification = notificationList.get(position);

        holder.binding.tvTitle.setText(notification.getTitle());
        holder.binding.tvMessage.setText(notification.getBody());
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemNotificationBinding binding;

        public ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}