package com.aidentx.configuration.dto;

import com.aidentx.configuration.config.AppConstants;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for configuration requests with simplified structure.
 * <p>
 * Used to submit configuration data (JSON or raw string/binary) along with the configuration type
 * and encryption preference. This class is typically used in API endpoints that accept configuration
 * uploads or updates.
 * </p>
 *
 * <ul>
 *   <li><b>data</b>: The configuration data to store (required).</li>
 *   <li><b>configType</b>: The type/category of the configuration (required).</li>
 *   <li><b>encryptEntireConfig</b>: If true, encrypts the entire configuration; if false, only sensitive fields are encrypted.</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 *   new ConfigurationRequest(Map.of("field", "value"), "sso", true);
 * </pre>
 *
 * @author aidentx Development Team
 */
public class ConfigurationRequest {

    /**
     * The configuration data to store (JSON object, string, or binary).
     */
    @NotNull(message = AppConstants.CONFIG_DATA_REQUIRED_MSG)
    private Object data;

    /**
     * The type/category of the configuration (e.g., "sso", "ldap").
     */
    @NotNull(message = AppConstants.CONFIG_TYPE_REQUIRED_MSG)
    private String configType;

    /**
     * Whether to encrypt the entire configuration or just sensitive fields.
     * When true, the entire JSON is encrypted using the existing encryption service.
     * When false (default), only sensitive fields are encrypted using the $crypto format.
     */
    private boolean encryptEntireConfig = false;

    /**
     * Default constructor for deserialization and manual instantiation.
     */
    public ConfigurationRequest() {
    }

    /**
     * Constructs a configuration request with data and type.
     *
     * @param data       the configuration data
     * @param configType the type/category of the configuration
     */
    public ConfigurationRequest(Object data, String configType) {
        this.data = data;
        this.configType = configType;
    }

    /**
     * Constructs a configuration request with data, type, and encryption flag.
     *
     * @param data                the configuration data
     * @param configType          the type/category of the configuration
     * @param encryptEntireConfig whether to encrypt the entire configuration
     */
    public ConfigurationRequest(Object data, String configType, boolean encryptEntireConfig) {
        this.data = data;
        this.configType = configType;
        this.encryptEntireConfig = encryptEntireConfig;
    }

    /**
     * @return the configuration data
     */
    public Object getData() {
        return data;
    }

    /**
     * Sets the configuration data.
     *
     * @param data the new configuration data
     */
    public void setData(Object data) {
        this.data = data;
    }

    /**
     * @return the configuration type
     */
    public String getConfigType() {
        return configType;
    }

    /**
     * Sets the configuration type.
     *
     * @param configType the new configuration type
     */
    public void setConfigType(String configType) {
        this.configType = configType;
    }

    /**
     * @return true if the entire configuration should be encrypted
     */
    public boolean isEncryptEntireConfig() {
        return encryptEntireConfig;
    }

    /**
     * Sets whether to encrypt the entire configuration.
     *
     * @param encryptEntireConfig true to encrypt the entire config, false for field-level encryption
     */
    public void setEncryptEntireConfig(boolean encryptEntireConfig) {
        this.encryptEntireConfig = encryptEntireConfig;
    }

    /**
     * Returns a string representation of the configuration request for debugging.
     *
     * @return a string describing the configuration request
     */
    @Override
    public String toString() {
        return "ConfigurationRequest{" +
                "configType='" + configType + '\'' +
                ", dataType=" + (data != null ? data.getClass().getSimpleName() : "null") +
                ", encryptEntireConfig=" + encryptEntireConfig +
                '}';
    }
}
