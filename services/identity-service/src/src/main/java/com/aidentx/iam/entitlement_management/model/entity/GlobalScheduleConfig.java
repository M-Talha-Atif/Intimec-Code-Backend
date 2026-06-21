package com.aidentx.iam.entitlement_management.model.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Stores the tenant-level global schedule configuration for assignment rules.
 *
 * <p>One row per tenant. When a new assignment rule is created, it inherits
 * the schedule type, cron expression, and timezone from this config instead
 * of defaulting to IMMEDIATE.
 *
 * <p>Updated via the global schedule endpoint. All existing rules are also
 * bulk-updated when this config changes.
 */
@Entity
@Table(name = "global_schedule_config",
        uniqueConstraints = @UniqueConstraint(columnNames = "tenant_id"))
public class GlobalScheduleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private String tenantId;

    /**
     * The default schedule type for new rules: IMMEDIATE, CRON, or MANUAL.
     */
    @Column(name = "schedule_type", nullable = false)
    private String scheduleType;

    /**
     * The default cron expression (6-field Spring format).
     * Null when scheduleType is IMMEDIATE or MANUAL.
     */
    @Column(name = "cron_expression")
    private String cronExpression;

    /**
     * The default IANA timezone for cron evaluation.
     * Null when scheduleType is IMMEDIATE or MANUAL.
     */
    @Column(name = "schedule_timezone")
    private String scheduleTimezone;

    @Column(name = "updated_at")
    private Long updatedAt;

    // --- Constructors ---

    public GlobalScheduleConfig() {}

    public GlobalScheduleConfig(String tenantId, String scheduleType,
                                 String cronExpression, String scheduleTimezone) {
        this.tenantId = tenantId;
        this.scheduleType = scheduleType;
        this.cronExpression = cronExpression;
        this.scheduleTimezone = scheduleTimezone;
        this.updatedAt = System.currentTimeMillis();
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getScheduleTimezone() { return scheduleTimezone; }
    public void setScheduleTimezone(String scheduleTimezone) { this.scheduleTimezone = scheduleTimezone; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}