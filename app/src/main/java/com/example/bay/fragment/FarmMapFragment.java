package com.example.bay.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.ImageGalleryAdapter;
import com.example.bay.databinding.FragmentFarmMapBinding;
import com.example.bay.databinding.ItemModalBigCardLocationsDataBinding;
import com.example.bay.model.Location;
import com.example.bay.repository.LocationRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FarmMapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FragmentFarmMapBinding binding;
    private LocationRepository repository;
    private Map<String, Location> allLocations;
    private BottomSheetDialog detailDialog;
    private int currentPhotoPosition = 0;
    private List<String> currentPhotos;
    private HomeActivity homeActivity;

    private static final String CATEGORY_FARM = "Farm";
    private static final String CATEGORY_MARKET = "Market";
    private static final String CATEGORY_FARM_KH = "កសិដ្ឋាន";
    private static final String CATEGORY_MARKET_KH = "ផ្សារ";
    private static final String FILTER_ALL = "ទាំងអស់";
    private static final String FILTER_FARM = "កសិដ្ឋាន";
    private static final String FILTER_MARKET = "ផ្សារ";
    private static final String FILTER_MY_LOCATIONS = "MY_LOCATIONS";
    private static final String FILTER_SAVED_LOCATIONS = "SAVED_LOCATIONS";

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

        repository = new LocationRepository();
        setupDrawer();
        setupBindings();
        setupMap();
        setupSearch();
        setupDrawerFilterChips();
        setupBackHandler();
    }

    private void setupDrawer() {
        binding.farmMapDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        binding.btnCreateLocation.setOnClickListener(v -> {
            binding.farmMapDrawer.closeDrawer(GravityCompat.END);
            openCreateLocationFragment();
        });
    }

    private void setupDrawerFilterChips() {
        binding.drawerFilterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String filter = FILTER_ALL;
            if (checkedId == R.id.drawer_chip_farm) {
                filter = FILTER_FARM;
            } else if (checkedId == R.id.drawer_chip_market) {
                filter = FILTER_MARKET;
            } else if (checkedId == R.id.drawer_chip_my_farms) {
                filter = FILTER_MY_LOCATIONS;
            } else if (checkedId == R.id.drawer_chip_saved) {
                filter = FILTER_SAVED_LOCATIONS;
            }

            binding.farmMapDrawer.closeDrawer(GravityCompat.END);
            applyFilter(filter);
        });
    }

    private void setupBackHandler() {
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (binding != null && binding.farmMapDrawer.isDrawerOpen(GravityCompat.END)) {
                            binding.farmMapDrawer.closeDrawer(GravityCompat.END);
                        } else {
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                }
        );
    }

    private void setupBindings() {
        binding.fabAddLocation.setOnClickListener(v -> openCreateLocationFragment());
        binding.fabOpenDrawer.setOnClickListener(v -> binding.farmMapDrawer.openDrawer(GravityCompat.END));
        binding.button.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void setupMap() {
        showLoading();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && getChildFragmentManager() != null) {
                SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
                if (mapFragment != null) mapFragment.getMapAsync(this);
            }
        }, 300); // Allow fragment transition animation to finish first
    }

    private void setupSearch() {
        binding.etSearchLocation.setOnEditorActionListener((textView, actionId, event) -> {
            String query = binding.etSearchLocation.getText().toString().trim();
            if (!TextUtils.isEmpty(query)) searchLocation(query);
            else showToast("សូមបញ្ចូលទីតាំងស្វែងរក");
            return true;
        });
    }

    private void openCreateLocationFragment() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "សូមចូលគណនីជាមុន", Toast.LENGTH_SHORT).show();
            return;
        }
        if (android.text.TextUtils.isEmpty(user.getPhoneNumber())) {
            Toast.makeText(requireContext(), "សូមបញ្ជាក់លេខទូរស័ព្ទរបស់អ្នកជាមុនសិន", Toast.LENGTH_LONG).show();
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).LoadFragment(new AddPhoneNumberFragment());
            }
            return;
        }

        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).LoadFragment(new CreateLocationFragment());
        }
    }

    private void openEditLocationFragment(String locationId) {
        if (TextUtils.isEmpty(locationId)) return;
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).LoadFragment(EditLocationFragment.newInstance(locationId));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Apply dark mode map style if the system is in night mode
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            try {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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
        if (mMap != null) mMap.clear();
        loadLocationsFromRepository(filter);
    }

    private boolean matchesFilter(String filter, String category) {
        if (TextUtils.isEmpty(filter) || FILTER_ALL.equals(filter)) return true;

        if (FILTER_MY_LOCATIONS.equals(filter)) {
            // Need to match when we actually have the location object, but we are just given category here.
            // We will handle MY_LOCATIONS in the loop inside loadLocationsFromRepository instead
            return true;
        }

        if (FILTER_SAVED_LOCATIONS.equals(filter)) {
            return true; // We handle this in the loop as well
        }

        if (TextUtils.isEmpty(category)) return false;

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
        repository.getAllLocations(new LocationRepository.LocationCallback<Map<String, Location>>() {
            @Override
            public void onSuccess(Map<String, Location> result) {
                hideLoading();
                if (result == null || result.isEmpty()) {
                    showToast("ទិន្នន័យមិនមាន!");
                    return;
                }

                allLocations = result;
                if (mMap != null) mMap.clear();

                String currentUserUid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

                for (Map.Entry<String, Location> entry : result.entrySet()) {
                    String id = entry.getKey();
                    Location loc = entry.getValue();
                    
                    if (!isLocationValid(loc)) continue;

                    boolean shouldShow = true;
                    if (FILTER_MY_LOCATIONS.equals(filter)) {
                        shouldShow = loc.owner != null && currentUserUid != null && currentUserUid.equals(loc.owner.uuid);
                    } else if (FILTER_SAVED_LOCATIONS.equals(filter)) {
                        shouldShow = isLocationSaved(id);
                    } else {
                        shouldShow = matchesFilter(filter, loc.category);
                    }

                    if (shouldShow) {
                        addMarkerToMap(id, loc);
                    }
                }

                setupMarkerClickListener();

                if (getArguments() != null) {
                    String focusedLocationId = getArguments().getString("focused_location_id");
                    if (focusedLocationId != null && allLocations.containsKey(focusedLocationId)) {
                        Location focusedLoc = allLocations.get(focusedLocationId);
                        if (focusedLoc != null && !Double.isNaN(focusedLoc.latitude) && !Double.isNaN(focusedLoc.longitude)) {
                            LatLng latLng = new LatLng(focusedLoc.latitude, focusedLoc.longitude);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
                            showLocationModal(focusedLocationId, focusedLoc);
                            
                            // Clear argument so it doesn't trigger again
                            getArguments().remove("focused_location_id");
                        }
                    }
                }
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

    private void addMarkerToMap(String id, Location loc) {
        LatLng latLng = new LatLng(loc.latitude, loc.longitude);
        float markerColor = getMarkerColorForCategory(loc.category);

        if (mMap != null) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(loc.name)
                    .snippet(loc.category)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
            if (marker != null) marker.setTag(id);
        }
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
        if (mMap == null) return;

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag == null) return false;

            String id = String.valueOf(tag);
            if (allLocations == null) return false;

            Location loc = allLocations.get(id);
            if (loc == null) return false;

            showLocationModal(id, loc);
            return true;
        });
    }

    private void showLocationModal(String locationId, Location loc) {
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
        ImageView imgCategoryIcon = view.findViewById(R.id.imgCategoryIcon);


        tvName.setText(loc.name);

        if (loc.category.equals("Farm")){
            tvCategory.setText("កសិដ្ឋាន");
            imgCategoryIcon.setImageResource(R.drawable.ico_location);
        }else{
            tvCategory.setText("ហាង");
            imgCategoryIcon.setImageResource(R.drawable.ico_location2);
        }
        tvPhone.setText(loc.contact != null ? loc.contact.phoneNumber : "-");

        tvCopy.setOnClickListener(v -> copyToClipboard(tvPhone.getText().toString(), "Phone"));
        btnDetails.setOnClickListener(v -> {
            dialog.dismiss();
            showLocationDetailModal(locationId, loc);
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }

    @SuppressLint("InflateParams")
    private void showLocationDetailModal(String locationId, Location loc) {
        ItemModalBigCardLocationsDataBinding detailBinding =
                ItemModalBigCardLocationsDataBinding.inflate(LayoutInflater.from(requireContext()));

        detailDialog = new BottomSheetDialog(requireContext(), R.style.BigBottomSheetDialogTheme);
        detailDialog.setContentView(detailBinding.getRoot());

        setupDetailModalLayout(detailDialog);
        bindLocationDetailData(detailBinding, loc);
        setupDetailModalListeners(detailBinding, locationId, loc);

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
            Glide.with(this).load(loc.profileUrl).into(binding.btnProfile);
        }

        setupPhotosSection(binding, loc.photos);
        setupContactSection(binding, loc.contact);
        setupAboutSection(binding, loc.detail != null ? loc.detail.about : null);
        setupGrowingCropsSection(binding, loc.detail != null ? loc.detail.growing : null);
    }

    private void setupDetailModalListeners(ItemModalBigCardLocationsDataBinding binding, String locationId, Location loc) {
        binding.btnClose.setOnClickListener(v -> detailDialog.dismiss());

        boolean isSaved = isLocationSaved(locationId);
        updateSaveButtonState(isSaved, binding.btnSave);
        binding.btnSave.setOnClickListener(v -> toggleLocationSaved(locationId, binding.btnSave));

        String uid = FirebaseAuth.getInstance().getUid();
        boolean isOwner = uid != null && loc != null && loc.owner != null && uid.equals(loc.owner.uuid);

        binding.constraintLayout6.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        binding.constraintLayout7.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        if (isOwner) {
            binding.btnEditLocation.setOnClickListener(v -> {
                detailDialog.dismiss();
                openEditLocationFragment(locationId);
            });
        } else {
            binding.ViewSellProfile.setOnClickListener(v -> showToast("Opening seller profile..."));
            binding.message.setOnClickListener(v -> showToast("Opening message..."));
        }
    }

    private void setupPhotosSection(ItemModalBigCardLocationsDataBinding binding, List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            binding.photoSection.setVisibility(View.GONE);
            return;
        }

        binding.photoSection.setVisibility(View.VISIBLE);
        currentPhotos = photos;
        currentPhotoPosition = 0;

        loadPhotoIntoDisplay(binding, photos.get(0));
        binding.allPhotos.setText(String.format(Locale.getDefault(), "1 / %d", photos.size()));
        binding.photoDisplay.setOnClickListener(v -> showFullscreenImage(photos, currentPhotoPosition));

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
            binding.photoDisplay.setOnClickListener(v -> showFullscreenImage(photos, position));

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
            currentPhotoPosition = (currentPhotoPosition + 1) % photos.size();
            updatePhotoDisplay(binding, photos, currentPhotoPosition, thumbnails);
        });

        binding.btnPrev.setOnClickListener(v -> {
            currentPhotoPosition = (currentPhotoPosition - 1 + photos.size()) % photos.size();
            updatePhotoDisplay(binding, photos, currentPhotoPosition, thumbnails);
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
            binding.rowPhone.setVisibility(View.GONE);
            binding.rowLocation.setVisibility(View.GONE);
            return;
        }

        setupPhoneRow(binding, contact.phoneNumber);
        setupLocationRow(binding, contact.locationLink);
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
            binding.tvLocationValue.setText(locationLink);
            binding.btnLocation.setOnClickListener(v -> openMap(locationLink));
            binding.rowLocation.setOnClickListener(v -> openMap(locationLink));
        } else {
            binding.rowLocation.setVisibility(View.GONE);
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
        LinearLayout cropsContainer = binding.getRoot().findViewById(R.id.verticalContainer);
        LinearLayout badgeFruit = binding.getRoot().findViewById(R.id.badgeFruit);
        if (badgeFruit == null) return;

        badgeFruit.removeAllViews();

        if (growingCrops == null || growingCrops.isEmpty()) {
            TextView noCropsText = new TextView(requireContext());
            noCropsText.setText("មិនមានដំណាំ");
            noCropsText.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            badgeFruit.addView(noCropsText);
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

                badgeFruit.addView(badge);
            }
        }
    }

    private void showFullscreenImage(List<String> photos, int startPosition) {
        if (photos == null || photos.isEmpty() || startPosition < 0 || startPosition >= photos.size()) return;

        Dialog fullscreenDialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_fullscreen_image, null);

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

    private void openPhone(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        startActivity(intent);
    }

    private void openMap(String url) {
        Uri mapUri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, mapUri);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) startActivity(intent);
        else startActivity(new Intent(Intent.ACTION_VIEW, mapUri));
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void showLoading() {
        if (homeActivity != null) homeActivity.showLoading();
    }

    public void hideLoading() {
        if (homeActivity != null) homeActivity.hideLoading();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeActivity) homeActivity = (HomeActivity) context;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private SharedPreferences getSavedLocationsPrefs() {
        return requireContext().getSharedPreferences("saved_locations", Context.MODE_PRIVATE);
    }

    private boolean isLocationSaved(String locationId) {
        if (locationId == null) return false;
        return getSavedLocationsPrefs().getBoolean(locationId, false);
    }

    private void toggleLocationSaved(String locationId, ImageButton btnSave) {
        if (locationId == null) return;
        boolean isSaved = isLocationSaved(locationId);
        getSavedLocationsPrefs().edit().putBoolean(locationId, !isSaved).apply();
        updateSaveButtonState(!isSaved, btnSave);
        String msg = !isSaved ? "បានរក្សាទុក" : "បានដកចេញពីការរក្សាទុក";
        showToast(msg);
    }

    private void updateSaveButtonState(boolean isSaved, ImageButton btnSave) {
        if (isSaved) {
            btnSave.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
        } else {
            btnSave.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray));
        }
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
                            if (diffX > 0) onSwipeRight();
                            else onSwipeLeft();
                            return true;
                        }
                    } else {
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY > 0) onSwipeBottom();
                            else onSwipeTop();
                            return true;
                        }
                    }
                } catch (Exception ignored) {
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
