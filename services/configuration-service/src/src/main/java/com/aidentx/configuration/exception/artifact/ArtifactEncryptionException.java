package com.aidentx.configuration.exception.artifact;

/**
 * Exception thrown when artifact encryption or decryption operations fail.
 * <p>
 * This exception is thrown when there are issues with encrypting artifact data
 * during save operations or decrypting data during retrieval operations.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class ArtifactEncryptionException extends ArtifactServiceException {

    private final String configType;
    private final String configKey;
    private final String artifactType;
    private final String operation;

    /**
     * Constructs a new artifact encryption exception.
     *
     * @param configType   the configuration type
     * @param configKey    the configuration key
     * @param artifactType the artifact type involved in the encryption operation
     * @param operation    the operation that failed ("encrypt" or "decrypt")
     * @param cause        the underlying cause of the encryption failure
     */
    public ArtifactEncryptionException(String configType, String configKey, String artifactType, 
                                      String operation, Throwable cause) {
        super(String.format("Artifact %s failed for %s artifact in configuration %s/%s", 
              operation, artifactType, configType, configKey), 
              cause, "ARTIFACT_ENCRYPTION_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.artifactType = artifactType;
        this.operation = operation;
    }

    /**
     * Constructs a new artifact encryption exception with a custom message.
     *
     * @param message      the detail message
     * @param configType   the configuration type
     * @param configKey    the configuration key
     * @param artifactType the artifact type involved in the encryption operation
     * @param operation    the operation that failed ("encrypt" or "decrypt")
     * @param cause        the underlying cause of the encryption failure
     */
    public ArtifactEncryptionException(String message, String configType, String configKey, String artifactType,
                                      String operation, Throwable cause) {
        super(message, cause, "ARTIFACT_ENCRYPTION_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.artifactType = artifactType;
        this.operation = operation;
    }

    /**
     * Gets the configuration type.
     *
     * @return the configuration type
     */
    public String getConfigType() {
        return configType;
    }

    /**
     * Gets the configuration key.
     *
     * @return the configuration key
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Gets the artifact type involved in the encryption operation.
     *
     * @return the artifact type
     */
    public String getArtifactType() {
        return artifactType;
    }

    /**
     * Gets the operation that failed.
     *
     * @return the operation ("encrypt" or "decrypt")
     */
    public String getOperation() {
        return operation;
    }
}
