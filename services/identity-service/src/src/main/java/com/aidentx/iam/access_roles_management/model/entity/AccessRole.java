package com.aidentx.iam.access_roles_management.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a governed access role in the IGA access catalog.
 * <p>
 * An access role is a container of entitlements that represents a logical grouping
 * of permissions. Roles can be of type business, technical, functional, or application,
 * and support hierarchical nesting through {@link AccessRoleComposition}.
 * </p>
 * <p>
 * All categorical fields (type, status, riskLevel, etc.) are stored as VARCHAR
 * to allow flexible, tenant-driven configuration without requiring code changes
 * when new categories are introduced.
 * </p>
 *
 * @see AccessRoleEntitlement
 * @see AccessRoleComposition
 */
@Entity
@Table(name = "access_roles", indexes = {
        @Index(name = "idx_access_roles_tenant_name", columnList = "tenant_id, name", unique = true),
        @Index(name = "idx_access_roles_tenant_type", columnList = "tenant_id, type"),
        @Index(name = "idx_access_roles_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_access_roles_tenant_organization", columnList = "tenant_id, organization_id"),
        @Index(name = "idx_access_roles_owner", columnList = "owner_id")
})
public class AccessRole {

    /** Unique identifier for this role. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The tenant this role belongs to (multi-tenancy support). */
    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** Human-readable name of the role (e.g., "Finance Manager"). */
    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    /** Optional display name for UI rendering. */
    @Column(name = "display_name")
    private String displayName;

    /** Detailed description of what this role represents and grants. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * The classification type of this role.
     * Expected values: business, technical, functional, application.
     * Stored as VARCHAR to allow tenant-driven extensibility.
     */
    @Column(name = "type")
    private String type = "No type";

    /**
     * Current lifecycle status of this role.
     * Expected values: draft, active, under_review, deprecated, archived.
     */
    @NotBlank
    @Column(name = "status", nullable = false)
    private String status = "enabled";

    /** The business unit this role is associated with. */
    @Column(name = "business_unit")
    private String businessUnit = "No business unit";

    /** Organization unit this role belongs to. */
    @Column(name = "organization_id")
    private UUID organization;

    /** The business function this role belongs to (e.g., "Finance", "HR", "IT"). */
    @Column(name = "business_function")
    private String businessFunction;

    /**
     * Governance classifications (e.g., "SOX", "PII", "GDPR", "High Risk").
     * Stored as JSONB array for flexible multi-label classification.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "classification", columnDefinition = "jsonb")
    private List<String> classification;

    /**
     * Compliance tags associated with this role (e.g., "HIPAA", "ISO-27001", "SOC2").
     * Stored as JSONB array for flexible multi-label tagging.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "compliance_tags", columnDefinition = "jsonb")
    private List<String> complianceTags;

    /**
     * Risk level assigned to this role.
     * Expected values: low, medium, high, critical.
     */
    @Column(name = "risk_level")
    private String riskLevel;

    /** Free-text justification for the assigned risk level. */
    @Column(name = "risk_reason", columnDefinition = "TEXT")
    private String riskReason;

    /** Identity ID of the role owner responsible for governance. */
    @Column(name = "owner_id")
    private UUID ownerId;

    /** Whether this role can be requested through the access request catalog. */
    @NotNull
    @Column(name = "requestable", nullable = false)
    private Boolean requestable = false;

    /** Whether assignment of this role requires an approval workflow. */
    @NotNull
    @Column(name = "require_approval", nullable = false)
    private Boolean requireApproval = false;

    /** Cron expression or duration for certification schedule (e.g., "QUARTERLY"). */
    @Column(name = "certification_schedule")
    private String certificationSchedule;

    /** Timestamp when this role was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp when this role was last updated. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}