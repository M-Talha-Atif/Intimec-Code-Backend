package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for the get users by role endpoint.
 * 
 * <p>Contains all users in the tenant with their role assignment status
 * for a specific role. Users with the role are shown first, followed by
 * users without the role.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Response containing users with their role assignment status")
public class GetUsersByRoleResponse {

    @Schema(description = "List of users with role assignment status")
    @JsonProperty("users")
    private List<UserWithRoleDto> users;

    @Schema(description = "Total number of users (before pagination)", example = "10")
    @JsonProperty("totalCount")
    private Integer totalCount;

    @Schema(description = "Number of users who have the role", example = "3")
    @JsonProperty("usersWithRoleCount")
    private Integer usersWithRoleCount;

    @Schema(description = "Number of users who don't have the role", example = "7")
    @JsonProperty("usersWithoutRoleCount")
    private Integer usersWithoutRoleCount;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    @JsonProperty("page")
    private Integer page;

    @Schema(description = "Page size", example = "10")
    @JsonProperty("size")
    private Integer size;

    @Schema(description = "Total number of pages", example = "2")
    @JsonProperty("totalPages")
    private Integer totalPages;

    @Schema(description = "Number of users on current page", example = "10")
    @JsonProperty("currentPageSize")
    private Integer currentPageSize;

    @Schema(description = "The role ID that was queried", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("roleId")
    private String roleId;

    @Schema(description = "Tenant ID", example = "tenant_001")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Indicates if the operation was successful", example = "true")
    @JsonProperty("success")
    private Boolean success;

    @Schema(description = "Response message", example = "Users retrieved successfully with role status")
    @JsonProperty("message")
    private String message;

    /**
     * Default constructor.
     */
    public GetUsersByRoleResponse() {
    }

    /**
     * Constructor for successful response.
     * 
     * @param users list of users with role status
     * @param totalCount total number of users
     * @param usersWithRoleCount number of users with the role
     * @param usersWithoutRoleCount number of users without the role
     * @param roleId the role ID that was queried
     * @param tenantId the tenant ID
     * @param message success message
     */
    public GetUsersByRoleResponse(List<UserWithRoleDto> users, Integer totalCount, 
                                 Integer usersWithRoleCount, Integer usersWithoutRoleCount,
                                 String roleId, String tenantId, String message) {
        this.users = users;
        this.totalCount = totalCount;
        this.usersWithRoleCount = usersWithRoleCount;
        this.usersWithoutRoleCount = usersWithoutRoleCount;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.success = true;
        this.message = message;
    }

    /**
     * Constructor for successful response with pagination.
     * 
     * @param users list of users with role status
     * @param totalCount total number of users (before pagination)
     * @param usersWithRoleCount number of users with the role
     * @param usersWithoutRoleCount number of users without the role
     * @param page current page number
     * @param size page size
     * @param totalPages total number of pages
     * @param roleId the role ID that was queried
     * @param tenantId the tenant ID
     * @param message success message
     */
    public GetUsersByRoleResponse(List<UserWithRoleDto> users, Integer totalCount, 
                                 Integer usersWithRoleCount, Integer usersWithoutRoleCount,
                                 Integer page, Integer size, Integer totalPages,
                                 String roleId, String tenantId, String message) {
        this.users = users;
        this.totalCount = totalCount;
        this.usersWithRoleCount = usersWithRoleCount;
        this.usersWithoutRoleCount = usersWithoutRoleCount;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.currentPageSize = users != null ? users.size() : 0;
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
    public GetUsersByRoleResponse(String roleId, String tenantId, String message) {
        this.users = null;
        this.totalCount = 0;
        this.usersWithRoleCount = 0;
        this.usersWithoutRoleCount = 0;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.success = false;
        this.message = message;
    }

    // Getters and Setters

    public List<UserWithRoleDto> getUsers() {
        return users;
    }

    public void setUsers(List<UserWithRoleDto> users) {
        this.users = users;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getUsersWithRoleCount() {
        return usersWithRoleCount;
    }

    public void setUsersWithRoleCount(Integer usersWithRoleCount) {
        this.usersWithRoleCount = usersWithRoleCount;
    }

    public Integer getUsersWithoutRoleCount() {
        return usersWithoutRoleCount;
    }

    public void setUsersWithoutRoleCount(Integer usersWithoutRoleCount) {
        this.usersWithoutRoleCount = usersWithoutRoleCount;
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

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getCurrentPageSize() {
        return currentPageSize;
    }

    public void setCurrentPageSize(Integer currentPageSize) {
        this.currentPageSize = currentPageSize;
    }

    @Override
    public String toString() {
        return "GetUsersByRoleResponse{" +
                "users=" + users +
                ", totalCount=" + totalCount +
                ", usersWithRoleCount=" + usersWithRoleCount +
                ", usersWithoutRoleCount=" + usersWithoutRoleCount +
                ", roleId='" + roleId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}