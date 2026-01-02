package com.example.bay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.fragment.DetailItemShoppingFragment;
import com.example.bay.model.ShoppingItem;
import com.example.bay.model.User;

import java.util.List;
import java.util.Map;

public class ShoppingItemAdapter extends RecyclerView.Adapter<ShoppingItemAdapter.ViewHolder> {
    private List<ShoppingItem> shoppingItems;
    private Map<String, User> users;
    private OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(ShoppingItem item);
        void onSellerClick(String userId);
    }

    public ShoppingItemAdapter(Context context, List<ShoppingItem> shoppingItems,
                               Map<String, User> users, OnItemClickListener listener) {
        this.context = context;
        this.shoppingItems = shoppingItems;
        this.users = users;
        this.listener = listener;
    }

    public void updateData(List<ShoppingItem> newItems, Map<String, User> newUsers) {
        this.shoppingItems = newItems;
        this.users = newUsers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_shop_home, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = shoppingItems.get(position);

        // Load first image
        if (item.getImages() != null && !item.getImages().isEmpty()) {
            String imageUrl = item.getImages().get(0);
            if (imageUrl != null && (imageUrl.startsWith("https://") || imageUrl.startsWith("http://"))) {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.img)
                        .error(R.drawable.img)
                        .into(holder.ivShoppingItem);
            } else {
                holder.ivShoppingItem.setImageResource(R.drawable.img);
            }
        } else {
            holder.ivShoppingItem.setImageResource(R.drawable.img);
        }

        // Set category
        holder.tvCategoryChip.setText(item.getCategory() != null ? item.getCategory() : "");

        // Set item name
        holder.tvItemName.setText(item.getName() != null ? item.getName() : "");

        // Set unit
        holder.tvUnit.setText(item.getUnit() != null ? item.getUnit() : "");

        // Set price with Riel symbol
        holder.tvPrice.setText(formatPrice(item.getPrice()));

        // Set seller info
        User seller = users != null ? users.get(item.getUserId()) : null;
        if (seller != null) {
            String fullName = (seller.getFirst_name() != null ? seller.getFirst_name() : "") + " " +
                    (seller.getLast_name() != null ? seller.getLast_name() : "");
            holder.tvSellerName.setText(fullName.trim());

            if (seller.getProfileImageUrl() != null && !seller.getProfileImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(seller.getProfileImageUrl())
                        .placeholder(R.drawable.img)
                        .circleCrop()
                        .into(holder.ivSellerAvatar);
            } else {
                holder.ivSellerAvatar.setImageResource(R.drawable.img);
            }
        } else {
            holder.tvSellerName.setText("មិនស្គាល់");
            holder.ivSellerAvatar.setImageResource(R.drawable.img);
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
            // Navigate to detail fragment
            navigateToDetailFragment(item);
        });

        holder.layoutSeller.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSellerClick(item.getUserId());
            }
        });
    }

    private void navigateToDetailFragment(ShoppingItem item) {
        if (context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;

            // Create detail fragment with the ShoppingItem object
            DetailItemShoppingFragment fragment = DetailItemShoppingFragment.newInstance(item);

            // Replace current fragment with detail fragment
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack("marketplace")
                    .commit();
        }
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

    @Override
    public int getItemCount() {
        return shoppingItems != null ? shoppingItems.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivShoppingItem;
        TextView tvCategoryChip;
        TextView tvItemName;
        TextView tvUnit;
        TextView tvPrice;
        TextView tvSellerName;
        ImageView ivSellerAvatar;
        View layoutSeller;

        ViewHolder(View itemView) {
            super(itemView);
            ivShoppingItem = itemView.findViewById(R.id.ivShoppingItem);
            tvCategoryChip = itemView.findViewById(R.id.tvCategoryChip);
            tvItemName = itemView.findViewById(R.id.textView11);
            tvUnit = itemView.findViewById(R.id.textView13);
            tvPrice = itemView.findViewById(R.id.textView12);
            tvSellerName = itemView.findViewById(R.id.tvSellerName);
            ivSellerAvatar = itemView.findViewById(R.id.ivSellerAvatar);
            layoutSeller = itemView.findViewById(R.id.layoutSeller);
        }
    }
}