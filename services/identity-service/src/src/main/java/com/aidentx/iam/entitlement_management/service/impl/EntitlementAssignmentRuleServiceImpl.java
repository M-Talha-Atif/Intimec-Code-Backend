package com.aidentx.iam.entitlement_management.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;
import com.aidentx.iam.entitlement_management.event.RuleEvaluationEvent;
import com.aidentx.iam.entitlement_management.model.dto.GlobalScheduleConfigResponse;
import com.aidentx.iam.entitlement_management.model.dto.GlobalScheduleUpdateRequest;
import com.aidentx.iam.entitlement_management.model.dto.GlobalScheduleUpdateResponse;
import com.aidentx.iam.entitlement_management.model.dto.RuleConditionDto;
import com.aidentx.iam.entitlement_management.model.dto.RuleConditionGroupDto;
import com.aidentx.iam.entitlement_management.model.dto.RuleCreateRequest;
import com.aidentx.iam.entitlement_management.model.dto.RuleEvaluationResult;
import com.aidentx.iam.entitlement_management.model.dto.RuleResponse;
import com.aidentx.iam.entitlement_management.model.dto.RuleUpdateRequest;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignmentRule;
import com.aidentx.iam.entitlement_management.model.entity.GlobalScheduleConfig;
import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionHistory;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRuleRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.entitlement_management.repository.GlobalScheduleConfigRepository;
import com.aidentx.iam.entitlement_management.service.EntitlementAssignmentRuleNormalizationService;
import com.aidentx.iam.entitlement_management.service.EntitlementAssignmentRuleService;
import com.aidentx.iam.entitlement_management.service.IdentityFieldRegistry;
import com.aidentx.iam.entitlement_management.service.NextExecutionCalculator;
import com.aidentx.iam.entitlement_management.service.RuleEvaluationEngine;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing dynamic entitlement assignment rules.
 *
 * <p>Handles CRUD operations for assignment rules with:
 * <ul>
 *   <li>Grouped conditions with per-group AND/OR operator and negation</li>
 *   <li>Backward compatibility with legacy flat condition format</li>
 *   <li>Three schedule types: IMMEDIATE, CRON, MANUAL</li>
 *   <li>Auto-evaluation for IMMEDIATE rules via Spring application events</li>
 *   <li>CRON schedule calculation via {@link NextExecutionCalculator}</li>
 *   <li>Global schedule updates affecting all enabled rules</li>
 * </ul>
 *
 * <p>Evaluation is delegated to {@link RuleEvaluationEngine} which owns the
 * full pipeline: transaction management, execution history collection,
 * history persistence, and audit event publishing. This service never
 * duplicates that logic.
 *
 * <p>Condition format (canonical grouped):
 * <pre>
 * [
 *   {
 *     "groupOperator": "AND",
 *     "negated": false,
 *     "conditions": [
 *       {"field": "department", "operator": "EQUALS", "value": "Engineering"},
 *       {"field": "jobTitle", "operator": "STARTS_WITH", "value": "Senior"}
 *     ]
 *   }
 * ]
 * </pre>
 */
@Service
public class EntitlementAssignmentRuleServiceImpl implements EntitlementAssignmentRuleService {

    private final EntitlementAssignmentRuleRepository ruleRepository;
    private final EntitlementRepository entitlementRepository;
    private final IdentityFieldRegistry fieldRegistry;
    private final NextExecutionCalculator executionCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final RuleEvaluationEngine evaluationEngine;
    private final EntitlementAssignmentRuleNormalizationService normalizationService;
    private final IUnifiedLogger logger;
    private final EntityManager entityManager;
    private final GlobalScheduleConfigRepository globalScheduleConfigRepository;

    @Autowired
    public EntitlementAssignmentRuleServiceImpl(EntitlementAssignmentRuleRepository ruleRepository,
                                                EntitlementRepository entitlementRepository,
                                                IdentityFieldRegistry fieldRegistry,
                                                NextExecutionCalculator executionCalculator,
                                                ApplicationEventPublisher eventPublisher,
                                                RuleEvaluationEngine evaluationEngine,
                                                 EntitlementAssignmentRuleNormalizationService normalizationService,
                                                UnifiedLoggerFactory loggerFactory,
                                                GlobalScheduleConfigRepository globalScheduleConfigRepository,
                                                EntityManager entityManager) {
        this.ruleRepository = ruleRepository;
        this.entitlementRepository = entitlementRepository;
        this.fieldRegistry = fieldRegistry;
        this.executionCalculator = executionCalculator;
        this.eventPublisher = eventPublisher;
        this.evaluationEngine = evaluationEngine;
        this.normalizationService = normalizationService;
        this.entityManager = entityManager;
        this.globalScheduleConfigRepository = globalScheduleConfigRepository;
        this.logger = loggerFactory.getLogger(getClass(), RuleEvaluationConstants.LOGGER_MODULE_NAME);
    }

    // ===================================================================
    //  Create Rule
    // ===================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new assignment rule with grouped conditions.
     *
     * <p>Accepts both grouped ({@code conditionGroups}) and legacy flat
     * ({@code conditions}) formats. If the legacy format is provided,
     * it is wrapped into a single group with AND operator and no negation.
     *
     * <p>After creation:
     * <ul>
     *   <li>IMMEDIATE rules: publishes a {@link RuleEvaluationEvent} for
     *       auto-evaluation after the transaction commits</li>
     *   <li>CRON rules: calculates and sets {@code next_execution_at}</li>
     *   <li>MANUAL rules: no auto-evaluation, no scheduled execution</li>
     * </ul>
     */
    // ===================================================================
    //  Create Rule (replace entire method)
    // ===================================================================
    @Override
    @Transactional
    public RuleResponse createRule(String tenantId, RuleCreateRequest request, String createdBy) {
        validateTenantId(tenantId);

        Entitlement entitlement = entitlementRepository.findById(request.getEntitlementId())
                .orElseThrow(() -> new IllegalArgumentException(
                        RuleEvaluationConstants.ERROR_ENTITLEMENT_NOT_FOUND + ": " + request.getEntitlementId()));

        // Validate no duplicate rule for this entitlement
        ruleRepository.findByTenantIdAndEntitlementId(tenantId, request.getEntitlementId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            RuleEvaluationConstants.ERROR_RULE_ALREADY_EXISTS + ": " + request.getEntitlementId());
                });

        // ---------------------------------------------------------------
        // Resolve schedule: inherit from the global schedule config, else default to Manual
        // ---------------------------------------------------------------
        EntitlementAssignmentRule.ScheduleType scheduleType;
        String cronExpression;
        String scheduleTime;
        String scheduleDate;
        String scheduleTimezone;

        // No schedule in request — inherit from global config
        Optional<GlobalScheduleConfig> globalConfig =
                globalScheduleConfigRepository.findByTenantId(tenantId);

        if (globalConfig.isPresent()) {
            GlobalScheduleConfig gc = globalConfig.get();
            scheduleType = parseScheduleType(gc.getScheduleType());
            cronExpression = gc.getCronExpression();
            scheduleTime = null;   // global config doesn't store scheduleTime
            scheduleDate = null;   // global config doesn't store scheduleDate
            scheduleTimezone = gc.getScheduleTimezone();

            logger.info()
                    .message("New rule inheriting global schedule config")
                    .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(RuleEvaluationConstants.DETAIL_SCHEDULE_TYPE, scheduleType.name())
                    .detail(RuleEvaluationConstants.DETAIL_CRON_EXPRESSION,
                            cronExpression != null ? cronExpression : "none")
                    .log();
        } else {
            // No global config exists — default to Manual
            scheduleType = EntitlementAssignmentRule.ScheduleType.MANUAL;
            cronExpression = null;
            scheduleTime = null;
            scheduleDate = null;
            scheduleTimezone = null;
        }


        // Validate cron schedule if applicable
        validateCronSchedule(scheduleType, cronExpression,
                scheduleTime, scheduleDate, scheduleTimezone);

        List<String> identityTypes = normalizationService.normalizeIdentityTypes(request.getIdentityTypes(), true);

        // Resolve and normalize conditions (grouped or legacy)
        List<Map<String, Object>> conditionGroupMaps = resolveAndNormalizeConditionGroups(
                request.getConditionGroups(), request.getConditions(), identityTypes);

        // Build entity
        long now = Instant.now().toEpochMilli();
        EntitlementAssignmentRule rule = new EntitlementAssignmentRule();
        rule.setTenantId(tenantId);
        rule.setName("Dynamic Rule - " + entitlement.getDisplayName());
        rule.setEntitlementId(request.getEntitlementId());
        rule.setResourceId(entitlement.getResourceId());
        rule.setConditions(conditionGroupMaps);
        rule.setEnabled(request.isEnabled());
        rule.setScheduleType(scheduleType);
        rule.setIdentityTypes(identityTypes);
        rule.setCreatedBy(createdBy);
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);

        // Apply cron fields
        applyCronFields(rule, scheduleType, cronExpression,
                scheduleTime, scheduleDate, scheduleTimezone);

        EntitlementAssignmentRule saved = ruleRepository.save(rule);

        // Calculate next execution for CRON rules
        calculateNextExecutionIfCron(saved);

           // Always publish evaluation event, evaluated post-commit regardless of schedule type
        eventPublisher.publishEvent(new RuleEvaluationEvent(
                tenantId,
                saved.getId(),
                  RuleExecutionHistory.TriggerType.ENTITLEMENT_RULE_CREATED,
                createdBy,
                saved.getScheduleType().name()
        ));

        logger.info()
                .message("Assignment rule created")
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, saved.getId().toString())
                .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, saved.getName())
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .detail(RuleEvaluationConstants.DETAIL_SCHEDULE_TYPE, scheduleType.name())
                .detail(RuleEvaluationConstants.DETAIL_GROUP_COUNT,
                        String.valueOf(conditionGroupMaps.size()))
                .log();

        return toResponse(saved);
    }

    // ===================================================================
    //  Update Rule
    // ===================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Updates an existing assignment rule. Only non-null fields in the
     * request are applied.
     *
     * <p>If conditions are provided (grouped or legacy format), they replace
     * all existing conditions. Legacy conditions are auto-migrated to
     * grouped format.
     *
     * <p>After update:
     * <ul>
     *   <li>IMMEDIATE rules: publishes evaluation event</li>
     *   <li>CRON rules: recalculates {@code next_execution_at}</li>
     *   <li>MANUAL rules: no auto-evaluation</li>
     * </ul>
     */
    @Override
    @Transactional
    public RuleResponse updateRule(String tenantId, UUID ruleId, RuleUpdateRequest request, String updatedBy) {
        validateTenantId(tenantId);

        EntitlementAssignmentRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        RuleEvaluationConstants.ERROR_RULE_NOT_FOUND + ": " + ruleId));

        // Update schedule type if provided
        if (request.getScheduleType() != null) {
            EntitlementAssignmentRule.ScheduleType newScheduleType = parseScheduleType(request.getScheduleType());
            validateCronSchedule(newScheduleType, request.getCronExpression(),
                    request.getScheduleTime(), request.getScheduleDate(), request.getScheduleTimezone());
            rule.setScheduleType(newScheduleType);
            applyCronFields(rule, newScheduleType, request.getCronExpression(),
                    request.getScheduleTime(), request.getScheduleDate(), request.getScheduleTimezone());
        }

        // Update enabled if provided
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }

        List<String> identityTypes = normalizationService.normalizeIdentityTypes(rule.getIdentityTypes(), true);

        // Update identity types if provided
        if (request.getIdentityTypes() != null) {
            identityTypes = normalizationService.normalizeIdentityTypes(request.getIdentityTypes(), false);
            rule.setIdentityTypes(identityTypes);
        }

        // Update conditions if provided (grouped or legacy)
        if (request.getConditionGroups() != null || request.getConditions() != null) {
            if (request.getConditionGroups() != null && !request.getConditionGroups().isEmpty()) {
                rule.setConditions(normalizationService.normalizeConditionGroups(request.getConditionGroups(), identityTypes));
            } else if (request.getConditions() != null && !request.getConditions().isEmpty()) {
                rule.setConditions(normalizationService.wrapLegacyConditions(request.getConditions(), identityTypes));
            }
        }

        rule.setUpdatedAt(Instant.now().toEpochMilli());

        // Recalculate next execution
        recalculateNextExecution(rule);

        EntitlementAssignmentRule saved = ruleRepository.save(rule);

          // Always publish evaluation event, evaluated post-commit regardless of schedule type
        eventPublisher.publishEvent(new RuleEvaluationEvent(
                tenantId,
                saved.getId(),
                RuleExecutionHistory.TriggerType.ENTITLEMENT_RULE_UPDATED,
                updatedBy,
                  saved.getScheduleType().name()
        ));


        logger.info()
                .message("Assignment rule updated")
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, saved.getId().toString())
                .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, saved.getName())
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .detail(RuleEvaluationConstants.DETAIL_SCHEDULE_TYPE, saved.getScheduleType().name())
                .log();


        return toResponse(saved);
    }

    // ===================================================================
    //  Delete Rule
    // ===================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Deletes the rule and all dynamic assignments created by it.
     * The deletion of assignments is handled by the repository
     * cascade or a custom query.
     */
    @Override
    @Transactional
    public void deleteRule(String tenantId, UUID ruleId) {
        validateTenantId(tenantId);

        EntitlementAssignmentRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        RuleEvaluationConstants.ERROR_RULE_NOT_FOUND + ": " + ruleId));

        logger.info()
                .message("Deleting assignment rule")
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, ruleId.toString())
                .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        ruleRepository.delete(rule);

        logger.info()
                .message("Assignment rule deleted")
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, ruleId.toString())
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .log();
    }

    // ===================================================================
    //  Get Rule
    // ===================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public RuleResponse getRule(String tenantId, UUID ruleId) {
        validateTenantId(tenantId);

        EntitlementAssignmentRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        RuleEvaluationConstants.ERROR_RULE_NOT_FOUND + ": " + ruleId));

        return toResponse(rule);
    }

    // ===================================================================
    //  List Rules (Paginated)
    // ===================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RuleResponse> listRules(String tenantId, int page, int size, String sort) {
        validateTenantId(tenantId);

        Pageable pageable;
        if (sort != null && !sort.isBlank()) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort));
        } else {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Page<EntitlementAssignmentRule> rules = ruleRepository.findByTenantId(tenantId, pageable);
        return rules.map(this::toResponse);
    }

    // ===================================================================
    //  Get Rule by Entitlement
    // ===================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<RuleResponse> getRuleByEntitlement(String tenantId, UUID entitlementId) {
        validateTenantId(tenantId);

        return ruleRepository.findByTenantIdAndEntitlementId(tenantId, entitlementId)
                .map(this::toResponse);
    }

    // ===================================================================
    //  Evaluate Single Rule — delegates to engine
    // ===================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link RuleEvaluationEngine#evaluateRuleById(String, UUID, String)}
     * which handles the full pipeline: transaction management, execution history
     * collection, history persistence, and audit event publishing.
     *
     * <p>The engine uses MANUAL trigger type when called from the API.
     */
    @Override
    public RuleEvaluationResult evaluateRule(String tenantId, UUID ruleId, String username) {
        validateTenantId(tenantId);

        // Verify rule exists before delegating (fail-fast with clear error)
        ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        RuleEvaluationConstants.ERROR_RULE_NOT_FOUND + ": " + ruleId));

        logger.info()
                .message("Manual evaluation requested via service")
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, ruleId.toString())
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        return evaluationEngine.evaluateRuleById(tenantId, ruleId, username);
    }

    // ===================================================================
    //  Evaluate All Rules — delegates to engine
    // ===================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link RuleEvaluationEngine#evaluateAllRules(String, String)}
     * which evaluates all enabled rules with per-rule transaction isolation,
     * execution history persistence, and audit event publishing.
     */
    @Override
    public List<RuleEvaluationResult> evaluateAllRules(String tenantId, String username) {
        validateTenantId(tenantId);

        logger.info()
                .message("Evaluate-all requested via service")
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        return evaluationEngine.evaluateAllRules(tenantId, username);
    }

    // ===================================================================
    //  Global Schedule Update
    // ===================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Updates the schedule type and cron configuration of all enabled rules
     * for a tenant in one operation.
     *
     * <p>When switching to IMMEDIATE:
     * <ul>
     *   <li>Cron fields are cleared on all rules</li>
     *   <li>{@code next_execution_at} is set to null for all rules</li>
     *   <li>Rules will auto-evaluate on next create/update</li>
     * </ul>
     *
     * <p>When switching to CRON:
     * <ul>
     *   <li>The provided cron configuration is applied to all enabled rules</li>
     *   <li>{@code next_execution_at} is recalculated for each rule</li>
     * </ul>
     *
     * <p>When switching to MANUAL:
     * <ul>
     *   <li>Cron fields are cleared on all rules</li>
     *   <li>{@code next_execution_at} is set to null for all rules</li>
     *   <li>Rules will only evaluate via explicit API call</li>
     * </ul>
     */
    @Override
    @Transactional
    public GlobalScheduleUpdateResponse updateGlobalScheduleType(String tenantId,
                                                                 GlobalScheduleUpdateRequest request) {
        validateTenantId(tenantId);

        EntitlementAssignmentRule.ScheduleType parsed = parseScheduleType(request.getScheduleType());

        String cronExpression = null;
        String scheduleTime = null;
        String scheduleDate = null;
        String scheduleTimezone = null;

        if (parsed == EntitlementAssignmentRule.ScheduleType.CRON) {
            validateCronSchedule(parsed, request.getCronExpression(), request.getScheduleTime(),
                    request.getScheduleDate(), request.getScheduleTimezone());
            cronExpression = request.getCronExpression();
            scheduleTime = request.getScheduleTime();
            scheduleDate = request.getScheduleDate();
            scheduleTimezone = request.getScheduleTimezone() != null
                    ? request.getScheduleTimezone() : RuleEvaluationConstants.DEFAULT_TIMEZONE;
        }

        long now = Instant.now().toEpochMilli();

        // ---------------------------------------------------------------
        // Persist the global schedule config (upsert)
        // ---------------------------------------------------------------
        GlobalScheduleConfig config = globalScheduleConfigRepository.findByTenantId(tenantId)
                .orElse(new GlobalScheduleConfig());
        config.setTenantId(tenantId);
        config.setScheduleType(parsed.name());
        config.setCronExpression(cronExpression);
        config.setScheduleTimezone(scheduleTimezone);
        config.setUpdatedAt(now);
        globalScheduleConfigRepository.save(config);

        logger.info()
                .message("Global schedule config persisted")
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .detail(RuleEvaluationConstants.DETAIL_SCHEDULE_TYPE, parsed.name())
                .detail(RuleEvaluationConstants.DETAIL_CRON_EXPRESSION,
                        cronExpression != null ? cronExpression : "none")
                .log();

        // ---------------------------------------------------------------
        // Bulk update all existing enabled rules
        // ---------------------------------------------------------------
        int updated = ruleRepository.updateGlobalScheduleForAllEnabled(
                tenantId, parsed, cronExpression, scheduleTime, scheduleDate, scheduleTimezone, now);

        recalculateAllNextExecutions(tenantId, parsed.name());

        logger.info()
                .message("Global schedule updated for all enabled rules")
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .detail(RuleEvaluationConstants.DETAIL_SCHEDULE_TYPE, parsed.name())
                .detail(RuleEvaluationConstants.DETAIL_CRON_EXPRESSION,
                        cronExpression != null ? cronExpression : "none")
                .detail(RuleEvaluationConstants.DETAIL_RULES_UPDATED, String.valueOf(updated))
                .log();

        return new GlobalScheduleUpdateResponse(
                parsed.name(), cronExpression, scheduleTime, scheduleDate, scheduleTimezone, updated);
    }

    // ===================================================================
    //  Get Global Schedule Config
    // ===================================================================

    /**
     * Returns the current global schedule config for a tenant.
     * Returns a default IMMEDIATE config if none has been saved yet.
     *
     * @param tenantId the tenant identifier
     * @return the global schedule config response
     */
    @Override
    @Transactional(readOnly = true)
    public GlobalScheduleConfigResponse getGlobalScheduleConfig(String tenantId) {
        validateTenantId(tenantId);

        return globalScheduleConfigRepository.findByTenantId(tenantId)
                .map(config -> new GlobalScheduleConfigResponse(
                        config.getScheduleType(),
                        config.getCronExpression(),
                        config.getScheduleTimezone(),
                        config.getUpdatedAt()))
                .orElse(new GlobalScheduleConfigResponse(
                        "IMMEDIATE", null, null, null));
    }

    // ===================================================================
    //  Condition Resolution — Grouped Format
    // ===================================================================

    /**
     * Resolves condition groups from the request, supporting both grouped
     * and legacy flat formats.
     *
     * <p>Priority:
     * <ol>
     *   <li>If {@code conditionGroups} is non-null and non-empty → use it</li>
     *   <li>Else if {@code conditions} (flat) is non-null and non-empty → wrap in single group</li>
     *   <li>Else → throw validation error</li>
     * </ol>
     *
     * <p>The legacy flat format is wrapped in a single group with
     * {@code groupOperator=AND} and {@code negated=false}, preserving
     * backward compatibility with existing API consumers.
     *
     * @param conditionGroups the grouped conditions (preferred)
     * @param conditions      the flat conditions (legacy)
     * @param identityTypes   the normalized rule identity types
     * @return normalized condition group maps ready for JSONB storage
     */
    private List<Map<String, Object>> resolveAndNormalizeConditionGroups(
            List<RuleConditionGroupDto> conditionGroups,
            List<RuleConditionDto> conditions,
            List<String> identityTypes) {

        if (conditionGroups != null && !conditionGroups.isEmpty()) {
            return normalizationService.normalizeConditionGroups(conditionGroups, identityTypes);
        }

        if (conditions != null && !conditions.isEmpty()) {
            return normalizationService.wrapLegacyConditions(conditions, identityTypes);
        }

        throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_CONDITION_GROUPS_EMPTY);
    }

    /**
     * Wraps legacy flat conditions into a single condition group.
     *
     * <p>Creates one group with {@code groupOperator=AND}, {@code interGroupOperator=AND},
     * {@code negated=false}, empty {@code conditionGroups}, and all conditions from the flat list.
     *
     * @param conditions    the flat condition DTOs
     * @param identityClass the JPA entity class for field resolution
     * @return single-element list containing the wrapper group as JSONB map
     */
    private List<Map<String, Object>> wrapLegacyConditions(
            List<RuleConditionDto> conditions,
            Class<? extends Identity> identityClass) {

        List<Map<String, Object>> conditionMaps = conditions.stream()
                .map(dto -> toConditionMap(dto, identityClass))
                .collect(Collectors.toList());

        Map<String, Object> group = new LinkedHashMap<>();
        group.put("groupOperator", RuleEvaluationConstants.DEFAULT_GROUP_OPERATOR);
        group.put("interGroupOperator", RuleEvaluationConstants.DEFAULT_GROUP_OPERATOR);
        group.put("negated", false);
        group.put("conditions", conditionMaps);
        group.put("conditionGroups", List.of());

        return List.of(group);
    }

    /**
     * Converts condition group DTOs to JSONB-compatible maps with normalization.
     * Supports recursive nesting of sub-groups.
     *
     * <p>Validations applied per group:
     * <ul>
     *   <li>Max {@value RuleEvaluationConstants#MAX_CONDITION_GROUPS} top-level groups</li>
     *   <li>Max {@value RuleEvaluationConstants#MAX_CONDITIONS_PER_GROUP} conditions per group</li>
     *   <li>Max {@value RuleEvaluationConstants#MAX_GROUP_DEPTH} nesting depth</li>
     *   <li>Each group must have at least one condition or one sub-group</li>
     *   <li>{@code groupOperator} normalized to AND/OR (default AND)</li>
     *   <li>{@code interGroupOperator} normalized to AND/OR (default AND)</li>
     *   <li>{@code negated} defaults to false</li>
     * </ul>
     *
     * @param groups        the condition group DTOs from the request
     * @param identityClass the JPA entity class (for field name resolution)
     * @return list of normalized condition group maps ready for JSONB storage
     */
    private List<Map<String, Object>> toConditionGroupMaps(
            List<RuleConditionGroupDto> groups,
            Class<? extends Identity> identityClass) {

        if (groups.size() > RuleEvaluationConstants.MAX_CONDITION_GROUPS) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_TOO_MANY_GROUPS);
        }

        return groups.stream()
                .map(group -> toConditionGroupMap(group, identityClass, 1))
                .collect(Collectors.toList());
    }

    /**
     * Recursively converts a single condition group DTO to a JSONB-compatible map.
     *
     * @param group         the condition group DTO
     * @param identityClass the JPA entity class for field resolution
     * @param currentDepth  the current nesting depth (1 = top level)
     * @return normalized condition group map
     */
    private Map<String, Object> toConditionGroupMap(
            RuleConditionGroupDto group,
            Class<? extends Identity> identityClass,
            int currentDepth) {

        // Validate nesting depth
        if (currentDepth > RuleEvaluationConstants.MAX_GROUP_DEPTH) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_GROUP_DEPTH_EXCEEDED);
        }

        boolean hasConditions = group.getConditions() != null && !group.getConditions().isEmpty();
        boolean hasSubGroups = group.getConditionGroups() != null && !group.getConditionGroups().isEmpty();

        // Must have at least one condition or one sub-group
        if (!hasConditions && !hasSubGroups) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_GROUP_EMPTY);
        }

        // Validate condition count
        if (hasConditions && group.getConditions().size() > RuleEvaluationConstants.MAX_CONDITIONS_PER_GROUP) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_TOO_MANY_CONDITIONS_IN_GROUP);
        }

        // Normalize operators
        String groupOperator = normalizeGroupOperator(group.getGroupOperator());
        String interGroupOperator = normalizeGroupOperator(group.getInterGroupOperator());

        // Normalize negated
        boolean negated = group.getNegated() != null && group.getNegated();

        Map<String, Object> groupMap = new LinkedHashMap<>();
        groupMap.put("groupOperator", groupOperator);
        groupMap.put("interGroupOperator", interGroupOperator);
        groupMap.put("negated", negated);

        // Normalize leaf conditions
        if (hasConditions) {
            List<Map<String, Object>> conditionMaps = group.getConditions().stream()
                    .map(dto -> toConditionMap(dto, identityClass))
                    .collect(Collectors.toList());
            groupMap.put("conditions", conditionMaps);
        } else {
            groupMap.put("conditions", List.of());
        }

        // Recursively normalize sub-groups
        if (hasSubGroups) {
            List<Map<String, Object>> subGroupMaps = group.getConditionGroups().stream()
                    .map(subGroup -> toConditionGroupMap(subGroup, identityClass, currentDepth + 1))
                    .collect(Collectors.toList());
            groupMap.put("conditionGroups", subGroupMaps);
        } else {
            groupMap.put("conditionGroups", List.of());
        }

        return groupMap;
    }

    /**
     * Converts a single condition DTO to a JSONB-compatible map.
     *
     * <p>Resolves field name to the exact JPA attribute name.
     * Normalizes operator to uppercase.
     *
     * @param dto           the condition DTO
     * @param identityClass the JPA entity class for field resolution
     * @return normalized condition map
     */
    private Map<String, Object> toConditionMap(RuleConditionDto dto,
                                               Class<? extends Identity> identityClass) {
        Map<String, Object> map = new LinkedHashMap<>();

        String resolvedField = fieldRegistry.resolveField(dto.getField(), identityClass);
        map.put("field", resolvedField);
        map.put("operator", dto.getOperator().toUpperCase());

        if (dto.getValue() != null) map.put("value", dto.getValue());
        if (dto.getValues() != null) map.put("values", dto.getValues());

        return map;
    }

    /**
     * Normalizes and validates a group operator.
     * Defaults to AND if null or blank. Rejects anything other than AND/OR.
     *
     * @param groupOperator the raw group operator string
     * @return normalized uppercase group operator ("AND" or "OR")
     * @throws IllegalArgumentException if the operator is not AND or OR
     */
    private String normalizeGroupOperator(String groupOperator) {
        if (groupOperator == null || groupOperator.isBlank()) {
            return RuleEvaluationConstants.DEFAULT_GROUP_OPERATOR;
        }
        String normalized = groupOperator.trim().toUpperCase();
        if (!"AND".equals(normalized) && !"OR".equals(normalized)) {
            throw new IllegalArgumentException(
                    RuleEvaluationConstants.ERROR_INVALID_GROUP_OPERATOR + ". Got: " + groupOperator);
        }
        return normalized;
    }

    // ===================================================================
    //  Schedule Helpers
    // ===================================================================

    /**
     * Parses schedule type string to enum.
     * Defaults to IMMEDIATE if null or blank.
     *
     * @param type the schedule type string
     * @return the parsed enum value
     * @throws IllegalArgumentException if the type is not IMMEDIATE, CRON, or MANUAL
     */
    private EntitlementAssignmentRule.ScheduleType parseScheduleType(String type) {
        if (type == null || type.isBlank()) {
            return EntitlementAssignmentRule.ScheduleType.IMMEDIATE;
        }
        String upper = type.trim().toUpperCase();
        try {
            return EntitlementAssignmentRule.ScheduleType.valueOf(upper);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    RuleEvaluationConstants.ERROR_INVALID_SCHEDULE_TYPE + ". Got: " + type
                            + ". Allowed: IMMEDIATE, CRON, MANUAL");
        }
    }

    /**
     * Validates cron schedule configuration.
     *
     * <p>For IMMEDIATE: no cron fields required, validation is a no-op.
     * <p>For MANUAL: no cron fields required, validation is a no-op.
     * <p>For CRON: must provide either {@code cronExpression} or {@code scheduleTime}.
     *
     * @param scheduleType   the parsed schedule type
     * @param cronExpression the cron expression (optional if scheduleTime is provided)
     * @param scheduleTime   the schedule time in HH:mm format
     * @param scheduleDate   the schedule date in yyyy-MM-dd format (optional)
     * @param timezone       the IANA timezone identifier (optional, defaults to UTC)
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCronSchedule(EntitlementAssignmentRule.ScheduleType scheduleType,
                                      String cronExpression, String scheduleTime,
                                      String scheduleDate, String timezone) {
        if (scheduleType == EntitlementAssignmentRule.ScheduleType.IMMEDIATE
                || scheduleType == EntitlementAssignmentRule.ScheduleType.MANUAL) {
            return;
        }

        boolean hasCron = cronExpression != null && !cronExpression.isBlank();
        boolean hasTime = scheduleTime != null && !scheduleTime.isBlank();

        if (!hasCron && !hasTime) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_CRON_SCHEDULE_REQUIRED);
        }

        if (hasCron) {
            validateCronExpression(cronExpression);
        }
        if (hasTime) {
            validateScheduleTime(scheduleTime);
        }
        if (scheduleDate != null && !scheduleDate.isBlank()) {
            validateScheduleDate(scheduleDate);
        }
        if (timezone != null && !timezone.isBlank()) {
            validateTimezone(timezone);
        }
    }

    /**
     * Validates a Spring 6-field cron expression.
     *
     * @param cronExpression the cron expression to validate
     * @throws IllegalArgumentException if the expression is invalid
     */
    private void validateCronExpression(String cronExpression) {
        try {
            org.springframework.scheduling.support.CronExpression.parse(cronExpression);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    RuleEvaluationConstants.ERROR_INVALID_CRON_EXPRESSION + ": " + e.getMessage());
        }
    }

    /**
     * Validates schedule time format (HH:mm).
     *
     * @param scheduleTime the time string
     * @throws IllegalArgumentException if format is invalid
     */
    private void validateScheduleTime(String scheduleTime) {
        if (!scheduleTime.matches(RuleEvaluationConstants.SCHEDULE_TIME_PATTERN)) {
            throw new IllegalArgumentException(
                    RuleEvaluationConstants.ERROR_INVALID_SCHEDULE_TIME + ": " + scheduleTime);
        }
    }

    /**
     * Validates schedule date format (yyyy-MM-dd).
     *
     * @param scheduleDate the date string
     * @throws IllegalArgumentException if format is invalid
     */
    private void validateScheduleDate(String scheduleDate) {
        try {
            java.time.LocalDate.parse(scheduleDate);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(
                    RuleEvaluationConstants.ERROR_INVALID_SCHEDULE_DATE + ": " + scheduleDate);
        }
    }

    /**
     * Validates an IANA timezone identifier.
     *
     * @param timezone the timezone string
     * @throws IllegalArgumentException if the timezone is invalid
     */
    private void validateTimezone(String timezone) {
        if (!java.time.ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new IllegalArgumentException(
                    RuleEvaluationConstants.ERROR_INVALID_TIMEZONE + ": " + timezone);
        }
    }

    /**
     * Applies cron fields to the rule entity.
     *
     * <p>For CRON: sets provided values, defaults timezone to UTC if not specified.
     * <p>For IMMEDIATE and MANUAL: clears all cron fields and {@code next_execution_at}.
     *
     * @param rule           the rule entity to update
     * @param scheduleType   the schedule type
     * @param cronExpression the cron expression
     * @param scheduleTime   the schedule time
     * @param scheduleDate   the schedule date
     * @param timezone       the timezone
     */
    private void applyCronFields(EntitlementAssignmentRule rule,
                                 EntitlementAssignmentRule.ScheduleType scheduleType,
                                 String cronExpression, String scheduleTime,
                                 String scheduleDate, String timezone) {
        if (scheduleType == EntitlementAssignmentRule.ScheduleType.CRON) {
            rule.setCronExpression(cronExpression);
            rule.setScheduleTime(scheduleTime);
            rule.setScheduleDate(scheduleDate);
            rule.setScheduleTimezone(timezone != null && !timezone.isBlank()
                    ? timezone : RuleEvaluationConstants.DEFAULT_TIMEZONE);
        } else {
            // IMMEDIATE and MANUAL — no cron fields, no scheduled execution
            rule.setCronExpression(null);
            rule.setScheduleTime(null);
            rule.setScheduleDate(null);
            rule.setScheduleTimezone(null);
            rule.setNextExecutionAt(null);
        }
    }

    /**
     * Calculates and sets {@code next_execution_at} for a newly created CRON rule.
     *
     * <p>Only applies when the rule is CRON and enabled. For IMMEDIATE, MANUAL,
     * or disabled rules, this is a no-op.
     *
     * @param rule the newly saved rule entity
     */
    private void calculateNextExecutionIfCron(EntitlementAssignmentRule rule) {
        if (rule.getScheduleType() != EntitlementAssignmentRule.ScheduleType.CRON || !rule.isEnabled()) {
            return;
        }

        Long nextExecution = executionCalculator.calculate(rule);
        rule.setNextExecutionAt(nextExecution);
        ruleRepository.save(rule);

        logger.info()
                .message("Set next_execution_at for new CRON rule")
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                .detail("nextExecutionAt", nextExecution != null
                        ? Instant.ofEpochMilli(nextExecution).toString() : "null")
                .log();
    }

    /**
     * Recalculates {@code next_execution_at} after a rule update.
     *
     * <p>Handles all transitions:
     * <ul>
     *   <li>CRON + enabled → calculate next execution</li>
     *   <li>CRON + disabled → clear next_execution_at</li>
     *   <li>IMMEDIATE (any) → clear next_execution_at</li>
     *   <li>MANUAL (any) → clear next_execution_at (never auto-evaluated)</li>
     * </ul>
     *
     * @param rule the updated rule entity
     */
    private void recalculateNextExecution(EntitlementAssignmentRule rule) {
        if (rule.getScheduleType() == EntitlementAssignmentRule.ScheduleType.CRON && rule.isEnabled()) {
            Long nextExecution = executionCalculator.calculate(rule);
            rule.setNextExecutionAt(nextExecution);

            logger.debug()
                    .message("Recalculated next_execution_at on rule update")
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail("nextExecutionAt", nextExecution != null
                            ? Instant.ofEpochMilli(nextExecution).toString() : "null")
                    .log();
        } else {
            // IMMEDIATE, MANUAL, or disabled → no scheduled execution
            rule.setNextExecutionAt(null);

            logger.debug()
                    .message("Cleared next_execution_at (IMMEDIATE, MANUAL, or disabled)")
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_SCHEDULE_TYPE,
                            rule.getScheduleType().name())
                    .log();
        }
    }

    /**
     * Recalculates {@code next_execution_at} for ALL enabled rules of a tenant
     * after a global schedule change.
     *
     * <p>Three scenarios:
     * <ul>
     *   <li><b>Switch to CRON:</b> Load each rule and recalculate its
     *       {@code next_execution_at} based on the new cron configuration.</li>
     *   <li><b>Switch to IMMEDIATE:</b> Bulk-set {@code next_execution_at = null}.</li>
     *   <li><b>Switch to MANUAL:</b> Bulk-set {@code next_execution_at = null}.</li>
     * </ul>
     *
     * @param tenantId        the tenant identifier
     * @param newScheduleType the new schedule type
     */
    private void recalculateAllNextExecutions(String tenantId, String newScheduleType) {
        if ("CRON".equalsIgnoreCase(newScheduleType)) {

            // CRITICAL: flush and clear the persistence context so findCronRules
            // loads FRESH entities with the NEW cron expression set by the bulk update.
            entityManager.flush();
            entityManager.clear();

            List<EntitlementAssignmentRule> cronRules = ruleRepository.findCronRules(tenantId);

            for (EntitlementAssignmentRule rule : cronRules) {
                Long nextExecution = executionCalculator.calculate(rule);
                rule.setNextExecutionAt(nextExecution);
                ruleRepository.save(rule);
            }

            logger.info()
                    .message("Recalculated next_execution_at for all CRON rules")
                    .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(RuleEvaluationConstants.DETAIL_RULE_COUNT, String.valueOf(cronRules.size()))
                    .log();
        } else {
            // IMMEDIATE or MANUAL — clear all next_execution_at
            ruleRepository.bulkUpdateNextExecutionAt(tenantId, null, Instant.now().toEpochMilli());

            logger.info()
                    .message("Cleared next_execution_at for all rules (switched to " + newScheduleType + ")")
                    .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(RuleEvaluationConstants.DETAIL_SCHEDULE_TYPE, newScheduleType)
                    .log();
        }
    }


    // ===================================================================
    //  Response Mapping
    // ===================================================================

    /**
     * Converts an {@link EntitlementAssignmentRule} entity to a {@link RuleResponse} DTO.
     *
     * <p>Provides both the grouped format ({@code conditionGroups}) and the
     * flattened format ({@code conditions}) for backward compatibility.
     *
     * <p>The flattened format extracts all conditions from all groups into
     * a single ordered list, losing group boundaries and negation info.
     * Clients that support grouping should use {@code conditionGroups}.
     *
     * @param rule the entity
     * @return the response DTO
     */
    private RuleResponse toResponse(EntitlementAssignmentRule rule) {
        RuleResponse response = new RuleResponse();
        response.setId(rule.getId());
        response.setTenantId(rule.getTenantId());
        response.setName(rule.getName());
        response.setEntitlementId(rule.getEntitlementId());
        response.setResourceId(rule.getResourceId());

        // Canonical grouped format
        response.setConditionGroups(rule.getConditions());

        // Flattened legacy format
        response.setConditions(flattenConditionGroups(rule.getConditions()));

        response.setEnabled(rule.isEnabled());
        response.setScheduleType(rule.getScheduleType().name());
        response.setCronExpression(rule.getCronExpression());
        response.setScheduleTime(rule.getScheduleTime());
        response.setScheduleDate(rule.getScheduleDate());
        response.setScheduleTimezone(rule.getScheduleTimezone());
        response.setLastEvaluatedAt(rule.getLastEvaluatedAt());
        response.setNextExecutionAt(rule.getNextExecutionAt());
        response.setPriority(rule.getPriority());
        response.setIdentityTypes(normalizationService.normalizeIdentityTypes(rule.getIdentityTypes(), true));
        response.setCreatedBy(rule.getCreatedBy());
        response.setCreatedAt(rule.getCreatedAt());
        response.setUpdatedAt(rule.getUpdatedAt());
        return response;
    }

    /**
     * Flattens condition groups into a single list for backward compatibility.
     *
     * <p>Extracts all conditions from all groups in order.
     * Group boundaries, group operators, and negation are lost.
     *
     * <p>Also handles legacy flat format (conditions that are already flat
     * and don't have "groupOperator" keys) by returning them as-is.
     *
     * @param conditionGroups the grouped conditions from the entity
     * @return flattened condition list, or empty list if null
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> flattenConditionGroups(List<Map<String, Object>> conditionGroups) {
        if (conditionGroups == null || conditionGroups.isEmpty()) {
            return List.of();
        }

        // If already flat (legacy format), return as-is
        if (conditionGroups.get(0).containsKey("field") && !conditionGroups.get(0).containsKey("groupOperator")) {
            return conditionGroups;
        }

        List<Map<String, Object>> flattened = new ArrayList<>();
        for (Map<String, Object> group : conditionGroups) {
            Object conditions = group.get("conditions");
            if (conditions instanceof List<?> condList) {
                for (Object cond : condList) {
                    if (cond instanceof Map<?, ?> condMap) {
                        flattened.add((Map<String, Object>) condMap);
                    }
                }
            }
        }
        return flattened;
    }

    // ===================================================================
    //  Validation Helpers
    // ===================================================================

    /**
     * Validates that the tenant ID is not null or blank.
     *
     * @param tenantId the tenant identifier
     * @throws IllegalArgumentException if tenantId is null or blank
     */
    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_TENANT_ID_REQUIRED);
        }
    }

    /**
     * Resolves the JPA entity class based on identity type string.
     *
     * @param identityType the identity type string (EMPLOYEE, NON_EMPLOYEE, NON_HUMAN)
     * @return the corresponding JPA entity class
     */
    private Class<? extends Identity> resolveIdentityClass(String identityType) {
        if (identityType == null || identityType.isBlank()) {
            return Employee.class;
        }
        return switch (identityType.toUpperCase()) {
            case "EMPLOYEE" -> Employee.class;
            case "NON_EMPLOYEE" -> NonEmployee.class;
            case "NON_HUMAN" -> NonHumanIdentity.class;
            default -> Employee.class;
        };
    }

}