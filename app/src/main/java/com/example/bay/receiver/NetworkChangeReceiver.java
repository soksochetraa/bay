package com.example.bay.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.example.bay.HomeActivity;
import com.example.bay.service.ChatNotificationService;
import com.example.bay.util.FirebaseDBHelper;
import com.google.firebase.auth.FirebaseAuth;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            handleNetworkChange(context, isConnected);
        }
    }

    private void handleNetworkChange(Context context, boolean isConnected) {
        if (isConnected) {
            onNetworkConnected(context);
        } else {
            onNetworkDisconnected(context);
        }
    }

    private void onNetworkConnected(Context context) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId != null) {
            FirebaseDBHelper.getOnlineStatusRef(currentUserId).setValue(true);
        }

        if (context instanceof HomeActivity) {
            ((HomeActivity) context).showNetworkConnectedMessage();
        }

        startChatService(context);
    }

    private void onNetworkDisconnected(Context context) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId != null) {
            FirebaseDBHelper.getOnlineStatusRef(currentUserId).onDisconnect().setValue(false);
        }

        if (context instanceof HomeActivity) {
            ((HomeActivity) context).showNetworkDisconnectedMessage();
        }
    }

    private void startChatService(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent serviceIntent = new Intent(context, ChatNotificationService.class);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }

    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}