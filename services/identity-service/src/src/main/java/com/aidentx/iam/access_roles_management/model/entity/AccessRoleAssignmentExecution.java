package com.aidentx.iam.access_roles_management.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Run-level execution history for a dynamic access-role assignment rule.
 *
 * <p>One record is created per rule evaluation scan. It preserves the exact
 * rule, target role, identity type set, condition group snapshot, trigger
 * context, aggregate counts, timing, and failure state used for audit and
 * troubleshooting.</p>
 */
@Entity
@Table(name = "access_role_assignment_executions", indexes = {
        @Index(name = "idx_arae_tenant", columnList = "tenant_id"),
        @Index(name = "idx_arae_rule", columnList = "tenant_id, rule_id"),
        @Index(name = "idx_arae_role", columnList = "tenant_id, role_id"),
        @Index(name = "idx_arae_status", columnList = "tenant_id, status"),
        @Index(name = "idx_arae_started_at", columnList = "tenant_id, started_at"),
        @Index(name = "idx_arae_trigger", columnList = "tenant_id, trigger_type")
})
public class AccessRoleAssignmentExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotNull
    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    /** Snapshot of the rule name at execution time. */
    @NotBlank
    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @NotNull
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    /** Snapshot of the access role display/name at execution time. */
    @Column(name = "role_name")
    private String roleName;

    @Column(name = "role_type", length = 50)
    private String roleType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 40)
    private TriggerType triggerType;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;

    /** Immutable snapshot of grouped rule conditions evaluated in this run. */
    @NotNull
    @Type(JsonBinaryType.class)
    @Column(name = "condition_groups_snapshot", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> conditionGroupsSnapshot;

    /** Identity categories scanned by this execution, e.g. EMPLOYEE/NON_EMPLOYEE. */
    @Type(JsonBinaryType.class)
    @Column(name = "identity_types", columnDefinition = "jsonb")
    private List<String> identityTypes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExecutionStatus status = ExecutionStatus.RUNNING;

    @Column(name = "identities_matched", nullable = false)
    private int identitiesMatched = 0;

    @Column(name = "roles_assigned", nullable = false)
    private int rolesAssigned = 0;

    @Column(name = "roles_reactivated", nullable = false)
    private int rolesReactivated = 0;

    @Column(name = "roles_revoked", nullable = false)
    private int rolesRevoked = 0;

    @Column(name = "skipped_already_assigned", nullable = false)
    private int skippedAlreadyAssigned = 0;

    @Column(name = "skipped_not_matched", nullable = false)
    private int skippedNotMatched = 0;

    @Column(name = "error_count", nullable = false)
    private int errorCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Long startedAt;

    @Column(name = "completed_at")
    private Long completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = Instant.now().toEpochMilli();
        }
        if (status == null) {
            status = ExecutionStatus.RUNNING;
        }
    }

    /** Marks the execution as completed and calculates duration. */
    public void complete(ExecutionStatus finalStatus) {
        long now = Instant.now().toEpochMilli();
        this.status = finalStatus;
        this.completedAt = now;
        this.durationMs = startedAt != null ? now - startedAt : null;
    }

    /** Marks the execution as failed with a top-level error. */
    public void fail(String message) {
        this.errorMessage = message;
        this.errorCount = Math.max(this.errorCount, 1);
        complete(ExecutionStatus.FAILURE);
    }

    public enum TriggerType {
        ACCESS_ROLE_RULE_CREATED,
        ACCESS_ROLE_RULE_UPDATED,
        MANUAL,
        CRON,
        EVALUATE_ALL,
        IDENTITY_CHANGED
    }

    public enum ExecutionStatus {
        RUNNING,
        SUCCESS,
        PARTIAL_FAILURE,
        FAILURE
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }

    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public List<Map<String, Object>> getConditionGroupsSnapshot() { return conditionGroupsSnapshot; }
    public void setConditionGroupsSnapshot(List<Map<String, Object>> conditionGroupsSnapshot) { this.conditionGroupsSnapshot = conditionGroupsSnapshot; }

    public List<String> getIdentityTypes() { return identityTypes; }
    public void setIdentityTypes(List<String> identityTypes) { this.identityTypes = identityTypes; }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public int getIdentitiesMatched() { return identitiesMatched; }
    public void setIdentitiesMatched(int identitiesMatched) { this.identitiesMatched = identitiesMatched; }

    public int getRolesAssigned() { return rolesAssigned; }
    public void setRolesAssigned(int rolesAssigned) { this.rolesAssigned = rolesAssigned; }

    public int getRolesReactivated() { return rolesReactivated; }
    public void setRolesReactivated(int rolesReactivated) { this.rolesReactivated = rolesReactivated; }

    public int getRolesRevoked() { return rolesRevoked; }
    public void setRolesRevoked(int rolesRevoked) { this.rolesRevoked = rolesRevoked; }

    public int getSkippedAlreadyAssigned() { return skippedAlreadyAssigned; }
    public void setSkippedAlreadyAssigned(int skippedAlreadyAssigned) { this.skippedAlreadyAssigned = skippedAlreadyAssigned; }

    public int getSkippedNotMatched() { return skippedNotMatched; }
    public void setSkippedNotMatched(int skippedNotMatched) { this.skippedNotMatched = skippedNotMatched; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getStartedAt() { return startedAt; }
    public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}

