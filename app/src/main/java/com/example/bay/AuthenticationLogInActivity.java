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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.bay.databinding.ActivityAuthenticationLogInBinding;
import com.example.bay.model.User;
import com.example.bay.repository.UserRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Objects;

public class AuthenticationLogInActivity extends AppCompatActivity {

    private ActivityAuthenticationLogInBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private UserRepository userRepository;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAuthenticationLogInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        checkCurrentUser();
        configureGoogleSignIn();
        setupClickListeners();
        setupEnterKeyListeners();
        setupPasswordToggle();
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            showLoading("Checking profile...");
            checkUserProfileCompletion(currentUser.getUid());
        }
    }

    private void checkUserProfileCompletion(String userId) {
        userRepository.getUserById(userId, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User user) {
                hideLoading();

                if (user != null && user.isProfileCompleted()) {

                    if (user.isBanned()) {
                        showDialog(
                                "Your account has been banned.\n\nReason: "
                                        + user.getModeration().getWarningMessage(),
                                "យល់ព្រម",
                                null,
                                null,
                                null,
                                true
                        );
                        return;
                    } else if (user.isSuspension()) {
                        showDialog(
                                "Your account has been banned.\n\nReason: "
                                        + user.getModeration().getSuspensionReason() + " "+user.getModeration().getSuspendedUntil(),
                                "OK",
                                "យល់ព្រម",
                                null,
                                null,
                                true
                        );

                        mAuth.signOut();
                        return;
                    }

                    navigateToHome();

                } else {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    navigateToCompleteProfile(user, firebaseUser);
                }
            }

            @Override
            public void onError(String errorMsg) {
                hideLoading();
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                navigateToCompleteProfile(null, firebaseUser);
            }
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToCompleteProfile(User existingUser, FirebaseUser firebaseUser) {
        Intent intent = new Intent(this, CompleteProfileActivity.class);

        intent.putExtra("openFrom", "openFromPhoneNumber");

        if (existingUser != null) {
            intent.putExtra("firstName", existingUser.getFirstName());
            intent.putExtra("lastName", existingUser.getLastName());
            intent.putExtra("email", existingUser.getEmail());
            intent.putExtra("phone_number", existingUser.getPhone());
            intent.putExtra("userId", existingUser.getUserId());
        } else if (firebaseUser != null) {

            String name = firebaseUser.getDisplayName();
            if (name != null && name.contains(" ")) {
                String[] parts = name.split(" ", 2);
                intent.putExtra("firstName", parts[0]);
                intent.putExtra("lastName", parts.length > 1 ? parts[1] : "");
            }

            intent.putExtra("email", firebaseUser.getEmail());
            intent.putExtra("phone_number", firebaseUser.getPhoneNumber());
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupEnterKeyListeners() {
        binding.etEmail.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                hideKeyboard();
                binding.etPassword.requestFocus();
                return true;
            }
            return false;
        });

        binding.etPassword.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                hideKeyboard();
                handleEmailLogin();
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        binding.register.setOnClickListener(v -> {
            hideKeyboard();
            startActivity(new Intent(this, AuthenticationRegisterActivity.class));
            finish();
        });

        binding.forgetPassword.setOnClickListener(v -> {
            hideKeyboard();
            startActivity(new Intent(this, ForgetPasswordActivity.class));
            finish();
        });

        binding.btnPhone.setOnClickListener(v -> {
            hideKeyboard();
            Intent intent = new Intent(this, AuthenticationPhoneActivity.class);
            intent.putExtra("openFrom", "openFromLogIn");
            startActivity(intent);
            finish();
        });

        binding.loginButton.setOnClickListener(v -> {
            hideKeyboard();
            handleEmailLogin();
        });

        binding.btnGoogle.setOnClickListener(v -> {
            hideKeyboard();
            signInWithGoogle();
        });
    }

    private void signInWithGoogle() {
        showLoading("Opening Google Sign-In...");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                hideLoading();
                binding.tvValidate.setText("Google sign in failed: " + e.getMessage());
                binding.tvValidate.setVisibility(View.VISIBLE);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showLoading("Authenticating with Google...");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserProfileCompletion(user.getUid());
                        }
                    } else {
                        hideLoading();
                        binding.tvValidate.setText("Google authentication failed");
                        binding.tvValidate.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void handleEmailLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        binding.tvValidate.setVisibility(View.GONE);

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required!");
            binding.etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Invalid email!");
            binding.etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password required!");
            binding.etPassword.requestFocus();
            return;
        }

        showLoading("Logging in...");
        loginWithEmail(email, password);
    }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserProfileCompletion(user.getUid());
                        }
                    } else {
                        hideLoading();
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "";
                        binding.tvValidate.setText(errorMessage);
                        binding.tvValidate.setVisibility(View.VISIBLE);
                    }
                });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordToggle() {
        binding.etPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {

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
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showLoading(String message) {
        binding.loadingText.setText(message);
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
        binding.etEmail.setEnabled(!disable);
        binding.etPassword.setEnabled(!disable);
        binding.loginButton.setEnabled(!disable);
        binding.btnPhone.setEnabled(!disable);
        binding.btnGoogle.setEnabled(!disable);
        binding.register.setEnabled(!disable);
        binding.forgetPassword.setEnabled(!disable);
    }

    public void showDialog(String messageText, String positiveText, String negativeText,
                           Runnable onPositive, Runnable onNegative, boolean isSingleButton) {

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.custom_dialog, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();

        ConstraintLayout layoutTwoBtn = view.findViewById(R.id.constraintLayout6);
        ConstraintLayout layoutOneBtn = view.findViewById(R.id.constraintLayout7);

        TextView message = view.findViewById(R.id.textView1);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirm);
        MaterialButton btnPositive = view.findViewById(R.id.btnPositive);
        MaterialButton btnNegative = view.findViewById(R.id.btnNegative);

        message.setText(messageText);

        if (isSingleButton) {
            layoutTwoBtn.setVisibility(View.GONE);
            layoutOneBtn.setVisibility(View.VISIBLE);

            btnConfirm.setText(positiveText);
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                if (onPositive != null) onPositive.run();
            });

        } else {
            layoutOneBtn.setVisibility(View.GONE);
            layoutTwoBtn.setVisibility(View.VISIBLE);

            btnPositive.setText(positiveText);
            btnNegative.setText(negativeText);

            btnPositive.setOnClickListener(v -> {
                dialog.dismiss();
                if (onPositive != null) onPositive.run();
            });

            btnNegative.setOnClickListener(v -> {
                dialog.dismiss();
                if (onNegative != null) onNegative.run();
            });
        }

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}