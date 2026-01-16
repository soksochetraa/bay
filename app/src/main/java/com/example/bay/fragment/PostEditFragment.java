package com.example.bay.fragment;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.databinding.FragmentPostEditBinding;
import com.example.bay.model.PostCardItem;
import com.example.bay.model.User;
import com.example.bay.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PostEditFragment extends Fragment {

    private static final String TAG = "PostEditFragment";

    private FragmentPostEditBinding binding;
    private ActivityResultLauncher<Intent> pickImagesLauncher;

    // Image management
    private final List<String> currentImageUrls = new ArrayList<>();
    private final List<Uri> selectedNewImageUris = new ArrayList<>();
    private final List<String> imagesToDelete = new ArrayList<>();

    // Firebase references
    private FirebaseAuth mAuth;
    private DatabaseReference postRef;
    private StorageReference storageRef;
    private UserRepository userRepository;
    private HomeActivity homeActivity;

    // Post data
    private String postId;
    private PostCardItem currentPost;
    private String currentUserId;

    public PostEditFragment() {
        // Required empty public constructor
    }

    public static PostEditFragment newInstance(String postId) {
        PostEditFragment fragment = new PostEditFragment();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : null;

        userRepository = new UserRepository();

        // Initialize image picker launcher
        pickImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleImagePickerResult
        );
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentPostEditBinding.inflate(inflater, container, false);
        homeActivity = (HomeActivity) requireActivity();

        // Get post ID from arguments
        if (getArguments() != null) {
            postId = getArguments().getString("postId");
        }

        if (postId == null || postId.isEmpty()) {
            Toast.makeText(requireContext(), "មិនមានប្រកាស", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return binding.getRoot();
        }

        // Initialize Firebase references
        postRef = FirebaseDatabase.getInstance()
                .getReference("postCardItems")
                .child(postId);
        storageRef = FirebaseStorage.getInstance().getReference();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupClickListeners();
        setupCurrentUserUI();
        loadPostData();
    }

    private void setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.onBackPressed();
            }
        });

        // Add/Edit images button
        binding.btnAddOrEditImage.setOnClickListener(v -> openImagePicker());

        // Update post button
        binding.btnUpdatePost.setOnClickListener(v -> updatePost());
    }

    private void setupCurrentUserUI() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            binding.tvCurrentUserName.setText("អ្នកប្រើប្រាស់");
            binding.ivCurrentUserAvatar.setImageResource(R.drawable.img);
            return;
        }

        String uid = firebaseUser.getUid();

        userRepository.getUserById(uid, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded() || binding == null) return;

                if (user != null) {
                    String displayName;

                    if (!TextUtils.isEmpty(user.getLast_name()) && !TextUtils.isEmpty(user.getFirst_name())) {
                        displayName = user.getFirst_name() + " " + user.getLast_name();
                    } else if (!TextUtils.isEmpty(user.getLast_name())) {
                        displayName = user.getLast_name();
                    } else if (!TextUtils.isEmpty(user.getFirst_name())) {
                        displayName = user.getFirst_name();
                    } else {
                        displayName = "អ្នកប្រើប្រាស់";
                    }

                    binding.tvCurrentUserName.setText(displayName);

                    String profileUrl = user.getProfileImageUrl();
                    if (!TextUtils.isEmpty(profileUrl)) {
                        Glide.with(requireContext())
                                .load(profileUrl)
                                .placeholder(R.drawable.img)
                                .error(R.drawable.img)
                                .centerCrop()
                                .into(binding.ivCurrentUserAvatar);
                    } else {
                        binding.ivCurrentUserAvatar.setImageResource(R.drawable.img);
                    }
                } else {
                    populateFromFirebaseUser(firebaseUser);
                }
            }

            @Override
            public void onError(String errorMsg) {
                populateFromFirebaseUser(firebaseUser);
            }
        });
    }

    private void populateFromFirebaseUser(FirebaseUser firebaseUser) {
        String name = firebaseUser.getDisplayName();
        if (TextUtils.isEmpty(name)) {
            name = "អ្នកប្រើប្រាស់";
        }
        binding.tvCurrentUserName.setText(name);

        if (firebaseUser.getPhotoUrl() != null) {
            Glide.with(requireContext())
                    .load(firebaseUser.getPhotoUrl())
                    .placeholder(R.drawable.img)
                    .centerCrop()
                    .into(binding.ivCurrentUserAvatar);
        } else {
            binding.ivCurrentUserAvatar.setImageResource(R.drawable.img);
        }
    }

    private void loadPostData() {
        postRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null || !isAdded()) return;

                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "ប្រកាសមិនមានទៀតទេ", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                    return;
                }

                currentPost = snapshot.getValue(PostCardItem.class);
                if (currentPost == null) return;

                // Check if current user is the owner of the post
                if (!currentPost.getUserId().equals(currentUserId)) {
                    Toast.makeText(requireContext(), "អ្នកមិនមានសិទ្ធិកែប្រែប្រកាសនេះទេ", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                    return;
                }

                // Set post content
                binding.etPostContent.setText(currentPost.getContent() != null ? currentPost.getContent() : "");

                // Load current images
                if (currentPost.getImageUrls() != null && !currentPost.getImageUrls().isEmpty()) {
                    currentImageUrls.clear();
                    currentImageUrls.addAll(currentPost.getImageUrls());

                    // Show images preview
                    showCurrentImages();
                } else {
                    // Show no image hint
                    binding.tvNoImageHint.setVisibility(View.VISIBLE);
                    binding.horizontalImageScroll.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "មិនអាចទាញយកទិន្នន័យបាន", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCurrentImages() {
        LinearLayout container = binding.layoutSelectedImages;
        container.removeAllViews();

        if (currentImageUrls.isEmpty() && selectedNewImageUris.isEmpty()) {
            binding.tvNoImageHint.setVisibility(View.VISIBLE);
            binding.horizontalImageScroll.setVisibility(View.GONE);
            return;
        } else {
            binding.tvNoImageHint.setVisibility(View.GONE);
            binding.horizontalImageScroll.setVisibility(View.VISIBLE);
        }

        int imageSize = dpToPx(110);
        int marginEnd = dpToPx(8);
        int removeSize = dpToPx(22);

        // Show current images (from Firebase)
        for (int i = 0; i < currentImageUrls.size(); i++) {
            String imageUrl = currentImageUrls.get(i);

            FrameLayout frameLayout = new FrameLayout(requireContext());
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(imageSize, imageSize);
            if (i < currentImageUrls.size() - 1 || !selectedNewImageUris.isEmpty()) {
                frameParams.setMarginEnd(marginEnd);
            }
            frameLayout.setLayoutParams(frameParams);

            ImageView imageView = new ImageView(requireContext());
            FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(R.drawable.bg_image_placeholder);

            // Load image from Firebase URL
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.image_border)
                    .error(R.drawable.image_border)
                    .centerCrop()
                    .into(imageView);

            ImageView btnRemove = new ImageView(requireContext());
            FrameLayout.LayoutParams removeParams = new FrameLayout.LayoutParams(removeSize, removeSize);
            removeParams.gravity = Gravity.TOP | Gravity.END;
            removeParams.setMargins(0, dpToPx(4), dpToPx(4), 0);
            btnRemove.setLayoutParams(removeParams);
            btnRemove.setImageResource(R.drawable.ic_close);
            btnRemove.setBackgroundResource(R.drawable.rounded_close_bg);
            btnRemove.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            btnRemove.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

            final int position = i;
            btnRemove.setOnClickListener(v -> {
                // Add to delete list and remove from display
                imagesToDelete.add(currentImageUrls.get(position));
                currentImageUrls.remove(position);
                showCurrentImages();
            });

            frameLayout.addView(imageView);
            frameLayout.addView(btnRemove);
            container.addView(frameLayout);
        }

        // Show newly selected images (from gallery/camera)
        for (int i = 0; i < selectedNewImageUris.size(); i++) {
            Uri uri = selectedNewImageUris.get(i);

            FrameLayout frameLayout = new FrameLayout(requireContext());
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(imageSize, imageSize);
            if (i < selectedNewImageUris.size() - 1) {
                frameParams.setMarginEnd(marginEnd);
            }
            frameLayout.setLayoutParams(frameParams);

            ImageView imageView = new ImageView(requireContext());
            FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(R.drawable.bg_image_placeholder);

            // Load local image
            Glide.with(requireContext())
                    .load(uri)
                    .centerCrop()
                    .into(imageView);

            ImageView btnRemove = new ImageView(requireContext());
            FrameLayout.LayoutParams removeParams = new FrameLayout.LayoutParams(removeSize, removeSize);
            removeParams.gravity = Gravity.TOP | Gravity.END;
            removeParams.setMargins(0, dpToPx(4), dpToPx(4), 0);
            btnRemove.setLayoutParams(removeParams);
            btnRemove.setImageResource(R.drawable.ic_close);
            btnRemove.setBackgroundResource(R.drawable.rounded_close_bg);
            btnRemove.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            btnRemove.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

            final int position = i;
            btnRemove.setOnClickListener(v -> {
                selectedNewImageUris.remove(position);
                showCurrentImages();
            });

            frameLayout.addView(imageView);
            frameLayout.addView(btnRemove);
            container.addView(frameLayout);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        pickImagesLauncher.launch(Intent.createChooser(intent, "ជ្រើសរើសរូបភាព"));
    }

    private void handleImagePickerResult(ActivityResult result) {
        if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) {
            return;
        }

        Intent data = result.getData();

        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            int count = clipData.getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null && !selectedNewImageUris.contains(uri)) {
                    selectedNewImageUris.add(uri);
                }
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            if (uri != null && !selectedNewImageUris.contains(uri)) {
                selectedNewImageUris.add(uri);
            }
        }

        showCurrentImages();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void setPostingState(boolean isPosting) {
        binding.btnUpdatePost.setEnabled(!isPosting);
        binding.btnAddOrEditImage.setEnabled(!isPosting);
        if (isPosting) {
            binding.btnUpdatePost.setText("កំពុងរក្សាទុក...");
        } else {
            binding.btnUpdatePost.setText("រក្សាទុកការកែប្រែ");
        }
    }

    private void updatePost() {
        String content = binding.etPostContent.getText().toString().trim();

        if (TextUtils.isEmpty(content)) {
            binding.etPostContent.setError("សូមសរសេរអ្វីមួយ");
            binding.etPostContent.requestFocus();
            return;
        }

        if (currentUserId == null) {
            Toast.makeText(requireContext(), "អ្នកមិនបានចូលទេ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify user owns the post
        if (currentPost == null || !currentPost.getUserId().equals(currentUserId)) {
            Toast.makeText(requireContext(), "អ្នកមិនមានសិទ្ធិកែប្រែប្រកាសនេះទេ", Toast.LENGTH_SHORT).show();
            return;
        }

        setPostingState(true);

        // Delete images marked for removal
        deleteImagesFromStorage(() -> {
            // Upload new images
            uploadNewImagesToStorage(new UploadCallback() {
                @Override
                public void onSuccess(List<String> newImageUrls) {
                    // Combine remaining current images with new images
                    List<String> finalImageUrls = new ArrayList<>(currentImageUrls);
                    finalImageUrls.addAll(newImageUrls);

                    // Update post in database
                    updatePostInDatabase(content, finalImageUrls);
                }

                @Override
                public void onFailure(String errorMessage) {
                    setPostingState(false);
                    Toast.makeText(requireContext(),
                            "មិនអាចផ្ទុករូបភាពថ្មីបាន: " + errorMessage,
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void deleteImagesFromStorage(Runnable onComplete) {
        if (imagesToDelete.isEmpty()) {
            onComplete.run();
            return;
        }

        List<Task<Void>> deleteTasks = new ArrayList<>();

        for (String imageUrl : imagesToDelete) {
            try {
                // Extract file name from URL
                String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                if (fileName.contains("?")) {
                    fileName = fileName.substring(0, fileName.indexOf("?"));
                }

                StorageReference imageRef = storageRef.child("post_images").child(fileName);
                deleteTasks.add(imageRef.delete());
            } catch (Exception e) {
                // Continue even if some deletions fail
                Log.e(TAG, "Error deleting image: " + e.getMessage());
            }
        }

        Tasks.whenAllComplete(deleteTasks)
                .addOnCompleteListener(task -> {
                    // Run completion regardless of success/failure
                    onComplete.run();
                });
    }

    private void uploadNewImagesToStorage(UploadCallback callback) {
        if (selectedNewImageUris.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<Task<Uri>> uploadTasks = new ArrayList<>();

        for (Uri localUri : selectedNewImageUris) {
            String filename = UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("post_images")
                    .child(filename);

            UploadTask uploadTask = storageRef.putFile(localUri);

            Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return storageRef.getDownloadUrl();
            });

            uploadTasks.add(urlTask);
        }

        Tasks.whenAllSuccess(uploadTasks)
                .addOnSuccessListener(results -> {
                    List<String> downloadUrls = new ArrayList<>();
                    for (Object result : results) {
                        if (result instanceof Uri) {
                            downloadUrls.add(((Uri) result).toString());
                        }
                    }
                    callback.onSuccess(downloadUrls);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                });
    }

    private void updatePostInDatabase(String content, List<String> imageUrls) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("content", content);
        updates.put("imageUrls", imageUrls);
        updates.put("timestamp", String.valueOf(System.currentTimeMillis()));

        postRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    setPostingState(false);
                    Toast.makeText(requireContext(),
                            "បានកែប្រែប្រកាសដោយជោគជ័យ",
                            Toast.LENGTH_SHORT).show();

                    // Navigate back to post detail or previous fragment
                    if (homeActivity != null) {
                        homeActivity.onBackPressed();
                    }
                })
                .addOnFailureListener(e -> {
                    setPostingState(false);
                    Toast.makeText(requireContext(),
                            "មិនអាចកែប្រែប្រកាសបាន: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    interface UploadCallback {
        void onSuccess(List<String> imageUrls);
        void onFailure(String errorMessage);
    }
}