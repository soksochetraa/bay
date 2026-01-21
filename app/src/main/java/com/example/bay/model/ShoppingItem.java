package com.example.bay.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class ShoppingItem implements Parcelable {
    private String itemId;
    private String userId;
    private String name;
    private String description;
    private String category;
    private String price;
    private String unit;
    private float rating;
    private int review_count;
    private List<String> images;
    private Long createdAt;
    private Long updatedAt;
    private String firebaseKey; // Add this field

    public ShoppingItem() {
        // Default constructor required for Firebase
    }

    public ShoppingItem(String itemId, String userId, String name, String description,
                        String category, String price, String unit, float rating,
                        int review_count, List<String> images) {
        this.itemId = itemId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.unit = unit;
        this.rating = rating;
        this.review_count = review_count;
        this.images = images;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    protected ShoppingItem(Parcel in) {
        itemId = in.readString();
        userId = in.readString();
        name = in.readString();
        description = in.readString();
        category = in.readString();
        price = in.readString();
        unit = in.readString();
        rating = in.readFloat();
        review_count = in.readInt();
        images = in.createStringArrayList();
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
        firebaseKey = in.readString(); // Read Firebase key
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(itemId);
        dest.writeString(userId);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(category);
        dest.writeString(price);
        dest.writeString(unit);
        dest.writeFloat(rating);
        dest.writeInt(review_count);
        dest.writeStringList(images);
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
        dest.writeString(firebaseKey); // Write Firebase key
    }

    @Override
    public int describeContents() {
        return 0;
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

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getReview_count() { return review_count; }
    public void setReview_count(int review_count) { this.review_count = review_count; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public String getFirebaseKey() { return firebaseKey; }
    public void setFirebaseKey(String firebaseKey) { this.firebaseKey = firebaseKey; }
}