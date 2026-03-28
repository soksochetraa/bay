package com.example.bay.adapter;

import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
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
import com.example.bay.util.FirebaseDBHelper;
import com.example.bay.util.TimeUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(Chat chat);

        void onUserClick(String userId);
    }

    private final List<Chat> chatList;
    private final String currentUserId;
    private final OnChatClickListener listener;
    private final UserRepository userRepository;
    private final Context context;

    private final Map<String, DatabaseReference> onlineRefs = new HashMap<>();
    private final Map<String, ValueEventListener> onlineListeners = new HashMap<>();
    private final Map<String, Boolean> onlineCache = new HashMap<>();

    public ChatAdapter(List<Chat> chatList, String currentUserId, OnChatClickListener listener, Context context) {
        this.chatList = chatList != null ? new ArrayList<>(chatList) : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.userRepository = new UserRepository();
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        String partnerId = chat.getChatPartnerId(currentUserId);

        holder.boundPartnerId = partnerId;

        boolean hasUnread = chat.getUnreadCount() > 0 && !currentUserId.equals(chat.getLastMessageSenderId());

        holder.tvLastMessage.setTypeface(null, hasUnread ? Typeface.BOLD : Typeface.NORMAL);
        holder.tvUserName.setTypeface(null, hasUnread ? Typeface.BOLD : Typeface.NORMAL);

        if (hasUnread) {
            holder.tvUnreadCount.setText(String.valueOf(chat.getUnreadCount()));
            holder.tvUnreadCount.setVisibility(VISIBLE);
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        if (chat.getLastMessageTime() > 0) {
            holder.tvTime.setText(TimeUtils.getChatTime(chat.getLastMessageTime()));
        } else {
            holder.tvTime.setText("");
        }

        holder.tvLastMessage.setText(buildLastMessagePreview(chat, hasUnread));

        if ("image".equals(chat.getLastMessageType())) {
            holder.imgAttachment.setVisibility(VISIBLE);
        } else {
            holder.imgAttachment.setVisibility(View.GONE);
        }

        Boolean cached = onlineCache.get(partnerId);
        holder.onlineIndicator.setVisibility(cached != null && cached ? VISIBLE : View.GONE);
        ensureOnlineListener(partnerId);

        userRepository.getUserById(partnerId, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;
                if (!partnerId.equals(holder.boundPartnerId)) return;

                holder.tvUserName.setText(user.getFirstName() + " " + user.getLastName());

                if (user.isUserVerified()) {
                    holder.verified.setVisibility(VISIBLE);
                }

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

            @Override
            public void onError(String errorMsg) {
                if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;
                if (!partnerId.equals(holder.boundPartnerId)) return;

                holder.tvUserName.setText("Unknown User");
                holder.imgProfile.setImageResource(R.drawable.img);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChatClick(chat);
        });

        holder.imgProfile.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(partnerId);
        });

    }

    private void ensureOnlineListener(String partnerId) {
        if (onlineListeners.containsKey(partnerId)) return;

        DatabaseReference ref = FirebaseDBHelper.getOnlineStatusRef(partnerId);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isOnline = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                onlineCache.put(partnerId, isOnline);
                notifyOnlineChanged(partnerId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        onlineRefs.put(partnerId, ref);
        onlineListeners.put(partnerId, listener);
        ref.addValueEventListener(listener);
    }

    private void notifyOnlineChanged(String partnerId) {
        for (int i = 0; i < chatList.size(); i++) {
            String pid = chatList.get(i).getChatPartnerId(currentUserId);
            if (partnerId.equals(pid)) notifyItemChanged(i);
        }
    }

    private String buildLastMessagePreview(Chat chat, boolean hasUnread) {
        String base;
        if ("image".equals(chat.getLastMessageType())) {
            boolean isMine = currentUserId.equals(chat.getLastMessageSenderId());
            base = isMine ? "You sent an image" : "Sent an image";
        } else {
            base = chat.getLastMessage();
            if (TextUtils.isEmpty(base)) base = "";
        }

        boolean isMine = currentUserId.equals(chat.getLastMessageSenderId());
        if (isMine) {
            if ("image".equals(chat.getLastMessageType())) return base;
            if (TextUtils.isEmpty(base)) return "You sent a message";
            return "You: " + base;
        } else {
            if (TextUtils.isEmpty(base)) return hasUnread ? "New message" : "";
            return base;
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public Chat getChatAt(int position) {
        if (position < 0 || position >= chatList.size()) return null;
        return chatList.get(position);
    }

    public void removeAt(int position) {
        if (position < 0 || position >= chatList.size()) return;
        chatList.remove(position);
        notifyItemRemoved(position);
    }

    public void restore(Chat chat, int position) {
        if (chat == null) return;
        int safePos = Math.max(0, Math.min(position, chatList.size()));
        chatList.add(safePos, chat);
        notifyItemInserted(safePos);
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

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        for (Map.Entry<String, ValueEventListener> e : onlineListeners.entrySet()) {
            DatabaseReference ref = onlineRefs.get(e.getKey());
            if (ref != null) ref.removeEventListener(e.getValue());
        }
        onlineListeners.clear();
        onlineRefs.clear();
        onlineCache.clear();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView tvUserName;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvUnreadCount;
        ImageView imgAttachment, verified;
        View onlineIndicator;
        String boundPartnerId;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.imgProfile);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            imgAttachment = itemView.findViewById(R.id.imgAttachment);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            verified = itemView.findViewById(R.id.verified);
        }
    }
}