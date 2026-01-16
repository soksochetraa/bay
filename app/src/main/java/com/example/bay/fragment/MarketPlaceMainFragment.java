package com.example.bay.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bay.R;
import com.example.bay.AddShoppingItemActivity;
import com.example.bay.viewmodel.ShoppingViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

public class MarketPlaceMainFragment extends Fragment {

    private EditText etSearch;
    private MaterialButton btnAddItem;
    private ShoppingViewModel sharedViewModel;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    private String[] tabTitles = {"ហាង", "ផុសបស់ខ្ញុំ"};
    private int[] tabIcons = {R.drawable.ic_store, R.drawable.ic_my_post};

    // Add ViewPager callback to handle swipe
    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            // Check authentication for my posts tab
            if (position == 1) {
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Toast.makeText(requireContext(), "សូមចូលគណនីជាមុន", Toast.LENGTH_SHORT).show();
                    viewPager.setCurrentItem(0, true);
                    return;
                }
            }

            // Clear search when switching tabs
            if (etSearch != null) {
                etSearch.setText("");
            }

            // Clear search in ViewModel for both tabs
            sharedViewModel.searchItems("");
            sharedViewModel.searchUserPosts("");

            // Update icon colors when page changes via swipe
            updateTabIcons();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_market_place_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get SHARED ViewModel instance
        sharedViewModel = new ViewModelProvider(requireActivity()).get(ShoppingViewModel.class);

        initializeViews(view);
        setupViewPager();
        setupSearch();
        setupAddItemButton();
        setupTabIcons();
    }

    private void initializeViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        btnAddItem = view.findViewById(R.id.btnAddItem);
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);
    }

    private void setupViewPager() {
        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return new MarketPlaceFragment();
                    case 1:
                        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                            Toast.makeText(requireContext(), "សូមចូលគណនីជាមុន", Toast.LENGTH_SHORT).show();
                            return new MarketPlaceFragment();
                        }
                        return new MyPostsFragment();
                    default:
                        return new MarketPlaceFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        };

        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);

        // Register the page change callback
        viewPager.registerOnPageChangeCallback(pageChangeCallback);

        // Use TabLayoutMediator to connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(tabTitles[position]);
            tab.setIcon(tabIcons[position]);
        }).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 1) {
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                        Toast.makeText(requireContext(), "សូមចូលគណនីជាមុន", Toast.LENGTH_SHORT).show();
                        viewPager.setCurrentItem(0);
                        return;
                    }
                }

                // Clear search when switching tabs
                if (etSearch != null) {
                    etSearch.setText("");
                }

                // Clear search in ViewModel for both tabs
                sharedViewModel.searchItems("");
                sharedViewModel.searchUserPosts("");

                // Update icon color for selected tab
                updateTabIcons();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Update icon color for unselected tab
                updateTabIcons();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Set initial icon colors
        updateTabIcons();
    }

    private void setupTabIcons() {
        // Update icon colors based on selection state
        updateTabIcons();
    }

    private void updateTabIcons() {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null && tab.getIcon() != null) {
                if (tab.isSelected()) {
                    // Selected tab - primary color
                    tab.getIcon().setTint(ContextCompat.getColor(requireContext(), R.color.primary));
                    tab.view.setSelected(true); // Ensure the tab view is marked as selected
                } else {
                    // Unselected tab - gray color
                    tab.getIcon().setTint(ContextCompat.getColor(requireContext(), R.color.gray));
                    tab.view.setSelected(false);
                }
            }
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchQuery = s.toString().trim();
                int currentTab = viewPager.getCurrentItem();

                Log.d("MarketPlaceMain", "Search: '" + searchQuery + "' in tab: " + currentTab);

                if (currentTab == 0) {
                    // Search in marketplace - Update ViewModel
                    sharedViewModel.searchItems(searchQuery);
                } else if (currentTab == 1) {
                    // Search in my posts
                    sharedViewModel.searchUserPosts(searchQuery);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupAddItemButton() {
        btnAddItem.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(requireContext(), "សូមចូលគណនីជាមុន", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(AddShoppingItemActivity.newIntent(requireContext()));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Clear search when returning
        if (etSearch != null) {
            etSearch.setText("");
        }

        // Update tab icons when returning to fragment
        if (tabLayout != null) {
            updateTabIcons();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister the callback to prevent memory leaks
        if (viewPager != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
    }
}