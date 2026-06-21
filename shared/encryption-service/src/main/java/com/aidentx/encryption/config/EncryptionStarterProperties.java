package com.aidentx.encryption.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "encryption")
@Validated // Enable validation on these properties
public class EncryptionStarterProperties {

    /**
     * Whether to enable the encryption starter auto-configuration.
     */
    private boolean enabled = true;

    /**
     * A high-entropy secret key used to derive the master encryption key.
     * MUST be at least 32 characters long.
     */
    @NotBlank(message = "Property 'encryption.master-key' must not be blank.")
    @Size(min = 32, message = "Property 'encryption.master-key' must be at least 32 characters long.")
    private String masterKey;

    /**
     * A salt used in the key derivation process. Should be a unique, static value
     * per application.
     */
    @NotBlank(message = "Property 'encryption.salt' must not be blank.")
    private String salt;

    // Getters and Setters...
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}