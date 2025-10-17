package com.example.bay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bay.databinding.ActivitySuccessfulForgetPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class SuccessfulForgetPasswordActivity extends AppCompatActivity {

    private ActivitySuccessfulForgetPasswordBinding binding;
    private FirebaseAuth mAuth;
    private String email;
    private boolean isEmailSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySuccessfulForgetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        getIntentData();
        setupClickListeners();

        if (email != null && !email.isEmpty()) {
            sendResetEmail();
        } else {
            Toast.makeText(this, "Email address not found", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void getIntentData() {
        email = getIntent().getStringExtra("email");
    }

    private void setupClickListeners() {
        binding.backButton.setOnClickListener(v -> navigateToLogin());
    }

    private void sendResetEmail() {
        if (isEmailSent) {
            return; // Prevent multiple sends
        }

        showLoading("កំពុងផ្ញើអ៊ីមែល...");

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideLoading();

                    if (task.isSuccessful()) {
                        handleEmailSuccess();
                    } else {
                        handleEmailFailure(task.getException());
                    }
                });
    }

    private void handleEmailSuccess() {
        isEmailSent = true;
        Toast.makeText(this, "✅ ផ្ញើអ៊ីមែលដើម្បីកំណត់ពាក្យសម្ងាត់ឡើងវិញដោយជោគជ័យ!", Toast.LENGTH_LONG).show();
    }

    private void handleEmailFailure(Exception exception) {
        String errorMessage = getFirebaseErrorMessage(exception);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private String getFirebaseErrorMessage(Exception exception) {
        if (exception == null) {
            return "🚫 មិនអាចផ្ញើអ៊ីមែលបានទេ។ សូមព្យាយាមម្តងទៀត!";
        }

        if (exception instanceof FirebaseAuthInvalidUserException) {
            return "🚫 រកមិនឃើញគណនីដែលមានអ៊ីមែលនេះទេ";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "🚫 ទម្រង់អ៊ីមែលមិនត្រឹមត្រូវទេ";
        } else if (exception.getMessage() != null && exception.getMessage().contains("network error")) {
            return "🚫 កំហុសបណ្តាញ។ សូមពិនិត្យមើលការតភ្ជាប់អ៊ីនធឺណិតរបស់អ្នក";
        } else {
            return "🚫 មិនអាចផ្ញើអ៊ីមែលកំណត់ឡើងវិញបានទេ: " + exception.getMessage();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, AuthenticationLogInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showLoading(String message) {
        binding.backButton.setEnabled(false);
        binding.loading.setVisibility(View.VISIBLE);

        if (binding.loadingText != null) {
            binding.loadingText.setText(message);
        }
    }

    private void hideLoading() {
        binding.backButton.setEnabled(true);
        binding.loading.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}