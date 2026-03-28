package com.example.bay.service;

import com.example.bay.model.Notification;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NotificationService {

    @GET("notifications/{userId}.json")
    Call<Map<String, Notification>> getUserNotifications(
            @Path("userId") String userId
    );

    @GET("notifications/{userId}/{notificationId}.json")
    Call<Notification> getNotificationById(
            @Path("userId") String userId,
            @Path("notificationId") String notificationId
    );

    @PUT("notifications/{userId}/{notificationId}.json")
    Call<Notification> sendNotification(
            @Path("userId") String userId,
            @Path("notificationId") String notificationId,
            @retrofit2.http.Body Notification notification
    );

    @PUT("notifications/{userId}/{notificationId}.json")
    Call<Notification> updateNotification(
            @Path("userId") String userId,
            @Path("notificationId") String notificationId,
            @retrofit2.http.Body Notification notification
    );

    @DELETE("notifications/{userId}/{notificationId}.json")
    Call<Void> deleteNotification(
            @Path("userId") String userId,
            @Path("notificationId") String notificationId
    );

    @GET("notifications/{userId}.json")
    Call<Map<String, Notification>> getUnreadNotifications(
            @Path("userId") String userId,
            @Query("orderBy") String orderBy,
            @Query("equalTo") String isRead
    );

    static String quote(String value) {
        return "\"" + value + "\"";
    }
}