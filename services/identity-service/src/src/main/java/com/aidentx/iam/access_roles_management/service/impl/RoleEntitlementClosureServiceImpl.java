package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleComposition;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleEntitlement;
import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import com.aidentx.iam.access_roles_management.repository.AccessRoleCompositionRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleEntitlementRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementClosureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Rebuilds materialized role-entitlement closure rows from direct entitlement
 * links and parent-child role composition links.
 */
@Service
public class RoleEntitlementClosureServiceImpl implements RoleEntitlementClosureService {

    private static final int MAX_HIERARCHY_DEPTH = 64;
    private static final int ROLE_REBUILD_PAGE_SIZE = 500;

    private final RoleEntitlementClosureRepository closureRepository;
    private final AccessRoleRepository accessRoleRepository;
    private final AccessRoleEntitlementRepository entitlementRepository;
    private final AccessRoleCompositionRepository compositionRepository;

    public RoleEntitlementClosureServiceImpl(RoleEntitlementClosureRepository closureRepository,
                                             AccessRoleRepository accessRoleRepository,
                                             AccessRoleEntitlementRepository entitlementRepository,
                                             AccessRoleCompositionRepository compositionRepository) {
        this.closureRepository = closureRepository;
        this.accessRoleRepository = accessRoleRepository;
        this.entitlementRepository = entitlementRepository;
        this.compositionRepository = compositionRepository;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<RoleEntitlementClosure> rebuildRoleClosure(String tenantId, UUID roleId) {
        validateTenantId(tenantId);
        assertTenantRole(tenantId, roleId);

        return rebuildRoleClosureInternal(tenantId, roleId);
    }

    private List<RoleEntitlementClosure> rebuildRoleClosureInternal(String tenantId, UUID roleId) {
        assertTenantRole(tenantId, roleId);

        closureRepository.flush();
        closureRepository.deleteByTenantIdAndRoleId(tenantId, roleId);
        closureRepository.flush();
        List<RoleEntitlementClosure> rebuiltRows = calculateClosureRows(tenantId, roleId);
        if (rebuiltRows.isEmpty()) {
            return List.of();
        }
        return closureRepository.saveAllAndFlush(rebuiltRows);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Set<UUID> rebuildRoleAndAncestors(String tenantId, UUID roleId) {
        validateTenantId(tenantId);
        assertTenantRole(tenantId, roleId);

        Set<UUID> rolesToRebuild = collectSelfAndAncestors(tenantId, roleId);
        for (UUID affectedRoleId : rolesToRebuild) {
            rebuildRoleClosureInternal(tenantId, affectedRoleId);
        }
        return rolesToRebuild;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Set<UUID> rebuildAllForTenant(String tenantId) {
        validateTenantId(tenantId);
        Set<UUID> rebuiltRoleIds = new LinkedHashSet<>();
        int pageNumber = 0;
        Page<AccessRole> page;
        do {
            page = accessRoleRepository.findByTenantId(tenantId, PageRequest.of(pageNumber++, ROLE_REBUILD_PAGE_SIZE));
            for (AccessRole role : page.getContent()) {
                rebuildRoleClosureInternal(tenantId, role.getId());
                rebuiltRoleIds.add(role.getId());
            }
        } while (page.hasNext());
        return rebuiltRoleIds;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Set<UUID> refreshAfterEntitlementsChanged(String tenantId, UUID roleId) {
        validateTenantId(tenantId);
        assertTenantRole(tenantId, roleId);
        Set<UUID> rolesToRebuild = collectSelfAndAncestors(tenantId, roleId);
        for (UUID affectedRoleId : rolesToRebuild) {
            rebuildRoleClosureInternal(tenantId, affectedRoleId);
        }
        return rolesToRebuild;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Set<UUID> refreshAfterCompositionChanged(String tenantId, UUID parentRoleId, UUID childRoleId) {
        validateTenantId(tenantId);
        assertTenantRole(tenantId, parentRoleId);
        assertTenantRole(tenantId, childRoleId);
        Set<UUID> rolesToRebuild = collectSelfAndAncestors(tenantId, parentRoleId);
        for (UUID affectedRoleId : rolesToRebuild) {
            rebuildRoleClosureInternal(tenantId, affectedRoleId);
        }
        return rolesToRebuild;
    }

    private List<RoleEntitlementClosure> calculateClosureRows(String tenantId, UUID rootRoleId) {
        Map<ClosureKey, RoleEntitlementClosure> rowsByKey = new LinkedHashMap<>();
        Map<UUID, Integer> shallowestDepthByRole = new HashMap<>();
        Queue<RolePath> queue = new ArrayDeque<>();
        queue.add(new RolePath(rootRoleId, 0, List.of(rootRoleId)));

        while (!queue.isEmpty()) {
            RolePath current = queue.remove();
            Integer previousDepth = shallowestDepthByRole.get(current.roleId());
            if (previousDepth != null && previousDepth < current.depth()) {
                continue;
            }
            shallowestDepthByRole.put(current.roleId(), current.depth());

            addDirectEntitlementsForSourceRole(tenantId, rootRoleId, current, rowsByKey);
            if (current.depth() >= MAX_HIERARCHY_DEPTH) {
                continue;
            }

            for (AccessRoleComposition composition : compositionRepository.findByParentRoleId(current.roleId())) {
                UUID childRoleId = composition.getChildRoleId();
                if (current.pathRoleIds().contains(childRoleId) || !isTenantRole(tenantId, childRoleId)) {
                    continue;
                }
                List<UUID> childPath = new ArrayList<>(current.pathRoleIds());
                childPath.add(childRoleId);
                queue.add(new RolePath(childRoleId, current.depth() + 1, List.copyOf(childPath)));
            }
        }

        return rowsByKey.values().stream()
                .sorted(Comparator.comparing(RoleEntitlementClosure::isDirect).reversed()
                        .thenComparing(RoleEntitlementClosure::getDepth)
                        .thenComparing(row -> row.getEntitlementId().toString())
                        .thenComparing(row -> row.getSourceRoleId().toString()))
                .toList();
    }

    private void addDirectEntitlementsForSourceRole(String tenantId,
                                                    UUID rootRoleId,
                                                    RolePath sourceRolePath,
                                                    Map<ClosureKey, RoleEntitlementClosure> rowsByKey) {
        for (AccessRoleEntitlement link : entitlementRepository.findByRoleId(sourceRolePath.roleId())) {
            ClosureKey key = new ClosureKey(link.getEntitlementId(), sourceRolePath.roleId());
            RoleEntitlementClosure existing = rowsByKey.get(key);
            if (existing != null && existing.getDepth() <= sourceRolePath.depth()) {
                continue;
            }

            RoleEntitlementClosure row = new RoleEntitlementClosure();
            row.setTenantId(tenantId);
            row.setRoleId(rootRoleId);
            row.setEntitlementId(link.getEntitlementId());
            row.setSourceRoleId(sourceRolePath.roleId());
            row.setDepth(sourceRolePath.depth());
            row.setDirect(rootRoleId.equals(sourceRolePath.roleId()));
            row.setPathRoleIds(sourceRolePath.pathRoleIds());
            rowsByKey.put(key, row);
        }
    }

    private Set<UUID> collectSelfAndAncestors(String tenantId, UUID roleId) {
        Set<UUID> ancestors = new LinkedHashSet<>();
        Queue<RolePath> queue = new ArrayDeque<>();
        queue.add(new RolePath(roleId, 0, List.of(roleId)));

        while (!queue.isEmpty()) {
            RolePath current = queue.remove();
            if (!ancestors.add(current.roleId()) || current.depth() >= MAX_HIERARCHY_DEPTH) {
                continue;
            }

            for (AccessRoleComposition composition : compositionRepository.findByChildRoleId(current.roleId())) {
                UUID parentRoleId = composition.getParentRoleId();
                if (current.pathRoleIds().contains(parentRoleId) || !isTenantRole(tenantId, parentRoleId)) {
                    continue;
                }
                List<UUID> parentPath = new ArrayList<>(current.pathRoleIds());
                parentPath.add(parentRoleId);
                queue.add(new RolePath(parentRoleId, current.depth() + 1, List.copyOf(parentPath)));
            }
        }
        return ancestors;
    }

    private void assertTenantRole(String tenantId, UUID roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required");
        }
        accessRoleRepository.findById(roleId)
                .filter(role -> tenantId.equals(role.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Access role not found for tenant: " + roleId));
    }

    private boolean isTenantRole(String tenantId, UUID roleId) {
        return roleId != null && accessRoleRepository.findById(roleId)
                .filter(role -> tenantId.equals(role.getTenantId()))
                .isPresent();
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_TENANT_REQUIRED);
        }
    }

    private record RolePath(UUID roleId, int depth, List<UUID> pathRoleIds) {
    }

    private record ClosureKey(UUID entitlementId, UUID sourceRoleId) {
    }
}

