package com.aidentx.applicationpermissions.roles_management.model.entity;

import com.aidentx.applicationpermissions.roles_management.enums.ScheduleType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing the execution history of dynamic rules.
 * 
 * <p>This entity tracks when dynamic rules were executed to prevent
 * duplicate executions within the same schedule period (daily, weekly, monthly).
 * It ensures that scheduled rules are executed only once per their defined schedule.</p>
 * 
 * <p>Key functions:</p>
 * <ul>
 *   <li>Prevents duplicate executions within schedule periods</li>
 *   <li>Tracks execution success/failure for monitoring</li>
 *   <li>Provides audit trail for compliance</li>
 *   <li>Enables performance analytics and optimization</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "dynamic_rule_execution_history", indexes = {
    @Index(name = "idx_rule_execution_time", columnList = "rule_id, execution_date"),
    @Index(name = "idx_execution_date", columnList = "execution_date"),
    @Index(name = "idx_rule_schedule", columnList = "rule_id, schedule_type")
})
public class DynamicRuleExecutionHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;
    
    @Column(name = "role_id", nullable = false)
    private UUID roleId;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;
    
    @Column(name = "execution_date", nullable = false)
    private LocalDateTime executionDate;
    
    @Column(name = "scheduled_time", nullable = true)
    private String scheduledTime; // e.g., "14:30"
    
    @Column(name = "execution_duration_ms", nullable = false)
    private Long executionDurationMs;
    
    @Column(name = "total_users_evaluated", nullable = false)
    private Integer totalUsersEvaluated;
    
    @Column(name = "users_matching_condition", nullable = false)
    private Integer usersMatchingCondition;
    
    @Column(name = "successful_assignments", nullable = false)
    private Integer successfulAssignments;
    
    @Column(name = "failed_assignments", nullable = false)
    private Integer failedAssignments;
    
    @Column(name = "skipped_assignments", nullable = false)
    private Integer skippedAssignments;
    
    @Column(name = "execution_status", nullable = false)
    private String executionStatus; // SUCCESS, FAILED, PARTIAL
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public DynamicRuleExecutionHistory() {
        this.createdAt = LocalDateTime.now();
    }

    public DynamicRuleExecutionHistory(UUID ruleId, UUID roleId, String tenantId, ScheduleType scheduleType,
                                     String scheduledTime, Long executionDurationMs, Integer totalUsersEvaluated,
                                     Integer usersMatchingCondition, Integer successfulAssignments,
                                     Integer failedAssignments, Integer skippedAssignments,
                                     String executionStatus, String errorMessage) {
        this.ruleId = ruleId;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.scheduleType = scheduleType;
        this.executionDate = LocalDateTime.now();
        this.scheduledTime = scheduledTime;
        this.executionDurationMs = executionDurationMs;
        this.totalUsersEvaluated = totalUsersEvaluated;
        this.usersMatchingCondition = usersMatchingCondition;
        this.successfulAssignments = successfulAssignments;
        this.failedAssignments = failedAssignments;
        this.skippedAssignments = skippedAssignments;
        this.executionStatus = executionStatus;
        this.errorMessage = errorMessage;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public void setRuleId(UUID ruleId) {
        this.ruleId = ruleId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDateTime executionDate) {
        this.executionDate = executionDate;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(Long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }

    public Integer getTotalUsersEvaluated() {
        return totalUsersEvaluated;
    }

    public void setTotalUsersEvaluated(Integer totalUsersEvaluated) {
        this.totalUsersEvaluated = totalUsersEvaluated;
    }

    public Integer getUsersMatchingCondition() {
        return usersMatchingCondition;
    }

    public void setUsersMatchingCondition(Integer usersMatchingCondition) {
        this.usersMatchingCondition = usersMatchingCondition;
    }

    public Integer getSuccessfulAssignments() {
        return successfulAssignments;
    }

    public void setSuccessfulAssignments(Integer successfulAssignments) {
        this.successfulAssignments = successfulAssignments;
    }

    public Integer getFailedAssignments() {
        return failedAssignments;
    }

    public void setFailedAssignments(Integer failedAssignments) {
        this.failedAssignments = failedAssignments;
    }

    public Integer getSkippedAssignments() {
        return skippedAssignments;
    }

    public void setSkippedAssignments(Integer skippedAssignments) {
        this.skippedAssignments = skippedAssignments;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "DynamicRuleExecutionHistory{" +
                "id=" + id +
                ", ruleId=" + ruleId +
                ", roleId=" + roleId +
                ", tenantId='" + tenantId + '\'' +
                ", scheduleType=" + scheduleType +
                ", executionDate=" + executionDate +
                ", scheduledTime='" + scheduledTime + '\'' +
                ", executionStatus='" + executionStatus + '\'' +
                ", successfulAssignments=" + successfulAssignments +
                '}';
    }
}