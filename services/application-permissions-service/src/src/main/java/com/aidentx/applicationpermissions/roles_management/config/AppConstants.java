package com.aidentx.applicationpermissions.roles_management.config;

/**
 * Application constants for the Roles Management module.
 * Contains all constant values to avoid hardcoding throughout the role management components.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public final class AppConstants {

    // API Endpoints
    public static final String ROLES_BASE_PATH = "/api/v1/roles";
    public static final String ROLE_ID_PATH_VAR = "/{id}";

    // Error Messages
    public static final String ROLE_NOT_FOUND = "Role not found";
    public static final String ROLE_ACCESS_DENIED = "Access denied: Role belongs to different tenant";
    public static final String ROLE_CREATION_FAILED = "Failed to create role";
    public static final String ROLE_UPDATE_FAILED = "Failed to update role";
    public static final String ROLE_DELETION_FAILED = "Failed to delete role";
    public static final String ROLE_RETRIEVAL_FAILED = "Failed to retrieve role(s)";
    public static final String ROLE_TAGS_RETRIEVAL_FAILED = "Failed to retrieve tags";
    public static final String INVALID_ROLE_ID = "Invalid role ID format";
    public static final String ROLE_NAME_REQUIRED = "Role name is required";
    public static final String ROLE_TYPE_REQUIRED = "Role type is required";
    public static final String TENANT_ID_REQUIRED = "Tenant ID is required";
    public static final String PERMISSION_ASSIGNMENT_FAILED = "Failed to assign permission to role";
    public static final String DUPLICATE_PERMISSION_ASSIGNMENT = "Permission for this action is already assigned to the role";
    public static final String ACTION_NOT_FOUND = "Action not found";
    public static final String PERMISSION_ALREADY_EXISTS_FOR_ACTION = "A permission for this action already exists for the role";
    public static final String ROLE_NAME_ALREADY_EXISTS = "A role with this name already exists in the tenant";
    public static final String ROLE_NAME_MUST_BE_UNIQUE = "Role name must be unique within the tenant";
    public static final String ROLE_NOT_FOUND_OR_ACCESS_DENIED = "Role not found or access denied";
    public static final String PERMISSION_ASSIGNMENT_RUNTIME_ERROR = "Failed to assign permission to role: {0}";
    public static final String PERMISSION_DELETION_FAILED = "Failed to delete permission from role";
    public static final String PERMISSION_NOT_FOUND_FOR_ROLE = "Permission not found for this role and action";
    public static final String PERMISSION_UPDATE_FAILED = "Failed to update permission for role";
    public static final String PERMISSION_ALREADY_EXISTS_FOR_ROLE_ACTION = "Permission already exists for this role-action combination";
    public static final String PERMISSION_ALREADY_ASSIGNED_TO_ROLE = "Permission for this action is already assigned to the role";
    public static final String NO_EXISTING_PERMISSION_FOUND_FOR_ROLE_ACTION = "No existing permission found for role-action combination, proceeding with creation";
    public static final String CONVERTING_UPDATE_PERMISSION_REQUEST = "Converting UpdatePermissionRequest to AssignPermissionToRoleRequest";
    public static final String CANNOT_ASSIGN_PERMISSION_TO_INACTIVE_ROLE = "Cannot assign permissions to an inactive role";
    public static final String ROLE_IS_INACTIVE = "Role is inactive and cannot be modified";
    
    // SYSTEM Role Type Constants
    public static final String ROLE_TYPE_SYSTEM = "SYSTEM";
    public static final String SYSTEM_ROLE_CREATION_FORBIDDEN = "SYSTEM roles cannot be created by users";
    public static final String SYSTEM_ROLE_DELETION_FORBIDDEN = "SYSTEM roles cannot be deleted";
    public static final String SYSTEM_ROLE_UPDATE_FORBIDDEN = "SYSTEM roles cannot be modified";
    public static final String SYSTEM_ROLE_PERMISSION_ASSIGNMENT_FORBIDDEN = "Cannot assign permissions to SYSTEM roles";
    
    // Log Messages for SYSTEM Role
    public static final String LOG_SYSTEM_ROLE_CREATION_ATTEMPTED = "Attempted to create SYSTEM role - operation blocked";
    public static final String LOG_SYSTEM_ROLE_DELETION_ATTEMPTED = "Attempted to delete SYSTEM role - operation blocked";
    public static final String LOG_SYSTEM_ROLE_UPDATE_ATTEMPTED = "Attempted to update SYSTEM role - operation blocked";
    public static final String LOG_SYSTEM_ROLE_PERMISSION_ASSIGNMENT_ATTEMPTED = "Attempted to assign permission to SYSTEM role - operation blocked";
    
    // Success Messages
    public static final String ROLE_CREATED_SUCCESS = "Role created successfully";
    public static final String ROLE_UPDATED_SUCCESS = "Role updated successfully";
    public static final String ROLE_DELETED_SUCCESS = "Role deleted successfully";
    public static final String ROLES_RETRIEVED_SUCCESS = "Roles retrieved successfully";
    public static final String ROLE_RETRIEVED_SUCCESS = "Role retrieved successfully";
    public static final String ROLE_TAGS_RETRIEVED_SUCCESS = "Tags retrieved successfully";
    public static final String PERMISSION_ASSIGNED_TO_ROLE_SUCCESS = "Permission assigned to role successfully";
    public static final String PERMISSION_DELETED_FROM_ROLE_SUCCESS = "Permission deleted from role successfully";
    public static final String PERMISSION_UPDATED_FOR_ROLE_SUCCESS = "Permission updated for role successfully";

    // Validation Messages
    public static final String ROLE_NAME_MIN_LENGTH = "Role name must be at least 2 characters long";
    public static final String ROLE_NAME_MAX_LENGTH = "Role name must not exceed 100 characters";
    public static final String DESCRIPTION_MAX_LENGTH = "Description must not exceed 500 characters";

    // Log Messages
    public static final String LOG_ROLE_CREATED = "Role created for tenant";
    public static final String LOG_ROLE_UPDATED = "Role updated for tenant";
    public static final String LOG_ROLE_DELETED = "Role deleted for tenant";
    public static final String LOG_ROLE_RETRIEVED = "Role retrieved for tenant";
    public static final String LOG_ROLES_RETRIEVED = "Roles retrieved for tenant";
    public static final String LOG_ROLE_ACCESS_VALIDATION = "Validating role access for tenant";
    public static final String LOG_TENANT_CONTEXT_EXTRACTED = "Tenant context extracted from JWT";
    public static final String LOG_ROLE_CREATE_REQUEST = "Processing role creation request";
    public static final String LOG_ROLE_UPDATE_REQUEST = "Processing role update request";
    public static final String LOG_ROLE_DELETE_REQUEST = "Processing role deletion request";
    public static final String LOG_ROLE_GET_REQUEST = "Processing role retrieval request";
    public static final String LOG_ROLES_GET_REQUEST = "Processing roles list request";
    public static final String LOG_ROLE_TAGS_GET_REQUEST = "Processing role tags retrieval request";
    public static final String LOG_ROLE_TAGS_RETRIEVED = "Role tags retrieved successfully";
    public static final String LOG_ROLE_TAGS_RETRIEVAL_FAILED = "Failed to retrieve role tags";
    public static final String LOG_ROLE_VALIDATION_SUCCESS = "Role access validation successful";
    public static final String LOG_ROLE_VALIDATION_FAILED = "Role access validation failed";

    // Permission Assignment Log Messages
    public static final String LOG_ASSIGNING_PERMISSION_TO_ROLE = "Assigning permission to role";
    public static final String LOG_CHECKING_ROLE_ACTIVE_STATUS = "Checking if role is active before assigning permission";
    public static final String LOG_ROLE_IS_INACTIVE_CANNOT_ASSIGN = "Role is inactive, cannot assign permission";
    public static final String LOG_ROLE_IS_ACTIVE_PROCEEDING = "Role is active, proceeding with permission assignment";
    public static final String LOG_PROCESSING_PERMISSION_ASSIGNMENT = "Processing permission assignment request";
    public static final String LOG_PERMISSION_ASSIGNMENT_COMPLETED = "Permission assignment completed successfully";
    public static final String LOG_DUPLICATE_PERMISSION_ATTEMPTED = "Duplicate permission assignment attempted";
    public static final String LOG_DUPLICATE_PERMISSION_WITH_CONDITIONAL_RULE = "Attempted to assign duplicate permission with identical conditional rule";
    public static final String LOG_PERMISSION_ASSIGNMENT_FAILED = "Permission assignment failed";
    public static final String LOG_VALIDATION_PASSED_NO_DUPLICATE = "Validation passed - no duplicate permission assignment found";
    public static final String LOG_CREATED_NEW_PERMISSION = "Created new permission";
    public static final String LOG_CREATED_ROLE_PERMISSION_ASSOCIATION = "Created role-permission association";
    public static final String LOG_CREATED_CONDITIONAL_RULE = "Created conditional rule";
    public static final String LOG_SUCCESSFULLY_ASSIGNED_PERMISSION = "Successfully assigned permission to role";
    public static final String LOG_FAILED_TO_ASSIGN_PERMISSION = "Failed to assign permission to role";
    public static final String LOG_RETHROWING_DUPLICATE_EXCEPTION = "Re-throwing DuplicatePermissionException for controller handling";

    // Permission Deletion Log Messages
    public static final String LOG_DELETING_PERMISSION_FROM_ROLE = "Deleting permission from role";
    public static final String LOG_PERMISSION_NOT_FOUND_FOR_DELETION = "Permission not found for deletion";
    public static final String LOG_DELETED_CONDITIONAL_RULE = "Deleted conditional rule";
    public static final String LOG_DELETED_ROLE_PERMISSION = "Deleted role-permission association";
    public static final String LOG_DELETED_PERMISSION = "Deleted permission";
    public static final String LOG_SUCCESSFULLY_DELETED_PERMISSION = "Successfully deleted permission from role";
    public static final String LOG_FAILED_TO_DELETE_PERMISSION = "Failed to delete permission from role";

    // Permission Update Log Messages
    public static final String LOG_UPDATING_PERMISSION_FOR_ROLE = "Updating permission for role";
    public static final String LOG_PERMISSION_NOT_FOUND_FOR_UPDATE = "Permission not found for update";
    public static final String LOG_UPDATED_PERMISSION_JUSTIFICATION = "Updated permission justification";
    public static final String LOG_UPDATED_CONDITIONAL_RULE = "Updated conditional rule";
    public static final String LOG_CREATED_NEW_CONDITIONAL_RULE = "Created new conditional rule for permission";
    public static final String LOG_REMOVED_CONDITIONAL_RULE = "Removed conditional rule from permission";
    public static final String LOG_SUCCESSFULLY_UPDATED_PERMISSION = "Successfully updated permission for role";
    public static final String LOG_FAILED_TO_UPDATE_PERMISSION = "Failed to update permission for role";

    // Controller Log Messages
    public static final String LOG_ROLE_CREATION_ENDPOINT_CALLED = "Role creation endpoint called";
    public static final String LOG_ROLE_CREATION_SUCCESSFUL = "Role creation successful";
    public static final String LOG_ROLE_CREATION_FAILED_CONTROLLER = "Role creation failed in controller";
    public static final String LOG_ROLE_UPDATE_ENDPOINT_CALLED = "Role update endpoint called";
    public static final String LOG_ROLE_UPDATE_SUCCESSFUL = "Role update successful";
    public static final String LOG_ROLE_UPDATE_FAILED_CONTROLLER = "Role update failed in controller";
    public static final String LOG_ROLE_RETRIEVAL_ENDPOINT_CALLED = "Role retrieval endpoint called";
    public static final String LOG_ROLE_RETRIEVAL_SUCCESSFUL = "Role retrieval successful";
    public static final String LOG_ROLE_RETRIEVAL_FAILED_CONTROLLER = "Role retrieval failed in controller";
    public static final String LOG_ROLES_LIST_ENDPOINT_CALLED = "Roles list endpoint called";
    public static final String LOG_ROLES_LIST_RETRIEVAL_SUCCESSFUL = "Roles list retrieval successful";
    public static final String LOG_ROLES_LIST_RETRIEVAL_FAILED_CONTROLLER = "Roles list retrieval failed in controller";
    public static final String LOG_ROLE_DELETION_ENDPOINT_CALLED = "Role deletion endpoint called";
    public static final String LOG_ROLE_DELETION_SUCCESSFUL = "Role deletion successful";
    public static final String LOG_ROLE_DELETION_FAILED_CONTROLLER = "Role deletion failed in controller";

    // Bulk Role Import Controller Log Messages
    public static final String LOG_BULK_ROLE_IMPORT_ENDPOINT_CALLED = "Bulk role import endpoint called";
    public static final String LOG_BULK_ROLE_IMPORT_COMPLETED = "Bulk role import completed";
    public static final String LOG_BULK_ROLE_IMPORT_FAILED_CONTROLLER = "Bulk role import failed at controller level";
    public static final String LOG_PROCESSING_ROLE_IMPORT = "Processing role import";
    public static final String LOG_ROLE_IMPORTED_SUCCESSFULLY = "Role imported successfully";
    public static final String LOG_FAILED_TO_IMPORT_ROLE = "Failed to import role";
    
    // Bulk Role Import Messages
    public static final String BULK_ROLE_IMPORT_SUCCESS_MESSAGE = "Bulk import completed: %d successful, %d failed";
    public static final String BULK_ROLE_IMPORT_FAILED = "Bulk role import failed";
    public static final String UNKNOWN_ERROR_DURING_ROLE_IMPORT = "Unknown error during role import";
    
    // Role Assignment Type Log Messages
    public static final String LOG_CHECKING_MANUAL_ASSIGNMENTS = "Checking if role has manual user assignments";
    public static final String LOG_ROLE_HAS_MANUAL_ASSIGNMENTS = "Role has manual user assignments";
    public static final String LOG_ROLE_NO_MANUAL_ASSIGNMENTS = "Role has no manual user assignments";
    public static final String LOG_CHECKING_DYNAMIC_RULES = "Checking if role has active dynamic assignment rules";
    public static final String LOG_ROLE_HAS_DYNAMIC_RULES = "Role has active dynamic assignment rules";
    public static final String LOG_ROLE_NO_DYNAMIC_RULES = "Role has no active dynamic assignment rules";

    // Permission Assignment Controller Log Messages
    public static final String LOG_PERMISSION_ASSIGNMENT_ENDPOINT_CALLED = "Permission assignment endpoint called";
    public static final String LOG_PERMISSION_ASSIGNMENT_SUCCESSFUL_CONTROLLER = "Permission assignment completed successfully";
    public static final String LOG_DUPLICATE_PERMISSION_ATTEMPTED_CONTROLLER = "Duplicate permission assignment attempted";
    public static final String LOG_PERMISSION_ASSIGNMENT_FAILED_CONTROLLER = "Permission assignment failed";

    // Permission Deletion Controller Log Messages
    public static final String LOG_PROCESSING_PERMISSION_DELETION = "Processing permission deletion request";
    public static final String LOG_PERMISSION_DELETION_SUCCESSFUL_CONTROLLER = "Permission deletion completed successfully";
    public static final String LOG_PERMISSION_NOT_FOUND_CONTROLLER = "Permission not found for deletion";
    public static final String LOG_PERMISSION_DELETION_FAILED_CONTROLLER = "Permission deletion failed";

    // Permission Update Controller Log Messages
    public static final String LOG_PROCESSING_PERMISSION_UPDATE = "Processing permission update request";
    public static final String LOG_PERMISSION_UPDATE_SUCCESSFUL_CONTROLLER = "Permission update completed successfully";
    public static final String LOG_PERMISSION_UPDATE_FAILED_CONTROLLER = "Permission update failed";

    // Debug and Trace Messages
    public static final String LOG_DEBUG_STARTING_ROLE_CREATION = "Starting role creation process";
    public static final String LOG_DEBUG_ROLE_VALIDATION_STARTED = "Starting role validation";
    public static final String LOG_DEBUG_ROLE_FOUND_IN_DATABASE = "Role found in database";
    public static final String LOG_DEBUG_BUILDING_RESPONSE = "Building response object";
    public static final String LOG_TRACE_ENTERING_METHOD = "Entering method";
    public static final String LOG_TRACE_EXITING_METHOD = "Exiting method";
    public static final String LOG_TRACE_PARAMETER_VALUES = "Method parameter values";
    public static final String LOG_TRACE_RESPONSE_PREPARED = "Response prepared for client";
    public static final String LOG_DEBUG_ERROR_RESPONSE_PREPARED = "Error response prepared";
    public static final String LOG_DEBUG_EXCEPTION_ANALYSIS = "Analyzing exception type for appropriate response";

    // Logger Names and Categories
    public static final String ROLE_SERVICE_LOGGER_NAME = "RoleService";
    public static final String ROLE_CONTROLLER_LOGGER_NAME = "RoleController";
    public static final String USER_ROLE_SERVICE_LOGGER_NAME = "UserRoleService";
    public static final String USER_ROLE_CONTROLLER_LOGGER_NAME = "UserRoleController";
    public static final String LOG_CATEGORY_ROLE_MANAGEMENT = "Role-Management";
    public static final String LOG_CATEGORY_ROLE_VALIDATION = "Role-Validation";
    public static final String LOG_CATEGORY_USER_ROLE_MANAGEMENT = "User-Role-Management";

    // Log Object Types
    public static final String LOG_OBJECT_TYPE_ROLE = "Role";
    public static final String LOG_OBJECT_TYPE_ROLE_REQUEST = "RoleRequest";
    public static final String LOG_OBJECT_TYPE_ROLE_RESPONSE = "RoleResponse";
    public static final String LOG_OBJECT_TYPE_PERMISSION = "Permission";
    public static final String LOG_OBJECT_TYPE_ROLE_PERMISSION = "RolePermission";
    public static final String LOG_OBJECT_TYPE_CONDITIONAL_RULE = "ConditionalRule";
    public static final String LOG_OBJECT_TYPE_BULK_ROLE_IMPORT_REQUEST = "BULK_ROLE_IMPORT_REQUEST";
    public static final String LOG_OBJECT_TYPE_BULK_ROLE_IMPORT_RESPONSE = "BULK_ROLE_IMPORT_RESPONSE";
    public static final String LOG_OBJECT_TYPE_ROLE_IMPORT_ITEM = "ROLE_IMPORT_ITEM";

    // ==================== CONDITIONAL RULE EVALUATION CONSTANTS ====================
    
    // Conditional Rule Evaluation Logger Name
    public static final String CONDITIONAL_RULE_EVALUATION_SERVICE_LOGGER_NAME = "ConditionalRuleEvaluationService";
    
    // Conditional Rule Evaluation Log Category
    public static final String LOG_CATEGORY_CONDITIONAL_RULE_EVALUATION = "CONDITIONAL_RULE_EVALUATION";
    
    // Conditional Rule Evaluation Log Messages
    public static final String LOG_NO_CONDITION_SCRIPT_PROVIDED = "No condition script provided, allowing access";
    public static final String LOG_STARTING_CONDITIONAL_RULE_EVALUATION = "Starting conditional rule evaluation";
    public static final String LOG_CREATED_SPEL_EVALUATION_CONTEXT = "Created SpEL evaluation context with user as root object";
    public static final String LOG_CONDITIONAL_RULE_EVALUATION_COMPLETED = "Conditional rule evaluation completed";
    public static final String LOG_ERROR_EVALUATING_CONDITIONAL_RULE = "Error evaluating conditional rule";
    public static final String LOG_VALIDATING_CONDITION_SCRIPT_SYNTAX = "Validating condition script syntax";
    public static final String LOG_CONDITION_SCRIPT_VALIDATION_SUCCESSFUL = "Condition script validation successful";
    public static final String LOG_INVALID_CONDITION_SCRIPT_SYNTAX = "Invalid condition script syntax";
    
    // Conditional Rule Evaluation Detail Values
    public static final String EVALUATION_TYPE_DYNAMIC_SPEL = "Dynamic SpEL evaluation against user properties";
    public static final String CONTEXT_INFO_USER_AS_ROOT = "User object set as SpEL root - all user properties accessible directly";
    
    // Conditional Rule Evaluation Detail Keys
    public static final String DETAIL_KEY_CONDITION_SCRIPT = "conditionScript";
    public static final String DETAIL_KEY_USER_ID = "userId";
    public static final String DETAIL_KEY_EVALUATION_TYPE = "evaluationType";
    public static final String DETAIL_KEY_CONTEXT_INFO = "contextInfo";
    public static final String DETAIL_KEY_CONDITION_MET = "conditionMet";
    public static final String DETAIL_KEY_EVALUATION_RESULT = "evaluationResult";
    public static final String DETAIL_KEY_RESULT_TYPE = "resultType";
    public static final String DETAIL_KEY_ERROR = "error";

    // HTTP Status Messages
    public static final String HTTP_200_OK = "OK";
    public static final String HTTP_201_CREATED = "Created";
    public static final String HTTP_204_NO_CONTENT = "No Content";
    public static final String HTTP_400_BAD_REQUEST = "Bad Request";
    public static final String HTTP_401_UNAUTHORIZED = "Unauthorized";
    public static final String HTTP_403_FORBIDDEN = "Forbidden";
    public static final String HTTP_404_NOT_FOUND = "Not Found";
    public static final String HTTP_500_INTERNAL_ERROR = "Internal Server Error";

    // DTO Validation Messages
    public static final String DTO_ROLE_TYPE_REQUIRED = "Role type is required";
    public static final String DTO_ROLE_NAME_REQUIRED = "Role name is required";
    public static final String DTO_ROLE_NAME_LENGTH_VALIDATION = "Role name must be between 2 and 128 characters";
    public static final String DTO_DESCRIPTION_LENGTH_VALIDATION = "Description must not exceed 500 characters";

    // DTO Schema Descriptions
    public static final String DTO_CREATE_ROLE_REQUEST_DESCRIPTION = "Request object for creating a new role";
    public static final String DTO_UPDATE_ROLE_REQUEST_DESCRIPTION = "Request object for updating an existing role";
    public static final String DTO_ROLE_RESPONSE_DESCRIPTION = "Response object containing role information";
    
    // DTO Field Descriptions
    public static final String DTO_ROLE_TYPE_DESCRIPTION = "The hierarchical type of the role";
    public static final String DTO_ROLE_NAME_DESCRIPTION = "The human-readable name of the role";
    public static final String DTO_ROLE_DESCRIPTION_DESCRIPTION = "Optional description explaining the purpose and scope of the role";
    public static final String DTO_ROLE_ID_DESCRIPTION = "Unique identifier for the role";
    public static final String DTO_TENANT_ID_DESCRIPTION = "The tenant identifier that owns this role";
    public static final String DTO_BUSINESS_JUSTIFICATION_DESCRIPTION = "Business justification explaining why this role is needed";
    public static final String DTO_REQUIRE_MANAGER_APPROVAL_DESCRIPTION = "Flag indicating whether manager approval is required for role assignment";
    public static final String DTO_TAGS_DESCRIPTION = "Tags associated with this role for categorization and filtering";
    public static final String DTO_USER_COUNT_DESCRIPTION = "Number of users currently assigned to this role";
    public static final String DTO_PERMISSIONS_COUNT_DESCRIPTION = "Number of permissions currently assigned to this role";
    public static final String DTO_HAS_MANUAL_ASSIGNMENTS_DESCRIPTION = "Indicates whether this role has been manually assigned to any users";
    public static final String DTO_HAS_DYNAMIC_RULES_DESCRIPTION = "Indicates whether this role has active dynamic assignment rules configured";
    public static final String DTO_RISK_LEVEL_DESCRIPTION = "Risk level associated with this role based on its potential security impact";
    public static final String DTO_CREATED_AT_DESCRIPTION = "Timestamp when this role was created (epoch time in milliseconds)";
    public static final String DTO_UPDATED_AT_DESCRIPTION = "Timestamp when this role was last updated (epoch time in milliseconds)";
    public static final String DTO_CREATED_BY_NAME_DESCRIPTION = "Name of the user who created this role";
    public static final String DTO_CREATED_BY_ID_DESCRIPTION = "ID of the user who created this role";
    
    // DTO Example Values
    public static final String DTO_ROLE_TYPE_EXAMPLE_ADMIN = "ADMINISTRATOR";
    public static final String DTO_ROLE_TYPE_EXAMPLE_REVIEWER = "REVIEWER";
    public static final String DTO_ROLE_NAME_EXAMPLE_ADMIN = "System Administrator";
    public static final String DTO_ROLE_NAME_EXAMPLE_REVIEWER = "Senior Reviewer";
    public static final String DTO_ROLE_DESCRIPTION_EXAMPLE_ADMIN = "Full system access and management capabilities";
    public static final String DTO_ROLE_DESCRIPTION_EXAMPLE_REVIEWER = "Review and approve system changes";
    public static final String DTO_ROLE_ID_EXAMPLE = "550e8400-e29b-41d4-a716-446655440000";
    public static final String DTO_TENANT_ID_EXAMPLE = "tenant_001";
    public static final String DTO_BUSINESS_JUSTIFICATION_EXAMPLE = "Required for system administration and compliance oversight";
    public static final String DTO_REQUIRE_MANAGER_APPROVAL_EXAMPLE = "true";
    public static final String DTO_TAGS_EXAMPLE = "[\"security\", \"admin\", \"compliance\"]";
    public static final String DTO_USER_COUNT_EXAMPLE = "5";
    public static final String DTO_PERMISSIONS_COUNT_EXAMPLE = "12";
    public static final String DTO_HAS_MANUAL_ASSIGNMENTS_EXAMPLE = "true";
    public static final String DTO_HAS_DYNAMIC_RULES_EXAMPLE = "false";
    public static final String DTO_RISK_LEVEL_EXAMPLE = "Medium";
    public static final String DTO_CREATED_AT_EXAMPLE = "1696723200000";
    public static final String DTO_UPDATED_AT_EXAMPLE = "1696809600000";
    public static final String DTO_CREATED_BY_NAME_EXAMPLE = "admin@company.com";
    public static final String DTO_CREATED_BY_ID_EXAMPLE = "550e8400-e29b-41d4-a716-446655440001";
    
    // DTO Validation Messages for New Fields
    public static final String DTO_BUSINESS_JUSTIFICATION_MAX_LENGTH = "Business justification must not exceed 1000 characters";
    public static final String DTO_TAGS_SIZE_VALIDATION = "Maximum 10 tags are allowed";
    public static final String DTO_TAG_LENGTH_VALIDATION = "Each tag must be between 1 and 50 characters";
    
    // DTO Owner IDs Constants
    public static final String DTO_OWNER_IDS_DESCRIPTION = "List of user IDs who will be owners of this role. Owners will completely replace any existing owners.";
    public static final String DTO_OWNER_IDS_EXAMPLE = "[\"2d1c4b5a-8e9f-3762-a154-c8b7d6e5f4a3\", \"4bb2c7df-5677-4cd2-a0d1-aad3fa8afe8f\"]";
    public static final String DTO_OWNER_IDS_SIZE_VALIDATION = "Maximum 50 owners are allowed per role";

    // ==================== AUDIT LOGGING CONSTANTS ====================
    
    // Audit Entity Types
    public static final String ENTITY_TYPE_ROLE = "role";
    
    // Audit Event Types
    public static final String AUDIT_EVENT_CREATE = "CREATE";
    public static final String AUDIT_EVENT_UPDATE = "UPDATE";
    public static final String AUDIT_EVENT_DELETE = "DELETE";
    
    // Audit Logger Names
    public static final String ROLE_AUDIT_EVENT_PUBLISHER_LOGGER_NAME = "RoleAuditEventPublisher";
    public static final String ROLE_AUDIT_ASPECT_LOGGER_NAME = "RoleAuditAspect";
    
    // Audit Log Categories
    public static final String LOG_CATEGORY_ROLE_AUDIT = "ROLE_AUDIT";
    public static final String EXTRACTED_USER_INFO_FROM_TENANT_CONTEXT = "Extracted user info from TenantContextService (JWT)";
    public static final String FAILED_TO_EXTRACT_USER_INFO_FROM_TENANT_CONTEXT = "Failed to extract user info from TenantContextService, falling back";
    public static final String EXTRACTED_USER_INFO_FROM_USER_CONTEXT_HOLDER = "Extracted user info from UserContextHolder";
    public static final String EXTRACTED_USER_INFO_FROM_REQUEST_ATTRIBUTES = "Extracted user info from request attributes";

    
    // Audit Log Messages - Aspect Triggered
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_ROLE_CREATION = "Audit aspect triggered for role creation";
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_ROLE_UPDATE = "Audit aspect triggered for role update";
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_ROLE_DELETION = "Audit aspect triggered for role deletion";
    
    // Audit Log Messages - Auditing Operations
    public static final String AUDIT_LOG_AUDITING_ROLE_CREATION = "Auditing role creation";
    public static final String AUDIT_LOG_AUDITING_ROLE_UPDATE = "Auditing role update";
    public static final String AUDIT_LOG_AUDITING_ROLE_DELETION = "Auditing role deletion";
    
    // Audit Log Messages - Event Publishing
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_ROLE_CREATION = "Successfully published audit event for role creation";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_ROLE_UPDATE = "Successfully published audit event for role update";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_ROLE_DELETION = "Successfully published audit event for role deletion";
    
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_ROLE_CREATION = "Failed to publish audit event for role creation";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_ROLE_UPDATE = "Failed to publish audit event for role update";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_ROLE_DELETION = "Failed to publish audit event for role deletion";
    
    // Audit Log Messages - State Retrieval
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_ROLE_STATE_BEFORE_UPDATE = "Could not retrieve role state before update";
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_ROLE_STATE_BEFORE_DELETION = "Could not retrieve role state before deletion";
    
    // Audit Log Messages - Event Creation
    public static final String AUDIT_LOG_CREATING_AUDIT_EVENT = "Creating audit event";
    public static final String AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_ROLE_AUDIT_EVENT = "Failed to create or publish role audit event";
    
    // Audit Log Messages - User Role Assignment
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_USER_ROLE_ASSIGNMENT = "Audit aspect triggered for user role assignment";
    public static final String AUDIT_LOG_AUDITING_USER_ROLE_ASSIGNMENT = "Auditing user role assignment";
    public static final String AUDIT_LOG_CREATING_AUDIT_EVENT_USER_ROLE_ASSIGNMENT = "Creating audit event for user role assignment";
    public static final String AUDIT_LOG_FAILED_TO_CREATE_AUDIT_EVENT_USER_ROLE_ASSIGNMENT = "Failed to create audit event for user role assignment";
    public static final String AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_USER_ROLE_ASSIGNMENT_AUDIT_EVENT = "Failed to create or publish user role assignment audit event";
    public static final String AUDIT_LOG_FAILED_TO_FETCH_USER_INFO_FOR_AUDIT_USING_FALLBACK = "Failed to fetch user information for audit, using fallback usernames";
    public static final String AUDIT_LOG_FAILED_TO_EXTRACT_USER_INFO_FROM_ASSIGNMENT_RESULT = "Failed to extract user information from assignment result";
    
    // Audit Log Messages - User Role Removal  
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_USER_ROLE_REMOVAL = "Audit aspect triggered for user role removal";
    public static final String AUDIT_LOG_AUDITING_USER_ROLE_REMOVAL = "Auditing user role removal";
    public static final String AUDIT_LOG_CREATING_AUDIT_EVENT_USER_ROLE_REMOVAL = "Creating audit event for user role removal";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_USER_ROLE_REMOVAL_AUDIT_EVENT = "Successfully published user role removal audit event";
    public static final String AUDIT_LOG_FAILED_TO_CREATE_AUDIT_EVENT_USER_ROLE_REMOVAL = "Failed to create audit event for user role removal";
    public static final String AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_USER_ROLE_REMOVAL_AUDIT_EVENT = "Failed to create or publish user role removal audit event";
    public static final String AUDIT_LOG_FAILED_TO_FETCH_USER_INFO_FOR_REMOVAL_STATE_BEFORE_USING_FALLBACK = "Failed to fetch user information for removal stateBefore, using fallback";
    public static final String AUDIT_LOG_FAILED_TO_FETCH_USER_INFO_FOR_REMOVAL_STATE_AFTER_USING_FALLBACK = "Failed to fetch user information for removal stateAfter, using fallback";
    
    // Audit Log Messages - Permission Assignment to Role
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_PERMISSION_ASSIGNMENT = "Audit aspect triggered for permission assignment to role";
    public static final String AUDIT_LOG_AUDITING_PERMISSION_ASSIGNMENT = "Auditing permission assignment to role";
    public static final String AUDIT_LOG_CREATING_AUDIT_EVENT_PERMISSION_ASSIGNMENT = "Creating audit event for permission assignment to role";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_PERMISSION_ASSIGNMENT_AUDIT_EVENT = "Successfully published permission assignment audit event";
    public static final String AUDIT_LOG_FAILED_TO_CREATE_AUDIT_EVENT_PERMISSION_ASSIGNMENT = "Failed to create audit event for permission assignment";
    public static final String AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_PERMISSION_ASSIGNMENT_AUDIT_EVENT = "Failed to create or publish permission assignment audit event";
    public static final String AUDIT_LOG_FAILED_TO_FETCH_PERMISSION_HIERARCHY_FOR_AUDIT = "Failed to fetch permission hierarchy for audit, using fallback";
    
    // Audit Log Messages - Security Context
    public static final String AUDIT_LOG_DEBUG_AUTHENTICATION_OBJECT = "Debug authentication object";
    public static final String AUDIT_LOG_EXTRACTING_USER_INFO_FROM_REQUEST_ATTRIBUTES = "Extracting user information from request attributes";
    public static final String AUDIT_LOG_EXTRACTING_USER_INFO_FROM_JWT_CLAIMS = "Extracting user information from JWT claims";
    public static final String AUDIT_LOG_FALLBACK_TO_AUTHENTICATION_NAME = "Fallback to authentication name";
    public static final String AUDIT_LOG_NO_AUTHENTICATION_USING_SYSTEM_USER = "No authentication found, using system user";
    public static final String AUDIT_LOG_ERROR_EXTRACTING_USER_INFO = "Error extracting user information";
    public static final String AUDIT_LOG_FAILED_TO_GET_CLIENT_IP = "Failed to get client IP address";
    
    // Audit Log Messages - Event Publisher
    public static final String AUDIT_LOG_CONVERTING_EVENT_TO_STANDARD_FORMAT = "Converting event to standard format";
    public static final String AUDIT_LOG_PUBLISHING_EVENT_TO_KAFKA = "Publishing event to Kafka";
    public static final String AUDIT_LOG_EVENT_PUBLISHED_SUCCESSFULLY = "Event published successfully";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_EVENT = "Failed to publish event";
    public static final String CONVERTING_ROLE_AUDIT_EVENT = "Converting role audit event to standard format";
    public static final String CREATED_DETAILS_FOR_AUDIT_EVENT = "Created details for audit event";
    public static final String CREATED_STANDARD_AUDIT_EVENT = "Created standard audit event";
    public static final String CREATING_AUDIT_DETAILS_FOR_ROLE_EVENT = "Creating audit details for role event";
    public static final String FAILED_TO_GET_USER_AGENT = "Failed to get User-Agent from request";
    public static final String CREATED_AUDIT_DETAILS = "Created audit details";
    public static final String EMPTY_OR_NULL_UUID_GENERATING_DEFAULT = "Empty or null value for UUID field, generating default UUID";
    public static final String INVALID_UUID_FORMAT_GENERATING_DEFAULT = "Invalid UUID format detected, generating default UUID";
    
    // Audit Log Messages - Message Templates
    public static final String AUDIT_MSG_CREATE_DYNAMIC_RULE = "New dynamic rule was created for the %s role.";
    public static final String AUDIT_MSG_UPDATE_DYNAMIC_RULE = "Dynamic rule was updated for the %s role.";
    public static final String AUDIT_MSG_ASSIGN_PERMISSION = "Permission/s was added to the %s role.";
    public static final String AUDIT_MSG_DELETE_PERMISSION = "Permission/s was removed from the %s role.";
    public static final String AUDIT_MSG_UPDATE_PERMISSION = "Permission/s was updated for the %s role.";
    public static final String AUDIT_MSG_ASSIGN_ROLE = "The %s role was assigned to a user/s.";
    public static final String AUDIT_MSG_REVOKE_ROLE = "The %s role was revoked from user/s.";
    public static final String AUDIT_MSG_ROLE_RECONCILIATION = "System-initiated role reconciliation was performed for the %s role.";

    // ==================== USER ROLE MANAGEMENT CONSTANTS ====================
    
    // UserRole Controller Log Messages - Endpoint Calls
    public static final String LOG_USER_ROLE_ASSIGNMENT_ENDPOINT_CALLED = "User role assignment endpoint called";
    public static final String LOG_USER_ROLE_REMOVAL_ENDPOINT_CALLED = "User role removal endpoint called";
    public static final String LOG_GET_USER_ROLES_ENDPOINT_CALLED = "Get user roles endpoint called";
    public static final String LOG_GET_ROLE_USERS_ENDPOINT_CALLED = "Get role users endpoint called";
    public static final String LOG_GET_ALL_USER_ROLES_ENDPOINT_CALLED = "Get all user roles endpoint called";
    public static final String LOG_CHECK_USER_ROLE_ENDPOINT_CALLED = "Check user role endpoint called";
    
    // UserRole Controller Log Messages - Success Operations
    public static final String LOG_USER_ROLE_ASSIGNMENT_SUCCESSFUL = "User role assignment successful";
    public static final String LOG_USER_ROLE_REMOVAL_COMPLETED = "User role removal completed";
    public static final String LOG_USER_ROLES_RETRIEVED_SUCCESSFULLY = "User roles retrieved successfully";
    public static final String LOG_ROLE_USERS_RETRIEVED_SUCCESSFULLY = "Role users retrieved successfully";
    public static final String LOG_ALL_USER_ROLES_RETRIEVED_SUCCESSFULLY = "All user roles retrieved successfully";
    public static final String LOG_USER_ROLE_CHECK_COMPLETED = "User role check completed";
    
    // UserRole Controller Log Messages - Error Operations
    public static final String LOG_USER_ROLE_ASSIGNMENT_FAILED_CONTROLLER_INVALID_ARGUMENTS = "User role assignment failed in controller - invalid arguments";
    public static final String LOG_USER_ROLE_ASSIGNMENT_FAILED_CONTROLLER_INVALID_STATE = "User role assignment failed in controller - invalid state";
    public static final String LOG_USER_ROLE_ASSIGNMENT_FAILED_CONTROLLER = "User role assignment failed in controller";
    public static final String LOG_USER_ROLE_REMOVAL_FAILED_CONTROLLER_INVALID_ARGUMENTS = "User role removal failed in controller - invalid arguments";
    public static final String LOG_USER_ROLE_REMOVAL_FAILED_CONTROLLER = "User role removal failed in controller";
    public static final String LOG_GET_USER_ROLES_FAILED_CONTROLLER_INVALID_ARGUMENTS = "Get user roles failed in controller - invalid arguments";
    public static final String LOG_GET_USER_ROLES_FAILED_CONTROLLER = "Get user roles failed in controller";
    public static final String LOG_GET_ROLE_USERS_FAILED_CONTROLLER_INVALID_ARGUMENTS = "Get role users failed in controller - invalid arguments";
    public static final String LOG_GET_ROLE_USERS_FAILED_CONTROLLER = "Get role users failed in controller";
    public static final String LOG_GET_ALL_USER_ROLES_FAILED_CONTROLLER = "Get all user roles failed in controller";
    public static final String LOG_CHECK_USER_ROLE_FAILED_CONTROLLER = "Check user role failed in controller";
    
    // UserRole Controller Log Messages - Debug Level
    public static final String LOG_DEBUG_STARTING_USER_ROLE_ASSIGNMENT = "Starting user role assignment process";
    public static final String LOG_DEBUG_STARTING_USER_ROLE_REMOVAL = "Starting user role removal process";
    public static final String LOG_DEBUG_STARTING_GET_USER_ROLES = "Starting get user roles process";
    public static final String LOG_DEBUG_STARTING_GET_ROLE_USERS = "Starting get role users process";
    public static final String LOG_DEBUG_STARTING_GET_ALL_USER_ROLES = "Starting get all user roles process";
    public static final String LOG_DEBUG_STARTING_CHECK_USER_ROLE = "Starting check user role process";
    public static final String LOG_DEBUG_VALIDATING_REQUEST_PARAMETERS = "Validating request parameters";
    public static final String LOG_DEBUG_TENANT_CONTEXT_EXTRACTED = "Tenant context extracted successfully";
    public static final String LOG_DEBUG_CALLING_SERVICE_METHOD = "Calling service method";
    public static final String LOG_DEBUG_SERVICE_METHOD_COMPLETED = "Service method completed successfully";
    public static final String LOG_DEBUG_BUILDING_SUCCESS_RESPONSE = "Building success response";
    public static final String LOG_DEBUG_BUILDING_ERROR_RESPONSE = "Building error response";
    public static final String LOG_DEBUG_EXCEPTION_CAUGHT = "Exception caught in controller";
    public static final String LOG_DEBUG_ANALYZING_EXCEPTION_TYPE = "Analyzing exception type for appropriate response";
    
    // UserRole Controller Log Messages - Trace Level
    public static final String LOG_TRACE_ENTERING_ASSIGN_ROLE_METHOD = "Entering assignRole method";
    public static final String LOG_TRACE_ENTERING_REMOVE_ROLE_METHOD = "Entering removeRole method";
    public static final String LOG_TRACE_ENTERING_GET_USER_ROLES_METHOD = "Entering getUserRoles method";
    public static final String LOG_TRACE_ENTERING_GET_ROLE_USERS_METHOD = "Entering getRoleUsers method";
    public static final String LOG_TRACE_ENTERING_GET_ALL_USER_ROLES_METHOD = "Entering getAllUserRoles method";
    public static final String LOG_TRACE_ENTERING_HAS_ROLE_METHOD = "Entering hasRole method";
    public static final String LOG_TRACE_EXITING_ASSIGN_ROLE_METHOD = "Exiting assignRole method";
    public static final String LOG_TRACE_EXITING_REMOVE_ROLE_METHOD = "Exiting removeRole method";
    public static final String LOG_TRACE_EXITING_GET_USER_ROLES_METHOD = "Exiting getUserRoles method";
    public static final String LOG_TRACE_EXITING_GET_ROLE_USERS_METHOD = "Exiting getRoleUsers method";
    public static final String LOG_TRACE_EXITING_GET_ALL_USER_ROLES_METHOD = "Exiting getAllUserRoles method";
    public static final String LOG_TRACE_EXITING_HAS_ROLE_METHOD = "Exiting hasRole method";
    public static final String LOG_TRACE_REQUEST_PARAMETERS = "Request parameters";
    public static final String LOG_TRACE_RESPONSE_DATA = "Response data prepared";
    public static final String LOG_TRACE_EXCEPTION_DETAILS = "Exception details";
    
    // UserRole Controller Log Object Types
    public static final String LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT = "UserRoleAssignmentEndpoint";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_SUCCESS = "UserRoleAssignmentSuccess";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_VALIDATION_ERROR = "UserRoleAssignmentValidationError";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_STATE_ERROR = "UserRoleAssignmentStateError";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ERROR = "UserRoleAssignmentError";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ENDPOINT = "UserRoleRemovalEndpoint";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_SUCCESS = "UserRoleRemovalSuccess";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_VALIDATION_ERROR = "UserRoleRemovalValidationError";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ERROR = "UserRoleRemovalError";
    public static final String LOG_OBJECT_TYPE_GET_USER_ROLES_ENDPOINT = "GetUserRolesEndpoint";
    public static final String LOG_OBJECT_TYPE_GET_USER_ROLES_SUCCESS = "GetUserRolesSuccess";
    public static final String LOG_OBJECT_TYPE_GET_USER_ROLES_VALIDATION_ERROR = "GetUserRolesValidationError";
    public static final String LOG_OBJECT_TYPE_GET_USER_ROLES_ERROR = "GetUserRolesError";
    public static final String LOG_OBJECT_TYPE_GET_ROLE_USERS_ENDPOINT = "GetRoleUsersEndpoint";
    public static final String LOG_OBJECT_TYPE_GET_ROLE_USERS_SUCCESS = "GetRoleUsersSuccess";
    public static final String LOG_OBJECT_TYPE_GET_ROLE_USERS_VALIDATION_ERROR = "GetRoleUsersValidationError";
    public static final String LOG_OBJECT_TYPE_GET_ROLE_USERS_ERROR = "GetRoleUsersError";
    public static final String LOG_OBJECT_TYPE_GET_ALL_USER_ROLES_ENDPOINT = "GetAllUserRolesEndpoint";
    public static final String LOG_OBJECT_TYPE_GET_ALL_USER_ROLES_SUCCESS = "GetAllUserRolesSuccess";
    public static final String LOG_OBJECT_TYPE_GET_ALL_USER_ROLES_ERROR = "GetAllUserRolesError";
    public static final String LOG_OBJECT_TYPE_CHECK_USER_ROLE_ENDPOINT = "CheckUserRoleEndpoint";
    public static final String LOG_OBJECT_TYPE_CHECK_USER_ROLE_SUCCESS = "CheckUserRoleSuccess";
    public static final String LOG_OBJECT_TYPE_CHECK_USER_ROLE_ERROR = "CheckUserRoleError";
    
    // UserRole Response Messages
    public static final String USER_ROLE_ASSIGNED_SUCCESSFULLY = "Role assigned successfully";
    public static final String USER_ROLE_REMOVED_SUCCESSFULLY = "Role removed successfully";
    public static final String USER_ROLE_ASSIGNMENT_NOT_FOUND = "Role assignment not found";
    public static final String USER_ROLES_RETRIEVED_SUCCESSFULLY = "User roles retrieved successfully";
    public static final String ROLE_USERS_RETRIEVED_SUCCESSFULLY = "Role users retrieved successfully";
    public static final String ALL_USER_ROLES_RETRIEVED_SUCCESSFULLY = "All user roles retrieved successfully";
    public static final String ROLE_CHECK_COMPLETED = "Role check completed";
    public static final String FAILED_TO_ASSIGN_ROLE_TO_USER = "Failed to assign role to user";
    public static final String FAILED_TO_REMOVE_ROLE_FROM_USER = "Failed to remove role from user";
    public static final String FAILED_TO_RETRIEVE_USER_ROLES = "Failed to retrieve user roles";
    public static final String FAILED_TO_RETRIEVE_ROLE_USERS = "Failed to retrieve role users";
    public static final String FAILED_TO_RETRIEVE_ALL_USER_ROLES = "Failed to retrieve all user roles";
    public static final String FAILED_TO_CHECK_USER_ROLE = "Failed to check user role";
    
    // UserRole Detail Keys
    public static final String DETAIL_KEY_ROLE_ID = "roleId";
    public static final String DETAIL_KEY_USER_ROLE_ID = "userRoleId";
    public static final String DETAIL_KEY_TENANT_ID = "tenantId";
    public static final String DETAIL_KEY_ERROR_MESSAGE = "errorMessage";
    public static final String DETAIL_KEY_REMOVED = "removed";
    public static final String DETAIL_KEY_HAS_ROLE = "hasRole";

    // ==================== GRANULAR PERMISSIONS CONSTANTS ====================
    
    // Granular Permission Logger Names
    public static final String GRANULAR_PERMISSION_SERVICE_LOGGER_NAME = "GranularPermissionService";
    public static final String GRANULAR_PERMISSION_CONTROLLER_LOGGER_NAME = "GranularPermissionController";
    
    // Granular Permission Log Categories
    public static final String LOG_CATEGORY_GRANULAR_PERMISSION_MANAGEMENT = "GRANULAR_PERMISSION_MANAGEMENT";
    
    // Module Management Log Messages
    public static final String LOG_MODULE_CREATE_REQUEST = "Processing system module creation request";
    public static final String LOG_MODULE_CREATED = "System module created successfully";
    public static final String LOG_MODULE_CREATION_FAILED = "System module creation failed";
    
    // Page Management Log Messages
    public static final String LOG_PAGE_CREATE_REQUEST = "Processing system page creation request";
    public static final String LOG_PAGE_CREATED = "System page created successfully";
    public static final String LOG_PAGE_CREATION_FAILED = "System page creation failed";
    
    // Action Management Log Messages
    public static final String LOG_ACTION_CREATE_REQUEST = "Processing system action creation request";
    public static final String LOG_ACTION_CREATED = "System action created successfully";
    public static final String LOG_ACTION_CREATION_FAILED = "System action creation failed";
    
    // Hierarchical Permissions Log Messages
    public static final String LOG_HIERARCHICAL_PERMISSIONS_REQUEST = "Processing hierarchical permissions request";
    public static final String LOG_HIERARCHICAL_PERMISSIONS_SUCCESS = "Successfully retrieved hierarchical permissions";
    public static final String LOG_HIERARCHICAL_PERMISSIONS_FAILED = "Failed to retrieve hierarchical permissions";
    
    // Validation Log Messages
    public static final String LOG_MODULE_VALIDATION_FAILED = "Module validation failed";
    public static final String LOG_PAGE_VALIDATION_FAILED = "Page validation failed";
    public static final String LOG_ACTION_VALIDATION_FAILED = "Action validation failed";
    
    // Error Messages for Granular Permissions
    public static final String MODULE_CREATION_FAILED = "Failed to create system module";
    public static final String PAGE_CREATION_FAILED = "Failed to create system page";
    public static final String ACTION_CREATION_FAILED = "Failed to create system action";
    public static final String MODULE_NOT_FOUND = "Module not found";
    public static final String PAGE_NOT_FOUND = "Page not found";
    public static final String MODULE_ALREADY_EXISTS = "Module with this name already exists";
    public static final String PAGE_ALREADY_EXISTS = "Page with this name already exists";
    public static final String ACTION_ALREADY_EXISTS = "Action with this name already exists";
    
    // Granular Permission Object Types
    public static final String LOG_OBJECT_TYPE_MODULE_REQUEST = "SystemModuleRequest";
    public static final String LOG_OBJECT_TYPE_MODULE = "SystemModule";
    public static final String LOG_OBJECT_TYPE_PAGE_REQUEST = "SystemPageRequest";
    public static final String LOG_OBJECT_TYPE_PAGE = "SystemPage";
    public static final String LOG_OBJECT_TYPE_ACTION_REQUEST = "SystemActionRequest";
    public static final String LOG_OBJECT_TYPE_ACTION = "SystemAction";
    public static final String LOG_OBJECT_TYPE_HIERARCHICAL_PERMISSIONS_REQUEST = "HierarchicalPermissionsRequest";
    public static final String LOG_OBJECT_TYPE_HIERARCHICAL_PERMISSIONS_RESPONSE = "HierarchicalPermissionsResponse";
    
    // Detail Keys for Granular Permissions
    public static final String DETAIL_KEY_MODULE_ID = "moduleId";
    public static final String DETAIL_KEY_MODULE_NAME = "moduleName";
    public static final String DETAIL_KEY_PAGE_ID = "pageId";
    public static final String DETAIL_KEY_PAGE_NAME = "pageName";
    public static final String DETAIL_KEY_ACTION_ID = "actionId";
    public static final String DETAIL_KEY_ACTION_NAME = "actionName";
    public static final String DETAIL_KEY_ENDPOINT = "endpoint";
    public static final String DETAIL_KEY_ORIGINAL_PATH = "originalPath";
    public static final String DETAIL_KEY_PARENT_PATH = "parentPath";

    // Additional Detail Keys for UserRole Controller
    public static final String DETAIL_KEY_ROLES_COUNT = "rolesCount";
    public static final String DETAIL_KEY_USERS_COUNT = "usersCount";
    public static final String DETAIL_KEY_TOTAL_COUNT = "totalCount";
    public static final String DETAIL_KEY_REQUEST = "request";
    public static final String DETAIL_KEY_RESPONSE = "response";
    public static final String DETAIL_KEY_METHOD_NAME = "methodName";
    public static final String DETAIL_KEY_EXECUTION_TIME = "executionTime";

    // ==================== USER CONTROLLER CONSTANTS ====================
    
    // User Controller Logger Names
    public static final String USER_CONTROLLER_LOGGER_NAME = "UserController";
    
    // User Controller Log Categories
    public static final String LOG_CATEGORY_USER_CONTROLLER = "USER_CONTROLLER";
    
    // User Controller Log Messages
    public static final String LOG_USER_RETRIEVAL_ENDPOINT_CALLED = "User retrieval endpoint called";
    public static final String LOG_HEALTH_CHECK_ENDPOINT_CALLED = "Health check endpoint called";
    public static final String LOG_TENANT_ID_NOT_FOUND_IN_JWT = "Tenant ID not found in JWT token";
    public static final String LOG_PROCESSING_USER_RETRIEVAL_REQUEST = "Processing user retrieval request";
    public static final String LOG_USER_RETRIEVAL_SUCCESSFUL = "User retrieval successful";
    public static final String LOG_ERROR_RETRIEVING_USERS_BY_TENANT = "Error retrieving users by tenant";
    public static final String LOG_HEALTH_CHECK_COMPLETED = "Health check completed";
    public static final String LOG_HEALTH_CHECK_FAILED = "Health check failed";
    public static final String LOG_EXTRACTED_TENANT_ID_FROM_USER_CONTEXT = "Extracted tenant ID from UserContext";
    public static final String LOG_EXTRACTED_TENANT_ID_FROM_JWT_TOKEN = "Extracted tenant ID from JWT token";
    public static final String LOG_NO_AUTHORIZATION_HEADER_FOUND = "No Authorization header found in request";
    public static final String LOG_ERROR_EXTRACTING_TENANT_ID = "Error extracting tenant ID from request";
    
    // User Controller Response Messages
    public static final String TENANT_ID_NOT_FOUND_IN_JWT_TOKEN = "Tenant ID not found in JWT token";
    public static final String ERROR_RETRIEVING_USERS = "Error retrieving users";
    
    // User Controller Detail Keys
    public static final String DETAIL_KEY_USER_COUNT = "userCount";
    public static final String DETAIL_KEY_RESPONSE_STATUS = "responseStatus";
    public static final String DETAIL_KEY_IS_REACHABLE = "isReachable";
    public static final String DETAIL_KEY_STATUS = "status";
    public static final String DETAIL_KEY_TIMESTAMP = "timestamp";

    // ==================== USER GRPC CLIENT CONSTANTS ====================
    
    // User gRPC Client Logger Names
    public static final String USER_GRPC_CLIENT_LOGGER_NAME = "UserGrpcClient";
    
    // User gRPC Client Log Categories
    public static final String LOG_CATEGORY_USER_GRPC_CLIENT = "USER_GRPC_CLIENT";
    
    // User gRPC Client Log Messages
    public static final String LOG_INITIATING_GRPC_CALL_RETRIEVE_USERS = "Initiating gRPC call to retrieve users by tenant";
    public static final String LOG_SENDING_GRPC_REQUEST_TO_AUTH_SERVICE = "Sending gRPC request to auth-service";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_USERS_FROM_AUTH_SERVICE = "Successfully retrieved users from auth-service";
    public static final String LOG_GRPC_CALL_FAILED_WITH_STATUS_ERROR = "gRPC call failed with status error";
    public static final String LOG_UNEXPECTED_ERROR_DURING_GRPC_CALL = "Unexpected error during gRPC call";
    public static final String LOG_RETRIEVING_USER_LIST_BY_TENANT = "Retrieving user list by tenant";
    public static final String LOG_CONVERTED_GRPC_RESPONSE_TO_USER_LIST = "Converted gRPC response to user list";
    public static final String LOG_ERROR_CONVERTING_GRPC_RESPONSE_TO_USER_LIST = "Error converting gRPC response to user list";
    public static final String LOG_TESTING_CONNECTIVITY_TO_AUTH_SERVICE = "Testing connectivity to auth-service";
    public static final String LOG_AUTH_SERVICE_REACHABLE_VALIDATION_ERROR = "Auth-service is reachable (returned validation error as expected)";
    public static final String LOG_AUTH_SERVICE_CONNECTIVITY_TEST_FAILED = "Auth-service connectivity test failed";
    
    // User gRPC Client Error Messages
    public static final String ERROR_FAILED_TO_RETRIEVE_USERS_FROM_AUTH_SERVICE = "Failed to retrieve users from auth-service";
    public static final String ERROR_UNEXPECTED_ERROR_WHILE_RETRIEVING_USERS = "Unexpected error while retrieving users";
    
    // User gRPC Client Detail Keys
    public static final String DETAIL_KEY_SERVICE = "service";
    public static final String DETAIL_KEY_METHOD = "method";
    public static final String DETAIL_KEY_RESPONSE_TIME = "responseTime";
    public static final String DETAIL_KEY_DESCRIPTION = "description";
    
    // User gRPC Client Values
    public static final String VALUE_USER_GRPC_SERVICE = "UserGrpcService";
    public static final String VALUE_GET_USERS_BY_TENANT_METHOD = "getUsersByTenant";
    public static final String VALUE_SUCCESS = "success";
    public static final String VALUE_NO_DESCRIPTION = "No description";

    // ==================== GRPC CLIENT CONFIG CONSTANTS ====================
    
    // gRPC Client Config Logger Names
    public static final String GRPC_CLIENT_CONFIG_LOGGER_NAME = "GrpcClientConfig";
    
    // gRPC Client Config Log Categories
    public static final String LOG_CATEGORY_GRPC_CLIENT = "GRPC_CLIENT";
    
    // gRPC Client Config Log Messages
    public static final String LOG_INTERCEPTING_GRPC_CALL = "Intercepting gRPC call";
    public static final String LOG_ADDED_USER_CONTEXT_TO_GRPC_HEADERS = "Added user context to gRPC headers";
    public static final String LOG_NO_USER_CONTEXT_AVAILABLE_FOR_GRPC_CALL = "No user context available for gRPC call";
    public static final String LOG_ERROR_ADDING_USER_CONTEXT_TO_GRPC_HEADERS = "Error adding user context to gRPC headers";
    public static final String LOG_GRPC_CALL_COMPLETED = "gRPC call completed";
    
    // gRPC Client Config Detail Keys
    public static final String DETAIL_KEY_USERNAME = "username";
    
    // gRPC Metadata Keys
    public static final String GRPC_METADATA_TENANT_ID = "tenant-id";
    public static final String GRPC_METADATA_USER_ID = "user-id";
    public static final String GRPC_METADATA_USERNAME = "username";

    // ==================== USER DTO CONSTANTS ====================
    
    // User DTO Schema Descriptions
    public static final String USER_DTO_SCHEMA_DESCRIPTION = "User information retrieved from authentication service";
    public static final String USER_DTO_ID_DESCRIPTION = "Unique identifier for the user";
    public static final String USER_DTO_USERNAME_DESCRIPTION = "User's username";
    public static final String USER_DTO_EMAIL_DESCRIPTION = "User's email address";
    public static final String USER_DTO_FIRST_NAME_DESCRIPTION = "User's first name";
    public static final String USER_DTO_LAST_NAME_DESCRIPTION = "User's last name";
    public static final String USER_DTO_TENANT_ID_DESCRIPTION = "Tenant ID the user belongs to";
    public static final String USER_DTO_IS_ACTIVE_DESCRIPTION = "Whether the user account is active";
    public static final String USER_DTO_CREATED_AT_DESCRIPTION = "User creation timestamp";
    public static final String USER_DTO_UPDATED_AT_DESCRIPTION = "Last update timestamp";
    
    // User DTO Example Values
    public static final String USER_DTO_ID_EXAMPLE = "550e8400-e29b-41d4-a716-446655440000";
    public static final String USER_DTO_USERNAME_EXAMPLE = "john.doe";
    public static final String USER_DTO_EMAIL_EXAMPLE = "john.doe@company.com";
    public static final String USER_DTO_FIRST_NAME_EXAMPLE = "John";
    public static final String USER_DTO_LAST_NAME_EXAMPLE = "Doe";
    public static final String USER_DTO_TENANT_ID_EXAMPLE = "tenant_001";
    public static final String USER_DTO_IS_ACTIVE_EXAMPLE = "true";
    public static final String USER_DTO_CREATED_AT_EXAMPLE = "2024-01-15T10:30:00Z";
    public static final String USER_DTO_UPDATED_AT_EXAMPLE = "2024-01-20T14:45:00Z";
    
    // User DTO JSON Property Names
    public static final String USER_DTO_JSON_ID = "id";
    public static final String USER_DTO_JSON_USERNAME = "username";
    public static final String USER_DTO_JSON_EMAIL = "email";
    public static final String USER_DTO_JSON_FIRST_NAME = "firstName";
    public static final String USER_DTO_JSON_LAST_NAME = "lastName";
    public static final String USER_DTO_JSON_TENANT_ID = "tenantId";
    public static final String USER_DTO_JSON_IS_ACTIVE = "isActive";
    public static final String USER_DTO_JSON_CREATED_AT = "createdAt";
    public static final String USER_DTO_JSON_UPDATED_AT = "updatedAt";

    // ==================== USERS RESPONSE DTO CONSTANTS ====================
    
    // Users Response Schema Descriptions
    public static final String USERS_RESPONSE_SCHEMA_DESCRIPTION = "Response containing users for a specific tenant";
    public static final String USERS_RESPONSE_USERS_DESCRIPTION = "List of users belonging to the tenant";
    public static final String USERS_RESPONSE_TOTAL_COUNT_DESCRIPTION = "Total number of users found";
    public static final String USERS_RESPONSE_TENANT_ID_DESCRIPTION = "Tenant ID that was queried";
    public static final String USERS_RESPONSE_SUCCESS_DESCRIPTION = "Whether the operation was successful";
    public static final String USERS_RESPONSE_MESSAGE_DESCRIPTION = "Additional message about the operation";
    
    // Users Response Example Values
    public static final String USERS_RESPONSE_TOTAL_COUNT_EXAMPLE = "25";
    public static final String USERS_RESPONSE_TENANT_ID_EXAMPLE = "tenant_001";
    public static final String USERS_RESPONSE_SUCCESS_EXAMPLE = "true";
    public static final String USERS_RESPONSE_MESSAGE_EXAMPLE = "Users retrieved successfully";
    
    // Users Response JSON Property Names
    public static final String USERS_RESPONSE_JSON_USERS = "users";
    public static final String USERS_RESPONSE_JSON_TOTAL_COUNT = "totalCount";
    public static final String USERS_RESPONSE_JSON_TENANT_ID = "tenantId";
    public static final String USERS_RESPONSE_JSON_SUCCESS = "success";
    public static final String USERS_RESPONSE_JSON_MESSAGE = "message";
    
    // Users Response Messages
    public static final String USERS_RETRIEVED_SUCCESSFULLY_MESSAGE = "Users retrieved successfully";
    
    // Users Response toString Constants
    public static final String USERS_RESPONSE_USERS_SUFFIX = " users";
    public static final String USERS_RESPONSE_NULL_USERS = "null";

    // ==================== GLOBAL EXCEPTION HANDLER CONSTANTS ====================
    
    // Global Exception Handler Logger Names
    public static final String GLOBAL_EXCEPTION_HANDLER_LOGGER_NAME = "GlobalExceptionHandler";
    
    // Global Exception Handler Log Categories
    public static final String LOG_CATEGORY_EXCEPTION_HANDLING = "Exception-Handling";
    
    // Global Exception Handler Log Messages
    public static final String LOG_VALIDATION_ERROR_OCCURRED = "Validation error occurred";
    public static final String LOG_ACCESS_DENIED = "Access denied";
    public static final String LOG_RUNTIME_EXCEPTION_OCCURRED = "Runtime exception occurred";
    public static final String LOG_UNEXPECTED_EXCEPTION_OCCURRED = "Unexpected exception occurred";
    
    // Global Exception Handler Object Types
    public static final String LOG_OBJECT_TYPE_VALIDATION_ERROR = "ValidationError";
    public static final String LOG_OBJECT_TYPE_ACCESS_DENIED = "AccessDenied";
    public static final String LOG_OBJECT_TYPE_RUNTIME_EXCEPTION = "RuntimeException";
    public static final String LOG_OBJECT_TYPE_EXCEPTION = "Exception";
    
    // Global Exception Handler Detail Keys
    public static final String DETAIL_KEY_FIELD_ERRORS = "fieldErrors";
    public static final String DETAIL_KEY_EXCEPTION_TYPE = "exceptionType";
    
    // Global Exception Handler Response Keys
    public static final String RESPONSE_KEY_MESSAGE = "message";
    public static final String RESPONSE_KEY_STATUS = "status";
    public static final String RESPONSE_KEY_ERROR = "error";
    public static final String RESPONSE_KEY_ERRORS = "errors";
    public static final String RESPONSE_KEY_DATA = "data";

    // ==================== GRANULAR PERMISSION SERVICE IMPL CONSTANTS ====================
    
    // Granular Permission Service Validation Messages
    public static final String MODULE_NAME_CANNOT_BE_NULL_OR_EMPTY = "Module name cannot be null or empty";
    public static final String PAGE_NAME_CANNOT_BE_NULL_OR_EMPTY = "Page name cannot be null or empty";
    public static final String ACTION_NAME_CANNOT_BE_NULL_OR_EMPTY = "Action name cannot be null or empty";
    
    // Granular Permission Service Log Messages
    public static final String LOG_RETRIEVING_HIERARCHICAL_PERMISSIONS = "Retrieving hierarchical permissions";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_HIERARCHICAL_PERMISSIONS = "Successfully retrieved hierarchical permissions";
    public static final String LOG_FAILED_TO_RETRIEVE_HIERARCHICAL_PERMISSIONS = "Failed to retrieve hierarchical permissions";
    
    // Granular Permission Service Detail Keys
    public static final String DETAIL_KEY_MODULE_FILTER = "moduleFilter";
    public static final String DETAIL_KEY_TOTAL_AVAILABLE_MODULES = "totalAvailableModules";
    public static final String DETAIL_KEY_FILTERED_MODULES = "filteredModules";
    
    // Granular Permission Service Runtime Exception Messages
    public static final String FAILED_TO_RETRIEVE_HIERARCHICAL_PERMISSIONS = "Failed to retrieve hierarchical permissions";
    
    // HTTP Method Constants
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_DELETE = "DELETE";
    
    // HTTP Method Detection Keywords
    public static final String KEYWORD_CREATE = "create";
    public static final String KEYWORD_ADD = "add";
    public static final String KEYWORD_UPDATE = "update";
    public static final String KEYWORD_EDIT = "edit";
    public static final String KEYWORD_DELETE = "delete";
    public static final String KEYWORD_REMOVE = "remove";

    // UserRoleServiceImpl Constants  
    public static final String LOG_PROCESSING_USER_ROLE_ASSIGNMENT_REQUEST = "Processing user role assignment request";
    public static final String LOG_USER_ROLE_ASSIGNED_SUCCESSFULLY = "User role assigned successfully";
    public static final String LOG_FAILED_TO_ASSIGN_ROLE_TO_USER = "Failed to assign role to user";
    public static final String LOG_PROCESSING_USER_ROLE_REMOVAL_REQUEST = "Processing user role removal request";
    public static final String LOG_FAILED_TO_REMOVE_ROLE_FROM_USER = "Failed to remove role from user";
    public static final String LOG_RETRIEVING_USER_ROLES = "Retrieving user roles";
    public static final String LOG_FAILED_TO_RETRIEVE_USER_ROLES = "Failed to retrieve user roles";
    public static final String LOG_RETRIEVING_ROLE_USERS = "Retrieving role users";
    public static final String LOG_FAILED_TO_RETRIEVE_ROLE_USERS = "Failed to retrieve role users";
    public static final String LOG_RETRIEVING_ALL_USER_ROLES = "Retrieving all user roles";
    public static final String LOG_FAILED_TO_RETRIEVE_ALL_USER_ROLES = "Failed to retrieve all user roles";
    public static final String LOG_FAILED_TO_CHECK_USER_ROLE = "Failed to check user role";
    public static final String LOG_REMOVING_ALL_USER_ROLES = "Removing all user roles";
    public static final String LOG_ALL_USER_ROLES_REMOVED = "All user roles removed";
    public static final String LOG_FAILED_TO_REMOVE_ALL_USER_ROLES = "Failed to remove all user roles";
    public static final String LOG_REMOVING_ALL_ROLE_USERS = "Removing all role users";
    public static final String LOG_ALL_ROLE_USERS_REMOVED = "All role users removed";
    public static final String LOG_FAILED_TO_REMOVE_ALL_ROLE_USERS = "Failed to remove all role users";
    public static final String LOG_FAILED_TO_VALIDATE_USER_EXISTENCE = "Failed to validate user existence";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_REQUEST = "UserRoleAssignmentRequest";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT = "UserRoleAssignment";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_REQUEST = "UserRoleRemovalRequest";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_REMOVAL = "UserRoleRemoval";
    public static final String LOG_OBJECT_TYPE_USER_ROLES_QUERY = "UserRolesQuery";
    public static final String LOG_OBJECT_TYPE_USER_ROLES_QUERY_ERROR = "UserRolesQueryError";
    public static final String LOG_OBJECT_TYPE_ROLE_USERS_QUERY = "RoleUsersQuery";
    public static final String LOG_OBJECT_TYPE_ROLE_USERS_QUERY_ERROR = "RoleUsersQueryError";
    public static final String LOG_OBJECT_TYPE_ALL_USER_ROLES_QUERY = "AllUserRolesQuery";
    public static final String LOG_OBJECT_TYPE_ALL_USER_ROLES_QUERY_ERROR = "AllUserRolesQueryError";
    public static final String LOG_OBJECT_TYPE_USER_ROLE_CHECK_ERROR = "UserRoleCheckError";
    public static final String LOG_OBJECT_TYPE_REMOVE_ALL_USER_ROLES = "RemoveAllUserRoles";
    public static final String LOG_OBJECT_TYPE_ALL_USER_ROLES_REMOVED = "AllUserRolesRemoved";
    public static final String LOG_OBJECT_TYPE_REMOVE_ALL_USER_ROLES_ERROR = "RemoveAllUserRolesError";
    public static final String LOG_OBJECT_TYPE_REMOVE_ALL_ROLE_USERS = "RemoveAllRoleUsers";
    public static final String LOG_OBJECT_TYPE_ALL_ROLE_USERS_REMOVED = "AllRoleUsersRemoved";
    public static final String LOG_OBJECT_TYPE_REMOVE_ALL_ROLE_USERS_ERROR = "RemoveAllRoleUsersError";
    public static final String LOG_OBJECT_TYPE_USER_VALIDATION_ERROR = "UserValidationError";
    public static final String ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT = "Role not found or does not belong to tenant";
    public static final String USER_ALREADY_HAS_THIS_ROLE_ASSIGNED = "User already has this role assigned";
    public static final String USER_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT = "User not found or does not belong to tenant";
    public static final String UNABLE_TO_VALIDATE_USER_EXISTENCE = "Unable to validate user existence";
    public static final String DETAIL_KEY_DELETED_COUNT = "deletedCount";
    public static final String DEFAULT_UNKNOWN_ROLE = "Unknown Role";
    public static final String DEFAULT_ROLE_DETAILS_NOT_AVAILABLE = "Role details not available";

    // Bulk Operations Constants
    public static final String LOG_PROCESSING_BULK_USER_ROLE_ASSIGNMENT_REQUEST = "Processing bulk user role assignment request";
    public static final String LOG_BULK_USER_ROLE_ASSIGNED_SUCCESSFULLY = "Bulk user role assigned successfully";
    public static final String LOG_BULK_USER_ROLE_ASSIGNMENT_FAILED = "Bulk user role assignment failed";
    public static final String LOG_BULK_USER_ROLE_ASSIGNMENT_COMPLETED = "Bulk user role assignment completed";
    public static final String LOG_FAILED_TO_PROCESS_BULK_USER_ROLE_ASSIGNMENT = "Failed to process bulk user role assignment";
    public static final String LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_REQUEST = "BulkUserRoleAssignmentRequest";
    public static final String LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT = "BulkUserRoleAssignment";
    public static final String LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_ERROR = "BulkUserRoleAssignmentError";
    public static final String LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_RESPONSE = "BulkUserRoleAssignmentResponse";
    public static final String LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_ENDPOINT = "BulkUserRoleAssignmentEndpoint";
    public static final String LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_SUCCESS = "BulkUserRoleAssignmentSuccess";
    public static final String DETAIL_KEY_TOTAL_USERS = "totalUsers";
    public static final String DETAIL_KEY_SUCCESSFUL_ASSIGNMENTS = "successfulAssignments";
    public static final String DETAIL_KEY_FAILED_ASSIGNMENTS = "failedAssignments";
    public static final String BULK_USER_ROLES_ASSIGNED_SUCCESSFULLY = "Bulk user roles assigned successfully";
    public static final String FAILED_TO_ASSIGN_ROLES_TO_USERS = "Failed to assign roles to users";
    public static final String LOG_BULK_USER_ROLE_ASSIGNMENT_ENDPOINT_CALLED = "Bulk user role assignment endpoint called";
    public static final String LOG_BULK_USER_ROLE_ASSIGNMENT_SUCCESSFUL = "Bulk user role assignment successful";
    public static final String LOG_BULK_USER_ROLE_ASSIGNMENT_FAILED_CONTROLLER = "Bulk user role assignment failed in controller";

    // ===================================================================
    // PERMISSION CHECK CONTROLLER LOG MESSAGES
    // ===================================================================
    public static final String LOG_PERMISSION_CHECK_REQUEST_STARTED = "Permission check request started";
    public static final String LOG_PERMISSION_CHECK_REQUEST_COMPLETED = "Permission check request completed";
    public static final String LOG_PERMISSION_CHECK_PARAMETERS_RECEIVED = "Permission check parameters received";
    public static final String LOG_PERMISSION_CHECK_SERVICE_CALL = "Calling user permission service";
    public static final String LOG_PERMISSION_CHECK_SERVICE_RESPONSE = "User permission service response received";
    public static final String LOG_PERMISSION_CHECK_RESPONSE_BUILDING = "Building permission check response";
    public static final String LOG_PERMISSION_CHECK_RESPONSE_BUILT = "Permission check response built successfully";
    public static final String LOG_ENDPOINT_CHECK_REQUEST_STARTED = "Endpoint check request started";
    public static final String LOG_ENDPOINT_CHECK_REQUEST_COMPLETED = "Endpoint check request completed";
    public static final String LOG_ENDPOINT_CHECK_PARAMETERS_RECEIVED = "Endpoint check parameters received";
    public static final String LOG_ENDPOINT_CHECK_SERVICE_CALL = "Calling endpoint management service";
    public static final String LOG_ENDPOINT_CHECK_SERVICE_RESPONSE = "Endpoint management service response received";
    public static final String LOG_ENDPOINT_CHECK_RETURNING_SAFE_VALUE = "Returning safe value due to error";
    public static final String LOG_CHECKING_IF_ENDPOINT_EXISTS = "Checking if endpoint exists";
    
    // ===================================================================
    // PERMISSION CHECK CONTROLLER ERROR MESSAGES
    // ===================================================================
    public static final String ERROR_PERMISSION_CHECK_FAILED = "Permission check failed";
    public static final String ERROR_ENDPOINT_CHECK_FAILED = "Endpoint check failed";
    public static final String ERROR_INVALID_PERMISSION_CHECK_PARAMETERS = "Invalid permission check parameters";
    public static final String ERROR_INVALID_ENDPOINT_CHECK_PARAMETERS = "Invalid endpoint check parameters";
    
    // ===================================================================
    // PERMISSION CHECK CONTROLLER DETAIL KEYS
    // ===================================================================
    public static final String DETAIL_USER_ID = "userId";
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_API_ENDPOINT = "apiEndpoint";
    public static final String DETAIL_HTTP_METHOD = "httpMethod";
    public static final String DETAIL_ENDPOINT = "endpoint";
    public static final String DETAIL_METHOD = "method";
    public static final String DETAIL_HAS_PERMISSION = "hasPermission";
    public static final String DETAIL_IS_MANAGED = "isManaged";
    public static final String DETAIL_RESPONSE_SIZE = "responseSize";
    public static final String DETAIL_PROCESSING_TIME = "processingTimeMs";
    
    // ===================================================================
    // PERMISSION CHECK CONTROLLER OBJECT TYPES
    // ===================================================================
    public static final String OBJECT_TYPE_PERMISSION_CHECK = "PermissionCheck";
    public static final String OBJECT_TYPE_ENDPOINT_CHECK = "EndpointCheck";
    public static final String OBJECT_TYPE_PERMISSION_CONTROLLER = "PermissionCheckController";

    // ===================================================================
    // USER PERMISSION SERVICE LOG MESSAGES
    // ===================================================================
    public static final String LOG_CHECKING_USER_PERMISSION_FOR_ENDPOINT = "Checking user permission for endpoint";
    public static final String LOG_FOUND_MATCHING_ACTIONS_FOR_ENDPOINT = "Found matching actions for endpoint";
    public static final String LOG_FOUND_USER_ROLES_IN_TENANT = "Found user roles in tenant";
    public static final String LOG_STARTING_PERMISSION_CHECK_FOR_USER_ROLES_AND_ACTIONS = "Starting permission check for user roles and actions";
    public static final String LOG_CHECKING_PERMISSIONS_FOR_USER_ROLE = "Checking permissions for user role";
    public static final String LOG_CHECKING_PERMISSIONS_FOR_ACTION = "Checking permissions for action";
    public static final String LOG_FOUND_PERMISSIONS_FOR_ACTION = "Found permissions for action";
    public static final String LOG_CHECKING_ROLE_PERMISSION_ASSOCIATION = "Checking role-permission association";
    public static final String LOG_ROLE_PERMISSION_CHECK_RESULT = "Role-permission check result";
    public static final String LOG_PERMISSION_GRANTED_FOR_USER = "Permission granted for user";
    public static final String LOG_CHECKING_IF_ENDPOINT_IS_MANAGED = "Checking if endpoint is managed";
    public static final String LOG_ENDPOINT_MANAGEMENT_CHECK_RESULT = "Endpoint management check result";
    public static final String LOG_ENDPOINT_NOT_FOUND_IN_ACTION_TABLE_NOT_MANAGED = "Endpoint not found in Action table - not managed";
    public static final String LOG_VALIDATING_HTTP_METHOD_ENUM = "Validating HTTP method enum";
    public static final String LOG_VALIDATING_USER_ID_FORMAT = "Validating user ID format";
    public static final String LOG_SEARCHING_ACTIONS_FOR_ENDPOINT_AND_METHOD = "Searching actions for endpoint and method";
    public static final String LOG_SEARCHING_USER_ROLES_IN_TENANT = "Searching user roles in tenant";
    public static final String LOG_ITERATING_THROUGH_USER_ROLES = "Iterating through user roles";
    public static final String LOG_ITERATING_THROUGH_MATCHING_ACTIONS = "Iterating through matching actions";
    public static final String LOG_SEARCHING_PERMISSIONS_FOR_ACTION = "Searching permissions for action";
    public static final String LOG_ITERATING_THROUGH_PERMISSIONS = "Iterating through permissions";
    public static final String LOG_QUERYING_ROLE_PERMISSION_ASSOCIATION = "Querying role-permission association";
    public static final String LOG_CHECKING_ROLES_WITH_PERMISSIONS_FOR_ACTIONS = "Checking roles with permissions for actions";
    public static final String LOG_COUNTING_ROLES_FOR_PERMISSION = "Counting roles for permission";
    
    // ===================================================================
    // USER PERMISSION SERVICE WARNING MESSAGES
    // ===================================================================
    public static final String WARN_INVALID_HTTP_METHOD = "Invalid HTTP method";
    public static final String WARN_NO_ACTIONS_FOUND_FOR_ENDPOINT = "No actions found for endpoint";
    public static final String WARN_INVALID_USER_ID_FORMAT = "Invalid userId format";
    public static final String WARN_NO_ROLES_FOUND_FOR_USER_IN_TENANT = "No roles found for user in tenant";
    public static final String WARN_NO_PERMISSION_FOUND_FOR_USER_ENDPOINT_ACCESS = "No permission found for user endpoint access";
    
    // ===================================================================
    // USER PERMISSION SERVICE ERROR MESSAGES
    // ===================================================================
    public static final String ERROR_CHECKING_USER_PERMISSION = "Error checking user permission";
    public static final String ERROR_CHECKING_IF_ENDPOINT_IS_MANAGED = "Error checking if endpoint is managed";
    
    // ===================================================================
    // USER PERMISSION SERVICE DETAIL KEYS
    // ===================================================================
    public static final String DETAIL_ACTION_COUNT = "actionCount";
    public static final String DETAIL_ROLE_COUNT = "roleCount";
    public static final String DETAIL_USER_ROLE_COUNT = "userRoleCount";
    public static final String DETAIL_ROLE_ID = "roleId";
    public static final String DETAIL_ACTION_ID = "actionId";
    public static final String DETAIL_ACTION_NAME = "actionName";
    public static final String DETAIL_PERMISSION_COUNT = "permissionCount";
    public static final String DETAIL_PERMISSION_ID = "permissionId";
    public static final String DETAIL_ACTIONS_FOUND = "actionsFound";
    public static final String DETAIL_HAS_ROLE_PERMISSIONS = "hasRolePermissions";
    public static final String DETAIL_ROLE_COUNT_FOR_PERMISSION = "roleCountForPermission";
    
    // ===================================================================
    // USER PERMISSION SERVICE OBJECT TYPES  
    // ===================================================================
    public static final String OBJECT_TYPE_USER_PERMISSION_SERVICE = "UserPermissionService";
    public static final String OBJECT_TYPE_HTTP_METHOD_VALIDATION = "HttpMethodValidation";
    public static final String OBJECT_TYPE_USER_ID_VALIDATION = "UserIdValidation";
    public static final String OBJECT_TYPE_ACTION_SEARCH = "ActionSearch";
    public static final String OBJECT_TYPE_USER_ROLE_SEARCH = "UserRoleSearch";
    public static final String OBJECT_TYPE_PERMISSION_SEARCH = "PermissionSearch";
    public static final String OBJECT_TYPE_ROLE_PERMISSION_CHECK = "RolePermissionCheck";
    public static final String OBJECT_TYPE_ENDPOINT_MANAGEMENT_CHECK = "EndpointManagementCheck";

    // ===================================================================
    // USER PERMISSION SERVICE ADDITIONAL LOG MESSAGES (HARD-CODED FIXES)
    // ===================================================================
    public static final String LOG_ROLE_HAS_BASIC_PERMISSION_CHECKING_CONDITIONAL_RULES = "Role has basic permission, checking conditional rules";
    public static final String LOG_EVALUATING_CONDITIONAL_RULE = "Evaluating conditional rule";
    public static final String LOG_CONDITIONAL_RULE_NOT_MET = "Conditional rule not met";
    public static final String LOG_CONDITIONAL_RULE_SATISFIED = "Conditional rule satisfied";
    public static final String LOG_PERMISSION_DENIED_DUE_TO_UNMET_CONDITIONAL_RULES = "Permission denied due to unmet conditional rules";
    public static final String LOG_UNABLE_TO_RETRIEVE_USER_DATA_FOR_CONDITIONAL_RULE_EVALUATION = "Unable to retrieve user data for conditional rule evaluation";
    public static final String LOG_RETRIEVING_USER_DATA_VIA_GRPC = "Retrieving user data via gRPC";
    public static final String LOG_RETRIEVED_USERS_FROM_TENANT_SEARCHING_FOR_SPECIFIC_USER = "Retrieved users from tenant, searching for specific user";
    public static final String LOG_USER_IN_TENANT = "User in tenant";
    public static final String LOG_USER_NOT_FOUND_IN_TENANT = "User not found in tenant";
    public static final String LOG_FOUND_USER_IN_TENANT = "Found user in tenant";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_USER_DATA_VIA_GRPC = "Successfully retrieved user data via gRPC";
    public static final String LOG_FAILED_TO_RETRIEVE_USER_DATA_VIA_GRPC = "Failed to retrieve user data via gRPC";

    // ===================================================================
    // USER PERMISSION SERVICE ADDITIONAL DETAIL KEYS
    // ===================================================================
    public static final String DETAIL_CONDITIONAL_RULES_COUNT = "conditionalRulesCount";
    public static final String DETAIL_ALL_CONDITIONS_MET = "allConditionsMet";
    public static final String DETAIL_CONDITION_MET = "conditionMet";
    public static final String DETAIL_TOTAL_USERS_RETRIEVED = "totalUsersRetrieved";
    public static final String DETAIL_USER_ID_IN_LIST = "userIdInList";
    public static final String DETAIL_USERNAME_IN_LIST = "usernameInList";
    public static final String DETAIL_EMAIL_IN_LIST = "emailInList";
    public static final String DETAIL_SEARCHING_FOR_USER_ID = "searchingForUserId";
    public static final String DETAIL_TOTAL_USERS_IN_TENANT = "totalUsersInTenant";
    public static final String DETAIL_FOUND_USER_EMAIL = "foundUserEmail";
    public static final String DETAIL_FOUND_USER_STATUS = "foundUserStatus";
    public static final String DETAIL_USER_EMAIL = "userEmail";
    public static final String DETAIL_USER_STATUS = "userStatus";
    public static final String DETAIL_CONDITIONAL_RULE_ID = "conditionalRuleId";
    public static final String DETAIL_CONDITION_SCRIPT = "conditionScript";

    // ===================================================================
    // USER PERMISSION SERVICE GRPC OBJECT TYPES
    // ===================================================================
    public static final String OBJECT_TYPE_GRPC_USER_DATA = "GRPC_USER_DATA";

    // ===================================================================
    // GET USERS BY ROLE ENDPOINT CONSTANTS
    // ===================================================================
    
    // Success Messages
    public static final String USERS_BY_ROLE_RETRIEVED_SUCCESS = "Users retrieved successfully with role status";
    
    // Error Messages
    public static final String FAILED_TO_RETRIEVE_USERS_BY_ROLE = "Failed to retrieve users for role";
    public static final String INVALID_ROLE_ID_FOR_USERS_RETRIEVAL = "Invalid role ID format for users retrieval";
    public static final String ROLE_NOT_FOUND_FOR_USERS_RETRIEVAL = "Role not found for users retrieval";
    public static final String FAILED_TO_FETCH_TENANT_USERS_GRPC = "Failed to fetch tenant users via gRPC";
    public static final String FAILED_TO_FETCH_USER_ROLE_ASSIGNMENTS = "Failed to fetch user role assignments";
    
    // ===================================================================
    // GET ROLE OWNERS ENDPOINT CONSTANTS
    // ===================================================================
    
    // Success Messages
    public static final String ROLE_OWNERS_RETRIEVED_SUCCESS = "Role owners retrieved successfully";
    
    // Error Messages
    public static final String FAILED_TO_RETRIEVE_ROLE_OWNERS = "Failed to retrieve role owners";
    public static final String ROLE_NOT_FOUND_FOR_OWNERS_RETRIEVAL = "Role not found for owners retrieval";
    
    // Log Messages - Controller
    public static final String LOG_GET_USERS_BY_ROLE_ENDPOINT_CALLED = "Get users by role endpoint called";
    public static final String LOG_GET_USERS_BY_ROLE_SUCCESSFUL = "Get users by role completed successfully";
    public static final String LOG_GET_USERS_BY_ROLE_FAILED_CONTROLLER = "Get users by role failed in controller";
    public static final String LOG_GET_MANUAL_USERS_BY_ROLE_ENDPOINT_CALLED = "Get manually assigned users by role endpoint called";
    public static final String LOG_GET_MANUAL_USERS_BY_ROLE_SUCCESSFUL = "Get manually assigned users by role successful";
    public static final String LOG_GET_MANUAL_USERS_BY_ROLE_FAILED_CONTROLLER = "Get manually assigned users by role failed in controller";
    public static final String FAILED_TO_RETRIEVE_MANUAL_USERS = "Failed to retrieve manually assigned users";
    
    // Log Messages - Service
    public static final String LOG_PROCESSING_GET_USERS_BY_ROLE_REQUEST = "Processing get users by role request";
    public static final String LOG_PROCESSING_GET_MANUAL_USERS_BY_ROLE_REQUEST = "Processing get manually assigned users by role request";
    public static final String LOG_FETCHING_TENANT_USERS_VIA_GRPC = "Fetching all tenant users via gRPC";
    public static final String LOG_FETCHING_ROLE_ASSIGNMENTS_FOR_ROLE = "Fetching role assignments for specific role";
    public static final String LOG_FETCHING_MANUAL_USERS_FOR_ROLE = "Fetching manually assigned users for role";
    public static final String LOG_FOUND_MANUAL_USERS_WITH_ROLE = "Found manually assigned users with role";
    public static final String LOG_MAPPING_USERS_WITH_ROLE_STATUS = "Mapping users with hasRole status";
    public static final String LOG_SORTING_USERS_BY_ROLE_STATUS = "Sorting users with role first, then without role";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_USERS_BY_ROLE = "Successfully retrieved users with role status";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_MANUAL_USERS_BY_ROLE = "Successfully retrieved manually assigned users by role";
    public static final String LOG_FAILED_TO_RETRIEVE_USERS_BY_ROLE = "Failed to retrieve users with role status";
    public static final String LOG_FAILED_TO_RETRIEVE_MANUAL_USERS_BY_ROLE = "Failed to retrieve manually assigned users by role";
    public static final String LOG_ROLE_VALIDATION_FOR_USERS_RETRIEVAL = "Validating role exists and belongs to tenant";
    public static final String LOG_INVALID_PAGE_SIZE_DEFAULTING = "Invalid page size requested, defaulting to 10";
    public static final String LOG_INVALID_STATUS_FILTER_DEFAULTING = "Invalid status filter, defaulting to ALL";
    public static final String MANUALLY_ASSIGNED_USERS_RETRIEVED_SUCCESS = "Manually assigned users retrieved successfully";
    public static final String LOG_FOUND_USERS_WITH_ROLE = "Found users with the specified role";
    public static final String LOG_FOUND_USERS_WITHOUT_ROLE = "Found users without the specified role";
    
    // Role Members Endpoint - Log Messages
    public static final String LOG_GET_ROLE_MEMBERS_ENDPOINT_CALLED = "Get role members endpoint called";
    public static final String LOG_GET_ROLE_MEMBERS_SUCCESSFUL = "Get role members completed successfully";
    public static final String LOG_GET_ROLE_MEMBERS_FAILED_CONTROLLER = "Get role members failed in controller";
    public static final String LOG_PROCESSING_GET_ROLE_MEMBERS_REQUEST = "Processing get role members request";
    public static final String LOG_FETCHING_ROLE_MEMBERS_WITH_ASSIGNMENT_TYPE = "Fetching role members with assignment type";
    public static final String LOG_FOUND_ROLE_MEMBERS = "Found role members for role";
    public static final String LOG_FILTERING_ROLE_MEMBERS = "Filtering role members by search and status";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_ROLE_MEMBERS = "Successfully retrieved role members";
    public static final String LOG_FAILED_TO_RETRIEVE_ROLE_MEMBERS = "Failed to retrieve role members";
    public static final String ROLE_MEMBERS_RETRIEVED_SUCCESS = "Role members retrieved successfully";
    public static final String FAILED_TO_RETRIEVE_ROLE_MEMBERS = "Failed to retrieve role members";
    public static final String ROLE_NOT_FOUND_FOR_MEMBERS_RETRIEVAL = "Role not found for members retrieval";
    
    // Detail Keys
    public static final String DETAIL_KEY_TOTAL_USERS_COUNT = "totalUsersCount";
    public static final String DETAIL_KEY_USERS_WITH_ROLE_COUNT = "usersWithRoleCount";
    public static final String DETAIL_KEY_USERS_WITHOUT_ROLE_COUNT = "usersWithoutRoleCount";
    public static final String DETAIL_KEY_MANUAL_ASSIGNMENT_COUNT = "manualAssignmentCount";
    public static final String DETAIL_KEY_DYNAMIC_ASSIGNMENT_COUNT = "dynamicAssignmentCount";
    public static final String DETAIL_KEY_TOTAL_MEMBERS_COUNT = "totalMembersCount";
    public static final String DETAIL_KEY_USER_HAS_ROLE = "userHasRole";

    // ==================== DYNAMIC RULE MANAGEMENT CONSTANTS ====================
    
    // Dynamic Rule Success Messages
    public static final String DYNAMIC_RULE_CREATED_SUCCESSFULLY = "Dynamic rule created successfully";
    public static final String FAILED_TO_CREATE_DYNAMIC_RULE = "Failed to create dynamic rule";
    
    // Dynamic Rule Log Messages - Controller
    public static final String LOG_DYNAMIC_RULE_CREATION_ENDPOINT_CALLED = "Dynamic rule creation endpoint called";
    public static final String LOG_DYNAMIC_RULE_CREATED_SUCCESSFULLY = "Dynamic rule created successfully";
    public static final String LOG_DYNAMIC_RULE_CREATION_FAILED_INVALID_ARGUMENTS = "Dynamic rule creation failed - invalid arguments";
    public static final String LOG_DYNAMIC_RULE_CREATION_FAILED = "Dynamic rule creation failed";
    
    // Dynamic Rule Log Messages - Trace Level
    public static final String LOG_TRACE_ENTERING_CREATE_DYNAMIC_RULE_METHOD = "Entering createDynamicRule method";
    public static final String LOG_TRACE_EXITING_CREATE_DYNAMIC_RULE_METHOD = "Exiting createDynamicRule method";
    
    // Dynamic Rule Log Messages - Debug Level
    public static final String LOG_DEBUG_STARTING_DYNAMIC_RULE_CREATION = "Starting dynamic rule creation process";
    
    // Dynamic Rule Service Log Messages
    public static final String LOG_DEBUG_STARTING_DYNAMIC_RULE_CREATION_SERVICE = "Starting dynamic rule creation";
    public static final String LOG_ERROR_ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT = "Role not found or does not belong to tenant";
    public static final String LOG_INFO_DYNAMIC_RULE_CREATED_SUCCESSFULLY_SERVICE = "Dynamic rule created successfully";
    
    // Dynamic Rule Service Error Messages
    public static final String ERROR_ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT = "Role not found or does not belong to tenant";
    
    // Dynamic Rule Execution Log Messages
    public static final String LOG_STARTING_DYNAMIC_RULE_EXECUTION = "Starting dynamic rule execution";
    public static final String LOG_RETRIEVING_DYNAMIC_RULE = "Retrieving dynamic rule from database";
    public static final String LOG_DYNAMIC_RULE_FOUND = "Dynamic rule found in database";
    public static final String LOG_FETCHING_TENANT_USERS_FOR_DYNAMIC_RULE = "Fetching tenant users for dynamic rule evaluation";
    public static final String LOG_EVALUATING_CONDITION_SCRIPT_FOR_USERS = "Evaluating condition script for tenant users";
    public static final String LOG_USER_MATCHES_CONDITION = "User matches dynamic rule condition";
    public static final String LOG_USER_DOES_NOT_MATCH_CONDITION = "User does not match dynamic rule condition";
    public static final String LOG_ATTEMPTING_ROLE_ASSIGNMENT = "Attempting role assignment for user";
    public static final String LOG_ROLE_ASSIGNMENT_SUCCESSFUL = "Role assignment successful for user";
    public static final String LOG_ROLE_ASSIGNMENT_SKIPPED_ALREADY_HAS_ROLE = "Role assignment skipped - user already has role";
    public static final String LOG_ROLE_ASSIGNMENT_FAILED = "Role assignment failed for user";
    public static final String LOG_DYNAMIC_RULE_EXECUTION_COMPLETED = "Dynamic rule execution completed";
    public static final String LOG_DYNAMIC_RULE_EXECUTION_FAILED = "Dynamic rule execution failed";
    
    // Dynamic Rule Execution Error Messages
    public static final String ERROR_DYNAMIC_RULE_NOT_FOUND = "Dynamic rule not found";
    public static final String ERROR_FAILED_TO_FETCH_TENANT_USERS = "Failed to fetch tenant users";
    public static final String ERROR_FAILED_TO_EXECUTE_DYNAMIC_RULE = "Failed to execute dynamic rule";
    public static final String LOG_CANNOT_EXECUTE_DYNAMIC_RULE_ROLE_INACTIVE = "Cannot execute dynamic rule: role is inactive";
    public static final String ERROR_CANNOT_ASSIGN_INACTIVE_ROLE = "Cannot assign inactive role: ";
    public static final String LOG_CANNOT_EXECUTE_SCHEDULED_DYNAMIC_RULE_ROLE_INACTIVE = "Cannot execute scheduled dynamic rule: role is inactive";
    public static final String ERROR_CANNOT_ASSIGN_INACTIVE_ROLE_VIA_SCHEDULED_EXECUTION = "Cannot assign inactive role via scheduled execution: ";
    
    // Dynamic Rule Immediate Execution Messages
    public static final String LOG_EXECUTING_DYNAMIC_RULE_IMMEDIATELY = "Executing dynamic rule immediately due to IMMEDIATE schedule type";
    public static final String LOG_IMMEDIATE_DYNAMIC_RULE_EXECUTION_COMPLETED = "Immediate dynamic rule execution completed";
    public static final String LOG_FAILED_TO_EXECUTE_DYNAMIC_RULE_IMMEDIATELY = "Failed to execute dynamic rule immediately";
    public static final String LOG_ERROR_EVALUATING_CONDITION_FOR_USER = "Error evaluating condition for user";

    // Dynamic Rule Unique Constraint Messages
    public static final String ERROR_DYNAMIC_RULE_ALREADY_EXISTS_FOR_ROLE = "Dynamic rule already exists for this role. Each role can only have one dynamic rule.";
    public static final String LOG_CHECKING_EXISTING_DYNAMIC_RULE_FOR_ROLE = "Checking if dynamic rule already exists for role";
    public static final String LOG_NO_EXISTING_DYNAMIC_RULE_FOUND = "No existing dynamic rule found for role, proceeding with creation";
    public static final String LOG_EXISTING_DYNAMIC_RULE_FOUND = "Existing dynamic rule found for role";

    // Hierarchical Permissions Available Roles Messages
    public static final String LOG_SUCCESSFULLY_RETRIEVED_AVAILABLE_ROLES = "Successfully retrieved available roles for hierarchical permissions";
    public static final String LOG_FAILED_TO_RETRIEVE_AVAILABLE_ROLES = "Failed to retrieve available roles for hierarchical permissions";
    public static final String LOG_FILTERING_ACTIVE_ROLES_ONLY = "Filtering available roles to include only active roles";
    public static final String LOG_FILTERED_INACTIVE_ROLES = "Filtered out inactive roles from available roles list";
    public static final String DETAIL_KEY_AVAILABLE_ROLES_COUNT = "availableRolesCount";
    public static final String DETAIL_KEY_ACTIVE_ROLES_COUNT = "activeRolesCount";
    public static final String DETAIL_KEY_INACTIVE_ROLES_FILTERED = "inactiveRolesFiltered";
    public static final String DETAIL_KEY_ROLE_FILTER = "roleFilter";

    // Dynamic Rule Scheduler Constants
    public static final String DYNAMIC_RULE_SCHEDULER_EXECUTION = "DYNAMIC_RULE_SCHEDULER_EXECUTION";
    public static final String DYNAMIC_RULE_SCHEDULER_HEALTH = "DYNAMIC_RULE_SCHEDULER_HEALTH";
    public static final String DYNAMIC_RULE_SCHEDULER_CIRCUIT_BREAKER = "DYNAMIC_RULE_SCHEDULER_CIRCUIT_BREAKER";
    public static final String DYNAMIC_RULE_SCHEDULER_CLEANUP = "DYNAMIC_RULE_SCHEDULER_CLEANUP";
    public static final String DYNAMIC_RULE_SCHEDULER_MONITORING = "DYNAMIC_RULE_SCHEDULER_MONITORING";
    public static final String DYNAMIC_RULE_SCHEDULER_THREAD_POOL = "DYNAMIC_RULE_SCHEDULER_THREAD_POOL";
    public static final String DYNAMIC_RULE_EXECUTION_HISTORY = "DYNAMIC_RULE_EXECUTION_HISTORY";
    public static final String DYNAMIC_RULE_SCHEDULE_EVALUATION = "DYNAMIC_RULE_SCHEDULE_EVALUATION";
    public static final String DYNAMIC_RULE_TENANT_EXECUTION = "DYNAMIC_RULE_TENANT_EXECUTION";
    public static final String DYNAMIC_RULE_PERFORMANCE_METRICS = "DYNAMIC_RULE_PERFORMANCE_METRICS";

    // ==================== DYNAMIC RULE SERVICE IMPLEMENTATION CONSTANTS ====================
    
    // Dynamic Rule Execution with Tenant Context
    public static final String LOG_STARTING_SCHEDULED_DYNAMIC_RULE_EXECUTION_WITH_TENANT = "Starting scheduled dynamic rule execution with tenant context";
    public static final String LOG_SCHEDULED_DYNAMIC_RULE_RECONCILIATION_COMPLETED = "Scheduled dynamic rule reconciliation completed successfully";
    public static final String LOG_FAILED_TO_EXECUTE_SCHEDULED_DYNAMIC_RULE_RECONCILIATION = "Failed to execute scheduled dynamic rule reconciliation";
    public static final String LOG_SCHEDULED_DYNAMIC_RULE_EXECUTION_FAILED = "Scheduled dynamic rule execution failed";
    public static final String LOG_NO_USERS_FOUND_FOR_TENANT_DURING_SCHEDULED_EXECUTION = "No users found for tenant during scheduled execution";
    public static final String LOG_EXECUTING_SCHEDULED_DYNAMIC_RULE_WITH_RECONCILIATION = "Executing scheduled dynamic rule with reconciliation";
    public static final String LOG_FETCHING_TENANT_USERS_FOR_SCHEDULED_EXECUTION = "Fetching tenant users for scheduled execution";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_USERS_FOR_SCHEDULED_EXECUTION = "Successfully retrieved users for scheduled execution";
    public static final String LOG_FAILED_TO_FETCH_TENANT_USERS_FOR_SCHEDULED_EXECUTION = "Failed to fetch tenant users for scheduled execution";
    
    // Dynamic Rule Update Operations
    public static final String LOG_STARTING_DYNAMIC_RULE_UPDATE = "Starting dynamic rule update";
    public static final String LOG_FOUND_EXISTING_DYNAMIC_RULE_FOR_UPDATE = "Found existing dynamic rule for update";
    public static final String LOG_DYNAMIC_RULE_UPDATED_SUCCESSFULLY = "Dynamic rule updated successfully";
    public static final String LOG_SCHEDULE_CHANGED_FROM_DAILY_TO_IMMEDIATE = "Schedule changed from DAILY to IMMEDIATE - executing rule with reconciliation";
    public static final String LOG_CONDITION_SCRIPT_CHANGED_FOR_IMMEDIATE_RULE = "Condition script changed for IMMEDIATE rule - executing reconciliation";
    public static final String LOG_DYNAMIC_RULE_RECONCILIATION_COMPLETED_AFTER_SCHEDULE_CHANGE = "Dynamic rule reconciliation completed after schedule change";
    public static final String LOG_DYNAMIC_RULE_RECONCILIATION_COMPLETED_AFTER_CONDITION_CHANGE = "Dynamic rule reconciliation completed after condition change";
    public static final String LOG_FAILED_TO_EXECUTE_DYNAMIC_RULE_RECONCILIATION_AFTER_SCHEDULE_CHANGE = "Failed to execute dynamic rule reconciliation after schedule change";
    public static final String LOG_FAILED_TO_EXECUTE_DYNAMIC_RULE_RECONCILIATION_AFTER_CONDITION_CHANGE = "Failed to execute dynamic rule reconciliation after condition change";
    public static final String LOG_DYNAMIC_RULE_ACTIVATED_TRIGGERING_RECONCILIATION = "Dynamic rule activated - triggering reconciliation for all users";
    public static final String LOG_DYNAMIC_RULE_RECONCILIATION_COMPLETED_AFTER_ACTIVATION = "Dynamic rule reconciliation completed after activation";
    public static final String LOG_FAILED_TO_EXECUTE_RECONCILIATION_AFTER_RULE_ACTIVATION = "Failed to execute reconciliation after rule activation";
    public static final String LOG_DYNAMIC_RULE_DEACTIVATED_REMOVING_ROLE = "Dynamic rule deactivated - removing role from all users";
    public static final String LOG_FAILED_TO_EXECUTE_RECONCILIATION_AFTER_RULE_DEACTIVATION = "Failed to execute reconciliation after rule deactivation";
    
    // Dynamic Rule Immediate Execution with Reconciliation
    public static final String LOG_EXECUTING_DYNAMIC_RULE_IMMEDIATELY_WITH_RECONCILIATION = "Executing dynamic rule immediately with reconciliation";
    public static final String LOG_DYNAMIC_RULE_RECONCILIATION_COMPLETED_FOR_NEW_RULE = "Dynamic rule reconciliation completed for new rule";
    public static final String LOG_FAILED_TO_EXECUTE_DYNAMIC_RULE_RECONCILIATION_IMMEDIATELY = "Failed to execute dynamic rule reconciliation immediately";
    
    // JWT Token Extraction and Audit Fields
    public static final String LOG_FAILED_TO_EXTRACT_USER_INFO_FROM_JWT_USING_SYSTEM_USER = "Failed to extract user info from JWT, using system user";
    
    // Role Assignment Reconciliation Process
    public static final String LOG_STARTING_ROLE_ASSIGNMENT_RECONCILIATION = "Starting role assignment reconciliation";
    public static final String LOG_FAILED_TO_EVALUATE_CONDITION_FOR_USER_DURING_RECONCILIATION = "Failed to evaluate condition for user during reconciliation";
    public static final String LOG_ROLE_RECONCILIATION_ANALYSIS_COMPLETED = "Role reconciliation analysis completed";
    public static final String LOG_SUCCESSFULLY_REMOVED_ROLE_FROM_USER = "Successfully removed role from user";
    public static final String LOG_BULK_ROLE_ASSIGNMENT_COMPLETED_DURING_RECONCILIATION = "Bulk role assignment completed during reconciliation";
    public static final String LOG_FAILED_TO_PERFORM_BULK_ROLE_ASSIGNMENT_DURING_RECONCILIATION = "Failed to perform bulk role assignment during reconciliation";
    public static final String LOG_ROLE_ASSIGNMENT_RECONCILIATION_COMPLETED = "Role assignment reconciliation completed";
    public static final String LOG_ROLE_ASSIGNMENT_RECONCILIATION_FAILED = "Role assignment reconciliation failed";
    
    // Role Reconciliation Audit Event Publishing
    public static final String LOG_PUBLISHING_ROLE_RECONCILIATION_AUDIT_EVENT = "Publishing role reconciliation audit event";
    public static final String LOG_CANNOT_CREATE_RECONCILIATION_AUDIT_ROLE_NOT_FOUND = "Cannot create reconciliation audit - role not found";
    public static final String LOG_FAILED_TO_PUBLISH_ROLE_RECONCILIATION_AUDIT_EVENT = "Failed to publish role reconciliation audit event";
    public static final String LOG_SKIPPING_AUDIT_EVENT_NO_RECONCILIATION_CHANGES = "Skipping audit event - no reconciliation changes occurred";
    
    // Error Messages for Dynamic Rule Operations
    public static final String ERROR_DYNAMIC_RULE_NOT_FOUND_WITH_ID = "Dynamic rule not found: ";
    public static final String ERROR_ROLE_NOT_FOUND_WITH_ID = "Role not found: ";
    public static final String ERROR_ROLE_DOES_NOT_BELONG_TO_TENANT = "Role does not belong to tenant: ";
    public static final String ERROR_ROLE_NOT_FOUND_OR_DOES_NOT_BELONG_TO_TENANT_UPDATE = "Role not found or does not belong to tenant";
    public static final String ERROR_DYNAMIC_RULE_NOT_FOUND_FOR_ROLE = "Dynamic rule not found for this role";
    public static final String ERROR_FAILED_TO_EXECUTE_SCHEDULED_DYNAMIC_RULE_RECONCILIATION = "Failed to execute scheduled dynamic rule reconciliation";
    public static final String ERROR_FAILED_TO_EXECUTE_SCHEDULED_DYNAMIC_RULE = "Failed to execute scheduled dynamic rule";
    
    // Dynamic Rule Retrieval Log Messages
    public static final String LOG_RETRIEVING_DYNAMIC_RULE_BY_ROLE_ID = "Retrieving dynamic rule by role ID";
    public static final String LOG_ROLE_NOT_FOUND_FOR_DYNAMIC_RULE_RETRIEVAL = "Role not found for dynamic rule retrieval";
    public static final String LOG_ROLE_DOES_NOT_BELONG_TO_TENANT = "Role does not belong to tenant";
    public static final String LOG_NO_DYNAMIC_RULE_FOUND_FOR_ROLE = "No dynamic rule found for role";
    public static final String LOG_DYNAMIC_RULE_RETRIEVED_SUCCESSFULLY = "Dynamic rule retrieved successfully";
    public static final String LOG_FAILED_TO_RETRIEVE_DYNAMIC_RULE = "Failed to retrieve dynamic rule";
    public static final String LOG_GET_DYNAMIC_RULE_ENDPOINT_CALLED = "Get dynamic rule endpoint called";
    public static final String LOG_NO_DYNAMIC_RULE_FOR_ROLE = "No dynamic rule found for this role";
    public static final String LOG_DYNAMIC_RULE_RETRIEVED_SUCCESSFULLY_CONTROLLER = "Dynamic rule retrieved successfully in controller";
    public static final String LOG_INVALID_REQUEST_FOR_DYNAMIC_RULE_RETRIEVAL = "Invalid request for dynamic rule retrieval";
    public static final String LOG_FAILED_TO_RETRIEVE_DYNAMIC_RULE_CONTROLLER = "Failed to retrieve dynamic rule in controller";
    
    // Dynamic Rule Retrieval Response Messages
    public static final String RESPONSE_NO_DYNAMIC_RULE_FOR_ROLE = "No dynamic rule found for this role";
    public static final String RESPONSE_DYNAMIC_RULE_RETRIEVED_SUCCESSFULLY = "Dynamic rule retrieved successfully";
    public static final String RESPONSE_FAILED_TO_RETRIEVE_DYNAMIC_RULE = "Failed to retrieve dynamic rule";
    public static final String ERROR_FAILED_TO_RECONCILE_ROLE_ASSIGNMENTS = "Failed to reconcile role assignments";
    
    // Role Reconciliation Action Types
    public static final String ACTION_ROLE_RECONCILIATION = "role reconciliation";
    
    // System User Constants
    public static final String SYSTEM_USER_ID = "00000000-0000-0000-0000-000000000000";
    public static final String SYSTEM_USER_NAME = "system";
    public static final String UNKNOWN_IP_ADDRESS = "unknown";
    public static final String UNKNOWN_USER_NAME = "unknown";
    
    // ==================== GRANULAR PERMISSION SERVICE IMPLEMENTATION CONSTANTS ====================
    
    // Granular Permission Service Debug Log Messages
    public static final String LOG_RETRIEVED_ROLE_PERMISSIONS = "Retrieved role permissions";
    public static final String LOG_INVALID_ROLE_FILTER_PROVIDED = "Invalid role filter provided";
    public static final String LOG_FILTERING_MODULES_BY_NAME = "Filtering modules by name";
    public static final String LOG_MODULE_FILTER_CHECK = "Module filter check";
    public static final String LOG_FILTERING_RESULT = "Filtering result";
    public static final String LOG_PROCESSING_MODULE = "Processing module";
    public static final String LOG_PROCESSING_PAGE = "Processing page";
    
    // Granular Permission Service Detail Keys
    public static final String DETAIL_KEY_ROLE_PERMISSIONS_COUNT = "rolePermissionsCount";
    public static final String DETAIL_KEY_ROLE_ACTION_IDS = "roleActionIds";
    public static final String DETAIL_KEY_HAS_MANUAL_ASSIGNMENTS = "hasManualAssignments";
    public static final String DETAIL_KEY_HAS_DYNAMIC_RULES = "hasDynamicRules";
    public static final String DETAIL_KEY_AVAILABLE_MODULES = "availableModules";
    public static final String DETAIL_KEY_TRIMMED_FILTER = "filter";
    public static final String DETAIL_KEY_EXACT_MATCH = "exactMatch";
    public static final String DETAIL_KEY_PARTIAL_MATCH = "partialMatch";
    public static final String DETAIL_KEY_MATCHES = "matches";
    public static final String DETAIL_KEY_ORIGINAL_MODULE_COUNT = "originalModuleCount";
    public static final String DETAIL_KEY_FILTERED_MODULE_COUNT = "filteredModuleCount";
    public static final String DETAIL_KEY_PAGES_FOUND = "pagesFound";
    public static final String DETAIL_KEY_ACTIONS_FOUND = "actionsFound";
    
    // Dynamic Rule Scheduler Messages
    public static final String LOG_SCHEDULER_STARTING_EXECUTION_CHECK = "Starting scheduled dynamic rule execution check";
    public static final String LOG_SCHEDULER_NO_SCHEDULED_RULES = "No scheduled rules found, skipping execution";
    public static final String LOG_SCHEDULER_NO_RULES_DUE = "No rules due for execution at this time";
    public static final String LOG_SCHEDULER_RULES_DUE_FOR_EXECUTION = "Found {} rules due for execution";
    public static final String LOG_SCHEDULER_EXECUTING_RULES_FOR_TENANT = "Executing {} rules for tenant: {}";
    public static final String LOG_SCHEDULER_EXECUTION_COMPLETED = "All tenant rule executions completed successfully";
    public static final String LOG_SCHEDULER_EXECUTION_TIMEOUT = "Timeout waiting for rule executions to complete";
    public static final String LOG_SCHEDULER_PARALLEL_EXECUTION_ERROR = "Error during parallel rule execution";
    public static final String LOG_SCHEDULER_INDIVIDUAL_RULE_EXECUTION = "Executing scheduled rule: {} ({})";
    public static final String LOG_SCHEDULER_RULE_EXECUTION_SUCCESS = "Successfully executed rule {} - Users: {}, Assigned: {}, Duration: {}ms";
    public static final String LOG_SCHEDULER_RULE_EXECUTION_FAILED = "Failed to execute scheduled rule {}: {}";
    public static final String LOG_SCHEDULER_CIRCUIT_BREAKER_OPEN = "Circuit breaker is open, skipping scheduled execution";
    public static final String LOG_SCHEDULER_CIRCUIT_BREAKER_RESET = "Circuit breaker reset after successful execution";
    public static final String LOG_SCHEDULER_CIRCUIT_BREAKER_OPENED = "Circuit breaker opened after {} consecutive failures";
    public static final String LOG_SCHEDULER_MANUAL_TRIGGER = "Manual trigger for scheduled execution initiated";
    public static final String LOG_SCHEDULER_SHUTDOWN = "Shutting down scheduled execution service";
    public static final String LOG_SCHEDULER_CLEANUP_COMPLETED = "Cleanup completed: Deleted {} execution history records older than {} days";
    public static final String LOG_SCHEDULER_CLEANUP_FAILED = "Failed to cleanup execution history: {}";
    public static final String LOG_SCHEDULER_PERFORMANCE_REPORT = "Dynamic Rule Scheduler Performance Report (24h):";
    public static final String LOG_SCHEDULER_PERFORMANCE_REPORT_FAILED = "Failed to generate performance report: {}";
    public static final String LOG_SCHEDULER_HEALTH_CHECK_FAILED = "Health check failed: {}";

    // Additional Scheduler Detail Keys
    public static final String DETAIL_KEY_DYNAMIC_RULE_ID = "dynamicRuleId";
    public static final String DETAIL_KEY_SCHEDULE_TYPE = "scheduleType";
    public static final String DETAIL_KEY_EXECUTION_DURATION = "executionDuration";
    public static final String DETAIL_KEY_RULES_COUNT = "rulesCount";
    public static final String DETAIL_KEY_TENANT_COUNT = "tenantCount";
    public static final String DETAIL_KEY_TOTAL_USERS_EVALUATED = "totalUsersEvaluated";
    public static final String DETAIL_KEY_USERS_MATCHING_CONDITION = "usersMatchingCondition";
    public static final String DETAIL_KEY_SKIPPED_ASSIGNMENTS = "skippedAssignments";
    public static final String DETAIL_KEY_EXECUTION_STATUS = "executionStatus";
    public static final String DETAIL_KEY_CONSECUTIVE_FAILURES = "consecutiveFailures";
    public static final String DETAIL_KEY_TIMEOUT_MINUTES = "timeoutMinutes";
    public static final String DETAIL_KEY_THREAD_POOL_SIZE = "threadPoolSize";
    public static final String DETAIL_KEY_RULE_IS_ACTIVE = "ruleIsActive";
    public static final String DETAIL_KEY_CURRENT_TIME = "currentTime";
    public static final String DETAIL_KEY_FORMATTED_TIME = "formattedTime";
    public static final String DETAIL_KEY_CIRCUIT_BREAKER_STATE = "circuitBreakerState";
    public static final String DETAIL_KEY_ERROR_CLASS = "errorClass";
    public static final String DETAIL_KEY_EXECUTION_TIMEOUT_MINUTES = "executionTimeoutMinutes";
    public static final String DETAIL_KEY_USERS_EVALUATED = "usersEvaluated";

    // Scheduler Message Constants
    public static final String SCHEDULER_MESSAGE_CHECKING_RULES = "Scheduler Check: Looking for rules due at current time";
    public static final String SCHEDULER_MESSAGE_CIRCUIT_BREAKER_OPEN = "Circuit breaker is open, skipping scheduled execution";
    public static final String SCHEDULER_MESSAGE_STARTING_EXECUTION_CHECK = "Starting scheduled dynamic rule execution check";
    public static final String SCHEDULER_MESSAGE_NO_ACTIVE_RULES = "No active scheduled rules found, skipping execution";
    public static final String SCHEDULER_MESSAGE_NO_RULES_DUE = "No rules due for execution at this time";
    public static final String SCHEDULER_MESSAGE_FOUND_RULES_DUE = "Found rules due for execution";
    public static final String SCHEDULER_MESSAGE_ROLE_NOT_FOUND = "Role not found for rule, using default tenant";
    public static final String SCHEDULER_MESSAGE_ALL_EXECUTIONS_COMPLETED = "All tenant rule executions completed successfully";
    public static final String SCHEDULER_MESSAGE_EXECUTION_TIMEOUT = "Timeout waiting for rule executions to complete";
    public static final String SCHEDULER_MESSAGE_PARALLEL_EXECUTION_ERROR = "Error during parallel rule execution";
    public static final String SCHEDULER_MESSAGE_EXECUTING_RULES_FOR_TENANT = "Executing rules for tenant";
    public static final String SCHEDULER_MESSAGE_FAILED_RULE_EXECUTION = "Failed to execute rule for tenant";
    public static final String SCHEDULER_MESSAGE_EXECUTING_SCHEDULED_RULE = "Executing scheduled rule";
    public static final String SCHEDULER_MESSAGE_SKIPPING_INACTIVE_RULE = "Skipping execution of rule - rule is not active";
    public static final String SCHEDULER_MESSAGE_EXECUTING_RULE_FOR_TENANT = "Executing rule for tenant";
    public static final String SCHEDULER_MESSAGE_SUCCESSFULLY_EXECUTED_RULE = "Successfully executed rule";
    public static final String SCHEDULER_MESSAGE_FAILED_SCHEDULED_RULE = "Failed to execute scheduled rule";
    public static final String SCHEDULER_MESSAGE_FAILED_EXECUTION_HISTORY = "Failed to record execution history for rule";
    public static final String SCHEDULER_MESSAGE_EXECUTION_FAILED = "Scheduled execution failed";
    public static final String SCHEDULER_MESSAGE_CIRCUIT_BREAKER_OPENED = "Circuit breaker opened after consecutive failures";
    public static final String SCHEDULER_MESSAGE_CIRCUIT_BREAKER_RESET = "Circuit breaker reset after successful execution";
    public static final String SCHEDULER_MESSAGE_MANUAL_TRIGGER = "Manual trigger for scheduled execution initiated";
    public static final String SCHEDULER_MESSAGE_SHUTTING_DOWN = "Shutting down scheduled execution service";
    public static final String SCHEDULER_MESSAGE_FOUND_ACTIVE_RULES = "Found active rules for evaluation";

    // Scheduler Detail Keys (new ones only - others already exist)
    public static final String DETAIL_KEY_SCHEDULED_RULES_COUNT = "scheduledRulesCount";
    public static final String DETAIL_KEY_IMMEDIATE_RULES_COUNT = "immediateRulesCount";
    public static final String DETAIL_KEY_TOTAL_RULES_COUNT = "totalRulesCount";

    // Scheduler Log Object Types
    public static final String LOG_OBJECT_TYPE_SCHEDULER_EXECUTION = "SchedulerExecution";
    public static final String LOG_OBJECT_TYPE_TENANT_EXECUTION = "TenantExecution";
    public static final String LOG_OBJECT_TYPE_RULE_EXECUTION = "RuleExecution";
    public static final String LOG_OBJECT_TYPE_EXECUTION_HISTORY = "ExecutionHistory";
    public static final String LOG_OBJECT_TYPE_CIRCUIT_BREAKER = "CircuitBreaker";
    public static final String LOG_OBJECT_TYPE_PERFORMANCE_REPORT = "PerformanceReport";
    public static final String LOG_OBJECT_TYPE_RULE_EVALUATION = "RuleEvaluation";
    public static final String LOG_OBJECT_TYPE_TENANT_CONTEXT = "TenantContext";
    public static final String LOG_OBJECT_TYPE_EXECUTION_ERROR = "ExecutionError";
    public static final String LOG_OBJECT_TYPE_RULE_VALIDATION = "RuleValidation";
    public static final String LOG_OBJECT_TYPE_MANUAL_TRIGGER = "ManualTrigger";
    public static final String LOG_OBJECT_TYPE_SERVICE_LIFECYCLE = "ServiceLifecycle";

    // ===================================================================================
    // USER ROLE SERVICE IMPLEMENTATION CONSTANTS
    // ===================================================================================
    
    // Logger Messages - Processing
    public static final String LOG_ERROR_PROCESSING_USER_ROLE_ASSIGNMENT = "Error processing user role assignment";
    public static final String LOG_ERROR_PROCESSING_BULK_USER_ROLE_ASSIGNMENT = "Error processing bulk user role assignment";
    
    // Logger Messages - Role Reconciliation
    public static final String LOG_GETTING_USERS_WITH_ROLE_FOR_RECONCILIATION = "Getting users with role for reconciliation";
    public static final String LOG_RETRIEVED_USERS_WITH_ROLE_FOR_RECONCILIATION = "Retrieved users with role for reconciliation";
    public static final String LOG_FAILED_TO_GET_USERS_WITH_ROLE_FOR_RECONCILIATION = "Failed to get users with role for reconciliation";
    public static final String LOG_UNASSIGNING_ROLE_FOR_RECONCILIATION = "Unassigning role for reconciliation";
    public static final String LOG_ROLE_UNASSIGNMENT_COMPLETED_FOR_RECONCILIATION = "Role unassignment completed for reconciliation";
    public static final String LOG_FAILED_TO_UNASSIGN_ROLE_FOR_RECONCILIATION = "Failed to unassign role for reconciliation";
    
    // Logger Messages - Bulk Role Revocation
    public static final String LOG_PROCESSING_BULK_ROLE_REVOCATION_REQUEST = "Processing bulk role revocation request";
    public static final String LOG_SUCCESSFULLY_REVOKED_ROLE_FROM_USER = "Successfully revoked role from user";
    public static final String LOG_FAILED_TO_REVOKE_ROLE_NO_ASSIGNMENT_FOUND = "Failed to revoke role - no assignment found";
    public static final String LOG_USER_DIDNT_HAVE_ROLE_TREATING_AS_SUCCESSFUL_REVOCATION = "User didn't have role - treating as successful revocation";
    public static final String LOG_FAILED_TO_REVOKE_ROLE_FROM_USER = "Failed to revoke role from user";
    public static final String LOG_BULK_ROLE_REVOCATION_COMPLETED = "Bulk role revocation completed";
    public static final String LOG_FAILED_TO_PROCESS_BULK_ROLE_REVOCATION_REQUEST = "Failed to process bulk role revocation request";
    
    // Logger Messages - Dynamic Role Assignment Type
    public static final String LOG_UNASSIGNING_DYNAMIC_ROLE_FOR_RECONCILIATION = "Unassigning DYNAMIC role assignment during reconciliation";
    public static final String LOG_SUCCESSFULLY_REMOVED_DYNAMIC_ROLE_ASSIGNMENT = "Successfully removed DYNAMIC role assignment";
    public static final String LOG_NO_DYNAMIC_ASSIGNMENT_FOUND_USER_MAY_HAVE_MANUAL = "No DYNAMIC assignment found to remove (user may have MANUAL assignment)";
    public static final String LOG_FAILED_TO_UNASSIGN_DYNAMIC_ROLE_FOR_RECONCILIATION = "Failed to unassign DYNAMIC role during reconciliation";
    public static final String LOG_SKIPPED_ROLE_REMOVAL_USER_HAS_MANUAL_ASSIGNMENT = "Skipped role removal - user has MANUAL assignment";
    
    // Detail Keys - Assignment Type
    public static final String DETAIL_KEY_ASSIGNMENT_TYPE = "assignmentType";
    
    // Error Codes
    public static final String ERROR_CODE_ROLE_ALREADY_ASSIGNED = "ROLE_ALREADY_ASSIGNED";
    public static final String ERROR_CODE_USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String ERROR_CODE_USER_NOT_IN_TENANT = "USER_NOT_IN_TENANT";
    public static final String ERROR_CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String ERROR_CODE_INTERNAL_ERROR = "INTERNAL_ERROR";
    
    // Object Types - Role Reconciliation
    public static final String LOG_OBJECT_TYPE_ROLE_RECONCILIATION_USERS_QUERY = "ROLE_RECONCILIATION_USERS_QUERY";
    public static final String LOG_OBJECT_TYPE_ROLE_RECONCILIATION_USERS_RESULT = "ROLE_RECONCILIATION_USERS_RESULT";
    public static final String LOG_OBJECT_TYPE_ROLE_RECONCILIATION_USERS_ERROR = "ROLE_RECONCILIATION_USERS_ERROR";
    public static final String LOG_OBJECT_TYPE_ROLE_RECONCILIATION_UNASSIGN = "ROLE_RECONCILIATION_UNASSIGN";
    public static final String LOG_OBJECT_TYPE_ROLE_RECONCILIATION_UNASSIGN_RESULT = "ROLE_RECONCILIATION_UNASSIGN_RESULT";
    public static final String LOG_OBJECT_TYPE_ROLE_RECONCILIATION_UNASSIGN_ERROR = "ROLE_RECONCILIATION_UNASSIGN_ERROR";
    
    // Object Types - Bulk Role Revocation
    public static final String LOG_OBJECT_TYPE_BULK_ROLE_REVOCATION_REQUEST = "BULK_ROLE_REVOCATION_REQUEST";
    public static final String LOG_OBJECT_TYPE_BULK_ROLE_REVOCATION_COMPLETED = "BULK_ROLE_REVOCATION_COMPLETED";
    public static final String LOG_OBJECT_TYPE_BULK_ROLE_REVOCATION_ERROR = "BULK_ROLE_REVOCATION_ERROR";
    
    // Detail Keys - Bulk Operations
    public static final String DETAIL_KEY_IS_BULK_REVOCATION = "isBulkRevocation";
    public static final String DETAIL_KEY_TOTAL_REQUESTED = "totalRequested";
    public static final String DETAIL_KEY_TOTAL_SUCCESSFUL = "totalSuccessful";
    public static final String DETAIL_KEY_TOTAL_FAILED = "totalFailed";
    public static final String DETAIL_KEY_ROLE_NAME = "roleName";
    public static final String DETAIL_KEY_USER_COUNT_DETAIL = "user_count";
    
    // Error Detection Strings
    public static final String ERROR_DETECTION_USER_NOT_FOUND = "User not found";
    public static final String ERROR_DETECTION_DOES_NOT_BELONG_TO_TENANT = "does not belong to tenant";

    // ===================================================================================
    // DTO VALIDATION MESSAGES CONSTANTS
    // ===================================================================================
    
    // Common Validation Messages
    public static final String VALIDATION_ROLE_ID_REQUIRED = "Role ID is required";
    public static final String VALIDATION_USER_ID_REQUIRED = "User ID is required";
    public static final String VALIDATION_USER_IDS_LIST_REQUIRED = "User IDs list is required";
    public static final String VALIDATION_USER_IDS_LIST_CANNOT_BE_EMPTY = "User IDs list cannot be empty";
    public static final String VALIDATION_USER_IDS_CANNOT_BE_NULL = "User IDs cannot be null";
    public static final String VALIDATION_ROLE_ID_CANNOT_BE_NULL = "Role ID cannot be null";
    public static final String VALIDATION_MAXIMUM_100_USERS_CAN_BE_ASSIGNED_AT_ONCE = "Maximum 100 users can be assigned at once";
    public static final String VALIDATION_ACTION_ID_REQUIRED = "Action ID is required";
    public static final String VALIDATION_JUSTIFICATION_REQUIRED = "Justification is required";
    
    // Dynamic Rule Validation Messages
    public static final String VALIDATION_RULE_ACTIVE_STATUS_REQUIRED = "Rule active status is required";
    public static final String VALIDATION_RULE_CONDITION_SCRIPT_REQUIRED = "Rule condition script is required";
    public static final String VALIDATION_RULE_CONDITION_SCRIPT_LENGTH = "Rule condition script must be between 1 and 1000 characters";
    public static final String VALIDATION_SEND_NOTIFICATION_FLAG_REQUIRED = "Send notification flag is required";
    public static final String VALIDATION_REQUIRE_APPROVAL_FLAG_REQUIRED = "Require approval flag is required";
    public static final String VALIDATION_SCHEDULE_TYPE_REQUIRED = "Schedule type is required";
    public static final String VALIDATION_TIME_MUST_BE_IN_HH_MM_FORMAT = "Time must be in HH:mm format";
    
    // Global Dynamic Rule Validation Messages (Aliases for consistency)
    public static final String VALIDATION_CONDITION_SCRIPT_REQUIRED = "Condition script is required";
    public static final String VALIDATION_CONDITION_SCRIPT_NOT_BLANK = "Condition script cannot be blank";
    public static final String VALIDATION_RULE_IS_ACTIVE_REQUIRED = "Rule active status is required";
    public static final String VALIDATION_SEND_NOTIFICATION_REQUIRED = "Send notification flag is required";
    public static final String VALIDATION_REQUIRE_APPROVAL_REQUIRED = "Require approval flag is required";
    
    // Action/Page/Module Validation Messages
    public static final String VALIDATION_ACTION_NAME_REQUIRED = "Action name is required";
    public static final String VALIDATION_ACTION_NAME_LENGTH = "Action name must be between 1 and 100 characters";
    public static final String VALIDATION_ENDPOINT_MAX_LENGTH = "Endpoint must not exceed 255 characters";
    public static final String VALIDATION_PAGE_NAME_REQUIRED = "Page name is required";
    public static final String VALIDATION_PAGE_NAME_LENGTH = "Page name must be between 1 and 100 characters";
    public static final String VALIDATION_PAGE_PATH_LENGTH = "Page path must not exceed 255 characters";
    public static final String VALIDATION_MODULE_NAME_REQUIRED = "Module name is required";
    public static final String VALIDATION_MODULE_NAME_LENGTH = "Module name must be between 1 and 100 characters";
    
    // Business Logic Error Messages
    public static final String ERROR_EITHER_USER_ID_OR_USER_IDS_MUST_BE_PROVIDED = "Either userId (for single assignment) or userIds (for bulk assignment) must be provided, but not both";
    public static final String ERROR_DUPLICATE_USER_IDS_NOT_ALLOWED = "Duplicate user IDs are not allowed";
    public static final String ERROR_TIME_REQUIRED_FOR_DAILY_SCHEDULE = "Time is required for DAILY schedule type";
    public static final String ERROR_TIME_NOT_ALLOWED_FOR_IMMEDIATE_SCHEDULE = "Time should not be provided for IMMEDIATE schedule type";

    // ==================== DYNAMIC RULE CONTROLLER CONSTANTS ====================
    
    // Dynamic Rule Controller Log Messages - Update Operations
    public static final String LOG_RECEIVED_REQUEST_TO_UPDATE_DYNAMIC_RULE = "Received request to update dynamic rule";
    public static final String LOG_DYNAMIC_RULE_UPDATED_SUCCESSFULLY_CONTROLLER = "Dynamic rule updated successfully";
    public static final String LOG_INVALID_REQUEST_FOR_DYNAMIC_RULE_UPDATE = "Invalid request for dynamic rule update";
    public static final String LOG_FAILED_TO_UPDATE_DYNAMIC_RULE_CONTROLLER = "Failed to update dynamic rule";
    
    // Dynamic Rule Controller Response Messages
    public static final String RESPONSE_DYNAMIC_RULE_UPDATED_SUCCESSFULLY = "Dynamic rule updated successfully";
    public static final String RESPONSE_FAILED_TO_UPDATE_DYNAMIC_RULE = "Failed to update dynamic rule";

    // ==================== DYNAMIC RULE SCHEDULER CONTROLLER CONSTANTS ====================
    
    // Scheduler Response Keys
    public static final String SCHEDULER_RESPONSE_KEY_STATUS = "status";
    public static final String SCHEDULER_RESPONSE_KEY_MESSAGE = "message";
    public static final String SCHEDULER_RESPONSE_KEY_TIMESTAMP = "timestamp";
    public static final String SCHEDULER_RESPONSE_KEY_CIRCUIT_BREAKER_OPEN = "circuitBreakerOpen";
    public static final String SCHEDULER_RESPONSE_KEY_CONSECUTIVE_FAILURES = "consecutiveFailures";
    public static final String SCHEDULER_RESPONSE_KEY_HISTORY = "history";
    public static final String SCHEDULER_RESPONSE_KEY_TOTAL_RECORDS = "totalRecords";
    public static final String SCHEDULER_RESPONSE_KEY_START_DATE = "startDate";
    public static final String SCHEDULER_RESPONSE_KEY_END_DATE = "endDate";
    public static final String SCHEDULER_RESPONSE_KEY_TENANT_ID = "tenantId";
    public static final String SCHEDULER_RESPONSE_KEY_LIMITED = "limited";
    public static final String SCHEDULER_RESPONSE_KEY_TIME_WINDOW = "timeWindow";
    public static final String SCHEDULER_RESPONSE_KEY_TOTAL_EXECUTIONS = "totalExecutions";
    public static final String SCHEDULER_RESPONSE_KEY_SUCCESSFUL_EXECUTIONS = "successfulExecutions";
    public static final String SCHEDULER_RESPONSE_KEY_FAILED_EXECUTIONS = "failedExecutions";
    public static final String SCHEDULER_RESPONSE_KEY_SUCCESS_RATE = "successRate";
    public static final String SCHEDULER_RESPONSE_KEY_AVG_EXECUTION_TIME_MS = "averageExecutionTimeMs";
    public static final String SCHEDULER_RESPONSE_KEY_TOTAL_USERS_PROCESSED = "totalUsersProcessed";
    public static final String SCHEDULER_RESPONSE_KEY_TOTAL_ROLE_ASSIGNMENTS = "totalRoleAssignments";
    public static final String SCHEDULER_RESPONSE_KEY_RECENT_FAILURES = "recentFailures";
    public static final String SCHEDULER_RESPONSE_KEY_LONG_RUNNING_EXECUTIONS = "longRunningExecutions";
    
    // Scheduler Status Values
    public static final String SCHEDULER_STATUS_SUCCESS = "SUCCESS";
    public static final String SCHEDULER_STATUS_ERROR = "ERROR";
    
    // Scheduler Response Messages
    public static final String SCHEDULER_MSG_EXECUTION_TRIGGERED_SUCCESSFULLY = "Scheduled execution triggered successfully";
    public static final String SCHEDULER_MSG_CANNOT_TRIGGER_CIRCUIT_BREAKER_OPEN = "Cannot trigger execution - circuit breaker is open";
    public static final String SCHEDULER_MSG_FAILED_TO_TRIGGER_EXECUTION = "Failed to trigger execution: %s";
    public static final String SCHEDULER_MSG_START_DATE_BEFORE_END_DATE = "Start date must be before end date";
    
    // Scheduler Execution Status Values
    public static final String SCHEDULER_EXECUTION_STATUS_SUCCESS = "SUCCESS";
    public static final String SCHEDULER_EXECUTION_STATUS_FAILED = "FAILED";
    
    // Scheduler Time Constants
    public static final String SCHEDULER_TIME_WINDOW_HOURS_SUFFIX = " hours";
    public static final int SCHEDULER_DEFAULT_HISTORY_DAYS = 7;
    public static final int SCHEDULER_DEFAULT_STATISTICS_HOURS = 24;
    public static final int SCHEDULER_DEFAULT_HISTORY_LIMIT = 100;
    public static final long SCHEDULER_LONG_RUNNING_THRESHOLD_MS = 30000L;
    public static final int SCHEDULER_METRICS_LOOKBACK_HOURS = 1;

    // ==================== ROLE CONTROLLER CONDITIONAL SCRIPT CONSTANTS ====================
    
    // Conditional Script Validation Log Messages
    public static final String LOG_VALIDATING_CONDITIONAL_SCRIPT_SYNTAX = "Validating conditional script syntax";
    public static final String LOG_INVALID_CONDITIONAL_SCRIPT_PROVIDED = "Invalid conditional script provided";
    public static final String LOG_CONDITIONAL_SCRIPT_VALIDATION_SUCCESSFUL = "Conditional script validation successful";
    
    // Conditional Script Error Response Keys (reusing existing RESPONSE_KEY_ERROR and RESPONSE_KEY_MESSAGE)
    public static final String RESPONSE_KEY_CONDITION_SCRIPT = "conditionScript";
    
    // Conditional Script Error Messages
    public static final String ERROR_INVALID_CONDITIONAL_SCRIPT = "Invalid conditional script";
    public static final String ERROR_INVALID_CONDITIONAL_SCRIPT_SYNTAX = "The provided conditional script has invalid syntax";
    
    // Permission Response Status Values
    public static final String PERMISSION_STATUS_CREATED = "CREATED";
    public static final String PERMISSION_STATUS_OK = "OK";
    public static final String PERMISSION_STATUS_CONFLICT = "CONFLICT";
    public static final String PERMISSION_STATUS_NOT_FOUND = "NOT_FOUND";
    public static final String PERMISSION_STATUS_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    
    // Permission Response Keys
    public static final String PERMISSION_RESPONSE_KEY_MESSAGE = "message";
    public static final String PERMISSION_RESPONSE_KEY_STATUS = "status";
    public static final String PERMISSION_RESPONSE_KEY_DATA = "data";
    public static final String PERMISSION_RESPONSE_KEY_ROLE_ID = "roleId";
    public static final String PERMISSION_RESPONSE_KEY_ACTION_ID = "actionId";
    public static final String PERMISSION_RESPONSE_KEY_ERROR = "error";
    
    // Role Controller Response Keys
    public static final String ROLE_RESPONSE_KEY_MESSAGE = "message";
    public static final String ROLE_RESPONSE_KEY_DATA = "data";
    public static final String ROLE_RESPONSE_KEY_STATUS = "status";
    public static final String ROLE_RESPONSE_KEY_COUNT = "count";
    public static final String ROLE_RESPONSE_KEY_ERROR = "error";
    
    // Role Controller Detail Keys
    public static final String ROLE_DETAIL_KEY_METHOD = "method";
    public static final String ROLE_DETAIL_KEY_ROLE_ID = "roleId";
    public static final String ROLE_DETAIL_KEY_ACTION_ID = "actionId";
    public static final String ROLE_DETAIL_KEY_ROLE_NAME = "roleName";
    public static final String ROLE_DETAIL_KEY_ROLE_TYPE = "roleType";
    public static final String ROLE_DETAIL_KEY_DESCRIPTION = "description";
    public static final String ROLE_DETAIL_KEY_TENANT_ID = "tenantId";
    public static final String ROLE_DETAIL_KEY_ENDPOINT = "endpoint";
    public static final String ROLE_DETAIL_KEY_ROLE_COUNT = "roleCount";
    public static final String ROLE_DETAIL_KEY_RESPONSE_STATUS = "responseStatus";
    public static final String ROLE_DETAIL_KEY_RESPONSE_MESSAGE = "responseMessage";
    public static final String ROLE_DETAIL_KEY_SUCCESS = "success";
    public static final String ROLE_DETAIL_KEY_EXCEPTION_TYPE = "exceptionType";
    public static final String ROLE_DETAIL_KEY_EXCEPTION_MESSAGE = "exceptionMessage";
    public static final String ROLE_DETAIL_KEY_ERROR_TYPE = "errorType";
    public static final String ROLE_DETAIL_KEY_ERROR_MESSAGE = "errorMessage";
    public static final String ROLE_DETAIL_KEY_HTTP_STATUS = "httpStatus";
    public static final String ROLE_DETAIL_KEY_OPERATION = "operation";
    public static final String ROLE_DETAIL_KEY_CONDITION_SCRIPT = "conditionScript";
    public static final String ROLE_DETAIL_KEY_HAS_CONDITIONAL_RULE = "hasConditionalRule";
    public static final String ROLE_DETAIL_KEY_PERMISSION_ID = "permissionId";
    public static final String ROLE_DETAIL_KEY_ERROR = "error";
    
    // Role Controller Error Types
    public static final String ERROR_TYPE_NOT_FOUND = "NOT_FOUND";
    public static final String ERROR_TYPE_FORBIDDEN = "FORBIDDEN";
    public static final String ERROR_TYPE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String ERROR_TYPE_CONFLICT = "CONFLICT";
    
    // Role Controller Method Names
    public static final String METHOD_CREATE_ROLE = "createRole";
    public static final String METHOD_UPDATE_ROLE = "updateRole";
    public static final String METHOD_GET_ROLE_BY_ID = "getRoleById";
    public static final String METHOD_GET_ALL_ROLES = "getAllRoles";
    public static final String METHOD_GET_USERS_BY_ROLE = "getUsersByRole";
    public static final String METHOD_DELETE_ROLE = "deleteRole";
    public static final String METHOD_ASSIGN_PERMISSION_TO_ROLE = "assignPermissionToRole";
    public static final String METHOD_DELETE_PERMISSION_FROM_ROLE = "deletePermissionFromRole";
    public static final String METHOD_UPDATE_PERMISSION_FOR_ROLE = "updatePermissionForRole";
    
    // Role Controller Operation Types
    public static final String OPERATION_DELETE = "DELETE";
    public static final String OPERATION_POST = "POST";
    public static final String OPERATION_PUT = "PUT";
    public static final String OPERATION_GET = "GET";

    // ===========================================================================================
    // User Role Controller Constants
    // ===========================================================================================
    
    // Assignment Type Values
    public static final String ASSIGNMENT_TYPE_BULK = "bulk";
    public static final String ASSIGNMENT_TYPE_SINGLE = "single";
    
    // Response Keys for User Role Operations
    public static final String USER_ROLE_RESPONSE_KEY_MESSAGE = "message";
    public static final String USER_ROLE_RESPONSE_KEY_STATUS = "status";
    public static final String USER_ROLE_RESPONSE_KEY_DATA = "data";
    public static final String USER_ROLE_RESPONSE_KEY_COUNT = "count";
    public static final String USER_ROLE_RESPONSE_KEY_REMOVED = "removed";
    public static final String USER_ROLE_RESPONSE_KEY_HAS_ROLE = "hasRole";
    public static final String USER_ROLE_RESPONSE_KEY_TIMESTAMP = "timestamp";
    public static final String USER_ROLE_RESPONSE_KEY_ERROR = "error";
    public static final String USER_ROLE_RESPONSE_KEY_EXCEPTION_TYPE = "exceptionType";
    
    // Detail Keys for User Role Logging
    public static final String USER_ROLE_DETAIL_KEY_ASSIGNMENT_TYPE = "assignmentType";
    public static final String USER_ROLE_DETAIL_KEY_SUCCESSFUL_ASSIGNMENT_COUNT = "successfulAssignmentCount";
    public static final String USER_ROLE_DETAIL_KEY_ERROR = "error";
    public static final String USER_ROLE_DETAIL_KEY_EXCEPTION_TYPE = "exceptionType";
    public static final String USER_ROLE_DETAIL_KEY_STACK_TRACE = "stackTrace";
    public static final String USER_ROLE_DETAIL_KEY_USER_COUNT = "userCount";
    public static final String USER_ROLE_DETAIL_KEY_IS_BULK_REVOCATION = "isBulkRevocation";
    public static final String USER_ROLE_DETAIL_KEY_TOTAL_REQUESTED = "totalRequested";
    public static final String USER_ROLE_DETAIL_KEY_TOTAL_SUCCESSFUL = "totalSuccessful";
    public static final String USER_ROLE_DETAIL_KEY_TOTAL_FAILED = "totalFailed";
    public static final String USER_ROLE_DETAIL_KEY_ROLE_NAME = "roleName";
    public static final String USER_ROLE_DETAIL_KEY_SUCCESSFUL_USER_COUNT = "successfulUserCount";
    public static final String USER_ROLE_DETAIL_KEY_TOTAL_USERS_IN_TENANT = "totalUsersInTenant";
    public static final String USER_ROLE_DETAIL_KEY_USERNAME = "username";
    public static final String USER_ROLE_DETAIL_KEY_EMAIL = "email";
    public static final String USER_ROLE_DETAIL_KEY_OPERATION_TYPE = "operationType";
    
    // Log Messages for User Role Controller
    public static final String USER_ROLE_LOG_NOTIFICATIONS_SENT = "Role assignment notifications sent for successful assignments";
    public static final String USER_ROLE_LOG_NOTIFICATIONS_DISABLED = "Notifications disabled for role assignment, skipping notifications";
    public static final String USER_ROLE_LOG_NO_SUCCESSFUL_ASSIGNMENTS = "No successful role assignments, skipping notifications";
    public static final String USER_ROLE_LOG_FAILED_TO_SEND_NOTIFICATIONS = "Failed to send role assignment notifications";
    public static final String USER_ROLE_LOG_REVOCATION_ENDPOINT_CALLED = "Role revocation endpoint called";
    public static final String USER_ROLE_LOG_REVOCATION_COMPLETED = "Role revocation endpoint completed successfully";
    public static final String USER_ROLE_LOG_REVOCATION_FAILED_INVALID_ARGS = "Role revocation failed - invalid arguments";
    public static final String USER_ROLE_LOG_REVOCATION_FAILED = "Role revocation failed";
    public static final String USER_ROLE_LOG_PREPARING_NOTIFICATIONS = "Preparing role assignment notifications for successful assignments only";
    public static final String USER_ROLE_LOG_ROLE_NOT_FOUND_NOTIFICATION = "Role not found for notification";
    public static final String USER_ROLE_LOG_ROLE_INFO_RETRIEVED = "Role information retrieved for notification";
    public static final String USER_ROLE_LOG_USER_DATA_RETRIEVED = "Retrieved user data for notification";
    public static final String USER_ROLE_LOG_NOTIFICATION_DATA_CREATED = "Created notification data for user";
    public static final String USER_ROLE_LOG_USER_NOT_FOUND_NOTIFICATION = "User not found for notification";
    public static final String USER_ROLE_LOG_SENDING_NOTIFICATION_EVENT = "Sending role assignment notification event";
    public static final String USER_ROLE_LOG_NOTIFICATION_SENT_SUCCESS = "Role assignment notification sent successfully";
    public static final String USER_ROLE_LOG_NO_VALID_USERS_NOTIFICATION = "No valid users found for notification";
    public static final String USER_ROLE_LOG_NOTIFICATION_SEND_FAILED = "Failed to send role assignment notifications";
    
    // Notification Messages for Role Revocation
    public static final String NOTIFICATION_DESCRIPTION_ROLE_REVOKED = "Internal Role revoked from %s";
    public static final String NOTIFICATION_DETAIL_ROLE_REVOKED = "Role %s has been revoked";
    
    // Log Messages for Role Revocation Notifications
    public static final String USER_ROLE_LOG_PREPARING_REVOKE_NOTIFICATIONS = "Preparing role revocation notifications for successfully revoked users only";
    public static final String USER_ROLE_LOG_REVOKE_NOTIFICATION_DATA_CREATED = "Created revocation notification data for user";
    public static final String USER_ROLE_LOG_SENDING_REVOKE_NOTIFICATION_EVENT = "Sending role revocation notification event";
    public static final String USER_ROLE_LOG_REVOKE_NOTIFICATION_SENT_SUCCESS = "Role revocation notification sent successfully";
    public static final String USER_ROLE_LOG_NO_VALID_USERS_REVOKE_NOTIFICATION = "No valid users found for revocation notification";
    public static final String USER_ROLE_LOG_REVOKE_NOTIFICATION_SEND_FAILED = "Failed to send role revocation notifications";
    
    // Error Messages for User Role Controller
    public static final String USER_ROLE_ERROR_REVOCATION_FAILED = "Failed to revoke role from users";
    
    // Object Types for User Role Controller Logging
    public static final String USER_ROLE_OBJECT_TYPE_REVOCATION_REQUEST = "ROLE_REVOCATION_REQUEST";
    public static final String USER_ROLE_OBJECT_TYPE_REVOCATION_SUCCESS = "ROLE_REVOCATION_SUCCESS";
    public static final String USER_ROLE_OBJECT_TYPE_REVOCATION_INVALID_ARGS = "ROLE_REVOCATION_INVALID_ARGS";
    public static final String USER_ROLE_OBJECT_TYPE_REVOCATION_ERROR = "ROLE_REVOCATION_ERROR";
    public static final String USER_ROLE_OBJECT_TYPE_NOTIFICATION = "ROLE_ASSIGNMENT_NOTIFICATION";

    // ===========================================================================================
    // Dynamic Rule Scheduler Health Service Constants
    // ===========================================================================================
    
    // Response Keys for Health Status
    public static final String HEALTH_RESPONSE_KEY_CIRCUIT_BREAKER_OPEN = "circuitBreakerOpen";
    public static final String HEALTH_RESPONSE_KEY_CONSECUTIVE_FAILURES = "consecutiveFailures";
    public static final String HEALTH_RESPONSE_KEY_TOTAL_EXECUTIONS = "totalExecutions";
    public static final String HEALTH_RESPONSE_KEY_FAILED_EXECUTIONS = "failedExecutions";
    public static final String HEALTH_RESPONSE_KEY_FAILURE_RATE_PERCENT = "failureRatePercent";
    public static final String HEALTH_RESPONSE_KEY_MONITORING_WINDOW_MINUTES = "monitoringWindowMinutes";
    public static final String HEALTH_RESPONSE_KEY_STATUS = "status";
    public static final String HEALTH_RESPONSE_KEY_REASON = "reason";
    public static final String HEALTH_RESPONSE_KEY_ERROR = "error";
    public static final String HEALTH_RESPONSE_KEY_EXECUTIONS_LAST_HOUR = "executionsLastHour";
    public static final String HEALTH_RESPONSE_KEY_TOTAL_HISTORY_RECORDS = "totalHistoryRecords";
    public static final String HEALTH_RESPONSE_KEY_TIMESTAMP = "timestamp";
    
    // Health Status Values
    public static final String HEALTH_STATUS_DOWN = "DOWN";
    public static final String HEALTH_STATUS_DEGRADED = "DEGRADED";
    public static final String HEALTH_STATUS_UP = "UP";
    public static final String HEALTH_STATUS_HEALTHY = "HEALTHY";
    public static final String HEALTH_STATUS_ERROR = "ERROR";
    
    // Execution Status Values
    public static final String EXECUTION_STATUS_FAILED = "FAILED";
    public static final String EXECUTION_STATUS_SUCCESS = "SUCCESS";
    
    // Health Status Reasons
    public static final String HEALTH_REASON_CIRCUIT_BREAKER_OPEN = "Circuit breaker is open";
    public static final String HEALTH_REASON_HIGH_FAILURE_RATE = "High failure rate: ";
    public static final String HEALTH_REASON_MULTIPLE_CONSECUTIVE_FAILURES = "Multiple consecutive failures detected";
    public static final String HEALTH_ERROR_CHECK_FAILED = "Health check failed: ";
    
    // Log Messages for Health Service
    public static final String HEALTH_LOG_CLEANUP_COMPLETED = "Cleanup completed: Deleted ";
    public static final String HEALTH_LOG_CLEANUP_RECORDS_OLDER_THAN = " execution history records older than ";
    public static final String HEALTH_LOG_CLEANUP_DAYS = " days";
    public static final String HEALTH_LOG_CLEANUP_FAILED = "Failed to cleanup execution history: ";
    public static final String HEALTH_LOG_PERFORMANCE_REPORT_TITLE = "Dynamic Rule Scheduler Performance Report (24h):";
    public static final String HEALTH_LOG_TOTAL_EXECUTIONS = "  Total Executions: ";
    public static final String HEALTH_LOG_SUCCESS_RATE = "  Success Rate: ";
    public static final String HEALTH_LOG_AVG_EXECUTION_TIME = "  Average Execution Time: ";
    public static final String HEALTH_LOG_TOTAL_USERS_PROCESSED = "  Total Users Processed: ";
    public static final String HEALTH_LOG_TOTAL_ROLE_ASSIGNMENTS = "  Total Role Assignments: ";
    public static final String HEALTH_LOG_PERFORMANCE_REPORT_FAILED = "Failed to generate performance report: ";
    
    // Formatting Constants
    public static final String HEALTH_PERCENT_SUFFIX = "%";
    public static final String HEALTH_MS_SUFFIX = "ms";

    // ===========================================================================================
    // Service Implementation Exception Messages
    // ===========================================================================================
    
    // RoleServiceImpl Exception Messages
    public static final String EXCEPTION_INVALID_OWNER_IDS = "Invalid owner IDs: ";
    public static final String EXCEPTION_USERS_MUST_BELONG_TO_TENANT = ". Users must exist and belong to tenant: ";
    public static final String EXCEPTION_FAILED_TO_VALIDATE_ROLE_OWNERS = "Failed to validate role owners: ";
    
    //GranularPermissionServiceImpl Exception Messages
    public static final String EXCEPTION_ACTION_NAME_EXISTS = "Action with this name already exists in this page";
    
    // DynamicRuleServiceImpl Exception Messages
    public static final String EXCEPTION_DYNAMIC_RULE_EXISTS = "A dynamic rule already exists for role ";
    public static final String EXCEPTION_EACH_ROLE_ONE_RULE = ". Each role can have only one dynamic rule.";
    
    // Service Implementation Log Messages
    public static final String LOG_ROLE_UPDATE_AUDIT_INFO_SET = "Role update audit information set";
    public static final String LOG_ROLE_OWNERSHIP_CREATED = "Role ownership relationships created successfully";
    public static final String LOG_FAILED_TO_VALIDATE_CREATE_OWNERS = "Failed to validate or create role owners";
    public static final String LOG_DYNAMIC_RULE_EXISTS_FOR_ROLE = "Dynamic rule already exists for this role";
    public static final String LOG_UPDATING_ROLE_OWNERS = "Updating role owners";
    public static final String LOG_DELETING_EXISTING_ROLE_OWNERS = "Deleting existing role owners";
    public static final String LOG_NEW_OWNERS_VALIDATED_CREATED = "New role owners validated and created";
    
    // Detail Keys for Service Implementation Logging (reusing existing global constants DETAIL_KEY_TENANT_ID, DETAIL_KEY_ROLE_NAME)
    public static final String DETAIL_KEY_ROLE_TYPE = "roleType";
    public static final String DETAIL_KEY_CREATED_BY = "createdBy";
    public static final String DETAIL_KEY_UPDATED_BY_NAME = "updatedByName";
    public static final String DETAIL_KEY_UPDATED_BY_ID = "updatedById";
    public static final String DETAIL_KEY_OWNER_COUNT = "ownerCount";
    public static final String DETAIL_KEY_OWNERS_CREATED = "ownersCreated";
    public static final String DETAIL_KEY_RISK_LEVEL = "riskLevel";
    public static final String DETAIL_KEY_INVALID_OWNER_IDS = "invalidOwnerIds";
    public static final String DETAIL_KEY_NEW_OWNER_IDS_COUNT = "newOwnerIdsCount";
    public static final String DETAIL_KEY_EXISTING_OWNERS_COUNT = "existingOwnersCount";

    // ========================================================================================
    // DynamicRuleServiceImpl Logger Messages and Detail Keys
    // ========================================================================================
    
    // Logger Messages
    public static final String LOG_CHECKING_EXISTING_DYNAMIC_RULE = "Checking for existing dynamic rule for role";
    public static final String LOG_SENDING_DYNAMIC_RULE_NOTIFICATIONS_IMMEDIATE = "Sending dynamic rule assignment notifications for immediate execution";
    public static final String LOG_FAILED_SEND_DYNAMIC_RULE_NOTIFICATIONS_IMMEDIATE = "Failed to send dynamic rule assignment notifications for immediate execution";
    public static final String LOG_NOTIFICATIONS_DISABLED_DYNAMIC_RULE = "Notifications disabled for dynamic rule assignment, skipping notifications";
    public static final String LOG_ABOUT_TO_QUERY_USERS_WITH_ROLE = "About to query users with role BEFORE any modifications";
    public static final String LOG_QUERIED_USERS_WITH_ROLE_SNAPSHOT = "Queried users with role - this is the SNAPSHOT";
    public static final String LOG_DEBUG_NOTIFICATION_STORED_USERS = "DEBUG NOTIFICATION: Stored users to notify";
    public static final String LOG_SENDING_NOTIFICATIONS_BEFORE_ASSIGNMENT = "Sending dynamic rule assignment notifications BEFORE assignment attempt";
    public static final String LOG_FAILED_SEND_NOTIFICATIONS_BEFORE_ASSIGNMENT = "Failed to send dynamic rule assignment notifications before assignment";
    public static final String LOG_NOTIFICATIONS_DISABLED_RECONCILIATION = "Notifications disabled for dynamic rule reconciliation, skipping notifications";
    public static final String LOG_FAILED_PUBLISH_RECONCILIATION_AUDIT_SAFELY = "Failed to publish role reconciliation audit event safely";
    public static final String LOG_ROLE_RECONCILIATION_ROLE_DETAILS = "Role reconciliation - role details";
    public static final String LOG_CREATING_ROLE_RECONCILIATION_AUDIT = "Creating role reconciliation audit event";
    public static final String LOG_PREPARING_DYNAMIC_RULE_NOTIFICATIONS = "Preparing dynamic rule assignment notifications for successful assignments only";
    public static final String LOG_ROLE_NOT_FOUND_DYNAMIC_RULE_NOTIFICATION = "Role not found for dynamic rule notification";
    public static final String LOG_ROLE_INFO_RETRIEVED_NOTIFICATION = "Role information retrieved for dynamic rule notification";
    public static final String LOG_RETRIEVED_USER_DATA_NOTIFICATION = "Retrieved user data for dynamic rule notification";
    public static final String LOG_CREATED_NOTIFICATION_DATA_USER = "Created notification data for user via dynamic rule";
    public static final String LOG_USER_NOT_FOUND_NOTIFICATION = "User not found for dynamic rule notification";
    public static final String LOG_SENDING_DYNAMIC_RULE_NOTIFICATION_EVENT = "Sending dynamic rule assignment notification event";
    public static final String LOG_DYNAMIC_RULE_NOTIFICATION_SENT_SUCCESSFULLY = "Dynamic rule assignment notification sent successfully";
    public static final String LOG_NO_VALID_USERS_FOR_NOTIFICATION = "No valid users found for dynamic rule notification";
    public static final String LOG_FAILED_SEND_DYNAMIC_RULE_NOTIFICATIONS = "Failed to send dynamic rule assignment notifications";
    
    // Log Messages for Dynamic Rule Revocation Notifications
    public static final String LOG_PREPARING_DYNAMIC_RULE_REVOKE_NOTIFICATIONS = "Preparing dynamic rule revocation notifications for successfully revoked users";
    public static final String LOG_SENDING_DYNAMIC_RULE_REVOKE_NOTIFICATION_EVENT = "Sending dynamic rule revocation notification event";
    public static final String LOG_DYNAMIC_RULE_REVOKE_NOTIFICATION_SENT_SUCCESSFULLY = "Dynamic rule revocation notification sent successfully";
    public static final String LOG_NO_VALID_USERS_FOR_REVOKE_NOTIFICATION = "No valid users found for dynamic rule revocation notification";
    public static final String LOG_FAILED_SEND_DYNAMIC_RULE_REVOKE_NOTIFICATIONS = "Failed to send dynamic rule revocation notifications";
    
    public static final String DETAIL_KEY_IS_ACTIVE = "isActive";
    public static final String DETAIL_KEY_SUCCESSFUL_ASSIGNMENT_COUNT = "successfulAssignmentCount";
    public static final String DETAIL_KEY_USERS_TO_NOTIFY_COUNT = "usersToNotifyCount";
    public static final String DETAIL_KEY_USERS_TO_NOTIFY_LIST = "usersToNotifyList";
    public static final String DETAIL_KEY_USERS_TO_ADD_COUNT = "usersToAddCount";
    public static final String DETAIL_KEY_SEND_NOTIFICATION_ON_ASSIGNMENT = "sendNotificationOnAssignment";
    public static final String DETAIL_KEY_USERS_TO_NOTIFY = "usersToNotify";
    public static final String DETAIL_KEY_ROLE_RISK_LEVEL = "roleRiskLevel";
    public static final String DETAIL_KEY_ROLE_RISK_LEVEL_LOWERCASE = "roleRiskLevelLowercase";
    public static final String DETAIL_KEY_RISK_LEVEL_BEING_USED = "riskLevelBeingUsed";
    public static final String DETAIL_KEY_AUDIT_EVENT_SOURCE = "auditEventSource";
    public static final String DETAIL_KEY_USERS_BEFORE_COUNT = "usersBeforeCount";
    public static final String DETAIL_KEY_USERS_AFTER_COUNT = "usersAfterCount";
    public static final String DETAIL_KEY_USERS_ADDED_COUNT = "usersAddedCount";
    public static final String DETAIL_KEY_USERS_REMOVED_COUNT = "usersRemovedCount";
    public static final String DETAIL_KEY_THREAD_ID = "threadId";
    public static final String DETAIL_KEY_THREAD_NAME = "threadName";
    public static final String DETAIL_KEY_STACK_TRACE = "stackTrace";
    public static final String DETAIL_KEY_USERS_BEFORE = "usersBefore";
    public static final String DETAIL_KEY_USERS_AFTER = "usersAfter";
    public static final String DETAIL_KEY_SUCCESSFUL_USER_COUNT = "successfulUserCount";
    public static final String DETAIL_KEY_TOTAL_USERS_IN_TENANT = "totalUsersInTenant";
    public static final String DETAIL_KEY_EMAIL = "email";
    public static final String DETAIL_KEY_OPERATION_TYPE = "operationType";
    public static final String DETAIL_KEY_TIMESTAMP_MS = "timestampMs";
    public static final String DETAIL_KEY_USER_IDS = "userIds";
    public static final String DETAIL_KEY_OLD_CONDITION_SCRIPT = "oldConditionScript";
    public static final String DETAIL_KEY_NEW_CONDITION_SCRIPT = "newConditionScript";
    public static final String DETAIL_KEY_USERS_WHO_SHOULD_HAVE = "usersWhoShouldHave";
    public static final String DETAIL_KEY_USERS_TO_REMOVE = "usersToRemove";
    public static final String DETAIL_KEY_USERS_TO_ADD = "usersToAdd";
    public static final String DETAIL_KEY_USERS_TO_KEEP = "usersToKeep";
    public static final String DETAIL_KEY_USERS_KEPT = "usersKept";
    public static final String DETAIL_KEY_EXECUTION_TIME_MS = "executionTimeMs";
    public static final String DETAIL_KEY_SUCCESSFUL_REMOVALS = "successfulRemovals";
    public static final String DETAIL_KEY_FAILED_REMOVALS = "failedRemovals";
    public static final String DETAIL_KEY_SUCCESSFUL_ADDITIONS = "successfulAdditions";
    public static final String DETAIL_KEY_FAILED_ADDITIONS = "failedAdditions";
    public static final String DETAIL_KEY_USERS_REMOVED = "usersRemoved";
    public static final String DETAIL_KEY_USERS_ADDED = "usersAdded";
    public static final String DETAIL_KEY_USERS_KEPT_SIZE = "usersKept";
    public static final String DETAIL_KEY_NEW_SCHEDULE_TYPE = "newScheduleType";
    public static final String DETAIL_KEY_NEW_RULE_IS_ACTIVE = "newRuleIsActive";
    public static final String DETAIL_KEY_PREVIOUS_SCHEDULE_TYPE = "previousScheduleType";
    public static final String DETAIL_KEY_CONDITION_SCRIPT_CHANGED = "conditionScriptChanged";
    public static final String DETAIL_KEY_PREVIOUS_CONDITION_SCRIPT = "previousConditionScript";
    public static final String DETAIL_KEY_NEW_CONDITION_SCRIPT_VALUE = "newConditionScript";
    public static final String DETAIL_KEY_PREVIOUS_CONDITION = "previousCondition";
    public static final String DETAIL_KEY_NEW_CONDITION = "newCondition";

    // ==================== RoleServiceImpl Logger Messages ====================
    public static final String LOG_NO_USERS_FOUND_IN_TENANT = "No users found in tenant";
    public static final String LOG_VALIDATING_ROLE_OWNERS_VIA_GRPC = "Validating role owners via gRPC";
    public static final String LOG_INVALID_OWNER_IDS_PROVIDED = "Invalid owner IDs provided - users not found or not in tenant";
    public static final String LOG_FETCHING_OWNERS_DATA_FOR_ROLE = "Fetching owners data for role";
    public static final String LOG_ROLE_OWNER_USER_NOT_FOUND = "Role owner user not found in tenant or deleted";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_OWNERS_DATA = "Successfully retrieved owners data for role";
    public static final String LOG_FAILED_TO_RETRIEVE_OWNERS_DATA = "Failed to retrieve owners data for role";
    
    // Role Owners Endpoint Logger Messages
    public static final String LOG_PROCESSING_GET_ROLE_OWNERS_REQUEST = "Processing get role owners request";
    public static final String LOG_ROLE_VALIDATION_FOR_OWNERS_RETRIEVAL = "Validating role for owners retrieval";
    public static final String LOG_FETCHING_ROLE_OWNERS_FOR_ROLE = "Fetching role owners for role";
    public static final String LOG_FOUND_OWNERS_FOR_ROLE = "Found owners for role";
    public static final String LOG_MAPPING_USERS_WITH_OWNERSHIP_STATUS = "Mapping users with ownership status";
    public static final String LOG_SORTING_USERS_BY_OWNERSHIP_STATUS = "Sorting users by ownership status";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_ROLE_OWNERS = "Successfully retrieved role owners";
    public static final String LOG_FAILED_TO_RETRIEVE_ROLE_OWNERS = "Failed to retrieve role owners";
    public static final String LOG_GET_ROLE_OWNERS_ENDPOINT_CALLED = "Get role owners endpoint called";
    public static final String LOG_GET_ROLE_OWNERS_SUCCESSFUL = "Get role owners successful";
    public static final String LOG_GET_ROLE_OWNERS_FAILED_CONTROLLER = "Get role owners failed at controller level";

    // ==================== RoleServiceImpl Detail Keys ====================
    public static final String DETAIL_KEY_ROLE_COUNT = "roleCount";
    public static final String DETAIL_KEY_HAS_CONDITIONAL_RULE = "hasConditionalRule";
    public static final String DETAIL_KEY_JUSTIFICATION = "justification";
    public static final String DETAIL_KEY_EXISTING_PERMISSION_COUNT = "existingPermissionCount";
    public static final String DETAIL_KEY_PERMISSION_ID = "permissionId";
    public static final String DETAIL_KEY_ROLE_PERMISSION_ID = "rolePermissionId";
    public static final String DETAIL_KEY_CONDITIONAL_RULE_ID = "conditionalRuleId";
    public static final String DETAIL_KEY_CONDITION_SCRIPT_PROVIDED = "conditionScriptProvided";
    public static final String DETAIL_KEY_SUCCESS = "success";
    public static final String DETAIL_KEY_OLD_JUSTIFICATION = "oldJustification";
    public static final String DETAIL_KEY_NEW_JUSTIFICATION = "newJustification";
    public static final String DETAIL_KEY_VALID_OWNERS_COUNT = "validOwnersCount";
    public static final String DETAIL_KEY_TOTAL_OWNER_IDS = "totalOwnerIds";
    public static final String DETAIL_KEY_USER_TENANT_ID = "userTenantId";
    public static final String DETAIL_KEY_ROLE_TENANT_ID = "roleTenantId";
    public static final String DETAIL_KEY_OWNER_ID = "ownerId";
    public static final String DETAIL_KEY_OWNERS_COUNT = "ownersCount";
    public static final String DETAIL_KEY_USERS_WITH_OWNERSHIP_COUNT = "usersWithOwnershipCount";
    public static final String DETAIL_KEY_USERS_WITHOUT_OWNERSHIP_COUNT = "usersWithoutOwnershipCount";

    // ==================== ScheduledDynamicRuleExecutionServiceImpl Logger Messages ====================
    public static final String LOG_SKIPPING_RULE_ALREADY_EXECUTED = "Skipping rule execution - already executed in current period";
    public static final String LOG_CREATED_PENDING_EXECUTION_HISTORY = "Created pending execution history record";
    public static final String LOG_FAILED_CREATE_PENDING_HISTORY = "Failed to create pending execution history - duplicate execution prevented";
    public static final String LOG_UPDATED_EXECUTION_HISTORY = "Updated execution history with final results";
    public static final String LOG_FAILED_UPDATE_EXECUTION_HISTORY = "Failed to update execution history";

    // ==================== ScheduledDynamicRuleExecutionServiceImpl Detail Keys ====================
    public static final String DETAIL_KEY_HISTORY_ID = "historyId";
    public static final String DETAIL_KEY_SUCCESSFUL = "successful";

    // ==================== UserPermissionServiceImpl Logger Messages ====================
    public static final String LOG_STARTING_PAGE_ACCESS_CHECK_FOR_USER = "Starting page access check for user";
    public static final String LOG_RETRIEVED_SYSTEM_PAGES_FOR_ACCESS_CHECK = "Retrieved system pages for access check";
    public static final String LOG_CHECKED_VIEW_PERMISSION_FOR_PAGE_WITH_ASSIGNED_PERMISSIONS = "Checked View permission for page with assigned permissions";
    public static final String LOG_PAGE_HAS_VIEW_ACTION_BUT_NO_PERMISSIONS_ASSIGNED = "Page has View action but no permissions assigned - allowing open access";
    public static final String LOG_PAGE_HAS_NO_VIEW_ACTION_ALLOWING_OPEN_ACCESS = "Page has no View action - allowing open access";
    public static final String LOG_COMPLETED_PAGE_ACCESS_CHECK_FOR_USER = "Completed page access check for user";
    public static final String LOG_FAILED_TO_CHECK_PAGE_ACCESS_FOR_USER = "Failed to check page access for user";
    public static final String LOG_CHECKING_USER_PERMISSION_FOR_ACTION = "Checking user permission for action";
    public static final String LOG_USER_HAS_NO_ROLES_ASSIGNED = "User has no roles assigned";
    public static final String LOG_ERROR_CHECKING_PERMISSION_FOR_ACTION = "Error checking permission for action";

    // ==================== UserPermissionServiceImpl Detail Keys ====================
    public static final String DETAIL_KEY_PAGE_COUNT = "pageCount";
    public static final String DETAIL_KEY_CAN_ACCESS = "canAccess";
    public static final String DETAIL_KEY_REQUIRES_PERMISSION = "requiresPermission";
    public static final String DETAIL_KEY_TOTAL_PAGES = "totalPages";
    public static final String DETAIL_KEY_ACCESSIBLE_PAGES = "accessiblePages";

    // ==================== UserPermissionServiceImpl Object Types ====================
    public static final String OBJECT_TYPE_PAGE_ACCESS_CHECK = "PAGE_ACCESS_CHECK";
    public static final String OBJECT_TYPE_ACTION_PERMISSION_CHECK = "ACTION_PERMISSION_CHECK";

    // ==================== ROLE AUDIT ASPECT DEBUG CONSTANTS ====================
    
    // Role Audit Aspect Debug Messages
    public static final String LOG_ASSIGN_ROLE_POINTCUT_TRIGGERED_DEBUG = "assignRoleToUserPointcut triggered - DEBUG DUPLICATE AUDIT";
    public static final String LOG_SKIPPING_INDIVIDUAL_ROLE_ASSIGNMENT_DEBUG = "Skipping individual role assignment audit - in dynamic rule execution context - DEBUG";
    public static final String LOG_USING_TENANT_ID_FROM_METHOD_PARAMETER = "Using tenant ID from method parameter (scheduled execution context)";
    public static final String LOG_USING_TENANT_ID_FROM_HTTP_CONTEXT = "Using tenant ID from HTTP request context";
    public static final String LOG_AUDIT_ASPECT_TRIGGERED_BULK_REVOCATION = "Audit aspect triggered for bulk role revocation";
    public static final String LOG_FAILED_TO_COMPLETE_BULK_REVOCATION_AUDIT = "Failed to complete bulk role revocation audit";
    public static final String LOG_FAILED_TO_CREATE_STATE_BEFORE_BULK_REVOCATION = "Failed to create stateBefore for bulk revocation audit";
    public static final String LOG_FAILED_TO_CREATE_STATE_AFTER_BULK_REVOCATION = "Failed to create stateAfter for bulk revocation audit";
    public static final String LOG_CREATING_AUDIT_EVENT_BULK_REVOCATION = "Creating audit event for bulk role revocation";
    public static final String LOG_SUCCESSFULLY_PUBLISHED_BULK_REVOCATION_AUDIT = "Successfully published bulk role revocation audit event";
    public static final String LOG_FAILED_TO_CREATE_OR_PUBLISH_BULK_REVOCATION_AUDIT = "Failed to create or publish bulk role revocation audit event";
    public static final String LOG_FAILED_TO_CREATE_AUDIT_EVENT_PERMISSION_DELETION = "Failed to create audit event for permission deletion";
    public static final String LOG_CREATING_AUDIT_EVENT_PERMISSION_DELETION = "Creating audit event for permission deletion";
    public static final String LOG_SUCCESSFULLY_PUBLISHED_PERMISSION_DELETION_AUDIT = "Successfully published permission deletion audit event";
    public static final String LOG_FAILED_TO_CREATE_OR_PUBLISH_PERMISSION_DELETION_AUDIT = "Failed to create or publish permission deletion audit event";
    public static final String LOG_FAILED_TO_CREATE_AUDIT_EVENT_PERMISSION_UPDATE = "Failed to create audit event for permission update";
    public static final String LOG_CREATING_AUDIT_EVENT_PERMISSION_UPDATE = "Creating audit event for permission update";
    public static final String LOG_SUCCESSFULLY_PUBLISHED_PERMISSION_UPDATE_AUDIT = "Successfully published permission update audit event";
    public static final String LOG_FAILED_TO_CREATE_OR_PUBLISH_PERMISSION_UPDATE_AUDIT = "Failed to create or publish permission update audit event";
    public static final String LOG_ACTION_NOT_FOUND_FOR_AUDIT = "Action not found for audit, using minimal information";
    public static final String LOG_ACTION_NOT_FOUND_FOR_DELETION_AUDIT = "Action not found for permission deletion audit, using minimal information";
    public static final String LOG_ACTION_NOT_FOUND_FOR_UPDATE_AUDIT = "Action not found for permission update audit, using minimal information";
    public static final String LOG_FAILED_TO_FETCH_PERMISSION_HIERARCHY_FOR_DELETION = "Failed to fetch permission hierarchy for deletion audit";
    public static final String LOG_FAILED_TO_FETCH_PERMISSION_HIERARCHY_FOR_UPDATE_STATE_BEFORE = "Failed to fetch permission hierarchy for update audit stateBefore";
    public static final String LOG_FAILED_TO_FETCH_PERMISSION_HIERARCHY_FOR_UPDATE = "Failed to fetch permission hierarchy for update audit";
    public static final String LOG_AUDIT_ASPECT_TRIGGERED_DYNAMIC_RULE_CREATION = "Audit aspect triggered for dynamic rule creation";
    public static final String LOG_SET_DYNAMIC_RULE_CONTEXT_FOR_IMMEDIATE = "Set dynamic rule execution context for IMMEDIATE rule";
    public static final String LOG_CLEARED_DYNAMIC_RULE_CONTEXT = "Cleared dynamic rule execution context";
    public static final String LOG_FAILED_TO_CREATE_OR_PUBLISH_DYNAMIC_RULE_AUDIT = "Failed to create or publish dynamic rule audit event";
    public static final String LOG_CREATING_IMMEDIATE_DYNAMIC_RULE_AS_ROLE_ASSIGNMENT = "Creating audit event for IMMEDIATE dynamic rule as role assignment";
    public static final String LOG_SUCCESSFULLY_PUBLISHED_IMMEDIATE_DYNAMIC_RULE_AUDIT = "Successfully published audit event for IMMEDIATE dynamic rule";
    public static final String LOG_FAILED_TO_CREATE_OR_PUBLISH_IMMEDIATE_DYNAMIC_RULE_AUDIT = "Failed to create or publish IMMEDIATE dynamic rule audit event";
    public static final String LOG_FAILED_TO_GET_ELIGIBLE_USERS_FOR_DYNAMIC_RULE = "Failed to get eligible users for dynamic rule";
    public static final String LOG_CREATING_AUDIT_EVENT_DYNAMIC_RULE_CREATION = "Creating audit event for dynamic rule creation";
    public static final String LOG_SUCCESSFULLY_PUBLISHED_DYNAMIC_RULE_CREATION_AUDIT = "Successfully published audit event for dynamic rule creation";
    public static final String LOG_FAILED_TO_CREATE_OR_PUBLISH_DYNAMIC_RULE_CREATION_AUDIT = "Failed to create or publish dynamic rule creation audit event";
    public static final String LOG_AUDIT_ASPECT_TRIGGERED_DYNAMIC_RULE_UPDATE = "Audit aspect triggered for dynamic rule update";
    public static final String LOG_SET_DYNAMIC_RULE_CONTEXT_FOR_DAILY_TO_IMMEDIATE = "Set dynamic rule execution context for DAILY->IMMEDIATE schedule change";
    public static final String LOG_FAILED_TO_CREATE_OR_PUBLISH_DYNAMIC_RULE_UPDATE_AUDIT = "Failed to create or publish dynamic rule update audit event";
    public static final String LOG_CREATING_SCHEDULE_CHANGE_AS_ROLE_ASSIGNMENT = "Creating audit event for schedule change as role assignment";
    public static final String LOG_FAILED_TO_CREATE_OR_PUBLISH_SCHEDULE_CHANGE_AUDIT = "Failed to create or publish schedule change audit event";
    public static final String LOG_CREATING_AUDIT_EVENT_DYNAMIC_RULE_UPDATE = "Creating audit event for dynamic rule update";
    public static final String LOG_FAILED_TO_CREATE_OR_PUBLISH_DYNAMIC_RULE_UPDATE_AUDIT_EVENT = "Failed to create or publish dynamic rule update audit event";
    public static final String LOG_FAILED_TO_GET_ELIGIBLE_USERS_FOR_UPDATE_RULE_AUDIT = "Failed to get eligible users for update rule audit";
    public static final String LOG_FAILED_TO_EVALUATE_CONDITION_FOR_USER_IN_UPDATE_AUDIT = "Failed to evaluate condition for user in update audit";
    public static final String LOG_ACTION_NOT_FOUND_FOR_PERMISSION_UPDATE_AUDIT = "Action not found for permission update audit, using minimal information";
    public static final String LOG_SET_DYNAMIC_RULE_CONTEXT_FOR_IMMEDIATE_RULE = "Set dynamic rule execution context for IMMEDIATE rule";
    public static final String LOG_FAILED_TO_CREATE_OR_PUBLISH_DYNAMIC_RULE_AUDIT_EVENT = "Failed to create or publish dynamic rule audit event";
    public static final String LOG_CLEARED_DYNAMIC_RULE_EXECUTION_CONTEXT = "Cleared dynamic rule execution context";
    public static final String LOG_CREATING_AUDIT_EVENT_IMMEDIATE_DYNAMIC_RULE = "Creating audit event for IMMEDIATE dynamic rule as role assignment";
    public static final String ACTION_ASSIGN_ROLE = "assign role";
    public static final String ACTION_CREATE_DYNAMIC_RULE = "create dynamic rule";
    public static final String DEFAULT_UNKNOWN_ACTION = "Unknown Action";
    public static final String ENTITY_TYPE_ROLE_TITLE_CASE = "Role";
    public static final String DETAIL_KEY_ID = "id";
    public static final String DETAIL_KEY_NAME = "name";
    public static final String DETAIL_KEY_ACTION = "action";
    public static final String DETAIL_KEY_USER_ID_CAMEL = "userId";
    public static final String DETAIL_KEY_USER_NAME_CAMEL = "userName";
    public static final String DETAIL_KEY_USERS = "users";
    public static final String DETAIL_KEY_RULE_CONDITION_SCRIPT = "ruleConditionScript";
    public static final String DETAIL_KEY_REQUIRE_APPROVAL_BEFORE_ASSIGNMENT = "requireApprovalBeforeAssignment";
    public static final String DETAIL_KEY_TIME = "time";
    public static final String ACTION_UPDATE_DYNAMIC_RULE = "update dynamic rule";
    public static final String ENTITY_TYPE_DYNAMIC_RULE = "dynamic rule";
    public static final String DETAIL_KEY_SCHEDULE_CHANGE_VALUE = "DAILY->IMMEDIATE";

    // Role Audit Aspect Detail Keys (new ones only - reusing existing: DETAIL_KEY_THREAD_ID, DETAIL_KEY_THREAD_NAME, DETAIL_KEY_SCHEDULE_TYPE, DETAIL_KEY_ROLE_NAME, DETAIL_KEY_RULE_IS_ACTIVE, DETAIL_KEY_SEND_NOTIFICATION_ON_ASSIGNMENT)
    public static final String DETAIL_KEY_IN_DYNAMIC_RULE_CONTEXT = "inDynamicRuleContext";
    public static final String DETAIL_KEY_CONTEXT_VALUE = "contextValue";
    // DETAIL_KEY_ASSIGNMENT_TYPE already defined above at line 1439
    public static final String DETAIL_KEY_USER_COUNT_VALUE = "userCount";
    public static final String DETAIL_KEY_ELIGIBLE_USER_COUNT = "eligibleUserCount";
    public static final String DETAIL_KEY_SCHEDULE_CHANGE = "scheduleChange";
    public static final String DETAIL_KEY_HAS_CONDITION = "hasCondition";
    
    // Global Dynamic Rule Audit Constants
    public static final String AUDIT_EVENT_CREATE_GLOBAL_DYNAMIC_RULE = "create";
    public static final String AUDIT_EVENT_UPDATE_GLOBAL_DYNAMIC_RULE = "update";
    public static final String AUDIT_EVENT_DELETE_GLOBAL_DYNAMIC_RULE = "delete";
    public static final String ENTITY_TYPE_GLOBAL_DYNAMIC_RULE = "Global Dynamic Rule";
    public static final String ENTITY_NAME_GLOBAL_DYNAMIC_RULE = "Global Dynamic Rule";
    public static final String RISK_LEVEL_HIGH = "high";
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_GLOBAL_DYNAMIC_RULE_CREATION = "Audit aspect triggered for global dynamic rule creation";
    public static final String AUDIT_LOG_AUDITING_GLOBAL_DYNAMIC_RULE_CREATION = "Auditing global dynamic rule creation";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_GLOBAL_DYNAMIC_RULE_CREATION = "Successfully published audit event for global dynamic rule creation";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_GLOBAL_DYNAMIC_RULE_CREATION = "Failed to publish audit event for global dynamic rule creation";
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_GLOBAL_DYNAMIC_RULE_UPDATE = "Audit aspect triggered for global dynamic rule update";
    public static final String AUDIT_LOG_AUDITING_GLOBAL_DYNAMIC_RULE_UPDATE = "Auditing global dynamic rule update";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_GLOBAL_DYNAMIC_RULE_UPDATE = "Successfully published audit event for global dynamic rule update";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_GLOBAL_DYNAMIC_RULE_UPDATE = "Failed to publish audit event for global dynamic rule update";
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_GLOBAL_DYNAMIC_RULE_DELETION = "Audit aspect triggered for global dynamic rule deletion";
    public static final String AUDIT_LOG_AUDITING_GLOBAL_DYNAMIC_RULE_DELETION = "Auditing global dynamic rule deletion";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_GLOBAL_DYNAMIC_RULE_DELETION = "Successfully published audit event for global dynamic rule deletion";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_GLOBAL_DYNAMIC_RULE_DELETION = "Failed to publish audit event for global dynamic rule deletion";
    public static final String DETAIL_KEY_GLOBAL_DYNAMIC_RULE_ID = "globalDynamicRuleId";

    // Global Dynamic Rule Service Implementation Messages
    public static final String ERROR_GLOBAL_RULE_ALREADY_EXISTS = "Global dynamic rule already exists for tenant: ";
    public static final String ERROR_TIME_REQUIRED_FOR_SCHEDULED_RULES = "Time is required for scheduled rules (DAILY/WEEKLY/MONTHLY)";
    public static final String ERROR_TIME_NOT_ALLOWED_FOR_IMMEDIATE = "Time should not be provided for IMMEDIATE schedule type";
    public static final String ERROR_GLOBAL_RULE_NOT_FOUND_FOR_TENANT = "Global dynamic rule not found for tenant: ";
    public static final String ERROR_GLOBAL_RULE_NOT_FOUND = "Global dynamic rule not found: ";
    public static final String ERROR_RULE_DOES_NOT_BELONG_TO_TENANT = "Global dynamic rule does not belong to tenant: ";
    public static final String ERROR_GLOBAL_RULE_NOT_ACTIVE = "Global dynamic rule is not active";
    
    public static final String LOG_EXECUTING_RULE_IMMEDIATELY = "Executing global dynamic rule immediately";
    public static final String LOG_FAILED_TO_EXECUTE_RULE_IMMEDIATELY = "Failed to execute global dynamic rule immediately";
    public static final String LOG_EXECUTING_RULE_AFTER_UPDATE = "Executing global dynamic rule after update";
    public static final String LOG_FAILED_TO_EXECUTE_RULE_AFTER_UPDATE = "Failed to execute global dynamic rule after update";
    public static final String LOG_FOUND_ROLES_FOR_TENANT = "Found roles for tenant";
    public static final String LOG_FETCHING_ALL_USERS_FOR_TENANT = "Fetching all users for tenant";
    public static final String LOG_EVALUATING_CONDITION_SCRIPT = "Evaluating condition script for users across all roles";
    public static final String LOG_PROCESSING_ROLE = "Processing role";
    public static final String LOG_ASSIGNING_ROLE_TO_USER = "Assigning role to user";
    public static final String LOG_SUCCESSFULLY_ASSIGNED_ROLE_TO_USER = "Successfully assigned role to user";
    public static final String LOG_USER_ALREADY_HAS_ROLE = "User already has role - skipping";
    public static final String LOG_ERROR_EVALUATING_CONDITION = "Error evaluating condition for user";
    public static final String LOG_REMOVING_ROLE_FROM_USER = "Removing role from user (condition no longer matches)";
    public static final String LOG_SUCCESSFULLY_REMOVED_ROLE = "Successfully removed role from user";
    public static final String LOG_FAILED_TO_REMOVE_ROLE = "Failed to remove role from user";
    public static final String LOG_SENDING_NOTIFICATIONS_FOR_ROLE_ASSIGNMENTS = "Sending notifications for role assignments";
    public static final String LOG_FAILED_TO_SEND_NOTIFICATIONS = "Failed to send notifications for role assignments";
    public static final String LOG_NOTIFICATIONS_DISABLED = "Notifications disabled for global dynamic rule";
    public static final String LOG_FAILED_TO_PUBLISH_CONSOLIDATED_AUDIT = "Failed to publish consolidated audit log for role";
    public static final String LOG_FAILED_TO_SERIALIZE_BREAKDOWN = "Failed to serialize per-role breakdown to JSON";
    public static final String LOG_PREPARING_GLOBAL_RULE_NOTIFICATIONS = "Preparing global dynamic rule notifications";
    public static final String LOG_ROLE_NOT_FOUND_FOR_NOTIFICATION = "Role not found for notification";
    public static final String LOG_ROLE_INFO_RETRIEVED_FOR_NOTIFICATION = "Role info retrieved for notification";
    public static final String LOG_RETRIEVED_USER_DATA_FOR_NOTIFICATIONS = "Retrieved user data for notifications";
    public static final String LOG_SENT_INDIVIDUAL_NOTIFICATION = "Sent individual notification for role assignment";
    public static final String LOG_FAILED_TO_SEND_NOTIFICATION_TO_USER = "Failed to send notification to user";
    public static final String LOG_USER_NOT_FOUND_FOR_NOTIFICATION = "User not found for notification";
    public static final String LOG_GLOBAL_RULE_NOTIFICATIONS_COMPLETED = "Global dynamic rule notifications completed";
    public static final String LOG_FAILED_TO_SEND_GLOBAL_RULE_NOTIFICATIONS = "Failed to send global dynamic rule notifications";
    public static final String LOG_PUBLISHED_CONSOLIDATED_AUDIT_LOG = "Published consolidated audit log for global dynamic rule role assignments";
    public static final String LOG_FAILED_TO_PUBLISH_CONSOLIDATED_AUDIT_LOG = "Failed to publish consolidated audit log";

    // Page Access Check Log Messages
    public static final String LOG_PROCESSING_USER_PAGES_ACCESS_REQUEST = "Processing user pages access request";
    public static final String LOG_EXTRACTED_USER_CONTEXT_FROM_JWT = "Extracted user context from JWT";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_USER_PAGE_ACCESS = "Successfully retrieved user page access information";
    public static final String LOG_FAILED_TO_RETRIEVE_USER_PAGE_ACCESS = "Failed to retrieve user page access information";
    public static final String LOG_PROCESSING_PAGE_ACCESS_CHECK_REQUEST = "Processing page access check request";
    public static final String LOG_EXTRACTED_USER_CONTEXT_FOR_PAGE_ACCESS_CHECK = "Extracted user context from JWT for page access check";
    public static final String LOG_PAGE_NOT_FOUND_FOR_PATH = "Page not found for path";
    public static final String LOG_TRYING_PARENT_PATH_FOR_PAGE_ACCESS = "Trying parent path for page access check";
    public static final String LOG_FOUND_PARENT_PAGE_FOR_SUB_ROUTE = "Found parent page for sub-route";
    public static final String LOG_NO_PAGE_OR_PARENT_FOUND_ALLOWING_ACCESS = "No page or parent page found - allowing access for unmanaged route";
    public static final String LOG_PAGE_REQUIRES_PERMISSION_CHECKED_USER_ACCESS = "Page requires permission - checked user access";
    public static final String LOG_PAGE_HAS_VIEW_ACTION_NO_PERMISSIONS = "Page has View action but no permissions assigned - access granted";
    public static final String LOG_PAGE_HAS_NO_VIEW_ACTION = "Page has no View action - access granted";
    public static final String LOG_PAGE_ACCESS_CHECK_COMPLETED = "Page access check completed";
    public static final String LOG_FAILED_TO_CHECK_PAGE_ACCESS = "Failed to check page access";
    
    // Page Access Check Object Types
    public static final String OBJECT_TYPE_PAGE_ACCESS_REQUEST = "PAGE_ACCESS_REQUEST";
    // Note: OBJECT_TYPE_PAGE_ACCESS_CHECK is already defined earlier in this file
    
    // Page Access Check Detail Keys
    public static final String DETAIL_KEY_PATH = "path";
    public static final String GET_PERMISSION_DETAILS = "getPermissionDetails";
    public static final String PERMISSION_DETAILS_RETRIEVED = "Permission details retrieved successfully";
    public static final String PERMISSION_NOT_FOUND_ROLE_ACTION = "Permission not found for role and action";
    public static final String FAILED_TO_GET_PERMISSION_DETAILS = "Failed to get permission details";
    public static final String PERMISSION_NOT_FOUND_THIS_ROLE_ACTION = "Permission not found for this role and action";
    // ======================================================
    // Available Modules messages
    // ======================================================
    public static final String RETRIEVING_AVAILABLE_MODULES = "Retrieving available modules";
    public static final String AVAILABLE_MODULES_RETRIEVED_SUCCESSFULLY = "Available modules retrieved successfully";
    public static final String FAILED_TO_RETRIEVE_AVAILABLE_MODULES = "Failed to retrieve available modules";
    public static final String AVAILABLE_MODULES_REQUEST = "AVAILABLE_MODULES_REQUEST";
    public static final String AVAILABLE_MODULES_RESPONSE = "AVAILABLE_MODULES_RESPONSE";
    public static final String AVAILABLE_ROLES_RETRIEVED_SUCCESS = "Available roles retrieved successfully";
    public static final String AVAILABLE_MODULES_RETRIEVED_SUCCESS = "Available modules retrieved successfully";

    // ======================================================
    // Available Roles messages
    // ======================================================
    public static final String RETRIEVING_AVAILABLE_ROLES = "Retrieving available roles";
    public static final String AVAILABLE_ROLES_RETRIEVED_SUCCESSFULLY = "Available roles retrieved successfully";
    public static final String FAILED_TO_RETRIEVE_AVAILABLE_ROLES = "Failed to retrieve available roles";
    public static final String AVAILABLE_ROLES_REQUEST = "AVAILABLE_ROLES_REQUEST";
    public static final String AVAILABLE_ROLES_RESPONSE = "AVAILABLE_ROLES_RESPONSE";
    
    // Page Access Check Error Messages
    public static final String ERROR_PAGE_NOT_FOUND = "Page not found";
    public static final String ERROR_FAILED_TO_RETRIEVE_PAGE_ACCESS = "Failed to retrieve page access information";
    public static final String ERROR_FAILED_TO_CHECK_PAGE_ACCESS = "Failed to check page access";
    
    // Page Access Check Response Messages
    public static final String MESSAGE_UNMANAGED_ROUTE_OPEN_ACCESS = "Unmanaged route - open access";

    // ===================== PERMISSIONS / AUTHORIZATION =====================
    public static final String PROCESSING_GET_PERMISSION_DETAILS = "Processing get permission details request";
    public static final String ROLE_NOT_FOUND_OR_ACCESS_DENIED_FOR_PERMISSION_DETAILS = "Role not found or access denied for permission details";
    public static final String SEARCHING_PERMISSION_BY_ROLE_AND_ACTION = "Searching for permission by role and action";
    public static final String PERMISSION_NOT_FOUND_FOR_ROLE_AND_ACTION = "Permission not found for this role and action";
    public static final String PERMISSION_NOT_ASSIGNED_TO_ROLE_FOR_ACTION = "Permission not assigned to this role for this action";
    public static final String CONDITIONAL_RULE_FOUND_FOR_PERMISSION = "Conditional rule found for permission";
    public static final String PERMISSION_DETAILS_RETRIEVED_SUCCESSFULLY = "Permission details retrieved successfully";
    public static final String PERMISSION_NOT_FOUND_EXCEPTION = "Permission not found exception";
    public static final String FAILED_TO_GET_PERMISSION_DETAILS_WITH_REASON = "Failed to get permission details: ";
    public static final String SEARCHING_FOR_PERMISSION_BY_ROLE_AND_ACTION = "Searching for permission by role and action";
    
    // ==================== ROLE DELETION CASCADE CONSTANTS ====================
    
    // Role Deletion Cascade Log Messages
    public static final String LOG_DELETING_ROLE_OWNERS = "Deleting role owners";
    public static final String LOG_DELETING_USER_ROLE_ASSIGNMENTS = "Deleting user-role assignments";
    public static final String LOG_DELETING_DYNAMIC_RULES = "Deleting dynamic rules";
    public static final String LOG_DELETING_ROLE_PERMISSION_ASSOCIATIONS = "Deleting role-permission associations and related entities";
    public static final String LOG_DELETING_ORPHANED_PERMISSION = "Deleting orphaned permission and its conditional rules";
    public static final String LOG_PERMISSION_STILL_IN_USE = "Permission still used by other roles, skipping deletion";
    
    // Role Deletion Detail Keys
    public static final String DETAIL_KEY_COUNT = "count";
    // Note: DETAIL_KEY_ROLE_COUNT already defined at line 1781

    // ==================== GLOBAL DYNAMIC RULE CONTROLLER CONSTANTS ====================
    
    // Global Dynamic Rule Controller Logger Names
    public static final String GLOBAL_DYNAMIC_RULE_CONTROLLER_LOGGER_NAME = "GlobalDynamicRuleController";
    
    // Global Dynamic Rule Controller Log Categories
    public static final String LOG_CATEGORY_GLOBAL_DYNAMIC_RULE = "GLOBAL_DYNAMIC_RULE";
    
    // Global Dynamic Rule Controller Log Messages - Endpoint Calls
    public static final String LOG_CREATE_GLOBAL_DYNAMIC_RULE_ENDPOINT_CALLED = "Create global dynamic rule endpoint called";
    public static final String LOG_GET_GLOBAL_DYNAMIC_RULE_ENDPOINT_CALLED = "Get global dynamic rule endpoint called";
    public static final String LOG_UPDATE_GLOBAL_DYNAMIC_RULE_ENDPOINT_CALLED = "Update global dynamic rule endpoint called";
    public static final String LOG_DELETE_GLOBAL_DYNAMIC_RULE_ENDPOINT_CALLED = "Delete global dynamic rule endpoint called";
    public static final String LOG_EXECUTE_GLOBAL_DYNAMIC_RULE_ENDPOINT_CALLED = "Execute global dynamic rule endpoint called";
    
    // Global Dynamic Rule Controller Log Messages - Operations
    public static final String LOG_CREATING_GLOBAL_DYNAMIC_RULE = "Creating global dynamic rule";
    public static final String LOG_GETTING_GLOBAL_DYNAMIC_RULE = "Getting global dynamic rule";
    public static final String LOG_UPDATING_GLOBAL_DYNAMIC_RULE = "Updating global dynamic rule";
    public static final String LOG_DELETING_GLOBAL_DYNAMIC_RULE = "Deleting global dynamic rule";
    public static final String LOG_EXECUTING_GLOBAL_DYNAMIC_RULE = "Executing global dynamic rule";
    
    // Global Dynamic Rule Controller Log Messages - Success
    public static final String LOG_GLOBAL_DYNAMIC_RULE_CREATED_SUCCESSFULLY = "Global dynamic rule created successfully";
    public static final String LOG_GLOBAL_DYNAMIC_RULE_RETRIEVED_SUCCESSFULLY = "Global dynamic rule retrieved successfully";
    public static final String LOG_GLOBAL_DYNAMIC_RULE_UPDATED_SUCCESSFULLY = "Global dynamic rule updated successfully";
    public static final String LOG_GLOBAL_DYNAMIC_RULE_DELETED_SUCCESSFULLY = "Global dynamic rule deleted successfully";
    public static final String LOG_GLOBAL_DYNAMIC_RULE_EXECUTED_SUCCESSFULLY = "Global dynamic rule executed successfully";
    
    // Global Dynamic Rule Controller Log Messages - Failures
    public static final String LOG_FAILED_TO_CREATE_GLOBAL_DYNAMIC_RULE = "Failed to create global dynamic rule";
    public static final String LOG_FAILED_TO_GET_GLOBAL_DYNAMIC_RULE = "Failed to get global dynamic rule";
    public static final String LOG_FAILED_TO_UPDATE_GLOBAL_DYNAMIC_RULE = "Failed to update global dynamic rule";
    public static final String LOG_FAILED_TO_DELETE_GLOBAL_DYNAMIC_RULE = "Failed to delete global dynamic rule";
    public static final String LOG_FAILED_TO_EXECUTE_GLOBAL_DYNAMIC_RULE = "Failed to execute global dynamic rule";
    
    // Global Dynamic Rule Controller Log Messages - Unexpected Errors
    public static final String LOG_UNEXPECTED_ERROR_CREATING_GLOBAL_DYNAMIC_RULE = "Unexpected error creating global dynamic rule";
    public static final String LOG_UNEXPECTED_ERROR_GETTING_GLOBAL_DYNAMIC_RULE = "Unexpected error getting global dynamic rule";
    public static final String LOG_UNEXPECTED_ERROR_UPDATING_GLOBAL_DYNAMIC_RULE = "Unexpected error updating global dynamic rule";
    public static final String LOG_UNEXPECTED_ERROR_DELETING_GLOBAL_DYNAMIC_RULE = "Unexpected error deleting global dynamic rule";
    public static final String LOG_UNEXPECTED_ERROR_EXECUTING_GLOBAL_DYNAMIC_RULE = "Unexpected error executing global dynamic rule";
    
    // Global Dynamic Rule Controller Log Messages - Debug Level
    public static final String LOG_DEBUG_EXTRACTING_TENANT_AND_USER_INFO = "Extracting tenant and user information from security context";
    public static final String LOG_DEBUG_TENANT_AND_USER_INFO_EXTRACTED = "Tenant and user information extracted successfully";
    public static final String LOG_DEBUG_VALIDATING_GLOBAL_DYNAMIC_RULE_REQUEST = "Validating global dynamic rule request";
    public static final String LOG_DEBUG_REQUEST_VALIDATION_PASSED = "Request validation passed";
    public static final String LOG_DEBUG_CALLING_SERVICE_TO_CREATE_RULE = "Calling service to create global dynamic rule";
    public static final String LOG_DEBUG_CALLING_SERVICE_TO_GET_RULE = "Calling service to get global dynamic rule";
    public static final String LOG_DEBUG_CALLING_SERVICE_TO_UPDATE_RULE = "Calling service to update global dynamic rule";
    public static final String LOG_DEBUG_CALLING_SERVICE_TO_DELETE_RULE = "Calling service to delete global dynamic rule";
    public static final String LOG_DEBUG_CALLING_SERVICE_TO_EXECUTE_RULE = "Calling service to execute global dynamic rule";
    public static final String LOG_DEBUG_SERVICE_CALL_COMPLETED = "Service call completed successfully";
    public static final String LOG_DEBUG_MAPPING_ENTITY_TO_RESPONSE = "Mapping entity to response DTO";
    public static final String LOG_DEBUG_RESPONSE_DTO_CREATED = "Response DTO created successfully";
    public static final String LOG_DEBUG_BUILDING_SUCCESS_RESPONSE_BODY = "Building success response body";
    public static final String LOG_DEBUG_BUILDING_ERROR_RESPONSE_BODY = "Building error response body";
    public static final String LOG_DEBUG_VALIDATION_ERROR_DETECTED = "Validation error detected";
    public static final String LOG_DEBUG_EXCEPTION_CAUGHT_IN_CONTROLLER = "Exception caught in controller";
    
    // Global Dynamic Rule Controller Log Messages - Trace Level
    public static final String LOG_TRACE_ENTERING_CREATE_GLOBAL_DYNAMIC_RULE = "Entering createGlobalDynamicRule method";
    public static final String LOG_TRACE_ENTERING_GET_GLOBAL_DYNAMIC_RULE = "Entering getGlobalDynamicRule method";
    public static final String LOG_TRACE_ENTERING_UPDATE_GLOBAL_DYNAMIC_RULE = "Entering updateGlobalDynamicRule method";
    public static final String LOG_TRACE_ENTERING_DELETE_GLOBAL_DYNAMIC_RULE = "Entering deleteGlobalDynamicRule method";
    public static final String LOG_TRACE_ENTERING_EXECUTE_GLOBAL_DYNAMIC_RULE = "Entering executeGlobalDynamicRule method";
    public static final String LOG_TRACE_EXITING_CREATE_GLOBAL_DYNAMIC_RULE = "Exiting createGlobalDynamicRule method";
    public static final String LOG_TRACE_EXITING_GET_GLOBAL_DYNAMIC_RULE = "Exiting getGlobalDynamicRule method";
    public static final String LOG_TRACE_EXITING_UPDATE_GLOBAL_DYNAMIC_RULE = "Exiting updateGlobalDynamicRule method";
    public static final String LOG_TRACE_EXITING_DELETE_GLOBAL_DYNAMIC_RULE = "Exiting deleteGlobalDynamicRule method";
    public static final String LOG_TRACE_EXITING_EXECUTE_GLOBAL_DYNAMIC_RULE = "Exiting executeGlobalDynamicRule method";
    public static final String LOG_TRACE_REQUEST_PAYLOAD = "Request payload";
    public static final String LOG_TRACE_RESPONSE_PAYLOAD = "Response payload prepared";
    public static final String LOG_TRACE_ERROR_DETAILS = "Error details";
    
    // Global Dynamic Rule Controller Response Messages
    public static final String MSG_GLOBAL_DYNAMIC_RULE_CREATED_SUCCESS = "Global dynamic rule created successfully";
    public static final String MSG_GLOBAL_DYNAMIC_RULE_UPDATED_SUCCESS = "Global dynamic rule updated successfully";
    public static final String MSG_GLOBAL_DYNAMIC_RULE_DELETED_SUCCESS = "Global dynamic rule deleted successfully";
    public static final String MSG_GLOBAL_DYNAMIC_RULE_EXECUTED_SUCCESS = "Global dynamic rule executed successfully";
    public static final String MSG_UNEXPECTED_ERROR_OCCURRED = "Unexpected error occurred: ";
    
    // Global Dynamic Rule Controller Detail Keys (most keys reused from earlier definitions)
    // Note: DETAIL_KEY_SCHEDULE_TYPE, DETAIL_KEY_RULE_CONDITION_SCRIPT, DETAIL_KEY_RULE_IS_ACTIVE, DETAIL_KEY_SUCCESS already defined earlier
    public static final String DETAIL_KEY_REQUIRE_APPROVAL = "requireApproval";
    public static final String DETAIL_KEY_SEND_NOTIFICATION = "sendNotification";
    public static final String DETAIL_KEY_RULE_TIME = "time";
    public static final String DETAIL_KEY_RULE_ID = "ruleId";
    public static final String DETAIL_KEY_EXECUTION_RESULT = "executionResult";
    public static final String DETAIL_KEY_ROLES_ASSIGNED = "rolesAssigned";
    public static final String DETAIL_KEY_ROLES_REVOKED = "rolesRevoked";
    public static final String DETAIL_KEY_USERS_AFFECTED = "usersAffected";
    public static final String DETAIL_KEY_HTTP_STATUS = "httpStatus";
    
    // Global Dynamic Rule Controller Log Object Types
    public static final String LOG_OBJECT_TYPE_GLOBAL_DYNAMIC_RULE_CREATE_REQUEST = "GlobalDynamicRuleCreateRequest";
    public static final String LOG_OBJECT_TYPE_GLOBAL_DYNAMIC_RULE_UPDATE_REQUEST = "GlobalDynamicRuleUpdateRequest";
    public static final String LOG_OBJECT_TYPE_GLOBAL_DYNAMIC_RULE_RESPONSE = "GlobalDynamicRuleResponse";
    public static final String LOG_OBJECT_TYPE_GLOBAL_DYNAMIC_RULE_EXECUTION_RESULT = "GlobalDynamicRuleExecutionResult";
    public static final String LOG_OBJECT_TYPE_GLOBAL_DYNAMIC_RULE_ERROR = "GlobalDynamicRuleError";
    public static final String LOG_OBJECT_TYPE_GLOBAL_DYNAMIC_RULE_VALIDATION_ERROR = "GlobalDynamicRuleValidationError";
    
    // ==================== Granular Permission Controller Constants ====================
    
    // Granular Permission Controller - Trace Level Logs
    public static final String LOG_TRACE_ENTERING_CREATE_MODULE = "Entering createSystemModule method";
    public static final String LOG_TRACE_EXITING_CREATE_MODULE = "Exiting createSystemModule method";
    public static final String LOG_TRACE_ENTERING_CREATE_PAGE = "Entering createSystemPage method";
    public static final String LOG_TRACE_EXITING_CREATE_PAGE = "Exiting createSystemPage method";
    public static final String LOG_TRACE_ENTERING_CREATE_ACTION = "Entering createSystemAction method";
    public static final String LOG_TRACE_EXITING_CREATE_ACTION = "Exiting createSystemAction method";
    public static final String LOG_TRACE_ENTERING_GET_HIERARCHY = "Entering getHierarchicalPermissions method";
    public static final String LOG_TRACE_EXITING_GET_HIERARCHY = "Exiting getHierarchicalPermissions method";
    public static final String LOG_TRACE_ENTERING_GET_AVAILABLE_MODULES = "Entering getAvailableModules method";
    public static final String LOG_TRACE_EXITING_GET_AVAILABLE_MODULES = "Exiting getAvailableModules method";
    public static final String LOG_TRACE_ENTERING_GET_AVAILABLE_ROLES = "Entering getAvailableRoles method";
    public static final String LOG_TRACE_EXITING_GET_AVAILABLE_ROLES = "Exiting getAvailableRoles method";
    public static final String LOG_TRACE_MODULE_REQUEST_PAYLOAD = "Module creation request payload received";
    public static final String LOG_TRACE_PAGE_REQUEST_PAYLOAD = "Page creation request payload received";
    public static final String LOG_TRACE_ACTION_REQUEST_PAYLOAD = "Action creation request payload received";
    public static final String LOG_TRACE_MODULE_RESPONSE = "Module creation response prepared";
    public static final String LOG_TRACE_PAGE_RESPONSE = "Page creation response prepared";
    public static final String LOG_TRACE_ACTION_RESPONSE = "Action creation response prepared";
    public static final String LOG_TRACE_HIERARCHY_RESPONSE = "Hierarchical permissions response prepared";
    
    // Granular Permission Controller - Debug Level Logs
    public static final String LOG_DEBUG_CALLING_SERVICE_CREATE_MODULE = "Calling service to create system module";
    public static final String LOG_DEBUG_MODULE_CREATED_BY_SERVICE = "Service successfully created module";
    public static final String LOG_DEBUG_CALLING_SERVICE_CREATE_PAGE = "Calling service to create system page";
    public static final String LOG_DEBUG_PAGE_CREATED_BY_SERVICE = "Service successfully created page";
    public static final String LOG_DEBUG_CALLING_SERVICE_CREATE_ACTION = "Calling service to create system action";
    public static final String LOG_DEBUG_ACTION_CREATED_BY_SERVICE = "Service successfully created action";
    public static final String LOG_DEBUG_CALLING_SERVICE_GET_HIERARCHY = "Calling service to retrieve hierarchical permissions";
    public static final String LOG_DEBUG_HIERARCHY_RETRIEVED_BY_SERVICE = "Service successfully retrieved hierarchical permissions";
    public static final String LOG_DEBUG_EXTRACTING_MODULES_FROM_RESPONSE = "Extracting modules array from response";
    public static final String LOG_DEBUG_EXTRACTING_AVAILABLE_MODULES = "Extracting available modules from response";
    public static final String LOG_DEBUG_EXTRACTING_AVAILABLE_ROLES = "Extracting available roles from response";
    public static final String LOG_DEBUG_BUILDING_MODULE_SUCCESS_RESPONSE = "Building success response for module creation";
    public static final String LOG_DEBUG_BUILDING_PAGE_SUCCESS_RESPONSE = "Building success response for page creation";
    public static final String LOG_DEBUG_BUILDING_ACTION_SUCCESS_RESPONSE = "Building success response for action creation";
    
    // Granular Permission Controller - Info Level Logs
    public static final String LOG_CREATE_MODULE_ENDPOINT_CALLED = "Create system module endpoint called";
    public static final String LOG_CREATE_PAGE_ENDPOINT_CALLED = "Create system page endpoint called";
    public static final String LOG_CREATE_ACTION_ENDPOINT_CALLED = "Create system action endpoint called";
    public static final String LOG_GET_HIERARCHY_ENDPOINT_CALLED = "Get hierarchical permissions endpoint called";
    public static final String LOG_GET_AVAILABLE_MODULES_ENDPOINT_CALLED = "Get available modules endpoint called";
    public static final String LOG_GET_AVAILABLE_ROLES_ENDPOINT_CALLED = "Get available roles endpoint called";
    public static final String LOG_MODULE_CREATED_SUCCESSFULLY = "System module created successfully";
    public static final String LOG_PAGE_CREATED_SUCCESSFULLY = "System page created successfully";
    public static final String LOG_ACTION_CREATED_SUCCESSFULLY = "System action created successfully";
    public static final String LOG_HIERARCHY_RETRIEVED_SUCCESSFULLY = "Hierarchical permissions retrieved successfully";
    public static final String LOG_AVAILABLE_MODULES_RETRIEVED_SUCCESSFULLY = "Available modules retrieved successfully";
    public static final String LOG_AVAILABLE_ROLES_RETRIEVED_SUCCESSFULLY = "Available roles retrieved successfully";
    
    // Granular Permission Controller - Error Level Logs
    public static final String LOG_MODULE_CREATION_FAILED_ERROR = "Failed to create system module";
    public static final String LOG_PAGE_CREATION_FAILED_ERROR = "Failed to create system page";
    public static final String LOG_ACTION_CREATION_FAILED_ERROR = "Failed to create system action";
    public static final String LOG_HIERARCHY_RETRIEVAL_FAILED = "Failed to retrieve hierarchical permissions";
    public static final String LOG_AVAILABLE_MODULES_RETRIEVAL_FAILED = "Failed to retrieve available modules";
    public static final String LOG_AVAILABLE_ROLES_RETRIEVAL_FAILED = "Failed to retrieve available roles";
    
    // Granular Permission Controller - Response Messages
    public static final String MSG_INVALID_REQUEST = "Invalid request";
    public static final String MSG_MODULE_CREATION_FAILED = "Module creation failed";
    public static final String MSG_PAGE_CREATION_FAILED = "Page creation failed";
    public static final String MSG_ACTION_CREATION_FAILED = "Action creation failed";
    public static final String MSG_HIERARCHY_RETRIEVAL_FAILED = "Failed to retrieve hierarchical permissions";
    public static final String MSG_AVAILABLE_MODULES_RETRIEVAL_FAILED = "Failed to retrieve available modules";
    public static final String MSG_AVAILABLE_ROLES_RETRIEVAL_FAILED = "Failed to retrieve available roles";
    
    // Granular Permission Controller - Detail Keys
    public static final String DETAIL_KEY_PAGE_PATH = "pagePath";
    public static final String DETAIL_KEY_MODULE_COUNT = "moduleCount";
    public static final String DETAIL_KEY_AVAILABLE_MODULE_COUNT = "availableModuleCount";
    public static final String DETAIL_KEY_AVAILABLE_ROLE_COUNT = "availableRoleCount";
    
    // ==================== Notification Kafka Service Constants ====================
    
    // Notification Kafka Service - Logger Names
    public static final String NOTIFICATION_KAFKA_SERVICE_LOGGER_NAME = "NotificationKafkaService";
    
    // Notification Kafka Service - Log Categories
    public static final String LOG_CATEGORY_NOTIFICATION_KAFKA = "notification-kafka";
    
    // Notification Kafka Service - Log Messages
    public static final String LOG_SENDING_NOTIFICATION_EVENT_TO_KAFKA = "Sending notification event to Kafka";
    public static final String LOG_NOTIFICATION_EVENT_SENT_SUCCESSFULLY = "Notification event sent successfully";
    public static final String LOG_FAILED_TO_SEND_NOTIFICATION_EVENT_TO_KAFKA = "Failed to send notification event to Kafka";
    
    // Notification Kafka Service - Detail Keys
    // Note: DETAIL_KEY_OPERATION_TYPE already defined at line 1781
    // Note: DETAIL_KEY_USER_COUNT already defined at line 609
    public static final String DETAIL_KEY_TOPIC = "topic";
    
    // Notification Kafka Service - Object Types
    public static final String LOG_OBJECT_TYPE_NOTIFICATION_KAFKA_EVENT = "notification-kafka-event";
    
    // ==================== Copy Role Audit Constants ====================
    
    // Copy Role Audit Event
    public static final String AUDIT_EVENT_COPY_ROLE = "copy";
    
    // Copy Role Audit Log Messages
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_ROLE_COPY = "Audit aspect triggered for role copy";
    public static final String AUDIT_LOG_AUDITING_ROLE_COPY = "Auditing role copy";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_ROLE_COPY = "Successfully published audit event for role copy";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_ROLE_COPY = "Failed to publish audit event for role copy";
    
    // Copy Role Detail Keys
    public static final String DETAIL_KEY_SOURCE_ROLE_ID = "sourceRoleId";
    public static final String DETAIL_KEY_NEW_ROLE_ID = "newRoleId";
    public static final String DETAIL_KEY_SOURCE_ROLE_NAME = "sourceRoleName";
    public static final String DETAIL_KEY_NEW_ROLE_NAME = "newRoleName";
    public static final String DETAIL_KEY_OWNERS_COPIED = "ownersCopied";
    public static final String DETAIL_KEY_SOURCE_ROLE_TYPE = "sourceRoleType";
    public static final String DETAIL_KEY_COPIED_ROLE_TYPE = "copiedRoleType";
    
    // ==================== ROLE COPY CONSTANTS ====================
    
    // Role Copy Log Messages
    public static final String LOG_ROLE_COPY_REQUESTED = "Role copy requested";
    public static final String LOG_NEW_ROLE_CREATED_FROM_COPY = "New role created from copy";
    public static final String LOG_ROLE_OWNERS_COPIED = "Role owners copied";
    public static final String LOG_ROLE_COPIED_SUCCESSFULLY = "Role copied successfully";
    public static final String LOG_ROLE_COPY_FAILED = "Role copy failed";
    public static final String LOG_STARTING_PERMISSION_COPY = "Starting to copy permissions for role";
    public static final String LOG_PERMISSION_COPIED = "Created new permission copy";
    public static final String LOG_CONDITIONAL_RULES_COPIED = "Copied conditional rules for permission";
    public static final String LOG_ALL_PERMISSIONS_COPIED = "Successfully copied all permissions and conditional rules";
    public static final String LOG_ROLE_TYPE_ADJUSTED_FOR_COPY = "Role type adjusted for copy (SYSTEM roles copied as VIEWER)";
    
    // Role Copy Error Messages
    public static final String ROLE_COPY_FAILED = "Failed to copy role";
    
    // Role Copy Detail Keys
    public static final String DETAIL_KEY_PERMISSION_COUNT = "permissionCount";
    public static final String DETAIL_KEY_SOURCE_PERMISSION_ID = "sourcePermissionId";
    public static final String DETAIL_KEY_NEW_PERMISSION_ID = "newPermissionId";
    public static final String DETAIL_KEY_CONDITIONAL_RULE_COUNT = "conditionalRuleCount";
    public static final String DETAIL_KEY_PERMISSIONS_COPIED = "permissionsCopied";
    
    // ==================== ROLE STATS CONSTANTS ====================
    
    // Role Stats Endpoints
    public static final String STATS_BASE_PATH = "/api/v1/roles/stats";
    
    // Role Stats Log Messages
    public static final String LOG_FETCHING_ROLE_STATS = "Fetching role statistics for tenant";
    public static final String LOG_ROLE_STATS_RETRIEVED = "Role statistics retrieved successfully";
    public static final String LOG_ROLE_STATS_FAILED = "Failed to retrieve role statistics";
    
    // Role Stats Success Messages
    public static final String ROLE_STATS_RETRIEVED_SUCCESS = "Role statistics retrieved successfully";
    
    // Role Stats Error Messages
    public static final String ROLE_STATS_RETRIEVAL_FAILED = "Failed to retrieve role statistics";
    
    // Role Stats Field Names
    public static final String STATS_FIELD_INACTIVE = "inactive";
    public static final String STATS_FIELD_ACTIVE = "active";
    public static final String STATS_FIELD_HIGH_RISK = "highRisk";
    
    // ======================================================
    // Role Reconciliation Audit Event Constants
    // ======================================================
    public static final String EVENT_ROLE_RECONCILIATION = "role reconciliation";
    public static final String LOG_ROLE_RECONCILIATION_AUDIT_CREATED = "Role reconciliation audit event created successfully";
    public static final String LOG_FAILED_TO_CREATE_ROLE_RECONCILIATION_AUDIT = "Failed to create role reconciliation audit event";
    public static final String ROLE_RECONCILIATION_SUCCESS_MESSAGE = "Role reconciliation completed via global dynamic rule";
    
    // ======================================================
    // Bulk Role Deletion Constants
    // ======================================================
    public static final String LOG_BULK_ROLE_DELETION_ENDPOINT_CALLED = "Bulk role deletion endpoint called";
    public static final String LOG_BULK_ROLE_DELETION_STARTED = "Starting bulk role deletion";
    public static final String LOG_BULK_ROLE_DELETION_SUCCESSFUL = "Bulk role deletion completed successfully";
    public static final String LOG_BULK_ROLE_DELETION_FAILED = "Bulk role deletion failed";
    public static final String LOG_PROCESSING_ROLE_DELETION = "Processing role deletion";
    public static final String LOG_ROLE_DELETED_SUCCESSFULLY = "Role deleted successfully";
    public static final String LOG_ROLE_DELETION_FAILED = "Role deletion failed";
    public static final String BULK_ROLE_DELETION_SUCCESS = "Successfully deleted roles";
    public static final String BULK_ROLE_DELETION_PARTIAL_SUCCESS = "Partially deleted roles - some failed";
    public static final String BULK_ROLE_DELETION_FAILED = "Failed to delete roles";
    public static final String ERROR_NO_ROLE_IDS_PROVIDED = "No role IDs provided for bulk deletion";
    public static final String ERROR_BULK_DELETION_EXCEPTION = "An error occurred during bulk role deletion";
    
    // ==================== Bulk Role Deletion Audit Constants ====================
    
    // Bulk Role Deletion Audit Event
    public static final String AUDIT_EVENT_BULK_ROLE_DELETE = "bulk role delete";
    
    // Bulk Role Deletion Audit Log Messages
    public static final String AUDIT_LOG_AUDITING_BULK_ROLE_DELETE = "Auditing bulk role deletion";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_BULK_ROLE_DELETE = "Successfully published audit event for bulk role deletion";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_BULK_ROLE_DELETE = "Failed to publish audit event for bulk role deletion";
    
    // Bulk Role Deletion Detail Keys
    public static final String DETAIL_KEY_DELETED_ROLE_IDS = "deletedRoleIds";
    public static final String DETAIL_KEY_DELETED_ROLE_NAMES = "deletedRoleNames";
    public static final String DETAIL_KEY_SUCCESSFUL_DELETIONS = "successfulDeletions";
    public static final String DETAIL_KEY_FAILED_DELETIONS = "failedDeletions";
    
    // ==================== Role Assignment Type Constants ====================
    
    // Database Field Names
    public static final String FIELD_HAS_MANUAL_ASSIGNMENTS = "hasManualAssignments";
    public static final String FIELD_HAS_DYNAMIC_RULES = "hasDynamicRules";
    
    // Role Assignment Type Messages
    public static final String LOG_UPDATING_ROLE_MANUAL_ASSIGNMENT_FLAG = "Updating role manual assignment flag";
    public static final String LOG_UPDATING_ROLE_DYNAMIC_RULES_FLAG = "Updating role dynamic rules flag";
    public static final String LOG_ROLE_MANUAL_ASSIGNMENT_FLAG_UPDATED = "Role manual assignment flag updated successfully";
    public static final String LOG_ROLE_DYNAMIC_RULES_FLAG_UPDATED = "Role dynamic rules flag updated successfully";
    public static final String ERROR_FAILED_TO_UPDATE_ASSIGNMENT_FLAGS = "Failed to update role assignment flags";
    
    // ==================== Change Data Capture (CDC) Constants ====================
    
    // CDC Configuration
    public static final String CDC_CHANNEL_USER_CHANGES = "user_changes";
    public static final String CDC_ENABLED_PROPERTY = "postgres.cdc.enabled";
    public static final String CDC_DEFAULT_ENABLED = "true";
    
    // CDC Log Categories
    public static final String LOG_CATEGORY_CDC = "cdc";
    public static final String LOG_CATEGORY_CDC_LISTENER = "cdc-listener";
    public static final String LOG_CATEGORY_CDC_RECONCILIATION = "cdc-reconciliation";
    
    // CDC Log Messages - Connection
    public static final String LOG_CDC_LISTENER_STARTING = "Starting PostgreSQL CDC listener";
    public static final String LOG_CDC_LISTENER_STARTED = "PostgreSQL CDC listener started successfully";
    public static final String LOG_CDC_LISTENER_STOPPED = "PostgreSQL CDC listener stopped";
    public static final String LOG_CDC_CONNECTION_ESTABLISHED = "CDC connection established to PostgreSQL";
    public static final String LOG_CDC_CONNECTION_FAILED = "Failed to establish CDC connection";
    public static final String LOG_CDC_LISTENING_TO_CHANNEL = "Listening to PostgreSQL channel";
    public static final String LOG_CDC_LISTENER_SHUTDOWN = "Shutting down CDC listener";
    
    // CDC Log Messages - Notifications
    public static final String LOG_CDC_NOTIFICATION_RECEIVED = "CDC notification received";
    public static final String LOG_CDC_NOTIFICATION_PARSED = "CDC notification parsed successfully";
    public static final String LOG_CDC_NOTIFICATION_PARSE_FAILED = "Failed to parse CDC notification";
    public static final String LOG_CDC_PROCESSING_NOTIFICATION = "Processing CDC notification";
    public static final String LOG_CDC_NOTIFICATION_PROCESSED = "CDC notification processed successfully";
    public static final String LOG_CDC_NOTIFICATION_PROCESSING_FAILED = "Failed to process CDC notification";
    
    // CDC Log Messages - Reconciliation
    public static final String LOG_CDC_TRIGGERING_RECONCILIATION = "CDC triggering dynamic rule reconciliation";
    public static final String LOG_CDC_RECONCILIATION_STARTED = "CDC reconciliation started for user";
    public static final String LOG_CDC_RECONCILIATION_COMPLETED = "CDC reconciliation completed for user";
    public static final String LOG_CDC_RECONCILIATION_FAILED = "CDC reconciliation failed for user";
    public static final String LOG_CDC_SKIPPING_RECONCILIATION_NO_IMMEDIATE_RULES = "Skipping CDC reconciliation - no active IMMEDIATE rules";
    public static final String LOG_CDC_RECONCILIATION_USER_NOT_FOUND = "CDC reconciliation skipped - user not found";
    public static final String LOG_CDC_RECONCILIATION_AUDIT_EVENT_PUBLISHED = "CDC role reconciliation audit event published successfully";
    
    // CDC Detail Keys
    public static final String DETAIL_KEY_CDC_CHANNEL = "cdcChannel";
    public static final String DETAIL_KEY_CDC_OPERATION = "operation";
    public static final String DETAIL_KEY_CDC_TABLE = "table";
    public static final String DETAIL_KEY_CDC_USER_ID = "userId";
    public static final String DETAIL_KEY_CDC_TENANT_ID = "tenantId";
    public static final String DETAIL_KEY_CDC_TIMESTAMP = "timestamp";
    public static final String DETAIL_KEY_CDC_NOTIFICATION = "notification";
    public static final String DETAIL_KEY_CDC_IMMEDIATE_RULES_COUNT = "immediateRulesCount";
    
    // CDC Operations
    public static final String CDC_OPERATION_INSERT = "INSERT";
    public static final String CDC_OPERATION_UPDATE = "UPDATE";
    public static final String CDC_OPERATION_DELETE = "DELETE";
    public static final String CDC_OPERATION_LISTEN = "LISTEN ";
    public static final String CDC_OPERATION_UNLISTEN = "UNLISTEN ";
    
    // CDC Configuration
    public static final int CDC_POLL_INTERVAL_MS = 1000;
    
    // CDC SQL Queries
    public static final String CDC_CREATE_NOTIFICATION_TABLE_SQL = 
        "CREATE TABLE IF NOT EXISTS user_change_notifications (" +
        "id BIGSERIAL PRIMARY KEY, " +
        "payload TEXT NOT NULL, " +
        "processed BOOLEAN DEFAULT FALSE, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    public static final String CDC_QUERY_PENDING_NOTIFICATIONS = 
        "SELECT id, payload FROM user_change_notifications WHERE processed = FALSE ORDER BY id LIMIT 100";
    
    public static final String CDC_UPDATE_NOTIFICATION_PROCESSED = 
        "UPDATE user_change_notifications SET processed = TRUE WHERE id = ?";
    
    // CDC Error Messages
    public static final String ERROR_CDC_LISTENER_INITIALIZATION_FAILED = "Failed to initialize CDC listener";
    public static final String ERROR_CDC_CONNECTION_INTERRUPTED = "CDC connection interrupted";
    public static final String ERROR_CDC_INVALID_NOTIFICATION_FORMAT = "Invalid CDC notification format";
    public static final String ERROR_CDC_RECONCILIATION_EXCEPTION = "Exception during CDC-triggered reconciliation";
    public static final String ERROR_CDC_LISTENER_CLOSE_FAILED = "Failed to close CDC listener connection";
    public static final String ERROR_CDC_POLL_FAILED = "Failed to poll for CDC notifications";
    
    // CDC Detail Keys (additional)
    public static final String DETAIL_KEY_CDC_ERROR = "error";
    public static final String DETAIL_KEY_CDC_RULES_EVALUATED = "rulesEvaluated";
    public static final String DETAIL_KEY_CDC_ROLE_ID = "roleId";
    public static final String DETAIL_KEY_CDC_ROLE_NAME = "roleName";
    public static final String DETAIL_KEY_CDC_RULE_ID = "ruleId";
    public static final String DETAIL_KEY_CDC_RULES_COUNT = "rulesCount";
    
    // CDC Log Messages - Role Assignment
    public static final String LOG_CDC_ROLE_ASSIGNING = "Assigning role via CDC reconciliation";
    public static final String LOG_CDC_ROLE_UNASSIGNING = "Unassigning role via CDC reconciliation";
    public static final String LOG_CDC_ROLE_ASSIGNED = "Role assigned via CDC reconciliation";
    public static final String LOG_CDC_ROLE_UNASSIGNED = "Role unassigned via CDC reconciliation";
    
    // CDC Error Messages (additional)
    public static final String ERROR_CDC_FETCH_USER_DATA_FAILED = "Failed to fetch user data via gRPC";
    public static final String ERROR_CDC_USER_ROLE_CHECK_FAILED = "Failed to check if user has role";
    
    // CDC Notification Messages
    public static final String LOG_CDC_USER_NOT_FOUND_FOR_NOTIFICATION = "User not found for CDC notification";
    public static final String LOG_CDC_ROLE_NOT_FOUND_FOR_NOTIFICATION = "Role not found for CDC notification";
    public static final String LOG_CDC_PREPARING_NOTIFICATION = "Preparing CDC role assignment notification";
    public static final String LOG_CDC_NOTIFICATION_SENT = "CDC role assignment notification sent successfully";
    public static final String ERROR_CDC_NOTIFICATION_FAILED = "Failed to send CDC role assignment notification";
    
    // CDC Audit Event Messages
    public static final String LOG_CDC_AUDIT_EVENT_PUBLISHING = "Publishing CDC role reconciliation audit event";
    public static final String LOG_CDC_AUDIT_EVENT_PUBLISHED = "CDC role reconciliation audit event published successfully";
    public static final String ERROR_CDC_AUDIT_EVENT_FAILED = "Failed to publish CDC role reconciliation audit event";
    public static final String LOG_CDC_AUDIT_FETCH_USER_FALLBACK = "Failed to fetch user details for audit, using fallback";
    
    // ==================== Reconciliation Service Constants ====================
    
    // Reconciliation Log Category
    public static final String LOG_CATEGORY_RECONCILIATION = "reconciliation";
    
    // Reconciliation Log Messages
    public static final String LOG_RECONCILIATION_ALL_USERS_STARTED = "Reconciling all users for tenant";
    public static final String LOG_RECONCILIATION_NO_SCHEDULED_RULES = "No active scheduled rules for tenant";
    public static final String LOG_RECONCILIATION_FOUND_SCHEDULED_RULES = "Found scheduled rules for tenant";
    public static final String LOG_RECONCILIATION_EXECUTING_SCHEDULED_RULE = "Executing scheduled rule for tenant";
    public static final String LOG_RECONCILIATION_SCHEDULED_RULE_COMPLETED = "Scheduled rule execution completed";
    public static final String LOG_RECONCILIATION_COMPLETED_AFTER_DEACTIVATION = "Dynamic rule reconciliation completed after deactivation";
    
    // Reconciliation Error Messages
    public static final String ERROR_RECONCILIATION_SCHEDULED_RULE_FAILED = "Failed to execute scheduled rule";
    
    // Private constructor to prevent instantiation
    private AppConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}