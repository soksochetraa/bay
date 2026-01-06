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
import com.example.bay.R;
import com.example.bay.databinding.FragmentCreatePostCardBinding;
import com.example.bay.model.PostCardItem;
import com.example.bay.model.User;
import com.example.bay.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CreatePostCardFragment extends Fragment {

    private static final String TAG = "CreatePostCardFragment";

    private FragmentCreatePostCardBinding binding;
    private ActivityResultLauncher<Intent> pickImagesLauncher;
    private final List<Uri> selectedImageUris = new ArrayList<>();

    private UserRepository userRepository;
    private FirebaseStorage storage;
    private DatabaseReference databaseReference;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userRepository = new UserRepository();
        storage = FirebaseStorage.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("postCardItems");

        pickImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleImagePickerResult
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreatePostCardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupCurrentUserUI();
        setupClickListeners();
        updateImagePreviews();
    }

    private void setupCurrentUserUI() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            binding.tvCurrentUserName.setText(getString(R.string.app_name));
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
                        displayName = getString(R.string.app_name);
                    }

                    binding.tvCurrentUserName.setText(displayName);

                    String profileUrl = user.getProfileImageUrl();
                    if (!TextUtils.isEmpty(profileUrl)) {
                        Glide.with(CreatePostCardFragment.this)
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
            name = getString(R.string.app_name);
        }
        binding.tvCurrentUserName.setText(name);

        if (firebaseUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(firebaseUser.getPhotoUrl())
                    .placeholder(R.drawable.img)
                    .centerCrop()
                    .into(binding.ivCurrentUserAvatar);
        } else {
            binding.ivCurrentUserAvatar.setImageResource(R.drawable.img);
        }
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnAddImage.setOnClickListener(v -> openImagePicker());
        binding.btnPost.setOnClickListener(v -> createPost());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        pickImagesLauncher.launch(Intent.createChooser(intent, "Select Images"));
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
                if (uri != null && !selectedImageUris.contains(uri)) {
                    selectedImageUris.add(uri);
                }
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            if (uri != null && !selectedImageUris.contains(uri)) {
                selectedImageUris.add(uri);
            }
        }

        updateImagePreviews();
    }

    private void updateImagePreviews() {
        LinearLayout container = binding.layoutSelectedImages;
        container.removeAllViews();

        if (selectedImageUris.isEmpty()) {
            binding.horizontalImageScroll.setVisibility(View.GONE);
            binding.tvNoImageHint.setVisibility(View.VISIBLE);
            return;
        } else {
            binding.horizontalImageScroll.setVisibility(View.VISIBLE);
            binding.tvNoImageHint.setVisibility(View.GONE);
        }

        int imageSize = dpToPx(120);
        int marginEnd = dpToPx(8);
        int removeSize = dpToPx(22);

        for (int i = 0; i < selectedImageUris.size(); i++) {
            Uri uri = selectedImageUris.get(i);

            FrameLayout frameLayout = new FrameLayout(requireContext());
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(imageSize, imageSize);
            if (i < selectedImageUris.size() - 1) {
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

            Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(imageView);

            ImageView btnRemove = new ImageView(requireContext());
            FrameLayout.LayoutParams removeParams = new FrameLayout.LayoutParams(removeSize, removeSize);
            removeParams.gravity = Gravity.TOP | Gravity.END;
            removeParams.setMargins(0, dpToPx(4), dpToPx(4), 0);
            btnRemove.setLayoutParams(removeParams);
            btnRemove.setImageResource(R.drawable.ico_close);
            btnRemove.setBackgroundResource(R.drawable.bg_circle_button);
            btnRemove.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            btnRemove.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

            btnRemove.setOnClickListener(v -> {
                selectedImageUris.remove(uri);
                updateImagePreviews();
            });

            frameLayout.addView(imageView);
            frameLayout.addView(btnRemove);

            container.addView(frameLayout);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void setPostingState(boolean isPosting) {
        binding.btnPost.setEnabled(!isPosting);
        binding.btnAddImage.setEnabled(!isPosting);
        if (isPosting) {
            binding.btnPost.setText("កំពុងបង្ហោះ...");
        } else {
            binding.btnPost.setText("បង្ហោះ");
        }
    }

    private void createPost() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(),
                    "គ្មានអ្នកប្រើប្រាស់បានចូល", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        String content = binding.etPostContent.getText().toString().trim();

        if (TextUtils.isEmpty(content)) {
            binding.etPostContent.setError("សូមសរសេរអ្វីមួយមុនពេលបង្ហោះ");
            binding.etPostContent.requestFocus();
            return;
        }

        setPostingState(true);

        uploadImagesToStorage(new UploadCallback() {
            @Override
            public void onSuccess(List<String> imageUrls) {
                savePostToRealtimeDatabase(userId, content, imageUrls);
            }

            @Override
            public void onFailure(String errorMessage) {
                setPostingState(false);
                Toast.makeText(requireContext(),
                        "បង្ហោះរូបភាពមិនបានជោគជ័យ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImagesToStorage(UploadCallback callback) {
        if (selectedImageUris.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<Task<Uri>> uploadTasks = new ArrayList<>();

        for (Uri localUri : selectedImageUris) {
            String filename = UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = storage.getReference()
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

    private void savePostToRealtimeDatabase(String userId, String content, List<String> imageUrls) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String key = databaseReference.push().getKey();

        if (key == null) {
            setPostingState(false);
            Toast.makeText(requireContext(),
                    "មានបញ្ហាក្នុងការបង្កើតសោ", Toast.LENGTH_SHORT).show();
            return;
        }

        PostCardItem post = new PostCardItem(
                key,
                userId,
                content,
                content,
                imageUrls,
                timestamp
        );

        Map<String, Object> postValues = postToMap(post);

        databaseReference.child(key)
                .setValue(postValues)
                .addOnSuccessListener(unused -> {
                    setPostingState(false);
                    Toast.makeText(requireContext(),
                            "បង្ហោះប្រកាសបានជោគជ័យ", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    setPostingState(false);
                    Toast.makeText(requireContext(),
                            "បង្ហោះមិនបានជោគជ័យ", Toast.LENGTH_SHORT).show();
                });
    }

    private Map<String, Object> postToMap(PostCardItem post) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemId", post.getItemId());
        map.put("userId", post.getUserId());
        map.put("title", post.getTitle());
        map.put("content", post.getContent());
        map.put("imageUrls", post.getImageUrls());
        map.put("timestamp", post.getTimestamp());
        map.put("likedBy", post.getLikedBy());
        map.put("savedBy", post.getSavedBy());
        map.put("comments", post.getComments());
        return map;
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