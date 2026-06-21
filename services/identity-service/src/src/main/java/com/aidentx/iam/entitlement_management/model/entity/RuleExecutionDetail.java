package com.aidentx.iam.entitlement_management.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.UUID;

/**
 * Identity-level detail record for a rule execution.
 *
 * <p>One record per identity affected in a rule evaluation run.
 * Only created for actionable outcomes:
 * <ul>
 *   <li>ASSIGNED - new assignment was created</li>
 *   <li>SKIPPED_NO_SHADOW - identity matched but has no account on the target resource</li>
 *   <li>REVOKED - identity no longer matches, assignment removed</li>
 *   <li>ERROR - processing failed for this specific identity</li>
 * </ul>
 *
 * <p>ALREADY_ASSIGNED outcomes are NOT recorded (lazy detail capture).
 * After the first evaluation, most re-evaluations are no-ops. Recording
 * every ALREADY_ASSIGNED would produce millions of low-value rows.
 *
 * <p>The matchEvidence field captures the actual field values and per-condition
 * evaluation results at the time of processing. This is the compliance receipt
 * that proves why a decision was made even if the rule or identity data changes later.
 */
@Entity
@Table(name = "rule_execution_details", indexes = {
        @Index(name = "idx_red_execution", columnList = "execution_id"),
        @Index(name = "idx_red_tenant", columnList = "tenant_id"),
        @Index(name = "idx_red_identity", columnList = "tenant_id, identity_id"),
        @Index(name = "idx_red_rule_identity", columnList = "tenant_id, rule_id, identity_id"),
        @Index(name = "idx_red_outcome", columnList = "tenant_id, execution_id, outcome"),
        @Index(name = "idx_red_processed_at", columnList = "tenant_id, processed_at")
})
public class RuleExecutionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * FK to the run-level execution record.
     */
    @NotNull
    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @NotNull
    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @NotNull
    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    /**
     * Snapshot of the identity's display name at time of processing.
     */
    @Column(name = "identity_display_name")
    private String identityDisplayName;

    /**
     * The result of processing this identity.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 30)
    private Outcome outcome;

    /**
     * The shadow/account ID used for the assignment.
     * Null if outcome is SKIPPED_NO_SHADOW or ERROR.
     */
    @Column(name = "shadow_id")
    private UUID shadowId;

    /**
     * Immutable compliance receipt capturing the exact field values
     * and per-condition evaluation results at the time of processing.
     *
     * <p>Example:
     * <pre>
     * {
     *   "identityFields": {
     *     "department": "Engineering",
     *     "jobTitle": "Senior Software Engineer",
     *     "email": "talha.khan@company.com",
     *     "employeeId": "EMP-001"
     *   },
     *   "conditionsEvaluated": [
     *     {
     *       "field": "department",
     *       "operator": "EQUALS",
     *       "expectedValue": "Engineering",
     *       "actualValue": "Engineering",
     *       "matched": true
     *     },
     *     {
     *       "logicalOperator": "AND",
     *       "field": "jobTitle",
     *       "operator": "STARTS_WITH",
     *       "expectedValue": "Senior",
     *       "actualValue": "Senior Software Engineer",
     *       "matched": true
     *     }
     *   ],
     *   "overallMatch": true,
     *   "shadowFound": true,
     *   "shadowId": "22222222-aaaa-bbbb-cccc-000000000001"
     * }
     * </pre>
     */
    @Type(JsonBinaryType.class)
    @Column(name = "match_evidence", columnDefinition = "jsonb")
    private Map<String, Object> matchEvidence;

    /**
     * Error message if outcome is ERROR.
     * Null for all other outcomes.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at", nullable = false)
    private Long processedAt;

    public enum Outcome {
        /** New entitlement assignment was created for this identity */
        ASSIGNED,
        /** Identity matched conditions but has no shadow/account on the target resource */
        SKIPPED_NO_SHADOW,
        /** Identity no longer matches conditions, assignment was revoked */
        REVOKED,
        /** Processing failed for this specific identity */
        ERROR
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public UUID getIdentityId() { return identityId; }
    public void setIdentityId(UUID identityId) { this.identityId = identityId; }

    public String getIdentityDisplayName() { return identityDisplayName; }
    public void setIdentityDisplayName(String identityDisplayName) { this.identityDisplayName = identityDisplayName; }

    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }

    public UUID getShadowId() { return shadowId; }
    public void setShadowId(UUID shadowId) { this.shadowId = shadowId; }

    public Map<String, Object> getMatchEvidence() { return matchEvidence; }
    public void setMatchEvidence(Map<String, Object> matchEvidence) { this.matchEvidence = matchEvidence; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getProcessedAt() { return processedAt; }
    public void setProcessedAt(Long processedAt) { this.processedAt = processedAt; }
}