package com.aidentx.iam.organization_management.repository;

import com.aidentx.iam.organization_management.model.entity.OrgUnit;
import com.aidentx.iam.organization_management.model.enums.HierarchyType;
import com.aidentx.iam.organization_management.model.enums.OrgUnitStatus;
import com.aidentx.iam.organization_management.model.enums.OrgUnitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgUnitRepository extends JpaRepository<OrgUnit, UUID> {

    Optional<OrgUnit> findByIdAndTenantId(UUID id, String tenantId);

    List<OrgUnit> findByTenantIdAndHierarchyTypeAndParentIdIsNullOrderByNameAsc(
            String tenantId, HierarchyType hierarchyType);

    List<OrgUnit> findByTenantIdAndParentIdOrderByNameAsc(String tenantId, UUID parentId);

    @Query("""
        SELECT o FROM OrgUnit o
        WHERE o.tenantId = :tenantId
          AND o.hierarchyType = :hierarchyType
          AND o.status = com.aidentx.iam.organization_management.model.enums.OrgUnitStatus.ACTIVE
        ORDER BY o.path ASC
        """)
    List<OrgUnit> findAllForHierarchy(
            @Param("tenantId") String tenantId,
            @Param("hierarchyType") HierarchyType hierarchyType);

    @Query("""
        SELECT o FROM OrgUnit o
        WHERE o.tenantId = :tenantId
          AND o.path LIKE CONCAT(:pathPrefix, '%')
          AND o.status = com.aidentx.iam.organization_management.model.enums.OrgUnitStatus.ACTIVE
        ORDER BY o.path ASC
        """)
    List<OrgUnit> findSubtree(
            @Param("tenantId") String tenantId,
            @Param("pathPrefix") String pathPrefix);

    @Query("""
        SELECT o FROM OrgUnit o
        WHERE o.tenantId = :tenantId
          AND (:hierarchyType IS NULL OR o.hierarchyType = :hierarchyType)
          AND (:type IS NULL OR o.type = :type)
          AND (:status IS NULL OR o.status = :status)
          AND (:search IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%'))
                              OR LOWER(o.code) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<OrgUnit> search(
            @Param("tenantId") String tenantId,
            @Param("hierarchyType") HierarchyType hierarchyType,
            @Param("type") OrgUnitType type,
            @Param("status") OrgUnitStatus status,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Bulk-update the path/depth of every unit in a moved subtree.
     * Replaces the {@code oldPrefix} part of the path with {@code newPrefix}
     * and adjusts depth by {@code depthDelta}.
     */
    @Modifying
    @Query(value = """
        UPDATE org_units
           SET path  = :newPrefix || SUBSTRING(path, LENGTH(:oldPrefix) + 1),
               depth = depth + :depthDelta,
               updated_at = NOW()
         WHERE tenant_id = :tenantId
           AND path LIKE :oldPattern
        """, nativeQuery = true)
    int rewriteSubtreePaths(
            @Param("tenantId") String tenantId,
            @Param("oldPrefix") String oldPrefix,
            @Param("newPrefix") String newPrefix,
            @Param("depthDelta") int depthDelta,
            @Param("oldPattern") String oldPattern);

    @Query("""
        SELECT COUNT(o) FROM OrgUnit o
        WHERE o.tenantId = :tenantId
          AND o.parentId = :parentId
          AND o.status = com.aidentx.iam.organization_management.model.enums.OrgUnitStatus.ACTIVE
        """)
    long countActiveByTenantIdAndParentId(
            @Param("tenantId") String tenantId,
            @Param("parentId") UUID parentId);

    boolean existsByTenantIdAndParentIdAndNameIgnoreCase(String tenantId, UUID parentId, String name);

    /**
     * Find org units by case-insensitive name match within a hierarchy. Used by the
     * Phase 2 backfill to resolve a free-text {@code Employee.department} string to
     * an org unit. May return multiple rows when names are not unique across the tree.
     */
    @Query("""
        SELECT o FROM OrgUnit o
        WHERE o.tenantId = :tenantId
          AND o.hierarchyType = :hierarchyType
          AND o.status = com.aidentx.iam.organization_management.model.enums.OrgUnitStatus.ACTIVE
          AND LOWER(o.name) = LOWER(:name)
        """)
    List<OrgUnit> findActiveByName(
            @Param("tenantId") String tenantId,
            @Param("hierarchyType") HierarchyType hierarchyType,
            @Param("name") String name);

    /**
     * IGA-standard org resolution: match an identity-side value against the most
     * stable identifier first ({@code externalId}), then the operator-defined
     * short {@code code}, and finally the human-readable {@code name}. Returns
     * all active matches; callers decide how to handle ambiguity.
     *
     * <p>Used by reconciliation when computing org assignments from staged
     * shadow data — operators may map the source attribute to any of the three
     * identifier styles (e.g. SAP department code, Workday cost-centre external
     * id, or display name).
     */
    @Query("""
        SELECT o FROM OrgUnit o
        WHERE o.tenantId = :tenantId
          AND o.hierarchyType = :hierarchyType
          AND o.status = com.aidentx.iam.organization_management.model.enums.OrgUnitStatus.ACTIVE
          AND (
                LOWER(o.externalId) = LOWER(:value)
             OR LOWER(o.code)       = LOWER(:value)
             OR LOWER(o.name)       = LOWER(:value)
          )
        """)
    List<OrgUnit> findActiveByNameCodeOrExternalId(
            @Param("tenantId") String tenantId,
            @Param("hierarchyType") HierarchyType hierarchyType,
            @Param("value") String value);
}
