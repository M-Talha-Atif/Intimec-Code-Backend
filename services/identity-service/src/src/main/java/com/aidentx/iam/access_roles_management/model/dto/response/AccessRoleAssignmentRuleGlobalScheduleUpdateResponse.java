package com.aidentx.iam.access_roles_management.model.dto.response;

/**
 * Response DTO for bulk-updating the tenant-level schedule of access-role assignment rules.
 */
public class AccessRoleAssignmentRuleGlobalScheduleUpdateResponse {

    private String scheduleType;
    private String cronExpression;
    private String scheduleTime;
    private String scheduleDate;
    private String scheduleTimezone;
    private int rulesUpdated;
    private String nextExecutionPreview;

    public AccessRoleAssignmentRuleGlobalScheduleUpdateResponse() {
    }

    public AccessRoleAssignmentRuleGlobalScheduleUpdateResponse(String scheduleType,
                                                                String cronExpression,
                                                                String scheduleTime,
                                                                String scheduleDate,
                                                                String scheduleTimezone,
                                                                int rulesUpdated) {
        this.scheduleType = scheduleType;
        this.cronExpression = cronExpression;
        this.scheduleTime = scheduleTime;
        this.scheduleDate = scheduleDate;
        this.scheduleTimezone = scheduleTimezone;
        this.rulesUpdated = rulesUpdated;
    }

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

    public int getRulesUpdated() { return rulesUpdated; }
    public void setRulesUpdated(int rulesUpdated) { this.rulesUpdated = rulesUpdated; }

    public String getNextExecutionPreview() { return nextExecutionPreview; }
    public void setNextExecutionPreview(String nextExecutionPreview) { this.nextExecutionPreview = nextExecutionPreview; }
}

