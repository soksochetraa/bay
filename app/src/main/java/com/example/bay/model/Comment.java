package com.example.bay.model;

public class Comment {
    private String commentId;
    private String userId;
    private String text;
    private String timestamp;

    // For replies: ID of the comment this one replies to
    private String parentCommentId;

    public Comment() {
        // Required for Firebase
    }

    // Normal comment (no reply)
    public Comment(String commentId, String userId, String text, String timestamp) {
        this.commentId = commentId;
        this.userId = userId;
        this.text = text;
        this.timestamp = timestamp;
        this.parentCommentId = null;
    }

    // Reply comment
    public Comment(String commentId, String userId, String text, String timestamp,
                   String parentCommentId) {
        this.commentId = commentId;
        this.userId = userId;
        this.text = text;
        this.timestamp = timestamp;
        this.parentCommentId = parentCommentId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }
}
