package com.example.bay.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Notification {
    private String notificationId;
    private String senderId;
    private String receiverId;
    private String type;
    private String title;
    private String message;
    private String chatId;
    private String messageId;
    private String imageUrl;
    private boolean read;
    private long timestamp;
    private Map<String, String> metadata;

    public Notification() {
    }

    public Notification(String senderId, String receiverId, String type,
                        String title, String message, String chatId, String messageId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.chatId = chatId;
        this.messageId = messageId;
        this.read = false;
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("senderId", senderId);
        result.put("receiverId", receiverId);
        result.put("type", type);
        result.put("title", title);
        result.put("message", message);
        result.put("chatId", chatId);
        result.put("messageId", messageId);
        result.put("imageUrl", imageUrl);
        result.put("read", read);
        result.put("timestamp", ServerValue.TIMESTAMP);
        result.put("metadata", metadata);
        return result;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public static String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) { // Less than 1 minute
            return "មុននេះបន្តិច";
        } else if (diff < 3600000) { // Less than 1 hour
            long minutes = diff / 60000;
            return minutes + " នាទីមុន";
        } else if (diff < 86400000) { // Less than 1 day
            long hours = diff / 3600000;
            return hours + " ម៉ោងមុន";
        } else if (diff < 604800000) { // Less than 1 week
            long days = diff / 86400000;
            return days + " ថ្ងៃមុន";
        } else {
            // Format as date
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    public static String getRelativeTime(String timestampStr) {
        try {
            long timestamp = Long.parseLong(timestampStr);
            return getRelativeTime(timestamp);
        } catch (NumberFormatException e) {
            return timestampStr;
        }
    }
}