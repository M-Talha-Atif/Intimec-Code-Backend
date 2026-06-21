package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementSprawlKpiResponse;

/**
 * Service interface for Entitlement Sprawl KPI Analytics.
 * Provides business logic for calculating and retrieving entitlement sprawl metrics
 * used for tracking entitlement sprawl within a tenant.
 * 
 * This is specifically for Sprawl KPI analytics - differentiated from other KPI types.
 */
public interface EntitlementSprawlKpiService {

    /**
     * Retrieves sprawl KPI metrics for a given tenant across all resources.
     *
     * KPIs Calculated:
     * - sprawlIndex: Average entitlements per user (currently mock - returns 0)
     * - staleEntitlements: Entitlements deprecated/unused since start of year (currently mock - returns 0)
     * - overProvisionedCount: Entitlements assigned to 100+ users (real)
     * - directAssignmentCount: Entitlements with direct manual assignments (real)
     *
     * @param tenantId the tenant ID to filter by (from X-Tenant-ID header)
     * @return populated response DTO with all sprawl KPI metrics
     * @throws IllegalArgumentException if tenantId is invalid
     */
    EntitlementSprawlKpiResponse getSprawlKpis(String tenantId);
}

