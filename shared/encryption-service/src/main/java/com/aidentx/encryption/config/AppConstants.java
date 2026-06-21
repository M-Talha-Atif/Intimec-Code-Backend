package com.aidentx.encryption.config;

public final class AppConstants {

    // --- Cryptographic Algorithms and Parameters ---
    public static final String MASTER_KEY_ALGORITHM = "AES";
    public static final String ENCRYPTION_ALGORITHM_AES_GCM = "AES/GCM/NoPadding";
    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    // --- Key and Parameter Sizes ---
    public static final int AES_KEY_LENGTH_BITS = 256;
    public static final int GCM_IV_LENGTH_BYTES = 12;
    public static final int GCM_TAG_LENGTH_BITS = 128;
    public static final int PBKDF2_ITERATIONS = 65536;

    // --- Logger Names and Categories ---
    public static final String KEY_MANAGEMENT_LOGGER_NAME = "KeyManagementService";
    public static final String KEY_MANAGEMENT_LOGGER_CATEGORY = "key-management";
    public static final String KEY_MANAGEMENT_CATEGORY = "key-management"; // Consistent category name

    // --- Field Encryption Constants ---
    public static final String ENCRYPT_SENSITIVE_FIELDS_START = "Starting sensitive field encryption.";
    public static final String ENCRYPT_SENSITIVE_FIELDS_NULL = "JSON data is null, skipping encryption.";
    public static final String ENCRYPT_FULL_CONFIG = "Encrypting entire configuration.";
    public static final String ENCRYPT_JSON_TO_STRING = "Serialized JSON to string for full encryption.";
    public static final String ENCRYPT_FULL_CONFIG_SUCCESS = "Full configuration encryption successful.";
    public static final String ENCRYPT_FULL_CONFIG_COMPLETE = "Full configuration encryption process complete.";
    public static final String ENCRYPT_FIELD_LEVEL = "Starting field-level encryption.";
    public static final String ENCRYPT_FIELD_LEVEL_COMPLETE = "Field-level encryption complete.";
    public static final String ENCRYPT_SENSITIVE_FIELDS_FAILED = "Failed to encrypt sensitive fields for configType: %s";
    public static final String ENCRYPT_FIELD_PROCESS_FAILED = "Error processing field for encryption: %s";
    public static final String ENCRYPTION_RUNTIME_EXCEPTION = "Encryption process failed";
    public static final String DECRYPT_SENSITIVE_FIELDS_START = "Starting sensitive field decryption.";
    public static final String DECRYPT_SENSITIVE_FIELDS_NULL = "JSON data is null, skipping decryption.";
    public static final String DECRYPT_FIELD_LEVEL = "Starting field-level decryption.";
    public static final String DECRYPT_SENSITIVE_FIELDS_COMPLETE = "Field-level decryption complete.";
    public static final String DECRYPT_SENSITIVE_FIELDS_FAILED = "Failed to decrypt sensitive fields for configType: %s";
    public static final String DECRYPT_FIELD_PROCESS_FAILED = "Error processing field for decryption: %s";
    public static final String DECRYPTION_RUNTIME_EXCEPTION = "Decryption process failed";
    public static final String INVALID_CRYPTO_FIELD_STRUCTURE = "Invalid $crypto field structure";
    public static final String ENCRYPT_ENTIRE_CONFIGRATION = "encryptEntireConfig";

    // --- Encryption Service Constants ---
    public static final String ENCRYPTION_PROCESS_FAILED = "Encryption process failed";
    public static final String DECRYPTION_INTEGRITY_FAILED = "Decryption failed due to data integrity check. The data may have been tampered with.";
    public static final String DECRYPTION_PROCESS_FAILED = "Decryption failed for configType: %s";

    // --- Cryptographic Parameters ---
    public static final String CRYPTO_ALGORITHM_AES_GCM = "AES/GCM/NoPadding";
    public static final String CRYPTO_KEY_ALGORITHM_AES = "AES";
    public static final int CRYPTO_IV_LENGTH_BYTES = 12;
    public static final int CRYPTO_TAG_LENGTH_BITS = 128;
    public static final String EMPTY_AAD = ""; // Empty Additional Authenticated Data

    // --- Logger Configuration ---
    public static final String LOGGER_NAME_ENCRYPTION_SERVICE = "EncryptionService";
    public static final String LOG_CATEGORY_ENCRYPTION = "service-encryption";
    public static final String LOG_SUB_CATEGORY_ENCRYPTION_OPS = "encryption";

    // --- Logging Detail Keys and Values ---
    public static final String LOG_DETAIL_OPERATION = "operation";
    public static final String LOG_DETAIL_FAILURE_REASON = "failureReason";
    public static final String LOG_VALUE_OP_ENCRYPT = "ENCRYPT";
    public static final String LOG_VALUE_OP_DECRYPT = "DECRYPT";
    public static final String LOG_VALUE_FAILURE_TAG_MISMATCH = "TAG_MISMATCH";

    // --- Logging Messages ---
    public static final String LOG_MSG_ENCRYPTION_START = "Starting encryption";
    public static final String LOG_MSG_IV_GENERATED = "IV generated";
    public static final String LOG_MSG_CIPHER_INIT_ENCRYPT = "Cipher initialized for encryption";
    public static final String LOG_MSG_ENCRYPTION_COMPLETE = "Encryption complete";
    public static final String LOG_MSG_CONTAINER_SERIALIZED = "Encrypted container serialized";
    public static final String LOG_MSG_ENCRYPTION_FAILED_FORMAT = "Encryption failed for configType: %s";
    public static final String LOG_MSG_DECRYPTION_START = "Starting decryption";
    public static final String LOG_MSG_CONTAINER_DECODED = "Container decoded";
    public static final String LOG_MSG_CONTAINER_DESERIALIZED = "Container deserialized";
    public static final String LOG_MSG_CIPHER_INIT_DECRYPT = "Cipher initialized for decryption";
    public static final String LOG_MSG_DECRYPTION_COMPLETE = "Decryption complete";
    public static final String LOG_MSG_DECRYPTION_TAG_MISMATCH_FORMAT = "Decryption failed due to authentication tag mismatch for configType: %s";
    public static final String LOG_MSG_DECRYPTION_FAILED_FORMAT = "Decryption failed for configType: %s";

    // --- Exception Messages ---
    public static final String ERROR_MSG_ENCRYPTION_PROCESS_FAILED = "Encryption process failed.";
    public static final String ERROR_MSG_DECRYPTION_INTEGRITY_FAILED = "Decryption failed due to an integrity check mismatch (authentication tag).";
    public static final String ERROR_MSG_DECRYPTION_PROCESS_FAILED_FORMAT = "Decryption process failed for configType: %s";

    public static final String LOG_DETAIL_CONFIG_TYPE = "configType";

    // --- Logger Configuration ---
    public static final String LOGGER_NAME_KEY_MANAGEMENT_SERVICE = "KeyManagementService";

    // --- Logging Detail Keys ---
    public static final String LOG_DETAIL_KEY_TYPE = "keyType";
    public static final String LOG_DETAIL_EXCEPTION_TYPE = "exceptionType";
    public static final String LOG_DETAIL_EXCEPTION_MESSAGE = "exceptionMessage";

    // --- Logging Messages ---
    public static final String LOG_MSG_INIT_MASTER_KEY = "Initializing master key via PBKDF2 derivation.";
    public static final String LOG_MSG_MASTER_KEY_DERIVED_SUCCESS = "Master key successfully derived.";
    public static final String LOG_MSG_MASTER_KEY_DERIVATION_FATAL = "FATAL: Failed to derive master key from properties.";
    public static final String LOG_MSG_LOADING_KEY_FROM_DB = "Loading configuration key from database.";
    public static final String LOG_MSG_NO_ACTIVE_KEY_GENERATING_NEW = "No active key found for type, generating new key.";
    public static final String LOG_MSG_KEY_ROTATION_START = "Starting configuration key rotation for all key types.";
    public static final String LOG_MSG_KEY_ROTATION_NO_KEYS = "No existing keys found to rotate.";
    public static final String LOG_MSG_KEY_ROTATION_FOUND_KEYS_FORMAT = "Found %d configuration key(s) to rotate.";
    public static final String LOG_MSG_KEY_ROTATION_SUCCESS = "All configuration keys rotated successfully.";
    public static final String LOG_MSG_KEY_ROTATION_UNEXPECTED_ERROR = "Failed to rotate configuration keys due to an unexpected error.";

    // --- Exception Messages ---
    public static final String ERROR_MSG_MASTER_KEY_DERIVATION_FAILED = "Failed to derive master key from configuration properties";
    public static final String ERROR_MSG_DECRYPT_CONFIG_KEY_FAILED_FORMAT = "Failed to decrypt configuration key for type: %s";
    public static final String ERROR_MSG_GENERATE_KEY_FAILED_FORMAT = "Failed to generate and save new key for type: %s";
    public static final String ERROR_MSG_KEY_ROTATION_FAILED = "Failed to rotate configuration keys";

    private AppConstants() {
        // Utility class, should not be instantiated.
    }
}