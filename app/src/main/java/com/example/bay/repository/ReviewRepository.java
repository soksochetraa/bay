package com.example.bay.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import com.example.bay.model.Review;
import com.example.bay.model.ShoppingItem;
import com.google.firebase.database.*;

import java.util.*;

public class ReviewRepository {
    private static final String TAG = "ReviewRepository";
    private final DatabaseReference reviewsRef;
    private final DatabaseReference shoppingItemsRef;
    private ValueEventListener latestReviewsListener;
    private Query latestReviewsQuery;

    public ReviewRepository() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        reviewsRef = database.getReference("reviews");
        shoppingItemsRef = database.getReference("shoppingItems");
    }

    public interface ReviewCallback<T> {
        void onSuccess(T result);
        void onError(String errorMsg);
    }

    // ✅ Get product by itemId (UUID)
    public void getProductByItemId(String uuidItemId, ReviewCallback<ShoppingItem> callback) {
        Log.d(TAG, "getProductByItemId - UUID: " + uuidItemId);

        if (uuidItemId == null || uuidItemId.isEmpty()) {
            callback.onError("Item ID is empty");
            return;
        }

        Query query = shoppingItemsRef.orderByChild("itemId").equalTo(uuidItemId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    Log.d(TAG, "Product not found with UUID: " + uuidItemId);
                    callback.onError("Product not found");
                    return;
                }

                ShoppingItem product = null;
                String firebaseKey = null;

                for (DataSnapshot child : snapshot.getChildren()) {
                    product = child.getValue(ShoppingItem.class);
                    firebaseKey = child.getKey();
                    break;
                }

                if (product == null) {
                    Log.d(TAG, "Failed to parse product");
                    callback.onError("Failed to parse product");
                    return;
                }

                // Set the Firebase key in the product object for later use
                product.setFirebaseKey(firebaseKey);
                callback.onSuccess(product);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error getting product: " + error.getMessage());
                callback.onError("Failed to get product: " + error.getMessage());
            }
        });
    }

    // ✅ Submit review with improved logic
    public void submitReview(String uuidItemId, String userId, float rating,
                             String comment, ReviewCallback<String> callback) {
        Log.d(TAG, "submitReview called - UUID: " + uuidItemId + ", userId: " + userId);

        if (uuidItemId == null || uuidItemId.isEmpty()) {
            Log.e(TAG, "Item ID is empty");
            callback.onError("ទិន្នន័យមិនត្រឹមត្រូវ");
            return;
        }

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is empty");
            callback.onError("សូមចូលគណនីជាមុន");
            return;
        }

        if (comment == null || comment.trim().isEmpty()) {
            Log.e(TAG, "Comment is empty");
            callback.onError("សូមបញ្ចូលមតិ");
            return;
        }

        if (rating < 1 || rating > 5) {
            Log.e(TAG, "Invalid rating: " + rating);
            callback.onError("សូមជ្រើសរើសការវាយតម្លៃពី ១ ទៅ ៥");
            return;
        }

        // Step 1: Get the product to check ownership and get Firebase key
        getProductByItemId(uuidItemId, new ReviewCallback<ShoppingItem>() {
            @Override
            public void onSuccess(ShoppingItem product) {
                // Check if user is the product owner
                if (userId.equals(product.getUserId())) {
                    Log.d(TAG, "User is the product owner - cannot review own product");
                    callback.onError("អ្នកមិនអាចផ្តល់មតិលើផលិតផលរបស់អ្នកបានទេ");
                    return;
                }

                String firebaseKey = product.getFirebaseKey();
                if (firebaseKey == null) {
                    Log.e(TAG, "Firebase key is null");
                    callback.onError("ទិន្នន័យមិនត្រឹមត្រូវ");
                    return;
                }

                // Step 2: Check if user already reviewed
                checkUserReview(uuidItemId, userId, new ReviewCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean hasReviewed) {
                        if (hasReviewed) {
                            Log.d(TAG, "User already reviewed this item");
                            callback.onError("អ្នកបានផ្តល់មតិរួចហើយ");
                            return;
                        }

                        // Step 3: Create review object
                        String reviewId = reviewsRef.push().getKey();
                        if (reviewId == null) {
                            Log.e(TAG, "Failed to generate review ID");
                            callback.onError("ទិន្នន័យមិនត្រឹមត្រូវ");
                            return;
                        }

                        Review review = new Review();
                        review.setReviewId(reviewId);
                        review.setItemId(uuidItemId);
                        review.setUserId(userId);
                        review.setRating(rating);
                        review.setComment(comment.trim());
                        review.setCreatedAt(System.currentTimeMillis());
                        review.setUpdatedAt(System.currentTimeMillis());

                        // Step 4: Save the review
                        reviewsRef.child(reviewId).setValue(review)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Review saved successfully, ID: " + reviewId);

                                    // Step 5: Update product stats using the correct Firebase key
                                    updateProductStatsTransaction(firebaseKey, rating, callback);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to save review: " + e.getMessage());
                                    callback.onError("បរាជ័យក្នុងការរក្សាទុកមតិ");
                                });
                    }

                    @Override
                    public void onError(String errorMsg) {
                        Log.e(TAG, "Error checking user review: " + errorMsg);
                        callback.onError("ទិន្នន័យមិនត្រឹមត្រូវ");
                    }
                });
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "Error getting product: " + errorMsg);
                callback.onError("ទិន្នន័យមិនត្រឹមត្រូវ");
            }
        });
    }

    // ✅ Update product stats - FIXED VERSION
    private void updateProductStatsTransaction(String firebaseKey, float newRating, ReviewCallback<String> callback) {
        DatabaseReference productRef = shoppingItemsRef.child(firebaseKey);

        productRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                ShoppingItem item = currentData.getValue(ShoppingItem.class);
                if (item == null) return Transaction.success(currentData);

                int oldCount = item.getReview_count();
                float oldAvg = item.getRating();

                int newCount = oldCount + 1;
                float newAvg = ((oldAvg * oldCount) + newRating) / newCount;

                item.setReview_count(newCount);
                item.setRating(newAvg);
                item.setUpdatedAt(System.currentTimeMillis());

                currentData.setValue(item);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    callback.onError("បរាជ័យក្នុងការធ្វើបច្ចុប្បន្នភាពការវាយតម្លៃ");
                } else {
                    callback.onSuccess("មតិរបស់អ្នកត្រូវបានបញ្ជូនដោយជោគជ័យ!");
                }
            }
        });
    }

    // ✅ Get latest 2 reviews for preview
    public void getLatestReviews(String uuidItemId, ReviewCallback<List<Review>> callback) {
        Log.d(TAG, "getLatestReviews called for UUID: " + uuidItemId);

        if (uuidItemId == null || uuidItemId.isEmpty()) {
            callback.onError("ទិន្នន័យមិនត្រឹមត្រូវ");
            return;
        }

        Query query = reviewsRef.orderByChild("itemId").equalTo(uuidItemId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Review> reviews = new ArrayList<>();

                if (snapshot.exists()) {
                    Log.d(TAG, "Found " + snapshot.getChildrenCount() + " reviews");

                    for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                        Review review = reviewSnapshot.getValue(Review.class);
                        if (review != null) {
                            reviews.add(review);
                        }
                    }

                    // Sort by createdAt (newest first)
                    Collections.sort(reviews, (r1, r2) -> {
                        Long time1 = r1.getCreatedAt() != null ? r1.getCreatedAt() : 0L;
                        Long time2 = r2.getCreatedAt() != null ? r2.getCreatedAt() : 0L;
                        return time2.compareTo(time1);
                    });

                    // Take only latest 2
                    if (reviews.size() > 2) {
                        reviews = reviews.subList(0, 2);
                    }
                } else {
                    Log.d(TAG, "No reviews found for this item");
                }

                Log.d(TAG, "Returning " + reviews.size() + " latest reviews");
                callback.onSuccess(reviews);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load latest reviews: " + error.getMessage());
                callback.onError("មិនអាចទាញយកមតិបាន");
            }
        });
    }

    // ✅ Get all reviews for a product
    public void getAllReviews(String uuidItemId, ReviewCallback<List<Review>> callback) {
        if (uuidItemId == null || uuidItemId.isEmpty()) {
            callback.onError("ទិន្នន័យមិនត្រឹមត្រូវ");
            return;
        }

        Query query = reviewsRef.orderByChild("itemId").equalTo(uuidItemId);

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

                    // Sort by createdAt (newest first)
                    Collections.sort(reviews, (r1, r2) -> {
                        Long time1 = r1.getCreatedAt() != null ? r1.getCreatedAt() : 0L;
                        Long time2 = r2.getCreatedAt() != null ? r2.getCreatedAt() : 0L;
                        return time2.compareTo(time1);
                    });
                }

                callback.onSuccess(reviews);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("មិនអាចទាញយកមតិបាន");
            }
        });
    }

    // ✅ Check if user has already reviewed
    public void checkUserReview(String uuidItemId, String userId, ReviewCallback<Boolean> callback) {
        Log.d(TAG, "checkUserReview - UUID: " + uuidItemId + ", userId: " + userId);

        if (uuidItemId == null || uuidItemId.isEmpty() || userId == null || userId.isEmpty()) {
            callback.onSuccess(false);
            return;
        }

        Query query = reviewsRef.orderByChild("itemId").equalTo(uuidItemId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasReviewed = false;

                if (snapshot.exists()) {
                    Log.d(TAG, "Checking " + snapshot.getChildrenCount() + " reviews");

                    for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                        Review review = reviewSnapshot.getValue(Review.class);
                        if (review != null && userId.equals(review.getUserId())) {
                            hasReviewed = true;
                            Log.d(TAG, "User has already reviewed this item");
                            break;
                        }
                    }
                }

                callback.onSuccess(hasReviewed);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking user review: " + error.getMessage());
                callback.onError("ទិន្នន័យមិនត្រឹមត្រូវ");
            }
        });
    }

    // ✅ Get product rating and count
    public void getProductStats(String uuidItemId, ReviewCallback<Map<String, Object>> callback) {
        if (uuidItemId == null || uuidItemId.isEmpty()) {
            callback.onError("ទិន្នន័យមិនត្រឹមត្រូវ");
            return;
        }

        Query query = reviewsRef.orderByChild("itemId").equalTo(uuidItemId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float totalRating = 0;
                int reviewCount = 0;

                if (snapshot.exists()) {
                    reviewCount = (int) snapshot.getChildrenCount();

                    for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                        Review review = reviewSnapshot.getValue(Review.class);
                        if (review != null) {
                            totalRating += review.getRating();
                        }
                    }
                }

                float averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;

                Map<String, Object> stats = new HashMap<>();
                stats.put("rating", averageRating);
                stats.put("review_count", reviewCount);

                callback.onSuccess(stats);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("មិនអាចទាញយកទិន្នន័យបាន");
            }
        });
    }

    // ✅ Check if user is product owner
    public void checkProductOwner(String uuidItemId, String userId, ReviewCallback<Boolean> callback) {
        Log.d(TAG, "checkProductOwner - UUID: " + uuidItemId + ", userId: " + userId);

        if (uuidItemId == null || uuidItemId.isEmpty() || userId == null || userId.isEmpty()) {
            callback.onSuccess(false);
            return;
        }

        Query query = shoppingItemsRef.orderByChild("itemId").equalTo(uuidItemId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isOwner = false;

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        ShoppingItem product = child.getValue(ShoppingItem.class);
                        if (product != null && userId.equals(product.getUserId())) {
                            isOwner = true;
                            break;
                        }
                    }
                }

                Log.d(TAG, "User is product owner: " + isOwner);
                callback.onSuccess(isOwner);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking product owner: " + error.getMessage());
                callback.onError("Failed to check product owner: " + error.getMessage());
            }
        });
    }
    public void observeLatestReviews(String uuidItemId, ReviewCallback<List<Review>> callback) {
        if (uuidItemId == null || uuidItemId.isEmpty()) {
            callback.onError("ទិន្នន័យមិនត្រឹមត្រូវ");
            return;
        }

        // Listen continuously
        latestReviewsQuery = reviewsRef.orderByChild("itemId").equalTo(uuidItemId);
        latestReviewsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Review> reviews = new ArrayList<>();

                for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                    Review review = reviewSnapshot.getValue(Review.class);
                    if (review != null) reviews.add(review);
                }

                // Sort newest first
                Collections.sort(reviews, (r1, r2) -> {
                    Long t1 = r1.getCreatedAt() != null ? r1.getCreatedAt() : 0L;
                    Long t2 = r2.getCreatedAt() != null ? r2.getCreatedAt() : 0L;
                    return t2.compareTo(t1);
                });

                // Latest 2 only
                if (reviews.size() > 2) reviews = reviews.subList(0, 2);

                callback.onSuccess(reviews);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("មិនអាចទាញយកមតិបាន");
            }
        };

        latestReviewsQuery.addValueEventListener(latestReviewsListener);
    }

    // Call this when fragment destroyed / ViewModel cleared
    public void removeLatestReviewsListener() {
        if (latestReviewsQuery != null && latestReviewsListener != null) {
            latestReviewsQuery.removeEventListener(latestReviewsListener);
        }
    }
}