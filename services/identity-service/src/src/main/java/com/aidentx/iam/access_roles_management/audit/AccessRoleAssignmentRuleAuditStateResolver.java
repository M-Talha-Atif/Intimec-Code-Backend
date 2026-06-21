package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleResponse;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves DB state needed for access-role assignment rule auditing.
 *
 * <p>The audit aspect delegates all repository reads here so snapshot capture and
 * diff construction always happen inside a transactional Spring bean rather than
 * directly inside AOP advice code.</p>
 */
@Service
@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
public class AccessRoleAssignmentRuleAuditStateResolver {

    private final AccessRoleAssignmentRuleRepository ruleRepository;
    private final AccessRoleRepository roleRepository;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    public AccessRoleAssignmentRuleAuditStateResolver(AccessRoleAssignmentRuleRepository ruleRepository,
                                                      AccessRoleRepository roleRepository,
                                                      ObjectMapper objectMapper,
                                                      UnifiedLoggerFactory loggerFactory) {
        this.ruleRepository = ruleRepository;
        this.roleRepository = roleRepository;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAssignmentRuleAuditConstants.LOG_CATEGORY);
    }

    /**
     * Loads the current rule state and its owning role context for auditing.
     *
     * @param tenantId tenant identifier
     * @param ruleId rule identifier
     * @return optional snapshot containing entity, role context, and JSON state
     */
    public Optional<RuleAuditSnapshot> captureRuleSnapshot(String tenantId, UUID ruleId) {
        try {
            return ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                    .map(rule -> {
                        AccessRole role = roleRepository.findById(rule.getRoleId())
                                .filter(found -> tenantId.equals(found.getTenantId()))
                                .orElse(null);

                        logger.info()
                                .category(AccessRoleAssignmentRuleAuditConstants.LOG_CATEGORY)
                                .message(AccessRoleAssignmentRuleAuditConstants.LOG_RULE_CONTEXT_RESOLVED)
                                .details(Map.of(
                                        AccessRoleAssignmentRuleAuditConstants.DETAIL_TENANT_ID, tenantId,
                                        AccessRoleAssignmentRuleAuditConstants.DETAIL_RULE_ID, rule.getId().toString(),
                                        AccessRoleAssignmentRuleAuditConstants.DETAIL_ROLE_ID,
                                        rule.getRoleId() != null ? rule.getRoleId().toString() : AccessRoleAssignmentRuleAuditConstants.NOT_AVAILABLE
                                ))
                                .log();

                        return new RuleAuditSnapshot(rule, role, buildRuleState(rule, role));
                    });
        } catch (Exception e) {
            logger.warn()
                    .category(AccessRoleAssignmentRuleAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentRuleAuditConstants.LOG_STATE_CAPTURE_FAILED)
                    .details(Map.of(
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_TENANT_ID, safe(tenantId),
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_RULE_ID, ruleId != null ? ruleId.toString() : AccessRoleAssignmentRuleAuditConstants.NOT_AVAILABLE,
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName()
                    ))
                    .log();
            return Optional.empty();
        }
    }

    /**
     * Builds a user-friendly rule state snapshot from the response DTO returned
     * by service-layer CRUD methods.
     *
     * @param response rule response DTO
     * @return structured JSON snapshot
     */
    public JsonNode buildRuleState(AccessRoleAssignmentRuleResponse response) {
        ObjectNode node = objectMapper.createObjectNode();
        if (response == null) {
            return node;
        }

        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_RULE_ID, response.getId());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_RULE_NAME, response.getName());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_ROLE_ID, response.getRoleId());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_ROLE_NAME, response.getRoleName());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_ROLE_TYPE, response.getRoleType());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_ROLE_STATUS, response.getRoleStatus());
        node.put(AccessRoleAssignmentRuleAuditConstants.STATE_ENABLED, response.isEnabled());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_SCHEDULE_TYPE, response.getScheduleType());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_CRON_EXPRESSION, response.getCronExpression());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_SCHEDULE_TIME, response.getScheduleTime());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_SCHEDULE_DATE, response.getScheduleDate());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_SCHEDULE_TIMEZONE, response.getScheduleTimezone());
        node.put(AccessRoleAssignmentRuleAuditConstants.STATE_PRIORITY, response.getPriority());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_LAST_EVALUATED_AT, response.getLastEvaluatedAt());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_NEXT_EXECUTION_AT, response.getNextExecutionAt());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_CREATED_BY, response.getCreatedBy());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_CREATED_AT, response.getCreatedAt());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_UPDATED_AT, response.getUpdatedAt());

        if (response.getIdentityTypes() != null) {
            node.set(AccessRoleAssignmentRuleAuditConstants.STATE_IDENTITY_TYPES,
                    objectMapper.valueToTree(response.getIdentityTypes()));
        }
        if (response.getConditionGroups() != null) {
            node.set(AccessRoleAssignmentRuleAuditConstants.STATE_CONDITION_GROUPS,
                    objectMapper.valueToTree(response.getConditionGroups()));
        }
        return node;
    }

    /**
     * Computes a diff-only before/after payload for update audit events.
     *
     * @param stateBefore full before snapshot
     * @param stateAfter full after snapshot
     * @return diff containing only changed fields
     */
    public StateDiff buildDiff(JsonNode stateBefore, JsonNode stateAfter) {
        if (stateBefore == null) {
            return new StateDiff(null, stateAfter);
        }
        if (stateAfter == null) {
            return new StateDiff(stateBefore, null);
        }
        if (!stateBefore.isObject() || !stateAfter.isObject()) {
            return new StateDiff(stateBefore, stateAfter);
        }

        ObjectNode beforeDiff = objectMapper.createObjectNode();
        ObjectNode afterDiff = objectMapper.createObjectNode();
        Set<String> allFields = new LinkedHashSet<>();
        stateBefore.fieldNames().forEachRemaining(allFields::add);
        stateAfter.fieldNames().forEachRemaining(allFields::add);

        for (String field : allFields) {
            JsonNode beforeValue = stateBefore.get(field);
            JsonNode afterValue = stateAfter.get(field);
            if (!Objects.equals(beforeValue, afterValue)) {
                if (beforeValue != null) {
                    beforeDiff.set(field, beforeValue);
                }
                if (afterValue != null) {
                    afterDiff.set(field, afterValue);
                }
            }
        }

        return new StateDiff(beforeDiff.isEmpty() ? null : beforeDiff, afterDiff.isEmpty() ? null : afterDiff);
    }

    private JsonNode buildRuleState(AccessRoleAssignmentRule rule, AccessRole role) {
        ObjectNode node = objectMapper.createObjectNode();
        if (rule == null) {
            return node;
        }

        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_RULE_ID, rule.getId());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_RULE_NAME, rule.getName());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_ROLE_ID, rule.getRoleId());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_ROLE_NAME, role != null ? role.getName() : null);
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_ROLE_TYPE, role != null ? role.getType() : null);
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_ROLE_STATUS, role != null ? role.getStatus() : null);
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_ROLE_RISK_LEVEL, role != null ? role.getRiskLevel() : null);
        node.put(AccessRoleAssignmentRuleAuditConstants.STATE_ENABLED, rule.isEnabled());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_SCHEDULE_TYPE,
                rule.getScheduleType() != null ? rule.getScheduleType().name() : null);
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_CRON_EXPRESSION, rule.getCronExpression());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_SCHEDULE_TIME, rule.getScheduleTime());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_SCHEDULE_DATE, rule.getScheduleDate());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_SCHEDULE_TIMEZONE, rule.getScheduleTimezone());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_LAST_EVALUATED_AT, rule.getLastEvaluatedAt());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_NEXT_EXECUTION_AT, rule.getNextExecutionAt());
        node.put(AccessRoleAssignmentRuleAuditConstants.STATE_PRIORITY, rule.getPriority());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_CREATED_BY, rule.getCreatedBy());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_CREATED_AT, rule.getCreatedAt());
        putIfNotNull(node, AccessRoleAssignmentRuleAuditConstants.STATE_UPDATED_AT, rule.getUpdatedAt());

        if (rule.getIdentityTypes() != null) {
            node.set(AccessRoleAssignmentRuleAuditConstants.STATE_IDENTITY_TYPES,
                    objectMapper.valueToTree(rule.getIdentityTypes()));
        }
        if (rule.getConditionGroups() != null) {
            node.set(AccessRoleAssignmentRuleAuditConstants.STATE_CONDITION_GROUPS,
                    objectMapper.valueToTree(rule.getConditionGroups()));
        }
        return node;
    }

    private void putIfNotNull(ObjectNode node, String key, UUID value) {
        if (value != null) {
            node.put(key, value.toString());
        }
    }

    private void putIfNotNull(ObjectNode node, String key, String value) {
        if (value != null) {
            node.put(key, value);
        }
    }

    private void putIfNotNull(ObjectNode node, String key, Long value) {
        if (value != null) {
            node.put(key, value);
        }
    }

    private String safe(String value) {
        return value != null ? value : AccessRoleAssignmentRuleAuditConstants.NOT_AVAILABLE;
    }

    /**
     * Snapshot holder used by the audit aspect.
     */
    public record RuleAuditSnapshot(AccessRoleAssignmentRule rule, AccessRole role, JsonNode state) {
        public int conditionGroupCount() {
            return rule != null && rule.getConditionGroups() != null ? rule.getConditionGroups().size() : 0;
        }

        public List<String> identityTypes() {
            return rule != null ? rule.getIdentityTypes() : null;
        }
    }

    /**
     * Diff holder for update audits.
     */
    public record StateDiff(JsonNode before, JsonNode after) {
    }
}
