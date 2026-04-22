package com.example.bay.viewmodel;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bay.model.User;
import com.example.bay.util.FirebaseDBHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class SharedUserViewModel extends ViewModel {

    private static final String TAG = "SharedUserViewModel";

    private final MutableLiveData<User> currentUserData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private ValueEventListener userListener;
    private String currentUid;

    public SharedUserViewModel() {
        startListeningToUser();
    }

    public LiveData<User> getCurrentUser() {
        return currentUserData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void startListeningToUser() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            error.setValue("No user logged in");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // If we're already listening to this user, do nothing
        if (userListener != null && uid.equals(currentUid)) {
            return;
        }

        // Clean up previous listener if uid changed
        stopListening();

        currentUid = uid;
        isLoading.setValue(true);

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isLoading.setValue(false);
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    currentUserData.setValue(user);
                } else {
                    error.setValue("User data not found in database");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                isLoading.setValue(false);
                error.setValue(databaseError.getMessage());
                Log.e(TAG, "Error fetching user data: " + databaseError.getMessage());
            }
        };

        FirebaseDBHelper.getUserRef(currentUid).addValueEventListener(userListener);
    }

    public void stopListening() {
        if (userListener != null && currentUid != null) {
            FirebaseDBHelper.getUserRef(currentUid).removeEventListener(userListener);
            userListener = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopListening();
    }
}
