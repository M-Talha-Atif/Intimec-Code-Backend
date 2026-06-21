package com.aidentx.iam.entitlement_management.service.impl;
import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;
import com.aidentx.iam.entitlement_management.model.dto.RuleConditionDto;
import com.aidentx.iam.entitlement_management.model.dto.RuleConditionGroupDto;
import com.aidentx.iam.entitlement_management.model.enums.RuleOperator;
import com.aidentx.iam.entitlement_management.service.EntitlementAssignmentRuleNormalizationService;
import com.aidentx.iam.entitlement_management.service.IdentityFieldRegistry;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
/**
 * Shared normalization for entitlement assignment-rule identity types and grouped conditions.
 */
@Service
public class EntitlementAssignmentRuleNormalizationServiceImpl implements EntitlementAssignmentRuleNormalizationService {
    private final IdentityFieldRegistry fieldRegistry;
    public EntitlementAssignmentRuleNormalizationServiceImpl(IdentityFieldRegistry fieldRegistry) {
        this.fieldRegistry = fieldRegistry;
    }
    @Override
    public List<String> normalizeIdentityTypes(List<String> identityTypes, boolean defaultWhenMissing) {
        if (identityTypes == null) {
            if (defaultWhenMissing) {
                return List.of(RuleEvaluationConstants.DEFAULT_IDENTITY_TYPE);
            }
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_IDENTITY_TYPES_REQUIRED);
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
                return List.of(RuleEvaluationConstants.DEFAULT_IDENTITY_TYPE);
            }
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_IDENTITY_TYPES_REQUIRED);
        }
        return List.copyOf(normalized);
    }
    @Override
    public List<Class<? extends Identity>> resolveIdentityClasses(List<String> identityTypes) {
        List<Class<? extends Identity>> classes = new ArrayList<>(identityTypes.size());
        for (String identityType : identityTypes) {
            classes.add(resolveIdentityClass(identityType));
        }
        return List.copyOf(classes);
    }
    @Override
    public List<Map<String, Object>> normalizeConditionGroups(List<RuleConditionGroupDto> conditionGroups,
                                                              List<String> identityTypes) {
        if (conditionGroups == null || conditionGroups.isEmpty()) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_CONDITION_GROUPS_EMPTY);
        }
        if (conditionGroups.size() > RuleEvaluationConstants.MAX_CONDITION_GROUPS) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_TOO_MANY_GROUPS);
        }
        List<Class<? extends Identity>> identityClasses = resolveIdentityClasses(identityTypes);
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (RuleConditionGroupDto group : conditionGroups) {
            normalized.add(normalizeConditionGroup(group, identityClasses, 1));
        }
        return normalized;
    }
    @Override
    public List<Map<String, Object>> wrapLegacyConditions(List<RuleConditionDto> conditions,
                                                          List<String> identityTypes) {
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_CONDITION_GROUPS_EMPTY);
        }
        List<Class<? extends Identity>> identityClasses = resolveIdentityClasses(identityTypes);
        List<Map<String, Object>> conditionMaps = conditions.stream()
                .map(condition -> normalizeCondition(condition, identityClasses))
                .toList();
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("groupOperator", RuleEvaluationConstants.DEFAULT_GROUP_OPERATOR);
        group.put("interGroupOperator", RuleEvaluationConstants.DEFAULT_INTER_GROUP_OPERATOR);
        group.put("negated", false);
        group.put("conditions", conditionMaps);
        group.put("conditionGroups", List.of());
        return List.of(group);
    }
    private Map<String, Object> normalizeConditionGroup(RuleConditionGroupDto group,
                                                        List<Class<? extends Identity>> identityClasses,
                                                        int currentDepth) {
        if (group == null) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_GROUP_EMPTY);
        }
        if (currentDepth > RuleEvaluationConstants.MAX_GROUP_DEPTH) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_GROUP_DEPTH_EXCEEDED);
        }
        boolean hasConditions = group.getConditions() != null && !group.getConditions().isEmpty();
        boolean hasSubGroups = group.getConditionGroups() != null && !group.getConditionGroups().isEmpty();
        if (!hasConditions && !hasSubGroups) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_GROUP_EMPTY);
        }
        if (hasConditions && group.getConditions().size() > RuleEvaluationConstants.MAX_CONDITIONS_PER_GROUP) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_TOO_MANY_CONDITIONS_IN_GROUP);
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("groupOperator", normalizeLogicalOperator(group.getGroupOperator()));
        normalized.put("interGroupOperator", normalizeLogicalOperator(group.getInterGroupOperator()));
        normalized.put("negated", Boolean.TRUE.equals(group.getNegated()));
        normalized.put("conditions", hasConditions
                ? group.getConditions().stream().map(condition -> normalizeCondition(condition, identityClasses)).toList()
                : List.of());
        normalized.put("conditionGroups", hasSubGroups
                ? group.getConditionGroups().stream()
                .map(subGroup -> normalizeConditionGroup(subGroup, identityClasses, currentDepth + 1))
                .toList()
                : List.of());
        return normalized;
    }
    private Map<String, Object> normalizeCondition(RuleConditionDto condition,
                                                   List<Class<? extends Identity>> identityClasses) {
        if (condition == null) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_GROUP_EMPTY);
        }
        if (condition.getField() == null || condition.getField().isBlank()) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_FIELD_UNRESOLVED + ": (blank)");
        }
        if (condition.getOperator() == null || condition.getOperator().isBlank()) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_OPERATOR_BLANK);
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("field", resolveFieldForIdentityTypes(condition.getField(), identityClasses));
        normalized.put("operator", RuleOperator.fromString(condition.getOperator()).name());
        if (condition.getValues() != null && !condition.getValues().isEmpty()) {
            normalized.put("values", condition.getValues());
        } else if (condition.getValue() != null) {
            normalized.put("value", condition.getValue());
        }
        return normalized;
    }
    private String normalizeLogicalOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            return RuleEvaluationConstants.DEFAULT_GROUP_OPERATOR;
        }
        String normalized = operator.trim().toUpperCase(Locale.ROOT);
        if (!"AND".equals(normalized) && !"OR".equals(normalized)) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_INVALID_GROUP_OPERATOR + ". Got: " + operator);
        }
        return normalized;
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
        throw lastFailure != null ? lastFailure : new IllegalArgumentException(RuleEvaluationConstants.ERROR_FIELD_UNRESOLVED + ": " + field);
    }
    private void validateIdentityType(String identityType) {
        if (!"EMPLOYEE".equals(identityType)
                && !"NON_EMPLOYEE".equals(identityType)
                && !"NON_HUMAN".equals(identityType)) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_INVALID_IDENTITY_TYPE + ": " + identityType);
        }
    }
    private Class<? extends Identity> resolveIdentityClass(String identityType) {
        return switch (identityType) {
            case "EMPLOYEE" -> Employee.class;
            case "NON_EMPLOYEE" -> NonEmployee.class;
            case "NON_HUMAN" -> NonHumanIdentity.class;
            default -> throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_INVALID_IDENTITY_TYPE + ": " + identityType);
        };
    }
}
