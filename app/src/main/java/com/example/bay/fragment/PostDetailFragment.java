package com.example.bay.fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.core.view.WindowInsetsCompat.Builder;
import androidx.core.graphics.Insets;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.adapter.CommentThreadDecoration;
import com.example.bay.adapter.PostCommentAdapter;
import com.example.bay.databinding.FragmentPostDetailBinding;
import com.example.bay.model.Comment;
import com.example.bay.model.PostCardItem;
import com.example.bay.model.User;
import com.example.bay.util.TimeUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostDetailFragment extends Fragment {

    private FragmentPostDetailBinding binding;
    private String postId;
    private DatabaseReference postRef;
    private PostCommentAdapter commentAdapter;
    private CommentThreadDecoration threadDecoration;
    private List<Comment> commentList = new ArrayList<>();
    private FirebaseAuth mAuth;

    private String replyToCommentId = null;

    private PostCardItem currentPost;

    public static PostDetailFragment newInstance(String postId) {
        PostDetailFragment fragment = new PostDetailFragment();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        fragment.setArguments(args);
        return fragment;
    }

    public PostDetailFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPostDetailBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            postId = getArguments().getString("postId");
        }

        if (postId == null || postId.isEmpty()) {
            Toast.makeText(getContext(), "Post not found", Toast.LENGTH_SHORT).show();
            return binding.getRoot();
        }

        postRef = FirebaseDatabase.getInstance()
                .getReference("postCardItems")
                .child(postId);

        setupCommentsRecyclerView();
        loadPostData();
        setupSendComment();
        setupCancelReply();
        loadCurrentUserProfile();

        // 🔥 FIX: move layout up when keyboard (IME) is shown
        ViewCompat.setOnApplyWindowInsetsListener(binding.postDetailRoot, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = imeInsets.bottom; // > 0 when keyboard visible, 0 when hidden

            // Add bottom padding so commentInputContainer is above keyboard
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    bottom
            );

            return insets;
        });

        // When user taps comment input or whole bar → focus + open keyboard
        binding.etComment.setOnClickListener(v -> focusCommentInput());
        binding.commentInputContainer.setOnClickListener(v -> focusCommentInput());

        return binding.getRoot();
    }

    private void setupCommentsRecyclerView() {
        commentAdapter = new PostCommentAdapter(requireContext(), comment -> {
            replyToCommentId = getThreadRootId(comment);

            String userId = comment.getUserId();
            if (userId != null && !userId.isEmpty()) {
                FirebaseDatabase.getInstance().getReference("users")
                        .child(userId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                User user = snapshot.getValue(User.class);
                                String fullName = "";
                                if (user != null) {
                                    if (user.getFirst_name() != null)
                                        fullName += user.getFirst_name() + " ";
                                    if (user.getLast_name() != null)
                                        fullName += user.getLast_name();
                                    fullName = fullName.trim();
                                }

                                if (fullName.isEmpty()) {
                                    fullName = "អ្នកប្រើប្រាស់";
                                }

                                if (binding.layoutReplyInfo != null) {
                                    binding.layoutReplyInfo.setVisibility(View.VISIBLE);
                                    binding.tvReplyToUsernameInput.setText(fullName);
                                }

                                binding.etComment.setHint("ឆ្លើយតបទៅកាន់ " + fullName);
                                focusCommentInput();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                if (binding.layoutReplyInfo != null) {
                                    binding.layoutReplyInfo.setVisibility(View.VISIBLE);
                                    binding.tvReplyToUsernameInput.setText("អ្នកប្រើប្រាស់");
                                }
                                binding.etComment.setHint("កំពុងឆ្លើយតប...");
                                focusCommentInput();
                            }
                        });
            } else {
                if (binding.layoutReplyInfo != null) {
                    binding.layoutReplyInfo.setVisibility(View.VISIBLE);
                    binding.tvReplyToUsernameInput.setText("អ្នកប្រើប្រាស់");
                }
                binding.etComment.setHint("កំពុងឆ្លើយតប...");
                focusCommentInput();
            }
        });

        binding.rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvComments.setAdapter(commentAdapter);

        binding.button.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        threadDecoration = new CommentThreadDecoration(commentAdapter, requireContext());
        binding.rvComments.addItemDecoration(threadDecoration);
    }

    private String getThreadRootId(Comment comment) {
        if (comment == null) return null;

        if (comment.getParentCommentId() == null || comment.getParentCommentId().isEmpty()) {
            return comment.getCommentId();
        }

        Map<String, Comment> map = new HashMap<>();
        for (Comment c : commentList) {
            if (c.getCommentId() != null) {
                map.put(c.getCommentId(), c);
            }
        }

        String currentId = comment.getParentCommentId();
        Comment current = map.get(currentId);

        while (current != null &&
                current.getParentCommentId() != null &&
                !current.getParentCommentId().isEmpty() &&
                map.containsKey(current.getParentCommentId())) {

            currentId = current.getParentCommentId();
            current = map.get(currentId);
        }

        return currentId != null ? currentId : comment.getCommentId();
    }

    private void loadPostData() {
        postRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), "Post not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                DataSnapshot commentsSnap = snapshot.child("comments");

                PostCardItem post = snapshot.getValue(PostCardItem.class);
                if (post == null) {
                    Toast.makeText(getContext(), "Post data invalid", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (post.getItemId() == null || post.getItemId().isEmpty()) {
                    post.setItemId(postId);
                }

                if (post.getLikedBy() != null) {
                    post.setLikeCount(post.getLikedBy().size());
                }
                if (post.getSavedBy() != null) {
                    post.setSaveCount(post.getSavedBy().size());
                }
                if (commentsSnap.exists()) {
                    post.setCommentCount(commentsSnap.getChildrenCount());
                }

                currentPost = post;

                bindPostToViews(post);

                Map<String, Comment> commentMap = new HashMap<>();
                if (commentsSnap.exists()) {
                    for (DataSnapshot cSnap : commentsSnap.getChildren()) {
                        Comment c = cSnap.getValue(Comment.class);
                        if (c != null) {
                            if (c.getCommentId() == null || c.getCommentId().isEmpty()) {
                                c.setCommentId(cSnap.getKey());
                            }
                            commentMap.put(c.getCommentId(), c);
                        }
                    }
                }

                Map<String, List<Comment>> childrenMap = new HashMap<>();
                List<Comment> roots = new ArrayList<>();

                for (Comment c : commentMap.values()) {
                    String parentId = c.getParentCommentId();
                    if (parentId == null || parentId.isEmpty() || !commentMap.containsKey(parentId)) {
                        roots.add(c);
                    } else {
                        List<Comment> list = childrenMap.get(parentId);
                        if (list == null) {
                            list = new ArrayList<>();
                            childrenMap.put(parentId, list);
                        }
                        list.add(c);
                    }
                }

                Collections.sort(roots, (c1, c2) ->
                        Long.compare(parseTimestamp(c2.getTimestamp()), parseTimestamp(c1.getTimestamp()))
                );

                for (Map.Entry<String, List<Comment>> entry : childrenMap.entrySet()) {
                    List<Comment> list = entry.getValue();
                    Collections.sort(list, (c1, c2) ->
                            Long.compare(parseTimestamp(c1.getTimestamp()), parseTimestamp(c2.getTimestamp()))
                    );
                }

                List<Comment> orderedComments = new ArrayList<>();
                for (Comment root : roots) {
                    orderedComments.add(root);
                    addChildrenRecursively(root, childrenMap, orderedComments);
                }

                commentList = orderedComments;
                commentAdapter.setComments(commentList);
                binding.textCommentCount.setText(String.valueOf(commentList.size()));

                binding.rvComments.invalidateItemDecorations();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostDetailFragment", "loadPostData cancelled: " + error.getMessage());
            }
        });
    }

    private void addChildrenRecursively(Comment parent,
                                        Map<String, List<Comment>> childrenMap,
                                        List<Comment> out) {
        List<Comment> children = childrenMap.get(parent.getCommentId());
        if (children == null || children.isEmpty()) return;

        for (Comment child : children) {
            out.add(child);
            addChildrenRecursively(child, childrenMap, out);
        }
    }

    private long parseTimestamp(String ts) {
        if (ts == null) return 0L;

        try {
            return Long.parseLong(ts);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void bindPostToViews(PostCardItem post) {
        if (binding == null) return;

        binding.tvContent.setText(post.getContent() != null ? post.getContent() : "");

        if (post.getTimestamp() != null && !post.getTimestamp().isEmpty()) {
            String timeAgo = TimeUtils.formatTimeAgo(post.getTimestamp());
            binding.tvDuration.setText(timeAgo);
        } else {
            binding.tvDuration.setText("មិនទាន់មាន");
        }

        long likeCount = post.getLikedBy() != null ? post.getLikedBy().size() : post.getLikeCount();
        long saveCount = post.getSavedBy() != null ? post.getSavedBy().size() : post.getSaveCount();
        long commentCount = post.getComments() != null ? post.getComments().size() : post.getCommentCount();

        binding.textLikeCount.setText(String.valueOf(likeCount));
        binding.textCommentCount.setText(String.valueOf(commentCount));
        binding.textSaveCount.setText(String.valueOf(saveCount));

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;

        boolean isLiked = uid != null && post.isLikedByUser(uid);
        boolean isSaved = uid != null && post.isSavedByUser(uid);

        updateLikeUi(isLiked, likeCount);
        updateSaveUi(isSaved, saveCount);

        binding.layoutLike.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "សូមចូលប្រើប្រព័ន្ធជាមុន", Toast.LENGTH_SHORT).show();
                return;
            }
            toggleLike();
        });

        binding.layoutSave.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "សូមចូលប្រើប្រព័ន្ធជាមុន", Toast.LENGTH_SHORT).show();
                return;
            }
            toggleSave();
        });

        if (post.getUserId() != null && !post.getUserId().isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(post.getUserId());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (binding == null) return;

                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            String fullName = "";
                            if (user.getFirst_name() != null)
                                fullName += user.getFirst_name() + " ";
                            if (user.getLast_name() != null)
                                fullName += user.getLast_name();
                            fullName = fullName.trim();
                            if (!fullName.isEmpty()) {
                                binding.tvUsername.setText(fullName);
                            } else {
                                binding.tvUsername.setText("អ្នកប្រើប្រាស់");
                            }

                            String profileUrl = user.getProfileImageUrl();
                            if (profileUrl != null && !profileUrl.isEmpty()) {
                                Glide.with(requireContext())
                                        .load(profileUrl)
                                        .placeholder(R.drawable.img)
                                        .into(binding.btnProfile);
                            } else {
                                binding.btnProfile.setImageResource(R.drawable.img);
                            }
                        } else {
                            binding.tvUsername.setText("អ្នកប្រើប្រាស់");
                            binding.btnProfile.setImageResource(R.drawable.img);
                        }
                    } else {
                        binding.tvUsername.setText("អ្នកប្រើប្រាស់");
                        binding.btnProfile.setImageResource(R.drawable.img);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (binding == null) return;
                    binding.tvUsername.setText("អ្នកប្រើប្រាស់");
                    binding.btnProfile.setImageResource(R.drawable.img);
                }
            });
        } else {
            binding.tvUsername.setText("អ្នកប្រើប្រាស់");
            binding.btnProfile.setImageResource(R.drawable.img);
        }

        setupPhotoGrid(post);
    }

    private void setupPhotoGrid(PostCardItem item) {
        List<String> images = item.getImageUrls();

        binding.singleImage.setVisibility(View.GONE);
        binding.twoImagesLayout.setVisibility(View.GONE);
        binding.threeImagesLayout.setVisibility(View.GONE);
        binding.fourImagesLayout.setVisibility(View.GONE);
        binding.imageOverlay.setVisibility(View.GONE);
        binding.tvMoreImages.setVisibility(View.GONE);

        if (images != null && !images.isEmpty()) {
            binding.photoGridContainer.setVisibility(View.VISIBLE);
            int imageCount = Math.min(images.size(), 4);

            switch (imageCount) {
                case 1:
                    setupSingleImage(images);
                    break;
                case 2:
                    setupTwoImages(images);
                    break;
                case 3:
                    setupThreeImages(images);
                    break;
                case 4:
                    setupFourImages(images, images.size());
                    break;
            }
        } else {
            binding.photoGridContainer.setVisibility(View.GONE);
        }
    }

    private void setupSingleImage(List<String> images) {
        binding.singleImage.setVisibility(View.VISIBLE);
        Glide.with(requireContext())
                .load(images.get(0))
                .placeholder(R.drawable.img)
                .into(binding.singleImage);
    }

    private void setupTwoImages(List<String> images) {
        binding.twoImagesLayout.setVisibility(View.VISIBLE);

        Glide.with(requireContext())
                .load(images.get(0))
                .placeholder(R.drawable.img)
                .into(binding.twoImage1);

        Glide.with(requireContext())
                .load(images.get(1))
                .placeholder(R.drawable.img)
                .into(binding.twoImage2);
    }

    private void setupThreeImages(List<String> images) {
        binding.threeImagesLayout.setVisibility(View.VISIBLE);

        Glide.with(requireContext())
                .load(images.get(0))
                .placeholder(R.drawable.img)
                .into(binding.threeImage1);

        Glide.with(requireContext())
                .load(images.get(1))
                .placeholder(R.drawable.img)
                .into(binding.threeImage2);

        Glide.with(requireContext())
                .load(images.get(2))
                .placeholder(R.drawable.img)
                .into(binding.threeImage3);
    }

    private void setupFourImages(List<String> images, int totalCount) {
        binding.fourImagesLayout.setVisibility(View.VISIBLE);

        Glide.with(requireContext())
                .load(images.get(0))
                .placeholder(R.drawable.img)
                .into(binding.fourImage1);

        Glide.with(requireContext())
                .load(images.get(1))
                .placeholder(R.drawable.img)
                .into(binding.fourImage2);

        Glide.with(requireContext())
                .load(images.get(2))
                .placeholder(R.drawable.img)
                .into(binding.fourImage3);

        Glide.with(requireContext())
                .load(images.get(3))
                .placeholder(R.drawable.img)
                .into(binding.fourImage4);

        if (totalCount > 4) {
            binding.imageOverlay.setVisibility(View.VISIBLE);
            binding.tvMoreImages.setVisibility(View.VISIBLE);
            binding.tvMoreImages.setText("+" + (totalCount - 4));
        }
    }

    private void setupSendComment() {
        binding.btnSendComment.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(getContext(), "សូមចូលប្រើប្រព័ន្ធជាមុន", Toast.LENGTH_SHORT).show();
                return;
            }

            String text = binding.etComment.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                return;
            }

            String userId = currentUser.getUid();
            sendCommentToDatabase(userId, text);
        });
    }

    private void setupCancelReply() {
        if (binding.tvCancelReply != null && binding.layoutReplyInfo != null) {
            binding.tvCancelReply.setOnClickListener(v -> {
                replyToCommentId = null;
                binding.layoutReplyInfo.setVisibility(View.GONE);
                binding.etComment.setHint("សរសេរមតិយោបល់...");
                focusCommentInput();
            });
        }
    }

    private void loadCurrentUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (binding == null) return;

        if (currentUser == null) {
            if (binding.ivCurrentUserProfile != null) {
                binding.ivCurrentUserProfile.setImageResource(R.drawable.img);
            }
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null || binding.ivCurrentUserProfile == null) return;

                if (!snapshot.exists()) {
                    binding.ivCurrentUserProfile.setImageResource(R.drawable.img);
                    return;
                }

                User user = snapshot.getValue(User.class);
                if (user == null) {
                    binding.ivCurrentUserProfile.setImageResource(R.drawable.img);
                    return;
                }

                String profileUrl = user.getProfileImageUrl();
                if (profileUrl != null && !profileUrl.isEmpty()) {
                    Glide.with(requireContext())
                            .load(profileUrl)
                            .placeholder(R.drawable.img)
                            .into(binding.ivCurrentUserProfile);
                } else {
                    binding.ivCurrentUserProfile.setImageResource(R.drawable.img);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null && binding.ivCurrentUserProfile != null) {
                    binding.ivCurrentUserProfile.setImageResource(R.drawable.img);
                }
            }
        });
    }

    private void sendCommentToDatabase(String userId, String text) {
        String commentId = postRef.child("comments").push().getKey();
        if (commentId == null) {
            Toast.makeText(getContext(), "Error sending comment", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = String.valueOf(System.currentTimeMillis());

        Comment comment;
        if (replyToCommentId != null && !replyToCommentId.isEmpty()) {
            comment = new Comment(
                    commentId,
                    userId,
                    text,
                    timestamp,
                    replyToCommentId
            );
        } else {
            comment = new Comment(
                    commentId,
                    userId,
                    text,
                    timestamp
            );
        }

        postRef.child("comments").child(commentId).setValue(comment)
                .addOnSuccessListener(unused -> {
                    binding.etComment.setText("");
                    Toast.makeText(getContext(), "បានបញ្ចូនមតិយោបល់", Toast.LENGTH_SHORT).show();

                    replyToCommentId = null;
                    if (binding.layoutReplyInfo != null) {
                        binding.layoutReplyInfo.setVisibility(View.GONE);
                    }
                    binding.etComment.setHint("សរសេរមតិយោបល់...");

                    hideKeyboard();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "បញ្ចូនមតិយោបល់បរាជ័យ", Toast.LENGTH_SHORT).show()
                );
    }

    private void updateLikeUi(boolean isLiked, long likeCount) {
        if (binding == null) return;

        binding.textLikeCount.setText(String.valueOf(likeCount));

        if (binding.ivLike != null) {
            int color = ContextCompat.getColor(
                    requireContext(),
                    isLiked ? R.color.primary : R.color.textColors
            );
            ImageViewCompat.setImageTintList(
                    binding.ivLike,
                    ColorStateList.valueOf(color)
            );
        }
    }

    private void updateSaveUi(boolean isSaved, long saveCount) {
        if (binding == null) return;

        binding.textSaveCount.setText(String.valueOf(saveCount));

        if (binding.ivSave != null) {
            int color = ContextCompat.getColor(
                    requireContext(),
                    isSaved ? R.color.primary : R.color.textColors
            );
            ImageViewCompat.setImageTintList(
                    binding.ivSave,
                    ColorStateList.valueOf(color)
            );
        }
    }

    private void toggleLike() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "សូមចូលប្រើប្រព័ន្ធជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference likeRef = postRef.child("likedBy").child(uid);

        likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    likeRef.removeValue();
                } else {
                    likeRef.setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostDetailFragment", "toggleLike cancelled: " + error.getMessage());
            }
        });
    }

    private void toggleSave() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "សូមចូលប្រើប្រព័ន្ធជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference saveRef = postRef.child("savedBy").child(uid);

        saveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    saveRef.removeValue();
                } else {
                    saveRef.setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostDetailFragment", "toggleSave cancelled: " + error.getMessage());
            }
        });
    }

    private void focusCommentInput() {
        if (binding == null) return;

        binding.etComment.requestFocus();

        binding.scrollViewPostDetail.post(() ->
                binding.scrollViewPostDetail.fullScroll(View.FOCUS_DOWN)
        );

        InputMethodManager imm =
                (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        if (binding == null) return;
        InputMethodManager imm =
                (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && binding.etComment != null) {
            imm.hideSoftInputFromWindow(binding.etComment.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
