package com.example.bay;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import com.example.bay.BaseActivity;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
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

public class MapPickerActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedProvince = "";
    private ActivityMapPickerBinding binding;
    private EditText etSearchLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int DEFAULT_ZOOM = 15;
    private static final int CAMBODIA_ZOOM = 7;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private static final LatLng CAMBODIA_SOUTHWEST = new LatLng(9.0, 102.0);
    private static final LatLng CAMBODIA_NORTHEAST = new LatLng(15.0, 108.0);
    private static final LatLng CAMBODIA_CENTER = new LatLng(12.5657, 104.9910);

    private ActivityResultLauncher<String> locationPermissionLauncher;
    private boolean isWaitingForPermission = false;

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

        etSearchLocation = findViewById(R.id.etSearchLocation);

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

    private void setupSearch() {
        etSearchLocation.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearchLocation.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchLocation(query);
                    hideKeyboard();
                } else {
                    Toast.makeText(this, "សូមបញ្ចូលទីតាំងស្វែងរក", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    private void searchLocation(String query) {
        showLoading();
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                runOnUiThread(() -> {
                    hideLoading();
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        
                        clearProvinceBorders();
                        selectedLatLng = latLng;
                        addMarker(latLng, query);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f));
                        
                        getProvinceName(latLng);
                        drawProvinceBorder(selectedProvince);
                    } else {
                        Toast.makeText(this, "រកមិនឃើញទីតាំង!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(this, "មានបញ្ហាក្នុងការស្វែងរក!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void hideKeyboard() {
        etSearchLocation.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etSearchLocation.getWindowToken(), 0);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Apply dark mode map style if the system is in night mode
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            try {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
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
    }
}