package com.aidentx.iam.access_roles_management.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Join entity representing parent-child relationships between access roles.
 * <p>
 * Enables role nesting — for example, a business role "Finance Manager" can
 * contain technical roles "SAP Finance Access" and "Reporting Access" as children.
 * This supports the tree hierarchy rendered in {@code RoleHierarchyTree} on the frontend.
 * </p>
 */
@Entity
@Table(name = "access_role_compositions", indexes = {
        @Index(name = "idx_role_comp_parent", columnList = "parent_role_id"),
        @Index(name = "idx_role_comp_child", columnList = "child_role_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_composition", columnNames = {"parent_role_id", "child_role_id"})
})
public class AccessRoleComposition {

    /** Unique identifier for this composition link. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The parent role in the hierarchy. */
    @NotNull
    @Column(name = "parent_role_id", nullable = false)
    private UUID parentRoleId;

    /** The child role nested under the parent. */
    @NotNull
    @Column(name = "child_role_id", nullable = false)
    private UUID childRoleId;

    /** Timestamp when this composition link was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getParentRoleId() { return parentRoleId; }
    public void setParentRoleId(UUID parentRoleId) { this.parentRoleId = parentRoleId; }

    public UUID getChildRoleId() { return childRoleId; }
    public void setChildRoleId(UUID childRoleId) { this.childRoleId = childRoleId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

