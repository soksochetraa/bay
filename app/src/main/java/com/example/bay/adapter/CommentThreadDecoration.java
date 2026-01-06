package com.example.bay.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bay.model.Comment;

import java.util.List;

public class CommentThreadDecoration extends RecyclerView.ItemDecoration {

    private final PostCommentAdapter adapter;
    private final Paint paint;
    private float progress = 1f;

    // ====== CUSTOMIZABLE KNOBS ======
    private final int xOffset;          // horizontal alignment under avatar
    private final int yOffset;          // vertical bend into reply
    private final int curveRadius;      // curve softness
    private final int parentTopMargin;  // distance from parent profile top
    private final int lastChildCut;     // how much to shorten last child line
    // ================================

    private final RecyclerView recyclerView;

    public CommentThreadDecoration(PostCommentAdapter adapter,
                                   Context ctx,
                                   RecyclerView rv) {

        this.adapter = adapter;
        this.recyclerView = rv;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2, ctx));
        paint.setColor(0xFFB0B0B0);

        xOffset = dp(21, ctx);
        yOffset = dp(32, ctx);
        curveRadius = dp(10, ctx);
        parentTopMargin = dp(34, ctx);
        lastChildCut = dp(0, ctx);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(250);
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            recyclerView.invalidateItemDecorations();
        });
        animator.start();
    }

    @Override
    public void onDraw(@NonNull Canvas canvas,
                       @NonNull RecyclerView parent,
                       @NonNull RecyclerView.State state) {

        List<Comment> comments = adapter.getComments();
        if (comments == null || comments.isEmpty()) return;

        for (int i = 0; i < parent.getChildCount(); i++) {

            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos == RecyclerView.NO_POSITION) continue;

            Comment comment = comments.get(pos);
            if (comment.getParentCommentId() == null) continue;

            View parentView = findParentView(parent, comments, pos);
            if (parentView == null) continue;

            float startX = parentView.getLeft() + xOffset;
            float startY = parentView.getTop() + parentTopMargin;

            float endX = child.getLeft() + xOffset;
            float endY = child.getTop() + yOffset;

            if (isLastChild(comments, pos)) {
                endY -= lastChildCut;
            }

            float animatedEndY =
                    startY + (endY - startY) * easeOut(progress);

            drawCurve(canvas, startX, startY, endX, animatedEndY);
        }
    }

    private void drawCurve(Canvas canvas,
                           float sx, float sy,
                           float ex, float ey) {

        Path path = new Path();
        path.moveTo(sx, sy);
        path.lineTo(sx, ey - curveRadius);
        path.quadTo(sx, ey, sx + curveRadius, ey);
        path.lineTo(ex, ey);
        canvas.drawPath(path, paint);
    }

    private View findParentView(RecyclerView rv,
                                List<Comment> list,
                                int pos) {

        String parentId = list.get(pos).getParentCommentId();
        for (int i = 0; i < rv.getChildCount(); i++) {
            View v = rv.getChildAt(i);
            int p = rv.getChildAdapterPosition(v);
            if (p != RecyclerView.NO_POSITION &&
                    p < pos &&
                    parentId.equals(list.get(p).getCommentId())) {
                return v;
            }
        }
        return null;
    }

    private boolean isLastChild(List<Comment> list, int pos) {
        String parentId = list.get(pos).getParentCommentId();
        for (int i = pos + 1; i < list.size(); i++) {
            if (parentId.equals(list.get(i).getParentCommentId())) {
                return false;
            }
        }
        return true;
    }

    private float easeOut(float t) {
        return (float) (1 - Math.pow(1 - t, 2));
    }

    private int dp(int dp, Context ctx) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
}
