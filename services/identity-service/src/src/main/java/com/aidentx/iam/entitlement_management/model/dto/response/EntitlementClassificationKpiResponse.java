package com.aidentx.iam.entitlement_management.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Entitlement Classification KPI Analytics.
 * Provides metrics for tracking entitlement classification and governance gaps within a tenant.
 *
 * This is specifically for Classification KPI analytics - differentiated from other KPI types.
 *
 * KPI Definitions:
 * - ownershipGaps: Count of entitlements without owners (ownerId = NULL)
 * - unclassified: Count of entitlements without classification labels (currently mock - returns 0)
 * - requestabilityRate: Percentage of requestable entitlements (currently mock - returns 0%)
 * - requestableEntitlements: Count of entitlements marked as requestable (requestable = true)
 */
public class EntitlementClassificationKpiResponse {

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("ownershipGaps")
    private long ownershipGaps;

    @JsonProperty("unclassified")
    private long unclassified;

    @JsonProperty("requestabilityRate")
    private double requestabilityRate;

    @JsonProperty("requestableEntitlements")
    private long requestableEntitlements;

    @JsonProperty("timestamp")
    private long timestamp;

    public EntitlementClassificationKpiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public EntitlementClassificationKpiResponse(String tenantId) {
        this.tenantId = tenantId;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public long getOwnershipGaps() {
        return ownershipGaps;
    }

    public void setOwnershipGaps(long ownershipGaps) {
        this.ownershipGaps = ownershipGaps;
    }

    public long getUnclassified() {
        return unclassified;
    }

    public void setUnclassified(long unclassified) {
        this.unclassified = unclassified;
    }

    public double getRequestabilityRate() {
        return requestabilityRate;
    }

    public void setRequestabilityRate(double requestabilityRate) {
        this.requestabilityRate = requestabilityRate;
    }

    public long getRequestableEntitlements() {
        return requestableEntitlements;
    }

    public void setRequestableEntitlements(long requestableEntitlements) {
        this.requestableEntitlements = requestableEntitlements;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "EntitlementClassificationKpiResponse{" +
                "tenantId='" + tenantId + '\'' +
                ", ownershipGaps=" + ownershipGaps +
                ", unclassified=" + unclassified +
                ", requestabilityRate=" + requestabilityRate +
                ", requestableEntitlements=" + requestableEntitlements +
                ", timestamp=" + timestamp +
                '}';
    }
}

