package com.example.bay;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import com.example.bay.BaseActivity;

import com.example.bay.databinding.ActivityAuthenticationPhoneBinding;
import com.example.bay.repository.UserRepository;

public class AuthenticationPhoneActivity extends BaseActivity {

    private ActivityAuthenticationPhoneBinding binding;
    private final String PREFIX = "+855 ";
    private String openFrom = "";
    UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAuthenticationPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeData();
        setupViews();
        setupListeners();
        setupEnterKeyListener();
    }

    private void initializeData() {
        openFrom = getIntent().getStringExtra("openFrom");
        if (openFrom == null) openFrom = "";

        userRepository = new UserRepository();
    }

    private void setupViews() {
        setupPhoneEditText();
        binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
    }

    private void setupListeners() {
        binding.etPhoneNumber.setOnClickListener(v -> {
            handlePhoneNumberClick();
        });

        binding.button.setOnClickListener(v -> {
            hideKeyboard();
            handleBackAction();
        });

        binding.nextButton.setOnClickListener(v -> {
            hideKeyboard();
            handleNextAction();
        });
    }
    private void setupEnterKeyListener() {
        binding.etPhoneNumber.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                hideKeyboard();
                handleNextAction();
                return true;
            }
            return false;
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null && (ev.getAction() == android.view.MotionEvent.ACTION_UP ||
                ev.getAction() == android.view.MotionEvent.ACTION_MOVE) &&
                view instanceof android.widget.EditText && !view.getClass().getName().startsWith("android.webkit.")) {
            int[] scrcoords = new int[2];
            view.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + view.getLeft() - scrcoords[0];
            float y = ev.getRawY() + view.getTop() - scrcoords[1];

            if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom()) {
                hideKeyboard();
            }
        }
        return super.dispatchTouchEvent(ev);
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

            // Optional: Hide keyboard when losing focus
            if (!hasFocus) {
                hideKeyboard();
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

                if (current.length() == 14) {
                    binding.getRoot().postDelayed(() -> {
                        hideKeyboard();
                        handleNextAction();
                    }, 300);
                }
            }
        });

        binding.etPhoneNumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
    }

    private void showLoading() {
        binding.nextButton.setEnabled(false);
        binding.etPhoneNumber.setEnabled(false);
        binding.loading.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        binding.nextButton.setEnabled(true);
        binding.etPhoneNumber.setEnabled(true);
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