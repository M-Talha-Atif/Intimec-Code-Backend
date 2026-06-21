package com.aidentx.configuration.exception.configuration;

/**
 * Exception thrown when encryption or decryption operations fail.
 * <p>
 * This exception is thrown when there are issues with encrypting configuration data
 * during save operations or decrypting data during retrieval operations.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class ConfigurationEncryptionException extends ConfigurationServiceException {

    private final String configType;
    private final String configKey;
    private final String operation;

    /**
     * Constructs a new configuration encryption exception.
     *
     * @param configType the configuration type involved in the encryption operation
     * @param configKey  the configuration key involved in the encryption operation
     * @param operation  the operation that failed ("encrypt" or "decrypt")
     * @param cause      the underlying cause of the encryption failure
     */
    public ConfigurationEncryptionException(String configType, String configKey, String operation, Throwable cause) {
        super(String.format("Configuration %s failed for %s/%s", operation, configType, configKey), 
              cause, "CONFIG_ENCRYPTION_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.operation = operation;
    }

    /**
     * Constructs a new configuration encryption exception with a custom message.
     *
     * @param message    the detail message
     * @param configType the configuration type involved in the encryption operation
     * @param configKey  the configuration key involved in the encryption operation
     * @param operation  the operation that failed ("encrypt" or "decrypt")
     * @param cause      the underlying cause of the encryption failure
     */
    public ConfigurationEncryptionException(String message, String configType, String configKey, String operation, Throwable cause) {
        super(message, cause, "CONFIG_ENCRYPTION_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.operation = operation;
    }

    /**
     * Gets the configuration type involved in the encryption operation.
     *
     * @return the configuration type
     */
    public String getConfigType() {
        return configType;
    }

    /**
     * Gets the configuration key involved in the encryption operation.
     *
     * @return the configuration key
     */
    public String getConfigKey() {
        return configKey;
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
