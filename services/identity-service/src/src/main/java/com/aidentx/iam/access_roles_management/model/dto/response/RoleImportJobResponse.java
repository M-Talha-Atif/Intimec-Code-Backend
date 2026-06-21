package com.aidentx.iam.access_roles_management.model.dto.response;

import com.aidentx.iam.access_roles_management.model.enums.RoleImportJobStatus;

import java.time.Instant;
import java.util.UUID;

/** Summary response for an access-role import job. */
public class RoleImportJobResponse {
    private UUID jobId;
    private String tenantId;
    private RoleImportJobStatus status;
    private long totalCount;
    private long succeededCount;
    private long failedCount;
    private long skippedCount;
    private long createdCount;
    private long fullySucceededCount;
    private long createdWithErrorsCount;
    private long rowsNeedingAttentionCount;
    private int progressPercent;
    private String summaryMessage;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private String statusUrl;
    private String itemsUrl;
    private String errorsUrl;

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
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
    public long getCreatedCount() { return createdCount; }
    public void setCreatedCount(long createdCount) { this.createdCount = createdCount; }
    public long getFullySucceededCount() { return fullySucceededCount; }
    public void setFullySucceededCount(long fullySucceededCount) { this.fullySucceededCount = fullySucceededCount; }
    public long getCreatedWithErrorsCount() { return createdWithErrorsCount; }
    public void setCreatedWithErrorsCount(long createdWithErrorsCount) { this.createdWithErrorsCount = createdWithErrorsCount; }
    public long getRowsNeedingAttentionCount() { return rowsNeedingAttentionCount; }
    public void setRowsNeedingAttentionCount(long rowsNeedingAttentionCount) { this.rowsNeedingAttentionCount = rowsNeedingAttentionCount; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public String getSummaryMessage() { return summaryMessage; }
    public void setSummaryMessage(String summaryMessage) { this.summaryMessage = summaryMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getStatusUrl() { return statusUrl; }
    public void setStatusUrl(String statusUrl) { this.statusUrl = statusUrl; }
    public String getItemsUrl() { return itemsUrl; }
    public void setItemsUrl(String itemsUrl) { this.itemsUrl = itemsUrl; }
    public String getErrorsUrl() { return errorsUrl; }
    public void setErrorsUrl(String errorsUrl) { this.errorsUrl = errorsUrl; }
}

