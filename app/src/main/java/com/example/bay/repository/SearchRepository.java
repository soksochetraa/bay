package com.example.bay.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.bay.model.PostCardItem;
import com.example.bay.model.SearchResult;
import com.example.bay.model.User;
import com.example.bay.service.PostCardCommunityService;
import com.example.bay.service.UserService;
import com.example.bay.util.RetrofitClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchRepository {

    private static final String TAG = "SearchRepository";
    private static final int MAX_USERS = 5;
    private static final int MAX_POSTS = 20;

    private final UserService userService;
    private final PostCardCommunityService postService;

    // Separate LiveData for users and posts
    private final MutableLiveData<List<User>> userSearchResultsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<PostCardItem>> postSearchResultsLiveData = new MutableLiveData<>(new ArrayList<>());

    // Original combined results (if still needed)
    private final MutableLiveData<List<SearchResult>> searchResultsLiveData = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public SearchRepository() {
        userService = RetrofitClient.getClient().create(UserService.class);
        postService = RetrofitClient.getClient().create(PostCardCommunityService.class);
    }

    // Separate search methods
    public void searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            userSearchResultsLiveData.postValue(new ArrayList<>());
            return;
        }

        String searchQuery = query.trim().toLowerCase(Locale.getDefault());
        isLoadingLiveData.postValue(true);

        userService.getAllUsers().enqueue(new Callback<Map<String, User>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, User>> call,
                                   @NonNull Response<Map<String, User>> response) {

                isLoadingLiveData.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<User> matchedUsers = new ArrayList<>();
                    Map<String, User> users = response.body();

                    for (Map.Entry<String, User> entry : users.entrySet()) {
                        User user = entry.getValue();
                        if (user != null && matchesUser(user, searchQuery)) {
                            matchedUsers.add(user);
                        }
                    }

                    // Sort users by relevance
                    Collections.sort(matchedUsers, (u1, u2) -> {
                        // Verified users first
                        if (u1.isUserVerified() && !u2.isUserVerified()) return -1;
                        if (!u1.isUserVerified() && u2.isUserVerified()) return 1;

                        // Then by name match relevance
                        String name1 = (u1.getFirst_name() + " " + u1.getLast_name()).toLowerCase();
                        String name2 = (u2.getFirst_name() + " " + u2.getLast_name()).toLowerCase();

                        boolean exactMatch1 = name1.startsWith(searchQuery) || u1.getFirst_name().toLowerCase().startsWith(searchQuery);
                        boolean exactMatch2 = name2.startsWith(searchQuery) || u2.getFirst_name().toLowerCase().startsWith(searchQuery);

                        if (exactMatch1 && !exactMatch2) return -1;
                        if (!exactMatch1 && exactMatch2) return 1;

                        return name1.compareTo(name2);
                    });

                    // Limit to MAX_USERS
                    if (matchedUsers.size() > MAX_USERS) {
                        matchedUsers = matchedUsers.subList(0, MAX_USERS);
                    }

                    userSearchResultsLiveData.postValue(matchedUsers);
                    errorLiveData.postValue(null);

                } else {
                    userSearchResultsLiveData.postValue(new ArrayList<>());
                    errorLiveData.postValue("Failed to load users");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, User>> call, @NonNull Throwable t) {
                isLoadingLiveData.postValue(false);
                userSearchResultsLiveData.postValue(new ArrayList<>());
                errorLiveData.postValue("Network error: " + t.getMessage());
                Log.e(TAG, "User search failed: " + t.getMessage());
            }
        });
    }

    public void searchPosts(String query) {
        if (query == null || query.trim().isEmpty()) {
            postSearchResultsLiveData.postValue(new ArrayList<>());
            return;
        }

        String searchQuery = query.trim().toLowerCase(Locale.getDefault());
        isLoadingLiveData.postValue(true);

        postService.getAllPostCardItems().enqueue(new Callback<Map<String, PostCardItem>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, PostCardItem>> call,
                                   @NonNull Response<Map<String, PostCardItem>> response) {

                isLoadingLiveData.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<PostCardItem> matchedPosts = new ArrayList<>();
                    Map<String, PostCardItem> posts = response.body();

                    for (Map.Entry<String, PostCardItem> entry : posts.entrySet()) {
                        PostCardItem post = entry.getValue();
                        if (post != null && matchesPost(post, searchQuery)) {
                            matchedPosts.add(post);
                        }
                    }

                    // Sort posts by timestamp (newest first)
                    Collections.sort(matchedPosts, new Comparator<PostCardItem>() {
                        @Override
                        public int compare(PostCardItem p1, PostCardItem p2) {
                            if (p1.getTimestamp() == null && p2.getTimestamp() == null) return 0;
                            if (p1.getTimestamp() == null) return 1;
                            if (p2.getTimestamp() == null) return -1;
                            return p2.getTimestamp().compareTo(p1.getTimestamp());
                        }
                    });

                    // Limit to MAX_POSTS
                    if (matchedPosts.size() > MAX_POSTS) {
                        matchedPosts = matchedPosts.subList(0, MAX_POSTS);
                    }

                    postSearchResultsLiveData.postValue(matchedPosts);
                    errorLiveData.postValue(null);

                } else {
                    postSearchResultsLiveData.postValue(new ArrayList<>());
                    errorLiveData.postValue("Failed to load posts");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, PostCardItem>> call, @NonNull Throwable t) {
                isLoadingLiveData.postValue(false);
                postSearchResultsLiveData.postValue(new ArrayList<>());
                errorLiveData.postValue("Network error: " + t.getMessage());
                Log.e(TAG, "Post search failed: " + t.getMessage());
            }
        });
    }

    // Original search method that combines both (if still needed)
    public void search(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchResultsLiveData.postValue(new ArrayList<>());
            userSearchResultsLiveData.postValue(new ArrayList<>());
            postSearchResultsLiveData.postValue(new ArrayList<>());
            return;
        }

        String searchQuery = query.trim().toLowerCase(Locale.getDefault());
        isLoadingLiveData.postValue(true);

        // Search users first
        userService.getAllUsers().enqueue(new Callback<Map<String, User>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, User>> call,
                                   @NonNull Response<Map<String, User>> response) {

                List<SearchResult> results = new ArrayList<>();

                if (response.isSuccessful() && response.body() != null) {
                    List<User> matchedUsers = new ArrayList<>();
                    Map<String, User> users = response.body();

                    for (Map.Entry<String, User> entry : users.entrySet()) {
                        User user = entry.getValue();
                        if (user != null && matchesUser(user, searchQuery)) {
                            matchedUsers.add(user);
                        }
                    }

                    // Sort users
                    Collections.sort(matchedUsers, (u1, u2) -> {
                        if (u1.isUserVerified() && !u2.isUserVerified()) return -1;
                        if (!u1.isUserVerified() && u2.isUserVerified()) return 1;
                        String name1 = (u1.getFirst_name() + " " + u1.getLast_name()).toLowerCase();
                        String name2 = (u2.getFirst_name() + " " + u2.getLast_name()).toLowerCase();
                        return name1.compareTo(name2);
                    });

                    // Add users to results
                    int userCount = Math.min(matchedUsers.size(), MAX_USERS);
                    for (int i = 0; i < userCount; i++) {
                        results.add(new SearchResult(SearchResult.TYPE_USER, matchedUsers.get(i)));
                    }

                    // Post search will continue with combined results
                    searchPostsCombined(searchQuery, results);
                } else {
                    searchPostsCombined(searchQuery, new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, User>> call, @NonNull Throwable t) {
                Log.e(TAG, "User search failed: " + t.getMessage());
                searchPostsCombined(searchQuery, new ArrayList<>());
            }
        });
    }

    private void searchPostsCombined(String query, List<SearchResult> existingResults) {
        postService.getAllPostCardItems().enqueue(new Callback<Map<String, PostCardItem>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, PostCardItem>> call,
                                   @NonNull Response<Map<String, PostCardItem>> response) {

                isLoadingLiveData.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<SearchResult> finalResults = new ArrayList<>(existingResults);
                    Map<String, PostCardItem> posts = response.body();

                    for (Map.Entry<String, PostCardItem> entry : posts.entrySet()) {
                        PostCardItem post = entry.getValue();
                        if (post != null && matchesPost(post, query)) {
                            finalResults.add(new SearchResult(SearchResult.TYPE_POST, post));
                        }
                    }

                    // Sort posts by timestamp (newest first) within the combined results
                    sortPosts(finalResults);
                    searchResultsLiveData.postValue(finalResults);
                    errorLiveData.postValue(null);

                } else {
                    searchResultsLiveData.postValue(existingResults);
                    errorLiveData.postValue("Failed to load posts");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, PostCardItem>> call, @NonNull Throwable t) {
                isLoadingLiveData.postValue(false);
                searchResultsLiveData.postValue(existingResults);
                errorLiveData.postValue("Network error: " + t.getMessage());
                Log.e(TAG, "Post search failed: " + t.getMessage());
            }
        });
    }

    private boolean matchesUser(User user, String query) {
        if (user.getFirst_name() != null && user.getFirst_name().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (user.getLast_name() != null && user.getLast_name().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (user.getEmail() != null && user.getEmail().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (user.getBio() != null && user.getBio().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        return false;
    }

    private boolean matchesPost(PostCardItem post, String query) {
        if (post.getTitle() != null && post.getTitle().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (post.getContent() != null && post.getContent().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        return false;
    }

    private void sortPosts(List<SearchResult> results) {
        List<SearchResult> users = new ArrayList<>();
        List<SearchResult> posts = new ArrayList<>();

        for (SearchResult result : results) {
            if (result.getType() == SearchResult.TYPE_USER) {
                users.add(result);
            } else {
                posts.add(result);
            }
        }

        Collections.sort(posts, new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult r1, SearchResult r2) {
                PostCardItem p1 = r1.getPost();
                PostCardItem p2 = r2.getPost();

                if (p1.getTimestamp() == null && p2.getTimestamp() == null) return 0;
                if (p1.getTimestamp() == null) return 1;
                if (p2.getTimestamp() == null) return -1;

                return p2.getTimestamp().compareTo(p1.getTimestamp());
            }
        });

        results.clear();
        results.addAll(users);
        results.addAll(posts);
    }

    public void clearSearch() {
        searchResultsLiveData.postValue(new ArrayList<>());
        userSearchResultsLiveData.postValue(new ArrayList<>());
        postSearchResultsLiveData.postValue(new ArrayList<>());
    }

    // Getters for LiveData
    public MutableLiveData<List<User>> getUserSearchResultsLiveData() {
        return userSearchResultsLiveData;
    }

    public MutableLiveData<List<PostCardItem>> getPostSearchResultsLiveData() {
        return postSearchResultsLiveData;
    }

    public MutableLiveData<List<SearchResult>> getSearchResultsLiveData() {
        return searchResultsLiveData;
    }

    public MutableLiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public MutableLiveData<String> getErrorLiveData() {
        return errorLiveData;
    }
}