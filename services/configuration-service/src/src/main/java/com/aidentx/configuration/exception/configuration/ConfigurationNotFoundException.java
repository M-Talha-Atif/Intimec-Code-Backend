package com.aidentx.configuration.exception.configuration;

/**
 * Exception thrown when a requested configuration is not found.
 * <p>
 * This exception is thrown when attempting to retrieve, update, or delete
 * a configuration that does not exist in the system.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class ConfigurationNotFoundException extends ConfigurationServiceException {

    private final String configType;
    private final String configKey;

    /**
     * Constructs a new configuration not found exception.
     *
     * @param configType the configuration type that was not found
     * @param configKey  the configuration key that was not found
     */
    public ConfigurationNotFoundException(String configType, String configKey) {
        super(String.format("Configuration not found: %s/%s", configType, configKey), "CONFIG_NOT_FOUND");
        this.configType = configType;
        this.configKey = configKey;
    }

    /**
     * Constructs a new configuration not found exception with a custom message.
     *
     * @param message    the detail message
     * @param configType the configuration type that was not found
     * @param configKey  the configuration key that was not found
     */
    public ConfigurationNotFoundException(String message, String configType, String configKey) {
        super(message, "CONFIG_NOT_FOUND");
        this.configType = configType;
        this.configKey = configKey;
    }

    /**
     * Gets the configuration type that was not found.
     *
     * @return the configuration type
     */
    public String getConfigType() {
        return configType;
    }

    /**
     * Gets the configuration key that was not found.
     *
     * @return the configuration key
     */
    public String getConfigKey() {
        return configKey;
    }
}
