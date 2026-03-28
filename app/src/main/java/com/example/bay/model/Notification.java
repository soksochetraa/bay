package com.example.bay.model;

public class Notification {
    private String type;
    private String title;
    private String body;
    private String postId;
    private String sender;
    private String receiverId;
    private Boolean isSent;
    private Boolean isRead;

    // Empty constructor (required for Firebase)
    public Notification() {
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
}