package com.example.bay.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.LearninghubCardAdapter;
import com.example.bay.model.LearninghubCard;
import com.example.bay.viewmodel.LearningHubViewModel;

import java.util.ArrayList;
import java.util.List;

public class SavedCardsFragment extends Fragment {

    private RecyclerView recyclerViewSavedCards;
    private TextView tvEmptyState;
    private Button btnBack;
    private LearninghubCardAdapter adapter;
    private LearningHubViewModel viewModel;

    private HomeActivity homeActivity;
    private final Handler loadingHandler = new Handler(Looper.getMainLooper());
    private static final int LOADING_DELAY = 300;
    private List<LearninghubCard> savedCards = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_cards, container, false);

        initViews(view);
        homeActivity = (HomeActivity) getActivity();
        if (homeActivity != null) {
            homeActivity.setBottomNavigationVisible(false);
        }

        setupViewModel();
        setupRecyclerView();

        showLoadingDelayed();
        viewModel.loadSavedCards();

        return view;
    }

    private void initViews(View view) {
        recyclerViewSavedCards = view.findViewById(R.id.recyclerViewSavedCards);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        btnBack = view.findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                // Simply go back - the fragment manager will handle the stack
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(LearningHubViewModel.class);

        viewModel.getSavedCards().observe(getViewLifecycleOwner(), cards -> {
            cancelLoading();
            if (cards != null) {
                savedCards = cards;
                if (!savedCards.isEmpty()) {
                    adapter.submitCards(savedCards);
                    hideEmptyState();
                } else {
                    showEmptyState("មិនមានកាតដែលរក្សាទុកទេ");
                    adapter.submitCards(new ArrayList<>());
                }
            } else {
                showEmptyState("មិនអាចទាញយកទិន្នន័យបាន");
                adapter.submitCards(new ArrayList<>());
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                if (savedCards.isEmpty()) {
                    showEmptyState("មិនអាចទាញយកទិន្នន័យបាន: " + errorMessage);
                }
                showErrorToast(errorMessage);
            }
        });

        viewModel.getRefreshSavedCards().observe(getViewLifecycleOwner(), shouldRefresh -> {
            if (Boolean.TRUE.equals(shouldRefresh)) {
                showLoadingDelayed();
                viewModel.loadSavedCards();
                viewModel.refreshSavedCardsComplete();
            }
        });

        viewModel.getUpdatedCardId().observe(getViewLifecycleOwner(), cardId -> {
            if (cardId != null) {
                showLoadingDelayed();
                viewModel.loadSavedCards();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new LearninghubCardAdapter(
                this::openCardDetail,
                (card, isSaved) -> {
                    viewModel.toggleSaveCard(card.getUuid(), isSaved);

                    String message = isSaved ?
                            getString(R.string.save_card) :
                            getString(R.string.unsave_card);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

                    if (!isSaved) {
                        savedCards.removeIf(c -> c.getUuid().equals(card.getUuid()));
                        if (savedCards.isEmpty()) {
                            showEmptyState("មិនមានកាតដែលរក្សាទុកទេ");
                        }
                        adapter.submitCards(savedCards);
                    }
                },
                this::openCardDetail,
                false
        );

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerViewSavedCards.setLayoutManager(layoutManager);
        recyclerViewSavedCards.setAdapter(adapter);

        recyclerViewSavedCards.setHasFixedSize(true);
        recyclerViewSavedCards.setItemViewCacheSize(20);
        recyclerViewSavedCards.setDrawingCacheEnabled(true);
        recyclerViewSavedCards.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }

    private void showEmptyState(String message) {
        if (tvEmptyState == null || recyclerViewSavedCards == null) return;

        tvEmptyState.setText(message);

        if (tvEmptyState.getVisibility() != View.VISIBLE) {
            AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setDuration(300);
            tvEmptyState.startAnimation(fadeIn);
            tvEmptyState.setVisibility(View.VISIBLE);
        }

        if (recyclerViewSavedCards.getVisibility() != View.GONE) {
            recyclerViewSavedCards.setVisibility(View.GONE);
        }
    }

    private void hideEmptyState() {
        if (tvEmptyState == null || recyclerViewSavedCards == null) return;

        if (tvEmptyState.getVisibility() == View.VISIBLE) {
            AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setDuration(200);
            tvEmptyState.startAnimation(fadeOut);
            tvEmptyState.setVisibility(View.GONE);
        }

        if (recyclerViewSavedCards.getVisibility() != View.VISIBLE) {
            recyclerViewSavedCards.setVisibility(View.VISIBLE);
        }
    }

    private void openCardDetail(LearninghubCard card) {
        Log.d("SavedCardsFragment", " Opening card detail from saved: " + card.getTitle());

        if (card == null || card.getUuid() == null) {
            Toast.makeText(requireContext(), getString(R.string.cannot_open_card), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            CardDetailFragment fragment = new CardDetailFragment();
            Bundle args = new Bundle();
            args.putString("card_id", card.getUuid());
            fragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack("saved_to_detail")
                    .commit();

        } catch (Exception e) {
            Log.e("SavedCardsFragment", "Navigation error: " + e.getMessage(), e);
            Toast.makeText(requireContext(), getString(R.string.error_opening_card), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoadingDelayed() {
        loadingHandler.postDelayed(() -> {
            if (isAdded()) showLoading();
        }, LOADING_DELAY);
    }

    private void cancelLoading() {
        loadingHandler.removeCallbacksAndMessages(null);
        hideLoading();
    }

    private void showLoading() {
        if (homeActivity != null && isAdded()) {
            homeActivity.runOnUiThread(() -> {
                View loadingView = homeActivity.findViewById(R.id.loading);
                if (loadingView != null) {
                    loadingView.setVisibility(View.VISIBLE);
                }
                if (recyclerViewSavedCards != null) {
                    recyclerViewSavedCards.setVisibility(View.GONE);
                }
                if (tvEmptyState != null) {
                    tvEmptyState.setVisibility(View.GONE);
                }
            });
        }
    }

    private void hideLoading() {
        if (homeActivity != null && isAdded()) {
            homeActivity.runOnUiThread(() -> {
                View loadingView = homeActivity.findViewById(R.id.loading);
                if (loadingView != null) {
                    loadingView.setVisibility(View.GONE);
                }
            });
        }
    }

    private void showSuccessToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("SavedCardsFragment", "onResume - Refreshing saved cards");
        showLoadingDelayed();
        viewModel.loadSavedCards();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Clear animations to prevent memory leaks
        if (recyclerViewSavedCards != null) {
            for (int i = 0; i < recyclerViewSavedCards.getChildCount(); i++) {
                recyclerViewSavedCards.getChildAt(i).clearAnimation();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        loadingHandler.removeCallbacksAndMessages(null);

        // Clear Glide cache
        if (getContext() != null) {
            Glide.get(getContext()).clearMemory();
        }

        if (homeActivity != null) {
            homeActivity.setBottomNavigationVisible(true);
        }
    }
}