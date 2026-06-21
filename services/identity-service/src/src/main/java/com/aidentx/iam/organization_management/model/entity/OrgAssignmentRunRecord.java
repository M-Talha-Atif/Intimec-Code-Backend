package com.aidentx.iam.organization_management.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per identity processed during a single reconciliation
 * org-assignment pass. Provides the audit detail rendered in the task-run
 * "Execution Results" UI: what value was attempted, the outcome, the
 * resolved org unit (when applicable), and the previous assignment (when
 * the run reassigned the identity).
 *
 * <p>Written from {@link com.aidentx.iam.organization_management.service.IdentityOrgSyncService}
 * whenever a {@code taskRunId} is supplied. Inline import-time calls (no
 * task-run context) are not persisted here — only summarised failures
 * land in {@code org_resolution_failures}.
 */
@Entity
@Table(name = "org_assignment_run_records", indexes = {
        @Index(name = "idx_org_run_rec_run", columnList = "tenant_id,task_run_id"),
        @Index(name = "idx_org_run_rec_identity", columnList = "tenant_id,identity_id"),
        @Index(name = "idx_org_run_rec_outcome", columnList = "tenant_id,task_run_id,outcome")
})
public class OrgAssignmentRunRecord {

    public enum Outcome {
        /** A new primary FUNCTIONAL assignment was created. */
        ASSIGNED,
        /** Identity was moved from one org unit to another. */
        REASSIGNED,
        /** Identity already pointed to the resolved unit; no write was necessary. */
        UNCHANGED,
        /** No active org unit matched the source value by externalId, code, or name. */
        FAILED_NO_MATCH,
        /** Multiple active org units matched the source value. */
        FAILED_AMBIGUOUS,
        /** Resolution threw an exception; see {@code detail}. */
        FAILED_ERROR,
        /** Source value was null or blank — nothing to resolve. */
        SKIPPED_BLANK
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @NotBlank
    @Column(name = "task_run_id", nullable = false, length = 64)
    private String taskRunId;

    @NotNull
    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @Column(name = "attempted_value", length = 512)
    private String attemptedValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private Outcome outcome;

    @Column(name = "resolved_org_unit_id")
    private UUID resolvedOrgUnitId;

    @Column(name = "previous_org_unit_id")
    private UUID previousOrgUnitId;

    @Column(name = "detail", length = 1024)
    private String detail;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTaskRunId() { return taskRunId; }
    public void setTaskRunId(String taskRunId) { this.taskRunId = taskRunId; }
    public UUID getIdentityId() { return identityId; }
    public void setIdentityId(UUID identityId) { this.identityId = identityId; }
    public String getAttemptedValue() { return attemptedValue; }
    public void setAttemptedValue(String attemptedValue) { this.attemptedValue = attemptedValue; }
    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }
    public UUID getResolvedOrgUnitId() { return resolvedOrgUnitId; }
    public void setResolvedOrgUnitId(UUID resolvedOrgUnitId) { this.resolvedOrgUnitId = resolvedOrgUnitId; }
    public UUID getPreviousOrgUnitId() { return previousOrgUnitId; }
    public void setPreviousOrgUnitId(UUID previousOrgUnitId) { this.previousOrgUnitId = previousOrgUnitId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
