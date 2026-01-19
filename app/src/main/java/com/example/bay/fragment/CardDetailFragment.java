package com.example.bay.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.RelatedCardsAdapter;
import com.example.bay.databinding.FragmentCardDetailBinding;
import com.example.bay.model.LearninghubCard;
import com.example.bay.viewmodel.LearningHubViewModel;

import java.util.ArrayList;
import java.util.List;

public class CardDetailFragment extends Fragment {

    private static final String TAG = "CardDetailFragment";

    private FragmentCardDetailBinding binding;

    private LearningHubViewModel viewModel;
    private LearninghubCard currentCard;

    private RelatedCardsAdapter relatedCardsAdapter;
    private final List<LearninghubCard> relatedCards = new ArrayList<>();

    private String currentCardId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCardDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            currentCardId = getArguments().getString("card_id");
            Log.d(TAG, "Received card_id: " + currentCardId);
        }

        setupViewModel();
        setupRelatedCards();
        setupClickListeners();

        if (currentCardId != null) loadCardData();
    }

    @Override
    public void onResume() {
        super.onResume();
        hideNavBar();
    }

    @Override
    public void onPause() {
        super.onPause();
        showNavBar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showNavBar();
        binding = null;
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(LearningHubViewModel.class);

        viewModel.getCards().observe(getViewLifecycleOwner(), cards -> {
            Log.d(TAG, "Cards observed: " + (cards != null ? cards.size() : 0));
            if (cards == null || cards.isEmpty() || currentCardId == null) return;

            findCurrentCard(cards, currentCardId);
            loadRelatedCards(cards);
        });

        viewModel.getUpdatedCardId().observe(getViewLifecycleOwner(), cardId -> {
            if (cardId != null && cardId.equals(currentCardId)) {
                viewModel.loadCards();
            }
        });
    }

    private void setupRelatedCards() {
        relatedCardsAdapter = new RelatedCardsAdapter();
        relatedCardsAdapter.setOnRelatedCardClickListener(this::openRelatedCardDetail);

        binding.rvRelatedCards.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );
        binding.rvRelatedCards.setAdapter(relatedCardsAdapter);
        binding.rvRelatedCards.setNestedScrollingEnabled(true);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> navigateBack());

        binding.btnSave.setOnClickListener(v -> {
            if (currentCard == null) return;

            boolean newSavedState = !currentCard.getIsSaved();
            viewModel.toggleSaveCard(currentCard.getUuid(), newSavedState);
            currentCard.setIsSaved(newSavedState);
            updateSaveButton(newSavedState);

            showToast(newSavedState ? "បានរក្សាទុកកាត" : "បានលុបកាតចេញពីបញ្ជីរក្សាទុក");
        });
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    private void hideNavBar() {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).hideBottomNavigation();
        }
    }

    private void showNavBar() {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).showBottomNavigation();
        }
    }

    private void loadCardData() {
        List<LearninghubCard> cached = viewModel.getCards().getValue();
        if (cached != null && !cached.isEmpty()) {
            findCurrentCard(cached, currentCardId);
            loadRelatedCards(cached);
        } else {
            viewModel.loadCards();
        }
    }

    private void findCurrentCard(List<LearninghubCard> cards, String cardId) {
        for (LearninghubCard card : cards) {
            if (card != null && cardId.equals(card.getUuid())) {
                currentCard = card;
                displayCardData();
                Log.d(TAG, "Found current card: " + card.getTitle());
                return;
            }
        }
        Log.e(TAG, "Card not found with ID: " + cardId);
    }

    private void displayCardData() {
        if (binding == null || currentCard == null) return;

        binding.tvDetailTitle.setText(
                currentCard.getTitle() != null ? currentCard.getTitle() : "No Title"
        );

        String imageUrl = currentCard.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty() && !"null".equals(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.imagecard1)
                    .error(R.drawable.imagecard1)
                    .into(binding.ivDetailImage);
        } else {
            binding.ivDetailImage.setImageResource(R.drawable.imagecard1);
        }

        String content = currentCard.getContent() != null ? currentCard.getContent() : "";
        if (content.isEmpty() && currentCard.getDescription() != null) {
            content = currentCard.getDescription();
        }
        binding.tvDetailContent.setText(content);

        updateSaveButton(currentCard.getIsSaved());
    }

    private void updateSaveButton(boolean isSaved) {
        if (binding == null) return;

        if (isSaved) {
            binding.btnSave.setText("បានរក្សាទុក");
            binding.btnSave.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.primary)));
            binding.btnSave.setTextColor(getResources().getColor(R.color.white));
            binding.btnSave.setStrokeWidth(0);
        } else {
            binding.btnSave.setText("រក្សាទុក");
            binding.btnSave.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
            binding.btnSave.setTextColor(getResources().getColor(R.color.primary));
            binding.btnSave.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.primary)));
            binding.btnSave.setStrokeWidth(3);
        }
    }

    private void loadRelatedCards(List<LearninghubCard> allCards) {
        if (binding == null || currentCard == null || allCards == null) return;

        relatedCards.clear();

        for (LearninghubCard card : allCards) {
            if (card == null) continue;

            boolean sameCategory =
                    card.getCategory() != null &&
                            currentCard.getCategory() != null &&
                            card.getCategory().equals(currentCard.getCategory());

            boolean notSameCard = !card.getUuid().equals(currentCardId);

            if (notSameCard && sameCategory) {
                relatedCards.add(card);
                if (relatedCards.size() >= 10) break;
            }
        }

        relatedCardsAdapter.setRelatedCards(relatedCards);

        binding.rvRelatedCards.setVisibility(relatedCards.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openRelatedCardDetail(LearninghubCard card) {
        if (card == null || card.getUuid() == null) {
            showToast("មិនអាចបើកកាតនេះបាន");
            return;
        }

        try {
            CardDetailFragment fragment = new CardDetailFragment();
            Bundle args = new Bundle();
            args.putString("card_id", card.getUuid());
            fragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack("related_detail")
                    .commit();

        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage(), e);
            showToast("កំហុសក្នុងការបើកកាត");
        }
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
