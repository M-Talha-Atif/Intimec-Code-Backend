package com.aidentx.authservice.enums;

/**
 * Enum representing supported audit log actions.
 * Used to categorize and track different types of system events that require
 * auditing.
 */
public enum AuditAction {
    // Authentication actions
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    PASSWORD_RESET,
    LOCKOUT,
    LOGOUT_SUCCESS,
    PASSWORD_UPDATE_FAILURE,
    PASSWORD_UPDATE_SUCCESS,

    // Encryption and security actions
    ENCRYPTION_ENABLED,
    ENCRYPTION_DISABLED,
    ENCRYPTION_KEY_ROTATION,
    DECRYPTION_OPERATION,
    ENCRYPTION_OPERATION,
    FIELD_ENCRYPTION_APPLIED,
    FULL_ENCRYPTION_APPLIED,

    // Administrative actions
    ADMIN_ACCESS,
    SYSTEM_CONFIGURATION_CHANGE,
    SECURITY_POLICY_CHANGE,

    // Configuration viewing actions
    VIEW,

    // Configuration management actions
    CREATE,
    CONFIG_UPDATE,
    DELETE,
    EXPORT,
    IMPORT,

    // Artifact management actions
    ARTIFACT_VIEW,
    ARTIFACT_CREATE,
    ARTIFACT_UPDATE,
    ARTIFACT_DELETE,
    ARTIFACT_PATCH,
    ARTIFACT_EXPORT,
    ARTIFACT_IMPORT,

    // Configuration service actions
    SERVICE_SAVE,
    SERVICE_LOAD,
    SERVICE_DELETE,
    CACHE_CLEAR,
    CACHE_HIT,
    CACHE_MISS,

    // Configuration validation actions
    VALIDATION_SUCCESS,
    VALIDATION_FAILURE
}