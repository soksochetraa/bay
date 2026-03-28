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
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.bay.R;
import com.example.bay.model.Message;
import com.example.bay.repository.NotificationRepository;
import com.example.bay.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final long SESSION_GAP_MS = 60 * 60 * 1000L;
    private static final long GROUP_GAP_MS = 30 * 60 * 1000L;

    private static final int VIEW_TYPE_TIME = 0;

    private static final int VIEW_TYPE_MY_FIRST_TEXT = 1;
    private static final int VIEW_TYPE_MY_MIDDLE_TEXT = 2;
    private static final int VIEW_TYPE_MY_LATEST_TEXT = 3;

    private static final int VIEW_TYPE_OTHER_FIRST_TEXT = 4;
    private static final int VIEW_TYPE_OTHER_MIDDLE_TEXT = 5;
    private static final int VIEW_TYPE_OTHER_LATEST_TEXT = 6;

    private static final int VIEW_TYPE_MY_IMAGE_NO_TIME = 7;
    private static final int VIEW_TYPE_MY_IMAGE_WITH_TIME = 8;
    private static final int VIEW_TYPE_OTHER_IMAGE_NO_TIME = 9;
    private static final int VIEW_TYPE_OTHER_IMAGE_WITH_TIME = 10;

    private final Context context;
    private final String currentUserId;
    private final OnImageClickListener imageClickListener;

    private final List<ChatListItem> displayItems = new ArrayList<>();
    NotificationRepository notificationRepository = new NotificationRepository();

    public interface OnImageClickListener {
        void onImageClick(Message message, ImageView imageView);
    }

    public MessageAdapter(String currentUserId, Context context, OnImageClickListener listener) {
        this.currentUserId = currentUserId;
        this.context = context;
        this.imageClickListener = listener;
    }

    public void submitMessages(List<Message> messages) {
        displayItems.clear();
        if (messages == null || messages.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        for (int i = 0; i < messages.size(); i++) {
            Message curr = messages.get(i);
            Message prev = (i > 0) ? messages.get(i - 1) : null;

            boolean newSession = (prev == null) || (curr.getTimestamp() - prev.getTimestamp() > SESSION_GAP_MS);

            if (newSession) {
                displayItems.add(new TimeItem(curr.getTimestamp()));
            }

            boolean hideTime = newSession;
            displayItems.add(new MessageItem(curr, hideTime));
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatListItem item = displayItems.get(position);

        if (item.getType() == ChatListItem.TYPE_TIME) return VIEW_TYPE_TIME;

        MessageItem msgItem = (MessageItem) item;
        Message curr = msgItem.getMessage();

        Message prevMsg = getPrevMessage(position);
        Message nextMsg = getNextMessage(position);

        boolean samePrev = isSameSender(curr, prevMsg) && withinGroupGap(curr, prevMsg);
        boolean sameNext = isSameSender(curr, nextMsg) && withinGroupGap(curr, nextMsg);

        boolean isMine = curr.getSenderId().equals(currentUserId);

        if ("image".equals(curr.getType())) {
            boolean showTime = !msgItem.isHideTime() && !sameNext;
            if (isMine) return showTime ? VIEW_TYPE_MY_IMAGE_WITH_TIME : VIEW_TYPE_MY_IMAGE_NO_TIME;
            return showTime ? VIEW_TYPE_OTHER_IMAGE_WITH_TIME : VIEW_TYPE_OTHER_IMAGE_NO_TIME;
        }

        if (msgItem.isHideTime()) {
            return isMine ? VIEW_TYPE_MY_FIRST_TEXT : VIEW_TYPE_OTHER_FIRST_TEXT;
        }

        if (!samePrev && !sameNext) {
            return isMine ? VIEW_TYPE_MY_LATEST_TEXT : VIEW_TYPE_OTHER_LATEST_TEXT;
        } else if (!samePrev) {
            return isMine ? VIEW_TYPE_MY_FIRST_TEXT : VIEW_TYPE_OTHER_FIRST_TEXT;
        } else if (!sameNext) {
            return isMine ? VIEW_TYPE_MY_LATEST_TEXT : VIEW_TYPE_OTHER_LATEST_TEXT;
        } else {
            return isMine ? VIEW_TYPE_MY_MIDDLE_TEXT : VIEW_TYPE_OTHER_MIDDLE_TEXT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        switch (viewType) {
            case VIEW_TYPE_TIME:
                view = inflater.inflate(R.layout.item_chat_time, parent, false);
                return new TimeViewHolder(view);

            case VIEW_TYPE_MY_FIRST_TEXT:
                view = inflater.inflate(R.layout.item_my_first_chat, parent, false);
                return new TextNoTimeViewHolder(view);

            case VIEW_TYPE_MY_MIDDLE_TEXT:
                view = inflater.inflate(R.layout.item_my_middle_chat, parent, false);
                return new TextNoTimeViewHolder(view);

            case VIEW_TYPE_MY_LATEST_TEXT:
                view = inflater.inflate(R.layout.item_my_latest_chat, parent, false);
                return new TextWithTimeViewHolder(view);

            case VIEW_TYPE_OTHER_FIRST_TEXT:
                view = inflater.inflate(R.layout.item_other_first_chat, parent, false);
                return new TextNoTimeViewHolder(view);

            case VIEW_TYPE_OTHER_MIDDLE_TEXT:
                view = inflater.inflate(R.layout.item_other_middle_chat, parent, false);
                return new TextNoTimeViewHolder(view);

            case VIEW_TYPE_OTHER_LATEST_TEXT:
                view = inflater.inflate(R.layout.item_other_latest_chat, parent, false);
                return new TextWithTimeViewHolder(view);

            case VIEW_TYPE_MY_IMAGE_NO_TIME:
                view = inflater.inflate(R.layout.item_my_image_message, parent, false);
                return new ImageViewHolder(view, false);

            case VIEW_TYPE_MY_IMAGE_WITH_TIME:
                view = inflater.inflate(R.layout.item_my_image_message, parent, false);
                return new ImageViewHolder(view, true);

            case VIEW_TYPE_OTHER_IMAGE_NO_TIME:
                view = inflater.inflate(R.layout.item_other_image_message, parent, false);
                return new ImageViewHolder(view, false);

            case VIEW_TYPE_OTHER_IMAGE_WITH_TIME:
                view = inflater.inflate(R.layout.item_other_image_message, parent, false);
                return new ImageViewHolder(view, true);

            default:
                view = inflater.inflate(R.layout.item_my_latest_chat, parent, false);
                return new TextWithTimeViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatListItem item = displayItems.get(position);

        if (holder instanceof TimeViewHolder) {
            ((TimeViewHolder) holder).bind((TimeItem) item);
            return;
        }

        MessageItem msgItem = (MessageItem) item;
        Message message = msgItem.getMessage();

        if (holder instanceof TextNoTimeViewHolder) {
            ((TextNoTimeViewHolder) holder).bind(message);
        } else if (holder instanceof TextWithTimeViewHolder) {
            ((TextWithTimeViewHolder) holder).bind(message);
        } else if (holder instanceof ImageViewHolder) {
            ((ImageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    private boolean isSameSender(Message a, Message b) {
        return a != null && b != null && a.getSenderId() != null && a.getSenderId().equals(b.getSenderId());
    }

    private boolean withinGroupGap(Message a, Message b) {
        return a != null && b != null && Math.abs(a.getTimestamp() - b.getTimestamp()) <= GROUP_GAP_MS;
    }

    private Message getPrevMessage(int position) {
        for (int i = position - 1; i >= 0; i--) {
            ChatListItem it = displayItems.get(i);
            if (it.getType() == ChatListItem.TYPE_MESSAGE) return ((MessageItem) it).getMessage();
        }
        return null;
    }

    private Message getNextMessage(int position) {
        for (int i = position + 1; i < displayItems.size(); i++) {
            ChatListItem it = displayItems.get(i);
            if (it.getType() == ChatListItem.TYPE_MESSAGE) return ((MessageItem) it).getMessage();
        }
        return null;
    }

    public static abstract class ChatListItem {
        public static final int TYPE_TIME = 1000;
        public static final int TYPE_MESSAGE = 2000;

        public abstract int getType();
    }

    public static class TimeItem extends ChatListItem {
        private final long time;

        public TimeItem(long time) {
            this.time = time;
        }

        public long getTime() {
            return time;
        }

        @Override
        public int getType() {
            return TYPE_TIME;
        }
    }

    public static class MessageItem extends ChatListItem {
        private final Message message;
        private final boolean hideTime;

        public MessageItem(Message message, boolean hideTime) {
            this.message = message;
            this.hideTime = hideTime;
        }

        public Message getMessage() {
            return message;
        }

        public boolean isHideTime() {
            return hideTime;
        }

        @Override
        public int getType() {
            return TYPE_MESSAGE;
        }
    }

    static class TimeViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;

        TimeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(TimeItem item) {
            tvTime.setText(TimeUtils.formatTime(item.getTime()));
        }
    }

    static class TextNoTimeViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        TextNoTimeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }

        void bind(Message message) {
            tvMessage.setText(message.getText());
        }
    }

    static class TextWithTimeViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTime;

        TextWithTimeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(Message message) {
            tvMessage.setText(message.getText());
            tvTime.setText(TimeUtils.formatTime(message.getTimestamp()));
        }
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMessage;
        TextView tvTime;
        View progressBar;
        boolean showTime;

        ImageViewHolder(@NonNull View itemView, boolean showTime) {
            super(itemView);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            progressBar = itemView.findViewById(R.id.progressBar);
            this.showTime = showTime;
        }

        void bind(Message message) {
            String imageUrl = message.getThumbnailUrl() != null && !message.getThumbnailUrl().isEmpty()
                    ? message.getThumbnailUrl()
                    : message.getImageUrl();

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.img)
                                .error(R.drawable.img)
                                .transform(new RoundedCorners(16)))
                        .into(imgMessage);
            } else {
                imgMessage.setImageResource(R.drawable.img);
            }

            if (tvTime != null) {
                if (showTime) {
                    tvTime.setVisibility(View.VISIBLE);
                    tvTime.setText(TimeUtils.formatTime(message.getTimestamp()));
                } else {
                    tvTime.setVisibility(View.GONE);
                }
            }

            imgMessage.setOnClickListener(v -> {
                if (imageClickListener != null && message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    imageClickListener.onImageClick(message, imgMessage);
                }
            });
        }
    }
}