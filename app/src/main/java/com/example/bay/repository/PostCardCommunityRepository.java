package com.example.bay.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bay.model.PostCardItem;
import com.example.bay.util.FirebaseDBHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostCardCommunityRepository {

    private static final String TAG = "PostCommunityRepo";
    private static final int PAGE_SIZE = 10;
    private static final String VISIBILITY_VISIBLE = "visible";

    private final DatabaseReference postsRef;
    private final MutableLiveData<List<PostCardItem>> pagedPostsLiveData = new MutableLiveData<>();

    private final List<PostCardItem> allPosts = new ArrayList<>();
    private int currentLimit = PAGE_SIZE;
    private ValueEventListener liveListener;
    private Query currentQuery;
    private boolean isLastPage = false;

    public PostCardCommunityRepository() {
        postsRef = FirebaseDBHelper.getDatabase().getReference("postCardItems");
    }

    public LiveData<List<PostCardItem>> getPagedPostsLiveData() {
        return pagedPostsLiveData;
    }

    public void startListening() {
        if (liveListener != null) return;

        liveListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allPosts.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    PostCardItem post = child.getValue(PostCardItem.class);
                    if (post == null) continue;

                    if (post.getItemId() == null || post.getItemId().isEmpty()) {
                        post.setItemId(child.getKey());
                    }

                    // Filter: Only show posts with visibility = "visible"
                    String visibility = post.getVisibility();
                    if (visibility == null || VISIBILITY_VISIBLE.equals(visibility)) {
                        allPosts.add(post);
                    } else {
                        Log.d(TAG, "Filtered out hidden post: " + post.getItemId() + " (visibility: " + visibility + ")");
                    }
                }

                // Sort by timestamp (newest first)
                Collections.sort(allPosts, (o1, o2) -> {
                    String t1 = o1.getTimestamp();
                    String t2 = o2.getTimestamp();
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return t2.compareTo(t1);
                });

                // Check if we've reached the last page
                long totalReceived = snapshot.getChildrenCount();
                isLastPage = totalReceived < currentLimit;

                pagedPostsLiveData.postValue(new ArrayList<>(allPosts));

                Log.d(TAG, "Live update: " + allPosts.size() + " visible posts out of " + totalReceived + " total");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Live listener cancelled: " + error.getMessage());
                pagedPostsLiveData.postValue(new ArrayList<>());
            }
        };

        attachListener();
    }

    private void attachListener() {
        if (currentQuery != null && liveListener != null) {
            currentQuery.removeEventListener(liveListener);
        }
        currentQuery = postsRef.orderByKey().limitToLast(currentLimit);
        currentQuery.addValueEventListener(liveListener);
    }

    public void stopListening() {
        if (currentQuery != null && liveListener != null) {
            currentQuery.removeEventListener(liveListener);
            liveListener = null;
            currentQuery = null;
        }
    }

    public void loadMorePosts() {
        if (allPosts.isEmpty() || isLastPage) return;

        currentLimit += PAGE_SIZE;
        attachListener();
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    // Method to hide a post
    public void hidePost(String postId, OnVisibilityChangeListener listener) {
        DatabaseReference postRef = postsRef.child(postId).child("visibility");
        postRef.setValue("hidden")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Post hidden: " + postId);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to hide post: " + e.getMessage());
                    if (listener != null) listener.onFailure(e);
                });
    }

    // Method to show a post
    public void showPost(String postId, OnVisibilityChangeListener listener) {
        DatabaseReference postRef = postsRef.child(postId).child("visibility");
        postRef.setValue("visible")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Post shown: " + postId);
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to show post: " + e.getMessage());
                    if (listener != null) listener.onFailure(e);
                });
    }

    public interface OnVisibilityChangeListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}