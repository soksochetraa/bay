package com.example.bay.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.model.Comment;
import com.example.bay.model.User;
import com.example.bay.util.TimeUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostCommentAdapter extends RecyclerView.Adapter<PostCommentAdapter.ViewHolder> {

    public interface OnCommentActionListener {
        void onReplyClicked(Comment comment);
        void onDeleteRequested(Comment comment);
    }

    private List<Comment> commentList = new ArrayList<>();
    private final Map<String, User> userCache = new HashMap<>();
    private final Context context;
    private final OnCommentActionListener listener;
    private final String postId;

    private String editingCommentId = null;

    private final String currentUserId =
            FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;

    public PostCommentAdapter(Context context, String postId, OnCommentActionListener listener) {
        this.context = context;
        this.postId = postId;
        this.listener = listener;
    }

    public void setComments(List<Comment> comments) {
        this.commentList = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Comment> getComments() {
        return commentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_comment, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        boolean isReply = comment.getParentCommentId() != null && !comment.getParentCommentId().isEmpty();
        boolean isEditing = comment.getCommentId() != null && comment.getCommentId().equals(editingCommentId);

        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();

        int indent = dpToPx(28);

        if (isReply) {
            if (ViewCompat.getLayoutDirection(holder.itemView)
                    == ViewCompat.LAYOUT_DIRECTION_RTL) {
                lp.setMarginEnd(indent);
                lp.setMarginStart(0);
            } else {
                lp.setMarginStart(indent);
                lp.setMarginEnd(0);
            }
        } else {
            lp.setMarginStart(0);
            lp.setMarginEnd(0);
        }
        holder.itemView.setLayoutParams(lp);

        holder.llReplyTo.setVisibility(isReply ? View.VISIBLE : View.GONE);
        holder.forReplyShow.setVisibility(isReply ? View.VISIBLE : View.GONE);

        if (isReply && !isLastChild(comment, position)) {
            holder.viewReplyLine.setVisibility(View.VISIBLE);
        } else {
            holder.viewReplyLine.setVisibility(View.GONE);
        }

        holder.layoutEditComment.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        holder.tvCommentText.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        holder.tvReply.setVisibility(isEditing ? View.GONE : View.VISIBLE);

        holder.ivCommentMenu.setVisibility(
                !isEditing && currentUserId != null && currentUserId.equals(comment.getUserId())
                        ? View.VISIBLE : View.GONE
        );

        holder.tvCommentText.setText(comment.getText() != null ? comment.getText() : "");
        holder.tvEdited.setVisibility(comment.isEdited() ? View.VISIBLE : View.GONE);

        String ts = comment.getTimestamp();
        holder.tvCommentTime.setText(
                ts != null ? TimeUtils.formatTimeAgo(ts) : "មិនទាន់មាន"
        );

        holder.tvCommentUsername.setText("កំពុងផ្ទុក...");
        holder.imgProfile.setImageResource(R.drawable.img);

        bindCommentUser(comment.getUserId(), holder);

        if (isReply) {
            bindReplyInfo(comment, holder);
        }

        holder.tvReply.setOnClickListener(v -> {
            if (listener != null) listener.onReplyClicked(comment);
        });

        holder.ivCommentMenu.setOnClickListener(v ->
                showCommentMenu(v, comment, holder.getAdapterPosition())
        );

        if (isEditing) {
            holder.etEditComment.setText(comment.getText() != null ? comment.getText() : "");
            holder.etEditComment.requestFocus();
        }

        holder.tvCancelEdit.setOnClickListener(v -> {
            editingCommentId = null;
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos);
        });

        holder.tvSaveEdit.setOnClickListener(v -> {
            String newText = holder.etEditComment.getText().toString().trim();
            if (newText.isEmpty()) return;

            Map<String, Object> updates = new HashMap<>();
            updates.put("text", newText);
            updates.put("edited", true);

            String id = comment.getCommentId();
            if (id == null) return;

            commentRef(id)
                    .updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        editingCommentId = null;
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos);
                    });
        });
    }

    private DatabaseReference commentRef(String commentId) {
        return FirebaseDatabase.getInstance()
                .getReference("postCardItems")
                .child(postId)
                .child("comments")
                .child(commentId);
    }

    private boolean isLastChild(Comment comment, int position) {
        String parentId = comment.getParentCommentId();
        if (parentId == null || parentId.isEmpty()) return false;

        for (int i = position + 1; i < commentList.size(); i++) {
            Comment next = commentList.get(i);
            if (parentId.equals(next.getParentCommentId())) {
                return false;
            }
            if (next.getParentCommentId() == null || next.getParentCommentId().isEmpty()) {
                break;
            }
        }
        return true;
    }

    private void showCommentMenu(View anchor, Comment comment, int position) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add("កែប្រែ");
        popup.getMenu().add("លុប");

        popup.setOnMenuItemClickListener(item -> {
            if ("កែប្រែ".equals(item.getTitle())) {
                editingCommentId = comment.getCommentId();
                notifyItemChanged(position);
                return true;
            }
            if ("លុប".equals(item.getTitle())) {
                if (listener != null) listener.onDeleteRequested(comment);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void bindCommentUser(String userId, ViewHolder holder) {
        if (userId == null) return;

        if (userCache.containsKey(userId)) {
            bindUserData(holder, userCache.get(userId));
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User u = snapshot.getValue(User.class);
                        if (u != null) {
                            userCache.put(userId, u);
                            notifyDataSetChanged();
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @SuppressLint("SetTextI18n")
    private void bindUserData(ViewHolder holder, User user) {
        String name = ((user.getFirst_name() != null ? user.getFirst_name() : "") + " " +
                (user.getLast_name() != null ? user.getLast_name() : "")).trim();
        holder.tvCommentUsername.setText(name.isEmpty() ? "អ្នកប្រើប្រាស់" : name);

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.img)
                    .into(holder.imgProfile);
        }

        if (user.isUserVerified()) {
            holder.tvCommentUsername.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(context, R.drawable.ico_user_verified),
                    null
            );
        } else {
            holder.tvCommentUsername.setCompoundDrawablesWithIntrinsicBounds(
                    null, null, null, null
            );
        }
    }

    private void bindReplyInfo(Comment comment, ViewHolder holder) {
        String parentId = comment.getParentCommentId();
        if (parentId == null) return;

        for (Comment c : commentList) {
            if (parentId.equals(c.getCommentId())) {
                User u = userCache.get(c.getUserId());
                holder.tvReplyToUsername.setText(
                        u != null
                                ? (safe(u.getFirst_name()) + " " + safe(u.getLast_name())).trim()
                                : "កំពុងផ្ទុក..."
                );
                break;
            }
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvCommentUsername, tvCommentText, tvCommentTime, tvEdited;
        TextView tvReply, tvReplyToUsername;
        ImageView imgProfile, ivCommentMenu;
        LinearLayout llReplyTo, layoutEditComment;
        View viewReplyLine, forReplyShow;
        EditText etEditComment;
        TextView tvCancelEdit, tvSaveEdit;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCommentUsername = itemView.findViewById(R.id.tvCommentUsername);
            tvCommentText = itemView.findViewById(R.id.tvCommentText);
            tvCommentTime = itemView.findViewById(R.id.tvCommentTime);
            tvEdited = itemView.findViewById(R.id.tvEdited);

            tvReply = itemView.findViewById(R.id.tvReply);
            tvReplyToUsername = itemView.findViewById(R.id.tvReplyToUsername);

            imgProfile = itemView.findViewById(R.id.ivCommentProfile);
            ivCommentMenu = itemView.findViewById(R.id.ivCommentMenu);

            llReplyTo = itemView.findViewById(R.id.llReplyTo);
            viewReplyLine = itemView.findViewById(R.id.viewReplyLine);
            forReplyShow = itemView.findViewById(R.id.forReplyShow);

            layoutEditComment = itemView.findViewById(R.id.layoutEditComment);
            etEditComment = itemView.findViewById(R.id.etEditComment);
            tvCancelEdit = itemView.findViewById(R.id.tvCancelEdit);
            tvSaveEdit = itemView.findViewById(R.id.tvSaveEdit);
        }
    }
}