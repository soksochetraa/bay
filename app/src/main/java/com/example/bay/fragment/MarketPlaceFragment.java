package com.example.bay.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import com.example.bay.R;
import com.example.bay.AddShoppingItemActivity;
import com.example.bay.adapter.ShoppingItemAdapter;
import com.example.bay.model.ShoppingItem;
import com.example.bay.viewmodel.ShoppingViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;

public class MarketPlaceFragment extends Fragment {
    private ShoppingViewModel viewModel;
    private ShoppingItemAdapter adapter;
    private RecyclerView rvShoppingItems;
    private EditText etSearch;
    private MaterialButton btnAddItem;

    // Loading UI elements
    private View loadingView;
    private LottieAnimationView lottieView;
    private TextView loadingText;

    private Chip chipAll, chipVegetable, chipFruit, chipTool, chipSeeds,
            chipFertilizer, chipPesticide, chipMedical, chipOthers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_market_place, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ShoppingViewModel.class);
        initializeViews(view);
        setupRecyclerView();
        setupSearch();
        setupCategoryFilters();
        setupAddItemButton();
        observeViewModel();
    }

    private void initializeViews(View view) {
        rvShoppingItems = view.findViewById(R.id.rvShoppingItems);
        etSearch = view.findViewById(R.id.etSearch);
        btnAddItem = view.findViewById(R.id.btnAddItem);

        // Initialize loading views
        loadingView = view.findViewById(R.id.loading);
        lottieView = view.findViewById(R.id.lottieView);
        loadingText = view.findViewById(R.id.loadingText);

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


    // In setupRecyclerView() method
    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        rvShoppingItems.setLayoutManager(layoutManager);

        adapter = new ShoppingItemAdapter(requireContext(), new ArrayList<>(), null,
                new ShoppingItemAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(ShoppingItem item) {
                        // Optional: You can add some feedback here
                        Log.d("MarketPlace", "Item clicked: " + item.getName());
                    }

                    @Override
                    public void onSellerClick(String userId) {
                        // Optional: Handle seller click
                        Log.d("MarketPlace", "Seller clicked: " + userId);
                    }
                });

        rvShoppingItems.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryFilters() {
        chipAll.setOnClickListener(v -> {
            clearChipSelections();
            chipAll.setChecked(true);
            viewModel.filterByCategory("ទាំងអស់");
        });

        chipVegetable.setOnClickListener(v -> {
            clearChipSelections();
            chipVegetable.setChecked(true);
            viewModel.filterByCategory("បន្លែ");
        });

        chipFruit.setOnClickListener(v -> {
            clearChipSelections();
            chipFruit.setChecked(true);
            viewModel.filterByCategory("ផ្លែឈើ");
        });

        chipTool.setOnClickListener(v -> {
            clearChipSelections();
            chipTool.setChecked(true);
            viewModel.filterByCategory("សម្ភារៈ");
        });

        chipSeeds.setOnClickListener(v -> {
            clearChipSelections();
            chipSeeds.setChecked(true);
            viewModel.filterByCategory("គ្រាប់ពូជ");
        });

        chipFertilizer.setOnClickListener(v -> {
            clearChipSelections();
            chipFertilizer.setChecked(true);
            viewModel.filterByCategory("ជី");
        });

        chipPesticide.setOnClickListener(v -> {
            clearChipSelections();
            chipPesticide.setChecked(true);
            viewModel.filterByCategory("ថ្នាំ");
        });

        chipMedical.setOnClickListener(v -> {
            clearChipSelections();
            chipMedical.setChecked(true);
            viewModel.filterByCategory("សម្ភារៈវេជ្ជសាស្រ្ត");
        });

        chipOthers.setOnClickListener(v -> {
            clearChipSelections();
            chipOthers.setChecked(true);
            viewModel.filterByCategory("ផ្សេងៗ");
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

    private void setupAddItemButton() {
        btnAddItem.setOnClickListener(v -> {
            startActivity(AddShoppingItemActivity.newIntent(requireContext()));
        });
    }

    private void observeViewModel() {
        viewModel.getFilteredItems().observe(getViewLifecycleOwner(), items -> {
            viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
                adapter.updateData(items, users);
            });
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                if (isLoading) {
                    // Show loading immediately when data starts loading
                    showLoading();
                } else {
                    // Hide loading after 2000ms delay
                    new android.os.Handler().postDelayed(() -> {
                        hideLoading();
                    }, 2000);
                }
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "កំហុស: " + error, Toast.LENGTH_SHORT).show();
                hideLoading(); // Hide loading on error immediately
            }
        });
    }

    private void showLoading() {
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
            rvShoppingItems.setVisibility(View.GONE);

            // Ensure Lottie animation is playing
            if (lottieView != null && !lottieView.isAnimating()) {
                lottieView.playAnimation();
            }

            // Optional: Change loading text based on what's happening
            if (loadingText != null) {
                loadingText.setText("កំពុងផ្ទុកទិន្នន័យ...");
            }
        }
    }

    private void hideLoading() {
        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
            rvShoppingItems.setVisibility(View.VISIBLE);

            // Stop Lottie animation to save resources
            if (lottieView != null && lottieView.isAnimating()) {
                lottieView.cancelAnimation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.filterByCategory(getCurrentCategory());
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop animation when fragment is paused to save resources
        if (lottieView != null && lottieView.isAnimating()) {
            lottieView.cancelAnimation();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up Lottie animation resources
        if (lottieView != null) {
            lottieView.cancelAnimation();
            lottieView = null;
        }
    }

    private String getCurrentCategory() {
        if (chipVegetable.isChecked()) return "បន្លែ";
        if (chipFruit.isChecked()) return "ផ្លែឈើ";
        if (chipTool.isChecked()) return "សម្ភារៈ";
        if (chipSeeds.isChecked()) return "គ្រាប់ពូជ";
        if (chipFertilizer.isChecked()) return "ជី";
        if (chipPesticide.isChecked()) return "ថ្នាំ";
        if (chipMedical.isChecked()) return "សម្ភារៈវេជ្ជសាស្រ្ត";
        if (chipOthers.isChecked()) return "ផ្សេងៗ";
        return "ទាំងអស់";
    }
}