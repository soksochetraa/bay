package com.example.bay.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostCardItem {

    private String itemId;
    private String userId;
    private String title;
    private String content;
    private List<String> imageUrls;
    private String timestamp;

    // 🔹 Use primitive long – Firebase can map Long → long safely.
    private long likeCount;
    private long commentCount;
    private long saveCount;

    private Map<String, Boolean> likedBy;  // {"userId": true}
    private Map<String, Boolean> savedBy;  // {"userId": true}
    private Map<String, Comment> comments; // {"commentId": CommentObject}

    public PostCardItem(String itemId, String userId, String title, String content,
                        List<String> imageUrls, String timestamp) {
        this.itemId = itemId;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.imageUrls = imageUrls;
        this.timestamp = timestamp;
    }

    public PostCardItem() {}

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // 🔹 Counters
    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }

    public long getSaveCount() {
        return saveCount;
    }

    public void setSaveCount(long saveCount) {
        this.saveCount = saveCount;
    }

    public Map<String, Boolean> getLikedBy() {
        return likedBy;
    }

    public void setLikedBy(Map<String, Boolean> likedBy) {
        this.likedBy = likedBy;
    }

    public Map<String, Boolean> getSavedBy() {
        return savedBy;
    }

    public void setSavedBy(Map<String, Boolean> savedBy) {
        this.savedBy = savedBy;
    }

    public Map<String, Comment> getComments() {
        return comments;
    }

    public void setComments(Map<String, Comment> comments) {
        this.comments = comments;
    }

    public List<Comment> getCommentsAsList() {
        if (comments != null) {
            return new ArrayList<>(comments.values());
        }
        return new ArrayList<>();
    }

    public List<String> getLikedByUserIds() {
        if (likedBy != null) {
            return new ArrayList<>(likedBy.keySet());
        }
        return new ArrayList<>();
    }

    public List<String> getSavedByUserIds() {
        if (savedBy != null) {
            return new ArrayList<>(savedBy.keySet());
        }
        return new ArrayList<>();
    }

    public boolean isLikedByUser(String userId) {
        return likedBy != null && likedBy.containsKey(userId);
    }

    public boolean isSavedByUser(String userId) {
        return savedBy != null && savedBy.containsKey(userId);
    }
}
