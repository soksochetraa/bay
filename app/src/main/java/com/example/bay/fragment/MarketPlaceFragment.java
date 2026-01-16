package com.example.bay.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.bay.R;
import com.example.bay.adapter.ShoppingItemAdapter;
import com.example.bay.model.ShoppingItem;
import com.example.bay.viewmodel.ShoppingViewModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;

public class MarketPlaceFragment extends Fragment {
    private ShoppingViewModel viewModel;
    private ShoppingItemAdapter adapter;
    private RecyclerView rvShoppingItems;

    private View loadingView;
    private LinearLayout emptyState;
    private LottieAnimationView lottieView;

    private Chip chipAll, chipVegetable, chipFruit, chipTool, chipSeeds,
            chipFertilizer, chipPesticide, chipMedical, chipOthers;

    private String currentCategory = "ទាំងអស់";
    private String lastSearchQuery = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_market_place, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Use requireActivity() to get SHARED ViewModel instance
        viewModel = new ViewModelProvider(requireActivity()).get(ShoppingViewModel.class);

        initializeViews(view);
        setupRecyclerView();
        setupCategoryFilters();
        observeViewModel();

        // Load initial data
        viewModel.loadShoppingItems();
    }

    private void initializeViews(View view) {
        rvShoppingItems = view.findViewById(R.id.rvShoppingItems);

        loadingView = view.findViewById(R.id.loading);
        lottieView = view.findViewById(R.id.lottieView);
        emptyState = view.findViewById(R.id.emptyState);

        // Initialize chips
        chipAll = view.findViewById(R.id.chip_all);
        chipVegetable = view.findViewById(R.id.chip_vegetable);
        chipFruit = view.findViewById(R.id.chip_fruit);
        chipTool = view.findViewById(R.id.chip_tool);
        chipSeeds = view.findViewById(R.id.chip_seeds);
        chipFertilizer = view.findViewById(R.id.chip_fertilizer);
        chipPesticide = view.findViewById(R.id.chip_pesticide);
        chipMedical = view.findViewById(R.id.chip_medical);
        chipOthers = view.findViewById(R.id.chip_others);

        chipAll.setChecked(true);
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        rvShoppingItems.setLayoutManager(layoutManager);

        adapter = new ShoppingItemAdapter(requireContext(), new ArrayList<>(), null,
                new ShoppingItemAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(ShoppingItem item) {
                        Log.d("MarketPlace", "Item clicked: " + item.getName());
                        navigateToDetailFragment(item);
                    }

                    @Override
                    public void onSellerClick(String userId) {
                        Log.d("MarketPlace", "Seller clicked: " + userId);
                    }
                });

        rvShoppingItems.setAdapter(adapter);
    }

    private void setupCategoryFilters() {
        chipAll.setOnClickListener(v -> {
            clearChipSelections();
            chipAll.setChecked(true);
            currentCategory = "ទាំងអស់";
            viewModel.filterByCategory(currentCategory);
        });

        chipVegetable.setOnClickListener(v -> {
            clearChipSelections();
            chipVegetable.setChecked(true);
            currentCategory = "បន្លែ";
            viewModel.filterByCategory(currentCategory);
        });

        chipFruit.setOnClickListener(v -> {
            clearChipSelections();
            chipFruit.setChecked(true);
            currentCategory = "ផ្លែឈើ";
            viewModel.filterByCategory(currentCategory);
        });

        chipTool.setOnClickListener(v -> {
            clearChipSelections();
            chipTool.setChecked(true);
            currentCategory = "សម្ភារៈ";
            viewModel.filterByCategory(currentCategory);
        });

        chipSeeds.setOnClickListener(v -> {
            clearChipSelections();
            chipSeeds.setChecked(true);
            currentCategory = "គ្រាប់ពូជ";
            viewModel.filterByCategory(currentCategory);
        });

        chipFertilizer.setOnClickListener(v -> {
            clearChipSelections();
            chipFertilizer.setChecked(true);
            currentCategory = "ជី";
            viewModel.filterByCategory(currentCategory);
        });

        chipPesticide.setOnClickListener(v -> {
            clearChipSelections();
            chipPesticide.setChecked(true);
            currentCategory = "ថ្នាំ";
            viewModel.filterByCategory(currentCategory);
        });

        chipMedical.setOnClickListener(v -> {
            clearChipSelections();
            chipMedical.setChecked(true);
            currentCategory = "សម្ភារៈវេជ្ជសាស្រ្ត";
            viewModel.filterByCategory(currentCategory);
        });

        chipOthers.setOnClickListener(v -> {
            clearChipSelections();
            chipOthers.setChecked(true);
            currentCategory = "ផ្សេងៗ";
            viewModel.filterByCategory(currentCategory);
        });
    }

    private void clearChipSelections() {
        chipAll.setChecked(false);
        chipVegetable.setChecked(false);
        chipFruit.setChecked(false);
        chipTool.setChecked(false);
        chipSeeds.setChecked(false);
        chipFertilizer.setChecked(false);
        chipPesticide.setChecked(false);
        chipMedical.setChecked(false);
        chipOthers.setChecked(false);
    }

    private void observeViewModel() {
        // Observe FILTERED ITEMS (this already includes search results)
        viewModel.getFilteredItems().observe(getViewLifecycleOwner(), items -> {
            Log.d("MarketPlaceFragment", "Filtered items updated: " + items.size() + " items");

            viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
                adapter.updateData(items, users);

                if (items.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                }
            });
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                if (isLoading) {
                    showLoading();
                } else {
                    hideLoading();
                }
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "កំហុស: " + error, Toast.LENGTH_SHORT).show();
                hideLoading();
            }
        });
    }

    // Search method - not needed anymore as ViewModel handles it
    public void searchItems(String query) {
        lastSearchQuery = query;
        viewModel.searchItems(query);
    }

    private void navigateToDetailFragment(ShoppingItem item) {
        DetailItemShoppingFragment fragment = DetailItemShoppingFragment.newInstance(item);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack("marketplace")
                .commit();
    }

    private void showLoading() {
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
            rvShoppingItems.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);

            if (lottieView != null && !lottieView.isAnimating()) {
                lottieView.playAnimation();
            }
        }
    }

    private void hideLoading() {
        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
            rvShoppingItems.setVisibility(View.VISIBLE);

            if (lottieView != null && lottieView.isAnimating()) {
                lottieView.cancelAnimation();
            }
        }
    }

    private void showEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
            rvShoppingItems.setVisibility(View.GONE);
        }
    }

    private void hideEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
            rvShoppingItems.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("MarketPlaceFragment", "onResume - applying category: " + currentCategory);
        viewModel.filterByCategory(currentCategory);
    }
}