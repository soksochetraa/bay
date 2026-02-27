package com.example.bay.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bay.HomeActivity;
import com.example.bay.adapter.ChatAdapter;
import com.example.bay.adapter.ChatSwipeHelper;
import com.example.bay.databinding.FragmentMessageBinding;
import com.example.bay.model.Chat;
import com.example.bay.model.Message;
import com.example.bay.model.User;
import com.example.bay.repository.ChatRepository;
import com.example.bay.util.FirebaseDBHelper;
import com.example.bay.viewmodel.MessageViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class MessageFragment extends Fragment {

    private FragmentMessageBinding binding;
    private HomeActivity homeActivity;
    private ChatAdapter chatAdapter;
    private com.example.bay.adapter.OnlineUserAdapter onlineUserAdapter;
    private ChatRepository chatRepository;
    private MessageViewModel messageViewModel;
    private String currentUserId;
    private ItemTouchHelper swipeHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMessageBinding.inflate(inflater, container, false);
        homeActivity = (HomeActivity) requireActivity();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatRepository = new ChatRepository();
        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        messageViewModel.setCurrentUserId(currentUserId);

        setupRecyclerViews();
        attachSwipeToDelete();
        setupObservers();
        setupSearch();
        setupClickListeners();
        setupOnlineStatus();
    }

    private void setupRecyclerViews() {
        chatAdapter = new ChatAdapter(new ArrayList<>(), currentUserId, new ChatAdapter.OnChatClickListener() {
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

        onlineUserAdapter = new com.example.bay.adapter.OnlineUserAdapter(
                new ArrayList<>(),
                new ArrayList<>(),
                currentUserId,
                null,
                user -> startChatWithUser(user),
                requireContext()
        );

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.onlineUserRecyclerView.setLayoutManager(layoutManager);
        binding.onlineUserRecyclerView.setAdapter(onlineUserAdapter);
    }

    private void attachSwipeToDelete() {
        if (swipeHelper != null) return;

        swipeHelper = ChatSwipeHelper.attachSwipeToDelete(
                requireContext(),
                binding.chatRecyclerView,
                chatAdapter,
                (chat, position) -> {
                    if (chat == null) return;

                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Delete chat?")
                            .setMessage("This will permanently delete this conversation for both users. This action cannot be undone.")
                            .setCancelable(false)
                            .setPositiveButton("Delete", (dialog, which) -> {

                                Chat removed = chat;
                                int removedPos = position;

                                chatAdapter.removeAt(removedPos);

                                chatRepository.deleteChatForBothUsers(
                                        removed.getChatId(),
                                        removed.getUser1Id(),
                                        removed.getUser2Id(),
                                        new ChatRepository.ChatCallback<Void>() {
                                            @Override
                                            public void onSuccess(Void result) {
                                            }

                                            @Override
                                            public void onError(String error) {
                                                if (isAdded()) {
                                                    chatAdapter.restore(removed, removedPos);
                                                    Toast.makeText(requireContext(), "Delete failed: " + error, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                );

                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                chatAdapter.notifyItemChanged(position);
                                dialog.dismiss();
                            })
                            .show();
                }
        );
    }

    private void setupObservers() {
        messageViewModel.getCurrentUser().observe(getViewLifecycleOwner(), new Observer<User>() {
            @Override
            public void onChanged(User user) {
                if (user != null) {
                    onlineUserAdapter.updateData(
                            messageViewModel.getAllUsers().getValue() != null ?
                                    messageViewModel.getAllUsers().getValue() : new ArrayList<>(),
                            messageViewModel.getAllMessages().getValue() != null ?
                                    messageViewModel.getAllMessages().getValue() : new ArrayList<>(),
                            user
                    );
                }
            }
        });

        messageViewModel.getAllUsers().observe(getViewLifecycleOwner(), new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> users) {
                if (users != null) {
                    User currentUser = messageViewModel.getCurrentUser().getValue();
                    onlineUserAdapter.updateData(
                            users,
                            messageViewModel.getAllMessages().getValue() != null ?
                                    messageViewModel.getAllMessages().getValue() : new ArrayList<>(),
                            currentUser
                    );
                }
            }
        });

        messageViewModel.getAllMessages().observe(getViewLifecycleOwner(), new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messages) {
                if (messages != null) {
                    User currentUser = messageViewModel.getCurrentUser().getValue();
                    onlineUserAdapter.updateData(
                            messageViewModel.getAllUsers().getValue() != null ?
                                    messageViewModel.getAllUsers().getValue() : new ArrayList<>(),
                            messages,
                            currentUser
                    );
                }
            }
        });

        messageViewModel.getChats().observe(getViewLifecycleOwner(), new Observer<List<Chat>>() {
            @Override
            public void onChanged(List<Chat> chats) {
                if (chats != null) {
                    chatAdapter.updateData(chats);
                    binding.chatRecyclerView.setVisibility(chats.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }
        });

        messageViewModel.getError().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null && !error.isEmpty() && isAdded()) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupOnlineStatus() {
        FirebaseDBHelper.getOnlineStatusRef(currentUserId).setValue(true);
    }

    private void setupSearch() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChats(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterChats(String query) {
        List<Chat> allChats = messageViewModel.getChats().getValue();
        if (allChats == null) return;

        List<Chat> filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(allChats);
        } else {
            for (Chat chat : allChats) {
                String partnerName = getChatPartnerName(chat);
                if (partnerName.toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(chat);
                }
            }
        }
        chatAdapter.filterList(filteredList);
    }

    private String getChatPartnerName(Chat chat) {
        String partnerId = chat.getChatPartnerId(currentUserId);
        List<User> allUsers = messageViewModel.getAllUsers().getValue();
        if (allUsers != null) {
            for (User user : allUsers) {
                if (user.getUserId().equals(partnerId)) {
                    return user.getFirst_name() + " " + user.getLast_name();
                }
            }
        }
        return "User";
    }

    private void setupClickListeners() {
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
                            Toast.makeText(getContext(), "Failed to start chat: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void openChat(Chat chat) {
        com.example.bay.fragment.PersonalMessageFragment fragment = com.example.bay.fragment.PersonalMessageFragment.newInstance(
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