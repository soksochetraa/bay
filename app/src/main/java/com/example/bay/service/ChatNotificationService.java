package com.example.bay.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.model.Message;
import com.example.bay.repository.UserRepository;
import com.example.bay.util.FirebaseDBHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class ChatNotificationService extends Service {

    private static final String CHANNEL_ID = "chat_foreground_service";
    private static final String CHANNEL_NAME = "Chat Service";
    private static final int NOTIFICATION_ID = 101;

    private String currentUserId;
    private ValueEventListener messageListener;

    @Override
    public void onCreate() {
        super.onCreate();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        if (currentUserId != null) {
            setupMessageListener();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Service for real-time chat notifications");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bay Chat")
                .setContentText("Chat service is running")
                .setSmallIcon(R.drawable.ic_bell)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();
    }

    private void setupMessageListener() {
        messageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();

                    FirebaseDBHelper.getChatMessagesRef(chatId)
                            .orderByChild("timestamp")
                            .limitToLast(1)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot messageSnapshot) {
                                    for (DataSnapshot msgSnapshot : messageSnapshot.getChildren()) {
                                        Message message = msgSnapshot.getValue(Message.class);
                                        if (message != null &&
                                                message.getReceiverId().equals(currentUserId) &&
                                                !message.isRead()) {

                                            updateNotification(message);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        FirebaseDBHelper.getUserChatsRef(currentUserId)
                .addValueEventListener(messageListener);
    }

    private void updateNotification(Message message) {
        UserRepository userRepository = new UserRepository();
        userRepository.getUserById(message.getSenderId(), new UserRepository.UserCallback<com.example.bay.model.User>() {
            @Override
            public void onSuccess(com.example.bay.model.User user) {
                String senderName = user.getFirst_name() + " " + user.getLast_name();

                Intent notificationIntent = new Intent(ChatNotificationService.this, HomeActivity.class);
                notificationIntent.putExtra("chatId", getChatIdFromMessage(message));
                notificationIntent.putExtra("navigateTo", "chat");

                PendingIntent pendingIntent = PendingIntent.getActivity(
                        ChatNotificationService.this, 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

                Notification notification = new NotificationCompat.Builder(
                        ChatNotificationService.this, CHANNEL_ID)
                        .setContentTitle(senderName)
                        .setContentText(message.getText() != null ? message.getText() : "Sent an image")
                        .setSmallIcon(R.drawable.ic_bell)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .build();

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }

            @Override
            public void onError(String errorMsg) {
            }
        });
    }

    private String getChatIdFromMessage(Message message) {
        String senderId = message.getSenderId();
        String receiverId = message.getReceiverId();

        if (senderId.compareTo(receiverId) < 0) {
            return senderId + "_" + receiverId;
        } else {
            return receiverId + "_" + senderId;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (messageListener != null && currentUserId != null) {
            FirebaseDBHelper.getUserChatsRef(currentUserId)
                    .removeEventListener(messageListener);
        }

        stopForeground(true);
    }
}