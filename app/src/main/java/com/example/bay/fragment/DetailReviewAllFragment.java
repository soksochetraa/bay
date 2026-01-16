package com.example.bay.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bay.R;
import com.example.bay.adapter.ReviewAdapter;
import com.example.bay.model.ShoppingItem;
import com.example.bay.viewmodel.ProductDetailViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class DetailReviewAllFragment extends Fragment {
    private static final String TAG = "DetailReviewAllFragment";
    private static final String ARG_SHOPPING_ITEM = "shopping_item";

    // Views
    private Button btnBack;
    private Button btnCreateReview;
    private RecyclerView rvAllReviews;
    private TextView tvReviewSubLabel, tvReviewCount, tvReviewRating;
    private ImageView ivStar1, ivStar2, ivStar3, ivStar4, ivStar5;

    // Adapter
    private ReviewAdapter reviewAdapter;

    // ViewModel
    private ProductDetailViewModel viewModel;
    private ShoppingItem shoppingItem;
    private String itemId;
    private String currentUserId;
    private boolean isUserProductOwner = false;
    private boolean hasInitialButtonStateSet = false;

    public DetailReviewAllFragment() {
        // Required empty public constructor
    }

    public static DetailReviewAllFragment newInstance(ShoppingItem item) {
        DetailReviewAllFragment fragment = new DetailReviewAllFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SHOPPING_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        // Get shopping item from arguments
        Bundle args = getArguments();
        if (args != null) {
            shoppingItem = args.getParcelable(ARG_SHOPPING_ITEM);
            if (shoppingItem != null) {
                itemId = shoppingItem.getItemId();
                Log.d(TAG, "ShoppingItem received - Name: " + shoppingItem.getName() + ", ID: " + itemId);
            }
        }

        // Get ViewModel - use requireActivity() to share ViewModel with parent fragment
        viewModel = new ViewModelProvider(requireActivity()).get(ProductDetailViewModel.class);

        // Get current user ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        Log.d(TAG, "Current user ID: " + currentUserId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_detail_review_all, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        initializeViews(view);
        setupAdapters();
        setupClickListeners();

        // Validate required data
        if (shoppingItem == null || itemId == null || itemId.isEmpty()) {
            Log.e(TAG, "ShoppingItem or Item ID is invalid!");
            Toast.makeText(getContext(), "ទិន្នន័យមិនត្រឹមត្រូវ", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        Log.d(TAG, "Setting up all reviews for product: " + shoppingItem.getName() + ", ID: " + itemId);

        // Check if user is product owner IMMEDIATELY and set button state
        checkIfUserIsProductOwnerAndSetButtonState();

        // Set product item in ViewModel
        viewModel.setProductItem(shoppingItem);

        // Initialize with current rating data
        if (shoppingItem.getRating() > 0) {
            updateRatingUI(shoppingItem.getRating());
            tvReviewRating.setText(String.format(Locale.getDefault(), "%.1f", shoppingItem.getRating()));
        }

        if (shoppingItem.getReview_count() > 0) {
            updateReviewCountUI(shoppingItem.getReview_count());
        }

        // Set up observers
        observeViewModel();

        // Load all reviews
        viewModel.loadAllReviews(itemId, currentUserId);
    }

    private void checkIfUserIsProductOwnerAndSetButtonState() {
        Log.d(TAG, "Checking if user is product owner");
        Log.d(TAG, "Current user ID: " + currentUserId);
        Log.d(TAG, "Product owner ID: " + shoppingItem.getUserId());

        if (currentUserId != null && shoppingItem.getUserId() != null &&
                currentUserId.equals(shoppingItem.getUserId())) {
            isUserProductOwner = true;
            Log.d(TAG, "User IS product owner");

            // Immediately set button state for product owner
            setButtonStateForProductOwner();
            hasInitialButtonStateSet = true;
        } else {
            isUserProductOwner = false;
            Log.d(TAG, "User is NOT product owner");

            // For non-owners, set initial state
            setInitialButtonStateForNonOwner();
            hasInitialButtonStateSet = true;
        }
    }

    private void setButtonStateForProductOwner() {
        btnCreateReview.setText("ផលិតផលរបស់អ្នក");
        btnCreateReview.setEnabled(false);
        btnCreateReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray));
        btnCreateReview.setTextColor(Color.parseColor("#FFFFFF"));
    }

    private void setInitialButtonStateForNonOwner() {
        // Default state for non-owners
        btnCreateReview.setText("បញ្ចេញមតិ");
        btnCreateReview.setEnabled(currentUserId != null);
        btnCreateReview.setTextColor(Color.parseColor("#FFFFFF"));
        btnCreateReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(),
                currentUserId != null ? R.color.primary : R.color.gray));
    }

    private void initializeViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnCreateReview = view.findViewById(R.id.btn_create_review);
        rvAllReviews = view.findViewById(R.id.rvAllReviews);

        tvReviewSubLabel = view.findViewById(R.id.tvReviewSubLabel);
        tvReviewCount = view.findViewById(R.id.ReviewCount);
        tvReviewRating = view.findViewById(R.id.ReviewCalulate);

        // Rating stars
        ivStar1 = view.findViewById(R.id.ivStar1Summary);
        ivStar2 = view.findViewById(R.id.ivStar2Summary);
        ivStar3 = view.findViewById(R.id.ivStar3Summary);
        ivStar4 = view.findViewById(R.id.ivStar4Summary);
        ivStar5 = view.findViewById(R.id.ivStar5Summary);
    }

    private void setupAdapters() {
        reviewAdapter = new ReviewAdapter();
        reviewAdapter.setShowAllReviews(true); // Show all reviews in this fragment

        rvAllReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAllReviews.setAdapter(reviewAdapter);
        rvAllReviews.setNestedScrollingEnabled(true);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> navigateBack());

        btnCreateReview.setOnClickListener(v -> handleReviewButtonClick());
    }

    private void handleReviewButtonClick() {
        Log.d(TAG, "Create review button clicked");
        Log.d(TAG, "Current user ID: " + currentUserId);
        Log.d(TAG, "Is user product owner: " + isUserProductOwner);

        // Check if user is logged in
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "User not logged in");
            Toast.makeText(getContext(), "សូមចូលគណនីជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is the product owner (using local check)
        if (isUserProductOwner) {
            Log.d(TAG, "User is product owner, cannot review own product");
            Toast.makeText(
                    getContext(),
                    "អ្នកមិនអាចផ្តល់មតិលើផលិតផលរបស់អ្នកបានទេ",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Check if user has already reviewed (from ViewModel)
        Boolean hasReviewed = viewModel.getHasUserReviewed().getValue();
        if (hasReviewed != null && hasReviewed) {
            Log.d(TAG, "User has already reviewed this product");
            Toast.makeText(getContext(), "អ្នកបានផ្តល់មតិរួចហើយ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show review dialog
        showReviewDialog();
    }

    private void observeViewModel() {
        Log.d(TAG, "Setting up ViewModel observers");

        // Observe ALL reviews
        viewModel.getAllReviews().observe(getViewLifecycleOwner(), allReviews -> {
            Log.d(TAG, "All reviews received: " + (allReviews != null ? allReviews.size() : 0));
            if (allReviews != null) {
                reviewAdapter.updateReviews(allReviews);
            }
        });

        // Observe review users
        viewModel.getReviewUsers().observe(getViewLifecycleOwner(), userMap -> {
            Log.d(TAG, "Review users received: " + (userMap != null ? userMap.size() : 0));
            if (userMap != null) {
                reviewAdapter.updateUserData(userMap);
            }
        });

        // Observe product rating
        viewModel.getProductRating().observe(getViewLifecycleOwner(), rating -> {
            Log.d(TAG, "Product rating updated: " + rating);
            if (rating != null) {
                updateRatingUI(rating);
                tvReviewRating.setText(String.format(Locale.getDefault(), "%.1f", rating));
            }
        });

        // Observe product review count
        viewModel.getProductReviewCount().observe(getViewLifecycleOwner(), count -> {
            Log.d(TAG, "Product review count updated: " + count);
            if (count != null) {
                updateReviewCountUI(count);
            }
        });

        // Observe if user has reviewed (only relevant if user is NOT product owner)
        viewModel.getHasUserReviewed().observe(getViewLifecycleOwner(), hasReviewed -> {
            Log.d(TAG, "Has user reviewed updated: " + hasReviewed);
            if (!isUserProductOwner && hasInitialButtonStateSet) {
                updateButtonStateForNonOwner(hasReviewed);
            }
        });

        // Observe loading state - SKIP FOR PRODUCT OWNERS
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Loading state: " + isLoading);

            // If user is product owner, ignore loading state
            if (isUserProductOwner) {
                return;
            }

            if (isLoading != null && isLoading) {
                btnCreateReview.setEnabled(false);
                btnCreateReview.setText("កំពុងដំណើរការ...");
                btnCreateReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray));
            } else {
                // Update button state when loading is complete
                if (hasInitialButtonStateSet) {
                    Boolean hasReviewed = viewModel.getHasUserReviewed().getValue();
                    updateButtonStateForNonOwner(hasReviewed);
                }
            }
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Error from ViewModel: " + error);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe success messages
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), success -> {
            if (success != null && !success.isEmpty()) {
                Log.d(TAG, "Success from ViewModel: " + success);
                Toast.makeText(getContext(), success, Toast.LENGTH_SHORT).show();

                // Update button state after successful review submission
                if (!isUserProductOwner) {
                    btnCreateReview.setText("អ្នកបានផ្តល់មតិរួចហើយ");
                    btnCreateReview.setEnabled(false);
                    btnCreateReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray));
                }
            }
        });
    }

    private void updateButtonStateForNonOwner(Boolean hasReviewed) {
        // This method is only called for non-owners

        // Check if user has already reviewed
        if (hasReviewed != null && hasReviewed) {
            btnCreateReview.setText("អ្នកបានផ្តល់មតិរួចហើយ");
            btnCreateReview.setEnabled(false);
            btnCreateReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray));
            return;
        }

        // Check if user is logged in
        if (currentUserId == null || currentUserId.isEmpty()) {
            btnCreateReview.setText("បញ្ចេញមតិ");
            btnCreateReview.setEnabled(false); // Disable if not logged in
            btnCreateReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray));
            return;
        }

        // User can review
        btnCreateReview.setText("បញ្ចេញមតិ");
        btnCreateReview.setEnabled(true);
        btnCreateReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));
    }

    private void updateRatingUI(float rating) {
        Log.d(TAG, "Updating rating UI: " + rating);

        if (ivStar1 == null || ivStar2 == null || ivStar3 == null ||
                ivStar4 == null || ivStar5 == null) {
            Log.e(TAG, "Rating stars are null!");
            return;
        }

        int fullStars = (int) rating;
        ivStar1.setImageResource(fullStars >= 1 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        ivStar2.setImageResource(fullStars >= 2 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        ivStar3.setImageResource(fullStars >= 3 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        ivStar4.setImageResource(fullStars >= 4 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        ivStar5.setImageResource(fullStars >= 5 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
    }

    private void updateReviewCountUI(int count) {
        Log.d(TAG, "Updating review count UI: " + count);

        if (count > 0) {
            tvReviewCount.setText(String.format(Locale.getDefault(), "(%d reviews)", count));
            tvReviewSubLabel.setText("អ្នកលក់ទទួលបានការវាយតម្លៃរហូតដល់");
        } else {
            tvReviewCount.setText("(មិនទាន់មានមតិ)");
            tvReviewSubLabel.setText("មិនទាន់មានការវាយតម្លៃ");
        }
    }

    // Review Dialog
    private void showReviewDialog() {
        Log.d(TAG, "Showing review dialog for itemId: " + itemId);

        View dialogView = getLayoutInflater().inflate(R.layout.item_create_review_product, null);

        ImageView ivStar1Dialog = dialogView.findViewById(R.id.ivStar1);
        ImageView ivStar2Dialog = dialogView.findViewById(R.id.ivStar2);
        ImageView ivStar3Dialog = dialogView.findViewById(R.id.ivStar3);
        ImageView ivStar4Dialog = dialogView.findViewById(R.id.ivStar4);
        ImageView ivStar5Dialog = dialogView.findViewById(R.id.ivStar5);
        TextInputEditText etComment = dialogView.findViewById(R.id.etComment);

        final float[] selectedRating = {0};

        // Star click listeners
        View.OnClickListener starClickListener = v -> {
            int rating = 0;
            if (v.getId() == R.id.ivStar1) rating = 1;
            else if (v.getId() == R.id.ivStar2) rating = 2;
            else if (v.getId() == R.id.ivStar3) rating = 3;
            else if (v.getId() == R.id.ivStar4) rating = 4;
            else if (v.getId() == R.id.ivStar5) rating = 5;

            selectedRating[0] = rating;
            updateDialogStars(ivStar1Dialog, ivStar2Dialog, ivStar3Dialog,
                    ivStar4Dialog, ivStar5Dialog, rating);

            Log.d(TAG, "Rating selected: " + rating);
        };

        ivStar1Dialog.setOnClickListener(starClickListener);
        ivStar2Dialog.setOnClickListener(starClickListener);
        ivStar3Dialog.setOnClickListener(starClickListener);
        ivStar4Dialog.setOnClickListener(starClickListener);
        ivStar5Dialog.setOnClickListener(starClickListener);

        // Create dialog with custom background
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("បញ្ចេញមតិ")
                .setView(dialogView)
                .setPositiveButton("បញ្ជូន", (dialog, which) -> {
                    String comment = etComment.getText().toString().trim();
                    Log.d(TAG, "Submitting review - ItemId: " + itemId +
                            ", UserId: " + currentUserId +
                            ", Rating: " + selectedRating[0] +
                            ", Comment: " + comment);

                    if (selectedRating[0] == 0) {
                        Toast.makeText(getContext(), "សូមជ្រើសរើសការវាយតម្លៃ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (comment.isEmpty()) {
                        Toast.makeText(getContext(), "សូមបញ្ចូលមតិ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Submit review
                    viewModel.submitReview(itemId, currentUserId, selectedRating[0], comment);
                })
                .setNegativeButton("បោះបង់", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // Set background color
        dialog.getWindow().setBackgroundDrawableResource(R.color.white);

        // Show the dialog
        dialog.show();

        // Customize title
        TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            titleView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }

        // Customize buttons
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setAllCaps(false);
            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            positiveButton.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
            positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }

        if (negativeButton != null) {
            negativeButton.setAllCaps(false);
            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            negativeButton.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
            negativeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }

        // Customize message text if any
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            messageView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
        }
    }
    private void updateDialogStars(ImageView ivStar1, ImageView ivStar2, ImageView ivStar3,
                                   ImageView ivStar4, ImageView ivStar5, int rating) {
        ivStar1.setImageResource(rating >= 1 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        ivStar2.setImageResource(rating >= 2 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        ivStar3.setImageResource(rating >= 3 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        ivStar4.setImageResource(rating >= 4 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
        ivStar5.setImageResource(rating >= 5 ?
                R.drawable.ic_star_filled : R.drawable.ic_star_unfilled);
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        // Refresh data when fragment resumes
        if (itemId != null && !itemId.isEmpty()) {
            viewModel.loadAllReviews(itemId, currentUserId);
        }
    }
}