package com.aidentx.configuration.dto;

/**
 * A Data Transfer Object (DTO) that represents the encryption metadata for a
 * configuration.
 * <p>
 * This record is immutable and provides a concise way to transfer information
 * about the encryption status and parameters.
 * </p>
 *
 * <ul>
 * <li><b>algorithm</b>: The encryption algorithm used (e.g.,
 * "AES-256-GCM").</li>
 * <li><b>keyType</b>: The type or source of the encryption key (e.g.,
 * "application", "tenant").</li>
 * <li><b>isEncryptionEnabled</b>: True if encryption is enabled for this
 * configuration, false otherwise.</li>
 * </ul>
 *
 * <b>Usage Example:</b>
 * 
 * <pre>
 * EncryptionMetadata metadata = new EncryptionMetadata("AES-256-GCM", "application", true);
 * </pre>
 *
 * <b>Design Notes:</b>
 * <ul>
 * <li>No hardcoded messages are present in this DTO; all status or error
 * messages should be handled at the controller/service layer using
 * constants.</li>
 * <li>All fields are immutable and exposed as-is for clarity and
 * serialization.</li>
 * </ul>
 */
public record EncryptionMetadata(
        String algorithm,
        String keyType) {

    /**
     * Returns the encryption algorithm used, or null if not set.
     *
     * @return the encryption algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the key type or source, or null if not set.
     *
     * @return the key type
     */
    public String getKeyType() {
        return keyType;
    }

}