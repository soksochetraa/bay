package com.example.bay.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.RelatedCardsAdapter;
import com.example.bay.model.LearninghubCard;
import com.example.bay.viewmodel.LearningHubViewModel;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class CardDetailFragment extends Fragment {

    private static final String TAG = "CardDetailFragment";

    private LearningHubViewModel viewModel;
    private LearninghubCard currentCard;
    private RelatedCardsAdapter relatedCardsAdapter;
    private List<LearninghubCard> relatedCards = new ArrayList<>();

    private ImageView ivDetailImage;
    private TextView tvDetailTitle, tvDetailContent;
    private MaterialButton btnSave;
    private ImageButton btnBack;
    private RecyclerView rvRelatedCards;

    private String currentCardId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        initViews(view);
        setupViewModel();
        setupRelatedCards();
        setupClickListeners();

        if (getArguments() != null) {
            currentCardId = getArguments().getString("card_id");
            Log.d(TAG, "Received card_id: " + currentCardId);
            if (currentCardId != null) {
                loadCardData();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Hide navbar when this fragment is active
        hideNavBar();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Show navbar when leaving this fragment
        showNavBar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Ensure navbar is shown when fragment is destroyed
        showNavBar();
    }

    private void initViews(View view) {
        ivDetailImage = view.findViewById(R.id.iv_detail_image);
        tvDetailTitle = view.findViewById(R.id.tv_detail_title);
        tvDetailContent = view.findViewById(R.id.tv_detail_content);
        btnSave = view.findViewById(R.id.btn_save);
        btnBack = view.findViewById(R.id.btn_back);
        rvRelatedCards = view.findViewById(R.id.rv_related_cards);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(LearningHubViewModel.class);

        viewModel.getCards().observe(getViewLifecycleOwner(), cards -> {
            Log.d(TAG, "Cards observed: " + (cards != null ? cards.size() : 0));
            if (cards != null && !cards.isEmpty()) {
                if (currentCardId != null) {
                    findCurrentCard(cards, currentCardId);
                    loadRelatedCards(cards);
                }
            }
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

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        rvRelatedCards.setLayoutManager(layoutManager);
        rvRelatedCards.setAdapter(relatedCardsAdapter);
        rvRelatedCards.setNestedScrollingEnabled(true);
        Log.d(TAG, "Related cards adapter setup complete");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            try {
                // Navigate back to LearninghubFragment
                LearninghubFragment fragment = new LearninghubFragment();
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment, fragment)
                        .commit();
            } catch (Exception e) {
                Log.e(TAG, "Back navigation error: " + e.getMessage(), e);
                // Fallback: pop back stack
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });


        btnSave.setOnClickListener(v -> {
            if (currentCard != null) {
                boolean newSavedState = !currentCard.getIsSaved();
                viewModel.toggleSaveCard(currentCard.getUuid(), newSavedState);
                updateSaveButton(newSavedState);
                currentCard.setIsSaved(newSavedState);

                String message = newSavedState ? "បានរក្សាទុកកាត" : "បានលុបកាតចេញពីបញ្ជីរក្សាទុក";
                showToast(message);
            }
        });
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
        if (viewModel.getCards().getValue() != null && !viewModel.getCards().getValue().isEmpty()) {
            List<LearninghubCard> cards = viewModel.getCards().getValue();
            findCurrentCard(cards, currentCardId);
            loadRelatedCards(cards);
        } else {
            viewModel.loadCards();
        }
    }

    private void findCurrentCard(List<LearninghubCard> cards, String cardId) {
        for (LearninghubCard card : cards) {
            if (card.getUuid().equals(cardId)) {
                currentCard = card;
                displayCardData();
                Log.d(TAG, "Found current card: " + card.getTitle());
                return;
            }
        }
        Log.e(TAG, "Card not found with ID: " + cardId);
    }

    private void displayCardData() {
        if (currentCard == null) return;

        tvDetailTitle.setText(currentCard.getTitle() != null ? currentCard.getTitle() : "No Title");

        String imageUrl = currentCard.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty() && !"null".equals(imageUrl)) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.imagecard1)
                    .error(R.drawable.imagecard1)
                    .into(ivDetailImage);
        }

        String content = currentCard.getContent() != null ? currentCard.getContent() : "";
        if (content.isEmpty() && currentCard.getDescription() != null) {
            content = currentCard.getDescription();
        }
        tvDetailContent.setText(content);

        updateSaveButton(currentCard.getIsSaved());
    }

    private void updateSaveButton(boolean isSaved) {
        if (isSaved) {
            btnSave.setText("បានរក្សាទុក");
            btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
            btnSave.setTextColor(getResources().getColor(R.color.white));

        } else {
            btnSave.setText("រក្សាទុក");
            btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.white)));
            btnSave.setTextColor(getResources().getColor(R.color.primary));
            btnSave.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
            btnSave.setStrokeWidth(3);

        }
    }

    private void loadRelatedCards(List<LearninghubCard> allCards) {
        if (currentCard == null || allCards == null) return;

        relatedCards.clear();
        for (LearninghubCard card : allCards) {
            if (!card.getUuid().equals(currentCardId) &&
                    card.getCategory() != null &&
                    currentCard.getCategory() != null &&
                    card.getCategory().equals(currentCard.getCategory())) {
                relatedCards.add(card);
                if (relatedCards.size() >= 10) break;
            }
        }

        Log.d(TAG, "Loading " + relatedCards.size() + " related cards");
        relatedCardsAdapter.setRelatedCards(relatedCards);

        if (relatedCards.isEmpty()) {
            rvRelatedCards.setVisibility(View.GONE);
        } else {
            rvRelatedCards.setVisibility(View.VISIBLE);
        }
    }

    private void openRelatedCardDetail(LearninghubCard card) {
        Log.d(TAG, "Opening related card detail: " + card.getTitle() + " ID: " + card.getUuid());

        if (card == null || card.getUuid() == null) {
            Log.e(TAG, "Cannot open null card or card with null UUID");
            showToast("មិនអាចបើកកាតនេះបាន");
            return;
        }

        try {
            // Use FragmentManager approach for consistency
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
            showToast("កំហុសក្នុងការបើកកាត: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}