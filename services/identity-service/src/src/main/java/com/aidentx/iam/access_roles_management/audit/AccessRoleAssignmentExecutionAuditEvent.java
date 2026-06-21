package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable audit event for access-role assignment rule execution operations.
 */
public record AccessRoleAssignmentExecutionAuditEvent(
        String actorId,
        String actorUsername,
        String tenantId,
        String action,
        String executionId,
        String ruleId,
        String ruleName,
        String roleId,
        String roleName,
        String roleType,
        String roleStatus,
        String riskLevel,
        String triggerType,
        String executionStatus,
        Integer executionCount,
        Integer identitiesMatched,
        Integer rolesAssigned,
        Integer rolesReactivated,
        Integer rolesRevoked,
        Integer skippedAlreadyAssigned,
        Integer skippedNotMatched,
        Integer errorCount,
        JsonNode stateAfter,
        String description,
        Instant timestamp
) {

    public static AccessRoleAssignmentExecutionAuditEvent singleExecution(
            String actorId,
            String actorUsername,
            String tenantId,
            String executionId,
            String ruleId,
            String ruleName,
            String roleId,
            String roleName,
            String roleType,
            String roleStatus,
            String riskLevel,
            String triggerType,
            String executionStatus,
            Integer identitiesMatched,
            Integer rolesAssigned,
            Integer rolesReactivated,
            Integer rolesRevoked,
            Integer skippedAlreadyAssigned,
            Integer skippedNotMatched,
            Integer errorCount,
            JsonNode stateAfter) {
        return new AccessRoleAssignmentExecutionAuditEvent(
                actorId,
                actorUsername,
                tenantId,
                AccessRoleAssignmentExecutionAuditConstants.ACTION_RULE_EXECUTED,
                executionId,
                ruleId,
                ruleName,
                roleId,
                roleName,
                roleType,
                roleStatus,
                riskLevel,
                triggerType,
                executionStatus,
                1,
                identitiesMatched,
                rolesAssigned,
                rolesReactivated,
                rolesRevoked,
                skippedAlreadyAssigned,
                skippedNotMatched,
                errorCount,
                stateAfter,
                String.format(
                        AccessRoleAssignmentExecutionAuditConstants.DESC_RULE_EXECUTED_TEMPLATE,
                        AccessRoleAssignmentExecutionAuditConstants.capitalizeRoleType(roleType).toLowerCase(),
                        ruleName,
                        roleName,
                        actorUsername),
                Instant.now()
        );
    }

    public static AccessRoleAssignmentExecutionAuditEvent allExecutions(
            String actorId,
            String actorUsername,
            String tenantId,
            String riskLevel,
            Integer executionCount,
            Integer identitiesMatched,
            Integer rolesAssigned,
            Integer rolesReactivated,
            Integer rolesRevoked,
            Integer skippedAlreadyAssigned,
            Integer skippedNotMatched,
            Integer errorCount,
            JsonNode stateAfter) {
        return new AccessRoleAssignmentExecutionAuditEvent(
                actorId,
                actorUsername,
                tenantId,
                AccessRoleAssignmentExecutionAuditConstants.ACTION_RULES_EXECUTED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                riskLevel,
                "EVALUATE_ALL",
                null,
                executionCount,
                identitiesMatched,
                rolesAssigned,
                rolesReactivated,
                rolesRevoked,
                skippedAlreadyAssigned,
                skippedNotMatched,
                errorCount,
                stateAfter,
                String.format(AccessRoleAssignmentExecutionAuditConstants.DESC_RULES_EXECUTED_TEMPLATE, actorUsername),
                Instant.now()
        );
    }

    public AccessRoleAuditLog toAuditLog() {
        AccessRoleAuditLog log = new AccessRoleAuditLog();
        log.setTenantId(tenantId);
        log.setRuleId(parseUuid(ruleId));
        log.setRuleName(ruleName);
        log.setRoleId(parseUuid(roleId));
        log.setRoleName(roleName);
        log.setRoleType(roleType);
        log.setEntityType(resolveEntityType());
        log.setActivityLabel(resolveActivityLabel());
        log.setAction(action);
        log.setTriggerSource(AccessRoleAssignmentExecutionAuditConstants.TRIGGER_EXECUTION);
        log.setDescription(description);
        log.setActorId(actorId);
        log.setActorUsername(actorUsername);
        log.setStateBefore(null);
        log.setStateAfter(stateAfter);
        log.setChangedFields(List.of());
        log.setAddedChildren(null);
        log.setRemovedChildren(null);
        log.setFailedChildren(null);
        log.setAddedEntitlements(null);
        log.setRemovedEntitlements(null);
        return log;
    }

    private String resolveEntityType() {
        if (AccessRoleAssignmentExecutionAuditConstants.ACTION_RULES_EXECUTED.equals(action)) {
            return AccessRoleAssignmentExecutionAuditConstants.ENTITY_TYPE_ALL;
        }
        return AccessRoleAssignmentRuleAuditConstants.toEntityType(roleType);
    }

    private String resolveActivityLabel() {
        if (AccessRoleAssignmentExecutionAuditConstants.ACTION_RULES_EXECUTED.equals(action)) {
            return AccessRoleAssignmentExecutionAuditConstants.ACTIVITY_RULES_EXECUTED;
        }
        return AccessRoleAssignmentExecutionAuditConstants.toSingleActivityLabel(roleType);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

