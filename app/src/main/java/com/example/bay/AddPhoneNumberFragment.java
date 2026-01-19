package com.example.bay;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.bay.databinding.FragmentAddPhoneNumberBinding;

public class AddPhoneNumberFragment extends Fragment {

    private FragmentAddPhoneNumberBinding binding;
    HomeActivity homeActivity;
    private final String PREFIX = "+855";

    public AddPhoneNumberFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentAddPhoneNumberBinding.inflate(inflater, container, false);

        homeActivity = (HomeActivity) getActivity();

        setupListeners();
        return binding.getRoot();
    }

    private void setupListeners() {
        // Set the prefix initially and move cursor to the end
        binding.etPhoneNumber.setText(PREFIX);
        binding.etPhoneNumber.setSelection(PREFIX.length());

        binding.nextButton.setOnClickListener(v -> {
            // Get the full phone number with prefix
            String fullPhoneNumber = binding.etPhoneNumber.getText().toString().trim();

            // Remove any spaces
            fullPhoneNumber = fullPhoneNumber.replaceAll("\\s+", "");

            // Extract digits after prefix (from position 3 onwards, since +855 is positions 0-3)
            String phoneNumberDigits = "";
            if (fullPhoneNumber.startsWith(PREFIX) && fullPhoneNumber.length() > PREFIX.length()) {
                phoneNumberDigits = fullPhoneNumber.substring(PREFIX.length());
            }

            // Clear previous validation message
            binding.tvValidate.setVisibility(View.GONE);

            // Validation checks
            boolean isValid = true;

            // Check 1: Phone number should start with +855
            if (!fullPhoneNumber.startsWith(PREFIX)) {
                binding.tvValidate.setText("Phone number must start with +855");
                binding.tvValidate.setVisibility(View.VISIBLE);
                isValid = false;
            }
            // Check 2: Should have digits after prefix
            else if (phoneNumberDigits.isEmpty()) {
                binding.tvValidate.setText("Please enter phone number after +855");
                binding.tvValidate.setVisibility(View.VISIBLE);
                isValid = false;
            }
            // Check 3: Should have at least 8 digits after prefix (for Cambodia)
            else if (phoneNumberDigits.length() < 8) {
                binding.tvValidate.setText("Phone number should have at least 8 digits");
                binding.tvValidate.setVisibility(View.VISIBLE);
                isValid = false;
            }
            // Check 4: Should not be just +8550 or +855 0
            else if (phoneNumberDigits.equals("0") || phoneNumberDigits.trim().equals("0")) {
                binding.tvValidate.setText("Please enter a valid phone number");
                binding.tvValidate.setVisibility(View.VISIBLE);
                isValid = false;
            }
            // Check 5: First digit after +855 should be valid (typically 9, 1, 6, 7, 8 for Cambodia)
            else if (!isValidCambodianPhoneNumber(phoneNumberDigits)) {
                binding.tvValidate.setText("Please enter a valid Cambodian phone number");
                binding.tvValidate.setVisibility(View.VISIBLE);
                isValid = false;
            }

            // If all validations pass
            if (isValid) {
                // Format the final phone number as "+8559897878" (no space)
                String finalPhoneNumber = PREFIX + phoneNumberDigits.replaceAll("\\s+", "");

                // Navigate to verification fragment
                navigateToVerificationFragment(finalPhoneNumber);
            }
        });

        binding.etPhoneNumber.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                String text = binding.etPhoneNumber.getText().toString();
                // Prevent deleting the prefix
                if (text.equals(PREFIX) || text.length() <= PREFIX.length()) {
                    binding.etPhoneNumber.setText(PREFIX);
                    binding.etPhoneNumber.setSelection(PREFIX.length());
                    return true;
                }
            }
            return false;
        });

        binding.etPhoneNumber.setOnClickListener(v -> {
            binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
        });

        binding.etPhoneNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
            }
        });

        binding.button.setOnClickListener(v -> {
            if (homeActivity != null){
                homeActivity.onBackPressed();
            }
        });
    }

    private boolean isValidCambodianPhoneNumber(String digits) {
        if (digits == null || digits.isEmpty()) return false;

        // Remove any spaces
        String cleanDigits = digits.replaceAll("\\s+", "");

        // Cambodian phone numbers typically start with: 9, 1, 6, 7, 8
        // and have 8 digits total after +855
        if (cleanDigits.length() < 8) return false;

        char firstDigit = cleanDigits.charAt(0);
        return firstDigit == '9' || firstDigit == '1' || firstDigit == '6' ||
                firstDigit == '7' || firstDigit == '8';
    }

    private void navigateToVerificationFragment(String phoneNumber) {
        Bundle bundle = new Bundle();
        bundle.putString("phone_number", phoneNumber);

        PhoneNumberVerifyingFragment fragment = new PhoneNumberVerifyingFragment();
        fragment.setArguments(bundle);

        if (homeActivity != null) {
            homeActivity.LoadFragment(fragment);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}