package com.example.bay;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bay.databinding.ActivityForgetPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class ForgetPasswordActivity extends AppCompatActivity {

    private ActivityForgetPasswordBinding binding;
    private FirebaseAuth mAuth;
    private boolean isResetInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityForgetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        setupClickListeners();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupClickListeners() {
        binding.nextButton.setOnClickListener(v -> handleResetPassword());
        binding.button.setOnClickListener(v -> navigateToLogin());
    }

    private void handleResetPassword() {
        if (isResetInProgress) {
            return;
        }

        String email = binding.etEmail.getText().toString().trim();

        if (!isValidEmail(email)) {
            return;
        }

        resetPassword(email);
    }

    private boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required!");
            binding.etEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Please enter a valid email address!");
            binding.etEmail.requestFocus();
            return false;
        }

        binding.etEmail.setError(null);
        return true;
    }

    private void resetPassword(String email) {
        showLoading();
        isResetInProgress = true;

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideLoading();
                    isResetInProgress = false;

                    if (task.isSuccessful()) {
                        handleResetSuccess(email);
                    } else {
                        handleResetFailure(task.getException());
                    }
                });
    }

    private void handleResetSuccess(String email) {
        Toast.makeText(this, "Password reset email sent successfully!", Toast.LENGTH_SHORT).show();
        navigateToSuccess(email);
    }

    private void handleResetFailure(Exception exception) {
        String errorMessage = getFirebaseErrorMessage(exception);
        showError(errorMessage);
    }

    private String getFirebaseErrorMessage(Exception exception) {
        if (exception == null) {
            return "An unknown error occurred";
        }

        if (exception instanceof FirebaseAuthInvalidUserException) {
            return "No account found with this email address";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "Invalid email address format";
        } else if (exception.getMessage() != null && exception.getMessage().contains("network error")) {
            return "Network error. Please check your internet connection";
        } else {
            return "Failed to send reset email: " + exception.getMessage();
        }
    }

    private void navigateToSuccess(String email) {
        Intent intent = new Intent(this, SuccessfulForgetPasswordActivity.class);
        intent.putExtra("email", email);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, AuthenticationLogInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showLoading() {
        binding.nextButton.setEnabled(false);
        binding.button.setEnabled(false);
        binding.loading.setVisibility(View.VISIBLE);
        if (binding.loadingText != null) {
            binding.loadingText.setText("Sending reset email...");
        }
    }

    private void hideLoading() {
        binding.nextButton.setEnabled(true);
        binding.button.setEnabled(true);
        binding.loading.setVisibility(View.GONE);
    }

    private void showError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

        // Clear the error after a delay
        binding.getRoot().postDelayed(() -> {
            binding.etEmail.setError(null);
        }, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}