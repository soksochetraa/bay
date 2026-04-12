package com.example.bay.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.PostCardCommunityAdapter;
import com.example.bay.model.PostCardItem;
import com.example.bay.util.FirebaseDBHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SavedCommunityPostsFragment extends Fragment {

    private static final String TAG = "SavedPostsFragment";

    private RecyclerView recyclerView;
    private RelativeLayout loadingView;
    private LinearLayout emptyState;
    private LottieAnimationView lottieView;

    private PostCardCommunityAdapter adapter;
    private ValueEventListener savedPostsListener;
    private Query savedPostsQuery;

    public SavedCommunityPostsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_community_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();

        loadSavedPosts();
    }

    private void initializeViews(View view) {
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        recyclerView = view.findViewById(R.id.recyclerView);
        loadingView = view.findViewById(R.id.loading);
        emptyState = view.findViewById(R.id.emptyState);
        lottieView = view.findViewById(R.id.lottieView);
    }

    private void setupRecyclerView() {
        adapter = new PostCardCommunityAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        
        // Listen to adapter data changes to update empty state
        adapter.getPostCardItemsLiveData().observe(getViewLifecycleOwner(), posts -> {
            if (posts == null || posts.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
            }
        });
    }

    private void loadSavedPosts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            hideLoading();
            showEmptyState();
            return;
        }

        String uid = user.getUid();
        showLoading();

        DatabaseReference postsRef = FirebaseDBHelper.getDatabase().getReference("postCardItems");
        savedPostsQuery = postsRef.orderByChild("savedBy/" + uid).equalTo(true);

        savedPostsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<PostCardItem> savedPosts = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    PostCardItem post = child.getValue(PostCardItem.class);
                    if (post != null) {
                        if (post.getItemId() == null || post.getItemId().isEmpty()) {
                            post.setItemId(child.getKey());
                        }
                        savedPosts.add(post);
                    }
                }

                hideLoading();
                adapter.setPostCardItemList(savedPosts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching saved posts: " + error.getMessage());
                hideLoading();
            }
        };

        savedPostsQuery.addValueEventListener(savedPostsListener);

        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).hideBottomNavigation();
        }
    }

    private void showLoading() {
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);

            if (lottieView != null && !lottieView.isAnimating()) {
                lottieView.playAnimation();
            }
        }
    }

    private void hideLoading() {
        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (lottieView != null && lottieView.isAnimating()) {
                lottieView.cancelAnimation();
            }
        }
    }

    private void showEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void hideEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (savedPostsQuery != null && savedPostsListener != null) {
            savedPostsQuery.removeEventListener(savedPostsListener);
        }
    }
}
