package com.aidentx.configuration.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * A Data Transfer Object (DTO) that bundles decrypted configuration data with
 * its
 * corresponding encryption metadata.
 * <p>
 * This is useful for API endpoints that need to return both the configuration
 * content
 * and information about how it was secured.
 *
 * @param data               The decrypted configuration data as a
 *                           {@link JsonNode}.
 * @param encryptionMetadata The {@link EncryptionMetadata} associated with the
 *                           configuration. Can be null if not encrypted.
 * @param encrypted   A flag indicating if the original configuration was
 *                           stored
 *                           as a single encrypted block (as opposed to
 *                           field-level encryption).
 */
public record ConfigurationDataWithMetadata(
        JsonNode data,
        EncryptionMetadata encryptionMetadata,
        boolean encrypted,
        Instant updatedAt) {

    public JsonNode getData() {
        return data == null ? null : data.deepCopy();
    }

    public EncryptionMetadata getEncryptionMetadata() {
        return encryptionMetadata == null ? null
                : new EncryptionMetadata(
                        encryptionMetadata.algorithm(),
                        encryptionMetadata.keyType());
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}