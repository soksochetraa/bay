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
    }

    public void setPostCardItemList(List<PostCardItem> list) {
        this.postCardItemList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        PostCardItem item = postCardItemList.get(position);

        holder.tvContent.setText(item.getContent() != null && !item.getContent().isEmpty() ? item.getContent() : "No content");
        holder.tvDuration.setText(item.getTimestamp() != null && !item.getTimestamp().isEmpty()
                ? TimeUtils.formatTimeAgo(item.getTimestamp())
                : "មិនទាន់មាន");

        long likeCount = item.getLikedBy() != null ? item.getLikedBy().size() : 0;
        long saveCount = item.getSavedBy() != null ? item.getSavedBy().size() : 0;
        long commentCount = item.getComments() != null ? item.getComments().size() : 0;

        holder.tvLike.setText(String.valueOf(likeCount));
        holder.tvSave.setText(String.valueOf(saveCount));
        holder.tvComment.setText(String.valueOf(commentCount));

        if (item.getUserId() != null && !item.getUserId().isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(item.getUserId());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        String name = (user.getFirst_name() != null ? user.getFirst_name() : "") +
                                " " +
                                (user.getLast_name() != null ? user.getLast_name() : "");
                        holder.tvUsername.setText(name.trim().isEmpty() ? "អ្នកប្រើប្រាស់" : name.trim());

                        Glide.with(context)
                                .load(user.getProfileImageUrl())
                                .placeholder(R.drawable.img)
                                .into(holder.btnProfile);
                    } else {
                        holder.tvUsername.setText("អ្នកប្រើប្រាស់");
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

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;

        updateLikeUi(holder, uid != null && item.isLikedByUser(uid));
        updateSaveUi(holder, uid != null && item.isSavedByUser(uid));

        setupPhotoGrid(holder, item);

        View.OnClickListener openDetail = v -> {
            Fragment fragment = PostDetailFragment.newInstance(item.getItemId());
            if (context instanceof HomeActivity) {
                ((HomeActivity) context).LoadFragment(fragment);
                ((HomeActivity) context).hideBottomNavigation();
            }
        };

        holder.itemView.setOnClickListener(openDetail);
        if (holder.layoutCommentCard != null) holder.layoutCommentCard.setOnClickListener(openDetail);
        if (holder.layoutLikeCard != null) holder.layoutLikeCard.setOnClickListener(v -> handleToggleLike(item, holder));
        if (holder.layoutSaveCard != null) holder.layoutSaveCard.setOnClickListener(v -> handleToggleSave(item, holder));

        holder.btnProfile.setOnClickListener(openDetail);
    }

    private void setupPhotoGrid(ViewHolder holder, PostCardItem item) {
        List<String> images = item.getImageUrls();

        holder.photoGridContainer.setVisibility(images != null && !images.isEmpty() ? View.VISIBLE : View.GONE);
        holder.singleImage.setVisibility(View.GONE);
        holder.twoImagesLayout.setVisibility(View.GONE);
        holder.threeImagesLayout.setVisibility(View.GONE);
        holder.fourImagesLayout.setVisibility(View.GONE);
        holder.imageOverlay.setVisibility(View.GONE);
        holder.tvMoreImages.setVisibility(View.GONE);

        if (images == null || images.isEmpty()) return;

        int count = Math.min(images.size(), 4);

        if (count == 1) {
            holder.singleImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(images.get(0)).placeholder(R.drawable.img).into(holder.singleImage);
        } else if (count == 2) {
            holder.twoImagesLayout.setVisibility(View.VISIBLE);
            Glide.with(context).load(images.get(0)).placeholder(R.drawable.img).into(holder.twoImage1);
            Glide.with(context).load(images.get(1)).placeholder(R.drawable.img).into(holder.twoImage2);
        } else if (count == 3) {
            holder.threeImagesLayout.setVisibility(View.VISIBLE);
            Glide.with(context).load(images.get(0)).placeholder(R.drawable.img).into(holder.threeImage1);
            Glide.with(context).load(images.get(1)).placeholder(R.drawable.img).into(holder.threeImage2);
            Glide.with(context).load(images.get(2)).placeholder(R.drawable.img).into(holder.threeImage3);
        } else {
            holder.fourImagesLayout.setVisibility(View.VISIBLE);
            Glide.with(context).load(images.get(0)).placeholder(R.drawable.img).into(holder.fourImage1);
            Glide.with(context).load(images.get(1)).placeholder(R.drawable.img).into(holder.fourImage2);
            Glide.with(context).load(images.get(2)).placeholder(R.drawable.img).into(holder.fourImage3);
            Glide.with(context).load(images.get(3)).placeholder(R.drawable.img).into(holder.fourImage4);

            if (images.size() > 4) {
                holder.imageOverlay.setVisibility(View.VISIBLE);
                holder.tvMoreImages.setVisibility(View.VISIBLE);
                holder.tvMoreImages.setText("+" + (images.size() - 4));
            }
        }
    }

    @Override
    public int getItemCount() {
        return postCardItemList.size();
    }

    private void updateLikeUi(ViewHolder holder, boolean liked) {
        int color = ContextCompat.getColor(context, liked ? R.color.primary : R.color.textColors);
        ImageViewCompat.setImageTintList(holder.ivLikeCard, ColorStateList.valueOf(color));
    }

    private void updateSaveUi(ViewHolder holder, boolean saved) {
        int color = ContextCompat.getColor(context, saved ? R.color.primary : R.color.textColors);
        ImageViewCompat.setImageTintList(holder.ivSaveCard, ColorStateList.valueOf(color));
    }

    private void handleToggleLike(PostCardItem item, ViewHolder holder) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "សូមចូលប្រើប្រព័ន្ធជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("postCardItems")
                .child(item.getItemId())
                .child("likedBy")
                .child(uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean liked = snapshot.exists();
                if (liked) {
                    ref.removeValue();
                    if (item.getLikedBy() != null) item.getLikedBy().remove(uid);
                } else {
                    ref.setValue(true);
                    if (item.getLikedBy() == null) item.setLikedBy(new java.util.HashMap<>());
                    item.getLikedBy().put(uid, true);
                }
                holder.tvLike.setText(String.valueOf(item.getLikedBy() != null ? item.getLikedBy().size() : 0));
                updateLikeUi(holder, !liked);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void handleToggleSave(PostCardItem item, ViewHolder holder) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "សូមចូលប្រើប្រព័ន្ធជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("postCardItems")
                .child(item.getItemId())
                .child("savedBy")
                .child(uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean saved = snapshot.exists();
                if (saved) {
                    ref.removeValue();
                    if (item.getSavedBy() != null) item.getSavedBy().remove(uid);
                } else {
                    ref.setValue(true);
                    if (item.getSavedBy() == null) item.setSavedBy(new java.util.HashMap<>());
                    item.getSavedBy().put(uid, true);
                }
                holder.tvSave.setText(String.valueOf(item.getSavedBy() != null ? item.getSavedBy().size() : 0));
                updateSaveUi(holder, !saved);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
