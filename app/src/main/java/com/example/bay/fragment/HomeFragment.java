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
    private FragmentHomeShoppingCardAdapter shoppingAdapter;
    private FragmentHomePostCardItemAdapter postAdapter;
    private WeatherForecastAdapter forecastAdapter;

    private ShoppingItemRepository shoppingRepository;
    private PostCardItemRepository postRepository;
    private UserRepository userRepository;

    private FirebaseAuth mAuth;
    private HomeViewModel weatherViewModel;
    private HomeActivity homeActivity;

    private final List<ShoppingItem> masterShoppingItems = new ArrayList<>();
    private String city = "Phnom Penh";

    private static final String BASE_URL =
            "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=kh";
    private static final String FORECAST_URL =
            "https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric&lang=kh";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        weatherViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        shoppingRepository = new ShoppingItemRepository();
        postRepository = new PostCardItemRepository();
        userRepository = new UserRepository();
        mAuth = FirebaseAuth.getInstance();

        homeActivity = (HomeActivity) getActivity();
        if (homeActivity != null) homeActivity.showBottomNavigation();

        setupRecyclerView();
        setupPostRecyclerView();
        setupForecastRecyclerView();
        setupFilterChips();
        setCurrentDate();

        loadShoppingItems();
        loadPostCardItems();

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            showLoading();
            loadUserProfile(firebaseUser.getUid());
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
                homeActivity.setBottomNavigationVisible(false);
                homeActivity.LoadFragment(DetailItemShoppingFragment.newInstance(item));
            }
        });

        binding.farmMap.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.setBottomNavigationVisible(false);
                homeActivity.LoadFragment(new FarmMapFragment());
            }
        });

        binding.goToLearninghub.setOnClickListener(v -> {
            if (homeActivity != null) {
                homeActivity.setBottomNavigationVisible(false);
                homeActivity.LoadFragment(new LearninghubFragment());
            }
        });

        binding.goToMarketplace.setOnClickListener(v->{
            if (homeActivity != null) {
                homeActivity.navigateTo(R.id.nav_marketplace, new MarketPlaceFragment());
            }
        });

        binding.rvListCardShopItems.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
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

    private void setupForecastRecyclerView() {
        forecastAdapter = new WeatherForecastAdapter();
        binding.rvWeatherForecast.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWeatherForecast.setAdapter(forecastAdapter);
    }

    private void setupFilterChips() {
        binding.filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            List<ShoppingItem> filtered = new ArrayList<>();
            if (checkedId == R.id.chip_all || checkedId == View.NO_ID) {
                filtered.addAll(masterShoppingItems);
            } else {
                for (ShoppingItem item : masterShoppingItems) {
                    if (item == null || item.getCategory() == null) continue;
                    String c = item.getCategory().toLowerCase(Locale.ENGLISH);
                    if (checkedId == R.id.chip_vegetable && (c.contains("vegetable") || c.contains("បន្លែ")))
                        filtered.add(item);
                    if (checkedId == R.id.chip_fruit && (c.contains("fruit") || c.contains("ផ្លែឈើ")))
                        filtered.add(item);
                    if (checkedId == R.id.chip_tool && (c.contains("tool") || c.contains("សម្ភារៈ")))
                        filtered.add(item);
                }
            }
            shoppingAdapter.setShoppingItems(filtered.size() > 5 ? filtered.subList(0, 5) : filtered);
        });
    }

    private void loadShoppingItems() {
        shoppingRepository.fetchLimitedShoppingItems(5, new ShoppingItemRepository.ShoppingItemCallback<List<ShoppingItem>>() {
            @Override
            public void onSuccess(List<ShoppingItem> items) {
                masterShoppingItems.clear();
                if (items != null) masterShoppingItems.addAll(items);
                shoppingAdapter.setShoppingItems(new ArrayList<>(masterShoppingItems));
            }

            @Override
            public void onError(String errorMsg) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPostCardItems() {
        postRepository.fetchLatestTwoPosts(new PostCardItemRepository.OnLatestPostsLoadedListener() {
            @Override
            public void onSuccess(List<PostCardItem> posts) {
                postAdapter.setPostCardItemList(posts);
            }

            @Override
            public void onError(Throwable t) {
                Log.e("HomeFragment", "Post load error");
            }
        });
    }

    private void setCurrentDate() {
        Calendar cal = Calendar.getInstance();
        Locale km = new Locale("km", "KH");
        SimpleDateFormat df = new SimpleDateFormat("EEEE, dd MMMM yyyy", km);
        binding.tvDate.setText(df.format(cal.getTime()));
    }

    private void loadUserProfile(String userId) {
        userRepository.getUserById(userId, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User user) {
                city = normalizeCityName(user.getLocation());
                binding.tvUsername.setText(user.getLast_name());
                Glide.with(requireContext()).load(user.getProfileImageUrl()).into(binding.btnProfile);
                fetchWeatherData();
                hideLoading();
            }

            @Override
            public void onError(String errorMsg) {
                hideLoading();
            }
        });
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

                requireActivity().runOnUiThread(() -> binding.tvWeatherLocation.setText(city));
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
                LinkedHashMap<String, int[]> map = new LinkedHashMap<>();
                LinkedHashMap<String, String> icons = new LinkedHashMap<>();

                for (int i = 0; i < list.length(); i++) {
                    JSONObject o = list.getJSONObject(i);
                    String date = o.getString("dt_txt").substring(0, 10);
                    int min = (int) o.getJSONObject("main").getDouble("temp_min");
                    int max = (int) o.getJSONObject("main").getDouble("temp_max");
                    String icon = o.getJSONArray("weather").getJSONObject(0).getString("icon");

                    if (!map.containsKey(date)) {
                        map.put(date, new int[]{min, max});
                        icons.put(date, icon);
                    }
                    if (map.size() == 5) break;
                }

                List<ForecastDay> result = new ArrayList<>();
                SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat out = new SimpleDateFormat("EEE", new Locale("km", "KH"));

                for (String d : map.keySet()) {
                    int[] mm = map.get(d);
                    result.add(new ForecastDay(out.format(in.parse(d)), mm[0], mm[1], "", icons.get(d)));
                }

                requireActivity().runOnUiThread(() -> forecastAdapter.setItems(result));

            } catch (Exception ignored) {}
        });
    }

    private void updateWeatherUI(double temp, String icon) {
        binding.tvWeatherNumber.setText(String.format(Locale.getDefault(), "%.0f°", temp));
        binding.weatherIcon.setImageResource(R.drawable.pcloudy);
    }

    private String normalizeCityName(String input) {
        if (input == null || input.isEmpty()) return "Phnom Penh";
        input = input.replace("Province", "").replace("City", "").replace("ខេត្ត", "").trim();
        input = input.toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    private void showLoading() {
        if (homeActivity != null) homeActivity.showLoading();
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
