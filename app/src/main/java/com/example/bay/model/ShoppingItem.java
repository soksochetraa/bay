package com.example.bay.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ShoppingItem implements Parcelable {

    private String firebaseKey;
    private String itemId;
    private String name;
    private String category;
    private String description;
    private String price;
    private String unit;
    private List<String> images = new ArrayList<>();
    private String userId;

    private Float rating;
    private Integer review_count;

    private Long createdAt;
    private Long updatedAt;

    private String status;      // active, deleted, sold...
    private String visibility;  // visible, hidden

    private Moderation moderation; // warned/pending...

    // =========================
    // ✅ Moderation nested class
    // =========================
    public static class Moderation implements Parcelable {
        private String status; // warned
        private Long warnedAt;
        private Long expiresAt;
        private String warnedBy;
        private String warningMessage;

        public Moderation() {}

        protected Moderation(Parcel in) {
            status = in.readString();
            warnedAt = (Long) in.readValue(Long.class.getClassLoader());
            expiresAt = (Long) in.readValue(Long.class.getClassLoader());
            warnedBy = in.readString();
            warningMessage = in.readString();
        }

        public static final Creator<Moderation> CREATOR = new Creator<Moderation>() {
            @Override public Moderation createFromParcel(Parcel in) { return new Moderation(in); }
            @Override public Moderation[] newArray(int size) { return new Moderation[size]; }
        };

        @Override public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(status);
            dest.writeValue(warnedAt);
            dest.writeValue(expiresAt);
            dest.writeString(warnedBy);
            dest.writeString(warningMessage);
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Long getWarnedAt() { return warnedAt; }
        public void setWarnedAt(Long warnedAt) { this.warnedAt = warnedAt; }

        public Long getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

        public String getWarnedBy() { return warnedBy; }
        public void setWarnedBy(String warnedBy) { this.warnedBy = warnedBy; }

        public String getWarningMessage() { return warningMessage; }
        public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
    }

    // =========================
    // ✅ Helpers (IMPORTANT)
    // =========================
    public boolean isWarned() {
        return moderation != null && "warned".equalsIgnoreCase(moderation.getStatus());
    }

    public boolean isHiddenOnMarketplace() {
        // hide if visibility=hidden OR moderation warned
        if ("hidden".equalsIgnoreCase(visibility)) return true;
        return isWarned();
    }

    public boolean isDeleted() {
        return "deleted".equalsIgnoreCase(status);
    }

    public long getExpiresAtSafe() {
        if (moderation != null && moderation.getExpiresAt() != null) return moderation.getExpiresAt();
        return 0L;
    }

    public String getWarningMessageSafe() {
        if (moderation != null && moderation.getWarningMessage() != null && !moderation.getWarningMessage().trim().isEmpty()) {
            return moderation.getWarningMessage();
        }
        return "Your product was warned by admin. Please edit and reupload.";
    }

    public boolean shouldAutoDelete(long nowMs) {
        if (!isWarned()) return false;
        long exp = getExpiresAtSafe();
        return exp > 0 && nowMs >= exp;
    }

    // =========================
    // Parcelable (for edit intent)
    // =========================
    public ShoppingItem() {}

    protected ShoppingItem(Parcel in) {
        firebaseKey = in.readString();
        itemId = in.readString();
        name = in.readString();
        category = in.readString();
        description = in.readString();
        price = in.readString();
        unit = in.readString();
        images = in.createStringArrayList();
        userId = in.readString();
        rating = (Float) in.readValue(Float.class.getClassLoader());
        review_count = (Integer) in.readValue(Integer.class.getClassLoader());
        createdAt = (Long) in.readValue(Long.class.getClassLoader());
        updatedAt = (Long) in.readValue(Long.class.getClassLoader());
        status = in.readString();
        visibility = in.readString();
        moderation = in.readParcelable(Moderation.class.getClassLoader());
    }

    public static final Creator<ShoppingItem> CREATOR = new Creator<ShoppingItem>() {
        @Override public ShoppingItem createFromParcel(Parcel in) { return new ShoppingItem(in); }
        @Override public ShoppingItem[] newArray(int size) { return new ShoppingItem[size]; }
    };

    @Override public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(firebaseKey);
        dest.writeString(itemId);
        dest.writeString(name);
        dest.writeString(category);
        dest.writeString(description);
        dest.writeString(price);
        dest.writeString(unit);
        dest.writeStringList(images);
        dest.writeString(userId);
        dest.writeValue(rating);
        dest.writeValue(review_count);
        dest.writeValue(createdAt);
        dest.writeValue(updatedAt);
        dest.writeString(status);
        dest.writeString(visibility);
        dest.writeParcelable(moderation, flags);
    }

    // =========================
    // Getters / setters
    // =========================
    public String getFirebaseKey() { return firebaseKey; }
    public void setFirebaseKey(String firebaseKey) { this.firebaseKey = firebaseKey; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Float getRating() { return rating; }
    public void setRating(Float rating) { this.rating = rating; }

    public Integer getReview_count() { return review_count; }
    public void setReview_count(Integer review_count) { this.review_count = review_count; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    @Nullable
    public Moderation getModeration() { return moderation; }
    public void setModeration(Moderation moderation) { this.moderation = moderation; }
}