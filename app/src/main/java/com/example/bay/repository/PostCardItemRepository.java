package com.example.bay.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bay.model.PostCardItem;
import com.example.bay.service.PostCardItemService;
import com.example.bay.util.RetrofitClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostCardItemRepository {

    public interface OnLatestPostsLoadedListener {
        void onSuccess(List<PostCardItem> posts);
        void onError(Throwable t);
    }

    private final PostCardItemService service;
    private final MutableLiveData<List<PostCardItem>> postCardItemsLiveData = new MutableLiveData<>();

    public PostCardItemRepository() {
        service = RetrofitClient.getClient().create(PostCardItemService.class);
        Log.d("PostRepository", "Repository created");
    }

    public LiveData<List<PostCardItem>> getAllPostCardItems() {
        Log.d("PostRepository", "Getting all post card items");

        service.getAllPostCardItems().enqueue(new Callback<Map<String, PostCardItem>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, PostCardItem>> call, @NonNull Response<Map<String, PostCardItem>> response) {
                Log.d("PostRepository", "Response code: " + response.code());
                Log.d("PostRepository", "Response message: " + response.message());
                Log.d("PostRepository", "Response isSuccessful: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("PostRepository", "Response body size: " + response.body().size());

                    List<PostCardItem> posts = new ArrayList<>();
                    for (Map.Entry<String, PostCardItem> entry : response.body().entrySet()) {
                        PostCardItem post = entry.getValue();
                        if (post == null) continue;

                        if (post.getItemId() == null) {
                            post.setItemId(entry.getKey());
                        }

                        posts.add(post);
                        Log.d("PostRepository", "Added post with key: " + entry.getKey() + ", itemId: " + post.getItemId());
                    }

                    postCardItemsLiveData.postValue(posts);
                    Log.d("PostRepository", "Posted " + posts.size() + " posts to LiveData");
                } else {
                    Log.e("PostRepository", "Response not successful or body is null");
                    Log.e("PostRepository", "Error body: " + response.errorBody());
                    postCardItemsLiveData.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, PostCardItem>> call, @NonNull Throwable t) {
                Log.e("PostRepository", "Network failure: " + t.getMessage());
                t.printStackTrace();
                postCardItemsLiveData.postValue(new ArrayList<>());
            }
        });

        return postCardItemsLiveData;
    }

    public void fetchLatestTwoPosts(OnLatestPostsLoadedListener listener) {
        Log.d("PostRepository", "Fetching latest two post card items");
        Call<Map<String, PostCardItem>> call = service.getLatestPostCardItems("\"timestamp\"", 2);

        call.enqueue(new Callback<Map<String, PostCardItem>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, PostCardItem>> call, @NonNull Response<Map<String, PostCardItem>> response) {
                Log.d("PostRepository", "Latest posts response code: " + response.code());
                Log.d("PostRepository", "Latest posts response message: " + response.message());
                Log.d("PostRepository", "Latest posts isSuccessful: " + response.isSuccessful());

                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("PostRepository", "Latest posts response not successful or body is null");
                    if (listener != null) listener.onSuccess(new ArrayList<>());
                    return;
                }

                Map<String, PostCardItem> body = response.body();
                Log.d("PostRepository", "Latest posts body size: " + body.size());

                List<PostCardItem> posts = new ArrayList<>();
                for (Map.Entry<String, PostCardItem> entry : body.entrySet()) {
                    PostCardItem post = entry.getValue();
                    if (post == null) continue;

                    if (post.getItemId() == null || post.getItemId().isEmpty()) {
                        post.setItemId(entry.getKey());
                    }


                    posts.add(post);
                    Log.d("PostRepository", "Latest post added with key: " + entry.getKey() + ", itemId: " + post.getItemId());
                }

                Collections.sort(posts, new Comparator<PostCardItem>() {
                    @Override
                    public int compare(PostCardItem o1, PostCardItem o2) {
                        String t1 = o1.getTimestamp();
                        String t2 = o2.getTimestamp();

                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;

                        return t2.compareTo(t1);
                    }
                });

                if (posts.size() > 2) {
                    posts = posts.subList(0, 2);
                }

                if (listener != null) {
                    listener.onSuccess(posts);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, PostCardItem>> call, @NonNull Throwable t) {
                Log.e("PostRepository", "Latest posts network failure: " + t.getMessage());
                t.printStackTrace();
                if (listener != null) {
                    listener.onError(t);
                }
            }
        });
    }
}
