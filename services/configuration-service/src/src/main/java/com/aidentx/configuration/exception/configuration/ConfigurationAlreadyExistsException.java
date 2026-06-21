package com.aidentx.configuration.exception.configuration;

/**
 * Exception thrown when attempting to create a configuration that already exists.
 * <p>
 * This exception is thrown when trying to create a new configuration with
 * a type and key combination that already exists in the system.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class ConfigurationAlreadyExistsException extends ConfigurationServiceException {

    private final String configType;
    private final String configKey;

    /**
     * Constructs a new configuration already exists exception.
     *
     * @param configType the configuration type that already exists
     * @param configKey  the configuration key that already exists
     */
    public ConfigurationAlreadyExistsException(String configType, String configKey) {
        super(String.format("Configuration already exists: %s/%s", configType, configKey), "CONFIG_ALREADY_EXISTS");
        this.configType = configType;
        this.configKey = configKey;
    }

    /**
     * Constructs a new configuration already exists exception with a custom message.
     *
     * @param message    the detail message
     * @param configType the configuration type that already exists
     * @param configKey  the configuration key that already exists
     */
    public ConfigurationAlreadyExistsException(String message, String configType, String configKey) {
        super(message, "CONFIG_ALREADY_EXISTS");
        this.configType = configType;
        this.configKey = configKey;
    }

    /**
     * Gets the configuration type that already exists.
     *
     * @return the configuration type
     */
    public String getConfigType() {
        return configType;
    }

    /**
     * Gets the configuration key that already exists.
     *
     * @return the configuration key
     */
    public String getConfigKey() {
        return configKey;
    }
}
