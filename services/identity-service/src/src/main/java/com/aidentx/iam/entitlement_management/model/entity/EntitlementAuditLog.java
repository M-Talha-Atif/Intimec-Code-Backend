package com.aidentx.iam.entitlement_management.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persistent audit trail for all entitlement-related changes.
 *
 * <p>Captures governance updates, rule CRUD operations, and rule evaluation
 * results with full before/after state diffs. Each record is immutable
 * after creation  audit logs are append-only.
 *
 * <p>This table complements the Kafka audit pipeline by providing a
 * queryable, tenant-scoped history that can be served directly to the
 * governance dashboard without depending on Kafka consumers.
 */
@Entity
@Table(name = "entitlement_audit_log", indexes = {
        @Index(name = "idx_eal_tenant_entitlement_created",
                columnList = "tenant_id, entitlement_id, created_at DESC"),
        @Index(name = "idx_eal_tenant_action_created",
                columnList = "tenant_id, action, created_at DESC"),
        @Index(name = "idx_eal_tenant_actor_created",
                columnList = "tenant_id, actor_username, created_at DESC"),
        @Index(name = "idx_eal_tenant_rule_created",
                columnList = "tenant_id, rule_id, created_at DESC")
})
public class EntitlementAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // ===================================================================
    //  Entitlement Context
    // ===================================================================

    @Column(name = "entitlement_id")
    private UUID entitlementId;

    @Column(name = "entitlement_name")
    private String entitlementName;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "resource_name")
    private String resourceName;

    // ===================================================================
    //  Action Context
    // ===================================================================

    /** What happened: GOVERNANCE_UPDATED, RULE_CREATED, RULE_UPDATED, RULE_DELETED, RULE_EVALUATED */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /** What initiated it: API, ENTITLEMENT_RULE_CREATED, ENTITLEMENT_RULE_UPDATED, CRON, EVALUATE_ALL */
    @Column(name = "trigger_source", length = 50)
    private String triggerSource;

    /** Human-readable summary for UI display. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ===================================================================
    //  Actor
    // ===================================================================

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "actor_username")
    private String actorUsername;

    // ===================================================================
    //  State Diff
    // ===================================================================

    @Type(JsonBinaryType.class)
    @Column(name = "state_before", columnDefinition = "jsonb")
    private Object stateBefore;

    @Type(JsonBinaryType.class)
    @Column(name = "state_after", columnDefinition = "jsonb")
    private Object stateAfter;

    /** List of field names that changed, e.g. ["riskLevel", "lifecycle", "requestable"] */
    @Type(JsonBinaryType.class)
    @Column(name = "changed_fields", columnDefinition = "jsonb")
    private List<String> changedFields;

    // ===================================================================
    //  Rule Context (nullable  only populated for rule operations)
    // ===================================================================

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "rule_name")
    private String ruleName;

    @Column(name = "execution_id")
    private UUID executionId;

    // ===================================================================
    //  Evaluation Metrics (nullable only populated for RULE_EVALUATED)
    // ===================================================================

    @Column(name = "identities_matched")
    private Integer identitiesMatched;

    @Column(name = "assignments_created")
    private Integer assignmentsCreated;

    @Column(name = "assignments_revoked")
    private Integer assignmentsRevoked;

    @Column(name = "skipped_no_shadow")
    private Integer skippedNoShadow;

    @Column(name = "duration_ms")
    private Long durationMs;

    // ===================================================================
    //  Timestamp
    // ===================================================================

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now().toEpochMilli();
    }

    // ===================================================================
    //  Getters and Setters
    // ===================================================================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getEntitlementId() { return entitlementId; }
    public void setEntitlementId(UUID entitlementId) { this.entitlementId = entitlementId; }

    public String getEntitlementName() { return entitlementName; }
    public void setEntitlementName(String entitlementName) { this.entitlementName = entitlementName; }

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public Object getStateBefore() { return stateBefore; }
    public void setStateBefore(Object stateBefore) { this.stateBefore = stateBefore; }

    public Object getStateAfter() { return stateAfter; }
    public void setStateAfter(Object stateAfter) { this.stateAfter = stateAfter; }

    public List<String> getChangedFields() { return changedFields; }
    public void setChangedFields(List<String> changedFields) { this.changedFields = changedFields; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }

    public Integer getIdentitiesMatched() { return identitiesMatched; }
    public void setIdentitiesMatched(Integer identitiesMatched) { this.identitiesMatched = identitiesMatched; }

    public Integer getAssignmentsCreated() { return assignmentsCreated; }
    public void setAssignmentsCreated(Integer assignmentsCreated) { this.assignmentsCreated = assignmentsCreated; }

    public Integer getAssignmentsRevoked() { return assignmentsRevoked; }
    public void setAssignmentsRevoked(Integer assignmentsRevoked) { this.assignmentsRevoked = assignmentsRevoked; }

    public Integer getSkippedNoShadow() { return skippedNoShadow; }
    public void setSkippedNoShadow(Integer skippedNoShadow) { this.skippedNoShadow = skippedNoShadow; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
