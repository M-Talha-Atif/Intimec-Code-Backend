package com.aidentx.applicationpermissions.roles_management.model.entity;

import com.aidentx.applicationpermissions.roles_management.enums.ScheduleType;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Entity representing a Dynamic Rule that controls automated role assignment behavior.
 * 
 * <p>Dynamic Rules provide automated role assignment logic based on conditions (ruleConditionScript)
 * and can include notification and approval workflows. These rules can be activated or deactivated
 * as needed and provide flexible, scalable role management for millions of users.</p>
 * 
 * <p><strong>Schedule Types:</strong></p>
 * <ul>
 *   <li><strong>IMMEDIATE:</strong> Continuous reconciliation every minute - roles are assigned/removed
 *       in near real-time as user attributes change or new users are added</li>
 *   <li><strong>DAILY/WEEKLY/MONTHLY:</strong> Scheduled reconciliation at specific times - roles are
 *       evaluated and assigned/removed only during the scheduled execution</li>
 * </ul>
 * 
 * <p><strong>Example use cases:</strong></p>
 * <ul>
 *   <li>Automatically assign "Manager" role when department equals "Management" (IMMEDIATE)</li>
 *   <li>Assign "Contractor" role when employment type is "Contract" (IMMEDIATE)</li>
 *   <li>Review and assign compliance roles monthly based on training completion (MONTHLY)</li>
 *   <li>Send notifications when sensitive roles are assigned</li>
 *   <li>Require approval workflow for high-privilege role assignments</li>
 * </ul>
 * 
 * <p><strong>Reconciliation Behavior:</strong></p>
 * <p>When a reconciliation runs, the system:</p>
 * <ol>
 *   <li>Fetches all users in the tenant</li>
 *   <li>Evaluates the ruleConditionScript for each user</li>
 *   <li>Assigns the role to users who match the conditions</li>
 *   <li>Removes the role from users who no longer match</li>
 *   <li>Logs audit events ONLY when actual changes occur (assignments/removals)</li>
 * </ol>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "dynamic_rule", uniqueConstraints = {
    @UniqueConstraint(name = "uk_dynamic_rule_role_id", columnNames = {"role_id"})
})
public class DynamicRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "role_id", nullable = false)
    private UUID roleId;
    
    @Column(name = "rule_is_active", nullable = false)
    private Boolean ruleIsActive = true;
    
    @Column(name = "rule_condition_script", nullable = true, columnDefinition = "TEXT")
    private String ruleConditionScript;
    
    @Column(name = "send_notification_on_assignment", nullable = false)
    private Boolean sendNotificationOnAssignment = false;
    
    @Column(name = "require_approval_before_assignment", nullable = false)
    private Boolean requireApprovalBeforeAssignment = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType = ScheduleType.IMMEDIATE;
    
    @Column(name = "time", nullable = true)
    private LocalTime time;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;
    
    @Column(name = "created_by_id", nullable = false, updatable = false)
    private UUID createdById;
    
    @Column(name = "created_by_name", nullable = false, updatable = false)
    private String createdByName;
    
    @Column(name = "updated_by_id", nullable = false)
    private UUID updatedById;
    
    @Column(name = "updated_by_name", nullable = false)
    private String updatedByName;
    
    @PrePersist
    protected void onCreate() {
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    // Constructors
    public DynamicRule() {}

    public DynamicRule(UUID roleId, Boolean ruleIsActive, String ruleConditionScript, 
                      Boolean sendNotificationOnAssignment, Boolean requireApprovalBeforeAssignment,
                      ScheduleType scheduleType, LocalTime time) {
        this.roleId = roleId;
        this.ruleIsActive = ruleIsActive;
        this.ruleConditionScript = ruleConditionScript;
        this.sendNotificationOnAssignment = sendNotificationOnAssignment;
        this.requireApprovalBeforeAssignment = requireApprovalBeforeAssignment;
        this.scheduleType = scheduleType;
        this.time = time;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public Boolean getRuleIsActive() {
        return ruleIsActive;
    }

    public void setRuleIsActive(Boolean ruleIsActive) {
        this.ruleIsActive = ruleIsActive;
    }

    public String getRuleConditionScript() {
        return ruleConditionScript;
    }

    public void setRuleConditionScript(String ruleConditionScript) {
        this.ruleConditionScript = ruleConditionScript;
    }

    public Boolean getSendNotificationOnAssignment() {
        return sendNotificationOnAssignment;
    }

    public void setSendNotificationOnAssignment(Boolean sendNotificationOnAssignment) {
        this.sendNotificationOnAssignment = sendNotificationOnAssignment;
    }

    public Boolean getRequireApprovalBeforeAssignment() {
        return requireApprovalBeforeAssignment;
    }

    public void setRequireApprovalBeforeAssignment(Boolean requireApprovalBeforeAssignment) {
        this.requireApprovalBeforeAssignment = requireApprovalBeforeAssignment;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
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

    public UUID getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(UUID updatedById) {
        this.updatedById = updatedById;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    @Override
    public String toString() {
        return "DynamicRule{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", ruleIsActive=" + ruleIsActive +
                ", ruleConditionScript='" + ruleConditionScript + '\'' +
                ", sendNotificationOnAssignment=" + sendNotificationOnAssignment +
                ", requireApprovalBeforeAssignment=" + requireApprovalBeforeAssignment +
                ", scheduleType=" + scheduleType +
                ", time=" + time +
                '}';
    }
}