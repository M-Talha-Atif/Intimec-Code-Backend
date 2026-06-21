package com.aidentx.applicationpermissions.roles_management.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing an asynchronous batch role assignment operation.
 *
 * <p>Tracks the progress and status of bulk role assignments where multiple roles
 * are assigned to multiple users. Enables async processing and status polling.</p>
 *
 * <p><b>Lifecycle:</b></p>
 * <ul>
 *   <li>PENDING → Job created, waiting for processing</li>
 *   <li>PROCESSING → Worker actively processing assignments</li>
 *   <li>COMPLETED → All assignments processed successfully</li>
 *   <li>COMPLETED_WITH_ERRORS → Some assignments failed</li>
 *   <li>FAILED → Entire batch failed</li>
 * </ul>
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "batch_jobs")
public class BatchJob {

    /**
     * Unique identifier for the batch job.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Tenant ID that owns this batch job (for multi-tenancy isolation).
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Type of batch operation (e.g., MULTI_ROLE_ASSIGNMENT).
     */
    @Column(name = "batch_type", nullable = false, length = 50)
    private String batchType;

    /**
     * Current status of the batch job.
     * Values: PENDING, PROCESSING, COMPLETED, COMPLETED_WITH_ERRORS, FAILED
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /**
     * Total number of role assignments to be created (roleIds.size() × userIds.size()).
     */
    @Column(name = "total_assignments", nullable = false)
    private Integer totalAssignments;

    /**
     * Number of assignments completed successfully.
     */
    @Column(name = "completed_count", nullable = false)
    private Integer completedCount = 0;

    /**
     * Number of assignments that failed.
     */
    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;

    /**
     * ID of the user who created this batch job.
     */
    @Column(name = "created_by_id", nullable = false)
    private UUID createdById;

    /**
     * Name of the user who created this batch job.
     */
    @Column(name = "created_by_name")
    private String createdByName;

    /**
     * Timestamp when the batch job was created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Timestamp when processing started.
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * Timestamp when processing completed.
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * JSON metadata containing request details:
     * - roleIds: List of role UUIDs
     * - userIds: List of user UUIDs
     * - justification: Reason for assignment
     * - validityDays: Validity period (optional)
     * - assignmentType: MANUAL or DYNAMIC
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Error message if batch failed completely.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Constructors
    public BatchJob() {}

    /**
     * Constructor for creating a new batch job.
     *
     * @param tenantId tenant identifier
     * @param batchType type of batch operation
     * @param status initial status (typically PENDING)
     * @param totalAssignments total number of assignments
     * @param createdById ID of the creator
     * @param createdByName name of the creator
     * @param metadata JSON metadata with request details
     */
    public BatchJob(String tenantId, String batchType, String status,
                    Integer totalAssignments, UUID createdById, String createdByName,
                    Map<String, Object> metadata) {
        this.tenantId = tenantId;
        this.batchType = batchType;
        this.status = status;
        this.totalAssignments = totalAssignments;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.metadata = metadata;
        this.createdAt = Instant.now();
        this.completedCount = 0;
        this.failedCount = 0;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getBatchType() {
        return batchType;
    }

    public void setBatchType(String batchType) {
        this.batchType = batchType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalAssignments() {
        return totalAssignments;
    }

    public void setTotalAssignments(Integer totalAssignments) {
        this.totalAssignments = totalAssignments;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public void setCreatedById(UUID createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}