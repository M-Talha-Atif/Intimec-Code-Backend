package com.aidentx.iam.shadow_management.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.aidentx.iam.shadow_management.model.entity.Shadow;
import com.aidentx.iam.shadow_management.model.entity.Shadow.SynchronizationSituation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Shadow entity operations.
 * Provides CRUD operations and custom queries for shadow management.
 */
@Repository
public interface ShadowRepository extends JpaRepository<Shadow, UUID>, JpaSpecificationExecutor<Shadow> {

    /**
     * Find a shadow by resource ID, resource UID, and tenant.
     * This enforces the uniqueness constraint.
     *
     * @param resourceId  The resource identifier (which system)
     * @param resourceUid The unique identifier within the resource
     * @param tenantId    The tenant ID
     * @return Optional containing the shadow if found
     */
    Optional<Shadow> findByResourceIdAndResourceUidAndTenantId(
            String resourceId,
            String resourceUid,
            String tenantId);

    /**
     * Check if a shadow exists by resource ID, resource UID, and tenant.
     *
     * @param resourceId  The resource identifier (which system)
     * @param resourceUid The unique identifier within the resource
     * @param tenantId    The tenant ID
     * @return true if exists, false otherwise
     */
    boolean existsByResourceIdAndResourceUidAndTenantId(
            String resourceId,
            String resourceUid,
            String tenantId);

    /**
     * Find all shadows for a specific resource within a tenant.
     *
     * @param resourceId The resource identifier
     * @param tenantId   The tenant ID
     * @param pageable   Pagination information
     * @return Page of shadows
     */
    Page<Shadow> findByResourceIdAndTenantId(
            String resourceId,
            String tenantId,
            Pageable pageable);

    /**
     * Find all shadows linked to a specific identity.
     *
     * @param linkedIdentityId The identity ID
     * @param tenantId         The tenant ID
     * @return List of shadows
     */
    List<Shadow> findByLinkedIdentityIdAndTenantId(
            UUID linkedIdentityId,
            String tenantId);

    /**
     * Find all shadows linked to a specific identity with optional filters.
     *
     * @param linkedIdentityId The identity ID
     * @param tenantId         The tenant ID
     * @param resourceUid       Optional resource ID filter
     * @param objectClass      Optional object class filter
     * @return List of shadows
     */
    @Query(value = "SELECT * FROM shadows s WHERE s.linked_identity_id = CAST(:linkedIdentityId AS uuid) " +
            "AND s.tenant_id = :tenantId " +
            "AND (CAST(:resourceUid AS varchar) IS NULL OR s.resource_uid = :resourceUid) " +
            "AND (CAST(:objectClass AS varchar) IS NULL OR s.object_class = :objectClass)",
            nativeQuery = true)
    List<Shadow> findByLinkedIdentityIdWithFilters(
            @Param("linkedIdentityId") UUID linkedIdentityId,
            @Param("tenantId") String tenantId,
            @Param("resourceUid") String resourceUid,
            @Param("objectClass") String objectClass);

    /**
     * Find all shadows by synchronization situation within a tenant.
     *
     * @param synchronizationSituation The synchronization situation
     * @param tenantId                 The tenant ID
     * @param pageable                 Pagination information
     * @return Page of shadows
     */
    Page<Shadow> findBySynchronizationSituationAndTenantId(
            SynchronizationSituation synchronizationSituation,
            String tenantId,
            Pageable pageable);

    /**
     * Find all shadows by object class within a tenant.
     *
     * @param objectClass The object class
     * @param tenantId    The tenant ID
     * @param pageable    Pagination information
     * @return Page of shadows
     */
    Page<Shadow> findByObjectClassAndTenantId(
            String objectClass,
            String tenantId,
            Pageable pageable);

    /**
     * Find all linked shadows (shadows with a linked identity) within a tenant.
     *
     * @param tenantId The tenant ID
     * @param pageable Pagination information
     * @return Page of shadows
     */
    @Query("SELECT s FROM Shadow s WHERE s.linkedIdentityId IS NOT NULL AND s.tenantId = :tenantId")
    Page<Shadow> findLinkedShadows(
            @Param("tenantId") String tenantId,
            Pageable pageable);

    /**
     * Find all unlinked shadows (shadows without a linked identity) within a
     * tenant.
     *
     * @param tenantId The tenant ID
     * @param pageable Pagination information
     * @return Page of shadows
     */
    @Query("SELECT s FROM Shadow s WHERE s.linkedIdentityId IS NULL AND s.tenantId = :tenantId")
    Page<Shadow> findUnlinkedShadows(
            @Param("tenantId") String tenantId,
            Pageable pageable);

    /**
     * Find shadows by resource and object class within a tenant.
     *
     * @param resourceUid  The resource identifier
     * @param objectClass The object class
     * @param tenantId    The tenant ID
     * @param pageable    Pagination information
     * @return Page of shadows
     */
    Page<Shadow> findByResourceUidAndObjectClassAndTenantId(
            String resourceUid,
            String objectClass,
            String tenantId,
            Pageable pageable);

    /**
     * Count shadows by resource UID within a tenant.
     *
     * @param resourceUid The resource UID
     * @param tenantId    The tenant ID
     * @return Count of shadows
     */
    long countByResourceUidAndTenantId(String resourceUid, String tenantId);

    /**
     * Count shadows linked to a specific identity.
     *
     * @param linkedIdentityId The identity ID
     * @param tenantId         The tenant ID
     * @return Count of shadows
     */
    long countByLinkedIdentityIdAndTenantId(UUID linkedIdentityId, String tenantId);

    /**
     * Count shadows by synchronization situation within a tenant.
     *
     * @param synchronizationSituation The synchronization situation
     * @param tenantId                 The tenant ID
     * @return Count of shadows
     */
    long countBySynchronizationSituationAndTenantId(
            SynchronizationSituation synchronizationSituation,
            String tenantId);

    /**
     * Find all shadows within a tenant with pagination.
     *
     * @param tenantId The tenant ID
     * @param pageable Pagination information
     * @return Page of shadows
     */
    Page<Shadow> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Check if a shadow exists by resource UID and tenant.
     *
     * @param resourceUid The unique identifier on the resource
     * @param tenantId    The tenant ID
     * @return true if exists, false otherwise
     */
    boolean existsByResourceUidAndTenantId(String resourceUid, String tenantId);

    /**
     * Delete all shadows for a specific resource within a tenant.
     *
     * @param resourceUid The resource UID
     * @param tenantId    The tenant ID
     * @return Number of deleted shadows
     */
    long deleteByResourceUidAndTenantId(String resourceUid, String tenantId);

    /**
     * Find shadows by DGraph UID.
     *
     * @param dgraphUid The DGraph UID
     * @param tenantId  The tenant ID
     * @return Optional containing the shadow if found
     */
    Optional<Shadow> findByDgraphUidAndTenantId(String dgraphUid, String tenantId);

    /**
     * Find shadows that were NOT seen during a specific sync run (paginated).
     * Used for server-side deletion detection — replaces the previous approach
     * of sending all seen UIDs over gRPC.
     * Returns a page at a time to prevent OOM when an entire application is decommissioned.
     *
     * @param resourceId The resource identifier
     * @param tenantId   The tenant ID
     * @param syncRunId  The current sync run ID
     * @param pageable   Pagination parameters
     * @return Page of shadows not touched by this sync run (candidates for deletion)
     */
    @Query("SELECT s FROM Shadow s WHERE s.resourceId = :resourceId " +
            "AND s.tenantId = :tenantId " +
            "AND s.synchronizationSituation <> 'DELETED' " +
            "AND (s.lastSyncRunId IS NULL OR s.lastSyncRunId <> :syncRunId)")
    Page<Shadow> findAbsentShadows(
            @Param("resourceId") String resourceId,
            @Param("tenantId") String tenantId,
            @Param("syncRunId") String syncRunId,
            Pageable pageable);

    /**
     * Find absent shadows filtered by object class (paginated).
     * Scopes deletion detection to a specific object class (e.g., __ACCOUNT__)
     * so that group shadows are not incorrectly flagged for deletion when
     * only account objects were imported.
     */
    @Query("SELECT s FROM Shadow s WHERE s.resourceId = :resourceId " +
            "AND s.tenantId = :tenantId " +
            "AND s.objectClass = :objectClass " +
            "AND s.synchronizationSituation <> 'DELETED' " +
            "AND (s.lastSyncRunId IS NULL OR s.lastSyncRunId <> :syncRunId)")
    Page<Shadow> findAbsentShadowsByObjectClass(
            @Param("resourceId") String resourceId,
            @Param("tenantId") String tenantId,
            @Param("objectClass") String objectClass,
            @Param("syncRunId") String syncRunId,
            Pageable pageable);

    /**
     * Dry-run variant of absent-shadow detection.
     *
     * <p>During a preview (dry-run) reconciliation the sync pipeline runs
     * {@code processDryRun} which is {@code @Transactional(readOnly=true)} and
     * never writes {@code lastSyncRunId} to shadow rows.  The standard
     * {@link #findAbsentShadowsByObjectClass} query therefore returns every shadow
     * (none have the preview run ID) and falsely reports all accounts as deletion
     * candidates.</p>
     *
     * <p>This query solves the problem by explicitly excluding the set of
     * {@code resourceUid}s that the caller already processed in the dry-run scan,
     * producing an accurate preview of which accounts are <em>genuinely</em>
     * missing from the source system.</p>
     *
     * @param resourceId  The resource (application) identifier
     * @param tenantId    The tenant ID
     * @param objectClass The object class to scope (e.g. {@code __ACCOUNT__})
     * @param seenUids    All resource UIDs that were returned by the connector in
     *                    this dry-run scan — these must NOT be treated as absent
     * @param pageable    Pagination parameters
     * @return Shadows that exist in the store but whose UID was NOT returned by
     * the connector in this scan
     */
    @Query("SELECT s FROM Shadow s WHERE s.resourceId = :resourceId " +
            "AND s.tenantId = :tenantId " +
            "AND s.objectClass = :objectClass " +
            "AND s.synchronizationSituation <> 'DELETED' " +
            "AND s.resourceUid NOT IN :seenUids")
    Page<Shadow> findAbsentShadowsByObjectClassExcludingUids(
            @Param("resourceId") String resourceId,
            @Param("tenantId") String tenantId,
            @Param("objectClass") String objectClass,
            @Param("seenUids") java.util.Set<String> seenUids,
            Pageable pageable);

    /** Delete all shadows for a resource within a tenant (cascade delete on application removal). */
    @Modifying
    @Query("DELETE FROM Shadow s WHERE s.resourceId = :resourceId AND s.tenantId = :tenantId")
    void deleteAllByResourceIdAndTenantId(@Param("resourceId") String resourceId, @Param("tenantId") String tenantId);

    /**
     * Retrieves all shadow IDs linked to a given identity within a tenant.
     * Used for cross-domain queries where the caller needs to resolve
     * an identity's accounts without loading full Shadow entities.
     *
     * @param linkedIdentityId the UUID of the identity whose shadows are requested
     * @param tenantId         the tenant scope for multi-tenant isolation
     * @return list of shadow UUIDs linked to the given identity
     */
    List<UUID> findIdsByLinkedIdentityIdAndTenantId(UUID linkedIdentityId, String tenantId);

    /**
     * Retrieves all shadow IDs linked to a given identity within a tenant.
     * Returns only UUIDs (not full entities) to minimize memory and serialization cost.
     * <p>
     * Typical cardinality: 1 to 50 results per identity. An identity represents
     * a person or service, and each shadow represents one account on one target system.
     *
     * @param linkedIdentityId the UUID of the identity whose shadows are requested
     * @param tenantId         the tenant scope for multi-tenant isolation
     * @return list of shadow UUIDs linked to the given identity
     */
    @Query("SELECT s.id FROM Shadow s WHERE s.linkedIdentityId = :linkedIdentityId AND s.tenantId = :tenantId")
    List<UUID> findShadowIdsByLinkedIdentityId(@Param("linkedIdentityId") UUID linkedIdentityId,
                                               @Param("tenantId") String tenantId);

    /**
     * Batch-loads shadows by a collection of IDs within a tenant.
     * Used to resolve account display information (resourceUid, resourceId)
     * after paginated assignment queries.
     *
     * @param ids      collection of shadow UUIDs to load
     * @param tenantId the tenant scope for multi-tenant isolation
     * @return list of Shadow entities matching the given IDs and tenant
     */
    @Query("SELECT s FROM Shadow s WHERE s.id IN :ids AND s.tenantId = :tenantId")
    List<Shadow> findAllByIdInAndTenantId(@Param("ids") Collection<UUID> ids,
                                          @Param("tenantId") String tenantId);

    /**
     * Bulk-loads distinct resource IDs for a set of identity IDs within a tenant.
     * Returns (linkedIdentityId, resourceId) pairs for building per-identity account coverage maps.
     * Used by role member entitlement status computation to avoid N+1 shadow queries.
     */
    @Query("SELECT s.linkedIdentityId, s.resourceId FROM Shadow s " +
            "WHERE s.linkedIdentityId IN :identityIds AND s.tenantId = :tenantId " +
            "AND s.kind = 'account' AND s.synchronizationSituation <> 'DELETED'")
    List<Object[]> findResourceIdsByIdentityIds(@Param("identityIds") Collection<UUID> identityIds,
                                                @Param("tenantId") String tenantId);
}
