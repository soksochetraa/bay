package com.example.bay;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.example.bay.databinding.ActivityHomeBinding;
import com.example.bay.fragment.CommunityAccountFragment;
import com.example.bay.fragment.CommunityFragment;
import com.example.bay.fragment.HomeFragment;
import com.example.bay.fragment.MarketPlaceMainFragment;
import com.example.bay.fragment.MessageFragment;
import com.example.bay.fragment.PostDetailFragment;
import com.example.bay.CommunitySearchFragment;
import com.example.bay.service.ChatNotificationService;
import com.example.bay.util.FirebaseDBHelper;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private FirebaseUser currentUser;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private boolean isDrawerEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation, (v, insets) -> {
            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        LoadFragment(new HomeFragment());
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
                setDrawerEnabled(true);
            } else if (itemId == R.id.nav_community) {
                fragment = new CommunityFragment();
                setDrawerEnabled(false);
            } else if (itemId == R.id.nav_marketplace) {
                fragment = new MarketPlaceMainFragment();
                setDrawerEnabled(false);
            } else if (itemId == R.id.nav_message) {
                fragment = new MessageFragment();
                setDrawerEnabled(false);
            } else if (itemId == R.id.nav_profile) {
                if (currentUser != null) {
                    fragment = CommunityAccountFragment.newInstance(currentUser.getUid());
                } else {
                    fragment = new CommunityAccountFragment();
                }
                setDrawerEnabled(false);
            } else {
                return false;
            }

            LoadFragment(fragment);
            return true;
        });
    }


    private void setupDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_profile) {
                navigateToMyProfile();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    public void openDrawer() {
        if (drawerLayout != null && isDrawerEnabled) {
            Log.d("DrawerDebug", "Drawer opening - isDrawerEnabled: " + isDrawerEnabled);
            drawerLayout.openDrawer(GravityCompat.START);
        } else {
            Log.d("DrawerDebug", "Cannot open drawer - isDrawerEnabled: " + isDrawerEnabled);
        }
    }

    public void setDrawerEnabled(boolean enabled) {
        isDrawerEnabled = enabled;
        Log.d("DrawerDebug", "setDrawerEnabled: " + enabled);
        if (drawerLayout != null) {
            if (enabled) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        }
    }

    public void closeDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
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
        setDrawerEnabled(false);
    }

    public void navigateTo(int navItemId, Fragment fragment) {
        binding.bottomNavigation.setSelectedItemId(navItemId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
        setDrawerEnabled(navItemId == R.id.nav_home);
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
    protected void onResume() {
        super.onResume();
        setupNetworkListener();
        startChatService();
        setOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cleanupNetworkStatus();
        setOnlineStatus(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;

        if (getCurrentUserId() != null) {
            FirebaseDBHelper.getOnlineStatusRef(getCurrentUserId()).setValue(false);
        }

        stopService(new Intent(this, ChatNotificationService.class));
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);

            if (currentFragment instanceof HomeFragment) {
                setDrawerEnabled(true);
            } else {
                setDrawerEnabled(false);
            }

            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
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
        setDrawerEnabled(false);
    }

    public void loadPostDetail(String postId) {
        PostDetailFragment fragment = PostDetailFragment.newInstance(postId);
        LoadFragment(fragment);
        hideBottomNavigation();
        setDrawerEnabled(false);
    }

    public void loadFullUserSearch(String query) {
        CommunitySearchFragment fragment = new CommunitySearchFragment();
        Bundle args = new Bundle();
        args.putString("initial_search", query);
        fragment.setArguments(args);
        LoadFragment(fragment);
        hideBottomNavigation();
        setDrawerEnabled(false);
    }

    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("Farm Management App v1.0")
                .setPositiveButton("OK", null)
                .show();
    }

    private void handleLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (getCurrentUserId() != null) {
                        FirebaseDBHelper.getOnlineStatusRef(getCurrentUserId()).setValue(false);
                    }
                    FirebaseAuth.getInstance().signOut();
                    stopService(new Intent(this, ChatNotificationService.class));
                    startActivity(new Intent(this, AuthenticationLogInActivity.class));
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void showNetworkConnectedMessage() {
        runOnUiThread(() -> {
            Snackbar snackbar = Snackbar.make(
                    binding.getRoot(),
                    "អ៊ីនធឺណិតត្រលប់មកវិញ",
                    Snackbar.LENGTH_SHORT
            );
            snackbar.setBackgroundTint(getResources().getColor(R.color.verified_color));
            snackbar.show();
        });
    }

    public void showNetworkDisconnectedMessage() {
        runOnUiThread(() -> {
            Snackbar snackbar = Snackbar.make(
                    binding.getRoot(),
                    "អ៊ីនធឺណិតបាត់",
                    Snackbar.LENGTH_LONG
            );
            snackbar.setBackgroundTint(getResources().getColor(R.color.red));
            snackbar.show();
        });
    }

    private void setupNetworkListener() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (isConnected) {
                if (getCurrentUserId() != null) {
                    FirebaseDBHelper.getOnlineStatusRef(getCurrentUserId()).setValue(true);
                }
            }
        }
    }

    private void startChatService() {
        if (getCurrentUserId() != null) {
            Intent serviceIntent = new Intent(this, ChatNotificationService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    private void cleanupNetworkStatus() {
        if (getCurrentUserId() != null) {
            FirebaseDBHelper.getOnlineStatusRef(getCurrentUserId()).onDisconnect().setValue(false);
        }
    }

    private void setOnlineStatus(boolean isOnline) {
        if (getCurrentUserId() != null) {
            if (isOnline) {
                FirebaseDBHelper.getOnlineStatusRef(getCurrentUserId()).setValue(true);
            } else {
                FirebaseDBHelper.getOnlineStatusRef(getCurrentUserId()).setValue(false);
            }
        }
    }

    private void createAppDirectories() {
        try {
            java.io.File cacheDir = new java.io.File(getCacheDir(), "chat_images");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            java.io.File externalDir = new java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "Bay_Chat");
            if (!externalDir.exists()) {
                externalDir.mkdirs();
            }
        } catch (Exception e) {
            Log.e("HomeActivity", "Error creating directories: " + e.getMessage());
        }
    }
}