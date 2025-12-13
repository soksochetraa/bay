package com.example.bay.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bay.HomeActivity;
import com.example.bay.adapter.PostCardCommunityAdapter;
import com.example.bay.databinding.FragmentCommunityBinding;
import com.example.bay.model.PostCardItem;
import com.example.bay.viewmodel.CommunityViewModel;

import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends Fragment {

    private FragmentCommunityBinding binding;

    private PostCardCommunityAdapter adapter;
    private CommunityViewModel viewModel;

    private final List<PostCardItem> currentFullList = new ArrayList<>();

    private boolean isLoadingMore = false;
    private boolean isLastPage = false;
    private HomeActivity homeActivity;

    public CommunityFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeActivity = (HomeActivity) getActivity();
        if (homeActivity != null) homeActivity.showBottomNavigation();

        binding.buttonAdd.setOnClickListener(v -> {
            if (homeActivity != null) {
                Fragment fragment = new CreatePostCardFragment();
                homeActivity.LoadFragment(fragment);
                homeActivity.hideBottomNavigation();
            }
        });

        adapter = new PostCardCommunityAdapter(requireContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.postCardContainer.setLayoutManager(layoutManager);
        binding.postCardContainer.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(CommunityViewModel.class);

        viewModel.getPagedPostsLiveData().observe(getViewLifecycleOwner(), posts -> {
            currentFullList.clear();
            if (posts != null) {
                currentFullList.addAll(posts);
            }
            applySearchFilter(binding.editTextSearch.getText().toString());
            isLoadingMore = false;
            isLastPage = viewModel.isLastPage();
        });

        viewModel.loadInitialPosts();

        binding.postCardContainer.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoadingMore && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                            && firstVisibleItemPosition >= 0) {
                        isLoadingMore = true;
                        viewModel.loadMorePosts();
                    }
                }
            }
        });

        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applySearchFilter(s.toString());
            }
        });
    }

    private void applySearchFilter(String query) {
        if (currentFullList.isEmpty()) {
            adapter.setPostCardItemList(new ArrayList<>());
            return;
        }

        if (query == null || query.trim().isEmpty()) {
            adapter.setPostCardItemList(new ArrayList<>(currentFullList));
            return;
        }

        String lower = query.toLowerCase().trim();
        List<PostCardItem> filtered = new ArrayList<>();

        for (PostCardItem item : currentFullList) {
            String content = item.getContent() != null ? item.getContent().toLowerCase() : "";
            String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";

            if (content.contains(lower) || title.contains(lower)) {
                filtered.add(item);
            }
        }

        adapter.setPostCardItemList(filtered);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
