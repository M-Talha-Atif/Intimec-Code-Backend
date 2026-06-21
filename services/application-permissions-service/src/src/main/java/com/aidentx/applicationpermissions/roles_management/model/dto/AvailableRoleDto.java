package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.util.UUID;

/**
 * DTO for available roles with user count information.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class AvailableRoleDto {
    
    private UUID id;
    private String roleName;
    private String role;
    private long userCount;

    // Constructors
    public AvailableRoleDto() {}

    public AvailableRoleDto(UUID id, String roleName, String role, long userCount) {
        this.id = id;
        this.roleName = roleName;
        this.role = role;
        this.userCount = userCount;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getUserCount() {
        return userCount;
    }

    public void setUserCount(long userCount) {
        this.userCount = userCount;
    }

    @Override
    public String toString() {
        return "AvailableRoleDto{" +
                "id=" + id +
                ", roleName='" + roleName + '\'' +
                ", role='" + role + '\'' +
                ", userCount=" + userCount +
                '}';
    }
}