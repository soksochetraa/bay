package com.example.bay.service;

import com.example.bay.model.Location;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FarmMapService {

    // ✅ 1. Get all locations
    @GET("locations.json")
    Call<Map<String, Location>> getAllLocations();

    // ✅ 2. Get single location by ID
    @GET("locations/{id}.json")
    Call<Location> getLocationById(@Path("id") String id);

    // ✅ 3. Create a new location
    @POST("locations.json")
    Call<Void> createLocation(@Body Location location);

    // ✅ 4. Update an existing location (partial update)
    @PATCH("locations/{id}.json")
    Call<Void> updateLocation(@Path("id") String id, @Body Location location);

    // ✅ 5. Delete a location
    @DELETE("locations/{id}.json")
    Call<Void> deleteLocation(@Path("id") String id);
}
