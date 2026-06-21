package com.aidentx.iam.access_roles_management.model.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a dynamic access-role assignment rule.
 *
 * <p>The rule targets a single access role via {@code roleId}. Conditions are
 * accepted only in the canonical grouped format through {@code conditionGroups};
 * legacy flat {@code conditions} are intentionally not supported for access-role
 * rules. Schedule fields are retained for backward compatibility but ignored;
 * access-role assignment rules always inherit the tenant global schedule.</p>
 */
public class CreateAccessRoleAssignmentRuleRequest {

    @NotNull
    private UUID roleId;

    /** Canonical grouped identity conditions used to match identities. */
    private List<AccessRoleAssignmentRuleConditionGroupDto> conditionGroups;

    private boolean enabled = true;
    /** Ignored; scheduling is controlled by the tenant global schedule. */
    private String scheduleType;
    /** Ignored; scheduling is controlled by the tenant global schedule. */
    private String cronExpression;
    /** Ignored; scheduling is controlled by the tenant global schedule. */
    private String scheduleTime;
    /** Ignored; scheduling is controlled by the tenant global schedule. */
    private String scheduleDate;
    /** Ignored; scheduling is controlled by the tenant global schedule. */
    private String scheduleTimezone;
    private Integer priority;
    private List<String> identityTypes;

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public List<AccessRoleAssignmentRuleConditionGroupDto> getConditionGroups() { return conditionGroups; }
    public void setConditionGroups(List<AccessRoleAssignmentRuleConditionGroupDto> conditionGroups) { this.conditionGroups = conditionGroups; }

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

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public List<String> getIdentityTypes() { return identityTypes; }
    public void setIdentityTypes(List<String> identityTypes) { this.identityTypes = identityTypes; }
}

