package com.example.bay.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bay.model.Chat;
import com.example.bay.model.Message;
import com.example.bay.model.User;
import com.example.bay.repository.ChatRepository;
import com.example.bay.repository.UserRepository;
import com.example.bay.util.FirebaseDBHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MessageViewModel extends ViewModel {

    private MutableLiveData<List<Chat>> chats = new MutableLiveData<>();
    private MutableLiveData<List<User>> allUsers = new MutableLiveData<>();
    private MutableLiveData<List<Message>> allMessages = new MutableLiveData<>();
    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();

    private String currentUserId;
    private ChatRepository chatRepository = new ChatRepository();
    private UserRepository userRepository = new UserRepository();

    private ValueEventListener chatsListener;
    private ValueEventListener usersListener;
    private ValueEventListener messagesListener;
    private ValueEventListener currentUserListener;

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        loadCurrentUser();
        loadChats();
        loadAllUsers();
        loadAllMessages();
    }

    private void loadCurrentUser() {
        currentUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    user.setUserId(currentUserId);
                    currentUser.setValue(user);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };

        FirebaseDBHelper.getUserRef(currentUserId).addValueEventListener(currentUserListener);
    }

    private void loadChats() {
        chatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> chatIds = new ArrayList<>();
                for (DataSnapshot chatIdSnapshot : snapshot.getChildren()) {
                    String chatId = chatIdSnapshot.getKey();
                    chatIds.add(chatId);
                }

                if (chatIds.isEmpty()) {
                    chats.setValue(new ArrayList<>());
                    return;
                }

                loadChatDetails(chatIds);
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };

        FirebaseDBHelper.getUserChatsRef(currentUserId).addValueEventListener(chatsListener);
    }

    private void loadChatDetails(List<String> chatIds) {
        List<Chat> loadedChats = new ArrayList<>();
        final int[] loadedCount = {0};

        for (String chatId : chatIds) {
            FirebaseDBHelper.getChatRef(chatId).addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Chat chat = snapshot.getValue(Chat.class);
                            if (chat != null) {
                                chat.setChatId(snapshot.getKey());
                                loadedChats.add(chat);
                            }

                            loadedCount[0]++;
                            if (loadedCount[0] == chatIds.size()) {
                                loadedChats.sort((c1, c2) -> Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));
                                chats.setValue(loadedChats);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            loadedCount[0]++;
                            if (loadedCount[0] == chatIds.size()) {
                                chats.setValue(loadedChats);
                            }
                        }
                    });
        }
    }

    private void loadAllUsers() {
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        user.setUserId(userSnapshot.getKey());
                        users.add(user);
                    }
                }
                allUsers.setValue(users);
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };

        FirebaseDBHelper.getUsersRef().addValueEventListener(usersListener);
    }

    private void loadAllMessages() {
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Message> messages = new ArrayList<>();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        message.setMessageId(messageSnapshot.getKey());
                        messages.add(message);
                    }
                }
                allMessages.setValue(messages);
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };

        FirebaseDBHelper.getMessagesRef().addValueEventListener(messagesListener);
    }

    public LiveData<List<Chat>> getChats() {
        return chats;
    }

    public LiveData<List<User>> getAllUsers() {
        return allUsers;
    }

    public LiveData<List<Message>> getAllMessages() {
        return allMessages;
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<String> getError() {
        return error;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (chatsListener != null && currentUserId != null) {
            FirebaseDBHelper.getUserChatsRef(currentUserId).removeEventListener(chatsListener);
        }
        if (usersListener != null) {
            FirebaseDBHelper.getUsersRef().removeEventListener(usersListener);
        }
        if (messagesListener != null) {
            FirebaseDBHelper.getMessagesRef().removeEventListener(messagesListener);
        }
        if (currentUserListener != null) {
            FirebaseDBHelper.getUserRef(currentUserId).removeEventListener(currentUserListener);
        }
    }
}