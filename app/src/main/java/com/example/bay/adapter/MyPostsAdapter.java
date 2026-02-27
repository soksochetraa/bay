package com.example.bay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

        // image
        if (item.getImages() != null && !item.getImages().isEmpty()) {
            Glide.with(context)
                    .load(item.getImages().get(0))
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .into(holder.ivProductImage);
        } else {
            holder.ivProductImage.setImageResource(R.drawable.img);
        }

        holder.tvProductName.setText(item.getName() != null ? item.getName() : "");
        holder.tvCategory.setText(item.getCategory() != null ? item.getCategory() : "");
        holder.tvPrice.setText(formatPrice(item.getPrice()));

        if (item.getCreatedAt() != null) {
            holder.tvCreatedDate.setText("បានបង្កើត: " + formatDate(item.getCreatedAt()));
        } else {
            holder.tvCreatedDate.setText("");
        }

        // ✅ WARNING UI
        if (item.isWarned()) {
            holder.layoutWarningBox.setVisibility(View.VISIBLE);
            holder.tvWarningMessage.setText(item.getWarningMessageSafe());

            long expiresAt = item.getExpiresAtSafe();
            long now = System.currentTimeMillis();
            long diff = Math.max(0, expiresAt - now);

            long days = diff / (1000L * 60L * 60L * 24L);
            long hours = (diff / (1000L * 60L * 60L)) % 24L;

            holder.tvWarningCountdown.setText("ផលិតផលនឺងត្រូវបានលុបចេញនៅក្នុងរយះពេល " + days + " ថ្ងៃ " + hours + " ម៉ោងទៀត");
        } else {
            holder.layoutWarningBox.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEditClicked(item);
        });

        holder.btnMore.setOnClickListener(v -> {
            if (listener != null) listener.onMoreOptionsClicked(item, holder.btnMore);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClicked(item);
                return true;
            }
            return false;
        });
    }

    private String formatPrice(String price) {
        if (price == null || price.isEmpty()) return "0៛";
        try {
            double priceValue = Double.parseDouble(price);
            if (priceValue == (long) priceValue) return String.format("%,d", (long) priceValue) + "៛";
            return String.format("%,.2f", priceValue) + "៛";
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
        TextView tvProductName, tvCategory, tvPrice, tvCreatedDate;
        ImageButton btnMore;

        // ✅ warning views
        LinearLayout layoutWarningBox;
        TextView tvWarningMessage;
        TextView tvWarningCountdown;

        ViewHolder(View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            btnMore = itemView.findViewById(R.id.btnMore);

            layoutWarningBox = itemView.findViewById(R.id.layoutWarningBox);
            tvWarningMessage = itemView.findViewById(R.id.tvWarningMessage);
            tvWarningCountdown = itemView.findViewById(R.id.tvWarningCountdown);
        }
    }
}