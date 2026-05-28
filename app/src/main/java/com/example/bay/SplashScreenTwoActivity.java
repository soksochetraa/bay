package com.example.bay;

import android.content.Intent;
import android.os.Bundle;

import com.example.bay.BaseActivity;
import androidx.activity.EdgeToEdge;
import com.example.bay.databinding.ActivitySplashScreenTwoBinding;

public class SplashScreenTwoActivity extends BaseActivity {

    ActivitySplashScreenTwoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        binding = ActivitySplashScreenTwoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.nextButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SplashScreenThreeActivity.class));
            finish();
        });

        binding.button.setOnClickListener(v->{
            startActivity(new Intent(this, AuthenticationLogInActivity.class));
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
