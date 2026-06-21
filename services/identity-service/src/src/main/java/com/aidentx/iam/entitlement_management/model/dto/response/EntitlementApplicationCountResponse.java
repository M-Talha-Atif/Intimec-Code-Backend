package com.aidentx.iam.entitlement_management.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Entitlement Application Count Analytics.
 * Provides entitlement count for each application within a tenant.
 */
public class EntitlementApplicationCountResponse {

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("applications")
    private List<ApplicationEntitlementCount> applications;

    @JsonProperty("timestamp")
    private long timestamp;

    public EntitlementApplicationCountResponse() {
        this.applications = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public EntitlementApplicationCountResponse(String tenantId) {
        this.tenantId = tenantId;
        this.applications = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public List<ApplicationEntitlementCount> getApplications() {
        return applications;
    }

    public void setApplications(List<ApplicationEntitlementCount> applications) {
        this.applications = applications;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "EntitlementApplicationCountResponse{" +
                "tenantId='" + tenantId + '\'' +
                ", applications=" + applications +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Nested DTO for individual application entitlement count
     */
    public static class ApplicationEntitlementCount {

        @JsonProperty("applicationId")
        private String applicationId;

        @JsonProperty("applicationName")
        private String applicationName;

        @JsonProperty("entitlementCount")
        private long entitlementCount;

        public ApplicationEntitlementCount() {
        }

        public ApplicationEntitlementCount(String applicationId, String applicationName, long entitlementCount) {
            this.applicationId = applicationId;
            this.applicationName = applicationName;
            this.entitlementCount = entitlementCount;
        }

        // Getters and Setters
        public String getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(String applicationId) {
            this.applicationId = applicationId;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public long getEntitlementCount() {
            return entitlementCount;
        }

        public void setEntitlementCount(long entitlementCount) {
            this.entitlementCount = entitlementCount;
        }

        @Override
        public String toString() {
            return "ApplicationEntitlementCount{" +
                    "applicationId='" + applicationId + '\'' +
                    ", applicationName='" + applicationName + '\'' +
                    ", entitlementCount=" + entitlementCount +
                    '}';
        }
    }
}

