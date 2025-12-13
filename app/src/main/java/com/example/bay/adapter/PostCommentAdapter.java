package com.example.bay.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.model.Comment;
import com.example.bay.model.User;
import com.example.bay.util.TimeUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostCommentAdapter extends RecyclerView.Adapter<PostCommentAdapter.ViewHolder> {

    public interface OnCommentActionListener {
        void onReplyClicked(Comment comment);
    }

    private List<Comment> commentList = new ArrayList<>();
    private final Map<String, User> userCache = new HashMap<>();
    private final Context context;
    private final OnCommentActionListener listener;

    public PostCommentAdapter(Context context, OnCommentActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setComments(List<Comment> comments) {
        this.commentList = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Comment> getComments() {
        return commentList;
    }

    public int getPositionForCommentId(String commentId) {
        if (commentId == null) return -1;
        for (int i = 0; i < commentList.size(); i++) {
            Comment c = commentList.get(i);
            if (commentId.equals(c.getCommentId())) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public PostCommentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostCommentAdapter.ViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.tvCommentText.setText(comment.getText());

        String ts = comment.getTimestamp();
        String timeAgo = ts != null ? TimeUtils.formatTimeAgo(ts) : "មិនទាន់មាន";
        holder.tvCommentTime.setText(timeAgo);

        holder.tvCommentUsername.setText("កំពុងផ្ទុក...");
        holder.imgProfile.setImageResource(R.drawable.img);

        bindCommentUser(comment.getUserId(), holder);
        bindReplyInfo(comment, holder, position);

        holder.tvReply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReplyClicked(comment);
            }
        });
    }

    private void bindCommentUser(String userId, ViewHolder holder) {
        if (userId == null || userId.isEmpty()) {
            holder.tvCommentUsername.setText("អ្នកប្រើប្រាស់");
            holder.imgProfile.setImageResource(R.drawable.img);
            return;
        }

        User cached = userCache.get(userId);
        if (cached != null) {
            bindUserData(holder, cached);
            return;
        }

        holder.tvCommentUsername.setText("កំពុងផ្ទុក...");
        holder.imgProfile.setImageResource(R.drawable.img);

        fetchUserOnce(userId);
    }

    private void bindReplyInfo(Comment comment, ViewHolder holder, int position) {
        String parentCommentId = comment.getParentCommentId();

        View itemView = holder.itemView;
        ViewGroup.LayoutParams lp = itemView.getLayoutParams();
        ViewGroup.MarginLayoutParams params;
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            params = (ViewGroup.MarginLayoutParams) lp;
        } else {
            params = new ViewGroup.MarginLayoutParams(lp);
        }

        if (parentCommentId == null || parentCommentId.isEmpty()) {
            holder.llReplyTo.setVisibility(View.GONE);
            holder.viewReplyLine.setVisibility(View.GONE);
            holder.forReplyShow.setVisibility(View.GONE);

            params.leftMargin = 0;
            itemView.setLayoutParams(params);
            return;
        }

        holder.llReplyTo.setVisibility(View.VISIBLE);
        holder.forReplyShow.setVisibility(View.VISIBLE);

        params.leftMargin = dpToPx(16, itemView.getContext());
        itemView.setLayoutParams(params);

        boolean hasNextSibling = false;
        for (int i = position + 1; i < commentList.size(); i++) {
            Comment next = commentList.get(i);
            String nextParentId = next.getParentCommentId();
            if (parentCommentId.equals(nextParentId)) {
                hasNextSibling = true;
                break;
            }
        }

        if (hasNextSibling) {
            holder.viewReplyLine.setVisibility(View.VISIBLE);
        } else {
            holder.viewReplyLine.setVisibility(View.GONE);
        }

        Comment parent = null;
        for (Comment c : commentList) {
            if (parentCommentId.equals(c.getCommentId())) {
                parent = c;
                break;
            }
        }

        if (parent == null) {
            holder.tvReplyToUsername.setText("អ្នកប្រើប្រាស់");
            return;
        }

        String parentUserId = parent.getUserId();
        if (parentUserId == null || parentUserId.isEmpty()) {
            holder.tvReplyToUsername.setText("អ្នកប្រើប្រាស់");
            return;
        }

        User cached = userCache.get(parentUserId);
        if (cached != null) {
            String fullName = buildFullName(cached);
            holder.tvReplyToUsername.setText(
                    fullName != null && !fullName.isEmpty() ? fullName : "អ្នកប្រើប្រាស់"
            );
        } else {
            holder.tvReplyToUsername.setText("កំពុងផ្ទុក...");
            fetchUserOnce(parentUserId);
        }
    }

    private void fetchUserOnce(String userId) {
        if (userId == null || userId.isEmpty()) return;
        if (userCache.containsKey(userId)) return;

        FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            userCache.put(userId, user);
                            notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private void bindUserData(ViewHolder holder, User user) {
        String fullName = buildFullName(user);
        if (fullName != null && !fullName.isEmpty()) {
            holder.tvCommentUsername.setText(fullName);
        } else {
            holder.tvCommentUsername.setText("អ្នកប្រើប្រាស់");
        }

        String profileUrl = user.getProfileImageUrl();
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(context)
                    .load(profileUrl)
                    .placeholder(R.drawable.img)
                    .into(holder.imgProfile);
        } else {
            holder.imgProfile.setImageResource(R.drawable.img);
        }
    }

    private String buildFullName(User user) {
        String fullName = "";
        if (user.getFirst_name() != null) fullName += user.getFirst_name() + " ";
        if (user.getLast_name() != null) fullName += user.getLast_name();
        return fullName.trim();
    }

    private int dpToPx(int dp, Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvCommentUsername, tvCommentText, tvCommentTime;
        TextView tvReply, tvReplyToUsername;
        ImageView imgProfile;
        LinearLayout llReplyTo;
        View viewReplyLine;
        View forReplyShow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentUsername = itemView.findViewById(R.id.tvCommentUsername);
            tvCommentText = itemView.findViewById(R.id.tvCommentText);
            tvCommentTime = itemView.findViewById(R.id.tvCommentTime);
            tvReply = itemView.findViewById(R.id.tvReply);
            tvReplyToUsername = itemView.findViewById(R.id.tvReplyToUsername);
            llReplyTo = itemView.findViewById(R.id.llReplyTo);
            viewReplyLine = itemView.findViewById(R.id.viewReplyLine);
            imgProfile = itemView.findViewById(R.id.ivCommentProfile);
            forReplyShow = itemView.findViewById(R.id.forReplyShow);
        }
    }
}
