package com.example.bay;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.bay.databinding.ActivityMapPickerBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedProvince = "";
    private ActivityMapPickerBinding binding;

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

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
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
                Toast.makeText(this, "Please tap a location on the map first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng cambodia = new LatLng(12.5657, 104.9910);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cambodia, 7));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            selectedLatLng = latLng;
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8));
            getProvinceName(latLng);
        });
    }

    private void getProvinceName(LatLng latLng) {
        // Force Geocoder to return English results
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                selectedProvince = addresses.get(0).getAdminArea();

                if (selectedProvince == null) selectedProvince = "Unknown";
                else {
                    // 🔹 Remove unwanted suffixes or Khmer prefixes
                    selectedProvince = selectedProvince
                            .replace("Province", "")
                            .replace("City", "")
                            .replace("Municipality", "")
                            .replace("State", "")
                            .replace("ខេត្ត", "")
                            .replace("រាជធានី", "")
                            .trim();

                    // 🔹 Ensure proper capitalization
                    selectedProvince = toTitleCase(selectedProvince);
                }

                binding.locationButton.setText("ជ្រើសរើសទីតាំង (" + selectedProvince + ")");
            } else {
                selectedProvince = "";
                binding.locationButton.setText("ទីតាំងមិនត្រឹមត្រូវ");
            }
        } catch (IOException e) {
            e.printStackTrace();
            selectedProvince = "";
            binding.locationButton.setText("មានបញ្ហា​ក្នុងការទទួលទិន្នន័យ");
        }
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        input = input.toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
