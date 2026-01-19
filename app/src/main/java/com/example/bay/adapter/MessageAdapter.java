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
import com.example.bay.util.TimeUtils;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MY_TEXT = 1;
    private static final int VIEW_TYPE_OTHER_TEXT = 2;
    private static final int VIEW_TYPE_MY_IMAGE = 3;
    private static final int VIEW_TYPE_OTHER_IMAGE = 4;

    private List<Message> messageList;
    private String currentUserId;
    private Context context;
    private OnImageClickListener imageClickListener;

    public interface OnImageClickListener {
        void onImageClick(Message message, ImageView imageView);
    }

    public MessageAdapter(List<Message> messageList, String currentUserId,
                          Context context, OnImageClickListener listener) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.context = context;
        this.imageClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);

        if (message.getSenderId().equals(currentUserId)) {
            return "image".equals(message.getType()) ? VIEW_TYPE_MY_IMAGE : VIEW_TYPE_MY_TEXT;
        } else {
            return "image".equals(message.getType()) ? VIEW_TYPE_OTHER_IMAGE : VIEW_TYPE_OTHER_TEXT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        switch (viewType) {
            case VIEW_TYPE_MY_TEXT:
                view = inflater.inflate(R.layout.item_my_chat, parent, false);
                return new MyTextViewHolder(view);
            case VIEW_TYPE_OTHER_TEXT:
                view = inflater.inflate(R.layout.item_other_chat, parent, false);
                return new OtherTextViewHolder(view);
            case VIEW_TYPE_MY_IMAGE:
                view = inflater.inflate(R.layout.item_my_image_message, parent, false);
                return new MyImageViewHolder(view);
            case VIEW_TYPE_OTHER_IMAGE:
                view = inflater.inflate(R.layout.item_other_image_message, parent, false);
                return new OtherImageViewHolder(view);
            default:
                view = inflater.inflate(R.layout.item_my_chat, parent, false);
                return new MyTextViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MY_TEXT:
                ((MyTextViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_OTHER_TEXT:
                ((OtherTextViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MY_IMAGE:
                ((MyImageViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_OTHER_IMAGE:
                ((OtherImageViewHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class MyTextViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTime;

        MyTextViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(Message message) {
            tvMessage.setText(message.getText());
            tvTime.setText(TimeUtils.formatTime(message.getTimestamp()));
        }
    }

    class OtherTextViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTime;

        OtherTextViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(Message message) {
            tvMessage.setText(message.getText());
            tvTime.setText(TimeUtils.formatTime(message.getTimestamp()));
        }
    }

    class MyImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMessage;
        TextView tvTime;
        View progressBar;

        MyImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            progressBar = itemView.findViewById(R.id.progressBar);
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

            tvTime.setText(TimeUtils.formatTime(message.getTimestamp()));

            imgMessage.setOnClickListener(v -> {
                if (imageClickListener != null && message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    imageClickListener.onImageClick(message, imgMessage);
                }
            });
        }
    }

    class OtherImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMessage;
        TextView tvTime;

        OtherImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
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

            tvTime.setText(TimeUtils.formatTime(message.getTimestamp()));

            imgMessage.setOnClickListener(v -> {
                if (imageClickListener != null && message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    imageClickListener.onImageClick(message, imgMessage);
                }
            });
        }
    }
}