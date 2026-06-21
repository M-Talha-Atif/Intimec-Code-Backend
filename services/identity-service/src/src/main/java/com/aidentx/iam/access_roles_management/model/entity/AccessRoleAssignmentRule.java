package com.aidentx.iam.access_roles_management.model.entity;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.entitlement_management.converter.JsonbConditionsConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity representing a dynamic access-role assignment rule.
 *
 * <p>A rule defines grouped identity-attribute conditions under which identities
 * should be assigned an access role. Unlike entitlement assignment rules, this
 * rule targets a role via {@code roleId}; it does not store {@code entitlementId}
 * or {@code resourceId} because access roles can aggregate entitlements across
 * multiple resources and inherited child roles.</p>
 *
 * <p>Only the canonical grouped condition format is stored in
 * {@code conditionGroups}. Legacy flat conditions are intentionally not modeled
 * for access-role rules.</p>
 */
@Entity
@Table(name = AccessRoleAssignmentRuleConstants.TABLE_ACCESS_ROLE_ASSIGNMENT_RULES, indexes = {
        @Index(name = "idx_ar_assignment_rules_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ar_assignment_rules_tenant_role", columnList = "tenant_id, role_id", unique = true),
        @Index(name = "idx_ar_assignment_rules_schedule", columnList = "tenant_id, schedule_type, enabled"),
        @Index(name = "idx_ar_assignment_rules_next_exec", columnList = "tenant_id, next_execution_at")
})
public class AccessRoleAssignmentRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    /**
     * Canonical grouped conditions stored as JSONB.
     *
     * <p>Each group may include leaf conditions and nested condition groups.
     * Groups support {@code groupOperator}, {@code interGroupOperator}, and
     * {@code negated} metadata.</p>
     */
    @Column(name = "condition_groups", columnDefinition = "jsonb")
    @Convert(converter = JsonbConditionsConverter.class)
    private List<Map<String, Object>> conditionGroups;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType = ScheduleType.MANUAL;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "schedule_time")
    private String scheduleTime;

    @Column(name = "schedule_date")
    private String scheduleDate;

    @Column(name = "schedule_timezone")
    private String scheduleTimezone;

    @Column(name = "last_evaluated_at")
    private Long lastEvaluatedAt;

    @Column(name = "next_execution_at")
    private Long nextExecutionAt;

    @Column(name = "priority")
    private int priority = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "identity_types", columnDefinition = "jsonb")
    private List<String> identityTypes;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = Instant.now().toEpochMilli();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now().toEpochMilli();
    }

    /**
     * Determines when and how the access-role assignment rule is evaluated.
     */
    public enum ScheduleType {
        IMMEDIATE,
        CRON,
        MANUAL
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public List<Map<String, Object>> getConditionGroups() { return conditionGroups; }
    public void setConditionGroups(List<Map<String, Object>> conditionGroups) { this.conditionGroups = conditionGroups; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

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

