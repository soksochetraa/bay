package com.example.bay.util;

import android.util.Base64;
import android.util.Log;

/**
 * AESUtils provides static encryption/decryption utilities for chat messages.
 * It delegates to CryptoUtil which uses AndroidKeyStore with AES/GCM.
 */
public class AESUtils {
    private static final String TAG = "AESUtils";

    /**
     * Encrypt plain text using AES/GCM and return Base64 encoded ciphertext.
     */
    public static String encrypt(String plainText) {
        try {
            return CryptoUtil.encrypt(plainText);
        } catch (Exception e) {
            Log.e(TAG, "Encryption error", e);
            return null;
        }
    }

    /**
     * Decrypt Base64 encoded ciphertext and return the original plain text.
     */
    public static String decrypt(String encryptedText) {
        try {
            return CryptoUtil.decrypt(encryptedText);
        } catch (Exception e) {
            Log.e(TAG, "Decryption error", e);
            return null;
        }
    }
}
