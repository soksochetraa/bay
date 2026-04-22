package com.example.bay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.fragment.CardDetailFragment;
import com.example.bay.fragment.LearninghubFragment;
import com.example.bay.model.LearninghubCard;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class FragmentHomeLearninghubAdapter extends RecyclerView.Adapter<FragmentHomeLearninghubAdapter.ViewHolder> {

    public interface OnSaveClickListener {
        void onSaveClick(LearninghubCard card, boolean isSaved);
    }

    private final Context context;
    private List<LearninghubCard> items = new ArrayList<>();
    private OnSaveClickListener saveListener;

    private static final RequestOptions glideOptions = new RequestOptions()
            .centerCrop()
            .placeholder(R.drawable.img)
            .error(R.drawable.img)
            .dontTransform();

    public FragmentHomeLearninghubAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<LearninghubCard> list) {
        this.items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnSaveClickListener(OnSaveClickListener listener) {
        this.saveListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_learninghub_card_home, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LearninghubCard card = items.get(position);

        holder.tvTitle.setText(card.getTitle() != null ? card.getTitle() : "");
        holder.tvDescription.setText(card.getDescription() != null ? card.getDescription() : "");
        holder.tvAuthor.setText(card.getAuthor() != null ? card.getAuthor() : "");
        holder.tvDate.setText(card.getDate() != null ? card.getDate() : "");
        holder.tvCategory.setText(card.getCategory() != null ? card.getCategory() : "");

        String imageUrl = card.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty() && !"null".equals(imageUrl)) {
            Glide.with(context)
                    .load(imageUrl)
                    .apply(glideOptions)
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(holder.ivCardImage);
        } else {
            holder.ivCardImage.setImageResource(R.drawable.img);
        }

        if (holder.ivSave != null) {
            holder.ivSave.setVisibility(View.VISIBLE);
            int heartIcon = card.getIsSaved() ? R.drawable.ic_heart_outline : R.drawable.ic_heart;
            holder.ivSave.setImageResource(heartIcon);

            holder.ivSave.setOnClickListener(v -> {
                if (saveListener != null) {
                    boolean newSavedState = !card.getIsSaved();
                    saveListener.onSaveClick(card, newSavedState);
                    card.setIsSaved(newSavedState);
                    holder.ivSave.setImageResource(newSavedState ? R.drawable.ic_heart_outline : R.drawable.ic_heart);
                }
            });
        }

        View.OnClickListener clickListener = v -> {
            if (context instanceof HomeActivity) {
                CardDetailFragment fragment = new CardDetailFragment();
                Bundle args = new Bundle();
                args.putString("card_id", card.getUuid());
                args.putBoolean("from_learninghub", true);
                args.putBoolean("from_save_tab", false);
                fragment.setArguments(args);

                ((HomeActivity) context).getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment, fragment)
                        .addToBackStack("learninghub_to_detail")
                        .commit();
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        if (holder.btnReadArticle != null) holder.btnReadArticle.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvAuthor, tvDate, tvCategory;
        ImageView ivCardImage;
        ImageButton ivSave;
        View btnReadArticle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_card_title);
            tvDescription = itemView.findViewById(R.id.tv_card_description);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvDate = itemView.findViewById(R.id.tv_card_date);
            tvCategory = itemView.findViewById(R.id.tv_category);
            ivCardImage = itemView.findViewById(R.id.iv_card_image);
            ivSave = itemView.findViewById(R.id.iv_save);
            btnReadArticle = itemView.findViewById(R.id.btn_read_article);
        }
    }
}
