package com.aidentx.iam.shadow_management.repository;

import com.aidentx.iam.shadow_management.model.entity.ShadowAttributeChangelog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for shadow attribute changelog entries.
 * Provides queries for audit trail retrieval and compliance reporting.
 */
@Repository
public interface ShadowAttributeChangelogRepository extends JpaRepository<ShadowAttributeChangelog, UUID> {

    /**
     * Finds all changelog entries for a specific shadow, ordered by most recent first.
     */
    Page<ShadowAttributeChangelog> findByShadowIdOrderByChangedAtDesc(UUID shadowId, Pageable pageable);

    /**
     * Finds all changelog entries for a specific shadow and field.
     */
    List<ShadowAttributeChangelog> findByShadowIdAndFieldNameOrderByChangedAtDesc(
            UUID shadowId, String fieldName);

    /**
     * Finds all changelog entries for a specific sync run within a tenant.
     */
    Page<ShadowAttributeChangelog> findByTenantIdAndSyncRunIdOrderByChangedAtDesc(
            String tenantId, String syncRunId, Pageable pageable);

    /**
     * Finds all changelog entries for a specific resource within a tenant.
     */
    Page<ShadowAttributeChangelog> findByTenantIdAndResourceIdOrderByChangedAtDesc(
            String tenantId, String resourceId, Pageable pageable);

    /**
     * Counts changes per sync run for a shadow (useful for drift analysis).
     */
    long countByShadowIdAndSyncRunId(UUID shadowId, String syncRunId);

    /** Delete all changelog entries for a resource within a tenant (cascade delete on application removal). */
    @Modifying
    @Query("DELETE FROM ShadowAttributeChangelog c WHERE c.resourceId = :resourceId AND c.tenantId = :tenantId")
    void deleteAllByResourceIdAndTenantId(@Param("resourceId") String resourceId, @Param("tenantId") String tenantId);
}
