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

    private static final String TAG = "ShoppingViewModel";

    private final ShoppingItemRepository repository;
    private final UserRepository userRepository;

    private final MutableLiveData<List<ShoppingItem>> allItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ShoppingItem>> filteredItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ShoppingItem>> userPosts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, User>> users = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>("");

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

    // ✅ CREATE
    public void createShoppingItem(ShoppingItem item, ShoppingItemCallback<ShoppingItem> callback) {
        isLoading.setValue(true);
        errorMessage.setValue("");

        repository.createShoppingItem(item, new ShoppingItemRepository.ShoppingItemCallback<ShoppingItem>() {
            @Override
            public void onSuccess(ShoppingItem result) {
                isLoading.setValue(false);
                loadShoppingItems();
                String uid = getCurrentUserId();
                if (uid != null) loadUserPosts(uid);
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.setValue(false);
                errorMessage.setValue(errorMsg);
                if (callback != null) callback.onError(errorMsg);
            }
        });
    }

    // ✅ UPDATE
    public void updateShoppingItem(ShoppingItem item, ShoppingItemCallback<ShoppingItem> callback) {
        isLoading.setValue(true);
        errorMessage.setValue("");

        repository.updateShoppingItem(item, new ShoppingItemRepository.ShoppingItemCallback<ShoppingItem>() {
            @Override
            public void onSuccess(ShoppingItem result) {
                isLoading.setValue(false);
                loadShoppingItems();
                String uid = getCurrentUserId();
                if (uid != null) loadUserPosts(uid);
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.setValue(false);
                errorMessage.setValue(errorMsg);
                if (callback != null) callback.onError(errorMsg);
            }
        });
    }

    public void loadShoppingItems() {
        isLoading.setValue(true);
        errorMessage.setValue("");

        repository.getAllShoppingItems(new ShoppingItemRepository.ShoppingItemCallback<Map<String, ShoppingItem>>() {
            @Override
            public void onSuccess(Map<String, ShoppingItem> result) {
                List<ShoppingItem> items = new ArrayList<>(result.values());

                // newest first
                Collections.sort(items, (a, b) -> {
                    long t1 = a.getCreatedAt() != null ? a.getCreatedAt() : 0L;
                    long t2 = b.getCreatedAt() != null ? b.getCreatedAt() : 0L;
                    return Long.compare(t2, t1);
                });

                // ✅ client fallback auto-delete expired warned items
                long now = System.currentTimeMillis();
                for (ShoppingItem it : items) {
                    if (it == null) continue;
                    if (it.shouldAutoDelete(now)) {
                        String key = it.getFirebaseKey();
                        if (key != null && !key.isEmpty()) {
                            Log.d(TAG, "Auto-delete expired warned item: " + key);
                            repository.deleteShoppingItemByFirebaseKey(key, new ShoppingItemRepository.ShoppingItemCallback<Void>() {
                                @Override public void onSuccess(Void r) {}
                                @Override public void onError(String err) { Log.e(TAG, "Auto-delete failed: " + err); }
                            });
                        }
                    }
                }

                allItems.setValue(items);
                applyCurrentFilters(); // ✅ IMPORTANT
                isLoading.setValue(false);
            }

            @Override
            public void onError(String errorMsg) {
                errorMessage.setValue(errorMsg);
                isLoading.setValue(false);
            }
        });
    }

    public void loadUsers() {
        userRepository.getAllUsers(new UserRepository.UserCallback<Map<String, User>>() {
            @Override public void onSuccess(Map<String, User> result) { users.setValue(result); }
            @Override public void onError(String errorMsg) { errorMessage.setValue(errorMsg); }
        });
    }

    // ✅ My posts show warned/hidden items (owner still sees)
    public void loadUserPosts(String userId) {
        isLoading.setValue(true);
        errorMessage.setValue("");

        repository.getUserItems(userId, new ShoppingItemRepository.ShoppingItemCallback<List<ShoppingItem>>() {
            @Override
            public void onSuccess(List<ShoppingItem> userItems) {
                if (lastUserPostsSearch != null && !lastUserPostsSearch.trim().isEmpty()) {
                    userItems = repository.searchItems(userItems, lastUserPostsSearch);
                }
                userPosts.setValue(userItems);
                isLoading.setValue(false);
            }

            @Override
            public void onError(String errorMsg) {
                errorMessage.setValue(errorMsg);
                isLoading.setValue(false);
            }
        });
    }

    public void filterByCategory(String category) {
        currentCategory = category;
        applyCurrentFilters();
    }

    public void searchItems(String query) {
        lastMarketplaceSearch = query;
        applyCurrentFilters();
    }

    public void searchUserPosts(String query) {
        lastUserPostsSearch = query;

        if (query == null || query.trim().isEmpty()) {
            String uid = getCurrentUserId();
            if (uid != null) loadUserPosts(uid);
            return;
        }

        List<ShoppingItem> current = userPosts.getValue();
        if (current != null) {
            userPosts.setValue(repository.searchItems(current, query));
        }
    }

    // ✅ Marketplace filter: hide warned/hidden
    private void applyCurrentFilters() {
        List<ShoppingItem> items = allItems.getValue();
        if (items == null) return;

        List<ShoppingItem> marketplaceVisible = new ArrayList<>();
        for (ShoppingItem it : items) {
            if (it == null) continue;
            if (it.isDeleted()) continue;
            if (it.isHiddenOnMarketplace()) continue; // ✅ KEY FIX
            marketplaceVisible.add(it);
        }

        List<ShoppingItem> filtered = repository.filterByCategory(marketplaceVisible, currentCategory);

        if (lastMarketplaceSearch != null && !lastMarketplaceSearch.trim().isEmpty()) {
            filtered = repository.searchItems(filtered, lastMarketplaceSearch);
        }

        filteredItems.setValue(filtered);
    }

    public void deleteShoppingItem(String itemId, DeleteCallback callback) {
        isLoading.setValue(true);
        errorMessage.setValue("");

        repository.deleteShoppingItem(itemId, new ShoppingItemRepository.ShoppingItemCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isLoading.setValue(false);
                loadShoppingItems();
                String uid = getCurrentUserId();
                if (uid != null) loadUserPosts(uid);
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.setValue(false);
                errorMessage.setValue(errorMsg);
                if (callback != null) callback.onError(errorMsg);
            }
        });
    }

    private String getCurrentUserId() {
        try {
            return FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    public LiveData<List<ShoppingItem>> getFilteredItems() { return filteredItems; }
    public LiveData<List<ShoppingItem>> getUserPosts() { return userPosts; }
    public LiveData<Map<String, User>> getUsers() { return users; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
}