package com.example.bay.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.ImageGalleryAdapter;
import com.example.bay.databinding.FragmentFarmMapBinding;
import com.example.bay.databinding.ItemModalBigCardLocationsDataBinding;
import com.example.bay.model.Location;
import com.example.bay.repository.FarmMapRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FarmMapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FragmentFarmMapBinding binding;
    private FarmMapRepository repository;
    private Map<String, Location> allLocations;
    private BottomSheetDialog detailDialog;
    private int currentPhotoPosition = 0;
    private List<String> currentPhotos;
    HomeActivity homeActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFarmMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof HomeActivity) {
            homeActivity = (HomeActivity) getActivity();
        }

        repository = new FarmMapRepository();

        binding.fabAddLocation.setOnClickListener(v -> {
            openCreateLocationFragment();
        });

        binding.fabMenu.setOnClickListener(v -> {
            showFarmMapMenu();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.button.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        binding.etSearchLocation.setOnEditorActionListener((textView, actionId, event) -> {
            String query = binding.etSearchLocation.getText().toString().trim();
            if (!TextUtils.isEmpty(query)) {
                searchLocation(query);
            } else {
                Toast.makeText(requireContext(), "សូមបញ្ចូលទីតាំងស្វែងរក", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        binding.filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                Chip selectedChip = view.findViewById(checkedId);
                if (selectedChip != null) {
                    applyFilter(selectedChip.getText().toString());
                }
            }
        });
    }

    private void showFarmMapMenu() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_farm_map_menu, null);

        bottomSheetView.findViewById(R.id.btn_create_location).setOnClickListener(v -> {
            openCreateLocationFragment();
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.btn_my_locations).setOnClickListener(v -> {
            showMyLocations();
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.btn_saved_locations).setOnClickListener(v -> {
            showSavedLocations();
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.btn_filter).setOnClickListener(v -> {
            showFilterDialog();
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.btn_settings).setOnClickListener(v -> {
            showMapSettings();
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.btn_help).setOnClickListener(v -> {
            showHelp();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void openCreateLocationFragment() {
        if (getActivity() instanceof HomeActivity) {
            HomeActivity activity = (HomeActivity) getActivity();
            activity.hideBottomNavigation();
            Toast.makeText(requireContext(), "Opening create location...", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMyLocations() {
        Toast.makeText(requireContext(), "My Locations", Toast.LENGTH_SHORT).show();
    }

    private void showSavedLocations() {
        Toast.makeText(requireContext(), "Saved Locations", Toast.LENGTH_SHORT).show();
    }

    private void showFilterDialog() {
        Toast.makeText(requireContext(), "Filter Locations", Toast.LENGTH_SHORT).show();
    }

    private void showMapSettings() {
        Toast.makeText(requireContext(), "Map Settings", Toast.LENGTH_SHORT).show();
    }

    private void showHelp() {
        Toast.makeText(requireContext(), "Help & Support", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng cambodia = new LatLng(12.5657, 104.9910);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cambodia, 7f));
        loadLocationsFromRepository("ទាំងអស់");
    }

    private void searchLocation(String query) {
        showLoading();
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                requireActivity().runOnUiThread(() -> {
                    hideLoading();
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(latLng).title(query));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f));
                    } else {
                        Toast.makeText(requireContext(), "រកមិនឃើញទីតាំង!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(requireContext(), "មានបញ្ហាក្នុងការស្វែងរក!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void applyFilter(String filter) {
        showLoading();
        mMap.clear();
        loadLocationsFromRepository(filter);
    }

    private boolean matchesFilter(String filter, String category) {
        if (TextUtils.isEmpty(category)) return false;
        if (TextUtils.isEmpty(filter) || "ទាំងអស់".equals(filter)) return true;

        switch (filter) {
            case "Farm":
            case "កសិដ្ឋាន":
                return "Farm".equalsIgnoreCase(category) || "កសិដ្ឋាន".equals(category);
            case "NGO":
                return "NGO".equalsIgnoreCase(category);
            case "Market":
            case "ផ្សារ":
                return "Market".equalsIgnoreCase(category) || "ផ្សារ".equals(category);
            case "Supplier":
                return "Supplier".equalsIgnoreCase(category);
            default:
                return filter.equalsIgnoreCase(category);
        }
    }

    private void loadLocationsFromRepository(String filter) {
        showLoading();
        repository.getAllLocations(new FarmMapRepository.LocationCallback<Map<String, Location>>() {
            @Override
            public void onSuccess(Map<String, Location> result) {
                hideLoading();
                if (result == null || result.isEmpty()) {
                    Toast.makeText(requireContext(), "ទិន្នន័យមិនមាន!", Toast.LENGTH_SHORT).show();
                    return;
                }

                allLocations = result;
                mMap.clear();

                for (Map.Entry<String, Location> entry : result.entrySet()) {
                    Location loc = entry.getValue();
                    if (loc == null) continue;
                    if (loc.visibility == null || !loc.visibility.isVisible) continue;
                    if (Double.isNaN(loc.latitude) || Double.isNaN(loc.longitude)) continue;
                    if (!matchesFilter(filter, loc.category)) continue;

                    LatLng latLng = new LatLng(loc.latitude, loc.longitude);
                    float markerColor;

                    switch (loc.category) {
                        case "Farm":
                        case "កសិដ្ឋាន":
                            markerColor = BitmapDescriptorFactory.HUE_GREEN;
                            break;
                        case "NGO":
                            markerColor = BitmapDescriptorFactory.HUE_AZURE;
                            break;
                        case "Market":
                        case "ផ្សារ":
                            markerColor = BitmapDescriptorFactory.HUE_ORANGE;
                            break;
                        case "Supplier":
                            markerColor = BitmapDescriptorFactory.HUE_ROSE;
                            break;
                        default:
                            markerColor = BitmapDescriptorFactory.HUE_RED;
                            break;
                    }

                    mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(loc.name)
                            .snippet(loc.category)
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
                }

                mMap.setOnMarkerClickListener(marker -> {
                    for (Location loc : allLocations.values()) {
                        if (marker.getTitle().equals(loc.name)) {
                            showLocationModal(loc);
                            break;
                        }
                    }
                    return true;
                });
            }

            @Override
            public void onFailure(String error) {
                hideLoading();
                Toast.makeText(requireContext(), "បញ្ហាក្នុងការទាញទិន្នន័យ!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLocationModal(Location loc) {
        if (loc == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        @SuppressLint("InflateParams") View view = LayoutInflater.from(requireContext()).inflate(R.layout.item_modal_small_card_locations_data, null);

        TextView tvName = view.findViewById(R.id.tvLocationName);
        TextView tvCategory = view.findViewById(R.id.tvCategory);
        TextView tvPhone = view.findViewById(R.id.tvPhoneNumber);
        TextView tvCopy = view.findViewById(R.id.tvCopy);
        ImageButton btnClose = view.findViewById(R.id.btnClose);
        MaterialButton btnDetails = view.findViewById(R.id.btnDetails);

        tvName.setText(loc.name);

        if (loc.category.equals("Farm") || loc.category.equals("កសិដ្ឋាន")) {
            tvCategory.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ico_location, 0, 0, 0);
        } else {
            tvCategory.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ico_location2, 0, 0, 0);
        }

        tvCategory.setText(loc.category);
        tvPhone.setText(loc.contact != null ? loc.contact.phoneNumber : "-");

        tvCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Phone", tvPhone.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "ចម្លងលេខទូរស័ព្ទ!", Toast.LENGTH_SHORT).show();
        });

        btnDetails.setOnClickListener(v -> {
            dialog.dismiss();
            showFarmDetailModal(loc);
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }

    @SuppressLint("InflateParams")
    private void showFarmDetailModal(Location loc) {
        ItemModalBigCardLocationsDataBinding detailBinding =
                ItemModalBigCardLocationsDataBinding.inflate(LayoutInflater.from(requireContext()));

        detailDialog = new BottomSheetDialog(requireContext(), R.style.BigBottomSheetDialogTheme);
        detailDialog.setContentView(detailBinding.getRoot());

        bindLocationDataToModal(detailBinding, loc);

        detailDialog.setOnShowListener(dialogInterface -> {
            View bottomSheet = detailDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenHeight = displayMetrics.heightPixels;

                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                params.height = (int) (screenHeight * 0.85);
                bottomSheet.setLayoutParams(params);

                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(params.height);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                bottomSheet.setBackgroundResource(R.drawable.bg_item_location_modal2);
            }
        });

        detailBinding.btnClose.setOnClickListener(v -> detailDialog.dismiss());

        detailDialog.show();
    }

    private void bindLocationDataToModal(ItemModalBigCardLocationsDataBinding binding, Location loc) {
        binding.name.setText(loc.name != null ? loc.name : "");
        binding.category.setText(loc.category != null ? loc.category : "");

        if (!TextUtils.isEmpty(loc.profileUrl)) {
            Glide.with(this)
                    .load(loc.profileUrl)
                    .into(binding.btnProfile);
        }

        handlePhotos(binding, loc.photos);
        bindContactInfo(binding, loc.contact);

        if (loc.detail != null && !TextUtils.isEmpty(loc.detail.about)) {
            binding.textView9.setText(loc.detail.about);
        } else {
            binding.textView9.setText("មិនមានព័ត៌មានបន្ថែម");
        }

        bindGrowingCrops(binding, loc.detail != null ? loc.detail.growing : null);
        bindCertificates(binding, loc.detail != null ? loc.detail.certificate : null);
        setupButtonListeners(binding, loc);
    }

    private void handlePhotos(ItemModalBigCardLocationsDataBinding binding, List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            binding.photoSection.setVisibility(View.GONE);
            return;
        }

        binding.photoSection.setVisibility(View.VISIBLE);
        currentPhotos = photos;
        currentPhotoPosition = 0;

        if (!photos.isEmpty()) {
            Glide.with(this)
                    .load(photos.get(0))
                    .override(800, 600)
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .into(binding.photoDisplay);

            binding.allPhotos.setText(String.format(Locale.getDefault(), "1 / %d", photos.size()));

            binding.photoDisplay.setOnClickListener(v -> {
                showFullscreenImage(photos, currentPhotoPosition);
            });
        }

        setupThumbnails(binding, photos);
        setupPhotoNavigation(binding, photos);
    }

    private void setupThumbnails(ItemModalBigCardLocationsDataBinding binding, List<String> photos) {
        ShapeableImageView[] thumbnails = {
                binding.thumb1, binding.thumb2, binding.thumb3, binding.thumb4, binding.thumb5
        };

        if (photos == null || photos.isEmpty()) {
            for (ShapeableImageView thumbnail : thumbnails) {
                thumbnail.setVisibility(View.GONE);
            }
            binding.moreImagesIndicator.setVisibility(View.GONE);
            return;
        }

        int thumbnailsToShow = Math.min(photos.size(), thumbnails.length);

        for (int i = 0; i < thumbnails.length; i++) {
            if (i < thumbnailsToShow) {
                Glide.with(this)
                        .load(photos.get(i))
                        .override(200, 150)
                        .placeholder(R.drawable.img)
                        .error(R.drawable.img)
                        .into(thumbnails[i]);

                final int position = i;
                thumbnails[i].setOnClickListener(v -> {
                    updatePhotoDisplay(binding, photos, position, thumbnails);
                    currentPhotoPosition = position;
                });

                thumbnails[i].setVisibility(View.VISIBLE);
                thumbnails[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                thumbnails[i].setVisibility(View.GONE);
            }
        }

        if (photos.size() > 5) {
            binding.moreImagesIndicator.setVisibility(View.VISIBLE);
            binding.moreImagesIndicator.setText(String.format(Locale.getDefault(), "+%d", photos.size() - 5));
            binding.moreImagesIndicator.setOnClickListener(v -> showImageGallery(photos, 0));
        } else {
            binding.moreImagesIndicator.setVisibility(View.GONE);
        }

        updateActiveThumbnail(thumbnails, 0);
    }

    private void showImageGallery(List<String> photos, int startPosition) {
        if (photos == null || photos.isEmpty()) {
            Toast.makeText(requireContext(), "មិនមានរូបភាព", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog galleryDialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        @SuppressLint("InflateParams") View galleryView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_image_gallery, null);

        ViewPager2 viewPager = galleryView.findViewById(R.id.viewPager);
        TextView pageIndicator = galleryView.findViewById(R.id.pageIndicator);
        ImageButton btnClose = galleryView.findViewById(R.id.btnClose);

        ImageGalleryAdapter adapter = new ImageGalleryAdapter(photos);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPosition, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                pageIndicator.setText(String.format(Locale.getDefault(), "%d / %d", position + 1, photos.size()));
            }
        });

        pageIndicator.setText(String.format(Locale.getDefault(), "%d / %d", startPosition + 1, photos.size()));
        btnClose.setOnClickListener(v -> galleryDialog.dismiss());
        galleryDialog.setContentView(galleryView);
        galleryDialog.show();
    }

    private void updatePhotoDisplay(ItemModalBigCardLocationsDataBinding binding, List<String> photos, int position, ShapeableImageView[] thumbnails) {
        if (position < 0 || position >= photos.size()) {
            return;
        }

        binding.photoDisplay.post(() -> {
            Glide.with(this)
                    .load(photos.get(position))
                    .override(800, 600)
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .into(binding.photoDisplay);
            binding.allPhotos.setText(String.format(Locale.getDefault(), "%d / %d", position + 1, photos.size()));

            final int currentPosition = position;
            binding.photoDisplay.setOnClickListener(v -> {
                showFullscreenImage(photos, currentPosition);
            });

            if (position < Math.min(photos.size(), thumbnails.length)) {
                updateActiveThumbnail(thumbnails, position);
            }
        });
    }

    private void updateActiveThumbnail(ShapeableImageView[] thumbnails, int activePosition) {
        for (int i = 0; i < thumbnails.length; i++) {
            if (thumbnails[i].getVisibility() == View.VISIBLE) {
                if (i == activePosition) {
                    thumbnails[i].setAlpha(1f);
                    thumbnails[i].setStrokeWidth(2);
                    thumbnails[i].setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.primary));
                } else {
                    thumbnails[i].setAlpha(0.7f);
                    thumbnails[i].setStrokeWidth(0);
                }
            }
        }
    }

    private void setupPhotoNavigation(ItemModalBigCardLocationsDataBinding binding, List<String> photos) {
        if (photos == null || photos.size() <= 1) {
            binding.btnPrev.setVisibility(View.GONE);
            binding.btnNext.setVisibility(View.GONE);
            return;
        }

        binding.btnPrev.setVisibility(View.VISIBLE);
        binding.btnNext.setVisibility(View.VISIBLE);

        ShapeableImageView[] thumbnails = {binding.thumb1, binding.thumb2, binding.thumb3, binding.thumb4, binding.thumb5};

        binding.btnNext.setOnClickListener(v -> {
            if (photos.size() > 1) {
                currentPhotoPosition = (currentPhotoPosition + 1) % photos.size();
                updatePhotoDisplay(binding, photos, currentPhotoPosition, thumbnails);
            }
        });

        binding.btnPrev.setOnClickListener(v -> {
            if (photos.size() > 1) {
                currentPhotoPosition = (currentPhotoPosition - 1 + photos.size()) % photos.size();
                updatePhotoDisplay(binding, photos, currentPhotoPosition, thumbnails);
            }
        });
    }

    private void bindContactInfo(ItemModalBigCardLocationsDataBinding binding, Location.Contact contact) {
        if (contact == null) {
            binding.rowPhone.setVisibility(View.GONE);
            binding.rowLocation.setVisibility(View.GONE);
            binding.rowFacebook.setVisibility(View.GONE);
            binding.rowTelegram.setVisibility(View.GONE);
            binding.rowTiktok.setVisibility(View.GONE);
            return;
        }

        if (!TextUtils.isEmpty(contact.phoneNumber)) {
            binding.rowPhone.setVisibility(View.VISIBLE);
            binding.tvPhoneValue.setText(contact.phoneNumber);
            binding.btnCall.setVisibility(View.VISIBLE);
            binding.btnCall.setOnClickListener(v -> openPhone(contact.phoneNumber));
            binding.rowPhone.setOnClickListener(v -> openPhone(contact.phoneNumber));
        } else {
            binding.rowPhone.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(contact.locationLink)) {
            binding.rowLocation.setVisibility(View.VISIBLE);
            binding.tvLocationValue.setText("ប៉ះដើម្បីមើលលើផែនទី");
            binding.btnLocation.setOnClickListener(v -> openMap(contact.locationLink));
            binding.rowLocation.setOnClickListener(v -> openMap(contact.locationLink));
        } else {
            binding.rowLocation.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(contact.facebook)) {
            binding.rowFacebook.setVisibility(View.VISIBLE);
            binding.tvFacebookValue.setText(contact.facebook);
            binding.btnFacebook.setVisibility(View.VISIBLE);
            binding.btnFacebook.setOnClickListener(v -> openFacebook(contact.facebook));
            binding.rowFacebook.setOnClickListener(v -> openFacebook(contact.facebook));
        } else {
            binding.rowFacebook.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(contact.telegram)) {
            String handle = contact.telegram.startsWith("@")
                    ? contact.telegram.substring(1)
                    : contact.telegram;
            binding.rowTelegram.setVisibility(View.VISIBLE);
            binding.tvTelegramValue.setText("@" + handle);
            binding.btnTelegram.setVisibility(View.VISIBLE);
            binding.btnTelegram.setOnClickListener(v -> openTelegram(handle));
            binding.rowTelegram.setOnClickListener(v -> openTelegram(handle));
        } else {
            binding.rowTelegram.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(contact.tiktok)) {
            String handle = contact.tiktok.startsWith("@")
                    ? contact.tiktok.substring(1)
                    : contact.tiktok;
            binding.rowTiktok.setVisibility(View.VISIBLE);
            binding.tvTiktokValue.setText("@" + handle);
            binding.btnTiktok.setVisibility(View.VISIBLE);
            binding.btnTiktok.setOnClickListener(v -> openTiktok(handle));
            binding.rowTiktok.setOnClickListener(v -> openTiktok(handle));
        } else {
            binding.rowTiktok.setVisibility(View.GONE);
        }
    }

    private void bindGrowingCrops(ItemModalBigCardLocationsDataBinding binding, List<String> growingCrops) {
        LinearLayout cropsContainer = (LinearLayout) binding.getRoot().findViewById(R.id.badgeFruit).getParent();
        if (cropsContainer == null) return;

        cropsContainer.removeAllViews();

        if (growingCrops == null || growingCrops.isEmpty()) {
            TextView noCropsText = new TextView(requireContext());
            noCropsText.setText("មិនមានដំណាំ");
            noCropsText.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            cropsContainer.addView(noCropsText);
            return;
        }

        for (String crop : growingCrops) {
            @SuppressLint("InflateParams") LinearLayout badge = (LinearLayout) LayoutInflater.from(requireContext())
                    .inflate(R.layout.badge_chip_layout, null);

            TextView badgeText = badge.findViewById(R.id.badge_text);
            ImageView badgeIcon = badge.findViewById(R.id.badge_icon);

            badgeText.setText(crop);
            badgeIcon.setImageResource(R.drawable.ico_leaf);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 8, 0);
            badge.setLayoutParams(params);

            cropsContainer.addView(badge);
        }
    }

    private void bindCertificates(ItemModalBigCardLocationsDataBinding binding, List<Map<String, String>> certificates) {
        LinearLayout certContainer = (LinearLayout) binding.getRoot().findViewById(R.id.badgeCertificate).getParent();
        if (certContainer == null) return;

        certContainer.removeAllViews();

        if (certificates == null || certificates.isEmpty()) {
            TextView noCertText = new TextView(requireContext());
            noCertText.setText("មិនមានវិញ្ញាបនបត្រ");
            noCertText.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            certContainer.addView(noCertText);
            return;
        }

        for (Map<String, String> cert : certificates) {
            @SuppressLint("InflateParams") LinearLayout badge = (LinearLayout) LayoutInflater.from(requireContext())
                    .inflate(R.layout.badge_chip_layout, null);

            TextView badgeText = badge.findViewById(R.id.badge_text);
            ImageView badgeIcon = badge.findViewById(R.id.badge_icon);

            String certName = cert.get("name");
            String certStatus = cert.get("status");

            badgeText.setText(certName != null ? certName : "");
            if ("Verified".equals(certStatus)) {
                badge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.verified_color));
            }
            badgeIcon.setImageResource(R.drawable.ico_certification);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 8, 0);
            badge.setLayoutParams(params);

            certContainer.addView(badge);
        }
    }

    private void showFullscreenImage(List<String> photos, int startPosition) {
        if (photos == null || photos.isEmpty() || startPosition < 0 || startPosition >= photos.size()) {
            return;
        }

        Dialog fullscreenDialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_fullscreen_image, null);

        ImageView fullscreenImageView = dialogView.findViewById(R.id.fullscreenImageView);
        TextView imageCounter = dialogView.findViewById(R.id.imageCounter);
        ImageButton btnClose = dialogView.findViewById(R.id.btnCloseFullscreen);
        ImageButton btnPrev = dialogView.findViewById(R.id.btnPrevFullscreen);
        ImageButton btnNext = dialogView.findViewById(R.id.btnNextFullscreen);

        final int[] currentIndex = {startPosition};

        Runnable updateUi = () -> {
            int pos = currentIndex[0];

            Glide.with(this)
                    .load(photos.get(pos))
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .into(fullscreenImageView);

            imageCounter.setText(String.format(Locale.getDefault(), "%d / %d", pos + 1, photos.size()));

            btnPrev.setVisibility(pos > 0 ? View.VISIBLE : View.INVISIBLE);
            btnNext.setVisibility(pos < photos.size() - 1 ? View.VISIBLE : View.INVISIBLE);
        };

        updateUi.run();

        btnPrev.setOnClickListener(v -> {
            if (currentIndex[0] > 0) {
                currentIndex[0]--;
                updateUi.run();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex[0] < photos.size() - 1) {
                currentIndex[0]++;
                updateUi.run();
            }
        });

        btnClose.setOnClickListener(v -> fullscreenDialog.dismiss());

        fullscreenImageView.setOnTouchListener(new OnSwipeTouchListener(requireContext()) {
            @Override
            public void onSwipeLeft() {
                if (currentIndex[0] < photos.size() - 1) {
                    currentIndex[0]++;
                    updateUi.run();
                }
            }

            @Override
            public void onSwipeRight() {
                if (currentIndex[0] > 0) {
                    currentIndex[0]--;
                    updateUi.run();
                }
            }

            @Override
            public void onSwipeTop() {
                fullscreenDialog.dismiss();
            }

            @Override
            public void onSwipeBottom() {
                fullscreenDialog.dismiss();
            }
        });

        fullscreenDialog.setContentView(dialogView);
        fullscreenDialog.show();
    }

    private void setupButtonListeners(ItemModalBigCardLocationsDataBinding binding, Location loc) {
        binding.ViewSellProfile.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Opening seller profile...", Toast.LENGTH_SHORT).show();
        });

        binding.message.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Opening message...", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean isAppInstalled(String packageName) {
        try {
            requireContext().getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void openPhone(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        startActivity(intent);
    }

    private void openMap(String url) {
        Uri mapUri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, mapUri);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, mapUri));
        }
    }

    private void openFacebook(String url) {
        String fbAppUrl;
        if (url.contains("facebook.com/")) {
            fbAppUrl = "fb://facewebmodal/f?href=" + url;
        } else {
            fbAppUrl = "fb://facewebmodal/f?href=https://facebook.com/" + url;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (isAppInstalled("com.facebook.katana")) {
            intent.setData(Uri.parse(fbAppUrl));
        } else {
            intent.setData(Uri.parse(url));
        }
        startActivity(intent);
    }

    private void openTelegram(String username) {
        String handle = username.startsWith("@") ? username.substring(1) : username;
        Uri uri = Uri.parse("tg://resolve?domain=" + handle);
        Intent tgIntent = new Intent(Intent.ACTION_VIEW, uri);
        if (isAppInstalled("org.telegram.messenger")) {
            startActivity(tgIntent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/" + handle)));
        }
    }

    private void openTiktok(String username) {
        String handle = username.startsWith("@") ? username.substring(1) : username;
        Uri uri = Uri.parse("snssdk1128://user/profile/" + handle);
        Intent appIntent = new Intent(Intent.ACTION_VIEW, uri);
        if (isAppInstalled("com.zhiliaoapp.musically")) {
            startActivity(appIntent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.tiktok.com/@" + handle)));
        }
    }

    private void openLink(String url) {
        if (!TextUtils.isEmpty(url)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(requireContext(), "តំណមិនត្រឹមត្រូវ!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "មិនមានតំណភ្ជាប់!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeActivity) {
            homeActivity = (HomeActivity) context;
        }
    }

    public void showLoading() {
        if (homeActivity != null) {
            homeActivity.showLoading();
        }
    }

    public void hideLoading() {
        if (homeActivity != null) {
            homeActivity.hideLoading();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).hideBottomNavigation();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public class OnSwipeTouchListener implements View.OnTouchListener {
        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight();
                            } else {
                                onSwipeLeft();
                            }
                            return true;
                        }
                    } else {
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY > 0) {
                                onSwipeBottom();
                            } else {
                                onSwipeTop();
                            }
                            return true;
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return false;
            }
        }

        public void onSwipeRight() {}
        public void onSwipeLeft() {}
        public void onSwipeTop() {}
        public void onSwipeBottom() {}
    }
}