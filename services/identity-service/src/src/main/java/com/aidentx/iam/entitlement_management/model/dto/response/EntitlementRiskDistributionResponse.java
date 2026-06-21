package com.aidentx.iam.entitlement_management.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Entitlement Risk Distribution Analytics.
 * Provides entitlement counts grouped by risk level within a tenant.
 *
 * This is specifically for Risk Distribution analytics - differentiated from KPI summary types.
 *
 * Definitions:
 * - highRiskEntitlements: Count of entitlements with HIGH risk level
 * - mediumRiskEntitlements: Count of entitlements with MEDIUM risk level
 * - lowRiskEntitlements: Count of entitlements with LOW risk level
 */
public class EntitlementRiskDistributionResponse {

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("highRiskEntitlements")
    private long highRiskEntitlements;

    @JsonProperty("mediumRiskEntitlements")
    private long mediumRiskEntitlements;

    @JsonProperty("lowRiskEntitlements")
    private long lowRiskEntitlements;

    @JsonProperty("timestamp")
    private long timestamp;

    public EntitlementRiskDistributionResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public EntitlementRiskDistributionResponse(String tenantId) {
        this.tenantId = tenantId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public long getHighRiskEntitlements() {
        return highRiskEntitlements;
    }

    public void setHighRiskEntitlements(long highRiskEntitlements) {
        this.highRiskEntitlements = highRiskEntitlements;
    }

    public long getMediumRiskEntitlements() {
        return mediumRiskEntitlements;
    }

    public void setMediumRiskEntitlements(long mediumRiskEntitlements) {
        this.mediumRiskEntitlements = mediumRiskEntitlements;
    }

    public long getLowRiskEntitlements() {
        return lowRiskEntitlements;
    }

    public void setLowRiskEntitlements(long lowRiskEntitlements) {
        this.lowRiskEntitlements = lowRiskEntitlements;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "EntitlementRiskDistributionResponse{" +
                "tenantId='" + tenantId + '\'' +
                ", highRiskEntitlements=" + highRiskEntitlements +
                ", mediumRiskEntitlements=" + mediumRiskEntitlements +
                ", lowRiskEntitlements=" + lowRiskEntitlements +
                ", timestamp=" + timestamp +
                '}';
    }
}