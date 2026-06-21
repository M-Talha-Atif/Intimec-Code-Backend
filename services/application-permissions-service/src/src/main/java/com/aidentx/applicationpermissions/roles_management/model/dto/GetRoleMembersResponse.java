package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for the get role members endpoint.
 * 
 * <p>Contains paginated list of users assigned to a specific role along with
 * their assignment type (MANUAL or DYNAMIC). Includes pagination metadata
 * and filtering information.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Response containing role members with assignment types")
public class GetRoleMembersResponse {

    @Schema(description = "List of role members with assignment type information")
    @JsonProperty("members")
    private List<RoleMemberDto> members;

    @Schema(description = "Total number of members (after filtering, before pagination)", example = "25")
    @JsonProperty("totalCount")
    private Integer totalCount;

    @Schema(description = "Number of manually assigned members", example = "15")
    @JsonProperty("manualAssignmentCount")
    private Integer manualAssignmentCount;

    @Schema(description = "Number of dynamically assigned members", example = "10")
    @JsonProperty("dynamicAssignmentCount")
    private Integer dynamicAssignmentCount;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    @JsonProperty("page")
    private Integer page;

    @Schema(description = "Page size", example = "10")
    @JsonProperty("size")
    private Integer size;

    @Schema(description = "Total number of pages", example = "3")
    @JsonProperty("totalPages")
    private Integer totalPages;

    @Schema(description = "Number of members on current page", example = "10")
    @JsonProperty("currentPageSize")
    private Integer currentPageSize;

    @Schema(description = "Role ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("roleId")
    private String roleId;

    @Schema(description = "Tenant ID", example = "tenant_001")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Success indicator", example = "true")
    @JsonProperty("success")
    private Boolean success;

    @Schema(description = "Response message", example = "Role members retrieved successfully")
    @JsonProperty("message")
    private String message;

    /**
     * Default constructor.
     */
    public GetRoleMembersResponse() {
    }

    /**
     * Success constructor with full data.
     * 
     * @param members list of role members
     * @param totalCount total number of members after filtering
     * @param manualAssignmentCount count of manually assigned members
     * @param dynamicAssignmentCount count of dynamically assigned members
     * @param page current page number
     * @param size page size
     * @param totalPages total number of pages
     * @param roleId the role ID
     * @param tenantId the tenant ID
     * @param message response message
     */
    public GetRoleMembersResponse(List<RoleMemberDto> members, Integer totalCount,
                                  Integer manualAssignmentCount, Integer dynamicAssignmentCount,
                                  Integer page, Integer size, Integer totalPages,
                                  String roleId, String tenantId, String message) {
        this.members = members;
        this.totalCount = totalCount;
        this.manualAssignmentCount = manualAssignmentCount;
        this.dynamicAssignmentCount = dynamicAssignmentCount;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.currentPageSize = members != null ? members.size() : 0;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.success = true;
        this.message = message;
    }

    /**
     * Error constructor.
     * 
     * @param roleId the role ID
     * @param tenantId the tenant ID
     * @param message error message
     */
    public GetRoleMembersResponse(String roleId, String tenantId, String message) {
        this.members = List.of();
        this.totalCount = 0;
        this.manualAssignmentCount = 0;
        this.dynamicAssignmentCount = 0;
        this.page = 0;
        this.size = 0;
        this.totalPages = 0;
        this.currentPageSize = 0;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.success = false;
        this.message = message;
    }

    // Getters and Setters

    public List<RoleMemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<RoleMemberDto> members) {
        this.members = members;
        this.currentPageSize = members != null ? members.size() : 0;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getManualAssignmentCount() {
        return manualAssignmentCount;
    }

    public void setManualAssignmentCount(Integer manualAssignmentCount) {
        this.manualAssignmentCount = manualAssignmentCount;
    }

    public Integer getDynamicAssignmentCount() {
        return dynamicAssignmentCount;
    }

    public void setDynamicAssignmentCount(Integer dynamicAssignmentCount) {
        this.dynamicAssignmentCount = dynamicAssignmentCount;
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
}
