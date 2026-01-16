package com.example.bay.model;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

public class ShoppingItem implements Parcelable {
    private String itemId;
    private String name;
    private String category;
    private String price;
    private String description;
    private String unit;
    private List<String> images;
    private float rating;
    private int review_count;
    private String userId;
    private String status; // "active", "sold", "inactive"
    private Long createdAt;
    private Long updatedAt;

    // Add Firebase key field
    private String firebaseKey;

    public ShoppingItem() {
        this.status = "active";
    }

    public ShoppingItem(String itemId, String name, String category, String price,
                        String description, String unit, List<String> images,
                        float rating, int review_count, String userId) {
        this.itemId = itemId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.unit = unit;
        this.images = images;
        this.rating = rating;
        this.review_count = review_count;
        this.userId = userId;
        this.status = "active";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public ShoppingItem(String itemId, String name, String category, String price,
                        String description, String unit, List<String> images,
                        float rating, int review_count, String userId, String status) {
        this.itemId = itemId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.unit = unit;
        this.images = images;
        this.rating = rating;
        this.review_count = review_count;
        this.userId = userId;
        this.status = status != null ? status : "active";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    protected ShoppingItem(Parcel in) {
        itemId = in.readString();
        name = in.readString();
        category = in.readString();
        price = in.readString();
        description = in.readString();
        unit = in.readString();
        images = in.createStringArrayList();
        rating = in.readFloat();
        review_count = in.readInt();
        userId = in.readString();
        status = in.readString();
        firebaseKey = in.readString();
        if (in.readByte() == 0) {
            createdAt = null;
        } else {
            createdAt = in.readLong();
        }
        if (in.readByte() == 0) {
            updatedAt = null;
        } else {
            updatedAt = in.readLong();
        }
    }

    public static final Creator<ShoppingItem> CREATOR = new Creator<ShoppingItem>() {
        @Override
        public ShoppingItem createFromParcel(Parcel in) {
            return new ShoppingItem(in);
        }

        @Override
        public ShoppingItem[] newArray(int size) {
            return new ShoppingItem[size];
        }
    };

    // Getters and Setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getReview_count() { return review_count; }
    public void setReview_count(int review_count) { this.review_count = review_count; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStatus() { return status != null ? status : "active"; }
    public void setStatus(String status) { this.status = status != null ? status : "active"; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    // Firebase key getter and setter
    public String getFirebaseKey() { return firebaseKey; }
    public void setFirebaseKey(String firebaseKey) { this.firebaseKey = firebaseKey; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(itemId);
        dest.writeString(name);
        dest.writeString(category);
        dest.writeString(price);
        dest.writeString(description);
        dest.writeString(unit);
        dest.writeStringList(images);
        dest.writeFloat(rating);
        dest.writeInt(review_count);
        dest.writeString(userId);
        dest.writeString(status);
        dest.writeString(firebaseKey);
        if (createdAt == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(createdAt);
        }
        if (updatedAt == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(updatedAt);
        }
    }

    @Override
    public String toString() {
        return "ShoppingItem{" +
                "itemId='" + itemId + '\'' +
                ", name='" + name + '\'' +
                ", firebaseKey='" + firebaseKey + '\'' +
                ", category='" + category + '\'' +
                ", price='" + price + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}