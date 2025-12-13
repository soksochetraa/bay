package com.example.bay.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bay.model.ShoppingItem;
import com.example.bay.service.ShoppingItemService;
import com.example.bay.util.RetrofitClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShoppingItemRepository {

    private final ShoppingItemService service;

    private final MutableLiveData<List<ShoppingItem>> allShoppingItemsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ShoppingItem>> limitedShoppingItemsLiveData = new MutableLiveData<>(new ArrayList<>());

    public ShoppingItemRepository() {
        service = RetrofitClient.getClient().create(ShoppingItemService.class);
    }

    // Call this to refresh "all"
    public void fetchAllShoppingCards() {
        service.getAllShoppingCard().enqueue(new Callback<Map<String, ShoppingItem>>() {
            @Override
            public void onResponse(Call<Map<String, ShoppingItem>> call, Response<Map<String, ShoppingItem>> response) {
                allShoppingItemsLiveData.postValue(mapToListWithKey(response.body()));
            }

            @Override
            public void onFailure(Call<Map<String, ShoppingItem>> call, Throwable t) {
                allShoppingItemsLiveData.postValue(new ArrayList<>());
            }
        });
    }

    // Call this to refresh "limited"
    public void fetchLimitedShoppingCards() {
        service.getAllShoppingCard().enqueue(new Callback<Map<String, ShoppingItem>>() {
            @Override
            public void onResponse(Call<Map<String, ShoppingItem>> call, Response<Map<String, ShoppingItem>> response) {
                List<ShoppingItem> all = mapToListWithKey(response.body());

                // group by category (keep insertion order)
                Map<String, List<ShoppingItem>> grouped = new LinkedHashMap<>();
                for (ShoppingItem item : all) {
                    String cat = safeCategory(item != null ? item.getCategory() : null);
                    if (!grouped.containsKey(cat)) grouped.put(cat, new ArrayList<>());
                    grouped.get(cat).add(item);
                }

                // take 2 per category
                List<ShoppingItem> limited = new ArrayList<>();
                for (List<ShoppingItem> items : grouped.values()) {
                    for (int i = 0; i < Math.min(2, items.size()); i++) {
                        limited.add(items.get(i));
                    }
                }

                limitedShoppingItemsLiveData.postValue(limited);
            }

            @Override
            public void onFailure(Call<Map<String, ShoppingItem>> call, Throwable t) {
                limitedShoppingItemsLiveData.postValue(new ArrayList<>());
            }
        });
    }

    public LiveData<List<ShoppingItem>> observeAllShoppingCards() {
        return allShoppingItemsLiveData;
    }

    public LiveData<List<ShoppingItem>> observeLimitedShoppingCards() {
        return limitedShoppingItemsLiveData;
    }

    private List<ShoppingItem> mapToListWithKey(Map<String, ShoppingItem> body) {
        if (body == null || body.isEmpty()) return new ArrayList<>();

        List<ShoppingItem> list = new ArrayList<>();
        for (Map.Entry<String, ShoppingItem> e : body.entrySet()) {
            ShoppingItem item = e.getValue();
            if (item == null) continue;

            // Ensure itemId from Firebase key
            if (item.getItemId() == null || item.getItemId().isEmpty()) {
                item.setItemId(e.getKey());
            }

            // Normalize category so filter works
            item.setCategory(safeCategory(item.getCategory()));

            list.add(item);
        }
        return list;
    }

    private String safeCategory(String c) {
        if (c == null) return "others";
        String x = c.trim();
        if (x.isEmpty()) return "others";
        return x.toLowerCase(); // make filter easier: vegetables, fruits, tools
    }
}
