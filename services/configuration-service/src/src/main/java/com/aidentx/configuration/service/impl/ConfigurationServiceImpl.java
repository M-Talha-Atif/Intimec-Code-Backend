package com.aidentx.configuration.service.impl;

import com.aidentx.configuration.config.AppConstants;
import com.aidentx.configuration.config.ConfigurationType;
import com.aidentx.configuration.dto.ConfigurationDataWithMetadata;
import com.aidentx.configuration.dto.ConfigurationPatchRequest;
import com.aidentx.configuration.dto.ConfigurationSummary;
import com.aidentx.configuration.dto.EncryptionMetadata;
import com.aidentx.configuration.exception.configuration.*;
import com.aidentx.configuration.model.entity.Configuration;
import com.aidentx.configuration.repository.ConfigurationRepository;
import com.aidentx.configuration.service.ArtifactService;
import com.aidentx.configuration.service.ConfigurationService;
import com.aidentx.configuration.service.util.ConfigurationMergeUtil;
import com.aidentx.configuration.service.util.EncryptionHelper;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of
 * {@link ConfigurationService}.
 * <p>
 * Handles the business logic for the lifecycle of {@link Configuration}
 * entities and their
 * primary {@code configData}, including persistence and encryption. It
 * collaborates with the
 * {@link ArtifactService} for operations involving supplementary artifacts.
 * </p>
 */
@Service
public class ConfigurationServiceImpl implements ConfigurationService {

    private final IUnifiedLogger logger;
    private final ConfigurationRepository configurationRepository;
    private final ArtifactService artifactService;
    private final EncryptionHelper encryptionService;
    private final ObjectMapper objectMapper;

    public ConfigurationServiceImpl(ConfigurationRepository configurationRepository,
            ArtifactService artifactService,
            EncryptionHelper encryptionService,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.configurationRepository = configurationRepository;
        this.artifactService = artifactService;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.logger = unifiedLoggerFactory.getLogger("ConfigurationServiceImpl", AppConstants.LOG_CATEGORY_SERVICE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Configuration partialUpdateConfiguration(ConfigurationType configType, String configKey, String tenantId,
            ConfigurationPatchRequest patchRequest) {
        String typeValue = configType.getValue();
        logger.info().message(String.format(AppConstants.LOG_PATCH_STARTED_FORMAT, typeValue, configKey,
                patchRequest.isEncryptEntireConfig()));

        Configuration configuration = Optional.ofNullable(
                configurationRepository.findByConfigTypeAndConfigKeyAndTenantId(typeValue, configKey, tenantId))
                .orElseThrow(() -> ConfigurationExceptionUtils.configurationNotFound(configType, configKey));

        int previousVersion = configuration.getVersion();

        try {
            // Decrypt the current data to prepare for merging
            JsonNode currentData = configuration.getConfigData() != null
                    ? encryptionService.decryptInlineData(configuration.getConfigData(), typeValue, configKey)
                    : objectMapper.createObjectNode();

            JsonNode dataToSave;

            // Merge the patch data from the request, if it exists
            if (patchRequest.getData() != null) {
                JsonNode patchData = objectMapper.valueToTree(patchRequest.getData());
                dataToSave = ConfigurationMergeUtil.merge(currentData, patchData);
            } else {
                dataToSave = currentData;
            }

            // Handle the 'isActive' flag from the patch request DTO
            if (patchRequest.getIsActive() != null) {
                configuration.setIsActive(patchRequest.getIsActive());
                // Remove isActive from the JSON data if it's an object node
                if (dataToSave instanceof ObjectNode) {
                    ((ObjectNode) dataToSave).remove(AppConstants.JSON_KEY_IS_ACTIVE);
                }
            }

            // Always enforce tenant from parameter and keep tenantId out of JSON
            configuration.setTenantId(tenantId);
            if (dataToSave instanceof ObjectNode) {
                ((ObjectNode) dataToSave).remove("tenantId");
            }

            JsonNode finalEncryptedData = encryptionService.encryptDataForSaving(
                    dataToSave,
                    typeValue,
                    configKey,
                    patchRequest.isEncryptEntireConfig());

            configuration.setConfigData(finalEncryptedData);
            configuration.setVersion(previousVersion + 1);
            Configuration saved = configurationRepository.save(configuration);

            boolean isEncrypted = encryptionService.isDataEncrypted(saved.getConfigData());
            String details = String.format(AppConstants.AUDIT_PATCH_DETAILS_FORMAT, typeValue, configKey,
                    previousVersion, saved.getVersion(), isEncrypted);

            logger.info().message(details)
                    .details(Map.of(AppConstants.DETAIL_CONFIG_TYPE, typeValue, AppConstants.DETAIL_CONFIG_KEY,
                            configKey,
                            AppConstants.DETAIL_NEW_VERSION, saved.getVersion(), "isEncrypted", isEncrypted))
                    .log();

            return saved;
        } catch (ConfigurationNotFoundException e) {
            // Re-throw configuration not found exceptions
            throw e;
        } catch (Exception e) {
            logger.error(e).message(
                    String.format(AppConstants.ERROR_PATCH_DATA_PROCESSING_FAILED_FORMAT, typeValue, configKey));
            throw new ConfigurationPatchException(configType.getValue(), configKey,
                    "Failed to process patch data", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Configuration saveConfiguration(ConfigurationType configType, String configKey, String tenantId, JsonNode data,
            boolean encryptEntireConfig) {
        String typeValue = configType.getValue();
        try {
            Configuration config = Optional.ofNullable(
                    configurationRepository.findByConfigTypeAndConfigKeyAndTenantId(typeValue, configKey, tenantId))
                    .orElse(new Configuration(typeValue, configKey));

            if (config.getId() != null) {
                config.setVersion(config.getVersion() + 1);
            }

            // Handle the 'isActive' state from the payload if it exists
            if (data != null && data.has(AppConstants.JSON_KEY_IS_ACTIVE)) {
                // Set the entity's state from the flag in the JSON data
                config.setIsActive(data.get(AppConstants.JSON_KEY_IS_ACTIVE).asBoolean());
                // Remove isActive from the JSON data if it's an object node
                if (data instanceof ObjectNode) {
                    ((ObjectNode) data).remove(AppConstants.JSON_KEY_IS_ACTIVE);
                }
            }

            // Enforce tenantId parameter and keep it as a column, not inside JSON
            config.setTenantId(tenantId);
            if (data instanceof ObjectNode) {
                ((ObjectNode) data).remove("tenantId");
            }

            JsonNode finalData = encryptionService.encryptDataForSaving(data, typeValue, configKey,
                    encryptEntireConfig);
            config.setConfigData(finalData);

            logger.info().category(AppConstants.LOG_CATEGORY_SERVICE).message("Saving main configuration data")
                    .details(Map.of(AppConstants.DETAIL_CONFIG_TYPE, typeValue, AppConstants.DETAIL_CONFIG_KEY,
                            configKey, AppConstants.DETAIL_NEW_VERSION, config.getVersion()))
                    .log();

            return configurationRepository.save(config);
        } catch (Exception e) {
            logger.error(e).message(String.format("Failed to save main configuration for %s/%s", typeValue, configKey))
                    .log();
            throw new ConfigurationServiceException("Failed to save configuration", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public boolean deleteConfiguration(ConfigurationType configType, String configKey, String tenantId) {
        String typeValue = configType.getValue();
        return Optional.ofNullable(
                configurationRepository.findByConfigTypeAndConfigKeyAndTenantId(typeValue, configKey, tenantId))
                .map(config -> {
                    artifactService.deleteAllPhysicalArtifacts(config);
                    configurationRepository.delete(config);
                    logger.info().message(String.format(AppConstants.LOG_DELETED_CONFIG_FORMAT, typeValue, configKey))
                            .log();
                    return true;
                }).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Configuration> getConfiguration(ConfigurationType configType, String configKey, String tenantId) {
        return Optional.ofNullable(
                configurationRepository.findByConfigTypeAndConfigKeyAndTenantId(configType.getValue(), configKey,
                        tenantId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ConfigurationSummary> getConfigurationsByType(ConfigurationType configType) {
        List<Configuration> configs = configurationRepository.findByConfigTypeWithArtifacts(configType.getValue());
        return configs.stream()
                .map(ConfigurationSummary::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ConfigurationSummary> getAllConfigurations() {
        List<Configuration> configs = configurationRepository.findAllWithArtifacts();
        return configs.stream()
                .map(ConfigurationSummary::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public boolean configurationExists(ConfigurationType configType, String configKey, String tenantId) {
        return configurationRepository.existsByConfigTypeAndConfigKeyAndTenantId(configType.getValue(), configKey,
                tenantId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<JsonNode> getConfigurationData(ConfigurationType configType, String configKey, String tenantId) {
        String typeValue = configType.getValue();
    return getConfiguration(configType, configKey, tenantId)
                .flatMap(config -> {
                    try {
                        return Optional.ofNullable(
                                encryptionService.decryptInlineData(config.getConfigData(), typeValue, configKey));
                    } catch (Exception e) {
                        logger.error(e).message(
                                String.format(AppConstants.ERROR_DECRYPTING_DATA_FAILED_FORMAT, typeValue, configKey));
                        throw new ConfigurationEncryptionException(typeValue, configKey, "decrypt", e);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<ConfigurationDataWithMetadata> getConfigurationDataWithMetadata(ConfigurationType configType,
            String configKey, String tenantId) {
    return getConfiguration(configType, configKey, tenantId)
        .map(config -> {
                    try {
                        String typeValue = configType.getValue();
                        JsonNode data = encryptionService.decryptInlineData(config.getConfigData(), typeValue,
                                configKey);
                        EncryptionMetadata metadata = encryptionService.getEncryptionMetadata(config.getConfigType());
                        boolean isEncrypted = encryptionService.isDataEncrypted(config.getConfigData());
                        Instant updatedAt = config.getUpdatedAt();
                        return new ConfigurationDataWithMetadata(data, metadata, isEncrypted, updatedAt);
                    } catch (Exception e) {
                        throw new ConfigurationEncryptionException(configType.getValue(), configKey, "decrypt", e);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRelatedArtifacts(ConfigurationType configType, String configKey, String tenantId) {
        return getConfiguration(configType, configKey, tenantId)
                .map(artifactService::getRelatedArtifactsData)
                .orElse(Map.of());
    }
}