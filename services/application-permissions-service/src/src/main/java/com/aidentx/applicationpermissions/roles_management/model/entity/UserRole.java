package com.aidentx.applicationpermissions.roles_management.model.entity;

import com.aidentx.applicationpermissions.roles_management.enums.AssignmentType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing the association between Users and Roles.
 * 
 * <p>This is a junction table that establishes the many-to-many relationship
 * between users and roles in the role-based access control (RBAC) system.
 * Each record represents a role assignment to a specific user.</p>
 * 
 * <p>Through this entity, users can be assigned multiple roles, and roles
 * can be assigned to multiple users, providing flexible permission management.</p>
 * 
 * <p>The User reference points to the auth-service User entity, maintaining
 * proper separation of concerns between the authentication and authorization
 * domains.</p>
 * 
 * <p>Each assignment is tagged with an {@link AssignmentType} to indicate
 * whether it was created manually by an administrator or automatically through
 * dynamic rule evaluation.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "user_role")
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Reference to the batch job if this assignment was part of a bulk operation.
     * NULL for manual single assignments.
     */
    @Column(name = "batch_id")
    private UUID batchId;

    /**
     * Timestamp when this role was assigned.
     */
    @Column(name = "assigned_at")
    private Instant assignedAt;

    /**
     * Number of days this assignment is valid (NULL = permanent).
     * Used for temporary access grants.
     */
    @Column(name = "validity_days")
    private Integer validityDays;

    /**
     * The type of assignment indicating how this role was assigned.
     *
     * <p>MANUAL indicates direct assignment by an administrator.
     * DYNAMIC indicates automatic assignment through dynamic rules.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false)
    private AssignmentType assignmentType;

    // Constructors
    public UserRole() {
    }

    // constructor with batchId and validityDays
    public UserRole(UUID userId, UUID roleId, String tenantId, AssignmentType assignmentType,
                    UUID batchId, Integer validityDays) {
        this.userId = userId;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.assignmentType = assignmentType;
        this.batchId = batchId;
        this.assignedAt = Instant.now();
        this.validityDays = validityDays;
    }


    // without batchId and validityDays
    public UserRole(UUID userId, UUID roleId, String tenantId, AssignmentType assignmentType
                   ) {
        this.userId = userId;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.assignmentType = assignmentType;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Defaults to MANUAL assignment type.
     *
     * @deprecated Use {@link #UserRole(UUID, UUID, String, AssignmentType)} instead.
     */
    @Deprecated
    public UserRole(UUID userId, UUID roleId, String tenantId) {
        this(userId, roleId, tenantId, AssignmentType.MANUAL);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public AssignmentType getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(AssignmentType assignmentType) {
        this.assignmentType = assignmentType;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Integer getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(Integer validityDays) {
        this.validityDays = validityDays;

    }


}