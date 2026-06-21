package com.aidentx.iam.entitlement_management.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Entitlement Sprawl KPI Analytics.
 * Provides metrics for tracking entitlement sprawl within a tenant.
 *
 * This is specifically for Sprawl KPI analytics - differentiated from other KPI types.
 *
 * KPI Definitions:
 * - sprawlIndex: Average entitlements per user (currently mock - returns 0)
 * - staleEntitlements: Count of deprecated/unused entitlements since start of year (currently mock - returns 0)
 * - overProvisionedCount: Count of entitlements assigned to 100+ users
 * - directAssignmentCount: Count of entitlements with direct manual assignments (source = MANUAL)
 */
public class EntitlementSprawlKpiResponse {

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("sprawlIndex")
    private double sprawlIndex;

    @JsonProperty("staleEntitlements")
    private long staleEntitlements;

    @JsonProperty("overProvisionedCount")
    private long overProvisionedCount;

    @JsonProperty("directAssignmentCount")
    private long directAssignmentCount;

    @JsonProperty("timestamp")
    private long timestamp;

    public EntitlementSprawlKpiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public EntitlementSprawlKpiResponse(String tenantId) {
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

    public double getSprawlIndex() {
        return sprawlIndex;
    }

    public void setSprawlIndex(double sprawlIndex) {
        this.sprawlIndex = sprawlIndex;
    }

    public long getStaleEntitlements() {
        return staleEntitlements;
    }

    public void setStaleEntitlements(long staleEntitlements) {
        this.staleEntitlements = staleEntitlements;
    }

    public long getOverProvisionedCount() {
        return overProvisionedCount;
    }

    public void setOverProvisionedCount(long overProvisionedCount) {
        this.overProvisionedCount = overProvisionedCount;
    }

    public long getDirectAssignmentCount() {
        return directAssignmentCount;
    }

    public void setDirectAssignmentCount(long directAssignmentCount) {
        this.directAssignmentCount = directAssignmentCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "EntitlementSprawlKpiResponse{" +
                "tenantId='" + tenantId + '\'' +
                ", sprawlIndex=" + sprawlIndex +
                ", staleEntitlements=" + staleEntitlements +
                ", overProvisionedCount=" + overProvisionedCount +
                ", directAssignmentCount=" + directAssignmentCount +
                ", timestamp=" + timestamp +
                '}';
    }
}

