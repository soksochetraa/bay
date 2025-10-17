package com.example.bay;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentUser != null) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            } else {
                startActivity(new Intent(this, SplashScreenOneActivity.class));
                finish();
            }
        }, 1500);
    }
}