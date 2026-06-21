package com.aidentx.iam.access_roles_management.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Join entity representing the many-to-many relationship between
 * {@link AccessRole} and entitlements.
 * <p>
 * Each record links one role to one entitlement from the entitlement catalog.
 * The entitlement_id references the entitlement table managed by the
 * entitlement_management package.
 * </p>
 */
@Entity
@Table(name = "access_role_entitlements", indexes = {
        @Index(name = "idx_role_entitlements_role", columnList = "role_id"),
        @Index(name = "idx_role_entitlements_entitlement", columnList = "entitlement_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_entitlement", columnNames = {"role_id", "entitlement_id"})
})
public class AccessRoleEntitlement {

    /** Unique identifier for this role-entitlement link. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The role that contains this entitlement. */
    @NotNull
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    /** The entitlement assigned to this role (references entitlement catalog). */
    @NotNull
    @Column(name = "entitlement_id", nullable = false)
    private UUID entitlementId;

    /** Timestamp when this link was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public UUID getEntitlementId() { return entitlementId; }
    public void setEntitlementId(UUID entitlementId) { this.entitlementId = entitlementId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

