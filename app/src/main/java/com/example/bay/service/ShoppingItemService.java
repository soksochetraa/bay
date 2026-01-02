package com.example.bay.service;

import com.example.bay.model.ShoppingItem;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ShoppingItemService {
    @GET("shoppingItems.json")
    Call<Map<String, ShoppingItem>> getAllShoppingItems();

    @POST("shoppingItems.json")
    Call<ShoppingItem> createShoppingItem(@Body ShoppingItem item);

    @PUT("shoppingItems/{itemId}.json")
    Call<ShoppingItem> updateShoppingItem(@Path("itemId") String itemId, @Body ShoppingItem item);
}