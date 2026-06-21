package com.aidentx.iam.access_management.service;

import com.aidentx.iam.access_management.model.dto.response.AccessKpiResponseDto;

/**
 * Service contract for cross-domain access KPI analytics.
 *
 * <p>Implementations aggregate data from entitlement assignments, access roles,
 * role-entitlement mappings, and identity-role assignments to calculate current
 * access-governance health metrics.</p>
 */
public interface AccessKpiService {

    /**
     * Calculates live access KPIs for one tenant.
     *
     * <p>No historical snapshot is read in the current implementation, so delta
     * values are intentionally absent until snapshot storage or scheduled jobs are
     * introduced.</p>
     *
     * @param tenantId tenant identifier used to scope all aggregate queries
     * @return current access KPI values for the tenant
     */
    AccessKpiResponseDto getAccessKpis(String tenantId);
}

