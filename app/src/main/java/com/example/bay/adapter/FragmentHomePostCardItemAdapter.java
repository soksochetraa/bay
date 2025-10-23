package com.example.bay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.model.PostCardItem;
import com.example.bay.model.User;
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

    public FragmentHomePostCardItemAdapter(Context context) {
        this.context = context;
    }

    public void setPostCardItemList(List<PostCardItem> list) {
        this.postCardItemList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FragmentHomePostCardItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card_knowledge_home, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FragmentHomePostCardItemAdapter.ViewHolder holder, int position) {
        PostCardItem item = postCardItemList.get(position);
        holder.tvContent.setText(item.getContent() != null ? item.getContent() : "");
        holder.tvLike.setText(String.valueOf(item.getLikeCount()));
        holder.tvComment.setText(String.valueOf(item.getCommentCount()));
        holder.tvSave.setText(String.valueOf(item.getSaveCount()));
        if (item.getUserId() != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(item.getUserId());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.getName() != null && !user.getName().isEmpty()) {
                        holder.tvUsername.setText(user.getName());
                        Glide.with(context).load(user.getProfileImageUrl()).placeholder(R.drawable.img).into(holder.btnProfile);
                    } else {
                        holder.tvUsername.setText("អ្នកប្រើប្រាស់");
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
        List<String> images = item.getImageUrls();
        ImageView[] imageViews = {holder.image1, holder.image2, holder.image3, holder.image4};
        for (int i = 0; i < imageViews.length; i++) {
            if (images != null && i < images.size() && i < 4) {
                imageViews[i].setVisibility(View.VISIBLE);
                Glide.with(context).load(images.get(i)).into(imageViews[i]);
            } else {
                imageViews[i].setVisibility(View.GONE);
            }
        }
        if (images != null && images.size() > 4) {
            holder.overlay.setVisibility(View.VISIBLE);
            holder.tvMoreImages.setVisibility(View.VISIBLE);
            holder.tvMoreImages.setText("+" + (images.size() - 4));
        } else {
            holder.overlay.setVisibility(View.GONE);
            holder.tvMoreImages.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return postCardItemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView btnProfile, image1, image2, image3, image4;
        TextView tvUsername, tvContent, tvLike, tvComment, tvSave, tvMoreImages;
        View overlay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            btnProfile = itemView.findViewById(R.id.btnProfile);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvContent = itemView.findViewById(R.id.tvContent);
            image1 = itemView.findViewById(R.id.image1);
            image2 = itemView.findViewById(R.id.image2);
            image3 = itemView.findViewById(R.id.image3);
            image4 = itemView.findViewById(R.id.image4);
            overlay = itemView.findViewById(R.id.overlay);
            tvMoreImages = itemView.findViewById(R.id.tvMoreImages);
            tvLike = itemView.findViewById(R.id.text_like_count);
            tvComment = itemView.findViewById(R.id.text_comment_count);
            tvSave = itemView.findViewById(R.id.text_save_count);
        }
    }
}
