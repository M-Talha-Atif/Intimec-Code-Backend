package com.aidentx.iam.entitlement_management.event;

import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;
import com.aidentx.iam.entitlement_management.service.RuleEvaluationEngine;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link RuleEvaluationEvent} and triggers rule evaluation
 * AFTER the originating transaction has committed.
 *
 * <p>Always evaluates regardless of schedule type. The AOP audit aspect
 * on evaluateRuleById() automatically publishes the RULE_EVALUATED audit
 * event since RULE_CREATED and RULE_UPDATED are in ASPECT_AUDITED_TRIGGERS.
 */
@Component
public class RuleEvaluationEventListener {

    private final RuleEvaluationEngine evaluationEngine;
    private final IUnifiedLogger logger;

    @Autowired
    public RuleEvaluationEventListener(RuleEvaluationEngine evaluationEngine,
                                        UnifiedLoggerFactory loggerFactory) {
        this.evaluationEngine = evaluationEngine;
        this.logger = loggerFactory.getLogger(getClass(), RuleEvaluationConstants.LOGGER_MODULE_SERVICE);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRuleEvaluation(RuleEvaluationEvent event) {
        logger.info()
                .message("Post-commit rule evaluation triggered on " + event.getTriggerType().name())
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, event.getRuleId().toString())
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, event.getTenantId())
                .detail(RuleEvaluationConstants.DETAIL_SCHEDULE_TYPE, event.getScheduleType())
                .detail(RuleEvaluationConstants.DETAIL_TRIGGER_TYPE, event.getTriggerType().name())
                .log();

        try {
            evaluationEngine.evaluateRuleById(
                    event.getTenantId(),
                    event.getRuleId(),
                    event.getTriggerType(),
                    event.getTriggeredBy()
            );
        } catch (Exception e) {
            logger.warn()
                    .message("Post-commit evaluation failed on " + event.getTriggerType().name() + ", cron will retry")
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, event.getRuleId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_SCHEDULE_TYPE, event.getScheduleType())
                    .detail(RuleEvaluationConstants.DETAIL_ERROR, e.getMessage())
                    .log();
        }
    }
}