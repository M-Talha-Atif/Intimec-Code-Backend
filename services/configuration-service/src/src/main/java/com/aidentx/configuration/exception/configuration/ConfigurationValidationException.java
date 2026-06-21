package com.aidentx.configuration.exception.configuration;

/**
 * Exception thrown when configuration validation fails.
 * <p>
 * This exception is thrown when attempting to save or update a configuration
 * with invalid data, structure, or when it fails business rule validation.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class ConfigurationValidationException extends ConfigurationServiceException {

    private final String configType;
    private final String configKey;
    private final String validationError;

    /**
     * Constructs a new configuration validation exception.
     *
     * @param configType      the configuration type being validated
     * @param configKey       the configuration key being validated
     * @param validationError the specific validation error
     */
    public ConfigurationValidationException(String configType, String configKey, String validationError) {
        super(String.format("Configuration validation failed for %s/%s: %s", configType, configKey, validationError), 
              "CONFIG_VALIDATION_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.validationError = validationError;
    }

    /**
     * Constructs a new configuration validation exception with a custom message.
     *
     * @param message         the detail message
     * @param configType      the configuration type being validated
     * @param configKey       the configuration key being validated
     * @param validationError the specific validation error
     */
    public ConfigurationValidationException(String message, String configType, String configKey, String validationError) {
        super(message, "CONFIG_VALIDATION_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.validationError = validationError;
    }

    /**
     * Constructs a new configuration validation exception with a cause.
     *
     * @param configType      the configuration type being validated
     * @param configKey       the configuration key being validated
     * @param validationError the specific validation error
     * @param cause           the cause of the validation failure
     */
    public ConfigurationValidationException(String configType, String configKey, String validationError, Throwable cause) {
        super(String.format("Configuration validation failed for %s/%s: %s", configType, configKey, validationError), 
              cause, "CONFIG_VALIDATION_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.validationError = validationError;
    }

    /**
     * Gets the configuration type being validated.
     *
     * @return the configuration type
     */
    public String getConfigType() {
        return configType;
    }

    /**
     * Gets the configuration key being validated.
     *
     * @return the configuration key
     */
    public String getConfigKey() {
        return configKey;
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
