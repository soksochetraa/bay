package com.example.bay;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.bay.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhoneNumberVerifyingFragment extends Fragment {

    private EditText etDigitOne, etDigitTwo, etDigitThree, etDigitFour, etDigitFive, etDigitSix;
    private TextView tvResend, textView6;
    private MaterialButton btnVerify;
    private Button btnBack;

    private String phoneNumber = "";
    private String verificationId = "";
    private PhoneAuthProvider.ForceResendingToken resendingToken;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private DatabaseReference userRef;

    private CountDownTimer countDownTimer;
    private boolean isResendEnabled = false;
    private final long RESEND_TIME = 60000; // 60 seconds

    public PhoneNumberVerifyingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_phone_number_verifying, container, false);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        userRef = databaseReference.child("Users");

        // Initialize views
        initViews(view);

        // Get phone number from arguments
        if (getArguments() != null) {
            phoneNumber = getArguments().getString("phone_number", "");
            if (!phoneNumber.isEmpty()) {
                if (currentUser != null) {
                    // Log current user info
                    Log.d("PhoneVerification", "Current User ID: " + currentUser.getUid());
                    Log.d("PhoneVerification", "Current User Email: " + currentUser.getEmail());
                    Log.d("PhoneVerification", "Phone to verify: " + phoneNumber);

                    // Check if phone number is already authenticated in Firebase Auth
                    checkIfPhoneAuthenticatedInFirebase();
                } else {
                    Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                }
            } else {
                Toast.makeText(requireContext(), "No phone number received", Toast.LENGTH_SHORT).show();
            }
        }

        setupListeners();
        setupOTPListeners();

        return view;
    }

    private void initViews(View view) {
        etDigitOne = view.findViewById(R.id.etDigitOne);
        etDigitTwo = view.findViewById(R.id.etDigitTwo);
        etDigitThree = view.findViewById(R.id.etDigitThree);
        etDigitFour = view.findViewById(R.id.etDigitFour);
        etDigitFive = view.findViewById(R.id.etDigitFive);
        etDigitSix = view.findViewById(R.id.etDigitSix);

        tvResend = view.findViewById(R.id.resend);
        textView6 = view.findViewById(R.id.textView6);
        btnVerify = view.findViewById(R.id.nextButton);
        btnBack = view.findViewById(R.id.button);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnVerify.setOnClickListener(v -> {
            String otp = getOTP();
            if (otp.length() == 6) {
                verifyOTP(otp);
            } else {
                Toast.makeText(requireContext(), "Please enter 6-digit OTP", Toast.LENGTH_SHORT).show();
            }
        });

        tvResend.setOnClickListener(v -> {
            if (isResendEnabled) {
                resendOTP();
            }
        });
    }

    private void setupOTPListeners() {
        // Auto-focus between OTP fields
        TextWatcher otpTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    moveToNextField();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etDigitOne.addTextChangedListener(otpTextWatcher);
        etDigitTwo.addTextChangedListener(otpTextWatcher);
        etDigitThree.addTextChangedListener(otpTextWatcher);
        etDigitFour.addTextChangedListener(otpTextWatcher);
        etDigitFive.addTextChangedListener(otpTextWatcher);
        etDigitSix.addTextChangedListener(otpTextWatcher);

        // Handle backspace
        etDigitOne.setOnKeyListener((v, keyCode, event) -> handleBackspace(keyCode, event, null, etDigitOne));
        etDigitTwo.setOnKeyListener((v, keyCode, event) -> handleBackspace(keyCode, event, etDigitOne, etDigitTwo));
        etDigitThree.setOnKeyListener((v, keyCode, event) -> handleBackspace(keyCode, event, etDigitTwo, etDigitThree));
        etDigitFour.setOnKeyListener((v, keyCode, event) -> handleBackspace(keyCode, event, etDigitThree, etDigitFour));
        etDigitFive.setOnKeyListener((v, keyCode, event) -> handleBackspace(keyCode, event, etDigitFour, etDigitFive));
        etDigitSix.setOnKeyListener((v, keyCode, event) -> handleBackspace(keyCode, event, etDigitFive, etDigitSix));
    }

    private boolean handleBackspace(int keyCode, android.view.KeyEvent event, EditText previousField, EditText currentField) {
        if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
            if (currentField.getText().toString().isEmpty() && previousField != null) {
                previousField.requestFocus();
                previousField.setText("");
            }
            return true;
        }
        return false;
    }

    private void moveToNextField() {
        if (etDigitOne.getText().length() == 1) {
            etDigitTwo.requestFocus();
        }
        if (etDigitTwo.getText().length() == 1) {
            etDigitThree.requestFocus();
        }
        if (etDigitThree.getText().length() == 1) {
            etDigitFour.requestFocus();
        }
        if (etDigitFour.getText().length() == 1) {
            etDigitFive.requestFocus();
        }
        if (etDigitFive.getText().length() == 1) {
            etDigitSix.requestFocus();
        }
    }

    private String getOTP() {
        return etDigitOne.getText().toString() +
                etDigitTwo.getText().toString() +
                etDigitThree.getText().toString() +
                etDigitFour.getText().toString() +
                etDigitFive.getText().toString() +
                etDigitSix.getText().toString();
    }

    private void checkIfPhoneAuthenticatedInFirebase() {
        Log.d("PhoneVerification", "Checking if phone is already authenticated in Firebase");

        // Check if current user already has phone authentication provider
        if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty()) {
            Log.d("PhoneVerification", "User already has phone authenticated: " + currentUser.getPhoneNumber());

            if (currentUser.getPhoneNumber().equals(phoneNumber)) {
                // User already has this phone number authenticated
                Toast.makeText(requireContext(),
                        "This phone number is already verified for your account",
                        Toast.LENGTH_LONG).show();
                requireActivity().onBackPressed();
                return;
            } else {
                // User has a different phone number authenticated
                Toast.makeText(requireContext(),
                        "You already have a different phone number verified: " + currentUser.getPhoneNumber(),
                        Toast.LENGTH_LONG).show();
                requireActivity().onBackPressed();
                return;
            }
        }

        // If no phone authenticated, continue to check database
        checkIfPhoneNumberExistsInDatabase();
    }

    private void checkIfPhoneNumberExistsInDatabase() {
        Log.d("PhoneVerification", "Checking if phone exists in database: " + phoneNumber);

        // Query to check if phone number is already registered in database
        userRef.orderByChild("phone").equalTo(phoneNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Log.d("PhoneVerification", "Phone number exists in database");
                            // Phone number exists, check if it belongs to current user
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                String existingUserId = userSnapshot.getKey();
                                User existingUser = userSnapshot.getValue(User.class);

                                Log.d("PhoneVerification", "Existing User ID: " + existingUserId);
                                Log.d("PhoneVerification", "Current User ID: " + currentUser.getUid());

                                if (existingUserId != null && !existingUserId.equals(currentUser.getUid())) {
                                    // Phone number belongs to another user
                                    Log.d("PhoneVerification", "Phone belongs to different user");
                                    Toast.makeText(requireContext(),
                                            "This phone number is already registered with another account",
                                            Toast.LENGTH_LONG).show();
                                    requireActivity().onBackPressed();
                                    return;
                                } else if (existingUserId != null && existingUserId.equals(currentUser.getUid())) {
                                    // Phone already belongs to current user
                                    Log.d("PhoneVerification", "Phone already belongs to current user in database");
                                    if (existingUser != null && existingUser.isPhoneVerified()) {
                                        Toast.makeText(requireContext(),
                                                "Phone number already verified for your account",
                                                Toast.LENGTH_SHORT).show();
                                        requireActivity().onBackPressed();
                                        return;
                                    }
                                }
                            }
                        }
                        // Phone number doesn't exist or belongs to current user (not verified)
                        Log.d("PhoneVerification", "Phone not found or belongs to current user (unverified), sending OTP");
                        sendOTP(phoneNumber);
                        startResendTimer();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("PhoneVerification", "Error checking phone in database: " + error.getMessage());
                        Toast.makeText(requireContext(), "Error checking phone number: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        // Allow sending OTP even if check fails (but show warning)
                        sendOTP(phoneNumber);
                        startResendTimer();
                    }
                });
    }

    private void sendOTP(String phoneNumber) {
        Log.d("PhoneVerification", "Attempting to send OTP to: " + phoneNumber);

        // Validate phone number format
        if (!isValidPhoneNumber(phoneNumber)) {
            Toast.makeText(requireContext(), "Invalid phone number format: " + phoneNumber, Toast.LENGTH_LONG).show();
            return;
        }

        // Show loading
        btnVerify.setEnabled(false);
        btnVerify.setText("កំពុងផ្ញើ OTP...");

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        Log.d("PhoneVerification", "Auto-verification completed");
                        btnVerify.setEnabled(true);
                        btnVerify.setText("ផ្ទៀងផ្ទាត់");
                        // Auto-retrieval or instant verification
                        linkPhoneNumberToUser(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.e("PhoneVerification", "Verification failed: " + e.getMessage(), e);
                        btnVerify.setEnabled(true);
                        btnVerify.setText("ផ្ទៀងផ្ទាត់");
                        String errorMessage = getErrorMessage(e);
                        Toast.makeText(requireContext(), "Verification failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        Log.d("PhoneVerification", "Code sent successfully, verificationId: " + verificationId);
                        PhoneNumberVerifyingFragment.this.verificationId = verificationId;
                        resendingToken = token;
                        btnVerify.setEnabled(true);
                        btnVerify.setText("ផ្ទៀងផ្ទាត់");
                        Toast.makeText(requireContext(), "OTP sent successfully to " + phoneNumber, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                        super.onCodeAutoRetrievalTimeOut(s);
                        Log.d("PhoneVerification", "Code auto-retrieval timeout");
                        btnVerify.setEnabled(true);
                        btnVerify.setText("ផ្ទៀងផ្ទាត់");
                        Toast.makeText(requireContext(), "Timeout, please try again", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        try {
            PhoneAuthProvider.verifyPhoneNumber(options);
            Log.d("PhoneVerification", "verifyPhoneNumber called successfully");
        } catch (Exception e) {
            Log.e("PhoneVerification", "Exception in verifyPhoneNumber: " + e.getMessage(), e);
            btnVerify.setEnabled(true);
            btnVerify.setText("ផ្ទៀងផ្ទាត់");
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        // Cambodian phone numbers: +855 followed by 8-9 digits
        String pattern = "^\\+855[0-9]{8,9}$";
        boolean isValid = phoneNumber.matches(pattern);
        Log.d("PhoneVerification", "Phone validation result for " + phoneNumber + ": " + isValid);
        return isValid;
    }

    private String getErrorMessage(FirebaseException e) {
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Invalid phone number format";
        } else if (e.getMessage() != null && e.getMessage().contains("quota")) {
            return "Quota exceeded. Try again later.";
        } else if (e.getMessage() != null && e.getMessage().contains("network")) {
            return "Network error. Check your connection.";
        } else if (e.getMessage() != null && e.getMessage().contains("invalid")) {
            return "Invalid phone number or not supported.";
        } else {
            return e.getMessage() != null ? e.getMessage() : "Unknown error";
        }
    }

    private void resendOTP() {
        if (phoneNumber.isEmpty()) return;

        Log.d("PhoneVerification", "Resending OTP to: " + phoneNumber);

        // Show loading
        tvResend.setEnabled(false);
        tvResend.setText("កំពុងផ្ញើ...");

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        linkPhoneNumberToUser(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        tvResend.setEnabled(true);
                        tvResend.setText("ផ្ញើម្តងទៀត");
                        Toast.makeText(requireContext(), "Resend failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        PhoneNumberVerifyingFragment.this.verificationId = verificationId;
                        resendingToken = token;
                        tvResend.setEnabled(true);
                        tvResend.setText("ផ្ញើម្តងទៀត");
                        Toast.makeText(requireContext(), "New OTP sent", Toast.LENGTH_SHORT).show();
                        startResendTimer();
                    }
                })
                .setForceResendingToken(resendingToken)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyOTP(String otp) {
        Log.d("PhoneVerification", "Verifying OTP: " + otp);

        if (verificationId.isEmpty()) {
            Log.e("PhoneVerification", "Verification ID is empty!");
            Toast.makeText(requireContext(), "Verification ID not found. Please request OTP again.", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        linkPhoneNumberToUser(credential);
    }

    private void linkPhoneNumberToUser(PhoneAuthCredential credential) {
        Log.d("PhoneVerification", "Linking phone to user: " + currentUser.getUid());

        btnVerify.setEnabled(false);
        btnVerify.setText("កំពុងផ្ទៀងផ្ទាត់...");

        // First, check if phone is already linked to current user
        if (currentUser.getPhoneNumber() != null && currentUser.getPhoneNumber().equals(phoneNumber)) {
            Log.d("PhoneVerification", "Phone already linked to current user in Firebase Auth");
            updateUserPhoneInDatabase();
            return;
        }

        // Link phone credential to current user
        currentUser.linkWithCredential(credential)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("PhoneVerification", "Phone linked successfully in Firebase Auth");
                            // Phone linked successfully in Firebase Auth
                            // Now update the Realtime Database
                            updateUserPhoneInDatabase();
                        } else {
                            btnVerify.setEnabled(true);
                            btnVerify.setText("ផ្ទៀងផ្ទាត់");

                            Exception exception = task.getException();
                            Log.e("PhoneVerification", "Link failed: " + (exception != null ? exception.getMessage() : "Unknown error"));

                            if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(requireContext(), "Invalid OTP code", Toast.LENGTH_SHORT).show();
                            } else if (exception != null && exception.getMessage().contains("already linked")) {
                                // If already linked to another account
                                Log.d("PhoneVerification", "Phone already linked to another account");
                                Toast.makeText(requireContext(),
                                        "This phone number is already linked to another account",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(requireContext(),
                                        "Failed to link phone number: " + (exception != null ? exception.getMessage() : "Unknown error"),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void updateUserPhoneInDatabase() {
        Log.d("PhoneVerification", "Updating database for user: " + currentUser.getUid());

        // Get current user data first
        userRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User exists, update phone fields
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("phone", phoneNumber);
                    updates.put("phoneVerified", true);

                    Log.d("PhoneVerification", "Updating existing user with: phone=" + phoneNumber + ", phoneVerified=true");

                    // Apply updates
                    userRef.child(currentUser.getUid()).updateChildren(updates)
                            .addOnCompleteListener(task -> {
                                btnVerify.setEnabled(true);
                                btnVerify.setText("ផ្ទៀងផ្ទាត់");

                                if (task.isSuccessful()) {
                                    Log.d("PhoneVerification", "Database updated successfully");
                                    Toast.makeText(requireContext(),
                                            "Phone number verified and saved to profile!",
                                            Toast.LENGTH_SHORT).show();

                                    // Navigate back or to home
                                    navigateToHome();
                                } else {
                                    Log.e("PhoneVerification", "Failed to update database: " + task.getException().getMessage());
                                    Toast.makeText(requireContext(),
                                            "Failed to update profile: " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    // User doesn't exist in database, create new entry
                    Log.d("PhoneVerification", "User not found in database, creating new entry");
                    createNewUserInDatabase();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnVerify.setEnabled(true);
                btnVerify.setText("ផ្ទៀងផ្ទាត់");
                Log.e("PhoneVerification", "Database error: " + error.getMessage());
                Toast.makeText(requireContext(),
                        "Error updating profile: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewUserInDatabase() {
        Log.d("PhoneVerification", "Creating new user in database");

        // Create a new user with email and phone
        User user = new User(
                currentUser.getUid(),
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "",
                "",
                currentUser.getEmail() != null ? currentUser.getEmail() : "",
                phoneNumber,
                "user", // default role
                "",
                currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "",
                "" // device token
        );

        // Set phone verified
        user.setPhoneVerified(true);
        user.setEmailVerified(currentUser.isEmailVerified());

        Log.d("PhoneVerification", "User object created: " + user.getUserId() + ", phone: " + user.getPhone());

        // Save to database
        userRef.child(currentUser.getUid()).setValue(user)
                .addOnCompleteListener(task -> {
                    btnVerify.setEnabled(true);
                    btnVerify.setText("ផ្ទៀងផ្ទាត់");

                    if (task.isSuccessful()) {
                        Log.d("PhoneVerification", "User created successfully in database");
                        Toast.makeText(requireContext(),
                                "Profile created successfully with phone number!",
                                Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        Log.e("PhoneVerification", "Failed to create user: " + task.getException().getMessage());
                        Toast.makeText(requireContext(),
                                "Failed to create profile: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startResendTimer() {
        isResendEnabled = false;
        tvResend.setTextColor(getResources().getColor(R.color.gray));
        tvResend.setEnabled(false);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(RESEND_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                textView6.setText("មិនបានទទួលសារ? (" + seconds + "s)");
            }

            @Override
            public void onFinish() {
                isResendEnabled = true;
                tvResend.setTextColor(getResources().getColor(R.color.primary));
                textView6.setText("មិនបានទទួលសារ?");
                tvResend.setEnabled(true);
            }
        }.start();
    }

    private void navigateToHome() {
        Log.d("PhoneVerification", "Navigation to home");

        requireActivity().onBackPressed();

        Toast.makeText(requireContext(),
                "Phone number added successfully! You can now login with phone or email.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}