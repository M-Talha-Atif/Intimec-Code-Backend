package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementClosureService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementResolutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves effective entitlements using the materialized role-entitlement closure.
 */
@Service
public class RoleEntitlementResolutionServiceImpl implements RoleEntitlementResolutionService {

    private final RoleEntitlementClosureRepository closureRepository;
    private final RoleEntitlementClosureService closureService;
    private final AccessRoleRepository accessRoleRepository;

    public RoleEntitlementResolutionServiceImpl(RoleEntitlementClosureRepository closureRepository,
                                                RoleEntitlementClosureService closureService,
                                                AccessRoleRepository accessRoleRepository) {
        this.closureRepository = closureRepository;
        this.closureService = closureService;
        this.accessRoleRepository = accessRoleRepository;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<UUID> resolveEffectiveEntitlementIds(String tenantId, UUID roleId) {
        ensureClosureAvailable(tenantId, roleId);
        return closureRepository.findDistinctEntitlementIdsByTenantIdAndRoleId(tenantId, roleId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<UUID> resolveDirectEntitlementIds(String tenantId, UUID roleId) {
        ensureClosureAvailable(tenantId, roleId);
        return closureRepository.findDistinctEntitlementIdsByTenantIdAndRoleIdAndDirect(tenantId, roleId, true);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<UUID> resolveInheritedEntitlementIds(String tenantId, UUID roleId) {
        ensureClosureAvailable(tenantId, roleId);
        List<UUID> direct = closureRepository.findDistinctEntitlementIdsByTenantIdAndRoleIdAndDirect(tenantId, roleId, true);
        List<UUID> inherited = closureRepository.findDistinctEntitlementIdsByTenantIdAndRoleIdAndDirect(tenantId, roleId, false);
        Set<UUID> directSet = new LinkedHashSet<>(direct);
        Set<UUID> inheritedOnly = new LinkedHashSet<>(inherited);
        inheritedOnly.removeAll(directSet);
        return List.copyOf(inheritedOnly);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<RoleEntitlementClosure> resolveEffectiveEntitlementMappings(String tenantId, UUID roleId) {
        ensureClosureAvailable(tenantId, roleId);
        return closureRepository.findByTenantIdAndRoleId(tenantId, roleId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public boolean hasEffectiveEntitlement(String tenantId, UUID roleId, UUID entitlementId) {
        if (entitlementId == null) {
            return false;
        }
        ensureClosureAvailable(tenantId, roleId);
        return closureRepository.existsByTenantIdAndRoleIdAndEntitlementId(tenantId, roleId, entitlementId);
    }

    private void ensureClosureAvailable(String tenantId, UUID roleId) {
        validateTenantId(tenantId);
        validateTenantRole(tenantId, roleId);
        if (closureRepository.findByTenantIdAndRoleId(tenantId, roleId).isEmpty()) {
            closureService.rebuildRoleClosure(tenantId, roleId);
        }
    }

    private void validateTenantRole(String tenantId, UUID roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required");
        }
        accessRoleRepository.findById(roleId)
                .filter(role -> tenantId.equals(role.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Access role not found for tenant: " + roleId));
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_TENANT_REQUIRED);
        }
    }
}


