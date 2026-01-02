package com.example.bay.repository;

import androidx.annotation.NonNull;
import android.util.Log;

import com.example.bay.model.ShoppingItem;
import com.example.bay.service.ShoppingItemService;
import com.example.bay.util.RetrofitClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShoppingItemRepository {
    private static final String TAG = "ShoppingRepo";
    private final ShoppingItemService shoppingItemService;
    private final List<ShoppingItem> limitedShoppingItems = new ArrayList<>();

    public ShoppingItemRepository() {
        shoppingItemService = RetrofitClient.getClient().create(ShoppingItemService.class);
    }

    // ✅ Load only limited number of items for home screen
    public void fetchLimitedShoppingItems(int limit, ShoppingItemCallback<List<ShoppingItem>> callback) {
        Log.d(TAG, "Fetching " + limit + " shopping items for home screen...");

        shoppingItemService.getAllShoppingItems().enqueue(new Callback<Map<String, ShoppingItem>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, ShoppingItem>> call,
                                   @NonNull Response<Map<String, ShoppingItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, ShoppingItem> itemsMap = response.body();
                    List<ShoppingItem> allItems = new ArrayList<>(itemsMap.values());

                    // Sort by createdAt (newest first)
                    Collections.sort(allItems, new Comparator<ShoppingItem>() {
                        @Override
                        public int compare(ShoppingItem item1, ShoppingItem item2) {
                            Long time1 = item1.getCreatedAt() != null ? item1.getCreatedAt() : 0L;
                            Long time2 = item2.getCreatedAt() != null ? item2.getCreatedAt() : 0L;
                            return Long.compare(time2, time1); // Newest first
                        }
                    });

                    // Take only the specified limit
                    int count = Math.min(allItems.size(), limit);
                    List<ShoppingItem> limitedItems = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        limitedItems.add(allItems.get(i));
                    }

                    limitedShoppingItems.clear();
                    limitedShoppingItems.addAll(limitedItems);

                    Log.d(TAG, "Successfully fetched " + limitedItems.size() + " items for home screen");
                    callback.onSuccess(limitedItems);
                } else {
                    Log.e(TAG, "Failed to fetch items. Code: " + response.code());
                    callback.onError("Failed to fetch items: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, ShoppingItem>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching shopping items: " + t.getMessage());
                callback.onError("Error: " + t.getMessage());
            }
        });
    }

    // ✅ Get the cached limited items
    public List<ShoppingItem> getLimitedShoppingItems() {
        return new ArrayList<>(limitedShoppingItems);
    }

    // ✅ Original method (keep for compatibility)
    public void getAllShoppingItems(ShoppingItemCallback<Map<String, ShoppingItem>> callback) {
        Log.d(TAG, "Fetching all shopping items...");

        shoppingItemService.getAllShoppingItems().enqueue(new Callback<Map<String, ShoppingItem>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, ShoppingItem>> call,
                                   @NonNull Response<Map<String, ShoppingItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, ShoppingItem> items = response.body();
                    Log.d(TAG, "Successfully fetched " + items.size() + " items");
                    callback.onSuccess(items);
                } else {
                    Log.e(TAG, "Failed to fetch items. Code: " + response.code());
                    callback.onError("Failed to fetch items: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, ShoppingItem>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching shopping items: " + t.getMessage());
                callback.onError("Error: " + t.getMessage());
            }
        });
    }

    public void createShoppingItem(ShoppingItem item, ShoppingItemCallback<ShoppingItem> callback) {
        shoppingItemService.createShoppingItem(item).enqueue(new Callback<ShoppingItem>() {
            @Override
            public void onResponse(@NonNull Call<ShoppingItem> call, @NonNull Response<ShoppingItem> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to create item: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ShoppingItem> call, @NonNull Throwable t) {
                callback.onError("Error: " + t.getMessage());
            }
        });
    }

    public List<ShoppingItem> filterByCategory(List<ShoppingItem> items, String category) {
        if (category.equals("ទាំងអស់") || category.isEmpty()) {
            return items;
        }
        List<ShoppingItem> filtered = new ArrayList<>();
        for (ShoppingItem item : items) {
            if (item.getCategory() != null && item.getCategory().equals(category)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public List<ShoppingItem> searchItems(List<ShoppingItem> items, String query) {
        if (query == null || query.trim().isEmpty()) {
            return items;
        }

        String searchQuery = query.toLowerCase().trim();
        List<ShoppingItem> results = new ArrayList<>();

        for (ShoppingItem item : items) {
            boolean matches = false;

            if (item.getName() != null && item.getName().toLowerCase().contains(searchQuery)) {
                matches = true;
            } else if (item.getDescription() != null &&
                    item.getDescription().toLowerCase().contains(searchQuery)) {
                matches = true;
            } else if (item.getCategory() != null &&
                    item.getCategory().toLowerCase().contains(searchQuery)) {
                matches = true;
            }

            if (matches) {
                results.add(item);
            }
        }
        return results;
    }

    public interface ShoppingItemCallback<T> {
        void onSuccess(T result);
        void onError(String errorMsg);
    }
}