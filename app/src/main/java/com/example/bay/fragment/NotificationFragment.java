package com.example.bay.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bay.HomeActivity;
import com.example.bay.adapter.NotificationAdapter;
import com.example.bay.databinding.FragmentNotificationBinding;
import com.example.bay.model.Notification;
import com.example.bay.repository.IApiCallback;
import com.example.bay.repository.NotificationRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NotificationFragment extends Fragment {

    private FragmentNotificationBinding binding;
    private HomeActivity homeActivity;
    private NotificationAdapter adapter;
    private NotificationRepository repository;
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

        String currentUserId = Objects.requireNonNull(
                FirebaseAuth.getInstance().getCurrentUser()
        ).getUid();

        adapter = new NotificationAdapter();
        repository = new NotificationRepository();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        loadNotifications(currentUserId);

        setupClickListeners();
    }
    private void loadNotifications(String userId) {
        repository.getUserNotifications(userId, new IApiCallback<Map<String, Notification>>() {
            @Override
            public void onSuccess(Map<String, Notification> data) {

                List<Notification> filteredList = new ArrayList<>();

                for (Map.Entry<String, Notification> entry : data.entrySet()) {
                    String notificationId = entry.getKey();
                    Notification notification = entry.getValue();

                    boolean isForUser = notification.getReceiverId() != null &&
                            notification.getReceiverId().equals(userId);

                    boolean isFromAdmin = notification.getSender() != null &&
                            notification.getSender().equalsIgnoreCase("admin");

                    if (isForUser || isFromAdmin) {
                        filteredList.add(notification);

                        // Mark as read if unread
                        if (notification.isUnread()) {
                            notification.setIsRead(true);
                            repository.updateNotification(userId, notificationId, notification, new IApiCallback<Notification>() {
                                @Override
                                public void onSuccess(Notification data) {}

                                @Override
                                public void onError(String error) {}
                            });
                        }
                    }
                }

                // Sort descending by timestamp
                filteredList.sort((n1, n2) -> {
                    long t1 = n1.getTimestampMillis();
                    long t2 = n2.getTimestampMillis();
                    return Long.compare(t2, t1);
                });

                if (filteredList.isEmpty()) {
                    binding.emptyState.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                } else {
                    binding.emptyState.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                    adapter.setData(filteredList);
                }
            }

            @Override
            public void onError(String error) {
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.recyclerView.setVisibility(View.GONE);
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
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}