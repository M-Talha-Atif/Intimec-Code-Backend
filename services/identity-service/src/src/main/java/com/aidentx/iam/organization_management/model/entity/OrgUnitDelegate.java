package com.aidentx.iam.organization_management.model.entity;

import com.aidentx.iam.organization_management.model.enums.DelegateRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/** Delegate of an {@link OrgUnit} — admin / reviewer / approver. */
@Entity
@Table(name = "org_unit_delegates", indexes = {
        @Index(name = "idx_delegate_org_unit", columnList = "org_unit_id,delegate_role"),
        @Index(name = "idx_delegate_tenant", columnList = "tenant_id")
})
public class OrgUnitDelegate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @NotNull
    @Column(name = "org_unit_id", nullable = false)
    private UUID orgUnitId;

    @NotNull
    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    /** Cached display name of the delegate identity. */
    @Column(name = "identity_display_name", length = 255)
    private String identityDisplayName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "delegate_role", nullable = false, length = 16)
    private DelegateRole delegateRole;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @PrePersist
    void onCreate() {
        if (assignedAt == null) assignedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(UUID orgUnitId) { this.orgUnitId = orgUnitId; }

    public UUID getIdentityId() { return identityId; }
    public void setIdentityId(UUID identityId) { this.identityId = identityId; }

    public String getIdentityDisplayName() { return identityDisplayName; }
    public void setIdentityDisplayName(String identityDisplayName) { this.identityDisplayName = identityDisplayName; }

    public DelegateRole getDelegateRole() { return delegateRole; }
    public void setDelegateRole(DelegateRole delegateRole) { this.delegateRole = delegateRole; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public UUID getAssignedBy() { return assignedBy; }
    public void setAssignedBy(UUID assignedBy) { this.assignedBy = assignedBy; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public UUID getRevokedBy() { return revokedBy; }
    public void setRevokedBy(UUID revokedBy) { this.revokedBy = revokedBy; }
}
