package com.example.bay.service;

import com.example.bay.model.LearninghubCard;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface LearningHubService {

    // 🔹 Get all cards
    @GET("learning_hub/cards.json")
    Call<Map<String, LearninghubCard>> getAllCards();

    // 🔹 Get a single card by UUID
    @GET("learning_hub/cards/{cardUuid}.json")
    Call<LearninghubCard> getCardById(@Path("cardUuid") String cardUuid);

    // 🔹 Create a new card
    @PUT("learning_hub/cards/{cardUuid}.json")
    Call<LearninghubCard> createCard(@Path("cardUuid") String cardUuid, @Body LearninghubCard card);

    // 🔹 Update an existing card
    @PUT("learning_hub/cards/{cardUuid}.json")
    Call<LearninghubCard> updateCard(@Path("cardUuid") String cardUuid, @Body LearninghubCard card);

    // 🔹 Delete a card
    @DELETE("learning_hub/cards/{cardUuid}.json")
    Call<Void> deleteCard(@Path("cardUuid") String cardUuid);

    // 🔹 Get user's saved cards
    @GET("learning_hub/user_saved_cards/{userId}.json")
    Call<Map<String, Boolean>> getUserSavedCards(@Path("userId") String userId);

    // 🔹 Save a card for user
    @PUT("learning_hub/user_saved_cards/{userId}/{cardUuid}.json")
    Call<Void> saveCardForUser(@Path("userId") String userId,
                               @Path("cardUuid") String cardUuid,
                               @Body Boolean isSaved);

    // 🔹 Unsave a card for user
    @DELETE("learning_hub/user_saved_cards/{userId}/{cardUuid}.json")
    Call<Void> unsaveCardForUser(@Path("userId") String userId,
                                 @Path("cardUuid") String cardUuid);

    // 🔹 Get cards by category (Firebase requires quotes in query)
    @GET("learning_hub/cards.json")
    Call<Map<String, LearninghubCard>> getCardsByCategory(
            @Query("orderBy") String orderBy,
            @Query("equalTo") String category
    );

    // ⚙️ Helper static method for encoding query safely
    static String quote(String value) {
        return "\"" + value + "\"";
    }
}
