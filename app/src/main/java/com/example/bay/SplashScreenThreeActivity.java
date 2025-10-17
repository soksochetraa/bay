package com.example.bay;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bay.databinding.ActivitySplashScreenThreeBinding;

public class SplashScreenThreeActivity extends AppCompatActivity {

    ActivitySplashScreenThreeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySplashScreenThreeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.nextButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SplashScreenFourActivity.class));
            finish();
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}