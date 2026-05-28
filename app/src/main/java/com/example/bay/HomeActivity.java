package com.example.bay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import com.example.bay.BaseActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.bay.databinding.ActivityHomeBinding;
import com.example.bay.fragment.CommunityAccountFragment;
import com.example.bay.fragment.CommunityFragment;
import com.example.bay.fragment.EditProfileFragment;
import com.example.bay.fragment.HomeFragment;
import com.example.bay.fragment.MarketPlaceMainFragment;
import com.example.bay.fragment.MessageFragment;
import com.example.bay.fragment.PhoneNumberVerifyingFragment;
import com.example.bay.fragment.PostDetailFragment;
import com.example.bay.fragment.CommunitySearchFragment;
import com.example.bay.util.FirebaseDBHelper;
import com.example.bay.util.VerificationHelper;
import com.example.bay.viewmodel.SharedUserViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashSet;
import java.util.Set;

public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding binding;
    private FirebaseUser currentUser;
    private boolean bottomNavVisibleBeforeKeyboard = true;
    private SharedUserViewModel sharedUserViewModel;

    private static final String CHANNEL_ID = "chat_notifications";

    /**
     * Fragments that are considered "top-level" tabs and should show the bottom nav.
     * CommunityAccountFragment is handled separately (only shows for own profile).
     */
    private static final Set<Class<? extends Fragment>> TOP_LEVEL_FRAGMENTS = new HashSet<>();
    static {
        TOP_LEVEL_FRAGMENTS.add(HomeFragment.class);
        TOP_LEVEL_FRAGMENTS.add(CommunityFragment.class);
        TOP_LEVEL_FRAGMENTS.add(MarketPlaceMainFragment.class);
        TOP_LEVEL_FRAGMENTS.add(MessageFragment.class);
    }

    public HomeActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedUserViewModel = new ViewModelProvider(this).get(SharedUserViewModel.class);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_navigation), (v, insets) -> {
            v.setPadding(0, 0, 0, 0);
            return insets;
        });

        // Hide bottom navigation when keyboard is open
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (isKeyboardVisible) {
                // Remember the current visibility before hiding
                if (binding.bottomNavigation.getVisibility() == View.VISIBLE) {
                    bottomNavVisibleBeforeKeyboard = true;
                }
                binding.bottomNavigation.setVisibility(View.GONE);
            } else {
                // Restore bottom nav only if it was visible before the keyboard opened
                if (bottomNavVisibleBeforeKeyboard) {
                    binding.bottomNavigation.setVisibility(View.VISIBLE);
                }
            }
            return ViewCompat.onApplyWindowInsets(v, insets);
        });

        // ── Centralized bottom nav visibility ───────────────────────────
        // Automatically show/hide the bottom navigation based on the
        // current fragment. No individual fragment needs to call
        // hideBottomNavigation() or showBottomNavigation() anymore.
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateBottomNavVisibility);

        if (savedInstanceState == null) {
            LoadFragment(new HomeFragment());
            binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                LoadFragment(new HomeFragment());
            } else if (itemId == R.id.nav_community) {
                LoadFragment(new CommunityFragment());
            } else if (itemId == R.id.nav_marketplace) {
                LoadFragment(new MarketPlaceMainFragment());
            } else if (itemId == R.id.nav_message) {
                LoadFragment(new MessageFragment());
            } else if (itemId == R.id.nav_profile) {
                if (currentUser != null) {
                    CommunityAccountFragment fragment = CommunityAccountFragment.newInstance(currentUser.getUid());
                    LoadFragment(fragment);
                } else {
                    LoadFragment(new CommunityAccountFragment());
                }
            } else {
                return false;
            }
            return true;
        });

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chat Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        RemoteViews notificationLayout =
                new RemoteViews(getPackageName(), R.layout.small_notification);

        RemoteViews notificationLayoutExpanded =
                new RemoteViews(getPackageName(), R.layout.big_notification);

        Notification customNotification =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_bell)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .setCustomContentView(notificationLayout)
                        .setCustomBigContentView(notificationLayoutExpanded)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build();

        notificationManager.notify(666, customNotification);

        setOnlineStatus(true);

        // Check and show verification prompt once per week
        checkVerificationPrompt();
    }

    /**
     * Checks if the current user needs to verify email/phone and shows
     * a prompt dialog once per week if not fully verified.
     */
    private void checkVerificationPrompt() {
        if (currentUser == null) return;

        sharedUserViewModel.getCurrentUser().observe(this, user -> {
            if (user == null) return;

            // Only show once per session per lifecycle
            if (!VerificationHelper.shouldShowVerificationPrompt(this, user)) return;

            // Record that we've shown the prompt
            VerificationHelper.recordPromptShown(this);

            String message = VerificationHelper.getVerificationPromptMessage(user);

            boolean needsPhone = !user.isPhoneVerified();
            boolean needsEmail = !user.isEmailVerified();

            showDialog(
                    message,
                    "ផ្ទៀងផ្ទាត់ឥឡូវ", // "Verify Now"
                    "ពេលក្រោយ",          // "Later"
                    () -> {
                        // Navigate to phone verification if phone not verified
                        if (needsPhone && user.getPhone() != null && !user.getPhone().isEmpty()) {
                            PhoneNumberVerifyingFragment fragment = new PhoneNumberVerifyingFragment();
                            android.os.Bundle args = new android.os.Bundle();
                            args.putString("phone_number", user.getPhone());
                            fragment.setArguments(args);
                            LoadFragment(fragment);
                        } else if (needsPhone) {
                            // If no phone number on file, go to edit profile
                            LoadFragment(EditProfileFragment.newInstance(currentUser.getUid()));
                        } else if (needsEmail) {
                            // Phone is verified but email is not, go to edit profile
                            LoadFragment(EditProfileFragment.newInstance(currentUser.getUid()));
                        }
                    },
                    null,
                    false
            );
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isFinishing()) {
            setOnlineStatus(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setOnlineStatus(false);
        binding = null;
    }

    private void setOnlineStatus(boolean isOnline) {
        if (currentUser != null) {
            FirebaseDBHelper.getOnlineStatusRef(currentUser.getUid()).setValue(isOnline);
        }
    }

    public void LoadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToMyProfile() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
        CommunityAccountFragment fragment = CommunityAccountFragment.newInstance(getCurrentUserId());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateTo(int navItemId, Fragment fragment) {
        binding.bottomNavigation.setSelectedItemId(navItemId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void signOut() {
        setOnlineStatus(false);
        if (sharedUserViewModel != null) {
            sharedUserViewModel.stopListening();
        }

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(HomeActivity.this, AuthenticationLogInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void setBottomNavigationVisible(boolean visible) {
        if (binding == null) return;
        bottomNavVisibleBeforeKeyboard = visible;
        binding.bottomNavigation.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * @deprecated Prefer relying on the centralized back-stack listener.
     *             Kept for edge cases where a fragment must force-hide.
     */
    public void hideBottomNavigation() {
        setBottomNavigationVisible(false);
    }

    /**
     * @deprecated Prefer relying on the centralized back-stack listener.
     *             Kept for edge cases where a fragment must force-show.
     */
    public void showBottomNavigation() {
        setBottomNavigationVisible(true);
    }

    /**
     * Centralized logic that runs every time the back stack changes.
     * Determines whether the bottom nav should be visible based on
     * the currently displayed fragment.
     */

    private void updateBottomNavVisibility() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (current == null || binding == null) return;

        boolean shouldShow;

        if (TOP_LEVEL_FRAGMENTS.contains(current.getClass())) {
            shouldShow = true;
        } else if (current instanceof CommunityAccountFragment) {
            // Own profile tab → show nav; other user's profile → hide
            String profileUserId = current.getArguments() != null
                    ? current.getArguments().getString("user_id")
                    : null;
            shouldShow = profileUserId != null && profileUserId.equals(getCurrentUserId());
        } else {
            shouldShow = false;
        }

        setBottomNavigationVisible(shouldShow);
    }

    public void showLoading() {
        binding.loading.setVisibility(View.VISIBLE);
    }

    public void hideLoading() {
        binding.loading.postDelayed(() -> {
            binding.loading.setVisibility(View.GONE);
        }, 2000);
    }

    public FirebaseUser getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getUid() : null;
    }

    public void loadUserProfile(String userId) {
        CommunityAccountFragment fragment = CommunityAccountFragment.newInstance(userId);
        LoadFragment(fragment);
    }

    public void loadPostDetail(String postId) {
        PostDetailFragment fragment = PostDetailFragment.newInstance(postId);
        LoadFragment(fragment);
    }

    public void loadFullUserSearch(String query) {
        CommunitySearchFragment fragment = new CommunitySearchFragment();
        Bundle args = new Bundle();
        args.putString("initial_search", query);
        fragment.setArguments(args);
        LoadFragment(fragment);
    }

    public void showDialog(
            String messageText,
            String positiveText,
            String negativeText,
            Runnable onPositive,
            Runnable onNegative,
            boolean isSingleButton
    ) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.custom_dialog, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ConstraintLayout layoutTwoBtn = view.findViewById(R.id.constraintLayout6);
        ConstraintLayout layoutOneBtn = view.findViewById(R.id.constraintLayout7);

        TextView message = view.findViewById(R.id.textView1);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirm);
        MaterialButton btnPositive = view.findViewById(R.id.btnPositive);
        MaterialButton btnNegative = view.findViewById(R.id.btnNegative);

        message.setText(messageText);

        if (isSingleButton) {
            if (layoutTwoBtn != null) layoutTwoBtn.setVisibility(View.GONE);
            if (layoutOneBtn != null) layoutOneBtn.setVisibility(View.VISIBLE);

            btnConfirm.setText(positiveText);

            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                if (onPositive != null) onPositive.run();
            });

        } else {
            if (layoutOneBtn != null) layoutOneBtn.setVisibility(View.GONE);
            if (layoutTwoBtn != null) layoutTwoBtn.setVisibility(View.VISIBLE);

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

        dialog.setCancelable(false);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

//    1. Call dialog from Fragment via Activity
//
//    Inside your Fragment:
//
//        ((HomeActivity) requireActivity()).showDialog(
//        "Are you sure?",
//                "Yes",
//                "Cancel",
//                () -> { Any Function() },
//            null,
//            false
//        );
//    2. For single button
//        ((HomeActivity) requireActivity()).showDialog(
//        "Success",
//                "OK",
//                null,
//                null,
//                null,
//                true
//        );

}