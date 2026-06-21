package com.aidentx.iam.access_roles_management.model.entity;

import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemPhase;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemStatus;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-row import state for a role import job.
 *
 * <p>Each uploaded role row becomes one item. The original normalized row is
 * stored in {@code payload} as JSONB so the background worker can deserialize it
 * later without re-reading the uploaded file. {@code rowNumber} preserves the
 * source file position for user-friendly item results and error CSV output.</p>
 *
 * <p>The worker updates {@code status}, {@code roleId}, {@code errorCode},
 * {@code errorMessage}, and {@code processedAt}. A successfully created row has
 * {@code status=SUCCEEDED} and a non-null {@code roleId}. A failed row contains
 * a bounded error summary suitable for frontend display.</p>
 */
@Entity
@Table(name = "role_import_job_items", indexes = {
        @Index(name = "idx_role_import_items_job_status", columnList = "job_id, status"),
        @Index(name = "idx_role_import_items_job_phase_status", columnList = "job_id, phase, status"),
        @Index(name = "idx_role_import_items_tenant_job", columnList = "tenant_id, job_id")
})
public class RoleImportJobItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "external_ref")
    private String externalRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 20)
    private RoleImportItemPhase phase = RoleImportItemPhase.CREATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RoleImportItemStatus status = RoleImportItemStatus.QUEUED;

    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "warning_message", columnDefinition = "TEXT")
    private String warningMessage;

    @Type(JsonBinaryType.class)
    @Column(name = "payload", columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }
    public RoleImportItemPhase getPhase() { return phase; }
    public void setPhase(RoleImportItemPhase phase) { this.phase = phase; }
    public RoleImportItemStatus getStatus() { return status; }
    public void setStatus(RoleImportItemStatus status) { this.status = status; }
    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
    public JsonNode getPayload() { return payload; }
    public void setPayload(JsonNode payload) { this.payload = payload; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}

