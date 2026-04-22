package com.example.bay.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bay.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized helper for user verification logic.
 *
 * - Checks if both emailVerified and phoneVerified are true,
 *   and if so, sets userVerified = true in the database.
 * - Auto-marks email as verified for Google (passwordless Gmail) sign-in users.
 * - Manages weekly verification prompt timestamps.
 */
public class VerificationHelper {

    private static final String TAG = "VerificationHelper";
    private static final String PREF_NAME = "verification_prefs";
    private static final String KEY_LAST_PROMPT = "last_verification_prompt";
    private static final long ONE_WEEK_MS = 7L * 24 * 60 * 60 * 1000; // 7 days

    /**
     * After email or phone verification, call this to check if both are verified.
     * If both are true and userVerified is false, updates userVerified = true in the DB.
     *
     * @param userId   The user's UID
     * @param callback Optional callback called with true if userVerified was set to true
     */
    public static void checkAndUpdateUserVerified(String userId, OnVerificationUpdatedCallback callback) {
        if (userId == null) return;

        DatabaseReference userRef = FirebaseDBHelper.getUserRef(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                User user = snapshot.getValue(User.class);
                if (user == null) return;

                boolean emailVerified = user.isEmailVerified();
                boolean phoneVerified = user.isPhoneVerified();
                boolean userVerified = user.isUserVerified();

                Log.d(TAG, "Checking verification for user " + userId
                        + ": emailVerified=" + emailVerified
                        + ", phoneVerified=" + phoneVerified
                        + ", userVerified=" + userVerified);

                if (emailVerified && phoneVerified && !userVerified) {
                    // Both verified, update userVerified to true
                    userRef.child("userVerified").setValue(true)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "userVerified set to true for user: " + userId);
                                    if (callback != null) callback.onUpdated(true);
                                } else {
                                    Log.e(TAG, "Failed to update userVerified: " + task.getException());
                                    if (callback != null) callback.onUpdated(false);
                                }
                            });
                } else if (emailVerified && phoneVerified && userVerified) {
                    // Already verified
                    Log.d(TAG, "User already fully verified: " + userId);
                    if (callback != null) callback.onUpdated(true);
                } else {
                    // Not fully verified yet
                    if (callback != null) callback.onUpdated(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking verification: " + error.getMessage());
                if (callback != null) callback.onUpdated(false);
            }
        });
    }

    /**
     * For Google sign-in users, auto-mark emailVerified = true in the database
     * since Google already verifies the email.
     *
     * @param userId   The user's UID
     * @param callback Optional callback
     */
    public static void autoVerifyEmailForGoogleUser(String userId, OnVerificationUpdatedCallback callback) {
        if (userId == null) return;

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        // Check if user signed in with Google provider
        boolean isGoogleUser = false;
        for (com.google.firebase.auth.UserInfo profile : firebaseUser.getProviderData()) {
            if ("google.com".equals(profile.getProviderId())) {
                isGoogleUser = true;
                break;
            }
        }

        if (isGoogleUser) {
            Log.d(TAG, "Google user detected, auto-verifying email for: " + userId);

            DatabaseReference userRef = FirebaseDBHelper.getUserRef(userId);
            userRef.child("emailVerified").setValue(true)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email auto-verified for Google user: " + userId);
                            // Now check if both verifications are complete
                            checkAndUpdateUserVerified(userId, callback);
                        } else {
                            Log.e(TAG, "Failed to auto-verify email: " + task.getException());
                            if (callback != null) callback.onUpdated(false);
                        }
                    });
        } else {
            if (callback != null) callback.onUpdated(false);
        }
    }

    /**
     * Syncs the Firebase Auth email verification status to the database,
     * then checks if both verifications are complete.
     *
     * @param callback Optional callback
     */
    public static void syncEmailVerificationAndCheck(OnVerificationUpdatedCallback callback) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) callback.onUpdated(false);
            return;
        }

        String userId = firebaseUser.getUid();

        firebaseUser.reload().addOnCompleteListener(task -> {
            boolean isVerified = firebaseUser.isEmailVerified();

            // Also check if user is a Google user (always email-verified)
            boolean isGoogleUser = false;
            for (com.google.firebase.auth.UserInfo profile : firebaseUser.getProviderData()) {
                if ("google.com".equals(profile.getProviderId())) {
                    isGoogleUser = true;
                    break;
                }
            }

            boolean finalVerified = isVerified || isGoogleUser;

            FirebaseDBHelper.getUserRef(userId)
                    .child("emailVerified")
                    .setValue(finalVerified)
                    .addOnCompleteListener(t -> {
                        // After syncing email, check if userVerified should be updated
                        checkAndUpdateUserVerified(userId, callback);
                    });
        });
    }

    /**
     * Check whether the weekly verification prompt should be shown.
     * Returns true if:
     * - User is NOT yet fully verified (emailVerified or phoneVerified is false)
     * - AND it has been at least 1 week since last prompt (or never prompted)
     */
    public static boolean shouldShowVerificationPrompt(Context context, User user) {
        if (user == null) return false;

        // Already fully verified, no need to prompt
        if (user.isEmailVerified() && user.isPhoneVerified()) {
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0);
        long now = System.currentTimeMillis();

        return (now - lastPrompt) >= ONE_WEEK_MS;
    }

    /**
     * Record that the verification prompt was shown (to enforce weekly limit).
     */
    public static void recordPromptShown(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_PROMPT, System.currentTimeMillis()).apply();
    }

    /**
     * Build the appropriate message for the verification prompt dialog.
     */
    public static String getVerificationPromptMessage(User user) {
        boolean emailDone = user.isEmailVerified();
        boolean phoneDone = user.isPhoneVerified();

        if (!emailDone && !phoneDone) {
            return "សូមផ្ទៀងផ្ទាត់អ៊ីមែល និងលេខទូរស័ព្ទរបស់អ្នកដើម្បីទទួលបានសញ្ញាផ្ទៀងផ្ទាត់។\n\n"
                    + "Please verify your email and phone number to get a verified badge.";
        } else if (!emailDone) {
            return "សូមផ្ទៀងផ្ទាត់អ៊ីមែលរបស់អ្នកដើម្បីទទួលបានសញ្ញាផ្ទៀងផ្ទាត់។\n\n"
                    + "Please verify your email to get a verified badge.";
        } else {
            return "សូមផ្ទៀងផ្ទាត់លេខទូរស័ព្ទរបស់អ្នកដើម្បីទទួលបានសញ្ញាផ្ទៀងផ្ទាត់។\n\n"
                    + "Please verify your phone number to get a verified badge.";
        }
    }

    public interface OnVerificationUpdatedCallback {
        void onUpdated(boolean isFullyVerified);
    }
}
