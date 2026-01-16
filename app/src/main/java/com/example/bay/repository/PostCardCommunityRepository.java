package com.example.bay.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bay.model.PostCardItem;
import com.example.bay.service.PostCardCommunityService;
import com.example.bay.util.RetrofitClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostCardCommunityRepository {

    private static final String TAG = "PostCommunityRepo";
    private static final int PAGE_SIZE = 10;

    private final PostCardCommunityService service;
    private final MutableLiveData<List<PostCardItem>> pagedPostsLiveData = new MutableLiveData<>();

    private final List<PostCardItem> allPosts = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasLoadedOnce = false;

    public PostCardCommunityRepository() {
        service = RetrofitClient.getClient().create(PostCardCommunityService.class);
    }

    public LiveData<List<PostCardItem>> getPagedPostsLiveData() {
        return pagedPostsLiveData;
    }

    public void loadInitialPosts() {
        if (hasLoadedOnce && !allPosts.isEmpty()) {
            currentPage = 1;
            publishCurrentPage();
            return;
        }
        fetchFromNetwork();
    }

    public void loadMorePosts() {
        if (isLoading) return;
        if (allPosts.isEmpty()) return;

        int maxPages = (int) Math.ceil(allPosts.size() / (double) PAGE_SIZE);
        if (currentPage >= maxPages) return;

        currentPage++;
        publishCurrentPage();
    }

    public boolean isLastPage() {
        if (allPosts.isEmpty()) return true;
        int maxPages = (int) Math.ceil(allPosts.size() / (double) PAGE_SIZE);
        return currentPage >= maxPages;
    }

    private void fetchFromNetwork() {
        isLoading = true;
        Log.d(TAG, "Fetching community posts from network");

        service.getAllPostCardItems().enqueue(new Callback<Map<String, PostCardItem>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, PostCardItem>> call,
                                   @NonNull Response<Map<String, PostCardItem>> response) {

                isLoading = false;

                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Response not successful or body is null");
                    pagedPostsLiveData.postValue(new ArrayList<>());
                    return;
                }

                Map<String, PostCardItem> body = response.body();
                Log.d(TAG, "Response map size: " + body.size());

                allPosts.clear();

                for (Map.Entry<String, PostCardItem> entry : body.entrySet()) {
                    String key = entry.getKey();
                    PostCardItem post = entry.getValue();

                    if (post == null) continue;

                    if (post.getItemId() == null || post.getItemId().isEmpty()) {
                        post.setItemId(key);
                    }

                    allPosts.add(post);
                }

                // Sort by timestamp descending (ISO 8601 string works lexicographically)
                Collections.sort(allPosts, new Comparator<PostCardItem>() {
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

                hasLoadedOnce = true;
                currentPage = 1;
                publishCurrentPage();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, PostCardItem>> call, @NonNull Throwable t) {
                isLoading = false;
                Log.e(TAG, "Network failure: " + t.getMessage(), t);
                pagedPostsLiveData.postValue(new ArrayList<>());
            }
        });
    }

    private void publishCurrentPage() {
        if (allPosts.isEmpty()) {
            pagedPostsLiveData.postValue(new ArrayList<>());
            return;
        }

        int fromIndex = 0;
        int toIndex = Math.min(currentPage * PAGE_SIZE, allPosts.size());

        if (fromIndex >= toIndex) {
            pagedPostsLiveData.postValue(new ArrayList<>());
            return;
        }

        List<PostCardItem> subList = new ArrayList<>(allPosts.subList(fromIndex, toIndex));
        pagedPostsLiveData.postValue(subList);

        Log.d(TAG, "Publishing page " + currentPage + " (" + fromIndex + " - " + (toIndex - 1) + ")");
    }
}
