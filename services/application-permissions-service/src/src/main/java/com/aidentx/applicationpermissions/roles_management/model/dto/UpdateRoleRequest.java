package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.RiskLevel;
import com.aidentx.applicationpermissions.roles_management.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for updating an existing role.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = AppConstants.DTO_UPDATE_ROLE_REQUEST_DESCRIPTION)
public class UpdateRoleRequest {

    /**
     * The hierarchical type of this role.
     */
    @Schema(description = AppConstants.DTO_ROLE_TYPE_DESCRIPTION, 
            example = AppConstants.DTO_ROLE_TYPE_EXAMPLE_REVIEWER,
            allowableValues = {"ADMINISTRATOR", "REVIEWER", "OPERATOR", "VIEWER"},
            required = true)
    @NotNull(message = AppConstants.DTO_ROLE_TYPE_REQUIRED)
    private RoleType roleType;

    /**
     * The human-readable name of the role.
     */
    @Schema(description = AppConstants.DTO_ROLE_NAME_DESCRIPTION, 
            example = AppConstants.DTO_ROLE_NAME_EXAMPLE_REVIEWER,
            minLength = 2, 
            maxLength = 128,
            required = true)
    @NotBlank(message = AppConstants.DTO_ROLE_NAME_REQUIRED)
    @Size(min = 2, max = 128, message = AppConstants.DTO_ROLE_NAME_LENGTH_VALIDATION)
    private String roleName;

    /**
     * Optional description explaining the purpose and scope of this role.
     */
    @Schema(description = AppConstants.DTO_ROLE_DESCRIPTION_DESCRIPTION, 
            example = AppConstants.DTO_ROLE_DESCRIPTION_EXAMPLE_REVIEWER,
            maxLength = 500)
    @Size(max = 500, message = AppConstants.DTO_DESCRIPTION_LENGTH_VALIDATION)
    private String description;

    /**
     * Business justification for creating this role.
     */
    @Schema(description = AppConstants.DTO_BUSINESS_JUSTIFICATION_DESCRIPTION, 
            example = AppConstants.DTO_BUSINESS_JUSTIFICATION_EXAMPLE,
            maxLength = 1000)
    @Size(max = 1000, message = AppConstants.DTO_BUSINESS_JUSTIFICATION_MAX_LENGTH)
    private String businessJustification;

    /**
     * Flag indicating whether owner approval is required for role assignment.
     */
    @Schema(description = AppConstants.DTO_REQUIRE_MANAGER_APPROVAL_DESCRIPTION, 
            example = AppConstants.DTO_REQUIRE_MANAGER_APPROVAL_EXAMPLE)
    private Boolean requireOwnerApproval = false;

    /**
     * Tags associated with this role for categorization and filtering.
     */
    @Schema(description = AppConstants.DTO_TAGS_DESCRIPTION, 
            example = AppConstants.DTO_TAGS_EXAMPLE)
    private List<String> tags;

    /**
     * Risk level associated with this role.
     */
    @Schema(description = AppConstants.DTO_RISK_LEVEL_DESCRIPTION, 
            example = AppConstants.DTO_RISK_LEVEL_EXAMPLE,
            allowableValues = {"Low", "Medium", "High"})
    private RiskLevel riskLevel;

    /**
     * Flag indicating whether this role is active.
     * Inactive roles cannot be assigned to users.
     */
    @Schema(description = "Whether the role is active", 
            example = "true")
    private Boolean isActive;

    /**
     * List of user IDs who will be owners of this role.
     * These owners will completely replace any existing owners.
     */
    @Schema(description = AppConstants.DTO_OWNER_IDS_DESCRIPTION, 
            example = AppConstants.DTO_OWNER_IDS_EXAMPLE)
    private List<java.util.UUID> ownerIds;

    // Default constructor
    public UpdateRoleRequest() {}

    // Constructor with parameters
    public UpdateRoleRequest(RoleType roleType, String roleName, String description, 
                            String businessJustification, Boolean requireOwnerApproval, List<String> tags, 
                            RiskLevel riskLevel, Boolean isActive, List<java.util.UUID> ownerIds) {
        this.roleType = roleType;
        this.roleName = roleName;
        this.description = description;
        this.businessJustification = businessJustification;
        this.requireOwnerApproval = requireOwnerApproval != null ? requireOwnerApproval : false;
        this.tags = tags;
        this.riskLevel = riskLevel;
        this.isActive = isActive;
        this.ownerIds = ownerIds;
    }

    // Getters and Setters
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
        this.requireOwnerApproval = requireOwnerApproval != null ? requireOwnerApproval : false;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<java.util.UUID> getOwnerIds() {
        return ownerIds;
    }

    public void setOwnerIds(List<java.util.UUID> ownerIds) {
        this.ownerIds = ownerIds;
    }
}