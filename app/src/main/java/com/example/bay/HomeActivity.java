package com.example.bay;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.bay.databinding.ActivityHomeBinding;
import com.example.bay.fragment.CommunityAccountFragment;
import com.example.bay.fragment.CommunityFragment;
import com.example.bay.fragment.HomeFragment;
import com.example.bay.fragment.MarketPlaceFragment;
import com.example.bay.fragment.MarketPlaceMainFragment;
import com.example.bay.fragment.MessageFragment;
import com.example.bay.fragment.PostDetailFragment;
import com.example.bay.CommunitySearchFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        LoadFragment(new HomeFragment());
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                LoadFragment(new HomeFragment());
            } else if (itemId == R.id.nav_community) {
                LoadFragment(new CommunityFragment());
            } else if (itemId == R.id.nav_marketplace) {
                LoadFragment(new MarketPlaceMainFragment());
            } else if (itemId == R.id.nav_message) {
                LoadFragment(new MessageFragment());
            } else if (itemId == R.id.nav_profile) {
                if (currentUser != null) {
                    CommunityAccountFragment fragment = CommunityAccountFragment.newInstance(currentUser.getUid());
                    LoadFragment(fragment);
                } else {
                    LoadFragment(new CommunityAccountFragment());
                }
            } else {
                return false;
            }
            return true;
        });
    }

    public void LoadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToMyProfile() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
        CommunityAccountFragment fragment = CommunityAccountFragment.newInstance(getCurrentUserId());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateTo(int navItemId, Fragment fragment) {
        binding.bottomNavigation.setSelectedItemId(navItemId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void setBottomNavigationVisible(boolean visible) {
        if (binding == null) return;
        binding.bottomNavigation.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void hideBottomNavigation() {
        setBottomNavigationVisible(false);
    }

    public void showBottomNavigation() {
        setBottomNavigationVisible(true);
    }

    public void showLoading() {
        binding.loading.setVisibility(View.VISIBLE);
    }

    public void hideLoading() {
        binding.loading.postDelayed(() -> {
            binding.loading.setVisibility(View.GONE);
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    public FirebaseUser getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getUid() : null;
    }

    public void loadUserProfile(String userId) {
        CommunityAccountFragment fragment = CommunityAccountFragment.newInstance(userId);
        LoadFragment(fragment);
        hideBottomNavigation();
    }

    public void loadPostDetail(String postId) {
        PostDetailFragment fragment = PostDetailFragment.newInstance(postId);
        LoadFragment(fragment);
        hideBottomNavigation();
    }

//    public void openChatWithUser(String userId, String userName) {
//        MessageFragment fragment = MessageFragment.newInstance(userId, userName);
//        LoadFragment(fragment);
//        hideBottomNavigation();
//    }

    public void loadFullUserSearch(String query) {
        CommunitySearchFragment fragment = new CommunitySearchFragment();
        Bundle args = new Bundle();
        args.putString("initial_search", query);
        fragment.setArguments(args);
        LoadFragment(fragment);
        hideBottomNavigation();
    }
}