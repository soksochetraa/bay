package com.example.bay.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bay.model.User;
import com.example.bay.service.UserService;
import com.example.bay.util.RetrofitClient;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private final UserService userService;

    public UserRepository() {
        userService = RetrofitClient.getClient().create(UserService.class);
    }

    // 🔹 Create a new user
    public void createUser(User user, UserCallback<User> callback) {
        userService.createUser(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to create user: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError("Error: " + t.getMessage());
            }
        });
    }

    // 🔹 Get all users
    public void getAllUsers(UserCallback<Map<String, User>> callback) {
        userService.getAllUsers().enqueue(new Callback<Map<String, User>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, User>> call, @NonNull Response<Map<String, User>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to fetch users: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, User>> call, @NonNull Throwable t) {
                callback.onError("Error: " + t.getMessage());
            }
        });
    }

    // 🔹 Get user by ID
    public void getUserById(String userId, UserCallback<User> callback) {
        userService.getUserById(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("User not found: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError("Error: " + t.getMessage());
            }
        });
    }

    // 🔹 Update user
    public void updateUser(String userId, User user, UserCallback<User> callback) {
        userService.updateUser(userId, user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to update user: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError("Error: " + t.getMessage());
            }
        });
    }

    // 🔹 Delete user
    public void deleteUser(String userId, UserCallback<Void> callback) {
        userService.deleteUser(userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Failed to delete user: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError("Error: " + t.getMessage());
            }
        });
    }

    // 🔹 Generic callback interface
    public interface UserCallback<T> {
        void onSuccess(T result);
        void onError(String errorMsg);
    }
}
