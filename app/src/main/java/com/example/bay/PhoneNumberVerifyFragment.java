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

import com.example.bay.databinding.FragmentPhoneNumberVerifyBinding;
import com.example.bay.model.User;
import com.example.bay.repository.UserRepository;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneNumberVerifyFragment extends Fragment {

    private FragmentPhoneNumberVerifyBinding binding;
    private String phone;
    private String userId;
    private String verificationId;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();
        if (b != null) {
            phone = b.getString("phone");
            userId = b.getString("userId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPhoneNumberVerifyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.button.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.nextButton.setOnClickListener(v -> verifyCode());
        sendOtp();
    }

    private void sendOtp() {
        if (TextUtils.isEmpty(phone)) return;

        binding.loading.setVisibility(View.VISIBLE);

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(requireActivity())
                        .setCallbacks(callbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode() {
        String code = binding.etPhoneNumber.getText().toString().trim();
        if (TextUtils.isEmpty(code) || verificationId == null) return;

        binding.loading.setVisibility(View.VISIBLE);

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(verificationId, code);

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        user.updatePhoneNumber(credential)
                .addOnSuccessListener(v -> savePhone())
                .addOnFailureListener(e -> {
                    binding.loading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void savePhone() {
        userRepository.getUserById(userId, new UserRepository.UserCallback<User>() {
            @Override
            public void onSuccess(User u) {
                u.setPhone(phone);
                userRepository.updateUser(userId, u, new UserRepository.UserCallback<User>() {
                    @Override
                    public void onSuccess(User r) {
                        binding.loading.setVisibility(View.GONE);
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }

                    @Override
                    public void onError(String e) {
                        binding.loading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), e, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String e) {
                binding.loading.setVisibility(View.GONE);
                Toast.makeText(getContext(), e, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;

                    user.updatePhoneNumber(credential)
                            .addOnSuccessListener(v -> savePhone())
                            .addOnFailureListener(e -> {
                                binding.loading.setVisibility(View.GONE);
                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    binding.loading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCodeSent(@NonNull String id,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = id;
                    binding.loading.setVisibility(View.GONE);
                }
            };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
