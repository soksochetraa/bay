package com.example.bay;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bay.databinding.ActivityAuthenticationRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;

public class AuthenticationRegisterActivity extends AppCompatActivity {

    private ActivityAuthenticationRegisterBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAuthenticationRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.login.setOnClickListener(v -> navigateToLogin());
        binding.btnPhone.setOnClickListener(v -> navigateToPhoneRegister());
        binding.registerButton.setOnClickListener(v -> registerUser());
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, AuthenticationLogInActivity.class));
        finish();
    }

    private void navigateToPhoneRegister() {
        Intent intent = new Intent(this, AuthenticationPhoneActivity.class);
        intent.putExtra("openFrom", "openFromRegister");
        startActivity(intent);
        finish();
    }

    private void registerUser() {
        String username = binding.etUsername.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (!validateInputs(username, email, password, confirmPassword)) {
            return;
        }

        showLoading();
        proceedToCompleteProfile(username, email, password);
    }

    private boolean validateInputs(String username, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(username)) {
            binding.etUsername.setError("Username is required!");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required!");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required!");
            return false;
        }
        if (password.length() < 8) {
            binding.etPassword.setError("Password must be at least 8 characters!");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match!");
            return false;
        }
        return true;
    }

    private void proceedToCompleteProfile(String username, String email, String password) {
        Intent intent = new Intent(this, CompleteProfileActivity.class);
        intent.putExtra("openFrom", "openFromRegister");
        intent.putExtra("username", username);
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        startActivity(intent);
        finish();
    }

    private void showLoading() {
        binding.loading.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        binding.loading.postDelayed(() -> {
            binding.loading.setVisibility(View.GONE);
        }, 1500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}