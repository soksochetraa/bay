package com.example.bay.service;

import com.example.bay.model.Review;
import com.example.bay.model.ShoppingItem;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ProductDetailService {

    @GET("shoppingItems/{itemId}.json")
    Call<ShoppingItem> getShoppingItem(@Path("itemId") String itemId);

    @GET("reviews.json")
    Call<Map<String, Review>> getAllReviews();
}