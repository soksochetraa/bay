package com.example.bay.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.ChatAdapter;
import com.example.bay.adapter.OnlineUserAdapter;
import com.example.bay.databinding.FragmentMessageBinding;
import com.example.bay.model.Chat;
import com.example.bay.model.User;
import com.example.bay.repository.ChatRepository;
import com.example.bay.repository.UserRepository;
import com.example.bay.util.FirebaseDBHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MessageFragment extends Fragment {

    private FragmentMessageBinding binding;
    private HomeActivity homeActivity;
    private ChatAdapter chatAdapter;
    private OnlineUserAdapter onlineUserAdapter;
    private ChatRepository chatRepository;
    private UserRepository userRepository;
    private List<Chat> chatList = new ArrayList<>();
    private List<User> onlineUsers = new ArrayList<>();
    private String currentUserId;

    private ValueEventListener chatsListener;
    private ValueEventListener usersListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMessageBinding.inflate(inflater, container, false);
        homeActivity = (HomeActivity) requireActivity();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatRepository = new ChatRepository();
        userRepository = new UserRepository();

        setupRecyclerViews();
        loadChats();
        loadOnlineUsers();
        setupSearch();
        setupClickListeners();
        setupOnlineStatus();
    }

    private void setupRecyclerViews() {
        chatAdapter = new ChatAdapter(chatList, currentUserId, new ChatAdapter.OnChatClickListener() {
            @Override
            public void onChatClick(Chat chat) {
                openChat(chat);
            }

            @Override
            public void onUserClick(String userId) {
                homeActivity.loadUserProfile(userId);
            }
        }, requireContext());

        binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.chatRecyclerView.setAdapter(chatAdapter);

        onlineUserAdapter = new OnlineUserAdapter(onlineUsers, new OnlineUserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                startChatWithUser(user);
            }
        }, requireContext());

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.onlineUserRecyclerView.setLayoutManager(layoutManager);
        binding.onlineUserRecyclerView.setAdapter(onlineUserAdapter);
    }

    private void loadChats() {
        // Store the listener reference so we can remove it later
        chatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> chatIds = new ArrayList<>();

                for (DataSnapshot chatIdSnapshot : snapshot.getChildren()) {
                    String chatId = chatIdSnapshot.getKey();
                    chatIds.add(chatId);
                }

                if (chatIds.isEmpty()) {
                    updateChatList(new ArrayList<>());
                    return;
                }

                loadChatDetails(chatIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load chats", Toast.LENGTH_SHORT).show();
                }
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
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Chat chat = snapshot.getValue(Chat.class);
                            if (chat != null) {
                                chat.setChatId(snapshot.getKey());
                                loadedChats.add(chat);
                            }

                            loadedCount[0]++;
                            if (loadedCount[0] == chatIds.size()) {
                                updateChatList(loadedChats);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadedCount[0]++;
                            if (loadedCount[0] == chatIds.size()) {
                                updateChatList(loadedChats);
                            }
                        }
                    });
        }
    }

    private void updateChatList(List<Chat> chats) {
        if (!isAdded() || getView() == null) {
            return; // Fragment is not attached to activity
        }

        requireActivity().runOnUiThread(() -> {
            chatList.clear();
            chatList.addAll(chats);

            // Sort by last message time (newest first)
            chatList.sort((c1, c2) -> Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));

            chatAdapter.notifyDataSetChanged();

            if (chats.isEmpty()) {
                binding.chatRecyclerView.setVisibility(View.GONE);
                // You might want to show an empty state view here
            } else {
                binding.chatRecyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadOnlineUsers() {
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getView() == null) {
                    return;
                }

                onlineUsers.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null && !user.getUserId().equals(currentUserId)) {
                        checkOnlineStatus(user);
                        onlineUsers.add(user);
                    }
                }

                requireActivity().runOnUiThread(() -> {
                    onlineUserAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        FirebaseDBHelper.getUsersRef().addValueEventListener(usersListener);
    }

    private void checkOnlineStatus(User user) {
        FirebaseDBHelper.getOnlineStatusRef(user.getUserId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        boolean isOnline = snapshot.exists() &&
                                snapshot.getValue(Boolean.class);
                        user.setOnline(isOnline);

                        requireActivity().runOnUiThread(() -> {
                            onlineUserAdapter.notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void setupOnlineStatus() {
        FirebaseDBHelper.getOnlineStatusRef(currentUserId).setValue(true);
    }

    private void setupSearch() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChats(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterChats(String query) {
        if (query.isEmpty()) {
            chatAdapter.filterList(new ArrayList<>(chatList));
            return;
        }

        List<Chat> filteredList = new ArrayList<>();
        for (Chat chat : chatList) {
            String partnerName = getChatPartnerName(chat);
            if (partnerName.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(chat);
            }
        }
        chatAdapter.filterList(filteredList);
    }

    private String getChatPartnerName(Chat chat) {
        String partnerId = chat.getChatPartnerId(currentUserId);
        // For now, return a placeholder. You should fetch the actual user name.
        return "User " + partnerId.substring(0, 4);
    }

    private void setupClickListeners() {
        // Find the edit button by ID from your layout
        View view = getView();
        if (view != null) {
            ImageButton editButton = view.findViewById(R.id.editButton);
            if (editButton != null) {
                editButton.setOnClickListener(v -> {
                    showNewChatDialog();
                });
            }
        }
    }

    private void showNewChatDialog() {
        // Implement dialog to select user for new chat
    }

    private void startChatWithUser(User user) {
        chatRepository.getOrCreateChat(currentUserId, user.getUserId(),
                new ChatRepository.ChatCallback<Chat>() {
                    @Override
                    public void onSuccess(Chat chat) {
                        if (isAdded()) {
                            openChat(chat);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Failed to start chat", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void openChat(Chat chat) {
        PersonalMessageFragment fragment = PersonalMessageFragment.newInstance(
                chat.getChatId(),
                chat.getChatPartnerId(currentUserId)
        );
        homeActivity.LoadFragment(fragment);
        homeActivity.hideBottomNavigation();
    }

    @Override
    public void onResume() {
        super.onResume();

        homeActivity.showBottomNavigation();

        if (currentUserId != null) {
            FirebaseDBHelper.getOnlineStatusRef(currentUserId).setValue(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (currentUserId != null) {
            FirebaseDBHelper.getOnlineStatusRef(currentUserId).onDisconnect().setValue(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (chatsListener != null && currentUserId != null) {
            FirebaseDBHelper.getUserChatsRef(currentUserId).removeEventListener(chatsListener);
        }

        if (usersListener != null) {
            FirebaseDBHelper.getUsersRef().removeEventListener(usersListener);
        }

        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (currentUserId != null) {
            FirebaseDBHelper.getOnlineStatusRef(currentUserId).setValue(false);
        }
    }
}