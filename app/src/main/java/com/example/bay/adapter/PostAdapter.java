package com.example.bay.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.fragment.CommunityAccountFragment;
import com.example.bay.fragment.PostDetailFragment;
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
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    private Context context;
    private List<PostCardItem> posts = new ArrayList<>();
    private OnItemClickListener listener;
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String currentUserId;

    public interface OnItemClickListener {
        void onPostClick(PostCardItem post);
        void onLikeClick(PostCardItem post);
        void onCommentClick(PostCardItem post);
        void onSaveClick(PostCardItem post);
        void onUserClick(String userId);
    }

    public PostAdapter() {
        // Default constructor
    }

    public PostAdapter(Context context) {
        this.context = context;
        FirebaseUser currentUser = mAuth.getCurrentUser();
        this.currentUserId = currentUser != null ? currentUser.getUid() : null;
    }

    public void setPosts(List<PostCardItem> posts) {
        this.posts = posts != null ? posts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_post_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (position < 0 || position >= posts.size()) return;

        PostCardItem post = posts.get(position);

        // Set post content
        holder.tvContent.setText(post.getContent() != null && !post.getContent().isEmpty()
                ? post.getContent() : "No content");

        // Set timestamp
        holder.tvDuration.setText(post.getTimestamp() != null && !post.getTimestamp().isEmpty()
                ? TimeUtils.formatTimeAgo(post.getTimestamp())
                : "មិនទាន់មាន");

        // Calculate counts
        int likeCount = post.getLikedBy() != null ? post.getLikedBy().size() : 0;
        int commentCount = post.getComments() != null ? post.getComments().size() : 0;
        int saveCount = post.getSavedBy() != null ? post.getSavedBy().size() : 0;

        holder.text_like_count.setText(String.valueOf(likeCount));
        holder.text_comment_count.setText(String.valueOf(commentCount));
        holder.text_save_count.setText(String.valueOf(saveCount));

        // Set like icon based on liked state
        boolean isLiked = currentUserId != null && post.isLikedByUser(currentUserId);
        updateLikeUi(holder, isLiked);

        // Set save icon based on saved state
        boolean isSaved = currentUserId != null && post.isSavedByUser(currentUserId);
        updateSaveUi(holder, isSaved);

        // Fetch and display user info
        if (post.getUserId() != null && !post.getUserId().isEmpty()) {
            fetchUserInfo(post.getUserId(), holder, post);
        } else {
            holder.tvUsername.setText("User");
            holder.btnProfile.setImageResource(R.drawable.img);
        }

        // Handle image display
        setupImages(holder, post.getImageUrls());

        // Set click listeners
        holder.btnProfile.setOnClickListener(v -> {
            if (listener != null && post.getUserId() != null) {
                listener.onUserClick(post.getUserId());
            } else {
                // Fallback to HomeActivity navigation
                if (context instanceof HomeActivity && post.getUserId() != null) {
                    Fragment f = CommunityAccountFragment.newInstance(post.getUserId());
                    ((HomeActivity) context).LoadFragment(f);
                    ((HomeActivity) context).hideBottomNavigation();
                }
            }
        });

        holder.tvUsername.setOnClickListener(v -> {
            if (listener != null && post.getUserId() != null) {
                listener.onUserClick(post.getUserId());
            } else {
                // Fallback to HomeActivity navigation
                if (context instanceof HomeActivity && post.getUserId() != null) {
                    Fragment f = CommunityAccountFragment.newInstance(post.getUserId());
                    ((HomeActivity) context).LoadFragment(f);
                    ((HomeActivity) context).hideBottomNavigation();
                }
            }
        });

        holder.layoutLikeCard.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLikeClick(post);
            } else {
                handleToggleLike(post, holder);
            }
        });

        holder.layoutCommentCard.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCommentClick(post);
            } else {
                // Open post detail for comments
                if (context instanceof HomeActivity) {
                    Fragment f = PostDetailFragment.newInstance(post.getItemId());
                    ((HomeActivity) context).LoadFragment(f);
                    ((HomeActivity) context).hideBottomNavigation();
                }
            }
        });

        holder.layoutSaveCard.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSaveClick(post);
            } else {
                handleToggleSave(post, holder);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            } else {
                // Open post detail
                if (context instanceof HomeActivity) {
                    Fragment f = PostDetailFragment.newInstance(post.getItemId());
                    ((HomeActivity) context).LoadFragment(f);
                    ((HomeActivity) context).hideBottomNavigation();
                }
            }
        });
    }

    private void fetchUserInfo(String userId, ViewHolder holder, PostCardItem post) {
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user == null) {
                            holder.tvUsername.setText("User");
                            holder.btnProfile.setImageResource(R.drawable.img);
                            return;
                        }

                        String name = ((user.getFirst_name() != null ? user.getFirst_name() : "") + " " +
                                (user.getLast_name() != null ? user.getLast_name() : "")).trim();
                        holder.tvUsername.setText(name.isEmpty() ? "User" : name);

                        // Store user info in post for search functionality
                        post.setUser(user);

                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            Glide.with(context)
                                    .load(user.getProfileImageUrl())
                                    .placeholder(R.drawable.img)
                                    .circleCrop()
                                    .into(holder.btnProfile);
                        } else {
                            holder.btnProfile.setImageResource(R.drawable.img);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.tvUsername.setText("User");
                    }
                });
    }

    private void setupImages(ViewHolder holder, List<String> images) {
        int imageCount = images != null ? images.size() : 0;

        // Hide all layouts first
        holder.singleImage.setVisibility(View.GONE);
        holder.twoImagesLayout.setVisibility(View.GONE);
        holder.threeImagesLayout.setVisibility(View.GONE);
        holder.fourImagesLayout.setVisibility(View.GONE);
        holder.photoGridContainer.setVisibility(View.GONE);

        if (imageCount == 0) {
            // No images, hide the container
            holder.photoGridContainer.setVisibility(View.GONE);
            return;
        }

        holder.photoGridContainer.setVisibility(View.VISIBLE);

        switch (imageCount) {
            case 1:
                holder.singleImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(images.get(0))
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(holder.singleImage);
                break;

            case 2:
                holder.twoImagesLayout.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(images.get(0))
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(holder.twoImage1);
                Glide.with(context)
                        .load(images.get(1))
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(holder.twoImage2);
                break;

            case 3:
                holder.threeImagesLayout.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(images.get(0))
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(holder.threeImage1);
                Glide.with(context)
                        .load(images.get(1))
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(holder.threeImage2);
                Glide.with(context)
                        .load(images.get(2))
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(holder.threeImage3);
                break;

            default: // 4 or more
                holder.fourImagesLayout.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(images.get(0))
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(holder.fourImage1);
                if (imageCount > 1) {
                    Glide.with(context)
                            .load(images.get(1))
                            .placeholder(R.drawable.bg_image_placeholder)
                            .into(holder.fourImage2);
                }
                if (imageCount > 2) {
                    Glide.with(context)
                            .load(images.get(2))
                            .placeholder(R.drawable.bg_image_placeholder)
                            .into(holder.fourImage3);
                }
                if (imageCount > 3) {
                    Glide.with(context)
                            .load(images.get(3))
                            .placeholder(R.drawable.bg_image_placeholder)
                            .into(holder.fourImage4);
                }

                // Show overlay for more than 4 images
                if (imageCount > 4) {
                    holder.imageOverlay.setVisibility(View.VISIBLE);
                    holder.tvMoreImages.setVisibility(View.VISIBLE);
                    holder.tvMoreImages.setText("+" + (imageCount - 4));
                } else {
                    holder.imageOverlay.setVisibility(View.GONE);
                    holder.tvMoreImages.setVisibility(View.GONE);
                }
                break;
        }
    }

    private void updateLikeUi(ViewHolder holder, boolean liked) {
        ImageViewCompat.setImageTintList(
                holder.ivLikeCard,
                ColorStateList.valueOf(ContextCompat.getColor(context, liked ? R.color.primary : R.color.textColors))
        );
    }

    private void updateSaveUi(ViewHolder holder, boolean saved) {
        ImageViewCompat.setImageTintList(
                holder.ivSaveCard,
                ColorStateList.valueOf(ContextCompat.getColor(context, saved ? R.color.primary : R.color.textColors))
        );
    }

    private void handleToggleLike(PostCardItem post, ViewHolder holder) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "សូមចូលប្រើប្រព័ន្ធជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("postCardItems")
                .child(post.getItemId())
                .child("likedBy")
                .child(uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean liked = snapshot.exists();
                if (liked) {
                    ref.removeValue();
                    if (post.getLikedBy() != null) post.getLikedBy().remove(uid);
                } else {
                    ref.setValue(true);
                    if (post.getLikedBy() == null) post.setLikedBy(new java.util.HashMap<>());
                    post.getLikedBy().put(uid, true);
                }

                int likeCount = post.getLikedBy() != null ? post.getLikedBy().size() : 0;
                holder.text_like_count.setText(String.valueOf(likeCount));
                updateLikeUi(holder, !liked);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleToggleSave(PostCardItem post, ViewHolder holder) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "សូមចូលប្រើប្រព័ន្ធជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("postCardItems")
                .child(post.getItemId())
                .child("savedBy")
                .child(uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean saved = snapshot.exists();
                if (saved) {
                    ref.removeValue();
                    if (post.getSavedBy() != null) post.getSavedBy().remove(uid);
                } else {
                    ref.setValue(true);
                    if (post.getSavedBy() == null) post.setSavedBy(new java.util.HashMap<>());
                    post.getSavedBy().put(uid, true);
                }

                int saveCount = post.getSavedBy() != null ? post.getSavedBy().size() : 0;
                holder.text_save_count.setText(String.valueOf(saveCount));
                updateSaveUi(holder, !saved);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Failed to update save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView btnProfile;
        TextView tvUsername;
        TextView tvDuration;
        TextView tvContent;

        // Image views
        ImageView singleImage;
        ImageView twoImage1, twoImage2;
        ImageView threeImage1, threeImage2, threeImage3;
        ImageView fourImage1, fourImage2, fourImage3, fourImage4;
        View imageOverlay;
        TextView tvMoreImages;

        LinearLayout twoImagesLayout;
        ConstraintLayout threeImagesLayout;
        GridLayout fourImagesLayout;
        ConstraintLayout photoGridContainer;

        // Action buttons
        LinearLayout layoutLikeCard;
        ImageView ivLikeCard;
        TextView text_like_count;

        LinearLayout layoutCommentCard;
        TextView text_comment_count;

        LinearLayout layoutSaveCard;
        ImageView ivSaveCard;
        TextView text_save_count;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // User info
            btnProfile = itemView.findViewById(R.id.btnProfile);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvContent = itemView.findViewById(R.id.tvContent);

            // Image containers
            singleImage = itemView.findViewById(R.id.singleImage);
            twoImage1 = itemView.findViewById(R.id.twoImage1);
            twoImage2 = itemView.findViewById(R.id.twoImage2);
            threeImage1 = itemView.findViewById(R.id.threeImage1);
            threeImage2 = itemView.findViewById(R.id.threeImage2);
            threeImage3 = itemView.findViewById(R.id.threeImage3);
            fourImage1 = itemView.findViewById(R.id.fourImage1);
            fourImage2 = itemView.findViewById(R.id.fourImage2);
            fourImage3 = itemView.findViewById(R.id.fourImage3);
            fourImage4 = itemView.findViewById(R.id.fourImage4);
            imageOverlay = itemView.findViewById(R.id.imageOverlay);
            tvMoreImages = itemView.findViewById(R.id.tvMoreImages);

            twoImagesLayout = itemView.findViewById(R.id.twoImagesLayout);
            threeImagesLayout = itemView.findViewById(R.id.threeImagesLayout);
            fourImagesLayout = itemView.findViewById(R.id.fourImagesLayout);
            photoGridContainer = itemView.findViewById(R.id.photoGridContainer);

            // Action buttons
            layoutLikeCard = itemView.findViewById(R.id.layoutLikeCard);
            ivLikeCard = itemView.findViewById(R.id.ivLikeCard);
            text_like_count = itemView.findViewById(R.id.text_like_count);

            layoutCommentCard = itemView.findViewById(R.id.layoutCommentCard);
            text_comment_count = itemView.findViewById(R.id.text_comment_count);

            layoutSaveCard = itemView.findViewById(R.id.layoutSaveCard);
            ivSaveCard = itemView.findViewById(R.id.ivSaveCard);
            text_save_count = itemView.findViewById(R.id.text_save_count);
        }
    }
}