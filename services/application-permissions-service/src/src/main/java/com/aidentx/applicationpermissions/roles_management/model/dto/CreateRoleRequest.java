package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.RiskLevel;
import com.aidentx.applicationpermissions.roles_management.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new role.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = AppConstants.DTO_CREATE_ROLE_REQUEST_DESCRIPTION)
public class CreateRoleRequest {

    /**
     * The hierarchical type of this role.
     */
    @Schema(description = AppConstants.DTO_ROLE_TYPE_DESCRIPTION, 
            example = AppConstants.DTO_ROLE_TYPE_EXAMPLE_ADMIN,
            allowableValues = {"ADMINISTRATOR", "REVIEWER", "OPERATOR", "VIEWER"},
            required = true)
    @NotNull(message = AppConstants.DTO_ROLE_TYPE_REQUIRED)
    private RoleType roleType;

    /**
     * The human-readable name of the role.
     */
    @Schema(description = AppConstants.DTO_ROLE_NAME_DESCRIPTION, 
            example = AppConstants.DTO_ROLE_NAME_EXAMPLE_ADMIN,
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
            example = AppConstants.DTO_ROLE_DESCRIPTION_EXAMPLE_ADMIN,
            maxLength = 500)
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
    private RiskLevel riskLevel = RiskLevel.Low;

    /**
     * Flag indicating whether this role is active.
     * Inactive roles cannot be assigned to users.
     */
    @Schema(description = "Whether the role is active", 
            example = "true")
    private Boolean isActive = true;

    /**
     * List of user IDs who will be assigned as owners of this role.
     * These users will have management permissions for this role.
     */
    @Schema(description = "List of user IDs to be assigned as role owners", 
            example = "[\"550e8400-e29b-41d4-a716-446655440000\", \"550e8400-e29b-41d4-a716-446655440001\"]")
    private List<UUID> ownerIds;

    /**
     * ID of the workflow to be triggered for role creation approval.
     * Required when requireOwnerApproval is true.
     */
    @Schema(description = "Workflow ID for role creation approval (required when requireOwnerApproval is true)", 
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String workflowId;

    /**
     * Flag indicating whether this is a submit-for-approval operation (no immediate role creation).
     * When true, only publishes event to trigger workflow, does not create role in database.
     * The workflow will be responsible for creating the role after approval.
     */
    @Schema(description = "Whether to only submit for approval without creating the role immediately", 
            example = "false")
    private Boolean submitForApproval = false;

    // Default constructor
    public CreateRoleRequest() {}

    // Constructor with parameters
    public CreateRoleRequest(RoleType roleType, String roleName, String description, 
                            String businessJustification, Boolean requireOwnerApproval, List<String> tags, 
                            RiskLevel riskLevel, Boolean isActive, List<UUID> ownerIds, String workflowId) {
        this.roleType = roleType;
        this.roleName = roleName;
        this.description = description;
        this.businessJustification = businessJustification;
        this.requireOwnerApproval = requireOwnerApproval != null ? requireOwnerApproval : false;
        this.tags = tags;
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.Low;
        this.isActive = isActive != null ? isActive : true;
        this.ownerIds = ownerIds;
        this.workflowId = workflowId;
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
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.Low;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive != null ? isActive : true;
    }

    public List<UUID> getOwnerIds() {
        return ownerIds;
    }

    public void setOwnerIds(List<UUID> ownerIds) {
        this.ownerIds = ownerIds;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public Boolean getSubmitForApproval() {
        return submitForApproval;
    }

    public void setSubmitForApproval(Boolean submitForApproval) {
        this.submitForApproval = submitForApproval != null ? submitForApproval : false;
    }
}