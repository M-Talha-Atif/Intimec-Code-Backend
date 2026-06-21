package com.aidentx.applicationpermissions.roles_management.model.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity representing the association between Roles and Permissions.
 * 
 * <p>This is a junction table that establishes the many-to-many relationship
 * between roles and permissions in the role-based access control (RBAC) system.
 * Each record grants a specific permission to a role.</p>
 * 
 * <p>This entity enables:</p>
 * <ul>
 *   <li>Flexible permission assignment to roles</li>
 *   <li>Easy role modification without affecting other roles</li>
 *   <li>Granular control over what each role can access</li>
 *   <li>Efficient permission inheritance through role assignments</li>
 * </ul>
 * 
 * <p>When a user is assigned a role (via UserRole), they inherit all
 * permissions associated with that role through this entity.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "role_permission")
public class RolePermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "role_id", nullable = false)
    private UUID roleId;
    
    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

    // Constructors
    public RolePermission() {}

    public RolePermission(UUID roleId, UUID permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
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

    public UUID getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(UUID permissionId) {
        this.permissionId = permissionId;
    }
}