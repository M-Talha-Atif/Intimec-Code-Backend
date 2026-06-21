package com.aidentx.iam.organization_management.repository;

import com.aidentx.iam.organization_management.model.entity.IdentityOrgAssignment;
import com.aidentx.iam.organization_management.model.enums.HierarchyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdentityOrgAssignmentRepository extends JpaRepository<IdentityOrgAssignment, UUID> {

    List<IdentityOrgAssignment> findByTenantIdAndOrgUnitId(String tenantId, UUID orgUnitId);

    List<IdentityOrgAssignment> findByTenantIdAndIdentityId(String tenantId, UUID identityId);

    Optional<IdentityOrgAssignment> findByTenantIdAndIdentityIdAndHierarchyTypeAndPrimaryTrue(
            String tenantId, UUID identityId, HierarchyType hierarchyType);

    long countByTenantIdAndOrgUnitId(String tenantId, UUID orgUnitId);

    /** Count distinct identities assigned anywhere within a subtree (by path prefix). */
    @Query(value = """
        SELECT COUNT(DISTINCT a.identity_id)
          FROM identity_org_assignments a
          JOIN org_units o ON o.id = a.org_unit_id
         WHERE a.tenant_id = :tenantId
           AND o.path LIKE :pathPattern
        """, nativeQuery = true)
    long countDistinctIdentitiesInSubtree(
            @Param("tenantId") String tenantId,
            @Param("pathPattern") String pathPattern);

    /**
     * Count distinct non-employee (contractor/vendor) identities in a subtree.
     * Contractors are identities whose discriminator {@code identity_type = 'NON_EMPLOYEE'}
     * — they have a {@code contract_id}, {@code vendor_name} and an internal sponsor.
     * This is the IGA-standard way to track external workforce in an org scope.
     */
    @Query(value = """
        SELECT COUNT(DISTINCT a.identity_id)
          FROM identity_org_assignments a
          JOIN org_units o ON o.id = a.org_unit_id
          JOIN identities i ON i.identity_id = a.identity_id
         WHERE a.tenant_id = :tenantId
           AND o.path LIKE :pathPattern
           AND i.identity_type = 'NON_EMPLOYEE'
        """, nativeQuery = true)
    long countNonEmployeesInSubtree(
            @Param("tenantId") String tenantId,
            @Param("pathPattern") String pathPattern);

    /**
     * Count distinct non-human (privileged service account) identities in a subtree.
     * Non-human identities ({@code identity_type = 'NON_HUMAN'}) represent service accounts,
     * bots, and API accounts — they are classified as privileged by IGA standards because
     * they operate outside the normal HR identity lifecycle (no offboarding trigger,
     * machine credentials, elevated risk if left unmanaged).
     */
    @Query(value = """
        SELECT COUNT(DISTINCT a.identity_id)
          FROM identity_org_assignments a
          JOIN org_units o ON o.id = a.org_unit_id
          JOIN identities i ON i.identity_id = a.identity_id
         WHERE a.tenant_id = :tenantId
           AND o.path LIKE :pathPattern
           AND i.identity_type = 'NON_HUMAN'
        """, nativeQuery = true)
    long countNonHumanIdentitiesInSubtree(
            @Param("tenantId") String tenantId,
            @Param("pathPattern") String pathPattern);

    void deleteByTenantIdAndOrgUnitIdAndIdentityIdAndHierarchyType(
            String tenantId, UUID orgUnitId, UUID identityId, HierarchyType hierarchyType);

    Optional<IdentityOrgAssignment> findByTenantIdAndIdentityIdAndOrgUnitIdAndHierarchyType(
            String tenantId, UUID identityId, UUID orgUnitId, HierarchyType hierarchyType);
}
