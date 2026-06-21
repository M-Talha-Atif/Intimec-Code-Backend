package com.aidentx.applicationpermissions.roles_management.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response wrapper for owned roles query.
 * 
 * @author AidentX Development Team
 * @version 1.0
 */
@Schema(description = "Response containing list of roles owned by the current user")
public class OwnedRolesResponse {

    @Schema(description = "List of roles owned by the current user")
    private List<OwnedRoleDTO> roles;

    @Schema(description = "Total count of owned roles", example = "5")
    private int totalCount;

    @Schema(description = "Success status", example = "true")
    private boolean success;

    @Schema(description = "Response message", example = "Owned roles retrieved successfully")
    private String message;

    // Constructors
    public OwnedRolesResponse() {}

    public OwnedRolesResponse(List<OwnedRoleDTO> roles, int totalCount, boolean success, String message) {
        this.roles = roles;
        this.totalCount = totalCount;
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public List<OwnedRoleDTO> getRoles() {
        return roles;
    }

    public void setRoles(List<OwnedRoleDTO> roles) {
        this.roles = roles;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
