package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates dynamic access-role assignment scans.
 *
 * <p>The scan layer chooses the scan scope and trigger context. The lower-level
 * execution service performs identity matching, source-aware assignment writes,
 * stale assignment revocation, execution history, and per-identity outcome
 * logging.</p>
 */
public interface AccessRoleAssignmentRuleScanService {

    /** Runs one rule manually for the given tenant. */
    AccessRoleAssignmentExecution scanRule(String tenantId, UUID ruleId, String triggeredBy);

    /** Runs every enabled assignment rule for the tenant in priority order. */
    List<AccessRoleAssignmentExecution> scanAllRules(String tenantId, String triggeredBy);

    /** Runs enabled rules configured for immediate execution. */
    List<AccessRoleAssignmentExecution> scanImmediateRules(String tenantId, String triggeredBy);

    /** Runs enabled CRON rules whose next execution time is due. */
    List<AccessRoleAssignmentExecution> scanDueCronRules(String tenantId, String triggeredBy);
}

