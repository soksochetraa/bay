package com.example.bay.model;

public class SearchResult {
    public static final int TYPE_USER = 1;
    public static final int TYPE_POST = 2;

    private int type;
    private Object data;

    public SearchResult(int type, Object data) {
        this.type = type;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public User getUser() {
        if (type == TYPE_USER) {
            return (User) data;
        }
        return null;
    }

    public PostCardItem getPost() {
        if (type == TYPE_POST) {
            return (PostCardItem) data;
        }
        return null;
    }
}