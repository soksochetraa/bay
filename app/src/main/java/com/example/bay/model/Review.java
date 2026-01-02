package com.example.bay.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Review implements Parcelable {
    private String reviewId;
    private String itemId;
    private String userId;
    private String comment;
    private float rating;
    private Long createdAt;
    private Long updatedAt;

    public Review() {
        // Default constructor required for Firebase
    }

    public Review(String reviewId, String itemId, String userId,
                  String comment, float rating) {
        this.reviewId = reviewId;
        this.itemId = itemId;
        this.userId = userId;
        this.comment = comment;
        this.rating = rating;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    protected Review(Parcel in) {
        reviewId = in.readString();
        itemId = in.readString();
        userId = in.readString();
        comment = in.readString();
        rating = in.readFloat();
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

    public static final Creator<Review> CREATOR = new Creator<Review>() {
        @Override
        public Review createFromParcel(Parcel in) {
            return new Review(in);
        }

        @Override
        public Review[] newArray(int size) {
            return new Review[size];
        }
    };

    // Getters and Setters
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(reviewId);
        dest.writeString(itemId);
        dest.writeString(userId);
        dest.writeString(comment);
        dest.writeFloat(rating);
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
}