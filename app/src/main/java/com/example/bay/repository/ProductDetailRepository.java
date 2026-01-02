package com.example.bay.repository;

import androidx.annotation.NonNull;

import com.example.bay.model.Review;
import com.example.bay.model.User;
import com.google.firebase.database.*;

import java.util.*;

public class ProductDetailRepository {
    private final DatabaseReference usersRef;
    private final DatabaseReference reviewsRef;

    public ProductDetailRepository() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        reviewsRef = database.getReference("reviews");
    }

    public interface UserCallback<T> {
        void onSuccess(T result);
        void onError(String errorMsg);
    }

    public interface ReviewsCallback<T> {
        void onSuccess(T result);
        void onError(String errorMsg);
    }

    public interface UsersMapCallback {
        void onSuccess(Map<String, User> usersMap);
        void onError(String errorMsg);
    }

    // Get user by ID
    public void getUserById(String userId, UserCallback<User> callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onError("User ID is empty");
            return;
        }

        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        callback.onSuccess(user);
                    } else {
                        callback.onError("Failed to parse user data");
                    }
                } else {
                    callback.onError("User not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Failed to load user: " + error.getMessage());
            }
        });
    }

    // Get reviews for item
    public void getReviews(String itemId, ReviewsCallback<List<Review>> callback) {
        if (itemId == null || itemId.isEmpty()) {
            callback.onError("Item ID is empty");
            return;
        }

        Query query = reviewsRef.orderByChild("itemId").equalTo(itemId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Review> reviews = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                        Review review = reviewSnapshot.getValue(Review.class);
                        if (review != null) {
                            reviews.add(review);
                        }
                    }
                }
                callback.onSuccess(reviews);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Failed to load reviews: " + error.getMessage());
            }
        });
    }

    // Get latest reviews (for preview)
    public void getLatestReviews(String itemId, ReviewsCallback<List<Review>> callback) {
        getReviews(itemId, new ReviewsCallback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> reviews) {
                // Sort by createdAt (latest first)
                reviews.sort((r1, r2) -> {
                    Long time1 = r1.getCreatedAt() != null ? r1.getCreatedAt() : 0L;
                    Long time2 = r2.getCreatedAt() != null ? r2.getCreatedAt() : 0L;
                    return time2.compareTo(time1);
                });

                // Take only first 2
                if (reviews.size() > 2) {
                    callback.onSuccess(reviews.subList(0, 2));
                } else {
                    callback.onSuccess(reviews);
                }
            }

            @Override
            public void onError(String errorMsg) {
                callback.onError(errorMsg);
            }
        });
    }

    // Get multiple users by IDs
    public void getUsersByIds(List<String> userIds, UsersMapCallback callback) {
        if (userIds == null || userIds.isEmpty()) {
            callback.onSuccess(new HashMap<>());
            return;
        }

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, User> usersMap = new HashMap<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null && userIds.contains(user.getUserId())) {
                        usersMap.put(user.getUserId(), user);
                    }
                }
                callback.onSuccess(usersMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Failed to load users: " + error.getMessage());
            }
        });
    }
}