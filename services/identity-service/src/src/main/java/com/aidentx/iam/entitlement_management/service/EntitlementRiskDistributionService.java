package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementRiskDistributionResponse;

/**
 * Service interface for Entitlement Risk Distribution Analytics.
 * Provides business logic for calculating and retrieving entitlement counts
 * grouped by risk level within a tenant.
 *
 * This is specifically for Risk Distribution analytics.
 */
public interface EntitlementRiskDistributionService {

    /**
     * Retrieves risk distribution metrics for a given tenant across all resources.
     *
     * Metrics Calculated:
     * - highRiskEntitlements: Count of entitlements with HIGH risk level
     * - mediumRiskEntitlements: Count of entitlements with MEDIUM risk level
     * - lowRiskEntitlements: Count of entitlements with LOW risk level
     *
     * @param tenantId the tenant ID to filter by (from X-Tenant-ID header)
     * @return populated response DTO with entitlement counts by risk level
     * @throws IllegalArgumentException if tenantId is invalid
     */
    EntitlementRiskDistributionResponse getRiskDistribution(String tenantId);
}