package com.aidentx.configuration.repository;

import com.aidentx.configuration.model.entity.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for {@link Configuration} entity operations.
 */
@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, UUID> {

    /**
     * Find a configuration by type, key and tenant.
     *
     * @param configType the configuration type
     * @param configKey  the configuration key
     * @param tenantId   the tenant identifier
     * @return an Optional containing the configuration if found, or empty otherwise
     */
    Configuration findByConfigTypeAndConfigKeyAndTenantId(String configType, String configKey, String tenantId);

    /**
     * Find all configurations by type, eagerly fetching their associated artifacts to prevent LazyInitializationException.
     *
     * @param configType the configuration type
     * @return a list of configurations of the specified type with artifacts initialized
     */
    @Query("SELECT DISTINCT c FROM Configuration c LEFT JOIN FETCH c.artifacts WHERE c.configType = :configType")
    List<Configuration> findByConfigTypeWithArtifacts(@Param("configType") String configType);

    /**
     * Find all configurations, eagerly fetching their associated artifacts to prevent LazyInitializationException.
     *
     * @return a list of all configurations with artifacts initialized
     */
    @Query("SELECT DISTINCT c FROM Configuration c LEFT JOIN FETCH c.artifacts")
    List<Configuration> findAllWithArtifacts();


    /**
     * Find all active configurations by type.
     *
     * @param configType the configuration type
     * @return a list of active configurations of the specified type
     */
    List<Configuration> findByConfigTypeAndIsActiveTrue(String configType);

    /**
     * Find all active configurations.
     *
     * @return a list of active configurations
     */
    List<Configuration> findByIsActiveTrue();

    /**
     * Check if a configuration exists by type, key and tenantId.
     *
     * @param configType the configuration type
     * @param configKey  the configuration key
     * @param tenantId   the tenant identifier
     * @return true if a configuration exists, false otherwise
     */
    boolean existsByConfigTypeAndConfigKeyAndTenantId(String configType, String configKey, String tenantId);
}