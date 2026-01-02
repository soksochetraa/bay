package com.example.bay.adapter;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.databinding.ItemCardShopHomeBinding;
import com.example.bay.model.ShoppingItem;
import com.example.bay.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FragmentHomeShoppingCardAdapter extends RecyclerView.Adapter<FragmentHomeShoppingCardAdapter.ShoppingItemViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ShoppingItem item);
    }

    private final List<ShoppingItem> shoppingItems = new ArrayList<>();
    private final Map<String, User> userCache = new HashMap<>();
    private final Map<String, Boolean> userLoading = new HashMap<>();

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShoppingItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCardShopHomeBinding binding = ItemCardShopHomeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ShoppingItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ShoppingItemViewHolder holder, int position) {
        if (position < shoppingItems.size()) {
            holder.bind(shoppingItems.get(position));
        }
    }

    @Override
    public int getItemCount() {
        // Limit to maximum 5 items for home screen
        return Math.min(shoppingItems.size(), 5);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setShoppingItems(List<ShoppingItem> items) {
        shoppingItems.clear();
        if (items != null) {
            // Take only first 5 items for home screen
            int count = Math.min(items.size(), 5);
            for (int i = 0; i < count; i++) {
                shoppingItems.add(items.get(i));
            }
        }
        notifyDataSetChanged();
    }

    public class ShoppingItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemCardShopHomeBinding binding;

        public ShoppingItemViewHolder(@NonNull ItemCardShopHomeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ShoppingItem item) {
            if (item == null) return;

            // ---------- Product ----------
            String name = safe(item.getName(), "No name");
            String unit = safe(item.getUnit(), "");
            String priceRaw = safe(item.getPrice(), "");
            String category = safe(item.getCategory(), "others");

            binding.textView11.setText(name);
            binding.textView13.setText(unit);
            binding.textView12.setText(formatPrice(priceRaw));
            binding.tvCategoryChip.setText(toCategoryLabel(category));

            // ---------- Load Product Image ----------
            List<String> images = item.getImages();
            if (images != null && !images.isEmpty()) {
                String firstImageUrl = images.get(0);
                Glide.with(itemView.getContext())
                        .load(firstImageUrl)
                        .placeholder(R.drawable.img)
                        .error(R.drawable.img)
                        .centerCrop()
                        .into(binding.ivShoppingItem);
            } else {
                // Load default image if no images available
                Glide.with(itemView.getContext())
                        .load(R.drawable.img)
                        .centerCrop()
                        .into(binding.ivShoppingItem);
            }

            // ---------- Seller default (prevents wrong data because of recycling) ----------
            binding.tvSellerName.setText("Seller");
            Glide.with(itemView.getContext())
                    .load(R.drawable.img)
                    .centerCrop()
                    .into(binding.ivSellerAvatar);

            // ---------- Load seller from userId ----------
            String userId = item.getUserId();
            if (!TextUtils.isEmpty(userId)) {
                User cached = userCache.get(userId);
                if (cached != null) {
                    applyUserToUI(cached);
                } else {
                    Boolean isLoading = userLoading.get(userId);
                    if (isLoading == null || !isLoading) {
                        userLoading.put(userId, true);
                        fetchUserOnce(userId);
                    }
                }
            }

            // ---------- Make the whole card clickable ----------
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }

        private void applyUserToUI(User user) {
            String fullName = buildFullName(user.getFirst_name(), user.getLast_name());
            if (TextUtils.isEmpty(fullName)) fullName = "Seller";

            binding.tvSellerName.setText(fullName);

            String avatar = user.getProfileImageUrl();
            Glide.with(itemView.getContext())
                    .load(!TextUtils.isEmpty(avatar) ? avatar : R.drawable.img)
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .centerCrop()
                    .into(binding.ivSellerAvatar);
        }

        private void fetchUserOnce(String userId) {
            // IMPORTANT: change "Users" if your node name is different
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            userLoading.put(userId, false);

                            User u = snapshot.getValue(User.class);
                            if (u != null) {
                                userCache.put(userId, u);

                                int pos = getAdapterPosition();
                                if (pos != RecyclerView.NO_POSITION) {
                                    notifyItemChanged(pos);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            userLoading.put(userId, false);
                        }
                    });
        }

        private String safe(String v, String fallback) {
            return TextUtils.isEmpty(v) ? fallback : v;
        }

        private String formatPrice(String priceRaw) {
            if (TextUtils.isEmpty(priceRaw)) return "-";
            try {
                String digitsOnly = priceRaw.replaceAll("[^0-9]", "");
                if (!TextUtils.isEmpty(digitsOnly)) {
                    long value = Long.parseLong(digitsOnly);
                    return NumberFormat.getInstance(Locale.US).format(value) + "៛";
                }
            } catch (Exception ignored) {}
            return priceRaw;
        }

        private String buildFullName(String first, String last) {
            first = first != null ? first.trim() : "";
            last = last != null ? last.trim() : "";
            if (!first.isEmpty() && !last.isEmpty()) return first + " " + last;
            if (!first.isEmpty()) return first;
            return last;
        }

        private String toCategoryLabel(String cat) {
            if (cat == null) return "ផ្សេងៗ";
            String c = cat.trim().toLowerCase(Locale.ENGLISH);

            // Handle Khmer categories
            if (c.contains("បន្លែ")) return "បន្លែ";
            if (c.contains("ផ្លែឈើ")) return "ផ្លែឈើ";
            if (c.contains("សម្ភារៈ")) return "សម្ភារៈ";
            if (c.contains("គ្រាប់ពូជ")) return "គ្រាប់ពូជ";
            if (c.contains("ជី")) return "ជី";
            if (c.contains("ថ្នាំ")) return "ថ្នាំ";
            if (c.contains("សម្ភារៈវេជ្ជសាស្រ្ត") || c.contains("សម្ភារៈវេជ្ជសាស្ត្រ")) return "សម្ភារៈវេជ្ជសាស្រ្ត";
            if (c.contains("ផ្សេងៗ")) return "ផ្សេងៗ";

            // Handle English categories
            if (c.equals("vegetables") || c.equals("vegetable")) return "បន្លែ";
            if (c.equals("fruits") || c.equals("fruit")) return "ផ្លែឈើ";
            if (c.equals("tools") || c.equals("tool") || c.equals("supplies")) return "សម្ភារៈ";
            if (c.equals("seeds")) return "គ្រាប់ពូជ";
            if (c.equals("fertilizer")) return "ជី";
            if (c.equals("pesticide")) return "ថ្នាំ";
            if (c.equals("medical supplies") || c.equals("medical")) return "សម្ភារៈវេជ្ជសាស្រ្ត";

            return "ផ្សេងៗ";
        }
    }
}