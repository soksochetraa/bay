package com.example.bay.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bay.model.ShoppingItem;
import com.example.bay.service.ShoppingItemService;
import com.example.bay.util.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShoppingItemRepository {
    private final ShoppingItemService service;
    private final MutableLiveData<List<ShoppingItem>> allShoppingItemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ShoppingItem>> limitedShoppingItemsLiveData = new MutableLiveData<>();

    public ShoppingItemRepository() {
        service = RetrofitClient.getClient().create(ShoppingItemService.class);
    }

    public LiveData<List<ShoppingItem>> getAllShoppingCards() {
        service.getAllShoppingCard().enqueue(new Callback<Map<String, ShoppingItem>>() {
            @Override
            public void onResponse(Call<Map<String, ShoppingItem>> call, Response<Map<String, ShoppingItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allShoppingItemsLiveData.postValue(new ArrayList<>(response.body().values()));
                } else {
                    allShoppingItemsLiveData.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<Map<String, ShoppingItem>> call, Throwable t) {
                allShoppingItemsLiveData.postValue(new ArrayList<>());
            }
        });

        return allShoppingItemsLiveData;
    }

    public LiveData<List<ShoppingItem>> getLimitedShoppingCards() {
        service.getAllShoppingCard().enqueue(new Callback<Map<String, ShoppingItem>>() {
            @Override
            public void onResponse(Call<Map<String, ShoppingItem>> call, Response<Map<String, ShoppingItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, List<ShoppingItem>> categorizedItems = new HashMap<>();

                    for (ShoppingItem item : response.body().values()) {
                        String category = item.getCategory();
                        if (!categorizedItems.containsKey(category)) {
                            categorizedItems.put(category, new ArrayList<>());
                        }
                        categorizedItems.get(category).add(item);
                    }

                    List<ShoppingItem> limitedItems = new ArrayList<>();
                    for (List<ShoppingItem> items : categorizedItems.values()) {
                        for (int i = 0; i < Math.min(2, items.size()); i++) {
                            limitedItems.add(items.get(i));
                        }
                    }

                    limitedShoppingItemsLiveData.postValue(limitedItems);
                } else {
                    limitedShoppingItemsLiveData.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<Map<String, ShoppingItem>> call, Throwable t) {
                limitedShoppingItemsLiveData.postValue(new ArrayList<>());
            }
        });

        return limitedShoppingItemsLiveData;
    }
}
