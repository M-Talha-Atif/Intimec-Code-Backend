package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementClassificationKpiResponse;

/**
 * Service interface for Entitlement Classification KPI Analytics.
 * Provides business logic for calculating and retrieving entitlement classification metrics
 * used for tracking entitlement governance and classification status within a tenant.
 * 
 * This is specifically for Classification KPI analytics - differentiated from other KPI types.
 */
public interface EntitlementClassificationKpiService {

    /**
     * Retrieves classification KPI metrics for a given tenant across all resources.
     *
     * KPIs Calculated:
     * - ownershipGaps: Count of entitlements without owners (ownerId = NULL)
     * - unclassified: Count of unclassified entitlements (currently mock - returns 0)
     * - requestabilityRate: Percentage of requestable entitlements (currently mock - returns 0%)
     * - requestableEntitlements: Count of entitlements marked as requestable (real)
     *
     * @param tenantId the tenant ID to filter by (from X-Tenant-ID header)
     * @return populated response DTO with all classification KPI metrics
     * @throws IllegalArgumentException if tenantId is invalid
     */
    EntitlementClassificationKpiResponse getClassificationKpis(String tenantId);
}

