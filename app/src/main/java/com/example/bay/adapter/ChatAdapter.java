package com.example.bay.adapter;

import android.content.Context;
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
import com.example.bay.model.Chat;
import com.example.bay.model.User;
import com.example.bay.repository.UserRepository;
import com.example.bay.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
        void onUserClick(String userId);
    }

    private List<Chat> chatList;
    private String currentUserId;
    private OnChatClickListener listener;
    private UserRepository userRepository;
    private Context context;

    public ChatAdapter(List<Chat> chatList, String currentUserId,
                       OnChatClickListener listener, Context context) {
        this.chatList = chatList != null ? new ArrayList<>(chatList) : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.userRepository = new UserRepository();
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        String partnerId = chat.getChatPartnerId(currentUserId);

        userRepository.getUserById(partnerId, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User user) {
                holder.tvUserName.setText(user.getFirst_name() + " " + user.getLast_name());

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

                if (chat.getLastMessage() != null) {
                    if ("image".equals(chat.getLastMessageType())) {
                        holder.tvLastMessage.setText("📷 Photo");
                        holder.imgAttachment.setVisibility(View.VISIBLE);
                    } else {
                        holder.tvLastMessage.setText(chat.getLastMessage());
                        holder.imgAttachment.setVisibility(View.GONE);
                    }
                }

                if (chat.getLastMessageTime() > 0) {
                    holder.tvTime.setText(TimeUtils.getChatTime(chat.getLastMessageTime()));
                }

                if (chat.getUnreadCount() > 0 &&
                        !currentUserId.equals(chat.getLastMessageSenderId())) {
                    holder.tvUnreadCount.setText(String.valueOf(chat.getUnreadCount()));
                    holder.tvUnreadCount.setVisibility(View.VISIBLE);
                } else {
                    holder.tvUnreadCount.setVisibility(View.GONE);
                }

                if (currentUserId.equals(chat.getLastMessageSenderId())) {
                    holder.imgMessageStatus.setVisibility(View.VISIBLE);
                } else {
                    holder.imgMessageStatus.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMsg) {
                holder.tvUserName.setText("Unknown User");
                holder.imgProfile.setImageResource(R.drawable.img);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(chat);
            }
        });

        holder.imgProfile.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(partnerId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public void updateData(List<Chat> newChatList) {
        this.chatList.clear();
        this.chatList.addAll(newChatList != null ? newChatList : new ArrayList<>());
        notifyDataSetChanged();
    }

    public void filterList(List<Chat> filteredList) {
        this.chatList.clear();
        this.chatList.addAll(filteredList != null ? filteredList : new ArrayList<>());
        notifyDataSetChanged();
    }

    public void addChat(Chat chat) {
        this.chatList.add(0, chat);
        notifyItemInserted(0);
    }

    public void updateChat(Chat updatedChat) {
        for (int i = 0; i < chatList.size(); i++) {
            if (chatList.get(i).getChatId().equals(updatedChat.getChatId())) {
                chatList.set(i, updatedChat);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeChat(String chatId) {
        for (int i = 0; i < chatList.size(); i++) {
            if (chatList.get(i).getChatId().equals(chatId)) {
                chatList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void clear() {
        chatList.clear();
        notifyDataSetChanged();
    }

    public List<Chat> getChatList() {
        return new ArrayList<>(chatList);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView tvUserName;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvUnreadCount;
        ImageView imgMessageStatus;
        ImageView imgAttachment;
        View onlineIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.imgProfile);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            imgMessageStatus = itemView.findViewById(R.id.imgMessageStatus);
            imgAttachment = itemView.findViewById(R.id.imgAttachment);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
        }
    }
}