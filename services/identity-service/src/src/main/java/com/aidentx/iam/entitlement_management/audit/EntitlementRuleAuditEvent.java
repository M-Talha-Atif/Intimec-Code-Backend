package com.aidentx.iam.entitlement_management.audit;

import com.aidentx.iam.config.AppConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

/**
 * Immutable audit event record for all entitlement assignment rule operations.
 *
 * <p>Each factory method produces a fully populated event with:
 * <ul>
 *   <li>Actor identity (who performed the action)</li>
 *   <li>Action type (what happened)</li>
 *   <li>Entity context (which rule, entitlement, resource)</li>
 *   <li>Human-readable description (for UI display)</li>
 *   <li>Before/after state snapshots (for diff rendering)</li>
 *   <li>Evaluation metrics (for evaluation events)</li>
 * </ul>
 *
 * <p>The {@code description} field provides a user-friendly summary
 * suitable for display in audit log UIs without requiring clients to
 * interpret action codes or entity types.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntitlementRuleAuditEvent(
        String actorId,
        String actorUsername,
        String action,
        String entityType,
        String entityId,
        String entityName,
        String tenantId,
        String entitlementId,
        String entitlementName,
        String resourceId,
        List<String> identityTypes,
        String triggerSource,
        String description,
        JsonNode stateBefore,
        JsonNode stateAfter,
        String ruleId,
        String ruleName,
        String identityId,
        String identityName,
        String shadowId,
        String executionId,
        Integer identitiesMatched,
        Integer assignmentsCreated,
        Integer assignmentsRevoked,
        Integer skippedNoShadow,
        Long durationMs,
        Instant timestamp
) {

    // ===================================================================
    //  Factory Methods Rule CRUD
    // ===================================================================

    /**
     * Creates an audit event for a newly created assignment rule.
     *
     * @param actorId          the ID of the user who created the rule
     * @param actorUsername    the username of the user who created the rule
     * @param tenantId         the tenant identifier
     * @param ruleId           the new rule's UUID
     * @param ruleName         the new rule's display name
     * @param entitlementId    the target entitlement UUID
     * @param entitlementName  the target entitlement display name
     * @param resourceId       the resource (application) UUID
     * @param identityTypes    the identity types the rule targets (EMPLOYEE, etc.)
     * @param stateAfter       JSON snapshot of the rule after creation
     * @return a fully populated audit event
     */
    public static EntitlementRuleAuditEvent ruleCreated(
            String actorId, String actorUsername, String tenantId,
            String ruleId, String ruleName,
            String entitlementId, String entitlementName,
            String resourceId, List<String> identityTypes,
            JsonNode stateAfter) {

        String description = buildRuleCrudDescription(
                AppConstants.AUDIT_DESC_RULE_CREATED, ruleName, entitlementName, actorUsername);

        return new EntitlementRuleAuditEvent(
                actorId, actorUsername,
                AppConstants.AUDIT_EVENT_RULE_CREATED,
                AppConstants.AUDIT_ENTITY_TYPE_ASSIGNMENT_RULE,
                ruleId, ruleName,
                tenantId, entitlementId, entitlementName, resourceId, identityTypes,
                AppConstants.AUDIT_TRIGGER_SOURCE_API,
                description,
                null, stateAfter,
                null, null, null, null, null, null,
                null, null, null, null, null,
                Instant.now()
        );
    }

    /**
     * Creates an audit event for an updated assignment rule.
     *
     * @param actorId          the ID of the user who updated the rule
     * @param actorUsername    the username of the user who updated the rule
     * @param tenantId         the tenant identifier
     * @param ruleId           the rule's UUID
     * @param ruleName         the rule's display name
     * @param entitlementId    the target entitlement UUID
     * @param entitlementName  the target entitlement display name
     * @param resourceId       the resource (application) UUID
     * @param identityTypes    the identity types the rule targets
     * @param stateBefore      JSON snapshot of changed fields before the update
     * @param stateAfter       JSON snapshot of changed fields after the update
     * @return a fully populated audit event
     */
    public static EntitlementRuleAuditEvent ruleUpdated(
            String actorId, String actorUsername, String tenantId,
            String ruleId, String ruleName,
            String entitlementId, String entitlementName,
            String resourceId, List<String> identityTypes,
            JsonNode stateBefore, JsonNode stateAfter) {

        String description = buildRuleCrudDescription(
                AppConstants.AUDIT_DESC_RULE_UPDATED, ruleName, entitlementName, actorUsername);

        return new EntitlementRuleAuditEvent(
                actorId, actorUsername,
                AppConstants.AUDIT_EVENT_RULE_UPDATED,
                AppConstants.AUDIT_ENTITY_TYPE_ASSIGNMENT_RULE,
                ruleId, ruleName,
                tenantId, entitlementId, entitlementName, resourceId, identityTypes,
                AppConstants.AUDIT_TRIGGER_SOURCE_API,
                description,
                stateBefore, stateAfter,
                null, null, null, null, null, null,
                null, null, null, null, null,
                Instant.now()
        );
    }

    /**
     * Creates an audit event for a deleted assignment rule.
     *
     * @param actorId            the ID of the user who deleted the rule
     * @param actorUsername      the username of the user who deleted the rule
     * @param tenantId           the tenant identifier
     * @param ruleId             the deleted rule's UUID
     * @param ruleName           the deleted rule's display name
     * @param entitlementId      the target entitlement UUID
     * @param entitlementName    the target entitlement display name
     * @param resourceId         the resource (application) UUID
     * @param deletedAssignments the number of assignments removed with the rule
     * @param stateBefore        JSON snapshot of the rule before deletion
     * @return a fully populated audit event
     */
    public static EntitlementRuleAuditEvent ruleDeleted(
            String actorId, String actorUsername, String tenantId,
            String ruleId, String ruleName,
            String entitlementId, String entitlementName,
            String resourceId, int deletedAssignments,
            JsonNode stateBefore) {

        String description = buildRuleCrudDescription(
                AppConstants.AUDIT_DESC_RULE_DELETED, ruleName, entitlementName, actorUsername);

        return new EntitlementRuleAuditEvent(
                actorId, actorUsername,
                AppConstants.AUDIT_EVENT_RULE_DELETED,
                AppConstants.AUDIT_ENTITY_TYPE_ASSIGNMENT_RULE,
                ruleId, ruleName,
                tenantId, entitlementId, entitlementName, resourceId, null,
                AppConstants.AUDIT_TRIGGER_SOURCE_API,
                description,
                stateBefore, null,
                null, null, null, null, null, null,
                null, null, deletedAssignments, null, null,
                Instant.now()
        );
    }

    /**
     * Creates an audit event for an enabled/disabled toggle on a rule.
     *
     * @param actorId        the ID of the user who toggled the rule
     * @param actorUsername  the username of the user who toggled the rule
     * @param tenantId       the tenant identifier
     * @param ruleId         the rule's UUID
     * @param ruleName       the rule's display name
     * @param enabled        {@code true} if the rule was enabled, {@code false} if disabled
     * @param stateBefore    JSON snapshot of the enabled field before the toggle
     * @param stateAfter     JSON snapshot of the enabled field after the toggle
     * @return a fully populated audit event
     */
    public static EntitlementRuleAuditEvent ruleToggled(
            String actorId, String actorUsername, String tenantId,
            String ruleId, String ruleName, boolean enabled,
            JsonNode stateBefore, JsonNode stateAfter) {

        String action = enabled
                ? AppConstants.AUDIT_EVENT_RULE_ENABLED
                : AppConstants.AUDIT_EVENT_RULE_DISABLED;

        String baseDesc = enabled
                ? AppConstants.AUDIT_DESC_RULE_ENABLED
                : AppConstants.AUDIT_DESC_RULE_DISABLED;

        String description = buildRuleToggleDescription(baseDesc, ruleName, actorUsername);

        return new EntitlementRuleAuditEvent(
                actorId, actorUsername,
                action,
                AppConstants.AUDIT_ENTITY_TYPE_ASSIGNMENT_RULE,
                ruleId, ruleName,
                tenantId, null, null, null, null,
                AppConstants.AUDIT_TRIGGER_SOURCE_API,
                description,
                stateBefore, stateAfter,
                null, null, null, null, null, null,
                null, null, null, null, null,
                Instant.now()
        );
    }

    // ===================================================================
    //  Factory Methods Global Schedule
    // ===================================================================

    /**
     * Creates an audit event for a global schedule configuration update.
     *
     * @param actorId        the ID of the user who updated the schedule
     * @param actorUsername  the username of the user who updated the schedule
     * @param tenantId       the tenant identifier
     * @param rulesUpdated   the number of rules affected by the schedule change
     * @param stateBefore    JSON snapshot of the schedule before the update
     * @param stateAfter     JSON snapshot of the schedule after the update
     * @return a fully populated audit event
     */
    public static EntitlementRuleAuditEvent globalScheduleUpdated(
            String actorId, String actorUsername, String tenantId,
            int rulesUpdated,
            JsonNode stateBefore, JsonNode stateAfter) {

        String description = String.format(
                "%s by %s, affecting %d rule(s)",
                AppConstants.AUDIT_DESC_GLOBAL_SCHEDULE_UPDATED,
                safe(actorUsername),
                rulesUpdated);

        return new EntitlementRuleAuditEvent(
                actorId, actorUsername,
                AppConstants.AUDIT_EVENT_GLOBAL_SCHEDULE_UPDATED,
                AppConstants.AUDIT_ENTITY_TYPE_ASSIGNMENT_RULE,
                AppConstants.AUDIT_GLOBAL_ENTITY_ID,
                AppConstants.AUDIT_GLOBAL_SCHEDULE_NAME,
                tenantId, null, null, null, null,
                AppConstants.AUDIT_TRIGGER_SOURCE_API,
                description,
                stateBefore, stateAfter,
                null, null, null, null, null, null,
                null, null, null, null, null,
                Instant.now()
        );
    }

    // ===================================================================
    //  Factory Methods Rule Evaluation Summary
    // ===================================================================

    /**
     * Creates an audit event capturing the result of a rule evaluation.
     *
     * <p>The description dynamically reflects the trigger source and includes
     * evaluation metrics (identities matched, assignments created/revoked).
     *
     * @param actorId            the ID of the actor (user or SYSTEM)
     * @param actorUsername      the username of the actor
     * @param tenantId           the tenant identifier
     * @param ruleId             the evaluated rule's UUID
     * @param ruleName           the evaluated rule's display name
     * @param entitlementId      the target entitlement UUID
     * @param entitlementName    the target entitlement display name
     * @param resourceId         the resource (application) UUID
     * @param triggerSource      what triggered the evaluation (e.g. RULE_CREATED, CRON, API)
     * @param executionId        the execution history record UUID (may be null)
     * @param identitiesMatched  the number of identities that matched the rule conditions
     * @param assignmentsCreated the number of new assignments created
     * @param assignmentsRevoked the number of existing assignments revoked
     * @param skippedNoShadow    the number of identities skipped due to missing shadow accounts
     * @param durationMs         the total evaluation duration in milliseconds
     * @return a fully populated audit event
     */
    public static EntitlementRuleAuditEvent ruleEvaluated(
            String actorId, String actorUsername, String tenantId,
            String ruleId, String ruleName,
            String entitlementId, String entitlementName,
            String resourceId, String triggerSource,
            String executionId,
            int identitiesMatched, int assignmentsCreated,
            int assignmentsRevoked, int skippedNoShadow, long durationMs) {

        String description = buildEvaluationDescription(
                triggerSource, ruleName, entitlementName, actorUsername,
                identitiesMatched, assignmentsCreated, assignmentsRevoked);

        return new EntitlementRuleAuditEvent(
                actorId, actorUsername,
                AppConstants.AUDIT_EVENT_RULE_EVALUATED,
                AppConstants.AUDIT_ENTITY_TYPE_ASSIGNMENT_RULE,
                ruleId, ruleName,
                tenantId, entitlementId, entitlementName, resourceId, null,
                triggerSource,
                description,
                null, null,
                ruleId, ruleName, null, null, null, executionId,
                identitiesMatched, assignmentsCreated, assignmentsRevoked, skippedNoShadow, durationMs,
                Instant.now()
        );
    }

    // ===================================================================
    //  Factory Methods Individual Assignments
    // ===================================================================

    /**
     * Creates an audit event for an entitlement assignment to an identity.
     *
     * @param tenantId        the tenant identifier
     * @param ruleId          the rule that triggered the assignment
     * @param ruleName        the rule's display name
     * @param entitlementId   the assigned entitlement UUID
     * @param entitlementName the assigned entitlement display name
     * @param resourceId      the resource (application) UUID
     * @param identityId      the identity that received the assignment
     * @param identityName    the identity's display name
     * @param shadowId        the shadow account UUID used for the assignment
     * @param executionId     the execution history record UUID
     * @param triggerSource   what triggered the evaluation that produced this assignment
     * @return a fully populated audit event
     */
    public static EntitlementRuleAuditEvent entitlementAssigned(
            String tenantId,
            String ruleId, String ruleName,
            String entitlementId, String entitlementName,
            String resourceId,
            String identityId, String identityName,
            String shadowId, String executionId, String triggerSource) {

        String description = String.format(
                "%s \"%s\" assigned to %s via rule \"%s\"",
                AppConstants.AUDIT_DESC_ENTITLEMENT_ASSIGNED,
                safe(entitlementName), safe(identityName), safe(ruleName));

        return new EntitlementRuleAuditEvent(
                AppConstants.AUDIT_ACTOR_SYSTEM_ID,
                AppConstants.AUDIT_ACTOR_SYSTEM_ID,
                AppConstants.AUDIT_EVENT_ENTITLEMENT_ASSIGNED,
                AppConstants.AUDIT_ENTITY_TYPE_ENTITLEMENT_ASSIGNMENT,
                null, null,
                tenantId, entitlementId, entitlementName, resourceId, null,
                triggerSource,
                description,
                null, null,
                ruleId, ruleName, identityId, identityName, shadowId, executionId,
                null, null, null, null, null,
                Instant.now()
        );
    }

    /**
     * Creates an audit event for an entitlement revocation from an identity.
     *
     * @param tenantId        the tenant identifier
     * @param ruleId          the rule that triggered the revocation
     * @param ruleName        the rule's display name
     * @param entitlementId   the revoked entitlement UUID
     * @param entitlementName the revoked entitlement display name
     * @param resourceId      the resource (application) UUID
     * @param identityId      the identity that lost the assignment
     * @param identityName    the identity's display name
     * @param shadowId        the shadow account UUID
     * @param executionId     the execution history record UUID
     * @param triggerSource   what triggered the evaluation that produced this revocation
     * @return a fully populated audit event
     */
    public static EntitlementRuleAuditEvent entitlementRevoked(
            String tenantId,
            String ruleId, String ruleName,
            String entitlementId, String entitlementName,
            String resourceId,
            String identityId, String identityName,
            String shadowId, String executionId, String triggerSource) {

        String description = String.format(
                "%s \"%s\" revoked from %s via rule \"%s\"",
                AppConstants.AUDIT_DESC_ENTITLEMENT_REVOKED,
                safe(entitlementName), safe(identityName), safe(ruleName));

        return new EntitlementRuleAuditEvent(
                AppConstants.AUDIT_ACTOR_SYSTEM_ID,
                AppConstants.AUDIT_ACTOR_SYSTEM_ID,
                AppConstants.AUDIT_EVENT_ENTITLEMENT_REVOKED,
                AppConstants.AUDIT_ENTITY_TYPE_ENTITLEMENT_ASSIGNMENT,
                null, null,
                tenantId, entitlementId, entitlementName, resourceId, null,
                triggerSource,
                description,
                null, null,
                ruleId, ruleName, identityId, identityName, shadowId, executionId,
                null, null, null, null, null,
                Instant.now()
        );
    }

    // ===================================================================
    //  Description Builders Private Helpers
    // ===================================================================

    /**
     * Builds a human-readable description for rule CRUD operations.
     *
     * <p>Example output:
     * {@code "A new entitlement assignment rule was created "Finance Admin Access" for entitlement "Admin Role" by john.doe"}
     *
     * @param baseDescription  the base description constant
     * @param ruleName         the rule's display name
     * @param entitlementName  the entitlement's display name
     * @param actorUsername    the actor's username
     * @return formatted description string
     */
    private static String buildRuleCrudDescription(String baseDescription,
                                                    String ruleName,
                                                    String entitlementName,
                                                    String actorUsername) {
        StringBuilder sb = new StringBuilder(baseDescription);
        sb.append("  \"").append(safe(ruleName)).append("\"");

        if (entitlementName != null && !entitlementName.isBlank()) {
            sb.append(" for entitlement \"").append(entitlementName).append("\"");
        }

        sb.append(" by ").append(safe(actorUsername));
        return sb.toString();
    }

    /**
     * Builds a human-readable description for rule toggle operations.
     *
     * <p>Example output:
     * {@code "An entitlement assignment rule was enabled "Finance Admin Access" by john.doe"}
     *
     * @param baseDescription the base description constant (enabled/disabled)
     * @param ruleName        the rule's display name
     * @param actorUsername   the actor's username
     * @return formatted description string
     */
    private static String buildRuleToggleDescription(String baseDescription,
                                                      String ruleName,
                                                      String actorUsername) {
        return baseDescription
                + "  \"" + safe(ruleName) + "\""
                + " by " + safe(actorUsername);
    }

    /**
     * Builds a human-readable description for rule evaluation events.
     *
     * <p>Dynamically selects the base description based on the trigger source
     * and appends evaluation metrics.
     *
     * <p>Example output:
     * {@code "Entitlement assignment rule was automatically evaluated after creation "Finance Admin Access" for entitlement "Admin Role" by john.doe matched 15 identities, created 12 assignments, revoked 3 assignments"}
     *
     * @param triggerSource      the trigger source string
     * @param ruleName           the rule's display name
     * @param entitlementName    the entitlement's display name
     * @param actorUsername      the actor's username
     * @param identitiesMatched  the number of identities that matched
     * @param assignmentsCreated the number of assignments created
     * @param assignmentsRevoked the number of assignments revoked
     * @return formatted description string
     */
    private static String buildEvaluationDescription(String triggerSource,
                                                      String ruleName,
                                                      String entitlementName,
                                                      String actorUsername,
                                                      int identitiesMatched,
                                                      int assignmentsCreated,
                                                      int assignmentsRevoked) {
        String baseDesc = resolveEvaluationBaseDescription(triggerSource);

        StringBuilder sb = new StringBuilder(baseDesc);
        sb.append(" \"").append(safe(ruleName)).append("\"");

        if (entitlementName != null && !entitlementName.isBlank()) {
            sb.append(" for entitlement \"").append(entitlementName).append("\"");
        }

        sb.append(" by ").append(safe(actorUsername));

        sb.append(" matched ").append(identitiesMatched).append(" identit")
                .append(identitiesMatched == 1 ? "y" : "ies");
        sb.append(", created ").append(assignmentsCreated).append(" assignment")
                .append(assignmentsCreated == 1 ? "" : "s");
        sb.append(", revoked ").append(assignmentsRevoked).append(" assignment")
                .append(assignmentsRevoked == 1 ? "" : "s");

        return sb.toString();
    }

    /**
     * Maps a trigger source string to its human-readable evaluation description.
     *
     * @param triggerSource the trigger source constant
     * @return the corresponding base description
     */
    private static String resolveEvaluationBaseDescription(String triggerSource) {
        if (triggerSource == null) {
            return AppConstants.AUDIT_DESC_RULE_EVALUATED_PREFIX;
        }
        return switch (triggerSource) {
            case AppConstants.AUDIT_TRIGGER_SOURCE_ENTITLEMENT_RULE_CREATED ->
                    AppConstants.AUDIT_DESC_RULE_EVALUATED_VIA_CREATE;
            case AppConstants.AUDIT_TRIGGER_SOURCE_ENTITLEMENT_RULE_UPDATED ->
                    AppConstants.AUDIT_DESC_RULE_EVALUATED_VIA_UPDATE;
            case AppConstants.AUDIT_TRIGGER_SOURCE_CRON ->
                    AppConstants.AUDIT_DESC_RULE_EVALUATED_VIA_CRON;
            case AppConstants.AUDIT_TRIGGER_SOURCE_EVALUATE_ALL ->
                    AppConstants.AUDIT_DESC_RULE_EVALUATED_VIA_EVALUATE_ALL;
            case AppConstants.AUDIT_TRIGGER_SOURCE_API ->
                    AppConstants.AUDIT_DESC_RULE_EVALUATED_VIA_API;
            default -> AppConstants.AUDIT_DESC_RULE_EVALUATED_PREFIX;
        };
    }

    /**
     * Null-safe string accessor returns {@code "Unknown"} for null or blank input.
     *
     * @param value the input string
     * @return the input string, or {@code "Unknown"} if null/blank
     */
    private static String safe(String value) {
        return (value != null && !value.isBlank()) ? value : "Unknown";
    }
}