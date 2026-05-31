package com.example.bay.repository;

import androidx.annotation.NonNull;

import com.example.bay.model.Chat;
import com.example.bay.model.Message;
import com.example.bay.util.CryptoUtil;
import com.example.bay.util.AESUtils;
import com.example.bay.util.FirebaseDBHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {

    public interface ChatCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public void getOrCreateChat(String userId1, String userId2, ChatCallback<Chat> callback) {
        String chatId = generateChatId(userId1, userId2);

        FirebaseDBHelper.getChatRef(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat != null) {
                        chat.setChatId(chatId);
                        callback.onSuccess(chat);
                        return;
                    }
                    callback.onError("Invalid chat data");
                } else {
                    Chat newChat = new Chat(userId1, userId2);
                    newChat.setChatId(chatId);
                    callback.onSuccess(newChat);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void sendMessage(String chatId, Message message, ChatCallback<String> callback) {
        DatabaseReference messagesRef = FirebaseDBHelper.getChatMessagesRef(chatId);
        String messageId = messagesRef.push().getKey();

        if (messageId == null) {
            callback.onError("Failed to create message id");
            return;
        }

        message.setMessageId(messageId);
        // Encrypt the plain text before sending to Firebase
        String encrypted = AESUtils.encrypt(message.getText());
        message.setEncryptedText(encrypted);
        // Optionally clear plain text to avoid accidental storage
        // message.setText(null);

        messagesRef.child(messageId).setValue(message.toMap())
                .addOnSuccessListener(aVoid -> {
                    FirebaseDBHelper.getUserChatsRef(message.getSenderId()).child(chatId).setValue(ServerValue.TIMESTAMP);
                    FirebaseDBHelper.getUserChatsRef(message.getReceiverId()).child(chatId).setValue(ServerValue.TIMESTAMP);
                    updateChatLastMessage(chatId, message);
                    updateUnreadCount(chatId, message.getReceiverId());
                    sendFCMPushNotification(message);
                    callback.onSuccess(messageId);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void updateChatLastMessage(String chatId, Message message) {
        Map<String, Object> updates = new HashMap<>();
        
        // Basic user linkage properties to ensure chat can be instantiated natively 
        // if this is the very first message sent in the chat.
        updates.put("user1Id", message.getSenderId());
        updates.put("user2Id", message.getReceiverId());
        updates.put("participants/" + message.getSenderId(), true);
        updates.put("participants/" + message.getReceiverId(), true);
        
        // Store encrypted version of the last message for security
        updates.put("lastMessage", message.getEncryptedText());
        updates.put("lastMessageType", message.getType());
        updates.put("lastMessageSenderId", message.getSenderId());
        updates.put("lastMessageTime", ServerValue.TIMESTAMP);
        
        FirebaseDBHelper.getChatRef(chatId).updateChildren(updates);
    }

    private void updateUnreadCount(String chatId, String receiverId) {
        DatabaseReference unreadRef = FirebaseDBHelper.getUnreadCountRef(receiverId, chatId);
        unreadRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int currentCount = 0;
                if (snapshot.exists()) {
                    Integer v = snapshot.getValue(Integer.class);
                    if (v != null) currentCount = v;
                }
                unreadRef.setValue(currentCount + 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void markMessagesAsRead(String chatId, String userId) {
        FirebaseDBHelper.getChatMessagesRef(chatId)
                .orderByChild("receiverId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            Message message = messageSnapshot.getValue(Message.class);
                            if (message != null && !message.isRead()) {
                                messageSnapshot.getRef().child("read").setValue(true);
                            }
                        }
                        FirebaseDBHelper.getUnreadCountRef(userId, chatId).setValue(0);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    public void getUserChats(String userId, ChatCallback<List<Chat>> callback) {
        FirebaseDBHelper.getUserChatsRef(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Chat> chats = new ArrayList<>();
                List<String> chatIds = new ArrayList<>();

                for (DataSnapshot chatIdSnapshot : snapshot.getChildren()) {
                    String chatId = chatIdSnapshot.getKey();
                    if (chatId != null) chatIds.add(chatId);
                }

                if (chatIds.isEmpty()) {
                    callback.onSuccess(chats);
                    return;
                }

                final int[] loadedCount = {0};
                for (String chatId : chatIds) {
                    FirebaseDBHelper.getChatRef(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Chat chat = snapshot.getValue(Chat.class);
                            if (chat != null) {
                                chat.setChatId(chatId);
                                chats.add(chat);
                                Collections.sort(chats, (c1, c2) -> Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));
                            }

                            loadedCount[0]++;
                            if (loadedCount[0] == chatIds.size()) {
                                callback.onSuccess(new ArrayList<>(chats));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadedCount[0]++;
                            if (loadedCount[0] == chatIds.size()) {
                                callback.onSuccess(new ArrayList<>(chats));
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void getChatMessages(String chatId, ChatCallback<List<Message>> callback) {
        FirebaseDBHelper.getChatMessagesRef(chatId)
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Message> messages = new ArrayList<>();
                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            Message message = messageSnapshot.getValue(Message.class);
                            if (message != null) {
                                // Decrypt the encrypted text if available
                                String decrypted = AESUtils.decrypt(message.getEncryptedText());
                                if (decrypted != null) {
                                    message.setText(decrypted);
                                } else {
                                    // Fallback to plain text (should not happen)
                                    message.setText(message.getText());
                                }
                                message.setMessageId(messageSnapshot.getKey());
                                messages.add(message);
                            }
                        }
                        callback.onSuccess(messages);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void setTypingStatus(String chatId, String userId, boolean isTyping) {
        FirebaseDBHelper.getTypingRef(chatId).child(userId).setValue(isTyping);
    }

    public void listenForTyping(String chatId, String userId, ChatCallback<Boolean> callback) {
        FirebaseDBHelper.getTypingRef(chatId).child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isTyping = snapshot.getValue(Boolean.class);
                    callback.onSuccess(isTyping != null && isTyping);
                } else {
                    callback.onSuccess(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void deleteChatForBothUsers(String chatId, String user1Id, String user2Id, ChatCallback<Void> callback) {
        DatabaseReference root = FirebaseDBHelper.getRootRef();

        Map<String, Object> updates = new HashMap<>();
        updates.put("user-chats/" + user1Id + "/" + chatId, null);
        updates.put("user-chats/" + user2Id + "/" + chatId, null);

        updates.put("unread-counts/" + user1Id + "/" + chatId, null);
        updates.put("unread-counts/" + user2Id + "/" + chatId, null);

        updates.put("typing/" + chatId, null);

        updates.put("messages/" + chatId, null);
        updates.put("chats/" + chatId, null);

        root.updateChildren(updates).addOnSuccessListener(unused -> {
            if (callback != null) callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError(e.getMessage());
        });
    }

    private void sendFCMPushNotification(Message message) {
        UserRepository userRepository = new UserRepository();
        userRepository.getUserById(message.getSenderId(), new UserRepository.UserCallback<com.example.bay.model.User>() {
            @Override
            public void onSuccess(com.example.bay.model.User sender) {
                String senderName = sender.getFirstName() + " " + sender.getLastName();
                String notificationMessage = message.getText();

                if (notificationMessage == null || notificationMessage.isEmpty()) {
                    notificationMessage = "📷 Sent an image";
                }

                DatabaseReference fcmQueueRef = FirebaseDBHelper.getFcmQueueRef();
                String fcmId = fcmQueueRef.push().getKey();

                if (fcmId != null) {
                    Map<String, Object> fcmData = new HashMap<>();
                    fcmData.put("type", "chat_message");
                    fcmData.put("senderId", message.getSenderId());
                    fcmData.put("senderName", senderName);
                    fcmData.put("message", notificationMessage);
                    fcmData.put("chatId", generateChatId(message.getSenderId(), message.getReceiverId()));
                    fcmData.put("messageId", message.getMessageId());
                    fcmData.put("timestamp", ServerValue.TIMESTAMP);
                    fcmQueueRef.child(fcmId).setValue(fcmData);
                }
            }

            @Override
            public void onError(String errorMsg) {
            }
        });
    }

    private String generateChatId(String user1Id, String user2Id) {
        if (user1Id.compareTo(user2Id) < 0) {
            return user1Id + "_" + user2Id;
        } else {
            return user2Id + "_" + user1Id;
        }
    }
}