package com.example.bay.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.bay.model.PostCardItem;
import com.example.bay.repository.PostCardCommunityRepository;

import java.util.List;

public class CommunityViewModel extends ViewModel {

    private final PostCardCommunityRepository repository;
    private final LiveData<List<PostCardItem>> pagedPostsLiveData;

    public CommunityViewModel() {
        repository = new PostCardCommunityRepository();
        pagedPostsLiveData = repository.getPagedPostsLiveData();
    }

    public LiveData<List<PostCardItem>> getPagedPostsLiveData() {
        return pagedPostsLiveData;
    }

    public void startListening() {
        repository.startListening();
    }

    public void stopListening() {
        repository.stopListening();
    }

    public void loadMorePosts() {
        repository.loadMorePosts();
    }

    public boolean isLastPage() {
        return repository.isLastPage();
    }

    // Methods to hide/show posts
    public void hidePost(String postId, PostCardCommunityRepository.OnVisibilityChangeListener listener) {
        repository.hidePost(postId, listener);
    }

    public void showPost(String postId, PostCardCommunityRepository.OnVisibilityChangeListener listener) {
        repository.showPost(postId, listener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.stopListening();
    }
}