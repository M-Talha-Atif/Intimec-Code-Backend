package com.aidentx.iam.access_roles_management.model.entity;

import com.aidentx.iam.access_roles_management.model.enums.RoleImportJobStatus;
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

import java.time.Instant;
import java.util.UUID;

/**
 * Header row for an asynchronous access-role import job.
 */
@Entity
@Table(name = "role_import_jobs", indexes = {
        @Index(name = "idx_role_import_jobs_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_role_import_jobs_tenant_created", columnList = "tenant_id, created_at")
})
public class RoleImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "actor_username")
    private String actorUsername;

    @Column(name = "format", nullable = false, length = 20)
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private RoleImportJobStatus status = RoleImportJobStatus.PENDING;

    @Column(name = "total_count", nullable = false)
    private long totalCount;

    @Column(name = "succeeded_count", nullable = false)
    private long succeededCount;

    @Column(name = "failed_count", nullable = false)
    private long failedCount;

    @Column(name = "skipped_count", nullable = false)
    private long skippedCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public RoleImportJobStatus getStatus() { return status; }
    public void setStatus(RoleImportJobStatus status) { this.status = status; }
    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    public long getSucceededCount() { return succeededCount; }
    public void setSucceededCount(long succeededCount) { this.succeededCount = succeededCount; }
    public long getFailedCount() { return failedCount; }
    public void setFailedCount(long failedCount) { this.failedCount = failedCount; }
    public long getSkippedCount() { return skippedCount; }
    public void setSkippedCount(long skippedCount) { this.skippedCount = skippedCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

