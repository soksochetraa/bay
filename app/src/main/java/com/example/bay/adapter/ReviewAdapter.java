package com.example.bay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.model.Review;
import com.example.bay.model.User;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private List<Review> reviews;
    private Map<String, User> userMap;
    private boolean showAllReviews = false;

    public ReviewAdapter() {
        this.reviews = new ArrayList<>();
        this.userMap = new HashMap<>();
    }

    // Set whether to show all reviews or just 2
    public void setShowAllReviews(boolean showAll) {
        this.showAllReviews = showAll;
        notifyDataSetChanged();
    }

    public void updateReviews(List<Review> newReviews) {
        this.reviews = newReviews != null ? newReviews : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateUserData(Map<String, User> userMap) {
        this.userMap = userMap != null ? userMap : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_shop_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);
        if (review == null) return;

        // Get user from userMap
        User user = userMap.get(review.getUserId());

        // Set reviewer name
        String userName = "អ្នកប្រើប្រាស់";
        if (user != null) {
            String firstName = user.getFirst_name() != null ? user.getFirst_name() : "";
            String lastName = user.getLast_name() != null ? user.getLast_name() : "";
            String fullName = (firstName + " " + lastName).trim();
            if (!fullName.isEmpty()) {
                userName = fullName;
            }
        }
        holder.tvReviewerName.setText(userName);

        // Set review date
        if (review.getCreatedAt() != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("km"));
                String dateStr = sdf.format(new Date(review.getCreatedAt()));
                holder.tvReviewStatus.setText("បានបញ្ចេញមតិនៅ " + dateStr);
            } catch (Exception e) {
                holder.tvReviewStatus.setText("បានបញ្ចេញមតិ");
            }
        } else {
            holder.tvReviewStatus.setText("បានបញ្ចេញមតិ");
        }

        // Set rating stars
        updateStars(holder, review.getRating());

        // Set review text
        String comment = review.getComment();
        if (comment != null && !comment.trim().isEmpty()) {
            holder.tvReviewText.setText(comment);
            holder.tvReviewText.setVisibility(View.VISIBLE);
        } else {
            holder.tvReviewText.setVisibility(View.GONE);
        }

        // Load avatar
        if (user != null && user.getProfileImageUrl() != null &&
                !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.img_avatar)
                    .error(R.drawable.img_avatar)
                    .circleCrop()
                    .into(holder.ivReviewerAvatar);
        } else {
            holder.ivReviewerAvatar.setImageResource(R.drawable.img_avatar);
        }
    }

    private void updateStars(ViewHolder holder, float rating) {
        int fullStars = (int) rating;
        boolean hasHalfStar = rating - fullStars >= 0.5;

        holder.ivStar1.setImageResource(fullStars >= 1 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        holder.ivStar2.setImageResource(fullStars >= 2 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        holder.ivStar3.setImageResource(fullStars >= 3 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        holder.ivStar4.setImageResource(fullStars >= 4 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        holder.ivStar5.setImageResource(fullStars >= 5 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
    }

    @Override
    public int getItemCount() {
        if (showAllReviews) {
            return reviews.size(); // Show all reviews
        } else {
            return Math.min(reviews.size(), 2); // Show max 2 reviews for preview
        }
    }

    // Get total review count (for showing in dialog title)
    public int getTotalReviewCount() {
        return reviews.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivReviewerAvatar;
        TextView tvReviewerName, tvReviewStatus, tvReviewText;
        ImageView ivStar1, ivStar2, ivStar3, ivStar4, ivStar5;

        ViewHolder(View itemView) {
            super(itemView);
            ivReviewerAvatar = itemView.findViewById(R.id.ivReviewerAvatar);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvReviewStatus = itemView.findViewById(R.id.tvReviewStatus);
            tvReviewText = itemView.findViewById(R.id.tvReviewText);
            ivStar1 = itemView.findViewById(R.id.ivStar1);
            ivStar2 = itemView.findViewById(R.id.ivStar2);
            ivStar3 = itemView.findViewById(R.id.ivStar3);
            ivStar4 = itemView.findViewById(R.id.ivStar4);
            ivStar5 = itemView.findViewById(R.id.ivStar5);
        }
    }
}