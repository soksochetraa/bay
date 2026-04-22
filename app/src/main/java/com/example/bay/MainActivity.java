package com.example.bay;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bay.model.User;
import com.example.bay.repository.UserRepository;
import com.example.bay.util.VerificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentUser != null) {
                updateEmailVerification(currentUser, () -> {
                    checkUserProfileCompletion(currentUser);
                });
            } else {
                navigateToSplashScreen();
            }
        }, 1500);
    }

    private void updateEmailVerification(FirebaseUser firebaseUser, Runnable onDone) {
        // Use VerificationHelper to sync email verification (handles Google auto-verify)
        // and automatically check/update userVerified if both email & phone are verified
        VerificationHelper.syncEmailVerificationAndCheck(isFullyVerified -> {
            if (onDone != null) onDone.run();
        });
    }

    private void checkUserProfileCompletion(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();

        userRepository.getUserById(userId, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.isProfileCompleted()) {
                    navigateToHome();
                } else {
                    navigateToCompleteProfile(user, firebaseUser);
                }
            }

            @Override
            public void onError(String errorMsg) {
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

    private void navigateToSplashScreen() {
        startActivity(new Intent(this, SplashScreenOneActivity.class));
        finish();
    }
}