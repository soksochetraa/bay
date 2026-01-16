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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.CommentThreadDecoration;
import com.example.bay.adapter.PostCommentAdapter;
import com.example.bay.databinding.FragmentPostDetailBinding;
import com.example.bay.model.Comment;
import com.example.bay.model.PostCardItem;
import com.example.bay.model.User;
import com.example.bay.ui.PostMenuBottomSheet;
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
    private ValueEventListener postListener;
    private PostCommentAdapter commentAdapter;
    private CommentThreadDecoration threadDecoration;
    private List<Comment> commentList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private String replyToCommentId;
    private PostCardItem currentPost;

    public static PostDetailFragment newInstance(String postId) {
        PostDetailFragment fragment = new PostDetailFragment();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentPostDetailBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            postId = getArguments().getString("postId");
        }

        if (postId == null || postId.isEmpty()) {
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.postDetailRoot, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    imeInsets.bottom
            );
            return insets;
        });

        binding.etComment.setOnClickListener(v -> focusCommentInput());
        binding.commentInputContainer.setOnClickListener(v -> focusCommentInput());

        binding.ivPostMenu.setOnClickListener(v -> {
            if (currentPost == null || !isAdded()) return;

            PostMenuBottomSheet sheet =
                    new PostMenuBottomSheet(
                            requireActivity(),
                            new PostMenuBottomSheet.Callback() {
                                @Override
                                public void onViewSeller() {
                                    Fragment fragment =
                                            CommunityAccountFragment.newInstance(currentPost.getUserId());
                                    if (requireActivity() instanceof HomeActivity) {
                                        HomeActivity act = (HomeActivity) requireActivity();
                                        act.LoadFragment(fragment);
                                        act.hideBottomNavigation();
                                    }
                                }

                                @Override
                                public void onMessage() {}
                            }
                    );
            sheet.show();
        });

        return binding.getRoot();
    }

    private void setupCommentsRecyclerView() {
        commentAdapter = new PostCommentAdapter(
                requireContext(),
                comment -> {
                    replyToCommentId = getThreadRootId(comment);
                    String userId = comment.getUserId();
                    if (userId == null || binding == null) return;

                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(userId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (binding == null || !isAdded()) return;
                                    User user = snapshot.getValue(User.class);
                                    String name = "";
                                    if (user != null) {
                                        if (user.getFirst_name() != null)
                                            name += user.getFirst_name() + " ";
                                        if (user.getLast_name() != null)
                                            name += user.getLast_name();
                                    }
                                    if (name.trim().isEmpty()) name = "អ្នកប្រើប្រាស់";
                                    binding.layoutReplyInfo.setVisibility(View.VISIBLE);
                                    binding.tvReplyToUsernameInput.setText(name);
                                    binding.etComment.setHint("ឆ្លើយតបទៅកាន់ " + name);
                                    focusCommentInput();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
        );

        binding.rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvComments.setAdapter(commentAdapter);

        threadDecoration =
                new CommentThreadDecoration(commentAdapter, requireContext(), binding.rvComments);
        binding.rvComments.addItemDecoration(threadDecoration);
    }

    private String getThreadRootId(Comment comment) {
        if (comment == null) return null;
        if (comment.getParentCommentId() == null || comment.getParentCommentId().isEmpty()) {
            return comment.getCommentId();
        }

        Map<String, Comment> map = new HashMap<>();
        for (Comment c : commentList) {
            if (c.getCommentId() != null) map.put(c.getCommentId(), c);
        }

        String currentId = comment.getParentCommentId();
        Comment current = map.get(currentId);

        while (current != null &&
                current.getParentCommentId() != null &&
                map.containsKey(current.getParentCommentId())) {
            currentId = current.getParentCommentId();
            current = map.get(currentId);
        }

        return currentId != null ? currentId : comment.getCommentId();
    }

    private void loadPostData() {
        postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null || !isAdded()) return;
                if (!snapshot.exists()) return;

                PostCardItem post = snapshot.getValue(PostCardItem.class);
                if (post == null) return;

                if (post.getItemId() == null) post.setItemId(postId);
                currentPost = post;

                bindPostToViews(post);

                Map<String, Comment> map = new HashMap<>();
                DataSnapshot commentsSnap = snapshot.child("comments");

                for (DataSnapshot cSnap : commentsSnap.getChildren()) {
                    Comment c = cSnap.getValue(Comment.class);
                    if (c != null) {
                        if (c.getCommentId() == null) c.setCommentId(cSnap.getKey());
                        map.put(c.getCommentId(), c);
                    }
                }

                Map<String, List<Comment>> children = new HashMap<>();
                List<Comment> roots = new ArrayList<>();

                for (Comment c : map.values()) {
                    String parent = c.getParentCommentId();
                    if (parent == null || !map.containsKey(parent)) {
                        roots.add(c);
                    } else {
                        children.computeIfAbsent(parent, k -> new ArrayList<>()).add(c);
                    }
                }

                Collections.sort(roots, (a, b) ->
                        Long.compare(parseTimestamp(b.getTimestamp()), parseTimestamp(a.getTimestamp()))
                );

                List<Comment> ordered = new ArrayList<>();
                for (Comment r : roots) {
                    ordered.add(r);
                    addChildren(r, children, ordered);
                }

                commentList = ordered;
                commentAdapter.setComments(commentList);
                binding.textCommentCount.setText(String.valueOf(commentList.size()));
                binding.rvComments.invalidateItemDecorations();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostDetailFragment", error.getMessage());
            }
        };

        postRef.addValueEventListener(postListener);
    }

    private void addChildren(Comment parent,
                             Map<String, List<Comment>> map,
                             List<Comment> out) {
        List<Comment> list = map.get(parent.getCommentId());
        if (list == null) return;
        for (Comment c : list) {
            out.add(c);
            addChildren(c, map, out);
        }
    }

    private long parseTimestamp(String ts) {
        try {
            return Long.parseLong(ts);
        } catch (Exception e) {
            return 0;
        }
    }

    private void bindPostToViews(PostCardItem post) {
        if (binding == null) return;

        binding.tvContent.setText(post.getContent() != null ? post.getContent() : "");

        binding.tvDuration.setText(
                post.getTimestamp() != null
                        ? TimeUtils.formatTimeAgo(post.getTimestamp())
                        : "មិនទាន់មាន"
        );

        long likeCount = post.getLikedBy() != null ? post.getLikedBy().size() : 0;
        long saveCount = post.getSavedBy() != null ? post.getSavedBy().size() : 0;
        long commentCount = post.getComments() != null ? post.getComments().size() : 0;

        binding.textLikeCount.setText(String.valueOf(likeCount));
        binding.textSaveCount.setText(String.valueOf(saveCount));
        binding.textCommentCount.setText(String.valueOf(commentCount));

        FirebaseUser user = mAuth.getCurrentUser();
        String uid = user != null ? user.getUid() : null;

        updateLikeUi(uid != null && post.isLikedByUser(uid), likeCount);
        updateSaveUi(uid != null && post.isSavedByUser(uid), saveCount);

        binding.layoutLike.setOnClickListener(v -> toggleLike());
        binding.layoutSave.setOnClickListener(v -> toggleSave());

        if (post.getUserId() != null) {
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(post.getUserId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (binding == null || !isAdded()) return;
                            User u = snapshot.getValue(User.class);
                            if (u == null) return;
                            String name =
                                    ((u.getFirst_name() != null ? u.getFirst_name() : "") + " " +
                                            (u.getLast_name() != null ? u.getLast_name() : "")).trim();
                            binding.tvUsername.setText(name.isEmpty() ? "អ្នកប្រើប្រាស់" : name);

                            Glide.with(requireContext())
                                    .load(u.getProfileImageUrl())
                                    .placeholder(R.drawable.img)
                                    .into(binding.btnProfile);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
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

        if (images == null || images.isEmpty()) {
            binding.photoGridContainer.setVisibility(View.GONE);
            return;
        }

        binding.photoGridContainer.setVisibility(View.VISIBLE);
        int count = Math.min(images.size(), 4);

        if (count == 1) {
            binding.singleImage.setVisibility(View.VISIBLE);
            Glide.with(requireContext()).load(images.get(0)).placeholder(R.drawable.img).into(binding.singleImage);
        } else if (count == 2) {
            binding.twoImagesLayout.setVisibility(View.VISIBLE);
            Glide.with(requireContext()).load(images.get(0)).placeholder(R.drawable.img).into(binding.twoImage1);
            Glide.with(requireContext()).load(images.get(1)).placeholder(R.drawable.img).into(binding.twoImage2);
        } else if (count == 3) {
            binding.threeImagesLayout.setVisibility(View.VISIBLE);
            Glide.with(requireContext()).load(images.get(0)).placeholder(R.drawable.img).into(binding.threeImage1);
            Glide.with(requireContext()).load(images.get(1)).placeholder(R.drawable.img).into(binding.threeImage2);
            Glide.with(requireContext()).load(images.get(2)).placeholder(R.drawable.img).into(binding.threeImage3);
        } else {
            binding.fourImagesLayout.setVisibility(View.VISIBLE);
            Glide.with(requireContext()).load(images.get(0)).placeholder(R.drawable.img).into(binding.fourImage1);
            Glide.with(requireContext()).load(images.get(1)).placeholder(R.drawable.img).into(binding.fourImage2);
            Glide.with(requireContext()).load(images.get(2)).placeholder(R.drawable.img).into(binding.fourImage3);
            Glide.with(requireContext()).load(images.get(3)).placeholder(R.drawable.img).into(binding.fourImage4);
            if (images.size() > 4) {
                binding.imageOverlay.setVisibility(View.VISIBLE);
                binding.tvMoreImages.setVisibility(View.VISIBLE);
                binding.tvMoreImages.setText("+" + (images.size() - 4));
            }
        }
    }

    private void setupSendComment() {
        binding.btnSendComment.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;

            String text = binding.etComment.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;

            sendCommentToDatabase(user.getUid(), text);
        });
    }

    private void setupCancelReply() {
        binding.tvCancelReply.setOnClickListener(v -> {
            replyToCommentId = null;
            binding.layoutReplyInfo.setVisibility(View.GONE);
            binding.etComment.setHint("សរសេរមតិយោបល់...");
            focusCommentInput();
        });
    }

    private void loadCurrentUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (binding == null || !isAdded()) return;
                        User u = snapshot.getValue(User.class);
                        if (u != null) {
                            Glide.with(requireContext())
                                    .load(u.getProfileImageUrl())
                                    .placeholder(R.drawable.img)
                                    .into(binding.ivCurrentUserProfile);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sendCommentToDatabase(String userId, String text) {
        String id = postRef.child("comments").push().getKey();
        if (id == null) return;

        Comment c = replyToCommentId != null
                ? new Comment(id, userId, text, String.valueOf(System.currentTimeMillis()), replyToCommentId)
                : new Comment(id, userId, text, String.valueOf(System.currentTimeMillis()));

        postRef.child("comments").child(id).setValue(c)
                .addOnSuccessListener(v -> {
                    if (binding == null) return;
                    binding.etComment.setText("");
                    replyToCommentId = null;
                    binding.layoutReplyInfo.setVisibility(View.GONE);
                    binding.etComment.setHint("សរសេរមតិយោបល់...");
                    hideKeyboard();
                });
    }

    private void updateLikeUi(boolean liked, long count) {
        if (binding == null) return;
        binding.textLikeCount.setText(String.valueOf(count));
        int color = ContextCompat.getColor(
                requireContext(),
                liked ? R.color.primary : R.color.textColors
        );
        ImageViewCompat.setImageTintList(binding.ivLike, ColorStateList.valueOf(color));
    }

    private void updateSaveUi(boolean saved, long count) {
        if (binding == null) return;
        binding.textSaveCount.setText(String.valueOf(count));
        int color = ContextCompat.getColor(
                requireContext(),
                saved ? R.color.primary : R.color.textColors
        );
        ImageViewCompat.setImageTintList(binding.ivSave, ColorStateList.valueOf(color));
    }

    private void toggleLike() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = postRef.child("likedBy").child(user.getUid());
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) ref.removeValue();
                else ref.setValue(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void toggleSave() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = postRef.child("savedBy").child(user.getUid());
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) ref.removeValue();
                else ref.setValue(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
        if (imm != null) imm.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        if (binding == null) return;
        InputMethodManager imm =
                (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(binding.etComment.getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postRef != null && postListener != null) {
            postRef.removeEventListener(postListener);
        }
        binding = null;
    }
}
