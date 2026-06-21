package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementRiskKpiResponse;

/**
 * Service interface for Entitlement Risk KPI Analytics.
 * Provides business logic for calculating and retrieving entitlement risk metrics
 * used for tracking entitlement risk posture within a tenant.
 *
 * This is specifically for Risk KPI analytics - differentiated from other KPI types.
 */
public interface EntitlementRiskKpiService {

    /**
     * Retrieves risk KPI metrics for a given tenant across all resources.
     *
     * KPIs Calculated:
     * - highRiskEntitlements: Count of entitlements with HIGH risk level (real)
     * - sodExposure: Count of SoD exposure entitlements (currently mock - returns 0)
     * - complianceTagged: Count of compliance tagged entitlements (currently mock - returns 0)
     * - dataSensitive: Count of data sensitive entitlements (currently mock - returns 0)
     *
     * @param tenantId the tenant ID to filter by (from X-Tenant-ID header)
     * @return populated response DTO with all risk KPI metrics
     * @throws IllegalArgumentException if tenantId is invalid
     */
    EntitlementRiskKpiResponse getRiskKpis(String tenantId);
}