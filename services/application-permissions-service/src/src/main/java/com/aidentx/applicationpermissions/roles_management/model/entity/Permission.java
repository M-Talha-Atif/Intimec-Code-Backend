package com.aidentx.applicationpermissions.roles_management.model.entity;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a Permission in the role-based access control system.
 * 
 * <p>A Permission defines what actions can be performed on specific pages or resources.
 * Each permission is associated with a page and can have multiple conditional rules
 * that determine when the permission is granted.</p>
 * 
 * <p>Permissions are typically granted to roles, which are then assigned to users,
 * providing a flexible hierarchy for access control.</p>
 * 
 * <p>Example permissions:</p>
 * <ul>
 *   <li>Permission to 'VIEW' the 'User Management' page</li>
 *   <li>Permission to 'EDIT' the 'Financial Reports' page with region-based conditions</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "permission")
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    /**
     * ID of the action this permission allows.
     * References the Action entity.
     */
    @Column(name = "action_id", nullable = false)
    private UUID actionId;

    // Constructors
    public Permission() {}
    
    public Permission(UUID actionId) {
        this.actionId = actionId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getActionId() {
        return actionId;
    }
    
    public void setActionId(UUID actionId) {
        this.actionId = actionId;
    }
}