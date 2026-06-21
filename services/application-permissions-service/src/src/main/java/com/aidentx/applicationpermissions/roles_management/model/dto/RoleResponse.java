package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.RiskLevel;
import com.aidentx.applicationpermissions.roles_management.enums.RoleType;
import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for role operations.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = AppConstants.DTO_ROLE_RESPONSE_DESCRIPTION)
public class RoleResponse {

    /**
     * Unique identifier for the role.
     */
    @Schema(description = AppConstants.DTO_ROLE_ID_DESCRIPTION, 
            example = AppConstants.DTO_ROLE_ID_EXAMPLE)
    private UUID id;

    /**
     * The tenant identifier that owns this role.
     */
    @Schema(description = AppConstants.DTO_TENANT_ID_DESCRIPTION, 
            example = AppConstants.DTO_TENANT_ID_EXAMPLE)
    private String tenantId;

    /**
     * The hierarchical type of this role.
     */
    @Schema(description = AppConstants.DTO_ROLE_TYPE_DESCRIPTION, 
            example = AppConstants.DTO_ROLE_TYPE_EXAMPLE_ADMIN,
            allowableValues = {"ADMINISTRATOR", "REVIEWER", "OPERATOR", "VIEWER"})
    private RoleType roleType;

    /**
     * The human-readable name of the role.
     */
    @Schema(description = AppConstants.DTO_ROLE_NAME_DESCRIPTION, 
            example = AppConstants.DTO_ROLE_NAME_EXAMPLE_ADMIN)
    private String roleName;

    /**
     * Optional description explaining the purpose and scope of this role.
     */
    @Schema(description = AppConstants.DTO_ROLE_DESCRIPTION_DESCRIPTION, 
            example = AppConstants.DTO_ROLE_DESCRIPTION_EXAMPLE_ADMIN)
    private String description;

    /**
     * Business justification for creating this role.
     */
    @Schema(description = AppConstants.DTO_BUSINESS_JUSTIFICATION_DESCRIPTION, 
            example = AppConstants.DTO_BUSINESS_JUSTIFICATION_EXAMPLE)
    private String businessJustification;

    /**
     * Flag indicating whether owner approval is required for role assignment.
     */
    @Schema(description = AppConstants.DTO_REQUIRE_MANAGER_APPROVAL_DESCRIPTION, 
            example = AppConstants.DTO_REQUIRE_MANAGER_APPROVAL_EXAMPLE)
    private Boolean requireOwnerApproval;

    /**
     * Flag indicating whether this role is active.
     * Inactive roles cannot be assigned to users.
     */
    @Schema(description = "Whether the role is active", example = "true")
    private Boolean isActive;

    /**
     * Tags associated with this role for categorization and filtering.
     */
    @Schema(description = AppConstants.DTO_TAGS_DESCRIPTION, 
            example = AppConstants.DTO_TAGS_EXAMPLE)
    private List<String> tags;

    /**
     * Number of users assigned to this role.
     */
    @Schema(description = AppConstants.DTO_USER_COUNT_DESCRIPTION, 
            example = AppConstants.DTO_USER_COUNT_EXAMPLE)
    private Long userCount;

    /**
     * Number of permissions assigned to this role.
     */
    @Schema(description = AppConstants.DTO_PERMISSIONS_COUNT_DESCRIPTION, 
            example = AppConstants.DTO_PERMISSIONS_COUNT_EXAMPLE)
    private Long permissionsCount;

    /**
     * Indicates whether this role has been manually assigned to any users.
     */
    @Schema(description = AppConstants.DTO_HAS_MANUAL_ASSIGNMENTS_DESCRIPTION, 
            example = AppConstants.DTO_HAS_MANUAL_ASSIGNMENTS_EXAMPLE)
    private Boolean hasManualAssignments;

    /**
     * Indicates whether this role has active dynamic assignment rules configured.
     */
    @Schema(description = AppConstants.DTO_HAS_DYNAMIC_RULES_DESCRIPTION, 
            example = AppConstants.DTO_HAS_DYNAMIC_RULES_EXAMPLE)
    private Boolean hasDynamicRules;

    /**
     * Risk level associated with this role.
     */
    @Schema(description = AppConstants.DTO_RISK_LEVEL_DESCRIPTION, 
            example = AppConstants.DTO_RISK_LEVEL_EXAMPLE,
            allowableValues = {"Low", "Medium", "High"})
    private RiskLevel riskLevel;

    /**
     * Timestamp when this role was created.
     */
    @Schema(description = AppConstants.DTO_CREATED_AT_DESCRIPTION, 
            example = AppConstants.DTO_CREATED_AT_EXAMPLE)
    private Long createdAt;

    /**
     * Timestamp when this role was last updated.
     */
    @Schema(description = AppConstants.DTO_UPDATED_AT_DESCRIPTION, 
            example = AppConstants.DTO_UPDATED_AT_EXAMPLE)
    private Long updatedAt;

    /**
     * Name of the user who created this role.
     */
    @Schema(description = AppConstants.DTO_CREATED_BY_NAME_DESCRIPTION, 
            example = AppConstants.DTO_CREATED_BY_NAME_EXAMPLE)
    private String createdByName;

    /**
     * ID of the user who created this role.
     */
    @Schema(description = AppConstants.DTO_CREATED_BY_ID_DESCRIPTION, 
            example = AppConstants.DTO_CREATED_BY_ID_EXAMPLE)
    private UUID createdById;

    /**
     * Name of the user who last updated this role.
     */
    @Schema(description = "Name of the user who last updated this role", 
            example = "john.doe")
    private String updatedByName;

    /**
     * ID of the user who last updated this role.
     */
    @Schema(description = "ID of the user who last updated this role", 
            example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID updatedById;

    /**
     * List of users who are owners of this role.
     * Contains full user information for each role owner.
     */
    @Schema(description = "List of users who are owners of this role")
    private List<UserDto> ownersData;

    // Default constructor
    public RoleResponse() {}

    // Constructor with parameters
    public RoleResponse(UUID id, String tenantId, RoleType roleType, String roleName, String description, 
                       String businessJustification, Boolean requireOwnerApproval, Boolean isActive,
                       List<String> tags, Long userCount, Long permissionsCount, Boolean hasManualAssignments, 
                       Boolean hasDynamicRules, RiskLevel riskLevel, Long createdAt, Long updatedAt, 
                       String createdByName, UUID createdById, String updatedByName, UUID updatedById, 
                       List<UserDto> ownersData) {
        this.id = id;
        this.tenantId = tenantId;
        this.roleType = roleType;
        this.roleName = roleName;
        this.description = description;
        this.businessJustification = businessJustification;
        this.requireOwnerApproval = requireOwnerApproval;
        this.isActive = isActive;
        this.tags = tags;
        this.userCount = userCount;
        this.permissionsCount = permissionsCount;
        this.hasManualAssignments = hasManualAssignments;
        this.hasDynamicRules = hasDynamicRules;
        this.riskLevel = riskLevel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdByName = createdByName;
        this.createdById = createdById;
        this.updatedByName = updatedByName;
        this.updatedById = updatedById;
        this.ownersData = ownersData;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBusinessJustification() {
        return businessJustification;
    }

    public void setBusinessJustification(String businessJustification) {
        this.businessJustification = businessJustification;
    }

    public Boolean getRequireOwnerApproval() {
        return requireOwnerApproval;
    }

    public void setRequireOwnerApproval(Boolean requireOwnerApproval) {
        this.requireOwnerApproval = requireOwnerApproval;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public Long getPermissionsCount() {
        return permissionsCount;
    }

    public void setPermissionsCount(Long permissionsCount) {
        this.permissionsCount = permissionsCount;
    }

    public Boolean getHasManualAssignments() {
        return hasManualAssignments;
    }

    public void setHasManualAssignments(Boolean hasManualAssignments) {
        this.hasManualAssignments = hasManualAssignments;
    }

    public Boolean getHasDynamicRules() {
        return hasDynamicRules;
    }

    public void setHasDynamicRules(Boolean hasDynamicRules) {
        this.hasDynamicRules = hasDynamicRules;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
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

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public void setCreatedById(UUID createdById) {
        this.createdById = createdById;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    public UUID getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(UUID updatedById) {
        this.updatedById = updatedById;
    }

    public List<UserDto> getOwnersData() {
        return ownersData;
    }

    public void setOwnersData(List<UserDto> ownersData) {
        this.ownersData = ownersData;
    }
}