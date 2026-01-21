package com.example.bay.repository;

import androidx.annotation.NonNull;

import com.example.bay.model.Notification;
import com.example.bay.model.User;
import com.example.bay.util.FirebaseDBHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    public interface NotificationCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // Send chat notification
    public void sendChatNotification(String senderId, String receiverId,
                                     String message, String chatId, String messageId,
                                     NotificationCallback<Boolean> callback) {

        UserRepository userRepository = new UserRepository();

        // Get sender info for notification title
        userRepository.getUserById(senderId, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User sender) {
                String senderName = sender.getFirst_name() + " " + sender.getLast_name();

                // Create notification
                Notification notification = new Notification(
                        senderId,
                        receiverId,
                        "chat_message",
                        senderName,
                        message,
                        chatId,
                        messageId
                );

                // Save notification to database
                DatabaseReference notificationsRef = FirebaseDBHelper.getUserNotificationsRef(receiverId);
                String notificationId = notificationsRef.push().getKey();

                if (notificationId != null) {
                    notification.setNotificationId(notificationId);
                    notificationsRef.child(notificationId).setValue(notification.toMap())
                            .addOnSuccessListener(aVoid -> {
                                // Send FCM push notification
                                sendFCMPushNotification(receiverId, senderName, message, chatId);
                                callback.onSuccess(true);
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onError(String errorMsg) {
                callback.onError("Failed to get sender info: " + errorMsg);
            }
        });
    }

    // Send FCM push notification
    private void sendFCMPushNotification(String receiverId, String senderName,
                                         String message, String chatId) {
        // Get receiver's device token
        FirebaseDBHelper.getUserTokenRef(receiverId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String deviceToken = snapshot.getValue(String.class);
                            if (deviceToken != null && !deviceToken.isEmpty()) {
                                // Send FCM message
                                sendFCMToDevice(deviceToken, senderName, message, chatId);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Log error
                    }
                });
    }

    // Method to send FCM
    private void sendFCMToDevice(String deviceToken, String senderName,
                                 String message, String chatId) {
        // This should be implemented with your FCM server
        // For now, we'll just store it in the database for the NotificationService to pick up

        DatabaseReference fcmQueueRef = FirebaseDBHelper.getFcmQueueRef();
        String fcmId = fcmQueueRef.push().getKey();

        if (fcmId != null) {
            FCMNotification fcmNotification = new FCMNotification(
                    deviceToken,
                    senderName,
                    message,
                    chatId,
                    System.currentTimeMillis()
            );

            fcmQueueRef.child(fcmId).setValue(fcmNotification.toMap())
                    .addOnSuccessListener(aVoid -> {
                        // FCM request queued successfully
                    })
                    .addOnFailureListener(e -> {
                        // Log error
                    });
        }
    }

    // Get user notifications
    public void getUserNotifications(String userId, NotificationCallback<List<Notification>> callback) {
        FirebaseDBHelper.getUserNotificationsRef(userId)
                .orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Notification> notifications = new ArrayList<>();
                        for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                            Notification notification = notifSnapshot.getValue(Notification.class);
                            if (notification != null) {
                                notification.setNotificationId(notifSnapshot.getKey());
                                notifications.add(0, notification); // Add to beginning for reverse chronological order
                            }
                        }
                        callback.onSuccess(notifications);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    // Mark notification as read
    public void markNotificationAsRead(String userId, String notificationId,
                                       NotificationCallback<Boolean> callback) {
        FirebaseDBHelper.getNotificationRef(userId, notificationId)
                .child("read")
                .setValue(true)
                .addOnSuccessListener(aVoid -> callback.onSuccess(true))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Mark all notifications as read
    public void markAllNotificationsAsRead(String userId, NotificationCallback<Boolean> callback) {
        FirebaseDBHelper.getUserNotificationsRef(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> notificationIds = new ArrayList<>();
                        for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                            notificationIds.add(notifSnapshot.getKey());
                        }

                        if (notificationIds.isEmpty()) {
                            callback.onSuccess(true);
                            return;
                        }

                        // Update each notification
                        for (String notifId : notificationIds) {
                            FirebaseDBHelper.getNotificationRef(userId, notifId)
                                    .child("read")
                                    .setValue(true);
                        }
                        callback.onSuccess(true);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    // Delete notification
    public void deleteNotification(String userId, String notificationId,
                                   NotificationCallback<Boolean> callback) {
        FirebaseDBHelper.getNotificationRef(userId, notificationId)
                .removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess(true))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Delete all notifications
    public void deleteAllNotifications(String userId, NotificationCallback<Boolean> callback) {
        FirebaseDBHelper.getUserNotificationsRef(userId)
                .removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess(true))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Get unread notification count
    public void getUnreadNotificationCount(String userId, NotificationCallback<Integer> callback) {
        FirebaseDBHelper.getUserNotificationsRef(userId)
                .orderByChild("read")
                .equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int count = (int) snapshot.getChildrenCount();
                        callback.onSuccess(count);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    // FCM Notification model for queue
    static class FCMNotification {
        private String deviceToken;
        private String title;
        private String message;
        private String chatId;
        private long timestamp;

        public FCMNotification() {}

        public FCMNotification(String deviceToken, String title, String message,
                               String chatId, long timestamp) {
            this.deviceToken = deviceToken;
            this.title = title;
            this.message = message;
            this.chatId = chatId;
            this.timestamp = timestamp;
        }

        public java.util.Map<String, Object> toMap() {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("deviceToken", deviceToken);
            result.put("title", title);
            result.put("message", message);
            result.put("chatId", chatId);
            result.put("timestamp", timestamp);
            return result;
        }

        public String getDeviceToken() { return deviceToken; }
        public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}