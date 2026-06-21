package com.aidentx.configuration.exception.artifact;

/**
 * Exception thrown when a requested artifact is not found.
 * <p>
 * This exception is thrown when attempting to retrieve, update, or delete
 * an artifact that does not exist in the system.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class ArtifactNotFoundException extends ArtifactServiceException {

    private final String configType;
    private final String configKey;
    private final String artifactType;

    /**
     * Constructs a new artifact not found exception.
     *
     * @param configType   the configuration type
     * @param configKey    the configuration key
     * @param artifactType the artifact type that was not found
     */
    public ArtifactNotFoundException(String configType, String configKey, String artifactType) {
        super(String.format("Artifact not found: %s for configuration %s/%s", artifactType, configType, configKey), 
              "ARTIFACT_NOT_FOUND");
        this.configType = configType;
        this.configKey = configKey;
        this.artifactType = artifactType;
    }

    /**
     * Constructs a new artifact not found exception with a custom message.
     *
     * @param message      the detail message
     * @param configType   the configuration type
     * @param configKey    the configuration key
     * @param artifactType the artifact type that was not found
     */
    public ArtifactNotFoundException(String message, String configType, String configKey, String artifactType) {
        super(message, "ARTIFACT_NOT_FOUND");
        this.configType = configType;
        this.configKey = configKey;
        this.artifactType = artifactType;
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
     * Gets the artifact type that was not found.
     *
     * @return the artifact type
     */
    public String getArtifactType() {
        return artifactType;
    }
}
