package com.aidentx.configuration.repository;

import com.aidentx.configuration.model.entity.Artifact;
import com.aidentx.configuration.model.entity.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link Artifact} entities.
 * Provides database operations related to configuration artifacts.
 */
@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {

    /**
     * Retrieves all artifacts associated with the specified configuration.
     *
     * @param configuration the configuration entity
     * @return list of artifacts linked to the configuration
     */
    List<Artifact> findByConfiguration(Configuration configuration);

    /**
     * Retrieves all artifacts associated with the configuration identified by the given ID.
     *
     * @param configurationId the UUID of the configuration
     * @return list of artifacts for the specified configuration ID
     */
    List<Artifact> findByConfigurationId(UUID configurationId);

    /**
     * Retrieves all active (enabled) artifacts associated with the given configuration.
     *
     * @param configuration the configuration entity
     * @return list of active artifacts linked to the configuration
     */
    List<Artifact> findByConfigurationAndIsActiveTrue(Configuration configuration);

    /**
     * Retrieves all artifacts for a specific configuration and artifact type.
     *
     * @param configuration the configuration entity
     * @param artifactType  the type of artifact to filter by
     * @return list of matching artifacts
     */
    List<Artifact> findByConfigurationAndArtifactType(Configuration configuration, String artifactType);

    /**
     * Retrieves a single active artifact by configuration and artifact type.
     *
     * @param configuration the configuration entity
     * @param artifactType  the type of artifact
     * @return optional containing the artifact if found
     */
    Optional<Artifact> findByConfigurationAndArtifactTypeAndIsActiveTrue(Configuration configuration, String artifactType);

    /**
     * Retrieves artifacts by configuration type and configuration key.
     *
     * @param configType the type of configuration
     * @param configKey  the key of the configuration
     * @return list of artifacts matching the criteria
     */
    @Query("SELECT a FROM Artifact a WHERE a.configuration.configType = :configType AND a.configuration.configKey = :configKey")
    List<Artifact> findByConfigurationTypeAndKey(@Param("configType") String configType, @Param("configKey") String configKey);

    /**
     * Retrieves active artifacts by configuration type and key.
     *
     * @param configType the type of configuration
     * @param configKey  the key of the configuration
     * @return list of active artifacts matching the criteria
     */
    @Query("SELECT a FROM Artifact a WHERE a.configuration.configType = :configType AND a.configuration.configKey = :configKey AND a.isActive = true")
    List<Artifact> findActiveByConfigurationTypeAndKey(@Param("configType") String configType, @Param("configKey") String configKey);

    /**
     * Retrieves a specific active artifact by configuration type, key, and artifact type.
     *
     * @param configType   the type of configuration
     * @param configKey    the key of the configuration
     * @param artifactType the artifact type to filter by
     * @return optional containing the matching artifact if found
     */
    @Query("SELECT a FROM Artifact a WHERE a.configuration.configType = :configType AND a.configuration.configKey = :configKey AND a.artifactType = :artifactType AND a.isActive = true")
    Optional<Artifact> findByConfigurationTypeKeyAndArtifactType(@Param("configType") String configType, @Param("configKey") String configKey, @Param("artifactType") String artifactType);

    /**
     * Retrieves all artifacts that match the specified artifact type.
     *
     * @param artifactType the artifact type
     * @return list of artifacts of the specified type
     */
    List<Artifact> findByArtifactType(String artifactType);

    /**
     * Retrieves all active artifacts of the given artifact type.
     *
     * @param artifactType the artifact type
     * @return list of active artifacts matching the type
     */
    List<Artifact> findByArtifactTypeAndIsActiveTrue(String artifactType);

    /**
     * Counts the number of artifacts associated with the given configuration.
     *
     * @param configuration the configuration entity
     * @return the number of artifacts
     */
    long countByConfiguration(Configuration configuration);

    /**
     * Counts the number of active artifacts associated with the given configuration.
     *
     * @param configuration the configuration entity
     * @return the number of active artifacts
     */
    long countByConfigurationAndIsActiveTrue(Configuration configuration);

    /**
     * Checks whether an active artifact exists for a given configuration and artifact type.
     *
     * @param configuration the configuration entity
     * @param artifactType  the artifact type to check
     * @return true if such an artifact exists and is active, false otherwise
     */
    boolean existsByConfigurationAndArtifactTypeAndIsActiveTrue(Configuration configuration, String artifactType);
}
