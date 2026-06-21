package com.aidentx.encryption.service.impl;

import com.aidentx.encryption.config.AppConstants;
import com.aidentx.encryption.config.EncryptionStarterProperties;
import com.aidentx.encryption.exception.KeyManagementException;
import com.aidentx.encryption.model.EncryptedDataContainer;
import com.aidentx.encryption.model.EncryptionKey;
import com.aidentx.encryption.repository.EncryptionKeyRepository;
import com.aidentx.encryption.service.KeyManagementService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for the lifecycle of encryption keys.
 * <p>
 * This implementation derives a master key from user-provided properties using PBKDF2.
 * It uses this master key to encrypt and decrypt data encryption keys (DEKs), which are
 * then persisted by the consumer application via the {@link EncryptionKeyRepository}.
 */
public class KeyManagementServiceImpl implements KeyManagementService {

    private final IUnifiedLogger logger;
    private final EncryptionKeyRepository encryptionKeyRepository;
    private final EncryptionStarterProperties starterProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKey masterKey;

    public KeyManagementServiceImpl(
            EncryptionKeyRepository encryptionKeyRepository,
            EncryptionStarterProperties starterProperties,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.encryptionKeyRepository = encryptionKeyRepository;
        this.starterProperties = starterProperties;
        this.logger = unifiedLoggerFactory.getLogger(
                AppConstants.LOGGER_NAME_KEY_MANAGEMENT_SERVICE,
                AppConstants.KEY_MANAGEMENT_LOGGER_CATEGORY
        );
        initializeMasterKey();
    }

    /**
     * Initializes the master key by deriving it from the master-key and salt properties
     * using the PBKDF2 algorithm. This derived key is held in memory and is never stored at rest.
     */
    private void initializeMasterKey() {
        logger.info().message(AppConstants.LOG_MSG_INIT_MASTER_KEY).log();
        try {
            String password = starterProperties.getMasterKey();
            String salt = starterProperties.getSalt();

            SecretKeyFactory factory = SecretKeyFactory.getInstance(AppConstants.PBKDF2_ALGORITHM);
            KeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt.getBytes(),
                    AppConstants.PBKDF2_ITERATIONS,
                    AppConstants.AES_KEY_LENGTH_BITS
            );
            SecretKey tmp = factory.generateSecret(spec);
            this.masterKey = new SecretKeySpec(tmp.getEncoded(), AppConstants.MASTER_KEY_ALGORITHM);

            logger.info().message(AppConstants.LOG_MSG_MASTER_KEY_DERIVED_SUCCESS).log();

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_MSG_MASTER_KEY_DERIVATION_FATAL).log();
            throw new KeyManagementException(AppConstants.ERROR_MSG_MASTER_KEY_DERIVATION_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * If a key for the given `configType` does not exist in the database, a new one
     * is generated, encrypted with the master key, and persisted.
     */
    @Override
    public byte[] getConfigurationKey(String configType) {
        Optional<EncryptionKey> keyFromDb = encryptionKeyRepository.findByKeyTypeAndIsActiveTrue(configType);

        if (keyFromDb.isPresent()) {
            logger.trace()
                    .message(AppConstants.LOG_MSG_LOADING_KEY_FROM_DB)
                    .detail(AppConstants.LOG_DETAIL_KEY_TYPE, configType)
                    .log();
            EncryptionKey encryptionKey = keyFromDb.get();
            try {
                SecretKey decryptedConfigKey = decryptData(encryptionKey.getEncryptedKeyValue(), this.masterKey);
                return decryptedConfigKey.getEncoded();
            } catch (Exception e) {
                throw new KeyManagementException(String.format(AppConstants.ERROR_MSG_DECRYPT_CONFIG_KEY_FAILED_FORMAT, configType), e);
            }
        } else {
            logger.info()
                    .message(AppConstants.LOG_MSG_NO_ACTIVE_KEY_GENERATING_NEW)
                    .detail(AppConstants.LOG_DETAIL_KEY_TYPE, configType)
                    .log();
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance(AppConstants.MASTER_KEY_ALGORITHM);
                keyGen.init(AppConstants.AES_KEY_LENGTH_BITS);
                SecretKey newConfigKey = keyGen.generateKey();

                byte[] encryptedConfigKeyBytes = encryptData(newConfigKey.getEncoded(), this.masterKey);

                EncryptionKey newDbKey = new EncryptionKey(configType, 1, encryptedConfigKeyBytes, true);
                encryptionKeyRepository.save(newDbKey);

                return newConfigKey.getEncoded();
            } catch (Exception e) {
                throw new KeyManagementException(String.format(AppConstants.ERROR_MSG_GENERATE_KEY_FAILED_FORMAT, configType), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This operation generates a new key for every existing key type, encrypts it with the
     * current master key, and updates the record in the database, incrementing its version.
     */
    @Override
    public void rotateAllKeys() {
        logger.warn().message(AppConstants.LOG_MSG_KEY_ROTATION_START).log();
        try {
            List<EncryptionKey> allConfigKeys = encryptionKeyRepository.findAll();
            if (allConfigKeys.isEmpty()) {
                logger.info().message(AppConstants.LOG_MSG_KEY_ROTATION_NO_KEYS).log();
                return;
            }

            logger.info().message(String.format(AppConstants.LOG_MSG_KEY_ROTATION_FOUND_KEYS_FORMAT, allConfigKeys.size())).log();

            for (EncryptionKey configKey : allConfigKeys) {
                // Generate a new data encryption key
                KeyGenerator keyGen = KeyGenerator.getInstance(AppConstants.MASTER_KEY_ALGORITHM);
                keyGen.init(AppConstants.AES_KEY_LENGTH_BITS);
                SecretKey newConfigKey = keyGen.generateKey();

                // Encrypt the new key with the master key
                byte[] encryptedNewConfigKey = encryptData(newConfigKey.getEncoded(), this.masterKey);

                // Update the entity
                configKey.setEncryptedKeyValue(encryptedNewConfigKey);
                configKey.setVersion(configKey.getVersion() + 1);
            }

            encryptionKeyRepository.saveAll(allConfigKeys);

            logger.info().message(AppConstants.LOG_MSG_KEY_ROTATION_SUCCESS).log();

        } catch (Exception e) {
            logger.error(e)
                    .message(AppConstants.LOG_MSG_KEY_ROTATION_UNEXPECTED_ERROR)
                    .detail(AppConstants.LOG_DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName())
                    .detail(AppConstants.LOG_DETAIL_EXCEPTION_MESSAGE, e.getMessage())
                    .log();
            throw new KeyManagementException(AppConstants.ERROR_MSG_KEY_ROTATION_FAILED, e);
        }
    }

    /**
     * Encrypts data using AES-GCM with a random IV and returns a serialized container.
     */
    private byte[] encryptData(byte[] data, SecretKey key) throws Exception {
        byte[] iv = new byte[AppConstants.GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AppConstants.ENCRYPTION_ALGORITHM_AES_GCM);
        GCMParameterSpec spec = new GCMParameterSpec(AppConstants.GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] cipherText = cipher.doFinal(data);

        EncryptedDataContainer container = new EncryptedDataContainer(
                Base64.getEncoder().encodeToString(cipherText),
                Base64.getEncoder().encodeToString(iv),
                AppConstants.EMPTY_AAD
        );
        return objectMapper.writeValueAsBytes(container);
    }

    /**
     * Decrypts data from a serialized container using AES-GCM.
     */
    private SecretKey decryptData(byte[] encryptedPayload, SecretKey key) throws Exception {
        EncryptedDataContainer container = objectMapper.readValue(encryptedPayload, EncryptedDataContainer.class);
        byte[] iv = Base64.getDecoder().decode(container.getIv());
        byte[] cipherText = Base64.getDecoder().decode(container.getEncryptedData());

        Cipher cipher = Cipher.getInstance(AppConstants.ENCRYPTION_ALGORITHM_AES_GCM);
        GCMParameterSpec spec = new GCMParameterSpec(AppConstants.GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] decryptedBytes = cipher.doFinal(cipherText);

        return new SecretKeySpec(decryptedBytes, 0, decryptedBytes.length, AppConstants.MASTER_KEY_ALGORITHM);
    }
}