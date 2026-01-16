package com.example.bay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.model.ShoppingItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyPostsAdapter extends RecyclerView.Adapter<MyPostsAdapter.ViewHolder> {

    private Context context;
    private List<ShoppingItem> myPosts;
    private OnMyPostActionListener listener;

    public interface OnMyPostActionListener {
        void onEditClicked(ShoppingItem item);
        void onDeleteClicked(ShoppingItem item);
        void onMarkAsSoldClicked(ShoppingItem item);
        void onMoreOptionsClicked(ShoppingItem item, View anchorView);
    }

    public MyPostsAdapter(Context context, List<ShoppingItem> myPosts, OnMyPostActionListener listener) {
        this.context = context;
        this.myPosts = myPosts;
        this.listener = listener;
    }

    public void updateData(List<ShoppingItem> newPosts) {
        this.myPosts = newPosts;
        notifyDataSetChanged();
    }

    // ADD THIS METHOD: Get current items
    public List<ShoppingItem> getItems() {
        return myPosts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shopping_my_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = myPosts.get(position);

        // Load product image
        if (item.getImages() != null && !item.getImages().isEmpty()) {
            String imageUrl = item.getImages().get(0);
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .into(holder.ivProductImage);
        } else {
            holder.ivProductImage.setImageResource(R.drawable.img);
        }

        // Set product details
        holder.tvProductName.setText(item.getName() != null ? item.getName() : "");
        holder.tvCategory.setText(item.getCategory() != null ? item.getCategory() : "");
        holder.tvPrice.setText(formatPrice(item.getPrice()));

        // Set created date
        if (item.getCreatedAt() != null) {
            String date = formatDate(item.getCreatedAt());
            holder.tvCreatedDate.setText("បានបង្កើត: " + date);
        } else {
            holder.tvCreatedDate.setText("");
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClicked(item);
            }
        });

        // More options button click
        holder.btnMore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMoreOptionsClicked(item, holder.btnMore);
            }
        });

        // Long press for delete
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClicked(item);
                return true;
            }
            return false;
        });
    }

    private String formatPrice(String price) {
        if (price == null || price.isEmpty()) {
            return "0៛";
        }

        try {
            double priceValue = Double.parseDouble(price);
            if (priceValue == (long) priceValue) {
                return String.format("%,d", (long) priceValue) + "៛";
            } else {
                return String.format("%,.2f", priceValue) + "៛";
            }
        } catch (NumberFormatException e) {
            return price + "៛";
        }
    }

    private String formatDate(Long timestamp) {
        if (timestamp == null) return "";

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("km"));
        return sdf.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return myPosts != null ? myPosts.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName;
        TextView tvCategory;
        TextView tvPrice;
        TextView tvCreatedDate;
        ImageButton btnMore;

        ViewHolder(View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}