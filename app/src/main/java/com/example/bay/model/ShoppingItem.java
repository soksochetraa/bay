package com.example.bay.model;

import java.util.Date;

public class ShoppingItem {
    private String itemId;
    private String name;
    private String price;
    private String unit;
    private String imageUrl;
    private String userId;
    private String category;
    private Date createdAt;
    private Date updatedAt;

    public ShoppingItem() {
    }

    public ShoppingItem(String itemId, String name, String price, String unit, String imageUrl,
                        String userId, String category, Date createdAt, Date updatedAt) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.unit = unit;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public ShoppingItem(String itemId, String name, String price, String unit, String imageUrl,
                        String userId, String category) {
        this(itemId, name, price, unit, imageUrl, userId, category, new Date(), new Date());
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ShoppingItem{" +
                "itemId='" + itemId + '\'' +
                ", name='" + name + '\'' +
                ", price='" + price + '\'' +
                ", unit='" + unit + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", userId='" + userId + '\'' +
                ", category='" + category + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}