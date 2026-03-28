package com.example.bay.model;

import com.google.firebase.database.PropertyName;

public class User {

    private String userId;
    private String first_name;
    private String last_name;
    private String email;
    private String phone;
    private String role;
    private String location;
    private String profileImageUrl;
    private Integer point;
    private String bio;
    private String deviceToken;

    private Long createdAt;
    private Long lastNameChangedAt;

    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean userVerified;
    private boolean online;
    private boolean profileCompleted;
    private Moderation moderation;

    public Moderation getModeration() {
        return moderation;
    }

    public User() {
        this.createdAt = 0L;
        this.lastNameChangedAt = 0L;
    }

    public User(String userId,
                String first_name,
                String last_name,
                String email,
                String phone,
                String role,
                String location,
                String profileImageUrl,
                String deviceToken,
                boolean profileCompleted) {

        long now = System.currentTimeMillis();

        this.userId = userId;
        this.first_name = first_name;
        this.last_name = last_name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.location = location;
        this.profileImageUrl = profileImageUrl;
        this.deviceToken = deviceToken;

        this.point = 0;
        this.bio = "Not bio yet.";

        this.createdAt = now;
        this.lastNameChangedAt = now;

        this.emailVerified = false;
        this.phoneVerified = false;
        this.userVerified = false;
        this.online = false;
        this.profileCompleted = profileCompleted;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("first_name")
    public String getFirstName() {
        return first_name;
    }

    @PropertyName("first_name")
    public void setFirstName(String first_name) {
        this.first_name = first_name;
    }

    @PropertyName("last_name")
    public String getLastName() {
        return last_name;
    }

    @PropertyName("last_name")
    public void setLastName(String last_name) {
        this.last_name = last_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Integer getPoint() {
        return point;
    }

    public void setPoint(Integer point) {
        this.point = point;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getLastNameChangedAt() {
        return lastNameChangedAt;
    }

    public void setLastNameChangedAt(Long lastNameChangedAt) {
        this.lastNameChangedAt = lastNameChangedAt;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public boolean isUserVerified() {
        return userVerified;
    }

    public void setUserVerified(boolean userVerified) {
        this.userVerified = userVerified;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setModeration(Moderation moderation) {
        this.moderation = moderation;
    }

    public boolean isBanned() {
        return moderation != null
                && "banned".equals(moderation.getStatus())
                && moderation.getExpiresAt() != null
                && System.currentTimeMillis() < moderation.getExpiresAt();
    }

    public boolean isWarned() {
        return moderation != null
                && "warned".equals(moderation.getStatus())
                && moderation.getExpiresAt() != null
                && System.currentTimeMillis() < moderation.getExpiresAt();
    }

    public boolean isSuspension(){
        return moderation != null
                && "suspended".equals(moderation.getStatus())
                && moderation.getExpiresAt() != null
                && System.currentTimeMillis() < moderation.getExpiresAt();
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public String getFullName() {
        StringBuilder fullName = new StringBuilder();

        if (first_name != null && !first_name.trim().isEmpty()) {
            fullName.append(first_name.trim());
        }

        if (last_name != null && !last_name.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(last_name.trim());
        }

        if (fullName.length() == 0) {
            return "User";
        }

        return fullName.toString();
    }
}