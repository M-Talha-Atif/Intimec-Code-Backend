package com.aidentx.iam.entitlement_management.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for entitlement audit history records returned to the UI.
 *
 * <p>Contains only the fields the UI needs — no internal IDs,
 * no raw entity references, no PII beyond the actor username.
 *
 * @param id              the audit record UUID
 * @param actorUsername    who performed the action
 * @param activity        user-friendly action label (e.g. "Governance Updated")
 * @param description     human-readable summary
 * @param triggerSource   what initiated the action (e.g. "API", "CRON")
 * @param entitlementName the entitlement display name at time of change
 * @param ruleName        the rule name if applicable (nullable)
 * @param stateBefore     JSON snapshot before the change (nullable)
 * @param stateAfter      JSON snapshot after the change (nullable)
 * @param changedFields   list of field names that changed (nullable)
 * @param createdAt       epoch millis timestamp
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntitlementAuditHistoryDto(
        String id,
        String actorUsername,
        String activity,
        String description,
        String triggerSource,
        String entitlementName,
        String ruleName,
        Object stateBefore,
        Object stateAfter,
        Object changedFields,
        Long createdAt
) {
}