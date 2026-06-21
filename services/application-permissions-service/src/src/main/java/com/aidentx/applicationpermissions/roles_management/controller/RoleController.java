package com.aidentx.applicationpermissions.roles_management.controller;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.exception.DuplicatePermissionException;
import com.aidentx.applicationpermissions.roles_management.exception.PermissionNotFoundException;
import com.aidentx.applicationpermissions.roles_management.model.dto.AssignPermissionToRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitPermissionAssignmentForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitPermissionRevocationForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitBulkPermissionAssignmentForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitBulkPermissionRevocationForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkAssignPermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkAssignmentResult;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkRevokePermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkRevocationResult;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkRoleImportRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkRoleImportResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.CreateRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.UpdateRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.UpdatePermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.RoleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.GetUsersByRoleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.GetRoleMembersResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.GetRoleOwnersResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.PermissionDetailsResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.OwnedRolesResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.RoleStatsDTO;
import com.aidentx.applicationpermissions.roles_management.model.entity.Role;
import com.aidentx.applicationpermissions.roles_management.enums.RoleType;
import com.aidentx.applicationpermissions.roles_management.service.RoleService;
import com.aidentx.applicationpermissions.roles_management.service.ConditionalRuleEvaluationService;
import com.aidentx.applicationpermissions.shared.security.TenantContextService;
import com.aidentx.applicationpermissions.shared.security.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for role management operations.
 * 
 * <p>Provides endpoints for creating, updating, retrieving, and deleting roles
 * with automatic tenant isolation based on JWT token context.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping(AppConstants.ROLES_BASE_PATH)
@Tag(name = "Role Management", description = "API endpoints for managing application roles with multi-tenant support")
@SecurityRequirement(name = "Bearer Authentication")
public class RoleController {

    private final RoleService roleService;
    private final TenantContextService tenantContextService;
    private final JwtUtil jwtUtil;
    private final ConditionalRuleEvaluationService conditionalRuleEvaluationService;
    private final com.aidentx.applicationpermissions.roles_management.repository.RoleRepository roleRepository;
    private final IUnifiedLogger logger;

    /**
     * Constructor for RoleController.
     * 
     * @param roleService service for role operations
     * @param tenantContextService service for extracting tenant context
     * @param jwtUtil utility for JWT operations
     * @param conditionalRuleEvaluationService service for validating conditional rules
     * @param loggerFactory factory for creating unified loggers
     */
    @Autowired
    public RoleController(RoleService roleService,
                         TenantContextService tenantContextService,
                         JwtUtil jwtUtil,
                         ConditionalRuleEvaluationService conditionalRuleEvaluationService,
                         com.aidentx.applicationpermissions.roles_management.repository.RoleRepository roleRepository,
                         UnifiedLoggerFactory loggerFactory) {
        this.roleService = roleService;
        this.tenantContextService = tenantContextService;
        this.jwtUtil = jwtUtil;
        this.conditionalRuleEvaluationService = conditionalRuleEvaluationService;
        this.roleRepository = roleRepository;
        this.logger = loggerFactory.getLogger(AppConstants.ROLE_CONTROLLER_LOGGER_NAME, AppConstants.LOG_CATEGORY_ROLE_MANAGEMENT);
    }

    /**
     * Creates a new role for the current tenant.
     * 
     * @param request the role creation request
     * @return the created role response with success message
     */
    @Operation(
        summary = "Create a new role",
        description = "Creates a new role for the current tenant based on JWT token context",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Role creation request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateRoleRequest.class),
                examples = @ExampleObject(
                    name = "Administrator Role",
                    summary = "Example administrator role creation",
                    value = """
                        {
                            "role": "ADMINISTRATOR",
                            "roleName": "System Administrator",
                            "description": "Full system access and management capabilities",
                            "businessJustification": "Required for system maintenance, user management, and critical operations oversight",
                            "requireManagerApproval": true,
                            "tags": ["admin", "system", "critical", "management"],
                            "riskLevel": "High"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Role created successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                            "message": "Role created successfully",
                            "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                "tenantId": "tenant_001",
                                "role": "ADMINISTRATOR",
                                "roleName": "System Administrator",
                                "description": "Full system access and management capabilities",
                                "businessJustification": "Required for system maintenance, user management, and critical operations oversight",
                                "requireManagerApproval": true,
                                "tags": ["admin", "system", "critical", "management"],
                                "userCount": 0,
                                "permissionsCount": 0,
                                "riskLevel": "High",
                                "createdAt": 1696723200000,
                                "updatedAt": 1696723200000,
                                "createdByName": "admin@company.com",
                                "createdById": "550e8400-e29b-41d4-a716-446655440001"
                            },
                            "status": "201"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRole(
        @Parameter(description = "Role creation request", required = true)
        @Valid @RequestBody CreateRoleRequest request) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "createRole")
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
            
        logger.trace()
            .message(AppConstants.LOG_TRACE_PARAMETER_VALUES)
            .detail("roleName", request.getRoleName())
            .detail("roleType", request.getRoleType().name())
            .detail("description", request.getDescription())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
        
        try {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_STARTING_ROLE_CREATION)
                .detail("roleName", request.getRoleName())
                .detail("roleType", request.getRoleType().name())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_CREATION_ENDPOINT_CALLED)
                .detail("tenantId", tenantId)
                .detail("roleName", request.getRoleName())
                .detail("roleType", request.getRoleType().name())
                .detail("endpoint", "POST " + AppConstants.ROLES_BASE_PATH)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            RoleResponse roleResponse = roleService.createRole(request);
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_BUILDING_RESPONSE)
                .detail("roleId", roleResponse.getId().toString())
                .detail("roleName", roleResponse.getRoleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            // Determine message based on whether it was submitted for approval or created directly
            boolean isSubmitForApproval = request.getSubmitForApproval() != null && request.getSubmitForApproval();
            String message = isSubmitForApproval 
                ? "Role submitted for approval workflow successfully. The role will be created after approval."
                : AppConstants.ROLE_CREATED_SUCCESS;
            
            String logMessage = isSubmitForApproval 
                ? "Role submitted for approval workflow" 
                : AppConstants.LOG_ROLE_CREATION_SUCCESSFUL;
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", message);
            response.put("data", roleResponse);
            response.put("status", AppConstants.HTTP_201_CREATED);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_RESPONSE_PREPARED)
                .detail("responseStatus", HttpStatus.CREATED.value())
                .detail("responseMessage", AppConstants.ROLE_CREATED_SUCCESS)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            logger.info()
                .message(logMessage)
                .detail("tenantId", tenantId)
                .detail("roleId", roleResponse.getId().toString())
                .detail("roleName", roleResponse.getRoleName())
                .detail("submitForApproval", isSubmitForApproval)
                .detail("responseStatus", HttpStatus.CREATED.value())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "createRole")
                .detail("success", true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            // Handle validation errors (like duplicate role name) without stack trace
            logger.warn()
                .message(e.getMessage())
                .detail("roleName", request.getRoleName())
                .detail("roleType", request.getRoleType().name())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", AppConstants.HTTP_400_BAD_REQUEST);
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_ANALYSIS)
                .detail("exceptionType", e.getClass().getSimpleName())
                .detail("exceptionMessage", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            logger.error(e)
                .message(AppConstants.LOG_ROLE_CREATION_FAILED_CONTROLLER)
                .detail("roleName", request.getRoleName())
                .detail("roleType", request.getRoleType().name())
                .detail("endpoint", "POST " + AppConstants.ROLES_BASE_PATH)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                .detail("errorMessage", AppConstants.ROLE_CREATION_FAILED)
                .detail("httpStatus", HttpStatus.INTERNAL_SERVER_ERROR.value())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", AppConstants.ROLE_CREATION_FAILED);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", AppConstants.HTTP_500_INTERNAL_ERROR);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "createRole")
                .detail("success", false)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/import")
    @Operation(summary = "Bulk import roles from JSON", description = "Import multiple roles at once. Accepts up to 100 roles per request.")
    @ApiResponse(responseCode = "200", description = "Bulk import completed (check response for individual successes/failures)")
    @ApiResponse(responseCode = "400", description = "Invalid request or validation errors")
    @ApiResponse(responseCode = "500", description = "Internal server error during bulk import")
    public ResponseEntity<Map<String, Object>> bulkImportRoles(
        @Parameter(description = "Bulk role import request containing array of roles", required = true)
        @Valid @RequestBody BulkRoleImportRequest request) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "bulkImportRoles")
            .detail("roleCount", request.getRoles().size())
            .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_ROLE_IMPORT_REQUEST)
            .log();
        
        try {
            logger.info()
                .message(AppConstants.LOG_BULK_ROLE_IMPORT_ENDPOINT_CALLED)
                .detail("roleCount", request.getRoles().size())
                .detail("endpoint", "POST " + AppConstants.ROLES_BASE_PATH + "/import")
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_ROLE_IMPORT_REQUEST)
                .log();
                
            String tenantId = tenantContextService.getCurrentTenantId();
            
            List<RoleResponse> successfulImports = new ArrayList<>();
            List<BulkRoleImportResponse.RoleImportError> failedImports = new ArrayList<>();
            
            for (int i = 0; i < request.getRoles().size(); i++) {
                CreateRoleRequest roleRequest = request.getRoles().get(i);
                try {
                    logger.debug()
                        .message(AppConstants.LOG_PROCESSING_ROLE_IMPORT)
                        .detail("index", i)
                        .detail("roleName", roleRequest.getRoleName())
                        .detail("roleType", roleRequest.getRoleType().name())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_IMPORT_ITEM)
                        .log();
                    
                    RoleResponse roleResponse = roleService.createRole(roleRequest);
                    successfulImports.add(roleResponse);
                    
                    logger.debug()
                        .message(AppConstants.LOG_ROLE_IMPORTED_SUCCESSFULLY)
                        .detail("index", i)
                        .detail("roleId", roleResponse.getId().toString())
                        .detail("roleName", roleResponse.getRoleName())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_IMPORT_ITEM)
                        .log();
                        
                } catch (Exception e) {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : AppConstants.UNKNOWN_ERROR_DURING_ROLE_IMPORT;
                    failedImports.add(new BulkRoleImportResponse.RoleImportError(
                        roleRequest.getRoleName(),
                        errorMessage,
                        i
                    ));
                    
                    logger.warn()
                        .message(AppConstants.LOG_FAILED_TO_IMPORT_ROLE)
                        .detail("index", i)
                        .detail("roleName", roleRequest.getRoleName())
                        .detail("error", errorMessage)
                        .detail("exceptionType", e.getClass().getSimpleName())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_IMPORT_ITEM)
                        .log();
                }
            }
            
            BulkRoleImportResponse.ImportSummary summary = new BulkRoleImportResponse.ImportSummary(
                request.getRoles().size(),
                successfulImports.size(),
                failedImports.size()
            );
            
            BulkRoleImportResponse importResponse = new BulkRoleImportResponse(
                successfulImports,
                failedImports,
                summary
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", String.format(AppConstants.BULK_ROLE_IMPORT_SUCCESS_MESSAGE, 
                summary.successCount(), summary.failureCount()));
            response.put("data", importResponse);
            response.put("status", 200);
            
            logger.info()
                .message(AppConstants.LOG_BULK_ROLE_IMPORT_COMPLETED)
                .detail("tenantId", tenantId)
                .detail("totalRoles", summary.totalRoles())
                .detail("successCount", summary.successCount())
                .detail("failureCount", summary.failureCount())
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_ROLE_IMPORT_RESPONSE)
                .log();
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "bulkImportRoles")
                .detail("success", true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_ROLE_IMPORT_RESPONSE)
                .log();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_BULK_ROLE_IMPORT_FAILED_CONTROLLER)
                .detail("endpoint", "POST " + AppConstants.ROLES_BASE_PATH + "/import")
                .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_ROLE_IMPORT_REQUEST)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", AppConstants.BULK_ROLE_IMPORT_FAILED);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", 500);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "bulkImportRoles")
                .detail("success", false)
                .objectType("BULK_ROLE_IMPORT_REQUEST")
                .log();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Updates an existing role for the current tenant.
     * 
     * @param id the role ID to update
     * @param request the role update request
     * @return the updated role response with success message
     */
    @Operation(
        summary = "Update an existing role",
        description = "Updates an existing role for the current tenant. Only roles belonging to the authenticated tenant can be updated.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Role update request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UpdateRoleRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Update Role Without Owners",
                        summary = "Update role without changing owners",
                        value = """
                            {
                                "roleType": "REVIEWER",
                                "roleName": "Senior Reviewer",
                                "description": "Review and approve system changes",
                                "businessJustification": "Promotion to senior role requires expanded permissions for team management",
                                "requireOwnerApproval": true,
                                "tags": ["reviewer", "senior", "team-lead", "quality"],
                                "riskLevel": "Medium"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Update Role With New Owners",
                        summary = "Update role and replace all owners with new ones",
                        value = """
                            {
                                "roleType": "ADMINISTRATOR",
                                "roleName": "System Administrator with owners",
                                "description": "Full system access with administrative privileges",
                                "businessJustification": "Required for system maintenance, user management, and critical operations oversight",
                                "requireOwnerApproval": true,
                                "tags": ["admin", "system", "critical", "management"],
                                "riskLevel": "High",
                                "ownerIds": ["2d1c4b5a-8e9f-3762-a154-c8b7d6e5f4a3", "4bb2c7df-5677-4cd2-a0d1-aad3fa8afe8f"]
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role updated successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                            "message": "Role updated successfully",
                            "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                "tenantId": "tenant_001",
                                "role": "REVIEWER",
                                "roleName": "Senior Reviewer",
                                "description": "Review and approve system changes",
                                "businessJustification": "Promotion to senior role requires expanded permissions for team management",
                                "requireManagerApproval": true,
                                "tags": ["reviewer", "senior", "team-lead", "quality"],
                                "userCount": 3,
                                "permissionsCount": 8,
                                "riskLevel": "Medium",
                                "createdAt": 1696723100000,
                                "updatedAt": 1696809600000,
                                "createdByName": "manager@company.com",
                                "createdById": "550e8400-e29b-41d4-a716-446655440002"
                            },
                            "status": "200"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Access denied to this role",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Role not found",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    @PutMapping(AppConstants.ROLE_ID_PATH_VAR)
    public ResponseEntity<Map<String, Object>> updateRole(
        @Parameter(description = "Role ID to update", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID id, 
        @Parameter(description = "Role update request", required = true)
        @Valid @RequestBody UpdateRoleRequest request) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "updateRole")
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
            
        logger.trace()
            .message(AppConstants.LOG_TRACE_PARAMETER_VALUES)
            .detail("roleId", id.toString())
            .detail("roleName", request.getRoleName())
            .detail("roleType", request.getRoleType().name())
            .detail("description", request.getDescription())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
        
        try {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_ROLE_VALIDATION_STARTED)
                .detail("roleId", id.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_UPDATE_ENDPOINT_CALLED)
                .detail("tenantId", tenantId)
                .detail("roleId", id.toString())
                .detail("roleName", request.getRoleName())
                .detail("roleType", request.getRoleType().name())
                .detail("endpoint", "PUT " + AppConstants.ROLES_BASE_PATH + AppConstants.ROLE_ID_PATH_VAR)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            RoleResponse roleResponse = roleService.updateRole(id, request);
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_BUILDING_RESPONSE)
                .detail("roleId", roleResponse.getId().toString())
                .detail("roleName", roleResponse.getRoleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", AppConstants.ROLE_UPDATED_SUCCESS);
            response.put("data", roleResponse);
            response.put("status", AppConstants.HTTP_200_OK);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_RESPONSE_PREPARED)
                .detail("responseStatus", HttpStatus.OK.value())
                .detail("responseMessage", AppConstants.ROLE_UPDATED_SUCCESS)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_UPDATE_SUCCESSFUL)
                .detail("tenantId", tenantId)
                .detail("roleId", roleResponse.getId().toString())
                .detail("roleName", roleResponse.getRoleName())
                .detail("responseStatus", HttpStatus.OK.value())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "updateRole")
                .detail("success", true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn()
                .message(e.getMessage())
                .detail("roleId", id.toString())
                .detail("roleName", request.getRoleName())
                .detail("roleType", request.getRoleType().name())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", AppConstants.HTTP_400_BAD_REQUEST);
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_ANALYSIS)
                .detail("exceptionType", e.getClass().getSimpleName())
                .detail("exceptionMessage", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            logger.error(e)
                .message(AppConstants.LOG_ROLE_UPDATE_FAILED_CONTROLLER)
                .detail("roleId", id.toString())
                .detail("roleName", request.getRoleName())
                .detail("endpoint", "PUT " + AppConstants.ROLES_BASE_PATH + AppConstants.ROLE_ID_PATH_VAR)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            if (e.getMessage().contains("not found")) {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail("errorType", "NOT_FOUND")
                    .detail("httpStatus", HttpStatus.NOT_FOUND.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put("message", AppConstants.ROLE_NOT_FOUND);
                errorResponse.put("status", AppConstants.HTTP_404_NOT_FOUND);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else if (e.getMessage().contains("Access denied")) {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail("errorType", "FORBIDDEN")
                    .detail("httpStatus", HttpStatus.FORBIDDEN.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put("message", AppConstants.ROLE_ACCESS_DENIED);
                errorResponse.put("status", AppConstants.HTTP_403_FORBIDDEN);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            } else {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail("errorType", "INTERNAL_SERVER_ERROR")
                    .detail("httpStatus", HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put("message", AppConstants.ROLE_UPDATE_FAILED);
                errorResponse.put("error", e.getMessage());
                errorResponse.put("status", AppConstants.HTTP_500_INTERNAL_ERROR);
                
                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail("method", "updateRole")
                    .detail("success", false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                    
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
    }

    /**
     * Activates a role after workflow approval completion.
     * 
     * <p>This endpoint is called by the workflow service after successful approval
     * to activate a role that was created with requireOwnerApproval=true.</p>
     * 
     * @param id the role ID to activate
     * @return success response
     */
    @Operation(
        summary = "Activate a role",
        description = "Activates a role after workflow approval completion. Typically called by workflow service."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role activated successfully",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Role not found",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(mediaType = "application/json")
        )
    })
    @PutMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateRole(
        @Parameter(description = "Role ID to activate", required = true)
        @PathVariable UUID id) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "activateRole")
            .detail("roleId", id.toString())
            .objectType("ROLE_ACTIVATION")
            .log();
        
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message("Role activation endpoint called")
                .detail("tenantId", tenantId)
                .detail("roleId", id.toString())
                .detail("endpoint", "PUT " + AppConstants.ROLES_BASE_PATH + "/{id}/activate")
                .objectType("ROLE_ACTIVATION")
                .log();

            // Get role and verify tenant ownership
            com.aidentx.applicationpermissions.roles_management.model.entity.Role role = 
                roleRepository.findById(id).orElse(null);
            
            if (role == null || !role.getTenantId().equals(tenantId)) {
                logger.warn()
                    .message("Role not found for activation or access denied")
                    .detail("roleId", id.toString())
                    .detail("tenantId", tenantId)
                    .objectType("ROLE_ACTIVATION")
                    .log();
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Role not found");
                errorResponse.put("status", AppConstants.HTTP_404_NOT_FOUND);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            // Activate role
            role.setIsActive(true);
            roleRepository.save(role);
            
            logger.info()
                .message("Role activated successfully")
                .detail("tenantId", tenantId)
                .detail("roleId", id.toString())
                .detail("roleName", role.getRoleName())
                .objectType("ROLE_ACTIVATION")
                .log();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Role activated successfully");
            response.put("roleId", id.toString());
            response.put("roleName", role.getRoleName());
            response.put("status", AppConstants.HTTP_200_OK);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "activateRole")
                .detail("success", true)
                .objectType("ROLE_ACTIVATION")
                .log();

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to activate role")
                .detail("roleId", id.toString())
                .detail("error", e.getMessage())
                .objectType("ROLE_ACTIVATION")
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to activate role: " + e.getMessage());
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", AppConstants.HTTP_500_INTERNAL_ERROR);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "activateRole")
                .detail("success", false)
                .objectType("ROLE_ACTIVATION")
                .log();
                
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves a role by ID for the current tenant.
     * 
     * @param id the role ID to retrieve
     * @return the role response with success message
     */
    @Operation(
        summary = "Get role by ID",
        description = "Retrieves a specific role by its ID for the current tenant. Only roles belonging to the authenticated tenant can be accessed."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                            "message": "Role retrieved successfully",
                            "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                "tenantId": "tenant_001",
                                "role": "ADMINISTRATOR",
                                "roleName": "System Administrator",
                                "description": "Full system access and management capabilities",
                                "businessJustification": "Required for system maintenance, user management, and critical operations oversight",
                                "requireManagerApproval": true,
                                "tags": ["admin", "system", "critical", "management"],
                                "userCount": 1,
                                "permissionsCount": 15,
                                "riskLevel": "High",
                                "createdAt": 1696723200000,
                                "updatedAt": 1696723200000,
                                "createdByName": "admin@company.com",
                                "createdById": "550e8400-e29b-41d4-a716-446655440001"
                            },
                            "status": "200"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Access denied to this role",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Role not found",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping(AppConstants.ROLE_ID_PATH_VAR)
    public ResponseEntity<Map<String, Object>> getRoleById(
        @Parameter(description = "Role ID to retrieve", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID id) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "getRoleById")
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
            
        logger.trace()
            .message(AppConstants.LOG_TRACE_PARAMETER_VALUES)
            .detail("roleId", id.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
        
        try {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_ROLE_VALIDATION_STARTED)
                .detail("roleId", id.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_RETRIEVAL_ENDPOINT_CALLED)
                .detail("tenantId", tenantId)
                .detail("roleId", id.toString())
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH + AppConstants.ROLE_ID_PATH_VAR)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            RoleResponse roleResponse = roleService.getRoleById(id);
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_ROLE_FOUND_IN_DATABASE)
                .detail("roleId", roleResponse.getId().toString())
                .detail("roleName", roleResponse.getRoleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_BUILDING_RESPONSE)
                .detail("roleId", roleResponse.getId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", AppConstants.ROLE_RETRIEVED_SUCCESS);
            response.put("data", roleResponse);
            response.put("status", AppConstants.HTTP_200_OK);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_RESPONSE_PREPARED)
                .detail("responseStatus", HttpStatus.OK.value())
                .detail("responseMessage", AppConstants.ROLE_RETRIEVED_SUCCESS)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_RETRIEVAL_SUCCESSFUL)
                .detail("tenantId", tenantId)
                .detail("roleId", roleResponse.getId().toString())
                .detail("roleName", roleResponse.getRoleName())
                .detail("responseStatus", HttpStatus.OK.value())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "getRoleById")
                .detail("success", true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_ANALYSIS)
                .detail("exceptionType", e.getClass().getSimpleName())
                .detail("exceptionMessage", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            logger.error(e)
                .message(AppConstants.LOG_ROLE_RETRIEVAL_FAILED_CONTROLLER)
                .detail("roleId", id.toString())
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH + AppConstants.ROLE_ID_PATH_VAR)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            if (e.getMessage().contains("not found")) {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail("errorType", "NOT_FOUND")
                    .detail("httpStatus", HttpStatus.NOT_FOUND.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put("message", AppConstants.ROLE_NOT_FOUND);
                errorResponse.put("status", AppConstants.HTTP_404_NOT_FOUND);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else if (e.getMessage().contains("Access denied")) {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail("errorType", "FORBIDDEN")
                    .detail("httpStatus", HttpStatus.FORBIDDEN.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put("message", AppConstants.ROLE_ACCESS_DENIED);
                errorResponse.put("status", AppConstants.HTTP_403_FORBIDDEN);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            } else {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail("errorType", "INTERNAL_SERVER_ERROR")
                    .detail("httpStatus", HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put("message", AppConstants.ROLE_RETRIEVAL_FAILED);
                errorResponse.put("error", e.getMessage());
                errorResponse.put("status", AppConstants.HTTP_500_INTERNAL_ERROR);
                
                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail("method", "getRoleById")
                    .detail("success", false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                    
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
    }

    /**
     * Retrieves all roles for the current tenant.
     * 
     * @return list of role responses for the tenant
     */
    @Operation(
        summary = "Get all roles",
        description = "Retrieves all roles for the current tenant based on JWT token context"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Roles retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                            "message": "Roles retrieved successfully",
                            "data": [
                                {
                                    "id": "550e8400-e29b-41d4-a716-446655440000",
                                    "tenantId": "tenant_001",
                                    "role": "ADMINISTRATOR",
                                    "roleName": "System Administrator",
                                    "description": "Full system access and management capabilities",
                                    "businessJustification": "Required for system maintenance, user management, and critical operations oversight",
                                    "requireManagerApproval": true,
                                    "tags": ["admin", "system", "critical", "management"],
                                    "userCount": 1,
                                    "permissionsCount": 15,
                                    "riskLevel": "High",
                                    "createdAt": 1696723200000,
                                    "updatedAt": 1696723200000,
                                    "createdByName": "admin@company.com",
                                    "createdById": "550e8400-e29b-41d4-a716-446655440001"
                                },
                                {
                                    "id": "550e8400-e29b-41d4-a716-446655440001",
                                    "tenantId": "tenant_001",
                                    "role": "REVIEWER",
                                    "roleName": "Senior Reviewer",
                                    "description": "Review and approve system changes",
                                    "businessJustification": "Promotion to senior role requires expanded permissions for team management",
                                    "requireManagerApproval": true,
                                    "tags": ["reviewer", "senior", "team-lead", "quality"],
                                    "userCount": 0,
                                    "permissionsCount": 8,
                                    "riskLevel": "Medium",
                                    "createdAt": 1696723100000,
                                    "updatedAt": 1696809500000,
                                    "createdByName": "manager@company.com",
                                    "createdById": "550e8400-e29b-41d4-a716-446655440002"
                                }
                            ],
                            "status": "200",
                            "count": 2
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRoles(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "roleType", required = false) String roleType,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "riskLevel", required = false) String riskLevel,
            @RequestParam(value = "tags", required = false) List<String> tags) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "getAllRoles")
            .detail("page", page)
            .detail("size", size)
            .detail("search", search)
            .detail("roleType", roleType)
            .detail("isActive", isActive)
            .detail("riskLevel", riskLevel)
            .detail("tags", tags)
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
        
        try {
            // Validate pagination parameters
            if (page < 0) {
                throw new IllegalArgumentException("Page number must be >= 0");
            }
            if (size < 1 || size > 100) {
                throw new IllegalArgumentException("Page size must be between 1 and 100");
            }

            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLES_LIST_ENDPOINT_CALLED)
                .detail("tenantId", tenantId)
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH)
                .detail("page", page)
                .detail("size", size)
                .detail("search", search)
                .detail("roleType", roleType)
                .detail("isActive", isActive)
                .detail("riskLevel", riskLevel)
                .detail("tags", tags)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            org.springframework.data.domain.Page<RoleResponse> rolesPage = roleService.getAllRolesPaginated(
                page, size, search, roleType, isActive, riskLevel, tags
            );
            List<RoleResponse> roleResponses = rolesPage.getContent();
            
            // Get stats calculated by service (from ALL filtered results, not just current page)
            Map<String, Long> statsFromService = com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.getFilterStats();
            long inactiveCount = statsFromService != null ? statsFromService.getOrDefault(AppConstants.STATS_FIELD_INACTIVE, 0L) : 0L;
            long activeCount = statsFromService != null ? statsFromService.getOrDefault(AppConstants.STATS_FIELD_ACTIVE, 0L) : 0L;
            long highRiskCount = statsFromService != null ? statsFromService.getOrDefault(AppConstants.STATS_FIELD_HIGH_RISK, 0L) : 0L;
            
            // Clear ThreadLocal to prevent memory leaks
            com.aidentx.applicationpermissions.roles_management.service.impl.RoleServiceImpl.clearFilterStats();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_BUILDING_RESPONSE)
                .detail("roleCount", roleResponses.size())
                .detail(AppConstants.STATS_FIELD_INACTIVE, inactiveCount)
                .detail(AppConstants.STATS_FIELD_ACTIVE, activeCount)
                .detail(AppConstants.STATS_FIELD_HIGH_RISK, highRiskCount)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put(AppConstants.STATS_FIELD_INACTIVE, inactiveCount);
            stats.put(AppConstants.STATS_FIELD_ACTIVE, activeCount);
            stats.put(AppConstants.STATS_FIELD_HIGH_RISK, highRiskCount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", AppConstants.ROLES_RETRIEVED_SUCCESS);
            response.put("data", roleResponses);
            response.put("status", AppConstants.HTTP_200_OK);
            response.put("count", roleResponses.size());
            response.put("totalElements", rolesPage.getTotalElements());
            response.put("totalPages", rolesPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("stats", stats);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_RESPONSE_PREPARED)
                .detail("responseStatus", HttpStatus.OK.value())
                .detail("responseMessage", AppConstants.ROLES_RETRIEVED_SUCCESS)
                .detail("roleCount", roleResponses.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            logger.info()
                .message(AppConstants.LOG_ROLES_LIST_RETRIEVAL_SUCCESSFUL)
                .detail("tenantId", tenantId)
                .detail("roleCount", roleResponses.size())
                .detail("responseStatus", HttpStatus.OK.value())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "getAllRoles")
                .detail("success", true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_ANALYSIS)
                .detail("exceptionType", e.getClass().getSimpleName())
                .detail("exceptionMessage", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            logger.error(e)
                .message(AppConstants.LOG_ROLES_LIST_RETRIEVAL_FAILED_CONTROLLER)
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                .detail("errorMessage", AppConstants.ROLE_RETRIEVAL_FAILED)
                .detail("httpStatus", HttpStatus.INTERNAL_SERVER_ERROR.value())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", AppConstants.ROLE_RETRIEVAL_FAILED);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", AppConstants.HTTP_500_INTERNAL_ERROR);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "getAllRoles")
                .detail("success", false)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves all unique tags from roles in the current tenant.
     * 
     * @return response containing unique tags
     */
    @Operation(
        summary = "Get all unique role tags",
        description = "Retrieves all unique tags from all roles in the current tenant for filtering purposes"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tags retrieved successfully",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/tags")
    public ResponseEntity<Map<String, Object>> getAllRoleTags() {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_TAGS_GET_REQUEST)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH + "/tags")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            List<String> tags = roleService.getAllUniqueTags();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_TAGS_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("tagCount", tags.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", AppConstants.ROLE_TAGS_RETRIEVED_SUCCESS);
            response.put("data", tags);
            response.put("status", AppConstants.HTTP_200_OK);
            response.put("count", tags.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ROLE_TAGS_RETRIEVAL_FAILED)
                .detail("exceptionClass", e.getClass().getName())
                .detail("exceptionMessage", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", AppConstants.ROLE_TAGS_RETRIEVAL_FAILED);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", AppConstants.HTTP_500_INTERNAL_ERROR);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves all users in the tenant with their role assignment status for a specific role.
     * 
     * @param roleId the role ID to check assignments for
     * @return response containing all users with hasRole status
     */
    @Operation(
        summary = "Get users by role with pagination, filtering, and search",
        description = "Retrieves users in the tenant with their assignment status for a specific role. " +
                      "Supports pagination (5, 10, 25, 50 per page), status filtering (ALL, ACTIVE, INACTIVE), " +
                      "and search by username, displayName, or email. " +
                      "Users with the role (hasRole=true) are always shown first, followed by users without the role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully with role status",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GetUsersByRoleResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                            "users": [
                                {
                                    "id": "f8e7d6c5-b4a3-9281-7069-5847362514ab",
                                    "username": "youssef.noser",
                                    "email": "youssef.noser@intimec.com",
                                    "firstName": "Youssef",
                                    "lastName": "Noser",
                                    "displayName": "Youssef Noser",
                                    "status": "ACTIVE",
                                    "tenantId": "tenant_001",
                                    "hasRole": true
                                },
                                {
                                    "id": "2d1c4b5a-8e9f-3762-a154-c8b7d6e5f4a3",
                                    "username": "neal.atouani",
                                    "email": "neal.atouani@intimec.com",
                                    "firstName": "Neal",
                                    "lastName": "Atouani",
                                    "displayName": "Neal Atouani",
                                    "status": "ACTIVE",
                                    "tenantId": "tenant_001",
                                    "hasRole": false
                                }
                            ],
                            "totalCount": 2,
                            "usersWithRoleCount": 1,
                            "usersWithoutRoleCount": 1,
                            "roleId": "550e8400-e29b-41d4-a716-446655440000",
                            "tenantId": "tenant_001",
                            "success": true,
                            "message": "Users retrieved successfully with role status"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Access denied to this role",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Role not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Role Not Found",
                    value = """
                        {
                            "roleId": "550e8400-e29b-41d4-a716-446655440000",
                            "tenantId": "tenant_001",
                            "success": false,
                            "message": "Role not found for users retrieval"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/{roleId}/users")
    public ResponseEntity<GetUsersByRoleResponse> getUsersByRole(
        @Parameter(description = "Role ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID roleId,
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") Integer page,
        @Parameter(description = "Page size (5, 10, 25, 50)", example = "10")
        @RequestParam(defaultValue = "10") Integer size,
        @Parameter(description = "Status filter: ALL, ACTIVE, or INACTIVE", example = "ALL")
        @RequestParam(defaultValue = "ALL") String status,
        @Parameter(description = "Search query to filter by username, displayName, or email", example = "john")
        @RequestParam(required = false) String search) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "getUsersByRole")
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
        
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_GET_USERS_BY_ROLE_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH + "/" + roleId + "/users")
                .detail("page", page)
                .detail("size", size)
                .detail("statusFilter", status)
                .detail("search", search)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            GetUsersByRoleResponse response = roleService.getUsersByRole(roleId, page, size, status, search);
            
            if (response.getSuccess()) {
                logger.info()
                    .message(AppConstants.LOG_GET_USERS_BY_ROLE_SUCCESSFUL)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_TOTAL_USERS_COUNT, response.getTotalCount())
                    .detail(AppConstants.DETAIL_KEY_USERS_WITH_ROLE_COUNT, response.getUsersWithRoleCount())
                    .detail(AppConstants.DETAIL_KEY_USERS_WITHOUT_ROLE_COUNT, response.getUsersWithoutRoleCount())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                    .log();

                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail("method", "getUsersByRole")
                    .detail("success", true)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                    .log();

                return ResponseEntity.ok(response);
            } else {
                logger.warn()
                    .message(AppConstants.LOG_GET_USERS_BY_ROLE_FAILED_CONTROLLER)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail("responseMessage", response.getMessage())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail("method", "getUsersByRole")
                    .detail("success", false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.error(e)
                .message(AppConstants.LOG_GET_USERS_BY_ROLE_FAILED_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH + "/" + roleId + "/users")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            GetUsersByRoleResponse errorResponse = new GetUsersByRoleResponse(
                roleId.toString(), 
                tenantId, 
                AppConstants.FAILED_TO_RETRIEVE_USERS_BY_ROLE + ": " + e.getMessage()
            );
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "getUsersByRole")
                .detail("success", false)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves manually assigned users for a specific role.
     * 
     * <p>This endpoint returns only users who have been manually assigned to the role
     * (assignmentType = MANUAL). Dynamic rule-based assignments are excluded.</p>
     * 
     * @param roleId the role ID
     * @param page page number (0-indexed)
     * @param size page size (5, 10, 25, 50)
     * @param status status filter (ALL, ACTIVE, INACTIVE)
     * @param search search query (username, displayName, email)
     * @return response containing manually assigned users only
     */
    @GetMapping("/{roleId}/manual-users")
    public ResponseEntity<GetUsersByRoleResponse> getManuallyAssignedUsersByRole(
        @Parameter(description = "Role ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID roleId,
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") Integer page,
        @Parameter(description = "Page size (5, 10, 25, 50)", example = "10")
        @RequestParam(defaultValue = "10") Integer size,
        @Parameter(description = "Status filter: ALL, ACTIVE, or INACTIVE", example = "ALL")
        @RequestParam(defaultValue = "ALL") String status,
        @Parameter(description = "Search query to filter by username, displayName, or email", example = "john")
        @RequestParam(required = false) String search) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "getManuallyAssignedUsersByRole")
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
        
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_GET_MANUAL_USERS_BY_ROLE_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH + "/" + roleId + "/manual-users")
                .detail("page", page)
                .detail("size", size)
                .detail("statusFilter", status)
                .detail("search", search)
                .detail("assignmentType", "MANUAL")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            GetUsersByRoleResponse response = roleService.getManuallyAssignedUsersByRole(roleId, page, size, status, search);
            
            if (response.getSuccess()) {
                logger.info()
                    .message(AppConstants.LOG_GET_MANUAL_USERS_BY_ROLE_SUCCESSFUL)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_TOTAL_USERS_COUNT, response.getTotalCount())
                    .detail(AppConstants.DETAIL_KEY_USERS_WITH_ROLE_COUNT, response.getUsersWithRoleCount())
                    .detail("assignmentType", "MANUAL")
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                    .log();

                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail("method", "getManuallyAssignedUsersByRole")
                    .detail("success", true)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                    .log();

                return ResponseEntity.ok(response);
            } else {
                logger.warn()
                    .message(AppConstants.LOG_GET_MANUAL_USERS_BY_ROLE_FAILED_CONTROLLER)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail("responseMessage", response.getMessage())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail("method", "getManuallyAssignedUsersByRole")
                    .detail("success", false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.error(e)
                .message(AppConstants.LOG_GET_MANUAL_USERS_BY_ROLE_FAILED_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH + "/" + roleId + "/manual-users")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            GetUsersByRoleResponse errorResponse = new GetUsersByRoleResponse(
                roleId.toString(), 
                tenantId, 
                AppConstants.FAILED_TO_RETRIEVE_MANUAL_USERS + ": " + e.getMessage()
            );
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "getManuallyAssignedUsersByRole")
                .detail("success", false)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves role members with their assignment type information.
     * 
     * <p>This endpoint returns all users assigned to a specific role along with their
     * assignment type (MANUAL or DYNAMIC). Supports pagination, filtering by status,
     * and searching by username, displayName, or email.</p>
     * 
     * @param roleId the role ID
     * @param page page number (0-indexed)
     * @param size page size (5, 10, 25, 50)
     * @param status status filter (ALL, ACTIVE, INACTIVE)
     * @param search search query (username, displayName, email)
     * @return response containing role members with assignment type information
     */
    @GetMapping("/{roleId}/members")
    public ResponseEntity<GetRoleMembersResponse> getRoleMembers(
        @Parameter(description = "Role ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID roleId,
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") Integer page,
        @Parameter(description = "Page size (5, 10, 25, 50)", example = "10")
        @RequestParam(defaultValue = "10") Integer size,
        @Parameter(description = "Status filter: ALL, ACTIVE, or INACTIVE", example = "ALL")
        @RequestParam(defaultValue = "ALL") String status,
        @Parameter(description = "Search query to filter by username, displayName, or email", example = "john")
        @RequestParam(required = false) String search) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "getRoleMembers")
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
        
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_GET_ROLE_MEMBERS_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH + "/" + roleId + "/members")
                .detail("page", page)
                .detail("size", size)
                .detail("statusFilter", status)
                .detail("search", search)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            GetRoleMembersResponse response = roleService.getRoleMembers(roleId, page, size, status, search);
            
            if (response.getSuccess()) {
                logger.info()
                    .message(AppConstants.LOG_GET_ROLE_MEMBERS_SUCCESSFUL)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_TOTAL_MEMBERS_COUNT, response.getTotalCount())
                    .detail(AppConstants.DETAIL_KEY_MANUAL_ASSIGNMENT_COUNT, response.getManualAssignmentCount())
                    .detail(AppConstants.DETAIL_KEY_DYNAMIC_ASSIGNMENT_COUNT, response.getDynamicAssignmentCount())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                    .log();

                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail("method", "getRoleMembers")
                    .detail("success", true)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                    .log();

                return ResponseEntity.ok(response);
            } else {
                logger.warn()
                    .message(AppConstants.LOG_GET_ROLE_MEMBERS_FAILED_CONTROLLER)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail("responseMessage", response.getMessage())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail("method", "getRoleMembers")
                    .detail("success", false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.error(e)
                .message(AppConstants.LOG_GET_ROLE_MEMBERS_FAILED_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH + "/" + roleId + "/members")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            GetRoleMembersResponse errorResponse = new GetRoleMembersResponse(
                roleId.toString(), 
                tenantId, 
                AppConstants.FAILED_TO_RETRIEVE_ROLE_MEMBERS + ": " + e.getMessage()
            );
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "getRoleMembers")
                .detail("success", false)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves all users in the tenant with their ownership status for a specific role.
     * 
     * @param roleId the role ID to check ownership for
     * @return response containing all users with isOwner status
     */
    @Operation(
        summary = "Get role owners",
        description = "Retrieves all users in the tenant with their ownership status for a specific role. Users who are owners are shown first, followed by users who are not owners."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role owners retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GetRoleOwnersResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                            "users": [
                                {
                                    "id": "2d1c4b5a-8e9f-3762-a154-c8b7d6e5f4a3",
                                    "username": "neal.atouani",
                                    "email": "neal.atouani@intimec.com",
                                    "firstName": "Neal",
                                    "lastName": "Atouani",
                                    "displayName": "Neal Atouani",
                                    "status": "ACTIVE",
                                    "tenantId": "tenant_001",
                                    "lastLoginAt": "2025-10-27T14:31:29.717",
                                    "passwordChangedAt": "2025-06-25T12:00:00",
                                    "failedLoginAttempts": 0,
                                    "mfaEnabled": false,
                                    "isPasswordExpired": true,
                                    "profileData": "{}",
                                    "createdAt": "2025-06-25T12:00:00",
                                    "updatedAt": "2025-06-25T12:00:00",
                                    "createdBy": "",
                                    "updatedBy": "",
                                    "isOwner": true
                                },
                                {
                                    "id": "f8e7d6c5-b4a3-9281-7069-5847362514ab",
                                    "username": "youssef.noser",
                                    "email": "youssef.noser@intimec.com",
                                    "firstName": "Youssef",
                                    "lastName": "Noser",
                                    "displayName": "Youssef Noser",
                                    "status": "ACTIVE",
                                    "tenantId": "tenant_001",
                                    "lastLoginAt": "2025-11-01T12:11:32.214",
                                    "passwordChangedAt": "2025-06-25T12:00:00",
                                    "failedLoginAttempts": 0,
                                    "mfaEnabled": false,
                                    "isPasswordExpired": true,
                                    "profileData": "{}",
                                    "createdAt": "2025-06-25T12:00:00",
                                    "updatedAt": "2025-06-25T12:00:00",
                                    "createdBy": "",
                                    "updatedBy": "",
                                    "isOwner": false
                                }
                            ],
                            "totalCount": 2,
                            "usersWithOwnershipCount": 1,
                            "usersWithoutOwnershipCount": 1,
                            "roleId": "f78c51f1-69c4-4cb8-9f32-1608ea1fd4a1",
                            "tenantId": "tenant_001",
                            "success": true,
                            "message": "Role owners retrieved successfully"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Access denied to this role",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Role not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Role Not Found",
                    value = """
                        {
                            "roleId": "550e8400-e29b-41d4-a716-446655440000",
                            "tenantId": "tenant_001",
                            "success": false,
                            "message": "Role not found for owners retrieval"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/{roleId}/owners")
    public ResponseEntity<GetRoleOwnersResponse> getRoleOwners(
        @Parameter(description = "Role ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID roleId) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "getRoleOwners")
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
        
        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_GET_ROLE_OWNERS_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH + "/" + roleId + "/owners")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            GetRoleOwnersResponse response = roleService.getRoleOwners(roleId);
            
            if (response.getSuccess()) {
                logger.info()
                    .message(AppConstants.LOG_GET_ROLE_OWNERS_SUCCESSFUL)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_TOTAL_USERS_COUNT, response.getTotalCount())
                    .detail(AppConstants.DETAIL_KEY_USERS_WITH_OWNERSHIP_COUNT, response.getUsersWithOwnershipCount())
                    .detail(AppConstants.DETAIL_KEY_USERS_WITHOUT_OWNERSHIP_COUNT, response.getUsersWithoutOwnershipCount())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                    .log();

                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail("method", "getRoleOwners")
                    .detail("success", true)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                    .log();

                return ResponseEntity.ok(response);
            } else {
                logger.warn()
                    .message(AppConstants.LOG_GET_ROLE_OWNERS_FAILED_CONTROLLER)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail("responseMessage", response.getMessage())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail("method", "getRoleOwners")
                    .detail("success", false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.error(e)
                .message(AppConstants.LOG_GET_ROLE_OWNERS_FAILED_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("endpoint", "GET " + AppConstants.ROLES_BASE_PATH + "/" + roleId + "/owners")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            GetRoleOwnersResponse errorResponse = 
                new GetRoleOwnersResponse(
                    roleId.toString(), 
                    tenantId, 
                    AppConstants.FAILED_TO_RETRIEVE_ROLE_OWNERS + ": " + e.getMessage()
                );
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "getRoleOwners")
                .detail("success", false)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Deletes a role by ID for the current tenant.
     * 
     * @param id the role ID to delete
     * @return success message
     */
    @Operation(
        summary = "Delete a role",
        description = "Deletes an existing role by its ID for the current tenant. Only roles belonging to the authenticated tenant can be deleted."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role deleted successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                            "message": "Role deleted successfully",
                            "status": "200"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Access denied to this role",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Role not found",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    @DeleteMapping(AppConstants.ROLE_ID_PATH_VAR)
    public ResponseEntity<Map<String, Object>> deleteRole(
        @Parameter(description = "Role ID to delete", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID id,
        @org.springframework.web.bind.annotation.RequestHeader(value = "X-Workflow-Execution-Id", required = false)
        String workflowExecutionIdHeader) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "deleteRole")
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
            
        logger.trace()
            .message(AppConstants.LOG_TRACE_PARAMETER_VALUES)
            .detail("roleId", id.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
        
        // Extract executionId from header if present (indicates call from running workflow)
        // This is different from workflowId (definition ID) - executionId is the running instance ID
        UUID executionId = null;
        if (workflowExecutionIdHeader != null && !workflowExecutionIdHeader.trim().isEmpty()) {
            try {
                executionId = UUID.fromString(workflowExecutionIdHeader.trim());
            } catch (IllegalArgumentException e) {
                logger.warn()
                    .message("Invalid X-Workflow-Execution-Id format in delete request")
                    .detail("X-Workflow-Execution-Id", workflowExecutionIdHeader)
                    .log();
            }
        }
        
        try {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_ROLE_VALIDATION_STARTED)
                .detail("roleId", id.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_DELETION_ENDPOINT_CALLED)
                .detail("tenantId", tenantId)
                .detail("roleId", id.toString())
                .detail("endpoint", "DELETE " + AppConstants.ROLES_BASE_PATH + AppConstants.ROLE_ID_PATH_VAR)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            boolean wasDeleted = roleService.deleteRole(id, executionId);
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_BUILDING_RESPONSE)
                .detail("roleId", id.toString())
                .detail("operation", "DELETE")
                .detail("wasDeleted", wasDeleted)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            Map<String, Object> response = new HashMap<>();
            if (wasDeleted) {
                response.put("message", AppConstants.ROLE_DELETED_SUCCESS);
                response.put("status", AppConstants.HTTP_200_OK);
                response.put("deleted", true);
            } else {
                response.put("message", "Role deletion submitted for approval");
                response.put("status", "PENDING_APPROVAL");
                response.put("deleted", false);
            }
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_RESPONSE_PREPARED)
                .detail("responseStatus", HttpStatus.OK.value())
                .detail("responseMessage", AppConstants.ROLE_DELETED_SUCCESS)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            logger.info()
                .message(AppConstants.LOG_ROLE_DELETION_SUCCESSFUL)
                .detail("tenantId", tenantId)
                .detail("roleId", id.toString())
                .detail("responseStatus", HttpStatus.OK.value())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "deleteRole")
                .detail("success", true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_ANALYSIS)
                .detail("exceptionType", e.getClass().getSimpleName())
                .detail("exceptionMessage", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            logger.error(e)
                .message(AppConstants.LOG_ROLE_DELETION_FAILED_CONTROLLER)
                .detail("roleId", id.toString())
                .detail("endpoint", "DELETE " + AppConstants.ROLES_BASE_PATH + AppConstants.ROLE_ID_PATH_VAR)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            if (e.getMessage().contains("not found")) {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail("errorType", "NOT_FOUND")
                    .detail("httpStatus", HttpStatus.NOT_FOUND.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put("message", AppConstants.ROLE_NOT_FOUND);
                errorResponse.put("status", AppConstants.HTTP_404_NOT_FOUND);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else if (e.getMessage().contains("Access denied")) {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail("errorType", "FORBIDDEN")
                    .detail("httpStatus", HttpStatus.FORBIDDEN.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put("message", AppConstants.ROLE_ACCESS_DENIED);
                errorResponse.put("status", AppConstants.HTTP_403_FORBIDDEN);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            } else {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail("errorType", "INTERNAL_SERVER_ERROR")
                    .detail("httpStatus", HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put("message", AppConstants.ROLE_DELETION_FAILED);
                errorResponse.put("error", e.getMessage());
                errorResponse.put("status", AppConstants.HTTP_500_INTERNAL_ERROR);
                
                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail("method", "deleteRole")
                    .detail("success", false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                    
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
    }

    /**
     * Deletes multiple roles in bulk for the current tenant.
     * 
     * @param roleIds list of role IDs to delete (comma-separated query parameter)
     * @return response with deletion results
     */
    @Operation(
        summary = "Bulk delete roles",
        description = "Deletes multiple roles at once for the current tenant based on JWT token context. " +
                     "Pass role IDs as comma-separated query parameter: ?ids=uuid1,uuid2,uuid3"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Roles deleted successfully or partially",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid request - no role IDs provided",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkDeleteRoles(
        @Parameter(description = "Comma-separated list of role IDs to delete", required = true, example = "550e8400-e29b-41d4-a716-446655440000,650e8400-e29b-41d4-a716-446655440001")
        @RequestParam("ids") List<UUID> roleIds) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail("method", "bulkDeleteRoles")
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
            
        logger.trace()
            .message(AppConstants.LOG_TRACE_PARAMETER_VALUES)
            .detail("roleIdsCount", String.valueOf(roleIds != null ? roleIds.size() : 0))
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();
        
        try {
            if (roleIds == null || roleIds.isEmpty()) {
                logger.warn()
                    .message(AppConstants.ERROR_NO_ROLE_IDS_PROVIDED)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                    
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", AppConstants.ERROR_NO_ROLE_IDS_PROVIDED);
                errorResponse.put("status", AppConstants.HTTP_400_BAD_REQUEST);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_BULK_ROLE_DELETION_ENDPOINT_CALLED)
                .detail("tenantId", tenantId)
                .detail("roleIdsCount", String.valueOf(roleIds.size()))
                .detail("endpoint", "DELETE " + AppConstants.ROLES_BASE_PATH + "/bulk")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> result = roleService.bulkDeleteRoles(roleIds, tenantId);
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_BUILDING_RESPONSE)
                .detail("operation", "BULK_DELETE")
                .detail("successCount", result.get("successCount").toString())
                .detail("failedCount", result.get("failedCount").toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();
            
            Map<String, Object> response = new HashMap<>();
            response.put("result", result);
            response.put("status", AppConstants.HTTP_200_OK);
            
            int successCount = (int) result.get("successCount");
            int failedCount = (int) result.get("failedCount");
            
            if (failedCount == 0) {
                response.put("message", AppConstants.BULK_ROLE_DELETION_SUCCESS);
                logger.info()
                    .message(AppConstants.LOG_BULK_ROLE_DELETION_SUCCESSFUL)
                    .detail("tenantId", tenantId)
                    .detail("successCount", String.valueOf(successCount))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                    .log();
            } else {
                response.put("message", AppConstants.BULK_ROLE_DELETION_PARTIAL_SUCCESS);
                logger.warn()
                    .message(AppConstants.BULK_ROLE_DELETION_PARTIAL_SUCCESS)
                    .detail("tenantId", tenantId)
                    .detail("successCount", String.valueOf(successCount))
                    .detail("failedCount", String.valueOf(failedCount))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                    .log();
            }

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "bulkDeleteRoles")
                .detail("success", true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_RESPONSE)
                .log();

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_BULK_ROLE_DELETION_FAILED)
                .detail("endpoint", "DELETE " + AppConstants.ROLES_BASE_PATH + "/bulk")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", AppConstants.ERROR_BULK_DELETION_EXCEPTION);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", AppConstants.HTTP_500_INTERNAL_ERROR);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail("method", "bulkDeleteRoles")
                .detail("success", false)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Copies an existing role with all its owners.
     * 
     * <p>Creates a duplicate of the specified role with a new ID. The copied role will have
     * " (Copy)" appended to its name and will include all the same owners as the original role.</p>
     * 
     * @param roleId the ID of the role to copy
     * @return ResponseEntity containing the newly created role or error
     */
    @PostMapping("/{roleId}/copy")
    public ResponseEntity<RoleResponse> copyRole(@PathVariable UUID roleId) {
        // Extract tenant ID from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();
        
        logger.info()
            .message("Role copy request received")
            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            // Call service to copy the role
            RoleResponse copiedRole = roleService.copyRole(roleId);
            
            logger.info()
                .message("Role copied successfully")
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("sourceRoleId", roleId.toString())
                .detail("newRoleId", copiedRole.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_NAME, copiedRole.getRoleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.ok(copiedRole);

        } catch (RuntimeException e) {
            // Check if it's a role not found or forbidden access error
            if (e.getMessage() != null && 
                (e.getMessage().contains("not found") || e.getMessage().contains("does not belong"))) {
                
                logger.warn()
                    .message("Role copy failed - role not found or forbidden")
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
            }

            // Other runtime errors
            logger.error(e)
                .message("Role copy failed")
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    /**
     * Retrieves permission details for a specific role and action.
     * 
     * @param roleId the role ID to get permission details for
     * @param actionId the action ID to get permission details for
     * @return the permission details including justification and conditional script
     */
    @Operation(
        summary = "Get permission details",
        description = "Retrieves the permission details including justification and conditional script for a specific role and action"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Permission details retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PermissionDetailsResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                            "message": "Permission details retrieved successfully",
                            "status": "OK",
                            "data": {
                                "permissionId": "123e4567-e89b-12d3-a456-426614174000",
                                "actionId": "afe66985-b039-4629-b353-68bb7b6a37ec",
                                "actionName": null,
                                "roleId": "550e8400-e29b-41d4-a716-446655440000",
                                "roleName": "System Administrator",
                                "justification": "Role needs to view audit logs for compliance monitoring",
                                "conditionScript": "user.department = 'Finance' OR user.globalOrg = 'HQ'",
                                "hasConditionalRule": true,
                                "conditionalRuleId": "123e4567-e89b-12d3-a456-426614174002"
                            }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Permission not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Permission Not Found",
                    value = """
                        {
                            "message": "Permission not found for this role and action",
                            "status": "NOT_FOUND"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{roleId}/permissions/{actionId}")
    public ResponseEntity<Map<String, Object>> getPermissionDetails(
            @Parameter(description = "Role ID to get permission details for", required = true)
            @PathVariable UUID roleId,
            @Parameter(description = "Action ID to get permission details for", required = true)
            @PathVariable UUID actionId) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.GET_PERMISSION_DETAILS)
            .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
            .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.PROCESSING_GET_PERMISSION_DETAILS)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            PermissionDetailsResponse permissionDetails = 
                roleService.getPermissionDetails(roleId, actionId);

            Map<String, Object> response = new HashMap<>();
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.PERMISSION_DETAILS_RETRIEVED);
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_OK);
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_DATA, permissionDetails);

            logger.info()
                .message(AppConstants.PERMISSION_DETAILS_RETRIEVED)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_PERMISSION_ID, permissionDetails.getPermissionId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.GET_PERMISSION_DETAILS)
                .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.ok(response);
            
        } catch (PermissionNotFoundException e) {
            logger.warn()
                .message(AppConstants.PERMISSION_NOT_FOUND_THIS_ROLE_ACTION)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.PERMISSION_NOT_FOUND_THIS_ROLE_ACTION);
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, "NOT_FOUND");
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.FAILED_TO_GET_PERMISSION_DETAILS)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.FAILED_TO_GET_PERMISSION_DETAILS);
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, "INTERNAL_SERVER_ERROR");
            errorResponse.put("error", e.getMessage());
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.GET_PERMISSION_DETAILS)
                .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, false)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
                
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Assigns a permission to a role with optional conditional rules.
     * 
     * @param roleId the role ID to assign permission to
     * @param request the permission assignment request
     * @return the assignment result with created entity IDs
     */
    @Operation(
        summary = "Assign permission to role",
        description = "Creates a new Permission for the specified Action, associates it with the Role, and optionally creates ConditionalRule",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Permission assignment request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AssignPermissionToRoleRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Simple Permission Assignment",
                        summary = "Assign permission without conditional rules",
                        value = """
                            {
                                "actionId": "afe66985-b039-4629-b353-68bb7b6a37ec",
                                "justification": "Role needs to view audit logs for compliance monitoring"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Conditional Permission Assignment",
                        summary = "Assign permission with conditional access rules",
                        value = """
                            {
                                "actionId": "afe66985-b039-4629-b353-68bb7b6a37ec",
                                "justification": "Role needs to view audit logs with departmental restrictions",
                                "conditionScript": "user.department = 'Finance' OR user.globalOrg = 'HQ'"
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Permission assigned successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                            "message": "Permission assigned to role successfully",
                            "status": "CREATED",
                            "data": {
                                "permissionId": "123e4567-e89b-12d3-a456-426614174000",
                                "rolePermissionId": "123e4567-e89b-12d3-a456-426614174001",
                                "conditionalRuleId": "123e4567-e89b-12d3-a456-426614174002"
                            }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = """
                        {
                            "message": "Validation failed",
                            "status": "BAD_REQUEST",
                            "errors": ["Action ID is required", "Justification is required"]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Role not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Role Not Found",
                    value = """
                        {
                            "message": "Role not found or access denied",
                            "status": "NOT_FOUND"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                            "message": "Failed to assign permission to role",
                            "status": "INTERNAL_SERVER_ERROR",
                            "error": "Database connection failed"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/{roleId}/permissions/assign")
    public ResponseEntity<Map<String, Object>> assignPermissionToRole(
            @Parameter(description = "Role ID to assign permission to", required = true)
            @PathVariable UUID roleId,
            @Valid @RequestBody AssignPermissionToRoleRequest request) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_ASSIGN_PERMISSION_TO_ROLE)
            .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
            .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, request.getActionId().toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            // Check if role is SYSTEM type - cannot assign permissions to SYSTEM roles
            RoleResponse roleResponse = roleService.getRoleById(roleId);
            //if (roleResponse.getRoleType() == RoleType.SYSTEM) {
            //    logger.warn()
             //       .message(AppConstants.LOG_SYSTEM_ROLE_PERMISSION_ASSIGNMENT_ATTEMPTED)
             //       .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
             //       .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_NAME, roleResponse.getRoleName())
             //       .detail(AppConstants.ROLE_DETAIL_KEY_TENANT_ID, tenantId)
             //       .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
             //       .log();
              //  
            //    Map<String, Object> errorResponse = new HashMap<>();
             //   errorResponse.put(AppConstants.RESPONSE_KEY_ERROR, AppConstants.SYSTEM_ROLE_PERMISSION_ASSIGNMENT_FORBIDDEN);
            //    errorResponse.put(AppConstants.RESPONSE_KEY_MESSAGE, "Cannot assign permissions to SYSTEM roles. SYSTEM roles have unrestricted access to all permissions.");
             //   errorResponse.put(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString());
             //   errorResponse.put(AppConstants.ROLE_DETAIL_KEY_ROLE_TYPE, RoleType.SYSTEM.name());
             //   
             //   return ResponseEntity.badRequest().body(errorResponse);
            //}
            
            // Validate conditional script if provided
            if (request.hasConditionalRule() && request.getConditionScript() != null) {
                logger.debug()
                    .message(AppConstants.LOG_VALIDATING_CONDITIONAL_SCRIPT_SYNTAX)
                    .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.ROLE_DETAIL_KEY_CONDITION_SCRIPT, request.getConditionScript())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                    .log();
                
                boolean isValidScript = conditionalRuleEvaluationService.isValidConditionScript(request.getConditionScript());
                
                if (!isValidScript) {
                    logger.warn()
                        .message(AppConstants.LOG_INVALID_CONDITIONAL_SCRIPT_PROVIDED)
                        .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                        .detail(AppConstants.ROLE_DETAIL_KEY_CONDITION_SCRIPT, request.getConditionScript())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                        .log();
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put(AppConstants.RESPONSE_KEY_ERROR, AppConstants.ERROR_INVALID_CONDITIONAL_SCRIPT);
                    errorResponse.put(AppConstants.RESPONSE_KEY_MESSAGE, AppConstants.ERROR_INVALID_CONDITIONAL_SCRIPT_SYNTAX);
                    errorResponse.put(AppConstants.RESPONSE_KEY_CONDITION_SCRIPT, request.getConditionScript());
                    
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                logger.debug()
                    .message(AppConstants.LOG_CONDITIONAL_SCRIPT_VALIDATION_SUCCESSFUL)
                    .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.ROLE_DETAIL_KEY_CONDITION_SCRIPT, request.getConditionScript())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                    .log();
            }
            
            logger.info()
                .message(AppConstants.LOG_PROCESSING_PERMISSION_ASSIGNMENT)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, request.getActionId().toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.ROLE_DETAIL_KEY_HAS_CONDITIONAL_RULE, String.valueOf(request.hasConditionalRule()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, UUID> result = roleService.assignPermissionToRole(roleId, request);

            Map<String, Object> response = new HashMap<>();
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.PERMISSION_ASSIGNED_TO_ROLE_SUCCESS);
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_CREATED);
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_DATA, result);

            logger.info()
                .message(AppConstants.LOG_PERMISSION_ASSIGNMENT_SUCCESSFUL_CONTROLLER)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, request.getActionId().toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_PERMISSION_ID, result.get("permissionId").toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_HAS_CONDITIONAL_RULE, String.valueOf(result.containsKey("conditionalRuleId")))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_ASSIGN_PERMISSION_TO_ROLE)
                .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (DuplicatePermissionException e) {
            logger.warn()
                .message(AppConstants.LOG_DUPLICATE_PERMISSION_ATTEMPTED_CONTROLLER)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, request.getActionId().toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.DUPLICATE_PERMISSION_ASSIGNMENT);
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_CONFLICT);
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_ROLE_ID, e.getRoleId());
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_ACTION_ID, e.getActionId());
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                .detail(AppConstants.ROLE_DETAIL_KEY_ERROR_TYPE, AppConstants.ERROR_TYPE_CONFLICT)
                .detail(AppConstants.ROLE_DETAIL_KEY_HTTP_STATUS, HttpStatus.CONFLICT.value())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_ASSIGN_PERMISSION_TO_ROLE)
                .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, false)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_PERMISSION_ASSIGNMENT_FAILED_CONTROLLER)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, request.getActionId().toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            if (e.getMessage().contains("not found") || e.getMessage().contains("Access denied")) {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail(AppConstants.ROLE_DETAIL_KEY_ERROR_TYPE, AppConstants.ERROR_TYPE_NOT_FOUND)
                    .detail(AppConstants.ROLE_DETAIL_KEY_HTTP_STATUS, HttpStatus.NOT_FOUND.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.ROLE_NOT_FOUND_OR_ACCESS_DENIED);
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_NOT_FOUND);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail(AppConstants.ROLE_DETAIL_KEY_ERROR_TYPE, AppConstants.ERROR_TYPE_INTERNAL_SERVER_ERROR)
                    .detail(AppConstants.ROLE_DETAIL_KEY_HTTP_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.PERMISSION_ASSIGNMENT_FAILED);
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_ERROR, e.getMessage());
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_INTERNAL_SERVER_ERROR);
                
                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_ASSIGN_PERMISSION_TO_ROLE)
                    .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                    
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
    }

    /**
     * Submit a permission assignment request for approval workflow.
     * 
     * <p>Instead of immediately assigning the permission, this endpoint publishes an event
     * that triggers an approval workflow. Upon approval, the actual assignment is performed.</p>
     * 
     * @param roleId the role ID to assign permission to
     * @param request the submit-for-approval request with workflowId
     * @return response entity indicating submission status
     */
    @Operation(
        summary = "Submit permission assignment for approval",
        description = "Submit a permission assignment request through an approval workflow. " +
                     "The permission is not immediately assigned but triggers a workflow with approval nodes.",
        responses = {
            @ApiResponse(responseCode = "202", description = "Request submitted for approval"),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failure"),
            @ApiResponse(responseCode = "404", description = "Role not found or has no owners"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    @PostMapping("/{roleId}/permissions/assign/submit-for-approval")
    public ResponseEntity<Map<String, Object>> submitPermissionAssignmentForApproval(
            @Parameter(description = "Role ID to assign permission to", required = true)
            @PathVariable UUID roleId,
            @Valid @RequestBody SubmitPermissionAssignmentForApprovalRequest request) {
        
        logger.trace()
            .message("Entering method: submitPermissionAssignmentForApproval")
            .detail("roleId", roleId.toString())
            .detail("actionId", request.getActionId().toString())
            .detail("workflowId", request.getWorkflowId())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message("Processing permission assignment approval request")
                .detail("roleId", roleId.toString())
                .detail("actionId", request.getActionId().toString())
                .detail("workflowId", request.getWorkflowId())
                .detail("tenantId", tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> result = roleService.submitPermissionAssignmentForApproval(roleId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Permission assignment submitted for approval");
            response.put("status", "PENDING_APPROVAL");
            response.put("data", result);

            logger.info()
                .message("Permission assignment approval request submitted successfully")
                .detail("roleId", roleId.toString())
                .detail("actionId", request.getActionId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            logger.trace()
                .message("Exiting method: submitPermissionAssignmentForApproval")
                .detail("success", true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            logger.error(e)
                .message("Permission assignment approval request failed")
                .detail("roleId", roleId.toString())
                .detail("actionId", request.getActionId().toString())
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            if (e.getMessage().contains("not found") || e.getMessage().contains("no owners")) {
                errorResponse.put("message", e.getMessage());
                errorResponse.put("status", "NOT_FOUND");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else {
                errorResponse.put("message", "Failed to submit permission assignment for approval");
                errorResponse.put("error", e.getMessage());
                errorResponse.put("status", "INTERNAL_SERVER_ERROR");
                
                logger.trace()
                    .message("Exiting method: submitPermissionAssignmentForApproval")
                    .detail("success", false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                    
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
    }

    /**
     * Submits bulk permission assignments for approval workflow.
     * 
     * <p>This endpoint does NOT assign permissions immediately. It validates the request
     * and publishes an event to trigger the approval workflow.</p>
     */
    @Operation(
        summary = "Submit bulk permission assignments for approval",
        description = "Submits multiple permission assignments to an approval workflow"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully submitted for approval"),
        @ApiResponse(responseCode = "400", description = "Invalid request or role is inactive"),
        @ApiResponse(responseCode = "404", description = "Role not found or has no owners"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{roleId}/permissions/submit-bulk-assignment-for-approval")
    public ResponseEntity<Map<String, Object>> submitBulkPermissionAssignmentForApproval(
            @PathVariable UUID roleId,
            @Valid @RequestBody SubmitBulkPermissionAssignmentForApprovalRequest request) {
        
        logger.trace()
            .message("Entering method: submitBulkPermissionAssignmentForApproval")
            .detail("roleId", roleId.toString())
            .detail("assignmentCount", String.valueOf(request.getAssignmentCount()))
            .detail("workflowId", request.getWorkflowId())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            Map<String, Object> result = roleService.submitBulkPermissionAssignmentForApproval(roleId, request);
            
            logger.trace()
                .message("Exiting method: submitBulkPermissionAssignmentForApproval")
                .detail("roleId", roleId.toString())
                .detail("status", result.get("status").toString())
                .detail("success", true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error(e)
                .message("Error in submitBulkPermissionAssignmentForApproval")
                .detail("roleId", roleId.toString())
                .detail("assignmentCount", String.valueOf(request.getAssignmentCount()))
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            if (e.getMessage().contains("not found") || e.getMessage().contains("no owners")) {
                errorResponse.put("message", e.getMessage());
                errorResponse.put("status", "NOT_FOUND");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else {
                errorResponse.put("message", "Failed to submit bulk permission assignment for approval");
                errorResponse.put("error", e.getMessage());
                errorResponse.put("status", "INTERNAL_SERVER_ERROR");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
    }

    /**
     * Assigns multiple permissions to a role in a single atomic transaction.
     * 
     * <p>This endpoint performs bulk permission assignments with atomicity guarantee:
     * all assignments succeed or none do (rollback on any failure).</p>
     * 
     * @param roleId the role ID to assign permissions to
     * @param request the bulk assignment request containing list of assignments (1-100 items)
     * @return response entity with success status and created permission IDs or error details
     */
    @Operation(
        summary = "Bulk assign permissions to role",
        description = "Assigns multiple permissions to a role in a single atomic transaction. " +
                     "All assignments must succeed, or the entire operation is rolled back. " +
                     "Maximum 100 permissions per request.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Permissions assigned successfully",
                content = @Content(schema = @Schema(implementation = BulkAssignmentResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failure"),
            @ApiResponse(responseCode = "404", description = "Role not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Duplicate permission or inactive role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    @PostMapping("/{roleId}/permissions/bulk")
    public ResponseEntity<BulkAssignmentResult> assignPermissionsBulk(
            @Parameter(description = "Role ID", required = true)
            @PathVariable UUID roleId,
            @Valid @RequestBody BulkAssignPermissionRequest request) {
        
        logger.trace()
            .message("Entering method: assignPermissionsBulk")
            .detail("roleId", roleId.toString())
            .detail("assignmentCount", String.valueOf(request.getAssignmentCount()))
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            logger.info()
                .message("Processing bulk permission assignment request")
                .detail("roleId", roleId.toString())
                .detail("assignmentCount", String.valueOf(request.getAssignmentCount()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            BulkAssignmentResult result = roleService.assignPermissionsBulk(roleId, request);

            if (result.isSuccess()) {
                logger.info()
                    .message("Bulk permission assignment successful")
                    .detail("roleId", roleId.toString())
                    .detail("assignedCount", String.valueOf(result.getAssignedCount()))
                    .detail("totalRequested", String.valueOf(result.getTotalRequested()))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                logger.trace()
                    .message("Exiting method: assignPermissionsBulk")
                    .detail("success", "true")
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                return ResponseEntity.status(HttpStatus.CREATED).body(result);
            } else {
                logger.warn()
                    .message("Bulk permission assignment failed")
                    .detail("roleId", roleId.toString())
                    .detail("totalRequested", String.valueOf(result.getTotalRequested()))
                    .detail("errors", result.getErrors().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                // Determine appropriate HTTP status based on error
                HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
                if (!result.getErrors().isEmpty()) {
                    String firstError = result.getErrors().get(0).toLowerCase();
                    if (firstError.contains("not found")) {
                        status = HttpStatus.NOT_FOUND;
                    } else if (firstError.contains("duplicate") || firstError.contains("inactive")) {
                        status = HttpStatus.CONFLICT;
                    } else if (firstError.contains("invalid")) {
                        status = HttpStatus.BAD_REQUEST;
                    }
                }

                logger.trace()
                    .message("Exiting method: assignPermissionsBulk")
                    .detail("success", "false")
                    .detail("httpStatus", String.valueOf(status.value()))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                return ResponseEntity.status(status).body(result);
            }

        } catch (IllegalArgumentException e) {
            logger.warn()
                .message("Bulk assignment validation failed")
                .detail("roleId", roleId.toString())
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            BulkAssignmentResult errorResult = BulkAssignmentResult.failure(
                request.getAssignmentCount(),
                "Validation failed: " + e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);

        } catch (Exception e) {
            logger.error(e)
                .message("Bulk permission assignment failed with exception")
                .detail("roleId", roleId.toString())
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            BulkAssignmentResult errorResult = BulkAssignmentResult.failure(
                request.getAssignmentCount(),
                "Internal error: " + e.getMessage()
            );

            logger.trace()
                .message("Exiting method: assignPermissionsBulk")
                .detail("success", "false")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Revokes multiple permissions from a role in a single atomic transaction.
     * 
     * @param roleId the role ID to revoke permissions from
     * @param request the bulk revocation request containing action IDs to revoke
     * @return ResponseEntity with bulk revocation result
     */
    @Operation(
        summary = "Revoke multiple permissions from role (bulk operation)",
        description = "Revokes multiple permissions from a role in a single atomic transaction. " +
                     "All revocations must succeed, or the entire operation is rolled back. " +
                     "Maximum 100 permissions per request.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Permissions revoked successfully",
                content = @Content(schema = @Schema(implementation = BulkRevocationResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failure"),
            @ApiResponse(responseCode = "404", description = "Role not found or permissions don't exist"),
            @ApiResponse(responseCode = "409", description = "Role is inactive"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    @PostMapping("/{roleId}/permissions/bulk-revoke")
    public ResponseEntity<BulkRevocationResult> revokePermissionsBulk(
            @Parameter(description = "Role ID", required = true)
            @PathVariable UUID roleId,
            @Valid @RequestBody BulkRevokePermissionRequest request) {
        
        logger.trace()
            .message("Entering method: revokePermissionsBulk")
            .detail("roleId", roleId.toString())
            .detail("revocationCount", String.valueOf(request.getRevocationCount()))
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            logger.info()
                .message("Processing bulk permission revocation request")
                .detail("roleId", roleId.toString())
                .detail("revocationCount", String.valueOf(request.getRevocationCount()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            BulkRevocationResult result = roleService.revokePermissionsBulk(roleId, request);

            if (result.isSuccess()) {
                logger.info()
                    .message("Bulk permission revocation successful")
                    .detail("roleId", roleId.toString())
                    .detail("revokedCount", String.valueOf(result.getRevokedCount()))
                    .detail("totalRequested", String.valueOf(result.getTotalRequested()))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                logger.trace()
                    .message("Exiting method: revokePermissionsBulk")
                    .detail("success", "true")
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                return ResponseEntity.ok(result);
            } else {
                logger.warn()
                    .message("Bulk permission revocation failed")
                    .detail("roleId", roleId.toString())
                    .detail("totalRequested", String.valueOf(result.getTotalRequested()))
                    .detail("errors", result.getErrors().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                // Determine appropriate HTTP status based on error
                HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
                if (!result.getErrors().isEmpty()) {
                    String firstError = result.getErrors().get(0).toLowerCase();
                    if (firstError.contains("not found")) {
                        status = HttpStatus.NOT_FOUND;
                    } else if (firstError.contains("inactive")) {
                        status = HttpStatus.CONFLICT;
                    } else if (firstError.contains("invalid")) {
                        status = HttpStatus.BAD_REQUEST;
                    }
                }

                logger.trace()
                    .message("Exiting method: revokePermissionsBulk")
                    .detail("success", "false")
                    .detail("httpStatus", String.valueOf(status.value()))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();

                return ResponseEntity.status(status).body(result);
            }

        } catch (IllegalArgumentException e) {
            logger.warn()
                .message("Bulk revocation validation failed")
                .detail("roleId", roleId.toString())
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            BulkRevocationResult errorResult = BulkRevocationResult.failure(
                request.getRevocationCount(),
                "Validation failed: " + e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);

        } catch (Exception e) {
            logger.error(e)
                .message("Bulk permission revocation failed with exception")
                .detail("roleId", roleId.toString())
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            BulkRevocationResult errorResult = BulkRevocationResult.failure(
                request.getRevocationCount(),
                "Internal error: " + e.getMessage()
            );

            logger.trace()
                .message("Exiting method: revokePermissionsBulk")
                .detail("success", "false")
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Deletes a permission from a role.
     * 
     * @param roleId the role ID to remove permission from
     * @param actionId the action ID to remove permission for
     * @return ResponseEntity with deletion confirmation
     */
    @Operation(
        summary = "Delete permission from role",
        description = "Removes a permission from a role, including the permission entity, role-permission association, and any conditional rules.",
        parameters = {
            @Parameter(description = "Role ID to remove permission from", required = true),
            @Parameter(description = "Action ID to remove permission for", required = true)
        },
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Permission deleted successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                        type = "object",
                        example = """
                        {
                            "message": "Permission deleted from role successfully",
                            "status": "OK",
                            "data": {
                                "permissionId": "550e8400-e29b-41d4-a716-446655440001",
                                "rolePermissionId": "550e8400-e29b-41d4-a716-446655440002",
                                "conditionalRuleId": "550e8400-e29b-41d4-a716-446655440003"
                            }
                        }
                        """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "Permission not found for this role and action",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                        type = "object",
                        example = """
                        {
                            "message": "Permission not found for this role and action",
                            "status": "NOT_FOUND",
                            "roleId": "550e8400-e29b-41d4-a716-446655440000",
                            "actionId": "afe66985-b039-4629-b353-68bb7b6a37ec"
                        }
                        """
                    )
                )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    @DeleteMapping("/{roleId}/permissions/delete/{actionId}")
    public ResponseEntity<Map<String, Object>> deletePermissionFromRole(
            @Parameter(description = "Role ID to remove permission from", required = true)
            @PathVariable UUID roleId,
            @Parameter(description = "Action ID to remove permission for", required = true)
            @PathVariable UUID actionId) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_DELETE_PERMISSION_FROM_ROLE)
            .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
            .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_PROCESSING_PERMISSION_DELETION)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, UUID> result = roleService.deletePermissionFromRole(roleId, actionId);

            Map<String, Object> response = new HashMap<>();
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.PERMISSION_DELETED_FROM_ROLE_SUCCESS);
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_OK);
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_DATA, result);

            logger.info()
                .message(AppConstants.LOG_PERMISSION_DELETION_SUCCESSFUL_CONTROLLER)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_PERMISSION_ID, result.get("permissionId").toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_DELETE_PERMISSION_FROM_ROLE)
                .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.ok(response);

        } catch (PermissionNotFoundException e) {
            logger.warn()
                .message(AppConstants.LOG_PERMISSION_NOT_FOUND_CONTROLLER)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.PERMISSION_NOT_FOUND_FOR_ROLE);
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_NOT_FOUND);
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_ROLE_ID, e.getRoleId());
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_ACTION_ID, e.getActionId());
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                .detail(AppConstants.ROLE_DETAIL_KEY_ERROR_TYPE, AppConstants.ERROR_TYPE_NOT_FOUND)
                .detail(AppConstants.ROLE_DETAIL_KEY_HTTP_STATUS, HttpStatus.NOT_FOUND.value())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_DELETE_PERMISSION_FROM_ROLE)
                .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, false)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_PERMISSION_DELETION_FAILED_CONTROLLER)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            if (e.getMessage().contains("not found") || e.getMessage().contains("Access denied")) {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail(AppConstants.ROLE_DETAIL_KEY_ERROR_TYPE, AppConstants.ERROR_TYPE_NOT_FOUND)
                    .detail(AppConstants.ROLE_DETAIL_KEY_HTTP_STATUS, HttpStatus.NOT_FOUND.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.ROLE_NOT_FOUND_OR_ACCESS_DENIED);
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_NOT_FOUND);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail(AppConstants.ROLE_DETAIL_KEY_ERROR_TYPE, AppConstants.ERROR_TYPE_INTERNAL_SERVER_ERROR)
                    .detail(AppConstants.ROLE_DETAIL_KEY_HTTP_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.PERMISSION_DELETION_FAILED);
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_ERROR, e.getMessage());
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_INTERNAL_SERVER_ERROR);
                
                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_DELETE_PERMISSION_FROM_ROLE)
                    .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                    
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
    }

    /**
     * Submit a permission revocation request for approval workflow.
     * 
     * <p>Instead of immediately revoking the permission, this endpoint publishes an event
     * that triggers an approval workflow. Upon approval, the actual revocation is performed.</p>
     * 
     * @param roleId the role ID to revoke permission from
     * @param request the submit-for-approval request with workflowId
     * @return response entity indicating submission status
     */
    @Operation(
        summary = "Submit permission revocation for approval",
        description = "Submit a permission revocation request through an approval workflow. " +
                     "The permission is not immediately revoked but triggers a workflow with approval nodes.",
        responses = {
            @ApiResponse(responseCode = "202", description = "Request submitted for approval"),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failure"),
            @ApiResponse(responseCode = "404", description = "Role not found or has no owners"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    @PostMapping("/{roleId}/permissions/revoke/submit-for-approval")
    public ResponseEntity<Map<String, Object>> submitPermissionRevocationForApproval(
            @Parameter(description = "Role ID to revoke permission from", required = true)
            @PathVariable UUID roleId,
            @Valid @RequestBody SubmitPermissionRevocationForApprovalRequest request) {
        
        logger.trace()
            .message("Entering method: submitPermissionRevocationForApproval")
            .detail("roleId", roleId.toString())
            .detail("actionId", request.getActionId().toString())
            .detail("workflowId", request.getWorkflowId())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message("Processing permission revocation approval request")
                .detail("roleId", roleId.toString())
                .detail("actionId", request.getActionId().toString())
                .detail("workflowId", request.getWorkflowId())
                .detail("tenantId", tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> result = roleService.submitPermissionRevocationForApproval(roleId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Permission revocation submitted for approval");
            response.put("status", "PENDING_APPROVAL");
            response.put("data", result);

            logger.info()
                .message("Permission revocation approval request submitted successfully")
                .detail("roleId", roleId.toString())
                .detail("actionId", request.getActionId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            logger.trace()
                .message("Exiting method: submitPermissionRevocationForApproval")
                .detail("success", true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            logger.error(e)
                .message("Permission revocation approval request failed")
                .detail("roleId", roleId.toString())
                .detail("actionId", request.getActionId().toString())
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            if (e.getMessage().contains("not found") || e.getMessage().contains("no owners")) {
                errorResponse.put("message", e.getMessage());
                errorResponse.put("status", "NOT_FOUND");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else {
                errorResponse.put("message", "Failed to submit permission revocation for approval");
                errorResponse.put("error", e.getMessage());
                errorResponse.put("status", "INTERNAL_SERVER_ERROR");
                
                logger.trace()
                    .message("Exiting method: submitPermissionRevocationForApproval")
                    .detail("success", false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                    
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
    }

    /**
     * Submits bulk permission revocations for approval workflow.
     * 
     * <p>This endpoint does NOT revoke permissions immediately. It validates the request
     * and publishes an event to trigger the approval workflow.</p>
     */
    @Operation(
        summary = "Submit bulk permission revocations for approval",
        description = "Submits multiple permission revocations to an approval workflow"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully submitted for approval"),
        @ApiResponse(responseCode = "400", description = "Invalid request or role is inactive"),
        @ApiResponse(responseCode = "404", description = "Role not found or has no owners"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{roleId}/permissions/submit-bulk-revocation-for-approval")
    public ResponseEntity<Map<String, Object>> submitBulkPermissionRevocationForApproval(
            @PathVariable UUID roleId,
            @Valid @RequestBody SubmitBulkPermissionRevocationForApprovalRequest request) {
        
        logger.trace()
            .message("Entering method: submitBulkPermissionRevocationForApproval")
            .detail("roleId", roleId.toString())
            .detail("revocationCount", String.valueOf(request.getRevocationCount()))
            .detail("workflowId", request.getWorkflowId())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            Map<String, Object> result = roleService.submitBulkPermissionRevocationForApproval(roleId, request);
            
            logger.trace()
                .message("Exiting method: submitBulkPermissionRevocationForApproval")
                .detail("roleId", roleId.toString())
                .detail("status", result.get("status").toString())
                .detail("success", true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error(e)
                .message("Error in submitBulkPermissionRevocationForApproval")
                .detail("roleId", roleId.toString())
                .detail("revocationCount", String.valueOf(request.getRevocationCount()))
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            if (e.getMessage().contains("not found") || e.getMessage().contains("no owners")) {
                errorResponse.put("message", e.getMessage());
                errorResponse.put("status", "NOT_FOUND");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else {
                errorResponse.put("message", "Failed to submit bulk permission revocation for approval");
                errorResponse.put("error", e.getMessage());
                errorResponse.put("status", "INTERNAL_SERVER_ERROR");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
    }

    /**
     * Updates an existing permission for a role.
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail(AppConstants.ROLE_DETAIL_KEY_ERROR_TYPE, AppConstants.ERROR_TYPE_INTERNAL_SERVER_ERROR)
                    .detail(AppConstants.ROLE_DETAIL_KEY_HTTP_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.PERMISSION_DELETION_FAILED);
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_ERROR, e.getMessage());
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_INTERNAL_SERVER_ERROR);
                
                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_DELETE_PERMISSION_FROM_ROLE)
                    .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                    
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
    }

    /**
     * Updates a permission for a role.
     * 
     * @param roleId the role ID to update permission for
     * @param actionId the action ID to update permission for
     * @param request the permission update request
     * @return ResponseEntity with update confirmation
     */
    @Operation(
        summary = "Update permission for role",
        description = "Updates the justification and conditional rules for an existing permission assigned to a role.",
        parameters = {
            @Parameter(description = "Role ID to update permission for", required = true),
            @Parameter(description = "Action ID to update permission for", required = true)
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Permission update request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UpdatePermissionRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Update with Conditional Rule",
                        summary = "Update permission with conditional access rules",
                        value = """
                            {
                                "justification": "Updated role needs to view audit logs with departmental restrictions",
                                "conditionScript": "user.department == 'Finance' && user.level >= 'Manager'"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Update without Conditional Rule",
                        summary = "Update permission without conditional access rules",
                        value = """
                            {
                                "justification": "Updated role needs to view audit logs for compliance monitoring"
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Permission updated successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                        type = "object",
                        example = """
                        {
                            "message": "Permission updated for role successfully",
                            "status": "OK",
                            "data": {
                                "permissionId": "550e8400-e29b-41d4-a716-446655440001",
                                "conditionalRuleId": "550e8400-e29b-41d4-a716-446655440003"
                            }
                        }
                        """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "Permission not found for this role and action",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                        type = "object",
                        example = """
                        {
                            "message": "Permission not found for this role and action",
                            "status": "NOT_FOUND",
                            "roleId": "550e8400-e29b-41d4-a716-446655440000",
                            "actionId": "afe66985-b039-4629-b353-68bb7b6a37ec"
                        }
                        """
                    )
                )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    @PutMapping("/{roleId}/permissions/update/{actionId}")
    public ResponseEntity<Map<String, Object>> updatePermissionForRole(
            @Parameter(description = "Role ID to update permission for", required = true)
            @PathVariable UUID roleId,
            @Parameter(description = "Action ID to update permission for", required = true)
            @PathVariable UUID actionId,
            @Valid @RequestBody UpdatePermissionRequest request) {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_METHOD)
            .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_UPDATE_PERMISSION_FOR_ROLE)
            .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
            .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
            .log();

        try {
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info()
                .message(AppConstants.LOG_PROCESSING_PERMISSION_UPDATE)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.ROLE_DETAIL_KEY_HAS_CONDITIONAL_RULE, String.valueOf(request.hasConditionalRule()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, UUID> result = roleService.updatePermissionForRole(roleId, actionId, request);

            Map<String, Object> response = new HashMap<>();
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.PERMISSION_UPDATED_FOR_ROLE_SUCCESS);
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_OK);
            response.put(AppConstants.PERMISSION_RESPONSE_KEY_DATA, result);

            logger.info()
                .message(AppConstants.LOG_PERMISSION_UPDATE_SUCCESSFUL_CONTROLLER)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_PERMISSION_ID, result.get("permissionId").toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_HAS_CONDITIONAL_RULE, String.valueOf(result.containsKey("conditionalRuleId")))
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_UPDATE_PERMISSION_FOR_ROLE)
                .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, true)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            return ResponseEntity.ok(response);

        } catch (PermissionNotFoundException e) {
            logger.warn()
                .message(AppConstants.LOG_PERMISSION_NOT_FOUND_CONTROLLER)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.PERMISSION_NOT_FOUND_FOR_ROLE);
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_NOT_FOUND);
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_ROLE_ID, e.getRoleId());
            errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_ACTION_ID, e.getActionId());
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                .detail(AppConstants.ROLE_DETAIL_KEY_ERROR_TYPE, AppConstants.ERROR_TYPE_NOT_FOUND)
                .detail(AppConstants.ROLE_DETAIL_KEY_HTTP_STATUS, HttpStatus.NOT_FOUND.value())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_UPDATE_PERMISSION_FOR_ROLE)
                .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, false)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_PERMISSION_UPDATE_FAILED_CONTROLLER)
                .detail(AppConstants.ROLE_DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ACTION_ID, actionId.toString())
                .detail(AppConstants.ROLE_DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            if (e.getMessage().contains("not found") || e.getMessage().contains("Access denied")) {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail(AppConstants.ROLE_DETAIL_KEY_ERROR_TYPE, AppConstants.ERROR_TYPE_NOT_FOUND)
                    .detail(AppConstants.ROLE_DETAIL_KEY_HTTP_STATUS, HttpStatus.NOT_FOUND.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.ROLE_NOT_FOUND_OR_ACCESS_DENIED);
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_NOT_FOUND);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else {
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_ERROR_RESPONSE_PREPARED)
                    .detail(AppConstants.ROLE_DETAIL_KEY_ERROR_TYPE, AppConstants.ERROR_TYPE_INTERNAL_SERVER_ERROR)
                    .detail(AppConstants.ROLE_DETAIL_KEY_HTTP_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_MESSAGE, AppConstants.PERMISSION_UPDATE_FAILED);
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_ERROR, e.getMessage());
                errorResponse.put(AppConstants.PERMISSION_RESPONSE_KEY_STATUS, AppConstants.PERMISSION_STATUS_INTERNAL_SERVER_ERROR);
                
                logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_METHOD)
                    .detail(AppConstants.ROLE_DETAIL_KEY_METHOD, AppConstants.METHOD_UPDATE_PERMISSION_FOR_ROLE)
                    .detail(AppConstants.ROLE_DETAIL_KEY_SUCCESS, false)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                    
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }
    }

    /**
     * Retrieves role statistics for the current tenant.
     * 
     * @return role statistics including total roles and active administrators
     */
    @Operation(
        summary = "Get role statistics",
        description = "Retrieves aggregated role statistics for the current tenant including total roles and administrator count"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role statistics retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RoleStatsDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    @GetMapping("/stats")
    public ResponseEntity<?> getRoleStats() {
        String tenantId = tenantContextService.getCurrentTenantId();
        
        logger.info()
            .message(AppConstants.LOG_FETCHING_ROLE_STATS)
            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
            .log();

        try {
            RoleStatsDTO stats = roleService.getRoleStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalRoles", stats.getTotalRoles());
            response.put(AppConstants.STATS_FIELD_INACTIVE, stats.getInactiveRoles());
            response.put("activeAdmins", stats.getActiveAdmins());
            response.put(AppConstants.STATS_FIELD_ACTIVE, stats.getActiveAdmins());
            response.put(AppConstants.STATS_FIELD_HIGH_RISK, stats.getActiveAdmins());
            response.put("tenantId", stats.getTenantId());
            response.put("message", AppConstants.ROLE_STATS_RETRIEVED_SUCCESS);
            response.put("status", "success");

            logger.info()
                .message(AppConstants.LOG_ROLE_STATS_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail("totalRoles", stats.getTotalRoles())
                .detail(AppConstants.STATS_FIELD_INACTIVE, stats.getInactiveRoles())
                .detail(AppConstants.STATS_FIELD_ACTIVE, stats.getActiveAdmins())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ROLE_STATS_FAILED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", AppConstants.ROLE_STATS_RETRIEVAL_FAILED);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get roles owned by the current user.
     *
     * @return OwnedRolesResponse containing roles owned by current user
     */
    @Operation(
        summary = "Get Owned Roles",
        description = "Retrieves all roles owned by the currently authenticated user with their permissions and metadata"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Owned roles retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OwnedRolesResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    @GetMapping("/owned-by-me")
    public ResponseEntity<OwnedRolesResponse> getOwnedRoles() {
        String tenantId = tenantContextService.getCurrentTenantId();
        UUID userId = UUID.fromString(tenantContextService.getCurrentUserId());
        
        logger.info()
            .message("Fetching owned roles for current user")
            .detail("userId", userId)
            .detail("tenantId", tenantId)
            .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
            .log();
        
        try {
            OwnedRolesResponse response = roleService.getOwnedRolesByUser(userId);
            
            logger.info()
                .message("Owned roles retrieved successfully")
                .detail("userId", userId)
                .detail("roleCount", response.getTotalCount())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to retrieve owned roles")
                .detail("userId", userId)
                .detail("tenantId", tenantId)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                .log();
            
            OwnedRolesResponse errorResponse = new OwnedRolesResponse(
                java.util.Collections.emptyList(),
                0,
                false,
                "Failed to retrieve owned roles: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}