package com.aidentx.configuration.exception.configuration;

/**
 * Exception thrown when a patch operation fails due to invalid patch data or incompatible state.
 * <p>
 * This exception is thrown when attempting to apply a patch to a configuration
 * but the patch operation cannot be completed due to invalid patch structure,
 * conflicting data, or incompatible configuration state.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public class ConfigurationPatchException extends ConfigurationServiceException {

    private final String configType;
    private final String configKey;
    private final String patchError;

    /**
     * Constructs a new configuration patch exception.
     *
     * @param configType the configuration type being patched
     * @param configKey  the configuration key being patched
     * @param patchError the specific patch error
     */
    public ConfigurationPatchException(String configType, String configKey, String patchError) {
        super(String.format("Configuration patch failed for %s/%s: %s", configType, configKey, patchError), 
              "CONFIG_PATCH_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.patchError = patchError;
    }

    /**
     * Constructs a new configuration patch exception with a cause.
     *
     * @param configType the configuration type being patched
     * @param configKey  the configuration key being patched
     * @param patchError the specific patch error
     * @param cause      the underlying cause of the patch failure
     */
    public ConfigurationPatchException(String configType, String configKey, String patchError, Throwable cause) {
        super(String.format("Configuration patch failed for %s/%s: %s", configType, configKey, patchError), 
              cause, "CONFIG_PATCH_ERROR");
        this.configType = configType;
        this.configKey = configKey;
        this.patchError = patchError;
    }

    /**
     * Gets the configuration type being patched.
     *
     * @return the configuration type
     */
    public String getConfigType() {
        return configType;
    }

    /**
     * Gets the configuration key being patched.
     *
     * @return the configuration key
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Gets the specific patch error.
     *
     * @return the patch error
     */
    public String getPatchError() {
        return patchError;
    }
}
