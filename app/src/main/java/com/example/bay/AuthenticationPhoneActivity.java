package com.example.bay;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bay.databinding.ActivityAuthenticationPhoneBinding;

public class AuthenticationPhoneActivity extends AppCompatActivity {

    private ActivityAuthenticationPhoneBinding binding;
    private final String PREFIX = "+855 ";
    private String openFrom = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAuthenticationPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeData();
        setupViews();
        setupListeners();
    }

    private void initializeData() {
        openFrom = getIntent().getStringExtra("openFrom");
        if (openFrom == null) openFrom = "";
    }

    private void setupViews() {
        setupPhoneEditText();
        binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
    }

    private void setupListeners() {
        binding.etPhoneNumber.setOnClickListener(v -> handlePhoneNumberClick());
        binding.button.setOnClickListener(v -> handleBackAction());
        binding.nextButton.setOnClickListener(v -> handleNextAction());
    }

    private void handlePhoneNumberClick() {
        if (binding.etPhoneNumber.getText().toString().isEmpty()) {
            binding.etPhoneNumber.setText(PREFIX);
        }
        binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
    }

    private void handleBackAction() {
        if (openFrom.equals("openFromLogIn")) {
            navigateToActivity(AuthenticationLogInActivity.class);
        } else if (openFrom.equals("openFromRegister")) {
            navigateToActivity(AuthenticationRegisterActivity.class);
        } else {
            onBackPressed();
        }
    }

    private void handleNextAction() {
        String phoneNumber = binding.etPhoneNumber.getText().toString().trim();

        if (!isValidPhoneNumber(phoneNumber)) {
            return;
        }

        showLoading();
        processPhoneNumberVerification(phoneNumber);
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty() || phoneNumber.equals(PREFIX)) {
            showToast("សូមបញ្ចូលលេខទូរស័ព្ទ!");
            return false;
        }

        String numberPart = phoneNumber.replace(PREFIX, "").trim();
        if (!numberPart.matches("^[1-9][0-9]{7,9}$")) {
            showToast("លេខទូរស័ព្ទមិនត្រឹមត្រូវទេ!");
            return false;
        }

        return true;
    }

    private void processPhoneNumberVerification(String phoneNumber) {

        binding.getRoot().postDelayed(() -> {
            hideLoading();
            showToast("កំពុងផ្ញើ OTP ទៅកាន់ " + phoneNumber);
            navigateToVerification(phoneNumber);
        }, 2500);
    }

    private void navigateToVerification(String phoneNumber) {
        Intent intent = new Intent(this, AuthenticationPhoneVerifyActivity.class);
        intent.putExtra("phone_number", phoneNumber);
        intent.putExtra("openFrom", openFrom);
        startActivity(intent);
    }

    private void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
        finish();
    }

    private void setupPhoneEditText() {
        binding.etPhoneNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !binding.etPhoneNumber.getText().toString().startsWith(PREFIX)) {
                binding.etPhoneNumber.setText(PREFIX);
                binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
            }
        });

        binding.etPhoneNumber.addTextChangedListener(new TextWatcher() {
            private String previous = PREFIX;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previous = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String current = s.toString();

                if (!current.startsWith(PREFIX)) {
                    binding.etPhoneNumber.setText(PREFIX);
                    binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
                    return;
                }

                if (current.length() > PREFIX.length()) {
                    char firstDigit = current.charAt(PREFIX.length());
                    if (firstDigit == '0') {
                        showToast("មិនអនុញ្ញាតឱ្យប្រើលេខ 0 បន្ទាប់ពី +855");
                        binding.etPhoneNumber.setText(previous);
                        binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
                    }
                }
            }
        });

        binding.etPhoneNumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
    }

    private void showLoading() {
        binding.nextButton.setEnabled(false);
        binding.loading.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        binding.nextButton.setEnabled(true);
        binding.loading.setVisibility(View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}