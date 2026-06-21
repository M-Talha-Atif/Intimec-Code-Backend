package com.aidentx.iam.entitlement_management.audit;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.aidentx.iam.config.AppConstants;
import com.aidentx.iam.config.audit.AuditUser;
import com.aidentx.iam.config.audit.SecurityContextService;
import com.aidentx.iam.entitlement_management.model.dto.GlobalScheduleUpdateResponse;
import com.aidentx.iam.entitlement_management.model.dto.RuleEvaluationResult;
import com.aidentx.iam.entitlement_management.model.dto.RuleResponse;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignmentRule;
import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionHistory;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRuleRepository;
import com.aidentx.iam.entitlement_management.service.EntitlementAuditLogService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * AOP aspect responsible for publishing audit events for all entitlement
 * assignment rule operations.
 *
 * <p>Intercepts service-layer and engine-layer methods to capture before/after
 * state, compute diffs, resolve the authenticated actor, and publish structured
 * audit events to Kafka via {@link EntitlementRuleAuditPublisher}.
 *
 * <h3>Covered operations</h3>
 * <ul>
 *   <li><b>Create rule</b> captures the full post-creation snapshot.</li>
 *   <li><b>Update rule</b> captures a diff of changed fields plus an
 *       optional enabled/disabled toggle event.</li>
 *   <li><b>Delete rule</b> captures the pre-deletion snapshot.</li>
 *   <li><b>Evaluate rule (engine)</b>  publishes evaluation metrics for
 *       post-commit triggers ({@code ENTITLEMENT_RULE_CREATED},
 *       {@code ENTITLEMENT_RULE_UPDATED}, {@code MANUAL}). Falls back to
 *       {@code triggeredBy} from method arguments when the security context
 *       is no longer available (post-commit listener path).</li>
 *   <li><b>Evaluate all rules</b> publishes one evaluation event per rule.</li>
 *   <li><b>Global schedule update</b> captures the schedule configuration
 *       before and after the bulk update.</li>
 * </ul>
 *
 * <h3>Design principles</h3>
 * <ul>
 *   <li>Audit failures are caught and logged they never break the business
 *       operation.</li>
 *   <li>The engine's internal {@code publishEvaluationAuditEvent} is skipped
 *       for trigger types listed in the engine's {@code ASPECT_AUDITED_TRIGGERS}
 *       set, preventing duplicate audit events.</li>
 *   <li>Actor resolution prefers the security context; when unavailable
 *       (e.g. after transaction commit), the {@code triggeredBy} method
 *       argument is used as a fallback.</li>
 * </ul>
 *
 * @see EntitlementRuleAuditPublisher
 * @see EntitlementRuleAuditEvent
 */
@Aspect
@Component
public class EntitlementRuleAuditAspect {

    private final EntitlementRuleAuditPublisher auditPublisher;
    private final SecurityContextService securityContextService;
    private final EntitlementAssignmentRuleRepository ruleRepository;
    private final EntitlementAuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;


    /**
     * Constructs the audit aspect with all required dependencies.
     *
     * @param auditPublisher         publishes structured audit events to Kafka
     * @param securityContextService resolves the authenticated user from the security context
     * @param ruleRepository         reads rule entities for before-state capture and context resolution
     * @param objectMapper           serializes snapshots and diffs to JSON
     * @param loggerFactory          creates the unified logger for the audit category
     */
    public EntitlementRuleAuditAspect(EntitlementRuleAuditPublisher auditPublisher,
                                      SecurityContextService securityContextService,
                                      EntitlementAssignmentRuleRepository ruleRepository,
                                      EntitlementAuditLogService auditLogService,
                                      ObjectMapper objectMapper,
                                      UnifiedLoggerFactory loggerFactory) {
        this.auditPublisher = auditPublisher;
        this.securityContextService = securityContextService;
        this.ruleRepository = ruleRepository;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.logger = loggerFactory.getLogger(getClass(), AppConstants.AUDIT_LOG_CATEGORY);
    }

    // ===================================================================
    //  Pointcuts
    // ===================================================================

    /**
     * Matches {@code EntitlementAssignmentRuleServiceImpl.createRule(..)}.
     */
    @Pointcut("execution(public * com.aidentx.iam.entitlement_management.service.impl.EntitlementAssignmentRuleServiceImpl.createRule(..))")
    public void createRulePointcut() {
    }

    /**
     * Matches {@code EntitlementAssignmentRuleServiceImpl.updateRule(..)}.
     */
    @Pointcut("execution(public * com.aidentx.iam.entitlement_management.service.impl.EntitlementAssignmentRuleServiceImpl.updateRule(..))")
    public void updateRulePointcut() {
    }

    /**
     * Matches {@code RuleEvaluationEngine.evaluateRuleById(..)}.
     *
     * <p>This pointcut targets the engine directly because the post-commit
     * event listener calls the engine (not the service), so the service-level
     * pointcut would never fire for create/update-triggered evaluations.
     */
    @Pointcut("execution(public * com.aidentx.iam.entitlement_management.service.RuleEvaluationEngine.evaluateRuleById(..))")
    public void evaluateRuleByIdEnginePointcut() {
    }

    /**
     * Matches {@code EntitlementAssignmentRuleServiceImpl.deleteRule(..)}.
     */
    @Pointcut("execution(public * com.aidentx.iam.entitlement_management.service.impl.EntitlementAssignmentRuleServiceImpl.deleteRule(..))")
    public void deleteRulePointcut() {
    }

    /**
     * Matches {@code EntitlementAssignmentRuleServiceImpl.updateGlobalScheduleType(..)}.
     */
    @Pointcut("execution(public * com.aidentx.iam.entitlement_management.service.impl.EntitlementAssignmentRuleServiceImpl.updateGlobalScheduleType(..))")
    public void globalSchedulePointcut() {
    }

    /**
     * Matches {@code EntitlementAssignmentRuleServiceImpl.evaluateAllRules(..)}.
     */
    @Pointcut("execution(public * com.aidentx.iam.entitlement_management.service.impl.EntitlementAssignmentRuleServiceImpl.evaluateAllRules(..))")
    public void evaluateAllRulesPointcut() {
    }

    // ===================================================================
    //  Audit: Create Rule
    // ===================================================================

    /**
     * Publishes a {@code RULE_CREATED} audit event after a new assignment rule
     * is successfully created.
     *
     * <p>Captures the full post-creation state as {@code stateAfter}.
     * The actor is resolved from the current security context.
     *
     * @param pjp the join point wrapping {@code createRule()}
     * @return the original {@link RuleResponse} returned by the service
     * @throws Throwable if the underlying service method throws
     */
    @Around("createRulePointcut()")
    public Object auditCreateRule(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];

        Object result = pjp.proceed();

        try {
            RuleResponse created = (RuleResponse) result;
            AuditUser user = securityContextService.getAuditUser();
            JsonNode stateAfter = buildRuleSnapshot(created);

            EntitlementRuleAuditEvent event = EntitlementRuleAuditEvent.ruleCreated(
                    user.id(), user.username(), tenantId,
                    created.getId().toString(), created.getName(),
                    created.getEntitlementId().toString(),
                    resolveEntitlementName(created),
                    created.getResourceId().toString(),
                    created.getIdentityTypes(),
                    stateAfter
            );
            auditPublisher.publish(event);

            // Persist to DB
            auditLogService.logRuleCreated(
                    tenantId,
                    created.getEntitlementId(),
                    resolveEntitlementName(created),
                    created.getResourceId(),
                    user.id(), user.username(),
                    event.description(),
                    created.getId(), created.getName(),
                    stateAfter
            );

        } catch (Exception e) {
            logger.error()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_CREATE_RULE_AUDIT_FAILED)
                    .detail(AppConstants.AUDIT_DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName())
                    .detail(AppConstants.AUDIT_DETAIL_ERROR, e.getMessage())
                    .log();
        }

        return result;
    }

    // ===================================================================
    //  Audit: Update Rule
    // ===================================================================

    /**
     * Publishes a {@code RULE_UPDATED} audit event and optionally a
     * {@code RULE_TOGGLED} event after an assignment rule is updated.
     *
     * <p>Before the update, the current entity state is captured from the
     * database. After the update, a field-level diff is computed so that
     * only changed fields appear in {@code stateBefore} / {@code stateAfter}.
     *
     * <p>If the {@code enabled} flag changed, an additional
     * {@code RULE_ENABLED} or {@code RULE_DISABLED} toggle event is published.
     *
     * @param pjp the join point wrapping {@code updateRule()}
     * @return the original {@link RuleResponse} returned by the service
     * @throws Throwable if the underlying service method throws
     */
    @Around("updateRulePointcut()")
    public Object auditUpdateRule(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];
        UUID ruleId = (UUID) pjp.getArgs()[1];

        RuleSnapshot beforeSnapshot = null;
        Boolean enabledBefore = null;
        try {
            EntitlementAssignmentRule beforeRule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                    .orElse(null);
            if (beforeRule != null) {
                beforeSnapshot = RuleSnapshot.fromEntity(beforeRule, objectMapper);
                enabledBefore = beforeRule.isEnabled();
            }
        } catch (Exception e) {
            logger.warn()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_BEFORE_STATE_CAPTURE_FAILED)
                    .detail(AppConstants.AUDIT_DETAIL_OPERATION, AppConstants.AUDIT_OPERATION_UPDATE_RULE)
                    .detail(AppConstants.AUDIT_DETAIL_ERROR, e.getMessage())
                    .log();
        }

        Object result = pjp.proceed();

        try {
            RuleResponse updated = (RuleResponse) result;
            AuditUser user = securityContextService.getAuditUser();

            RuleSnapshot afterSnapshot = RuleSnapshot.fromResponse(updated, objectMapper);

            JsonNode stateBefore;
            JsonNode stateAfter;

            if (beforeSnapshot != null) {
                DiffResult diff = buildDiff(beforeSnapshot, afterSnapshot);
                stateBefore = diff.before;
                stateAfter = diff.after;
            } else {
                stateBefore = null;
                stateAfter = buildRuleSnapshot(updated);
            }

            EntitlementRuleAuditEvent event = EntitlementRuleAuditEvent.ruleUpdated(
                    user.id(), user.username(), tenantId,
                    updated.getId().toString(), updated.getName(),
                    updated.getEntitlementId().toString(),
                    resolveEntitlementName(updated),
                    updated.getResourceId().toString(),
                    updated.getIdentityTypes(),
                    stateBefore, stateAfter
            );
            auditPublisher.publish(event);

            // Persist to DB
            auditLogService.logRuleUpdated(
                    tenantId,
                    updated.getEntitlementId(),
                    resolveEntitlementName(updated),
                    updated.getResourceId(),
                    user.id(), user.username(),
                    event.description(),
                    updated.getId(), updated.getName(),
                    stateBefore, stateAfter
            );

            // Publish a separate toggle event if enabled/disabled changed
            if (enabledBefore != null && enabledBefore != updated.isEnabled()) {
                ObjectNode toggleBefore = objectMapper.createObjectNode();
                toggleBefore.put(AppConstants.AUDIT_FIELD_ENABLED, enabledBefore);
                ObjectNode toggleAfter = objectMapper.createObjectNode();
                toggleAfter.put(AppConstants.AUDIT_FIELD_ENABLED, updated.isEnabled());

                EntitlementRuleAuditEvent toggleEvent = EntitlementRuleAuditEvent.ruleToggled(
                        user.id(), user.username(), tenantId,
                        updated.getId().toString(), updated.getName(),
                        updated.isEnabled(),
                        toggleBefore, toggleAfter
                );
                auditPublisher.publish(toggleEvent);

            }

        } catch (Exception e) {
            logger.error()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_UPDATE_RULE_AUDIT_FAILED)
                    .detail(AppConstants.AUDIT_DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName())
                    .detail(AppConstants.AUDIT_DETAIL_ERROR, e.getMessage())
                    .log();
        }

        return result;
    }

    // ===================================================================
    //  Audit: Delete Rule
    // ===================================================================

    /**
     * Publishes a {@code RULE_DELETED} audit event after an assignment rule
     * is deleted.
     *
     * <p>Before deletion, the full entity state is captured as
     * {@code stateBefore}. If the pre-deletion read fails, the audit event
     * is still published with whatever context could be gathered.
     *
     * @param pjp the join point wrapping {@code deleteRule()}
     * @return the original result (void) from the service
     * @throws Throwable if the underlying service method throws
     */
    @Around("deleteRulePointcut()")
    public Object auditDeleteRule(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];
        UUID ruleId = (UUID) pjp.getArgs()[1];

        JsonNode stateBefore = null;
        String ruleName = AppConstants.AUDIT_UNKNOWN;
        String entitlementId = null;
        String entitlementName = null;
        String resourceId = null;
        try {
            EntitlementAssignmentRule beforeRule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                    .orElse(null);
            if (beforeRule != null) {
                stateBefore = buildEntitySnapshot(beforeRule);
                ruleName = beforeRule.getName();
                entitlementId = beforeRule.getEntitlementId().toString();
                resourceId = beforeRule.getResourceId().toString();
            }
        } catch (Exception e) {
            logger.warn()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_BEFORE_STATE_CAPTURE_FAILED)
                    .detail(AppConstants.AUDIT_DETAIL_OPERATION, AppConstants.AUDIT_OPERATION_DELETE_RULE)
                    .detail(AppConstants.AUDIT_DETAIL_ERROR, e.getMessage())
                    .log();
        }

        Object result = pjp.proceed();

        try {
            AuditUser user = securityContextService.getAuditUser();

            EntitlementRuleAuditEvent event = EntitlementRuleAuditEvent.ruleDeleted(
                    user.id(), user.username(), tenantId,
                    ruleId.toString(), ruleName,
                    entitlementId, entitlementName,
                    resourceId, 0,
                    stateBefore
            );
            auditPublisher.publish(event);

            // Persist to DB
            auditLogService.logRuleDeleted(
                    tenantId,
                    entitlementId != null ? UUID.fromString(entitlementId) : null,
                    entitlementName,
                    resourceId != null ? UUID.fromString(resourceId) : null,
                    user.id(), user.username(),
                    event.description(),
                    ruleId, ruleName,
                    stateBefore
            );

        } catch (Exception e) {
            logger.error()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_DELETE_RULE_AUDIT_FAILED)
                    .detail(AppConstants.AUDIT_DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName())
                    .detail(AppConstants.AUDIT_DETAIL_ERROR, e.getMessage())
                    .log();
        }

        return result;
    }

    // ===================================================================
    //  Audit: Evaluate Rule via Engine
    // ===================================================================

    /**
     * Publishes a {@code RULE_EVALUATED} audit event after the engine
     * completes a single-rule evaluation.
     *
     * <p>This advice targets {@link com.aidentx.iam.entitlement_management.service.RuleEvaluationEngine#evaluateRuleById(String, UUID, RuleExecutionHistory.TriggerType, String)}
     * directly, because the post-commit event listener calls the engine
     * (not the service), so a service-level pointcut would miss those calls.
     *
     * <h4>Actor resolution</h4>
     * <p>The security context is typically cleared after the originating
     * transaction commits. When the context is unavailable, this method
     * falls back to the {@code triggeredBy} argument (index 3) which
     * carries the original username from the service → event → listener
     * → engine call chain.
     *
     * <h4>Trigger type handling</h4>
     * <ul>
     *   <li>{@code ENTITLEMENT_RULE_CREATED} audit published here with
     *       trigger source {@code ENTITLEMENT_RULE_CREATED}</li>
     *   <li>{@code ENTITLEMENT_RULE_UPDATED} audit published here with
     *       trigger source {@code ENTITLEMENT_RULE_UPDATED}</li>
     *   <li>{@code MANUAL} audit published here with trigger source
     *       {@code API} (called from service's evaluateRule)</li>
     *   <li>{@code CRON} / {@code EVALUATE_ALL} skipped here; the engine
     *       or the evaluate-all aspect handles those</li>
     * </ul>
     *
     * @param pjp the join point wrapping {@code evaluateRuleById()}
     * @return the original {@link RuleEvaluationResult} from the engine
     * @throws Throwable if the underlying engine method throws
     */
    @Around("evaluateRuleByIdEnginePointcut()")
    public Object auditEvaluateRuleByIdEngine(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();

        if (args.length < 4 || !(args[2] instanceof RuleExecutionHistory.TriggerType)) {
            return pjp.proceed();
        }

        String tenantId = (String) args[0];
        UUID ruleId = (UUID) args[1];
        RuleExecutionHistory.TriggerType triggerType = (RuleExecutionHistory.TriggerType) args[2];
        String triggeredBy = (String) args[3];

        if (triggerType == RuleExecutionHistory.TriggerType.CRON
                || triggerType == RuleExecutionHistory.TriggerType.EVALUATE_ALL) {
            logger.info()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SKIPPING_ASPECT_AUDIT)
                    .detail(AppConstants.AUDIT_DETAIL_RULE_ID, ruleId.toString())
                    .detail(AppConstants.AUDIT_DETAIL_TRIGGER_TYPE, triggerType.name())
                    .log();
            return pjp.proceed();
        }

        AuditUser user = resolveAuditUser(triggeredBy);

        logger.info()
                .category(AppConstants.AUDIT_LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_STARTING_RULE_EVAL_AUDIT)
                .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.AUDIT_DETAIL_RULE_ID, ruleId.toString())
                .detail(AppConstants.AUDIT_DETAIL_TRIGGER_TYPE, triggerType.name())
                .detail(AppConstants.AUDIT_DETAIL_TRIGGERED_BY, triggeredBy)
                .detail(AppConstants.AUDIT_DETAIL_RESOLVED_ACTOR, user.username())
                .log();

        String entitlementId = null;
        String entitlementName = null;
        String resourceId = null;
        String ruleName = null;
        try {
            EntitlementAssignmentRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                    .orElse(null);
            if (rule != null) {
                ruleName = rule.getName();
                entitlementId = rule.getEntitlementId().toString();
                resourceId = rule.getResourceId().toString();
                if (ruleName != null && ruleName.startsWith(AppConstants.AUDIT_RULE_NAME_PREFIX)) {
                    entitlementName = ruleName.substring(AppConstants.AUDIT_RULE_NAME_PREFIX.length());
                }
                logger.info()
                        .category(AppConstants.AUDIT_LOG_CATEGORY)
                        .message(AppConstants.AUDIT_LOG_RULE_CONTEXT_RESOLVED)
                        .detail(AppConstants.AUDIT_DETAIL_RULE_ID, ruleId.toString())
                        .detail(AppConstants.AUDIT_DETAIL_RULE_NAME, ruleName)
                        .detail(AppConstants.AUDIT_DETAIL_ENTITLEMENT_ID, entitlementId)
                        .detail(AppConstants.AUDIT_DETAIL_RESOURCE_ID, resourceId)
                        .log();
            } else {
                logger.warn()
                        .category(AppConstants.AUDIT_LOG_CATEGORY)
                        .message(AppConstants.AUDIT_LOG_RULE_NOT_FOUND_FOR_AUDIT)
                        .detail(AppConstants.AUDIT_DETAIL_RULE_ID, ruleId.toString())
                        .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
                        .log();
            }
        } catch (Exception e) {
            logger.warn()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_BEFORE_STATE_CAPTURE_FAILED)
                    .detail(AppConstants.AUDIT_DETAIL_OPERATION, AppConstants.AUDIT_OPERATION_EVALUATE_RULE)
                    .detail(AppConstants.AUDIT_DETAIL_ERROR, e.getMessage())
                    .log();
        }

        Object result = pjp.proceed();

        try {
            RuleEvaluationResult evalResult = (RuleEvaluationResult) result;

            String finalRuleName = evalResult.getRuleName() != null ? evalResult.getRuleName() : ruleName;
            String triggerSource = resolveAuditTriggerSource(triggerType);

            logger.info()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_RULE_EVAL_COMPLETED)
                    .detail(AppConstants.AUDIT_DETAIL_RULE_ID, ruleId.toString())
                    .detail(AppConstants.AUDIT_DETAIL_RULE_NAME, finalRuleName)
                    .detail(AppConstants.AUDIT_DETAIL_TRIGGER_SOURCE, triggerSource)
                    .detail(AppConstants.AUDIT_DETAIL_IDENTITIES_MATCHED, evalResult.getIdentitiesMatched())
                    .detail(AppConstants.AUDIT_DETAIL_ASSIGNMENTS_CREATED, evalResult.getAssignmentsCreated())
                    .detail(AppConstants.AUDIT_DETAIL_ASSIGNMENTS_REVOKED, evalResult.getAssignmentsRevoked())
                    .detail(AppConstants.AUDIT_DETAIL_SKIPPED_NO_SHADOW, evalResult.getSkippedNoShadow())
                    .detail(AppConstants.AUDIT_DETAIL_DURATION_MS, evalResult.getDurationMs())
                    .log();

            EntitlementRuleAuditEvent event = EntitlementRuleAuditEvent.ruleEvaluated(
                    user.id(), user.username(), tenantId,
                    ruleId.toString(),
                    finalRuleName,
                    entitlementId, entitlementName,
                    resourceId,
                    triggerSource,
                    null,
                    evalResult.getIdentitiesMatched(),
                    evalResult.getAssignmentsCreated(),
                    evalResult.getAssignmentsRevoked(),
                    evalResult.getSkippedNoShadow(),
                    evalResult.getDurationMs()
            );
            auditPublisher.publish(event);

            logger.info()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_KAFKA_PUBLISHED_RULE_EVAL)
                    .detail(AppConstants.AUDIT_DETAIL_RULE_ID, ruleId.toString())
                    .detail(AppConstants.AUDIT_DETAIL_ENTITLEMENT_ID,
                            entitlementId != null ? entitlementId : AppConstants.AUDIT_DETAIL_NULL)
                    .log();

        } catch (Exception e) {
            logger.error()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_EVALUATE_RULE_AUDIT_FAILED)
                    .detail(AppConstants.AUDIT_DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName())
                    .detail(AppConstants.AUDIT_DETAIL_ERROR, e.getMessage())
                    .log();
        }

        return result;
    }
    // ===================================================================
    //  Audit: Evaluate All Rules
    // ===================================================================

    /**
     * Publishes one {@code RULE_EVALUATED} audit event per rule after a
     * bulk evaluate-all operation completes.
     *
     * <p>The actor is resolved from the security context (available because
     * evaluate-all is always an API call within the HTTP request lifecycle).
     *
     * <p>Each rule is looked up individually for entitlement context. Lookup
     * failures are silently ignored the audit event is still published
     * with whatever context could be gathered.
     *
     * @param pjp the join point wrapping {@code evaluateAllRules()}
     * @return the original {@code List<RuleEvaluationResult>} from the service
     * @throws Throwable if the underlying service method throws
     */
    @SuppressWarnings("unchecked")
    @Around("evaluateAllRulesPointcut()")
    public Object auditEvaluateAllRules(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];

        AuditUser user;
        try {
            user = securityContextService.getAuditUser();
        } catch (Exception e) {
            user = new AuditUser(AppConstants.AUDIT_ACTOR_SYSTEM_ID, AppConstants.AUDIT_ACTOR_SYSTEM_ID, AppConstants.AUDIT_METADATA_USER_IP);
        }

        logger.info()
                .category(AppConstants.AUDIT_LOG_CATEGORY)
                .message("Starting audit for evaluate-all-rules")
                .detail("tenantId", tenantId)
                .detail("actorUsername", user.username())
                .log();

        Object result = pjp.proceed();

        try {
            List<RuleEvaluationResult> results = (List<RuleEvaluationResult>) result;

            logger.info()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message("Evaluate-all completed, publishing audit events")
                    .detail("tenantId", tenantId)
                    .detail("totalRulesEvaluated", results.size())
                    .log();

            for (RuleEvaluationResult evalResult : results) {
                String entitlementId = null;
                String entitlementName = null;
                String resourceId = null;

                try {
                    EntitlementAssignmentRule rule = ruleRepository
                            .findByIdAndTenantId(evalResult.getRuleId(), tenantId)
                            .orElse(null);
                    if (rule != null) {
                        entitlementId = rule.getEntitlementId().toString();
                        resourceId = rule.getResourceId().toString();
                        String name = rule.getName();
                        if (name != null && name.startsWith(AppConstants.AUDIT_RULE_NAME_PREFIX)) {
                            entitlementName = name.substring(AppConstants.AUDIT_RULE_NAME_PREFIX.length());
                        }
                    }
                } catch (Exception ignored) {
                    // Best effort — continue with nulls
                }

                EntitlementRuleAuditEvent event = EntitlementRuleAuditEvent.ruleEvaluated(
                        user.id(), user.username(), tenantId,
                        evalResult.getRuleId() != null ? evalResult.getRuleId().toString() : null,
                        evalResult.getRuleName(),
                        entitlementId, entitlementName,
                        resourceId,
                        AppConstants.AUDIT_TRIGGER_SOURCE_EVALUATE_ALL,
                        null,
                        evalResult.getIdentitiesMatched(),
                        evalResult.getAssignmentsCreated(),
                        evalResult.getAssignmentsRevoked(),
                        evalResult.getSkippedNoShadow(),
                        evalResult.getDurationMs()
                );
                auditPublisher.publish(event);

                logger.info()
                        .category(AppConstants.AUDIT_LOG_CATEGORY)
                        .message("Kafka audit event published for evaluate-all rule")
                        .detail("ruleId", evalResult.getRuleId() != null ? evalResult.getRuleId().toString() : "null")
                        .detail("ruleName", evalResult.getRuleName())
                        .detail("entitlementId", entitlementId != null ? entitlementId : "null")
                        .detail("identitiesMatched", evalResult.getIdentitiesMatched())
                        .detail("assignmentsCreated", evalResult.getAssignmentsCreated())
                        .detail("assignmentsRevoked", evalResult.getAssignmentsRevoked())
                        .log();

                // Persist to DB
                auditLogService.logRuleEvaluated(
                        tenantId,
                        entitlementId != null ? UUID.fromString(entitlementId) : null,
                        entitlementName,
                        resourceId != null ? UUID.fromString(resourceId) : null,
                        user.id(), user.username(),
                        event.description(),
                        AppConstants.AUDIT_TRIGGER_SOURCE_EVALUATE_ALL,
                        evalResult.getRuleId(), evalResult.getRuleName(), null,
                        evalResult.getIdentitiesMatched(),
                        evalResult.getAssignmentsCreated(),
                        evalResult.getAssignmentsRevoked(),
                        evalResult.getSkippedNoShadow(),
                        evalResult.getDurationMs()
                );

                logger.info()
                        .category(AppConstants.AUDIT_LOG_CATEGORY)
                        .message("DB audit record persisted for evaluate-all rule")
                        .detail("ruleId", evalResult.getRuleId() != null ? evalResult.getRuleId().toString() : "null")
                        .detail("entitlementId", entitlementId != null ? entitlementId : "null")
                        .log();
            }

            logger.info()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message("All evaluate-all audit events published and persisted")
                    .detail("tenantId", tenantId)
                    .detail("totalProcessed", results.size())
                    .log();

        } catch (Exception e) {
            logger.error()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_EVALUATE_ALL_AUDIT_FAILED)
                    .detail(AppConstants.AUDIT_DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName())
                    .detail(AppConstants.AUDIT_DETAIL_ERROR, e.getMessage())
                    .log();
        }

        return result;
    }
    // ===================================================================
    //  Audit: Global Schedule Update
    // ===================================================================

    /**
     * Publishes a {@code GLOBAL_SCHEDULE_UPDATED} audit event after the
     * global schedule configuration is changed for a tenant.
     *
     * <p>Before the update, the current schedule configuration is captured
     * from the first enabled rule (representative sample). After the update,
     * the full {@link GlobalScheduleUpdateResponse} is serialized as
     * {@code stateAfter}.
     *
     * @param pjp the join point wrapping {@code updateGlobalScheduleType()}
     * @return the original {@link GlobalScheduleUpdateResponse} from the service
     * @throws Throwable if the underlying service method throws
     */
    @Around("globalSchedulePointcut()")
    public Object auditGlobalSchedule(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];

        JsonNode stateBefore = null;
        try {
            var rules = ruleRepository.findByTenantIdAndEnabledTrueOrderByPriorityAsc(tenantId);
            if (!rules.isEmpty()) {
                var firstRule = rules.get(0);
                var beforeMap = Map.of(
                        AppConstants.AUDIT_FIELD_SCHEDULE_TYPE, String.valueOf(firstRule.getScheduleType()),
                        AppConstants.AUDIT_FIELD_CRON_EXPRESSION, firstRule.getCronExpression() != null
                                ? firstRule.getCronExpression() : "",
                        AppConstants.AUDIT_FIELD_SCHEDULE_TIME, firstRule.getScheduleTime() != null
                                ? firstRule.getScheduleTime() : "",
                        AppConstants.AUDIT_FIELD_SCHEDULE_TIMEZONE, firstRule.getScheduleTimezone() != null
                                ? firstRule.getScheduleTimezone() : "",
                        AppConstants.AUDIT_FIELD_TOTAL_ENABLED_RULES, String.valueOf(rules.size())
                );
                stateBefore = objectMapper.valueToTree(beforeMap);
            }
        } catch (Exception e) {
            logger.warn()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_BEFORE_STATE_CAPTURE_FAILED)
                    .detail(AppConstants.AUDIT_DETAIL_OPERATION, AppConstants.AUDIT_OPERATION_GLOBAL_SCHEDULE)
                    .detail(AppConstants.AUDIT_DETAIL_ERROR, e.getMessage())
                    .log();
        }

        Object result = pjp.proceed();

        try {
            GlobalScheduleUpdateResponse response = (GlobalScheduleUpdateResponse) result;
            AuditUser user = securityContextService.getAuditUser();
            JsonNode stateAfter = objectMapper.valueToTree(response);

            EntitlementRuleAuditEvent event = EntitlementRuleAuditEvent.globalScheduleUpdated(
                    user.id(), user.username(), tenantId,
                    response.getRulesUpdated(),
                    stateBefore, stateAfter
            );
            auditPublisher.publish(event);

        } catch (Exception e) {
            logger.error()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_GLOBAL_SCHEDULE_AUDIT_FAILED)
                    .detail(AppConstants.AUDIT_DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName())
                    .detail(AppConstants.AUDIT_DETAIL_ERROR, e.getMessage())
                    .log();
        }

        return result;
    }

    // ===================================================================
    //  Actor Resolution
    // ===================================================================

    /**
     * Resolves the audit actor, preferring the security context and falling
     * back to the {@code triggeredBy} string from method arguments.
     *
     * <p>This fallback is essential for the post-commit listener path where
     * the HTTP request (and its security context) has already completed.
     *
     * @param triggeredBy the username passed through the event chain; used
     *                    as fallback when the security context is unavailable
     * @return an {@link AuditUser} representing the actor
     */
    private AuditUser resolveAuditUser(String triggeredBy) {
        try {
            return securityContextService.getAuditUser();
        } catch (Exception e) {
            return (triggeredBy != null && !triggeredBy.isBlank())
                    ? new AuditUser(triggeredBy, triggeredBy, AppConstants.AUDIT_METADATA_USER_IP)
                    : new AuditUser(AppConstants.AUDIT_ACTOR_SYSTEM_ID, AppConstants.AUDIT_ACTOR_SYSTEM_ID, AppConstants.AUDIT_METADATA_USER_IP);
        }
    }

    // ===================================================================
    //  Trigger Source Resolution
    // ===================================================================

    /**
     * Maps a {@link RuleExecutionHistory.TriggerType} to the audit-friendly
     * trigger source string stored in the Kafka event.
     *
     * @param triggerType the trigger type from the engine method argument
     * @return the corresponding {@code AppConstants.AUDIT_TRIGGER_SOURCE_*} value
     */
    private String resolveAuditTriggerSource(RuleExecutionHistory.TriggerType triggerType) {
        return switch (triggerType) {
            case ENTITLEMENT_RULE_CREATED -> AppConstants.AUDIT_TRIGGER_SOURCE_ENTITLEMENT_RULE_CREATED;
            case ENTITLEMENT_RULE_UPDATED -> AppConstants.AUDIT_TRIGGER_SOURCE_ENTITLEMENT_RULE_UPDATED;
            case MANUAL -> AppConstants.AUDIT_TRIGGER_SOURCE_API;
            case CRON -> AppConstants.AUDIT_TRIGGER_SOURCE_CRON;
            case EVALUATE_ALL -> AppConstants.AUDIT_TRIGGER_SOURCE_API;
        };
    }

    // ===================================================================
    //  Snapshot Builders
    // ===================================================================

    /**
     * Builds a JSON snapshot of a rule from its response DTO.
     *
     * <p>Captures: name, enabled, scheduleType, priority, identityTypes,
     * and optional cron fields and condition groups.
     *
     * @param response the rule response DTO
     * @return a JSON object representing the rule state
     */
    private JsonNode buildRuleSnapshot(RuleResponse response) {
        ObjectNode snapshot = objectMapper.createObjectNode();

        snapshot.put(AppConstants.AUDIT_FIELD_NAME, response.getName());
        snapshot.put(AppConstants.AUDIT_FIELD_ENABLED, response.isEnabled());
        snapshot.put(AppConstants.AUDIT_FIELD_SCHEDULE_TYPE, response.getScheduleType());
        snapshot.put(AppConstants.AUDIT_FIELD_PRIORITY, response.getPriority());
        if (response.getIdentityTypes() != null && !response.getIdentityTypes().isEmpty()) {
            snapshot.set(AppConstants.AUDIT_FIELD_IDENTITY_TYPES, objectMapper.valueToTree(response.getIdentityTypes()));
        }

        if (response.getCronExpression() != null) {
            snapshot.put(AppConstants.AUDIT_FIELD_CRON_EXPRESSION, response.getCronExpression());
        }
        if (response.getScheduleTime() != null) {
            snapshot.put(AppConstants.AUDIT_FIELD_SCHEDULE_TIME, response.getScheduleTime());
        }
        if (response.getScheduleTimezone() != null) {
            snapshot.put(AppConstants.AUDIT_FIELD_SCHEDULE_TIMEZONE, response.getScheduleTimezone());
        }

        if (response.getConditionGroups() != null) {
            snapshot.set(AppConstants.AUDIT_FIELD_CONDITION_GROUPS,
                    objectMapper.valueToTree(response.getConditionGroups()));
        }

        return snapshot;
    }

    /**
     * Builds a JSON snapshot of a rule from its JPA entity.
     *
     * <p>Used for pre-deletion state capture where no response DTO exists.
     *
     * @param entity the rule entity
     * @return a JSON object representing the rule state
     */
    private JsonNode buildEntitySnapshot(EntitlementAssignmentRule entity) {
        ObjectNode snapshot = objectMapper.createObjectNode();

        snapshot.put(AppConstants.AUDIT_FIELD_NAME, entity.getName());
        snapshot.put(AppConstants.AUDIT_FIELD_ENABLED, entity.isEnabled());
        snapshot.put(AppConstants.AUDIT_FIELD_SCHEDULE_TYPE, String.valueOf(entity.getScheduleType()));
        snapshot.put(AppConstants.AUDIT_FIELD_PRIORITY, entity.getPriority());
        if (entity.getIdentityTypes() != null && !entity.getIdentityTypes().isEmpty()) {
            snapshot.set(AppConstants.AUDIT_FIELD_IDENTITY_TYPES, objectMapper.valueToTree(entity.getIdentityTypes()));
        }

        if (entity.getCronExpression() != null) {
            snapshot.put(AppConstants.AUDIT_FIELD_CRON_EXPRESSION, entity.getCronExpression());
        }
        if (entity.getScheduleTime() != null) {
            snapshot.put(AppConstants.AUDIT_FIELD_SCHEDULE_TIME, entity.getScheduleTime());
        }
        if (entity.getScheduleTimezone() != null) {
            snapshot.put(AppConstants.AUDIT_FIELD_SCHEDULE_TIMEZONE, entity.getScheduleTimezone());
        }

        if (entity.getConditions() != null) {
            snapshot.set(AppConstants.AUDIT_FIELD_CONDITION_GROUPS,
                    objectMapper.valueToTree(entity.getConditions()));
        }

        return snapshot;
    }

    // ===================================================================
    //  Diff Builder
    // ===================================================================

    /**
     * Immutable snapshot of auditable rule fields, used for computing
     * before/after diffs on update operations.
     */
    private record RuleSnapshot(
            String name,
            boolean enabled,
            String scheduleType,
            String cronExpression,
            String scheduleTime,
            String scheduleTimezone,
            int priority,
            List<String> identityTypes,
            JsonNode conditionGroups
    ) {
        /**
         * Creates a snapshot from a JPA entity (before-state).
         *
         * @param entity the rule entity
         * @param om     the object mapper for condition group serialization
         * @return the snapshot
         */
        static RuleSnapshot fromEntity(EntitlementAssignmentRule entity, ObjectMapper om) {
            return new RuleSnapshot(
                    entity.getName(),
                    entity.isEnabled(),
                    String.valueOf(entity.getScheduleType()),
                    entity.getCronExpression(),
                    entity.getScheduleTime(),
                    entity.getScheduleTimezone(),
                    entity.getPriority(),
                    entity.getIdentityTypes(),
                    entity.getConditions() != null ? om.valueToTree(entity.getConditions()) : null
            );
        }

        /**
         * Creates a snapshot from a response DTO (after-state).
         *
         * @param response the rule response DTO
         * @param om       the object mapper for condition group serialization
         * @return the snapshot
         */
        static RuleSnapshot fromResponse(RuleResponse response, ObjectMapper om) {
            return new RuleSnapshot(
                    response.getName(),
                    response.isEnabled(),
                    response.getScheduleType(),
                    response.getCronExpression(),
                    response.getScheduleTime(),
                    response.getScheduleTimezone(),
                    response.getPriority(),
                    response.getIdentityTypes(),
                    response.getConditionGroups() != null ? om.valueToTree(response.getConditionGroups()) : null
            );
        }
    }

    /**
     * Holds the before and after JSON nodes containing only changed fields.
     */
    private record DiffResult(JsonNode before, JsonNode after) {
    }

    /**
     * Computes a field-level diff between two rule snapshots.
     *
     * <p>Only fields that actually changed between {@code before} and
     * {@code after} are included in the resulting JSON nodes. If nothing
     * changed, both nodes will be {@code null}.
     *
     * @param before the pre-update snapshot
     * @param after  the post-update snapshot
     * @return a {@link DiffResult} with only the changed fields, or nulls if identical
     */
    private DiffResult buildDiff(RuleSnapshot before, RuleSnapshot after) {
        ObjectNode beforeNode = objectMapper.createObjectNode();
        ObjectNode afterNode = objectMapper.createObjectNode();

        if (!Objects.equals(before.name(), after.name())) {
            beforeNode.put(AppConstants.AUDIT_FIELD_NAME, before.name());
            afterNode.put(AppConstants.AUDIT_FIELD_NAME, after.name());
        }

        if (before.enabled() != after.enabled()) {
            beforeNode.put(AppConstants.AUDIT_FIELD_ENABLED, before.enabled());
            afterNode.put(AppConstants.AUDIT_FIELD_ENABLED, after.enabled());
        }

        if (!Objects.equals(before.scheduleType(), after.scheduleType())) {
            beforeNode.put(AppConstants.AUDIT_FIELD_SCHEDULE_TYPE,
                    before.scheduleType() != null ? before.scheduleType() : "");
            afterNode.put(AppConstants.AUDIT_FIELD_SCHEDULE_TYPE,
                    after.scheduleType() != null ? after.scheduleType() : "");
        }

        if (!Objects.equals(before.cronExpression(), after.cronExpression())) {
            beforeNode.put(AppConstants.AUDIT_FIELD_CRON_EXPRESSION,
                    before.cronExpression() != null ? before.cronExpression() : "");
            afterNode.put(AppConstants.AUDIT_FIELD_CRON_EXPRESSION,
                    after.cronExpression() != null ? after.cronExpression() : "");
        }

        if (!Objects.equals(before.scheduleTime(), after.scheduleTime())) {
            beforeNode.put(AppConstants.AUDIT_FIELD_SCHEDULE_TIME,
                    before.scheduleTime() != null ? before.scheduleTime() : "");
            afterNode.put(AppConstants.AUDIT_FIELD_SCHEDULE_TIME,
                    after.scheduleTime() != null ? after.scheduleTime() : "");
        }

        if (!Objects.equals(before.scheduleTimezone(), after.scheduleTimezone())) {
            beforeNode.put(AppConstants.AUDIT_FIELD_SCHEDULE_TIMEZONE,
                    before.scheduleTimezone() != null ? before.scheduleTimezone() : "");
            afterNode.put(AppConstants.AUDIT_FIELD_SCHEDULE_TIMEZONE,
                    after.scheduleTimezone() != null ? after.scheduleTimezone() : "");
        }

        if (before.priority() != after.priority()) {
            beforeNode.put(AppConstants.AUDIT_FIELD_PRIORITY, before.priority());
            afterNode.put(AppConstants.AUDIT_FIELD_PRIORITY, after.priority());
        }

        if (!Objects.equals(before.identityTypes(), after.identityTypes())) {
            if (before.identityTypes() != null) {
                beforeNode.set(AppConstants.AUDIT_FIELD_IDENTITY_TYPES, objectMapper.valueToTree(before.identityTypes()));
            }
            if (after.identityTypes() != null) {
                afterNode.set(AppConstants.AUDIT_FIELD_IDENTITY_TYPES, objectMapper.valueToTree(after.identityTypes()));
            }
        }

        if (!Objects.equals(before.conditionGroups(), after.conditionGroups())) {
            if (before.conditionGroups() != null) {
                beforeNode.set(AppConstants.AUDIT_FIELD_CONDITION_GROUPS, before.conditionGroups());
            }
            if (after.conditionGroups() != null) {
                afterNode.set(AppConstants.AUDIT_FIELD_CONDITION_GROUPS, after.conditionGroups());
            }
        }

        return new DiffResult(
                beforeNode.isEmpty() ? null : beforeNode,
                afterNode.isEmpty() ? null : afterNode
        );
    }

    // ===================================================================
    //  Helpers
    // ===================================================================

    /**
     * Extracts the entitlement display name from the rule name by stripping
     * the standard prefix ({@code "Dynamic Rule - "}).
     *
     * @param response the rule response containing the rule name
     * @return the entitlement display name, or the full rule name if no prefix
     */
    private String resolveEntitlementName(RuleResponse response) {
        if (response.getName() != null && response.getName().startsWith(AppConstants.AUDIT_RULE_NAME_PREFIX)) {
            return response.getName().substring(AppConstants.AUDIT_RULE_NAME_PREFIX.length());
        }
        return response.getName();
    }
}