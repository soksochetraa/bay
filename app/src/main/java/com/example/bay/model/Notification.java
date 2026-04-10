package com.example.bay.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Notification {
    private String type;
    private String title;
    private String body;
    private String postId;
    private String sender;
    private String receiverId;
    private Boolean isSent;
    private Boolean isRead;
    private String timestamp;  // Added timestamp field
    private String notificationId;  // Added notification ID field

    // Empty constructor (required for Firebase)
    public Notification() {
        // Auto-generate timestamp when created
        this.timestamp = String.valueOf(System.currentTimeMillis());
    }

    // Full constructor
    public Notification(String type, String title, String body, String postId,
                        String sender, String receiverId,
                        Boolean isSent, Boolean isRead) {
        this.type = type;
        this.title = title;
        this.body = body;
        this.postId = postId;
        this.sender = sender;
        this.receiverId = receiverId;
        this.isSent = isSent;
        this.isRead = isRead;
        this.timestamp = String.valueOf(System.currentTimeMillis());
    }

    // Constructor with custom timestamp
    public Notification(String type, String title, String body, String postId,
                        String sender, String receiverId,
                        Boolean isSent, Boolean isRead, String timestamp) {
        this.type = type;
        this.title = title;
        this.body = body;
        this.postId = postId;
        this.sender = sender;
        this.receiverId = receiverId;
        this.isSent = isSent;
        this.isRead = isRead;
        this.timestamp = timestamp;
    }

    // Getters and Setters

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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public Boolean getIsSent() {
        return isSent;
    }

    public void setIsSent(Boolean isSent) {
        this.isSent = isSent;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    // Helper methods for timestamp handling
    public long getTimestampMillis() {
        if (timestamp == null || timestamp.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setCurrentTimestamp() {
        this.timestamp = String.valueOf(System.currentTimeMillis());
    }

    public boolean isChatNotification() {
        return type != null && type.equals("chat");
    }

    public boolean isPostNotification() {
        return type != null && type.equals("post");
    }

    public boolean isSystemNotification() {
        return type != null && type.equals("system");
    }

    public boolean isUnread() {
        return isRead != null && !isRead;
    }

    // Method to get formatted time using TimeUtils
    public String getFormattedTime() {
        if (timestamp == null || timestamp.isEmpty()) {
            return "មិនទាន់មាន";
        }
        return com.example.bay.util.TimeUtils.getRelativeTime(timestamp);
    }

    // Method to get sender's display name (to be used with UserRepository)
    public String getSenderDisplayName() {
        // This will be populated asynchronously in the adapter
        return sender;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", postId='" + postId + '\'' +
                ", sender='" + sender + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", isSent=" + isSent +
                ", isRead=" + isRead +
                ", timestamp='" + timestamp + '\'' +
                ", notificationId='" + notificationId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Notification that = (Notification) obj;
        return notificationId != null && notificationId.equals(that.notificationId);
    }

    @Override
    public int hashCode() {
        return notificationId != null ? notificationId.hashCode() : 0;
    }
}