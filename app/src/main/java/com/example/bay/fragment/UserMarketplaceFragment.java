package com.example.bay.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bay.R;
import com.example.bay.adapter.ShoppingItemAdapter;
import com.example.bay.model.ShoppingItem;
import com.example.bay.model.User;
import com.example.bay.viewmodel.ShoppingViewModel;
import com.example.bay.fragment.DetailItemShoppingFragment;
import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;

public class UserMarketplaceFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_USER_NAME = "user_name";

    private String targetUserId;
    private String targetUserName;

    private ShoppingViewModel viewModel;
    private ShoppingItemAdapter adapter;
    private RecyclerView rvShoppingItems;

    private RelativeLayout loadingView;
    private LinearLayout emptyState;
    private LottieAnimationView lottieView;

    public UserMarketplaceFragment() {
        // Required empty public constructor
    }

    public static UserMarketplaceFragment newInstance(String userId, String userName) {
        UserMarketplaceFragment fragment = new UserMarketplaceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_USER_NAME, userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetUserId = getArguments().getString(ARG_USER_ID);
            targetUserName = getArguments().getString(ARG_USER_NAME);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_marketplace, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Instantiate a fragment-scoped ViewModel so it doesn't conflict with Activity searches
        viewModel = new ViewModelProvider(this).get(ShoppingViewModel.class);

        initializeViews(view);
        setupRecyclerView();
        observeViewModel();

        loadData();
    }

    private void initializeViews(View view) {
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        if (targetUserName != null && !targetUserName.isEmpty()) {
            tvTitle.setText("ទំនិញរបស់ " + targetUserName);
        } else {
            tvTitle.setText("ទំនិញរបស់អ្នកគ្រូ");
        }

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        rvShoppingItems = view.findViewById(R.id.rvShoppingItems);
        loadingView = view.findViewById(R.id.loading);
        emptyState = view.findViewById(R.id.emptyState);
        lottieView = view.findViewById(R.id.lottieView);
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        rvShoppingItems.setLayoutManager(layoutManager);

        adapter = new ShoppingItemAdapter(requireContext(), new ArrayList<>(), null,
                new ShoppingItemAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(ShoppingItem item) {
                        navigateToDetailFragment(item);
                    }

                    @Override
                    public void onSellerClick(String userId) {
                        // Already looking at seller profile
                    }
                });

        rvShoppingItems.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getUserPosts().observe(getViewLifecycleOwner(), items -> {
            if (adapter != null) {
                // The ShoppingItemAdapter requires a Map of users but we only have this user.
                // It will just display "មិនស្គាល់" if it can't find the user profile picture in the adapter,
                // but since it uses the latestUsers map observed below, it should work fine.
                adapter.updateData(items != null ? items : new ArrayList<>(), viewModel.getUsers().getValue());
            }

            if (items == null || items.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
            }
        });

        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            if (adapter != null && viewModel.getUserPosts().getValue() != null) {
                adapter.updateData(viewModel.getUserPosts().getValue(), users);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                showLoading();
            } else {
                hideLoading();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "កំហុស: " + error, Toast.LENGTH_SHORT).show();
                hideLoading();
            }
        });
    }

    private void loadData() {
        if (targetUserId != null) {
            viewModel.loadUserPosts(targetUserId);
        } else {
            Toast.makeText(getContext(), "មិនមានអត្តសញ្ញាណអ្នកលក់", Toast.LENGTH_SHORT).show();
            showEmptyState();
        }
    }

    private void navigateToDetailFragment(ShoppingItem item) {
        DetailItemShoppingFragment fragment = DetailItemShoppingFragment.newInstance(item);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
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

}
