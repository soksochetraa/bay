package com.example.bay.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bay.model.PostCardItem;
import com.example.bay.service.PostCardItemService;
import com.example.bay.util.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostCardItemRepository {

    private final PostCardItemService service;
    private final MutableLiveData<List<PostCardItem>> postCardItemsLiveData = new MutableLiveData<>();

    public PostCardItemRepository() {
        service = RetrofitClient.getClient().create(PostCardItemService.class);
    }

    public LiveData<List<PostCardItem>> getAllPostCardItems() {
        service.getAllPostCardItems().enqueue(new Callback<Map<String, PostCardItem>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, PostCardItem>> call, @NonNull Response<Map<String, PostCardItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postCardItemsLiveData.postValue(new ArrayList<>(response.body().values()));
                } else {
                    postCardItemsLiveData.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, PostCardItem>> call, @NonNull Throwable t) {
                postCardItemsLiveData.postValue(new ArrayList<>());
            }
        });

        return postCardItemsLiveData;
    }
}
