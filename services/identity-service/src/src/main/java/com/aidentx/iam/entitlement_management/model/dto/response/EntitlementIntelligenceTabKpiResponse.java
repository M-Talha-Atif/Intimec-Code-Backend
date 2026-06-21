package com.aidentx.iam.entitlement_management.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Entitlement Intelligence Tab KPI Analytics.
 * Provides comprehensive metrics for entitlement governance and risk assessment
 * across all resources within a tenant.
 *
 * This is specifically for Intelligence Tab KPI analytics - differentiated from other KPI types.
 * 
 * KPI Definitions:
 * - totalEntitlements: Sum of all entitlements across all resources
 * - activeEntitlements: Entitlements with lifecycle = "Active"
 * - orphanedEntitlements: Entitlements where ownerId is NULL
 * - unclassifiedEntitlements: Entitlements where classification list is empty or NULL
 * - highRiskEntitlements: Entitlements where riskLevel = "High"
 * - averageUsersPerEntitlement: Total assignments / total entitlements
 */
public class EntitlementIntelligenceTabKpiResponse {

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("totalEntitlements")
    private long totalEntitlements;

    @JsonProperty("activeEntitlements")
    private long activeEntitlements;

    @JsonProperty("orphanedEntitlements")
    private long orphanedEntitlements;

    @JsonProperty("unclassifiedEntitlements")
    private long unclassifiedEntitlements;

    @JsonProperty("highRiskEntitlements")
    private long highRiskEntitlements;

    @JsonProperty("averageUsersPerEntitlement")
    private double averageUsersPerEntitlement;

    @JsonProperty("timestamp")
    private long timestamp;

    public EntitlementIntelligenceTabKpiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public EntitlementIntelligenceTabKpiResponse(String tenantId) {
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

    public long getTotalEntitlements() {
        return totalEntitlements;
    }

    public void setTotalEntitlements(long totalEntitlements) {
        this.totalEntitlements = totalEntitlements;
    }

    public long getActiveEntitlements() {
        return activeEntitlements;
    }

    public void setActiveEntitlements(long activeEntitlements) {
        this.activeEntitlements = activeEntitlements;
    }

    public long getOrphanedEntitlements() {
        return orphanedEntitlements;
    }

    public void setOrphanedEntitlements(long orphanedEntitlements) {
        this.orphanedEntitlements = orphanedEntitlements;
    }

    public long getUnclassifiedEntitlements() {
        return unclassifiedEntitlements;
    }

    public void setUnclassifiedEntitlements(long unclassifiedEntitlements) {
        this.unclassifiedEntitlements = unclassifiedEntitlements;
    }

    public long getHighRiskEntitlements() {
        return highRiskEntitlements;
    }

    public void setHighRiskEntitlements(long highRiskEntitlements) {
        this.highRiskEntitlements = highRiskEntitlements;
    }

    public double getAverageUsersPerEntitlement() {
        return averageUsersPerEntitlement;
    }

    public void setAverageUsersPerEntitlement(double averageUsersPerEntitlement) {
        this.averageUsersPerEntitlement = averageUsersPerEntitlement;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "EntitlementIntelligenceTabKpiResponse{" +
                "tenantId='" + tenantId + '\'' +
                ", totalEntitlements=" + totalEntitlements +
                ", activeEntitlements=" + activeEntitlements +
                ", orphanedEntitlements=" + orphanedEntitlements +
                ", unclassifiedEntitlements=" + unclassifiedEntitlements +
                ", highRiskEntitlements=" + highRiskEntitlements +
                ", averageUsersPerEntitlement=" + averageUsersPerEntitlement +
                ", timestamp=" + timestamp +
                '}';
    }
}



