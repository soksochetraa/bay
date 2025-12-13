package com.example.bay.service;

import com.example.bay.model.User;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface UserService {

    @GET("users.json")
    Call<Map<String, User>> getAllUsers();

    @GET("users/{userId}.json")
    Call<User> getUserById(@Path("userId") String userId);

    @PUT("users/{userId}.json")
    Call<User> createUser(@Path("userId") String userId, @Body User user);

    @PUT("users/{userId}.json")
    Call<User> updateUser(@Path("userId") String userId, @Body User user);

    @DELETE("users/{userId}.json")
    Call<Void> deleteUser(@Path("userId") String userId);
}
