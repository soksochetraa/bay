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

    // Users
    public static DatabaseReference getUsersRef() {
        return getDatabase().getReference("users");
    }

    public static DatabaseReference getUserRef(String userId) {
        return getUsersRef().child(userId);
    }

    // Chats
    public static DatabaseReference getChatsRef() {
        return getDatabase().getReference("chats");
    }

    public static DatabaseReference getChatRef(String chatId) {
        return getChatsRef().child(chatId);
    }

    public static DatabaseReference getUserChatsRef(String userId) {
        return getDatabase().getReference("user-chats").child(userId);
    }

    // Messages
    public static DatabaseReference getMessagesRef() {
        return getDatabase().getReference("messages");
    }

    public static DatabaseReference getChatMessagesRef(String chatId) {
        return getMessagesRef().child(chatId);
    }

    public static DatabaseReference getMessageRef(String chatId, String messageId) {
        return getChatMessagesRef(chatId).child(messageId);
    }

    // Typing Indicators
    public static DatabaseReference getTypingRef(String chatId) {
        return getDatabase().getReference("typing").child(chatId);
    }

    // Online Status
    public static DatabaseReference getOnlineStatusRef(String userId) {
        return getDatabase().getReference("online-status").child(userId);
    }

    // Unread Counts
    public static DatabaseReference getUnreadCountRef(String userId, String chatId) {
        return getDatabase().getReference("unread-counts").child(userId).child(chatId);
    }

    // Device Tokens for FCM Notifications
    public static DatabaseReference getTokensRef() {
        return getDatabase().getReference("tokens");
    }

    public static DatabaseReference getTokenRef(String userId) {
        return getTokensRef().child(userId);
    }

    // Alias method for getUserDeviceTokenRef (for backward compatibility)
    public static DatabaseReference getUserDeviceTokenRef(String userId) {
        return getTokensRef().child(userId);
    }

    // Posts
    public static DatabaseReference getPostsRef() {
        return getDatabase().getReference("posts");
    }

    public static DatabaseReference getPostRef(String postId) {
        return getPostsRef().child(postId);
    }

    public static DatabaseReference getUserPostsRef(String userId) {
        return getDatabase().getReference("user-posts").child(userId);
    }

    // Marketplace
    public static DatabaseReference getMarketplaceRef() {
        return getDatabase().getReference("marketplace");
    }

    public static DatabaseReference getMarketplaceItemRef(String itemId) {
        return getMarketplaceRef().child(itemId);
    }

    public static DatabaseReference getUserMarketplaceRef(String userId) {
        return getDatabase().getReference("user-marketplace").child(userId);
    }

    // Communities
    public static DatabaseReference getCommunitiesRef() {
        return getDatabase().getReference("communities");
    }

    public static DatabaseReference getCommunityRef(String communityId) {
        return getCommunitiesRef().child(communityId);
    }

    public static DatabaseReference getUserCommunitiesRef(String userId) {
        return getDatabase().getReference("user-communities").child(userId);
    }

    // Likes
    public static DatabaseReference getLikesRef() {
        return getDatabase().getReference("likes");
    }

    public static DatabaseReference getPostLikesRef(String postId) {
        return getLikesRef().child("posts").child(postId);
    }

    public static DatabaseReference getCommentLikesRef(String commentId) {
        return getLikesRef().child("comments").child(commentId);
    }

    // Comments
    public static DatabaseReference getCommentsRef() {
        return getDatabase().getReference("comments");
    }

    public static DatabaseReference getPostCommentsRef(String postId) {
        return getCommentsRef().child("posts").child(postId);
    }

    // User Settings
    public static DatabaseReference getUserSettingsRef(String userId) {
        return getDatabase().getReference("user-settings").child(userId);
    }

    // App Configuration
    public static DatabaseReference getAppConfigRef() {
        return getDatabase().getReference("app-config");
    }

    // Reports
    public static DatabaseReference getReportsRef() {
        return getDatabase().getReference("reports");
    }

    // Analytics/Stats
    public static DatabaseReference getStatsRef() {
        return getDatabase().getReference("stats");
    }

    // Search History
    public static DatabaseReference getSearchHistoryRef(String userId) {
        return getDatabase().getReference("search-history").child(userId);
    }

    // Blocked Users
    public static DatabaseReference getBlockedUsersRef() {
        return getDatabase().getReference("blocked-users");
    }

    public static DatabaseReference getUserBlockedRef(String userId) {
        return getBlockedUsersRef().child(userId);
    }

    // Friend Requests
    public static DatabaseReference getFriendRequestsRef() {
        return getDatabase().getReference("friend-requests");
    }

    public static DatabaseReference getSentRequestsRef(String userId) {
        return getFriendRequestsRef().child("sent").child(userId);
    }

    public static DatabaseReference getReceivedRequestsRef(String userId) {
        return getFriendRequestsRef().child("received").child(userId);
    }

    // Friends
    public static DatabaseReference getFriendsRef() {
        return getDatabase().getReference("friends");
    }

    public static DatabaseReference getUserFriendsRef(String userId) {
        return getFriendsRef().child(userId);
    }

    public static DatabaseReference getFcmQueueRef() {
        return FirebaseDatabase.getInstance().getReference("fcm_queue");
    }

    public static DatabaseReference getNotificationsRef() {
        return FirebaseDatabase.getInstance().getReference("notifications");
    }

    public static DatabaseReference getUserNotificationsRef(String userId) {
        return getNotificationsRef().child(userId);
    }

    public static DatabaseReference getNotificationRef(String userId, String notificationId) {
        return getUserNotificationsRef(userId).child(notificationId);
    }

    public static DatabaseReference getUserTokenRef(String userId) {
        return FirebaseDatabase.getInstance().getReference("tokens").child(userId);
    }

}