package com.aidentx.iam.access_roles_management.model.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing a single access role returned to the frontend.
 */
public class AccessRoleResponse {

    private UUID id;
    private String tenantId;
    private String name;
    private String displayName;
    private String description;
    private String type;
    private String status;
    private String businessUnit;
    private UUID organization;
    private String organizationName;
    private String businessFunction;

    /** Governance classifications (e.g., "SOX", "PII", "GDPR"). */
    private List<String> classification;

    /** Compliance tags (e.g., "HIPAA", "ISO-27001", "SOC2"). */
    private List<String> complianceTags;

    private String riskLevel;
    private String riskReason;
    private UUID ownerId;
    private Boolean requestable;
    private Boolean requireApproval;
    private String certificationSchedule;
    /** Number of entitlements linked to this role. */
    private long entitlementCount;
    private Instant createdAt;
    private Instant updatedAt;


    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }

    public UUID getOrganization() { return organization; }
    public void setOrganization(UUID organization) { this.organization = organization; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public String getBusinessFunction() { return businessFunction; }
    public void setBusinessFunction(String businessFunction) { this.businessFunction = businessFunction; }

    public List<String> getClassification() { return classification; }
    public void setClassification(List<String> classification) { this.classification = classification; }

    public List<String> getComplianceTags() { return complianceTags; }
    public void setComplianceTags(List<String> complianceTags) { this.complianceTags = complianceTags; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getRiskReason() { return riskReason; }
    public void setRiskReason(String riskReason) { this.riskReason = riskReason; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public Boolean getRequestable() { return requestable; }
    public void setRequestable(Boolean requestable) { this.requestable = requestable; }

    public Boolean getRequireApproval() { return requireApproval; }
    public void setRequireApproval(Boolean requireApproval) { this.requireApproval = requireApproval; }

    public long getEntitlementCount() { return entitlementCount; }
    public void setEntitlementCount(long entitlementCount) { this.entitlementCount = entitlementCount; }

    public String getCertificationSchedule() { return certificationSchedule; }
    public void setCertificationSchedule(String certificationSchedule) { this.certificationSchedule = certificationSchedule; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}