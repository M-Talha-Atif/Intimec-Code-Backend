package com.aidentx.iam.entitlement_management.event;

import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionHistory;

import java.util.UUID;

/**
 * Event published after a rule is created or updated.
 * Triggers asynchronous evaluation after the originating transaction commits.
 * Always published regardless of schedule type.
 */
public class RuleEvaluationEvent {

    private final String tenantId;
    private final UUID ruleId;
    private final RuleExecutionHistory.TriggerType triggerType;
    private final String triggeredBy;
    private final String scheduleType;

    public RuleEvaluationEvent(String tenantId, UUID ruleId,
                               RuleExecutionHistory.TriggerType triggerType,
                               String triggeredBy, String scheduleType) {
        this.tenantId = tenantId;
        this.ruleId = ruleId;
        this.triggerType = triggerType;
        this.triggeredBy = triggeredBy;
        this.scheduleType = scheduleType;
    }

    public String getTenantId() { return tenantId; }
    public UUID getRuleId() { return ruleId; }
    public RuleExecutionHistory.TriggerType getTriggerType() { return triggerType; }
    public String getTriggeredBy() { return triggeredBy; }
    public String getScheduleType() { return scheduleType; }
}