package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRulePreviewRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRulePreviewResponse;

/**
 * Read-only dry-run simulation contract for access-role assignment rules.
 */
public interface AccessRoleAssignmentRulePreviewService {

    AccessRoleAssignmentRulePreviewResponse previewRule(String tenantId, AccessRoleAssignmentRulePreviewRequest request);
}

