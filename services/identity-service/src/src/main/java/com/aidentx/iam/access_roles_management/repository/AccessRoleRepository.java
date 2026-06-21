package com.aidentx.iam.access_roles_management.repository;

import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.service.analytics.impl.AccessRoleIntelligenceAnalyticsServiceImpl.RoleBusinessUnitCountProjection;
import com.aidentx.iam.access_roles_management.service.analytics.impl.AccessRoleIntelligenceAnalyticsServiceImpl.RoleTypeDistributionProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for {@link AccessRole} entity operations.
 */
@Repository
public interface AccessRoleRepository extends JpaRepository<AccessRole, UUID>, JpaSpecificationExecutor<AccessRole> {

    /**
     * Projection representing grouped role counts by business unit and type.
     */
    interface RoleDirectoryGroupSummaryProjection {

        /**
         * Returns the business unit.
         *
         * @return business unit
         */
        String getBusinessUnit();

        /**
         * Returns the stored role type.
         *
         * @return role type
         */
        String getType();

        /**
         * Returns the count of roles for the business-unit/type bucket.
         *
         * @return role count
         */
        long getCount();
    }

    /**
     * Finds all roles for a given tenant with pagination.
     *
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return a page of access roles
     */
    Page<AccessRole> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Finds a role by tenant and name (unique per tenant).
     *
     * @param tenantId the tenant identifier
     * @param name     the role name
     * @return the matching role, if found
     */
    Optional<AccessRole> findByTenantIdAndName(String tenantId, String name);

    /**
     * Finds all roles for a tenant filtered by type.
     *
     * @param tenantId the tenant identifier
     * @param type     the role type
     * @param pageable pagination parameters
     * @return a page of matching roles
     */
    Page<AccessRole> findByTenantIdAndType(String tenantId, String type, Pageable pageable);

    /**
     * Finds all roles for a tenant filtered by status.
     *
     * @param tenantId the tenant identifier
     * @param status   the role status
     * @param pageable pagination parameters
     * @return a page of matching roles
     */
    Page<AccessRole> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);

    /**
     * Finds all roles owned by a specific identity.
     *
     * @param ownerId the owner identity ID
     * @return list of roles owned by the given identity
     */
    List<AccessRole> findByOwnerId(UUID ownerId);

    /**
     * Finds multiple roles by their IDs (used for composition lookups) using List of Ids
     *
     * @param ids list of role IDs
     * @return list of matching roles
     */
    List<AccessRole> findByIdIn(List<UUID> ids);


     /**
     * Finds multiple roles by their IDs (used for composition lookups) using Set of Ids
     *
     * @param ids list of role IDs
     * @return list of matching roles
     */
    List<AccessRole> findByIdIn(Set<UUID> ids);

    /**
     * Counts all roles for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return total role count
     */
    long countByTenantId(String tenantId);

    /**
     * Counts all roles for a tenant and role status.
     *
     * @param tenantId the tenant identifier
     * @param status   the role status
     * @return matching role count
     */
    long countByTenantIdAndStatus(String tenantId, String status);

    /** Counts all roles for a tenant by status, ignoring case. */
    long countByTenantIdAndStatusIgnoreCase(String tenantId, String status);

    /** Counts roles with no active identity assignment rows. */
    @Query("""
            SELECT COUNT(ar)
            FROM AccessRole ar
            WHERE ar.tenantId = :tenantId
              AND NOT EXISTS (
                    SELECT 1
                    FROM IdentityRoleAssignment ira
                    WHERE ira.tenantId = ar.tenantId
                      AND ira.roleId = ar.id
                      AND ira.status = :status
              )
            """)
    long countRolesWithoutActiveMembers(@Param("tenantId") String tenantId,
                                        @Param("status") com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment.Status status);

    /**
     * Counts all roles for a tenant by risk level, ignoring case.
     *
     * @param tenantId  the tenant identifier
     * @param riskLevel the risk-level value
     * @return matching high-risk role count
     */
    long countByTenantIdAndRiskLevelIgnoreCase(String tenantId, String riskLevel);

    /**
     * Returns role counts grouped by role type for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return grouped chart dataset
     */
    @Query("""
            SELECT ar.type AS type, COUNT(ar) AS count
            FROM AccessRole ar
            WHERE ar.tenantId = :tenantId
            GROUP BY ar.type
            ORDER BY ar.type
            """)
    List<RoleTypeDistributionProjection> findRoleDistributionByType(String tenantId);

    /**
     * Returns role counts grouped by business unit for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return grouped chart dataset
     */
    @Query("""
            SELECT ar.businessUnit AS businessUnit, COUNT(ar) AS roles
            FROM AccessRole ar
            WHERE ar.tenantId = :tenantId
            GROUP BY ar.businessUnit
            ORDER BY ar.businessUnit
            """)
    List<RoleBusinessUnitCountProjection> findRoleCountsByBusinessUnit(String tenantId);

    /**
     * Returns role counts grouped by business unit and stored role type.
     *
     * <p>This query is used for the directory navigator summary endpoint and
     * intentionally returns only aggregate buckets, not full role rows.</p>
     *
     * @param tenantId the tenant identifier
     * @return grouped role counts by business unit and type
     */
    @Query("""
            SELECT ar.businessUnit AS businessUnit, ar.type AS type, COUNT(ar) AS count
            FROM AccessRole ar
            WHERE ar.tenantId = :tenantId
            GROUP BY ar.businessUnit, ar.type
            ORDER BY ar.businessUnit, ar.type
            """)
    List<RoleDirectoryGroupSummaryProjection> findRoleDirectoryGroupSummary(String tenantId);
}