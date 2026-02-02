package com.example.bay;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bay.R;
import com.example.bay.databinding.FragmentContactUsBinding;
import com.google.android.material.snackbar.Snackbar;

public class ContactUsFragment extends Fragment {

    private FragmentContactUsBinding binding;

    // Contact information
    private static final String EMAIL = "info@baydigitals.com";
    private static final String PHONE = "+85523999999";
    private static final String ADDRESS = "ផ្លូវត្រឡោកបែក រាជធានីភ្នំពេញ";
    private static final String FACEBOOK_PAGE = "baydigitals";
    private static final String TELEGRAM_CHANNEL = "baydigitals";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentContactUsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupClickListeners();
        setupContactInfo();
    }

    private void setupContactInfo() {
        binding.tvEmail.setText(EMAIL);
        binding.tvPhone.setText(formatPhoneNumber(PHONE));
        binding.tvAddress.setText(ADDRESS);
    }

    private String formatPhoneNumber(String phone) {
        // Format phone number with Khmer numerals
        return phone.replace("+855", "+៨៥៥");
    }

    private void setupClickListeners() {
        binding.btnEmailContact.setOnClickListener(v -> sendEmail());

        binding.btnPhoneContact.setOnClickListener(v -> makePhoneCall());

        binding.btnAddress.setOnClickListener(v -> openMaps());

        binding.btnFacebook.setOnClickListener(v -> openFacebook());
        binding.btnTelegram.setOnClickListener(v -> openTelegram());

        binding.backButton.setOnClickListener(v -> requireActivity().onBackPressed());

    }

    private void sendEmail() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + EMAIL));
            intent.putExtra(Intent.EXTRA_SUBJECT, "ទំនាក់ទំនងពីកម្មវិធី Bay Digital Ecosystem");
            intent.putExtra(Intent.EXTRA_TEXT, "សួស្តីក្រុមហ៊ុន Bay Digital,\n\n");

            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                showToast("មិនមានកម្មវិធីអ៊ីមែលនៅក្នុងទូរស័ព្ទរបស់អ្នក");
            }
        } catch (Exception e) {
            showToast("មិនអាចបើកកម្មវិធីអ៊ីមែលបាន");
        }
    }

    private void makePhoneCall() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + PHONE));

            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                showToast("មិនអាចដាក់សំឡេងទូរស័ព្ទបាន");
            }
        } catch (Exception e) {
            showToast("មិនអាចដាក់សំឡេងទូរស័ព្ទបាន");
        }
    }

    private void openMaps() {
        try {
            String mapUri = "geo:0,0?q=" + Uri.encode(ADDRESS + ", Phnom Penh, Cambodia");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUri));
            intent.setPackage("com.google.android.apps.maps");

            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Open in browser if Google Maps not installed
                String webUrl = "https://maps.google.com/?q=" + Uri.encode(ADDRESS + ", Phnom Penh, Cambodia");
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)));
            }
        } catch (Exception e) {
            showToast("មិនអាចបើកផែនទីបាន");
        }
    }

    private void openFacebook() {
        try {
            // Try to open Facebook app first
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("fb://page/" + FACEBOOK_PAGE));

            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Open in browser if Facebook app not installed
                String url = "https://facebook.com/" + FACEBOOK_PAGE;
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        } catch (Exception e) {
            showToast("មិនអាចបើក Facebook បាន");
        }
    }

    private void openTelegram() {
        try {
            // Try to open Telegram app first
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("tg://resolve?domain=" + TELEGRAM_CHANNEL));

            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Open in browser if Telegram app not installed
                String url = "https://t.me/" + TELEGRAM_CHANNEL;
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        } catch (Exception e) {
            showToast("មិនអាចបើក Telegram បាន");
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showSnackbar(String message) {
        if (binding != null && binding.getRoot() != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // You can add any resume logic here
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private boolean isAppInstalled(String packageName) {
        try {
            requireContext().getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}