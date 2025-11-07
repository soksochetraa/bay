package com.example.bay;

import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.bay.databinding.ActivityHomeBinding;
import com.example.bay.fragment.AccountFragment;
import com.example.bay.fragment.CommunityFragment;
import com.example.bay.fragment.HomeFragment;
import com.example.bay.fragment.MarketPlaceFragment;
import com.example.bay.fragment.MessageFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load initial fragment
        LoadFragment(new HomeFragment());
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                LoadFragment(new HomeFragment());
            } else if (itemId == R.id.nav_community) {
                LoadFragment(new CommunityFragment());
            } else if (itemId == R.id.nav_marketplace) {
                LoadFragment(new MarketPlaceFragment());
            } else if (itemId == R.id.nav_message) {
                LoadFragment(new MessageFragment());
            } else if (itemId == R.id.nav_profile) {
                LoadFragment(new AccountFragment());
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
                .commit();
    }

    public void navigateTo(int navItemId, Fragment fragment) {
        binding.bottomNavigation.setSelectedItemId(navItemId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }


    public void hideBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setVisibility(View.GONE);
        }
    }

    public void showBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setVisibility(View.VISIBLE);
        }
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
}