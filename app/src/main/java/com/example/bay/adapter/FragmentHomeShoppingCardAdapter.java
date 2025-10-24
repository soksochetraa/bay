package com.example.bay.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.databinding.ItemCardShopHomeBinding;
import com.example.bay.model.ShoppingItem;

import java.util.ArrayList;
import java.util.List;

public class FragmentHomeShoppingCardAdapter extends RecyclerView.Adapter<FragmentHomeShoppingCardAdapter.ShoppingItemViewHolder> {

    private final List<ShoppingItem> shoppingItems = new ArrayList<>();

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
        holder.bind(shoppingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return shoppingItems.size();
    }

    public void setShoppingItems(List<ShoppingItem> items) {
        shoppingItems.clear();
        if (items != null) shoppingItems.addAll(items);
        notifyDataSetChanged();
    }

    public static class ShoppingItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemCardShopHomeBinding binding;

        public ShoppingItemViewHolder(@NonNull ItemCardShopHomeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ShoppingItem shoppingItem) {
            if (shoppingItem.getImageUrl() != null && !shoppingItem.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(shoppingItem.getImageUrl())
                        .centerCrop()
                        .into(binding.ivShoppingItem);
            }
            binding.textView11.setText(shoppingItem.getName());
            binding.textView12.setText(shoppingItem.getPrice());
            binding.textView13.setText(shoppingItem.getUnit());
        }
    }
}
