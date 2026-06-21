package com.aidentx.applicationpermissions.roles_management.controller;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.DynamicRuleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.UpdateDynamicRuleRequest;
import com.aidentx.applicationpermissions.roles_management.service.DynamicRuleService;
import com.aidentx.applicationpermissions.shared.security.TenantContextService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for dynamic rule management operations.
 * 
 * <p>Provides endpoints for creating and managing dynamic rules that control
 * role assignment behavior with automatic tenant isolation based on JWT token context.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/dynamic-rules")
@Tag(name = "Dynamic Rule Management", description = "API endpoints for managing dynamic rules with multi-tenant support")
@SecurityRequirement(name = "Bearer Authentication")
public class DynamicRuleController {

    private final DynamicRuleService dynamicRuleService;
    private final TenantContextService tenantContextService;
    private final IUnifiedLogger logger;

    /**
     * Constructor for DynamicRuleController.
     * 
     * @param dynamicRuleService service for dynamic rule operations
     * @param tenantContextService service for extracting tenant context
     * @param loggerFactory factory for creating unified loggers
     */
    @Autowired
    public DynamicRuleController(DynamicRuleService dynamicRuleService,
                                TenantContextService tenantContextService,
                                UnifiedLoggerFactory loggerFactory) {
        this.dynamicRuleService = dynamicRuleService;
        this.tenantContextService = tenantContextService;
        this.logger = loggerFactory.getLogger("DynamicRuleController", "DYNAMIC_RULE_MANAGEMENT");
    }

    /**
     * Creates a new dynamic rule within the current tenant.
     * 
     * @param request the dynamic rule creation request
     * @return the created dynamic rule response
     */
    @Operation(
        summary = "Create a dynamic rule",
        description = "Creates a new dynamic rule for role assignment automation within the current tenant based on JWT token context",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Dynamic rule creation request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateDynamicRuleRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Immediate Rule",
                        value = """
                            {
                                "roleId": "123e4567-e89b-12d3-a456-426614174000",
                                "ruleIsActive": true,
                                "ruleConditionScript": "user.department == 'Finance' && user.level >= 3",
                                "sendNotificationOnAssignment": false,
                                "requireApprovalBeforeAssignment": false,
                                "scheduleType": "IMMEDIATE",
                                "time": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Scheduled Daily Rule",
                        value = """
                            {
                                "roleId": "123e4567-e89b-12d3-a456-426614174000",
                                "ruleIsActive": true,
                                "ruleConditionScript": "user.region == 'US_EAST' && user.active == true",
                                "sendNotificationOnAssignment": true,
                                "requireApprovalBeforeAssignment": true,
                                "scheduleType": "DAILY",
                                "time": "14:30"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Weekly Approval Rule",
                        value = """
                            {
                                "roleId": "123e4567-e89b-12d3-a456-426614174000",
                                "ruleIsActive": true,
                                "ruleConditionScript": "user.role == 'Manager' && user.yearsOfService >= 2",
                                "sendNotificationOnAssignment": true,
                                "requireApprovalBeforeAssignment": true,
                                "scheduleType": "WEEKLY",
                                "time": "09:00"
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
            description = "Dynamic rule created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DynamicRuleResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or role not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                            "message": "Role not found or does not belong to tenant",
                            "status": "BAD_REQUEST",
                            "timestamp": "2024-03-15T10:30:00Z"
                        }
                        """
                )
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
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDynamicRule(@Valid @RequestBody CreateDynamicRuleRequest request) {
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_CREATE_DYNAMIC_RULE_METHOD)
            .detail("methodName", "createDynamicRule")
            .detail("roleId", request.roleId().toString())
            .detail("scheduleType", request.scheduleType().name())
            .log();
            
        logger.trace()
            .message(AppConstants.LOG_TRACE_REQUEST_PARAMETERS)
            .detail("request", request.toString())
            .detail("roleId", request.roleId().toString())
            .detail("scheduleType", request.scheduleType().name())
            .detail("ruleIsActive", String.valueOf(request.ruleIsActive()))
            .log();
            
        try {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_STARTING_DYNAMIC_RULE_CREATION)
                .detail("roleId", request.roleId().toString())
                .detail("scheduleType", request.scheduleType().name())
                .detail("ruleIsActive", String.valueOf(request.ruleIsActive()))
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_VALIDATING_REQUEST_PARAMETERS)
                .detail("roleId", request.roleId().toString())
                .detail("scheduleType", request.scheduleType().name())
                .log();
                
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_TENANT_CONTEXT_EXTRACTED)
                .detail("tenantId", tenantId)
                .log();

            logger.info()
                .message(AppConstants.LOG_DYNAMIC_RULE_CREATION_ENDPOINT_CALLED)
                .detail("tenantId", tenantId)
                .detail("roleId", request.roleId().toString())
                .detail("scheduleType", request.scheduleType().name())
                .detail("ruleIsActive", String.valueOf(request.ruleIsActive()))
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_CALLING_SERVICE_METHOD)
                .detail("tenantId", tenantId)
                .detail("roleId", request.roleId().toString())
                .log();

            DynamicRuleResponse response = dynamicRuleService.createDynamicRule(request);
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_SERVICE_METHOD_COMPLETED)
                .detail("tenantId", tenantId)
                .detail("dynamicRuleId", response.id().toString())
                .detail("roleId", response.roleId().toString())
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_BUILDING_SUCCESS_RESPONSE)
                .detail("dynamicRuleId", response.id().toString())
                .log();
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", AppConstants.DYNAMIC_RULE_CREATED_SUCCESSFULLY);
            successResponse.put("status", HttpStatus.CREATED.name());
            successResponse.put("data", response);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_RESPONSE_DATA)
                .detail("response", successResponse.toString())
                .log();

            logger.info()
                .message(AppConstants.LOG_DYNAMIC_RULE_CREATED_SUCCESSFULLY)
                .detail("tenantId", tenantId)
                .detail("dynamicRuleId", response.id().toString())
                .detail("roleId", response.roleId().toString())
                .detail("scheduleType", response.scheduleType().name())
                .log();
                
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_CREATE_DYNAMIC_RULE_METHOD)
                .detail("methodName", "createDynamicRule")
                .log();

            return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);

        } catch (IllegalArgumentException e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_CAUGHT)
                .detail("roleId", request.roleId().toString())
                .detail("errorMessage", e.getMessage())
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_ANALYZING_EXCEPTION_TYPE)
                .detail("errorMessage", e.getMessage())
                .log();
                
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXCEPTION_DETAILS)
                .detail("errorMessage", e.getMessage())
                .detail("exceptionType", e.getClass().getSimpleName())
                .log();

            logger.error()
                .message(AppConstants.LOG_DYNAMIC_RULE_CREATION_FAILED_INVALID_ARGUMENTS)
                .detail("roleId", request.roleId().toString())
                .detail("errorMessage", e.getMessage())
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_BUILDING_ERROR_RESPONSE)
                .detail("errorMessage", e.getMessage())
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", HttpStatus.BAD_REQUEST.name());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_CAUGHT)
                .detail("roleId", request.roleId().toString())
                .detail("errorMessage", e.getMessage())
                .log();
                
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXCEPTION_DETAILS)
                .detail("errorMessage", e.getMessage())
                .detail("exceptionType", e.getClass().getSimpleName())
                .detail("stackTrace", java.util.Arrays.toString(e.getStackTrace()))
                .log();

            logger.error()
                .message(AppConstants.LOG_DYNAMIC_RULE_CREATION_FAILED)
                .detail("roleId", request.roleId().toString())
                .detail("errorMessage", e.getMessage())
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", AppConstants.FAILED_TO_CREATE_DYNAMIC_RULE);
            errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.name());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Updates an existing dynamic rule.
     * 
     * <p>Updates a dynamic rule identified by the role ID in the request.
     * If the schedule type changes from DAILY to IMMEDIATE, the rule will be executed instantly.</p>
     * 
     * @param request the update dynamic rule request containing roleId and new properties
     * @return ResponseEntity containing the updated dynamic rule data or error information
     */
    @PutMapping
    @Operation(summary = "Update Dynamic Rule", 
               description = "Updates an existing dynamic rule for a role. If schedule changes from DAILY to IMMEDIATE, the rule executes instantly.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dynamic rule updated successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = DynamicRuleResponse.class),
                        examples = @ExampleObject(value = """
                            {
                                "message": "Dynamic rule updated successfully",
                                "status": "OK",
                                "data": {
                                    "id": "123e4567-e89b-12d3-a456-426614174000",
                                    "roleId": "64aedcba-f440-4b4c-b549-1083ed12070b",
                                    "roleName": "System Administrator",
                                    "ruleIsActive": true,
                                    "ruleConditionScript": "user.status == 'ACTIVE'",
                                    "sendNotificationOnAssignment": false,
                                    "requireApprovalBeforeAssignment": false,
                                    "scheduleType": "IMMEDIATE",
                                    "time": null,
                                    "createdAt": "2024-01-15T10:30:00",
                                    "updatedAt": "2024-01-15T14:28:00"
                                },
                                "timestamp": "2024-01-15T14:28:00"
                            }
                            """))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = """
                            {
                                "message": "Role not found or does not belong to tenant",
                                "status": "BAD_REQUEST",
                                "timestamp": "2024-01-15T14:28:00"
                            }
                            """))),
        @ApiResponse(responseCode = "404", description = "Dynamic rule not found",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = """
                            {
                                "message": "Dynamic rule not found for this role",
                                "status": "NOT_FOUND",
                                "timestamp": "2024-01-15T14:28:00"
                            }
                            """))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = """
                            {
                                "message": "Failed to update dynamic rule",
                                "status": "INTERNAL_SERVER_ERROR",
                                "timestamp": "2024-01-15T14:28:00"
                            }
                            """)))
    })
    public ResponseEntity<Map<String, Object>> updateDynamicRule(
            @Valid @RequestBody UpdateDynamicRuleRequest request) {
        
        logger.info()
                .message(AppConstants.LOG_RECEIVED_REQUEST_TO_UPDATE_DYNAMIC_RULE)
                .detail("roleId", request.roleId().toString())
                .detail("scheduleType", request.scheduleType().name())
                .detail("ruleIsActive", request.ruleIsActive().toString())
                .log();
        
        try {
            // Update the dynamic rule
            DynamicRuleResponse response = dynamicRuleService.updateDynamicRule(request);
            
            logger.info()
                .message(AppConstants.LOG_DYNAMIC_RULE_UPDATED_SUCCESSFULLY_CONTROLLER)
                .detail("dynamicRuleId", response.id().toString())
                .detail("roleId", request.roleId().toString())
                .detail("roleName", response.roleName())
                .log();
            
            // Prepare success response
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", AppConstants.RESPONSE_DYNAMIC_RULE_UPDATED_SUCCESSFULLY);
            successResponse.put("status", HttpStatus.OK.name());
            successResponse.put("data", response);
            successResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(successResponse);
            
        } catch (IllegalArgumentException e) {
            logger.error()
                .message(AppConstants.LOG_INVALID_REQUEST_FOR_DYNAMIC_RULE_UPDATE)
                .detail("roleId", request.roleId().toString())
                .detail("errorMessage", e.getMessage())
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", HttpStatus.BAD_REQUEST.name());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_FAILED_TO_UPDATE_DYNAMIC_RULE_CONTROLLER)
                .detail("roleId", request.roleId().toString())
                .detail("errorMessage", e.getMessage())
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", AppConstants.RESPONSE_FAILED_TO_UPDATE_DYNAMIC_RULE);
            errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.name());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Retrieves a dynamic rule by role ID.
     * 
     * @param roleId the role ID to get the dynamic rule for
     * @return the dynamic rule response or 404 if not found
     */
    @Operation(
        summary = "Get dynamic rule by role ID",
        description = "Retrieves the dynamic rule configuration for a specific role within the current tenant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dynamic rule retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DynamicRuleResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                            "message": "Dynamic rule retrieved successfully",
                            "status": "OK",
                            "data": {
                                "id": "123e4567-e89b-12d3-a456-426614174000",
                                "roleId": "a7e54b78-6eee-4901-a8be-285e670718bf",
                                "roleName": "System Administrator",
                                "ruleIsActive": true,
                                "ruleConditionScript": "user.status == 'ACTIVE'",
                                "sendNotificationOnAssignment": true,
                                "requireApprovalBeforeAssignment": false,
                                "scheduleType": "DAILY",
                                "time": "14:12",
                                "createdAt": "2024-01-15T10:30:00",
                                "updatedAt": "2024-01-15T10:30:00"
                            }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Dynamic rule not found for this role",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                            "message": "No dynamic rule found for this role",
                            "status": "NOT_FOUND"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid role ID or role not found"
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
    @GetMapping("/role/{roleId}")
    public ResponseEntity<Map<String, Object>> getDynamicRuleByRoleId(
            @PathVariable UUID roleId) {
        
        logger.info()
            .message(AppConstants.LOG_GET_DYNAMIC_RULE_ENDPOINT_CALLED)
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
            .log();
        
        try {
            DynamicRuleResponse response = dynamicRuleService.getDynamicRuleByRoleId(roleId);
            
            if (response == null) {
                logger.info()
                    .message(AppConstants.LOG_NO_DYNAMIC_RULE_FOR_ROLE)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .log();
                
                Map<String, Object> notFoundResponse = new HashMap<>();
                notFoundResponse.put(AppConstants.RESPONSE_KEY_MESSAGE, AppConstants.RESPONSE_NO_DYNAMIC_RULE_FOR_ROLE);
                notFoundResponse.put(AppConstants.RESPONSE_KEY_STATUS, HttpStatus.NOT_FOUND.name());
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundResponse);
            }
            
            logger.info()
                .message(AppConstants.LOG_DYNAMIC_RULE_RETRIEVED_SUCCESSFULLY_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, response.id().toString())
                .log();
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put(AppConstants.RESPONSE_KEY_MESSAGE, AppConstants.RESPONSE_DYNAMIC_RULE_RETRIEVED_SUCCESSFULLY);
            successResponse.put(AppConstants.RESPONSE_KEY_STATUS, HttpStatus.OK.name());
            successResponse.put(AppConstants.RESPONSE_KEY_DATA, response);
            
            return ResponseEntity.ok(successResponse);
            
        } catch (IllegalArgumentException e) {
            logger.error()
                .message(AppConstants.LOG_INVALID_REQUEST_FOR_DYNAMIC_RULE_RETRIEVAL)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.RESPONSE_KEY_MESSAGE, e.getMessage());
            errorResponse.put(AppConstants.RESPONSE_KEY_STATUS, HttpStatus.BAD_REQUEST.name());

            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_DYNAMIC_RULE_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.RESPONSE_KEY_MESSAGE, AppConstants.RESPONSE_FAILED_TO_RETRIEVE_DYNAMIC_RULE);
            errorResponse.put(AppConstants.RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.name());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}