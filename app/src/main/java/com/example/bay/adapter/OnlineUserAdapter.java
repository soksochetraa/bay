package com.example.bay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.bay.R;
import com.example.bay.model.User;

import java.util.List;

public class OnlineUserAdapter extends RecyclerView.Adapter<OnlineUserAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private List<User> userList;
    private OnUserClickListener listener;
    private Context context;

    public OnlineUserAdapter(List<User> userList, OnUserClickListener listener, Context context) {
        this.userList = userList;
        this.listener = listener;
        this.context = context;
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
        User user = userList.get(position);

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

        if (user.isOnline()) {
            holder.onlineIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.onlineIndicator.setVisibility(View.GONE);
        }

        holder.btnProfile.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView btnProfile;
        View onlineIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            btnProfile = itemView.findViewById(R.id.btnProfile);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
        }
    }
}