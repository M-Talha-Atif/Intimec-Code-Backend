package com.aidentx.configuration.config;

/**
 * Constants defining standard configuration keys for each configuration type.
 * <p>
 * These keys provide a structured approach to configuration management and are
 * used
 * throughout the application to reference specific configuration settings in a
 * type-safe way.
 * <p>
 * Nested static classes group keys by configuration domain (e.g., SSO,
 * SECURITY, NOTIFICATION, AUDIT).
 *
 * <p>
 * Example usage:
 * 
 * <pre>
 * String key = ConfigurationKeys.SSO.LDAP_CONFIG;
 * String securityKey = ConfigurationKeys.SECURITY.PASSWORD_POLICY;
 * </pre>
 */
public final class ConfigurationKeys {
    /**
     * SSO (Single Sign-On) configuration keys.
     * These keys are used for SSO provider and related settings.
     */
    public static final class SSO {
        public static final String SAML_CONFIG = "saml_config";
        public static final String OAUTH2_CONFIG = "oauth2_config";
        public static final String AZURE_AD_CONFIG = "azure_ad_config";
        public static final String LDAP_CONFIG = "ldap_config";
        public static final String DEFAULT_PROVIDER = "default_provider";
        public static final String ENABLED_PROVIDERS = "enabled_providers";
        public static final String AUTO_PROVISIONING = "auto_provisioning";
        public static final String USER_MAPPING = "user_mapping";
        public static final String GROUP_MAPPING = "group_mapping";

        // LDAP/Active Directory specific settings (under SSO)
        public static final String LDAP_CONNECTION_SETTINGS = "ldap_connection_settings";
        public static final String LDAP_USER_SEARCH_BASE = "ldap_user_search_base";
        public static final String LDAP_GROUP_SEARCH_BASE = "ldap_group_search_base";
        public static final String LDAP_USER_FILTER = "ldap_user_filter";
        public static final String LDAP_GROUP_FILTER = "ldap_group_filter";
        public static final String LDAP_ATTRIBUTE_MAPPING = "ldap_attribute_mapping";
        public static final String LDAP_BIND_CREDENTIALS = "ldap_bind_credentials";
        public static final String LDAP_SSL_SETTINGS = "ldap_ssl_settings";
        public static final String LDAP_SYNC_SCHEDULE = "ldap_sync_schedule";
    }

    /**
     * Security configuration keys.
     * These keys are used for security-related settings such as password policy,
     * session, and encryption.
     */
    public static final class SECURITY {
        public static final String PASSWORD_POLICY = "password_policy";
        public static final String SESSION_TIMEOUT = "session_timeout";
        public static final String MAX_LOGIN_ATTEMPTS = "max_login_attempts";
        public static final String LOCKOUT_DURATION = "lockout_duration";
        public static final String MFA_SETTINGS = "mfa_settings";
        public static final String JWT_SETTINGS = "jwt_settings";
        public static final String CORS_SETTINGS = "cors_settings";
        public static final String CSRF_PROTECTION = "csrf_protection";
        public static final String ENCRYPTION_ALGORITHM = "encryption_algorithm";
        public static final String ALLOWED_ORIGINS = "allowed_origins";
    }

    /**
     * Notification configuration keys.
     * These keys are used for notification and messaging settings.
     */
    public static final class NOTIFICATION {
        public static final String EMAIL_SMTP = "email_smtp";
        public static final String EMAIL_TEMPLATES = "email_templates";
        public static final String WELCOME_EMAIL = "welcome_email";
        public static final String PASSWORD_RESET_EMAIL = "password_reset_email";
        public static final String ACCOUNT_LOCKED_EMAIL = "account_locked_email";
        public static final String NOTIFICATION_PREFERENCES = "notification_preferences";
        public static final String SMS_PROVIDER = "sms_provider";
        public static final String WEBHOOK_ENDPOINTS = "webhook_endpoints";
    }

    /**
     * Audit configuration keys.
     * These keys are used for audit logging and retention policy settings.
     */
    public static final class AUDIT {
        public static final String AUDIT_LEVEL = "audit_level";
        public static final String RETENTION_POLICY = "retention_policy";
    }

    /**
     * API Limits configuration keys
     */
    public static final class API_LIMITS {
        public static final String RATE_LIMIT_GLOBAL = "rate_limit_global";
        public static final String RATE_LIMIT_USER = "rate_limit_user";
        public static final String RATE_LIMIT_IP = "rate_limit_ip";
        public static final String REQUEST_SIZE_LIMIT = "request_size_limit";
        public static final String TIMEOUT_SETTINGS = "timeout_settings";
        public static final String THROTTLING_RULES = "throttling_rules";
    }

    /**
     * Database configuration keys
     */
    public static final class DATABASE {
        public static final String CONNECTION_POOL = "connection_pool";
        public static final String PERFORMANCE_TUNING = "performance_tuning";
        public static final String BACKUP_SETTINGS = "backup_settings";
        public static final String MONITORING = "monitoring";
    }

    /**
     * Feature Flags configuration keys
     */
    public static final class FEATURE_FLAGS {
        public static final String NEW_USER_REGISTRATION = "new_user_registration";
        public static final String SOCIAL_LOGIN = "social_login";
        public static final String ADVANCED_AUDIT = "advanced_audit";
        public static final String API_V2 = "api_v2";
        public static final String BETA_FEATURES = "beta_features";
        public static final String MAINTENANCE_MODE = "maintenance_mode";
        public static final String DEBUG_MODE = "debug_mode";
    }

    /**
     * System configuration keys
     */
    public static final class SYSTEM {
        public static final String TIMEZONE = "timezone";
        public static final String LOCALE = "locale";
        public static final String DATE_FORMAT = "date_format";
        public static final String TIME_FORMAT = "time_format";
        public static final String CURRENCY = "currency";
        public static final String HEALTH_CHECK = "health_check";
        public static final String MONITORING = "monitoring";
        public static final String BACKUP_SCHEDULE = "backup_schedule";
        public static final String UPDATE_SETTINGS = "update_settings";
    }

    /**
     * UI Customization configuration keys.
     * These keys are used for theming, branding, and UI layout preferences.
     */
    public static final class UI {
        public static final String THEME = "theme";
        public static final String COMPANY_LOGO = "company_logo";
        public static final String COMPANY_NAME = "company_name";
        public static final String LOGIN_BACKGROUND = "login_background";
        public static final String PRIMARY_COLOR = "primary_color";
        public static final String SECONDARY_COLOR = "secondary_color";
        public static final String CUSTOM_CSS = "custom_css";
        public static final String FAVICON = "favicon";
        public static final String LOGIN_MESSAGE = "login_message";
        public static final String FOOTER_TEXT = "footer_text";
    }

    /**
     * Resource configuration keys.
     * These keys are used for managing application resources, such as file uploads
     * and storage.
     */
    public static final class RESOURCE {
        public static final String APPLICATION = "application";

    }

    private ConfigurationKeys() {
        // Utility class - prevent instantiation
    }
}
