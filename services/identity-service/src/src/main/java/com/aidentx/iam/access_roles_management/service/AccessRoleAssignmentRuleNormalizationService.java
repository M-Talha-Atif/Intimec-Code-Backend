package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionGroupDto;
import com.aidentx.iam.identity_management.model.entity.Identity;

import java.util.List;
import java.util.Map;

/**
 * Shared normalization/validation contract for access-role assignment-rule inputs.
 */
public interface AccessRoleAssignmentRuleNormalizationService {

    List<String> normalizeIdentityTypes(List<String> identityTypes, boolean defaultWhenMissing);

    List<Class<? extends Identity>> resolveIdentityClasses(List<String> identityTypes);

    List<Map<String, Object>> normalizeConditionGroups(List<AccessRoleAssignmentRuleConditionGroupDto> groups,
                                                       List<String> identityTypes);
}

