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
import com.example.bay.adapter.FragmentHomeLearninghubAdapter;
import com.example.bay.adapter.FragmentHomeLocationAdapter;
import com.example.bay.adapter.FragmentHomePostCardItemAdapter;
import com.example.bay.adapter.FragmentHomeShoppingCardAdapter;
import com.example.bay.adapter.WeatherForecastAdapter;
import com.example.bay.databinding.FragmentHomeBinding;
import com.example.bay.model.ForecastDay;
import com.example.bay.model.LearninghubCard;
import com.example.bay.model.Location;
import com.example.bay.model.PostCardItem;
import com.example.bay.model.ShoppingItem;
import com.example.bay.model.User;
import com.example.bay.model.Notification;
import com.example.bay.repository.LearningHubRepository;
import com.example.bay.repository.LocationRepository;
import com.example.bay.repository.PostCardItemRepository;
import com.example.bay.repository.ShoppingItemRepository;
import com.example.bay.repository.UserRepository;
import com.example.bay.repository.NotificationRepository;
import com.example.bay.repository.IApiCallback;
import com.example.bay.viewmodel.HomeViewModel;
import com.example.bay.viewmodel.SharedUserViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private static final String VISIBILITY_VISIBLE = "visible";

    private FragmentHomeBinding binding;
    private FragmentHomeShoppingCardAdapter shoppingAdapter;
    private FragmentHomePostCardItemAdapter postAdapter;
    private FragmentHomeLocationAdapter locationAdapter;
    private FragmentHomeLearninghubAdapter learninghubAdapter;
    private WeatherForecastAdapter forecastAdapter;

    private FirebaseUser currentUser;

    private ShoppingItemRepository shoppingRepository;
    private LocationRepository locationRepository;
    private LearningHubRepository learninghubRepository;
    private PostCardItemRepository postRepository;
    private NotificationRepository notificationRepository;
    private UserRepository userRepository;

    private FirebaseAuth mAuth;
    private HomeViewModel weatherViewModel;
    private SharedUserViewModel sharedUserViewModel;
    private HomeActivity homeActivity;
    private String userId;

    private final List<ShoppingItem> masterShoppingItems = new ArrayList<>();
    private String city = "Phnom Penh";
    private DatabaseReference postRef;
    private ValueEventListener postListener;

    private static final String BASE_URL =
            "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=kh";
    private static final String FORECAST_URL =
            "https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric&lang=kh";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        weatherViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        sharedUserViewModel = new ViewModelProvider(requireActivity()).get(SharedUserViewModel.class);
        shoppingRepository = new ShoppingItemRepository();
        locationRepository = new LocationRepository();
        learninghubRepository = new LearningHubRepository();
        postRepository = new PostCardItemRepository();
        userRepository = new UserRepository();
        notificationRepository = new NotificationRepository();
        mAuth = FirebaseAuth.getInstance();

        homeActivity = (HomeActivity) getActivity();
        if (homeActivity != null) {
            userId = homeActivity.getCurrentUserId();
        }

        setupRecyclerView();
        setupPostRecyclerView();
        setupLocationRecyclerView();      // FIXED: Added back
        setupLearninghubRecyclerView();   // FIXED: Uses different RecyclerView
        setupForecastRecyclerView();

        setCurrentDate();

        loadShoppingItems();
        loadPostCardItems();
        loadLocations();                  // FIXED: Added back
        loadLearninghubItems();

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            sharedUserViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    city = normalizeCityName(user.getLocation());
                    binding.tvUsername.setText(user.getLastName());
                    Glide.with(requireContext()).load(user.getProfileImageUrl()).into(binding.btnProfile);
                    fetchWeatherData();

                    if (user.getModeration() != null) {
                        if (user.isWarned()) {
                            if (homeActivity != null) homeActivity.showDialog(
                                    "គណនីរបស់អ្នកត្រូវបានព្រមាន!\n" + user.getModeration().getWarningMessage(),
                                    "យល់ព្រម",
                                    null,
                                    null,
                                    null,
                                    true
                            );
                        } else if (user.isSuspension()) {
                            if (homeActivity != null) homeActivity.showDialog(
                                    "គណនីរបស់អ្នកត្រូវបានបិទ!\n" + user.getModeration().getSuspensionReason() + "\n" + user.getModeration().getSuspendedUntil(),
                                    "យល់ព្រម",
                                    null,
                                    null,
                                    null,
                                    true
                            );
                        }
                    }
                }
            });
        }

        weatherViewModel.getTemperature().observe(getViewLifecycleOwner(), temp -> {
            String icon = weatherViewModel.getWeatherIcon().getValue();
            if (temp != null && icon != null && binding != null) updateWeatherUI(temp, icon);
        });

        weatherViewModel.getWeatherIcon().observe(getViewLifecycleOwner(), icon -> {
            Double temp = weatherViewModel.getTemperature().getValue();
            if (temp != null && icon != null && binding != null) updateWeatherUI(temp, icon);
        });

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        shoppingAdapter = new FragmentHomeShoppingCardAdapter();
        binding.rvListCardShopItems.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvListCardShopItems.setAdapter(shoppingAdapter);

        shoppingAdapter.setOnItemClickListener(item -> {
            if (homeActivity != null) {
                homeActivity.LoadFragment(DetailItemShoppingFragment.newInstance(item));
            }
        });

        binding.btnProfile.setOnClickListener(v -> {
            if (homeActivity != null) homeActivity.navigateToMyProfile();
        });

        binding.farmMap.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.showLoading();
                homeActivity.LoadFragment(new FarmMapFragment());
            }
        });

        binding.btnNotification.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.LoadFragment(new NotificationFragment());
            }
        });

        binding.goToLearninghub.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.LoadFragment(new LearninghubFragment());
            }
        });

        binding.goToMarketplace.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.navigateTo(R.id.nav_marketplace, new MarketPlaceMainFragment());
            }
        });

        binding.textView13.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.navigateTo(R.id.nav_marketplace, new MarketPlaceMainFragment());
            }
        });

        binding.textView11.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.navigateTo(R.id.nav_community, new CommunityFragment());
            }
        });

        // "See more" for locations
        binding.tvLocationMore.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.LoadFragment(new FarmMapFragment());
            }
        });

        // "See more" for learning hub
        binding.tvLearninghubMore.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.LoadFragment(new LearninghubFragment());
            }
        });

        binding.rvListCardShopItems.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect,
                                       @NonNull View view,
                                       @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                int spacing = getResources().getDimensionPixelSize(R.dimen.item_spacing);
                int pos = parent.getChildAdapterPosition(view);
                if (pos == 0) outRect.left = spacing;
                outRect.right = spacing;
            }
        });
    }

    private void setupPostRecyclerView() {
        postAdapter = new FragmentHomePostCardItemAdapter(requireContext());
        binding.rvListCardForum.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvListCardForum.setAdapter(postAdapter);
    }

    // FIXED: Location RecyclerView setup
    private void setupLocationRecyclerView() {
        locationAdapter = new FragmentHomeLocationAdapter(requireContext());
        binding.rvListCardLocations.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvListCardLocations.setAdapter(locationAdapter);

        binding.rvListCardLocations.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect,
                                       @NonNull View view,
                                       @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                int spacing = getResources().getDimensionPixelSize(R.dimen.item_spacing);
                int pos = parent.getChildAdapterPosition(view);
                if (pos == 0) outRect.left = spacing;
                outRect.right = spacing;
            }
        });
    }

    // FIXED: Learning Hub uses its own RecyclerView (rvListCardLearninghub)
    private void setupLearninghubRecyclerView() {
        learninghubAdapter = new FragmentHomeLearninghubAdapter(requireContext());

        learninghubAdapter.setOnSaveClickListener((card, isSaved) -> {
            if (learninghubRepository != null) {
                learninghubRepository.toggleSaveCard(card.getUuid(), isSaved);
            }
        });

        binding.rvListCardLearninghub.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvListCardLearninghub.setAdapter(learninghubAdapter);

        binding.rvListCardLearninghub.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect,
                                       @NonNull View view,
                                       @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                int spacing = getResources().getDimensionPixelSize(R.dimen.item_spacing);
                int pos = parent.getChildAdapterPosition(view);
                if (pos == 0) outRect.left = spacing;
                outRect.right = spacing;
            }
        });
    }

    private void setupForecastRecyclerView() {
        forecastAdapter = new WeatherForecastAdapter();
        binding.rvWeatherForecast.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWeatherForecast.setAdapter(forecastAdapter);
    }

    // FIXED: Load locations with visibility filter
    private void loadLocations() {
        locationRepository.getAllLocations(new LocationRepository.LocationCallback<Map<String, Location>>() {
            @Override
            public void onSuccess(Map<String, Location> result) {
                if (result != null) {
                    List<Location> locationList = new ArrayList<>();
                    for (Map.Entry<String, Location> entry : result.entrySet()) {
                        Location loc = entry.getValue();
                        loc.id = entry.getKey();

                        Location.Visibility visibility = loc.visibility;
                        if (visibility == null || visibility.isVisible) {
                            locationList.add(loc);
                        }
                    }
                    Collections.reverse(locationList);
                    int count = Math.min(locationList.size(), 5);
                    if (locationAdapter != null && count > 0) {
                        locationAdapter.setLocations(locationList.subList(0, count));
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("HomeFragment", "Error loading locations: " + error);
            }
        });
    }

    private void loadLearninghubItems() {
        learninghubRepository.getCardsLiveData().observe(getViewLifecycleOwner(), result -> {
            if (result != null && !result.isEmpty()) {
                List<LearninghubCard> copy = new ArrayList<>(result);
                Collections.reverse(copy);
                int count = Math.min(copy.size(), 5);
                if (learninghubAdapter != null) {
                    learninghubAdapter.setItems(copy.subList(0, count));
                }
            }
        });
        learninghubRepository.loadCards();
    }

    private void loadShoppingItems() {
        shoppingRepository.fetchLimitedShoppingItems(30, new ShoppingItemRepository.ShoppingItemCallback<List<ShoppingItem>>() {
            @Override
            public void onSuccess(List<ShoppingItem> items) {
                masterShoppingItems.clear();
                if (items != null) {
                    List<ShoppingItem> filtered = new ArrayList<>();
                    for (ShoppingItem it : items) {
                        if (it == null) continue;
                        if ("deleted".equalsIgnoreCase(it.getStatus())) continue;
                        if ("hidden".equalsIgnoreCase(it.getVisibility())) continue;
                        if (it.getModeration() != null && "warned".equalsIgnoreCase(it.getModeration().getStatus())) {
                            continue;
                        }
                        filtered.add(it);
                    }
                    int count = Math.min(filtered.size(), 5);
                    masterShoppingItems.addAll(filtered.subList(0, count));
                }
                shoppingAdapter.setShoppingItems(new ArrayList<>(masterShoppingItems));
            }

            @Override
            public void onError(String errorMsg) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private long parseTimestamp(String ts) {
        if (ts == null) return 0;
        if (ts.matches("\\d+")) {
            try { return Long.parseLong(ts); }
            catch (Exception e) { return 0; }
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US).parse(ts).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    private void loadPostCardItems() {
        postRef = FirebaseDatabase.getInstance().getReference("postCardItems");
        postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<PostCardItem> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    PostCardItem item = child.getValue(PostCardItem.class);
                    if (item != null) {
                        item.setItemId(child.getKey());
                        String visibility = item.getVisibility();
                        if (visibility == null || VISIBILITY_VISIBLE.equals(visibility)) {
                            list.add(item);
                        }
                    }
                }
                Collections.sort(list, (p1, p2) -> {
                    long t1 = parseTimestamp(p1.getTimestamp());
                    long t2 = parseTimestamp(p2.getTimestamp());
                    return Long.compare(t2, t1);
                });
                if (list.size() > 2) {
                    list = list.subList(0, 2);
                }
                if (postAdapter != null) {
                    postAdapter.setPostCardItemList(list);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Error loading posts", error.toException());
            }
        };
        postRef.addValueEventListener(postListener);
    }

    private void setCurrentDate() {
        Calendar cal = Calendar.getInstance();
        Locale km = new Locale("km", "KH");
        SimpleDateFormat df = new SimpleDateFormat("EEEE, dd MMMM yyyy", km);
        binding.tvDate.setText(df.format(cal.getTime()));
    }

    private void fetchWeatherData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String url = String.format(Locale.getDefault(), BASE_URL,
                        city.replace(" ", "%20"), BuildConfig.OPENWEATHER_API_KEY);

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() != 200) return;

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                double temp = json.getJSONObject("main").getDouble("temp");
                String icon = json.getJSONArray("weather").getJSONObject(0).getString("icon");

                double rainVolume = 0;
                if (json.has("rain")) {
                    JSONObject rainObj = json.getJSONObject("rain");
                    rainVolume = rainObj.optDouble("1h", 0);
                }

                final double finalRainVolume = rainVolume;
                final int rainPercent = calculateRainPercentage(rainVolume, json);

                requireActivity().runOnUiThread(() -> {
                    binding.tvWeatherLocation.setText(city);
                    updateRainPredictionUI(rainPercent, finalRainVolume);
                });

                weatherViewModel.setWeatherData(temp, icon);
                fetchForecast();

            } catch (Exception ignored) {}
        });
    }

    private void fetchForecast() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String url = String.format(Locale.getDefault(), FORECAST_URL,
                        city.replace(" ", "%20"), BuildConfig.OPENWEATHER_API_KEY);

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() != 200) return;

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONArray list = new JSONObject(sb.toString()).getJSONArray("list");

                LinkedHashMap<String, int[]> tempMap = new LinkedHashMap<>();
                LinkedHashMap<String, String> iconMap = new LinkedHashMap<>();
                LinkedHashMap<String, Integer> rainMap = new LinkedHashMap<>();

                for (int i = 0; i < list.length(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    String dateTime = item.getString("dt_txt");
                    String date = dateTime.substring(0, 10);

                    int tempMin = (int) Math.round(item.getJSONObject("main").getDouble("temp_min"));
                    int tempMax = (int) Math.round(item.getJSONObject("main").getDouble("temp_max"));
                    String icon = item.getJSONArray("weather").getJSONObject(0).getString("icon");

                    int rainProb = calculateRainProbabilityFromForecast(item);

                    if (!tempMap.containsKey(date)) {
                        tempMap.put(date, new int[]{tempMin, tempMax});
                        iconMap.put(date, icon);
                        rainMap.put(date, rainProb);
                    } else {
                        int[] existing = tempMap.get(date);
                        existing[0] = Math.min(existing[0], tempMin);
                        existing[1] = Math.max(existing[1], tempMax);
                        tempMap.put(date, existing);
                        int existingRain = rainMap.get(date);
                        rainMap.put(date, Math.max(existingRain, rainProb));
                    }

                    if (tempMap.size() >= 10) break;
                }

                List<ForecastDay> result = new ArrayList<>();
                SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat out = new SimpleDateFormat("EEE", new Locale("km", "KH"));

                int dayCount = 0;
                for (String d : tempMap.keySet()) {
                    if (dayCount >= 10) break;
                    int[] mm = tempMap.get(d);
                    int rainPercent = rainMap.get(d);
                    result.add(new ForecastDay(
                            out.format(in.parse(d)),
                            mm[0],
                            mm[1],
                            "",
                            iconMap.get(d),
                            rainPercent
                    ));
                    dayCount++;
                }

                requireActivity().runOnUiThread(() -> {
                    if (forecastAdapter != null) {
                        forecastAdapter.setItems(result);
                    }
                });

            } catch (Exception ignored) {}
        });
    }

    private int calculateRainPercentage(double rainVolume, JSONObject weatherJson) {
        int percentage = 0;
        if (rainVolume > 0) {
            percentage = (int) Math.min(rainVolume * 15, 95);
        }
        try {
            int clouds = weatherJson.getJSONObject("clouds").getInt("all");
            if (clouds > 50) {
                percentage = Math.max(percentage, (int) (clouds * 0.6));
            }
        } catch (Exception e) {}
        try {
            String weatherMain = weatherJson.getJSONArray("weather").getJSONObject(0).getString("main");
            switch (weatherMain) {
                case "Rain":
                case "Drizzle":
                    percentage = Math.max(percentage, 75);
                    break;
                case "Thunderstorm":
                    percentage = Math.max(percentage, 85);
                    break;
                case "Clouds":
                    if (percentage == 0) percentage = 20;
                    break;
            }
        } catch (Exception e) {}
        return Math.min(percentage, 100);
    }

    private int calculateRainProbabilityFromForecast(JSONObject forecastItem) {
        int probability = 0;
        try {
            if (forecastItem.has("rain")) {
                JSONObject rain = forecastItem.getJSONObject("rain");
                double rain3h = rain.optDouble("3h", 0);
                probability = (int) Math.min(rain3h * 8, 95);
            }
            if (forecastItem.has("snow")) {
                probability = Math.max(probability, 50);
            }
            if (forecastItem.has("clouds")) {
                int clouds = forecastItem.getJSONObject("clouds").getInt("all");
                probability = Math.max(probability, (int) (clouds * 0.5));
            }
            String weatherMain = forecastItem.getJSONArray("weather").getJSONObject(0).getString("main");
            switch (weatherMain) {
                case "Rain":
                case "Drizzle":
                    probability = Math.max(probability, 70);
                    break;
                case "Thunderstorm":
                    probability = Math.max(probability, 85);
                    break;
                case "Clouds":
                    if (probability < 30) probability = 25;
                    break;
            }
        } catch (Exception e) {}
        return Math.min(probability, 100);
    }

    private void updateRainPredictionUI(int rainPercent, double rainVolume) {
        if (binding == null) return;
        if (rainPercent > 0) {
            binding.rainPredictionContainer.setVisibility(View.VISIBLE);
            binding.tvRainProbability.setText(String.format(Locale.getDefault(), "%d%%", rainPercent));
            if (rainVolume > 5 || rainPercent > 70) {
                binding.imgRain.setImageResource(R.drawable.ic_rain_heavy);
            } else if (rainVolume > 1 || rainPercent > 40) {
                binding.imgRain.setImageResource(R.drawable.ic_rain_light);
            } else {
                binding.imgRain.setImageResource(R.drawable.ic_rain);
            }
        } else {
            binding.rainPredictionContainer.setVisibility(View.GONE);
        }
    }

    private void updateWeatherUI(double temp, String icon) {
        binding.tvWeatherNumber.setText(String.format(Locale.getDefault(), "%.0f°", temp));
        updateWeatherIcon(icon);
    }

    private void updateWeatherIcon(String iconCode) {
        if (binding == null) return;
        int iconResId;
        switch (iconCode) {
            case "01d": case "01n": iconResId = R.drawable.sunny; break;
            case "02d": case "02n": case "03d": case "03n": iconResId = R.drawable.pcloudy; break;
            case "04d": case "04n": iconResId = R.drawable.cloudy; break;
            case "09d": case "09n": case "10d": case "10n": iconResId = R.drawable.rainy; break;
            case "11d": case "11n": iconResId = R.drawable.tstorm; break;
            case "13d": case "13n": iconResId = R.drawable.snowy; break;
            default: iconResId = R.drawable.pcloudy; break;
        }
        binding.weatherIcon.setImageResource(iconResId);
    }

    private String normalizeCityName(String input) {
        if (input == null || input.isEmpty()) return "Phnom Penh";
        input = input.replace("Province", "").replace("City", "").replace("ខេត្ត", "").trim();
        if (input.contains("បន្ទាយមានជ័យ") || input.contains("Banteay Meanchey")) return "Banteay Meanchey";
        if (input.contains("ភ្នំពេញ") || input.contains("Phnom Penh")) return "Phnom Penh";
        if (input.contains("សៀមរាប") || input.contains("Siem Reap")) return "Siem Reap";
        if (input.contains("បាត់ដំបង") || input.contains("Battambang")) return "Battambang";
        input = input.toLowerCase(Locale.ENGLISH);
        if (input.isEmpty()) return "Phnom Penh";
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadShoppingItems();
        loadPostCardItems();
        loadLocations();
        checkUnreadNotifications();
    }

    private void checkUnreadNotifications() {
        if (userId != null) {
            notificationRepository.getUserNotifications(userId, new IApiCallback<Map<String, Notification>>() {
                @Override
                public void onSuccess(Map<String, Notification> data) {
                    boolean hasUnread = false;
                    for (Notification notification : data.values()) {
                        boolean isForUser = notification.getReceiverId() != null &&
                                notification.getReceiverId().equals(userId);
                        boolean isFromAdmin = notification.getSender() != null &&
                                notification.getSender().equalsIgnoreCase("admin");
                        if ((isForUser || isFromAdmin) && notification.isUnread()) {
                            hasUnread = true;
                            break;
                        }
                    }
                    boolean finalHasUnread = hasUnread;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null && binding.notificationBadge != null) {
                                binding.notificationBadge.setVisibility(finalHasUnread ? View.VISIBLE : View.GONE);
                            }
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null && binding.notificationBadge != null) {
                                binding.notificationBadge.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postRef != null && postListener != null) {
            postRef.removeEventListener(postListener);
        }
        binding = null;
    }
}