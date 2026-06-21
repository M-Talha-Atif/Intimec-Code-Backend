package com.aidentx.iam.access_roles_management.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Identity-level detail row for an access-role assignment-rule execution.
 *
 * <p>Each row records what happened for one identity during one execution run:
 * a role assignment was created/reactivated, it already existed, it was revoked,
 * the identity did not match anymore, or identity processing failed.</p>
 */
@Entity
@Table(name = "access_role_assignment_execution_items", indexes = {
        @Index(name = "idx_araei_execution", columnList = "execution_id"),
        @Index(name = "idx_araei_tenant", columnList = "tenant_id"),
        @Index(name = "idx_araei_identity", columnList = "tenant_id, identity_id"),
        @Index(name = "idx_araei_rule_identity", columnList = "tenant_id, rule_id, identity_id"),
        @Index(name = "idx_araei_role_identity", columnList = "tenant_id, role_id, identity_id"),
        @Index(name = "idx_araei_outcome", columnList = "tenant_id, execution_id, outcome"),
        @Index(name = "idx_araei_processed_at", columnList = "tenant_id, processed_at")
})
public class AccessRoleAssignmentExecutionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotNull
    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @NotNull
    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @NotNull
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @NotNull
    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @Column(name = "identity_display_name")
    private String identityDisplayName;

    @Column(name = "identity_type", length = 30)
    private String identityType;

    /** IdentityRoleAssignment row affected by this item, when applicable. */
    @Column(name = "assignment_id")
    private UUID assignmentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 40)
    private Outcome outcome;

    /** Immutable evidence snapshot for why this identity did or did not match. */
    @Type(JsonBinaryType.class)
    @Column(name = "match_evidence", columnDefinition = "jsonb")
    private Map<String, Object> matchEvidence;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at", nullable = false)
    private Long processedAt;

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = Instant.now().toEpochMilli();
        }
    }

    public enum Outcome {
        /** Identity matched and a new role assignment was created. */
        ASSIGNED,
        /** Identity matched and a previously revoked assignment was reactivated. */
        REACTIVATED,
        /** Identity matched but already had this role from the same source. */
        ALREADY_ASSIGNED,
        /** Identity no longer matched and the role assignment was revoked. */
        REVOKED,
        /** Identity was scanned but did not match the rule criteria. */
        SKIPPED_NOT_MATCHED,
        /** Processing failed for this specific identity. */
        ERROR
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public UUID getIdentityId() { return identityId; }
    public void setIdentityId(UUID identityId) { this.identityId = identityId; }

    public String getIdentityDisplayName() { return identityDisplayName; }
    public void setIdentityDisplayName(String identityDisplayName) { this.identityDisplayName = identityDisplayName; }

    public String getIdentityType() { return identityType; }
    public void setIdentityType(String identityType) { this.identityType = identityType; }

    public UUID getAssignmentId() { return assignmentId; }
    public void setAssignmentId(UUID assignmentId) { this.assignmentId = assignmentId; }

    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }

    public Map<String, Object> getMatchEvidence() { return matchEvidence; }
    public void setMatchEvidence(Map<String, Object> matchEvidence) { this.matchEvidence = matchEvidence; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getProcessedAt() { return processedAt; }
    public void setProcessedAt(Long processedAt) { this.processedAt = processedAt; }
}

