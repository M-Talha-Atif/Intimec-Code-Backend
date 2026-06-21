package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRiskInsightsResponse;

import java.util.UUID;

/**
 * Computes risk-insight metrics for a single access role.
 */
public interface AccessRoleRiskInsightsService {

    /**
     * Returns high-risk, admin-privilege and overlap counts for the role's
     * effective entitlement closure, capped at the configured depth.
     *
     * @param tenantId the tenant identifier
     * @param roleId   the target role ID
     * @return populated {@link AccessRoleRiskInsightsResponse}
     */
    AccessRoleRiskInsightsResponse getRiskInsights(String tenantId, UUID roleId);
}

