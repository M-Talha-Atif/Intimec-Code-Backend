package com.aidentx.iam.entitlement_management.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.aidentx.iam.config.AppConstants;
import com.aidentx.iam.entitlement_management.audit.EntitlementRuleAuditEvent;
import com.aidentx.iam.entitlement_management.audit.EntitlementRuleAuditPublisher;
import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;
import com.aidentx.iam.entitlement_management.model.dto.RuleEvaluationResult;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignmentRule;
import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionHistory;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRuleRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Core engine responsible for evaluating entitlement assignment rules against
 * identity populations and producing assignment/revocation results.
 *
 * <p>This engine is the single entry point for all evaluation paths:
 * <ul>
 *   <li><b>API manual</b>  via {@link #evaluateRuleById(String, UUID, String)}</li>
 *   <li><b>Post-commit (create/update)</b>  via {@link #evaluateRuleById(String, UUID, RuleExecutionHistory.TriggerType, String)}</li>
 *   <li><b>Evaluate all</b>  via {@link #evaluateAllRules(String, String)}</li>
 *   <li><b>CRON scheduler</b>  via {@link #evaluateCronRules(String)}</li>
 * </ul>
 *
 * <h3>Audit responsibility split</h3>
 * <p>The engine publishes {@code RULE_EVALUATED} Kafka audit events <b>only</b>
 * for trigger types that are not covered by the AOP audit aspect
 * ({@link com.aidentx.iam.entitlement_management.audit.EntitlementRuleAuditAspect}).
 * The set of aspect-audited triggers is defined in {@link #ASPECT_AUDITED_TRIGGERS}.
 *
 * <p>However, the engine persists <b>all</b> evaluation results to the
 * entitlement audit log table via {@link EntitlementAuditLogService},
 * regardless of trigger type. This ensures MANUAL, CRON, EVALUATE_ALL,
 * and post-commit evaluations all appear in the entitlement audit history.
 *
 * <table border="1">
 *   <tr><th>Trigger Type</th><th>Kafka Audit Published By</th><th>DB Audit Persisted By</th></tr>
 *   <tr><td>{@code ENTITLEMENT_RULE_CREATED}</td><td>AOP aspect</td><td>Engine</td></tr>
 *   <tr><td>{@code ENTITLEMENT_RULE_UPDATED}</td><td>AOP aspect</td><td>Engine</td></tr>
 *   <tr><td>{@code EVALUATE_ALL}</td><td>AOP aspect (evaluateAllRules)</td><td>Engine</td></tr>
 *   <tr><td>{@code CRON}</td><td>Engine (this class)</td><td>Engine</td></tr>
 * </table>
 *
 * <h3>History persistence</h3>
 * <p>Every evaluation  regardless of trigger type  persists an
 * {@link RuleExecutionHistory} record via the {@link ExecutionHistoryWriter}.
 * The history collector captures per-identity detail records, and the writer
 * flushes them in a single batch after evaluation completes (success or failure).
 *
 * @see com.aidentx.iam.entitlement_management.audit.EntitlementRuleAuditAspect
 * @see ExecutionHistoryWriter
 * @see RuleEvaluationTransactionHelper
 */
@Service
public class RuleEvaluationEngine {

    private final EntitlementAssignmentRuleRepository ruleRepository;
    private final EntitlementRepository entitlementRepository;
    private final RuleEvaluationTransactionHelper transactionHelper;
    private final ExecutionHistoryWriter historyWriter;
    private final EntitlementRuleAuditPublisher auditPublisher;
    private final EntitlementAuditLogService auditLogService;
    private final IUnifiedLogger logger;

    /**
     * Trigger types where the AOP aspect handles Kafka audit publishing.
     *
     * <p>For these triggers, the engine skips its internal
     * {@link #publishEvaluationAuditEvent} call to prevent duplicate
     * Kafka audit events. The aspect has access to the real user (via security
     * context or {@code triggeredBy} fallback) and produces richer events.
     *
     * <p>Note: DB audit persistence is <b>always</b> handled by the engine
     * via {@link #persistEvaluationAuditLog}, regardless of this set.
     */
    private static final Set<RuleExecutionHistory.TriggerType> ASPECT_AUDITED_TRIGGERS = Set.of(
            RuleExecutionHistory.TriggerType.ENTITLEMENT_RULE_CREATED,
            RuleExecutionHistory.TriggerType.ENTITLEMENT_RULE_UPDATED,
            RuleExecutionHistory.TriggerType.EVALUATE_ALL
    );

    /**
     * Constructs the evaluation engine with all required dependencies.
     *
     * @param ruleRepository        reads assignment rules from the database
     * @param entitlementRepository reads entitlements for display name resolution
     * @param transactionHelper     executes rule evaluation within a managed transaction
     * @param historyWriter         persists execution history records after evaluation
     * @param auditPublisher        publishes audit events for non-aspect-audited triggers
     * @param auditLogService       persists evaluation results to the entitlement audit log table
     * @param loggerFactory         creates the unified logger for the evaluation module
     */
    @Autowired
    public RuleEvaluationEngine(EntitlementAssignmentRuleRepository ruleRepository,
                                EntitlementRepository entitlementRepository,
                                RuleEvaluationTransactionHelper transactionHelper,
                                ExecutionHistoryWriter historyWriter,
                                EntitlementRuleAuditPublisher auditPublisher,
                                EntitlementAuditLogService auditLogService,
                                UnifiedLoggerFactory loggerFactory) {
        this.ruleRepository = ruleRepository;
        this.entitlementRepository = entitlementRepository;
        this.transactionHelper = transactionHelper;
        this.historyWriter = historyWriter;
        this.auditPublisher = auditPublisher;
        this.auditLogService = auditLogService;
        this.logger = loggerFactory.getLogger(getClass(), RuleEvaluationConstants.LOGGER_MODULE_NAME);
    }

    // ===================================================================
    //  Evaluate All Rules
    // ===================================================================

    /**
     * Evaluates all enabled assignment rules for a tenant.
     *
     * <p>Each rule is evaluated independently via {@link #evaluateRuleSafely}.
     * A failure in one rule does not prevent evaluation of subsequent rules.
     * The trigger type is {@code EVALUATE_ALL}, and Kafka audit is handled by the
     * AOP aspect on the service's {@code evaluateAllRules()} method.
     *
     * @param tenantId    the tenant identifier
     * @param triggeredBy the username of the user who initiated the evaluation
     * @return a list of evaluation results, one per enabled rule
     */
    public List<RuleEvaluationResult> evaluateAllRules(String tenantId, String triggeredBy) {
        List<EntitlementAssignmentRule> rules =
                ruleRepository.findByTenantIdAndEnabledTrueOrderByPriorityAsc(tenantId);

        logger.info()
                .message(RuleEvaluationConstants.LOG_EVALUATE_ALL_START)
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .detail(RuleEvaluationConstants.DETAIL_RULE_COUNT, String.valueOf(rules.size()))
                .log();

        List<RuleEvaluationResult> results = new ArrayList<>();
        for (EntitlementAssignmentRule rule : rules) {
            results.add(evaluateRuleSafely(tenantId, rule,
                    RuleExecutionHistory.TriggerType.EVALUATE_ALL, triggeredBy));
        }

        logger.info()
                .message(RuleEvaluationConstants.LOG_EVALUATE_ALL_COMPLETE)
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .detail(RuleEvaluationConstants.DETAIL_RULE_COUNT, String.valueOf(results.size()))
                .log();

        return results;
    }

    // ===================================================================
    //  Evaluate IMMEDIATE Rules
    // ===================================================================

    /**
     * Evaluates all enabled IMMEDIATE-schedule rules for a tenant.
     *
     * <p>Typically invoked by the system after identity sync events.
     * Uses {@code ENTITLEMENT_RULE_CREATED} as the trigger type and
     * {@code SYSTEM} as the actor.
     *
     * @param tenantId the tenant identifier
     * @return a list of evaluation results for all IMMEDIATE rules
     */
    public List<RuleEvaluationResult> evaluateImmediateRules(String tenantId) {
        List<EntitlementAssignmentRule> rules = ruleRepository.findImmediateRules(tenantId);

        logger.info()
                .message(RuleEvaluationConstants.LOG_EVALUATE_IMMEDIATE_START)
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .detail(RuleEvaluationConstants.DETAIL_RULE_COUNT, String.valueOf(rules.size()))
                .log();

        List<RuleEvaluationResult> results = new ArrayList<>();
        for (EntitlementAssignmentRule rule : rules) {
            results.add(evaluateRuleSafely(tenantId, rule,
                    RuleExecutionHistory.TriggerType.ENTITLEMENT_RULE_CREATED, RuleEvaluationConstants.SYSTEM_TRIGGER));
        }
        return results;
    }

    // ===================================================================
    //  Evaluate CRON Rules
    // ===================================================================

    /**
     * Evaluates all enabled CRON-schedule rules for a tenant.
     *
     * <p>Invoked by the cron scheduler. Uses {@code CRON} as the trigger
     * type, which is <b>not</b> in {@link #ASPECT_AUDITED_TRIGGERS}, so
     * the engine publishes the Kafka audit event internally.
     *
     * @param tenantId the tenant identifier
     * @return a list of evaluation results for all CRON rules
     */
    public List<RuleEvaluationResult> evaluateCronRules(String tenantId) {
        List<EntitlementAssignmentRule> rules = ruleRepository.findCronRules(tenantId);

        logger.info()
                .message(RuleEvaluationConstants.LOG_EVALUATE_CRON_START)
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .detail(RuleEvaluationConstants.DETAIL_RULE_COUNT, String.valueOf(rules.size()))
                .log();

        List<RuleEvaluationResult> results = new ArrayList<>();
        for (EntitlementAssignmentRule rule : rules) {
            results.add(evaluateRuleSafely(tenantId, rule,
                    RuleExecutionHistory.TriggerType.CRON, RuleEvaluationConstants.SYSTEM_TRIGGER));
        }
        return results;
    }

    // ===================================================================
    //  Evaluate Single Rule by ID
    // ===================================================================

    /**
     * Evaluates a single rule by ID with {@code MANUAL} trigger type.
     *
     * <p>This is the convenience overload called from the service's
     * {@code evaluateRule()} method (API path). The 3-arg signature
     * defaults the trigger type to {@code MANUAL}.
     *
     * @param tenantId    the tenant identifier
     * @param ruleId      the rule UUID
     * @param triggeredBy the username of the user who initiated the evaluation
     * @return the evaluation result
     * @throws IllegalArgumentException if the rule is not found for the tenant
     */
    public RuleEvaluationResult evaluateRuleById(String tenantId, UUID ruleId, String triggeredBy) {
        EntitlementAssignmentRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        RuleEvaluationConstants.ERROR_RULE_NOT_FOUND + ": " + ruleId));

        return evaluateRuleSafely(tenantId, rule,
                RuleExecutionHistory.TriggerType.MANUAL, triggeredBy);
    }

    /**
     * Evaluates a single rule by ID with an explicit trigger type.
     *
     * <p>This overload is called by the {@link com.aidentx.iam.entitlement_management.event.RuleEvaluationEventListener}
     * after a create or update transaction commits. The trigger type
     * ({@code ENTITLEMENT_RULE_CREATED} or {@code ENTITLEMENT_RULE_UPDATED})
     * determines whether the AOP aspect or the engine publishes the Kafka audit event.
     *
     * @param tenantId    the tenant identifier
     * @param ruleId      the rule UUID
     * @param triggerType the trigger type indicating why this evaluation was initiated
     * @param triggeredBy the username of the user who initiated the original operation
     * @return the evaluation result
     * @throws IllegalArgumentException if the rule is not found for the tenant
     */
    public RuleEvaluationResult evaluateRuleById(String tenantId, UUID ruleId,
                                                 RuleExecutionHistory.TriggerType triggerType,
                                                 String triggeredBy) {
        EntitlementAssignmentRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        RuleEvaluationConstants.ERROR_RULE_NOT_FOUND + ": " + ruleId));

        return evaluateRuleSafely(tenantId, rule, triggerType, triggeredBy);
    }

    // ===================================================================
    //  Core Evaluation with History Integration
    // ===================================================================

    /**
     * Evaluates a single rule within a safe try-catch boundary.
     *
     * <p>This is the central evaluation method that all public methods delegate
     * to. It handles:
     * <ol>
     *   <li>Creating the {@link ExecutionHistoryCollector}</li>
     *   <li>Delegating to {@link RuleEvaluationTransactionHelper} for
     *       transactional evaluation</li>
     *   <li>Flushing execution history (success or failure)</li>
     *   <li>Publishing Kafka audit events for non-aspect-audited triggers</li>
     *   <li>Persisting DB audit records for all trigger types</li>
     *   <li>Returning a result (or a failed result on exception)</li>
     * </ol>
     *
     * <p>Exceptions from the evaluation are caught, logged, and converted
     * to a {@link RuleEvaluationResult#failed} result. The caller always
     * receives a result object  never an exception.
     *
     * @param tenantId    the tenant identifier
     * @param rule        the rule entity to evaluate
     * @param triggerType the trigger type for history and audit context
     * @param triggeredBy the username or system identifier that initiated the evaluation
     * @return the evaluation result (never null)
     */
    private RuleEvaluationResult evaluateRuleSafely(String tenantId,
                                                    EntitlementAssignmentRule rule,
                                                    RuleExecutionHistory.TriggerType triggerType,
                                                    String triggeredBy) {
        long startTime = System.currentTimeMillis();

        String entitlementDisplayName = resolveEntitlementDisplayName(rule.getEntitlementId());

        ExecutionHistoryCollector collector = new ExecutionHistoryCollector(
                tenantId,
                rule.getId(),
                rule.getName(),
                rule.getEntitlementId(),
                entitlementDisplayName,
                rule.getResourceId(),
                triggerType,
                triggeredBy,
                rule.getConditions(),
                rule.getIdentityTypes()
        );

        try {
            logger.info()
                    .message(RuleEvaluationConstants.LOG_EVALUATE_RULE_START)
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                    .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(RuleEvaluationConstants.DETAIL_TRIGGER_TYPE, triggerType.name())
                    .log();

            RuleEvaluationResult result = transactionHelper.evaluateRuleInTransaction(tenantId, rule, collector);

            long duration = System.currentTimeMillis() - startTime;

            // Flush history  success path
            historyWriter.flush(collector, true, null);

            logger.info()
                    .message(RuleEvaluationConstants.LOG_EVALUATE_RULE_COMPLETE)
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                    .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(RuleEvaluationConstants.DETAIL_MATCHED_COUNT, String.valueOf(result.getIdentitiesMatched()))
                    .detail(RuleEvaluationConstants.DETAIL_CREATED_COUNT, String.valueOf(result.getAssignmentsCreated()))
                    .detail(RuleEvaluationConstants.DETAIL_REVOKED_COUNT, String.valueOf(result.getAssignmentsRevoked()))
                    .detail(RuleEvaluationConstants.DETAIL_SKIPPED_NO_SHADOW, String.valueOf(result.getSkippedNoShadow()))
                    .detail(RuleEvaluationConstants.DETAIL_DURATION_MS, String.valueOf(duration))
                    .detail(RuleEvaluationConstants.DETAIL_EXECUTION_ID,
                            collector.getExecutionId() != null
                                    ? collector.getExecutionId().toString()
                                    : RuleEvaluationConstants.EXECUTION_ID_NOT_PERSISTED)
                    .log();

            // Publish Kafka audit only for triggers NOT handled by the AOP aspect
            if (!ASPECT_AUDITED_TRIGGERS.contains(triggerType)) {
                publishEvaluationAuditEvent(tenantId, rule, entitlementDisplayName,
                        triggerType, triggeredBy, collector, result, duration);
            }

            // Persist to entitlement audit log  ALL trigger types
            persistEvaluationAuditLog(tenantId, rule, entitlementDisplayName,
                    triggerType, triggeredBy, collector, result, duration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Flush history  failure path
            historyWriter.flush(collector, false, e.getMessage());

            logger.error()
                    .message(RuleEvaluationConstants.LOG_EVALUATE_RULE_FAILED)
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                    .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(RuleEvaluationConstants.DETAIL_ERROR, e.getMessage())
                    .detail(RuleEvaluationConstants.DETAIL_DURATION_MS, String.valueOf(duration))
                    .detail(RuleEvaluationConstants.DETAIL_EXECUTION_ID,
                            collector.getExecutionId() != null
                                    ? collector.getExecutionId().toString()
                                    : RuleEvaluationConstants.EXECUTION_ID_NOT_PERSISTED)
                    .log();

            return RuleEvaluationResult.failed(rule.getId(), rule.getName(), e.getMessage(), duration);
        }
    }

    // ===================================================================
    //  DB Audit Persistence  All Trigger Types
    // ===================================================================

    /**
     * Persists a rule evaluation audit record to the entitlement audit log table.
     *
     * <p>Called for every evaluation regardless of trigger type. This ensures
     * MANUAL, CRON, EVALUATE_ALL, and post-commit evaluations all appear
     * in the entitlement audit history queryable by the governance dashboard.
     *
     * <p>The trigger source is resolved from the {@link RuleExecutionHistory.TriggerType}
     * to its corresponding {@code AppConstants.AUDIT_TRIGGER_SOURCE_*} string.
     *
     * <p>Failures are caught and logged  they never affect the evaluation result
     * or propagate to the caller.
     *
     * @param tenantId               the tenant identifier
     * @param rule                   the evaluated rule entity
     * @param entitlementDisplayName the resolved entitlement display name
     * @param triggerType            the trigger type
     * @param triggeredBy            the username or system identifier
     * @param collector              the execution history collector (for execution ID)
     * @param result                 the evaluation result with metrics
     * @param duration               the evaluation duration in milliseconds
     */
    private void persistEvaluationAuditLog(String tenantId,
                                           EntitlementAssignmentRule rule,
                                           String entitlementDisplayName,
                                           RuleExecutionHistory.TriggerType triggerType,
                                           String triggeredBy,
                                           ExecutionHistoryCollector collector,
                                           RuleEvaluationResult result,
                                           long duration) {
        try {
            String triggerSource = resolveTriggerSource(triggerType);

            String actorId = triggeredBy != null ? triggeredBy : AppConstants.AUDIT_ACTOR_SYSTEM_ID;
            String actorUsername = triggeredBy != null ? triggeredBy : AppConstants.AUDIT_ACTOR_SYSTEM_ID;

            String description = String.format(
                    RuleEvaluationConstants.AUDIT_DESC_RULE_EVALUATED_FORMAT,
                    rule.getName(),
                    entitlementDisplayName != null ? entitlementDisplayName : RuleEvaluationConstants.ENTITLEMENT_UNKNOWN,
                    result.getIdentitiesMatched(),
                    result.getAssignmentsCreated(),
                    result.getAssignmentsRevoked()
            );

            auditLogService.logRuleEvaluated(
                    tenantId,
                    rule.getEntitlementId(),
                    entitlementDisplayName,
                    rule.getResourceId(),
                    actorId,
                    actorUsername,
                    description,
                    triggerSource,
                    rule.getId(),
                    rule.getName(),
                    collector.getExecutionId(),
                    result.getIdentitiesMatched(),
                    result.getAssignmentsCreated(),
                    result.getAssignmentsRevoked(),
                    result.getSkippedNoShadow(),
                    duration
            );

            logger.info()
                    .message(RuleEvaluationConstants.LOG_AUDIT_DB_PERSISTED)
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                    .detail(RuleEvaluationConstants.DETAIL_TRIGGER_SOURCE, triggerSource)
                    .detail(RuleEvaluationConstants.DETAIL_ENTITLEMENT_ID, rule.getEntitlementId().toString())
                    .log();

        } catch (Exception e) {
            logger.warn()
                    .message(RuleEvaluationConstants.LOG_AUDIT_DB_PERSIST_FAILED)
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_ERROR, e.getMessage())
                    .log();
        }
    }

    // ===================================================================
    //  Kafka Audit Event Publishing  CRON only
    // ===================================================================


    /**
     * Publishes a {@code RULE_EVALUATED} Kafka audit event for triggers that are
     * <b>not</b> handled by the AOP aspect.
     *
     * <p>For {@code MANUAL} triggers, the actor is the authenticated user.
     * For {@code CRON} triggers, the actor is {@code SYSTEM}.
     *
     * @param tenantId               the tenant identifier
     * @param rule                   the evaluated rule entity
     * @param entitlementDisplayName the resolved entitlement display name
     * @param triggerType            the trigger type (used as trigger source in the event)
     * @param triggeredBy            the username or system identifier that initiated the evaluation
     * @param collector              the execution history collector (for execution ID)
     * @param result                 the evaluation result with metrics
     * @param duration               the total evaluation duration in milliseconds
     */
    private void publishEvaluationAuditEvent(String tenantId,
                                             EntitlementAssignmentRule rule,
                                             String entitlementDisplayName,
                                             RuleExecutionHistory.TriggerType triggerType,
                                             String triggeredBy,
                                             ExecutionHistoryCollector collector,
                                             RuleEvaluationResult result,
                                             long duration) {
        try {
            String actor = resolveActor(triggeredBy);

            EntitlementRuleAuditEvent auditEvent = EntitlementRuleAuditEvent.ruleEvaluated(
                    actor,
                    actor,
                    tenantId,
                    rule.getId().toString(),
                    rule.getName(),
                    rule.getEntitlementId().toString(),
                    entitlementDisplayName,
                    rule.getResourceId().toString(),
                    triggerType.name(),
                    collector.getExecutionId() != null
                            ? collector.getExecutionId().toString() : null,
                    result.getIdentitiesMatched(),
                    result.getAssignmentsCreated(),
                    result.getAssignmentsRevoked(),
                    result.getSkippedNoShadow(),
                    duration
            );
            auditPublisher.publish(auditEvent);

        } catch (Exception auditEx) {
            logger.warn()
                    .message(RuleEvaluationConstants.LOG_AUDIT_PUBLISH_FAILED)
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_ERROR, auditEx.getMessage())
                    .log();
        }
    }

    // ===================================================================
    //  Trigger Source Resolution
    // ===================================================================

    /**
     * Maps a {@link RuleExecutionHistory.TriggerType} to the audit-friendly
     * trigger source string for the entitlement audit log.
     *
     * @param triggerType the trigger type from the evaluation context
     * @return the corresponding {@code AppConstants.AUDIT_TRIGGER_SOURCE_*} value
     */
    private String resolveTriggerSource(RuleExecutionHistory.TriggerType triggerType) {
        return switch (triggerType) {
            case ENTITLEMENT_RULE_CREATED -> AppConstants.AUDIT_TRIGGER_SOURCE_ENTITLEMENT_RULE_CREATED;
            case ENTITLEMENT_RULE_UPDATED -> AppConstants.AUDIT_TRIGGER_SOURCE_ENTITLEMENT_RULE_UPDATED;
            case MANUAL -> AppConstants.AUDIT_TRIGGER_SOURCE_API;
            case CRON -> AppConstants.AUDIT_TRIGGER_SOURCE_CRON;
            case EVALUATE_ALL -> AppConstants.AUDIT_TRIGGER_SOURCE_EVALUATE_ALL;
        };
    }

    // ===================================================================
    //  Helpers
    // ===================================================================

    private String resolveActor(String triggeredBy) {
        return (triggeredBy != null && !triggeredBy.isBlank())
                ? triggeredBy
                : AppConstants.AUDIT_ACTOR_SYSTEM_ID;
    }

    /**
     * Resolves the display name of an entitlement by its UUID.
     *
     * <p>Returns a descriptive fallback string if the entitlement is not
     * found or if the lookup fails, ensuring evaluation is never blocked
     * by a missing display name.
     *
     * @param entitlementId the entitlement UUID
     * @return the display name, or a fallback string
     */
    private String resolveEntitlementDisplayName(UUID entitlementId) {
        try {
            return entitlementRepository.findById(entitlementId)
                    .map(Entitlement::getDisplayName)
                    .orElse(RuleEvaluationConstants.ENTITLEMENT_UNKNOWN_PREFIX + entitlementId + ")");
        } catch (Exception e) {
            return RuleEvaluationConstants.ENTITLEMENT_UNRESOLVABLE_PREFIX + entitlementId + ")";
        }
    }
}