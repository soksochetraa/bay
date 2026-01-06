package com.example.bay;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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

    private ShoppingViewModel viewModel;
    private LinearLayout imagesContainer;
    private EditText etItemName, etPrice, etUnit, etDescription;
    private Button btnAddImages, btnSubmit;
    private List<Uri> selectedImages = new ArrayList<>();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        storage = FirebaseStorage.getInstance("gs://baydigitalecosystemmobileapp.firebasestorage.app");
        storageReference = storage.getReference();

        viewModel = new ShoppingViewModel();
        initializeViews();
        setupCategoryDropdown();
        setupListeners();
        clearSampleImages();
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

        if (categories.length > 0) {
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
            if (selectedImages.size() >= MAX_IMAGES) {
                Toast.makeText(this, "អ្នកអាចបញ្ចូលរូបភាពបានត្រឹម " + MAX_IMAGES + " រូប", Toast.LENGTH_SHORT).show();
                return;
            }

            if (checkStoragePermission()) {
                openImagePicker();
            } else {
                requestStoragePermission();
            }
        });

        btnSubmit.setOnClickListener(v -> {
            if (validateInput()) {
                createShoppingItem();
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
                    for (int i = 0; i < count && selectedImages.size() < MAX_IMAGES; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        selectedImages.add(imageUri);
                        addImageToContainer(imageUri);
                    }

                    if (selectedImages.size() >= MAX_IMAGES) {
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
        android.widget.ImageView ivImage = imageView.findViewById(R.id.ivSelectedImage);
        android.widget.ImageView ivRemove = imageView.findViewById(R.id.ivRemove);

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
            android.widget.ImageView ivRemove = imageView.findViewById(R.id.ivRemove);

            final int position = i;
            ivRemove.setOnClickListener(v -> {
                selectedImages.remove(position);
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

        if (etPrice.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "សូមបញ្ចូលតម្លៃ", Toast.LENGTH_SHORT).show();
            etPrice.requestFocus();
            return false;
        }

        if (etUnit.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "សូមបញ្ចូលឯកតា", Toast.LENGTH_SHORT).show();
            etUnit.requestFocus();
            return false;
        }

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "សូមបញ្ចូលរូបភាព", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void createShoppingItem() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "user_12345";

        btnSubmit.setEnabled(false);
        btnSubmit.setText("កំពុងដាក់រូបភាព...");

        uploadImagesToFirebase(userId, new ImageUploadCallback() {
            @Override
            public void onSuccess(List<String> downloadUrls) {
                // Create ShoppingItem using the correct constructor
                ShoppingItem item = new ShoppingItem();
                item.setName(etItemName.getText().toString().trim());
                item.setCategory(selectedCategory);
                item.setDescription(etDescription.getText().toString().trim());
                item.setPrice(etPrice.getText().toString().trim());
                item.setUnit(etUnit.getText().toString().trim());
                item.setImages(downloadUrls);
                item.setUserId(userId);
                item.setRating(0.0f); // Default rating
                item.setReview_count(0); // Default review count
                item.setCreatedAt(System.currentTimeMillis());
                item.setUpdatedAt(System.currentTimeMillis());

                // Generate itemId (Firebase will also generate one)
                String itemId = UUID.randomUUID().toString();
                item.setItemId(itemId);

                btnSubmit.setText("កំពុងបន្ថែមទំនិញ...");

                viewModel.createShoppingItem(item, new com.example.bay.repository.ShoppingItemRepository.ShoppingItemCallback<ShoppingItem>() {
                    @Override
                    public void onSuccess(ShoppingItem item) {
                        Toast.makeText(AddShoppingItemActivity.this, "✅ ទំនិញត្រូវបានបន្ថែមដោយជោគជ័យ", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(AddShoppingItemActivity.this, "❌ កំហុសក្នុងការបន្ថែមទំនិញ: " + error, Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("ដាក់ស្នើរ");
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AddShoppingItemActivity.this, "❌ ផ្ទុករូបភាពបរាជ័យ: " + error, Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
                btnSubmit.setText("ដាក់ស្នើរ");
            }
        });
    }

    private void uploadImagesToFirebase(String userId, ImageUploadCallback callback) {
        if (selectedImages.isEmpty()) {
            callback.onError("សូមជ្រើសរូបភាព");
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

            // FIXED: Changed from "maketplace/" to "marketplace/" - corrected the typo
            String filename = "marketplace/" + userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + ".jpg";
            StorageReference fileRef = storageReference.child(filename);

            final int index = i;
            UploadTask uploadTask = fileRef.putFile(compressedUri);

            uploadTask.addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    downloadUrls.add(downloadUri.toString());
                    uploadedCount[0]++;

                    // Update progress
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

    public static Intent newIntent(android.content.Context context) {
        return new Intent(context, AddShoppingItemActivity.class);
    }
}