package com.example.bay.model;

import java.util.UUID;

public class LearninghubCard {
    private String uuid;
    private String title;
    private String description;
    private String category;
    private String imageUrl;
    private String content;
    private String author;
    private String date;
    private boolean isSaved;
    private String createdAt;

    // Local cached searchable text
    private transient String searchableText;

    public LearninghubCard() {
        this.uuid = UUID.randomUUID().toString();
    }

    public LearninghubCard(String title, String description, String category,
                           String imageUrl, String content, String author,
                           String date, String readTime, boolean isSaved) {
        this.uuid = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.content = content;
        this.author = author;
        this.date = date;
        this.isSaved = isSaved;
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }

    // --- Getters & Setters ---
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public boolean getIsSaved() { return isSaved; }
    public void setIsSaved(boolean saved) { isSaved = saved; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // --- Helper Methods ---
    public void buildSearchIndex() {
        this.searchableText = (
                (title != null ? title : "") + " " +
                        (author != null ? author : "") + " " +
                        (description != null ? description : "") + " " +
                        (content != null ? content : "")
        ).toLowerCase();
    }

    public boolean matchesSearch(String query) {
        if (searchableText == null) buildSearchIndex();
        return query.isEmpty() || searchableText.contains(query.toLowerCase());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LearninghubCard that = (LearninghubCard) obj;
        return uuid != null && uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }
}
