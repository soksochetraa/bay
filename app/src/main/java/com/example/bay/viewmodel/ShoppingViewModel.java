package com.example.bay.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.bay.model.ShoppingItem;
import com.example.bay.model.User;
import com.example.bay.repository.ShoppingItemRepository;
import com.example.bay.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShoppingViewModel extends ViewModel {
    private final ShoppingItemRepository shoppingItemRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<List<ShoppingItem>> shoppingItems;
    private final MutableLiveData<List<ShoppingItem>> filteredItems;
    private final MutableLiveData<Map<String, User>> users;
    private final MutableLiveData<String> errorMessage;
    private final MutableLiveData<Boolean> isLoading;

    private String currentCategory = "ទាំងអស់";
    private String currentSearchQuery = "";

    public ShoppingViewModel() {
        shoppingItemRepository = new ShoppingItemRepository();
        userRepository = new UserRepository();
        shoppingItems = new MutableLiveData<>(new ArrayList<>());
        filteredItems = new MutableLiveData<>(new ArrayList<>());
        users = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);

        loadShoppingItems();
        loadUsers();
    }

    private void loadShoppingItems() {
        isLoading.setValue(true);
        shoppingItemRepository.getAllShoppingItems(new ShoppingItemRepository.ShoppingItemCallback<Map<String, ShoppingItem>>() {
            @Override
            public void onSuccess(Map<String, ShoppingItem> result) {
                List<ShoppingItem> itemList = new ArrayList<>(result.values());
                shoppingItems.setValue(itemList);
                applyFilters();
                isLoading.setValue(false);
            }

            @Override
            public void onError(String errorMsg) {
                errorMessage.setValue(errorMsg);
                isLoading.setValue(false);
            }
        });
    }

    private void loadUsers() {
        userRepository.getAllUsers(new UserRepository.UserCallback<Map<String, User>>() {
            @Override
            public void onSuccess(Map<String, User> result) {
                users.setValue(result);
            }

            @Override
            public void onError(String errorMsg) {
                errorMessage.setValue("Failed to load users: " + errorMsg);
            }
        });
    }

    public void filterByCategory(String category) {
        this.currentCategory = category;
        applyFilters();
    }

    public void searchItems(String query) {
        this.currentSearchQuery = query.toLowerCase().trim();
        applyFilters();
    }

    private void applyFilters() {
        List<ShoppingItem> allItems = shoppingItems.getValue();
        if (allItems == null) return;

        List<ShoppingItem> filtered = new ArrayList<>();

        for (ShoppingItem item : allItems) {
            boolean categoryMatch = currentCategory.equals("ទាំងអស់") ||
                    item.getCategory().equals(currentCategory);

            boolean searchMatch = currentSearchQuery.isEmpty() ||
                    (item.getName() != null &&
                            item.getName().toLowerCase().contains(currentSearchQuery)) ||
                    (item.getDescription() != null &&
                            item.getDescription().toLowerCase().contains(currentSearchQuery)) ||
                    (item.getCategory() != null &&
                            item.getCategory().toLowerCase().contains(currentSearchQuery));

            if (categoryMatch && searchMatch) {
                filtered.add(item);
            }
        }

        filteredItems.setValue(filtered);
    }

    public void createShoppingItem(ShoppingItem item, ShoppingItemRepository.ShoppingItemCallback<ShoppingItem> callback) {
        shoppingItemRepository.createShoppingItem(item, callback);
    }

    public LiveData<List<ShoppingItem>> getFilteredItems() { return filteredItems; }
    public LiveData<Map<String, User>> getUsers() { return users; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<List<ShoppingItem>> getShoppingItems() { return shoppingItems; }
}