package com.aidentx.configuration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for partial configuration updates via PATCH requests.
 * <p>
 * Allows updating specific fields of a configuration without replacing the entire configuration object.
 * This is useful for efficient, targeted updates (such as toggling the active status or updating a subset of fields)
 * in configuration management APIs.
 * </p>
 *
 * <ul>
 *   <li><b>data</b>: Partial data to update (can be a JSON object or value).</li>
 *   <li><b>isActive</b>: Controls whether the configuration is active (true) or inactive (false).</li>
 *   <li><b>encryptEntireConfig</b>: If true, encrypts the entire configuration; if false, only sensitive fields are encrypted.</li>
 * </ul>
 */
public class ConfigurationPatchRequest {

    /**
     * Partial data to update in the configuration.
     * Can be a JSON object with fields to merge, or a value to replace specific fields.
     */
    private Object data;

    /**
     * Controls the active status of the configuration.
     * When true, the configuration is active and available for use.
     * When false, the configuration is inactive and should be ignored by the system.
     */
    @JsonProperty("isActive")
    private Boolean isActive;

    /**
     * Whether to encrypt the entire configuration or just sensitive fields.
     */
    private boolean encryptEntireConfig = false;

    /**
     * Optional lifecycle or processing status for the configuration. This is kept
     * separate from the generic data object so callers (like onboarding sagas)
     * can send a simple top-level field {"status":"COMPLETED"} without having
     * to nest it inside a data payload. Service logic will merge it back into
     * the config JSON document.
     */
    @JsonProperty("status")
    private String status;

    /**
     * Default constructor for deserialization and manual instantiation.
     */
    public ConfigurationPatchRequest() {
    }

    /**
     * Constructs a patch request with data and active status.
     *
     * @param data     the partial data to update
     * @param isActive the new active status
     */
    public ConfigurationPatchRequest(Object data, Boolean isActive) {
        this.data = data;
        this.isActive = isActive;
    }

    /**
     * Convenience constructor including status.
     */
    public ConfigurationPatchRequest(Object data, Boolean isActive, String status) {
        this.data = data;
        this.isActive = isActive;
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isEncryptEntireConfig() {
        return encryptEntireConfig;
    }

    public void setEncryptEntireConfig(boolean encryptEntireConfig) {
        this.encryptEntireConfig = encryptEntireConfig;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ConfigurationPatchRequest{" +
                "data=" + (data != null ? data.getClass().getSimpleName() : "null") +
                ", isActive=" + isActive +
                ", status=" + status +
                ", encryptEntireConfig=" + encryptEntireConfig +
                '}';
    }
}