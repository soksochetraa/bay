package com.example.bay.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Manages dark / light theme preferences and applies them globally.
 *
 * Preference values:
 *   0 = Follow system (default)
 *   1 = Light
 *   2 = Dark
 */
public class ThemeHelper {

    private static final String PREFS_NAME = "bay_theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT  = 1;
    public static final int MODE_DARK   = 2;

    /**
     * Read the saved preference and apply the corresponding night-mode flag.
     * Call this in {@link android.app.Application#onCreate()} so the theme
     * is set before any Activity is created.
     */
    public static void applyTheme(Context context) {
        int mode = getSavedThemeMode(context);
        applyThemeMode(mode);
    }

    /**
     * Persist a new theme mode and apply it immediately.
     */
    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyThemeMode(mode);
    }

    /**
     * Returns the currently-saved theme mode (defaults to MODE_SYSTEM).
     */
    public static int getSavedThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    /**
     * Returns {@code true} when the current effective mode is night / dark.
     */
    public static boolean isNightModeActive(Context context) {
        int nightFlag = context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightFlag == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    /* ---- internal ---- */

    private static void applyThemeMode(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
