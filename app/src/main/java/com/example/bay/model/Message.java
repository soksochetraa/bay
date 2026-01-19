package com.example.bay.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String text;
    private String imageUrl;
    private String thumbnailUrl;
    private int imageWidth;
    private int imageHeight;
    private String fileName;
    private long fileSize;
    private String type;
    private long timestamp;
    private boolean read;
    private Map<String, String> metadata;

    public Message() {
    }

    public Message(String senderId, String receiverId, String text) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.type = "text";
        this.timestamp = System.currentTimeMillis();
        this.read = false;
        this.metadata = new HashMap<>();
    }

    public Message(String senderId, String receiverId, String imageUrl, String thumbnailUrl, int width, int height, String fileName, long fileSize) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.imageWidth = width;
        this.imageHeight = height;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.type = "image";
        this.timestamp = System.currentTimeMillis();
        this.read = false;
        this.metadata = new HashMap<>();
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("senderId", senderId);
        result.put("receiverId", receiverId);
        result.put("text", text);
        result.put("imageUrl", imageUrl);
        result.put("thumbnailUrl", thumbnailUrl);
        result.put("imageWidth", imageWidth);
        result.put("imageHeight", imageHeight);
        result.put("fileName", fileName);
        result.put("fileSize", fileSize);
        result.put("type", type);
        result.put("timestamp", ServerValue.TIMESTAMP);
        result.put("read", read);
        result.put("metadata", metadata);
        return result;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}