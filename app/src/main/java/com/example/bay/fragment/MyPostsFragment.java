package com.example.bay.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.bay.AddShoppingItemActivity;
import com.example.bay.R;
import com.example.bay.adapter.MyPostsAdapter;
import com.example.bay.model.ShoppingItem;
import com.example.bay.viewmodel.ShoppingViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class MyPostsFragment extends Fragment implements MyPostsAdapter.OnMyPostActionListener {

    private static final String TAG = "MyPostsFragment";

    private ShoppingViewModel sharedViewModel;
    private MyPostsAdapter adapter;
    private RecyclerView rvMyPosts;
    private LinearLayout emptyState;

    private View loadingView;
    private LottieAnimationView lottieView;

    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "សូមចូលគណនីជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "Current User ID: " + currentUserId);

        // Use same ViewModel as parent (SHARED ViewModel)
        sharedViewModel = new ViewModelProvider(requireActivity()).get(ShoppingViewModel.class);

        initializeViews(view);
        setupRecyclerView();
        observeViewModel();

        loadUserPosts();
    }

    private void initializeViews(View view) {
        rvMyPosts = view.findViewById(R.id.rvMyPosts);
        emptyState = view.findViewById(R.id.emptyState);

        loadingView = view.findViewById(R.id.loading);
        lottieView = view.findViewById(R.id.lottieView);
    }

    private void setupRecyclerView() {
        rvMyPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyPostsAdapter(requireContext(), new ArrayList<>(), this);
        rvMyPosts.setAdapter(adapter);
    }

    private void observeViewModel() {
        sharedViewModel.getUserPosts().observe(getViewLifecycleOwner(), items -> {
            Log.d(TAG, "User posts updated: " + (items != null ? items.size() : 0) + " items");
            adapter.updateData(items);

            if (items == null || items.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
            }
        });

        sharedViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                showLoading();
            } else {
                hideLoading();
            }
        });

        sharedViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Error: " + error);
                Toast.makeText(getContext(), "កំហុស: " + error, Toast.LENGTH_SHORT).show();
                hideLoading();
            }
        });
    }

    private void loadUserPosts() {
        Log.d(TAG, "Loading posts for user: " + currentUserId);
        if (currentUserId != null) {
            sharedViewModel.loadUserPosts(currentUserId);
        } else {
            Toast.makeText(getContext(), "មិនមានអត្តសញ្ញាណអ្នកប្រើប្រាស់", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ EDIT: Edit item
    @Override
    public void onEditClicked(ShoppingItem item) {
        if (item != null && item.getItemId() != null) {
            Log.d(TAG, "Editing item: " + item.getItemId());
            Log.d(TAG, "Firebase key: " + item.getFirebaseKey());
            Log.d(TAG, "Item details: " + item.toString());

            startActivity(AddShoppingItemActivity.newIntentForEdit(requireContext(), item));
        } else {
            Toast.makeText(getContext(), "មិនអាចកែប្រែទំនិញនេះបានទេ", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ DELETE: Delete item
    @Override
    public void onDeleteClicked(ShoppingItem item) {
        if (item != null && item.getItemId() != null) {
            Log.d(TAG, "Deleting item: " + item.getItemId());
            Log.d(TAG, "Firebase key: " + item.getFirebaseKey());
            showDeleteConfirmationDialog(item);
        } else {
            Toast.makeText(getContext(), "មិនអាចលុបទំនិញនេះបានទេ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMarkAsSoldClicked(ShoppingItem item) {
        Toast.makeText(getContext(), "សម្គាល់ថាបានលក់", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMoreOptionsClicked(ShoppingItem item, View anchorView) {
        if (item != null) {
            showSimpleOptionsDialog(item);
        }
    }

    private void showDeleteConfirmationDialog(ShoppingItem item) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("លុបប្រកាស")
                .setMessage("តើអ្នកពិតជាចង់លុប \"" + item.getName() + "\"?")
                .setPositiveButton("លុប", (dialogInterface, which) -> {
                    showLoading();
                    sharedViewModel.deleteShoppingItem(item.getItemId(), new ShoppingViewModel.DeleteCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "✅ Delete successful");
                            Toast.makeText(getContext(), "✅ បានលុបដោយជោគជ័យ", Toast.LENGTH_SHORT).show();
                            hideLoading();
                            loadUserPosts();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "❌ Delete error: " + error);
                            Toast.makeText(getContext(), "❌ កំហុស: " + error, Toast.LENGTH_SHORT).show();
                            hideLoading();
                        }
                    });
                })
                .setNegativeButton("បោះបង់", null)
                .create(); // Create dialog first

        // Apply custom styling before showing
        dialog.setOnShowListener(dialogInterface -> {
            try {
                // Apply Khmer font to title
                TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
                if (titleView != null) {
                    titleView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
                    titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                    titleView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                }

                // Apply Khmer font to message
                TextView messageView = dialog.findViewById(android.R.id.message);
                if (messageView != null) {
                    messageView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
                    messageView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                    messageView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                    messageView.setLineSpacing(0, 1.2f); // Better line spacing
                }

                // Get buttons and apply styling
                int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
                int redColor = ContextCompat.getColor(requireContext(), R.color.red);

                // Positive button (លុប) - RED color
                androidx.appcompat.widget.AppCompatButton positiveButton = (androidx.appcompat.widget.AppCompatButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    positiveButton.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
                    positiveButton.setTextColor(redColor);
                    positiveButton.setAllCaps(false); // Keep original text case
                    positiveButton.setPadding(32, 16, 32, 16); // Better padding
                }

                // Negative button (បោះបង់) - Primary color
                androidx.appcompat.widget.AppCompatButton negativeButton = (androidx.appcompat.widget.AppCompatButton) dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (negativeButton != null) {
                    negativeButton.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
                    negativeButton.setTextColor(primaryColor);
                    negativeButton.setAllCaps(false); // Keep original text case
                    negativeButton.setPadding(32, 16, 32, 16); // Better padding
                }

            } catch (Exception e) {
                Log.e(TAG, "Error styling confirmation dialog: " + e.getMessage());
            }
        });

        // Show the styled dialog
        dialog.show();
    }

    private void showSimpleOptionsDialog(ShoppingItem item) {
        String[] options = {"កែប្រែ", "លុប"};

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(item.getName())
                .setItems(options, (dialogInterface, which) -> {
                    if (which == 0) {
                        onEditClicked(item);
                    } else if (which == 1) {
                        onDeleteClicked(item);
                    }
                })
                .create(); // Create the dialog first instead of showing immediately

        // Apply custom style before showing
        dialog.setOnShowListener(dialogInterface -> {
            try {
                // Apply Khmer font to title
                TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
                if (titleView != null) {
                    titleView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
                    titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                    // Center align title if needed
                    titleView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                }

                // Get the list view for options
                android.widget.ListView listView = dialog.getListView();
                if (listView != null) {
                    // Create custom adapter for options
                    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            options
                    ) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            TextView textView = (TextView) view.findViewById(android.R.id.text1);

                            // Apply Khmer font
                            textView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));

                            // Center align text
                            textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                            textView.setPadding(80, 32, 0, 32); // Add padding for better touch area

                            // Set colors
                            if (options[position].equals("លុប")) {
                                // RED for delete option
                                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                            } else {
                                // Primary color for edit option
                                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                            }

                            return view;
                        }
                    };

                    listView.setAdapter(adapter);

                    // Optional: Remove divider lines for cleaner look
                    listView.setDivider(null);
                    listView.setDividerHeight(0);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error styling options dialog: " + e.getMessage());
            }
        });

        // Show the dialog
        dialog.show();
    }

    private void applyDialogStyle(AlertDialog dialog) {
        try {
            // Apply Khmer font to title
            TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
            if (titleView != null) {
                titleView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
                titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            }

            // Apply Khmer font to message
            TextView messageView = dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
                messageView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            }

            // Apply font and colors to buttons
            int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
            int redColor = ContextCompat.getColor(requireContext(), R.color.red); // Make sure you have red color in colors.xml

            dialog.setOnShowListener(dialogInterface -> {
                // Positive button (លុប) - RED color
                androidx.appcompat.widget.AppCompatButton positiveButton = (androidx.appcompat.widget.AppCompatButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    positiveButton.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
                    positiveButton.setTextColor(redColor);
                }

                // Negative button (បោះបង់) - Primary color
                androidx.appcompat.widget.AppCompatButton negativeButton = (androidx.appcompat.widget.AppCompatButton) dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (negativeButton != null) {
                    negativeButton.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));
                    negativeButton.setTextColor(primaryColor);
                }

                // For options dialog items
                if (dialog.getListView() != null) {
                    for (int i = 0; i < dialog.getListView().getChildCount(); i++) {
                        View child = dialog.getListView().getChildAt(i);
                        if (child instanceof TextView) {
                            TextView textView = (TextView) child;
                            textView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.kantumruypro_regular));

                            // Make "លុប" text red
                            if (textView.getText().toString().equals("លុប")) {
                                textView.setTextColor(redColor);
                            } else {
                                textView.setTextColor(primaryColor);
                            }
                        }
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error applying dialog style: " + e.getMessage());
        }
    }

    private void showLoading() {
        if (loadingView != null && lottieView != null) {
            loadingView.setVisibility(View.VISIBLE);
            rvMyPosts.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);

            if (!lottieView.isAnimating()) {
                lottieView.playAnimation();
            }
        }
    }

    private void hideLoading() {
        if (loadingView != null && lottieView != null) {
            loadingView.setVisibility(View.GONE);
            rvMyPosts.setVisibility(View.VISIBLE);

            if (lottieView.isAnimating()) {
                lottieView.cancelAnimation();
            }
        }
    }

    private void showEmptyState() {
        if (emptyState != null && rvMyPosts != null) {
            emptyState.setVisibility(View.VISIBLE);
            rvMyPosts.setVisibility(View.GONE);
        }
    }

    private void hideEmptyState() {
        if (emptyState != null && rvMyPosts != null) {
            emptyState.setVisibility(View.GONE);
            rvMyPosts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - refreshing data");
        if (currentUserId != null) {
            loadUserPosts();
        }
    }
}