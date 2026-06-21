package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.event.AccessRoleAssignmentRuleEvaluationEvent;
import com.aidentx.iam.access_roles_management.mapper.AccessRoleAssignmentRuleMapper;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionDto;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionGroupDto;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleGlobalScheduleUpdateRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleAssignmentRuleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.UpdateAccessRoleAssignmentRuleRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleGlobalScheduleConfigResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleGlobalScheduleUpdateResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleResponse;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRuleGlobalScheduleConfig;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleGlobalScheduleConfigRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleNormalizationService;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.iam.entitlement_management.model.enums.RuleOperator;
import com.aidentx.iam.entitlement_management.service.IdentityFieldRegistry;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementation for access-role assignment-rule CRUD.
 *
 * <p>This class mirrors the entitlement-rule CRUD model while targeting
 * {@link AccessRole} through {@code roleId}. It validates tenant isolation,
 * ensures one rule per role, normalizes grouped conditions, and prepares schedule
 * metadata for future evaluation workers.</p>
 */
@Service
public class AccessRoleAssignmentRuleServiceImpl implements AccessRoleAssignmentRuleService {

    private static final DateTimeFormatter SCHEDULE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter SCHEDULE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AccessRoleAssignmentRuleRepository ruleRepository;
    private final AccessRoleRepository accessRoleRepository;
    private final AccessRoleAssignmentRuleMapper mapper;
    private final IdentityFieldRegistry fieldRegistry;
    private final AccessRoleAssignmentRuleNormalizationService normalizationService;
    private final AccessRoleAssignmentRuleGlobalScheduleConfigRepository globalScheduleConfigRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final IUnifiedLogger logger;

    public AccessRoleAssignmentRuleServiceImpl(AccessRoleAssignmentRuleRepository ruleRepository,
                                               AccessRoleRepository accessRoleRepository,
                                               AccessRoleAssignmentRuleMapper mapper,
                                               IdentityFieldRegistry fieldRegistry,
                                               AccessRoleAssignmentRuleNormalizationService normalizationService,
                                               AccessRoleAssignmentRuleGlobalScheduleConfigRepository globalScheduleConfigRepository,
                                               ApplicationEventPublisher eventPublisher,
                                               UnifiedLoggerFactory unifiedLoggerFactory,
                                               EntityManager entityManager) {
        this.ruleRepository = ruleRepository;
        this.accessRoleRepository = accessRoleRepository;
        this.mapper = mapper;
        this.fieldRegistry = fieldRegistry;
        this.normalizationService = normalizationService;
        this.globalScheduleConfigRepository = globalScheduleConfigRepository;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.logger = unifiedLoggerFactory.getLogger(getClass(), AccessRoleAssignmentRuleConstants.LOG_CATEGORY);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public AccessRoleAssignmentRuleResponse createRule(String tenantId,
                                                       CreateAccessRoleAssignmentRuleRequest request,
                                                       String createdBy) {
        validateTenantId(tenantId);
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getRoleId() == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_ROLE_ID_REQUIRED);
        }

        AccessRole role = findTenantRoleOrThrow(tenantId, request.getRoleId());
        if (ruleRepository.existsByTenantIdAndRoleId(tenantId, request.getRoleId())) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_RULE_ALREADY_EXISTS + ": " + request.getRoleId());
        }

        List<String> identityTypes = normalizationService.normalizeIdentityTypes(request.getIdentityTypes(), true);
        List<Map<String, Object>> conditionGroups = normalizationService.normalizeConditionGroups(request.getConditionGroups(), identityTypes);
        AccessRoleAssignmentRule rule = new AccessRoleAssignmentRule();
        rule.setTenantId(tenantId);
        rule.setName(AccessRoleAssignmentRuleConstants.RULE_NAME_PREFIX + resolveRoleDisplayName(role));
        rule.setRoleId(role.getId());
        rule.setConditionGroups(conditionGroups);
        rule.setEnabled(request.isEnabled());
        applyGlobalSchedule(tenantId, rule);
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        rule.setIdentityTypes(identityTypes);
        rule.setCreatedBy(createdBy);
        rule.setCreatedAt(Instant.now().toEpochMilli());

        validateSchedule(rule);
        applyNextExecution(rule);

        AccessRoleAssignmentRule saved = ruleRepository.save(rule);
        publishRuleEvaluationEvent(saved, AccessRoleAssignmentExecution.TriggerType.ACCESS_ROLE_RULE_CREATED, createdBy);
        return mapper.toResponse(saved, role);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public AccessRoleAssignmentRuleResponse updateRule(String tenantId,
                                                       UUID ruleId,
                                                       UpdateAccessRoleAssignmentRuleRequest request,
                                                       String updatedBy) {
        validateTenantId(tenantId);
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        AccessRoleAssignmentRule rule = findRuleOrThrow(tenantId, ruleId);

        List<String> identityTypes = normalizationService.normalizeIdentityTypes(rule.getIdentityTypes(), true);
        if (request.getIdentityTypes() != null) {
            identityTypes = normalizationService.normalizeIdentityTypes(request.getIdentityTypes(), false);
            rule.setIdentityTypes(identityTypes);
        }

        if (request.getConditionGroups() != null) {
            rule.setConditionGroups(normalizationService.normalizeConditionGroups(request.getConditionGroups(), identityTypes));
        }
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }
        if (request.getPriority() != null) {
            rule.setPriority(request.getPriority());
        }

        applyGlobalSchedule(tenantId, rule);
        validateSchedule(rule);
        applyNextExecution(rule);

        AccessRoleAssignmentRule saved = ruleRepository.save(rule);
        publishRuleEvaluationEvent(saved, AccessRoleAssignmentExecution.TriggerType.ACCESS_ROLE_RULE_UPDATED, updatedBy);
        AccessRole role = findTenantRoleOrThrow(tenantId, saved.getRoleId());
        return mapper.toResponse(saved, role);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public AccessRoleAssignmentRuleResponse getRule(String tenantId, UUID ruleId) {
        validateTenantId(tenantId);
        AccessRoleAssignmentRule rule = findRuleOrThrow(tenantId, ruleId);
        AccessRole role = findTenantRoleOrThrow(tenantId, rule.getRoleId());
        return mapper.toResponse(rule, role);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Optional<AccessRoleAssignmentRuleResponse> getRuleByRole(String tenantId, UUID roleId) {
        validateTenantId(tenantId);
        AccessRole role = findTenantRoleOrThrow(tenantId, roleId);
        return ruleRepository.findByTenantIdAndRoleId(tenantId, roleId)
                .map(rule -> mapper.toResponse(rule, role));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<AccessRoleAssignmentRuleResponse> listRules(String tenantId, int page, int size, String sort) {
        validateTenantId(tenantId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizePageSize(size), buildSort(sort));
        Page<AccessRoleAssignmentRule> rules = ruleRepository.findByTenantId(tenantId, pageable);
        Map<UUID, AccessRole> rolesById = loadRolesById(rules.getContent());
        return rules.map(rule -> mapper.toResponse(rule, rolesById.get(rule.getRoleId())));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public AccessRoleAssignmentRuleGlobalScheduleUpdateResponse updateGlobalScheduleType(
            String tenantId,
            AccessRoleAssignmentRuleGlobalScheduleUpdateRequest request) {
        validateTenantId(tenantId);
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        AccessRoleAssignmentRule.ScheduleType scheduleType = parseScheduleType(request.getScheduleType());
        AccessRoleAssignmentRule template = new AccessRoleAssignmentRule();
        template.setTenantId(tenantId);
        template.setEnabled(true);
        template.setScheduleType(scheduleType);
        template.setCronExpression(trimToNull(request.getCronExpression()));
        template.setScheduleTime(trimToNull(request.getScheduleTime()));
        template.setScheduleDate(trimToNull(request.getScheduleDate()));
        template.setScheduleTimezone(trimToNull(request.getScheduleTimezone()));
        validateSchedule(template);

        long now = Instant.now().toEpochMilli();
        AccessRoleAssignmentRuleGlobalScheduleConfig config = globalScheduleConfigRepository.findByTenantId(tenantId)
                .orElse(new AccessRoleAssignmentRuleGlobalScheduleConfig());
        config.setTenantId(tenantId);
        config.setScheduleType(template.getScheduleType().name());
        config.setCronExpression(template.getCronExpression());
        config.setScheduleTime(template.getScheduleTime());
        config.setScheduleDate(template.getScheduleDate());
        config.setScheduleTimezone(template.getScheduleTimezone());
        config.setUpdatedAt(now);
        globalScheduleConfigRepository.save(config);

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_GLOBAL_SCHEDULE_PERSISTED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE, template.getScheduleType().name())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_CRON_EXPRESSION,
                        template.getCronExpression() != null ? template.getCronExpression() : "none")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TIME,
                        template.getScheduleTime() != null ? template.getScheduleTime() : "none")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_DATE,
                        template.getScheduleDate() != null ? template.getScheduleDate() : "none")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TIMEZONE,
                        template.getScheduleTimezone() != null ? template.getScheduleTimezone() : "none")
                .log();

        int updated = ruleRepository.updateGlobalScheduleForAllRules(
                tenantId,
                template.getScheduleType(),
                template.getCronExpression(),
                template.getScheduleTime(),
                template.getScheduleDate(),
                template.getScheduleTimezone(),
                now);

        String nextExecutionPreview = recalculateAllNextExecutions(tenantId, template.getScheduleType(), now);

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_GLOBAL_SCHEDULE_UPDATED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE, template.getScheduleType().name())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULES_UPDATED, String.valueOf(updated))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_NEXT_EXECUTION_PREVIEW,
                        nextExecutionPreview != null ? nextExecutionPreview : "none")
                .log();

        AccessRoleAssignmentRuleGlobalScheduleUpdateResponse response =
                new AccessRoleAssignmentRuleGlobalScheduleUpdateResponse(
                        template.getScheduleType().name(),
                        template.getCronExpression(),
                        template.getScheduleTime(),
                        template.getScheduleDate(),
                        template.getScheduleTimezone(),
                        updated);
        response.setNextExecutionPreview(nextExecutionPreview);
        return response;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public AccessRoleAssignmentRuleGlobalScheduleConfigResponse getGlobalScheduleConfig(String tenantId) {
        validateTenantId(tenantId);
        return globalScheduleConfigRepository.findByTenantId(tenantId)
                .map(config -> new AccessRoleAssignmentRuleGlobalScheduleConfigResponse(
                        config.getScheduleType(),
                        config.getCronExpression(),
                        config.getScheduleTime(),
                        config.getScheduleDate(),
                        config.getScheduleTimezone(),
                        config.getUpdatedAt()))
                .orElse(new AccessRoleAssignmentRuleGlobalScheduleConfigResponse(
                        AccessRoleAssignmentRule.ScheduleType.MANUAL.name(),
                        null,
                        null,
                        null,
                        null,
                        null));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteRule(String tenantId, UUID ruleId) {
        validateTenantId(tenantId);
        AccessRoleAssignmentRule rule = findRuleOrThrow(tenantId, ruleId);
        ruleRepository.delete(rule);
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_TENANT_REQUIRED);
        }
    }

    private AccessRoleAssignmentRule findRuleOrThrow(String tenantId, UUID ruleId) {
        if (ruleId == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_RULE_NOT_FOUND + ": null");
        }
        return ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_RULE_NOT_FOUND + ": " + ruleId));
    }

    private AccessRole findTenantRoleOrThrow(String tenantId, UUID roleId) {
        return accessRoleRepository.findById(roleId)
                .filter(role -> tenantId.equals(role.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_ROLE_NOT_FOUND + ": " + roleId));
    }

    private Map<UUID, AccessRole> loadRolesById(List<AccessRoleAssignmentRule> rules) {
        List<UUID> roleIds = rules.stream()
                .map(AccessRoleAssignmentRule::getRoleId)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return new HashMap<>();
        }
        return accessRoleRepository.findByIdIn(roleIds).stream()
                .collect(Collectors.toMap(AccessRole::getId, Function.identity()));
    }

    private List<Map<String, Object>> normalizeConditionGroups(List<AccessRoleAssignmentRuleConditionGroupDto> groups,
                                                               List<Class<? extends Identity>> identityClasses) {
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CONDITION_GROUPS_REQUIRED);
        }
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (AccessRoleAssignmentRuleConditionGroupDto group : groups) {
            normalized.add(normalizeConditionGroup(group, identityClasses));
        }
        return normalized;
    }

    private Map<String, Object> normalizeConditionGroup(AccessRoleAssignmentRuleConditionGroupDto group,
                                                        List<Class<? extends Identity>> identityClasses) {
        if (group == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CONDITION_GROUP_EMPTY);
        }

        List<Map<String, Object>> conditions = normalizeConditions(group.getConditions(), identityClasses);
        List<Map<String, Object>> subGroups = new ArrayList<>();
        if (group.getConditionGroups() != null) {
            for (AccessRoleAssignmentRuleConditionGroupDto subGroup : group.getConditionGroups()) {
                subGroups.add(normalizeConditionGroup(subGroup, identityClasses));
            }
        }

        if (conditions.isEmpty() && subGroups.isEmpty()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CONDITION_GROUP_EMPTY);
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("groupOperator", normalizeLogicalOperator(group.getGroupOperator(),
                AccessRoleAssignmentRuleConstants.DEFAULT_GROUP_OPERATOR));
        normalized.put("interGroupOperator", normalizeLogicalOperator(group.getInterGroupOperator(),
                AccessRoleAssignmentRuleConstants.DEFAULT_INTER_GROUP_OPERATOR));
        normalized.put("negated", Boolean.TRUE.equals(group.getNegated()));
        normalized.put("conditions", conditions);
        if (!subGroups.isEmpty()) {
            normalized.put("conditionGroups", subGroups);
        }
        return normalized;
    }

    private List<Map<String, Object>> normalizeConditions(List<AccessRoleAssignmentRuleConditionDto> conditionDtos,
                                                          List<Class<? extends Identity>> identityClasses) {
        if (conditionDtos == null || conditionDtos.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (AccessRoleAssignmentRuleConditionDto condition : conditionDtos) {
            normalized.add(normalizeCondition(condition, identityClasses));
        }
        return normalized;
    }

    private Map<String, Object> normalizeCondition(AccessRoleAssignmentRuleConditionDto condition,
                                                   List<Class<? extends Identity>> identityClasses) {
        if (condition == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CONDITION_GROUP_EMPTY);
        }
        if (condition.getField() == null || condition.getField().isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CONDITION_FIELD_REQUIRED);
        }
        if (condition.getOperator() == null || condition.getOperator().isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CONDITION_OPERATOR_REQUIRED);
        }

        String resolvedField = resolveFieldForIdentityTypes(condition.getField(), identityClasses);
        String operator = RuleOperator.fromString(condition.getOperator()).name();

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("field", resolvedField);
        normalized.put("operator", operator);
        if (condition.getValues() != null && !condition.getValues().isEmpty()) {
            normalized.put("values", condition.getValues());
        } else if (condition.getValue() != null) {
            normalized.put("value", condition.getValue());
        }
        return normalized;
    }

    private String normalizeLogicalOperator(String value, String defaultValue) {
        String operator = value == null || value.isBlank()
                ? defaultValue
                : value.trim().toUpperCase(Locale.ROOT);
        if (!"AND".equals(operator) && !"OR".equals(operator)) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_GROUP_OPERATOR + ": " + value);
        }
        return operator;
    }

    private AccessRoleAssignmentRule.ScheduleType parseScheduleType(String value) {
        if (value == null || value.isBlank()) {
            return AccessRoleAssignmentRule.ScheduleType.MANUAL;
        }
        try {
            return AccessRoleAssignmentRule.ScheduleType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_SCHEDULE_TYPE + ": " + value, ex);
        }
    }

    private void normalizeScheduleFields(AccessRoleAssignmentRule rule) {
        rule.setCronExpression(trimToNull(rule.getCronExpression()));
        rule.setScheduleTime(trimToNull(rule.getScheduleTime()));
        rule.setScheduleDate(trimToNull(rule.getScheduleDate()));
        rule.setScheduleTimezone(trimToNull(rule.getScheduleTimezone()));
        if (rule.getScheduleType() != AccessRoleAssignmentRule.ScheduleType.CRON) {
            rule.setCronExpression(null);
            rule.setScheduleTime(null);
            rule.setScheduleDate(null);
            rule.setScheduleTimezone(null);
        }
    }

    private void validateSchedule(AccessRoleAssignmentRule rule) {
        normalizeScheduleFields(rule);
        if (rule.getScheduleType() != AccessRoleAssignmentRule.ScheduleType.CRON) {
            return;
        }
        if (rule.getCronExpression() == null && rule.getScheduleTime() == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CRON_SCHEDULE_REQUIRED);
        }
        resolveTimezone(rule.getScheduleTimezone());
        if (rule.getCronExpression() != null) {
            try {
                CronExpression.parse(rule.getCronExpression());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_CRON_EXPRESSION + ": " + rule.getCronExpression(), ex);
            }
        }
        if (rule.getScheduleTime() != null) {
            parseScheduleTime(rule.getScheduleTime());
        }
        if (rule.getScheduleDate() != null) {
            parseScheduleDate(rule.getScheduleDate());
        }
    }

    private void applyNextExecution(AccessRoleAssignmentRule rule) {
        if (!rule.isEnabled() || rule.getScheduleType() != AccessRoleAssignmentRule.ScheduleType.CRON) {
            rule.setNextExecutionAt(null);
            return;
        }
        ZoneId zone = resolveTimezone(rule.getScheduleTimezone());
        if (rule.getCronExpression() != null) {
            LocalDateTime next = CronExpression.parse(rule.getCronExpression()).next(LocalDateTime.now(zone));
            rule.setNextExecutionAt(next != null ? next.atZone(zone).toInstant().toEpochMilli() : null);
            return;
        }
        if (rule.getScheduleTime() != null && rule.getScheduleDate() != null) {
            LocalDateTime scheduled = LocalDateTime.of(parseScheduleDate(rule.getScheduleDate()), parseScheduleTime(rule.getScheduleTime()));
            long epochMs = scheduled.atZone(zone).toInstant().toEpochMilli();
            rule.setNextExecutionAt(epochMs > Instant.now().toEpochMilli() ? epochMs : null);
            return;
        }
        if (rule.getScheduleTime() != null) {
            LocalDate today = LocalDate.now(zone);
            LocalDateTime candidate = LocalDateTime.of(today, parseScheduleTime(rule.getScheduleTime()));
            if (candidate.atZone(zone).toInstant().toEpochMilli() <= Instant.now().toEpochMilli()) {
                candidate = LocalDateTime.of(today.plusDays(1), parseScheduleTime(rule.getScheduleTime()));
            }
            rule.setNextExecutionAt(candidate.atZone(zone).toInstant().toEpochMilli());
        }
    }

    private ZoneId resolveTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(AccessRoleAssignmentRuleConstants.DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_TIMEZONE + ": " + timezone, ex);
        }
    }

    private LocalTime parseScheduleTime(String scheduleTime) {
        try {
            return LocalTime.parse(scheduleTime, SCHEDULE_TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_SCHEDULE_TIME + ": " + scheduleTime, ex);
        }
    }

    private LocalDate parseScheduleDate(String scheduleDate) {
        try {
            return LocalDate.parse(scheduleDate, SCHEDULE_DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_SCHEDULE_DATE + ": " + scheduleDate, ex);
        }
    }

    private List<String> normalizeIdentityTypes(List<String> identityTypes, boolean defaultWhenMissing) {
        if (identityTypes == null) {
            if (defaultWhenMissing) {
                return List.of(AccessRoleAssignmentRuleConstants.DEFAULT_IDENTITY_TYPE);
            }
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_IDENTITY_TYPES_REQUIRED);
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String identityType : identityTypes) {
            if (identityType == null || identityType.isBlank()) {
                continue;
            }
            String value = identityType.trim().toUpperCase(Locale.ROOT);
            validateIdentityType(value);
            normalized.add(value);
        }

        if (normalized.isEmpty()) {
            if (defaultWhenMissing) {
                return List.of(AccessRoleAssignmentRuleConstants.DEFAULT_IDENTITY_TYPE);
            }
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_IDENTITY_TYPES_REQUIRED);
        }

        return List.copyOf(normalized);
    }

    private void validateIdentityType(String identityType) {
        if (!"EMPLOYEE".equals(identityType)
                && !"NON_EMPLOYEE".equals(identityType)
                && !"NON_HUMAN".equals(identityType)) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_IDENTITY_TYPE + ": " + identityType);
        }
    }

    private List<Class<? extends Identity>> resolveIdentityClasses(List<String> identityTypes) {
        List<Class<? extends Identity>> identityClasses = new ArrayList<>(identityTypes.size());
        for (String identityType : identityTypes) {
            identityClasses.add(resolveIdentityClass(identityType));
        }
        return List.copyOf(identityClasses);
    }

    private Class<? extends Identity> resolveIdentityClass(String identityType) {
        return switch (identityType) {
            case "EMPLOYEE" -> Employee.class;
            case "NON_EMPLOYEE" -> NonEmployee.class;
            case "NON_HUMAN" -> NonHumanIdentity.class;
            default -> throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_IDENTITY_TYPE + ": " + identityType);
        };
    }

    private String resolveFieldForIdentityTypes(String field, List<Class<? extends Identity>> identityClasses) {
        IllegalArgumentException lastFailure = null;
        for (Class<? extends Identity> identityClass : identityClasses) {
            try {
                return fieldRegistry.resolveField(field, identityClass);
            } catch (IllegalArgumentException ex) {
                lastFailure = ex;
            }
        }
        throw lastFailure != null ? lastFailure : new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CONDITION_FIELD_REQUIRED);
    }

    private Sort buildSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String property = sort.trim();
        Sort.Direction direction = Sort.Direction.ASC;
        if (property.startsWith("-")) {
            direction = Sort.Direction.DESC;
            property = property.substring(1);
        }
        if (property.isBlank()) {
            property = "createdAt";
        }
        return Sort.by(direction, property);
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 200);
    }

    private String resolveRoleDisplayName(AccessRole role) {
        return role.getDisplayName() != null && !role.getDisplayName().isBlank()
                ? role.getDisplayName()
                : role.getName();
    }

    private void publishRuleEvaluationEvent(AccessRoleAssignmentRule rule,
                                            com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution.TriggerType triggerType,
                                            String triggeredBy) {
        String actor = triggeredBy == null || triggeredBy.isBlank()
                ? AccessRoleAssignmentRuleConstants.SYSTEM_TRIGGER
                : triggeredBy;

        eventPublisher.publishEvent(new AccessRoleAssignmentRuleEvaluationEvent(
                rule.getTenantId(),
                rule.getId(),
                triggerType,
                actor,
                rule.getScheduleType().name()));

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_RULE_EVALUATION_QUEUED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, rule.getTenantId())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, rule.getId().toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE, rule.getScheduleType().name())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGER_TYPE, triggerType.name())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, actor)
                .log();
    }

    private void applyGlobalSchedule(String tenantId,
                                     AccessRoleAssignmentRule rule) {
        globalScheduleConfigRepository.findByTenantId(tenantId).ifPresentOrElse(config -> {
            rule.setScheduleType(parseScheduleType(config.getScheduleType()));
            rule.setCronExpression(trimToNull(config.getCronExpression()));
            rule.setScheduleTime(trimToNull(config.getScheduleTime()));
            rule.setScheduleDate(trimToNull(config.getScheduleDate()));
            rule.setScheduleTimezone(trimToNull(config.getScheduleTimezone()));

            logger.info()
                    .message(AccessRoleAssignmentRuleConstants.LOG_RULE_INHERITED_GLOBAL_SCHEDULE)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE,
                            rule.getScheduleType() != null ? rule.getScheduleType().name() : "null")
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_CRON_EXPRESSION,
                            rule.getCronExpression() != null ? rule.getCronExpression() : "none")
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TIME,
                            rule.getScheduleTime() != null ? rule.getScheduleTime() : "none")
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_DATE,
                            rule.getScheduleDate() != null ? rule.getScheduleDate() : "none")
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TIMEZONE,
                            rule.getScheduleTimezone() != null ? rule.getScheduleTimezone() : "none")
                    .log();
        }, () -> {
            rule.setScheduleType(AccessRoleAssignmentRule.ScheduleType.MANUAL);
            rule.setCronExpression(null);
            rule.setScheduleTime(null);
            rule.setScheduleDate(null);
            rule.setScheduleTimezone(null);
        });
    }


    private String recalculateAllNextExecutions(String tenantId,
                                                AccessRoleAssignmentRule.ScheduleType scheduleType,
                                                long now) {
        if (scheduleType != AccessRoleAssignmentRule.ScheduleType.CRON) {
            ruleRepository.bulkUpdateNextExecutionAt(tenantId, null, now);
            logger.info()
                    .message("Cleared next execution time for all access role assignment rules")
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE, scheduleType.name())
                    .log();
            return null;
        }

        entityManager.flush();
        entityManager.clear();

        List<AccessRoleAssignmentRule> cronRules =
                ruleRepository.findByTenantIdAndScheduleTypeOrderByPriorityAsc(tenantId, scheduleType);
        Long earliestNextExecution = null;
        for (AccessRoleAssignmentRule rule : cronRules) {
            applyNextExecution(rule);
            if (rule.getNextExecutionAt() != null
                    && (earliestNextExecution == null || rule.getNextExecutionAt() < earliestNextExecution)) {
                earliestNextExecution = rule.getNextExecutionAt();
            }
        }
        ruleRepository.saveAll(cronRules);

        logger.info()
                .message("Recalculated next execution time for CRON access role assignment rules")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_COUNT, String.valueOf(cronRules.size()))
                .log();

        return earliestNextExecution != null ? Instant.ofEpochMilli(earliestNextExecution).toString() : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}


