package com.aidentx.iam.entitlement_management.model.dto;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

/**
 * Request DTO for updating an existing dynamic entitlement assignment rule.
 *
 * <p>Only non-null fields are applied. Null fields are left unchanged.
 *
 * <p>Supports two condition formats (same as create):
 * <ul>
 *   <li>{@code conditionGroups} (preferred): takes precedence if provided</li>
 *   <li>{@code conditions} (legacy): wrapped into single group if conditionGroups is absent</li>
 * </ul>
 */
public class RuleUpdateRequest {

    /**
     * Grouped conditions (preferred format).
     * If provided, replaces all existing conditions.
     */
    private List<RuleConditionGroupDto> conditionGroups;

    /**
     * Flat conditions (legacy format).
     * Used only if {@code conditionGroups} is null or empty.
     */
    private List<RuleConditionDto> conditions;

    private Boolean enabled;
    private String scheduleType;
    private String cronExpression;
    private String scheduleTime;
    private String scheduleDate;
    private String scheduleTimezone;
    private List<String> identityTypes;

    public List<RuleConditionGroupDto> getConditionGroups() { return conditionGroups; }
    public void setConditionGroups(List<RuleConditionGroupDto> conditionGroups) { this.conditionGroups = conditionGroups; }

    public List<RuleConditionDto> getConditions() { return conditions; }
    public void setConditions(List<RuleConditionDto> conditions) { this.conditions = conditions; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

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