package com.example.bay.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bay.model.ShoppingItem;
import com.example.bay.model.PostCardItem;
import com.example.bay.model.User;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> temperatureLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> weatherIconLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ShoppingItem>> shoppingItemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<PostCardItem>> postCardItemsLiveData = new MutableLiveData<>();

    public void setUser(User user) {
        userLiveData.setValue(user);
    }

    public LiveData<User> getUser() {
        return userLiveData;
    }

    public void setWeatherData(double temp, String icon) {
        temperatureLiveData.postValue(temp);
        weatherIconLiveData.postValue(icon);
    }

    public LiveData<Double> getTemperature() {
        return temperatureLiveData;
    }

    public LiveData<String> getWeatherIcon() {
        return weatherIconLiveData;
    }

    public void setShoppingItems(List<ShoppingItem> items) {
        shoppingItemsLiveData.postValue(items);
    }

    public LiveData<List<ShoppingItem>> getShoppingItems() {
        return shoppingItemsLiveData;
    }

    public void setPostCardItems(List<PostCardItem> posts) {
        postCardItemsLiveData.postValue(posts);
    }

    public LiveData<List<PostCardItem>> getPostCardItems() {
        return postCardItemsLiveData;
    }
}
