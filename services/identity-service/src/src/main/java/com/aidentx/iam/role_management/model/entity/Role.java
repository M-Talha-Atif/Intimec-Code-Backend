package com.aidentx.iam.role_management.model.entity;

import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a Business Role in the IGA platform.
 * A Role is a business-friendly collection of one or more Technical
 * Entitlements.
 * Users are typically assigned Roles, which in turn grant them the underlying
 * access.
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "dgraph_uid")
    private String dgraphUid;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotBlank
    @Column(name = "display_name", nullable = false, unique = true) // Role names should be unique within a tenant
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // --- The Many-to-Many Relationship ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "role_entitlement_mappings", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "entitlement_id"))
    private Set<Entitlement> includedEntitlements = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = Instant.now().toEpochMilli();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now().toEpochMilli();
    }

    // --- Getters and Setters ---
    public String getDgraphUid() {
        return dgraphUid;
    }

    public void setDgraphUid(String dgraphUid) {
        this.dgraphUid = dgraphUid;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Entitlement> getIncludedEntitlements() {
        return includedEntitlements;
    }

    public void setIncludedEntitlements(Set<Entitlement> includedEntitlements) {
        this.includedEntitlements = includedEntitlements;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}