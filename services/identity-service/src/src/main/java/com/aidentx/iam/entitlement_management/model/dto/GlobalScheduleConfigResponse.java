package com.aidentx.iam.entitlement_management.model.dto;

/**
 * Response DTO for the global schedule config.
 * Returned on GET and after PUT updates.
 */
public class GlobalScheduleConfigResponse {

    private String scheduleType;
    private String cronExpression;
    private String scheduleTimezone;
    private Long updatedAt;

    public GlobalScheduleConfigResponse() {}

    public GlobalScheduleConfigResponse(String scheduleType, String cronExpression,
                                         String scheduleTimezone, Long updatedAt) {
        this.scheduleType = scheduleType;
        this.cronExpression = cronExpression;
        this.scheduleTimezone = scheduleTimezone;
        this.updatedAt = updatedAt;
    }

    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getScheduleTimezone() { return scheduleTimezone; }
    public void setScheduleTimezone(String scheduleTimezone) { this.scheduleTimezone = scheduleTimezone; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}