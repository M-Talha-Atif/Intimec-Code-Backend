package com.aidentx.iam.access_roles_management.model.dto.request;

import java.util.List;

/**
 * Request DTO for updating an existing access-role assignment rule.
 *
 * <p>Only non-null fields are applied. When {@code conditionGroups} is supplied,
 * it replaces the rule's existing grouped conditions. The target {@code roleId}
 * is intentionally immutable after creation to preserve assignment provenance.
 * Schedule fields are retained for backward compatibility but ignored; access-role
 * assignment rules always inherit the tenant global schedule.</p>
 */
public class UpdateAccessRoleAssignmentRuleRequest {

    private List<AccessRoleAssignmentRuleConditionGroupDto> conditionGroups;
    private Boolean enabled;
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

    public List<AccessRoleAssignmentRuleConditionGroupDto> getConditionGroups() { return conditionGroups; }
    public void setConditionGroups(List<AccessRoleAssignmentRuleConditionGroupDto> conditionGroups) { this.conditionGroups = conditionGroups; }

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

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public List<String> getIdentityTypes() { return identityTypes; }
    public void setIdentityTypes(List<String> identityTypes) { this.identityTypes = identityTypes; }
}

