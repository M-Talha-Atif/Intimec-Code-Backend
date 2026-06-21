package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.dto.response.SyncChildRolesResultDto;
import com.aidentx.iam.access_roles_management.model.dto.response.SyncEntitlementsResultDto;

import java.util.List;
import java.util.UUID;

/**
 * Service for synchronizing a role's sub-resources (entitlements, children)
 * to a desired final state.
 *
 * <h3>Design Rationale — Why a Separate Service?</h3>
 * <p>This service exists as a <strong>separate Spring bean</strong> from
 * {@link AccessRoleEntitlementService} and {@link AccessRoleCompositionService}
 * for a critical reason: <strong>Spring AOP proxy limitations</strong>.</p>
 *
 * <p>Spring creates a proxy around beans that have AOP-annotated methods.
 * When a method is called from <em>outside</em> the bean, the proxy intercepts
 * it and runs the aspect advice. But when a method calls another method
 * <em>within the same bean</em> (a "self-call"), the call goes directly
 * to {@code this}, bypassing the proxy entirely. The aspect never fires.</p>
 *
 * <p>If sync methods were inside {@code AccessRoleEntitlementServiceImpl}
 * alongside {@code addEntitlements()}, the internal call to
 * {@code this.addEntitlements()} would bypass the proxy. By placing sync
 * methods in a separate bean, calls to {@code entitlementService.addEntitlements()}
 * go through the proxy correctly.</p>
 *
 * <h3>Desired-State Pattern</h3>
 * <p>Both sync methods accept the <strong>desired final state</strong> (the
 * complete list of IDs the role should have). The implementation computes
 * the diff against the current state and executes the minimum set of
 * additions and removals. This is idempotent: sending the same desired
 * state twice produces the same result.</p>
 *
 * <h3>Audit</h3>
 * <p>Each sync method is annotated with {@code @AuditAccessRoleChange},
 * producing exactly <strong>one audit event</strong> per call that captures
 * both additions and removals in a single entry.</p>
 */
public interface AccessRoleSyncService {

    /**
     * Synchronizes a role's entitlement assignments to the desired state.
     *
     * <p>Computes the diff between current and desired entitlement IDs,
     * then adds missing entitlements and removes excess ones.</p>
     *
     * <p>Entitlement additions are idempotent: already-linked entitlements
     * are silently skipped.</p>
     *
     * @param tenantId              the tenant identifier
     * @param roleId                the role to sync entitlements for
     * @param desiredEntitlementIds the complete list of entitlement IDs the role should have
     * @return result with added count, removed count, and final total
     * @throws jakarta.persistence.EntityNotFoundException if the role does not exist
     */
    SyncEntitlementsResultDto syncEntitlements(String tenantId, UUID roleId, List<UUID> desiredEntitlementIds);

    /**
     * Synchronizes a role's child role composition to the desired state.
     *
     * <p>Computes the diff between current and desired child role IDs,
     * then adds missing children and removes excess ones. Child additions
     * may fail individually (circular dependency, self-reference) — the
     * result includes a list of failed children with reasons.</p>
     *
     * @param tenantId           the tenant identifier
     * @param roleId             the parent role to sync children for
     * @param desiredChildRoleIds the complete list of child role IDs the role should have
     * @return result with added count, removed count, failed list, and final total
     * @throws jakarta.persistence.EntityNotFoundException if the parent role does not exist
     */
    SyncChildRolesResultDto syncChildRoles(String tenantId, UUID roleId, List<UUID> desiredChildRoleIds);
}

