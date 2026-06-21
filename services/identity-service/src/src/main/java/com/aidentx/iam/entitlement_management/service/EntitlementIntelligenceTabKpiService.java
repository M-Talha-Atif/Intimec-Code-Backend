package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementIntelligenceTabKpiResponse;

/**
 * Service interface for Entitlement Intelligence Tab KPI Analytics.
 * Provides business logic for calculating and retrieving entitlement-related KPIs
 * used for governance dashboards and reporting across all resources within a tenant.
 * 
 * This is specifically for Intelligence Tab KPI analytics - differentiated from other KPI types.
 */
public interface EntitlementIntelligenceTabKpiService {

    /**
     * Retrieves entitlement count KPIs for Intelligence Tab for a given tenant across all resources.
     *
     * KPIs Calculated:
     * - totalEntitlements: Sum of all entitlements for the tenant
     * - activeEntitlements: Entitlements with lifecycle = "Active"
     * - orphanedEntitlements: Entitlements where ownerId is NULL
     * - unclassifiedEntitlements: Entitlements where classification is empty or NULL
     * - highRiskEntitlements: Entitlements where riskLevel = "High"
     * - averageUsersPerEntitlement: Total assignments / total entitlements
     *
     * @param tenantId the tenant ID to filter by (from X-Tenant-ID header)
     * @return populated response DTO with all KPI metrics
     * @throws IllegalArgumentException if tenantId is invalid
     */
    EntitlementIntelligenceTabKpiResponse getIntelligenceTabKpis(String tenantId);
}



