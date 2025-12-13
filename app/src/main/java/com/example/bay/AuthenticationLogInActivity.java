package com.example.bay;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bay.databinding.ActivityAuthenticationLogInBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class AuthenticationLogInActivity extends AppCompatActivity {

    private ActivityAuthenticationLogInBinding binding;
    private FirebaseAuth mAuth;
    private int topMarginInPx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAuthenticationLogInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int topMarginInPx = (int) (80 * binding.getRoot().getResources().getDisplayMetrics().density);

        mAuth = FirebaseAuth.getInstance();
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.register.setOnClickListener(v -> {
            startActivity(new Intent(this, AuthenticationRegisterActivity.class));
            finish();
        });

        binding.forgetPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgetPasswordActivity.class));
            finish();
        });

        binding.btnPhone.setOnClickListener(v -> {
            Intent intent = new Intent(this, AuthenticationPhoneActivity.class);
            intent.putExtra("openFrom", "openFromLogIn");
            startActivity(intent);
            finish();
        });

        binding.loginButton.setOnClickListener(v -> handleEmailLogin());
    }

    private void handleEmailLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        binding.tvValidate.setVisibility(View.GONE);

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required!");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Invalid email!");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password required!");
            return;
        }

        showLoading();
        loginWithEmail(email, password);
    }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    hideLoading();

                    binding.loading.postDelayed(() -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        } else {
                            binding.tvValidate.setText("អុីម៉ែល ឬ លេខសម្ងាត់មិនត្រឹមត្រូវ!");
                            binding.tvValidate.setVisibility(View.VISIBLE);
                            Log.d("signInWithEmail:failure",
                                    Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()));
                        }
                    }, 300);
                });
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
