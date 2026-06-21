package com.aidentx.configuration.config;

/**
 * Enumeration of supported configuration types in the system.
 * <p>
 * Each type represents a distinct category of configuration settings managed by
 * the application.
 * This is typically used for identifying and retrieving configuration entries
 * from a central configuration store.
 */
public enum ConfigurationType {

    /**
     * UI customization settings (e.g., themes, branding, layout preferences).
     */
    UI_CUSTOMIZATION("ui_customization"),

    /**
     * Single Sign-On (SSO) configuration settings (e.g., SAML, OAuth2).
     */
    SSO("sso"),

    /**
     * Security-related configuration settings (e.g., password policy, encryption,
     * session timeout).
     */
    SECURITY("security"),

    /**
     * Email and notification configuration settings (e.g., SMTP server, templates).
     */
    NOTIFICATION("notification"),

    /**
     * Audit and logging configuration settings (e.g., log levels, audit retention).
     */
    AUDIT("audit"),

    /**
     * API rate limiting and throttling settings.
     */
    API_LIMITS("api_limits"),

    /**
     * Database configuration settings (e.g., connection pool, tuning, performance).
     */
    DATABASE("database"),

    /**
     * Feature flag and experimental feature toggles.
     */
    FEATURE_FLAGS("feature_flags"),

    /**
     * System maintenance and operational settings (e.g., system mode, uptime).
     */
    SYSTEM("system"),

    RESOURCE("resource"),

    IDENTITY_MAPPING_POLICY("IDENTITY_MAPPING_POLICY"),

    TENANT_POLICY("TENANT_POLICY"),

    /**
     * The configuration type for LLM service.
     * 
     */
    AI("ai");
    /**
     * The string representation of the configuration type.
     */
    private final String value;

    /**
     * Constructs a {@link ConfigurationType} enum with its associated string value.
     *
     * @param value the string value of the configuration type
     */
    ConfigurationType(String value) {
        this.value = value;
    }

    /**
     * Resolves the corresponding {@link ConfigurationType} from its string value.
     *
     * @param value the string value to convert
     * @return the matching {@link ConfigurationType}
     * @throws IllegalArgumentException if the value does not match any known type
     */
    public static ConfigurationType fromValue(String value) {
        for (ConfigurationType type : ConfigurationType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown configuration type: " + value);
    }

    /**
     * Returns the string value associated with this configuration type.
     *
     * @return the string representation
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the string representation of the configuration type.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return value;
    }
}
