package com.aidentx.encryption.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service for field-level encryption within JSON configurations.
 * Allows encrypting specific sensitive fields while keeping the rest of the configuration readable.
 */
public interface FieldEncryptionService {

    /**
     * Encrypts sensitive fields in a JSON configuration based on field patterns.
     *
     * @param jsonData            The JSON data to process
     * @param configType          The configuration type for key derivation
     * @param encryptEntireConfig Whether to encrypt the entire configuration or just sensitive fields
     * @return JsonNode with sensitive fields encrypted in $crypto format
     */
    JsonNode encryptSensitiveFields(JsonNode jsonData, String configType, boolean encryptEntireConfig);

    /**
     * Decrypts $crypto fields in a JSON configuration.
     *
     * @param jsonData   The JSON data containing encrypted fields
     * @param configType The configuration type for key derivation
     * @return JsonNode with decrypted sensitive fields
     */
    JsonNode decryptSensitiveFields(JsonNode jsonData, String configType);

    /**
     * Checks if a field name indicates sensitive data that should be encrypted.
     *
     * @param fieldName The field name to check
     * @return true if the field should be encrypted
     */
    boolean isSensitiveField(String fieldName);

    /**
     * Checks if a JSON object contains any encrypted $crypto fields.
     *
     * @param jsonData The JSON data to check
     * @return true if any encrypted fields are found
     */
    boolean containsEncryptedFields(JsonNode jsonData);
}
