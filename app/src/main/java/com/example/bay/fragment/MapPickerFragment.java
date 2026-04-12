package com.example.bay.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.databinding.FragmentMapPickerBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_REQUEST = 1001;

    private FragmentMapPickerBinding binding;
    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedAddress = "";

    private FusedLocationProviderClient locationClient;
    private HomeActivity homeActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMapPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (getActivity() instanceof HomeActivity) {
            homeActivity = (HomeActivity) getActivity();
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.locationButton.setOnClickListener(v -> {
            if (selectedLatLng == null || TextUtils.isEmpty(selectedAddress)) {
                Toast.makeText(requireContext(), "សូមជ្រើសរើសទីតាំងជាមុន", Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle result = new Bundle();
            result.putDouble("latitude", selectedLatLng.latitude);
            result.putDouble("longitude", selectedLatLng.longitude);
            result.putString("address", selectedAddress);

            getParentFragmentManager().setFragmentResult("map_picker_result", result);
            getParentFragmentManager().popBackStack();
        });

        binding.btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());

        setupSearch();
    }

    private void setupSearch() {
        binding.etSearchLocation.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.etSearchLocation.getText().toString().trim();
                if (!TextUtils.isEmpty(query)) {
                    searchLocation(query);
                    hideKeyboard();
                } else {
                    Toast.makeText(requireContext(), "សូមបញ្ចូលទីតាំងស្វែងរក", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    private void searchLocation(String query) {
        showLoading();
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    hideLoading();
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        mMap.clear();
                        selectedLatLng = latLng;
                        mMap.addMarker(new MarkerOptions().position(latLng).title(query));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f));
                        resolveShortKhmerAddress(latLng);
                    } else {
                        Toast.makeText(requireContext(), "រកមិនឃើញទីតាំង!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(requireContext(), "មានបញ្ហាក្នុងការស្វែងរក!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void hideKeyboard() {
        if (binding == null || getActivity() == null) return;
        binding.etSearchLocation.clearFocus();
        InputMethodManager imm = (InputMethodManager) requireActivity()
                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.etSearchLocation.getWindowToken(), 0);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng cambodia = new LatLng(12.5657, 104.9910);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cambodia, 7f));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            selectedLatLng = latLng;
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f));
            resolveShortKhmerAddress(latLng);
        });
    }

    private void moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST
            );
            return;
        }

        showLoading();

        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    hideLoading();
                    if (location == null) {
                        Toast.makeText(requireContext(), "មិនអាចយកទីតាំងបានទេ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.clear();
                    selectedLatLng = latLng;
                    mMap.addMarker(new MarkerOptions().position(latLng).title("Your Location"));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
                    resolveShortKhmerAddress(latLng);
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(requireContext(), "មានបញ្ហាក្នុងការទទួលទីតាំង", Toast.LENGTH_SHORT).show();
                });
    }

    private void resolveShortKhmerAddress(LatLng latLng) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);
                String admin = safe(a.getAdminArea());

                if (!TextUtils.isEmpty(admin)) {
                    selectedAddress = stripSuffix(admin);
                } else {
                    // Fallback to locality if admin is empty
                    String locality = safe(a.getLocality());
                    selectedAddress = !TextUtils.isEmpty(locality) ? stripSuffix(locality) : "Unknown Location";
                }
            } else {
                selectedAddress = "Unknown Location";
            }
        } catch (IOException e) {
            selectedAddress = "Unknown Location";
        }
    }

    /**
     * Strips common English geographic suffixes like "Province", "Municipality", etc.
     */
    private String stripSuffix(String s) {
        if (TextUtils.isEmpty(s)) return "";
        return s.replaceAll("(?i)\\s*(Province|Municipality|District|Commune|City|Khan)\\s*$", "").trim();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void showLoading() {
        if (homeActivity != null) homeActivity.showLoading();
    }

    private void hideLoading() {
        if (homeActivity != null) homeActivity.hideLoading();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
