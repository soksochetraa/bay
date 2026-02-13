package com.example.bay.model;

import java.util.List;

public class Location {
    public Owner owner;
    public String name;
    public String category;
    public String status;
    public double latitude;
    public double longitude;
    public String profileUrl;
    public List<String> photos;
    public Contact contact;
    public Detail detail;
    public Visibility visibility;
    public String createdAt;

    public Location() {}

    public Location(Owner owner, String name, String category, String status,
                    double latitude, double longitude, String profileUrl,
                    List<String> photos, Contact contact, Detail detail,
                    Visibility visibility, String createdAt) {
        this.owner = owner;
        this.name = name;
        this.category = category;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.profileUrl = profileUrl;
        this.photos = photos;
        this.contact = contact;
        this.detail = detail;
        this.visibility = visibility;
        this.createdAt = createdAt;
    }

    public static class Owner {
        public String uuid;
        public Owner() {}
        public Owner(String uuid) { this.uuid = uuid; }
    }

    public static class Contact {
        public String phoneNumber;
        public String locationLink;

        public Contact() {}

        public Contact(String phoneNumber, String locationLink) {
            this.phoneNumber = phoneNumber;
            this.locationLink = locationLink;
        }
    }

    public static class Detail {
        public List<String> growing;
        public String about;

        public Detail() {}

        public Detail(List<String> growing, String about) {
            this.growing = growing;
            this.about = about;
        }
    }

    public static class Visibility {
        public boolean isVisible;
        public Visibility() {}
        public Visibility(boolean isVisible) { this.isVisible = isVisible; }
    }
}
