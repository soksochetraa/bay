package com.example.bay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bay.databinding.ActivityAuthenticationRegisterBinding;
import com.example.bay.repository.UserRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.List;

public class AuthenticationRegisterActivity extends AppCompatActivity {

    private ActivityAuthenticationRegisterBinding binding;
    private FirebaseAuth mAuth;
    private UserRepository userRepository;
    private EditText[] inputFields;
    private GoogleSignInClient googleSignInClient;
    private static final String TAG = "AuthenticationRegister";
    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private String pendingEmailForGoogleSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAuthenticationRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        setupInputFieldsArray();
        setupClickListeners();
        setupEnterKeyListeners();
        setupPasswordToggle();
    }

    private void setupInputFieldsArray() {
        inputFields = new EditText[]{
                binding.etFirstName,
                binding.etLastName,
                binding.etEmail,
                binding.etPassword,
                binding.etConfirmPassword
        };
    }

    private void setupEnterKeyListeners() {
        for (int i = 0; i < inputFields.length; i++) {
            final int currentIndex = i;

            inputFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    hideKeyboard();

                    if (currentIndex < inputFields.length - 1) {
                        inputFields[currentIndex + 1].requestFocus();
                    } else {
                        registerUser();
                    }
                    return true;
                }
                return false;
            });
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null && (ev.getAction() == MotionEvent.ACTION_UP ||
                ev.getAction() == MotionEvent.ACTION_MOVE) &&
                view instanceof EditText && !view.getClass().getName().startsWith("android.webkit.")) {
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

    private void setupClickListeners() {
        binding.login.setOnClickListener(v -> {
            hideKeyboard();
            navigateToLogin();
        });

        binding.btnPhone.setOnClickListener(v -> {
            hideKeyboard();
            navigateToPhoneRegister();
        });

        binding.registerButton.setOnClickListener(v -> {
            hideKeyboard();
            registerUser();
        });

        binding.btnGoogle.setOnClickListener(v->{
            hideKeyboard();
            initiateGoogleSignIn();
        });
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
        String firstName = binding.etFirstName.getText().toString().trim();
        String lastName = binding.etLastName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (!validateInputs(firstName, lastName, email, password, confirmPassword)) {
            return;
        }

        showLoading();
        checkEmailExistsInBackend(email, firstName, lastName, password);
    }

    private void checkEmailExistsInBackend(String email, String firstName, String lastName, String password) {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        userRepository.checkEmailExists(email, currentUserId, new UserRepository.BoolCallback() {
            @Override
            public void onResult(boolean exists) {
                if (exists) {
                    hideLoading();
                    showEmailAlreadyExistsDialog(email);
                } else {
                    checkEmailProviders(email, firstName, lastName, password);
                }
            }

            @Override
            public void onError(String errorMsg) {
                hideLoading();
                Log.e(TAG, "Email check failed: " + errorMsg);
                binding.tvValidate.setText("Unable to verify email. Please check your connection.");
                binding.tvValidate.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkEmailProviders(String email, String firstName, String lastName, String password) {
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> signInMethods = task.getResult().getSignInMethods();

                        if (signInMethods != null && signInMethods.contains(GoogleAuthProvider.PROVIDER_ID)) {
                            hideLoading();
                            showGoogleSignInOption(email);
                        } else {
                            showLoading();
                            proceedToCompleteProfile(firstName, lastName, email, password);
                        }
                    } else {
                        hideLoading();
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        binding.tvValidate.setText("Error checking account: " + error);
                        binding.tvValidate.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showGoogleSignInOption(String email) {
        pendingEmailForGoogleSignIn = email;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.custom_dialog, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView message = view.findViewById(R.id.textView1);
        MaterialButton btnPositive = view.findViewById(R.id.btnPositive);
        MaterialButton btnNegative = view.findViewById(R.id.btnNegative);

        message.setText("This email is already registered with Google Sign-In.\n\n" +
                "Would you like to sign in with Google instead?");

        btnPositive.setText("Sign in with Google");
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            initiateGoogleSignIn();
        });

        btnNegative.setText("Cancel");
        btnNegative.setOnClickListener(v -> {
            dialog.dismiss();
            binding.etEmail.requestFocus();
            pendingEmailForGoogleSignIn = null;
        });

        dialog.setCancelable(false);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void initiateGoogleSignIn() {
        showLoading();
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    private void showEmailAlreadyExistsDialog(String email) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.custom_dialog, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView message = view.findViewById(R.id.textView1);
        MaterialButton btnPositive = view.findViewById(R.id.btnPositive);
        MaterialButton btnNegative = view.findViewById(R.id.btnNegative);

        message.setText("Email " + email + " is already registered.\nDo you want to sign in?");

        btnPositive.setText("Sign In");
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();

            Intent intent = new Intent(this, AuthenticationLogInActivity.class);
            intent.putExtra("prefill_email", email);
            startActivity(intent);
            finish();
        });

        btnNegative.setText("Cancel");
        btnNegative.setOnClickListener(v -> {
            dialog.dismiss();
            binding.etEmail.requestFocus();
        });

        dialog.setCancelable(false);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                hideLoading();
                Log.w(TAG, "Google sign in failed", e);
                binding.tvValidate.setText("Google Sign-In failed. Please try again.");
                binding.tvValidate.setVisibility(View.VISIBLE);
                pendingEmailForGoogleSignIn = null;
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        String idToken = acct.getIdToken();

        mAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(this, task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithCredential:success");
                        navigateToMainActivity();
                    } else {
                        // If sign in fails
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        binding.tvValidate.setText("Authentication failed.");
                        binding.tvValidate.setVisibility(View.VISIBLE);
                    }
                    pendingEmailForGoogleSignIn = null;
                });
    }

    private void navigateToMainActivity() {
        // Navigate to your main activity after successful sign-in
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean validateInputs(String firstName, String lastName, String email,
                                   String password, String confirmPassword) {
        for (EditText field : inputFields) {
            field.setError(null);
        }

        binding.tvValidate.setVisibility(View.GONE);

        if (TextUtils.isEmpty(firstName)) {
            binding.etFirstName.setError("First name is required");
            binding.etFirstName.requestFocus();
            return false;
        }

        if (firstName.length() < 2) {
            binding.etFirstName.setError("First name must be at least 2 characters");
            binding.etFirstName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(lastName)) {
            binding.etLastName.setError("Last name is required");
            binding.etLastName.requestFocus();
            return false;
        }

        if (lastName.length() < 2) {
            binding.etLastName.setError("Last name must be at least 2 characters");
            binding.etLastName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Please enter a valid email address");
            binding.etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return false;
        }

        if (password.length() < 8) {
            binding.etPassword.setError("Password must be at least 8 characters");
            binding.etPassword.requestFocus();
            return false;
        }

        // Check for password strength
        if (!isPasswordStrong(password)) {
            binding.etPassword.setError("Password must contain both letters and numbers");
            binding.etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isPasswordStrong(String password) {
        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        return hasLetter && hasDigit;
    }

    private void proceedToCompleteProfile(String firstName, String lastName,
                                          String email, String password) {
        Intent intent = new Intent(this, CompleteProfileActivity.class);
        intent.putExtra("openFrom", "openFromRegister");
        intent.putExtra("firstName", firstName);
        intent.putExtra("lastName", lastName);
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        intent.putExtra("hasGoogleProvider", false);
        startActivity(intent);
        finish();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordToggle() {
        binding.etPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {

                int drawableWidth = binding.etPassword.getCompoundDrawables()[2].getBounds().width();
                int padding = 60;

                if (event.getRawX() >= (binding.etPassword.getRight() - drawableWidth - padding)) {

                    if (binding.etPassword.getTransformationMethod()
                            instanceof android.text.method.PasswordTransformationMethod) {

                        binding.etPassword.setTransformationMethod(null);
                        binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                0, 0, R.drawable.ic_eye_close, 0
                        );

                    } else {

                        binding.etPassword.setTransformationMethod(
                                android.text.method.PasswordTransformationMethod.getInstance()
                        );
                        binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                0, 0, R.drawable.ic_eye_open, 0
                        );
                    }

                    binding.etPassword.setSelection(binding.etPassword.getText().length());
                    return true;
                }
            }
            return false;
        });
        binding.etConfirmPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {

                int drawableWidth = binding.etConfirmPassword.getCompoundDrawables()[2].getBounds().width();
                int padding = 60;

                if (event.getRawX() >= (binding.etConfirmPassword.getRight() - drawableWidth - padding)) {

                    if (binding.etConfirmPassword.getTransformationMethod()
                            instanceof android.text.method.PasswordTransformationMethod) {

                        binding.etConfirmPassword.setTransformationMethod(null);
                        binding.etConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(
                                0, 0, R.drawable.ic_eye_close, 0
                        );

                    } else {

                        binding.etConfirmPassword.setTransformationMethod(
                                android.text.method.PasswordTransformationMethod.getInstance()
                        );
                        binding.etConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(
                                0, 0, R.drawable.ic_eye_open, 0
                        );
                    }

                    binding.etConfirmPassword.setSelection(binding.etPassword.getText().length());
                    return true;
                }
            }
            return false;
        });
    }

    private void showLoading() {
        binding.loading.setVisibility(View.VISIBLE);
        disableInputs(true);
    }

    private void hideLoading() {
        binding.loading.postDelayed(() -> {
            binding.loading.setVisibility(View.GONE);
            disableInputs(false);
        }, 500);
    }

    private void disableInputs(boolean disable) {
        for (EditText field : inputFields) {
            field.setEnabled(!disable);
        }
        binding.registerButton.setEnabled(!disable);
        binding.login.setEnabled(!disable);
        binding.btnPhone.setEnabled(!disable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}