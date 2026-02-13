package com.example.bay;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

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

        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
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
                });
    }

    private void resolveShortKhmerAddress(LatLng latLng) {
        Geocoder geocoder = new Geocoder(requireContext(), new Locale("km", "KH"));
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);

                String subAdmin = clean(safe(a.getSubAdminArea()));
                String admin = clean(safe(a.getAdminArea()));

                if (TextUtils.isEmpty(subAdmin)) {
                    String locality = clean(safe(a.getLocality()));
                    subAdmin = locality;
                }

                admin = normalizeAdminForPhnomPenh(admin);

                String out;
                if (!TextUtils.isEmpty(subAdmin) && !TextUtils.isEmpty(admin)) {
                    out = subAdmin + ", " + admin;
                } else if (!TextUtils.isEmpty(admin)) {
                    out = admin;
                } else if (!TextUtils.isEmpty(subAdmin)) {
                    out = subAdmin;
                } else {
                    out = "ទីតាំងបានជ្រើសរើស";
                }

                selectedAddress = out;
            } else {
                selectedAddress = "ទីតាំងបានជ្រើសរើស";
            }
        } catch (IOException e) {
            selectedAddress = "ទីតាំងបានជ្រើសរើស";
        }
    }

    private String normalizeAdminForPhnomPenh(String admin) {
        if (TextUtils.isEmpty(admin)) return "";
        if (admin.contains("ភ្នំពេញ")) return "ភ្នំពេញ";
        return admin;
    }

    private String clean(String s) {
        if (TextUtils.isEmpty(s)) return "";
        String out = s.trim();
        out = out.replaceAll("^[A-Z0-9]{4}\\+[A-Z0-9]{3},\\s*", "");
        out = out.replaceAll("^[A-Z0-9]{4}\\+[A-Z0-9]{3}\\s*", "");
        out = out.replaceAll("\\s+,", ",");
        out = out.replaceAll(",\\s*,", ",");
        out = out.replaceAll("\\s{2,}", " ");
        out = out.replace("Cambodia", "")
                .replace("កម្ពុជា", "")
                .trim();
        if (out.endsWith(",")) out = out.substring(0, out.length() - 1).trim();
        return out;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
