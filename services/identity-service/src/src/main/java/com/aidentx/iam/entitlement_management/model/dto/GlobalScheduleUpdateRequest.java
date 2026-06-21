package com.aidentx.iam.entitlement_management.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating the schedule configuration of all rules at once.
 *
 * <p>Used by the global trigger endpoint to switch all rules
 * between IMMEDIATE and CRON in a single operation, with optional
 * cron expression, time, date, and timezone configuration.
 */
public class GlobalScheduleUpdateRequest {

    /**
     * The schedule type to apply to all rules. Must be IMMEDIATE or CRON.
     */
    @NotBlank
    private String scheduleType;

    /**
     * Cron expression to apply to all rules when switching to CRON.
     * Example: "0 0 2 * * ?"
     */
    private String cronExpression;

    /**
     * Time in HH:mm format to apply to all rules.
     * Example: "02:00"
     */
    private String scheduleTime;

    /**
     * Date in yyyy-MM-dd format to apply to all rules.
     * Example: "2026-05-01"
     */
    private String scheduleDate;

    /**
     * IANA timezone to apply to all rules.
     * Example: "America/New_York", "UTC"
     */
    private String scheduleTimezone;

    // --- Getters and Setters ---

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