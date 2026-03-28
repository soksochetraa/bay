package com.example.bay.repository;

import androidx.annotation.NonNull;

import com.example.bay.model.Notification;
import com.example.bay.service.NotificationService;
import com.example.bay.util.RetrofitClient;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationRepository {

    private final NotificationService notificationService;

    public NotificationRepository() {
        notificationService = RetrofitClient.getClient().create(NotificationService.class);
    }

    public void sendNotification(String userId, String notificationId,
                                 Notification notification,
                                 IApiCallback<Notification> callback) {

        notificationService.sendNotification(userId, notificationId, notification)
                .enqueue(new Callback<Notification>() {
                    @Override
                    public void onResponse(@NonNull Call<Notification> call,
                                           @NonNull Response<Notification> response) {

                        if (response.isSuccessful()) {
                            callback.onSuccess(notification);
                        } else {
                            callback.onError(response.message());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Notification> call,
                                          @NonNull Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void getUserNotifications(String userId,
                                     IApiCallback<Map<String, Notification>> callback) {

        notificationService.getUserNotifications(userId)
                .enqueue(new Callback<Map<String, Notification>>() {
                    @Override
                    public void onResponse(@NonNull Call<Map<String, Notification>> call,
                                           @NonNull Response<Map<String, Notification>> response) {

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError(response.message());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, Notification>> call,
                                          @NonNull Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void getNotificationById(String userId, String notificationId,
                                    IApiCallback<Notification> callback) {

        notificationService.getNotificationById(userId, notificationId)
                .enqueue(new Callback<Notification>() {
                    @Override
                    public void onResponse(@NonNull Call<Notification> call,
                                           @NonNull Response<Notification> response) {

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError(response.message());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Notification> call,
                                          @NonNull Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void updateNotification(String userId, String notificationId,
                                   Notification notification,
                                   IApiCallback<Notification> callback) {

        notificationService.updateNotification(userId, notificationId, notification)
                .enqueue(new Callback<Notification>() {
                    @Override
                    public void onResponse(@NonNull Call<Notification> call,
                                           @NonNull Response<Notification> response) {

                        if (response.isSuccessful()) {
                            callback.onSuccess(notification);
                        } else {
                            callback.onError(response.message());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Notification> call,
                                          @NonNull Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void deleteNotification(String userId, String notificationId,
                                   IApiCallback<Void> callback) {

        notificationService.deleteNotification(userId, notificationId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {

                        if (response.isSuccessful()) {
                            callback.onSuccess(null);
                        } else {
                            callback.onError(response.message());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call,
                                          @NonNull Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }
}