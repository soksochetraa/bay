package com.example.bay.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.databinding.FragmentSettingBinding;
import com.example.bay.util.LocaleHelper;
import com.example.bay.util.ThemeHelper;
import com.example.bay.viewmodel.SharedUserViewModel;
import androidx.lifecycle.ViewModelProvider;

public class SettingFragment extends Fragment {

    private static final String TAG = "SettingFragment";

    private FragmentSettingBinding binding;
    private SharedUserViewModel sharedUserViewModel;
    private String userId;

    public SettingFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedUserViewModel = new ViewModelProvider(requireActivity()).get(SharedUserViewModel.class);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSettingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof HomeActivity) {
            HomeActivity homeActivity = (HomeActivity) getActivity();
            userId = homeActivity.getCurrentUserId();
        }

        if (userId == null || sharedUserViewModel == null) {
            Log.w(TAG, "User ID or SharedUserViewModel is null");
            return;
        }

        // ── Dark-mode toggle ──────────────────────────────────────
        updateThemeLabel();
        binding.btnDarkMode.setOnClickListener(v -> showThemeChooserDialog());

        // ── Language switcher ─────────────────────────────────────
        updateLanguageLabel();
        binding.btnLanguage.setOnClickListener(v -> showLanguageChooserDialog());

        binding.btnSavedPosts.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getActivity();
                homeActivity.LoadFragment(new SavedCommunityPostsFragment());
            }
        });

        binding.buttonAboutApp.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getActivity();
                homeActivity.LoadFragment(new AboutAppFragment());
            }
        });

        binding.btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getActivity();
                homeActivity.signOut();
            }
        });

        binding.buttonContactUs.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getActivity();
                homeActivity.LoadFragment(new ContactUsFragment());
            }
        });


        binding.btnBack.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                requireActivity().onBackPressed();
            }
        });

        sharedUserViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
             if (!isAdded() || getActivity() == null || binding == null) {
                 return;
             }
             if (user != null) {
                 String fullName = user.getFirstName() + " " + user.getLastName();
                 binding.btnBack.setText(fullName);
             } else {
                 binding.btnBack.setText(R.string.back);
             }
        });
    }

    // ── Language chooser dialog ───────────────────────────────────

    private void showLanguageChooserDialog() {
        if (!isAdded() || getContext() == null) return;

        // Language codes and display labels
        final String[] languageCodes = {"km", "en"};
        final String[] languageLabels = {
                getString(R.string.language_khmer),
                getString(R.string.language_english)
        };

        // Determine which item is currently selected
        String currentLang = LocaleHelper.getSavedLanguage(requireContext());
        int checkedIndex = 0; // default Khmer
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLang)) {
                checkedIndex = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.choose_language))
                .setSingleChoiceItems(languageLabels, checkedIndex, (dialog, which) -> {
                    String selectedLang = languageCodes[which];
                    String savedLang = LocaleHelper.getSavedLanguage(requireContext());

                    if (!selectedLang.equals(savedLang)) {
                        // Persist and apply the new locale
                        LocaleHelper.setLocale(requireContext(), selectedLang);
                        dialog.dismiss();

                        // Recreate the activity to apply language change instantly
                        if (getActivity() != null) {
                            getActivity().recreate();
                        }
                    } else {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateLanguageLabel() {
        if (binding == null || getContext() == null) return;
        String lang = LocaleHelper.getSavedLanguage(requireContext());
        if ("en".equals(lang)) {
            binding.tvLanguage.setText(R.string.language_english);
        } else {
            binding.tvLanguage.setText(R.string.language_khmer);
        }
    }

    // ── Theme chooser dialog ──────────────────────────────────────

    private void showThemeChooserDialog() {
        if (!isAdded() || getContext() == null) return;

        int currentMode = ThemeHelper.getSavedThemeMode(requireContext());

        final String[] themeLabels = {
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.choose_theme))
                .setSingleChoiceItems(themeLabels, currentMode, (dialog, which) -> {
                    ThemeHelper.setThemeMode(requireContext(), which);
                    updateThemeLabel();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateThemeLabel() {
        if (binding == null || getContext() == null) return;
        int mode = ThemeHelper.getSavedThemeMode(requireContext());
        switch (mode) {
            case ThemeHelper.MODE_LIGHT:
                binding.tvThemeLabel.setText(R.string.theme_light);
                break;
            case ThemeHelper.MODE_DARK:
                binding.tvThemeLabel.setText(R.string.theme_dark);
                break;
            case ThemeHelper.MODE_SYSTEM:
            default:
                binding.tvThemeLabel.setText(R.string.theme_system);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}