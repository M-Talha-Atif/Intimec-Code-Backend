package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for bulk multi-role assignment operations.
 *
 * <p>Used for both synchronous and asynchronous processing results.</p>
 */
public class BulkMultiRoleAssignmentResponse {

    private UUID batchId;
    private String status;               // PENDING, PROCESSING, COMPLETED, FAILED
    private int totalAssignments;
    private int completedCount;
    private int failedCount;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String estimatedCompletionTime;
    private String trackingUrl;

    public BulkMultiRoleAssignmentResponse(
            UUID batchId,
            String status,
            int totalAssignments,
            int completedCount,
            int failedCount,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt,
            String estimatedCompletionTime,
            String trackingUrl
    ) {
        this.batchId = batchId;
        this.status = status;
        this.totalAssignments = totalAssignments;
        this.completedCount = completedCount;
        this.failedCount = failedCount;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.estimatedCompletionTime = estimatedCompletionTime;
        this.trackingUrl = trackingUrl;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public String getStatus() {
        return status;
    }

    public int getTotalAssignments() {
        return totalAssignments;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getEstimatedCompletionTime() {
        return estimatedCompletionTime;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }
}
