package com.example.bay.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bay.R;
import com.example.bay.adapter.LearninghubCardAdapter;
import com.example.bay.model.LearninghubCard;
import com.example.bay.viewmodel.LearningHubViewModel;
import java.util.ArrayList;
import java.util.List;

public class SavedCardsFragment extends Fragment {

    private RecyclerView recyclerViewSavedCards;
    private TextView tvEmptyState;
    private LearninghubCardAdapter adapter;
    private LearningHubViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_cards, container, false);

        initViews(view);
        setupViewModel();
        setupRecyclerView();

        return view;
    }

    private void initViews(View view) {
        recyclerViewSavedCards = view.findViewById(R.id.recyclerViewSavedCards);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(LearningHubViewModel.class);

        // Observe saved cards directly from the repository
        viewModel.getSavedCards().observe(getViewLifecycleOwner(), cards -> {
            if (cards != null) {
                List<LearninghubCard> savedCards = filterSavedCards(cards);
                adapter.setCards(savedCards);
                updateEmptyState(savedCards.isEmpty());
            } else {
                updateEmptyState(true);
            }
        });

        // Observe card updates to refresh the list
        viewModel.getUpdatedCardId().observe(getViewLifecycleOwner(), cardId -> {
            if (cardId != null) {
                // Refresh saved cards when a card is updated
                viewModel.loadSavedCards();
            }
        });

        // Load saved cards initially
        viewModel.loadSavedCards();
    }

    private void setupRecyclerView() {
        adapter = new LearninghubCardAdapter(
                card -> openCardDetail(card),
                (card, isSaved) -> {
                    viewModel.toggleSaveCard(card.getUuid(), isSaved);

                    String message = isSaved ? "Card saved" : "Card unsaved";
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

                    // If unsaving, the card will be automatically removed from the list
                    // due to the LiveData observation
                },
                this::openCardDetail
        );

        recyclerViewSavedCards.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSavedCards.setAdapter(adapter);
    }

    private List<LearninghubCard> filterSavedCards(List<LearninghubCard> allCards) {
        List<LearninghubCard> savedCards = new ArrayList<>();
        for (LearninghubCard card : allCards) {
            if (card.getIsSaved()) {
                savedCards.add(card);
            }
        }
        return savedCards;
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerViewSavedCards.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerViewSavedCards.setVisibility(View.VISIBLE);
        }
    }

    private void openCardDetail(LearninghubCard card) {
        android.util.Log.d("SavedCardsFragment", "🎯 Opening card detail from saved: " + card.getTitle());

        if (card == null || card.getUuid() == null) {
            Toast.makeText(requireContext(), "Cannot open card: Invalid data", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Use FragmentManager approach
            CardDetailFragment fragment = new CardDetailFragment();
            Bundle args = new Bundle();
            args.putString("card_id", card.getUuid());
            fragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack("saved_to_detail") // Use consistent back stack name
                    .commit();

        } catch (Exception e) {
            android.util.Log.e("SavedCardsFragment", "Navigation error: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error opening card", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh when fragment becomes visible (after back navigation)
        android.util.Log.d("SavedCardsFragment", "onResume - Refreshing saved cards");
        viewModel.loadSavedCards();
    }

    @Override
    public void onPause() {
        super.onPause();
        android.util.Log.d("SavedCardsFragment", "onPause");
    }
}