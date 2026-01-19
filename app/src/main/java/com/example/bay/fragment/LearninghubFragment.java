package com.example.bay.fragment;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class LearninghubFragment extends Fragment {

    private LinearLayout tabKnowledge, tabSave;
    private TextView tvKnowledge, tvSave;
    private View indicatorKnowledge, indicatorSave;
    private Spinner spinnerFilter;
    private ImageView icArrowDown;
    private RecyclerView recyclerViewCards;
    private EditText etSearch;
    private LinearLayout emptyStateView;
    private TextView emptyStateText;
    private Button button;

    private LearningHubViewModel viewModel;
    private LearninghubCardAdapter adapter;
    private List<LearninghubCard> allCards = new ArrayList<>();
    private List<LearninghubCard> savedCards = new ArrayList<>();
    private boolean isKnowledgeTabActive = true;
    private boolean isDropdownOpen = false;

    private static final String ACTIVE_COLOR = "#0E4123";
    private static final String INACTIVE_COLOR = "#6B7280";

    private HomeActivity homeActivity;
    private final Handler loadingHandler = new Handler(Looper.getMainLooper());
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private static final int LOADING_DELAY = 300;
    private static final int SEARCH_DELAY = 500;
    private final Runnable searchRunnable = this::applyFiltersAndCheckEmptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_learninghub, container, false);

        initViews(view);
        homeActivity = (HomeActivity) getActivity();
        if (homeActivity != null) {
            homeActivity.setBottomNavigationVisible(false);
        }

        setupViewModel();
        setupRecyclerView();
        setupClickListeners();
        setupSpinner();
        setupSearch();

        setActiveTab(tabKnowledge, true);
        loadKnowledgeContent();

        return view;
    }

    private void initViews(View view) {
        tabKnowledge = view.findViewById(R.id.tab_knowledge);
        tabSave = view.findViewById(R.id.tab_save);
        tvKnowledge = view.findViewById(R.id.tv_knowledge);
        tvSave = view.findViewById(R.id.tv_save);
        indicatorKnowledge = view.findViewById(R.id.indicator_knowledge);
        indicatorSave = view.findViewById(R.id.indicator_save);
        spinnerFilter = view.findViewById(R.id.spinner_filter);
        icArrowDown = view.findViewById(R.id.ic_arrow_down);
        recyclerViewCards = view.findViewById(R.id.recyclerViewCards);
        etSearch = view.findViewById(R.id.et_search);
        emptyStateView = view.findViewById(R.id.empty_state_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        button = view.findViewById(R.id.button);

        if (emptyStateView != null) {
            emptyStateView.setVisibility(View.GONE);
        }
        if (recyclerViewCards != null) {
            recyclerViewCards.setVisibility(View.GONE);
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(LearningHubViewModel.class);

        viewModel.getCards().observe(getViewLifecycleOwner(), cards -> {
            cancelLoading();
            if (cards != null) {
                allCards = cards;
                if (isKnowledgeTabActive) {
                    applyFiltersAndCheckEmptyState();
                }
            } else if (isKnowledgeTabActive) {
                showEmptyState("មិនអាចទាញយកទិន្នន័យបាន");
            }
        });

        viewModel.getSavedCards().observe(getViewLifecycleOwner(), cards -> {
            cancelLoading();
            if (cards != null) {
                savedCards = cards;
                if (!isKnowledgeTabActive) {
                    applyFiltersAndCheckEmptyState();
                }
            } else if (!isKnowledgeTabActive) {
                showEmptyState("មិនអាចទាញយកទិន្នន័យបាន");
            }
        });

        viewModel.getRefreshSavedCards().observe(getViewLifecycleOwner(), shouldRefresh -> {
            if (Boolean.TRUE.equals(shouldRefresh)) {
                if (!isKnowledgeTabActive) {
                    showLoadingDelayed();
                    viewModel.loadSavedCards();
                }
                viewModel.refreshSavedCardsComplete();
            }
        });

        viewModel.getUpdatedCardId().observe(getViewLifecycleOwner(), cardId -> {
            if (cardId != null) {
                if (isKnowledgeTabActive) {
                    viewModel.loadCards();
                } else {
                    viewModel.loadSavedCards();
                }
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                boolean hasData = isKnowledgeTabActive ? !allCards.isEmpty() : !savedCards.isEmpty();
                if (!hasData) {
                    showEmptyState("មិនអាចទាញយកទិន្នន័យបាន: " + errorMessage);
                }
                showErrorToast(errorMessage);
            }
        });

        showLoadingDelayed();
        viewModel.loadCards();
    }

    private void setupRecyclerView() {
        adapter = new LearninghubCardAdapter(
                this::openCardDetail,
                (card, isSaved) -> {
                    viewModel.toggleSaveCard(card.getUuid(), isSaved);
                    card.setIsSaved(isSaved);

                    String message = isSaved ?
                            getString(R.string.save_card) :
                            getString(R.string.unsave_card);
                    showSuccessToast(message);

                    if (isKnowledgeTabActive) {
                        applyFiltersAndCheckEmptyState();
                    } else {
                        if (!isSaved) {
                            savedCards.removeIf(c -> c.getUuid().equals(card.getUuid()));
                            applyFiltersAndCheckEmptyState();
                        }
                    }
                },
                this::openCardDetail,
                isKnowledgeTabActive
        );

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerViewCards.setLayoutManager(layoutManager);
        recyclerViewCards.setAdapter(adapter);

        // Performance optimizations
        recyclerViewCards.setHasFixedSize(true);
        recyclerViewCards.setItemViewCacheSize(20);
        recyclerViewCards.setDrawingCacheEnabled(true);
        recyclerViewCards.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }

    private void setupSearch() {
        button.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, new HomeFragment())
                    .commit();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }
        });

        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                animateSearchFocus(true);
            } else {
                animateSearchFocus(false);
            }
        });
    }

    private void setupClickListeners() {
        tabKnowledge.setOnClickListener(v -> {
            if (!isKnowledgeTabActive) {
                animateTabSwitch(tabKnowledge, () -> {
                    isKnowledgeTabActive = true;
                    setActiveTab(tabKnowledge, true);
                    loadKnowledgeContent();
                });
            }
        });

        tabSave.setOnClickListener(v -> {
            if (isKnowledgeTabActive) {
                animateTabSwitch(tabSave, () -> {
                    etSearch.setText("");
                    isKnowledgeTabActive = false;
                    setActiveTab(tabSave, true);
                    showLoadingDelayed();
                    loadSaveContent();
                });
            }
        });

        icArrowDown.setOnClickListener(v -> {
            if (!isDropdownOpen) {
                spinnerFilter.performClick();
                animateArrow(true);
                isDropdownOpen = true;
                new Handler().postDelayed(this::styleDropdownItems, 100);
            }
        });
    }

    private void setupSpinner() {
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(Color.parseColor(ACTIVE_COLOR));
                    textView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_bold), Typeface.BOLD);
                    textView.setTextSize(12);
                }
                applyFiltersAndCheckEmptyState();
                resetDropdownState();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { resetDropdownState(); }
        });

        spinnerFilter.setOnTouchListener((v, event) -> {
            if (!isDropdownOpen) {
                animateArrow(true);
                isDropdownOpen = true;
                new Handler().postDelayed(this::styleDropdownItems, 100);
            }
            return false;
        });
    }

    private void applyFiltersAndCheckEmptyState() {
        List<LearninghubCard> sourceList = isKnowledgeTabActive ? allCards : savedCards;

        if (sourceList == null || sourceList.isEmpty()) {
            showEmptyState(getEmptyStateMessage());
            adapter.submitCards(new ArrayList<>());
            return;
        }

        String selectedCategory = getSelectedCategory();
        String searchQuery = etSearch.getText().toString().trim().toLowerCase();
        List<LearninghubCard> filteredList = new ArrayList<>(sourceList.size());

        for (LearninghubCard card : sourceList) {
            boolean matchesCategory = selectedCategory.equals("all") ||
                    (card.getCategory() != null && card.getCategory().equals(selectedCategory));

            if (!matchesCategory) continue;

            if (!searchQuery.isEmpty()) {
                String title = card.getTitle();
                String author = card.getAuthor();
                boolean matchesSearch = (title != null && title.toLowerCase().contains(searchQuery)) ||
                        (author != null && author.toLowerCase().contains(searchQuery));
                if (!matchesSearch) continue;
            }

            filteredList.add(card);
        }

        if (filteredList.isEmpty()) {
            showEmptyState(getEmptyStateMessage());
            adapter.submitCards(new ArrayList<>());
        } else {
            hideEmptyState();
            adapter.submitCards(filteredList);
        }
    }

    private String getEmptyStateMessage() {
        String selectedCategory = getSelectedCategory();
        String searchQuery = etSearch.getText().toString().trim();

        if (!searchQuery.isEmpty()) {
            if (isKnowledgeTabActive) {
                return "មិនមានអត្ថបទដែលផ្គូផ្គងនឹង \"" + searchQuery + "\"";
            } else {
                return "មិនមានកាតដែលបានរក្សាទុកដែលផ្គូផ្គងនឹង \"" + searchQuery + "\"";
            }
        }

        if (!selectedCategory.equals("all")) {
            if (isKnowledgeTabActive) {
                return "មិនមានអត្ថបទនៅក្នុងប្រភេទ \"" + selectedCategory + "\"";
            } else {
                return "មិនមានកាតដែលបានរក្សាទុកនៅក្នុងប្រភេទ \"" + selectedCategory + "\"";
            }
        }

        if (isKnowledgeTabActive) {
            return "មិនមានអត្ថបទចំណេះដឹងទេ";
        } else {
            return "មិនមានកាតដែលបានរក្សាទុកទេ";
        }
    }

    private void showEmptyState(String message) {
        if (emptyStateView == null || emptyStateText == null) return;

        emptyStateText.setText(message);

        if (emptyStateView.getVisibility() != View.VISIBLE) {
            AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setDuration(300);
            emptyStateView.startAnimation(fadeIn);
            emptyStateView.setVisibility(View.VISIBLE);
        }

        if (recyclerViewCards.getVisibility() != View.GONE) {
            recyclerViewCards.setVisibility(View.GONE);
        }
    }

    private void hideEmptyState() {
        if (emptyStateView == null || recyclerViewCards == null) return;

        if (emptyStateView.getVisibility() == View.VISIBLE) {
            AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setDuration(200);
            emptyStateView.startAnimation(fadeOut);
            emptyStateView.setVisibility(View.GONE);
        }

        if (recyclerViewCards.getVisibility() != View.VISIBLE) {
            recyclerViewCards.setVisibility(View.VISIBLE);
        }
    }

    private void resetDropdownState() {
        if (isDropdownOpen) {
            animateArrow(false);
            isDropdownOpen = false;
        }
    }

    private void styleDropdownItems() {
        try {
            Field listViewField = Spinner.class.getDeclaredField("mPopup");
            listViewField.setAccessible(true);
            Object popup = listViewField.get(spinnerFilter);

            if (popup instanceof ListPopupWindow) {
                ListPopupWindow listPopup = (ListPopupWindow) popup;
                ListView listView = listPopup.getListView();
                if (listView != null) {
                    for (int i = 0; i < listView.getChildCount(); i++) {
                        View child = listView.getChildAt(i);
                        if (child instanceof TextView) {
                            TextView tv = (TextView) child;
                            tv.setTextColor(Color.parseColor(ACTIVE_COLOR));
                            tv.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_bold), Typeface.BOLD);
                            tv.setTextSize(12);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String mapCategory(String category) {
        switch (category) {
            case "ដីនិងជីកំប៉ុស": return "ដីនិងជីកំប៉ុស";
            case "ការត្រួតពិនិត្យសត្វល្អិតធម្មជាតិ": return "ការត្រួតពិនិត្យសត្វល្អិតធម្មជាតិ";
            case "ការគ្រប់គ្រងទឹក": return "ការគ្រប់គ្រងទឹក";
            case "គ្រាប់ពូជនិងផែនការ": return "គ្រាប់ពូជនិងផែនការ";
            case "ឧបករណ៍និងឧបករណ៍ផ្សេងៗ": return "ឧបករណ៍និងឧបករណ៍ផ្សេងៗ";
            case "ការរៀបចំផែនការកសិកម្ម": return "ការរៀបចំផែនការកសិកម្ម";
            default: return "all";
        }
    }

    private void loadKnowledgeContent() {
        updateKnowledgeTabUI();
        adapter.setTabActive(true);
        if (allCards.isEmpty()) {
            showLoadingDelayed();
            viewModel.loadCards();
        } else {
            applyFiltersAndCheckEmptyState();
        }
    }

    private void loadSaveContent() {
        updateSaveTabUI();
        adapter.setTabActive(false);
        showLoadingDelayed();
        viewModel.loadSavedCards();
    }

    private String getSelectedCategory() {
        if (spinnerFilter.getSelectedItem() != null) {
            return mapCategory(spinnerFilter.getSelectedItem().toString());
        }
        return "all";
    }

    private void updateKnowledgeTabUI() {
        spinnerFilter.setEnabled(true);
        spinnerFilter.setAlpha(1f);
        icArrowDown.setEnabled(true);
        icArrowDown.setAlpha(1f);
        etSearch.setEnabled(true);
        etSearch.setAlpha(1f);
        etSearch.setHint("ស្វែងរកចំណងជើង ឬអ្នកនិពន្ធ...");
    }

    private void updateSaveTabUI() {
        spinnerFilter.setEnabled(true);
        spinnerFilter.setAlpha(1f);
        icArrowDown.setEnabled(true);
        icArrowDown.setAlpha(1f);
        etSearch.setEnabled(true);
        etSearch.setAlpha(1f);
        etSearch.setHint("ស្វែងរកចំណងជើង ឬអ្នកនិពន្ធ...");

        if (isDropdownOpen) {
            resetDropdownState();
        }
    }

    private void openCardDetail(LearninghubCard card) {
        Log.d("LearninghubFragment", " Opening card detail: " + card.getTitle() + " ID: " + card.getUuid());

        if (card == null || card.getUuid() == null) {
            showErrorToast("Cannot open card: Invalid card data");
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
                    .addToBackStack("learninghub_detail")
                    .commit();

        } catch (Exception e) {
            Log.e("LearninghubFragment", "Navigation error: " + e.getMessage(), e);
            showErrorToast("Navigation error: " + e.getMessage());
        }
    }

    private void animateTabSwitch(View tab, Runnable onComplete) {
        tab.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> tab.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction(onComplete)
                        .start())
                .start();
    }

    private void animateArrow(boolean up) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(icArrowDown, "rotation", up ? 180f : 0f);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    private void animateSearchFocus(boolean hasFocus) {
        float scale = hasFocus ? 1.02f : 1f;
        etSearch.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(200)
                .start();
    }

    private void showSuccessToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void setActiveTab(LinearLayout selectedTab, boolean withAnimation) {
        resetTabs();
        if (selectedTab == tabKnowledge) {
            setTabActive(tvKnowledge, indicatorKnowledge, true);
        } else {
            setTabActive(tvSave, indicatorSave, true);
        }
    }

    private void setTabActive(TextView textView, View indicator, boolean isActive) {
        int color = Color.parseColor(isActive ? ACTIVE_COLOR : INACTIVE_COLOR);
        textView.setTextColor(color);
        indicator.setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
        indicator.setBackgroundColor(color);
        if (textView.getCompoundDrawablesRelative()[0] != null)
            textView.getCompoundDrawablesRelative()[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private void resetTabs() {
        setTabActive(tvKnowledge, indicatorKnowledge, false);
        setTabActive(tvSave, indicatorSave, false);
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
                if (recyclerViewCards != null) {
                    recyclerViewCards.setVisibility(View.GONE);
                }
                if (emptyStateView != null) {
                    emptyStateView.setVisibility(View.GONE);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        loadingHandler.removeCallbacksAndMessages(null);
        searchHandler.removeCallbacksAndMessages(null);

        // Clear Glide cache
        if (getContext() != null) {
            Glide.get(getContext()).clearMemory();
        }

        if (homeActivity != null) {
            homeActivity.setBottomNavigationVisible(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("LearninghubFragment", "onResume - isKnowledgeTabActive: " + isKnowledgeTabActive);

        if (isKnowledgeTabActive) {
            if (allCards.isEmpty()) {
                viewModel.loadCards();
            } else {
                applyFiltersAndCheckEmptyState();
            }
        } else {
            Log.d("LearninghubFragment", "Refreshing saved cards on resume");
            viewModel.loadSavedCards();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isKnowledgeTabActive", isKnowledgeTabActive);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            isKnowledgeTabActive = savedInstanceState.getBoolean("isKnowledgeTabActive", true);
            if (isKnowledgeTabActive) {
                setActiveTab(tabKnowledge, false);
            } else {
                setActiveTab(tabSave, false);
            }
        }
    }
}