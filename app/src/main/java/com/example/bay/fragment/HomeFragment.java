package com.example.bay.fragment;

import android.graphics.Rect;
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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bay.BuildConfig;
import com.example.bay.HomeActivity;
import com.example.bay.R;
import com.example.bay.adapter.FragmentHomePostCardItemAdapter;
import com.example.bay.adapter.FragmentHomeShoppingCardAdapter;
import com.example.bay.adapter.WeatherForecastAdapter;
import com.example.bay.databinding.FragmentHomeBinding;
import com.example.bay.model.ForecastDay;
import com.example.bay.model.PostCardItem;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FragmentHomeShoppingCardAdapter adapter;
    private FragmentHomePostCardItemAdapter postAdapter;
    private WeatherForecastAdapter forecastAdapter;

    private ShoppingItemRepository repository;
    private PostCardItemRepository postRepository;
    private UserRepository userRepository;
    private FirebaseAuth mAuth;
    private HomeViewModel viewModel;
    private HomeActivity homeActivity;

    private String city = "Phnom Penh";

    private static final String BASE_URL =
            "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=kh";

    private static final String FORECAST_URL =
            "https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric&lang=kh";

    private final List<ShoppingItem> masterShoppingItems = new ArrayList<>();

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        userRepository = new UserRepository();
        mAuth = FirebaseAuth.getInstance();

        homeActivity = (HomeActivity) getActivity();
        if (homeActivity != null) homeActivity.showBottomNavigation();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        setupRecyclerView();
        setupPostRecyclerView();
        setupForecastRecyclerView();
        setupFilterChips();

        // Load only 5 shopping items for home screen
        loadShoppingItems();
        loadPostCardItems();
        setCurrentDate();

        viewModel.getTemperature().observe(getViewLifecycleOwner(), temp -> {
            String icon = viewModel.getWeatherIcon().getValue();
            if (temp != null && icon != null && binding != null) updateWeatherUI(temp, icon);
        });

        viewModel.getWeatherIcon().observe(getViewLifecycleOwner(), icon -> {
            Double temp = viewModel.getTemperature().getValue();
            if (icon != null && temp != null && binding != null) updateWeatherUI(temp, icon);
        });

        binding.textView11.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).navigateTo(R.id.nav_community, new CommunityFragment());
            }
        });

        binding.textView13.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).navigateTo(R.id.nav_marketplace, new MarketPlaceMainFragment());
                ((HomeActivity) getActivity()).setBottomNavigationToMarketPlace();
            }
        });

        binding.farmMap.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity activity = (HomeActivity) getActivity();
                activity.setBottomNavigationVisible(false);
                activity.LoadFragment(new FarmMapFragment());
            }
        });

        binding.goToLearninghub.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity activity = (HomeActivity) getActivity();
                activity.setBottomNavigationVisible(false);
                activity.LoadFragment(new LearninghubFragment());
            }
        });

        binding.goToMarketplace.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity activity = (HomeActivity) getActivity();
                activity.LoadFragment(new MarketPlaceMainFragment());
                activity.setBottomNavigationToMarketPlace();
            }
        });

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new FragmentHomeShoppingCardAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.rvListCardShopItems.setLayoutManager(lm);
        binding.rvListCardShopItems.setHasFixedSize(true);
        binding.rvListCardShopItems.setAdapter(adapter);

        // Add click listener to navigate to detail page
        adapter.setOnItemClickListener(new FragmentHomeShoppingCardAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ShoppingItem item) {
                navigateToDetailFragment(item);
            }
        });

        // Add spacing between items
        binding.rvListCardShopItems.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                int spacing = getResources().getDimensionPixelSize(R.dimen.item_spacing);

                // Add left margin for first item
                if (position == 0) {
                    outRect.left = spacing;
                }
                // Add right margin for all items
                outRect.right = spacing;
            }
        });
    }

    private void setupPostRecyclerView() {
        postAdapter = new FragmentHomePostCardItemAdapter(requireContext());
        binding.rvListCardForum.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.rvListCardForum.setAdapter(postAdapter);
    }

    private void setupForecastRecyclerView() {
        forecastAdapter = new WeatherForecastAdapter();
        binding.rvWeatherForecast.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.rvWeatherForecast.setHasFixedSize(true);
        binding.rvWeatherForecast.setAdapter(forecastAdapter);
    }

    private void loadShoppingItems() {
        repository = new ShoppingItemRepository();

        // Load only 5 items for home screen
        repository.fetchLimitedShoppingItems(5, new ShoppingItemRepository.ShoppingItemCallback<List<ShoppingItem>>() {
            @Override
            public void onSuccess(List<ShoppingItem> items) {
                masterShoppingItems.clear();
                if (items != null && !items.isEmpty()) {
                    // Take only first 5 items
                    int count = Math.min(items.size(), 5);
                    for (int i = 0; i < count; i++) {
                        masterShoppingItems.add(items.get(i));
                    }
                    Log.d("HomeFragment", "Loaded " + count + " shopping items for home screen");
                } else {
                    Log.d("HomeFragment", "No shopping items loaded");
                }

                adapter.setShoppingItems(new ArrayList<>(masterShoppingItems));

                if (binding != null && binding.filterChipGroup != null
                        && binding.filterChipGroup.getCheckedChipId() == View.NO_ID) {
                    binding.filterChipGroup.check(R.id.chip_all);
                }
            }

            @Override
            public void onError(String errorMsg) {
                Log.e("HomeFragment", "Error loading shopping items: " + errorMsg);
                Toast.makeText(requireContext(), "មិនអាចទាញយកទំនិញបាន", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilterChips() {
        binding.filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (masterShoppingItems.isEmpty()) {
                adapter.setShoppingItems(new ArrayList<>());
                return;
            }

            List<ShoppingItem> filtered = new ArrayList<>();

            if (checkedId == R.id.chip_all || checkedId == View.NO_ID) {
                filtered.addAll(masterShoppingItems);
            } else if (checkedId == R.id.chip_vegetable) {
                for (ShoppingItem item : masterShoppingItems) {
                    if (item == null) continue;
                    String c = item.getCategory();
                    if (c != null) {
                        String lowerC = c.toLowerCase(Locale.ENGLISH);
                        if (lowerC.contains("បន្លែ") || lowerC.contains("vegetable")) {
                            filtered.add(item);
                        }
                    }
                }
            } else if (checkedId == R.id.chip_fruit) {
                for (ShoppingItem item : masterShoppingItems) {
                    if (item == null) continue;
                    String c = item.getCategory();
                    if (c != null) {
                        String lowerC = c.toLowerCase(Locale.ENGLISH);
                        if (lowerC.contains("ផ្លែឈើ") || lowerC.contains("fruit")) {
                            filtered.add(item);
                        }
                    }
                }
            } else if (checkedId == R.id.chip_tool) {
                for (ShoppingItem item : masterShoppingItems) {
                    if (item == null) continue;
                    String c = item.getCategory();
                    if (c != null) {
                        String lowerC = c.toLowerCase(Locale.ENGLISH);
                        if (lowerC.contains("សម្ភារៈ") || lowerC.contains("tool") || lowerC.contains("supplies")) {
                            filtered.add(item);
                        }
                    }
                }
            } else {
                filtered.addAll(masterShoppingItems);
            }

            // Still limit to max 5 items even after filtering
            int maxItems = Math.min(filtered.size(), 5);
            if (maxItems > 0) {
                filtered = filtered.subList(0, maxItems);
            }

            adapter.setShoppingItems(filtered);
        });
    }

    // ✅ New method to navigate to detail fragment
    private void navigateToDetailFragment(ShoppingItem item) {
        if (getActivity() instanceof HomeActivity) {
            HomeActivity activity = (HomeActivity) getActivity();

            // Hide bottom navigation for detail view
            activity.setBottomNavigationVisible(false);

            // Navigate to detail fragment
            DetailItemShoppingFragment detailFragment = DetailItemShoppingFragment.newInstance(item);
            activity.LoadFragment(detailFragment);

            Log.d("HomeFragment", "Navigating to detail for item: " + item.getName());
        }
    }

    private void loadPostCardItems() {
        postRepository = new PostCardItemRepository();

        postRepository.fetchLatestTwoPosts(new PostCardItemRepository.OnLatestPostsLoadedListener() {
            @Override
            public void onSuccess(List<PostCardItem> posts) {
                if (posts != null && !posts.isEmpty()) {
//                    viewModel.setPostCardItems(posts);
                    postAdapter.setPostCardItemList(posts);

                    if (binding != null && binding.rvListCardForum != null) {
                        binding.rvListCardForum.post(() -> {
                            binding.rvListCardForum.invalidate();
                            binding.rvListCardForum.requestLayout();
                        });
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.e("HomeFragment", "Error loading latest posts: " + (t != null ? t.getMessage() : "null"));
            }
        });
    }

    private void setCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        Locale khmerLocale = new Locale("km", "KH");
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", khmerLocale);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", khmerLocale);
        String dayName = dayFormat.format(calendar.getTime());
        String dateText = dateFormat.format(calendar.getTime());
        binding.tvDate.setText(dayName + ", " + dateText);
    }

    private void loadUserProfile(String userId) {
        userRepository.getUserById(userId, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User user) {
//                viewModel.setUser(user);
                hideLoading();
            }

            @Override
            public void onError(String errorMsg) {
                Log.e("UserLoadError", errorMsg);
                hideLoading();
            }
        });
    }

    private void fetchWeatherDataIfNeeded() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String normalizedCity = normalizeCityName(city);
                String encodedCity = normalizedCity.replace(" ", "%20");
                String urlString = String.format(Locale.getDefault(), BASE_URL, encodedCity, BuildConfig.OPENWEATHER_API_KEY);

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Weather not available for " + normalizedCity, Toast.LENGTH_SHORT).show());
                    }
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

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (binding != null) binding.tvWeatherLocation.setText(normalizedCity);
                    });
                }

                viewModel.setWeatherData(temp, icon);
                fetchForecastNextDays(normalizedCity);

            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Weather fetch failed", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void fetchForecastNextDays(String normalizedCity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String encodedCity = normalizedCity.replace(" ", "%20");
                String urlString = String.format(Locale.getDefault(), FORECAST_URL, encodedCity, BuildConfig.OPENWEATHER_API_KEY);

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) return;

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();
                conn.disconnect();

                JSONObject json = new JSONObject(response.toString());
                JSONArray list = json.getJSONArray("list");

                LinkedHashMap<String, int[]> dayMinMax = new LinkedHashMap<>();
                LinkedHashMap<String, String> dayIcon = new LinkedHashMap<>();
                LinkedHashMap<String, String> dayDesc = new LinkedHashMap<>();

                for (int i = 0; i < list.length(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    String dtTxt = item.getString("dt_txt");
                    String date = dtTxt.substring(0, 10);

                    JSONObject main = item.getJSONObject("main");
                    int tempMin = (int) Math.round(main.getDouble("temp_min"));
                    int tempMax = (int) Math.round(main.getDouble("temp_max"));

                    JSONArray weatherArr = item.getJSONArray("weather");
                    JSONObject w = weatherArr.getJSONObject(0);
                    String icon = w.optString("icon", "");
                    String desc = w.optString("description", "");

                    if (!dayMinMax.containsKey(date)) {
                        dayMinMax.put(date, new int[]{tempMin, tempMax});
                        dayIcon.put(date, icon);
                        dayDesc.put(date, desc);
                    } else {
                        int[] mm = dayMinMax.get(date);
                        if (tempMin < mm[0]) mm[0] = tempMin;
                        if (tempMax > mm[1]) mm[1] = tempMax;
                    }

                    if (dayMinMax.size() >= 5) break;
                }

                List<ForecastDay> result = new ArrayList<>();
                SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outFmt = new SimpleDateFormat("EEE", new Locale("km", "KH"));

                for (String date : dayMinMax.keySet()) {
                    int[] mm = dayMinMax.get(date);
                    String label;
                    try {
                        label = outFmt.format(inFmt.parse(date));
                    } catch (Exception e) {
                        label = date;
                    }
                    result.add(new ForecastDay(label, mm[0], mm[1], dayDesc.get(date), dayIcon.get(date)));
                }

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (binding != null && forecastAdapter != null) forecastAdapter.setItems(result);
                    });
                }

            } catch (Exception ignored) {}
        });
    }

    private String normalizeCityName(String input) {
        if (input == null || input.isEmpty()) return "Phnom Penh";
        input = input.replace("Province", "")
                .replace("City", "")
                .replace("Municipality", "")
                .replace("State", "")
                .replace("ខេត្ត", "")
                .replace("រាជធានី", "")
                .trim();
        input = input.toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    private void updateWeatherUI(double temp, String iconCode) {
        binding.tvWeatherNumber.setText(String.format(Locale.getDefault(), "%.0f°", temp));
        updateWeatherIcon(iconCode);
    }

    private void updateWeatherIcon(String iconCode) {
        int resId = R.drawable.pcloudy;
        binding.weatherIcon.setImageResource(resId);
    }

    private void showLoading() {
        if (homeActivity != null) {
            homeActivity.runOnUiThread(() -> homeActivity.findViewById(R.id.loading).setVisibility(View.VISIBLE));
        }
    }

    private void hideLoading() {
        if (homeActivity != null) homeActivity.hideLoading();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}