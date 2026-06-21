package com.aidentx.encryption.service.impl;

import com.aidentx.encryption.config.AppConstants;
import com.aidentx.encryption.service.EncryptionService;
import com.aidentx.encryption.service.FieldEncryptionService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Set;

/**
 * Implementation of field-level encryption service that uses the $crypto format
 * for encrypting sensitive fields within JSON configurations.
 * <p>
 * This implementation is unified to use AES/GCM/NoPadding for all cryptographic
 * operations by delegating to the core {@link EncryptionService}.
 */
public class FieldEncryptionServiceImpl implements FieldEncryptionService {

    private final IUnifiedLogger logger;
    // --- Constants ---
    private static final String CRYPTO_FIELD = "$crypto";
    private static final String ALGORITHM_NAME = "AES/GCM/NoPadding";

    // Sensitive field patterns (case-insensitive)
    private static final Set<String> SENSITIVE_FIELD_PATTERNS = Set.of(
            "password", "passwd", "pwd", "secret", "key", "token", "credential",
            "auth", "api_key", "apikey", "private_key", "privatekey", "client_secret",
            "clientsecret", "access_token", "accesstoken", "refresh_token",
            "refreshtoken", "bearer_token", "bearertoken", "jwt_secret", "jwtsecret",
            "encryption_key", "encryptionkey", "master_key", "masterkey", "salt",
            "connection_string", "connectionstring", "database_url", "databaseurl",
            "smtp_password", "smtppassword", "ldap_password", "ldappassword", "certificate");

    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the FieldEncryptionServiceImpl.
     *
     * @param encryptionService    The core encryption service that provides unified
     *                             AES-GCM encryption.
     * @param objectMapper         The Jackson ObjectMapper for JSON manipulation.
     * @param unifiedLoggerFactory The factory to create a logger instance.
     */
    public FieldEncryptionServiceImpl(EncryptionService encryptionService,
            ObjectMapper objectMapper, UnifiedLoggerFactory unifiedLoggerFactory) {
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.logger = unifiedLoggerFactory.getLogger("FieldEncryption",
                "encryption-fields");
    }

    /**
     * Encrypts sensitive fields in a JSON configuration object.
     * <p>
     * This method supports two modes of encryption:
     * <ul>
     * <li><b>Full configuration encryption</b>: The entire JSON object is
     * serialized to a string
     * and encrypted using a symmetric encryption algorithm (e.g., AES-256-GCM). The
     * result is
     * wrapped in a metadata object containing encryption metadata.</li>
     * <li><b>Field-level encryption</b>: Only predefined sensitive fields inside
     * the JSON structure
     * are selectively encrypted.</li>
     * </ul>
     *
     * @param jsonData            the input JSON object containing configuration
     *                            data; may contain sensitive fields
     * @param configType          the type of configuration being encrypted (used
     *                            for key selection or audit tagging)
     * @param encryptEntireConfig if {@code true}, encrypt the entire JSON config as
     *                            a single blob;
     *                            if {@code false}, encrypt only sensitive fields
     *                            inside the object
     * @return a {@link JsonNode} representing the encrypted configuration:
     *         <ul>
     *         <li>For full encryption: an object with fields: "encrypted", "data",
     *         "algorithm", and "keyType".</li>
     *         <li>For field-level encryption: the original structure with encrypted
     *         sensitive fields.</li>
     *         </ul>
     * @throws RuntimeException if any encryption step fails (serialization or
     *                          encryption failure)
     */
    @Override
    public JsonNode encryptSensitiveFields(JsonNode jsonData, String configType, boolean encryptEntireConfig) {
        logger.trace().category("field-encryption")
                .message(AppConstants.ENCRYPT_SENSITIVE_FIELDS_START).detail("configType", configType)
                .detail(AppConstants.ENCRYPT_ENTIRE_CONFIGRATION, encryptEntireConfig).log();

        if (jsonData == null || jsonData.isNull()) {
            logger.trace().category("field-encryption")
                    .message(AppConstants.ENCRYPT_SENSITIVE_FIELDS_NULL).detail("configType", configType).log();
            return jsonData;
        }

        try {
            // If encrypting entire config, use the existing encryption service
            if (encryptEntireConfig) {
                logger.trace().category("field-encryption").message(AppConstants.ENCRYPT_FULL_CONFIG)
                        .detail("configType", configType).log();
                String jsonString = objectMapper.writeValueAsString(jsonData);
                logger.trace().category("field-encryption").message(AppConstants.ENCRYPT_JSON_TO_STRING)
                        .detail("length", jsonString.length()).log();

                String encryptedData = encryptionService.encrypt(jsonString, configType);
                logger.trace().category("field-encryption").message(AppConstants.ENCRYPT_FULL_CONFIG_SUCCESS)
                        .detail("configType", configType).log();

                // Return a wrapper indicating full encryption
                ObjectNode wrapper = objectMapper.createObjectNode();
                wrapper.put("encrypted", true);
                wrapper.put("data", encryptedData);
                wrapper.put("algorithm", ALGORITHM_NAME);
                wrapper.put("keyType", configType);

                logger.trace().category("field-encryption").message(AppConstants.ENCRYPT_FULL_CONFIG_COMPLETE)
                        .detail("configType", configType).log();
                return wrapper;
            }

            // Otherwise, encrypt only sensitive fields
            logger.trace().category("field-encryption").message(AppConstants.ENCRYPT_FIELD_LEVEL)
                    .detail("configType", configType).log();
            JsonNode result = processNodeForEncryption(jsonData.deepCopy(), configType);

            logger.trace().category("field-encryption").message(AppConstants.ENCRYPT_FIELD_LEVEL_COMPLETE)
                    .detail("configType", configType).log();
            return result;

        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.ENCRYPT_SENSITIVE_FIELDS_FAILED, configType)).log();
            ;
            throw new RuntimeException(AppConstants.ENCRYPTION_RUNTIME_EXCEPTION, e);
        }
    }

    /**
     * Decrypts sensitive fields in a JSON configuration object.
     * <p>
     * This method processes a JSON structure that contains field-level encrypted
     * values.
     * Encrypted fields are expected to follow a specific marker format (e.g.,
     * <code>$crypto</code> wrapper),
     * which are decrypted individually using the encryption service.
     * <p>
     * Note: This method does not handle full-configuration decryption (where the
     * entire object is encrypted
     * as a single string). If such a structure is detected, the caller should first
     * unwrap the encrypted
     * data and pass it as a decrypted JSON string to this method if field-level
     * decryption is still required.
     *
     * @param jsonData   the input JSON object with potentially encrypted fields in
     *                   field-level encryption format
     * @param configType the configuration type (used for determining the correct
     *                   decryption key or algorithm)
     * @return a {@link JsonNode} where all encrypted fields are decrypted and
     *         restored to their original values
     * @throws RuntimeException if any decryption operation fails or the input
     *                          cannot be processed
     */
    @Override
    public JsonNode decryptSensitiveFields(JsonNode jsonData, String configType) {
        logger.trace().category("field-decryption").message(AppConstants.DECRYPT_SENSITIVE_FIELDS_START)
                .detail("configType", configType).log();

        if (jsonData == null || jsonData.isNull()) {
            logger.trace().category("field-decryption").message(AppConstants.DECRYPT_SENSITIVE_FIELDS_NULL)
                    .detail("configType", configType).log();
            return jsonData;
        }

        try {
            logger.trace().category("field-decryption").message(AppConstants.DECRYPT_FIELD_LEVEL)
                    .detail("configType", configType).log();
            // Only handle field-level encryption with $crypto format
            JsonNode result = processNodeForDecryption(jsonData.deepCopy(), configType);

            logger.trace().category("field-decryption").message(AppConstants.DECRYPT_SENSITIVE_FIELDS_COMPLETE)
                    .detail("configType", configType).log();
            return result;

        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.DECRYPT_SENSITIVE_FIELDS_FAILED, configType)).log();
            throw new RuntimeException(AppConstants.DECRYPTION_RUNTIME_EXCEPTION, e);
        }
    }

    /**
     * Determines whether a given field name is considered sensitive.
     * <p>
     * A field is considered sensitive if its name (case-insensitive) contains any
     * of the predefined
     * patterns (e.g., "password", "secret", "token", etc.), which are typically
     * defined in
     * {@code SENSITIVE_FIELD_PATTERNS}.
     *
     * @param fieldName the name of the field to evaluate
     * @return {@code true} if the field name matches a sensitive pattern,
     *         {@code false} otherwise
     */
    @Override
    public boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lowerFieldName = fieldName.toLowerCase();
        return SENSITIVE_FIELD_PATTERNS.stream().anyMatch(lowerFieldName::contains);
    }

    /**
     * Checks whether the provided JSON data contains any encrypted fields.
     * <p>
     * This method recursively traverses the given {@link JsonNode} and checks for
     * the presence
     * of fields marked with the current encryption format (e.g., objects containing
     * a {@code "$crypto"} marker).
     * Older encryption formats are not supported by this check.
     *
     * @param jsonData the JSON object to inspect for encrypted fields
     * @return {@code true} if any field is encrypted using the supported format;
     *         {@code false} otherwise
     */
    @Override
    public boolean containsEncryptedFields(JsonNode jsonData) {
        if (jsonData == null || jsonData.isNull()) {
            return false;
        }
        return hasEncryptedFieldsRecursive(jsonData);
    }

    /**
     * Recursively processes a {@link JsonNode} to encrypt sensitive fields.
     * <p>
     * This method traverses the given JSON structure (objects and arrays) and
     * selectively encrypts
     * values whose field names are considered sensitive (as determined by
     * {@link #isSensitiveField(String)}).
     * Only textual values are encrypted. Non-text fields and non-sensitive fields
     * are left unchanged or processed recursively.
     * <p>
     * Encrypted fields are replaced with a structured marker object (created using
     * {@link #createCryptoField(String, String)})
     * that includes encryption metadata and the ciphertext.
     *
     * @param node       the input JSON node to be processed (can be an object,
     *                   array, or primitive)
     * @param configType the configuration type or encryption context, used to
     *                   select encryption keys or strategies
     * @return a new {@link JsonNode} with sensitive textual fields encrypted and
     *         non-sensitive content preserved
     * @throws Exception if encryption fails unexpectedly during processing
     */
    private JsonNode processNodeForEncryption(JsonNode node, String configType) throws Exception {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            ObjectNode result = objectMapper.createObjectNode();

            for (String fieldName : (Iterable<String>) () -> objectNode.fieldNames()) {
                JsonNode fieldValue = objectNode.get(fieldName);
                try {
                    if (isSensitiveField(fieldName) && fieldValue.isTextual()) {
                        result.set(fieldName, createCryptoField(fieldValue.asText(), configType));
                    } else {
                        result.set(fieldName, processNodeForEncryption(fieldValue, configType));
                    }
                } catch (Exception e) {
                    logger.error(e).message(String.format(AppConstants.ENCRYPT_FIELD_PROCESS_FAILED, fieldName)).log();
                    result.set(fieldName, fieldValue); // Keep original on error
                }
            }
            return result;
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            ArrayNode result = objectMapper.createArrayNode();
            for (JsonNode item : arrayNode) {
                result.add(processNodeForEncryption(item, configType));
            }
            return result;
        }
        return node;
    }

    /**
     * Recursively processes a {@link JsonNode} to decrypt fields wrapped in a
     * <code>$crypto</code> format.
     * <p>
     * This method traverses the entire JSON tree, decrypting any field that
     * conforms to the expected encrypted format—
     * typically an object with a <code>$crypto</code> key. It supports nested
     * objects and arrays, ensuring all levels
     * of the structure are inspected and decrypted appropriately.
     * <p>
     * If a decryption error occurs for a specific field, the original field value
     * is retained and logged.
     *
     * @param node       the input {@link JsonNode} (can be an object, array, or
     *                   primitive) to scan and decrypt
     * @param configType the type of configuration or encryption context (used to
     *                   determine decryption key or algorithm)
     * @return a new {@link JsonNode} with decrypted values where applicable, and
     *         original values otherwise
     * @throws Exception if a decryption operation fails and is not caught during
     *                   recursion
     */
    private JsonNode processNodeForDecryption(JsonNode node, String configType) throws Exception {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            if (objectNode.has(CRYPTO_FIELD)) {
                return decryptCryptoField(objectNode, configType);
            }
            ObjectNode result = objectMapper.createObjectNode();
            for (String fieldName : (Iterable<String>) () -> objectNode.fieldNames()) {
                JsonNode fieldValue = objectNode.get(fieldName);
                try {
                    result.set(fieldName, processNodeForDecryption(fieldValue, configType));
                } catch (Exception e) {
                    logger.error(e).message(String.format(AppConstants.DECRYPT_FIELD_PROCESS_FAILED, fieldName)).log();
                    result.set(fieldName, fieldValue); // Keep original on error
                }
            }
            return result;
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            ArrayNode result = objectMapper.createArrayNode();
            for (JsonNode item : arrayNode) {
                result.add(processNodeForDecryption(item, configType));
            }
            return result;
        }
        return node;
    }

    /**
     * Creates a structured <code>$crypto</code> JSON field representing an
     * encrypted version of the given plaintext.
     * <p>
     * This method delegates the actual encryption to the unified
     * {@link EncryptionService},
     * which uses AES-GCM.
     * </p>
     * The resulting JSON structure follows this format:
     *
     * <pre>{@code
     * {
     *   "$crypto": {
     *     "encryptedValue": "...", // Base64 encoded EncryptedDataContainer
     *     "algorithm": "AES/GCM/NoPadding"
     *   }
     * }
     * }</pre>
     *
     * @param plaintext  the plain text value to be encrypted
     * @param configType the configuration context (used to determine the correct
     *                   encryption key via the EncryptionService)
     * @return a {@link JsonNode} representing a <code>$crypto</code> object
     *         containing the encrypted data.
     * @throws Exception if the encryption fails.
     */
    private JsonNode createCryptoField(String plaintext, String configType) throws Exception {
        // Delegate encryption to the core service
        String encryptedBlob = encryptionService.encrypt(plaintext, configType);

        // Create the simplified $crypto structure
        ObjectNode cryptoValue = objectMapper.createObjectNode();
        cryptoValue.put("encryptedValue", encryptedBlob);
        cryptoValue.put("algorithm", ALGORITHM_NAME);

        ObjectNode result = objectMapper.createObjectNode();
        result.set(CRYPTO_FIELD, cryptoValue);

        return result;
    }

    /**
     * Decrypts a structured <code>$crypto</code> field and returns the original
     * plaintext value.
     * <p>
     * This method expects a unified AES-GCM based structure:
     *
     * <pre>{@code
     * {
     *   "$crypto": {
     *     "encryptedValue": "...", // Base64 encoded EncryptedDataContainer
     *     "algorithm": "AES/GCM/NoPadding"
     *   }
     * }
     * }</pre>
     *
     * The decryption process is delegated to the unified {@link EncryptionService}.
     *
     * @param cryptoObject the object node containing the <code>$crypto</code>
     *                     structure
     * @param configType   the configuration type (used to resolve the appropriate
     *                     decryption key via the EncryptionService)
     * @return a {@link JsonNode} containing the decrypted plaintext as a text node
     * @throws IllegalArgumentException if the crypto structure is invalid or
     *                                  incomplete
     * @throws Exception                if decryption or key resolution fails
     */
    private JsonNode decryptCryptoField(ObjectNode cryptoObject, String configType) throws Exception {
        JsonNode cryptoNode = cryptoObject.get(CRYPTO_FIELD);
        if (cryptoNode == null || !cryptoNode.isObject() || !cryptoNode.has("encryptedValue")) {
            throw new IllegalArgumentException(AppConstants.INVALID_CRYPTO_FIELD_STRUCTURE);
        }

        String encryptedBlob = cryptoNode.get("encryptedValue").asText();

        // Delegate decryption to the core service
        String decryptedText = encryptionService.decrypt(encryptedBlob, configType);

        return objectMapper.getNodeFactory().textNode(decryptedText);
    }

    /**
     * Recursively checks whether a given {@link JsonNode} contains any encrypted
     * fields.
     * <p>
     * An encrypted field is identified by the presence of a specific field name,
     * defined by the constant {@code CRYPTO_FIELD}, typically representing a
     * {@code "$crypto"} object that signifies encrypted content.
     * <p>
     * This method traverses the entire JSON tree, checking each object node and
     * array
     * element for the presence of the encrypted field.
     *
     * @param node the root {@link JsonNode} to inspect
     * @return {@code true} if any part of the JSON contains an encrypted field;
     *         {@code false} otherwise
     */
    private boolean hasEncryptedFieldsRecursive(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            if (objectNode.has(CRYPTO_FIELD)) {
                return true;
            }
            for (JsonNode childNode : objectNode) {
                if (hasEncryptedFieldsRecursive(childNode)) {
                    return true;
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode item : arrayNode) {
                if (hasEncryptedFieldsRecursive(item)) {
                    return true;
                }
            }
        }
        return false;
    }
}