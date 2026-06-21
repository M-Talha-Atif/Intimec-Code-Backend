package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit event for access-role assignment rule CRUD operations.
 *
 * <p>The event is intentionally rich enough to drive both Kafka publishing and
 * DB-backed audit history persistence. This keeps orchestration in the aspect
 * thin while preserving user-friendly activity labels and descriptions across
 * both audit channels.</p>
 */
public record AccessRoleAssignmentRuleAuditEvent(
        String actorId,
        String actorUsername,
        String tenantId,
        String action,
        String ruleId,
        String ruleName,
        String roleId,
        String roleName,
        String roleType,
        String roleStatus,
        String riskLevel,
        boolean enabled,
        String scheduleType,
        Integer priority,
        Integer conditionGroupCount,
        List<String> identityTypes,
        JsonNode stateBefore,
        JsonNode stateAfter,
        String description,
        Instant timestamp
) {

    /**
     * Creates a rule-created audit event.
     */
    public static AccessRoleAssignmentRuleAuditEvent ruleCreated(
            String actorId,
            String actorUsername,
            String tenantId,
            String ruleId,
            String ruleName,
            String roleId,
            String roleName,
            String roleType,
            String roleStatus,
            String riskLevel,
            boolean enabled,
            String scheduleType,
            Integer priority,
            Integer conditionGroupCount,
            List<String> identityTypes,
            JsonNode stateAfter) {
        return new AccessRoleAssignmentRuleAuditEvent(
                actorId,
                actorUsername,
                tenantId,
                AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_CREATED,
                ruleId,
                ruleName,
                roleId,
                roleName,
                roleType,
                roleStatus,
                riskLevel,
                enabled,
                scheduleType,
                priority,
                conditionGroupCount,
                identityTypes,
                null,
                stateAfter,
                buildDescription(AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_CREATED, roleType, ruleName, roleName, actorUsername),
                Instant.now()
        );
    }

    /**
     * Creates a rule-updated audit event.
     */
    public static AccessRoleAssignmentRuleAuditEvent ruleUpdated(
            String actorId,
            String actorUsername,
            String tenantId,
            String ruleId,
            String ruleName,
            String roleId,
            String roleName,
            String roleType,
            String roleStatus,
            String riskLevel,
            boolean enabled,
            String scheduleType,
            Integer priority,
            Integer conditionGroupCount,
            List<String> identityTypes,
            JsonNode stateBefore,
            JsonNode stateAfter) {
        return new AccessRoleAssignmentRuleAuditEvent(
                actorId,
                actorUsername,
                tenantId,
                AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_UPDATED,
                ruleId,
                ruleName,
                roleId,
                roleName,
                roleType,
                roleStatus,
                riskLevel,
                enabled,
                scheduleType,
                priority,
                conditionGroupCount,
                identityTypes,
                stateBefore,
                stateAfter,
                buildDescription(AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_UPDATED, roleType, ruleName, roleName, actorUsername),
                Instant.now()
        );
    }

    /**
     * Creates a rule-deleted audit event.
     */
    public static AccessRoleAssignmentRuleAuditEvent ruleDeleted(
            String actorId,
            String actorUsername,
            String tenantId,
            String ruleId,
            String ruleName,
            String roleId,
            String roleName,
            String roleType,
            String roleStatus,
            String riskLevel,
            boolean enabled,
            String scheduleType,
            Integer priority,
            Integer conditionGroupCount,
            List<String> identityTypes,
            JsonNode stateBefore) {
        return new AccessRoleAssignmentRuleAuditEvent(
                actorId,
                actorUsername,
                tenantId,
                AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_DELETED,
                ruleId,
                ruleName,
                roleId,
                roleName,
                roleType,
                roleStatus,
                riskLevel,
                enabled,
                scheduleType,
                priority,
                conditionGroupCount,
                identityTypes,
                stateBefore,
                null,
                buildDescription(AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_DELETED, roleType, ruleName, roleName, actorUsername),
                Instant.now()
        );
    }

    /**
     * Converts this audit event into a persistent DB audit log record.
     *
     * @return append-only DB audit log entity
     */
    public AccessRoleAuditLog toAuditLog() {
        AccessRoleAuditLog log = new AccessRoleAuditLog();
        log.setTenantId(tenantId);
        log.setRuleId(parseUuid(ruleId));
        log.setRuleName(ruleName);
        log.setRoleId(parseUuid(roleId));
        log.setRoleName(roleName);
        log.setRoleType(roleType);
        log.setEntityType(AccessRoleAssignmentRuleAuditConstants.toEntityType(roleType));
        log.setActivityLabel(AccessRoleAssignmentRuleAuditConstants.toActivityLabel(action, roleType));
        log.setAction(action);
        log.setTriggerSource(AccessRoleAssignmentRuleAuditConstants.TRIGGER_API);
        log.setDescription(description);
        log.setActorId(actorId);
        log.setActorUsername(actorUsername);
        log.setStateBefore(stateBefore);
        log.setStateAfter(stateAfter);
        log.setChangedFields(computeChangedFields());
        log.setAddedChildren(null);
        log.setRemovedChildren(null);
        log.setFailedChildren(null);
        log.setAddedEntitlements(null);
        log.setRemovedEntitlements(null);
        return log;
    }

    /**
     * Computes the changed fields represented by the before/after diff payloads.
     *
     * @return ordered list of changed field names
     */
    public List<String> computeChangedFields() {
        if (!Objects.equals(action, AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_UPDATED)) {
            return List.of();
        }
        List<String> changedFields = new ArrayList<>();
        collectFieldNames(stateAfter, changedFields);
        collectFieldNames(stateBefore, changedFields);
        return changedFields;
    }

    private static void collectFieldNames(JsonNode state, List<String> changedFields) {
        if (state == null || !state.isObject()) {
            return;
        }

        Iterator<String> fieldNames = state.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            if (!changedFields.contains(field)) {
                changedFields.add(field);
            }
        }
    }

    private static String buildDescription(String action,
                                           String roleType,
                                           String ruleName,
                                           String roleName,
                                           String actorUsername) {
        String displayType = AccessRoleAssignmentRuleAuditConstants.capitalizeRoleType(roleType).toLowerCase();
        String template = resolveDescriptionTemplate(action);
        if (template == null) {
            return action;
        }

        return String.format(
                template,
                displayType,
                safe(ruleName),
                safe(roleName),
                safe(actorUsername));
    }

    private static String resolveDescriptionTemplate(String action) {
        return switch (action) {
            case AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_CREATED ->
                    AccessRoleAssignmentRuleAuditConstants.DESC_RULE_CREATED_TEMPLATE;
            case AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_UPDATED ->
                    AccessRoleAssignmentRuleAuditConstants.DESC_RULE_UPDATED_TEMPLATE;
            case AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_DELETED ->
                    AccessRoleAssignmentRuleAuditConstants.DESC_RULE_DELETED_TEMPLATE;
            default -> null;
        };
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String safe(String value) {
        return (value == null || value.isBlank())
                ? AccessRoleAssignmentRuleAuditConstants.UNKNOWN
                : value;
    }
}
