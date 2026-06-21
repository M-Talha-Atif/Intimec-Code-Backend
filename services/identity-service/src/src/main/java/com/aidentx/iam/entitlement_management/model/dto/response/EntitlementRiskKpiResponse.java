package com.aidentx.iam.entitlement_management.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Entitlement Risk KPI Analytics.
 * Provides metrics for tracking entitlement risk posture within a tenant.
 *
 * This is specifically for Risk KPI analytics - differentiated from other KPI types.
 *
 * KPI Definitions:
 * - highRiskEntitlements: Count of entitlements with HIGH risk level (real)
 * - sodExposure: Count of SoD exposure entitlements (currently mock - returns 0)
 * - complianceTagged: Count of compliance tagged entitlements (currently mock - returns 0)
 * - dataSensitive: Count of data sensitive entitlements (currently mock - returns 0)
 */
public class EntitlementRiskKpiResponse {

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("highRiskEntitlements")
    private long highRiskEntitlements;

    @JsonProperty("sodExposure")
    private long sodExposure;

    @JsonProperty("complianceTagged")
    private long complianceTagged;

    @JsonProperty("dataSensitive")
    private long dataSensitive;

    @JsonProperty("timestamp")
    private long timestamp;

    public EntitlementRiskKpiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public EntitlementRiskKpiResponse(String tenantId) {
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

    public long getSodExposure() {
        return sodExposure;
    }

    public void setSodExposure(long sodExposure) {
        this.sodExposure = sodExposure;
    }

    public long getComplianceTagged() {
        return complianceTagged;
    }

    public void setComplianceTagged(long complianceTagged) {
        this.complianceTagged = complianceTagged;
    }

    public long getDataSensitive() {
        return dataSensitive;
    }

    public void setDataSensitive(long dataSensitive) {
        this.dataSensitive = dataSensitive;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "EntitlementRiskKpiResponse{" +
                "tenantId='" + tenantId + '\'' +
                ", highRiskEntitlements=" + highRiskEntitlements +
                ", sodExposure=" + sodExposure +
                ", complianceTagged=" + complianceTagged +
                ", dataSensitive=" + dataSensitive +
                ", timestamp=" + timestamp +
                '}';
    }
}