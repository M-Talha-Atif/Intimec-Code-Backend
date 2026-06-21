package com.aidentx.encryption.service.impl;

import com.aidentx.encryption.config.AppConstants;
import com.aidentx.encryption.exception.DecryptionException;
import com.aidentx.encryption.model.EncryptedDataContainer;
import com.aidentx.encryption.service.EncryptionService;
import com.aidentx.encryption.service.KeyManagementService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * Implementation of {@link EncryptionService} using AES-GCM encryption.
 * <p>
 * This service provides methods to encrypt and decrypt strings using AES
 * algorithm
 * with Galois/Counter Mode (GCM) for authenticated encryption, ensuring
 * confidentiality
 * and integrity of data.
 * <p>
 * Encryption keys are managed externally and retrieved via the
 * {@link KeyManagementService}.
 * The encrypted data, initialization vector (IV), and any additional data are
 * encapsulated in an {@link EncryptedDataContainer} object, which is serialized
 * and base64-encoded
 * for storage or transmission.
 */
public class EncryptionServiceImpl implements EncryptionService {

    private final IUnifiedLogger logger;
    private final KeyManagementService keyManagementService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;

    /**
     * Constructs the encryption service with the given key management service.
     *
     * @param keyManagementService service used to retrieve encryption keys based on
     *                             configuration type
     * @param unifiedLoggerFactory the factory to create a logger instance for this
     *                             class
     */
    public EncryptionServiceImpl(KeyManagementService keyManagementService, UnifiedLoggerFactory unifiedLoggerFactory) {
        this.keyManagementService = keyManagementService;
        this.objectMapper = new ObjectMapper();
        this.secureRandom = new SecureRandom();
        this.logger = unifiedLoggerFactory.getLogger(
                AppConstants.LOGGER_NAME_ENCRYPTION_SERVICE,
                AppConstants.LOG_CATEGORY_ENCRYPTION
        );
    }

    /**
     * Encrypts the provided plaintext string using AES-GCM with a key corresponding
     * to the given configuration type.
     * <p>
     * The encrypted output is returned as a base64-encoded JSON string containing
     * the ciphertext and initialization vector.
     *
     * @param plainText  the plaintext string to encrypt
     * @param configType the configuration type to select the encryption key
     * @return base64-encoded JSON string representing the encrypted data container
     * @throws RuntimeException if encryption fails for any reason
     */
    @Override
    public String encrypt(String plainText, String configType) {
        logger.trace().category(AppConstants.LOG_SUB_CATEGORY_ENCRYPTION_OPS)
                .message(AppConstants.LOG_MSG_ENCRYPTION_START)
                .details(Map.of(
                        AppConstants.LOG_DETAIL_CONFIG_TYPE, configType,
                        AppConstants.LOG_DETAIL_OPERATION, AppConstants.LOG_VALUE_OP_ENCRYPT
                )).log();
        try {
            byte[] keyBytes = keyManagementService.getConfigurationKey(configType);
            SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, AppConstants.CRYPTO_KEY_ALGORITHM_AES);

            byte[] iv = new byte[AppConstants.CRYPTO_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            logger.trace().category(AppConstants.LOG_SUB_CATEGORY_ENCRYPTION_OPS)
                    .message(AppConstants.LOG_MSG_IV_GENERATED)
                    .detail(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType).log();

            Cipher cipher = Cipher.getInstance(AppConstants.CRYPTO_ALGORITHM_AES_GCM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AppConstants.CRYPTO_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
            logger.trace().category(AppConstants.LOG_SUB_CATEGORY_ENCRYPTION_OPS)
                    .message(AppConstants.LOG_MSG_CIPHER_INIT_ENCRYPT)
                    .detail(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType).log();

            byte[] cipherTextBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            logger.trace().category(AppConstants.LOG_SUB_CATEGORY_ENCRYPTION_OPS)
                    .message(AppConstants.LOG_MSG_ENCRYPTION_COMPLETE)
                    .detail(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType).log();

            EncryptedDataContainer container = new EncryptedDataContainer(
                    Base64.getEncoder().encodeToString(cipherTextBytes),
                    Base64.getEncoder().encodeToString(iv),
                    AppConstants.EMPTY_AAD);

            byte[] jsonContainer = objectMapper.writeValueAsBytes(container);
            logger.trace().category(AppConstants.LOG_SUB_CATEGORY_ENCRYPTION_OPS)
                    .message(AppConstants.LOG_MSG_CONTAINER_SERIALIZED)
                    .detail(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType).log();
            return Base64.getEncoder().encodeToString(jsonContainer);

        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_MSG_ENCRYPTION_FAILED_FORMAT, configType)).log();
            throw new RuntimeException(AppConstants.ERROR_MSG_ENCRYPTION_PROCESS_FAILED, e);
        }
    }

    /**
     * Decrypts the provided encrypted data string using AES-GCM with a key
     * corresponding to the given configuration type.
     * <p>
     * The input string should be a base64-encoded JSON representation of an
     * {@link EncryptedDataContainer} containing the ciphertext and initialization
     * vector.
     *
     * @param encryptedData base64-encoded JSON string representing the encrypted
     *                      data container
     * @param configType    the configuration type to select the encryption key
     * @return the decrypted plaintext string
     * @throws DecryptionException if decryption fails due to integrity checks or
     *                             other errors
     */
    @Override
    public String decrypt(String encryptedData, String configType) throws DecryptionException {
        logger.trace().category(AppConstants.LOG_SUB_CATEGORY_ENCRYPTION_OPS)
                .message(AppConstants.LOG_MSG_DECRYPTION_START)
                .details(Map.of(
                        AppConstants.LOG_DETAIL_CONFIG_TYPE, configType,
                        AppConstants.LOG_DETAIL_OPERATION, AppConstants.LOG_VALUE_OP_DECRYPT
                )).log();
        try {
            byte[] jsonContainer = Base64.getDecoder().decode(encryptedData);
            logger.trace().category(AppConstants.LOG_SUB_CATEGORY_ENCRYPTION_OPS)
                    .message(AppConstants.LOG_MSG_CONTAINER_DECODED)
                    .detail(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType).log();

            EncryptedDataContainer container = objectMapper.readValue(jsonContainer, EncryptedDataContainer.class);
            logger.trace().category(AppConstants.LOG_SUB_CATEGORY_ENCRYPTION_OPS)
                    .message(AppConstants.LOG_MSG_CONTAINER_DESERIALIZED)
                    .detail(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType).log();

            byte[] iv = Base64.getDecoder().decode(container.getIv());
            byte[] cipherText = Base64.getDecoder().decode(container.getEncryptedData());

            byte[] keyBytes = keyManagementService.getConfigurationKey(configType);
            SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, AppConstants.CRYPTO_KEY_ALGORITHM_AES);

            Cipher cipher = Cipher.getInstance(AppConstants.CRYPTO_ALGORITHM_AES_GCM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AppConstants.CRYPTO_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            logger.trace().category(AppConstants.LOG_SUB_CATEGORY_ENCRYPTION_OPS)
                    .message(AppConstants.LOG_MSG_CIPHER_INIT_DECRYPT)
                    .detail(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType).log();

            byte[] decryptedBytes = cipher.doFinal(cipherText);
            logger.trace().category(AppConstants.LOG_SUB_CATEGORY_ENCRYPTION_OPS)
                    .message(AppConstants.LOG_MSG_DECRYPTION_COMPLETE)
                    .detail(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType).log();

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (AEADBadTagException e) {
            logger.warn().category(AppConstants.LOG_SUB_CATEGORY_ENCRYPTION_OPS)
                    .failureReason(AppConstants.LOG_VALUE_FAILURE_TAG_MISMATCH)
                    .message(String.format(AppConstants.LOG_MSG_DECRYPTION_TAG_MISMATCH_FORMAT, configType))
                    .detail(AppConstants.LOG_DETAIL_CONFIG_TYPE, configType)
                    .log();
            throw new DecryptionException(AppConstants.ERROR_MSG_DECRYPTION_INTEGRITY_FAILED, e);
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_MSG_DECRYPTION_FAILED_FORMAT, configType)).log();
            throw new DecryptionException(String.format(AppConstants.ERROR_MSG_DECRYPTION_PROCESS_FAILED_FORMAT, configType), e);
        }
    }
}