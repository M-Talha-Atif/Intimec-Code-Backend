package com.aidentx.iam.organization_management.model.entity;

import com.aidentx.iam.organization_management.model.enums.HierarchyType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent record of a failed attempt to resolve a source-side org value
 * (e.g. {@code department}, {@code costCenter}, {@code externalId}) to an
 * {@link OrgUnit} during reconciliation.
 *
 * <p>Captured by {@code IdentityOrgSyncService} whenever
 * {@code findActiveByNameCodeOrExternalId} returns no matches or is
 * ambiguous. Operators consume these rows from the admin
 * {@code org-resolution-failures} endpoint to triage onboarding gaps
 * (missing org units, duplicate names, mis-typed CSV imports).
 *
 * <p>The {@link #resolved} flag is flipped to {@code true} by a subsequent
 * reconciliation run that successfully resolves the same identity-side
 * value, providing a self-clearing audit trail.
 */
@Entity
@Table(name = "org_resolution_failures", indexes = {
        @Index(name = "idx_org_res_fail_tenant_resolved", columnList = "tenant_id,resolved"),
        @Index(name = "idx_org_res_fail_identity", columnList = "tenant_id,identity_id"),
        @Index(name = "idx_org_res_fail_value", columnList = "tenant_id,attempted_value"),
        @Index(name = "idx_org_res_fail_run", columnList = "tenant_id,task_run_id")
})
public class OrgResolutionFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @NotNull
    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @NotBlank
    @Column(name = "attempted_value", nullable = false, length = 512)
    private String attemptedValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "hierarchy_type", nullable = false, length = 32)
    private HierarchyType hierarchyType;

    /** Short machine-readable reason: {@code NO_MATCH}, {@code AMBIGUOUS}, {@code ERROR}. */
    @NotBlank
    @Column(name = "reason", nullable = false, length = 64)
    private String reason;

    /** Human-readable detail; e.g. number of ambiguous matches or exception message. */
    @Column(name = "detail", length = 1024)
    private String detail;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /**
     * Optional id of the task run that produced this failure (set when the
     * resolution attempt is part of a reconciliation finalize pass).
     * Null for inline best-effort calls during import or HR sync — those
     * failures are still captured but not tied to a specific task run.
     */
    @Column(name = "task_run_id", length = 64)
    private String taskRunId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public UUID getIdentityId() { return identityId; }
    public void setIdentityId(UUID identityId) { this.identityId = identityId; }
    public String getAttemptedValue() { return attemptedValue; }
    public void setAttemptedValue(String attemptedValue) { this.attemptedValue = attemptedValue; }
    public HierarchyType getHierarchyType() { return hierarchyType; }
    public void setHierarchyType(HierarchyType hierarchyType) { this.hierarchyType = hierarchyType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getTaskRunId() { return taskRunId; }
    public void setTaskRunId(String taskRunId) { this.taskRunId = taskRunId; }
}
