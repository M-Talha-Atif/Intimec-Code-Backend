package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;

import java.util.List;
import java.util.UUID;

/**
 * Resolves direct and inherited effective entitlements for an access role.
 */
public interface RoleEntitlementResolutionService {

    /** Returns distinct direct + inherited entitlement IDs for a role. */
    List<UUID> resolveEffectiveEntitlementIds(String tenantId, UUID roleId);

    /** Returns distinct direct entitlement IDs attached to the role itself. */
    List<UUID> resolveDirectEntitlementIds(String tenantId, UUID roleId);

    /** Returns distinct inherited entitlement IDs coming from child roles. */
    List<UUID> resolveInheritedEntitlementIds(String tenantId, UUID roleId);

    /** Returns provenance rows for all effective entitlement mappings. */
    List<RoleEntitlementClosure> resolveEffectiveEntitlementMappings(String tenantId, UUID roleId);

    /** Returns true when the role effectively contains the entitlement directly or via children. */
    boolean hasEffectiveEntitlement(String tenantId, UUID roleId, UUID entitlementId);
}

