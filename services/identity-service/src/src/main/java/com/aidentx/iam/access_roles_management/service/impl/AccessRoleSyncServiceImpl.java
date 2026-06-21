package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditAction;
import com.aidentx.iam.access_roles_management.audit.AuditAccessRoleChange;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleConstants;
import com.aidentx.iam.access_roles_management.model.dto.response.AddChildRolesResultDto;
import com.aidentx.iam.access_roles_management.model.dto.response.SyncChildRolesResultDto;
import com.aidentx.iam.access_roles_management.model.dto.response.SyncEntitlementsResultDto;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleCompositionService;
import com.aidentx.iam.access_roles_management.service.AccessRoleEntitlementService;
import com.aidentx.iam.access_roles_management.service.AccessRoleSyncService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AccessRoleSyncService}.
 *
 * <p>Computes the diff between the current state and the desired state,
 * then delegates to the existing entitlement and composition services
 * to perform the actual additions and removals.</p>
 *
 * <h3>Diff Computation</h3>
 * <p>Uses {@link LinkedHashSet} for set operations (difference, intersection).
 * LinkedHashSet preserves insertion order (for predictable audit output)
 * and provides O(1) {@code contains()} lookups (for efficient diff).</p>
 *
 * <h3>Transaction Boundary</h3>
 * <p>Each sync method runs in a single transaction ({@code @Transactional}).
 * If any part fails, the entire sync is rolled back. This ensures the
 * role never ends up in an inconsistent state (e.g. removals done but
 * additions failed).</p>
 *
 * <h3>Why This is a Separate Bean</h3>
 * <p>See {@link AccessRoleSyncService} javadoc for the Spring AOP proxy
 * explanation. In short: internal calls within the same bean bypass AOP.
 * This service calls {@code entitlementService.addEntitlements()} through
 * the Spring proxy, ensuring correct behavior.</p>
 */
@Service
@Transactional
public class AccessRoleSyncServiceImpl implements AccessRoleSyncService {

    private final AccessRoleRepository roleRepository;
    private final AccessRoleEntitlementService entitlementService;
    private final AccessRoleCompositionService compositionService;
    private final IUnifiedLogger logger;

    /**
     * Constructs the sync service with all required dependencies.
     *
     * @param roleRepository      access role repository for existence validation
     * @param entitlementService   the entitlement link service (separate bean — AOP works)
     * @param compositionService   the composition link service (separate bean — AOP works)
     * @param loggerFactory        unified logger factory
     */
    public AccessRoleSyncServiceImpl(
            AccessRoleRepository roleRepository,
            AccessRoleEntitlementService entitlementService,
            AccessRoleCompositionService compositionService,
            UnifiedLoggerFactory loggerFactory) {
        this.roleRepository = roleRepository;
        this.entitlementService = entitlementService;
        this.compositionService = compositionService;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleConstants.LOG_CATEGORY);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@code @AuditAccessRoleChange} annotation causes the
     * {@link com.aidentx.iam.access_roles_management.audit.AccessRoleAuditAspect}
     * to intercept this method and publish a single "Business Role Entitlements Updated"
     * audit event with before/after state and rich entitlement details.</p>
     *
     * <h4>Algorithm</h4>
     * <ol>
     *   <li>Load current entitlement IDs from the link table</li>
     *   <li>Compute toAdd = desired − current (entitlements to link)</li>
     *   <li>Compute toRemove = current − desired (entitlements to unlink)</li>
     *   <li>Call entitlementService.addEntitlements() for new ones</li>
     *   <li>Call entitlementService.removeEntitlements() for excess ones</li>
     *   <li>Return summary with counts</li>
     * </ol>
     */
    @Override
    @AuditAccessRoleChange(action = AccessRoleAuditAction.ENTITLEMENTS_UPDATED)
    public SyncEntitlementsResultDto syncEntitlements(
            String tenantId, UUID roleId, List<UUID> desiredEntitlementIds) {

        validateRoleExists(roleId);

        // ---------------------------------------------------------------
        // 1. Load current state from the link table
        // ---------------------------------------------------------------
        List<UUID> currentIds = entitlementService.getEntitlementIds(roleId);

        // ---------------------------------------------------------------
        // 2. Compute diff using set operations
        //
        //    LinkedHashSet:
        //    - Preserves insertion order (audit output is deterministic)
        //    - O(1) contains() via hash table (efficient for large lists)
        //    - No duplicates (desired list may contain dupes from frontend)
        // ---------------------------------------------------------------
        Set<UUID> desiredSet = new LinkedHashSet<>(desiredEntitlementIds);
        Set<UUID> currentSet = new LinkedHashSet<>(currentIds);

        // toAdd: IDs in desired but NOT in current → need to link
        List<UUID> toAdd = desiredEntitlementIds.stream()
                .filter(id -> !currentSet.contains(id))
                .distinct()
                .collect(Collectors.toList());

        // toRemove: IDs in current but NOT in desired → need to unlink
        List<UUID> toRemove = currentIds.stream()
                .filter(id -> !desiredSet.contains(id))
                .collect(Collectors.toList());

        // ---------------------------------------------------------------
        // 3. Execute additions and removals via existing services
        //
        //    These calls go through the Spring proxy to the injected
        //    entitlementService bean. Since we removed @AuditAccessRoleChange
        //    from addEntitlements/removeEntitlements, no redundant audit
        //    events are fired. Only the sync-level audit from this method.
        // ---------------------------------------------------------------
        int addedCount = 0;
        int removedCount = 0;

        if (!toAdd.isEmpty()) {
            entitlementService.addEntitlements(tenantId, roleId, toAdd);
            addedCount = toAdd.size();
        }

        if (!toRemove.isEmpty()) {
            entitlementService.removeEntitlements(tenantId, roleId, toRemove);
            removedCount = toRemove.size();
        }

        // Final count from the link table (source of truth)
        int totalCount = entitlementService.getEntitlementIds(roleId).size();

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message("Entitlements synced for role")
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail("addedCount", String.valueOf(addedCount))
                .detail("removedCount", String.valueOf(removedCount))
                .detail("totalCount", String.valueOf(totalCount))
                .log();

        return new SyncEntitlementsResultDto(addedCount, removedCount, totalCount);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@code @AuditAccessRoleChange} annotation causes the aspect to
     * publish a single "Business Role Children Updated" audit event.</p>
     *
     * <h4>Algorithm</h4>
     * <ol>
     *   <li>Load current child role IDs from the composition table</li>
     *   <li>Compute toAdd = desired − current (children to link)</li>
     *   <li>Compute toRemove = current − desired (children to unlink)</li>
     *   <li>Call compositionService.removeChildRoles() for excess ones</li>
     *   <li>Call compositionService.addChildRoles() for new ones
     *       (returns partial success with failed children)</li>
     *   <li>Return summary with added, removed, and failed lists</li>
     * </ol>
     *
     * <p><strong>Order matters:</strong> We remove first, then add. This avoids
     * a scenario where adding a child that already exists (in the toRemove set)
     * causes a constraint violation. By removing first, the add is clean.</p>
     */
    @Override
    @AuditAccessRoleChange(action = AccessRoleAuditAction.CHILDREN_UPDATED)
    public SyncChildRolesResultDto syncChildRoles(
            String tenantId, UUID roleId, List<UUID> desiredChildRoleIds) {

        validateRoleExists(roleId);

        // ---------------------------------------------------------------
        // 1. Load current child IDs from the composition table
        // ---------------------------------------------------------------
        List<UUID> currentIds = compositionService.getChildRoleIds(roleId);

        // ---------------------------------------------------------------
        // 2. Compute diff
        // ---------------------------------------------------------------
        Set<UUID> desiredSet = new LinkedHashSet<>(desiredChildRoleIds);
        Set<UUID> currentSet = new LinkedHashSet<>(currentIds);

        List<UUID> toAdd = desiredChildRoleIds.stream()
                .filter(id -> !currentSet.contains(id))
                .distinct()
                .collect(Collectors.toList());

        List<UUID> toRemove = currentIds.stream()
                .filter(id -> !desiredSet.contains(id))
                .collect(Collectors.toList());

        // ---------------------------------------------------------------
        // 3. Execute removals first, then additions
        // ---------------------------------------------------------------
        int removedCount = 0;
        if (!toRemove.isEmpty()) {
            compositionService.removeChildRoles(tenantId, roleId, toRemove);
            removedCount = toRemove.size();
        }

        // addChildRoles returns partial success — some children may fail
        // (circular dependency, self-reference, not found)
        List<UUID> addedIds = Collections.emptyList();
        List<AddChildRolesResultDto.FailedChild> failedChildren = Collections.emptyList();

        if (!toAdd.isEmpty()) {
            AddChildRolesResultDto addResult = compositionService.addChildRoles(tenantId, roleId, toAdd);
            addedIds = addResult.getAdded();
            failedChildren = addResult.getFailed();
        }

        // Final count from the composition table (source of truth)
        int totalCount = compositionService.getChildRoleIds(roleId).size();

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message("Child roles synced for role")
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail("addedCount", String.valueOf(addedIds.size()))
                .detail("removedCount", String.valueOf(removedCount))
                .detail("failedCount", String.valueOf(failedChildren.size()))
                .detail("totalCount", String.valueOf(totalCount))
                .log();

        return new SyncChildRolesResultDto(
                addedIds, toRemove, failedChildren, totalCount);
    }

    // ===================================================================
    //  Helpers
    // ===================================================================

    /**
     * Validates that the role exists or throws {@link EntityNotFoundException}.
     *
     * @param roleId the role ID to validate
     * @throws EntityNotFoundException if the role does not exist
     */
    private void validateRoleExists(UUID roleId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> {
                    logger.warn()
                            .category(AccessRoleConstants.LOG_CATEGORY)
                            .message(AccessRoleConstants.LOG_ROLE_NOT_FOUND)
                            .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                            .log();
                    return new EntityNotFoundException(
                            String.format(AccessRoleConstants.ERROR_ROLE_NOT_FOUND, roleId));
                });
    }
}