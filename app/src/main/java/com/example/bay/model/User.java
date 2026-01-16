package com.example.bay.model;

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

    public User() {
    }

    public User(String userId, String first_name,String last_name, String email, String phone,
                String role, String location, String profileImageUrl, String deviceToken) {
        this.userId = userId;
        this.first_name = first_name;
        this.last_name = last_name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.location = location;
        this.profileImageUrl = profileImageUrl;
        this.point = 0;
        this.bio = "";
        this.deviceToken = deviceToken;
    }
    public String getDeviceToken() {
        return deviceToken;}
    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getBio() {
        return bio;
    }
    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getPoint() {
        return point;
    }

    public void setPoint(Integer point) {
        this.point = point;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
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
}
