package com.aidentx.configuration.exception.configuration;

import com.aidentx.configuration.config.ConfigurationType;
import com.aidentx.configuration.exception.artifact.ArtifactEncryptionException;
import com.aidentx.configuration.exception.artifact.ArtifactFileException;
import com.aidentx.configuration.exception.artifact.ArtifactNotFoundException;
import com.aidentx.configuration.exception.artifact.ArtifactValidationException;

/**
 * Utility class for creating and throwing configuration service exceptions.
 * <p>
 * This class provides convenient factory methods for creating common
 * configuration service exceptions with standardized messages and error codes.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public final class ConfigurationExceptionUtils {

    private ConfigurationExceptionUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Creates a configuration not found exception.
     *
     * @param configType the configuration type
     * @param configKey  the configuration key
     * @return a new ConfigurationNotFoundException
     */
    public static ConfigurationNotFoundException configurationNotFound(ConfigurationType configType, String configKey) {
        return new ConfigurationNotFoundException(configType.name(), configKey);
    }

    /**
     * Creates a configuration not found exception.
     *
     * @param configType the configuration type as string
     * @param configKey  the configuration key
     * @return a new ConfigurationNotFoundException
     */
    public static ConfigurationNotFoundException configurationNotFound(String configType, String configKey) {
        return new ConfigurationNotFoundException(configType, configKey);
    }

    /**
     * Creates a configuration already exists exception.
     *
     * @param configType the configuration type
     * @param configKey  the configuration key
     * @return a new AlreadyExistsException
     */
    public static ConfigurationAlreadyExistsException configurationAlreadyExists(ConfigurationType configType, String configKey) {
        return new ConfigurationAlreadyExistsException(configType.name(), configKey);
    }

    /**
     * Creates a configuration already exists exception.
     *
     * @param configType the configuration type as string
     * @param configKey  the configuration key
     * @return a new AlreadyExistsException
     */
    public static ConfigurationAlreadyExistsException configurationAlreadyExists(String configType, String configKey) {
        return new ConfigurationAlreadyExistsException(configType, configKey);
    }

    /**
     * Creates a configuration validation exception.
     *
     * @param configType      the configuration type
     * @param configKey       the configuration key
     * @param validationError the validation error message
     * @return a new ConfigurationValidationException
     */
    public static ConfigurationValidationException configurationValidationFailed(ConfigurationType configType,
            String configKey, String validationError) {
        return new ConfigurationValidationException(configType.name(), configKey, validationError);
    }

    /**
     * Creates a configuration validation exception.
     *
     * @param configType      the configuration type as string
     * @param configKey       the configuration key
     * @param validationError the validation error message
     * @return a new ConfigurationValidationException
     */
    public static ConfigurationValidationException configurationValidationFailed(String configType, String configKey,
            String validationError) {
        return new ConfigurationValidationException(configType, configKey, validationError);
    }

    /**
     * Creates a configuration validation exception with cause.
     *
     * @param configType      the configuration type
     * @param configKey       the configuration key
     * @param validationError the validation error message
     * @param cause           the underlying cause
     * @return a new ConfigurationValidationException
     */
    public static ConfigurationValidationException configurationValidationFailed(ConfigurationType configType,
            String configKey, String validationError, Throwable cause) {
        return new ConfigurationValidationException(configType.name(), configKey, validationError, cause);
    }

    /**
     * Creates a configuration patch exception.
     *
     * @param configType the configuration type
     * @param configKey  the configuration key
     * @param patchError the patch error message
     * @return a new ConfigurationPatchException
     */
    public static ConfigurationPatchException configurationPatchFailed(ConfigurationType configType, String configKey,
            String patchError) {
        return new ConfigurationPatchException(configType.name(), configKey, patchError);
    }

    /**
     * Creates a configuration patch exception.
     *
     * @param configType the configuration type as string
     * @param configKey  the configuration key
     * @param patchError the patch error message
     * @return a new ConfigurationPatchException
     */
    public static ConfigurationPatchException configurationPatchFailed(String configType, String configKey,
            String patchError) {
        return new ConfigurationPatchException(configType, configKey, patchError);
    }

    /**
     * Creates a configuration encryption exception.
     *
     * @param configType the configuration type
     * @param configKey  the configuration key
     * @param operation  the operation that failed
     * @param cause      the underlying cause
     * @return a new EncryptionException
     */
    public static ConfigurationEncryptionException configurationEncryptionFailed(ConfigurationType configType,
            String configKey, String operation, Throwable cause) {
        return new ConfigurationEncryptionException(configType.name(), configKey, operation, cause);
    }

    /**
     * Creates a configuration encryption exception.
     *
     * @param configType the configuration type as string
     * @param configKey  the configuration key
     * @param operation  the operation that failed
     * @param cause      the underlying cause
     * @return a new EncryptionException
     */
    public static ConfigurationEncryptionException configurationEncryptionFailed(String configType, String configKey,
            String operation, Throwable cause) {
        return new ConfigurationEncryptionException(configType, configKey, operation, cause);
    }

    /**
     * Creates an artifact not found exception.
     *
     * @param configType   the configuration type
     * @param configKey    the configuration key
     * @param artifactType the artifact type
     * @return a new ArtifactNotFoundException
     */
    public static ArtifactNotFoundException artifactNotFound(ConfigurationType configType, String configKey,
            String artifactType) {
        return new ArtifactNotFoundException(configType.name(), configKey, artifactType);
    }

    /**
     * Creates an artifact not found exception.
     *
     * @param configType   the configuration type as string
     * @param configKey    the configuration key
     * @param artifactType the artifact type
     * @return a new ArtifactNotFoundException
     */
    public static ArtifactNotFoundException artifactNotFound(String configType, String configKey, String artifactType) {
        return new ArtifactNotFoundException(configType, configKey, artifactType);
    }

    /**
     * Creates an artifact validation exception.
     *
     * @param configType      the configuration type
     * @param configKey       the configuration key
     * @param artifactType    the artifact type
     * @param validationError the validation error message
     * @return a new ArtifactValidationException
     */
    public static ArtifactValidationException artifactValidationFailed(ConfigurationType configType, String configKey,
            String artifactType, String validationError) {
        return new ArtifactValidationException(configType.name(), configKey, artifactType, validationError);
    }

    /**
     * Creates an artifact validation exception.
     *
     * @param configType      the configuration type as string
     * @param configKey       the configuration key
     * @param artifactType    the artifact type
     * @param validationError the validation error message
     * @return a new ArtifactValidationException
     */
    public static ArtifactValidationException artifactValidationFailed(String configType, String configKey,
            String artifactType, String validationError) {
        return new ArtifactValidationException(configType, configKey, artifactType, validationError);
    }

    /**
     * Creates an artifact file exception.
     *
     * @param configType   the configuration type
     * @param configKey    the configuration key
     * @param artifactType the artifact type
     * @param filePath     the file path
     * @param operation    the file operation
     * @param cause        the underlying cause
     * @return a new ArtifactFileException
     */
    public static ArtifactFileException artifactFileFailed(ConfigurationType configType, String configKey,
            String artifactType, String filePath, String operation, Throwable cause) {
        return new ArtifactFileException(configType.name(), configKey, artifactType, filePath, operation, cause);
    }

    /**
     * Creates an artifact file exception.
     *
     * @param configType   the configuration type as string
     * @param configKey    the configuration key
     * @param artifactType the artifact type
     * @param filePath     the file path
     * @param operation    the file operation
     * @param cause        the underlying cause
     * @return a new ArtifactFileException
     */
    public static ArtifactFileException artifactFileFailed(String configType, String configKey, String artifactType,
            String filePath, String operation, Throwable cause) {
        return new ArtifactFileException(configType, configKey, artifactType, filePath, operation, cause);
    }

    /**
     * Creates an artifact encryption exception.
     *
     * @param configType   the configuration type
     * @param configKey    the configuration key
     * @param artifactType the artifact type
     * @param operation    the operation that failed
     * @param cause        the underlying cause
     * @return a new ArtifactEncryptionException
     */
    public static ArtifactEncryptionException artifactEncryptionFailed(ConfigurationType configType, String configKey,
            String artifactType, String operation, Throwable cause) {
        return new ArtifactEncryptionException(configType.name(), configKey, artifactType, operation, cause);
    }

    /**
     * Creates an artifact encryption exception.
     *
     * @param configType   the configuration type as string
     * @param configKey    the configuration key
     * @param artifactType the artifact type
     * @param operation    the operation that failed
     * @param cause        the underlying cause
     * @return a new ArtifactEncryptionException
     */
    public static ArtifactEncryptionException artifactEncryptionFailed(String configType, String configKey,
            String artifactType, String operation, Throwable cause) {
        return new ArtifactEncryptionException(configType, configKey, artifactType, operation, cause);
    }
}
