package com.example.bay;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bay.HomeActivity;
import com.example.bay.adapter.NotificationAdapter;
import com.example.bay.databinding.FragmentNotificationBinding;
import com.example.bay.fragment.PersonalMessageFragment;
import com.example.bay.model.Notification;
import com.example.bay.viewmodel.NotificationViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private FragmentNotificationBinding binding;
    private HomeActivity homeActivity;
    private NotificationAdapter notificationAdapter;
    private NotificationViewModel notificationViewModel;
    private String currentUserId;

    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationBinding.inflate(inflater, container, false);
        homeActivity = (HomeActivity) requireActivity();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        notificationViewModel.setCurrentUserId(currentUserId);

        setupRecyclerView();
        setupObservers();
        setupClickListeners();
    }

    private void setupRecyclerView() {
        notificationAdapter = new NotificationAdapter(new ArrayList<>(), new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(Notification notification) {
                openChatFromNotification(notification);
            }

            @Override
            public void onNotificationLongClick(Notification notification) {
                showNotificationOptions(notification);
            }

            @Override
            public void onMarkAsReadClick(Notification notification) {
                markNotificationAsRead(notification);
            }
        }, requireContext());

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(notificationAdapter);
    }

    private void setupObservers() {
        notificationViewModel.getNotifications().observe(getViewLifecycleOwner(), new Observer<List<Notification>>() {
            @Override
            public void onChanged(List<Notification> notifications) {
                if (notifications != null) {
                    notificationAdapter.updateData(notifications);

                    // Show empty state if no notifications
                    if (notifications.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                }
            }
        });

        notificationViewModel.getUnreadCount().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer count) {
                if (count != null && count > 0) {
                    // Update the title to show unread count
                    binding.textView.setText("ការជូនដំណឹង (" + count + ")");
                } else {
                    binding.textView.setText("ការជូនដំណឹង");
                }
            }
        });

        notificationViewModel.getError().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null && !error.isEmpty() && isAdded()) {
                    Toast.makeText(getContext(), "កំហុស: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupClickListeners() {
        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeActivity.onBackPressed();
            }
        });

        // Add swipe to refresh functionality
        binding.recyclerView.setOnTouchListener((v, event) -> {
            // You can implement custom swipe to refresh or use a SwipeRefreshLayout
            return false;
        });
    }

    private void openChatFromNotification(Notification notification) {
        if ("chat_message".equals(notification.getType())) {
            // Mark as read first
            notificationViewModel.markNotificationAsRead(notification.getNotificationId());

            // Open chat
            PersonalMessageFragment fragment = PersonalMessageFragment.newInstance(
                    notification.getChatId(),
                    notification.getSenderId()
            );
            homeActivity.LoadFragment(fragment);
            homeActivity.hideBottomNavigation();
        } else if ("system".equals(notification.getType())) {
            // Handle system notifications
            handleSystemNotification(notification);
        }
        // Add more notification types as needed
    }

    private void handleSystemNotification(Notification notification) {
        // Mark as read
        notificationViewModel.markNotificationAsRead(notification.getNotificationId());

        // Show notification details or perform action
        showNotificationDetailsDialog(notification);
    }

    private void showNotificationDetailsDialog(Notification notification) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(notification.getTitle());
        builder.setMessage(notification.getMessage());

        if ("chat_message".equals(notification.getType())) {
            builder.setPositiveButton("បើក Chat", (dialog, which) -> {
                openChatFromNotification(notification);
            });
        } else {
            builder.setPositiveButton("យល់ព្រម", null);
        }

        builder.setNegativeButton("បិទ", null);
        builder.show();
    }

    private void markNotificationAsRead(Notification notification) {
        notificationViewModel.markNotificationAsRead(notification.getNotificationId());
    }

    private void showNotificationOptions(Notification notification) {
        String[] options = {"សម្គាល់ថាបានអាន", "លុប", "បិទ"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("ជម្រើស");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                notificationViewModel.markNotificationAsRead(notification.getNotificationId());
                Toast.makeText(requireContext(), "បានសម្គាល់ថាបានអាន", Toast.LENGTH_SHORT).show();
            } else if (which == 1) {
                showDeleteConfirmation(notification);
            }
        });
        builder.setNegativeButton("បោះបង់", null);
        builder.show();
    }

    private void showDeleteConfirmation(Notification notification) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("លុបការជូនដំណឹង")
                .setMessage("តើអ្នកពិតជាចង់លុបការជូនដំណឹងនេះមែនទេ?")
                .setPositiveButton("លុប", (dialog, which) -> {
                    notificationViewModel.deleteNotification(notification.getNotificationId());
                    Toast.makeText(requireContext(), "បានលុបការជូនដំណឹង", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("បោះបង់", null)
                .show();
    }

    private void showEmptyState() {
        // Show empty state message in the RecyclerView
        if (notificationAdapter != null) {
            // You can implement an empty view holder in your adapter
            // or show a placeholder message
        }
    }

    private void hideEmptyState() {
        // Hide empty state
    }

    @Override
    public void onResume() {
        super.onResume();
        homeActivity.showBottomNavigation();
        // Refresh notifications when fragment is resumed
        notificationViewModel.loadNotifications();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Mark all notifications as read when leaving the fragment (optional)
        // notificationViewModel.markAllNotificationsAsRead();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}