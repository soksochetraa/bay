package com.example.bay;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import com.example.bay.BaseActivity;

import com.example.bay.databinding.ActivitySplashScreenOneBinding;

public class SplashScreenOneActivity extends BaseActivity {

    ActivitySplashScreenOneBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySplashScreenOneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.nextButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SplashScreenTwoActivity.class));
            finish();
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}