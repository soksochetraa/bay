package com.example.bay.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bay.model.ShoppingItem;
import com.example.bay.model.User;
import com.example.bay.repository.ShoppingItemRepository;
import com.example.bay.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ShoppingViewModel extends ViewModel {

    private ShoppingItemRepository repository;
    private UserRepository userRepository;

    private MutableLiveData<List<ShoppingItem>> allItems = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<List<ShoppingItem>> filteredItems = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<List<ShoppingItem>> userPosts = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<Map<String, User>> users = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>("");

    private String currentCategory = "ទាំងអស់";
    private String lastMarketplaceSearch = "";
    private String lastUserPostsSearch = "";

    public interface ShoppingItemCallback<T> {
        void onSuccess(T result);
        void onError(String errorMsg);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }

    public ShoppingViewModel() {
        repository = new ShoppingItemRepository();
        userRepository = new UserRepository();
        loadShoppingItems();
        loadUsers();
    }

    // ✅ DELETE: Delete shopping item
    public void deleteShoppingItem(String itemId, DeleteCallback callback) {
        isLoading.setValue(true);
        errorMessage.setValue("");

        repository.deleteShoppingItem(itemId, new ShoppingItemRepository.ShoppingItemCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isLoading.setValue(false);
                refreshAllData();
                callback.onSuccess();
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.setValue(false);
                errorMessage.setValue(errorMsg);
                callback.onError(errorMsg);
            }
        });
    }

    // ✅ UPDATE: Update shopping item
    public void updateShoppingItem(ShoppingItem item, ShoppingItemCallback<ShoppingItem> callback) {
        isLoading.setValue(true);
        errorMessage.setValue("");

        repository.updateShoppingItem(item, new ShoppingItemRepository.ShoppingItemCallback<ShoppingItem>() {
            @Override
            public void onSuccess(ShoppingItem result) {
                isLoading.setValue(false);
                refreshAllData();
                callback.onSuccess(result);
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.setValue(false);
                errorMessage.setValue(errorMsg);
                callback.onError(errorMsg);
            }
        });
    }

    // ✅ CREATE: Create shopping item
    public void createShoppingItem(ShoppingItem item, ShoppingItemCallback<ShoppingItem> callback) {
        isLoading.setValue(true);
        errorMessage.setValue("");

        repository.createShoppingItem(item, new ShoppingItemRepository.ShoppingItemCallback<ShoppingItem>() {
            @Override
            public void onSuccess(ShoppingItem result) {
                isLoading.setValue(false);
                refreshAllData();
                callback.onSuccess(result);
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.setValue(false);
                errorMessage.setValue(errorMsg);
                callback.onError(errorMsg);
            }
        });
    }

    // ✅ REFRESH: Refresh all data
    private void refreshAllData() {
        loadShoppingItems();
        loadUsers();

        String currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            loadUserPosts(currentUserId);
        }
    }

    // ✅ LOAD: Load shopping items
    public void loadShoppingItems() {
        isLoading.setValue(true);
        errorMessage.setValue("");

        repository.getAllShoppingItems(new ShoppingItemRepository.ShoppingItemCallback<Map<String, ShoppingItem>>() {
            @Override
            public void onSuccess(Map<String, ShoppingItem> result) {
                List<ShoppingItem> items = new ArrayList<>(result.values());

                // Sort by date (newest first)
                Collections.sort(items, (item1, item2) -> {
                    Long time1 = item1.getCreatedAt() != null ? item1.getCreatedAt() : 0L;
                    Long time2 = item2.getCreatedAt() != null ? item2.getCreatedAt() : 0L;
                    return Long.compare(time2, time1);
                });

                allItems.setValue(items);
                applyCurrentFilters();
                isLoading.setValue(false);
                Log.d("ShoppingViewModel", "Loaded " + items.size() + " shopping items");
            }

            @Override
            public void onError(String errorMsg) {
                errorMessage.setValue(errorMsg);
                isLoading.setValue(false);
                Log.e("ShoppingViewModel", "Error loading items: " + errorMsg);
            }
        });
    }

    // ✅ LOAD: Load users
    public void loadUsers() {
        userRepository.getAllUsers(new UserRepository.UserCallback<Map<String, User>>() {
            @Override
            public void onSuccess(Map<String, User> result) {
                users.setValue(result);
                Log.d("ShoppingViewModel", "Loaded " + result.size() + " users");
            }

            @Override
            public void onError(String errorMsg) {
                errorMessage.setValue("Failed to load users: " + errorMsg);
                Log.e("ShoppingViewModel", "Error loading users: " + errorMsg);
            }
        });
    }

    // ✅ LOAD: Load user posts
    public void loadUserPosts(String userId) {
        isLoading.setValue(true);
        errorMessage.setValue("");
        Log.d("ShoppingViewModel", "Loading posts for user: " + userId);

        repository.getUserItems(userId, new ShoppingItemRepository.ShoppingItemCallback<List<ShoppingItem>>() {
            @Override
            public void onSuccess(List<ShoppingItem> userItems) {
                Log.d("ShoppingViewModel", "Loaded " + userItems.size() + " user posts");

                // Apply search filter if there was a previous search
                if (lastUserPostsSearch != null && !lastUserPostsSearch.trim().isEmpty()) {
                    userItems = repository.searchItems(userItems, lastUserPostsSearch);
                    Log.d("ShoppingViewModel", "Applied search '" + lastUserPostsSearch + "' to user posts");
                }
                userPosts.setValue(userItems);
                isLoading.setValue(false);
            }

            @Override
            public void onError(String errorMsg) {
                errorMessage.setValue(errorMsg);
                isLoading.setValue(false);
                Log.e("ShoppingViewModel", "Error loading user posts: " + errorMsg);
            }
        });
    }

    // ✅ FILTER: Filter by category
    public void filterByCategory(String category) {
        currentCategory = category;
        Log.d("ShoppingViewModel", "Filtering by category: " + category);
        applyCurrentFilters();
    }

    // ✅ SEARCH: Search items (for Marketplace tab)
    public void searchItems(String query) {
        lastMarketplaceSearch = query;
        Log.d("ShoppingViewModel", "Marketplace search: '" + query + "'");
        applyCurrentFilters();
    }

    // ✅ SEARCH: Search user posts (for My Posts tab)
    public void searchUserPosts(String query) {
        lastUserPostsSearch = query;
        Log.d("ShoppingViewModel", "MyPosts search: '" + query + "'");

        if (query == null || query.trim().isEmpty()) {
            // If search is cleared, reload user posts
            String currentUserId = getCurrentUserId();
            if (currentUserId != null) {
                loadUserPosts(currentUserId);
            }
            return;
        }

        List<ShoppingItem> currentUserPosts = userPosts.getValue();
        if (currentUserPosts != null && !currentUserPosts.isEmpty()) {
            List<ShoppingItem> filtered = repository.searchItems(currentUserPosts, query);
            Log.d("ShoppingViewModel", "Filtered user posts from " + currentUserPosts.size() + " to " + filtered.size() + " items");
            userPosts.setValue(filtered);
        }
    }

    // ✅ HELPER: Apply current filters (category + search)
    private void applyCurrentFilters() {
        List<ShoppingItem> items = allItems.getValue();
        if (items == null) {
            Log.d("ShoppingViewModel", "No items to filter");
            return;
        }

        Log.d("ShoppingViewModel", "Applying filters - Category: " + currentCategory + ", Search: '" + lastMarketplaceSearch + "'");

        // First filter by category
        List<ShoppingItem> filtered = repository.filterByCategory(items, currentCategory);
        Log.d("ShoppingViewModel", "After category filter: " + filtered.size() + " items");

        // Then apply search if exists
        if (lastMarketplaceSearch != null && !lastMarketplaceSearch.trim().isEmpty()) {
            filtered = repository.searchItems(filtered, lastMarketplaceSearch);
            Log.d("ShoppingViewModel", "After search filter: " + filtered.size() + " items");
        }

        filteredItems.setValue(filtered);
    }

    // ✅ GET: Get current user ID
    private String getCurrentUserId() {
        try {
            return FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Getters
    public LiveData<List<ShoppingItem>> getFilteredItems() {
        return filteredItems;
    }

    public LiveData<List<ShoppingItem>> getUserPosts() {
        return userPosts;
    }

    public LiveData<Map<String, User>> getUsers() {
        return users;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}