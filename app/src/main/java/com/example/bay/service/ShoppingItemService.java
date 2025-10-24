package com.example.bay.service;

import com.example.bay.model.ShoppingItem;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ShoppingItemService {

    @GET("shoppingItems.json")
    Call<Map<String, ShoppingItem>> getAllShoppingCard();

}
