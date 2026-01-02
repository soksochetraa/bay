package com.example.bay.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;

import com.example.bay.model.Review;
import com.example.bay.model.ShoppingItem;
import com.example.bay.model.User;
import com.example.bay.repository.ProductDetailRepository;
import com.example.bay.repository.ReviewRepository;

import java.util.*;

public class ProductDetailViewModel extends ViewModel {
    private static final String TAG = "ProductDetailViewModel";

    private final ProductDetailRepository productDetailRepository;
    private final ReviewRepository reviewRepository;

    // LiveData for UI
    private final MutableLiveData<User> sellerInfo = new MutableLiveData<>();
    private final MutableLiveData<List<Review>> latestReviews = new MutableLiveData<>();
    private final MutableLiveData<List<Review>> allReviews = new MutableLiveData<>(); // Add this line
    private final MutableLiveData<Map<String, User>> reviewUsers = new MutableLiveData<>();
    private final MutableLiveData<ShoppingItem> productItem = new MutableLiveData<>();
    private final MutableLiveData<Float> productRating = new MutableLiveData<>(0.0f);
    private final MutableLiveData<Integer> productReviewCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasUserReviewed = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isProductOwner = new MutableLiveData<>(false);

    public ProductDetailViewModel() {
        productDetailRepository = new ProductDetailRepository();
        reviewRepository = new ReviewRepository();
    }

    // Set current product item
    public void setProductItem(ShoppingItem item) {
        Log.d(TAG, "setProductItem: " + (item != null ? item.getName() : "null"));
        productItem.setValue(item);

        if (item != null) {
            productRating.setValue(item.getRating());
            productReviewCount.setValue(item.getReview_count());
        }
    }

    // Load seller info
    public void loadSellerInfo(String userId) {
        if (userId == null || userId.isEmpty()) {
            errorMessage.setValue("ទិន្នន័យមិនត្រឹមត្រូវ");
            return;
        }

        isLoading.setValue(true);
        productDetailRepository.getUserById(userId, new ProductDetailRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User user) {
                sellerInfo.setValue(user);
                isLoading.setValue(false);
                Log.d(TAG, "Seller info loaded: " + user.getFirst_name());
            }

            @Override
            public void onError(String errorMsg) {
                errorMessage.setValue("មិនអាចទាញយកព័ត៌មានអ្នកលក់បាន");
                isLoading.setValue(false);
                Log.e(TAG, "Failed to load seller: " + errorMsg);
            }
        });
    }

    // Check if current user is product owner
    public void checkProductOwner(String itemId, String currentUserId) {
        if (itemId == null || itemId.isEmpty() || currentUserId == null || currentUserId.isEmpty()) {
            isProductOwner.setValue(false);
            return;
        }

        Log.d(TAG, "Checking if user is product owner - Item: " + itemId + ", User: " + currentUserId);

        reviewRepository.checkProductOwner(itemId, currentUserId, new ReviewRepository.ReviewCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isOwner) {
                Log.d(TAG, "User is product owner: " + isOwner);
                isProductOwner.setValue(isOwner);

                // If user is owner, they cannot review
                if (isOwner) {
                    hasUserReviewed.setValue(true);
                }
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "Error checking product owner: " + errorMsg);
                isProductOwner.setValue(false);
            }
        });
    }

    // Load reviews and check if current user has reviewed (for main fragment - shows 2 reviews)
    public void loadReviews(String itemId, String currentUserId) {
        Log.d(TAG, "loadReviews called for item: " + itemId);

        if (itemId == null || itemId.isEmpty()) {
            errorMessage.setValue("ទិន្នន័យមិនត្រឹមត្រូវ");
            return;
        }

        isLoading.setValue(true);

        // First check if user is product owner (only if user is logged in)
        if (currentUserId != null && !currentUserId.isEmpty()) {
            checkProductOwner(itemId, currentUserId);
        } else {
            Log.d(TAG, "User not logged in, skipping owner check");
        }

        // Load latest 2 reviews
        reviewRepository.getLatestReviews(itemId, new ReviewRepository.ReviewCallback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> latestReviewList) {
                Log.d(TAG, "Loaded " + latestReviewList.size() + " latest reviews");
                latestReviews.setValue(latestReviewList);

                // Load user info for these reviews
                loadUsersForReviews(latestReviewList);

                // Check if current user has reviewed (only if not owner and logged in)
                Boolean isOwner = isProductOwner.getValue();
                if (currentUserId != null && !currentUserId.isEmpty() &&
                        (isOwner == null || !isOwner)) {
                    checkUserHasReviewed(itemId, currentUserId);
                } else if (isOwner != null && isOwner) {
                    // If user is owner, set hasReviewed to true to disable button
                    hasUserReviewed.setValue(true);
                }

                // Load product stats
                loadProductStats(itemId);

                isLoading.setValue(false);
            }

            @Override
            public void onError(String errorMsg) {
                latestReviews.setValue(new ArrayList<>());
                errorMessage.setValue("មិនអាចទាញយកមតិបាន");
                isLoading.setValue(false);
                Log.e(TAG, "Error loading reviews: " + errorMsg);
            }
        });
    }

    // Load ALL reviews (for DetailReviewAllFragment)
    public void loadAllReviews(String itemId, String currentUserId) {
        Log.d(TAG, "loadAllReviews called for item: " + itemId);

        if (itemId == null || itemId.isEmpty()) {
            errorMessage.setValue("ទិន្នន័យមិនត្រឹមត្រូវ");
            return;
        }

        isLoading.setValue(true);

        // First check if user is product owner (only if user is logged in)
        if (currentUserId != null && !currentUserId.isEmpty()) {
            checkProductOwner(itemId, currentUserId);
        }

        // Load ALL reviews
        reviewRepository.getAllReviews(itemId, new ReviewRepository.ReviewCallback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> allReviewList) {
                Log.d(TAG, "Loaded " + allReviewList.size() + " all reviews");
                allReviews.setValue(allReviewList);

                // Load user info for these reviews
                loadUsersForReviews(allReviewList);

                // Check if current user has reviewed (only if not owner and logged in)
                Boolean isOwner = isProductOwner.getValue();
                if (currentUserId != null && !currentUserId.isEmpty() &&
                        (isOwner == null || !isOwner)) {
                    checkUserHasReviewed(itemId, currentUserId);
                }

                // Load product stats
                loadProductStats(itemId);

                isLoading.setValue(false);
            }

            @Override
            public void onError(String errorMsg) {
                allReviews.setValue(new ArrayList<>());
                errorMessage.setValue("មិនអាចទាញយកមតិបាន");
                isLoading.setValue(false);
                Log.e(TAG, "Error loading all reviews: " + errorMsg);
            }
        });
    }

    // Load users for reviews
    private void loadUsersForReviews(List<Review> reviewList) {
        List<String> userIds = new ArrayList<>();
        for (Review review : reviewList) {
            if (review.getUserId() != null && !userIds.contains(review.getUserId())) {
                userIds.add(review.getUserId());
            }
        }

        if (!userIds.isEmpty()) {
            productDetailRepository.getUsersByIds(userIds, new ProductDetailRepository.UsersMapCallback() {
                @Override
                public void onSuccess(Map<String, User> usersMap) {
                    reviewUsers.setValue(usersMap);
                    Log.d(TAG, "Loaded " + usersMap.size() + " users for reviews");
                }

                @Override
                public void onError(String errorMsg) {
                    reviewUsers.setValue(new HashMap<>());
                    Log.e(TAG, "Error loading users: " + errorMsg);
                }
            });
        } else {
            reviewUsers.setValue(new HashMap<>());
        }
    }

    // Check if user has already reviewed this item
    private void checkUserHasReviewed(String itemId, String userId) {
        Log.d(TAG, "checkUserHasReviewed - itemId: " + itemId + ", userId: " + userId);

        reviewRepository.checkUserReview(itemId, userId, new ReviewRepository.ReviewCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean hasReviewed) {
                Log.d(TAG, "User has reviewed: " + hasReviewed);
                hasUserReviewed.setValue(hasReviewed);
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "Error checking user review: " + errorMsg);
                hasUserReviewed.setValue(false);
            }
        });
    }

    // Load product stats
    private void loadProductStats(String itemId) {
        reviewRepository.getProductStats(itemId, new ReviewRepository.ReviewCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> stats) {
                float rating = stats.get("rating") != null ? (float) stats.get("rating") : 0.0f;
                int count = stats.get("review_count") != null ? (int) stats.get("review_count") : 0;

                productRating.setValue(rating);
                productReviewCount.setValue(count);

                Log.d(TAG, "Product stats loaded - Rating: " + rating + ", Count: " + count);

                // Update the product item with new stats
                ShoppingItem currentItem = productItem.getValue();
                if (currentItem != null) {
                    currentItem.setRating(rating);
                    currentItem.setReview_count(count);
                    productItem.setValue(currentItem);
                }
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "Error loading product stats: " + errorMsg);
            }
        });
    }

    // Submit a new review
    public void submitReview(String itemId, String userId, float rating, String comment) {
        Log.d(TAG, "submitReview called - item: " + itemId + ", user: " + userId);

        if (itemId == null || itemId.isEmpty() || userId == null || userId.isEmpty()) {
            errorMessage.setValue("ទិន្នន័យមិនត្រឹមត្រូវ");
            return;
        }

        isLoading.setValue(true);
        reviewRepository.submitReview(itemId, userId, rating, comment.trim(),
                new ReviewRepository.ReviewCallback<String>() {
                    @Override
                    public void onSuccess(String successMsg) {
                        successMessage.setValue(successMsg);
                        hasUserReviewed.setValue(true);

                        // Reload reviews to update UI
                        loadReviews(itemId, userId);
                        loadAllReviews(itemId, userId); // Also reload all reviews

                        Log.d(TAG, "Review submitted successfully");
                    }

                    @Override
                    public void onError(String errorMsg) {
                        errorMessage.setValue(errorMsg);
                        isLoading.setValue(false);
                        Log.e(TAG, "Error submitting review: " + errorMsg);
                    }
                });
    }

    // Getters for LiveData
    public LiveData<User> getSellerInfo() { return sellerInfo; }
    public LiveData<List<Review>> getLatestReviews() { return latestReviews; }
    public LiveData<List<Review>> getAllReviews() { return allReviews; } // Add this getter
    public LiveData<Map<String, User>> getReviewUsers() { return reviewUsers; }
    public LiveData<ShoppingItem> getProductItem() { return productItem; }
    public LiveData<Float> getProductRating() { return productRating; }
    public LiveData<Integer> getProductReviewCount() { return productReviewCount; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Boolean> getHasUserReviewed() { return hasUserReviewed; }
    public LiveData<Boolean> getIsProductOwner() { return isProductOwner; }
}