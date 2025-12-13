package com.example.bay.service;

import com.example.bay.model.PostCardItem;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PostCardItemService {

    @GET("postCardItems.json")
    Call<Map<String, PostCardItem>> getAllPostCardItems();

    @GET("postCardItems.json")
    Call<Map<String, PostCardItem>> getLatestPostCardItems(
            @Query("orderBy") String orderBy,
            @Query("limitToLast") int limitToLast
    );
}
