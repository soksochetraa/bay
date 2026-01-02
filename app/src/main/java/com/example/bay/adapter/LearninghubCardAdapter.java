package com.example.bay.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.bay.R;
import com.example.bay.model.LearninghubCard;
import java.util.ArrayList;
import java.util.List;

public class LearninghubCardAdapter extends RecyclerView.Adapter<LearninghubCardAdapter.CardViewHolder> {

    private List<LearninghubCard> cards;
    private final OnItemClickListener listener;
    private final OnSaveClickListener saveListener;
    private final OnReadClickListener readListener;
    private int lastPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(LearninghubCard card);
    }

    public interface OnSaveClickListener {
        void onSaveClick(LearninghubCard card, boolean isSaved);
    }

    public interface OnReadClickListener {
        void onReadClick(LearninghubCard card);
    }

    public LearninghubCardAdapter(OnItemClickListener itemListener, OnSaveClickListener saveListener, OnReadClickListener readListener) {
        this.cards = new ArrayList<>();
        this.listener = itemListener;
        this.saveListener = saveListener;
        this.readListener = readListener;
    }

    public void setCards(List<LearninghubCard> cards) {
        if (cards != null) {
            this.cards = cards;
        } else {
            this.cards = new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    public void setCardsWithAnimation(List<LearninghubCard> newCards) {
        List<LearninghubCard> oldCards = new ArrayList<>(this.cards);
        this.cards = newCards != null ? newCards : new ArrayList<>();

        if (!oldCards.isEmpty() && !newCards.isEmpty()) {
            notifyDataSetChanged();
        } else {
            notifyDataSetChanged();
        }
    }

    public List<LearninghubCard> getCards() {
        return new ArrayList<>(cards);
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_learninghub_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        if (position >= 0 && position < cards.size()) {
            LearninghubCard card = cards.get(position);
            holder.bind(card, listener, saveListener, readListener);

            // Add entrance animation
            setAnimation(holder.itemView, position);
        }
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(400);
            anim.setStartOffset(position * 100);
            viewToAnimate.startAnimation(anim);
            lastPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull CardViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDescription, tvAuthor, tvDate, tvCategory;
        private final ImageView ivCardImage;
        private final ImageButton ivSave;
        private final Button btnReadArticle;

        public CardViewHolder(@NonNull View itemView) {
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

        public void bind(LearninghubCard card, OnItemClickListener listener,
                         OnSaveClickListener saveListener, OnReadClickListener readListener) {
            if (card == null) return;

            // Set text safely
            tvTitle.setText(card.getTitle() != null ? card.getTitle() : "");
            tvDescription.setText(card.getDescription() != null ? card.getDescription() : "");
            tvAuthor.setText(card.getAuthor() != null ? card.getAuthor() : "");
            tvDate.setText(card.getDate() != null ? card.getDate() : "");
            tvCategory.setText(card.getCategory() != null ? card.getCategory() : "");

            // Load image with Glide
            String imageUrl = card.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty() && !"null".equals(imageUrl)) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .placeholder(R.drawable.imagecard1)
                        .error(R.drawable.imagecard1)
                        .centerCrop()
                        .into(ivCardImage);
            } else {
                ivCardImage.setImageResource(R.drawable.imagecard1);
            }

            // Set save icon state
            updateSaveButton(card.getIsSaved());

            // Item click listener with animation
            itemView.setOnClickListener(v -> {
                animateCardClick(v, () -> {
                    if (listener != null) {
                        listener.onItemClick(card);
                    }
                });
            });

            // Save button click listener with animation
            ivSave.setOnClickListener(v -> {
                animateSaveButton(v, () -> {
                    if (saveListener != null) {
                        boolean newSavedState = !card.getIsSaved();
                        saveListener.onSaveClick(card, newSavedState);
                        updateSaveButton(newSavedState);
                    }
                });
            });

            // Read article button click listener with animation - FIXED
            btnReadArticle.setOnClickListener(v -> {
                // Debug log
                android.util.Log.d("CardAdapter", "Read Article button clicked for: " + card.getTitle());

                animateButtonClick(v, () -> {
                    if (readListener != null) {
                        android.util.Log.d("CardAdapter", "Calling readListener for: " + card.getTitle());
                        readListener.onReadClick(card);
                    } else {
                        android.util.Log.e("CardAdapter", "readListener is NULL for: " + card.getTitle());
                    }
                });
            });

            // Add long press for debugging
            btnReadArticle.setOnLongClickListener(v -> {
                android.widget.Toast.makeText(itemView.getContext(),
                        "Read Article: " + card.getTitle(),
                        android.widget.Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        private void updateSaveButton(boolean isSaved) {
            int heartIcon = isSaved ? R.drawable.ic_heart_outline : R.drawable.ic_heart;
            ivSave.setImageResource(heartIcon);

            String contentDescription = isSaved ?
                    itemView.getContext().getString(R.string.unsave_card) :
                    itemView.getContext().getString(R.string.save_card);
            ivSave.setContentDescription(contentDescription);
        }

        private void animateCardClick(View view, Runnable onComplete) {
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f);

            scaleDownX.setDuration(100);
            scaleDownY.setDuration(100);

            scaleDownX.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f);
                    ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f);

                    scaleUpX.setDuration(100);
                    scaleUpY.setDuration(100);

                    scaleUpX.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            onComplete.run();
                        }
                    });

                    scaleUpX.start();
                    scaleUpY.start();
                }
            });

            scaleDownX.start();
            scaleDownY.start();
        }

        private void animateSaveButton(View view, Runnable onComplete) {
            ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 1.2f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1.2f);

            scaleUp.setDuration(150);
            scaleUpY.setDuration(150);
            scaleUp.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleUpY.setInterpolator(new AccelerateDecelerateInterpolator());

            scaleUp.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f);
                    ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f);

                    scaleDown.setDuration(150);
                    scaleDownY.setDuration(150);
                    scaleDown.setInterpolator(new AccelerateDecelerateInterpolator());
                    scaleDownY.setInterpolator(new AccelerateDecelerateInterpolator());

                    scaleDown.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            onComplete.run();
                        }
                    });

                    scaleDown.start();
                    scaleDownY.start();
                }
            });

            scaleUp.start();
            scaleUpY.start();
        }

        private void animateButtonClick(View view, Runnable onComplete) {
            AlphaAnimation fadeOut = new AlphaAnimation(1, 0.7f);
            fadeOut.setDuration(100);
            fadeOut.setFillAfter(true);

            AlphaAnimation fadeIn = new AlphaAnimation(0.7f, 1);
            fadeIn.setDuration(100);
            fadeIn.setFillAfter(true);

            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.startAnimation(fadeIn);
                    onComplete.run();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });

            view.startAnimation(fadeOut);
        }
    }
}