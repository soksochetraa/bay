package com.example.bay.service;

import com.example.bay.model.User;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.POST;
import retrofit2.http.DELETE;
import retrofit2.http.Path;

public interface UserService {

    // 🔹 Get all users
    @GET("users.json")
    Call<Map<String, User>> getAllUsers();

    // 🔹 Get a single user by ID
    @GET("users/{userId}.json")
    Call<User> getUserById(@Path("userId") String userId);

    // 🔹 Create a new user (Firebase auto-generates the key)
    @POST("users.json")
    Call<User> createUser(@Body User user);

    // 🔹 Update an existing user
    @PUT("users/{userId}.json")
    Call<User> updateUser(@Path("userId") String userId, @Body User user);

    // 🔹 Delete a user
    @DELETE("users/{userId}.json")
    Call<Void> deleteUser(@Path("userId") String userId);
}
