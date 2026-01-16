package com.example.bay.adapter;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.bay.model.PostCardItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PostCardUserAdapter extends PostCardCommunityAdapter {

    private final String userId;

    public PostCardUserAdapter(@NonNull Context context, @NonNull String userId) {
        super(context);
        this.userId = userId;
        loadUserPosts();
    }

    private void loadUserPosts() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("postCardItems");

        ref.orderByChild("userId")
                .equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<PostCardItem> list = new ArrayList<>();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            PostCardItem item = child.getValue(PostCardItem.class);
                            if (item != null) {
                                item.setItemId(child.getKey());
                                list.add(item);
                            }
                        }

                        setPostCardItemList(list);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // ignored intentionally
                    }
                });
    }
}
