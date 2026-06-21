package com.aidentx.iam.entitlement_management.model.dto;

/**
 * Response DTO for the global schedule update operation.
 */
public class GlobalScheduleUpdateResponse {

    private String scheduleType;
    private String cronExpression;
    private String scheduleTime;
    private String scheduleDate;
    private String scheduleTimezone;
    private int rulesUpdated;
    /**
     * The next scheduled execution time across all updated rules.
     * Helpful for the admin to verify the global schedule took effect.
     */
    private String nextExecutionPreview;


    public GlobalScheduleUpdateResponse() {
    }

    public GlobalScheduleUpdateResponse(String scheduleType, String cronExpression,
                                        String scheduleTime, String scheduleDate,
                                        String scheduleTimezone, int rulesUpdated) {
        this.scheduleType = scheduleType;
        this.cronExpression = cronExpression;
        this.scheduleTime = scheduleTime;
        this.scheduleDate = scheduleDate;
        this.scheduleTimezone = scheduleTimezone;
        this.rulesUpdated = rulesUpdated;
    }

    // --- Getters and Setters ---

    public String getNextExecutionPreview() {
        return nextExecutionPreview;
    }

    public void setNextExecutionPreview(String nextExecutionPreview) {
        this.nextExecutionPreview = nextExecutionPreview;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(String scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public String getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(String scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public String getScheduleTimezone() {
        return scheduleTimezone;
    }

    public void setScheduleTimezone(String scheduleTimezone) {
        this.scheduleTimezone = scheduleTimezone;
    }

    public int getRulesUpdated() {
        return rulesUpdated;
    }

    public void setRulesUpdated(int rulesUpdated) {
        this.rulesUpdated = rulesUpdated;
    }
}