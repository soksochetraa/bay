package com.example.bay.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.bay.databinding.FragmentChangePhoneNumberBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ChangePhoneNumberFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FragmentChangePhoneNumberBinding binding;
    private final String PREFIX = "+855 ";
    private String phone;

    public ChangePhoneNumberFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            phone = getArguments().getString("phone", "");

            if (phone != null) {
                if (phone.startsWith("+855")) {
                    phone = phone.substring(4);
                } else if (phone.startsWith("0")) {
                    phone = phone.substring(1);
                } else if (phone.startsWith("855")) {
                    phone = phone.substring(3);
                }
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentChangePhoneNumberBinding.inflate(inflater, container, false);

        setupViews();
        setupListeners();

        return binding.getRoot();
    }

    private void setupListeners() {
        binding.etPhoneNumber.setOnClickListener(v -> handlePhoneNumberClick());

        binding.btnNext.setOnClickListener(v -> {
            String phoneInput = binding.etPhoneNumber.getText().toString().trim();

            if (!isValidPhoneNumber(phoneInput)) return;

            String clean = phoneInput.replace(PREFIX, "").trim();
            String finalPhone = "+855" + clean;

            checkPhoneExists(finalPhone);
        });
    }

    private void checkPhoneExists(String finalPhone) {

        String currentUid = mAuth.getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");

        Query query = ref.orderByChild("phone").equalTo(finalPhone);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    boolean isUsedByOther = false;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        String uid = child.getKey();

                        if (!uid.equals(currentUid)) {
                            isUsedByOther = true;
                            break;
                        }
                    }

                    if (isUsedByOther) {
                        showToast("លេខនេះត្រូវបានប្រើរួចហើយ!");
                    } else {
                        proceedNext(finalPhone);
                    }

                } else {
                    proceedNext(finalPhone);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("មានបញ្ហា សូមព្យាយាមម្ដងទៀត!");
            }
        });
    }

    private void proceedNext(String phone) {
        Toast.makeText(requireContext(), "លេខត្រឹមត្រូវ: " + phone, Toast.LENGTH_SHORT).show();

    }

    private void setupViews() {
        setupPhoneEditText();

        if (phone != null && !phone.isEmpty()) {
            binding.etPhoneNumber.setText(PREFIX + phone);
        } else {
            binding.etPhoneNumber.setText(PREFIX);
        }

        binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
    }

    private void handlePhoneNumberClick() {
        if (binding.etPhoneNumber.getText().toString().isEmpty()) {
            binding.etPhoneNumber.setText(PREFIX);
        }
        binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty() || phoneNumber.equals(PREFIX)) {
            showToast("សូមបញ្ចូលលេខទូរស័ព្ទ!");
            return false;
        }

        String numberPart = phoneNumber.replace(PREFIX, "").trim();
        if (!numberPart.matches("^[1-9][0-9]{7,9}$")) {
            showToast("លេខទូរស័ព្ទមិនត្រឹមត្រូវទេ!");
            return false;
        }

        return true;
    }

    private void setupPhoneEditText() {
        binding.etPhoneNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !binding.etPhoneNumber.getText().toString().startsWith(PREFIX)) {
                binding.etPhoneNumber.setText(PREFIX);
                binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
            }
        });

        binding.etPhoneNumber.addTextChangedListener(new TextWatcher() {
            private String previous = PREFIX;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previous = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String current = s.toString();

                if (!current.startsWith(PREFIX)) {
                    binding.etPhoneNumber.setText(PREFIX);
                    binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
                    return;
                }

                if (current.length() > PREFIX.length()) {
                    char firstDigit = current.charAt(PREFIX.length());
                    if (firstDigit == '0') {
                        showToast("មិនអនុញ្ញាតឱ្យប្រើលេខ 0 បន្ទាប់ពី +855");
                        binding.etPhoneNumber.setText(previous);
                        binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
                    }
                }
            }
        });

        binding.etPhoneNumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}