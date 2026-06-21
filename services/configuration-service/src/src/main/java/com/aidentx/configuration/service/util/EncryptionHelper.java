package com.aidentx.configuration.service.util;

import com.aidentx.configuration.config.AppConstants;
import com.aidentx.configuration.config.EncryptionRulesConfig;
import com.aidentx.configuration.dto.EncryptionMetadata;
import com.aidentx.encryption.service.EncryptionService;
import com.aidentx.encryption.service.FieldEncryptionService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service responsible for handling encryption and decryption of configuration
 * data, whether it is the main {@code configData}, an inline JSON artifact, or
 * the content of a file artifact.
 * <p>
 * This service is a shared dependency that provides centralized encryption
 * logic. It supports both full (blob) encryption and field-level encryption
 * based on configurable rules.
 * </p>
 */
@Service
public class EncryptionHelper {

    private final IUnifiedLogger logger;
    private final EncryptionService coreEncryptionService;
    private final FieldEncryptionService fieldEncryptionService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the EncryptionHelper with required dependencies.
     *
     * @param coreEncryptionService  core encryption service for full payload
     *                               encryption/decryption
     * @param fieldEncryptionService field-level encryption service for selective
     *                               field encryption/decryption
     * @param objectMapper           Jackson ObjectMapper for JSON serialization and
     *                               deserialization
     * @param unifiedLoggerFactory   Factory for creating the logger instance.
     */
    public EncryptionHelper(EncryptionService coreEncryptionService,
            FieldEncryptionService fieldEncryptionService,
            ObjectMapper objectMapper, UnifiedLoggerFactory unifiedLoggerFactory) {
        this.coreEncryptionService = coreEncryptionService;
        this.fieldEncryptionService = fieldEncryptionService;
        this.objectMapper = objectMapper;
        this.logger = unifiedLoggerFactory.getLogger(AppConstants.CONFIG_ENCRYPTION_SERVICE_LOGGER_NAME,
                AppConstants.CONFIG_ENCRYPTION_SERVICE_LOGGER_CATEGORY);
    }

    /**
     * Encrypts a given JSON node before saving, either fully or at the field-level,
     * depending on the provided flag and system-wide encryption rules.
     *
     * @param jsonData        JSON data to encrypt
     * @param configType      configuration type (used to determine encryption
     *                        rules)
     * @param configKey       configuration key (used to determine encryption rules)
     * @param encryptEntirely if true, encrypts the entire JSON as a blob;
     *                        otherwise,
     *                        applies field-level encryption if configured
     * @return an encrypted JsonNode, wrapped in an envelope if fully encrypted
     * @throws Exception if encryption or JSON processing fails
     */
    public JsonNode encryptDataForSaving(JsonNode jsonData, String configType, String configKey,
            boolean encryptEntirely) throws Exception {
        boolean shouldEncryptRule = EncryptionRulesConfig.shouldEncrypt(configType, configKey);

        if (encryptEntirely) {
            logger.trace().category(AppConstants.ENCRYPTION_CATEGORY)
                    .message(AppConstants.LOG_FULL_ENCRYPTION_APPLIED)
                    .details(Map.of(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType, AppConstants.LOG_DETAIL_CONFIG_KEY,
                            configKey))
                    .log();

            String jsonString = objectMapper.writeValueAsString(jsonData);
            String encryptedBlob = coreEncryptionService.encrypt(jsonString,
                    EncryptionRulesConfig.getKeyType(configType));

            ObjectNode envelope = objectMapper.createObjectNode();
            envelope.put(AppConstants.ENCRYPTION_FIELD_ENCRYPTED, true);
            envelope.put(AppConstants.ENCRYPTION_FIELD_DATA, encryptedBlob);
            return envelope;
        } else if (shouldEncryptRule) {
            logger.trace().category(AppConstants.ENCRYPTION_CATEGORY)
                    .message(AppConstants.LOG_FIELD_ENCRYPTION_APPLIED)
                    .details(Map.of(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType, AppConstants.LOG_DETAIL_CONFIG_KEY,
                            configKey))
                    .log();
            return fieldEncryptionService.encryptSensitiveFields(jsonData, configType, false);
        } else {
            logger.trace().category(AppConstants.ENCRYPTION_CATEGORY)
                    .message(AppConstants.LOG_NO_ENCRYPTION_RULE_MATCHED)
                    .details(Map.of(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType, AppConstants.LOG_DETAIL_CONFIG_KEY,
                            configKey))
                    .log();
            // Still process for any fields explicitly marked as sensitive even if the rule
            // is false
            return fieldEncryptionService.encryptSensitiveFields(jsonData, configType, false);
        }
    }

    /**
     * Decrypts inline JSON data that is either fully encrypted (in an envelope) or
     * partially
     * encrypted at the field level.
     *
     * @param rawData    raw JsonNode to decrypt
     * @param configType configuration type for looking up decryption keys/rules
     * @param configKey  configuration key for looking up decryption keys/rules
     * @return a decrypted JsonNode
     * @throws Exception if decryption or JSON parsing fails
     */
    public JsonNode decryptInlineData(JsonNode rawData, String configType, String configKey) throws Exception {
        if (rawData == null)
            return null;

        if (rawData.has(AppConstants.ENCRYPTION_FIELD_ENCRYPTED)
                && rawData.get(AppConstants.ENCRYPTION_FIELD_ENCRYPTED).asBoolean()
                && rawData.has(AppConstants.ENCRYPTION_FIELD_DATA)) {
            logger.trace().category(AppConstants.DECRYPTION_CATEGORY)
                    .message(AppConstants.LOG_FULL_DECRYPTION_APPLIED)
                    .details(Map.of(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType, AppConstants.LOG_DETAIL_CONFIG_KEY,
                            configKey))
                    .log();
            String encryptedBlob = rawData.get(AppConstants.ENCRYPTION_FIELD_DATA).asText();
            String decryptedJson = coreEncryptionService.decrypt(encryptedBlob,
                    EncryptionRulesConfig.getKeyType(configType));
            return objectMapper.readTree(decryptedJson);
        }

        if (fieldEncryptionService.containsEncryptedFields(rawData)) {
            logger.trace().category(AppConstants.DECRYPTION_CATEGORY)
                    .message(AppConstants.LOG_FIELD_DECRYPTION_APPLIED)
                    .details(Map.of(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType, AppConstants.LOG_DETAIL_CONFIG_KEY,
                            configKey))
                    .log();
            return fieldEncryptionService.decryptSensitiveFields(rawData, configType);
        }

        return rawData; // Return as-is if no encryption is detected
    }

    /**
     * Checks whether a given JsonNode contains encrypted data, either as a full
     * blob
     * or with field-level markers.
     *
     * @param rawData the JsonNode to check
     * @return true if any encryption is detected, false otherwise
     */
    public boolean isDataEncrypted(JsonNode rawData) {
        if (rawData == null)
            return false;

        if (rawData.has(AppConstants.ENCRYPTION_FIELD_ENCRYPTED)
                && rawData.get(AppConstants.ENCRYPTION_FIELD_ENCRYPTED).asBoolean()) {
            return true;
        }
        return fieldEncryptionService.containsEncryptedFields(rawData);
    }

    /**
     * Retrieves encryption metadata for a specified configuration type.
     *
     * @param configType the type of configuration to get metadata for
     * @return an {@link EncryptionMetadata} object describing the encryption
     *         details
     */
    public EncryptionMetadata getEncryptionMetadata(String configType) {
        String keyType = EncryptionRulesConfig.getKeyType(configType);
        return new EncryptionMetadata(AppConstants.ENCRYPTION_ALGORITHM_AES_256_GCM, keyType);
    }
}