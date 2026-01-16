package com.example.bay.fragment;

import static android.view.View.GONE;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.bay.R;
import com.example.bay.adapter.ProductImageAdapter;
import com.example.bay.adapter.ReviewAdapter;
import com.example.bay.model.ShoppingItem;
import com.example.bay.model.User;
import com.example.bay.viewmodel.ProductDetailViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.*;

public class DetailItemShoppingFragment extends Fragment {
    private static final String TAG = "DetailItemFragment";
    private static final String ARG_SHOPPING_ITEM = "shopping_item";

    // Views
    private Button btnBack;
    private ViewPager2 viewPagerProductImages;
    private LinearLayout dotIndicatorLayout;
    private TextView tvCategory, tvProductName, tvSellerName, tvDate;
    private TextView tvPrice, tvPriceUnit, tvInfoContent;
    private TextView tvReviewSubLabel, tvReviewCount, tvReviewRating;
    private TextView tvPointOfReview;
    private ImageView ivSellerAvatar;
    private Button btnContactPhoneNumber, btnReview;
    private TextView tvGoDetailReview;
    private RecyclerView rvReviews;
    private ImageView ivStar1, ivStar2, ivStar3, ivStar4, ivStar5;

    // Adapters
    private ProductImageAdapter imageAdapter;
    private ReviewAdapter reviewAdapter;

    // ViewModel and Repository
    private ProductDetailViewModel viewModel;
    private ShoppingItem shoppingItem;
    private String itemId;
    private String sellerPhoneNumber;
    private String currentUserId;
    private boolean isUserProductOwner = false;
    private boolean hasInitialButtonStateSet = false;

    // Dot indicators
    private List<ImageView> dots = new ArrayList<>();

    public DetailItemShoppingFragment() {
        // Required empty public constructor
    }

    public static DetailItemShoppingFragment newInstance(ShoppingItem item) {
        DetailItemShoppingFragment fragment = new DetailItemShoppingFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SHOPPING_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        Bundle args = getArguments();
        if (args != null) {
            shoppingItem = args.getParcelable(ARG_SHOPPING_ITEM);
            if (shoppingItem != null) {
                itemId = shoppingItem.getItemId();
                Log.d(TAG, "ShoppingItem received - Name: " + shoppingItem.getName() + ", ID: " + itemId);
            }
        }

        viewModel = new ViewModelProvider(this).get(ProductDetailViewModel.class);

        // Get current user ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        Log.d(TAG, "Current user ID: " + currentUserId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_detail_item_shopping, container, false);
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

        Log.d(TAG, "Setting up product: " + shoppingItem.getName() + ", ID: " + itemId);

        // Check if user is product owner IMMEDIATELY and set button state
        checkIfUserIsProductOwnerAndSetButtonState();

        // Initialize with product data
        viewModel.setProductItem(shoppingItem);
        updateProductUI(shoppingItem);

        // Load seller info
        if (shoppingItem.getUserId() != null && !shoppingItem.getUserId().isEmpty()) {
            Log.d(TAG, "Loading seller info: " + shoppingItem.getUserId());
            viewModel.loadSellerInfo(shoppingItem.getUserId());
        }

        // Set up observers
        observeViewModel();

        // Load reviews and check if current user has reviewed
        // BUT don't show loading for product owners
        Log.d(TAG, "Loading reviews for item: " + itemId + ", user: " + currentUserId);
        viewModel.loadReviews(itemId, currentUserId);
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
        btnReview.setText("ផលិតផលរបស់អ្នក");
        btnReview.setEnabled(false);
        btnReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray));
        btnReview.setTextColor(Color.parseColor("#FFFFFF"));
    }

    private void setInitialButtonStateForNonOwner() {
        // Default state for non-owners
        btnReview.setText("បញ្ចេញមតិ");
        btnReview.setEnabled(currentUserId != null); // Enable only if logged in
        btnReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(),
                currentUserId != null ? R.color.primary : R.color.gray));
        btnReview.setTextColor(Color.parseColor("#FFFFFF"));

    }

    private void initializeViews(View view) {
        Log.d(TAG, "Initializing views");

        btnBack = view.findViewById(R.id.btnBack);
        viewPagerProductImages = view.findViewById(R.id.viewPagerProductImages);
        dotIndicatorLayout = view.findViewById(R.id.dotIndicatorLayout);

        tvCategory = view.findViewById(R.id.tvCategory);
        tvProductName = view.findViewById(R.id.tvProductName);
        tvSellerName = view.findViewById(R.id.tvSellerName);
        tvDate = view.findViewById(R.id.tvDate);

        tvPrice = view.findViewById(R.id.tvPrice);
        tvPriceUnit = view.findViewById(R.id.tvPriceUnit);
        tvInfoContent = view.findViewById(R.id.tvInfoContent);

        tvReviewSubLabel = view.findViewById(R.id.tvReviewSubLabel);
        tvReviewCount = view.findViewById(R.id.ReviewCount);
        tvReviewRating = view.findViewById(R.id.ReviewCalulate);
        tvPointOfReview = view.findViewById(R.id.point_of_review);

        ivSellerAvatar = view.findViewById(R.id.ivSellerAvatar);
        btnContactPhoneNumber = view.findViewById(R.id.btnContactPhoneNumber);
        btnReview = view.findViewById(R.id.BtnReview);
        tvGoDetailReview = view.findViewById(R.id.GoDetailReview);

        rvReviews = view.findViewById(R.id.rvReviews);

        // Rating stars
        ivStar1 = view.findViewById(R.id.ivStar1Summary);
        ivStar2 = view.findViewById(R.id.ivStar2Summary);
        ivStar3 = view.findViewById(R.id.ivStar3Summary);
        ivStar4 = view.findViewById(R.id.ivStar4Summary);
        ivStar5 = view.findViewById(R.id.ivStar5Summary);
    }

    private void setupAdapters() {
        Log.d(TAG, "Setting up adapters");

        // Setup image slider
        imageAdapter = new ProductImageAdapter(new ArrayList<>());
        viewPagerProductImages.setAdapter(imageAdapter);

        // Setup dot indicators
        viewPagerProductImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDots(position);
            }
        });

        reviewAdapter = new ReviewAdapter();
        reviewAdapter.setShowAllReviews(false);

        rvReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        rvReviews.setAdapter(reviewAdapter);
        rvReviews.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> navigateBack());

        btnContactPhoneNumber.setOnClickListener(v -> {
            if (sellerPhoneNumber != null && !sellerPhoneNumber.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + sellerPhoneNumber));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "មិនអាចទាក់ទងទូរស័ព្ទបាន", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "មិនមានលេខទូរស័ព្ទ", Toast.LENGTH_SHORT).show();
            }
        });

        // Review button click listener
        btnReview.setOnClickListener(v -> {
            Log.d(TAG, "Review button clicked");
            Log.d(TAG, "Current user ID: " + currentUserId);
            Log.d(TAG, "Is user product owner: " + isUserProductOwner);

            handleReviewButtonClick();
        });

        tvGoDetailReview.setOnClickListener(v -> {
            Log.d(TAG, "Show all reviews clicked");
            showAllReviewsDialog();
        });
    }

    private void handleReviewButtonClick() {
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

        // Observe seller info
        viewModel.getSellerInfo().observe(getViewLifecycleOwner(), seller -> {
            if (seller != null) {
                Log.d(TAG, "Seller info received: " + seller.getFirst_name());
                updateSellerUI(seller);
            }
        });

        // Observe LATEST reviews (max 2) for the preview section
        viewModel.getLatestReviews().observe(getViewLifecycleOwner(), latestReviews -> {
            Log.d(TAG, "Latest reviews received: " + (latestReviews != null ? latestReviews.size() : 0));
            if (latestReviews != null) {
                reviewAdapter.updateReviews(latestReviews);
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
                btnReview.setEnabled(false);
                btnReview.setText("កំពុងដំណើរការ...");
                btnReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray));
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
                    btnReview.setText("អ្នកបានផ្តល់មតិរួចហើយ");
                    btnReview.setEnabled(false);
                    btnReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray));
                }
            }
        });
    }

    private void updateButtonStateForNonOwner(Boolean hasReviewed) {
        // This method is only called for non-owners

        // Check if user has already reviewed
        if (hasReviewed != null && hasReviewed) {
            btnReview.setText("អ្នកបានផ្តល់មតិរួចហើយ");
            btnReview.setEnabled(false);
            btnReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray));
            return;
        }

        // Check if user is logged in
        if (currentUserId == null || currentUserId.isEmpty()) {
            btnReview.setText("បញ្ចេញមតិ");
            btnReview.setEnabled(false); // Disable if not logged in
            btnReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray));
            return;
        }

        // User can review
        btnReview.setText("បញ្ចេញមតិ");
        btnReview.setEnabled(true);
        btnReview.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));
    }

    private void updateProductUI(ShoppingItem product) {
        Log.d(TAG, "Updating product UI");

        // Basic product info
        tvProductName.setText(product.getName() != null ? product.getName() : "");
        tvPrice.setText(formatPrice(product.getPrice()));
        tvPriceUnit.setText(product.getUnit() != null ? product.getUnit() : "");
        tvInfoContent.setText(product.getDescription() != null ? product.getDescription() : "");

        // Product images
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            Log.d(TAG, "Product has " + product.getImages().size() + " images");
            imageAdapter.updateImages(product.getImages());
            setupDots(product.getImages().size());
        } else {
            Log.d(TAG, "Product has no images");
            List<String> defaultImages = new ArrayList<>();
            defaultImages.add("");
            imageAdapter.updateImages(defaultImages);
            dotIndicatorLayout.setVisibility(GONE);
        }

        // Product date
        if (product.getCreatedAt() != null) {
            String dateStr = formatDate(product.getCreatedAt());
            tvDate.setText("បានដាក់លក់នៅថ្ងៃទី " + dateStr);
        } else {
            tvDate.setText("បានដាក់លក់");
        }

        // Initial rating display
        updateRatingUI(product.getRating());
        updateReviewCountUI(product.getReview_count());
    }

    private void updateSellerUI(User seller) {
        Log.d(TAG, "Updating seller UI");

        // Seller name
        String firstName = seller.getFirst_name() != null ? seller.getFirst_name() : "";
        String lastName = seller.getLast_name() != null ? seller.getLast_name() : "";
        String fullName = (firstName + " " + lastName).trim();
        tvSellerName.setText(fullName.isEmpty() ? "មិនស្គាល់អ្នកលក់" : fullName);

        // Seller avatar
        if (seller.getProfileImageUrl() != null && !seller.getProfileImageUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(seller.getProfileImageUrl())
                    .placeholder(R.drawable.img_avatar)
                    .error(R.drawable.img_avatar)
                    .circleCrop()
                    .into(ivSellerAvatar);
        } else {
            ivSellerAvatar.setImageResource(R.drawable.img_avatar);
        }

        // Seller phone
        sellerPhoneNumber = seller.getPhone();
        if (sellerPhoneNumber != null && !sellerPhoneNumber.isEmpty()) {
            btnContactPhoneNumber.setEnabled(true);
            btnContactPhoneNumber.setText("ទាក់ទងទៅកាន់អ្នកលក់");
        } else {
            btnContactPhoneNumber.setText("មិនមានលេខទូរស័ព្ទ");
            btnContactPhoneNumber.setEnabled(false);
        }

        // Seller points
        tvPointOfReview.setText("168 pts");
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

    private void showAllReviewsDialog() {
        if (shoppingItem == null) {
            Toast.makeText(getContext(), "ទិន្នន័យមិនត្រឹមត្រូវ", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Navigating to DetailReviewAllFragment");

        // Navigate to DetailReviewAllFragment
        DetailReviewAllFragment fragment = DetailReviewAllFragment.newInstance(shoppingItem);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack("reviews")
                .commit();
    }

    private void setupDots(int count) {
        dots.clear();
        dotIndicatorLayout.removeAllViews();

        if (count <= 1) {
            dotIndicatorLayout.setVisibility(GONE);
            return;
        }

        dotIndicatorLayout.setVisibility(View.VISIBLE);

        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);
            dot.setImageResource(R.drawable.product_image_inactive);
            dot.setAdjustViewBounds(true);
            dot.setMaxWidth(dpToPx(12));
            dot.setMaxHeight(dpToPx(12));

            final int position = i;
            dot.setOnClickListener(v -> viewPagerProductImages.setCurrentItem(position, true));

            dots.add(dot);
            dotIndicatorLayout.addView(dot);
        }

        if (!dots.isEmpty()) {
            dots.get(0).setImageResource(R.drawable.product_image_active);
        }
    }

    private void updateDots(int position) {
        for (int i = 0; i < dots.size(); i++) {
            if (i == position) {
                dots.get(i).setImageResource(R.drawable.product_image_active);
            } else {
                dots.get(i).setImageResource(R.drawable.product_image_inactive);
            }
        }
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    // Utility Methods
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private String formatPrice(String price) {
        if (price == null || price.isEmpty()) return "0៛";
        try {
            String cleanPrice = price.replaceAll("[^\\d.]", "");
            double priceValue = Double.parseDouble(cleanPrice);
            if (priceValue == (long) priceValue) {
                return String.format(Locale.getDefault(), "%,d", (long) priceValue) + "៛";
            } else {
                return String.format(Locale.getDefault(), "%,.0f", priceValue) + "៛";
            }
        } catch (NumberFormatException e) {
            return price + "៛";
        }
    }

    private String formatDate(Long timestamp) {
        if (timestamp == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("km"));
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }
}