package com.aidentx.encryption.service;

import com.aidentx.encryption.exception.DecryptionException;

public interface EncryptionService {

    /**
     * Encrypts plaintext data for a given configuration type.
     *
     * @param plainText  The string data to encrypt.
     * @param configType The type of configuration, used to select the correct
     *                   encryption key.
     * @return A structured, Base64-encoded string containing the ciphertext, IV,
     *         salt, and auth tag.
     */
    String encrypt(String plainText, String configType);

    /**
     * Decrypts a structured, encrypted string.
     *
     * @param encryptedData The structured, Base64-encoded string from the encrypt
     *                      method.
     * @param configType    The type of configuration, used to select the correct
     *                      decryption key.
     * @return The original plaintext string.
     * @throws DecryptionException if the data is tampered with or decryption fails.
     */
    String decrypt(String encryptedData, String configType) throws DecryptionException;
}