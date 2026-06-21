package com.aidentx.encryption.service;

public interface KeyManagementService {

    /**
     * Retrieves the decrypted, ready-to-use configuration key for a specific type.
     * Handles caching and decryption using the master key.
     *
     * @param configType The type of configuration (e.g., "sso").
     * @return The raw byte[] of the decryption key.
     */
    byte[] getConfigurationKey(String configType);

    /**
     * Performs a rotation of all configuration keys.
     * This generates new configuration keys and encrypts them with the certificate-derived master key.
     * Note: The master key is derived from the certificate and doesn't need rotation.
     */
    void rotateAllKeys();
}