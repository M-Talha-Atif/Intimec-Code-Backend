package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for the get role owners endpoint.
 * 
 * <p>Contains all users in the tenant with their ownership status
 * for a specific role. Users who are owners are shown first, followed by
 * users who are not owners.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Response containing users with their role ownership status")
public class GetRoleOwnersResponse {

    @Schema(description = "List of users with ownership status")
    @JsonProperty("users")
    private List<UserWithOwnershipDto> users;

    @Schema(description = "Total number of users", example = "10")
    @JsonProperty("totalCount")
    private Integer totalCount;

    @Schema(description = "Number of users who are owners", example = "3")
    @JsonProperty("usersWithOwnershipCount")
    private Integer usersWithOwnershipCount;

    @Schema(description = "Number of users who are not owners", example = "7")
    @JsonProperty("usersWithoutOwnershipCount")
    private Integer usersWithoutOwnershipCount;

    @Schema(description = "The role ID that was queried", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("roleId")
    private String roleId;

    @Schema(description = "Tenant ID", example = "tenant_001")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Indicates if the operation was successful", example = "true")
    @JsonProperty("success")
    private Boolean success;

    @Schema(description = "Response message", example = "Role owners retrieved successfully")
    @JsonProperty("message")
    private String message;

    /**
     * Default constructor.
     */
    public GetRoleOwnersResponse() {
    }

    /**
     * Constructor for successful response.
     * 
     * @param users list of users with ownership status
     * @param totalCount total number of users
     * @param usersWithOwnershipCount number of users who are owners
     * @param usersWithoutOwnershipCount number of users who are not owners
     * @param roleId the role ID that was queried
     * @param tenantId the tenant ID
     * @param message success message
     */
    public GetRoleOwnersResponse(List<UserWithOwnershipDto> users, Integer totalCount, 
                                 Integer usersWithOwnershipCount, Integer usersWithoutOwnershipCount,
                                 String roleId, String tenantId, String message) {
        this.users = users;
        this.totalCount = totalCount;
        this.usersWithOwnershipCount = usersWithOwnershipCount;
        this.usersWithoutOwnershipCount = usersWithoutOwnershipCount;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.success = true;
        this.message = message;
    }

    /**
     * Constructor for error response.
     * 
     * @param roleId the role ID that was queried
     * @param tenantId the tenant ID
     * @param message error message
     */
    public GetRoleOwnersResponse(String roleId, String tenantId, String message) {
        this.users = null;
        this.totalCount = 0;
        this.usersWithOwnershipCount = 0;
        this.usersWithoutOwnershipCount = 0;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.success = false;
        this.message = message;
    }

    // Getters and Setters

    public List<UserWithOwnershipDto> getUsers() {
        return users;
    }

    public void setUsers(List<UserWithOwnershipDto> users) {
        this.users = users;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getUsersWithOwnershipCount() {
        return usersWithOwnershipCount;
    }

    public void setUsersWithOwnershipCount(Integer usersWithOwnershipCount) {
        this.usersWithOwnershipCount = usersWithOwnershipCount;
    }

    public Integer getUsersWithoutOwnershipCount() {
        return usersWithoutOwnershipCount;
    }

    public void setUsersWithoutOwnershipCount(Integer usersWithoutOwnershipCount) {
        this.usersWithoutOwnershipCount = usersWithoutOwnershipCount;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "GetRoleOwnersResponse{" +
                "users=" + users +
                ", totalCount=" + totalCount +
                ", usersWithOwnershipCount=" + usersWithOwnershipCount +
                ", usersWithoutOwnershipCount=" + usersWithoutOwnershipCount +
                ", roleId='" + roleId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
