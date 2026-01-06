package com.example.bay.repository;

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

    public interface UserCallback<T> {
        void onSuccess(T result);
        void onError(String errorMsg);
    }

    public interface BoolCallback {
        void onResult(boolean exists);
        void onError(String errorMsg);
    }

    public void createUser(User user, UserCallback<User> callback) {
        userService.createUser(user.getUserId(), user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) callback.onSuccess(user);
                else callback.onError(response.message());
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getAllUsers(UserCallback<Map<String, User>> callback) {
        userService.getAllUsers().enqueue(new Callback<Map<String, User>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, User>> call,
                                   @NonNull Response<Map<String, User>> response) {
                if (response.isSuccessful() && response.body() != null)
                    callback.onSuccess(response.body());
                else callback.onError(response.message());
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, User>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getUserById(String userId, UserCallback<User> callback) {
        userService.getUserById(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null)
                    callback.onSuccess(response.body());
                else callback.onError(response.message());
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void updateUser(String userId, User user, UserCallback<User> callback) {
        userService.updateUser(userId, user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(response.message());
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void deleteUser(String userId, UserCallback<Void> callback) {
        userService.deleteUser(userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) callback.onSuccess(null);
                else callback.onError(response.message());
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void checkEmailExists(String email, String currentUserId, BoolCallback callback) {
        getAllUsers(new UserCallback<Map<String, User>>() {
            @Override
            public void onSuccess(Map<String, User> users) {
                for (Map.Entry<String, User> entry : users.entrySet()) {
                    User u = entry.getValue();
                    if (u.getEmail() != null &&
                            u.getEmail().equalsIgnoreCase(email) &&
                            !entry.getKey().equals(currentUserId)) {
                        callback.onResult(true);
                        return;
                    }
                }
                callback.onResult(false);
            }

            @Override
            public void onError(String errorMsg) {
                callback.onError(errorMsg);
            }
        });
    }

    public void checkPhoneExists(String phone, String currentUserId, BoolCallback callback) {
        getAllUsers(new UserCallback<Map<String, User>>() {
            @Override
            public void onSuccess(Map<String, User> users) {
                for (Map.Entry<String, User> entry : users.entrySet()) {
                    User u = entry.getValue();
                    if (u.getPhone() != null &&
                            u.getPhone().equals(phone) &&
                            !entry.getKey().equals(currentUserId)) {
                        callback.onResult(true);
                        return;
                    }
                }
                callback.onResult(false);
            }

            @Override
            public void onError(String errorMsg) {
                callback.onError(errorMsg);
            }
        });
    }
}
