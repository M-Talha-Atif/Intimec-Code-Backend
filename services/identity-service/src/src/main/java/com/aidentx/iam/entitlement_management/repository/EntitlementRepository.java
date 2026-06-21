package com.aidentx.iam.entitlement_management.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, UUID>,
        JpaSpecificationExecutor<Entitlement> {

    /**
     * Finds an entitlement by its unique natural key (resource + source UID + tenant).
     */
    Optional<Entitlement> findByResourceIdAndSourceUidAndTenantId(UUID resourceId, String sourceUid, String tenantId);

    /**
     * Legacy lookup without tenant isolation — kept for backward compatibility.
     */
    Optional<Entitlement> findByResourceIdAndSourceUid(UUID resourceId, String sourceUid);

    /**
     * Finds all entitlements for a given resource and tenant (paginated).
     */
    Page<Entitlement> findByResourceIdAndTenantId(UUID resourceId, String tenantId, Pageable pageable);

    /**
     * Finds entitlements that were NOT seen in the current sync run.
     * These are candidates for deletion detection during reconciliation —
     * they existed in the catalog but the connector did not return them.
     * Uses paginated results to avoid loading all absent entitlements into memory.
     */
    @Query("SELECT e FROM Entitlement e WHERE e.resourceId = :resourceId AND e.tenantId = :tenantId "
            + "AND (e.lastSyncRunId IS NULL OR e.lastSyncRunId <> :syncRunId)")
    Page<Entitlement> findAbsentEntitlements(
            @Param("resourceId") UUID resourceId,
            @Param("tenantId") String tenantId,
            @Param("syncRunId") String syncRunId,
            Pageable pageable);

    /**
     * Returns distinct values for a given column, used for building filter dropdowns.
     * E.g., all distinct riskLevel values for entitlements of a given resource.
     */
    @Query("SELECT DISTINCT e.riskLevel FROM Entitlement e WHERE e.resourceId = :resourceId "
            + "AND e.tenantId = :tenantId AND e.riskLevel IS NOT NULL")
    List<String> findDistinctRiskLevels(@Param("resourceId") UUID resourceId,
                                        @Param("tenantId") String tenantId);

    @Query("SELECT DISTINCT e.lifecycle FROM Entitlement e WHERE e.resourceId = :resourceId "
            + "AND e.tenantId = :tenantId AND e.lifecycle IS NOT NULL")
    List<String> findDistinctLifecycles(@Param("resourceId") UUID resourceId,
                                        @Param("tenantId") String tenantId);

    @Query("SELECT DISTINCT e.kind FROM Entitlement e WHERE e.resourceId = :resourceId "
            + "AND e.tenantId = :tenantId AND e.kind IS NOT NULL")
    List<String> findDistinctKinds(@Param("resourceId") UUID resourceId,
                                   @Param("tenantId") String tenantId);

    /**
     * Counts entitlements by resource and tenant — used for dashboard stats.
     */
    long countByResourceIdAndTenantId(UUID resourceId, String tenantId);

    // ===================================================================
    //  Phase G: Performance Optimizations — Bulk Load & Delete
    // ===================================================================

    /**
     * G1: Loads all entitlements for a resource in a single query.
     * Returns (id, sourceUid) pairs for building an in-memory lookup cache.
     * This eliminates N+1 queries during membership resolution.
     */
    @Query("SELECT e.id, e.sourceUid FROM Entitlement e WHERE e.resourceId = :resourceId AND e.tenantId = :tenantId")
    List<Object[]> findAllIdsAndSourceUids(@Param("resourceId") UUID resourceId,
                                           @Param("tenantId") String tenantId);

    /**
     * G4: Bulk delete absent entitlements in a single query.
     * Replaces paginated delete loop with set-based operation.
     * Returns the count of deleted rows.
     */
    @Modifying
    @Query("DELETE FROM Entitlement e WHERE e.resourceId = :resourceId AND e.tenantId = :tenantId "
            + "AND (e.lastSyncRunId IS NULL OR e.lastSyncRunId <> :syncRunId)")
    int deleteAbsentEntitlements(@Param("resourceId") UUID resourceId,
                                 @Param("tenantId") String tenantId,
                                 @Param("syncRunId") String syncRunId);

    // ===================================================================
    //  Analytics & KPI Queries (Tenant-wide)
    // ===================================================================

    /**
     * Count all entitlements for a tenant (across all resources).
     * Used for KPI reporting.
     */
    long countByTenantId(String tenantId);

    /**
     * Count entitlements with Active lifecycle for a tenant (across all resources).
     * Used in dashboard to show number of active entitlements.
     */
    @Query("SELECT COUNT(e) FROM Entitlement e WHERE e.tenantId = :tenantId AND e.lifecycle = :lifecycle")
    long countByTenantIdAndLifecycle(@Param("tenantId") String tenantId,
                                     @Param("lifecycle") String lifecycle);

    /**
     * Count entitlements without an assigned owner (orphaned) for a tenant.
     * Orphaned entitlements represent a governance risk.
     */
    @Query("SELECT COUNT(e) FROM Entitlement e WHERE e.tenantId = :tenantId AND e.ownerId IS NULL")
    long countOrphanedEntitlementsByTenantId(@Param("tenantId") String tenantId);

    /**
     * Count entitlements without any classification for a tenant.
     * Unclassified entitlements represent missing governance context.
     */
    @Query(value = """
                SELECT COUNT(*) FROM entitlements e 
                WHERE e.tenant_id = :tenantId 
                AND (e.classification IS NULL OR e.classification = '[]'::jsonb)
            """, nativeQuery = true)
    long countUnclassifiedEntitlementsByTenantId(@Param("tenantId") String tenantId);

    /**
     * Count entitlements marked with a specific risk level for a tenant.
     * Comparison is case-insensitive to handle mixed-case data in storage.
     * Used to identify high-risk entitlements requiring special governance.
     */
    @Query("SELECT COUNT(e) FROM Entitlement e WHERE e.tenantId = :tenantId AND UPPER(e.riskLevel) = UPPER(:riskLevel)")
    long countByTenantIdAndRiskLevel(@Param("tenantId") String tenantId,
                                     @Param("riskLevel") String riskLevel);

    /**
     * Finds all distinct resource IDs (applications) for a given tenant.
     * Used for entitlement count per application analytics.
     */
    @Query("SELECT DISTINCT e.resourceId FROM Entitlement e WHERE e.tenantId = :tenantId")
    List<UUID> findDistinctResourceIdsByTenantId(@Param("tenantId") String tenantId);

    /**
     * Count entitlements without an owner (null ownerId) for a tenant.
     * These are classified as ownership gaps - governance risk.
     * Used in Classification KPI analytics.
     */
    @Query("SELECT COUNT(e) FROM Entitlement e WHERE e.tenantId = :tenantId AND e.ownerId IS NULL")
    long countEntitlementsWithoutOwner(@Param("tenantId") String tenantId);

    /**
     * Count entitlements marked as requestable for a tenant.
     * Requestable entitlements can be self-service requested by users.
     * Used in Classification KPI analytics.
     */
    @Query("SELECT COUNT(e) FROM Entitlement e WHERE e.tenantId = :tenantId AND e.requestable = true")
    long countRequestableEntitlements(@Param("tenantId") String tenantId);

    /**
     * Finds distinct resource IDs (applications) for a given set of entitlement IDs.
     * Used to determine which applications a role touches via its linked entitlements.
     */
    @Query("SELECT DISTINCT e.resourceId FROM Entitlement e WHERE e.id IN :entitlementIds ORDER BY e.resourceId")
    List<UUID> findDistinctResourceIdsByEntitlementIds(@Param("entitlementIds") List<UUID> entitlementIds);

    /**
     * Finds entitlements matching a set of IDs filtered by a specific resource (application).
     * Paginated for per-application entitlement pagination within grouped views.
     */
    @Query("SELECT e FROM Entitlement e WHERE e.id IN :entitlementIds AND e.resourceId = :resourceId")
    Page<Entitlement> findByIdInAndResourceId(
            @Param("entitlementIds") List<UUID> entitlementIds,
            @Param("resourceId") UUID resourceId,
            Pageable pageable
    );

    /**
     * Counts entitlements matching a set of IDs filtered by resource and risk level (case-insensitive).
     * Used for computing high-risk count per application group.
     */
    @Query("SELECT COUNT(e) FROM Entitlement e WHERE e.id IN :entitlementIds AND e.resourceId = :resourceId AND UPPER(e.riskLevel) = 'HIGH'")
    long countHighRiskByIdsAndResourceId(
            @Param("entitlementIds") List<UUID> entitlementIds,
            @Param("resourceId") UUID resourceId
    );


    /** Delete all entitlements for a resource within a tenant (cascade delete on application removal). */
    @Modifying
    @Query("DELETE FROM Entitlement e WHERE e.resourceId = :resourceId AND e.tenantId = :tenantId")
    void deleteAllByResourceIdAndTenantId(@Param("resourceId") UUID resourceId, @Param("tenantId") String tenantId);
}
