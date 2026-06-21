package com.aidentx.iam.entitlement_management.scheduler;

import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;
import com.aidentx.iam.entitlement_management.model.dto.RuleEvaluationResult;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignmentRule;
import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionHistory;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRuleRepository;
import com.aidentx.iam.entitlement_management.service.NextExecutionCalculator;
import com.aidentx.iam.entitlement_management.service.RuleEvaluationEngine;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Fixed-interval poller that checks for CRON rules due for evaluation.
 *
 * <p>Runs every 60 seconds. On each tick:
 * <ol>
 *   <li>Discovers all tenants that have enabled CRON rules</li>
 *   <li>For each tenant, queries rules where {@code next_execution_at <= NOW()}</li>
 *   <li>The query uses {@code FOR UPDATE SKIP LOCKED} for distributed safety —
 *       if multiple app instances are running, each instance picks up different rules</li>
 *   <li>Evaluates each due rule via {@link RuleEvaluationEngine}</li>
 *   <li>After evaluation, advances {@code next_execution_at} to the next scheduled time</li>
 * </ol>
 *
 * <p><b>Restart safety:</b> If the app was down when a rule was due,
 * its {@code next_execution_at} remains in the past. On startup,
 * the first poller tick picks it up immediately. No missed evaluations.
 *
 * <p><b>Multi-instance safety:</b> {@code FOR UPDATE SKIP LOCKED} ensures
 * only one instance evaluates each rule. Other instances skip locked rows.
 *
 * <p><b>One-time schedules:</b> Rules with a {@code scheduleDate} that has passed
 * will have {@code next_execution_at = null} after evaluation, and the poller
 * will never pick them up again.
 *
 * <p>This component only handles CRON rules. IMMEDIATE rules are handled
 * by the event listener on create/update.
 */
@Component
public class CronSchedulerPoller {

    private final EntitlementAssignmentRuleRepository ruleRepository;
    private final RuleEvaluationEngine evaluationEngine;
    private final NextExecutionCalculator executionCalculator;
    private final IUnifiedLogger logger;

    @Autowired
    public CronSchedulerPoller(EntitlementAssignmentRuleRepository ruleRepository,
                                RuleEvaluationEngine evaluationEngine,
                                NextExecutionCalculator executionCalculator,
                                UnifiedLoggerFactory loggerFactory) {
        this.ruleRepository = ruleRepository;
        this.evaluationEngine = evaluationEngine;
        this.executionCalculator = executionCalculator;
        this.logger = loggerFactory.getLogger(getClass(), "CronSchedulerPoller");
    }

    /**
     * Main poller method. Runs every 60 seconds.
     *
     * <p>The {@code fixedRate} ensures consistent 60-second intervals
     * regardless of how long the previous tick took. If evaluation takes
     * longer than 60 seconds, the next tick starts immediately after.
     *
     * <p>Each tenant is processed independently. A failure in one tenant
     * does not affect others.
     */
    @Scheduled(fixedRate = 60_000, initialDelay = 30_000)
    public void pollDueRules() {
        try {
            List<String> tenantIds = ruleRepository.findTenantsWithCronRules();

            if (tenantIds.isEmpty()) {
                return; // No tenants with CRON rules, nothing to do
            }

            for (String tenantId : tenantIds) {
                processTenant(tenantId);
            }

        } catch (Exception e) {
            logger.error()
                    .message("Cron poller tick failed")
                    .detail(RuleEvaluationConstants.DETAIL_ERROR, e.getMessage())
                    .log();
        }
    }

    /**
     * Processes all due CRON rules for a single tenant.
     *
     * <p>The query uses {@code FOR UPDATE SKIP LOCKED}, which means:
     * <ul>
     *   <li>Rows returned are locked for this transaction</li>
     *   <li>Other instances running the same query will skip these rows</li>
     *   <li>After this method commits, the rows are unlocked</li>
     * </ul>
     *
     * <p>Each rule is evaluated independently. If one rule fails,
     * the error is logged and the next rule is processed.
     * The failed rule's {@code next_execution_at} is still advanced
     * to prevent it from being retried every 60 seconds forever.
     *
     * @param tenantId the tenant identifier
     */
    @Transactional
    public void processTenant(String tenantId) {
        long now = Instant.now().toEpochMilli();

        List<EntitlementAssignmentRule> dueRules = ruleRepository.findDueCronRules(tenantId, now);

        if (dueRules.isEmpty()) {
            return;
        }

        logger.info()
                .message("Cron poller found due rules")
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .detail(RuleEvaluationConstants.DETAIL_RULE_COUNT, String.valueOf(dueRules.size()))
                .log();

        for (EntitlementAssignmentRule rule : dueRules) {
            evaluateAndAdvance(tenantId, rule);
        }
    }

    /**
     * Evaluates a single due rule and advances its {@code next_execution_at}.
     *
     * <p>Flow:
     * <ol>
     *   <li>Evaluate the rule via the engine (with CRON trigger type)</li>
     *   <li>Calculate the next execution time</li>
     *   <li>Update the rule's {@code next_execution_at}</li>
     *   <li>Save the rule</li>
     * </ol>
     *
     * <p>If evaluation fails, the next execution is still advanced.
     * This prevents a broken rule from being retried every 60 seconds.
     * The failure is recorded in execution history by the engine.
     *
     * <p>If the next execution calculates to null (one-time schedule that's done),
     * the rule will never be picked up by the poller again.
     *
     * @param tenantId the tenant identifier
     * @param rule     the rule to evaluate
     */
    private void evaluateAndAdvance(String tenantId, EntitlementAssignmentRule rule) {
        try {
            logger.info()
                    .message("Cron evaluating rule")
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                    .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            // Evaluate the rule
            RuleEvaluationResult result = evaluationEngine.evaluateRuleById(
                    tenantId,
                    rule.getId(),
                    RuleExecutionHistory.TriggerType.CRON,
                    RuleEvaluationConstants.SYSTEM_TRIGGER
            );

            logger.info()
                    .message("Cron evaluation complete")
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                    .detail(RuleEvaluationConstants.DETAIL_MATCHED_COUNT,
                            String.valueOf(result.getIdentitiesMatched()))
                    .detail(RuleEvaluationConstants.DETAIL_CREATED_COUNT,
                            String.valueOf(result.getAssignmentsCreated()))
                    .detail(RuleEvaluationConstants.DETAIL_REVOKED_COUNT,
                            String.valueOf(result.getAssignmentsRevoked()))
                    .log();

        } catch (Exception e) {
            logger.error()
                    .message("Cron evaluation failed for rule")
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                    .detail(RuleEvaluationConstants.DETAIL_ERROR, e.getMessage())
                    .log();
        } finally {
            // ALWAYS advance next_execution_at, even on failure
            advanceNextExecution(rule);
        }
    }

    /**
     * Advances the rule's {@code next_execution_at} to the next scheduled time.
     *
     * <p>Uses {@link NextExecutionCalculator} to compute the next time.
     * If the calculator returns null (one-time schedule done, or invalid config),
     * the rule's {@code next_execution_at} is set to null and it will
     * never be picked up by the poller again.
     *
     * @param rule the rule to advance
     */
    private void advanceNextExecution(EntitlementAssignmentRule rule) {
        try {
            Long nextExecution = executionCalculator.calculate(rule);
            rule.setNextExecutionAt(nextExecution);
            rule.setLastEvaluatedAt(Instant.now().toEpochMilli());
            ruleRepository.save(rule);

            logger.debug()
                    .message("Advanced next_execution_at")
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail("nextExecutionAt", nextExecution != null
                            ? Instant.ofEpochMilli(nextExecution).toString() : "null (done)")
                    .log();

        } catch (Exception e) {
            logger.error()
                    .message("Failed to advance next_execution_at")
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_ERROR, e.getMessage())
                    .log();
        }
    }
}