package com.example.bay.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.ShoppingItemAdapter;
import com.example.bay.model.ShoppingItem;
import com.example.bay.model.User;
import com.example.bay.viewmodel.ShoppingViewModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private List<ShoppingItem> latestItems = new ArrayList<>();
    private Map<String, User> latestUsers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_market_place, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ShoppingViewModel.class);

        initializeViews(view);
        setupRecyclerView();
        setupCategoryFilters();
        observeViewModel();

        viewModel.loadShoppingItems();
        viewModel.loadUsers();
    }

    private void initializeViews(View view) {
        rvShoppingItems = view.findViewById(R.id.rvShoppingItems);

        loadingView = view.findViewById(R.id.loading);
        lottieView = view.findViewById(R.id.lottieView);
        emptyState = view.findViewById(R.id.emptyState);

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
                    @Override public void onItemClick(ShoppingItem item) { navigateToDetailFragment(item); }
                    @Override public void onSellerClick(String userId) { Log.d("MarketPlace", "Seller clicked: " + userId); }
                });

        rvShoppingItems.setAdapter(adapter);
    }

    private void setupCategoryFilters() {
        chipAll.setOnClickListener(v -> selectCategory("ទាំងអស់", chipAll));
        chipVegetable.setOnClickListener(v -> selectCategory("បន្លែ", chipVegetable));
        chipFruit.setOnClickListener(v -> selectCategory("ផ្លែឈើ", chipFruit));
        chipTool.setOnClickListener(v -> selectCategory("សម្ភារៈ", chipTool));
        chipSeeds.setOnClickListener(v -> selectCategory("គ្រាប់ពូជ", chipSeeds));
        chipFertilizer.setOnClickListener(v -> selectCategory("ជី", chipFertilizer));
        chipPesticide.setOnClickListener(v -> selectCategory("ថ្នាំ", chipPesticide));

        // ✅ FIX spelling (must match repository/items)
        chipMedical.setOnClickListener(v -> selectCategory("សម្ភារៈវេជ្ជសាស្ត្រ", chipMedical));

        chipOthers.setOnClickListener(v -> selectCategory("ផ្សេងៗ", chipOthers));
    }

    private void selectCategory(String category, Chip chip) {
        clearChipSelections();
        chip.setChecked(true);
        currentCategory = category;
        viewModel.filterByCategory(category);
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

        viewModel.getFilteredItems().observe(getViewLifecycleOwner(), items -> {
            latestItems = items != null ? items : new ArrayList<>();
            bindAdapter();
        });

        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            latestUsers = users;
            bindAdapter();
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) showLoading();
            else hideLoading();
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "កំហុស: " + error, Toast.LENGTH_SHORT).show();
                hideLoading();
            }
        });
    }

    private void bindAdapter() {
        if (adapter == null) return;

        adapter.updateData(latestItems, latestUsers);

        if (latestItems == null || latestItems.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    public void searchItems(String query) {
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
        HomeActivity activity = (HomeActivity) getActivity();
        if (activity == null) return;
        activity.showLoading();
    }

    private void hideLoading() {
        HomeActivity activity = (HomeActivity) getActivity();
        if (activity == null) return;
        activity.hideLoading();
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
        viewModel.loadShoppingItems();          // ✅ refresh data
        viewModel.filterByCategory(currentCategory);
    }
}