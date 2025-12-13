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

    public void loadInitialPosts() {
        repository.loadInitialPosts();
    }

    public void loadMorePosts() {
        repository.loadMorePosts();
    }

    public boolean isLastPage() {
        return repository.isLastPage();
    }
}
