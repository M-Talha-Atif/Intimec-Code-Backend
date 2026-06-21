package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleEvaluationService;
import com.aidentx.iam.entitlement_management.model.enums.RuleOperator;
import com.aidentx.iam.entitlement_management.service.IdentityFieldRegistry;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Default in-memory evaluator for access-role assignment-rule condition groups.
 *
 * <p>The evaluator supports the canonical access-role rule JSON structure:
 * leaf {@code conditions}, nested {@code conditionGroups}, {@code groupOperator},
 * {@code interGroupOperator}, and {@code negated}. It uses the shared
 * {@link IdentityFieldRegistry} so field names and operators remain aligned with
 * entitlement dynamic-assignment rule evaluation.</p>
 */
@Service
public class AccessRoleAssignmentRuleEvaluationServiceImpl implements AccessRoleAssignmentRuleEvaluationService {

    private static final String KEY_FIELD = "field";
    private static final String KEY_OPERATOR = "operator";
    private static final String KEY_VALUE = "value";
    private static final String KEY_VALUES = "values";
    private static final String KEY_CONDITIONS = "conditions";
    private static final String KEY_CONDITION_GROUPS = "conditionGroups";
    private static final String KEY_GROUP_OPERATOR = "groupOperator";
    private static final String KEY_INTER_GROUP_OPERATOR = "interGroupOperator";
    private static final String KEY_NEGATED = "negated";

    private final IdentityFieldRegistry fieldRegistry;

    public AccessRoleAssignmentRuleEvaluationServiceImpl(IdentityFieldRegistry fieldRegistry) {
        this.fieldRegistry = fieldRegistry;
    }

    /** {@inheritDoc} */
    @Override
    public EvaluationResult evaluateIdentity(String tenantId, AccessRoleAssignmentRule rule, Identity identity) {
        validateTenantId(tenantId);
        validateRule(rule, tenantId);
        validateIdentity(identity, tenantId);

        try {
            boolean identityTypeMatched = isIdentityTypeAllowed(rule, identity);
            ConditionEvaluationResult conditionResult = identityTypeMatched
                    ? evaluateConditionGroups(rule.getConditionGroups(), identity)
                    : new ConditionEvaluationResult(false, List.of());
            boolean matched = identityTypeMatched && conditionResult.matched();
            Map<String, Object> evidence = buildEvidence(rule, identity, matched, null, identityTypeMatched, conditionResult.groupEvidence());
            return new EvaluationResult(rule.getId(), rule.getRoleId(), identity.getId(), resolveIdentityType(identity), matched, evidence, null);
        } catch (RuntimeException ex) {
            boolean identityTypeMatched = safeIdentityTypeAllowed(rule, identity);
            Map<String, Object> evidence = buildEvidence(rule, identity, false, ex.getMessage(), identityTypeMatched, List.of());
            return new EvaluationResult(rule.getId(), rule.getRoleId(), identity.getId(), resolveIdentityType(identity), false, evidence, ex.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean matches(String tenantId, AccessRoleAssignmentRule rule, Identity identity) {
        return evaluateIdentity(tenantId, rule, identity).matched();
    }

    /** {@inheritDoc} */
    @Override
    public ConditionEvaluationResult evaluateConditionGroups(List<Map<String, Object>> conditionGroups, Identity identity) {
        if (identity == null) {
            throw new IllegalArgumentException("Identity is required");
        }
        if (conditionGroups == null || conditionGroups.isEmpty()) {
            return new ConditionEvaluationResult(true, List.of());
        }

        List<GroupEvaluation> groupEvaluations = new ArrayList<>();
        for (int i = 0; i < conditionGroups.size(); i++) {
            groupEvaluations.add(evaluateGroup(conditionGroups.get(i), i, identity));
        }

        boolean matched = groupEvaluations.get(0).matched();
        for (int i = 1; i < groupEvaluations.size(); i++) {
            String previousInterGroupOperator = groupEvaluations.get(i - 1).interGroupOperator();
            matched = "OR".equals(previousInterGroupOperator)
                    ? matched || groupEvaluations.get(i).matched()
                    : matched && groupEvaluations.get(i).matched();
        }

        return new ConditionEvaluationResult(matched, groupEvaluations.stream()
                .map(GroupEvaluation::evidence)
                .toList());
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> buildEvidence(AccessRoleAssignmentRule rule,
                                             Identity identity,
                                             boolean matched,
                                             String errorMessage) {
        boolean identityTypeMatched = rule != null && identity != null && isIdentityTypeAllowed(rule, identity);
        List<Map<String, Object>> groupEvidence = List.of();
        if (identity != null && identityTypeMatched) {
            groupEvidence = evaluateConditionGroups(rule.getConditionGroups(), identity).groupEvidence();
        }
        return buildEvidence(rule, identity, matched, errorMessage, identityTypeMatched, groupEvidence);
    }

    private GroupEvaluation evaluateGroup(Map<String, Object> group, int index, Identity identity) {
        if (group == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CONDITION_GROUP_EMPTY);
        }

        String groupOperator = normalizeLogicalOperator(group.get(KEY_GROUP_OPERATOR), AccessRoleAssignmentRuleConstants.DEFAULT_GROUP_OPERATOR);
        String interGroupOperator = normalizeLogicalOperator(group.get(KEY_INTER_GROUP_OPERATOR), AccessRoleAssignmentRuleConstants.DEFAULT_INTER_GROUP_OPERATOR);
        boolean negated = group.get(KEY_NEGATED) instanceof Boolean value && value;

        List<Boolean> childResults = new ArrayList<>();
        List<Map<String, Object>> conditionEvidence = new ArrayList<>();
        for (Map<String, Object> condition : extractMapList(group.get(KEY_CONDITIONS))) {
            ConditionEvaluation conditionEvaluation = evaluateCondition(condition, identity);
            childResults.add(conditionEvaluation.matched());
            conditionEvidence.add(conditionEvaluation.evidence());
        }

        List<Map<String, Object>> subGroupEvidence = new ArrayList<>();
        int childIndex = 0;
        for (Map<String, Object> subGroup : extractMapList(group.get(KEY_CONDITION_GROUPS))) {
            GroupEvaluation subGroupEvaluation = evaluateGroup(subGroup, childIndex++, identity);
            childResults.add(subGroupEvaluation.matched());
            subGroupEvidence.add(subGroupEvaluation.evidence());
        }

        boolean matched = childResults.isEmpty()
                ? !negated
                : combine(childResults, groupOperator);
        if (negated && !childResults.isEmpty()) {
            matched = !matched;
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("groupIndex", index);
        evidence.put(KEY_GROUP_OPERATOR, groupOperator);
        evidence.put(KEY_INTER_GROUP_OPERATOR, interGroupOperator);
        evidence.put(KEY_NEGATED, negated);
        evidence.put("matched", matched);
        evidence.put(KEY_CONDITIONS, conditionEvidence);
        if (!subGroupEvidence.isEmpty()) {
            evidence.put(KEY_CONDITION_GROUPS, subGroupEvidence);
        }

        return new GroupEvaluation(matched, interGroupOperator, evidence);
    }

    private ConditionEvaluation evaluateCondition(Map<String, Object> condition, Identity identity) {
        if (condition == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CONDITION_GROUP_EMPTY);
        }

        String rawField = stringValue(condition.get(KEY_FIELD));
        RuleOperator operator = RuleOperator.fromString(stringValue(condition.get(KEY_OPERATOR)));
        Class<? extends Identity> identityClass = identity.getClass().asSubclass(Identity.class);
        String resolvedField = fieldRegistry.resolveField(rawField, identityClass);
        Object actualValue = readFieldValue(identity, resolvedField);
        Object expectedValue = condition.get(KEY_VALUES) != null ? condition.get(KEY_VALUES) : condition.get(KEY_VALUE);
        boolean matched = evaluateConditionValue(actualValue, operator, condition.get(KEY_VALUE), condition.get(KEY_VALUES));

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put(KEY_FIELD, resolvedField);
        evidence.put(KEY_OPERATOR, operator.name());
        evidence.put("expectedValue", expectedValue);
        evidence.put("actualValue", actualValue);
        evidence.put("matched", matched);
        return new ConditionEvaluation(matched, evidence);
    }

    private boolean evaluateConditionValue(Object actualValue, RuleOperator operator, Object expectedValue, Object expectedValues) {
        if (actualValue == null) {
            return operator == RuleOperator.NOT_EQUALS || operator == RuleOperator.NOT_IN;
        }

        String actual = actualValue.toString().toUpperCase(Locale.ROOT);
        String expected = expectedValue != null ? expectedValue.toString().toUpperCase(Locale.ROOT) : "";
        return switch (operator) {
            case EQUALS -> actual.equals(expected);
            case NOT_EQUALS -> !actual.equals(expected);
            case CONTAINS -> actual.contains(expected);
            case STARTS_WITH -> actual.startsWith(expected);
            case ENDS_WITH -> actual.endsWith(expected);
            case IN -> resolveListValues(expectedValue, expectedValues).stream()
                    .map(item -> item.toUpperCase(Locale.ROOT))
                    .anyMatch(actual::equals);
            case NOT_IN -> resolveListValues(expectedValue, expectedValues).stream()
                    .map(item -> item.toUpperCase(Locale.ROOT))
                    .noneMatch(actual::equals);
        };
    }

    private Map<String, Object> buildEvidence(AccessRoleAssignmentRule rule,
                                             Identity identity,
                                             boolean matched,
                                             String errorMessage,
                                             boolean identityTypeMatched,
                                             List<Map<String, Object>> groupEvidence) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("overallMatch", matched);
        evidence.put("identityTypeMatched", identityTypeMatched);
        evidence.put("identityType", identity != null ? resolveIdentityType(identity) : null);
        evidence.put("configuredIdentityTypes", rule != null ? resolveIdentityTypes(rule) : List.of());
        evidence.put("fields", identity != null ? snapshotFields(identity) : Map.of());
        evidence.put(KEY_CONDITION_GROUPS, groupEvidence != null ? groupEvidence : List.of());
        if (errorMessage != null) {
            evidence.put("error", errorMessage);
        }
        return evidence;
    }

    private Map<String, Object> snapshotFields(Identity identity) {
        Map<String, Object> fields = new LinkedHashMap<>();
        Class<? extends Identity> identityClass = identity.getClass().asSubclass(Identity.class);
        for (String field : fieldRegistry.getFieldsForClass(identityClass)) {
            Object value = readFieldValue(identity, field);
            if (value != null) {
                fields.put(field, value.toString());
            }
        }
        return fields;
    }

    private Object readFieldValue(Identity identity, String fieldName) {
        String suffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        for (String getterName : List.of("get" + suffix, "is" + suffix)) {
            try {
                Method getter = identity.getClass().getMethod(getterName);
                return getter.invoke(identity);
            } catch (NoSuchMethodException ignored) {
                // Try the next JavaBean getter form.
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalArgumentException("Unable to read identity field: " + fieldName, ex);
            }
        }
        throw new IllegalArgumentException("Identity field not found: " + fieldName);
    }

    private boolean isIdentityTypeAllowed(AccessRoleAssignmentRule rule, Identity identity) {
        return resolveIdentityTypes(rule).contains(resolveIdentityType(identity));
    }

    private boolean safeIdentityTypeAllowed(AccessRoleAssignmentRule rule, Identity identity) {
        try {
            return isIdentityTypeAllowed(rule, identity);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private List<String> resolveIdentityTypes(AccessRoleAssignmentRule rule) {
        if (rule.getIdentityTypes() == null || rule.getIdentityTypes().isEmpty()) {
            return List.of(AccessRoleAssignmentRuleConstants.DEFAULT_IDENTITY_TYPE);
        }
        return rule.getIdentityTypes().stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String resolveIdentityType(Identity identity) {
        if (identity instanceof Employee) {
            return "EMPLOYEE";
        }
        if (identity instanceof NonEmployee) {
            return "NON_EMPLOYEE";
        }
        if (identity instanceof NonHumanIdentity) {
            return "NON_HUMAN";
        }
        return "IDENTITY";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractMapList(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                maps.add((Map<String, Object>) map);
            }
        }
        return maps;
    }

    private boolean combine(List<Boolean> values, String operator) {
        return "OR".equals(operator)
                ? values.stream().anyMatch(Boolean.TRUE::equals)
                : values.stream().allMatch(Boolean.TRUE::equals);
    }

    private List<String> resolveListValues(Object value, Object values) {
        if (values instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(Object::toString).toList();
        }
        if (value != null) {
            return Arrays.stream(value.toString().split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .toList();
        }
        return List.of();
    }

    private String normalizeLogicalOperator(Object value, String defaultValue) {
        String operator = value == null || value.toString().isBlank()
                ? defaultValue
                : value.toString().trim().toUpperCase(Locale.ROOT);
        if (!"AND".equals(operator) && !"OR".equals(operator)) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_GROUP_OPERATOR + ": " + value);
        }
        return operator;
    }

    private void validateRule(AccessRoleAssignmentRule rule, String tenantId) {
        if (rule == null) {
            throw new IllegalArgumentException("Access-role assignment rule is required");
        }
        if (!tenantId.equals(rule.getTenantId())) {
            throw new IllegalArgumentException("Rule does not belong to tenant: " + tenantId);
        }
    }

    private void validateIdentity(Identity identity, String tenantId) {
        if (identity == null) {
            throw new IllegalArgumentException("Identity is required");
        }
        if (!tenantId.equals(identity.getTenantId())) {
            throw new IllegalArgumentException("Identity does not belong to tenant: " + tenantId);
        }
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_TENANT_REQUIRED);
        }
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private record GroupEvaluation(boolean matched, String interGroupOperator, Map<String, Object> evidence) {
    }

    private record ConditionEvaluation(boolean matched, Map<String, Object> evidence) {
    }
}

