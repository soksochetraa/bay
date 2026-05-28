package com.example.bay;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.example.bay.util.LocaleHelper;
import com.example.bay.util.ThemeHelper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    protected void attachBaseContext(Context base) {
        // Apply saved locale at the Application level (earliest possible point)
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MyApplication onCreate");

        // Apply saved theme (light / dark / system) before any Activity is created
        ThemeHelper.applyTheme(this);

        initializeFirebase();
    }

    private void initializeFirebase() {
        try {
            FirebaseApp firebaseApp = FirebaseApp.getInstance();
            Log.d(TAG, "FirebaseApp already initialized: " + firebaseApp.getName());
        } catch (IllegalStateException e) {
            Log.d(TAG, "Initializing FirebaseApp...");
            FirebaseApp.initializeApp(this);
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(true);
        database.setPersistenceCacheSizeBytes(10 * 1024 * 1024);

        Log.d(TAG, "Firebase Database persistence enabled");
    }
}