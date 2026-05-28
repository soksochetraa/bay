package com.example.bay;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import com.example.bay.BaseActivity;

import com.example.bay.databinding.ActivityAuthenticationPhoneVerifyBinding;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.example.bay.util.VerificationHelper;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class AuthenticationPhoneVerifyActivity extends BaseActivity {

    private ActivityAuthenticationPhoneVerifyBinding binding;
    private FirebaseAuth auth;
    private String phoneNumber = "";
    private String verificationId = "";
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private boolean isVerificationInProgress = false;
    private EditText[] otpInputs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAuthenticationPhoneVerifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        String phoneData = getIntent().getStringExtra("phone_number");
        if (phoneData == null || phoneData.trim().isEmpty()) {
            finish();
            return;
        }
        phoneNumber = phoneData.replaceAll("\\s+", "");

        setupOtpInputs();
        setupListeners();
        setupEnterKeyListeners();

        if (!phoneNumber.isEmpty()) {
            sendVerificationCode(phoneNumber);
        }
    }

    private void setupListeners() {
        binding.button.setOnClickListener(v -> {
            Intent intent = new Intent(this, AuthenticationPhoneActivity.class);
            intent.putExtra("phone_number", phoneNumber);
            startActivity(intent);
            finish();
        });

        binding.resend.setOnClickListener(v -> {
            if (isVerificationInProgress) return;
            if (resendToken != null) resendVerificationCode();
        });

        binding.nextButton.setOnClickListener(v -> handleVerifyCode());
    }

    private void setupEnterKeyListeners() {
        otpInputs = new EditText[]{
                binding.etDigitOne, binding.etDigitTwo, binding.etDigitThree,
                binding.etDigitFour, binding.etDigitFive, binding.etDigitSix
        };

        for (int i = 0; i < otpInputs.length; i++) {
            final int index = i;
            otpInputs[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (index == otpInputs.length - 1) handleVerifyCode();
                    return true;
                }
                return false;
            });
        }
    }

    private void handleVerifyCode() {
        if (isVerificationInProgress) return;

        String code = getOtpInput();
        if (code.length() != 6) return;

        verifyCode(code);
    }

    private void sendVerificationCode(String phone) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendVerificationCode() {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .setForceResendingToken(resendToken)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {
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
                }

                @Override
                public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = s;
                    resendToken = token;
                }
            };

    private void verifyCode(String code) {
        if (verificationId.isEmpty() || isVerificationInProgress) return;

        isVerificationInProgress = true;
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            isVerificationInProgress = false;

            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();

                if (user != null) {

                    DatabaseReference ref = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(user.getUid());

                    ref.child("phoneVerified").setValue(true)
                            .addOnCompleteListener(updateTask -> {
                                // Check and update userVerified if both email & phone are verified
                                VerificationHelper.checkAndUpdateUserVerified(user.getUid(), isFullyVerified -> {
                                    checkUserProfileCompletion(user);
                                });
                            });

                } else {
                    goToCompleteProfile(null);
                }
            }
        });
    }

    private void checkUserProfileCompletion(FirebaseUser user) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    goToCompleteProfile(null);
                    return;
                }

                Boolean profileCompleted = snapshot.child("profileCompleted")
                        .getValue(Boolean.class);

                if (profileCompleted != null && profileCompleted) {
                    goToHome();
                } else {
                    goToCompleteProfile(snapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                goToCompleteProfile(null);
            }
        });
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToCompleteProfile(DataSnapshot snapshot) {
        Intent intent = new Intent(this, CompleteProfileActivity.class);

        intent.putExtra("phone_number", phoneNumber);
        intent.putExtra("openFrom", "openFromPhoneNumber");

        if (snapshot != null && snapshot.exists()) {

            String firstName = snapshot.child("first_name").getValue(String.class);
            String lastName = snapshot.child("last_name").getValue(String.class);
            String email = snapshot.child("email").getValue(String.class);

            if (firstName != null) {
                intent.putExtra("firstName", firstName);
            }

            if (lastName != null) {
                intent.putExtra("lastName", lastName);
            }

            if (email != null) {
                intent.putExtra("email", email);
            }
        }

        startActivity(intent);
        finish();
    }

    private void setupOtpInputs() {
        EditText[] fields = {
                binding.etDigitOne, binding.etDigitTwo, binding.etDigitThree,
                binding.etDigitFour, binding.etDigitFive, binding.etDigitSix
        };

        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            fields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < fields.length - 1) {
                        fields[index + 1].requestFocus();
                    } else if (s.length() == 0 && index > 0) {
                        fields[index - 1].requestFocus();
                    }

                    if (getOtpInput().length() == 6 && index == fields.length - 1) {
                        handleVerifyCode();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
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

        EditText[] fields = {
                binding.etDigitOne, binding.etDigitTwo, binding.etDigitThree,
                binding.etDigitFour, binding.etDigitFive, binding.etDigitSix
        };

        for (int i = 0; i < 6; i++) {
            fields[i].setText(String.valueOf(code.charAt(i)));
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null && view instanceof EditText) {
            int[] scrcoords = new int[2];
            view.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + view.getLeft() - scrcoords[0];
            float y = ev.getRawY() + view.getTop() - scrcoords[1];

            if (x < view.getLeft() || x > view.getRight() ||
                    y < view.getTop() || y > view.getBottom()) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}