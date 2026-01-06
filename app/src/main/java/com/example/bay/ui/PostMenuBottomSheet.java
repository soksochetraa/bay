package com.example.bay.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bay.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class PostMenuBottomSheet extends BottomSheetDialog {

    public interface Callback {
        void onViewSeller();
        void onMessage();
    }

    private final Callback callback;

    public PostMenuBottomSheet(
            @NonNull Context context,
            Callback callback
    ) {
        super(context, R.style.BottomSheetDialogTheme2);
        this.callback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View content = LayoutInflater.from(getContext())
                .inflate(R.layout.item_post_menu_modal, null);

        setContentView(content);

        content.findViewById(R.id.ViewSellProfile).setOnClickListener(v -> {
            dismiss();
            if (callback != null) callback.onViewSeller();
        });

        content.findViewById(R.id.message).setOnClickListener(v -> {
            dismiss();
            if (callback != null) callback.onMessage();
        });

        setOnShowListener(dialog -> {

            View sheet = findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (sheet == null) return;

            ViewCompat.setOnApplyWindowInsetsListener(
                    sheet,
                    (v, insets) -> WindowInsetsCompat.CONSUMED
            );

            sheet.post(() -> sheet.setPadding(0, 0, 0, 0));

            ViewGroup.LayoutParams lp = sheet.getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            sheet.setLayoutParams(lp);

            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
            behavior.setFitToContents(true);
            behavior.setSkipCollapsed(true);
            behavior.setDraggable(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }
}
