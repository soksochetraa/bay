package com.example.bay;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bay.databinding.ActivityAuthenticationPhoneVerifyBinding;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class AuthenticationPhoneVerifyActivity extends AppCompatActivity {

    private ActivityAuthenticationPhoneVerifyBinding binding;
    private FirebaseAuth auth;
    private String phoneNumber = "";
    private String verificationId = "";
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private boolean isVerificationInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAuthenticationPhoneVerifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        getIntentData();
        setupViews();
        setupListeners();

        if (!phoneNumber.isEmpty()) {
            sendVerificationCode(phoneNumber);
        }
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
    }

    private void getIntentData() {
        phoneNumber = getIntent().getStringExtra("phone_number");
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number is missing!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupViews() {
        setupOtpInputs();
    }

    private void setupListeners() {
        binding.button.setOnClickListener(v -> handleBackAction());
        binding.resend.setOnClickListener(v -> handleResendCode());
        binding.nextButton.setOnClickListener(v -> handleVerifyCode());
    }


    private void handleBackAction() {
        Intent intent = new Intent(this, AuthenticationPhoneActivity.class);
        intent.putExtra("phone_number", phoneNumber);
        startActivity(intent);
        finish();
    }

    private void handleResendCode() {
        if (isVerificationInProgress) {
            Toast.makeText(this, "Verification in progress. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (resendToken != null) {
            resendVerificationCode();
        } else {
            Toast.makeText(this, "Please wait before resending OTP.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleVerifyCode() {
        if (isVerificationInProgress) {
            return;
        }

        String code = getOtpInput();
        if (code.length() != 6) {
            Toast.makeText(this, "Please enter all 6 digits.", Toast.LENGTH_SHORT).show();
            return;
        }

        verifyCode(code);
    }

    private void sendVerificationCode(String phone) {

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(createVerificationCallbacks())
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendVerificationCode() {
        showLoading("កំពុងផ្ញើេលេខកូដ...");

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(createVerificationCallbacks())
                .setForceResendingToken(resendToken)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks createVerificationCallbacks() {
        return new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                hideLoading();
                String code = credential.getSmsCode();
                if (code != null) {
                    setOtpFields(code);
                    verifyCode(code);
                } else {
                    signInWithPhoneAuthCredential(credential);
                }
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                hideLoading();
                Log.e("PhoneVerify", "Verification failed: " + e.getMessage());
                String errorMessage = getFirebaseErrorMessage(e);
                Toast.makeText(AuthenticationPhoneVerifyActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(s, token);
                hideLoading();
                verificationId = s;
                resendToken = token;
                Toast.makeText(AuthenticationPhoneVerifyActivity.this,
                        "OTP sent to " + phoneNumber, Toast.LENGTH_SHORT).show();

                binding.resend.setEnabled(false);
                binding.resend.postDelayed(() -> {
                    binding.resend.setEnabled(true);
                }, 30000);
            }
        };
    }

    private void verifyCode(String code) {
        if (verificationId.isEmpty() || isVerificationInProgress) {
            return;
        }

        showLoading("Verifying OTP...");
        isVerificationInProgress = true;

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    hideLoading();
                    isVerificationInProgress = false;

                    if (task.isSuccessful()) {
                        handleVerificationSuccess();
                    } else {
                        handleVerificationFailure(task.getException());
                    }
                });
    }

    private void handleVerificationSuccess() {
        Toast.makeText(this, "Phone number verified successfully!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, CompleteProfileActivity.class);
        intent.putExtra("phone_number", phoneNumber);
        intent.putExtra("openFrom", "openFromPhoneNumber");
        startActivity(intent);
        finish();
    }

    private void handleVerificationFailure(Exception exception) {
        Log.e("PhoneVerify", "Sign in failed: " + exception.getMessage());
        Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();

        clearOtpFields();
        binding.etDigitOne.requestFocus();
    }

    private String getFirebaseErrorMessage(FirebaseException e) {
        String errorMessage = e.getMessage();
        if (errorMessage == null) {
            return "Verification failed. Please try again.";
        }

        if (errorMessage.contains("quota")) {
            return "SMS quota exceeded. Please try again later.";
        } else if (errorMessage.contains("invalid-phone-number")) {
            return "Invalid phone number format.";
        } else if (errorMessage.contains("too-many-requests")) {
            return "Too many attempts. Please try again later.";
        } else {
            return "Verification failed: " + errorMessage;
        }
    }

    private void setupOtpInputs() {
        EditText[] otpInputs = {
                binding.etDigitOne,
                binding.etDigitTwo,
                binding.etDigitThree,
                binding.etDigitFour,
                binding.etDigitFive,
                binding.etDigitSix
        };

        for (int i = 0; i < otpInputs.length; i++) {
            final int currentIndex = i;
            otpInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && currentIndex < otpInputs.length - 1) {
                        otpInputs[currentIndex + 1].requestFocus();
                    } else if (s.length() == 0 && currentIndex > 0) {
                        otpInputs[currentIndex - 1].requestFocus();
                    }

                    if (getOtpInput().length() == 6 && currentIndex == otpInputs.length - 1) {
                        binding.getRoot().postDelayed(() -> {
                            handleVerifyCode();
                        }, 300);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private String getOtpInput() {
        return binding.etDigitOne.getText().toString().trim() +
                binding.etDigitTwo.getText().toString().trim() +
                binding.etDigitThree.getText().toString().trim() +
                binding.etDigitFour.getText().toString().trim() +
                binding.etDigitFive.getText().toString().trim() +
                binding.etDigitSix.getText().toString().trim();
    }

    private void setOtpFields(String code) {
        if (code.length() != 6) return;

        EditText[] otpInputs = {
                binding.etDigitOne,
                binding.etDigitTwo,
                binding.etDigitThree,
                binding.etDigitFour,
                binding.etDigitFive,
                binding.etDigitSix
        };

        for (int i = 0; i < 6; i++) {
            otpInputs[i].setText(String.valueOf(code.charAt(i)));
        }
    }

    private void clearOtpFields() {
        EditText[] otpInputs = {
                binding.etDigitOne,
                binding.etDigitTwo,
                binding.etDigitThree,
                binding.etDigitFour,
                binding.etDigitFive,
                binding.etDigitSix
        };

        for (EditText otpInput : otpInputs) {
            otpInput.setText("");
        }
    }

    private void showLoading(String message) {
        binding.nextButton.setEnabled(false);
        binding.resend.setEnabled(false);
        binding.loading.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        binding.nextButton.setEnabled(true);
        binding.resend.setEnabled(true);
        binding.loading.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}