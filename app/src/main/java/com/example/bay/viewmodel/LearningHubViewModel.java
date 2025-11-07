    package com.example.bay.viewmodel;

    import androidx.lifecycle.LiveData;
    import androidx.lifecycle.MutableLiveData;
    import androidx.lifecycle.ViewModel;
    import com.example.bay.model.LearninghubCard;
    import com.example.bay.repository.LearningHubRepository;
    import java.util.List;

    public class LearningHubViewModel extends ViewModel {
        private final LearningHubRepository repository;
        private final MutableLiveData<Boolean> refreshSavedCards = new MutableLiveData<>();
        private final MutableLiveData<String> updatedCardId = new MutableLiveData<>();

        public LearningHubViewModel() {
            repository = new LearningHubRepository();
            refreshSavedCards.setValue(false);
        }

        public LiveData<List<LearninghubCard>> getCards() {
            return repository.getCardsLiveData();
        }

        public LiveData<List<LearninghubCard>> getSavedCards() {
            return repository.getSavedCardsLiveData();
        }

        public LiveData<String> getErrorMessage() {
            return repository.getErrorMessage();
        }

        public LiveData<Boolean> getRefreshSavedCards() {
            return refreshSavedCards;
        }

        public LiveData<String> getUpdatedCardId() {
            return updatedCardId;
        }

        public void loadCards() {
            repository.loadCards();
        }

        public void loadCardsByCategory(String category) {
            repository.loadCardsByCategory(category);
        }

        public void toggleSaveCard(String cardUuid, boolean isSaved) {
            repository.toggleSaveCard(cardUuid, isSaved);
            updatedCardId.postValue(cardUuid);
            if (!isSaved) {
                refreshSavedCards.postValue(true);
            }
        }

        public void loadSavedCards() {
            repository.loadSavedCards();
        }

        public void refreshSavedCardsComplete() {
            refreshSavedCards.postValue(false);
        }
        public void refreshAllData() {
            repository.loadCards();
            repository.loadSavedCards();
        }
    }