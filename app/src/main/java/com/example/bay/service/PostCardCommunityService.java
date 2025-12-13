package com.example.bay.service;

import com.example.bay.model.PostCardItem;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;

public interface PostCardCommunityService {

    @GET("postCardItems.json")
    Call<Map<String, PostCardItem>> getAllPostCardItems();
}
