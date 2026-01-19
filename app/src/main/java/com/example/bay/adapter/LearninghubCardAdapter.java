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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.bay.R;
import com.example.bay.model.LearninghubCard;
import java.util.ArrayList;
import java.util.List;

public class LearninghubCardAdapter extends ListAdapter<LearninghubCard, LearninghubCardAdapter.CardViewHolder> {

    private static final int VIEW_TYPE_GRID = 1;
    private static final int VIEW_TYPE_LIST = 2;

    private final OnItemClickListener listener;
    private final OnSaveClickListener saveListener;
    private final OnReadClickListener readListener;
    private boolean isKnowledgeTabActive = true;
    private int lastPosition = -1;

    private static final DiffUtil.ItemCallback<LearninghubCard> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<LearninghubCard>() {
                @Override
                public boolean areItemsTheSame(@NonNull LearninghubCard oldItem, @NonNull LearninghubCard newItem) {
                    return oldItem.getUuid().equals(newItem.getUuid());
                }

                @Override
                public boolean areContentsTheSame(@NonNull LearninghubCard oldItem, @NonNull LearninghubCard newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public interface OnItemClickListener {
        void onItemClick(LearninghubCard card);
    }

    public interface OnSaveClickListener {
        void onSaveClick(LearninghubCard card, boolean isSaved);
    }

    public interface OnReadClickListener {
        void onReadClick(LearninghubCard card);
    }

    public LearninghubCardAdapter(OnItemClickListener itemListener,
                                  OnSaveClickListener saveListener,
                                  OnReadClickListener readListener,
                                  boolean isKnowledgeTabActive) {
        super(DIFF_CALLBACK);
        this.listener = itemListener;
        this.saveListener = saveListener;
        this.readListener = readListener;
        this.isKnowledgeTabActive = isKnowledgeTabActive;
    }

    public void setTabActive(boolean isKnowledgeTabActive) {
        if (this.isKnowledgeTabActive != isKnowledgeTabActive) {
            this.isKnowledgeTabActive = isKnowledgeTabActive;
            notifyDataSetChanged();
        }
    }

    public void submitCards(List<LearninghubCard> cards) {
        submitList(cards != null ? new ArrayList<>(cards) : null);
    }

    @Override
    public int getItemViewType(int position) {
        return isKnowledgeTabActive ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_LIST) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card_save, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_learninghub_card, parent, false);
        }
        return new CardViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        LearninghubCard card = getItem(position);
        if (card != null) {
            holder.bind(card, listener, saveListener, readListener);
            setAnimation(holder.itemView, position);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull CardViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull CardViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.clearAnimation();
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(400);
            anim.setStartOffset(Math.min(position, 10) * 100);
            viewToAnimate.startAnimation(anim);
            lastPosition = position;
        }
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDescription, tvAuthor, tvDate, tvCategory;
        private final ImageView ivCardImage;
        private final ImageButton ivSave;
        private final Button btnReadArticle;
        private final int viewType;

        private static final RequestOptions glideOptions = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.img)
                .error(R.drawable.img)
                .dontTransform();

        public CardViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;

            tvTitle = itemView.findViewById(R.id.tv_card_title);
            tvDescription = itemView.findViewById(R.id.tv_card_description);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvDate = itemView.findViewById(R.id.tv_card_date);
            tvCategory = itemView.findViewById(R.id.tv_category);
            ivCardImage = itemView.findViewById(R.id.iv_card_image);
            ivSave = itemView.findViewById(R.id.iv_save);
            btnReadArticle = viewType == VIEW_TYPE_GRID ?
                    itemView.findViewById(R.id.btn_read_article) : null;
        }

        public void bind(LearninghubCard card, OnItemClickListener listener,
                         OnSaveClickListener saveListener, OnReadClickListener readListener) {
            if (card == null) return;

            tvTitle.setText(card.getTitle() != null ? card.getTitle() : "");
            tvDescription.setText(card.getDescription() != null ? card.getDescription() : "");
            tvAuthor.setText(card.getAuthor() != null ? card.getAuthor() : "");
            tvDate.setText(card.getDate() != null ? card.getDate() : "");
            tvCategory.setText(card.getCategory() != null ? card.getCategory() : "");

            String imageUrl = card.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty() && !"null".equals(imageUrl)) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .apply(glideOptions)
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(ivCardImage);
            } else {
                ivCardImage.setImageResource(R.drawable.img);
            }

            updateSaveButton(card.getIsSaved());

            itemView.setOnClickListener(v -> {
                v.setEnabled(false);
                animateCardClick(v, () -> {
                    if (listener != null) {
                        listener.onItemClick(card);
                    }
                    v.setEnabled(true);
                });
            });

            ivSave.setOnClickListener(v -> {
                v.setEnabled(false);
                animateSaveButton(v, () -> {
                    if (saveListener != null) {
                        boolean newSavedState = !card.getIsSaved();
                        saveListener.onSaveClick(card, newSavedState);
                        updateSaveButton(newSavedState);
                    }
                    v.setEnabled(true);
                });
            });

            if (viewType == VIEW_TYPE_GRID && btnReadArticle != null) {
                btnReadArticle.setOnClickListener(v -> {
                    v.setEnabled(false);
                    animateButtonClick(v, () -> {
                        if (readListener != null) {
                            readListener.onReadClick(card);
                        }
                        v.setEnabled(true);
                    });
                });
            }
        }

        private void updateSaveButton(boolean isSaved) {
            int heartIcon = isSaved ? R.drawable.ic_heart_outline : R.drawable.ic_heart;
            ivSave.setImageResource(heartIcon);

            if (viewType == VIEW_TYPE_LIST) {
                int color = isSaved ?
                        ContextCompat.getColor(itemView.getContext(), R.color.primary_green) :
                        ContextCompat.getColor(itemView.getContext(), R.color.gray_600);
                ivSave.setColorFilter(color);
            }

            String contentDescription = isSaved ?
                    itemView.getContext().getString(R.string.unsave_card) :
                    itemView.getContext().getString(R.string.save_card);
            ivSave.setContentDescription(contentDescription);
        }

        public void clearAnimation() {
            itemView.clearAnimation();
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