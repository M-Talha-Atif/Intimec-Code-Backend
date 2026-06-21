package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.enums.RiskLevel;
import com.aidentx.applicationpermissions.roles_management.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * DTO representing a role owned by the current user.
 * Used for responsibilities dashboard "My Internal Roles" section.
 * 
 * @author AidentX Development Team
 * @version 1.0
 */
@Schema(description = "Role owned by the current user with associated permissions and metadata")
public class OwnedRoleDTO {

    @Schema(description = "Unique identifier of the role", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Human-readable name of the role", example = "System Administrator")
    private String name;

    @Schema(description = "Role type indicating hierarchy level", 
            example = "ADMINISTRATOR",
            allowableValues = {"SYSTEM", "ADMINISTRATOR", "REVIEWER", "OPERATOR", "VIEWER"})
    private RoleType roleType;

    @Schema(description = "Descriptive scope based on role type", 
            example = "system",
            allowableValues = {"system", "administrator", "reviewer", "operator", "viewer"})
    private String scope;

    @Schema(description = "List of permission identifiers associated with this role")
    private List<String> permissions;

    @Schema(description = "Whether this is a critical/sensitive role requiring special oversight", example = "true")
    private boolean isCritical;

    @Schema(description = "ISO 8601 timestamp of last modification", example = "2026-02-08T10:30:00Z")
    private String lastModified;

    @Schema(description = "Risk level of the role", example = "HIGH")
    private RiskLevel riskLevel;

    @Schema(description = "Number of users currently assigned to this role", example = "12")
    private Long userCount;

    @Schema(description = "Number of permissions assigned to this role", example = "45")
    private Long permissionCount;

    @Schema(description = "Whether the role is currently active", example = "true")
    private Boolean isActive;

    // Constructors
    public OwnedRoleDTO() {}

    public OwnedRoleDTO(UUID id, String name, RoleType roleType, String scope, 
                       List<String> permissions, boolean isCritical, String lastModified,
                       RiskLevel riskLevel, Long userCount, Long permissionCount, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.roleType = roleType;
        this.scope = scope;
        this.permissions = permissions;
        this.isCritical = isCritical;
        this.lastModified = lastModified;
        this.riskLevel = riskLevel;
        this.userCount = userCount;
        this.permissionCount = permissionCount;
        this.isActive = isActive;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public Long getPermissionCount() {
        return permissionCount;
    }

    public void setPermissionCount(Long permissionCount) {
        this.permissionCount = permissionCount;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
