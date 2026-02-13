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

    @GET("locations.json")
    Call<Map<String, Location>> getAllLocations();

    @GET("locations/{id}.json")
    Call<Location> getLocationById(@Path("id") String id);

    @POST("locations.json")
    Call<Map<String, String>> createLocation(@Body Location location);

    @PATCH("locations/{id}.json")
    Call<Void> updateLocation(@Path("id") String id, @Body Location location);

    @DELETE("locations/{id}.json")
    Call<Void> deleteLocation(@Path("id") String id);
}
