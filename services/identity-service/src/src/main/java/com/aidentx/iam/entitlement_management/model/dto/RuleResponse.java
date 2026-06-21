package com.aidentx.iam.entitlement_management.model.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for a dynamic entitlement assignment rule.
 *
 * <p>Contains both the canonical grouped condition format ({@code conditionGroups})
 * and the legacy flat format ({@code conditions}) for backward compatibility.
 *
 * <p>The {@code conditionGroups} field is the authoritative representation.
 * The {@code conditions} field is a flattened view — it contains all conditions
 * from all groups in order, without group boundaries. Clients that don't
 * support grouping can still read conditions from this field.
 */
public class RuleResponse {

    private UUID id;
    private String tenantId;
    private String name;
    private UUID entitlementId;
    private UUID resourceId;

    /**
     * Canonical grouped conditions.
     *
     * <p>Each group has a {@code groupOperator} (AND/OR), {@code negated} flag,
     * and a list of conditions. Groups chain left-to-right.
     */
    private List<Map<String, Object>> conditionGroups;

    /**
     * Flattened conditions (legacy, backward compatible).
     *
     * <p>Contains all conditions from all groups in order, without group metadata.
     * Provided for clients that don't support the grouped format.
     */
    private List<Map<String, Object>> conditions;

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

    public UUID getEntitlementId() { return entitlementId; }
    public void setEntitlementId(UUID entitlementId) { this.entitlementId = entitlementId; }

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }

    public List<Map<String, Object>> getConditionGroups() { return conditionGroups; }
    public void setConditionGroups(List<Map<String, Object>> conditionGroups) { this.conditionGroups = conditionGroups; }

    public List<Map<String, Object>> getConditions() { return conditions; }
    public void setConditions(List<Map<String, Object>> conditions) { this.conditions = conditions; }

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