package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementApplicationCountResponse;

/**
 * Service interface for Entitlement Application Count Analytics.
 * Provides business logic for retrieving entitlement counts per application
 * within a tenant.
 */
public interface EntitlementApplicationCountService {

    /**
     * Retrieves entitlement count for each application within a tenant.
     *
     * Returns a list of applications with their corresponding entitlement counts.
     * Application name is fetched from application client.
     * Entitlement count is aggregated across all resources for each application.
     *
     * @param tenantId the tenant ID to filter by (from X-Tenant-ID header)
     * @return populated response DTO with applications and their entitlement counts
     * @throws IllegalArgumentException if tenantId is invalid
     */
    EntitlementApplicationCountResponse getApplicationEntitlementCounts(String tenantId);
}

