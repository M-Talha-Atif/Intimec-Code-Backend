package com.aidentx.iam.access_roles_management.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * Stores the tenant-level global schedule configuration for access-role assignment rules.
 *
 * <p>One row exists per tenant. When the global schedule is updated, all existing
 * access-role assignment rules for that tenant are updated in bulk and newly created
 * rules inherit the persisted schedule when no explicit schedule is supplied.</p>
 */
@Entity
@Table(name = "access_role_assignment_rule_global_schedule_config",
        uniqueConstraints = @UniqueConstraint(columnNames = "tenant_id"))
public class AccessRoleAssignmentRuleGlobalScheduleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private String tenantId;

    @Column(name = "schedule_type", nullable = false)
    private String scheduleType;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "schedule_time")
    private String scheduleTime;

    @Column(name = "schedule_date")
    private String scheduleDate;

    @Column(name = "schedule_timezone")
    private String scheduleTimezone;

    @Column(name = "updated_at")
    private Long updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getScheduleTime() { return scheduleTime; }
    public void setScheduleTime(String scheduleTime) { this.scheduleTime = scheduleTime; }

    public String getScheduleDate() { return scheduleDate; }
    public void setScheduleDate(String scheduleDate) { this.scheduleDate = scheduleDate; }

    public String getScheduleTimezone() { return scheduleTimezone; }
    public void setScheduleTimezone(String scheduleTimezone) { this.scheduleTimezone = scheduleTimezone; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}

