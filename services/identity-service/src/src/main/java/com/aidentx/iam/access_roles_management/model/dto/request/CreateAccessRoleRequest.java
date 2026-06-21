package com.aidentx.iam.access_roles_management.model.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new access role via the Add Role Wizard.
 */
public class CreateAccessRoleRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    private String displayName;

    private String description;

    private String type;

    private String businessUnit;

    private UUID organization;

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

    /** List of entitlement IDs to attach to this role. */
    private List<UUID> entitlementIds;

    /** List of child role IDs to nest under this role. */
    private List<UUID> childRoleIds;

    private String status;

    // --- Getters and Setters ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }

    public UUID getOrganization() { return organization; }
    public void setOrganization(UUID organization) { this.organization = organization; }

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

    public String getCertificationSchedule() { return certificationSchedule; }
    public void setCertificationSchedule(String certificationSchedule) { this.certificationSchedule = certificationSchedule; }

    public List<UUID> getEntitlementIds() { return entitlementIds; }
    public void setEntitlementIds(List<UUID> entitlementIds) { this.entitlementIds = entitlementIds; }

    public List<UUID> getChildRoleIds() { return childRoleIds; }
    public void setChildRoleIds(List<UUID> childRoleIds) { this.childRoleIds = childRoleIds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}