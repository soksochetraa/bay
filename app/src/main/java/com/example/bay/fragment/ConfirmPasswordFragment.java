package com.example.bay.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bay.databinding.FragmentConfirmPasswordBinding;
import com.example.bay.model.User;
import com.example.bay.viewmodel.SharedUserViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.bay.repository.UserRepository;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ConfirmPasswordFragment extends Fragment {

    private FragmentConfirmPasswordBinding binding;
    private String userId;
    private String firstName;
    private String lastName;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final UserRepository userRepository = new UserRepository();
    private SharedUserViewModel sharedUserViewModel;

    public ConfirmPasswordFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedUserViewModel = new ViewModelProvider(requireActivity()).get(SharedUserViewModel.class);
        if (getArguments() != null) {
            userId = getArguments().getString("user_id");
            firstName = getArguments().getString("first_name");
            lastName = getArguments().getString("last_name");
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentConfirmPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        binding.btnSave.setOnClickListener(v -> {
            String password = binding.etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "សូមបញ្ចូលលេខសម្ងាត់", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser firebaseUser = auth.getCurrentUser();
            if (firebaseUser == null || firebaseUser.getEmail() == null) return;

            AuthCredential credential =
                    EmailAuthProvider.getCredential(firebaseUser.getEmail(), password);

            firebaseUser.reauthenticate(credential)
                    .addOnSuccessListener(unused -> updateUserName())
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "លេខសម្ងាត់មិនត្រឹមត្រូវ",
                                    Toast.LENGTH_SHORT).show()
                    );
        });

        binding.tvForgetPassword.setOnClickListener(v -> {
            if (auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null) {
                auth.sendPasswordResetEmail(auth.getCurrentUser().getEmail());
                Toast.makeText(requireContext(),
                        "បានផ្ញើអ៊ីមែលស្តារលេខសម្ងាត់",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUserName() {
        User user = sharedUserViewModel.getCurrentUser().getValue();
        if (user != null) {
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setLastNameChangedAt(System.currentTimeMillis());

            userRepository.updateUser(userId, user, new UserRepository.UserCallback<User>() {
                @Override
                public void onSuccess(User result) {
                    Toast.makeText(requireContext(),
                            "ប្ដូរឈ្មោះបានជោគជ័យ",
                            Toast.LENGTH_SHORT).show();
                    // Navigate back to the previous screen
                    // Popping the confirm fragment, and popping the change fragment
                    requireActivity().onBackPressed();
                    requireActivity().onBackPressed();
                }

                @Override
                public void onError(String errorMsg) {
                    Toast.makeText(requireContext(),
                            errorMsg,
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(requireContext(), "ទិន្នន័យអ្នកប្រើប្រាស់មិនទាន់មាន", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
