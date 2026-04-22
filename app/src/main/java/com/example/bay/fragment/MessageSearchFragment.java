package com.example.bay.fragment;

import android.content.Context;
import android.os.Bundle;
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

import com.example.bay.HomeActivity;
import com.example.bay.adapter.SearchResultAdapter;
import com.example.bay.databinding.FragmentMessageSearchBinding;
import com.example.bay.model.Chat;
import com.example.bay.model.User;
import com.example.bay.repository.ChatRepository;
import com.example.bay.repository.SearchRepository;

import java.util.List;

public class MessageSearchFragment extends Fragment {

    private FragmentMessageSearchBinding binding;
    private HomeActivity homeActivity;
    private SearchRepository searchRepository;
    private ChatRepository chatRepository;
    private SearchResultAdapter userAdapter;

    private String currentSearchQuery = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userAdapter = new SearchResultAdapter();
        chatRepository = new ChatRepository();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMessageSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeActivity = (HomeActivity) getActivity();

        searchRepository = new SearchRepository();

        // Set current user ID
        if (homeActivity != null) {
            String currentUserId = homeActivity.getCurrentUserId();
            if (currentUserId != null && !currentUserId.isEmpty()) {
                userAdapter.setCurrentUserId(currentUserId);
            }
        }

        // Back button
        binding.btnBack.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.onBackPressed();
            }
        });

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.recyclerSearchResult.setLayoutManager(layoutManager);
        binding.recyclerSearchResult.setAdapter(userAdapter);
        binding.recyclerSearchResult.setNestedScrollingEnabled(false);

        // User click -> open direct chat
        userAdapter.setOnItemClickListener(new SearchResultAdapter.OnItemClickListener() {
            @Override
            public void onUserClick(User user) {
                if (homeActivity == null || user == null || user.getUserId() == null) return;

                String currentUserId = homeActivity.getCurrentUserId();
                if (currentUserId == null) return;

                homeActivity.showLoading();

                chatRepository.getOrCreateChat(currentUserId, user.getUserId(),
                        new ChatRepository.ChatCallback<Chat>() {
                            @Override
                            public void onSuccess(Chat chat) {
                                if (!isAdded()) return;
                                requireActivity().runOnUiThread(() -> {
                                    homeActivity.hideLoading();
                                    PersonalMessageFragment fragment =
                                            PersonalMessageFragment.newInstance(
                                                    chat.getChatId(),
                                                    chat.getChatPartnerId(currentUserId)
                                            );
                                    homeActivity.LoadFragment(fragment);
                                });
                            }

                            @Override
                            public void onError(String error) {
                                if (!isAdded() || getContext() == null) return;
                                requireActivity().runOnUiThread(() -> {
                                    homeActivity.hideLoading();
                                    Toast.makeText(getContext(),
                                            "Failed to open chat: " + error,
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            }

            @Override
            public void onMessageClick(User user) {
                // Same behavior as onUserClick
                onUserClick(user);
            }
        });

        // Observe user search results
        searchRepository.getUserSearchResultsLiveData().observe(getViewLifecycleOwner(), new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> users) {
                if (users == null || users.isEmpty()) {
                    binding.tvUserResults.setVisibility(View.GONE);
                    binding.recyclerSearchResult.setVisibility(View.GONE);
                    if (!currentSearchQuery.isEmpty()) {
                        binding.tvNoResults.setVisibility(View.VISIBLE);
                    }
                } else {
                    binding.tvNoResults.setVisibility(View.GONE);
                    binding.tvUserResults.setVisibility(View.VISIBLE);
                    binding.recyclerSearchResult.setVisibility(View.VISIBLE);
                    userAdapter.setUsers(users);
                }
            }
        });

        // Observe loading state
        searchRepository.getIsLoadingLiveData().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoading) {
                if (isLoading != null && isLoading) {
                    if (homeActivity != null) homeActivity.showLoading();
                } else {
                    if (homeActivity != null) homeActivity.hideLoading();
                }
            }
        });

        // Observe errors
        searchRepository.getErrorLiveData().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null && !error.isEmpty()) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
                if (homeActivity != null) homeActivity.hideLoading();
            }
        });

        // Search on IME action
        binding.editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.editTextSearch.getText().toString().trim();
                currentSearchQuery = query;
                performSearch(query);
                hideKeyboard();
                return true;
            }
            return false;
        });

        // Auto-focus and open keyboard
        binding.editTextSearch.postDelayed(() -> {
            binding.editTextSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(binding.editTextSearch, InputMethodManager.SHOW_IMPLICIT);
        }, 200);

        // Handle back key
        requireView().setFocusableInTouchMode(true);
        requireView().requestFocus();
        requireView().setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                binding.editTextSearch.clearFocus();
                hideKeyboard();
                if (homeActivity != null) {
                    homeActivity.onBackPressed();
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
            binding.tvNoResults.setVisibility(View.GONE);
            if (homeActivity != null) homeActivity.hideLoading();
        } else {
            // Only search users, not posts
            searchRepository.searchUsers(searchQuery);
        }
    }

    private void hideKeyboard() {
        binding.editTextSearch.clearFocus();
        InputMethodManager imm = (InputMethodManager) requireActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.editTextSearch.getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (homeActivity != null) homeActivity.hideLoading();
    }
}
