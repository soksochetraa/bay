package com.example.bay;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.bumptech.glide.Glide;
import com.example.bay.model.ShoppingItem;
import com.example.bay.viewmodel.ShoppingViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddShoppingItemActivity extends AppCompatActivity {
    private static final String TAG = "AddItemActivity";
    private static final int PICK_IMAGES_REQUEST = 100;
    private static final int STORAGE_PERMISSION_REQUEST = 101;
    private static final int MAX_IMAGES = 4;

    private static final String EXTRA_EDIT_ITEM = "edit_item";
    private static final int MINIMUM_PRICE = 500; // Minimum price requirement

    private ShoppingViewModel viewModel;
    private LinearLayout imagesContainer;
    private EditText etItemName, etPrice, etUnit, etDescription;
    private Button btnAddImages, btnSubmit;
    private TextView tvTitle;

    private List<Uri> selectedImages = new ArrayList<>();
    private List<String> existingImageUrls = new ArrayList<>();

    private FirebaseStorage storage;
    private StorageReference storageReference;

    private LinearLayout dropdownContainer;
    private TextView tvSelectedCategory;
    private ImageView ivDropdownArrow;
    private RelativeLayout btnSelectCategory;
    private LinearLayout categoryContainer;
    private boolean isDropdownOpen = false;
    private String selectedCategory = "";

    private String[] categories = {
            "បន្លែ",
            "ផ្លែឈើ",
            "សម្ភារៈ",
            "គ្រាប់ពូជ",
            "ជី",
            "ថ្នាំ",
            "សម្ភារៈវេជ្ជសាស្ត្រ",
            "ផ្សេងៗ"
    };

    private ShoppingItem editItem = null;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        Log.d(TAG, "onCreate called");

        if (getIntent() != null) {
            Log.d(TAG, "Intent received");
            if (getIntent().hasExtra(EXTRA_EDIT_ITEM)) {
                Log.d(TAG, "Intent has EXTRA_EDIT_ITEM");
                editItem = getIntent().getParcelableExtra(EXTRA_EDIT_ITEM);
                Log.d(TAG, "Edit item parceled: " + (editItem != null ? editItem.getName() : "null"));
            } else {
                Log.d(TAG, "Intent does NOT have EXTRA_EDIT_ITEM");
            }
        }

        storage = FirebaseStorage.getInstance("gs://baydigitalecosystemmobileapp.firebasestorage.app");
        storageReference = storage.getReference();

        viewModel = new ShoppingViewModel();
        initializeViews();
        setupCategoryDropdown();
        clearSampleImages();

        checkEditMode();
        setupListeners();
        setupPriceValidation();
    }

    private void checkEditMode() {
        Log.d(TAG, "checkEditMode called");
        if (getIntent() != null && getIntent().hasExtra(EXTRA_EDIT_ITEM)) {
            editItem = getIntent().getParcelableExtra(EXTRA_EDIT_ITEM);
            if (editItem != null) {
                isEditMode = true;
                Log.d(TAG, "Edit mode is ON for item: " + editItem.getName());
                Log.d(TAG, "Item ID: " + editItem.getItemId());
                Log.d(TAG, "Item Images: " + (editItem.getImages() != null ? editItem.getImages().size() : 0));
                Log.d(TAG, "Full item: " + editItem.toString());
                populateEditData();
            } else {
                Log.e(TAG, "Edit item is null after parceling!");
                isEditMode = false;
            }
        } else {
            Log.d(TAG, "Edit mode is OFF - creating new item");
            isEditMode = false;
        }

        Log.d(TAG, "isEditMode final value: " + isEditMode);
    }

    private void setupPriceValidation() {
        etPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String priceStr = s.toString().trim();
                if (!priceStr.isEmpty()) {
                    try {
                        int price = Integer.parseInt(priceStr);
                        if (price < MINIMUM_PRICE) {
                            etPrice.setError("តម្លៃយ៉ាងហោចណាស់ក៏៥០០៛ដែរ");
                            btnSubmit.setEnabled(false);
                            btnSubmit.setBackgroundColor(Color.parseColor("#CCCCCC")); // Grey out button
                        } else {
                            etPrice.setError(null);
                            btnSubmit.setEnabled(true);
                            // Restore original button color (use your primary color)
                            btnSubmit.setBackgroundColor(ContextCompat.getColor(AddShoppingItemActivity.this, R.color.primary));
                        }
                    } catch (NumberFormatException e) {
                        etPrice.setError("តម្លៃមិនត្រឹមត្រូវ");
                        btnSubmit.setEnabled(false);
                        btnSubmit.setBackgroundColor(Color.parseColor("#CCCCCC"));
                    }
                } else {
                    etPrice.setError(null);
                    btnSubmit.setEnabled(true);
                    btnSubmit.setBackgroundColor(ContextCompat.getColor(AddShoppingItemActivity.this, R.color.primary));
                }
            }
        });
    }

    private void populateEditData() {
        if (editItem == null) return;

        Log.d(TAG, "populateEditData for item: " + editItem.getName());

        if (tvTitle != null) {
            tvTitle.setText("កែប្រែទំនិញ");
        }

        etItemName.setText(editItem.getName());
        etPrice.setText(editItem.getPrice());
        etUnit.setText(editItem.getUnit());
        etDescription.setText(editItem.getDescription());

        selectedCategory = editItem.getCategory();
        if (selectedCategory != null && !selectedCategory.isEmpty()) {
            tvSelectedCategory.setText(selectedCategory);
            tvSelectedCategory.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            Log.d(TAG, "Category set to: " + selectedCategory);
        }

        if (editItem.getImages() != null && !editItem.getImages().isEmpty()) {
            existingImageUrls.addAll(editItem.getImages());
            Log.d(TAG, "Existing images count: " + existingImageUrls.size());
            for (String imageUrl : existingImageUrls) {
                addImageFromUrl(imageUrl);
            }
        }

        btnSubmit.setText("រក្សាទុកការផ្លាស់ប្តូរ");

        Log.d(TAG, "Edit data populated successfully");
    }

    private void addImageFromUrl(String imageUrl) {
        View imageView = getLayoutInflater().inflate(R.layout.image_border_item_shopping, null);
        ImageView ivImage = imageView.findViewById(R.id.ivSelectedImage);
        ImageView ivRemove = imageView.findViewById(R.id.ivRemove);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.img)
                .centerCrop()
                .into(ivImage);

        imageView.setTag("existing:" + imageUrl);

        ivRemove.setOnClickListener(v -> {
            imagesContainer.removeView(imageView);

            if (imageView.getTag() != null && imageView.getTag().toString().startsWith("existing:")) {
                String url = imageUrl.replace("existing:", "");
                existingImageUrls.remove(url);
                Log.d(TAG, "Removed existing image: " + url);
            }
        });

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                dpToPx(110),
                dpToPx(110)
        );
        layoutParams.setMargins(0, 0, dpToPx(12), 0);

        imageView.setLayoutParams(layoutParams);
        imagesContainer.addView(imageView);
    }

    private void initializeViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        imagesContainer = findViewById(R.id.imagesContainer);
        btnAddImages = findViewById(R.id.btnAddImages);

        etItemName = findViewById(R.id.etItemName);
        etPrice = findViewById(R.id.etPrice);
        etUnit = findViewById(R.id.etUnit);
        etDescription = findViewById(R.id.etDescription);
        btnSubmit = findViewById(R.id.btnSubmit);

        dropdownContainer = findViewById(R.id.dropdownContainer);
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory);
        ivDropdownArrow = findViewById(R.id.ivDropdownArrow);
        btnSelectCategory = findViewById(R.id.btnSelectCategory);
        categoryContainer = findViewById(R.id.categoryContainer);

        try {
            tvTitle = findViewById(R.id.tvTitle);
            Log.d(TAG, "tvTitle found");
        } catch (Exception e) {
            tvTitle = null;
            Log.d(TAG, "tvTitle not found in layout");
        }

        if (isEditMode) {
            btnSubmit.setText("រក្សាទុកការផ្លាស់ប្តូរ");
        } else {
            btnSubmit.setText("ដាក់ស្នើរ");
        }
    }

    private void setupCategoryDropdown() {
        dropdownContainer.removeAllViews();

        for (int i = 0; i < categories.length; i++) {
            String category = categories[i];

            TextView categoryItem = new TextView(this);
            categoryItem.setText(category);
            categoryItem.setTag(category);
            categoryItem.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
            categoryItem.setTypeface(ResourcesCompat.getFont(this, R.font.kantumruypro_medium));
            categoryItem.setTextSize(15);
            categoryItem.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            categoryItem.setBackgroundResource(R.drawable.category_item_background);
            categoryItem.setClickable(true);

            if (i < categories.length - 1) {
                View divider = new View(this);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                );
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
                dropdownContainer.addView(divider);
            }

            categoryItem.setOnClickListener(v -> {
                selectedCategory = category;
                tvSelectedCategory.setText(category);
                tvSelectedCategory.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                closeDropdown();
                Toast.makeText(this, "បានជ្រើសរើស: " + category, Toast.LENGTH_SHORT).show();
            });

            dropdownContainer.addView(categoryItem);
        }

        if (categories.length > 0 && !isEditMode) {
            selectedCategory = categories[0];
            tvSelectedCategory.setText(selectedCategory);
            tvSelectedCategory.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
    }

    private void clearSampleImages() {
        imagesContainer.removeAllViews();
    }

    private void setupListeners() {
        btnSelectCategory.setOnClickListener(v -> {
            if (isDropdownOpen) {
                closeDropdown();
            } else {
                openDropdown();
            }
        });

        findViewById(R.id.main).setOnTouchListener((v, event) -> {
            if (isDropdownOpen) {
                closeDropdown();
            }
            return false;
        });

        btnAddImages.setOnClickListener(v -> {
            if (selectedImages.size() + existingImageUrls.size() >= MAX_IMAGES) {
                Toast.makeText(this, "អ្នកអាចបញ្ចូលរូបភាពបានត្រឹម " + MAX_IMAGES + " រូប", Toast.LENGTH_SHORT).show();
                return;
            }

            if (checkStoragePermission()) {
                openImagePicker();
            } else {
                requestStoragePermission();
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Submit button clicked");
                Log.d(TAG, "isEditMode value at click: " + isEditMode);
                Log.d(TAG, "editItem is null? " + (editItem == null));

                if (validateInput()) {
                    if (isEditMode) {
                        Log.d(TAG, "Calling updateShoppingItem()");
                        updateShoppingItem();
                    } else {
                        Log.d(TAG, "Calling createShoppingItem()");
                        createShoppingItem();
                    }
                }
            }
        });
    }

    private void openDropdown() {
        isDropdownOpen = true;
        animateDropdown(true);
        ivDropdownArrow.setImageResource(R.drawable.ic_chevron_up);
        btnSelectCategory.setBackgroundResource(R.drawable.input_field_background_active);
    }

    private void closeDropdown() {
        isDropdownOpen = false;
        animateDropdown(false);
        ivDropdownArrow.setImageResource(R.drawable.ic_chevron_down);
        btnSelectCategory.setBackgroundResource(R.drawable.input_field_background);
    }

    private void animateDropdown(boolean show) {
        if (show) {
            dropdownContainer.setAlpha(0f);
            dropdownContainer.setVisibility(View.VISIBLE);
            dropdownContainer.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setListener(null);
        } else {
            dropdownContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            dropdownContainer.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    STORAGE_PERMISSION_REQUEST);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "ការអនុញ្ញាតត្រូវបានបដិសេធ។ មិនអាចជ្រើសរូបភាពបានទេ។", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "ជ្រើសរូបភាព"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count && (selectedImages.size() + existingImageUrls.size()) < MAX_IMAGES; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        selectedImages.add(imageUri);
                        addImageToContainer(imageUri);
                    }

                    if (selectedImages.size() + existingImageUrls.size() >= MAX_IMAGES) {
                        Toast.makeText(this, "បានឈប់ជ្រើសរូបភាពនៅ " + MAX_IMAGES + " រូប", Toast.LENGTH_SHORT).show();
                    }
                } else if (data.getData() != null) {
                    Uri imageUri = data.getData();
                    selectedImages.add(imageUri);
                    addImageToContainer(imageUri);
                }
            }
        }
    }

    private void addImageToContainer(Uri imageUri) {
        View imageView = getLayoutInflater().inflate(R.layout.image_border_item_shopping, null);
        ImageView ivImage = imageView.findViewById(R.id.ivSelectedImage);
        ImageView ivRemove = imageView.findViewById(R.id.ivRemove);

        Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.img)
                .centerCrop()
                .into(ivImage);

        final int position = selectedImages.size() - 1;
        ivRemove.setOnClickListener(v -> {
            selectedImages.remove(position);
            imagesContainer.removeView(imageView);
            updateRemoveButtonPositions();
        });

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                dpToPx(110),
                dpToPx(110)
        );
        layoutParams.setMargins(0, 0, dpToPx(12), 0);

        imageView.setLayoutParams(layoutParams);
        imagesContainer.addView(imageView);
    }

    private void updateRemoveButtonPositions() {
        for (int i = 0; i < imagesContainer.getChildCount(); i++) {
            View imageView = imagesContainer.getChildAt(i);
            ImageView ivRemove = imageView.findViewById(R.id.ivRemove);

            final int position = i;
            ivRemove.setOnClickListener(v -> {
                Object tag = imageView.getTag();
                if (tag != null && tag.toString().startsWith("existing:")) {
                    String url = tag.toString().replace("existing:", "");
                    existingImageUrls.remove(url);
                } else {
                    if (position < selectedImages.size()) {
                        selectedImages.remove(position);
                    }
                }
                imagesContainer.removeViewAt(position);
                updateRemoveButtonPositions();
            });
        }
    }

    private boolean validateInput() {
        if (etItemName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "សូមបញ្ចូលឈ្មោះទំនិញ", Toast.LENGTH_SHORT).show();
            etItemName.requestFocus();
            return false;
        }

        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, "សូមជ្រើសរើសប្រភេទ", Toast.LENGTH_SHORT).show();
            btnSelectCategory.requestFocus();
            return false;
        }

        String priceStr = etPrice.getText().toString().trim();
        if (priceStr.isEmpty()) {
            Toast.makeText(this, "សូមបញ្ចូលតម្លៃ", Toast.LENGTH_SHORT).show();
            etPrice.requestFocus();
            return false;
        }

        try {
            int price = Integer.parseInt(priceStr);
            if (price < MINIMUM_PRICE) {
                etPrice.setError("ផលិតផលនេះធូថ្លៃពេកហើយ");
                etPrice.requestFocus();
                Toast.makeText(this, "តម្លៃត្រូវតែយ៉ាងហោចណាស់ " + MINIMUM_PRICE + " ៛", Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "តម្លៃមិនត្រឹមត្រូវ", Toast.LENGTH_SHORT).show();
            etPrice.requestFocus();
            return false;
        }

        if (etUnit.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "សូមបញ្ចូលឯកតា", Toast.LENGTH_SHORT).show();
            etUnit.requestFocus();
            return false;
        }

        if (selectedImages.isEmpty() && existingImageUrls.isEmpty()) {
            Toast.makeText(this, "សូមបញ្ចូលរូបភាព", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void createShoppingItem() {
        Log.d(TAG, "Creating new shopping item");

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "user_12345";

        // Final price validation before creation
        String priceStr = etPrice.getText().toString().trim();
        try {
            int price = Integer.parseInt(priceStr);
            if (price < MINIMUM_PRICE) {
                Toast.makeText(this, "ផលិតផលនេះធូថ្លៃពេកហើយ. តម្លៃត្រូវតែយ៉ាងហោចណាស់ " + MINIMUM_PRICE + " ៛", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "តម្លៃមិនត្រឹមត្រូវ", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("កំពុងដាក់រូបភាព...");

        uploadImagesToFirebase(userId, new ImageUploadCallback() {
            @Override
            public void onSuccess(List<String> downloadUrls) {
                ShoppingItem item = new ShoppingItem();
                item.setItemId(UUID.randomUUID().toString());
                item.setName(etItemName.getText().toString().trim());
                item.setCategory(selectedCategory);
                item.setDescription(etDescription.getText().toString().trim());
                item.setPrice(etPrice.getText().toString().trim());
                item.setUnit(etUnit.getText().toString().trim());
                item.setImages(downloadUrls);
                item.setUserId(userId);
                item.setRating(0.0f);
                item.setReview_count(0);
                item.setCreatedAt(System.currentTimeMillis());
                item.setUpdatedAt(System.currentTimeMillis());

                btnSubmit.setText("កំពុងបន្ថែមទំនិញ...");

                viewModel.createShoppingItem(item, new ShoppingViewModel.ShoppingItemCallback<ShoppingItem>() {
                    @Override
                    public void onSuccess(ShoppingItem item) {
                        Toast.makeText(AddShoppingItemActivity.this, "✅ ទំនិញត្រូវបានបន្ថែមដោយជោគជ័យ", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(AddShoppingItemActivity.this, "❌ កំហុសក្នុងការបន្ថែមទំនិញ: " + error, Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText(isEditMode ? "រក្សាទុកការផ្លាស់ប្តូរ" : "ដាក់ស្នើរ");
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AddShoppingItemActivity.this, "❌ ផ្ទុករូបភាពបរាជ័យ: " + error, Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
                btnSubmit.setText(isEditMode ? "រក្សាទុកការផ្លាស់ប្តូរ" : "ដាក់ស្នើរ");
            }
        });
    }

    private void updateShoppingItem() {
        Log.d(TAG, "Updating shopping item: " + (editItem != null ? editItem.getName() : "null"));

        if (editItem == null) {
            Log.e(TAG, "Edit item is null in updateShoppingItem!");
            Toast.makeText(this, "❌ ទិន្នន័យទំនិញមិនត្រឹមត្រូវ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Final price validation before update
        String priceStr = etPrice.getText().toString().trim();
        try {
            int price = Integer.parseInt(priceStr);
            if (price < MINIMUM_PRICE) {
                Toast.makeText(this, "ផលិតផលនេះធូថ្លៃពេកហើយ. តម្លៃត្រូវតែយ៉ាងហោចណាស់ " + MINIMUM_PRICE + " ៛", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "តម្លៃមិនត្រឹមត្រូវ", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "user_12345";

        btnSubmit.setEnabled(false);
        btnSubmit.setText("កំពុងធ្វើបច្ចុប្បន្នភាព...");

        Log.d(TAG, "Existing images: " + existingImageUrls.size());
        Log.d(TAG, "New images to upload: " + selectedImages.size());

        if (!selectedImages.isEmpty()) {
            uploadImagesToFirebase(userId, new ImageUploadCallback() {
                @Override
                public void onSuccess(List<String> downloadUrls) {
                    List<String> allImageUrls = new ArrayList<>(existingImageUrls);
                    allImageUrls.addAll(downloadUrls);

                    Log.d(TAG, "Total images after upload: " + allImageUrls.size());

                    updateItemInDatabase(allImageUrls);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(AddShoppingItemActivity.this, "❌ ផ្ទុករូបភាពបរាជ័យ: " + error, Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("រក្សាទុកការផ្លាស់ប្តូរ");
                }
            });
        } else {
            Log.d(TAG, "No new images to upload, updating with existing images");
            updateItemInDatabase(existingImageUrls);
        }
    }

    private void updateItemInDatabase(List<String> imageUrls) {
        Log.d(TAG, "Updating item in database");
        Log.d(TAG, "Item Firebase key: " + editItem.getFirebaseKey());

        // Update the item with new data
        editItem.setName(etItemName.getText().toString().trim());
        editItem.setCategory(selectedCategory);
        editItem.setDescription(etDescription.getText().toString().trim());
        editItem.setPrice(etPrice.getText().toString().trim());
        editItem.setUnit(etUnit.getText().toString().trim());
        editItem.setImages(imageUrls);
        editItem.setUpdatedAt(System.currentTimeMillis());

        Log.d(TAG, "Updated item: " + editItem.toString());

        // Use the viewmodel to update shopping item
        viewModel.updateShoppingItem(editItem, new ShoppingViewModel.ShoppingItemCallback<ShoppingItem>() {
            @Override
            public void onSuccess(ShoppingItem item) {
                Log.d(TAG, "Item updated successfully");
                Toast.makeText(AddShoppingItemActivity.this, "✅ ទំនិញត្រូវបានធ្វើបច្ចុប្បន្នភាពដោយជោគជ័យ", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error updating item: " + error);
                Toast.makeText(AddShoppingItemActivity.this, "❌ កំហុសក្នុងការធ្វើបច្ចុប្បន្នភាពទំនិញ: " + error, Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
                btnSubmit.setText("រក្សាទុកការផ្លាស់ប្តូរ");
            }
        });
    }

    private void uploadImagesToFirebase(String userId, ImageUploadCallback callback) {
        if (selectedImages.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<String> downloadUrls = new ArrayList<>();
        int totalImages = selectedImages.size();
        final int[] uploadedCount = {0};

        for (int i = 0; i < selectedImages.size(); i++) {
            Uri imageUri = selectedImages.get(i);

            Uri compressedUri = compressImage(imageUri);
            if (compressedUri == null) {
                compressedUri = imageUri;
            }

            String filename = "marketplace/" + userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + ".jpg";
            StorageReference fileRef = storageReference.child(filename);

            final int index = i;
            UploadTask uploadTask = fileRef.putFile(compressedUri);

            uploadTask.addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    downloadUrls.add(downloadUri.toString());
                    uploadedCount[0]++;

                    int progress = (uploadedCount[0] * 100) / totalImages;
                    btnSubmit.setText("កំពុងដាក់រូបភាព... " + progress + "%");

                    if (uploadedCount[0] == totalImages) {
                        callback.onSuccess(downloadUrls);
                    }
                }).addOnFailureListener(e -> {
                    callback.onError("ទាញយក URL មិនអាចទៅរួច: " + e.getMessage());
                });
            }).addOnFailureListener(e -> {
                callback.onError("ផ្ទុករូបភាពបរាជ័យ: " + e.getMessage());
            });
        }
    }

    private Uri compressImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = (float) width / height;

            int newWidth = 1024;
            int newHeight = (int) (newWidth / ratio);
            if (newHeight > 1024) {
                newHeight = 1024;
                newWidth = (int) (newHeight * ratio);
            }

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            File tempFile = File.createTempFile("compressed", ".jpg", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(data);
            fos.flush();
            fos.close();

            return Uri.fromFile(tempFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    interface ImageUploadCallback {
        void onSuccess(List<String> downloadUrls);
        void onError(String error);
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, AddShoppingItemActivity.class);
    }

    public static Intent newIntentForEdit(Context context, ShoppingItem item) {
        Log.d(TAG, "Creating edit intent for item: " + (item != null ? item.getName() : "null"));
        Intent intent = new Intent(context, AddShoppingItemActivity.class);
        intent.putExtra(EXTRA_EDIT_ITEM, item);
        return intent;
    }
}