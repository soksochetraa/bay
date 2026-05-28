package com.example.bay.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/**
 * Manages app-level locale/language preferences and applies them to the
 * {@link Context} so that all resource lookups use the correct language.
 *
 * <p>Usage:
 * <ol>
 *   <li>Call {@link #onAttach(Context)} from every Activity's
 *       {@code attachBaseContext()} <b>and</b> from
 *       {@code Application.attachBaseContext()}.</li>
 *   <li>Call {@link #setLocale(Context, String)} when the user picks a new
 *       language, then recreate the current Activity.</li>
 * </ol>
 *
 * <p>Default language when nothing has been persisted yet is <b>Khmer ("km")</b>.
 */
public final class LocaleHelper {

    private static final String PREFS_NAME      = "bay_language_prefs";
    private static final String KEY_LANGUAGE     = "app_language";
    private static final String DEFAULT_LANGUAGE = "km";

    private LocaleHelper() { /* utility – no instances */ }

    // ── public API ───────────────────────────────────────────────────

    /**
     * Wraps the given {@link Context} with the saved locale.
     * Call this from {@code attachBaseContext()} in every Activity and in
     * your {@code Application} subclass.
     *
     * @return a new Context whose {@code Resources} are configured for the
     *         saved language.
     */
    public static Context onAttach(Context context) {
        String language = getSavedLanguage(context);
        return setContextLocale(context, language);
    }

    /**
     * Persists the chosen language code and returns a Context configured
     * for it.  After calling this, the caller should call
     * {@code activity.recreate()} to apply the change visually.
     *
     * @param languageCode ISO-639-1 code, e.g. "km" or "en".
     */
    public static Context setLocale(Context context, String languageCode) {
        persistLanguage(context, languageCode);
        return setContextLocale(context, languageCode);
    }

    /**
     * Returns the currently-saved language code (defaults to "km").
     */
    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }

    // ── internals ────────────────────────────────────────────────────

    private static void persistLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    /**
     * Creates a new Context whose resources are bound to the given locale.
     * Works on API 24+ (LocaleList) and falls back gracefully for older
     * versions (though minSdk 27 means we always hit the newer branch).
     */
    private static Context setContextLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        } else {
            config.setLocale(locale);
        }

        return context.createConfigurationContext(config);
    }
}
