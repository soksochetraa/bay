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

import java.io.IOException;
import java.util.ArrayList;
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
    private HomeActivity homeActivity;

    // Constants for categories
    private static final String CATEGORY_FARM = "Farm";
    private static final String CATEGORY_MARKET = "Market";
    private static final String CATEGORY_FARM_KH = "កសិដ្ឋាន";
    private static final String CATEGORY_MARKET_KH = "ផ្សារ";
    private static final String FILTER_ALL = "ទាំងអស់";
    private static final String FILTER_FARM = "កសិដ្ឋាន";
    private static final String FILTER_MARKET = "ផ្សារ";

    // Marker colors
    private static final float MARKER_COLOR_FARM = BitmapDescriptorFactory.HUE_GREEN;
    private static final float MARKER_COLOR_MARKET = BitmapDescriptorFactory.HUE_ORANGE;
    private static final float MARKER_COLOR_DEFAULT = BitmapDescriptorFactory.HUE_RED;

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
        setupBindings();
        setupMap();
        setupSearch();
        setupFilterChips();
    }

    private void setupBindings() {
        binding.fabAddLocation.setOnClickListener(v -> openCreateLocationFragment());
        binding.fabMenu.setOnClickListener(v -> showFarmMapMenu());
        binding.button.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupSearch() {
        binding.etSearchLocation.setOnEditorActionListener((textView, actionId, event) -> {
            String query = binding.etSearchLocation.getText().toString().trim();
            if (!TextUtils.isEmpty(query)) {
                searchLocation(query);
            } else {
                showToast("សូមបញ្ចូលទីតាំងស្វែងរក");
            }
            return true;
        });
    }

    private void setupFilterChips() {
        binding.filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                Chip selectedChip = binding.filterChipGroup.findViewById(checkedId);
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

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void openCreateLocationFragment() {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).hideBottomNavigation();
            showToast("Opening create location...");
        }
    }

    private void showMyLocations() {
        showToast("My Locations");
    }

    private void showSavedLocations() {
        showToast("Saved Locations");
    }

    private void showFilterDialog() {
        showToast("Filter Locations");
    }

    private void showMapSettings() {
        showToast("Map Settings");
    }

    private void showHelp() {
        showToast("Help & Support");
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng cambodia = new LatLng(12.5657, 104.9910);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cambodia, 7f));
        loadLocationsFromRepository(FILTER_ALL);
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
                        showToast("រកមិនឃើញទីតាំង!");
                    }
                });
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> {
                    hideLoading();
                    showToast("មានបញ្ហាក្នុងការស្វែងរក!");
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
        if (TextUtils.isEmpty(filter) || FILTER_ALL.equals(filter)) return true;

        switch (filter) {
            case FILTER_FARM:
                return CATEGORY_FARM.equalsIgnoreCase(category) || CATEGORY_FARM_KH.equals(category);
            case FILTER_MARKET:
                return CATEGORY_MARKET.equalsIgnoreCase(category) || CATEGORY_MARKET_KH.equals(category);
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
                    showToast("ទិន្នន័យមិនមាន!");
                    return;
                }

                allLocations = result;
                mMap.clear();

                for (Map.Entry<String, Location> entry : result.entrySet()) {
                    Location loc = entry.getValue();
                    if (isLocationValid(loc) && matchesFilter(filter, loc.category)) {
                        addMarkerToMap(loc);
                    }
                }

                setupMarkerClickListener();
            }

            @Override
            public void onFailure(String error) {
                hideLoading();
                showToast("បញ្ហាក្នុងការទាញទិន្នន័យ!");
            }
        });
    }

    private boolean isLocationValid(Location loc) {
        return loc != null &&
                loc.visibility != null &&
                loc.visibility.isVisible &&
                !Double.isNaN(loc.latitude) &&
                !Double.isNaN(loc.longitude);
    }

    private void addMarkerToMap(Location loc) {
        LatLng latLng = new LatLng(loc.latitude, loc.longitude);
        float markerColor = getMarkerColorForCategory(loc.category);

        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(loc.name)
                .snippet(loc.category)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
    }

    private float getMarkerColorForCategory(String category) {
        if (category == null) return MARKER_COLOR_DEFAULT;

        if (CATEGORY_FARM.equalsIgnoreCase(category) || CATEGORY_FARM_KH.equals(category)) {
            return MARKER_COLOR_FARM;
        } else if (CATEGORY_MARKET.equalsIgnoreCase(category) || CATEGORY_MARKET_KH.equals(category)) {
            return MARKER_COLOR_MARKET;
        }
        return MARKER_COLOR_DEFAULT;
    }

    private void setupMarkerClickListener() {
        mMap.setOnMarkerClickListener(marker -> {
            if (allLocations != null) {
                for (Location loc : allLocations.values()) {
                    if (loc != null && marker.getTitle().equals(loc.name)) {
                        showLocationModal(loc);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void showLocationModal(Location loc) {
        if (loc == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        @SuppressLint("InflateParams") View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_modal_small_card_locations_data, null);

        TextView tvName = view.findViewById(R.id.tvLocationName);
        TextView tvCategory = view.findViewById(R.id.tvCategory);
        TextView tvPhone = view.findViewById(R.id.tvPhoneNumber);
        TextView tvCopy = view.findViewById(R.id.tvCopy);
        ImageButton btnClose = view.findViewById(R.id.btnClose);
        MaterialButton btnDetails = view.findViewById(R.id.btnDetails);

        tvName.setText(loc.name);
        setCategoryIcon(tvCategory, loc.category);
        tvCategory.setText(loc.category);
        tvPhone.setText(loc.contact != null ? loc.contact.phoneNumber : "-");

        tvCopy.setOnClickListener(v -> copyToClipboard(tvPhone.getText().toString(), "Phone"));
        btnDetails.setOnClickListener(v -> {
            dialog.dismiss();
            showLocationDetailModal(loc);
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }

    private void setCategoryIcon(TextView categoryView, String category) {
        int iconRes;
        if (CATEGORY_FARM.equalsIgnoreCase(category) || CATEGORY_FARM_KH.equals(category)) {
            iconRes = R.drawable.ico_location;
        } else {
            iconRes = R.drawable.ico_location2;
        }
        categoryView.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
    }

    @SuppressLint("InflateParams")
    private void showLocationDetailModal(Location loc) {
        ItemModalBigCardLocationsDataBinding detailBinding =
                ItemModalBigCardLocationsDataBinding.inflate(LayoutInflater.from(requireContext()));

        detailDialog = new BottomSheetDialog(requireContext(), R.style.BigBottomSheetDialogTheme);
        detailDialog.setContentView(detailBinding.getRoot());

        setupDetailModalLayout(detailDialog);
        bindLocationDetailData(detailBinding, loc);
        setupDetailModalListeners(detailBinding, loc);

        detailDialog.show();
    }

    private void setupDetailModalLayout(BottomSheetDialog dialog) {
        dialog.setOnShowListener(dialogInterface -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
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
    }

    private void bindLocationDetailData(ItemModalBigCardLocationsDataBinding binding, Location loc) {
        binding.name.setText(loc.name != null ? loc.name : "");
        binding.category.setText(loc.category != null ? loc.category : "");

        if (!TextUtils.isEmpty(loc.profileUrl)) {
            Glide.with(this)
                    .load(loc.profileUrl)
                    .into(binding.btnProfile);
        }

        setupPhotosSection(binding, loc.photos);
        setupContactSection(binding, loc.contact);
        setupAboutSection(binding, loc.detail != null ? loc.detail.about : null);
        setupGrowingCropsSection(binding, loc.detail != null ? loc.detail.growing : null);
        setupCertificatesSection(binding, loc.detail != null ? loc.detail.certificate : null);
    }

    private void setupDetailModalListeners(ItemModalBigCardLocationsDataBinding binding, Location loc) {
        binding.btnClose.setOnClickListener(v -> detailDialog.dismiss());
        binding.ViewSellProfile.setOnClickListener(v ->
                showToast("Opening seller profile..."));
        binding.message.setOnClickListener(v ->
                showToast("Opening message..."));
    }

    private void setupPhotosSection(ItemModalBigCardLocationsDataBinding binding, List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            binding.photoSection.setVisibility(View.GONE);
            return;
        }

        binding.photoSection.setVisibility(View.VISIBLE);
        currentPhotos = photos;
        currentPhotoPosition = 0;

        if (!photos.isEmpty()) {
            loadPhotoIntoDisplay(binding, photos.get(0));
            binding.allPhotos.setText(String.format(Locale.getDefault(), "1 / %d", photos.size()));
            binding.photoDisplay.setOnClickListener(v ->
                    showFullscreenImage(photos, currentPhotoPosition));
        }

        setupPhotoThumbnails(binding, photos);
        setupPhotoNavigation(binding, photos);
    }

    private void loadPhotoIntoDisplay(ItemModalBigCardLocationsDataBinding binding, String photoUrl) {
        Glide.with(this)
                .load(photoUrl)
                .override(800, 600)
                .placeholder(R.drawable.img)
                .error(R.drawable.img)
                .into(binding.photoDisplay);
    }

    private void setupPhotoThumbnails(ItemModalBigCardLocationsDataBinding binding, List<String> photos) {
        List<ImageView> thumbnails = new ArrayList<>();
        thumbnails.add(binding.thumb1);
        thumbnails.add(binding.thumb2);
        thumbnails.add(binding.thumb3);
        thumbnails.add(binding.thumb4);
        thumbnails.add(binding.thumb5);

        if (photos == null || photos.isEmpty()) {
            for (ImageView thumbnail : thumbnails) {
                thumbnail.setVisibility(View.GONE);
            }
            binding.moreImagesIndicator.setVisibility(View.GONE);
            return;
        }

        int thumbnailsToShow = Math.min(photos.size(), thumbnails.size());

        for (int i = 0; i < thumbnails.size(); i++) {
            if (i < thumbnailsToShow) {
                Glide.with(this)
                        .load(photos.get(i))
                        .override(200, 150)
                        .placeholder(R.drawable.img)
                        .error(R.drawable.img)
                        .into(thumbnails.get(i));

                final int position = i;
                thumbnails.get(i).setOnClickListener(v -> {
                    updatePhotoDisplay(binding, photos, position, thumbnails);
                    currentPhotoPosition = position;
                });

                thumbnails.get(i).setVisibility(View.VISIBLE);
                thumbnails.get(i).setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                thumbnails.get(i).setVisibility(View.GONE);
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

    private void updatePhotoDisplay(ItemModalBigCardLocationsDataBinding binding, List<String> photos,
                                    int position, List<ImageView> thumbnails) {
        if (position < 0 || position >= photos.size()) return;

        binding.photoDisplay.post(() -> {
            loadPhotoIntoDisplay(binding, photos.get(position));
            binding.allPhotos.setText(String.format(Locale.getDefault(), "%d / %d", position + 1, photos.size()));
            binding.photoDisplay.setOnClickListener(v ->
                    showFullscreenImage(photos, position));

            if (position < Math.min(photos.size(), thumbnails.size())) {
                updateActiveThumbnail(thumbnails, position);
            }
        });
    }

    private void updateActiveThumbnail(List<ImageView> thumbnails, int activePosition) {
        for (int i = 0; i < thumbnails.size(); i++) {
            if (thumbnails.get(i).getVisibility() == View.VISIBLE) {
                if (i == activePosition) {
                    thumbnails.get(i).setAlpha(1f);
                    if (thumbnails.get(i) instanceof com.google.android.material.imageview.ShapeableImageView) {
                        ((com.google.android.material.imageview.ShapeableImageView) thumbnails.get(i))
                                .setStrokeWidth(2);
                        ((com.google.android.material.imageview.ShapeableImageView) thumbnails.get(i))
                                .setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.primary));
                    }
                } else {
                    thumbnails.get(i).setAlpha(0.7f);
                    if (thumbnails.get(i) instanceof com.google.android.material.imageview.ShapeableImageView) {
                        ((com.google.android.material.imageview.ShapeableImageView) thumbnails.get(i))
                                .setStrokeWidth(0);
                    }
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

        List<ImageView> thumbnails = new ArrayList<>();
        thumbnails.add(binding.thumb1);
        thumbnails.add(binding.thumb2);
        thumbnails.add(binding.thumb3);
        thumbnails.add(binding.thumb4);
        thumbnails.add(binding.thumb5);

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

    private void showImageGallery(List<String> photos, int startPosition) {
        if (photos == null || photos.isEmpty()) {
            showToast("មិនមានរូបភាព");
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

    private void setupContactSection(ItemModalBigCardLocationsDataBinding binding, Location.Contact contact) {
        if (contact == null) {
            hideAllContactRows(binding);
            return;
        }

        setupPhoneRow(binding, contact.phoneNumber);
        setupLocationRow(binding, contact.locationLink);
        setupFacebookRow(binding, contact.facebook);
        setupTelegramRow(binding, contact.telegram);
        setupTiktokRow(binding, contact.tiktok);
    }

    private void hideAllContactRows(ItemModalBigCardLocationsDataBinding binding) {
        binding.rowPhone.setVisibility(View.GONE);
        binding.rowLocation.setVisibility(View.GONE);
        binding.rowFacebook.setVisibility(View.GONE);
        binding.rowTelegram.setVisibility(View.GONE);
        binding.rowTiktok.setVisibility(View.GONE);
    }

    private void setupPhoneRow(ItemModalBigCardLocationsDataBinding binding, String phoneNumber) {
        if (!TextUtils.isEmpty(phoneNumber)) {
            binding.rowPhone.setVisibility(View.VISIBLE);
            binding.tvPhoneValue.setText(phoneNumber);
            binding.btnCall.setVisibility(View.VISIBLE);
            binding.btnCall.setOnClickListener(v -> openPhone(phoneNumber));
            binding.rowPhone.setOnClickListener(v -> openPhone(phoneNumber));
        } else {
            binding.rowPhone.setVisibility(View.GONE);
        }
    }

    private void setupLocationRow(ItemModalBigCardLocationsDataBinding binding, String locationLink) {
        if (!TextUtils.isEmpty(locationLink)) {
            binding.rowLocation.setVisibility(View.VISIBLE);
            binding.tvLocationValue.setText("ប៉ះដើម្បីមើលលើផែនទី");
            binding.btnLocation.setOnClickListener(v -> openMap(locationLink));
            binding.rowLocation.setOnClickListener(v -> openMap(locationLink));
        } else {
            binding.rowLocation.setVisibility(View.GONE);
        }
    }

    private void setupFacebookRow(ItemModalBigCardLocationsDataBinding binding, String facebookUrl) {
        if (!TextUtils.isEmpty(facebookUrl)) {
            binding.rowFacebook.setVisibility(View.VISIBLE);
            binding.tvFacebookValue.setText(facebookUrl);
            binding.btnFacebook.setVisibility(View.VISIBLE);
            binding.btnFacebook.setOnClickListener(v -> openFacebook(facebookUrl));
            binding.rowFacebook.setOnClickListener(v -> openFacebook(facebookUrl));
        } else {
            binding.rowFacebook.setVisibility(View.GONE);
        }
    }

    private void setupTelegramRow(ItemModalBigCardLocationsDataBinding binding, String telegramHandle) {
        if (!TextUtils.isEmpty(telegramHandle)) {
            String handle = telegramHandle.startsWith("@") ? telegramHandle.substring(1) : telegramHandle;
            binding.rowTelegram.setVisibility(View.VISIBLE);
            binding.tvTelegramValue.setText("@" + handle);
            binding.btnTelegram.setVisibility(View.VISIBLE);
            binding.btnTelegram.setOnClickListener(v -> openTelegram(handle));
            binding.rowTelegram.setOnClickListener(v -> openTelegram(handle));
        } else {
            binding.rowTelegram.setVisibility(View.GONE);
        }
    }

    private void setupTiktokRow(ItemModalBigCardLocationsDataBinding binding, String tiktokHandle) {
        if (!TextUtils.isEmpty(tiktokHandle)) {
            String handle = tiktokHandle.startsWith("@") ? tiktokHandle.substring(1) : tiktokHandle;
            binding.rowTiktok.setVisibility(View.VISIBLE);
            binding.tvTiktokValue.setText("@" + handle);
            binding.btnTiktok.setVisibility(View.VISIBLE);
            binding.btnTiktok.setOnClickListener(v -> openTiktok(handle));
            binding.rowTiktok.setOnClickListener(v -> openTiktok(handle));
        } else {
            binding.rowTiktok.setVisibility(View.GONE);
        }
    }

    private void setupAboutSection(ItemModalBigCardLocationsDataBinding binding, String about) {
        if (!TextUtils.isEmpty(about)) {
            binding.textView9.setText(about);
        } else {
            binding.textView9.setText("មិនមានព័ត៌មានបន្ថែម");
        }
    }

    private void setupGrowingCropsSection(ItemModalBigCardLocationsDataBinding binding, List<String> growingCrops) {
        LinearLayout cropsContainer = binding.getRoot().findViewById(R.id.badgeFruit);
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
            if (!TextUtils.isEmpty(crop)) {
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
    }

    private void setupCertificatesSection(ItemModalBigCardLocationsDataBinding binding, List<Map<String, String>> certificates) {
        LinearLayout certContainer = binding.getRoot().findViewById(R.id.badgeCertificate);
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
            String certName = cert.get("name");
            String certStatus = cert.get("status");

            if (!TextUtils.isEmpty(certName)) {
                @SuppressLint("InflateParams") LinearLayout badge = (LinearLayout) LayoutInflater.from(requireContext())
                        .inflate(R.layout.badge_chip_layout, null);

                TextView badgeText = badge.findViewById(R.id.badge_text);
                ImageView badgeIcon = badge.findViewById(R.id.badge_icon);

                badgeText.setText(certName);
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

    private void copyToClipboard(String text, String label) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        showToast("ចម្លង " + label + "!");
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

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Loading management
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeActivity) {
            homeActivity = (HomeActivity) context;
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

    // Swipe touch listener for fullscreen images
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