package com.example.bay;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bay.util.LocaleHelper;

/**
 * Base Activity that applies the user's saved locale to every screen.
 *
 * <p>All Activities in the app should extend this class instead of
 * {@link AppCompatActivity} directly, so the locale is applied
 * consistently without duplicating code.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Wrap the context with the saved locale before the Activity inflates
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}
