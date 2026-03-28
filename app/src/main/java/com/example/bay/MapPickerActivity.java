package com.example.bay;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.bay.databinding.ActivityMapPickerBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedProvince = "";
    private ActivityMapPickerBinding binding;
    private PlacesClient placesClient;
    private EditText etSearchLocation;
    private MaterialCardView suggestionsContainer;
    private LinearLayout suggestionsLayout;
    private boolean isSearching = false;
    private boolean autoSelectPending = false;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int DEFAULT_ZOOM = 15;
    private static final int CAMBODIA_ZOOM = 7;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int SEARCH_DEBOUNCE_DELAY = 1500;

    private static final LatLng CAMBODIA_SOUTHWEST = new LatLng(9.0, 102.0);
    private static final LatLng CAMBODIA_NORTHEAST = new LatLng(15.0, 108.0);
    private static final LatLng CAMBODIA_CENTER = new LatLng(12.5657, 104.9910);

    private ActivityResultLauncher<String> locationPermissionLauncher;
    private boolean isWaitingForPermission = false;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private Marker currentMarker;
    private List<Polygon> provincePolygons = new ArrayList<>();
    private Map<String, List<LatLng>> provinceBoundaries = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMapPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        hideSystemUI();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Places API
        initializePlacesAPI();

        etSearchLocation = findViewById(R.id.etSearchLocation);
        createSuggestionsContainer();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        binding.locationButton.setOnClickListener(v -> {
            if (selectedLatLng != null && !selectedProvince.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("province", selectedProvince);
                resultIntent.putExtra("latitude", selectedLatLng.latitude);
                resultIntent.putExtra("longitude", selectedLatLng.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "សូមជ្រើសរើសទីតាំងនៅលើផែនទី", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnMyLocation.setOnClickListener(v -> checkLocationPermissionAndGetLocation());

        setupSearch();
        setupKeyboardHandling();
        setupPermissionLauncher();
        initializeProvinceBoundaries();

        Log.d("MapPicker", "Activity created, Places client: " + (placesClient != null ? "initialized" : "null"));
    }

    private void initializePlacesAPI() {
        try {
            String apiKey = getString(R.string.google_maps_key);
            Log.d("MapPicker", "API Key: " + (apiKey != null ? "Found" : "NULL"));

            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), apiKey);
                Log.d("MapPicker", "Places API initialized successfully");
            }
            placesClient = Places.createClient(this);
            Log.d("MapPicker", "Places client created successfully");
        } catch (Exception e) {
            Log.e("MapPicker", "Failed to initialize Places API", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeProvinceBoundaries() {
        // Phnom Penh (ភ្នំពេញ) - More accurate boundary
        List<LatLng> phnomPenh = Arrays.asList(
                new LatLng(11.4833, 104.7833),
                new LatLng(11.5000, 104.8000),
                new LatLng(11.5200, 104.8200),
                new LatLng(11.5400, 104.8300),
                new LatLng(11.5600, 104.8400),
                new LatLng(11.5800, 104.8500),
                new LatLng(11.6000, 104.8600),
                new LatLng(11.6200, 104.8800),
                new LatLng(11.6300, 104.9000),
                new LatLng(11.6200, 104.9200),
                new LatLng(11.6000, 104.9400),
                new LatLng(11.5800, 104.9500),
                new LatLng(11.5600, 104.9600),
                new LatLng(11.5400, 104.9500),
                new LatLng(11.5200, 104.9400),
                new LatLng(11.5000, 104.9200),
                new LatLng(11.4800, 104.9000),
                new LatLng(11.4700, 104.8800),
                new LatLng(11.4700, 104.8600),
                new LatLng(11.4800, 104.8400),
                new LatLng(11.4833, 104.7833)
        );
        provinceBoundaries.put("Phnom Penh", phnomPenh);
        provinceBoundaries.put("ភ្នំពេញ", phnomPenh);

        // Siem Reap (សៀមរាប) - Including Angkor area
        List<LatLng> siemReap = Arrays.asList(
                new LatLng(13.2000, 103.7000),
                new LatLng(13.2500, 103.7500),
                new LatLng(13.3000, 103.8000),
                new LatLng(13.3500, 103.8500),
                new LatLng(13.4000, 103.9000),
                new LatLng(13.4500, 103.9500),
                new LatLng(13.5000, 104.0000),
                new LatLng(13.5500, 104.0500),
                new LatLng(13.5500, 104.1000),
                new LatLng(13.5000, 104.1500),
                new LatLng(13.4500, 104.1500),
                new LatLng(13.4000, 104.1000),
                new LatLng(13.3500, 104.0500),
                new LatLng(13.3000, 104.0000),
                new LatLng(13.2500, 103.9500),
                new LatLng(13.2000, 103.9000),
                new LatLng(13.1500, 103.8500),
                new LatLng(13.1500, 103.8000),
                new LatLng(13.1800, 103.7500),
                new LatLng(13.2000, 103.7000)
        );
        provinceBoundaries.put("Siem Reap", siemReap);
        provinceBoundaries.put("សៀមរាប", siemReap);

        // Battambang (បាត់ដំបង)
        List<LatLng> battambang = Arrays.asList(
                new LatLng(12.9000, 103.0000),
                new LatLng(12.9500, 103.0500),
                new LatLng(13.0000, 103.1000),
                new LatLng(13.0500, 103.1500),
                new LatLng(13.1000, 103.2000),
                new LatLng(13.1500, 103.2500),
                new LatLng(13.2000, 103.3000),
                new LatLng(13.2500, 103.3500),
                new LatLng(13.3000, 103.4000),
                new LatLng(13.3500, 103.4500),
                new LatLng(13.3500, 103.5000),
                new LatLng(13.3000, 103.5500),
                new LatLng(13.2500, 103.5500),
                new LatLng(13.2000, 103.5000),
                new LatLng(13.1500, 103.4500),
                new LatLng(13.1000, 103.4000),
                new LatLng(13.0500, 103.3500),
                new LatLng(13.0000, 103.3000),
                new LatLng(12.9500, 103.2500),
                new LatLng(12.9000, 103.2000),
                new LatLng(12.8500, 103.1500),
                new LatLng(12.8500, 103.1000),
                new LatLng(12.8800, 103.0500),
                new LatLng(12.9000, 103.0000)
        );
        provinceBoundaries.put("Battambang", battambang);
        provinceBoundaries.put("បាត់ដំបង", battambang);

        // Kampong Cham (កំពង់ចាម)
        List<LatLng> kampongCham = Arrays.asList(
                new LatLng(11.8000, 105.3000),
                new LatLng(11.8500, 105.3500),
                new LatLng(11.9000, 105.4000),
                new LatLng(11.9500, 105.4500),
                new LatLng(12.0000, 105.5000),
                new LatLng(12.0500, 105.5500),
                new LatLng(12.1000, 105.6000),
                new LatLng(12.1500, 105.6500),
                new LatLng(12.1500, 105.7000),
                new LatLng(12.1000, 105.7500),
                new LatLng(12.0500, 105.7500),
                new LatLng(12.0000, 105.7000),
                new LatLng(11.9500, 105.6500),
                new LatLng(11.9000, 105.6000),
                new LatLng(11.8500, 105.5500),
                new LatLng(11.8000, 105.5000),
                new LatLng(11.7500, 105.4500),
                new LatLng(11.7500, 105.4000),
                new LatLng(11.7800, 105.3500),
                new LatLng(11.8000, 105.3000)
        );
        provinceBoundaries.put("Kampong Cham", kampongCham);
        provinceBoundaries.put("កំពង់ចាម", kampongCham);

        // Takeo (តាកែវ)
        List<LatLng> takeo = Arrays.asList(
                new LatLng(10.8000, 104.7000),
                new LatLng(10.8500, 104.7500),
                new LatLng(10.9000, 104.8000),
                new LatLng(10.9500, 104.8500),
                new LatLng(11.0000, 104.9000),
                new LatLng(11.0500, 104.9500),
                new LatLng(11.1000, 105.0000),
                new LatLng(11.1500, 105.0500),
                new LatLng(11.1500, 105.1000),
                new LatLng(11.1000, 105.1500),
                new LatLng(11.0500, 105.1500),
                new LatLng(11.0000, 105.1000),
                new LatLng(10.9500, 105.0500),
                new LatLng(10.9000, 105.0000),
                new LatLng(10.8500, 104.9500),
                new LatLng(10.8000, 104.9000),
                new LatLng(10.7500, 104.8500),
                new LatLng(10.7500, 104.8000),
                new LatLng(10.7800, 104.7500),
                new LatLng(10.8000, 104.7000)
        );
        provinceBoundaries.put("Takeo", takeo);
        provinceBoundaries.put("តាកែវ", takeo);

        // Kampong Speu (កំពង់ស្ពឺ)
        List<LatLng> kampongSpeu = Arrays.asList(
                new LatLng(11.4000, 104.2000),
                new LatLng(11.4500, 104.2500),
                new LatLng(11.5000, 104.3000),
                new LatLng(11.5500, 104.3500),
                new LatLng(11.6000, 104.4000),
                new LatLng(11.6500, 104.4500),
                new LatLng(11.7000, 104.5000),
                new LatLng(11.7500, 104.5500),
                new LatLng(11.7500, 104.6000),
                new LatLng(11.7000, 104.6500),
                new LatLng(11.6500, 104.6500),
                new LatLng(11.6000, 104.6000),
                new LatLng(11.5500, 104.5500),
                new LatLng(11.5000, 104.5000),
                new LatLng(11.4500, 104.4500),
                new LatLng(11.4000, 104.4000),
                new LatLng(11.3500, 104.3500),
                new LatLng(11.3500, 104.3000),
                new LatLng(11.3800, 104.2500),
                new LatLng(11.4000, 104.2000)
        );
        provinceBoundaries.put("Kampong Speu", kampongSpeu);
        provinceBoundaries.put("កំពង់ស្ពឺ", kampongSpeu);

        // Kampot (កំពត)
        List<LatLng> kampot = Arrays.asList(
                new LatLng(10.5500, 104.1500),
                new LatLng(10.6000, 104.2000),
                new LatLng(10.6500, 104.2500),
                new LatLng(10.7000, 104.3000),
                new LatLng(10.7500, 104.3500),
                new LatLng(10.8000, 104.4000),
                new LatLng(10.8500, 104.4500),
                new LatLng(10.9000, 104.5000),
                new LatLng(10.9000, 104.5500),
                new LatLng(10.8500, 104.6000),
                new LatLng(10.8000, 104.6000),
                new LatLng(10.7500, 104.5500),
                new LatLng(10.7000, 104.5000),
                new LatLng(10.6500, 104.4500),
                new LatLng(10.6000, 104.4000),
                new LatLng(10.5500, 104.3500),
                new LatLng(10.5000, 104.3000),
                new LatLng(10.5000, 104.2500),
                new LatLng(10.5300, 104.2000),
                new LatLng(10.5500, 104.1500)
        );
        provinceBoundaries.put("Kampot", kampot);
        provinceBoundaries.put("កំពត", kampot);

        // Preah Sihanouk (ព្រះសីហនុ)
        List<LatLng> sihanoukville = Arrays.asList(
                new LatLng(10.5500, 103.5000),
                new LatLng(10.6000, 103.5500),
                new LatLng(10.6500, 103.6000),
                new LatLng(10.7000, 103.6500),
                new LatLng(10.7500, 103.7000),
                new LatLng(10.8000, 103.7500),
                new LatLng(10.8000, 103.8000),
                new LatLng(10.7500, 103.8500),
                new LatLng(10.7000, 103.8500),
                new LatLng(10.6500, 103.8000),
                new LatLng(10.6000, 103.7500),
                new LatLng(10.5500, 103.7000),
                new LatLng(10.5000, 103.6500),
                new LatLng(10.5000, 103.6000),
                new LatLng(10.5300, 103.5500),
                new LatLng(10.5500, 103.5000)
        );
        provinceBoundaries.put("Preah Sihanouk", sihanoukville);
        provinceBoundaries.put("ព្រះសីហនុ", sihanoukville);

        // Add more provinces as needed with accurate coordinates
        // You can expand this with all 25 provinces of Cambodia
    }

    private void drawProvinceBorder(String provinceName) {
        clearProvinceBorders();

        List<LatLng> boundary = provinceBoundaries.get(provinceName);
        if (boundary == null) {
            // Try partial match
            for (Map.Entry<String, List<LatLng>> entry : provinceBoundaries.entrySet()) {
                if (entry.getKey().toLowerCase().contains(provinceName.toLowerCase()) ||
                        provinceName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                    boundary = entry.getValue();
                    break;
                }
            }
        }

        if (boundary != null && mMap != null) {
            PolygonOptions polygonOptions = new PolygonOptions()
                    .addAll(boundary)
                    .strokeWidth(4)
                    .strokeColor(Color.RED)
                    .fillColor(Color.parseColor("#33FF0000"));
            Polygon polygon = mMap.addPolygon(polygonOptions);
            provincePolygons.add(polygon);

            // Fit camera to show the entire province
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : boundary) {
                builder.include(point);
            }
            LatLngBounds bounds = builder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
        } else {
            // If no boundary found, just center on the location
            Log.d("MapPicker", "No boundary found for: " + provinceName);
        }
    }

    private void clearProvinceBorders() {
        for (Polygon polygon : provincePolygons) {
            if (polygon != null) {
                polygon.remove();
            }
        }
        provincePolygons.clear();
    }

    private void removeMarker() {
        if (currentMarker != null) {
            currentMarker.remove();
            currentMarker = null;
        }
    }

    private void addMarker(LatLng position, String title) {
        removeMarker();
        currentMarker = mMap.addMarker(new MarkerOptions().position(position).title(title));
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setupPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    isWaitingForPermission = false;
                    if (isGranted) {
                        getCurrentLocation();
                        if (mMap != null) {
                            try {
                                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                        == PackageManager.PERMISSION_GRANTED) {
                                    mMap.setMyLocationEnabled(true);
                                }
                            } catch (SecurityException e) {
                                Log.e("MapPicker", "Error enabling location: " + e.getMessage());
                            }
                        }
                    } else {
                        showPermissionDeniedDialog();
                    }
                }
        );
    }

    private void checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionRationaleDialog();
            } else {
                isWaitingForPermission = true;
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
    }

    private void showPermissionRationaleDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("ត្រូវការការអនុញ្ញាតទីតាំង")
                .setMessage("កម្មវិធីត្រូវការចូលប្រើទីតាំងរបស់អ្នក ដើម្បីបង្ហាញទីតាំងបច្ចុប្បន្នរបស់អ្នកនៅលើផែនទី។")
                .setPositiveButton("អនុញ្ញាត", (dialog, which) -> {
                    isWaitingForPermission = true;
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                })
                .setNegativeButton("បដិសេធ", (dialog, which) -> {
                    Toast.makeText(this, "មិនអាចប្រើប្រាស់ទីតាំងបច្ចុប្បន្នបានទេ", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("ការអនុញ្ញាតទីតាំងត្រូវបានបដិសេធ")
                .setMessage("អ្នកបានបដិសេធមិនអនុញ្ញាតឱ្យប្រើប្រាស់ទីតាំង។ សូមចូលទៅកាន់ការកំណត់ និងអនុញ្ញាតឱ្យប្រើប្រាស់ទីតាំងដើម្បីប្រើប្រាស់មុខងារនេះ។")
                .setPositiveButton("ទៅកាន់ការកំណត់", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("បោះបង់", (dialog, which) -> {
                    Toast.makeText(this, "មិនអាចប្រើប្រាស់ទីតាំងបច្ចុប្បន្នបានទេ", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void setupKeyboardHandling() {
        binding.main.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int heightDiff = binding.main.getRootView().getHeight() - binding.main.getHeight();
            if (heightDiff > 200) {
                if (mMap != null) {
                    mMap.getUiSettings().setScrollGesturesEnabled(false);
                }
            } else {
                if (mMap != null) {
                    mMap.getUiSettings().setScrollGesturesEnabled(true);
                }
                if (suggestionsContainer != null && suggestionsContainer.getVisibility() == View.VISIBLE) {
                    suggestionsContainer.setVisibility(View.GONE);
                }
            }
        });
    }

    private void getCurrentLocation() {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "សូមអនុញ្ញាតឱ្យប្រើប្រាស់ទីតាំងរបស់អ្នក", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading();

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        hideLoading();
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            if (mMap != null) {
                                clearProvinceBorders();
                                selectedLatLng = currentLocation;
                                addMarker(currentLocation, "ទីតាំងរបស់អ្នក");
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
                                getProvinceName(currentLocation);
                                Toast.makeText(this, "បានរកឃើញទីតាំងរបស់អ្នក", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "មិនអាចទទួលបានទីតាំងបច្ចុប្បន្ន។ សូមពិនិត្យ GPS របស់អ្នក", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        Toast.makeText(this, "កំហុសក្នុងការទទួលទីតាំង: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("MapPicker", "Location error: " + e.getMessage());
                    });
        } catch (SecurityException e) {
            hideLoading();
            Toast.makeText(this, "សូមអនុញ្ញាតឱ្យប្រើប្រាស់ទីតាំងរបស់អ្នក", Toast.LENGTH_SHORT).show();
            Log.e("MapPicker", "Permission error: " + e.getMessage());
        }
    }

    private void createSuggestionsContainer() {
        suggestionsContainer = new MaterialCardView(this);
        suggestionsContainer.setCardElevation(8f);
        suggestionsContainer.setRadius(12f);
        suggestionsContainer.setVisibility(View.GONE);

        suggestionsLayout = new LinearLayout(this);
        suggestionsLayout.setOrientation(LinearLayout.VERTICAL);
        suggestionsLayout.setPadding(16, 8, 16, 8);

        suggestionsContainer.addView(suggestionsLayout);

        ((android.view.ViewGroup) etSearchLocation.getParent()).addView(suggestionsContainer);

        suggestionsContainer.post(() -> {
            int width = etSearchLocation.getWidth();
            suggestionsContainer.setLayoutParams(new android.view.ViewGroup.LayoutParams(width,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            suggestionsContainer.setX(etSearchLocation.getX());
            suggestionsContainer.setY(etSearchLocation.getY() + etSearchLocation.getHeight());
        });
    }

    private void setupSearch() {
        etSearchLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("MapPicker", "Text changed: " + s + ", length: " + s.length());

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                if (s.length() > 1) {
                    showLoading();
                    autoSelectPending = true;
                    final String query = s.toString();
                    searchRunnable = () -> {
                        Log.d("MapPicker", "Executing search for: " + query);
                        performSearch(query);
                    };
                    searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY);
                } else if (s.length() == 0) {
                    hideLoading();
                    suggestionsContainer.setVisibility(View.GONE);
                    clearProvinceBorders();
                    autoSelectPending = false;
                } else {
                    hideLoading();
                    autoSelectPending = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = v.getText().toString();
                Log.d("MapPicker", "Search button pressed: " + query);
                if (!query.isEmpty()) {
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    closeKeyboard();
                    autoSelectPending = true;
                    performSearch(query);
                }
                return true;
            }
            return false;
        });

        etSearchLocation.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d("MapPicker", "Search edit text focus: " + hasFocus);
            if (!hasFocus) {
                new Handler().postDelayed(() -> {
                    if (suggestionsContainer != null && !etSearchLocation.hasFocus()) {
                        suggestionsContainer.setVisibility(View.GONE);
                        autoSelectPending = false;
                    }
                }, 200);
            }
        });
    }

    private void performSearch(String query) {
        if (isSearching) {
            Log.d("MapPicker", "Already searching, skipping");
            return;
        }

        if (placesClient == null) {
            Log.e("MapPicker", "Places client is null - reinitializing");
            initializePlacesAPI();
            if (placesClient == null) {
                hideLoading();
                Toast.makeText(this, "Places API not available. Please check your API key.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        isSearching = true;
        Log.d("MapPicker", "Performing search for: " + query);

        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        RectangularBounds bounds = RectangularBounds.newInstance(
                CAMBODIA_SOUTHWEST,
                CAMBODIA_NORTHEAST
        );

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setLocationBias(bounds)
                .setCountries("KH")
                .setSessionToken(token)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener((response) -> {
                    isSearching = false;
                    hideLoading();
                    List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
                    Log.d("MapPicker", "Search successful, got " + predictions.size() + " predictions");

                    if (predictions != null && !predictions.isEmpty()) {
                        if (autoSelectPending) {
                            autoSelectPending = false;
                            String firstSuggestion = predictions.get(0).getPrimaryText(null).toString();
                            Log.d("MapPicker", "Auto-selecting first suggestion: " + firstSuggestion);

                            etSearchLocation.setText(firstSuggestion);
                            suggestionsContainer.setVisibility(View.GONE);
                            fetchPlaceDetails(predictions.get(0).getPlaceId());
                        } else {
                            updateSuggestions(predictions);
                        }
                    } else {
                        suggestionsContainer.setVisibility(View.GONE);
                        Toast.makeText(this, "មិនឃើញលទ្ធផលស្វែងរក", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener((exception) -> {
                    isSearching = false;
                    hideLoading();
                    String errorMessage = exception.getMessage();
                    Log.e("MapPicker", "Search failed", exception);

                    Toast.makeText(this, "ស្វែងរកមិនជោគជ័យ: " + errorMessage, Toast.LENGTH_LONG).show();

                    if (errorMessage != null && errorMessage.contains("9011")) {
                        Toast.makeText(this, "Please enable Places API in Google Cloud Console", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateSuggestions(List<AutocompletePrediction> predictions) {
        suggestionsLayout.removeAllViews();
        Log.d("MapPicker", "Updating suggestions, count: " + predictions.size());

        if (predictions.isEmpty()) {
            suggestionsContainer.setVisibility(View.GONE);
            hideLoading();
            return;
        }

        for (AutocompletePrediction prediction : predictions) {
            String predictionText = prediction.getPrimaryText(null).toString();
            Log.d("MapPicker", "Adding suggestion: " + predictionText);

            MaterialTextView suggestionView = new MaterialTextView(this);
            suggestionView.setText(predictionText);
            suggestionView.setPadding(16, 12, 16, 12);
            suggestionView.setTextSize(14);
            suggestionView.setBackgroundResource(android.R.drawable.list_selector_background);

            suggestionView.setOnClickListener(v -> {
                Log.d("MapPicker", "Selected suggestion: " + predictionText);
                etSearchLocation.setText(predictionText);
                suggestionsContainer.setVisibility(View.GONE);
                hideLoading();
                fetchPlaceDetails(prediction.getPlaceId());
            });

            suggestionsLayout.addView(suggestionView);

            if (predictions.indexOf(prediction) < predictions.size() - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                suggestionsLayout.addView(divider);
            }
        }

        suggestionsContainer.setVisibility(View.VISIBLE);
        hideLoading();
    }

    private void fetchPlaceDetails(String placeId) {
        showLoading();
        Log.d("MapPicker", "Fetching details for placeId: " + placeId);

        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.LOCATION,
                Place.Field.DISPLAY_NAME,
                Place.Field.FORMATTED_ADDRESS
        );

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request)
                .addOnSuccessListener((response) -> {
                    hideLoading();
                    Place place = response.getPlace();
                    LatLng latLng = place.getLocation();
                    Log.d("MapPicker", "Place details fetched, location: " + latLng);

                    if (latLng != null && mMap != null) {
                        clearProvinceBorders();
                        selectedLatLng = latLng;
                        String placeName = place.getDisplayName() != null ? place.getDisplayName().toString() : null;
                        String placeAddress = place.getFormattedAddress() != null ? place.getFormattedAddress().toString() : null;
                        String title = placeName != null ? placeName :
                                (placeAddress != null ? placeAddress : "Selected Location");

                        // Add marker at the location
                        addMarker(latLng, title);

                        // Draw red border for the province if applicable
                        drawProvinceBorder(title);

                        // Move camera to the location
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                        getProvinceName(latLng);

                        Toast.makeText(this, "បានរកឃើញ: " + title, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener((exception) -> {
                    hideLoading();
                    Log.e("MapPicker", "Fetch place failed", exception);
                    Toast.makeText(this, "មិនអាចទាញយកព័ត៌មានទីតាំងបានទេ", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CAMBODIA_CENTER, CAMBODIA_ZOOM));

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (SecurityException e) {
            Log.e("MapPicker", "Permission error: " + e.getMessage());
        }

        mMap.setOnMapClickListener(latLng -> {
            autoSelectPending = false;
            clearProvinceBorders();

            selectedLatLng = latLng;
            addMarker(latLng, "Selected Location");
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
            getProvinceName(latLng);
        });

        Log.d("MapPicker", "Map is ready");
    }

    private void getProvinceName(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                selectedProvince = addresses.get(0).getAdminArea();

                if (selectedProvince == null) selectedProvince = "Unknown";
                else {
                    selectedProvince = selectedProvince
                            .replace("Province", "")
                            .replace("City", "")
                            .replace("Municipality", "")
                            .replace("State", "")
                            .replace("ខេត្ត", "")
                            .replace("រាជធានី", "")
                            .trim();

                    selectedProvince = toTitleCase(selectedProvince);
                }

                binding.locationButton.setText("ជ្រើសរើសទីតាំង (" + selectedProvince + ")");
            } else {
                selectedProvince = "";
                binding.locationButton.setText("ជ្រើសរើសទីតាំង");
            }
        } catch (IOException e) {
            e.printStackTrace();
            selectedProvince = "";
            binding.locationButton.setText("ជ្រើសរើសទីតាំង");
        }
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        input = input.toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    public void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    public void showLoading() {
        binding.loading.setVisibility(View.VISIBLE);
    }

    public void hideLoading() {
        binding.loading.postDelayed(() -> {
            binding.loading.setVisibility(View.GONE);
        }, 500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (!isWaitingForPermission && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                try {
                    mMap.setMyLocationEnabled(true);
                } catch (SecurityException e) {
                    Log.e("MapPicker", "Error enabling location: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
                if (mMap != null) {
                    try {
                        mMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        Log.e("MapPicker", "Error enabling location: " + e.getMessage());
                    }
                }
            } else {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showPermissionDeniedDialog();
                } else {
                    Toast.makeText(this, "មិនអាចប្រើប្រាស់ទីតាំងបច្ចុប្បន្នបានទេ", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}