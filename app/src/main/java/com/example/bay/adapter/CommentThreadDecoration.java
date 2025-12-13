package com.example.bay.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;

import com.example.bay.R;
import com.example.bay.model.Comment;

import java.util.List;

public class CommentThreadDecoration extends RecyclerView.ItemDecoration {

    private final PostCommentAdapter adapter;
    private final Paint paint;
    private final int lineOffsetDp;
    private final int lineMarginTopDp; // ★ NEW

    public CommentThreadDecoration(PostCommentAdapter adapter, Context context) {
        this.adapter = adapter;
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(dpToPx(2, context));
        this.paint.setColor(ContextCompat.getColor(context, R.color.gray));

        this.lineOffsetDp = 16;     // X offset
        this.lineMarginTopDp = 20;   // ★ marginTop for the vertical line (tweak here)
    }

    @Override
    public void onDrawOver(@NonNull Canvas c,
                           @NonNull RecyclerView parent,
                           @NonNull RecyclerView.State state) {

        List<Comment> comments = adapter.getComments();
        if (comments == null || comments.isEmpty()) return;

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(childView);
            if (position == RecyclerView.NO_POSITION) continue;

            Comment comment = comments.get(position);
            String parentId = comment.getParentCommentId();
            if (parentId == null || parentId.isEmpty()) continue;

            int parentPos = adapter.getPositionForCommentId(parentId);
            if (parentPos == -1) continue;

            RecyclerView.ViewHolder parentVH = parent.findViewHolderForAdapterPosition(parentPos);
            if (parentVH == null) continue;

            View parentView = parentVH.itemView;

            float x = parentView.getLeft() + dpToPx(lineOffsetDp, parent.getContext());

            // ★ Add marginTop shift
            float marginTop = dpToPx(lineMarginTopDp, parent.getContext());

            // NEW Y positions with marginTop offset
            float parentCenterY = parentView.getTop() + parentView.getHeight() / 2f + marginTop;
            float childCenterY  = childView.getTop()  + childView.getHeight()  / 2f + marginTop;

            c.drawLine(x, parentCenterY, x, childCenterY, paint);
        }
    }

    private int dpToPx(int dp, Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
