package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Maintains the materialized direct/inherited entitlement closure for access roles.
 */
public interface RoleEntitlementClosureService {

    /** Rebuilds closure rows for one role only. */
    List<RoleEntitlementClosure> rebuildRoleClosure(String tenantId, UUID roleId);

    /** Rebuilds closure rows for a role and every ancestor role that inherits from it. */
    Set<UUID> rebuildRoleAndAncestors(String tenantId, UUID roleId);

    /** Rebuilds all closure rows for every role in a tenant. */
    Set<UUID> rebuildAllForTenant(String tenantId);

    /** Refresh hook to call after direct entitlement links are added/removed for a role. */
    Set<UUID> refreshAfterEntitlementsChanged(String tenantId, UUID roleId);

    /** Refresh hook to call after a parent-child composition link changes. */
    Set<UUID> refreshAfterCompositionChanged(String tenantId, UUID parentRoleId, UUID childRoleId);
}

