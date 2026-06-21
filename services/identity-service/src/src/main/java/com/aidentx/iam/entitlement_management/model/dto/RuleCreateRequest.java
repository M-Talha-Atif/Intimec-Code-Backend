package com.aidentx.iam.entitlement_management.model.dto;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a dynamic entitlement assignment rule.
 *
 * <p>Supports two condition formats for backward compatibility:
 * <ul>
 *   <li><b>{@code conditionGroups}</b> (preferred): Grouped conditions with
 *       per-group logical operator and negation. This is the canonical format.</li>
 *   <li><b>{@code conditions}</b> (legacy): Flat list of conditions. If provided
 *       without {@code conditionGroups}, the service wraps them into a single
 *       group with {@code groupOperator=AND} and {@code negated=false}.</li>
 * </ul>
 *
 * <p>If both are provided, {@code conditionGroups} takes precedence.
 *
 * <p>Rules default to {@code scheduleType=MANUAL} when not specified.
 * The schedule type controls how and when the rule is evaluated:
 * <ul>
 *   <li>{@code MANUAL} — only via explicit API call (Run Now)</li>
 *   <li>{@code IMMEDIATE} — auto-evaluated on create/update</li>
 *   <li>{@code CRON} — evaluated on a schedule</li>
 * </ul>
 */
public class RuleCreateRequest {

    private UUID entitlementId;

    /**
     * Grouped conditions (preferred format).
     *
     * <p>Each group contains conditions combined by the group's operator,
     * with optional group-level negation. Groups chain left-to-right.
     *
     * @see RuleConditionGroupDto
     */
    private List<RuleConditionGroupDto> conditionGroups;

    /**
     * Flat conditions (legacy format, backward compatible).
     *
     * <p>If {@code conditionGroups} is null or empty but {@code conditions}
     * is provided, the service wraps them into a single group with
     * {@code groupOperator=AND} and {@code negated=false}.
     */
    private List<RuleConditionDto> conditions;

    private boolean enabled = true;
    private String scheduleType;
    private String cronExpression;
    private String scheduleTime;
    private String scheduleDate;
    private String scheduleTimezone;
    private List<String> identityTypes;

    public UUID getEntitlementId() { return entitlementId; }
    public void setEntitlementId(UUID entitlementId) { this.entitlementId = entitlementId; }

    public List<RuleConditionGroupDto> getConditionGroups() { return conditionGroups; }
    public void setConditionGroups(List<RuleConditionGroupDto> conditionGroups) { this.conditionGroups = conditionGroups; }

    public List<RuleConditionDto> getConditions() { return conditions; }
    public void setConditions(List<RuleConditionDto> conditions) { this.conditions = conditions; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getScheduleTime() { return scheduleTime; }
    public void setScheduleTime(String scheduleTime) { this.scheduleTime = scheduleTime; }

    public String getScheduleDate() { return scheduleDate; }
    public void setScheduleDate(String scheduleDate) { this.scheduleDate = scheduleDate; }

    public String getScheduleTimezone() { return scheduleTimezone; }
    public void setScheduleTimezone(String scheduleTimezone) { this.scheduleTimezone = scheduleTimezone; }

    public List<String> getIdentityTypes() { return identityTypes; }
    public void setIdentityTypes(List<String> identityTypes) { this.identityTypes = identityTypes; }

    @JsonSetter("identityType")
    public void setLegacyIdentityType(String identityType) {
        if ((this.identityTypes == null || this.identityTypes.isEmpty())
                && identityType != null && !identityType.isBlank()) {
            this.identityTypes = List.of(identityType);
        }
    }
}