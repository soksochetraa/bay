package com.example.bay.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.bay.model.LearninghubCard;
import com.example.bay.service.LearningHubService;
import com.example.bay.util.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LearningHubRepository {

    private final LearningHubService apiService;
    private final MutableLiveData<List<LearninghubCard>> cardsLiveData;
    private final MutableLiveData<List<LearninghubCard>> savedCardsLiveData; // ADD THIS
    private final MutableLiveData<String> errorMessage;
    private final String currentUserId;

    public LearningHubRepository() {
        apiService = RetrofitClient.getClient().create(LearningHubService.class);
        cardsLiveData = new MutableLiveData<>();
        savedCardsLiveData = new MutableLiveData<>(); // INITIALIZE
        errorMessage = new MutableLiveData<>();

        // Get current user ID safely
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : "anonymous";

        // Initialize with empty lists
        cardsLiveData.setValue(new ArrayList<>());
        savedCardsLiveData.setValue(new ArrayList<>());
    }

    public LiveData<List<LearninghubCard>> getCardsLiveData() {
        return cardsLiveData;
    }

    // ADD THIS METHOD
    public LiveData<List<LearninghubCard>> getSavedCardsLiveData() {
        return savedCardsLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadCards() {
        apiService.getAllCards().enqueue(new Callback<Map<String, LearninghubCard>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, LearninghubCard>> call,
                                   @NonNull Response<Map<String, LearninghubCard>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LearninghubCard> cards = new ArrayList<>(response.body().values());
                    checkSavedStatusForCards(cards, cardsLiveData);
                } else {
                    String errorMsg = response.message() != null ?
                            response.message() : "Unknown error loading cards";
                    errorMessage.postValue("Failed to load cards: " + errorMsg);
                    cardsLiveData.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, LearninghubCard>> call,
                                  @NonNull Throwable t) {
                errorMessage.postValue("Network error: " + t.getMessage());
                cardsLiveData.postValue(new ArrayList<>());
            }
        });
    }

    public void loadCardsByCategory(String category) {
        if ("all".equals(category)) {
            loadCards();
            return;
        }

        apiService.getCardsByCategory("category", category).enqueue(new Callback<Map<String, LearninghubCard>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, LearninghubCard>> call,
                                   @NonNull Response<Map<String, LearninghubCard>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LearninghubCard> cards = new ArrayList<>(response.body().values());
                    checkSavedStatusForCards(cards, cardsLiveData);
                } else {
                    String errorMsg = response.message() != null ?
                            response.message() : "Unknown error loading filtered cards";
                    errorMessage.postValue("Failed to load filtered cards: " + errorMsg);
                    cardsLiveData.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, LearninghubCard>> call,
                                  @NonNull Throwable t) {
                errorMessage.postValue("Network error: " + t.getMessage());
                cardsLiveData.postValue(new ArrayList<>());
            }
        });
    }

    public void toggleSaveCard(String cardUuid, boolean isSaved) {
        if (isSaved) {
            // Save card for user
            apiService.saveCardForUser(currentUserId, cardUuid, true).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        updateCardStatusInList(cardUuid, true);
                        // Refresh saved cards list
                        loadSavedCards();
                    } else {
                        String errorMsg = response.message() != null ?
                                response.message() : "Unknown error saving card";
                        errorMessage.postValue("Failed to save card: " + errorMsg);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    errorMessage.postValue("Network error: " + t.getMessage());
                }
            });
        } else {
            // Unsave card for user
            apiService.unsaveCardForUser(currentUserId, cardUuid).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        updateCardStatusInList(cardUuid, false);
                        // Refresh saved cards list
                        loadSavedCards();
                    } else {
                        String errorMsg = response.message() != null ?
                                response.message() : "Unknown error unsaving card";
                        errorMessage.postValue("Failed to unsave card: " + errorMsg);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    errorMessage.postValue("Network error: " + t.getMessage());
                }
            });
        }
    }

    public void loadSavedCards() {
        apiService.getUserSavedCards(currentUserId).enqueue(new Callback<Map<String, Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Boolean>> call,
                                   @NonNull Response<Map<String, Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> savedCardUuids = new ArrayList<>();
                    for (Map.Entry<String, Boolean> entry : response.body().entrySet()) {
                        if (Boolean.TRUE.equals(entry.getValue())) {
                            savedCardUuids.add(entry.getKey());
                        }
                    }
                    loadCardsByUuids(savedCardUuids);
                } else {
                    savedCardsLiveData.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Boolean>> call,
                                  @NonNull Throwable t) {
                errorMessage.postValue("Failed to load saved cards: " + t.getMessage());
                savedCardsLiveData.postValue(new ArrayList<>());
            }
        });
    }

    public void loadCardById(String cardId) {
        apiService.getCardById(cardId).enqueue(new Callback<LearninghubCard>() {
            @Override
            public void onResponse(@NonNull Call<LearninghubCard> call, @NonNull Response<LearninghubCard> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LearninghubCard card = response.body();
                    // Check if this card is saved
                    checkSavedStatusForSingleCard(card);
                } else {
                    errorMessage.postValue("Failed to load card details");
                }
            }

            @Override
            public void onFailure(@NonNull Call<LearninghubCard> call, @NonNull Throwable t) {
                errorMessage.postValue("Network error: " + t.getMessage());
            }
        });
    }

    public void refreshAllData() {
        loadCards();
        loadSavedCards();
    }

    private void checkSavedStatusForSingleCard(LearninghubCard card) {
        apiService.getUserSavedCards(currentUserId).enqueue(new Callback<Map<String, Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Boolean>> call, @NonNull Response<Map<String, Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Boolean> savedCards = response.body();
                    card.setIsSaved(Boolean.TRUE.equals(savedCards.get(card.getUuid())));
                }
                // Create a list with single card and post it
                List<LearninghubCard> singleCardList = new ArrayList<>();
                singleCardList.add(card);
                cardsLiveData.postValue(singleCardList);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Boolean>> call, @NonNull Throwable t) {
                card.setIsSaved(false);
                List<LearninghubCard> singleCardList = new ArrayList<>();
                singleCardList.add(card);
                cardsLiveData.postValue(singleCardList);
            }
        });
    }

    private void checkSavedStatusForCards(List<LearninghubCard> cards, MutableLiveData<List<LearninghubCard>> targetLiveData) {
        apiService.getUserSavedCards(currentUserId).enqueue(new Callback<Map<String, Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Boolean>> call,
                                   @NonNull Response<Map<String, Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Boolean> savedCards = response.body();
                    for (LearninghubCard card : cards) {
                        card.setIsSaved(Boolean.TRUE.equals(savedCards.get(card.getUuid())));
                    }
                    targetLiveData.postValue(cards);
                } else {
                    // If failed, set all to not saved
                    for (LearninghubCard card : cards) {
                        card.setIsSaved(false);
                    }
                    targetLiveData.postValue(cards);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Boolean>> call,
                                  @NonNull Throwable t) {
                // On failure, set all to not saved
                for (LearninghubCard card : cards) {
                    card.setIsSaved(false);
                }
                targetLiveData.postValue(cards);
            }
        });
    }

    private void loadCardsByUuids(List<String> cardUuids) {
        if (cardUuids.isEmpty()) {
            savedCardsLiveData.postValue(new ArrayList<>());
            return;
        }

        // Load all cards and filter by saved UUIDs
        apiService.getAllCards().enqueue(new Callback<Map<String, LearninghubCard>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, LearninghubCard>> call,
                                   @NonNull Response<Map<String, LearninghubCard>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LearninghubCard> allCards = new ArrayList<>(response.body().values());
                    List<LearninghubCard> savedCards = new ArrayList<>();

                    for (LearninghubCard card : allCards) {
                        if (cardUuids.contains(card.getUuid())) {
                            card.setIsSaved(true);
                            savedCards.add(card);
                        }
                    }
                    savedCardsLiveData.postValue(savedCards);
                } else {
                    String errorMsg = response.message() != null ?
                            response.message() : "Unknown error loading cards";
                    errorMessage.postValue("Failed to load cards: " + errorMsg);
                    savedCardsLiveData.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, LearninghubCard>> call,
                                  @NonNull Throwable t) {
                errorMessage.postValue("Network error: " + t.getMessage());
                savedCardsLiveData.postValue(new ArrayList<>());
            }
        });
    }

    private void updateCardStatusInList(String cardUuid, boolean isSaved) {
        // Update in all cards list
        List<LearninghubCard> currentAllCards = cardsLiveData.getValue();
        if (currentAllCards != null) {
            for (LearninghubCard card : currentAllCards) {
                if (card.getUuid().equals(cardUuid)) {
                    card.setIsSaved(isSaved);
                    break;
                }
            }
            cardsLiveData.postValue(new ArrayList<>(currentAllCards));
        }

        // Update in saved cards list if unsaving
        if (!isSaved) {
            List<LearninghubCard> currentSavedCards = savedCardsLiveData.getValue();
            if (currentSavedCards != null) {
                List<LearninghubCard> updatedSavedCards = new ArrayList<>();
                for (LearninghubCard card : currentSavedCards) {
                    if (!card.getUuid().equals(cardUuid)) {
                        updatedSavedCards.add(card);
                    }
                }
                savedCardsLiveData.postValue(updatedSavedCards);
            }
        }
    }
}