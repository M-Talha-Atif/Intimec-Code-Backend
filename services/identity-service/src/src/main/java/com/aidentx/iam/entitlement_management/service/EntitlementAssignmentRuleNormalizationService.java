package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.model.dto.RuleConditionDto;
import com.aidentx.iam.entitlement_management.model.dto.RuleConditionGroupDto;
import com.aidentx.iam.identity_management.model.entity.Identity;

import java.util.List;
import java.util.Map;

/**
 * Shared normalization contract for entitlement-assignment-rule identity types and grouped conditions.
 */
public interface EntitlementAssignmentRuleNormalizationService {

    List<String> normalizeIdentityTypes(List<String> identityTypes, boolean defaultWhenMissing);

    List<Class<? extends Identity>> resolveIdentityClasses(List<String> identityTypes);

    List<Map<String, Object>> normalizeConditionGroups(List<RuleConditionGroupDto> conditionGroups,
                                                       List<String> identityTypes);

    List<Map<String, Object>> wrapLegacyConditions(List<RuleConditionDto> conditions,
                                                   List<String> identityTypes);
}

