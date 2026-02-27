package com.example.bay.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bay.model.Chat;

public class SwipeToDeleteChatCallback extends ItemTouchHelper.SimpleCallback {

    public interface OnSwipeDeleteListener {
        void onSwipedDelete(Chat chat, int position);
    }

    private final ChatAdapter adapter;
    private final OnSwipeDeleteListener listener;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bgRect = new RectF();

    private final int paddingPx;
    private final float cornerRadiusPx;

    public SwipeToDeleteChatCallback(Context context, ChatAdapter adapter, OnSwipeDeleteListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.adapter = adapter;
        this.listener = listener;

        float d = context.getResources().getDisplayMetrics().density;
        float sd = context.getResources().getDisplayMetrics().scaledDensity;

        paddingPx = (int) (16 * d);
        cornerRadiusPx = 14f * d;

        bgPaint.setColor(Color.parseColor("#E53935"));

        iconPaint.setColor(Color.WHITE);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(2.2f * d);
        iconPaint.setStrokeCap(Paint.Cap.ROUND);
        iconPaint.setStrokeJoin(Paint.Join.ROUND);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(14f * sd);
        textPaint.setFakeBoldText(true);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getBindingAdapterPosition();
        if (pos == RecyclerView.NO_POSITION) return;

        Chat chat = adapter.getChatAt(pos);
        if (listener != null) listener.onSwipedDelete(chat, pos);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
            float left = itemView.getRight() + dX;
            float right = itemView.getRight();
            float top = itemView.getTop();
            float bottom = itemView.getBottom();

            bgRect.set(left, top, right, bottom);
            c.drawRoundRect(bgRect, cornerRadiusPx, cornerRadiusPx, bgPaint);

            String text = "Delete";
            float textWidth = textPaint.measureText(text);
            float textX = right - paddingPx - textWidth;
            float textY = top + (bottom - top) / 2f - (textPaint.ascent() + textPaint.descent()) / 2f;
            c.drawText(text, textX, textY, textPaint);

            float iconRight = textX - paddingPx;
            float iconCenterY = top + (bottom - top) / 2f;

            float iconSize = (bottom - top) * 0.34f;
            float iconLeft = iconRight - iconSize;
            float iconTop = iconCenterY - iconSize / 2f;
            float iconBottom = iconCenterY + iconSize / 2f;

            drawTrashIcon(c, iconLeft, iconTop, iconRight, iconBottom);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawTrashIcon(Canvas c, float left, float top, float right, float bottom) {
        float w = right - left;
        float h = bottom - top;

        float lidY = top + h * 0.28f;
        float bodyTop = top + h * 0.33f;
        float bodyBottom = bottom - h * 0.10f;

        float bodyLeft = left + w * 0.18f;
        float bodyRight = right - w * 0.18f;

        c.drawLine(bodyLeft, lidY, bodyRight, lidY, iconPaint);

        float handleW = w * 0.22f;
        float handleLeft = left + (w - handleW) / 2f;
        float handleRight = handleLeft + handleW;
        c.drawLine(handleLeft, top + h * 0.18f, handleRight, top + h * 0.18f, iconPaint);

        RectF body = new RectF(bodyLeft, bodyTop, bodyRight, bodyBottom);
        c.drawRoundRect(body, w * 0.08f, w * 0.08f, iconPaint);

        float x1 = bodyLeft + w * 0.22f;
        float x2 = left + w * 0.50f;
        float x3 = bodyRight - w * 0.22f;

        c.drawLine(x1, bodyTop + h * 0.10f, x1, bodyBottom - h * 0.10f, iconPaint);
        c.drawLine(x2, bodyTop + h * 0.10f, x2, bodyBottom - h * 0.10f, iconPaint);
        c.drawLine(x3, bodyTop + h * 0.10f, x3, bodyBottom - h * 0.10f, iconPaint);
    }
}