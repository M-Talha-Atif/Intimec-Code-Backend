package com.aidentx.iam.organization_management.model.entity;

import com.aidentx.iam.organization_management.model.enums.AssignmentType;
import com.aidentx.iam.organization_management.model.enums.HierarchyType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Association of an {@code Identity} with an {@link OrgUnit}.
 *
 * <p>An identity can hold multiple assignments (one primary per
 * {@link HierarchyType}, plus dotted-line / project / temporary lines).
 * The pre-existing {@code Employee.department} string is treated as a
 * read-only denormalized view of the primary FUNCTIONAL assignment and
 * is updated by a service event when this table changes.
 */
@Entity
@Table(name = "identity_org_assignments", indexes = {
        @Index(name = "idx_ioa_org_unit", columnList = "org_unit_id,hierarchy_type"),
        @Index(name = "idx_ioa_identity", columnList = "identity_id"),
        @Index(name = "idx_ioa_tenant", columnList = "tenant_id")
})
public class IdentityOrgAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @NotNull
    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @NotNull
    @Column(name = "org_unit_id", nullable = false)
    private UUID orgUnitId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "hierarchy_type", nullable = false, length = 32)
    private HierarchyType hierarchyType = HierarchyType.FUNCTIONAL;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 32)
    private AssignmentType assignmentType = AssignmentType.HR_OF_RECORD;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem = "MANUAL";

    @Column(name = "external_id", length = 255)
    private String externalId;

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
    void onUpdate() { updatedAt = Instant.now(); }

    // --- Getters / Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getIdentityId() { return identityId; }
    public void setIdentityId(UUID identityId) { this.identityId = identityId; }

    public UUID getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(UUID orgUnitId) { this.orgUnitId = orgUnitId; }

    public HierarchyType getHierarchyType() { return hierarchyType; }
    public void setHierarchyType(HierarchyType hierarchyType) { this.hierarchyType = hierarchyType; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }

    public AssignmentType getAssignmentType() { return assignmentType; }
    public void setAssignmentType(AssignmentType assignmentType) { this.assignmentType = assignmentType; }

    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }

    public Instant getValidTo() { return validTo; }
    public void setValidTo(Instant validTo) { this.validTo = validTo; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
