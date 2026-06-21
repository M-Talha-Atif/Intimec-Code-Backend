package com.aidentx.configuration.service.impl;

import com.aidentx.configuration.config.AppConstants;
import com.aidentx.configuration.config.EncryptionRulesConfig;
import com.aidentx.configuration.exception.artifact.ArtifactEncryptionException;
import com.aidentx.configuration.exception.artifact.ArtifactFileException;
import com.aidentx.configuration.exception.artifact.ArtifactServiceException;
import com.aidentx.configuration.model.entity.Artifact;
import com.aidentx.configuration.model.entity.Configuration;
import com.aidentx.configuration.repository.ArtifactRepository;
import com.aidentx.configuration.service.ArtifactService;
import com.aidentx.configuration.service.util.EncryptionHelper;
import com.aidentx.encryption.service.EncryptionService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service implementation for managing all aspects of configuration artifacts,
 * including both inline JSON data and physical files.
 */
@Service
public class ArtifactServiceImpl implements ArtifactService {

    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    private final ArtifactRepository artifactRepository;
    private final EncryptionHelper encryptionService;
    private final EncryptionService coreEncryptionService;

    public ArtifactServiceImpl(ObjectMapper objectMapper,
            ArtifactRepository artifactRepository,
            EncryptionHelper encryptionService,
            EncryptionService coreEncryptionService,
            UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(AppConstants.ARTIFACT_SERVICE_LOGGER_NAME,
                AppConstants.ARTIFACT_SERVICE_LOGGER_CATEGORY);
        this.objectMapper = objectMapper;
        this.artifactRepository = artifactRepository;
        this.encryptionService = encryptionService;
        this.coreEncryptionService = coreEncryptionService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Artifact saveInlineArtifact(Configuration configuration, String artifactType, JsonNode jsonData,
            boolean encryptEntirely) throws ArtifactServiceException {
        try {
            Artifact artifact = findOrCreateArtifact(configuration, artifactType);
            artifact.setStorageType(Artifact.StorageType.INLINE);

            if (artifact.getFilePath() != null) {
                deletePhysicalFile(artifact.getFilePath());
                artifact.setFilePath(null);
                artifact.setFileName(null);
            }

            // Handle the 'isActive' state from the payload if it exists
            if (jsonData != null && jsonData.has(AppConstants.JSON_KEY_IS_ACTIVE)) {
                // Set the artifact's active state from the flag in the JSON data
                artifact.setIsActive(jsonData.get(AppConstants.JSON_KEY_IS_ACTIVE).asBoolean());
                // Remove the flag from the payload so it isn't stored redundantly
                if (jsonData instanceof ObjectNode) {
                    ((ObjectNode) jsonData).remove(AppConstants.JSON_KEY_IS_ACTIVE);
                }
            }

            // Delegate to the shared encryption service
            JsonNode encryptedData;
            try {
                encryptedData = encryptionService.encryptDataForSaving(
                        jsonData,
                        configuration.getConfigType(),
                        configuration.getConfigKey(),
                        encryptEntirely);
            } catch (Exception e) {
                throw new ArtifactEncryptionException(
                        configuration.getConfigType(),
                        configuration.getConfigKey(),
                        artifactType,
                        "encrypt",
                        e);
            }

            artifact.setArtifactData(encryptedData);
            artifact.setIsEncrypted(encryptionService.isDataEncrypted(encryptedData));

            return artifactRepository.save(artifact);
        } catch (ArtifactEncryptionException e) {
            // Re-throw encryption exceptions
            throw e;
        } catch (Exception e) {
            throw new ArtifactServiceException(
                    String.format("Failed to save inline artifact %s for configuration %s/%s",
                            artifactType, configuration.getConfigType(), configuration.getConfigKey()),
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Artifact saveFileArtifact(Configuration configuration, String artifactType, Object data) throws ArtifactServiceException {
        try {
            Artifact artifact = findOrCreateArtifact(configuration, artifactType);
            artifact.setStorageType(Artifact.StorageType.FILE);
            artifact.setArtifactData(null); // Clear any old inline data

            if (artifact.getFilePath() != null) {
                deletePhysicalFile(artifact.getFilePath());
            }

            boolean shouldEncrypt = EncryptionRulesConfig.shouldEncrypt(configuration.getConfigType(),
                    configuration.getConfigKey());
            String dataContent = data instanceof String ? (String) data : objectMapper.writeValueAsString(data);

            String contentToWrite;
            try {
                contentToWrite = shouldEncrypt
                        ? coreEncryptionService.encrypt(dataContent,
                                EncryptionRulesConfig.getKeyType(configuration.getConfigType()))
                        : dataContent;
            } catch (Exception e) {
                throw new ArtifactEncryptionException(
                        configuration.getConfigType(),
                        configuration.getConfigKey(),
                        artifactType,
                        "encrypt",
                        e);
            }

            Path filePath;
            try {
                filePath = prepareArtifactPath(configuration.getConfigType(), configuration.getConfigKey(), artifactType,
                        shouldEncrypt);
                Files.writeString(filePath, contentToWrite);
            } catch (IOException e) {
                throw new ArtifactFileException(
                        configuration.getConfigType(),
                        configuration.getConfigKey(),
                        artifactType,
                        "N/A",
                        "write",
                        e);
            }

            artifact.setFilePath(filePath.toString());
            artifact.setFileName(filePath.getFileName().toString());
            artifact.setIsEncrypted(shouldEncrypt);
            artifact.setIsActive(true); // File artifacts are considered active on save/update

            return artifactRepository.save(artifact);
        } catch (ArtifactEncryptionException | ArtifactFileException e) {
            // Re-throw specific exceptions
            throw e;
        } catch (Exception e) {
            throw new ArtifactServiceException(
                    String.format("Failed to save file artifact %s for configuration %s/%s",
                            artifactType, configuration.getConfigType(), configuration.getConfigKey()),
                    e);
        }
    }

    @Override
    @Transactional
    public Artifact saveBinaryFileArtifact(Configuration configuration, String artifactType, byte[] binary) throws ArtifactServiceException {
        try {
            Artifact artifact = findOrCreateArtifact(configuration, artifactType);
            artifact.setStorageType(Artifact.StorageType.FILE);
            artifact.setArtifactData(null);

            if (artifact.getFilePath() != null) {
                deletePhysicalFile(artifact.getFilePath());
            }

            boolean shouldEncrypt = EncryptionRulesConfig.shouldEncrypt(configuration.getConfigType(),
                    configuration.getConfigKey());

            byte[] bytesToWrite;
            if (shouldEncrypt && binary != null && binary.length > 0) {
                try {
                    // Encrypt as Base64 string then store encrypted string bytes to preserve decrypt path
                    String encrypted = coreEncryptionService.encrypt(java.util.Base64.getEncoder().encodeToString(binary),
                            EncryptionRulesConfig.getKeyType(configuration.getConfigType()));
                    bytesToWrite = encrypted.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception e) {
                    throw new ArtifactEncryptionException(
                            configuration.getConfigType(),
                            configuration.getConfigKey(),
                            artifactType,
                            "encrypt",
                            e);
                }
            } else {
                bytesToWrite = binary;
            }

            Path filePath;
            try {
                filePath = prepareArtifactPath(configuration.getConfigType(), configuration.getConfigKey(), artifactType,
                        shouldEncrypt);
                Files.write(filePath, bytesToWrite);
            } catch (IOException e) {
                throw new ArtifactFileException(
                        configuration.getConfigType(),
                        configuration.getConfigKey(),
                        artifactType,
                        "N/A",
                        "write",
                        e);
            }

            artifact.setFilePath(filePath.toString());
            artifact.setFileName(filePath.getFileName().toString());
            artifact.setIsEncrypted(shouldEncrypt);
            artifact.setIsActive(true);
            return artifactRepository.save(artifact);
        } catch (ArtifactEncryptionException | ArtifactFileException e) {
            throw e;
        } catch (Exception e) {
            throw new ArtifactServiceException(
                    String.format("Failed to save binary file artifact %s for configuration %s/%s",
                            artifactType, configuration.getConfigType(), configuration.getConfigKey()),
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<String> getArtifactContent(Artifact artifact) {
        if (artifact == null || !artifact.getIsActive() || artifact.getStorageType() != Artifact.StorageType.FILE
                || artifact.getFilePath() == null) {
            return Optional.empty();
        }
        try {
            Path path = Paths.get(artifact.getFilePath());
            byte[] fileBytes = Files.readAllBytes(path);

            // If encrypted: data is stored as encrypted string (possibly of Base64 original for binary)
            if (artifact.getIsEncrypted()) {
                try {
                    String encryptedString = new String(fileBytes, StandardCharsets.UTF_8);
                    String decrypted = coreEncryptionService.decrypt(encryptedString,
                            artifact.getConfiguration().getConfigType());
                    // For binary artifacts we encrypted Base64 of original bytes, so decrypted may itself be Base64.
                    return Optional.of(decrypted);
                } catch (Exception e) {
                    throw new ArtifactEncryptionException(
                            artifact.getConfiguration().getConfigType(),
                            artifact.getConfiguration().getConfigKey(),
                            artifact.getArtifactType(),
                            "decrypt",
                            e);
                }
            }

            // Not encrypted: attempt to decode as UTF-8 strictly; if fails treat as binary and return Base64
            try {
                CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
                CharBuffer cb = decoder.decode(ByteBuffer.wrap(fileBytes));
                String asText = cb.toString();
                // Heuristic: if text contains many replacement chars (�) treat as binary (shouldn't occur due to REPORT).
                return Optional.of(asText);
            } catch (Exception malformed) {
                String base64 = java.util.Base64.getEncoder().encodeToString(fileBytes);
                return Optional.of(base64);
            }
        } catch (ArtifactEncryptionException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e)
                    .message(String.format("Failed to read artifact file: %s", artifact.getFilePath()))
                    .log();
            throw new ArtifactFileException(
                    artifact.getConfiguration().getConfigType(),
                    artifact.getConfiguration().getConfigKey(),
                    artifact.getArtifactType(),
                    artifact.getFilePath(),
                    "read",
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> getArtifactBinaryContent(Artifact artifact) {
        if (artifact == null || !artifact.getIsActive() || artifact.getStorageType() != Artifact.StorageType.FILE
                || artifact.getFilePath() == null) {
            return Optional.empty();
        }
        try {
            Path path = Paths.get(artifact.getFilePath());
            byte[] rawBytes = Files.readAllBytes(path);
            if (artifact.getIsEncrypted()) {
                try {
                    String encryptedString = new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8);
                    String decrypted = coreEncryptionService.decrypt(encryptedString,
                            artifact.getConfiguration().getConfigType());
                    // Decrypted content for binary artifacts was originally Base64 encoded prior to encryption
                    return Optional.of(java.util.Base64.getDecoder().decode(decrypted));
                } catch (Exception e) {
                    throw new ArtifactEncryptionException(
                            artifact.getConfiguration().getConfigType(),
                            artifact.getConfiguration().getConfigKey(),
                            artifact.getArtifactType(),
                            "decrypt",
                            e);
                }
            }
            return Optional.of(rawBytes);
        } catch (ArtifactEncryptionException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e)
                    .message(String.format("Failed to read binary artifact file: %s", artifact.getFilePath()))
                    .log();
            throw new ArtifactFileException(
                    artifact.getConfiguration().getConfigType(),
                    artifact.getConfiguration().getConfigKey(),
                    artifact.getArtifactType(),
                    artifact.getFilePath(),
                    "read",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<JsonNode> getArtifactInlineContent(Artifact artifact) {
        if (artifact == null || !artifact.getIsActive() || artifact.getStorageType() != Artifact.StorageType.INLINE
                || artifact.getArtifactData() == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(encryptionService.decryptInlineData(
                    artifact.getArtifactData(),
                    artifact.getConfiguration().getConfigType(),
                    artifact.getConfiguration().getConfigKey()));
        } catch (Exception e) {
            logger.error(e).message("Failed to decrypt inline artifact data for type: " + artifact.getArtifactType())
                    .log();
            throw new ArtifactEncryptionException(
                    artifact.getConfiguration().getConfigType(),
                    artifact.getConfiguration().getConfigKey(),
                    artifact.getArtifactType(),
                    "decrypt",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public boolean deleteArtifact(Configuration configuration, String artifactType) {
        Optional<Artifact> artifactOpt = configuration.getArtifacts().stream()
                .filter(a -> artifactType.equals(a.getArtifactType()))
                .findFirst();

        if (artifactOpt.isPresent()) {
            Artifact artifact = artifactOpt.get();
            if (artifact.getStorageType() == Artifact.StorageType.FILE && artifact.getFilePath() != null) {
                deletePhysicalFile(artifact.getFilePath());
            }
            configuration.getArtifacts().remove(artifact);
            logger.info().message("Deleted artifact.")
                    .details(Map.of("configKey", configuration.getConfigKey(), "artifactType", artifactType))
                    .log();
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getRelatedArtifactsData(Configuration configuration) {
        Map<String, Object> artifactsMap = new HashMap<>();
        if (configuration.getArtifacts() == null) {
            return artifactsMap;
        }
        for (Artifact artifact : configuration.getArtifacts()) {
            if (!artifact.getIsActive())
                continue;

            Map<String, Object> artifactDetails = new HashMap<>();
            artifactDetails.put("storageType", artifact.getStorageType().name());
            artifactDetails.put("isEncrypted", artifact.getIsEncrypted());
            artifactDetails.put("isActive", artifact.getIsActive());

            if (artifact.getStorageType() == Artifact.StorageType.FILE) {
                artifactDetails.put("filePath", artifact.getFilePath());
                artifactDetails.put("fileName", artifact.getFileName());
                Optional<String> contentOpt = getArtifactContent(artifact);
                if (contentOpt.isPresent()) {
                    String c = contentOpt.get();
                    // If not encrypted and not valid UTF-8 we would have Base64 encoded; mark it
                    boolean looksBase64 = c.matches("[A-Za-z0-9+/=\\r\\n]+") && c.length() % 4 == 0;
                    if (looksBase64) {
                        artifactDetails.put("base64EncodedBinary", true);
                    }
                    artifactDetails.put("content", c);
                }
            } else { // INLINE
                getArtifactInlineContent(artifact)
                        .ifPresent(json -> artifactDetails.put("content", json));
            }
            artifactsMap.put(artifact.getArtifactType(), artifactDetails);
        }
        return artifactsMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAllPhysicalArtifacts(Configuration configuration) {
        if (configuration.getArtifacts() != null) {
            configuration.getArtifacts().stream()
                    .filter(a -> a.getStorageType() == Artifact.StorageType.FILE)
                    .forEach(artifact -> deletePhysicalFile(artifact.getFilePath()));
        }
    }

    /**
     * Finds or creates an artifact of the specified type for the given
     * configuration.
     *
     * @param configuration the parent Configuration entity
     * @param artifactType  the unique type of the artifact to find or create
     * @return the found or newly created Artifact entity
     */
    private Artifact findOrCreateArtifact(Configuration configuration, String artifactType) {
        return configuration.getArtifacts().stream()
                .filter(a -> artifactType.equals(a.getArtifactType()))
                .findFirst()
                .orElseGet(() -> {
                    Artifact newArtifact = new Artifact();
                    newArtifact.setArtifactType(artifactType);
                    newArtifact.setConfiguration(configuration);
                    configuration.getArtifacts().add(newArtifact);
                    return newArtifact;
                });
    }

    private Path prepareArtifactPath(String configType, String configKey, String artifactType, boolean isEncrypted)
            throws IOException {
        String basePath = String.format(AppConstants.ARTIFACT_BASE_PATH_TEMPLATE, configType.toLowerCase(),
                configKey.toLowerCase());
        Path baseDir = Paths.get(basePath);
        Files.createDirectories(baseDir);
        String fileName = artifactType + (isEncrypted ? AppConstants.ARTIFACT_ENCRYPTED_EXTENSION : "");
        return baseDir.resolve(fileName);
    }

    /**
     * Deletes a physical file from the filesystem.
     *
     * @param filePath the path of the file to delete
     */
    private void deletePhysicalFile(String filePath) {
        if (filePath == null || filePath.isBlank())
            return;
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info().message(String.format(AppConstants.LOG_ARTIFACT_DELETED, path)).log();
            }
        } catch (IOException e) {
            logger.warn().message(String.format(AppConstants.LOG_ARTIFACT_DELETE_FAILED, filePath, e.getMessage()))
                    .log();
            // Don't throw an exception here as this is often called during cleanup
            // and shouldn't break the main operation
        }
    }
}