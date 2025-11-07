package com.example.bay.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.model.LearninghubCard;
import java.util.ArrayList;
import java.util.List;

public class RelatedCardsAdapter extends RecyclerView.Adapter<RelatedCardsAdapter.RelatedCardViewHolder> {

    private List<LearninghubCard> relatedCards = new ArrayList<>();
    private OnRelatedCardClickListener listener;

    public interface OnRelatedCardClickListener {
        void onRelatedCardClick(LearninghubCard card);
    }

    public void setOnRelatedCardClickListener(OnRelatedCardClickListener listener) {
        this.listener = listener;
    }

    public void setRelatedCards(List<LearninghubCard> cards) {
        this.relatedCards = cards != null ? cards : new ArrayList<>();
        notifyDataSetChanged();
        Log.d("RelatedCardsAdapter", "Set " + relatedCards.size() + " related cards");
    }

    @NonNull
    @Override
    public RelatedCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_related_card, parent, false);
        return new RelatedCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RelatedCardViewHolder holder, int position) {
        LearninghubCard card = relatedCards.get(position);
        holder.bind(card);

        holder.itemView.setOnClickListener(v -> {
            Log.d("RelatedCardsAdapter", "Card clicked: " + card.getTitle());
            if (listener != null) {
                listener.onRelatedCardClick(card);
            } else {
                Log.e("RelatedCardsAdapter", "Listener is null!");
            }
        });

        holder.tvReadMore.setOnClickListener(v -> {
            Log.d("RelatedCardsAdapter", "Read More clicked: " + card.getTitle());
            if (listener != null) {
                listener.onRelatedCardClick(card);
            } else {
                Log.e("RelatedCardsAdapter", "Listener is null!");
            }
        });
    }

    @Override
    public int getItemCount() {
        return relatedCards.size();
    }

    static class RelatedCardViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCardImage;
        TextView tvCardTitle, tvCardDescription, tvReadMore;

        public RelatedCardViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCardImage = itemView.findViewById(R.id.iv_card_image);
            tvCardTitle = itemView.findViewById(R.id.tv_card_title);
            tvCardDescription = itemView.findViewById(R.id.tv_card_description);
            tvReadMore = itemView.findViewById(R.id.read_more);
        }

        public void bind(LearninghubCard card) {
            tvCardTitle.setText(card.getTitle() != null ? card.getTitle() : "No Title");

            String description = card.getDescription() != null ? card.getDescription() : "";
            if (description.length() > 100) {
                description = description.substring(0, 100) + "...";
            }
            tvCardDescription.setText(description);

            String imageUrl = card.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty() && !"null".equals(imageUrl)) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.imagecard1)
                        .error(R.drawable.imagecard1)
                        .into(ivCardImage);
            } else {
                ivCardImage.setImageResource(R.drawable.imagecard1);
            }
        }
    }
}