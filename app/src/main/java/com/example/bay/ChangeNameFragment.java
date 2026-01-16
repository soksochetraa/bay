package com.example.bay;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bay.databinding.FragmentChangeNameBinding;

public class ChangeNameFragment extends Fragment {

    private FragmentChangeNameBinding binding;
    private String userId;

    public ChangeNameFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null || TextUtils.isEmpty(args.getString("user_id"))) {
            Toast.makeText(requireContext(),
                    "Invalid session. Please reopen profile.",
                    Toast.LENGTH_LONG).show();
            requireActivity().onBackPressed();
            return;
        }

        userId = args.getString("user_id");
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentChangeNameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        binding.button.setOnClickListener(v ->
                requireActivity().onBackPressed()
        );

        binding.btnSaveName.setOnClickListener(v -> {
            String firstName = binding.etFirstName.getText().toString().trim();
            String lastName = binding.etLastName.getText().toString().trim();

            if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
                Toast.makeText(requireContext(),
                        "សូមបញ្ចូលឈ្មោះពេញ",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            bundle.putString("first_name", firstName);
            bundle.putString("last_name", lastName);

            ConfirmPasswordFragment fragment = new ConfirmPasswordFragment();
            fragment.setArguments(bundle);

            ((HomeActivity) requireActivity()).LoadFragment(fragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
