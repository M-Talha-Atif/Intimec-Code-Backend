package com.aidentx.configuration.dto;

import com.aidentx.configuration.model.entity.Artifact;
import com.aidentx.configuration.model.entity.Configuration;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A Data Transfer Object (DTO) representing a summary of a Configuration.
 * <p>
 * This DTO is used to avoid LazyInitializationExceptions by transforming the
 * Configuration entity within a transactional context. It includes the main
 * configuration data and a list of the types of its associated artifacts,
 * rather than the full artifact objects.
 * </p>
 *
 * @param id            The unique identifier of the configuration.
 * @param configType    The type/category of the configuration.
 * @param configKey     The key or identifier for the configuration.
 * @param configData    The main configuration data (decrypted if necessary).
 * @param isActive      Whether the configuration is currently active.
 * @param version       The version number of the configuration.
 * @param createdAt     The creation timestamp.
 * @param updatedAt     The last update timestamp.
 * @param artifactTypes A list of strings representing the types of associated
 *                      artifacts.
 */
public record ConfigurationSummary(
        UUID id,
        String configType,
        String configKey,
        JsonNode configData,
        Boolean isActive,
        Integer version,
        Instant createdAt,
        Instant updatedAt,
    List<String> artifactTypes,
    String tenantId) {
    /**
     * Factory method to create a ConfigurationSummary from a Configuration entity.
     * This should be called within a transactional context to ensure artifacts can
     * be accessed.
     *
     * @param configuration The Configuration entity to convert.
     * @return A new ConfigurationSummary instance.
     */
    public static ConfigurationSummary fromEntity(Configuration configuration) {
        if (configuration == null) {
            return null;
        }

        List<String> types = (configuration.getArtifacts() == null || configuration.getArtifacts().isEmpty())
                ? Collections.emptyList()
                : configuration.getArtifacts().stream()
                        .map(Artifact::getArtifactType)
                        .collect(Collectors.toList());

    return new ConfigurationSummary(
                configuration.getId(),
                configuration.getConfigType(),
                configuration.getConfigKey(),
                configuration.getConfigData(),
                configuration.getIsActive(),
                configuration.getVersion(),
                configuration.getCreatedAt(),
                configuration.getUpdatedAt(),
        types,
        configuration.getTenantId());
    }
}