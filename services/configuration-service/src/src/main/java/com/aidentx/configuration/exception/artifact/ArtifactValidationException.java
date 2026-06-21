package com.aidentx.configuration.exception.artifact;

/**
 * Exception thrown when artifact validation fails.
 * <p>
 * This exception is thrown when attempting to save or update an artifact
 * with invalid data, structure, or when it fails business rule validation.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class ArtifactValidationException extends ArtifactServiceException {

    private final String configType;
    private final String configKey;
    private final String artifactType;
    private final String validationError;

    /**
     * Constructs a new artifact validation exception.
     *
     * @param configType      the configuration type
     * @param configKey       the configuration key
     * @param artifactType    the artifact type being validated
     * @param validationError the specific validation error
     */
    public ArtifactValidationException(String configType, String configKey, String artifactType, String validationError) {
        super(String.format("Artifact validation failed for %s artifact in configuration %s/%s: %s", 
              artifactType, configType, configKey, validationError), 
              "ARTIFACT_VALIDATION_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.artifactType = artifactType;
        this.validationError = validationError;
    }

    /**
     * Constructs a new artifact validation exception with a cause.
     *
     * @param configType      the configuration type
     * @param configKey       the configuration key
     * @param artifactType    the artifact type being validated
     * @param validationError the specific validation error
     * @param cause           the underlying cause of the validation failure
     */
    public ArtifactValidationException(String configType, String configKey, String artifactType, 
                                      String validationError, Throwable cause) {
        super(String.format("Artifact validation failed for %s artifact in configuration %s/%s: %s", 
              artifactType, configType, configKey, validationError), 
              cause, "ARTIFACT_VALIDATION_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.artifactType = artifactType;
        this.validationError = validationError;
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
     * Gets the artifact type being validated.
     *
     * @return the artifact type
     */
    public String getArtifactType() {
        return artifactType;
    }

    /**
     * Gets the specific validation error.
     *
     * @return the validation error
     */
    public String getValidationError() {
        return validationError;
    }
}
