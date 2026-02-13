package com.example.bay.repository;

import androidx.annotation.NonNull;

import com.example.bay.model.Location;
import com.example.bay.service.FarmMapService;
import com.example.bay.util.RetrofitClient;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LocationRepository {

    private final FarmMapService service;

    public LocationRepository() {
        Retrofit retrofit = RetrofitClient.getClient();
        service = retrofit.create(FarmMapService.class);
    }

    public void getAllLocations(LocationCallback<Map<String, Location>> callback) {
        service.getAllLocations().enqueue(new Callback<Map<String, Location>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Location>> call, @NonNull Response<Map<String, Location>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("Failed to load locations: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Location>> call, @NonNull Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void getLocationById(String id, LocationCallback<Location> callback) {
        service.getLocationById(id).enqueue(new Callback<Location>() {
            @Override
            public void onResponse(@NonNull Call<Location> call, @NonNull Response<Location> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("Failed to get location: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Location> call, @NonNull Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void createLocation(Location location, LocationCallback<String> callback) {
        service.createLocation(location).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Map<String, String> body = response.body();
                    String id = body != null ? body.get("name") : null;
                    callback.onSuccess(id);
                } else {
                    callback.onFailure("Failed to create location: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void updateLocation(String id, Location location, LocationCallback<Void> callback) {
        service.updateLocation(id, location).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure("Failed to update location: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void deleteLocation(String id, LocationCallback<Void> callback) {
        service.deleteLocation(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure("Failed to delete location: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public interface LocationCallback<T> {
        void onSuccess(T result);
        void onFailure(String error);
    }
}
