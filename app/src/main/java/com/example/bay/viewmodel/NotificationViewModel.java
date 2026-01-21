package com.example.bay.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bay.model.Notification;
import com.example.bay.repository.NotificationRepository;
import com.example.bay.util.FirebaseDBHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationViewModel extends ViewModel {

    private MutableLiveData<List<Notification>> notifications = new MutableLiveData<>();
    private MutableLiveData<Integer> unreadCount = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();

    private String currentUserId;
    private NotificationRepository notificationRepository = new NotificationRepository();

    private ValueEventListener notificationsListener;
    private ValueEventListener unreadCountListener;

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        loadNotifications();
        loadUnreadCount();
    }

    public void loadNotifications() {
        if (notificationsListener != null && currentUserId != null) {
            FirebaseDBHelper.getUserNotificationsRef(currentUserId)
                    .removeEventListener(notificationsListener);
        }

        notificationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Notification> notifList = new ArrayList<>();
                for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                    Notification notification = notifSnapshot.getValue(Notification.class);
                    if (notification != null) {
                        notification.setNotificationId(notifSnapshot.getKey());
                        notifList.add(0, notification); // Reverse chronological order
                    }
                }
                notifications.setValue(notifList);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d("NotificationViewModel", "onCancelled: " + error.getMessage());
            }
        };

        FirebaseDBHelper.getUserNotificationsRef(currentUserId)
                .orderByChild("timestamp")
                .addValueEventListener(notificationsListener);
    }

    private void loadUnreadCount() {
        if (unreadCountListener != null && currentUserId != null) {
            FirebaseDBHelper.getUserNotificationsRef(currentUserId)
                    .orderByChild("read")
                    .equalTo(false)
                    .removeEventListener(unreadCountListener);
        }

        unreadCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();
                unreadCount.setValue(count);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d("NotificationViewModel", "onCancelled: " + error.getMessage());
            }
        };

        FirebaseDBHelper.getUserNotificationsRef(currentUserId)
                .orderByChild("read")
                .equalTo(false)
                .addValueEventListener(unreadCountListener);
    }

    public void markNotificationAsRead(String notificationId) {
        notificationRepository.markNotificationAsRead(currentUserId, notificationId,
                new NotificationRepository.NotificationCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        // Count will be updated via listener
                    }

                    @Override
                    public void onError(String errorMsg) {
                        error.setValue(errorMsg);
                    }
                });
    }

    public void markAllNotificationsAsRead() {
        notificationRepository.markAllNotificationsAsRead(currentUserId,
                new NotificationRepository.NotificationCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        // Count will be updated via listener
                    }

                    @Override
                    public void onError(String errorMsg) {
                        error.setValue(errorMsg);
                    }
                });
    }

    public void deleteNotification(String notificationId) {
        notificationRepository.deleteNotification(currentUserId, notificationId,
                new NotificationRepository.NotificationCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        // Notification will be removed from LiveData via listener
                    }

                    @Override
                    public void onError(String errorMsg) {
                        error.setValue(errorMsg);
                    }
                });
    }

    public void deleteAllNotifications() {
        notificationRepository.deleteAllNotifications(currentUserId,
                new NotificationRepository.NotificationCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        // Notifications will be cleared from LiveData via listener
                    }

                    @Override
                    public void onError(String errorMsg) {
                        error.setValue(errorMsg);
                    }
                });
    }

    public LiveData<List<Notification>> getNotifications() {
        return notifications;
    }

    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }

    public LiveData<String> getError() {
        return error;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (notificationsListener != null && currentUserId != null) {
            FirebaseDBHelper.getUserNotificationsRef(currentUserId)
                    .removeEventListener(notificationsListener);
        }
        if (unreadCountListener != null && currentUserId != null) {
            FirebaseDBHelper.getUserNotificationsRef(currentUserId)
                    .removeEventListener(unreadCountListener);
        }
    }
}