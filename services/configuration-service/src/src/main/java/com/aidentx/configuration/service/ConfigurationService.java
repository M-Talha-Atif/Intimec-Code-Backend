package com.aidentx.configuration.service;

import com.aidentx.configuration.config.ConfigurationType;
import com.aidentx.configuration.dto.ConfigurationDataWithMetadata;
import com.aidentx.configuration.dto.ConfigurationPatchRequest;
import com.aidentx.configuration.dto.ConfigurationSummary;
import com.aidentx.configuration.exception.configuration.ConfigurationEncryptionException;
import com.aidentx.configuration.exception.configuration.ConfigurationNotFoundException;
import com.aidentx.configuration.exception.configuration.ConfigurationPatchException;
import com.aidentx.configuration.exception.configuration.ConfigurationServiceException;
import com.aidentx.configuration.model.entity.Configuration;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing application configurations.
 * <p>
 * Provides a comprehensive API for the lifecycle of {@link Configuration}
 * entities,
 * including their primary {@code configData} JSON payload. It delegates the
 * management of
 * supplementary, categorized artifacts to the {@link ArtifactService}.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public interface ConfigurationService {

        /**
         * Retrieves a configuration entity by its type, key and tenant.
         *
         * @param configType The configuration type.
         * @param configKey  The configuration key.
         * @param tenantId   The tenant identifier.
         * @return An {@link Optional} containing the {@link Configuration} entity if
         *         found, otherwise empty.
         */
        Optional<Configuration> getConfiguration(ConfigurationType configType, String configKey, String tenantId);

        /**
         * Retrieves the primary configuration data along with its encryption metadata.
         *
         * @param configType The configuration type.
         * @param configKey  The configuration key.
         * @param tenantId   The tenant identifier.
         * @return An {@link Optional} containing the
         *         {@link ConfigurationDataWithMetadata} if found.
         */
        Optional<ConfigurationDataWithMetadata> getConfigurationDataWithMetadata(ConfigurationType configType,
                        String configKey, String tenantId);

        /**
         * Retrieves the decrypted primary configuration data (from the
         * {@code configData} field) as a {@link JsonNode}.
         *
         * @param configType The configuration type.
         * @param configKey  The configuration key.
         * @param tenantId   The tenant identifier.
         * @return An {@link Optional} containing the configuration data if found,
         *         otherwise empty.
         * @throws ConfigurationEncryptionException if decryption fails
         */
        Optional<JsonNode> getConfigurationData(ConfigurationType configType, String configKey, String tenantId) throws ConfigurationEncryptionException;

        /**
         * Retrieves summaries for all configuration entities of a specific type.
         *
         * @param configType The configuration type.
         * @return A {@link List} of {@link ConfigurationSummary} objects.
         */
        List<ConfigurationSummary> getConfigurationsByType(ConfigurationType configType);

        /**
         * Retrieves summaries for all configurations in the system.
         *
         * @return A {@link List} of all {@link ConfigurationSummary} objects.
         */
        List<ConfigurationSummary> getAllConfigurations();

        /**
         * Saves or updates the primary {@code configData} for a configuration.
         * If the configuration does not exist, it will be created.
         *
         * @param configType          The configuration type.
         * @param configKey           The configuration key.
         * @param data                The JSON data to save.
         * @param encryptEntireConfig If true, the entire JSON is encrypted as a single
         *                            block.
         * @return The saved {@link Configuration} entity.
         * @throws ConfigurationServiceException if saving fails
         */
        Configuration saveConfiguration(ConfigurationType configType, String configKey, String tenantId, JsonNode data,
                        boolean encryptEntireConfig) throws ConfigurationServiceException;

        /**
         * Partially updates an existing configuration's {@code configData}. This method
         * is ideal for
         * updating specific fields, toggling the active state, or changing the
         * encryption mode without
         * replacing the entire configuration object. The configuration version will be
         * incremented.
         *
         * @param configType   The configuration type.
         * @param configKey    The configuration key.
         * @param patchRequest A {@link ConfigurationPatchRequest} DTO containing the
         *                     updates to apply.
         * @return The updated {@link Configuration} entity.
         * @throws ConfigurationNotFoundException if the configuration to be patched is not found
         * @throws ConfigurationPatchException if the patch operation fails
         */
        Configuration partialUpdateConfiguration(ConfigurationType configType, String configKey, String tenantId,
                        ConfigurationPatchRequest patchRequest) throws ConfigurationNotFoundException, ConfigurationPatchException;

        /**
         * Deletes a configuration and all of its associated artifacts.
         *
         * @param configType The configuration type.
         * @param configKey  The configuration key.
         * @return true if the configuration was found and deleted, false otherwise.
         */
        boolean deleteConfiguration(ConfigurationType configType, String configKey, String tenantId);

        /**
         * Checks if a configuration exists for the given type and key.
         *
         * @param configType The configuration type.
         * @param configKey  The configuration key.
         * @return true if the configuration exists, false otherwise.
         */
        boolean configurationExists(ConfigurationType configType, String configKey, String tenantId);

        /**
         * Retrieves all supplementary artifacts associated with a configuration.
         *
         * @param configType The configuration type.
         * @param configKey  The configuration key.
         * @return A {@link Map} where keys are artifact types and values are their
         *         content and metadata.
         */
        Map<String, Object> getRelatedArtifacts(ConfigurationType configType, String configKey, String tenantId);
}