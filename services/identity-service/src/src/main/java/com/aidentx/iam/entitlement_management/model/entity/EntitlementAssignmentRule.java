package com.aidentx.iam.entitlement_management.model.entity;

import com.aidentx.iam.entitlement_management.converter.JsonbConditionsConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity representing a dynamic entitlement assignment rule.
 *
 * <p>A rule defines conditions under which identities are automatically
 * assigned (or revoked from) an entitlement. Conditions are stored as
 * JSONB in a grouped format supporting:
 * <ul>
 *   <li>Multiple condition groups with per-group AND/OR operator</li>
 *   <li>Per-group negation (NOT)</li>
 *   <li>Left-to-right group chaining</li>
 * </ul>
 *
 * <p>Schedule types control when and how rules are evaluated:
 * <ul>
 *   <li>{@code IMMEDIATE} — auto-evaluated after every create/update</li>
 *   <li>{@code CRON} — evaluated on a schedule</li>
 *   <li>{@code MANUAL} — only evaluated via explicit API call</li>
 * </ul>
 *
 * <p>Table: {@code entitlement_assignment_rules}
 */
@Entity
@Table(name = "entitlement_assignment_rules")
public class EntitlementAssignmentRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "entitlement_id", nullable = false)
    private UUID entitlementId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    /**
     * Rule conditions stored as JSONB in the grouped format.
     *
     * <p>Canonical format:
     * <pre>
     * [
     *   {
     *     "groupOperator": "AND",
     *     "negated": false,
     *     "conditions": [
     *       {"field": "department", "operator": "EQUALS", "value": "Engineering"},
     *       {"field": "jobTitle", "operator": "STARTS_WITH", "value": "Senior"}
     *     ]
     *   },
     *   {
     *     "groupOperator": "OR",
     *     "negated": true,
     *     "conditions": [
     *       {"field": "location", "operator": "IN", "values": ["NYC", "London"]}
     *     ]
     *   }
     * ]
     * </pre>
     *
     * <p>Legacy flat format is also supported for backward compatibility:
     * <pre>
     * [
     *   {"field": "department", "operator": "EQUALS", "value": "Engineering", "logicalOperator": "AND"},
     *   {"field": "jobTitle", "operator": "STARTS_WITH", "value": "Senior"}
     * ]
     * </pre>
     *
     * <p>The evaluation engine auto-detects the format and handles both.
     * On the next update via the API, legacy format is migrated to grouped format.
     */
    @Column(name = "conditions", columnDefinition = "jsonb")
    @Convert(converter = JsonbConditionsConverter.class)
    private List<Map<String, Object>> conditions;

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

    /**
     * Determines when and how a rule is evaluated.
     *
     * <ul>
     *   <li>{@code IMMEDIATE} — auto-evaluated after every create/update via
     *       {@code @TransactionalEventListener(AFTER_COMMIT)}. Ensures assignments
     *       are always in sync with the latest rule conditions.</li>
     *   <li>{@code CRON} — evaluated on a schedule by the cron poller.
     *       Uses {@code next_execution_at} to determine when to fire.</li>
     *   <li>{@code MANUAL} — never auto-evaluated. Rules are created and updated
     *       but evaluation only happens when an admin explicitly triggers it
     *       via the evaluate API endpoint. Useful for staging rules before
     *       activating them or for rules that should only run on-demand.</li>
     * </ul>
     */
    public enum ScheduleType {
        IMMEDIATE,
        CRON,
        MANUAL
    }

    // --- Getters and Setters ---

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

    public List<Map<String, Object>> getConditions() { return conditions; }
    public void setConditions(List<Map<String, Object>> conditions) { this.conditions = conditions; }

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