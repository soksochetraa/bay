package com.example.bay;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bay.adapter.SearchResultAdapter;
import com.example.bay.adapter.PostAdapter;
import com.example.bay.databinding.FragmentCommunitySearchBinding;
import com.example.bay.model.PostCardItem;
import com.example.bay.model.User;
import com.example.bay.repository.SearchRepository;

import java.util.List;

public class CommunitySearchFragment extends Fragment {

    private FragmentCommunitySearchBinding binding;
    private HomeActivity homeActivity;
    private SearchRepository searchRepository;
    private SearchResultAdapter userAdapter;
    private PostAdapter postAdapter;

    private static final String KEY_SEARCH_QUERY = "search_query";
    private String currentSearchQuery = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
        }

        // Initialize adapters in onCreate (before onViewCreated)
        userAdapter = new SearchResultAdapter();
        postAdapter = new PostAdapter(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCommunitySearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeActivity = (HomeActivity) getActivity();
        if (homeActivity != null) homeActivity.hideBottomNavigation();

        searchRepository = new SearchRepository();

        // Set current user ID from HomeActivity
        if (homeActivity != null) {
            String currentUserId = homeActivity.getCurrentUserId();
            if (currentUserId != null && !currentUserId.isEmpty()) {
                userAdapter.setCurrentUserId(currentUserId);
                postAdapter.setCurrentUserId(currentUserId);
            }
        }

        binding.button.setOnClickListener(v->{
            if (homeActivity != null) {
                homeActivity.onBackPressed();
                homeActivity.showBottomNavigation();
            }
        });

        LinearLayoutManager userLayoutManager = new LinearLayoutManager(getContext());
        binding.recyclerSearchResult.setLayoutManager(userLayoutManager);
        binding.recyclerSearchResult.setAdapter(userAdapter);
        binding.recyclerSearchResult.setNestedScrollingEnabled(false);

        LinearLayoutManager postLayoutManager = new LinearLayoutManager(getContext());
        binding.recyclerPostResults.setLayoutManager(postLayoutManager);
        binding.recyclerPostResults.setAdapter(postAdapter);
        binding.recyclerPostResults.setNestedScrollingEnabled(false);

        userAdapter.setOnItemClickListener(new SearchResultAdapter.OnItemClickListener() {
            @Override
            public void onUserClick(User user) {
                saveCurrentState();
                if (homeActivity != null && user != null && user.getUserId() != null) {
                    homeActivity.loadUserProfile(user.getUserId());
                }
            }

            @Override
            public void onMessageClick(User user) {
                saveCurrentState();
                if (homeActivity != null && user != null && user.getUserId() != null) {
                    String userName = user.getFirst_name() + " " + user.getLast_name();
                }
            }
        });

        postAdapter.setOnItemClickListener(new PostAdapter.OnItemClickListener() {
            @Override
            public void onPostClick(PostCardItem post) {
                saveCurrentState();
                if (homeActivity != null && post != null && post.getItemId() != null) {
                    homeActivity.loadPostDetail(post.getItemId());
                }
            }

            @Override
            public void onLikeClick(PostCardItem post) {}

            @Override
            public void onCommentClick(PostCardItem post) {
                saveCurrentState();
                if (homeActivity != null && post != null && post.getItemId() != null) {
                    homeActivity.loadPostDetail(post.getItemId());
                }
            }

            @Override
            public void onSaveClick(PostCardItem post) {}

            @Override
            public void onUserClick(String userId) {
                saveCurrentState();
                if (homeActivity != null && userId != null) {
                    homeActivity.loadUserProfile(userId);
                }
            }
        });

        binding.tvSeeMoreUsers.setOnClickListener(v -> {
            saveCurrentState();
            if (homeActivity != null) {
                String query = binding.editTextSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    homeActivity.loadFullUserSearch(query);
                }
            }
        });

        searchRepository.getUserSearchResultsLiveData().observe(getViewLifecycleOwner(), new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> users) {
                if (users == null || users.isEmpty()) {
                    binding.tvUserResults.setVisibility(View.GONE);
                    binding.recyclerSearchResult.setVisibility(View.GONE);
                    binding.tvSeeMoreUsers.setVisibility(View.GONE);
                } else {
                    binding.tvUserResults.setVisibility(View.VISIBLE);
                    binding.recyclerSearchResult.setVisibility(View.VISIBLE);

                    if (users.size() > 3) {
                        binding.tvSeeMoreUsers.setVisibility(View.VISIBLE);
                        userAdapter.setUsers(users.subList(0, Math.min(3, users.size())));
                    } else {
                        binding.tvSeeMoreUsers.setVisibility(View.GONE);
                        userAdapter.setUsers(users);
                    }
                }
            }
        });

        searchRepository.getPostSearchResultsLiveData().observe(getViewLifecycleOwner(), new Observer<List<PostCardItem>>() {
            @Override
            public void onChanged(List<PostCardItem> posts) {
                if (posts == null || posts.isEmpty()) {
                    binding.tvPostResults.setVisibility(View.GONE);
                    binding.recyclerPostResults.setVisibility(View.GONE);
                } else {
                    binding.tvPostResults.setVisibility(View.VISIBLE);
                    binding.recyclerPostResults.setVisibility(View.VISIBLE);
                    postAdapter.setPosts(posts);
                }
            }
        });

        searchRepository.getIsLoadingLiveData().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoading) {
                if (isLoading != null && isLoading) {
                    if (homeActivity != null) {
                        homeActivity.showLoading();
                    }
                } else {
                    if (homeActivity != null) {
                        homeActivity.hideLoading();
                    }
                }
            }
        });

        searchRepository.getErrorLiveData().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null && !error.isEmpty()) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
                if (homeActivity != null) {
                    homeActivity.hideLoading();
                }
            }
        });

        if (getArguments() != null) {
            String initialSearch = getArguments().getString("initial_search", "");
            if (!initialSearch.isEmpty()) {
                currentSearchQuery = initialSearch;
                binding.editTextSearch.setText(initialSearch);
                performSearch(initialSearch);
            }
        } else if (!currentSearchQuery.isEmpty()) {
            binding.editTextSearch.setText(currentSearchQuery);
            performSearch(currentSearchQuery);
        }

        binding.editTextSearch.postDelayed(() -> {
            if (!currentSearchQuery.isEmpty()) {
                binding.editTextSearch.requestFocus();
                binding.editTextSearch.setSelection(currentSearchQuery.length());
            } else {
                binding.editTextSearch.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(binding.editTextSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);

        binding.editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.editTextSearch.getText().toString().trim();
                currentSearchQuery = query;
                if (!query.isEmpty() && homeActivity != null) {
                    homeActivity.showLoading();
                }
                performSearch(query);
                hideKeyboardAndClearFocus();
                return true;
            }
            return false;
        });

        requireView().setFocusableInTouchMode(true);
        requireView().requestFocus();
        requireView().setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                binding.editTextSearch.clearFocus();
                hideKeyboardAndClearFocus();

                if (homeActivity != null) {
                    homeActivity.onBackPressed();
                    homeActivity.showBottomNavigation();
                }
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        String searchQuery = query.trim();
        currentSearchQuery = searchQuery;

        if (searchQuery.isEmpty()) {
            searchRepository.clearSearch();
            binding.tvUserResults.setVisibility(View.GONE);
            binding.recyclerSearchResult.setVisibility(View.GONE);
            binding.tvSeeMoreUsers.setVisibility(View.GONE);
            binding.tvPostResults.setVisibility(View.GONE);
            binding.recyclerPostResults.setVisibility(View.GONE);
            if (homeActivity != null) {
                homeActivity.hideLoading();
            }
        } else {
            searchRepository.searchUsers(searchQuery);
            searchRepository.searchPosts(searchQuery);
        }
    }

    private void hideKeyboardAndClearFocus() {
        binding.editTextSearch.clearFocus();
        InputMethodManager imm = (InputMethodManager) requireActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.editTextSearch.getWindowToken(), 0);
    }

    private void saveCurrentState() {
        currentSearchQuery = binding.editTextSearch.getText().toString().trim();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SEARCH_QUERY, currentSearchQuery);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!currentSearchQuery.isEmpty()) {
            binding.editTextSearch.setText(currentSearchQuery);
            binding.editTextSearch.postDelayed(() -> {
                binding.editTextSearch.setSelection(currentSearchQuery.length());
            }, 100);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (homeActivity != null) {
            homeActivity.hideLoading();
        }
    }
}