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

    public ShoppingItemRepository() {
        shoppingItemService = RetrofitClient.getClient().create(ShoppingItemService.class);
        Log.d(TAG, "Repository initialized");
    }

    // ✅ DELETE: Delete shopping item by Firebase key
    public void deleteShoppingItemByFirebaseKey(String firebaseKey, ShoppingItemCallback<Void> callback) {
        Log.d(TAG, "🗑️ Deleting item by Firebase key: " + firebaseKey);

        Call<Void> call = shoppingItemService.deleteShoppingItem(firebaseKey);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                Log.d(TAG, "DELETE Response Code: " + response.code());

                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ DELETE SUCCESS for Firebase key: " + firebaseKey);
                    callback.onSuccess(null);
                } else {
                    String error = "DELETE failed. Code: " + response.code() +
                            ", Message: " + response.message();
                    Log.e(TAG, "❌ " + error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, "❌ " + error);
                callback.onError(error);
            }
        });
    }

    // ✅ DELETE: Delete shopping item by itemId (finds Firebase key first)
    public void deleteShoppingItem(String itemId, ShoppingItemCallback<Void> callback) {
        Log.d(TAG, "🗑️ Deleting item by itemId: " + itemId);

        // First find the Firebase key for this itemId
        getFirebaseKeyByItemId(itemId, new ShoppingItemCallback<String>() {
            @Override
            public void onSuccess(String firebaseKey) {
                // Now delete using the Firebase key
                deleteShoppingItemByFirebaseKey(firebaseKey, callback);
            }

            @Override
            public void onError(String errorMsg) {
                callback.onError("Cannot find item: " + errorMsg);
            }
        });
    }

    // ✅ UPDATE: Update shopping item by Firebase key
    public void updateShoppingItemByFirebaseKey(String firebaseKey, ShoppingItem item, ShoppingItemCallback<ShoppingItem> callback) {
        Log.d(TAG, "✏️ Updating item by Firebase key: " + firebaseKey);
        Log.d(TAG, "Item data: " + item.toString());

        Call<ShoppingItem> call = shoppingItemService.updateShoppingItem(firebaseKey, item);
        call.enqueue(new Callback<ShoppingItem>() {
            @Override
            public void onResponse(@NonNull Call<ShoppingItem> call, @NonNull Response<ShoppingItem> response) {
                Log.d(TAG, "UPDATE Response Code: " + response.code());

                if (response.isSuccessful()) {
                    ShoppingItem updatedItem = response.body();
                    Log.d(TAG, "✅ UPDATE SUCCESS for Firebase key: " + firebaseKey);
                    callback.onSuccess(updatedItem);
                } else {
                    String error = "UPDATE failed. Code: " + response.code() +
                            ", Message: " + response.message();
                    Log.e(TAG, "❌ " + error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ShoppingItem> call, @NonNull Throwable t) {
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, "❌ " + error);
                callback.onError(error);
            }
        });
    }

    // ✅ UPDATE: Update shopping item (finds Firebase key first)
    public void updateShoppingItem(ShoppingItem item, ShoppingItemCallback<ShoppingItem> callback) {
        Log.d(TAG, "✏️ Updating item by itemId: " + item.getItemId());

        if (item.getFirebaseKey() != null && !item.getFirebaseKey().isEmpty()) {
            // Use existing Firebase key
            updateShoppingItemByFirebaseKey(item.getFirebaseKey(), item, callback);
        } else {
            // Find Firebase key first
            getFirebaseKeyByItemId(item.getItemId(), new ShoppingItemCallback<String>() {
                @Override
                public void onSuccess(String firebaseKey) {
                    item.setFirebaseKey(firebaseKey);
                    updateShoppingItemByFirebaseKey(firebaseKey, item, callback);
                }

                @Override
                public void onError(String errorMsg) {
                    callback.onError("Cannot find item: " + errorMsg);
                }
            });
        }
    }

    // ✅ CREATE: Create new shopping item
    public void createShoppingItem(ShoppingItem item, ShoppingItemCallback<ShoppingItem> callback) {
        Log.d(TAG, "➕ Creating new item: " + item.getName());

        Call<ShoppingItem> call = shoppingItemService.createShoppingItem(item);
        call.enqueue(new Callback<ShoppingItem>() {
            @Override
            public void onResponse(@NonNull Call<ShoppingItem> call, @NonNull Response<ShoppingItem> response) {
                Log.d(TAG, "CREATE Response Code: " + response.code());

                if (response.isSuccessful()) {
                    ShoppingItem createdItem = response.body();
                    Log.d(TAG, "✅ CREATE SUCCESS - Item ID: " +
                            (createdItem != null ? createdItem.getItemId() : "unknown"));
                    callback.onSuccess(createdItem);
                } else {
                    String error = "CREATE failed. Code: " + response.code() +
                            ", Message: " + response.message();
                    Log.e(TAG, "❌ " + error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ShoppingItem> call, @NonNull Throwable t) {
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, "❌ " + error);
                callback.onError(error);
            }
        });
    }

    // ✅ GET ALL: Get all shopping items with Firebase keys
    public void getAllShoppingItems(ShoppingItemCallback<Map<String, ShoppingItem>> callback) {
        Log.d(TAG, "📋 Fetching all shopping items...");

        Call<Map<String, ShoppingItem>> call = shoppingItemService.getAllShoppingItems();
        call.enqueue(new Callback<Map<String, ShoppingItem>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, ShoppingItem>> call,
                                   @NonNull Response<Map<String, ShoppingItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, ShoppingItem> items = response.body();

                    // Add Firebase keys to each item
                    for (Map.Entry<String, ShoppingItem> entry : items.entrySet()) {
                        entry.getValue().setFirebaseKey(entry.getKey());
                    }

                    Log.d(TAG, "✅ Successfully fetched " + items.size() + " items");
                    callback.onSuccess(items);
                } else {
                    String error = "Failed to fetch items. Code: " + response.code();
                    Log.e(TAG, "❌ " + error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, ShoppingItem>> call, @NonNull Throwable t) {
                String error = "Error fetching shopping items: " + t.getMessage();
                Log.e(TAG, "❌ " + error);
                callback.onError(error);
            }
        });
    }

    // ✅ GET: Get Firebase key by itemId
    public void getFirebaseKeyByItemId(String itemId, ShoppingItemCallback<String> callback) {
        Log.d(TAG, "🔍 Looking for Firebase key for itemId: " + itemId);

        getAllShoppingItems(new ShoppingItemCallback<Map<String, ShoppingItem>>() {
            @Override
            public void onSuccess(Map<String, ShoppingItem> result) {
                String firebaseKey = null;

                for (Map.Entry<String, ShoppingItem> entry : result.entrySet()) {
                    ShoppingItem item = entry.getValue();
                    if (item.getItemId() != null && item.getItemId().equals(itemId)) {
                        firebaseKey = entry.getKey();
                        break;
                    }
                }

                if (firebaseKey != null) {
                    Log.d(TAG, "✅ Found Firebase key: " + firebaseKey + " for itemId: " + itemId);
                    callback.onSuccess(firebaseKey);
                } else {
                    String error = "No Firebase key found for itemId: " + itemId;
                    Log.e(TAG, "❌ " + error);
                    callback.onError(error);
                }
            }

            @Override
            public void onError(String errorMsg) {
                callback.onError(errorMsg);
            }
        });
    }

    // ✅ GET USER ITEMS: Get items by user ID with Firebase keys
    public void getUserItems(String userId, ShoppingItemCallback<List<ShoppingItem>> callback) {
        Log.d(TAG, "👤 Fetching items for user: " + userId);

        getAllShoppingItems(new ShoppingItemCallback<Map<String, ShoppingItem>>() {
            @Override
            public void onSuccess(Map<String, ShoppingItem> result) {
                List<ShoppingItem> userItems = new ArrayList<>();

                for (Map.Entry<String, ShoppingItem> entry : result.entrySet()) {
                    ShoppingItem item = entry.getValue();
                    if (item.getUserId() != null && item.getUserId().equals(userId)) {
                        userItems.add(item);
                    }
                }

                // Sort by date (newest first)
                Collections.sort(userItems, new Comparator<ShoppingItem>() {
                    @Override
                    public int compare(ShoppingItem item1, ShoppingItem item2) {
                        Long time1 = item1.getCreatedAt() != null ? item1.getCreatedAt() : 0L;
                        Long time2 = item2.getCreatedAt() != null ? item2.getCreatedAt() : 0L;
                        return Long.compare(time2, time1);
                    }
                });

                Log.d(TAG, "✅ Found " + userItems.size() + " items for user: " + userId);
                callback.onSuccess(userItems);
            }

            @Override
            public void onError(String errorMsg) {
                callback.onError(errorMsg);
            }
        });
    }

    // ✅ FILTER: Filter by category
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

    // ✅ SEARCH: Improved search items (search in name, category, and description)
    public List<ShoppingItem> searchItems(List<ShoppingItem> items, String query) {
        if (query == null || query.trim().isEmpty()) {
            return items;
        }

        String searchQuery = query.toLowerCase().trim();
        List<ShoppingItem> results = new ArrayList<>();

        Log.d(TAG, "Searching for: '" + searchQuery + "' in " + items.size() + " items");

        for (ShoppingItem item : items) {
            boolean matches = false;

            // Search in name
            if (item.getName() != null && item.getName().toLowerCase().contains(searchQuery)) {
                matches = true;
                Log.d(TAG, "Found in name: " + item.getName());
            }
            // Search in description
            else if (item.getDescription() != null &&
                    item.getDescription().toLowerCase().contains(searchQuery)) {
                matches = true;
                Log.d(TAG, "Found in description: " + item.getName());
            }
            // Search in category
            else if (item.getCategory() != null &&
                    item.getCategory().toLowerCase().contains(searchQuery)) {
                matches = true;
                Log.d(TAG, "Found in category: " + item.getName());
            }

            if (matches) {
                results.add(item);
            }
        }

        Log.d(TAG, "Search found " + results.size() + " results");
        return results;
    }

    // ✅ GET LIMITED: Get limited number of items for home screen
    public void fetchLimitedShoppingItems(int limit, ShoppingItemCallback<List<ShoppingItem>> callback) {
        Log.d(TAG, "📥 Fetching " + limit + " shopping items for home screen...");

        getAllShoppingItems(new ShoppingItemCallback<Map<String, ShoppingItem>>() {
            @Override
            public void onSuccess(Map<String, ShoppingItem> result) {
                List<ShoppingItem> allItems = new ArrayList<>(result.values());

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

                Log.d(TAG, "✅ Successfully fetched " + limitedItems.size() + " items for home screen");
                callback.onSuccess(limitedItems);
            }

            @Override
            public void onError(String errorMsg) {
                callback.onError(errorMsg);
            }
        });
    }

    public interface ShoppingItemCallback<T> {
        void onSuccess(T result);
        void onError(String errorMsg);
    }
}