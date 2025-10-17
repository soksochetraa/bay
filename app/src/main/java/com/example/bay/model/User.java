package com.example.bay.model;

public class User {
    public String userId;
    public String name;
    public String email;
    public String phone;
    public String profileImageUrl;
    public String password;
    public String role;

    public String location;

    public User() {
    }

    public User(String userId, String name, String email, String phone,String role,String location, String profileImageUrl) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.location = location;
        this.profileImageUrl = profileImageUrl;

    }

}