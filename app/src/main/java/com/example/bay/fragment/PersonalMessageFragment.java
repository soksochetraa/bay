package com.example.bay.fragment;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.MessageAdapter;
import com.example.bay.databinding.FragmentPersonalMessageBinding;
import com.example.bay.model.Message;
import com.example.bay.model.User;
import com.example.bay.repository.ChatRepository;
import com.example.bay.repository.UserRepository;
import com.example.bay.util.FirebaseDBHelper;
import com.example.bay.util.ImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PersonalMessageFragment extends Fragment {

    private static final String ARG_CHAT_ID = "chatId";
    private static final String ARG_USER_ID = "userId";

    private FragmentPersonalMessageBinding binding;
    private HomeActivity homeActivity;
    private MessageAdapter messageAdapter;
    private ChatRepository chatRepository;
    private UserRepository userRepository;
    private List<Message> messageList = new ArrayList<>();

    private String chatId;
    private String otherUserId;
    private String currentUserId;
    private User otherUser;

    private ValueEventListener typingListener;
    private ValueEventListener onlineStatusListener;
    private ValueEventListener messagesListener;

    private FirebaseStorage storage;
    private StorageReference storageRef;
    private Uri selectedImageUri;
    private File cameraPhotoFile;
    private boolean isUploading = false;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    public static PersonalMessageFragment newInstance(String chatId, String userId) {
        PersonalMessageFragment fragment = new PersonalMessageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_ID, chatId);
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPersonalMessageBinding.inflate(inflater, container, false);
        homeActivity = (HomeActivity) requireActivity();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            chatId = args.getString(ARG_CHAT_ID);
            otherUserId = args.getString(ARG_USER_ID);
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatRepository = new ChatRepository();
        userRepository = new UserRepository();

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        setupActivityResultLaunchers();
        setupViews();
        loadUserData();
        loadMessages();
        setupListeners();

        chatRepository.markMessagesAsRead(chatId, currentUserId);
    }

    private void setupActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && cameraPhotoFile != null) {
                        selectedImageUri = Uri.fromFile(cameraPhotoFile);
                        showImagePreview(selectedImageUri);
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        showImagePreview(selectedImageUri);
                    }
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    Boolean cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false);
                    Boolean storageGranted;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        storageGranted = permissions.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false);
                    } else {
                        storageGranted = permissions.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false) &&
                                permissions.getOrDefault(Manifest.permission.WRITE_EXTERNAL_STORAGE, false);
                    }

                    if (Boolean.TRUE.equals(cameraGranted) && Boolean.TRUE.equals(storageGranted)) {
                        openCameraWithPermission();
                    } else {
                        Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupViews() {
        messageAdapter = new MessageAdapter(messageList, currentUserId, requireContext(),
                new MessageAdapter.OnImageClickListener() {
                    @Override
                    public void onImageClick(Message message, ImageView imageView) {
                        openImageFullScreen(message.getImageUrl());
                    }
                });

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(messageAdapter);

        binding.btnBack.setOnClickListener(v -> homeActivity.onBackPressed());

        binding.btnSend.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadAndSendImage();
            } else {
                sendTextMessage();
            }
        });

        binding.btnAttach.setOnClickListener(v -> showAttachmentOptions());

        binding.etMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (selectedImageUri != null) {
                        uploadAndSendImage();
                    } else {
                        sendTextMessage();
                    }
                    return true;
                }
                return false;
            }
        });

        binding.btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            binding.imagePreviewContainer.setVisibility(View.GONE);
            binding.etMessage.setText("");
            binding.etMessage.setHint("Type a message...");
            binding.etMessage.requestFocus();
        });

        binding.etMessage.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isTyping = false;
            private final long TYPING_INTERVAL = 1000;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isTyping && s.length() > 0) {
                    isTyping = true;
                    chatRepository.setTypingStatus(chatId, currentUserId, true);

                    binding.etMessage.postDelayed(() -> {
                        isTyping = false;
                        chatRepository.setTypingStatus(chatId, currentUserId, false);
                    }, TYPING_INTERVAL);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void showAttachmentOptions() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Send Image");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkAndOpenCamera();
            } else if (which == 1) {
                openImagePicker();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void checkAndOpenCamera() {
        boolean cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean storagePermission;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storagePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } else {
            storagePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }

        if (cameraPermission && storagePermission) {
            openCameraWithPermission();
        } else {
            requestPermissions();
        }
    }

    private void requestPermissions() {
        ArrayList<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            String[] permissions = permissionsNeeded.toArray(new String[0]);
            permissionLauncher.launch(permissions);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void openCameraWithPermission() {
        try {
            cameraPhotoFile = createImageFile();
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoURI = androidx.core.content.FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    cameraPhotoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraLauncher.launch(cameraIntent);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Cannot open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        galleryLauncher.launch(galleryIntent);
    }

    private void loadUserData() {
        userRepository.getUserById(otherUserId, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User user) {
                otherUser = user;
                updateUIWithUserData(user);
            }

            @Override
            public void onError(String errorMsg) {
                Toast.makeText(requireContext(), "Failed to load user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithUserData(User user) {
        binding.tvUserName.setText(user.getFirst_name() + " " + user.getLast_name());

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(user.getProfileImageUrl())
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .into(binding.btnProfile);
        } else {
            binding.btnProfile.setImageResource(R.drawable.img);
        }

        binding.btnProfile.setOnClickListener(v -> {
            homeActivity.loadUserProfile(otherUserId);
        });
    }

    private void loadMessages() {
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getView() == null) {
                    return;
                }

                messageList.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        message.setMessageId(messageSnapshot.getKey());
                        messageList.add(message);
                    }
                }

                requireActivity().runOnUiThread(() -> {
                    messageAdapter.notifyDataSetChanged();

                    if (messageList.size() > 0) {
                        binding.rvMessages.smoothScrollToPosition(messageList.size() - 1);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load messages", Toast.LENGTH_SHORT).show();
                }
            }
        };

        FirebaseDBHelper.getChatMessagesRef(chatId)
                .orderByChild("timestamp")
                .addValueEventListener(messagesListener);
    }

    private void setupListeners() {
        typingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getView() == null) {
                    return;
                }

                if (snapshot.exists()) {
                    Boolean isTyping = snapshot.getValue(Boolean.class);
                    if (isTyping != null && isTyping) {
                        requireActivity().runOnUiThread(() -> {
                            binding.tvTyping.setVisibility(View.VISIBLE);
                            binding.tvStatus.setVisibility(View.GONE);
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            binding.tvTyping.setVisibility(View.GONE);
                            binding.tvStatus.setVisibility(View.VISIBLE);
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        FirebaseDBHelper.getTypingRef(chatId).child(otherUserId)
                .addValueEventListener(typingListener);

        onlineStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getView() == null) {
                    return;
                }

                boolean isOnline = snapshot.exists() &&
                        snapshot.getValue(Boolean.class);

                requireActivity().runOnUiThread(() -> {
                    if (isOnline) {
                        binding.tvStatus.setText("Online");
                        binding.tvStatus.setTextColor(getResources().getColor(R.color.verified_color));
                        binding.onlineIndicator.setBackgroundTintList(
                                getResources().getColorStateList(R.color.verified_color));
                    } else {
                        binding.tvStatus.setText("Offline");
                        binding.tvStatus.setTextColor(getResources().getColor(R.color.textColors));
                        binding.onlineIndicator.setBackgroundTintList(
                                getResources().getColorStateList(R.color.textColors));
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        FirebaseDBHelper.getOnlineStatusRef(otherUserId)
                .addValueEventListener(onlineStatusListener);
    }

    private void sendTextMessage() {
        String messageText = binding.etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        Message message = new Message(currentUserId, otherUserId, messageText);

        chatRepository.sendMessage(chatId, message, new ChatRepository.ChatCallback<String>() {
            @Override
            public void onSuccess(String messageId) {
                requireActivity().runOnUiThread(() -> {
                    binding.etMessage.setText("");
                });
            }

            @Override
            public void onError(String error) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadAndSendImage() {
        if (selectedImageUri == null || isUploading) {
            return;
        }

        isUploading = true;
        showUploadProgress(true);

        String filename = "chat_images/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(filename);

        byte[] compressedImage = ImageUtils.compressImage(requireContext(), selectedImageUri, 1024);

        if (compressedImage == null) {
            Toast.makeText(requireContext(), "Failed to compress image", Toast.LENGTH_SHORT).show();
            showUploadProgress(false);
            isUploading = false;
            return;
        }

        UploadTask uploadTask = imageRef.putBytes(compressedImage);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            updateUploadProgress((int) progress);
        });

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                byte[] thumbnail = ImageUtils.createThumbnail(requireContext(), selectedImageUri, 200);
                if (thumbnail == null) {
                    sendImageMessage(uri.toString(), null, compressedImage.length, imageRef.getName());
                    return;
                }

                String thumbFilename = "chat_images/thumb_" + UUID.randomUUID().toString() + ".jpg";
                StorageReference thumbRef = storageRef.child(thumbFilename);

                thumbRef.putBytes(thumbnail).addOnSuccessListener(thumbTask -> {
                    thumbRef.getDownloadUrl().addOnSuccessListener(thumbUri -> {
                        sendImageMessage(uri.toString(), thumbUri.toString(), compressedImage.length, imageRef.getName());
                    }).addOnFailureListener(e -> {
                        sendImageMessage(uri.toString(), null, compressedImage.length, imageRef.getName());
                    });
                }).addOnFailureListener(e -> {
                    sendImageMessage(uri.toString(), null, compressedImage.length, imageRef.getName());
                });
            });
        }).addOnFailureListener(e -> {
            requireActivity().runOnUiThread(() -> {
                showUploadProgress(false);
                isUploading = false;
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void sendImageMessage(String imageUrl, String thumbnailUrl, long fileSize, String fileName) {
        int[] dimensions = ImageUtils.getImageDimensions(requireContext(), selectedImageUri);

        Message imageMessage = new Message(
                currentUserId,
                otherUserId,
                imageUrl,
                thumbnailUrl,
                dimensions[0],
                dimensions[1],
                fileName,
                fileSize
        );

        chatRepository.sendMessage(chatId, imageMessage,
                new ChatRepository.ChatCallback<String>() {
                    @Override
                    public void onSuccess(String messageId) {
                        requireActivity().runOnUiThread(() -> {
                            selectedImageUri = null;
                            binding.imagePreviewContainer.setVisibility(View.GONE);
                            binding.etMessage.setText("");
                            binding.etMessage.setHint("Type a message...");
                            showUploadProgress(false);
                            isUploading = false;
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            showUploadProgress(false);
                            isUploading = false;
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(getContext(), "Failed to send image", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    private void showUploadProgress(boolean show) {
        requireActivity().runOnUiThread(() -> {
            if (show) {
                binding.uploadProgressContainer.setVisibility(View.VISIBLE);
                binding.btnSend.setEnabled(false);
                binding.btnAttach.setEnabled(false);
                binding.etMessage.setEnabled(false);
            } else {
                binding.uploadProgressContainer.setVisibility(View.GONE);
                binding.btnSend.setEnabled(true);
                binding.btnAttach.setEnabled(true);
                binding.etMessage.setEnabled(true);
                updateUploadProgress(0);
            }
        });
    }

    private void updateUploadProgress(int progress) {
        requireActivity().runOnUiThread(() -> {
            binding.progressBar.setProgress(progress);
            binding.tvUploadProgress.setText(progress + "%");
        });
    }

    private void openImageFullScreen(String imageUrl) {
    }

    private void showImagePreview(Uri imageUri) {
        if (imageUri == null) return;

        Glide.with(requireContext())
                .load(imageUri)
                .placeholder(R.drawable.img)
                .error(R.drawable.img)
                .into(binding.ivImagePreview);

        binding.imagePreviewContainer.setVisibility(View.VISIBLE);
        binding.etMessage.setHint("Add a caption...");
    }

    @Override
    public void onResume() {
        super.onResume();
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

        if (typingListener != null) {
            FirebaseDBHelper.getTypingRef(chatId).child(otherUserId)
                    .removeEventListener(typingListener);
        }

        if (onlineStatusListener != null) {
            FirebaseDBHelper.getOnlineStatusRef(otherUserId)
                    .removeEventListener(onlineStatusListener);
        }

        if (messagesListener != null) {
            FirebaseDBHelper.getChatMessagesRef(chatId)
                    .removeEventListener(messagesListener);
        }

        chatRepository.setTypingStatus(chatId, currentUserId, false);

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