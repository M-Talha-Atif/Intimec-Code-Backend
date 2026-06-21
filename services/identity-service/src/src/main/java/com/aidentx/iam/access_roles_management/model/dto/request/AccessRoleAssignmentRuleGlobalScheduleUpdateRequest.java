package com.aidentx.iam.access_roles_management.model.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating the schedule configuration of all access-role
 * assignment rules for a tenant in one operation.
 */
public class AccessRoleAssignmentRuleGlobalScheduleUpdateRequest {

    /**
     * Schedule type to apply to all tenant rules.
     * Supported values are IMMEDIATE, CRON, and MANUAL.
     */
    @NotBlank
    private String scheduleType;

    /** Cron expression to apply when switching to CRON. */
    private String cronExpression;

    /** Time in HH:mm format to apply when switching to CRON. */
    private String scheduleTime;

    /** Date in yyyy-MM-dd format to apply when switching to CRON. */
    private String scheduleDate;

    /** IANA timezone to apply when switching to CRON. */
    private String scheduleTimezone;

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
}

