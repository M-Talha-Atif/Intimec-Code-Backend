package com.aidentx.iam.access_roles_management.event;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link AccessRoleAssignmentRuleEvaluationEvent} and executes the
 * referenced rule after the originating create/update transaction commits.
 */
@Component
public class AccessRoleAssignmentRuleEvaluationEventListener {

    private final AccessRoleAssignmentRuleEvaluationRunner evaluationRunner;
    private final IUnifiedLogger logger;

    public AccessRoleAssignmentRuleEvaluationEventListener(AccessRoleAssignmentRuleEvaluationRunner evaluationRunner,
                                                           UnifiedLoggerFactory loggerFactory) {
        this.evaluationRunner = evaluationRunner;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAssignmentRuleConstants.LOG_CATEGORY);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRuleEvaluation(AccessRoleAssignmentRuleEvaluationEvent event) {
        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_RULE_EVALUATION_TRIGGERED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, event.getTenantId())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, event.getRuleId().toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE, event.getScheduleType())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGER_TYPE, event.getTriggerType().name())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, event.getTriggeredBy())
                .log();

        try {
            evaluationRunner.run(event);
        } catch (Exception ex) {
            logger.warn()
                    .message(AccessRoleAssignmentRuleConstants.LOG_RULE_EVALUATION_FAILED)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, event.getTenantId())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, event.getRuleId().toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE, event.getScheduleType())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGER_TYPE, event.getTriggerType().name())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_ERROR, ex.getMessage())
                    .log();
        }
    }
}

