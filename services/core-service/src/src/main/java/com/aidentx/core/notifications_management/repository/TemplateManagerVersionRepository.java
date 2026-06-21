package com.aidentx.core.notifications_management.repository;

import com.aidentx.core.notifications_management.model.entity.TemplateManagerVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TemplateManagerVersion entity operations.
 * <p>
 * This repository provides methods to manage template version history,
 * including retrieving version histories, finding specific versions,
 * and managing current version flags.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-09-18
 */
@Repository
public interface TemplateManagerVersionRepository extends JpaRepository<TemplateManagerVersion, UUID> {
    
    /**
     * Finds all versions for a specific template and tenant, ordered by version number descending.
     * 
     * @param templateId the template ID
     * @param tenantId the tenant ID
     * @return list of template versions ordered by version number (newest first)
     */
    List<TemplateManagerVersion> findByTemplateIdAndTenantIdOrderByVersionNumberDesc(UUID templateId, String tenantId);
    
    /**
     * Finds the current active version for a specific template and tenant.
     * 
     * @param templateId the template ID
     * @param tenantId the tenant ID
     * @param isCurrent the current flag (should be true)
     * @return optional containing the current version if found
     */
    Optional<TemplateManagerVersion> findByTemplateIdAndTenantIdAndIsCurrent(UUID templateId, String tenantId, Boolean isCurrent);
    
    /**
     * Finds a specific version number for a template and tenant.
     * 
     * @param templateId the template ID
     * @param tenantId the tenant ID
     * @param versionNumber the version number
     * @return optional containing the version if found
     */
    Optional<TemplateManagerVersion> findByTemplateIdAndTenantIdAndVersionNumber(UUID templateId, String tenantId, Integer versionNumber);
    
    /**
     * Finds the maximum version number for a specific template and tenant.
     * 
     * @param templateId the template ID
     * @param tenantId the tenant ID
     * @return optional containing the maximum version number if any versions exist
     */
    @Query("SELECT MAX(v.versionNumber) FROM TemplateManagerVersion v WHERE v.templateId = :templateId AND v.tenantId = :tenantId")
    Optional<Integer> findMaxVersionNumber(@Param("templateId") UUID templateId, @Param("tenantId") String tenantId);
    
    /**
     * Sets all versions of a template to not current (used before setting a new current version).
     * 
     * @param templateId the template ID
     * @param tenantId the tenant ID
     */
    @Modifying
    @Query("UPDATE TemplateManagerVersion v SET v.isCurrent = false WHERE v.templateId = :templateId AND v.tenantId = :tenantId")
    void markAllVersionsAsNotCurrent(@Param("templateId") UUID templateId, @Param("tenantId") String tenantId);
    
    /**
     * Finds all versions for templates belonging to a specific tenant.
     * Used for admin operations or bulk tenant operations.
     * 
     * @param tenantId the tenant ID
     * @return list of all template versions for the tenant
     */
    List<TemplateManagerVersion> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    
    /**
     * Counts the total number of versions for a specific template and tenant.
     * 
     * @param templateId the template ID
     * @param tenantId the tenant ID
     * @return the total number of versions
     */
    long countByTemplateIdAndTenantId(UUID templateId, String tenantId);
    
    /**
     * Deletes all versions for a specific template and tenant.
     * Used when a template is permanently deleted.
     * 
     * @param templateId the template ID
     * @param tenantId the tenant ID
     */
    void deleteByTemplateIdAndTenantId(UUID templateId, String tenantId);
    
    /**
     * Finds versions created within a specific time range for a tenant.
     * Useful for audit trails and reporting.
     * 
     * @param tenantId the tenant ID
     * @param startTime the start timestamp (epoch time)
     * @param endTime the end timestamp (epoch time)
     * @return list of versions created within the time range
     */
    @Query("SELECT v FROM TemplateManagerVersion v WHERE v.tenantId = :tenantId AND v.createdAt BETWEEN :startTime AND :endTime ORDER BY v.createdAt DESC")
    List<TemplateManagerVersion> findByTenantIdAndCreatedAtBetween(@Param("tenantId") String tenantId, 
                                                                   @Param("startTime") Long startTime, 
                                                                   @Param("endTime") Long endTime);
}