package com.aidentx.iam.access_roles_management.repository;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link AccessRoleEntitlement} join entity operations.
 */
@Repository
public interface AccessRoleEntitlementRepository extends JpaRepository<AccessRoleEntitlement, UUID> {

    /**
     * Finds all entitlement links for a given role.
     *
     * @param roleId the role ID
     * @return list of role-entitlement links
     */
    List<AccessRoleEntitlement> findByRoleId(UUID roleId);

    /** Finds entitlement links for multiple roles. */
    List<AccessRoleEntitlement> findByRoleIdIn(Collection<UUID> roleIds);

    /**
     * Finds all roles that contain a specific entitlement.
     *
     * @param entitlementId the entitlement ID
     * @return list of role-entitlement links
     */
    List<AccessRoleEntitlement> findByEntitlementId(UUID entitlementId);

    /**
     * Deletes all entitlement links for a given role.
     *
     * @param roleId the role ID
     */
    void deleteByRoleId(UUID roleId);

    /**
     * Deletes a specific role-entitlement link.
     *
     * @param roleId        the role ID
     * @param entitlementId the entitlement ID
     */
    void deleteByRoleIdAndEntitlementId(UUID roleId, UUID entitlementId);

    /**
     * Deletes selected role-entitlement links for one entitlement.
     *
     * @param roleIds       role IDs to unlink
     * @param entitlementId the entitlement ID
     */
    void deleteByRoleIdInAndEntitlementId(List<UUID> roleIds, UUID entitlementId);

    /**
     * Checks if a specific link already exists (prevent duplicates).
     *
     * @param roleId        the role ID
     * @param entitlementId the entitlement ID
     * @return true if the link exists
     */
    boolean existsByRoleIdAndEntitlementId(UUID roleId, UUID entitlementId);

    /**
     * Counts how many entitlements are linked to a specific role.
     *
     * @param roleId the role ID
     * @return the count of linked entitlements
     */
    long countByRoleId(UUID roleId);
}