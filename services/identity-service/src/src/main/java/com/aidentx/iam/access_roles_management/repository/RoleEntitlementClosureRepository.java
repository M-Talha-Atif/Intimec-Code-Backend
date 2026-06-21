package com.aidentx.iam.access_roles_management.repository;

import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for materialized effective role-entitlement mappings.
 */
@Repository
public interface RoleEntitlementClosureRepository extends JpaRepository<RoleEntitlementClosure, UUID> {

    List<RoleEntitlementClosure> findByTenantIdAndRoleId(String tenantId, UUID roleId);

    List<RoleEntitlementClosure> findByTenantIdAndRoleIdAndDirect(String tenantId, UUID roleId, boolean direct);

    List<RoleEntitlementClosure> findByTenantIdAndRoleIdAndEntitlementId(String tenantId, UUID roleId, UUID entitlementId);

    List<RoleEntitlementClosure> findByTenantIdAndRoleIdInAndEntitlementIdIn(
            String tenantId,
            Collection<UUID> roleIds,
            Collection<UUID> entitlementIds);

    List<RoleEntitlementClosure> findByTenantIdAndSourceRoleId(String tenantId, UUID sourceRoleId);

    boolean existsByTenantIdAndRoleIdAndEntitlementId(String tenantId, UUID roleId, UUID entitlementId);

    long countByTenantIdAndRoleId(String tenantId, UUID roleId);

    @Query("""
            SELECT COUNT(DISTINCT c.entitlementId)
            FROM RoleEntitlementClosure c
            WHERE c.tenantId = :tenantId AND c.roleId = :roleId
            """)
    long countDistinctEntitlementIdByTenantIdAndRoleId(@Param("tenantId") String tenantId,
                                                       @Param("roleId") UUID roleId);

    /** Sums each active role's distinct effective entitlement count for role-complexity analytics. */
    @Query(value = """
            SELECT COALESCE(SUM(role_counts.entitlement_count), 0)
            FROM (
                SELECT ar.id, COUNT(DISTINCT c.entitlement_id) AS entitlement_count
                FROM access_roles ar
                LEFT JOIN access_role_entitlement_closure c
                  ON c.tenant_id = ar.tenant_id
                 AND c.role_id = ar.id
                WHERE ar.tenant_id = :tenantId
                  AND LOWER(ar.status) = LOWER(:status)
                GROUP BY ar.id
            ) role_counts
            """, nativeQuery = true)
    long sumDistinctEntitlementMembershipsForRolesByTenantIdAndStatus(@Param("tenantId") String tenantId,
                                                                      @Param("status") String status);

    @Query("""
            SELECT DISTINCT c.entitlementId
            FROM RoleEntitlementClosure c
            WHERE c.tenantId = :tenantId AND c.roleId = :roleId
            ORDER BY c.entitlementId
            """)
    List<UUID> findDistinctEntitlementIdsByTenantIdAndRoleId(@Param("tenantId") String tenantId,
                                                              @Param("roleId") UUID roleId);

    @Query("""
            SELECT DISTINCT c.entitlementId
            FROM RoleEntitlementClosure c
            WHERE c.tenantId = :tenantId AND c.roleId = :roleId AND c.direct = :direct
            ORDER BY c.entitlementId
            """)
    List<UUID> findDistinctEntitlementIdsByTenantIdAndRoleIdAndDirect(@Param("tenantId") String tenantId,
                                                                       @Param("roleId") UUID roleId,
                                                                       @Param("direct") boolean direct);

    @Query(value = """
            SELECT a.entitlementId,
                   SUM(CASE WHEN a.direct = true THEN 1 ELSE 0 END),
                   SUM(CASE WHEN a.direct = false THEN 1 ELSE 0 END),
                   COUNT(DISTINCT a.sourceRoleId),
                   SUM(CASE WHEN b.direct = true THEN 1 ELSE 0 END),
                   SUM(CASE WHEN b.direct = false THEN 1 ELSE 0 END),
                   COUNT(DISTINCT b.sourceRoleId)
            FROM RoleEntitlementClosure a
            JOIN RoleEntitlementClosure b
              ON b.tenantId = a.tenantId
             AND b.entitlementId = a.entitlementId
            WHERE a.tenantId = :tenantId
              AND a.roleId = :roleAId
              AND b.roleId = :roleBId
              AND (
                   :sourceFilter = 'ALL'
                   OR (:sourceFilter = 'DIRECT' AND (a.direct = true OR b.direct = true))
                   OR (:sourceFilter = 'INHERITED' AND (a.direct = false OR b.direct = false))
              )
            GROUP BY a.entitlementId
            ORDER BY a.entitlementId
            """,
            countQuery = """
            SELECT COUNT(DISTINCT a.entitlementId)
            FROM RoleEntitlementClosure a
            JOIN RoleEntitlementClosure b
              ON b.tenantId = a.tenantId
             AND b.entitlementId = a.entitlementId
            WHERE a.tenantId = :tenantId
              AND a.roleId = :roleAId
              AND b.roleId = :roleBId
              AND (
                   :sourceFilter = 'ALL'
                   OR (:sourceFilter = 'DIRECT' AND (a.direct = true OR b.direct = true))
                   OR (:sourceFilter = 'INHERITED' AND (a.direct = false OR b.direct = false))
              )
            """)
    Page<Object[]> findOverlappingEntitlementSummaries(@Param("tenantId") String tenantId,
                                                       @Param("roleAId") UUID roleAId,
                                                       @Param("roleBId") UUID roleBId,
                                                       @Param("sourceFilter") String sourceFilter,
                                                       Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void deleteByTenantIdAndRoleId(String tenantId, UUID roleId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void deleteByTenantIdAndRoleIdIn(String tenantId, Collection<UUID> roleIds);

    // ===================================================================
    //  Risk-insights aggregations (see AccessRoleRiskInsightsService)
    // ===================================================================

    /**
     * Counts distinct entitlements in the role's closure whose entitlement
     * {@code riskLevel} matches (case-insensitively) the supplied token and
     * whose closure depth is at or below {@code maxDepth}.
     */
    @Query("""
            SELECT COUNT(DISTINCT c.entitlementId)
            FROM RoleEntitlementClosure c,
                 com.aidentx.iam.entitlement_management.model.entity.Entitlement e
            WHERE c.tenantId = :tenantId
              AND c.roleId   = :roleId
              AND c.depth   <= :maxDepth
              AND e.id       = c.entitlementId
              AND LOWER(e.riskLevel) = :riskLevel
            """)
    long countDistinctEntitlementsByRiskLevel(@Param("tenantId") String tenantId,
                                              @Param("roleId") UUID roleId,
                                              @Param("riskLevel") String riskLevel,
                                              @Param("maxDepth") int maxDepth);

    /**
     * Counts distinct entitlements in the role's closure whose entitlement
     * {@code privilegeLevel} matches (case-insensitively) the supplied token
     * and whose closure depth is at or below {@code maxDepth}.
     */
    @Query("""
            SELECT COUNT(DISTINCT c.entitlementId)
            FROM RoleEntitlementClosure c,
                 com.aidentx.iam.entitlement_management.model.entity.Entitlement e
            WHERE c.tenantId = :tenantId
              AND c.roleId   = :roleId
              AND c.depth   <= :maxDepth
              AND e.id       = c.entitlementId
              AND LOWER(e.privilegeLevel) = :privilegeLevel
            """)
    long countDistinctEntitlementsByPrivilegeLevel(@Param("tenantId") String tenantId,
                                                   @Param("roleId") UUID roleId,
                                                   @Param("privilegeLevel") String privilegeLevel,
                                                   @Param("maxDepth") int maxDepth);

    /**
     * Returns IDs of entitlements that are contributed to the role's closure by
     * more than one distinct source role (i.e. overlap conflicts within the
     * role's own descendant tree), capped at {@code maxDepth}.
     */
    @Query("""
            SELECT c.entitlementId
            FROM RoleEntitlementClosure c
            WHERE c.tenantId = :tenantId
              AND c.roleId   = :roleId
              AND c.depth   <= :maxDepth
            GROUP BY c.entitlementId
            HAVING COUNT(DISTINCT c.sourceRoleId) > 1
            """)
    List<UUID> findOverlapEntitlementIds(@Param("tenantId") String tenantId,
                                         @Param("roleId") UUID roleId,
                                         @Param("maxDepth") int maxDepth);

    /**
     * Distribution of overlapping entitlements by closure depth. Each row is
     * {@code [depth, count]}; a single entitlement appearing at multiple
     * depths is counted in every bucket it appears in.
     */
    @Query("""
            SELECT c.depth, COUNT(DISTINCT c.entitlementId)
            FROM RoleEntitlementClosure c
            WHERE c.tenantId = :tenantId
              AND c.roleId   = :roleId
              AND c.depth   <= :maxDepth
              AND c.entitlementId IN :overlapIds
            GROUP BY c.depth
            ORDER BY c.depth ASC
            """)
    List<Object[]> countOverlapDepthDistribution(@Param("tenantId") String tenantId,
                                                 @Param("roleId") UUID roleId,
                                                 @Param("overlapIds") Collection<UUID> overlapIds,
                                                 @Param("maxDepth") int maxDepth);

    /**
     * Highest closure depth at which any overlapping entitlement appears,
     * bounded by {@code maxDepth}. Returns {@code null} when there are none.
     */
    @Query("""
            SELECT MAX(c.depth)
            FROM RoleEntitlementClosure c
            WHERE c.tenantId = :tenantId
              AND c.roleId   = :roleId
              AND c.depth   <= :maxDepth
              AND c.entitlementId IN :overlapIds
            """)
    Integer findMaxOverlapDepth(@Param("tenantId") String tenantId,
                                @Param("roleId") UUID roleId,
                                @Param("overlapIds") Collection<UUID> overlapIds,
                                @Param("maxDepth") int maxDepth);

    /**
     * Total distinct entitlements in the role's closure, bounded by {@code maxDepth}.
     */
    @Query("""
            SELECT COUNT(DISTINCT c.entitlementId)
            FROM RoleEntitlementClosure c
            WHERE c.tenantId = :tenantId
              AND c.roleId   = :roleId
              AND c.depth   <= :maxDepth
            """)
    long countDistinctEntitlementsByTenantIdAndRoleIdAndMaxDepth(
            @Param("tenantId") String tenantId,
            @Param("roleId") UUID roleId,
            @Param("maxDepth") int maxDepth);
}
