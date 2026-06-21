package com.aidentx.configuration.config;

import java.util.Set;

/**
 * Configuration class defining which configuration types and keys should be automatically encrypted.
 * This centralizes the encryption rules and makes them easily configurable.
 */
public final class EncryptionRulesConfig {

    /**
     * Configuration types that should always be encrypted regardless of the specific key.
     */
    public static final Set<String> ENCRYPTED_CONFIG_TYPES = Set.of(
//        ConfigurationType.SSO.getValue(),
//        ConfigurationType.SECURITY.getValue()
    );
    /**
     * Specific configuration keys that should be encrypted regardless of their configuration type.
     * This allows fine-grained control over encryption for sensitive keys in otherwise non-sensitive types.
     */
    public static final Set<String> ENCRYPTED_CONFIG_KEYS = Set.of(
            // SSO Configuration Keys
    );

    private EncryptionRulesConfig() {
        // Utility class - prevent instantiation
    }

    /**
     * Determines if a configuration should be encrypted based on its type and key.
     *
     * @param configurationType The configuration type
     * @param configurationKey  The configuration key
     * @return true if the configuration should be encrypted, false otherwise
     */
    public static boolean shouldEncrypt(String configurationType, String configurationKey) {
        // Check if configuration type is always encrypted
        if (ENCRYPTED_CONFIG_TYPES.contains(configurationType)) {
            return true;
        }

        // Check if specific key should be encrypted
        return ENCRYPTED_CONFIG_KEYS.contains(configurationKey);
    }

    /**
     * Determines the appropriate key type for encryption based on configuration type.
     * This is used for key derivation to ensure different configuration categories
     * use different encryption keys for security isolation.
     *
     * @param configurationType The configuration type
     * @return The key type string for encryption key derivation
     */
    public static String getKeyType(String configurationType) {
        switch (configurationType.toLowerCase()) {
            case "sso":
                return "sso_config";
            case "security":
                return "security_config";
            case "notification":
                return "notification_config";
            case "database":
                return "database_config";
            case "audit":
                return "audit_config";
            default:
                return "general_config";
        }
    }
}
