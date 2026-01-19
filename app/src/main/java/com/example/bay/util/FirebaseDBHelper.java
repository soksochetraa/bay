package com.example.bay.util;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseDBHelper {
    private static FirebaseDatabase database;

    public static FirebaseDatabase getDatabase() {
        if (database == null) {
            database = FirebaseDatabase.getInstance();
            database.setPersistenceEnabled(true);
        }
        return database;
    }

    public static DatabaseReference getUsersRef() {
        return getDatabase().getReference("users");
    }

    public static DatabaseReference getUserRef(String userId) {
        return getUsersRef().child(userId);
    }

    public static DatabaseReference getChatsRef() {
        return getDatabase().getReference("chats");
    }

    public static DatabaseReference getChatRef(String chatId) {
        return getChatsRef().child(chatId);
    }

    public static DatabaseReference getUserChatsRef(String userId) {
        return getDatabase().getReference("user-chats").child(userId);
    }

    public static DatabaseReference getMessagesRef() {
        return getDatabase().getReference("messages");
    }

    public static DatabaseReference getChatMessagesRef(String chatId) {
        return getMessagesRef().child(chatId);
    }

    public static DatabaseReference getMessageRef(String chatId, String messageId) {
        return getChatMessagesRef(chatId).child(messageId);
    }

    public static DatabaseReference getTypingRef(String chatId) {
        return getDatabase().getReference("typing").child(chatId);
    }

    public static DatabaseReference getOnlineStatusRef(String userId) {
        return getDatabase().getReference("online-status").child(userId);
    }

    public static DatabaseReference getUnreadCountRef(String userId, String chatId) {
        return getDatabase().getReference("unread-counts").child(userId).child(chatId);
    }

    public static DatabaseReference getTokensRef() {
        return getDatabase().getReference("tokens");
    }

    public static DatabaseReference getTokenRef(String userId) {
        return getTokensRef().child(userId);
    }
}