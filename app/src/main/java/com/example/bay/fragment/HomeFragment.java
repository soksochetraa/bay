package com.example.bay.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.bay.BuildConfig;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.FragmentHomePostCardItemAdapter;
import com.example.bay.adapter.FragmentHomeShoppingCardAdapter;
import com.example.bay.databinding.FragmentHomeBinding;
import com.example.bay.model.ShoppingItem;
import com.example.bay.model.User;
import com.example.bay.repository.PostCardItemRepository;
import com.example.bay.repository.ShoppingItemRepository;
import com.example.bay.repository.UserRepository;
import com.example.bay.viewmodel.HomeViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FragmentHomeShoppingCardAdapter adapter;
    private FragmentHomePostCardItemAdapter postAdapter;
    private ShoppingItemRepository repository;
    private PostCardItemRepository postRepository;
    private UserRepository userRepository;
    private FirebaseAuth mAuth;
    private HomeViewModel viewModel;
    private HomeActivity homeActivity;
    private String city = "Banteay MeanChey";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=kh";

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        userRepository = new UserRepository();
        mAuth = FirebaseAuth.getInstance();
        homeActivity = (HomeActivity) getActivity();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        setupRecyclerView();
        setupPostRecyclerView();
        loadShoppingItems();
        loadPostCardItems();
        setCurrentDate();
        setupLearningHubNavigation();

        setupFilterChips();

        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                city = user.getLocation();
                binding.tvUsername.setText(user.getName());
                Glide.with(getContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.img)
                        .error(R.drawable.img)
                        .into(binding.btnProfile);
                fetchWeatherDataIfNeeded();
            }
        });

        viewModel.getTemperature().observe(getViewLifecycleOwner(), temp -> {
            if (temp != null && viewModel.getWeatherIcon().getValue() != null) {
                updateWeatherUI(temp, viewModel.getWeatherIcon().getValue());
            }
        });

        viewModel.getWeatherIcon().observe(getViewLifecycleOwner(), icon -> {
            if (icon != null && viewModel.getTemperature().getValue() != null) {
                updateWeatherUI(viewModel.getTemperature().getValue(), icon);
            }
        });

        if (viewModel.getUser().getValue() == null && currentUser != null) {
            showLoading();
            loadUserProfile(currentUser.getUid());
        }

        binding.textView13.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity())
                        .navigateTo(R.id.nav_marketplace, new MarketPlaceFragment());
            }
        });

        return binding.getRoot();
    }

    private void setupLearningHubNavigation() {
        // Find the Learning Hub card view by ID and set click listener
        View learningHubCard = binding.getRoot().findViewById(R.id.go_to_learninghub);
        if (learningHubCard != null) {
            learningHubCard.setOnClickListener(v -> {
                navigateToLearningHub();
            });
        }
    }

    private void navigateToLearningHub() {
        // Create the LearningHub fragment
        LearninghubFragment learninghubFragment = new LearninghubFragment();

        // Use FragmentTransaction to replace the current fragment
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, learninghubFragment) // Replace with your container ID
                .addToBackStack("home") // Add to back stack so user can go back
                .commit();

        // Optional: Show a toast message
        Toast.makeText(requireContext(), "Opening Learning Hub", Toast.LENGTH_SHORT).show();
    }


    private void setupFilterChips() {
        binding.filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            List<ShoppingItem> allItems = viewModel.getShoppingItems().getValue();
            if (allItems == null) return;

            List<ShoppingItem> filtered = new ArrayList<>();

            if (checkedId == R.id.chip_all) {
                filtered.addAll(allItems);
            } else if (checkedId == R.id.chip_vegetable) {
                for (ShoppingItem item : allItems) {
                    if ("vegetables".equalsIgnoreCase(item.getCategory())) {
                        filtered.add(item);
                    }
                }
            } else if (checkedId == R.id.chip_fruit) {
                for (ShoppingItem item : allItems) {
                    if ("fruits".equalsIgnoreCase(item.getCategory()) ||
                            "fruit".equalsIgnoreCase(item.getCategory())) {
                        filtered.add(item);
                    }
                }
            } else if (checkedId == R.id.chip_tool) {
                for (ShoppingItem item : allItems) {
                    if ("tools".equalsIgnoreCase(item.getCategory()) ||
                            "tool".equalsIgnoreCase(item.getCategory())) {
                        filtered.add(item);
                    }
                }
            }

            adapter.setShoppingItems(filtered);
        });
    }

    private void setCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        Locale khmerLocale = new Locale("km", "KH");
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", khmerLocale);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", khmerLocale);
        String dayName = dayFormat.format(calendar.getTime());
        String dateText = dateFormat.format(calendar.getTime());
        String fullDate = dayName + ", " + dateText;
        binding.tvDate.setText(fullDate);
    }

    private void loadUserProfile(String userId) {
        userRepository.getUserById(userId, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User user) {
                viewModel.setUser(user);
                hideLoading();
            }

            @Override
            public void onError(String errorMsg) {
                Log.e("UserLoadError", errorMsg);
                hideLoading();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new FragmentHomeShoppingCardAdapter();
        binding.rvListCardShopItems.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.rvListCardShopItems.setAdapter(adapter);
    }

    private void setupPostRecyclerView() {
        postAdapter = new FragmentHomePostCardItemAdapter(requireContext());
        binding.rvListCardForum.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        );
        binding.rvListCardForum.setAdapter(postAdapter);
    }

    private void loadShoppingItems() {
        repository = new ShoppingItemRepository();
        repository.getLimitedShoppingCards().observe(getViewLifecycleOwner(), items -> {
            if (items != null && !items.isEmpty()) {
                viewModel.setShoppingItems(items);
            }
        });

        viewModel.getShoppingItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null && !items.isEmpty()) {
                adapter.setShoppingItems(items);
            }
        });
    }

    private void loadPostCardItems() {
        postRepository = new PostCardItemRepository();
        postRepository.getAllPostCardItems().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null && !posts.isEmpty()) {
                viewModel.setPostCardItems(posts);
            }
        });

        viewModel.getPostCardItems().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null && !posts.isEmpty()) {
                postAdapter.setPostCardItemList(posts);
            }
        });
    }

    private void fetchWeatherDataIfNeeded() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String encodedCity = city.replace(" ", "%20");
                String urlString = String.format(Locale.getDefault(), BASE_URL, encodedCity, BuildConfig.OPENWEATHER_API_KEY);
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    Log.e("WeatherError", "API response code: " + responseCode);
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Invalid response from weather API", Toast.LENGTH_SHORT).show());
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();
                conn.disconnect();

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONObject main = jsonObject.getJSONObject("main");
                double temp = main.getDouble("temp");
                JSONArray weatherArray = jsonObject.getJSONArray("weather");
                JSONObject weatherObj = weatherArray.getJSONObject(0);
                String icon = weatherObj.getString("icon");

                requireActivity().runOnUiThread(() -> binding.tvWeatherLocation.setText(city));
                viewModel.setWeatherData(temp, icon);

            } catch (Exception e) {
                Log.e("WeatherError", "Exception: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Weather fetch failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateWeatherUI(double temp, String iconCode) {
        binding.tvWeatherNumber.setText(String.format(Locale.getDefault(), "%.0f°", temp));
        updateWeatherIcon(iconCode);
    }

    private void updateWeatherIcon(String iconCode) {
        int resId;
        if (iconCode.contains("01")) resId = R.drawable.pcloudy;
        else if (iconCode.contains("02") || iconCode.contains("03") || iconCode.contains("04")) resId = R.drawable.pcloudy;
        else if (iconCode.contains("09") || iconCode.contains("10")) resId = R.drawable.pcloudy;
        else if (iconCode.contains("11")) resId = R.drawable.pcloudy;
        else if (iconCode.contains("13")) resId = R.drawable.pcloudy;
        else if (iconCode.contains("50")) resId = R.drawable.pcloudy;
        else resId = R.drawable.pcloudy;
        binding.weatherIcon.setImageResource(resId);
    }

    private void showLoading() {
        if (homeActivity != null) homeActivity.runOnUiThread(() ->
                homeActivity.findViewById(R.id.loading).setVisibility(View.VISIBLE));
    }

    private void hideLoading() {
        if (homeActivity != null) {
            homeActivity.hideLoading();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
