package com.aidentx.applicationpermissions.roles_management.model.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity representing the ownership/management relationship between Users and Roles.
 * 
 * <p>This entity tracks which users are designated as owners or managers of specific roles.
 * Role owners typically have permissions to manage the role assignments, modify role properties,
 * and oversee the role's usage within their tenant.</p>
 * 
 * <p>This provides an additional layer of role management where specific users can be
 * designated as responsible for managing particular roles without needing full administrative
 * privileges across all roles in the system.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "role_owners")
public class RoleOwners {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "role_id", nullable = false)
    private UUID roleId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    // Constructors
    public RoleOwners() {}

    public RoleOwners(UUID roleId, UUID userId) {
        this.roleId = roleId;
        this.userId = userId;
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
    }

    public RoleOwners(UUID roleId, UUID userId, Long createdAt, Long updatedAt) {
        this.roleId = roleId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    @PrePersist
    public void prePersist() {
        long currentTime = System.currentTimeMillis();
        if (this.createdAt == null) {
            this.createdAt = currentTime;
        }
        if (this.updatedAt == null) {
            this.updatedAt = currentTime;
        }
    }
}