package com.aidentx.iam.access_roles_management.event;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;

import java.util.UUID;

/**
 * Event published after an access-role assignment rule is created or updated.
 *
 * <p>The event is consumed after the surrounding transaction commits so rule
 * evaluation starts only after the persisted rule state is durable.</p>
 */
public class AccessRoleAssignmentRuleEvaluationEvent {

    private final String tenantId;
    private final UUID ruleId;
    private final AccessRoleAssignmentExecution.TriggerType triggerType;
    private final String triggeredBy;
    private final String scheduleType;

    public AccessRoleAssignmentRuleEvaluationEvent(String tenantId,
                                                   UUID ruleId,
                                                   AccessRoleAssignmentExecution.TriggerType triggerType,
                                                   String triggeredBy,
                                                   String scheduleType) {
        this.tenantId = tenantId;
        this.ruleId = ruleId;
        this.triggerType = triggerType;
        this.triggeredBy = triggeredBy;
        this.scheduleType = scheduleType;
    }

    public String getTenantId() { return tenantId; }
    public UUID getRuleId() { return ruleId; }
    public AccessRoleAssignmentExecution.TriggerType getTriggerType() { return triggerType; }
    public String getTriggeredBy() { return triggeredBy; }
    public String getScheduleType() { return scheduleType; }
}

