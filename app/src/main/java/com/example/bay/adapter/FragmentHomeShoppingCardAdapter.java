package com.example.bay.adapter;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.LayoutInflater;
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

public class FragmentHomeShoppingCardAdapter
        extends RecyclerView.Adapter<FragmentHomeShoppingCardAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(ShoppingItem item);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    private final List<ShoppingItem> items = new ArrayList<>();
    private final Map<String, User> userCache = new HashMap<>();
    private final Map<String, Boolean> userLoading = new HashMap<>();

    // ======================================================

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemCardShopHomeBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        ));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ShoppingItem item = items.get(position);

        // ---------- PRODUCT ----------
        h.binding.textView11.setText(safe(item.getName(), "No name"));
        h.binding.textView13.setText(safe(item.getUnit(), ""));
        h.binding.textView12.setText(formatPrice(item.getPrice()));
        h.binding.tvCategoryChip.setText(toCategoryLabel(item.getCategory()));

        // ---------- PRODUCT IMAGE ----------
        String img = (item.getImages() != null && !item.getImages().isEmpty())
                ? item.getImages().get(0)
                : null;

        Glide.with(h.itemView.getContext())
                .load(img != null ? img : R.drawable.img)
                .centerCrop()
                .placeholder(R.drawable.img)
                .into(h.binding.ivShoppingItem);

        // ---------- SELLER ----------
        String userId = item.getUserId();
        User user = userCache.get(userId);

        if (user != null) {
            bindUser(h, user);
        } else {
            // default
            h.binding.tvSellerName.setText("Seller");
            Glide.with(h.itemView.getContext())
                    .load(R.drawable.img)
                    .into(h.binding.ivSellerAvatar);

            fetchUserIfNeeded(userId);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(items.size(), 5);
    }

    // ======================================================

    @SuppressLint("NotifyDataSetChanged")
    public void setShoppingItems(List<ShoppingItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list.subList(0, Math.min(5, list.size())));
        }
        notifyDataSetChanged();
    }

    // ======================================================
    // ================= FIREBASE ===========================
    // ======================================================

    private void fetchUserIfNeeded(String userId) {
        if (TextUtils.isEmpty(userId)) return;

        Boolean loading = userLoading.get(userId);
        if (loading != null && loading) return;

        userLoading.put(userId, true);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userLoading.put(userId, false);

                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            userCache.put(userId, user);
                            notifyDataSetChanged(); // ✅ SAFE
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        userLoading.put(userId, false);
                    }
                });
    }

    // ======================================================

    private void bindUser(VH h, User u) {
        String name = buildFullName(u.getFirst_name(), u.getLast_name());
        h.binding.tvSellerName.setText(
                !TextUtils.isEmpty(name) ? name : "Seller"
        );

        Glide.with(h.itemView.getContext())
                .load(!TextUtils.isEmpty(u.getProfileImageUrl())
                        ? u.getProfileImageUrl()
                        : R.drawable.img)
                .placeholder(R.drawable.img)
                .into(h.binding.ivSellerAvatar);
    }

    // ======================================================

    static class VH extends RecyclerView.ViewHolder {
        ItemCardShopHomeBinding binding;

        VH(ItemCardShopHomeBinding b) {
            super(b.getRoot());
            binding = b;
        }
    }

    // ================= HELPERS ============================

    private static String safe(String v, String f) {
        return TextUtils.isEmpty(v) ? f : v;
    }

    private static String formatPrice(String raw) {
        if (TextUtils.isEmpty(raw)) return "-";
        try {
            long v = Long.parseLong(raw.replaceAll("[^0-9]", ""));
            return NumberFormat.getInstance(Locale.US).format(v) + "៛";
        } catch (Exception e) {
            return raw;
        }
    }

    private static String buildFullName(String f, String l) {
        f = f != null ? f.trim() : "";
        l = l != null ? l.trim() : "";
        if (!f.isEmpty() && !l.isEmpty()) return f + " " + l;
        return !f.isEmpty() ? f : l;
    }

    private String toCategoryLabel(String cat) {
        if (cat == null) return "ផ្សេងៗ";
        String c = cat.trim().toLowerCase(Locale.ENGLISH);

        if (c.contains("បន្លែ")) return "បន្លែ";
        if (c.contains("ផ្លែឈើ")) return "ផ្លែឈើ";
        if (c.contains("សម្ភារៈ")) return "សម្ភារៈ";
        if (c.contains("គ្រាប់ពូជ")) return "គ្រាប់ពូជ";
        if (c.contains("ជី")) return "ជី";
        if (c.contains("ថ្នាំ")) return "ថ្នាំ";

        if (c.equals("vegetables")) return "បន្លែ";
        if (c.equals("fruits")) return "ផ្លែឈើ";
        if (c.equals("tools")) return "សម្ភារៈ";

        return "ផ្សេងៗ";
    }
}
