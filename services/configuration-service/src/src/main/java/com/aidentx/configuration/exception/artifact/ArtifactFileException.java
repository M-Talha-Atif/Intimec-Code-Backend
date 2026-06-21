package com.aidentx.configuration.exception.artifact;

/**
 * Exception thrown when file operations related to artifacts fail.
 * <p>
 * This exception is thrown when there are issues with reading, writing,
 * or deleting physical artifact files on the file system.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class ArtifactFileException extends ArtifactServiceException {

    private final String configType;
    private final String configKey;
    private final String artifactType;
    private final String filePath;
    private final String operation;

    /**
     * Constructs a new artifact file exception.
     *
     * @param configType   the configuration type
     * @param configKey    the configuration key
     * @param artifactType the artifact type
     * @param filePath     the file path that caused the error
     * @param operation    the file operation that failed
     * @param cause        the underlying cause of the file operation failure
     */
    public ArtifactFileException(String configType, String configKey, String artifactType, 
                                String filePath, String operation, Throwable cause) {
        super(String.format("Artifact file %s failed for %s/%s artifact %s at path: %s", 
              operation, configType, configKey, artifactType, filePath), 
              cause, "ARTIFACT_FILE_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.artifactType = artifactType;
        this.filePath = filePath;
        this.operation = operation;
    }

    /**
     * Constructs a new artifact file exception with a custom message.
     *
     * @param message      the detail message
     * @param configType   the configuration type
     * @param configKey    the configuration key
     * @param artifactType the artifact type
     * @param filePath     the file path that caused the error
     * @param operation    the file operation that failed
     * @param cause        the underlying cause of the file operation failure
     */
    public ArtifactFileException(String message, String configType, String configKey, String artifactType,
                                String filePath, String operation, Throwable cause) {
        super(message, cause, "ARTIFACT_FILE_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.artifactType = artifactType;
        this.filePath = filePath;
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
     * Gets the artifact type.
     *
     * @return the artifact type
     */
    public String getArtifactType() {
        return artifactType;
    }

    /**
     * Gets the file path that caused the error.
     *
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Gets the file operation that failed.
     *
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }
}
