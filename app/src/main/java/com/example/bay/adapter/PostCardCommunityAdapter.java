package com.example.bay.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PostCardCommunityAdapter extends RecyclerView.Adapter<PostCardCommunityAdapter.ViewHolder> {

    private static final String TAG = "PostCommunityAdapter";

    private final Context context;
    private List<PostCardItem> postCardItemList = new ArrayList<>();
    private final MutableLiveData<List<PostCardItem>> postCardItemsLiveData = new MutableLiveData<>();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public PostCardCommunityAdapter(Context context) {
        this.context = context;
    }

    public LiveData<List<PostCardItem>> getPostCardItemsLiveData() {
        return postCardItemsLiveData;
    }

    public void setPostCardItemList(List<PostCardItem> list) {
        List<PostCardItem> newList;
        if (list == null) {
            newList = new ArrayList<>();
        } else {
            newList = new ArrayList<>(list);
        }

        Collections.sort(newList, (p1, p2) -> {
            long t1 = parseTimestamp(p1 != null ? p1.getTimestamp() : null);
            long t2 = parseTimestamp(p2 != null ? p2.getTimestamp() : null);

            if (t1 != t2) {
                return Long.compare(t2, t1);
            }

            long score1 = popularity(p1);
            long score2 = popularity(p2);

            return Long.compare(score2, score1);
        });

        this.postCardItemList = newList;
        postCardItemsLiveData.setValue(newList);
        notifyDataSetChanged();
    }

    private long popularity(PostCardItem p) {
        if (p == null) return 0L;

        long likes = p.getLikedBy() != null ? p.getLikedBy().size() : p.getLikeCount();
        long saves = p.getSavedBy() != null ? p.getSavedBy().size() : p.getSaveCount();
        long comments = p.getComments() != null ? p.getComments().size() : p.getCommentCount();

        return likes + saves + comments;
    }

    private long parseTimestamp(String ts) {
        if (ts == null) return 0L;

        if (ts.matches("\\d+")) {
            try {
                return Long.parseLong(ts);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
            return sdf.parse(ts).getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    @NonNull
    @Override
    public PostCardCommunityAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostCardCommunityAdapter.ViewHolder holder,
                                 @SuppressLint("RecyclerView") int position) {

        if (position < 0 || position >= postCardItemList.size()) return;

        PostCardItem item = postCardItemList.get(position);

        // Content
        if (item.getContent() != null && !item.getContent().isEmpty()) {
            holder.tvContent.setText(item.getContent());
        } else {
            holder.tvContent.setText("No content");
        }

        // Time
        if (item.getTimestamp() != null && !item.getTimestamp().isEmpty()) {
            String timeAgo = TimeUtils.formatTimeAgo(item.getTimestamp());
            holder.tvDuration.setText(timeAgo);
        } else {
            holder.tvDuration.setText("មិនទាន់មាន");
        }

        // Counts (prefer maps)
        long likeCount = item.getLikedBy() != null ? item.getLikedBy().size() : item.getLikeCount();
        long saveCount = item.getSavedBy() != null ? item.getSavedBy().size() : item.getSaveCount();
        long commentCount = item.getComments() != null ? item.getComments().size() : item.getCommentCount();

        holder.tvLike.setText(String.valueOf(likeCount));
        holder.tvComment.setText(String.valueOf(commentCount));
        holder.tvSave.setText(String.valueOf(saveCount));

        // User info
        if (item.getUserId() != null && !item.getUserId().isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(item.getUserId());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        holder.tvUsername.setText("អ្នកប្រើប្រាស់");
                        return;
                    }

                    User user = snapshot.getValue(User.class);
                    if (user == null) {
                        holder.tvUsername.setText("អ្នកប្រើប្រាស់");
                        return;
                    }

                    String firstName = user.getFirst_name();
                    String lastName = user.getLast_name();
                    if (firstName != null && !firstName.isEmpty()
                            && lastName != null && !lastName.isEmpty()) {
                        String fullName = firstName + " " + lastName;
                        holder.tvUsername.setText(fullName.trim());
                    } else {
                        holder.tvUsername.setText("អ្នកប្រើប្រាស់");
                    }

                    if (user.getProfileImageUrl() != null
                            && !user.getProfileImageUrl().isEmpty()) {
                        Glide.with(context)
                                .load(user.getProfileImageUrl())
                                .placeholder(R.drawable.img)
                                .into(holder.btnProfile);
                    } else {
                        holder.btnProfile.setImageResource(R.drawable.img);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.tvUsername.setText("អ្នកប្រើប្រាស់");
                }
            });
        } else {
            holder.tvUsername.setText("អ្នកប្រើប្រាស់");
        }

        // Initial like/save UI based on user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;
        boolean isLiked = uid != null && item.isLikedByUser(uid);
        boolean isSaved = uid != null && item.isSavedByUser(uid);

        updateLikeUi(holder, isLiked);
        updateSaveUi(holder, isSaved);

        // Photos
        setupPhotoGrid(holder, item);

        // --- Click behaviors ---

        View.OnClickListener openDetailListener = v -> {
            Fragment fragment = PostDetailFragment.newInstance(item.getItemId());
            if (context instanceof HomeActivity) {
                ((HomeActivity) context).LoadFragment(fragment);
                ((HomeActivity) context).hideBottomNavigation();
            }
        };

        // Only card + comment open detail
        holder.itemView.setOnClickListener(openDetailListener);
        if (holder.layoutCommentCard != null) {
            holder.layoutCommentCard.setOnClickListener(openDetailListener);
        }

        // Like & Save toggle only
        if (holder.layoutLikeCard != null) {
            holder.layoutLikeCard.setOnClickListener(v -> handleToggleLike(item, holder));
        }
        if (holder.layoutSaveCard != null) {
            holder.layoutSaveCard.setOnClickListener(v -> handleToggleSave(item, holder));
        }
    }

    private void setupPhotoGrid(ViewHolder holder, PostCardItem item) {
        List<String> images = item.getImageUrls();

        holder.singleImage.setVisibility(View.GONE);
        holder.twoImagesLayout.setVisibility(View.GONE);
        holder.threeImagesLayout.setVisibility(View.GONE);
        holder.fourImagesLayout.setVisibility(View.GONE);
        holder.imageOverlay.setVisibility(View.GONE);
        holder.tvMoreImages.setVisibility(View.GONE);

        if (images != null && !images.isEmpty()) {
            holder.photoGridContainer.setVisibility(View.VISIBLE);
            int imageCount = Math.min(images.size(), 4);

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
            holder.photoGridContainer.setVisibility(View.GONE);
        }
    }

    private void setupSingleImage(ViewHolder holder, List<String> images) {
        holder.singleImage.setVisibility(View.VISIBLE);

        Glide.with(context)
                .load(images.get(0))
                .placeholder(R.drawable.img)
                .into(holder.singleImage);
    }

    private void setupTwoImages(ViewHolder holder, List<String> images) {
        holder.twoImagesLayout.setVisibility(View.VISIBLE);

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
        }
    }

    @Override
    public int getItemCount() {
        return postCardItemList.size();
    }

    // ---- LIKE / SAVE HELPER METHODS ----

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
                // ignored
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
                // ignored
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

        // New: action layouts & icons
        LinearLayout layoutLikeCard, layoutCommentCard, layoutSaveCard;
        ImageView ivLikeCard, ivSaveCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

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
