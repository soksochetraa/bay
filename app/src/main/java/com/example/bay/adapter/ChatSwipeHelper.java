package com.example.bay.adapter;

import android.content.Context;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ChatSwipeHelper {

    public static ItemTouchHelper attachSwipeToDelete(
            Context context,
            RecyclerView recyclerView,
            ChatAdapter adapter,
            SwipeToDeleteChatCallback.OnSwipeDeleteListener listener
    ) {
        SwipeToDeleteChatCallback callback = new SwipeToDeleteChatCallback(context, adapter, listener);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(recyclerView);
        return helper;
    }
}