package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionDto;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionGroupDto;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleNormalizationService;
import com.aidentx.iam.entitlement_management.model.enums.RuleOperator;
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
 * Default shared normalizer for access-role assignment-rule conditions and identity types.
 */
@Service
public class AccessRoleAssignmentRuleNormalizationServiceImpl implements AccessRoleAssignmentRuleNormalizationService {

    private final IdentityFieldRegistry fieldRegistry;

    public AccessRoleAssignmentRuleNormalizationServiceImpl(IdentityFieldRegistry fieldRegistry) {
        this.fieldRegistry = fieldRegistry;
    }

    @Override
    public List<String> normalizeIdentityTypes(List<String> identityTypes, boolean defaultWhenMissing) {
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

    @Override
    public List<Class<? extends Identity>> resolveIdentityClasses(List<String> identityTypes) {
        List<Class<? extends Identity>> classes = new ArrayList<>(identityTypes.size());
        for (String identityType : identityTypes) {
            classes.add(resolveIdentityClass(identityType));
        }
        return List.copyOf(classes);
    }

    @Override
    public List<Map<String, Object>> normalizeConditionGroups(List<AccessRoleAssignmentRuleConditionGroupDto> groups,
                                                              List<String> identityTypes) {
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CONDITION_GROUPS_REQUIRED);
        }
        List<Class<? extends Identity>> identityClasses = resolveIdentityClasses(identityTypes);
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
        normalized.put("groupOperator", normalizeLogicalOperator(group.getGroupOperator(), AccessRoleAssignmentRuleConstants.DEFAULT_GROUP_OPERATOR));
        normalized.put("interGroupOperator", normalizeLogicalOperator(group.getInterGroupOperator(), AccessRoleAssignmentRuleConstants.DEFAULT_INTER_GROUP_OPERATOR));
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

    private void validateIdentityType(String identityType) {
        if (!"EMPLOYEE".equals(identityType)
                && !"NON_EMPLOYEE".equals(identityType)
                && !"NON_HUMAN".equals(identityType)) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_IDENTITY_TYPE + ": " + identityType);
        }
    }

    private Class<? extends Identity> resolveIdentityClass(String identityType) {
        return switch (identityType) {
            case "EMPLOYEE" -> Employee.class;
            case "NON_EMPLOYEE" -> NonEmployee.class;
            case "NON_HUMAN" -> NonHumanIdentity.class;
            default -> throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_INVALID_IDENTITY_TYPE + ": " + identityType);
        };
    }
}

