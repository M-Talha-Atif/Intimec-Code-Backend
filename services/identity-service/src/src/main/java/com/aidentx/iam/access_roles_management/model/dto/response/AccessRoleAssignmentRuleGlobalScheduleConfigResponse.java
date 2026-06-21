package com.aidentx.iam.access_roles_management.model.dto.response;

/**
 * Response DTO for reading the tenant-level access-role assignment-rule global schedule config.
 */
public class AccessRoleAssignmentRuleGlobalScheduleConfigResponse {

    private String scheduleType;
    private String cronExpression;
    private String scheduleTime;
    private String scheduleDate;
    private String scheduleTimezone;
    private Long updatedAt;

    public AccessRoleAssignmentRuleGlobalScheduleConfigResponse() {
    }

    public AccessRoleAssignmentRuleGlobalScheduleConfigResponse(String scheduleType,
                                                                String cronExpression,
                                                                String scheduleTime,
                                                                String scheduleDate,
                                                                String scheduleTimezone,
                                                                Long updatedAt) {
        this.scheduleType = scheduleType;
        this.cronExpression = cronExpression;
        this.scheduleTime = scheduleTime;
        this.scheduleDate = scheduleDate;
        this.scheduleTimezone = scheduleTimezone;
        this.updatedAt = updatedAt;
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

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}

