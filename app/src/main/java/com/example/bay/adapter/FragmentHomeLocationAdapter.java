package com.example.bay.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.fragment.FarmMapFragment;
import com.example.bay.model.Location;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class FragmentHomeLocationAdapter extends RecyclerView.Adapter<FragmentHomeLocationAdapter.ViewHolder> {

    private final Context context;
    private List<Location> locations = new ArrayList<>();

    public FragmentHomeLocationAdapter(Context context) {
        this.context = context;
    }

    public void setLocations(List<Location> list) {
        this.locations = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_location_card_home, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Location location = locations.get(position);

        holder.tvLocationName.setText(location.name != null ? location.name : "កសិដ្ឋាន");
        holder.tvCategory.setText(location.category != null ? location.category : "កសិដ្ឋាន");

        if (location.contact != null && location.contact.phoneNumber != null && !location.contact.phoneNumber.isEmpty()) {
            holder.tvPhoneNumber.setText(location.contact.phoneNumber);
        } else {
            holder.tvPhoneNumber.setText("មិនមានលេខទូរស័ព្ទ");
        }

        if ("Farm".equalsIgnoreCase(location.category) || "កសិដ្ឋាន".equalsIgnoreCase(location.category)) {
            holder.imgCategoryIcon.setImageResource(R.drawable.ico_location);
        } else if ("Market".equalsIgnoreCase(location.category) || "ផ្សារ".equalsIgnoreCase(location.category)) {
            holder.imgCategoryIcon.setImageResource(R.drawable.ic_marketplace);
        } else {
            holder.imgCategoryIcon.setImageResource(R.drawable.ico_location);
        }

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof HomeActivity && location.id != null) {
                // Open the farm map fragment with location id
                FarmMapFragment fragment = new FarmMapFragment();
                Bundle args = new Bundle();
                args.putString("focused_location_id", location.id);
                fragment.setArguments(args);
                ((HomeActivity) context).LoadFragment(fragment);
            }
        });
        
        if (holder.btnDetails != null) {
            holder.btnDetails.setOnClickListener(v -> holder.itemView.performClick());
        }
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName, tvCategory, tvPhoneNumber;
        ImageView imgCategoryIcon;
        View btnDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tvLocationName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            imgCategoryIcon = itemView.findViewById(R.id.imgCategoryIcon);
            btnDetails = itemView.findViewById(R.id.btnDetails);
        }
    }
}
