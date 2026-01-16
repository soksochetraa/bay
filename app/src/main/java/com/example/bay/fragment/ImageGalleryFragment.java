package com.example.bay.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bay.R;
import com.example.bay.adapter.ImageGalleryAdapter;

import java.util.ArrayList;
import java.util.List;

public class ImageGalleryFragment extends Fragment {

    private static final String ARG_PHOTOS = "photos";
    private static final String ARG_START_POSITION = "start_position";

    private ViewPager2 viewPager;
    private TextView pageIndicator;
    private List<String> photos;
    private int startPosition;

    public static ImageGalleryFragment newInstance(ArrayList<String> photos, int startPosition) {
        ImageGalleryFragment fragment = new ImageGalleryFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PHOTOS, photos);
        args.putInt(ARG_START_POSITION, startPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            photos = getArguments().getStringArrayList(ARG_PHOTOS);
            startPosition = getArguments().getInt(ARG_START_POSITION, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_image_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnClose = view.findViewById(R.id.btnClose);
        pageIndicator = view.findViewById(R.id.pageIndicator);
        viewPager = view.findViewById(R.id.viewPager);

        ImageGalleryAdapter adapter = new ImageGalleryAdapter(photos);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPosition, false);

        updatePageIndicator();
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updatePageIndicator();
            }
        });

        btnClose.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });
    }

    private void updatePageIndicator() {
        if (photos != null && pageIndicator != null) {
            int current = viewPager.getCurrentItem() + 1;
            int total = photos.size();
            pageIndicator.setText(current + " / " + total);
        }
    }
}