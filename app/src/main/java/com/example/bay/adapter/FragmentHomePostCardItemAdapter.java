package com.example.bay.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class FragmentHomePostCardItemAdapter extends RecyclerView.Adapter<FragmentHomePostCardItemAdapter.ViewHolder> {

    private final Context context;
    private List<PostCardItem> postCardItemList = new ArrayList<>();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public FragmentHomePostCardItemAdapter(Context context) {
        this.context = context;
        Log.d("PostAdapter", "Adapter created");
    }

    public void setPostCardItemList(List<PostCardItem> list) {
        Log.d("PostAdapter", "setPostCardItemList called with size: " + (list != null ? list.size() : 0));
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                PostCardItem item = list.get(i);
                Log.d("PostAdapter", "Item " + i + ": id=" + item.getItemId() +
                        ", content=" + item.getContent() +
                        ", userId=" + item.getUserId() +
                        ", timestamp=" + item.getTimestamp() +
                        ", images=" + (item.getImageUrls() != null ? item.getImageUrls().size() : 0));
            }
        }
        this.postCardItemList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FragmentHomePostCardItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("PostAdapter", "onCreateViewHolder called");
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FragmentHomePostCardItemAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (position < 0 || position >= postCardItemList.size()) {
            Log.e("PostAdapter", "Invalid position: " + position + ", list size: " + postCardItemList.size());
            return;
        }

        PostCardItem item = postCardItemList.get(position);
        Log.d("PostAdapter", "onBindViewHolder position: " + position +
                ", postId: " + item.getItemId() +
                ", content: " + item.getContent() +
                ", timestamp: " + item.getTimestamp());

        // Content
        if (item.getContent() != null && !item.getContent().isEmpty()) {
            holder.tvContent.setText(item.getContent());
            Log.d("PostAdapter", "Content set: " + item.getContent());
        } else {
            holder.tvContent.setText("No content");
            Log.w("PostAdapter", "Content is null or empty");
        }

        // Time
        if (item.getTimestamp() != null && !item.getTimestamp().isEmpty()) {
            String timeAgo = TimeUtils.formatTimeAgo(item.getTimestamp());
            holder.tvDuration.setText(timeAgo);
            Log.d("PostAdapter", "Timestamp set: " + timeAgo + " (original: " + item.getTimestamp() + ")");
        } else {
            holder.tvDuration.setText("មិនទាន់មាន");
            Log.w("PostAdapter", "Timestamp is null or empty");
        }

        // Counts (prefer maps if present)
        long likeCount = item.getLikedBy() != null ? item.getLikedBy().size() : item.getLikeCount();
        long saveCount = item.getSavedBy() != null ? item.getSavedBy().size() : item.getSaveCount();
        long commentCount = item.getComments() != null ? item.getComments().size() : item.getCommentCount();

        holder.tvLike.setText(String.valueOf(likeCount));
        holder.tvComment.setText(String.valueOf(commentCount));
        holder.tvSave.setText(String.valueOf(saveCount));
        Log.d("PostAdapter", "Counts set - likes: " + likeCount +
                ", comments: " + commentCount +
                ", saves: " + saveCount);

        // User info
        if (item.getUserId() != null && !item.getUserId().isEmpty()) {
            Log.d("PostAdapter", "Loading user info for userId: " + item.getUserId());
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(item.getUserId());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d("PostAdapter", "User data snapshot exists: " + snapshot.exists());
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            Log.d("PostAdapter", "User loaded: " + user.getFirst_name() + " " + user.getLast_name());

                            String first = user.getFirst_name();
                            String last = user.getLast_name();
                            if (first != null && !first.isEmpty() && last != null && !last.isEmpty()) {
                                String fullName = first + " " + last;
                                holder.tvUsername.setText(fullName.trim());
                                Log.d("PostAdapter", "Username set: " + fullName.trim());
                            } else {
                                holder.tvUsername.setText("អ្នកប្រើប្រាស់");
                                Log.w("PostAdapter", "User name fields are null or empty");
                            }

                            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                Log.d("PostAdapter", "Loading profile image: " + user.getProfileImageUrl());
                                Glide.with(context)
                                        .load(user.getProfileImageUrl())
                                        .placeholder(R.drawable.img)
                                        .into(holder.btnProfile);
                            } else {
                                Log.w("PostAdapter", "Profile image URL is null or empty");
                                holder.btnProfile.setImageResource(R.drawable.img);
                            }
                        } else {
                            holder.tvUsername.setText("អ្នកប្រើប្រាស់");
                            Log.w("PostAdapter", "User object is null");
                        }
                    } else {
                        holder.tvUsername.setText("អ្នកប្រើប្រាស់");
                        Log.w("PostAdapter", "User snapshot does not exist for userId: " + item.getUserId());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.tvUsername.setText("អ្នកប្រើប្រាស់");
                    Log.e("PostAdapter", "User data load cancelled: " + error.getMessage());
                }
            });
        } else {
            holder.tvUsername.setText("អ្នកប្រើប្រាស់");
            Log.w("PostAdapter", "User ID is null or empty");
        }

        // Initial like/save UI based on current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;
        boolean isLiked = uid != null && item.isLikedByUser(uid);
        boolean isSaved = uid != null && item.isSavedByUser(uid);

        updateLikeUi(holder, isLiked);
        updateSaveUi(holder, isSaved);

        // Photos
        setupPhotoGrid(holder, item);

        // Click to open detail (only card + comment)
        View.OnClickListener openDetailListener = v -> {
            Fragment fragment = PostDetailFragment.newInstance(item.getItemId());
            if (context instanceof HomeActivity) {
                ((HomeActivity) context).LoadFragment(fragment);
                ((HomeActivity) context).hideBottomNavigation();
            }
        };

        holder.itemView.setOnClickListener(openDetailListener);
        // Optional: comment area opens detail
        if (holder.layoutCommentCard != null) {
            holder.layoutCommentCard.setOnClickListener(openDetailListener);
        }

        // Like & Save → toggle only (no open detail)
        if (holder.layoutLikeCard != null) {
            holder.layoutLikeCard.setOnClickListener(v -> handleToggleLike(item, holder));
        }
        if (holder.layoutSaveCard != null) {
            holder.layoutSaveCard.setOnClickListener(v -> handleToggleSave(item, holder));
        }

        Log.d("PostAdapter", "Finished binding item at position " + position);
    }

    private void setupPhotoGrid(ViewHolder holder, PostCardItem item) {
        List<String> images = item.getImageUrls();
        Log.d("PostAdapter", "Image URLs list: " + (images != null ? images.size() : 0) + " images");

        holder.singleImage.setVisibility(View.GONE);
        holder.twoImagesLayout.setVisibility(View.GONE);
        holder.threeImagesLayout.setVisibility(View.GONE);
        holder.fourImagesLayout.setVisibility(View.GONE);
        holder.imageOverlay.setVisibility(View.GONE);
        holder.tvMoreImages.setVisibility(View.GONE);

        if (images != null && !images.isEmpty()) {
            holder.photoGridContainer.setVisibility(View.VISIBLE);
            int imageCount = Math.min(images.size(), 4);
            Log.d("PostAdapter", "Setting up photo grid for " + imageCount + " images (total: " + images.size() + ")");

            switch (imageCount) {
                case 1:
                    setupSingleImage(holder, images);
                    break;
                case 2:
                    setupTwoImages(holder, images);
                    break;
                case 3:
                    setupThreeImages(holder, images);
                    break;
                case 4:
                    setupFourImages(holder, images, images.size());
                    break;
            }
        } else {
            Log.w("PostAdapter", "No images to display");
            holder.photoGridContainer.setVisibility(View.GONE);
        }
    }

    private void setupSingleImage(ViewHolder holder, List<String> images) {
        holder.singleImage.setVisibility(View.VISIBLE);
        Log.d("PostAdapter", "Setting up single image layout");

        Glide.with(context)
                .load(images.get(0))
                .placeholder(R.drawable.img)
                .into(holder.singleImage);
    }

    private void setupTwoImages(ViewHolder holder, List<String> images) {
        holder.twoImagesLayout.setVisibility(View.VISIBLE);
        Log.d("PostAdapter", "Setting up two images layout");

        Glide.with(context)
                .load(images.get(0))
                .placeholder(R.drawable.img)
                .into(holder.twoImage1);

        Glide.with(context)
                .load(images.get(1))
                .placeholder(R.drawable.img)
                .into(holder.twoImage2);
    }

    private void setupThreeImages(ViewHolder holder, List<String> images) {
        holder.threeImagesLayout.setVisibility(View.VISIBLE);
        Log.d("PostAdapter", "Setting up three images layout");

        Glide.with(context)
                .load(images.get(0))
                .placeholder(R.drawable.img)
                .into(holder.threeImage1);

        Glide.with(context)
                .load(images.get(1))
                .placeholder(R.drawable.img)
                .into(holder.threeImage2);

        Glide.with(context)
                .load(images.get(2))
                .placeholder(R.drawable.img)
                .into(holder.threeImage3);
    }

    private void setupFourImages(ViewHolder holder, List<String> images, int totalCount) {
        holder.fourImagesLayout.setVisibility(View.VISIBLE);
        Log.d("PostAdapter", "Setting up four images layout");

        Glide.with(context)
                .load(images.get(0))
                .placeholder(R.drawable.img)
                .into(holder.fourImage1);

        Glide.with(context)
                .load(images.get(1))
                .placeholder(R.drawable.img)
                .into(holder.fourImage2);

        Glide.with(context)
                .load(images.get(2))
                .placeholder(R.drawable.img)
                .into(holder.fourImage3);

        Glide.with(context)
                .load(images.get(3))
                .placeholder(R.drawable.img)
                .into(holder.fourImage4);

        if (totalCount > 4) {
            holder.imageOverlay.setVisibility(View.VISIBLE);
            holder.tvMoreImages.setVisibility(View.VISIBLE);
            holder.tvMoreImages.setText("+" + (totalCount - 4));
            Log.d("PostAdapter", "Showing overlay for " + (totalCount - 4) + " more images");
        }
    }

    @Override
    public int getItemCount() {
        int count = postCardItemList.size();
        Log.d("PostAdapter", "getItemCount: " + count);
        return count;
    }

    // ---- LIKE / SAVE HELPERS ----

    private void updateLikeUi(ViewHolder holder, boolean isLiked) {
        if (holder.ivLikeCard == null) return;
        int color = ContextCompat.getColor(context, isLiked ? R.color.primary : R.color.textColors);
        ImageViewCompat.setImageTintList(holder.ivLikeCard, ColorStateList.valueOf(color));
    }

    private void updateSaveUi(ViewHolder holder, boolean isSaved) {
        if (holder.ivSaveCard == null) return;
        int color = ContextCompat.getColor(context, isSaved ? R.color.primary : R.color.textColors);
        ImageViewCompat.setImageTintList(holder.ivSaveCard, ColorStateList.valueOf(color));
    }

    private void handleToggleLike(PostCardItem item, ViewHolder holder) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "សូមចូលប្រើប្រព័ន្ធជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        if (item.getItemId() == null || item.getItemId().isEmpty()) return;

        String uid = user.getUid();
        DatabaseReference likeRef = FirebaseDatabase.getInstance()
                .getReference("postCardItems")
                .child(item.getItemId())
                .child("likedBy")
                .child(uid);

        likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean currentlyLiked = snapshot.exists();

                if (currentlyLiked) {
                    likeRef.removeValue();
                    if (item.getLikedBy() != null) {
                        item.getLikedBy().remove(uid);
                    }
                } else {
                    likeRef.setValue(true);
                    if (item.getLikedBy() == null) {
                        item.setLikedBy(new java.util.HashMap<>());
                    }
                    item.getLikedBy().put(uid, true);
                }

                long likeCount = item.getLikedBy() != null ? item.getLikedBy().size() : 0;
                holder.tvLike.setText(String.valueOf(likeCount));
                updateLikeUi(holder, !currentlyLiked);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostAdapter", "toggleLike cancelled: " + error.getMessage());
            }
        });
    }

    private void handleToggleSave(PostCardItem item, ViewHolder holder) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "សូមចូលប្រើប្រព័ន្ធជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        if (item.getItemId() == null || item.getItemId().isEmpty()) return;

        String uid = user.getUid();
        DatabaseReference saveRef = FirebaseDatabase.getInstance()
                .getReference("postCardItems")
                .child(item.getItemId())
                .child("savedBy")
                .child(uid);

        saveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean currentlySaved = snapshot.exists();

                if (currentlySaved) {
                    saveRef.removeValue();
                    if (item.getSavedBy() != null) {
                        item.getSavedBy().remove(uid);
                    }
                } else {
                    saveRef.setValue(true);
                    if (item.getSavedBy() == null) {
                        item.setSavedBy(new java.util.HashMap<>());
                    }
                    item.getSavedBy().put(uid, true);
                }

                long saveCount = item.getSavedBy() != null ? item.getSavedBy().size() : 0;
                holder.tvSave.setText(String.valueOf(saveCount));
                updateSaveUi(holder, !currentlySaved);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostAdapter", "toggleSave cancelled: " + error.getMessage());
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView btnProfile;
        TextView tvUsername, tvContent, tvDuration, tvLike, tvComment, tvSave;

        ConstraintLayout photoGridContainer;

        ImageView singleImage;
        LinearLayout twoImagesLayout;
        ImageView twoImage1, twoImage2;
        ConstraintLayout threeImagesLayout;
        ImageView threeImage1, threeImage2, threeImage3;
        ViewGroup fourImagesLayout;
        ImageView fourImage1, fourImage2, fourImage3, fourImage4;
        View imageOverlay;
        TextView tvMoreImages;

        // New action layouts & icons
        LinearLayout layoutLikeCard, layoutCommentCard, layoutSaveCard;
        ImageView ivLikeCard, ivSaveCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d("PostAdapter", "ViewHolder created");

            btnProfile = itemView.findViewById(R.id.btnProfile);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvLike = itemView.findViewById(R.id.text_like_count);
            tvComment = itemView.findViewById(R.id.text_comment_count);
            tvSave = itemView.findViewById(R.id.text_save_count);

            photoGridContainer = itemView.findViewById(R.id.photoGridContainer);

            singleImage = itemView.findViewById(R.id.singleImage);

            twoImagesLayout = itemView.findViewById(R.id.twoImagesLayout);
            twoImage1 = itemView.findViewById(R.id.twoImage1);
            twoImage2 = itemView.findViewById(R.id.twoImage2);

            threeImagesLayout = itemView.findViewById(R.id.threeImagesLayout);
            threeImage1 = itemView.findViewById(R.id.threeImage1);
            threeImage2 = itemView.findViewById(R.id.threeImage2);
            threeImage3 = itemView.findViewById(R.id.threeImage3);

            fourImagesLayout = itemView.findViewById(R.id.fourImagesLayout);
            fourImage1 = itemView.findViewById(R.id.fourImage1);
            fourImage2 = itemView.findViewById(R.id.fourImage2);
            fourImage3 = itemView.findViewById(R.id.fourImage3);
            fourImage4 = itemView.findViewById(R.id.fourImage4);
            imageOverlay = itemView.findViewById(R.id.imageOverlay);
            tvMoreImages = itemView.findViewById(R.id.tvMoreImages);

            layoutLikeCard = itemView.findViewById(R.id.layoutLikeCard);
            layoutCommentCard = itemView.findViewById(R.id.layoutCommentCard);
            layoutSaveCard = itemView.findViewById(R.id.layoutSaveCard);
            ivLikeCard = itemView.findViewById(R.id.ivLikeCard);
            ivSaveCard = itemView.findViewById(R.id.ivSaveCard);
        }
    }
}
