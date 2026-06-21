package com.aidentx.iam.organization_management.model.entity;

import com.aidentx.iam.organization_management.model.enums.HierarchyType;
import com.aidentx.iam.organization_management.model.enums.OrgUnitStatus;
import com.aidentx.iam.organization_management.model.enums.OrgUnitType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An Organizational Unit — the canonical hierarchical container used by
 * Identity, Access Roles, governance policies and approvals.
 *
 * <p>The hierarchy is materialized via {@link #parentId} (adjacency list)
 * plus {@link #path} (a slash-delimited string of UUIDs from the root,
 * e.g. {@code /a1b2.../c3d4.../}). The path enables fast subtree queries
 * with a {@code LIKE 'prefix%'} predicate against a B-tree index, without
 * requiring the Postgres {@code ltree} extension.
 *
 * <p>Field set is the strict superset of the frontend {@code OrgNodeData}
 * type ({@code intimec-development-frontend/src/types/organization.ts}).
 * Aggregate fields ({@code userCount}, {@code roleCount}, etc.) and risk
 * derivatives are computed at read-time and never stored on this entity.
 */
@Entity
@Table(name = "org_units", indexes = {
        @Index(name = "idx_org_unit_tenant", columnList = "tenant_id"),
        @Index(name = "idx_org_unit_parent", columnList = "parent_id"),
        @Index(name = "idx_org_unit_path", columnList = "path"),
        @Index(name = "idx_org_unit_tenant_type_status", columnList = "tenant_id,type,status"),
        @Index(name = "idx_org_unit_tenant_hierarchy", columnList = "tenant_id,hierarchy_type"),
        @Index(name = "idx_org_unit_external", columnList = "tenant_id,source_system,external_id")
})
public class OrgUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private OrgUnitType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "hierarchy_type", nullable = false, length = 32)
    private HierarchyType hierarchyType = HierarchyType.FUNCTIONAL;

    @NotBlank
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", length = 64)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "parent_id")
    private UUID parentId;

    /**
     * Materialized path: {@code /<root_uuid>/<...>/<self_uuid>/}.
     * Always begins and ends with {@code '/'} so prefix matches are unambiguous.
     */
    @NotBlank
    @Column(name = "path", nullable = false, length = 4096)
    private String path;

    @Column(name = "depth", nullable = false)
    private int depth;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OrgUnitStatus status = OrgUnitStatus.ACTIVE;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "replaced_by_id")
    private UUID replacedById;

    @Column(name = "head_identity_id")
    private UUID headIdentityId;

    /** Cached display name of the head, refreshed on assignment. */
    @Column(name = "head_display_name", length = 255)
    private String headDisplayName;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem = "MANUAL";

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "is_authoritative", nullable = false)
    private boolean authoritative = false;

    // --- Governance / location enrichment (matches frontend OrgNodeData) ---

    @Column(name = "cost_center", length = 64)
    private String costCenter;

    @Column(name = "legal_entity", length = 255)
    private String legalEntity;

    @Column(name = "region", length = 64)
    private String region;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "time_zone", length = 64)
    private String timeZone;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "building_code", length = 64)
    private String buildingCode;

    @Column(name = "floor", length = 32)
    private String floor;

    @Column(name = "data_classification", length = 16)
    private String dataClassification;

    @Column(name = "compliance_status", length = 16)
    private String complianceStatus;

    @Column(name = "certification_status", length = 16)
    private String certificationStatus;

    @Column(name = "last_audit_date")
    private LocalDate lastAuditDate;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    // --- Financial ---

    @Column(name = "budget", precision = 18, scale = 2)
    private BigDecimal budget;

    @Column(name = "actual_spend", precision = 18, scale = 2)
    private BigDecimal actualSpend;

    @Column(name = "license_cost", precision = 18, scale = 2)
    private BigDecimal licenseCost;

    // --- Free-form ---

    @Type(JsonBinaryType.class)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<String> tags = new ArrayList<>();

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private java.util.Map<String, Object> metadata = new java.util.HashMap<>();

    // --- Audit ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 255, updatable = false)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (validFrom == null) validFrom = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- Getters / Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public OrgUnitType getType() { return type; }
    public void setType(OrgUnitType type) { this.type = type; }

    public HierarchyType getHierarchyType() { return hierarchyType; }
    public void setHierarchyType(HierarchyType hierarchyType) { this.hierarchyType = hierarchyType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public OrgUnitStatus getStatus() { return status; }
    public void setStatus(OrgUnitStatus status) { this.status = status; }

    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }

    public Instant getValidTo() { return validTo; }
    public void setValidTo(Instant validTo) { this.validTo = validTo; }

    public UUID getReplacedById() { return replacedById; }
    public void setReplacedById(UUID replacedById) { this.replacedById = replacedById; }

    public UUID getHeadIdentityId() { return headIdentityId; }
    public void setHeadIdentityId(UUID headIdentityId) { this.headIdentityId = headIdentityId; }

    public String getHeadDisplayName() { return headDisplayName; }
    public void setHeadDisplayName(String headDisplayName) { this.headDisplayName = headDisplayName; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public boolean isAuthoritative() { return authoritative; }
    public void setAuthoritative(boolean authoritative) { this.authoritative = authoritative; }

    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }

    public String getLegalEntity() { return legalEntity; }
    public void setLegalEntity(String legalEntity) { this.legalEntity = legalEntity; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getBuildingCode() { return buildingCode; }
    public void setBuildingCode(String buildingCode) { this.buildingCode = buildingCode; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getDataClassification() { return dataClassification; }
    public void setDataClassification(String dataClassification) { this.dataClassification = dataClassification; }

    public String getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }

    public String getCertificationStatus() { return certificationStatus; }
    public void setCertificationStatus(String certificationStatus) { this.certificationStatus = certificationStatus; }

    public LocalDate getLastAuditDate() { return lastAuditDate; }
    public void setLastAuditDate(LocalDate lastAuditDate) { this.lastAuditDate = lastAuditDate; }

    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(LocalDate nextReviewDate) { this.nextReviewDate = nextReviewDate; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public BigDecimal getActualSpend() { return actualSpend; }
    public void setActualSpend(BigDecimal actualSpend) { this.actualSpend = actualSpend; }

    public BigDecimal getLicenseCost() { return licenseCost; }
    public void setLicenseCost(BigDecimal licenseCost) { this.licenseCost = licenseCost; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags == null ? new ArrayList<>() : tags; }

    public java.util.Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(java.util.Map<String, Object> metadata) {
        this.metadata = metadata == null ? new java.util.HashMap<>() : metadata;
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
