package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for a dynamic access-role assignment rule.
 *
 * <p>The response exposes only the canonical {@code conditionGroups} structure;
 * legacy flat condition output is not provided for access-role rules.</p>
 */
public class AccessRoleAssignmentRuleResponse {

    private UUID id;
    private String tenantId;
    private String name;
    private UUID roleId;
    private String roleName;
    private String roleType;
    private String roleStatus;
    private List<Map<String, Object>> conditionGroups;
    private boolean enabled;
    private String scheduleType;
    private String cronExpression;
    private String scheduleTime;
    private String scheduleDate;
    private String scheduleTimezone;
    private Long lastEvaluatedAt;
    private Long nextExecutionAt;
    private int priority;
    private List<String> identityTypes;
    private String createdBy;
    private Long createdAt;
    private Long updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }

    public String getRoleStatus() { return roleStatus; }
    public void setRoleStatus(String roleStatus) { this.roleStatus = roleStatus; }

    public List<Map<String, Object>> getConditionGroups() { return conditionGroups; }
    public void setConditionGroups(List<Map<String, Object>> conditionGroups) { this.conditionGroups = conditionGroups; }

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

    public Long getLastEvaluatedAt() { return lastEvaluatedAt; }
    public void setLastEvaluatedAt(Long lastEvaluatedAt) { this.lastEvaluatedAt = lastEvaluatedAt; }

    public Long getNextExecutionAt() { return nextExecutionAt; }
    public void setNextExecutionAt(Long nextExecutionAt) { this.nextExecutionAt = nextExecutionAt; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public List<String> getIdentityTypes() { return identityTypes; }
    public void setIdentityTypes(List<String> identityTypes) { this.identityTypes = identityTypes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}

