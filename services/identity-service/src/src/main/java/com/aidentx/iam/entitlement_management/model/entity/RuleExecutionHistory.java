package com.aidentx.iam.entitlement_management.model.entity;

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
 * Run-level execution history for a dynamic entitlement assignment rule.
 *
 * <p>One record is created per rule evaluation execution. Captures the
 * complete context of the evaluation including:
 * <ul>
 *   <li>Who/what triggered it (manual, cron, immediate on create/update)</li>
 *   <li>Snapshot of the conditions at time of execution (immutable audit trail)</li>
 *   <li>Aggregate counts (matched, assigned, revoked, skipped, errors)</li>
 *   <li>Duration and status</li>
 * </ul>
 *
 * <p>Even if the rule's conditions are changed later, this record preserves
 * exactly what was evaluated and when. This is required for compliance
 * reporting and audit trails.
 */
@Entity
@Table(name = "rule_execution_history", indexes = {
        @Index(name = "idx_reh_tenant", columnList = "tenant_id"),
        @Index(name = "idx_reh_rule", columnList = "tenant_id, rule_id"),
        @Index(name = "idx_reh_entitlement", columnList = "tenant_id, entitlement_id"),
        @Index(name = "idx_reh_status", columnList = "tenant_id, status"),
        @Index(name = "idx_reh_started_at", columnList = "tenant_id, started_at"),
        @Index(name = "idx_reh_trigger", columnList = "tenant_id, trigger_type")
})
public class RuleExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotNull
    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    /**
     * Snapshot of rule name at time of execution.
     * Preserved even if the rule is renamed or deleted later.
     */
    @NotBlank
    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @NotNull
    @Column(name = "entitlement_id", nullable = false)
    private UUID entitlementId;

    /**
     * Snapshot of entitlement display name at time of execution.
     */
    @Column(name = "entitlement_display_name")
    private String entitlementDisplayName;

    @NotNull
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    /**
     * What triggered this evaluation.
     * IMMEDIATE_CREATE - rule was just created with IMMEDIATE schedule type.
     * IMMEDIATE_UPDATE - rule was just updated with IMMEDIATE schedule type.
     * MANUAL - admin manually triggered evaluation via API.
     * CRON - triggered by the scheduled cron job.
     * EVALUATE_ALL - triggered by the evaluate-all endpoint.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 30)
    private TriggerType triggerType;

    /**
     * Who triggered this evaluation.
     * User ID for manual triggers, "SYSTEM" for automatic triggers.
     */
    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;

    /**
     * Immutable snapshot of the exact conditions evaluated in this run.
     * Even if the rule's conditions are changed later, this preserves
     * the evaluation criteria used for this specific execution.
     */
    @NotNull
    @Type(JsonBinaryType.class)
    @Column(name = "conditions_snapshot", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> conditionsSnapshot;

    /**
     * Identity types that were queried (EMPLOYEE, NON_EMPLOYEE, NON_HUMAN).
     */
    @Type(JsonBinaryType.class)
    @Column(name = "identity_types", columnDefinition = "jsonb")
    private List<String> identityTypes;

    /**
     * Overall status of this execution.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExecutionStatus status;

    // ===================================================================
    //  Aggregate Counts
    // ===================================================================

    /**
     * Total identities that matched the rule conditions.
     */
    @Column(name = "identities_matched", nullable = false)
    private int identitiesMatched = 0;

    /**
     * New entitlement assignments created in this run.
     */
    @Column(name = "assignments_created", nullable = false)
    private int assignmentsCreated = 0;

    /**
     * Stale assignments revoked in this run (identity no longer matches).
     */
    @Column(name = "assignments_revoked", nullable = false)
    private int assignmentsRevoked = 0;

    /**
     * Identities that matched but had no shadow/account on the target resource.
     */
    @Column(name = "skipped_no_shadow", nullable = false)
    private int skippedNoShadow = 0;

    /**
     * Identities that matched and already had the assignment (no-op).
     */
    @Column(name = "skipped_already_assigned", nullable = false)
    private int skippedAlreadyAssigned = 0;

    /**
     * Number of individual identity-level errors during this run.
     */
    @Column(name = "error_count", nullable = false)
    private int errorCount = 0;

    /**
     * Top-level error message if the entire execution failed.
     * Null if status is SUCCESS or PARTIAL_FAILURE.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ===================================================================
    //  Timing
    // ===================================================================

    @Column(name = "started_at", nullable = false)
    private Long startedAt;

    @Column(name = "completed_at")
    private Long completedAt;

    /**
     * Total execution duration in milliseconds.
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    public enum TriggerType {
    ENTITLEMENT_RULE_CREATED,
    ENTITLEMENT_RULE_UPDATED,
    MANUAL,
    CRON,
    EVALUATE_ALL
}
    public enum ExecutionStatus {
        /** All identities processed without errors */
        SUCCESS,
        /** Some identities failed but others succeeded */
        PARTIAL_FAILURE,
        /** Entire evaluation failed (e.g., invalid conditions, DB error) */
        FAILURE
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public UUID getEntitlementId() { return entitlementId; }
    public void setEntitlementId(UUID entitlementId) { this.entitlementId = entitlementId; }

    public String getEntitlementDisplayName() { return entitlementDisplayName; }
    public void setEntitlementDisplayName(String entitlementDisplayName) { this.entitlementDisplayName = entitlementDisplayName; }

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }

    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public List<Map<String, Object>> getConditionsSnapshot() { return conditionsSnapshot; }
    public void setConditionsSnapshot(List<Map<String, Object>> conditionsSnapshot) { this.conditionsSnapshot = conditionsSnapshot; }

    public List<String> getIdentityTypes() { return identityTypes; }
    public void setIdentityTypes(List<String> identityTypes) { this.identityTypes = identityTypes; }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public int getIdentitiesMatched() { return identitiesMatched; }
    public void setIdentitiesMatched(int identitiesMatched) { this.identitiesMatched = identitiesMatched; }

    public int getAssignmentsCreated() { return assignmentsCreated; }
    public void setAssignmentsCreated(int assignmentsCreated) { this.assignmentsCreated = assignmentsCreated; }

    public int getAssignmentsRevoked() { return assignmentsRevoked; }
    public void setAssignmentsRevoked(int assignmentsRevoked) { this.assignmentsRevoked = assignmentsRevoked; }

    public int getSkippedNoShadow() { return skippedNoShadow; }
    public void setSkippedNoShadow(int skippedNoShadow) { this.skippedNoShadow = skippedNoShadow; }

    public int getSkippedAlreadyAssigned() { return skippedAlreadyAssigned; }
    public void setSkippedAlreadyAssigned(int skippedAlreadyAssigned) { this.skippedAlreadyAssigned = skippedAlreadyAssigned; }

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
