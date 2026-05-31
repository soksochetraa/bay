package com.example.bay.util;

import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CryptoUtil {
    private static final String TAG = "CryptoUtil";
    private static final String KEY_ALIAS = "bay_chat_key";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12; // 96 bits for GCM
    private static final int TAG_LENGTH = 128; // bits

    private static SecretKey getSecretKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEY_STORE);
            keyGenerator.init(KEY_SIZE);
            keyGenerator.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    public static String encrypt(String plainText) {
        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));
            // Concatenate IV + cipherText
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encryption error", e);
            return null;
        }
    }

    public static String decrypt(String encrypted) {
        try {
            byte[] data = Base64.decode(encrypted, Base64.NO_WRAP);
            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            byte[] iv = new byte[IV_SIZE];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec);
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "Decryption error", e);
            return null;
        }
    }
}
