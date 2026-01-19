package com.example.bay.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class Chat {
    private String chatId;
    private String user1Id;
    private String user2Id;
    private String lastMessage;
    private String lastMessageType;
    private String lastMessageSenderId;
    private long lastMessageTime;
    private int unreadCount;
    private Map<String, Boolean> participants;
    private long createdAt;

    public Chat() {
    }

    public Chat(String user1Id, String user2Id) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.participants = new HashMap<>();
        this.participants.put(user1Id, true);
        this.participants.put(user2Id, true);
        this.createdAt = System.currentTimeMillis();
        this.unreadCount = 0;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("user1Id", user1Id);
        result.put("user2Id", user2Id);
        result.put("lastMessage", lastMessage);
        result.put("lastMessageType", lastMessageType);
        result.put("lastMessageSenderId", lastMessageSenderId);
        result.put("lastMessageTime", ServerValue.TIMESTAMP);
        result.put("unreadCount", unreadCount);
        result.put("participants", participants);
        result.put("createdAt", ServerValue.TIMESTAMP);
        return result;
    }

    @Exclude
    public String getChatPartnerId(String currentUserId) {
        if (currentUserId.equals(user1Id)) {
            return user2Id;
        } else {
            return user1Id;
        }
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(String user1Id) {
        this.user1Id = user1Id;
    }

    public String getUser2Id() {
        return user2Id;
    }

    public void setUser2Id(String user2Id) {
        this.user2Id = user2Id;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageType() {
        return lastMessageType;
    }

    public void setLastMessageType(String lastMessageType) {
        this.lastMessageType = lastMessageType;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Map<String, Boolean> getParticipants() {
        return participants;
    }

    public void setParticipants(Map<String, Boolean> participants) {
        this.participants = participants;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}