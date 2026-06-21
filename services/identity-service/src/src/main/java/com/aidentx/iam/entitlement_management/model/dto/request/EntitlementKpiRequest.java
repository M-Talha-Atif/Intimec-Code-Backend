package com.aidentx.iam.entitlement_management.model.dto.request;

/**
 * Request DTO for retrieving Entitlement Intelligence Tab KPI Analytics.
 * Tenant ID is provided via X-Tenant-ID header.
 * Retrieves KPI metrics for all entitlements across all resources within the tenant.
 * 
 * This is specifically for Intelligence Tab KPI analytics - differentiated from other KPI types.
 */
public class EntitlementKpiRequest {

    public EntitlementKpiRequest() {
    }

    @Override
    public String toString() {
        return "EntitlementKpiRequest{}";
    }
}

