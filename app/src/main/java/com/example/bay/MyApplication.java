package com.example.bay;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MyApplication onCreate");

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