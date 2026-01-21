package com.example.bay.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.fragment.PersonalMessageFragment;
import com.example.bay.model.Notification;
import com.example.bay.repository.NotificationRepository;
import com.example.bay.util.FirebaseDBHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "bay_chat_notifications";
    private static final String CHANNEL_NAME = "Bay Chat Notifications";
    private static final String CHANNEL_DESC = "Notifications for chat messages";

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        saveTokenToFirebase(token);
        sendRegistrationToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage.getNotification());
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        try {
            String type = data.get("type");

            if ("chat_message".equals(type)) {
                String senderId = data.get("senderId");
                String senderName = data.get("senderName");
                String message = data.get("message");
                String chatId = data.get("chatId");
                String messageId = data.get("messageId");
                String imageUrl = data.get("imageUrl");

                saveNotificationLocally(senderId, senderName, message, chatId, messageId, imageUrl);
                showChatNotification(senderName, message, chatId, senderId);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling data message: " + e.getMessage());
        }
    }

    private void handleNotificationMessage(RemoteMessage.Notification notification) {
        if (notification != null) {
            String title = notification.getTitle();
            String body = notification.getBody();
            showSimpleNotification(title, body);
        }
    }

    private void showChatNotification(String senderName, String message, String chatId, String senderId) {
        createNotificationChannel();

        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("notification", true);
        intent.putExtra("chatId", chatId);
        intent.putExtra("userId", senderId);
        intent.putExtra("navigateTo", "personal_message");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_bell)
                        .setContentTitle(senderName)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = new Random().nextInt();

        if (notificationManager != null) {
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

    private void showSimpleNotification(String title, String body) {
        createNotificationChannel();

        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_bell)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = new Random().nextInt();

        if (notificationManager != null) {
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void saveTokenToFirebase(String token) {
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();

        if (currentUserId != null) {
            FirebaseDBHelper.getUserTokenRef(currentUserId).setValue(token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved to Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save token: " + e.getMessage()));
        }
    }

    private void sendRegistrationToServer(String token) {
        Log.d(TAG, "Token ready for server: " + token);
    }

    private void saveNotificationLocally(String senderId, String senderName,
                                         String message, String chatId,
                                         String messageId, String imageUrl) {
        NotificationRepository notificationRepository = new NotificationRepository();

        Notification notification = new Notification(
                senderId,
                "",
                "chat_message",
                senderName,
                message,
                chatId,
                messageId
        );

        if (imageUrl != null) {
            notification.setImageUrl(imageUrl);
        }
    }
}