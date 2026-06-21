package com.aidentx.applicationpermissions.roles_management.controller;

import com.aidentx.applicationpermissions.kafka.NotificationEventData;
import com.aidentx.applicationpermissions.kafka.NotificationKafkaService;
import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.model.dto.*;
import com.aidentx.applicationpermissions.roles_management.model.entity.Role;
import com.aidentx.applicationpermissions.roles_management.repository.RoleRepository;
import com.aidentx.applicationpermissions.roles_management.service.UserRoleService;
import com.aidentx.applicationpermissions.shared.grpc.client.UserGrpcClient;
import com.aidentx.applicationpermissions.shared.security.TenantContextService;
import com.aidentx.auth.grpc.UserInfo;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for user-role assignment operations.
 * 
 * <p>Provides endpoints for assigning, removing, and retrieving user-role assignments
 * with automatic tenant isolation based on JWT token context.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/user-roles")
@Tag(name = "User Role Management", description = "API endpoints for managing user-role assignments with multi-tenant support")
@SecurityRequirement(name = "Bearer Authentication")
public class UserRoleController {

    private final UserRoleService userRoleService;
    private final TenantContextService tenantContextService;
    private final NotificationKafkaService notificationKafkaService;
    private final RoleRepository roleRepository;
    private final UserGrpcClient userGrpcClient;
    private final IUnifiedLogger logger;

    /**
     * Constructor for UserRoleController.
     * 
     * @param userRoleService service for user-role operations
     * @param tenantContextService service for extracting tenant context
     * @param notificationKafkaService service for sending notification events via Kafka
     * @param roleRepository repository for role operations
     * @param userGrpcClient client for user operations via gRPC
     * @param loggerFactory factory for creating unified loggers
     */
    @Autowired
    public UserRoleController(UserRoleService userRoleService,
                             TenantContextService tenantContextService,
                             NotificationKafkaService notificationKafkaService,
                             RoleRepository roleRepository,
                             UserGrpcClient userGrpcClient,
                             UnifiedLoggerFactory loggerFactory) {
        this.userRoleService = userRoleService;
        this.tenantContextService = tenantContextService;
        this.notificationKafkaService = notificationKafkaService;
        this.roleRepository = roleRepository;
        this.userGrpcClient = userGrpcClient;
        this.logger = loggerFactory.getLogger(AppConstants.USER_ROLE_CONTROLLER_LOGGER_NAME, AppConstants.LOG_CATEGORY_USER_ROLE_MANAGEMENT);
    }

    /**
     * Assigns a role to one or more users within the current tenant.
     * 
     * @param request the role assignment request (single or bulk)
     * @return the created user-role assignment response (single or bulk)
     */
    @Operation(
        summary = "Assign a role to one or more users",
        description = "Assigns a role to a single user or multiple users within the current tenant based on JWT token context",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Role assignment request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AssignRoleRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Single User Assignment",
                        value = """
                            {
                                "userId": "123e4567-e89b-12d3-a456-426614174000",
                                "roleId": "456e7890-e89b-12d3-a456-426614174001"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Bulk User Assignment",
                        value = """
                            {
                                "userIds": [
                                    "123e4567-e89b-12d3-a456-426614174000",
                                    "234e5678-e89b-12d3-a456-426614174001",
                                    "345e6789-e89b-12d3-a456-426614174002"
                                ],
                                "roleId": "456e7890-e89b-12d3-a456-426614174001"
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
            description = "Role assigned successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserRoleResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or user already has the role",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                            "message": "User already has this role assigned",
                            "status": "BAD_REQUEST",
                            "timestamp": "2024-03-15T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User or role not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                            "message": "Role not found or does not belong to tenant",
                            "status": "NOT_FOUND",
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
    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignRole(@Valid @RequestBody AssignRoleRequest request) {
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_ASSIGN_ROLE_METHOD)
            .detail(AppConstants.DETAIL_KEY_METHOD_NAME, "assignRole")
            .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT)
            .log();
            
        logger.trace()
            .message(AppConstants.LOG_TRACE_REQUEST_PARAMETERS)
            .detail(AppConstants.DETAIL_KEY_REQUEST, request.toString())
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
            .detail(AppConstants.USER_ROLE_DETAIL_KEY_ASSIGNMENT_TYPE, request.isBulkAssignment() ? AppConstants.ASSIGNMENT_TYPE_BULK : AppConstants.ASSIGNMENT_TYPE_SINGLE)
            .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT)
            .log();
            
        try {
            String assignmentType = request.isBulkAssignment() ? AppConstants.ASSIGNMENT_TYPE_BULK : AppConstants.ASSIGNMENT_TYPE_SINGLE;
            List<UUID> userIds = request.getAllUserIds();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_STARTING_USER_ROLE_ASSIGNMENT)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_ASSIGNMENT_TYPE, assignmentType)
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(userIds.size()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT)
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_VALIDATING_REQUEST_PARAMETERS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_ASSIGNMENT_TYPE, assignmentType)
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT)
                .log();
                
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_TENANT_CONTEXT_EXTRACTED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT)
                .log();

            logger.info()
                .message(AppConstants.LOG_USER_ROLE_ASSIGNMENT_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_ASSIGNMENT_TYPE, assignmentType)
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(userIds.size()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT)
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_CALLING_SERVICE_METHOD)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_ASSIGNMENT_TYPE, assignmentType)
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT)
                .log();

            Object response = userRoleService.assignRole(request);
            
            // Send notification events for successful role assignments only
            try {
                // Extract successful user IDs from response
                List<UUID> successfulUserIds = new ArrayList<>();
                if (request.isBulkAssignment()) {
                    BulkUserRoleResponse bulkResponse = (BulkUserRoleResponse) response;
                    successfulUserIds = bulkResponse.successfulAssignments().stream()
                        .map(assignment -> assignment.userId())
                        .collect(Collectors.toList());
                } else {
                    UserRoleResponse singleResponse = (UserRoleResponse) response;
                    // For single assignment, if we got a valid response, it means success
                    if (singleResponse != null && singleResponse.id() != null) {
                        successfulUserIds.add(request.userId());
                    }
                }
                
                // Only send notifications if there are successful assignments AND notifications are enabled
                if (!successfulUserIds.isEmpty() && request.shouldSendNotification()) {
                    sendRoleAssignmentNotifications(request.roleId(), successfulUserIds, tenantId);
                    
                    logger.debug()
                        .message(AppConstants.USER_ROLE_LOG_NOTIFICATIONS_SENT)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                        .detail(AppConstants.USER_ROLE_DETAIL_KEY_SUCCESSFUL_ASSIGNMENT_COUNT, successfulUserIds.size())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT)
                        .log();
                } else if (!successfulUserIds.isEmpty() && !request.shouldSendNotification()) {
                    logger.debug()
                        .message(AppConstants.USER_ROLE_LOG_NOTIFICATIONS_DISABLED)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                        .detail(AppConstants.USER_ROLE_DETAIL_KEY_SUCCESSFUL_ASSIGNMENT_COUNT, successfulUserIds.size())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT)
                        .log();
                } else {
                    logger.debug()
                        .message(AppConstants.USER_ROLE_LOG_NO_SUCCESSFUL_ASSIGNMENTS)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT)
                        .log();
                }
            } catch (Exception notificationException) {
                // Log notification failure but don't fail the main operation
                logger.warn()
                    .message(AppConstants.USER_ROLE_LOG_FAILED_TO_SEND_NOTIFICATIONS)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_ASSIGNMENT_TYPE, request.isBulkAssignment() ? AppConstants.ASSIGNMENT_TYPE_BULK : AppConstants.ASSIGNMENT_TYPE_SINGLE)
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_ERROR, notificationException.getMessage())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ENDPOINT)
                    .log();
            }
            
            Map<String, Object> successResponse = new HashMap<>();
            
            if (request.isBulkAssignment()) {
                BulkUserRoleResponse bulkResponse = (BulkUserRoleResponse) response;
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_SERVICE_METHOD_COMPLETED)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                    .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_ASSIGNMENTS, String.valueOf(bulkResponse.summary().successCount()))
                    .detail(AppConstants.DETAIL_KEY_FAILED_ASSIGNMENTS, String.valueOf(bulkResponse.summary().failureCount()))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_SUCCESS)
                    .log();
                    
                successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.BULK_USER_ROLES_ASSIGNED_SUCCESSFULLY);
                successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.OK.name());
                successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_DATA, bulkResponse);
            } else {
                UserRoleResponse singleResponse = (UserRoleResponse) response;
                logger.debug()
                    .message(AppConstants.LOG_DEBUG_SERVICE_METHOD_COMPLETED)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                    .detail(AppConstants.DETAIL_KEY_USER_ROLE_ID, singleResponse.id().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_SUCCESS)
                    .log();
                    
                successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.USER_ROLE_ASSIGNED_SUCCESSFULLY);
                successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.CREATED.name());
                successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_DATA, singleResponse);
            }
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_RESPONSE_DATA)
                .detail(AppConstants.DETAIL_KEY_RESPONSE, successResponse.toString())
                .objectType(request.isBulkAssignment() ? AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_SUCCESS : AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_SUCCESS)
                .log();

            if (request.isBulkAssignment()) {
                BulkUserRoleResponse bulkResponse = (BulkUserRoleResponse) response;
                logger.info()
                    .message(AppConstants.LOG_BULK_USER_ROLE_ASSIGNMENT_SUCCESSFUL)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                    .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_ASSIGNMENTS, String.valueOf(bulkResponse.summary().successCount()))
                    .detail(AppConstants.DETAIL_KEY_FAILED_ASSIGNMENTS, String.valueOf(bulkResponse.summary().failureCount()))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_SUCCESS)
                    .log();
            } else {
                UserRoleResponse singleResponse = (UserRoleResponse) response;
                logger.info()
                    .message(AppConstants.LOG_USER_ROLE_ASSIGNMENT_SUCCESSFUL)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                    .detail(AppConstants.DETAIL_KEY_USER_ROLE_ID, singleResponse.id().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_SUCCESS)
                    .log();
            }
                
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_ASSIGN_ROLE_METHOD)
                .detail(AppConstants.DETAIL_KEY_METHOD_NAME, "assignRole")
                .objectType(request.isBulkAssignment() ? AppConstants.LOG_OBJECT_TYPE_BULK_USER_ROLE_ASSIGNMENT_SUCCESS : AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_SUCCESS)
                .log();

            return request.isBulkAssignment() ? 
                ResponseEntity.ok(successResponse) : 
                ResponseEntity.status(HttpStatus.CREATED).body(successResponse);

        } catch (IllegalArgumentException e) {
            List<UUID> userIds = request.getAllUserIds();
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_CAUGHT)
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(userIds.size()))
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_VALIDATION_ERROR)
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_ANALYZING_EXCEPTION_TYPE)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_VALIDATION_ERROR)
                .log();
                
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXCEPTION_DETAILS)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_EXCEPTION_TYPE, e.getClass().getSimpleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_VALIDATION_ERROR)
                .log();

            logger.error()
                .message(AppConstants.LOG_USER_ROLE_ASSIGNMENT_FAILED_CONTROLLER_INVALID_ARGUMENTS)
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(userIds.size()))
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_VALIDATION_ERROR)
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_BUILDING_ERROR_RESPONSE)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_VALIDATION_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, e.getMessage());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.BAD_REQUEST.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (IllegalStateException e) {
            List<UUID> userIds = request.getAllUserIds();
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_CAUGHT)
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(userIds.size()))
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_STATE_ERROR)
                .log();
                
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXCEPTION_DETAILS)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_EXCEPTION_TYPE, e.getClass().getSimpleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_STATE_ERROR)
                .log();

            logger.error()
                .message(AppConstants.LOG_USER_ROLE_ASSIGNMENT_FAILED_CONTROLLER_INVALID_STATE)
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(userIds.size()))
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_STATE_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, e.getMessage());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.BAD_REQUEST.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            List<UUID> userIds = request.getAllUserIds();
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Exception thrown without message: " + e.getClass().getSimpleName();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_CAUGHT)
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(userIds.size()))
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, errorMessage)
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_EXCEPTION_TYPE, e.getClass().getSimpleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ERROR)
                .log();
                
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXCEPTION_DETAILS)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, errorMessage)
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_EXCEPTION_TYPE, e.getClass().getSimpleName())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_STACK_TRACE, java.util.Arrays.toString(e.getStackTrace()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ERROR)
                .log();

            logger.error()
                .message(AppConstants.LOG_USER_ROLE_ASSIGNMENT_FAILED_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, String.valueOf(userIds.size()))
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, errorMessage)
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_EXCEPTION_TYPE, e.getClass().getSimpleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_ASSIGNMENT_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, request.isBulkAssignment() ? AppConstants.FAILED_TO_ASSIGN_ROLES_TO_USERS : AppConstants.FAILED_TO_ASSIGN_ROLE_TO_USER);
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_ERROR, errorMessage);
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_EXCEPTION_TYPE, e.getClass().getSimpleName());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Removes a role from a user within the current tenant.
     * 
     * @param request the role removal request
     * @return success response indicating whether the role was removed
     */
    @Operation(
        summary = "Remove a role from a user",
        description = "Removes a role from a user within the current tenant based on JWT token context",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Role removal request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RemoveRoleRequest.class),
                examples = @ExampleObject(
                    name = "Remove Administrator Role",
                    value = """
                        {
                            "userId": "123e4567-e89b-12d3-a456-426614174000",
                            "roleId": "456e7890-e89b-12d3-a456-426614174001"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role removal completed (whether removed or not)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                            "message": "Role removed successfully",
                            "status": "OK",
                            "removed": true
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
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
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> removeRole(@Valid @RequestBody RemoveRoleRequest request) {
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_REMOVE_ROLE_METHOD)
            .detail(AppConstants.DETAIL_KEY_METHOD_NAME, "removeRole")
            .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ENDPOINT)
            .log();
            
        logger.trace()
            .message(AppConstants.LOG_TRACE_REQUEST_PARAMETERS)
            .detail(AppConstants.DETAIL_KEY_REQUEST, request.toString())
            .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
            .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ENDPOINT)
            .log();
            
        try {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_STARTING_USER_ROLE_REMOVAL)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ENDPOINT)
                .log();
                
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_TENANT_CONTEXT_EXTRACTED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ENDPOINT)
                .log();

            logger.info()
                .message(AppConstants.LOG_USER_ROLE_REMOVAL_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ENDPOINT)
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_CALLING_SERVICE_METHOD)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ENDPOINT)
                .log();

            boolean removed = userRoleService.removeRole(request);
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_SERVICE_METHOD_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_REMOVED, String.valueOf(removed))
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_SUCCESS)
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_BUILDING_SUCCESS_RESPONSE)
                .detail(AppConstants.DETAIL_KEY_REMOVED, String.valueOf(removed))
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_SUCCESS)
                .log();

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, removed ? AppConstants.USER_ROLE_REMOVED_SUCCESSFULLY : AppConstants.USER_ROLE_ASSIGNMENT_NOT_FOUND);
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.OK.name());
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_REMOVED, removed);
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_RESPONSE_DATA)
                .detail(AppConstants.DETAIL_KEY_RESPONSE, successResponse.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_SUCCESS)
                .log();

            logger.info()
                .message(AppConstants.LOG_USER_ROLE_REMOVAL_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_REMOVED, String.valueOf(removed))
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_SUCCESS)
                .log();
                
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_REMOVE_ROLE_METHOD)
                .detail(AppConstants.DETAIL_KEY_METHOD_NAME, "removeRole")
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_SUCCESS)
                .log();

            return ResponseEntity.ok(successResponse);

        } catch (IllegalArgumentException e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_CAUGHT)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_VALIDATION_ERROR)
                .log();
                
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXCEPTION_DETAILS)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_EXCEPTION_TYPE, e.getClass().getSimpleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_VALIDATION_ERROR)
                .log();

            logger.error()
                .message(AppConstants.LOG_USER_ROLE_REMOVAL_FAILED_CONTROLLER_INVALID_ARGUMENTS)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_VALIDATION_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, e.getMessage());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.BAD_REQUEST.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_CAUGHT)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ERROR)
                .log();
                
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXCEPTION_DETAILS)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_EXCEPTION_TYPE, e.getClass().getSimpleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ERROR)
                .log();

            logger.error()
                .message(AppConstants.LOG_USER_ROLE_REMOVAL_FAILED_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_USER_ID, request.userId().toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_USER_ROLE_REMOVAL_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.FAILED_TO_REMOVE_ROLE_FROM_USER);
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves all roles assigned to a specific user within the current tenant.
     * 
     * @param userId the user ID to get roles for
     * @return list of user-role assignment responses
     */
    @Operation(
        summary = "Get user roles",
        description = "Retrieves all roles assigned to a specific user within the current tenant",
        parameters = @Parameter(
            name = "userId",
            description = "The ID of the user to get roles for",
            required = true,
            example = "123e4567-e89b-12d3-a456-426614174000"
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User roles retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserRoleResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid user ID or user not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                            "message": "User not found or does not belong to tenant",
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


    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRoles(
            @PathVariable UUID userId
    ) {

        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_GET_USER_ROLES_METHOD)
            .detail(AppConstants.DETAIL_KEY_METHOD_NAME, "getUserRoles")
            .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_ENDPOINT)
            .log();
            
        try {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_STARTING_GET_USER_ROLES)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_ENDPOINT)
                .log();
                
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_TENANT_CONTEXT_EXTRACTED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_ENDPOINT)
                .log();

            logger.info()
                .message(AppConstants.LOG_GET_USER_ROLES_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_ENDPOINT)
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_CALLING_SERVICE_METHOD)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_ENDPOINT)
                .log();

            List<Map<String, Object>> userRoles = ((com.aidentx.applicationpermissions.roles_management.service.impl.UserRoleServiceImpl) userRoleService).getUserRolesEnriched(userId);
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_SERVICE_METHOD_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLES_COUNT, String.valueOf(userRoles.size()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_SUCCESS)
                .log();
                
            logger.debug()
                .message(AppConstants.LOG_DEBUG_BUILDING_SUCCESS_RESPONSE)
                .detail(AppConstants.DETAIL_KEY_ROLES_COUNT, String.valueOf(userRoles.size()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_SUCCESS)
                .log();

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.USER_ROLES_RETRIEVED_SUCCESSFULLY);
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.OK.name());
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_DATA, userRoles);
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_COUNT, userRoles.size());
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_RESPONSE_DATA)
                .detail(AppConstants.DETAIL_KEY_RESPONSE, successResponse.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_SUCCESS)
                .log();

            logger.info()
                .message(AppConstants.LOG_USER_ROLES_RETRIEVED_SUCCESSFULLY)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLES_COUNT, String.valueOf(userRoles.size()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_SUCCESS)
                .log();
                
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_GET_USER_ROLES_METHOD)
                .detail(AppConstants.DETAIL_KEY_METHOD_NAME, "getUserRoles")
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_SUCCESS)
                .log();

            return ResponseEntity.ok(successResponse);

        } catch (IllegalArgumentException e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_CAUGHT)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_VALIDATION_ERROR)
                .log();

            logger.error()
                .message(AppConstants.LOG_GET_USER_ROLES_FAILED_CONTROLLER_INVALID_ARGUMENTS)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_VALIDATION_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, e.getMessage());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.BAD_REQUEST.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_CAUGHT)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_ERROR)
                .log();

            logger.error()
                .message(AppConstants.LOG_GET_USER_ROLES_FAILED_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_USER_ROLES_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.FAILED_TO_RETRIEVE_USER_ROLES);
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves all users assigned to a specific role within the current tenant.
     * 
     * @param roleId the role ID to get users for
     * @return list of user-role assignment responses
     */
    @Operation(
        summary = "Get role users",
        description = "Retrieves all users assigned to a specific role within the current tenant",
        parameters = @Parameter(
            name = "roleId",
            description = "The ID of the role to get users for",
            required = true,
            example = "456e7890-e89b-12d3-a456-426614174001"
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role users retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserRoleResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid role ID or role not found",
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
    @GetMapping("/role/{roleId}")
    public ResponseEntity<Map<String, Object>> getRoleUsers(@PathVariable UUID roleId) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.info()
                .message(AppConstants.LOG_GET_ROLE_USERS_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_ROLE_USERS_ENDPOINT)
                .log();

            List<UserRoleResponse> roleUsers = userRoleService.getRoleUsers(roleId);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.ROLE_USERS_RETRIEVED_SUCCESSFULLY);
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.OK.name());
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_DATA, roleUsers);
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_COUNT, roleUsers.size());

            logger.info()
                .message(AppConstants.LOG_ROLE_USERS_RETRIEVED_SUCCESSFULLY)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_USERS_COUNT, String.valueOf(roleUsers.size()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_ROLE_USERS_SUCCESS)
                .log();

            return ResponseEntity.ok(successResponse);

        } catch (IllegalArgumentException e) {
            logger.error()
                .message(AppConstants.LOG_GET_ROLE_USERS_FAILED_CONTROLLER_INVALID_ARGUMENTS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_ROLE_USERS_VALIDATION_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, e.getMessage());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.BAD_REQUEST.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_GET_ROLE_USERS_FAILED_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_ROLE_USERS_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.FAILED_TO_RETRIEVE_ROLE_USERS);
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves all user-role assignments within the current tenant.
     * 
     * @return list of all user-role assignment responses
     */
    @Operation(
        summary = "Get all user roles",
        description = "Retrieves all user-role assignments within the current tenant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "All user roles retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserRoleResponse.class)
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
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUserRoles() {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.info()
                .message(AppConstants.LOG_GET_ALL_USER_ROLES_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_ALL_USER_ROLES_ENDPOINT)
                .log();

            List<UserRoleResponse> allUserRoles = userRoleService.getAllUserRoles();

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.ALL_USER_ROLES_RETRIEVED_SUCCESSFULLY);
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.OK.name());
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_DATA, allUserRoles);
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_COUNT, allUserRoles.size());

            logger.info()
                .message(AppConstants.LOG_ALL_USER_ROLES_RETRIEVED_SUCCESSFULLY)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_TOTAL_COUNT, String.valueOf(allUserRoles.size()))
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_ALL_USER_ROLES_SUCCESS)
                .log();

            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_GET_ALL_USER_ROLES_FAILED_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_GET_ALL_USER_ROLES_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.FAILED_TO_RETRIEVE_ALL_USER_ROLES);
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Checks if a user has a specific role within the current tenant.
     * 
     * @param userId the user ID to check
     * @param roleId the role ID to check
     * @return response indicating whether the user has the role
     */
    @Operation(
        summary = "Check if user has role",
        description = "Checks if a user has a specific role within the current tenant",
        parameters = {
            @Parameter(
                name = "userId",
                description = "The ID of the user to check",
                required = true,
                example = "123e4567-e89b-12d3-a456-426614174000"
            ),
            @Parameter(
                name = "roleId",
                description = "The ID of the role to check",
                required = true,
                example = "456e7890-e89b-12d3-a456-426614174001"
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role check completed successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                            "message": "Role check completed",
                            "status": "OK",
                            "hasRole": true
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
    @GetMapping("/check/{userId}/{roleId}")
    public ResponseEntity<Map<String, Object>> hasRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.info()
                .message(AppConstants.LOG_CHECK_USER_ROLE_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_CHECK_USER_ROLE_ENDPOINT)
                .log();

            boolean hasRole = userRoleService.hasRole(userId, roleId);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.ROLE_CHECK_COMPLETED);
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.OK.name());
            successResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_HAS_ROLE, hasRole);

            logger.info()
                .message(AppConstants.LOG_USER_ROLE_CHECK_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_HAS_ROLE, String.valueOf(hasRole))
                .objectType(AppConstants.LOG_OBJECT_TYPE_CHECK_USER_ROLE_SUCCESS)
                .log();

            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.LOG_CHECK_USER_ROLE_FAILED_CONTROLLER)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_CHECK_USER_ROLE_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.FAILED_TO_CHECK_USER_ROLE);
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Revokes a role from multiple users.
     * 
     * @param request the bulk role revocation request containing user IDs and role ID
     * @return ResponseEntity containing the bulk revocation response with success/failure details
     */
    @Operation(
        summary = "Revoke role from multiple users",
        description = "Removes a role from multiple users in a single operation. Users who don't have the role are treated as successful (idempotent operation)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role revocation completed (check response for individual success/failure details)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BulkRoleRevocationResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "roleId": "64aedcba-f440-4b4c-b549-1083ed12070b",
                        "roleName": "System Administrator",
                        "requestedUsers": ["user1-uuid", "user2-uuid", "user3-uuid"],
                        "successfulRevocations": ["user1-uuid", "user2-uuid"],
                        "failedRevocations": ["user3-uuid"],
                        "totalRequested": 3,
                        "totalSuccessful": 2,
                        "totalFailed": 1
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
                    value = """
                    {
                        "message": "Validation failed",
                        "status": "BAD_REQUEST",
                        "timestamp": "2024-01-01T12:00:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Role not found or doesn't belong to tenant",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "Role not found or does not belong to tenant",
                        "status": "NOT_FOUND",
                        "timestamp": "2024-01-01T12:00:00"
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
                    value = """
                    {
                        "message": "Failed to revoke role from users",
                        "status": "INTERNAL_SERVER_ERROR",
                        "timestamp": "2024-01-01T12:00:00"
                    }
                    """
                )
            )
        )
    })
    @DeleteMapping("/revoke")
    public ResponseEntity<?> revokeRole(
        @Parameter(description = "Bulk role revocation request", required = true)
        @Valid @RequestBody RevokeRoleRequest request
    ) {
        try {
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.info()
                .message(AppConstants.USER_ROLE_LOG_REVOCATION_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_USER_COUNT, request.getUserCount())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_IS_BULK_REVOCATION, request.isBulkRevocation())
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_REVOCATION_REQUEST)
                .log();

            BulkRoleRevocationResponse response = userRoleService.revokeRole(request);

            logger.info()
                .message(AppConstants.USER_ROLE_LOG_REVOCATION_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_TOTAL_REQUESTED, response.totalRequested())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_TOTAL_SUCCESSFUL, response.totalSuccessful())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_TOTAL_FAILED, response.totalFailed())
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, response.roleId().toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_ROLE_NAME, response.roleName())
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_REVOCATION_SUCCESS)
                .log();
            
            // Send revocation notifications for successfully revoked users
            if (!response.successfulRevocations().isEmpty()) {
                try {
                    sendRoleRevocationNotifications(response.roleId(), response.successfulRevocations(), tenantId);
                } catch (Exception e) {
                    // Log error but don't fail the request
                    logger.error(e)
                        .message(AppConstants.USER_ROLE_LOG_REVOKE_NOTIFICATION_SEND_FAILED)
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_KEY_ROLE_ID, response.roleId().toString())
                        .detail(AppConstants.USER_ROLE_DETAIL_KEY_ERROR, e.getMessage())
                        .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                        .log();
                }
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error()
                .message(AppConstants.USER_ROLE_LOG_REVOCATION_FAILED_INVALID_ARGS)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_USER_COUNT, request.getUserCount())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_REVOCATION_INVALID_ARGS)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, e.getMessage());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.BAD_REQUEST.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.error()
                .message(AppConstants.USER_ROLE_LOG_REVOCATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, request.roleId().toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_USER_COUNT, request.getUserCount())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_REVOCATION_ERROR)
                .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_MESSAGE, AppConstants.USER_ROLE_ERROR_REVOCATION_FAILED);
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Sends role assignment notifications via Kafka for successfully assigned users only.
     * 
     * @param roleId the role ID that was assigned
     * @param successfulUserIds the list of user IDs who successfully received the role
     * @param tenantId the tenant ID
     */
    private void sendRoleAssignmentNotifications(UUID roleId, List<UUID> successfulUserIds, String tenantId) {
        try {
            logger.debug()
                .message(AppConstants.USER_ROLE_LOG_PREPARING_NOTIFICATIONS)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_SUCCESSFUL_USER_COUNT, successfulUserIds.size())
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                .log();
            
            // Get role information
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.USER_ROLE_LOG_ROLE_NOT_FOUND_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                    .log();
                return;
            }
            
            Role role = roleOpt.get();
            String roleName = role.getRoleName();
            
            logger.debug()
                .message(AppConstants.USER_ROLE_LOG_ROLE_INFO_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_ROLE_NAME, roleName)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                .log();
            
            // Get all users in the tenant
            List<UserInfo> allUsers = userGrpcClient.getUserListByTenant(tenantId);
            Map<String, UserInfo> userMap = new HashMap<>();
            for (UserInfo user : allUsers) {
                userMap.put(user.getId(), user);
            }
            
            logger.debug()
                .message(AppConstants.USER_ROLE_LOG_USER_DATA_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_TOTAL_USERS_IN_TENANT, allUsers.size())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_SUCCESSFUL_USER_COUNT, successfulUserIds.size())
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                .log();
            
            // Create notification data for each successfully assigned user only
            List<NotificationEventData.UserNotificationData> notificationDataList = new ArrayList<>();
            
            for (UUID userId : successfulUserIds) {
                UserInfo userInfo = userMap.get(userId.toString());
                if (userInfo != null) {
                    NotificationEventData.UserData userData = new NotificationEventData.UserData(
                        userInfo.getEmail(), 
                        userInfo.getUsername()
                    );
                    
                    NotificationEventData.TemplateVariables templateVariables = 
                        new NotificationEventData.TemplateVariables(
                            userInfo.getUsername(),
                            userInfo.getEmail(),
                            roleName
                        );
                    
                    NotificationEventData.UserNotificationData userNotificationData = 
                        new NotificationEventData.UserNotificationData(
                            userId.toString(),
                            userData,
                            templateVariables
                        );
                    
                    notificationDataList.add(userNotificationData);
                    
                    logger.trace()
                        .message(AppConstants.USER_ROLE_LOG_NOTIFICATION_DATA_CREATED)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.USER_ROLE_DETAIL_KEY_USERNAME, userInfo.getUsername())
                        .detail(AppConstants.USER_ROLE_DETAIL_KEY_EMAIL, userInfo.getEmail())
                        .detail(AppConstants.USER_ROLE_DETAIL_KEY_ROLE_NAME, roleName)
                        .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                        .log();
                } else {
                    logger.warn()
                        .message(AppConstants.USER_ROLE_LOG_USER_NOT_FOUND_NOTIFICATION)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                        .log();
                }
            }
            
            if (!notificationDataList.isEmpty()) {
                // Create and send notification event
                NotificationEventData eventData = new NotificationEventData(
                    NotificationEventData.OperationType.ASSIGN_INTERNAL_ROLE,
                    tenantId,
                    notificationDataList
                );
                
                logger.debug()
                    .message(AppConstants.USER_ROLE_LOG_SENDING_NOTIFICATION_EVENT)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_ROLE_NAME, roleName)
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_OPERATION_TYPE, NotificationEventData.OperationType.ASSIGN_INTERNAL_ROLE.name())
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_USER_COUNT, notificationDataList.size())
                    .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                    .log();
                
                notificationKafkaService.sendNotificationEvent(eventData, tenantId);
                
                logger.info()
                    .message(AppConstants.USER_ROLE_LOG_NOTIFICATION_SENT_SUCCESS)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_ROLE_NAME, roleName)
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_USER_COUNT, notificationDataList.size())
                    .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                    .log();
            } else {
                logger.warn()
                    .message(AppConstants.USER_ROLE_LOG_NO_VALID_USERS_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                    .log();
            }
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.USER_ROLE_LOG_NOTIFICATION_SEND_FAILED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                .log();
            throw e;
        }
    }
    
    /**
     * Sends role revocation notifications via Kafka for successfully revoked users only.
     * This method is called after successful role revocations to notify affected users.
     * 
     * @param roleId the ID of the role that was revoked
     * @param successfulUserIds list of user IDs whose roles were successfully revoked
     * @param tenantId the tenant ID for multi-tenant isolation
     * @throws Exception if there's an error sending notifications
     */
    private void sendRoleRevocationNotifications(UUID roleId, List<UUID> successfulUserIds, String tenantId) throws Exception {
        try {
            logger.info()
                .message(AppConstants.USER_ROLE_LOG_PREPARING_REVOKE_NOTIFICATIONS)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_SUCCESSFUL_USER_COUNT, successfulUserIds.size())
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                .log();
            
            // Get role information
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.USER_ROLE_LOG_ROLE_NOT_FOUND_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                    .log();
                return;
            }
            
            Role role = roleOpt.get();
            String roleName = role.getRoleName();
            
            logger.debug()
                .message(AppConstants.USER_ROLE_LOG_ROLE_INFO_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_ROLE_NAME, roleName)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                .log();
            
            // Get all users in the tenant
            List<UserInfo> allUsers = userGrpcClient.getUserListByTenant(tenantId);
            Map<String, UserInfo> userMap = new HashMap<>();
            for (UserInfo user : allUsers) {
                userMap.put(user.getId(), user);
            }
            
            logger.debug()
                .message(AppConstants.USER_ROLE_LOG_USER_DATA_RETRIEVED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_TOTAL_USERS_IN_TENANT, allUsers.size())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_SUCCESSFUL_USER_COUNT, successfulUserIds.size())
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                .log();
            
            // Create notification data for each successfully revoked user only
            List<NotificationEventData.UserNotificationData> notificationDataList = new ArrayList<>();
            
            for (UUID userId : successfulUserIds) {
                UserInfo userInfo = userMap.get(userId.toString());
                if (userInfo != null) {
                    NotificationEventData.UserData userData = new NotificationEventData.UserData(
                        userInfo.getEmail(), 
                        userInfo.getUsername()
                    );
                    
                    NotificationEventData.TemplateVariables templateVariables = 
                        new NotificationEventData.TemplateVariables(
                            userInfo.getUsername(),
                            userInfo.getEmail(),
                            roleName
                        );
                    
                    NotificationEventData.UserNotificationData userNotificationData = 
                        new NotificationEventData.UserNotificationData(
                            userId.toString(),
                            userData,
                            templateVariables
                        );
                    
                    notificationDataList.add(userNotificationData);
                    
                    logger.trace()
                        .message(AppConstants.USER_ROLE_LOG_REVOKE_NOTIFICATION_DATA_CREATED)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.USER_ROLE_DETAIL_KEY_USERNAME, userInfo.getUsername())
                        .detail(AppConstants.USER_ROLE_DETAIL_KEY_EMAIL, userInfo.getEmail())
                        .detail(AppConstants.USER_ROLE_DETAIL_KEY_ROLE_NAME, roleName)
                        .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                        .log();
                } else {
                    logger.warn()
                        .message(AppConstants.USER_ROLE_LOG_USER_NOT_FOUND_NOTIFICATION)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId.toString())
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                        .log();
                }
            }
            
            if (!notificationDataList.isEmpty()) {
                // Create and send notification event
                NotificationEventData eventData = new NotificationEventData(
                    NotificationEventData.OperationType.REVOKE_INTERNAL_ROLE,
                    tenantId,
                    notificationDataList
                );
                
                logger.debug()
                    .message(AppConstants.USER_ROLE_LOG_SENDING_REVOKE_NOTIFICATION_EVENT)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_ROLE_NAME, roleName)
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_OPERATION_TYPE, NotificationEventData.OperationType.REVOKE_INTERNAL_ROLE.name())
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_USER_COUNT, notificationDataList.size())
                    .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                    .log();
                
                notificationKafkaService.sendNotificationEvent(eventData, tenantId);
                
                logger.info()
                    .message(AppConstants.USER_ROLE_LOG_REVOKE_NOTIFICATION_SENT_SUCCESS)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_ROLE_NAME, roleName)
                    .detail(AppConstants.USER_ROLE_DETAIL_KEY_USER_COUNT, notificationDataList.size())
                    .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                    .log();
            } else {
                logger.warn()
                    .message(AppConstants.USER_ROLE_LOG_NO_VALID_USERS_REVOKE_NOTIFICATION)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                    .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                    .log();
            }
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.USER_ROLE_LOG_REVOKE_NOTIFICATION_SEND_FAILED)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_ROLE_ID, roleId.toString())
                .detail(AppConstants.USER_ROLE_DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.USER_ROLE_OBJECT_TYPE_NOTIFICATION)
                .log();
            throw e;
        }
    }    
    /**
     * Submits user role assignment request for approval workflow.
     * 
     * @param request the approval submission request
     * @return success status and message
     */
    @Operation(
        summary = "Submit user role assignment for approval",
        description = "Submits a user role assignment request through an approval workflow. The actual assignment will occur after workflow approval."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Approval request submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Role not found or does not belong to tenant")
    })
    @PostMapping("/submit-assignment")
    public ResponseEntity<Map<String, Object>> submitUserRoleAssignmentForApproval(
            @Valid @RequestBody SubmitUserRoleAssignmentForApprovalRequest request) {
        
        String tenantId = tenantContextService.getCurrentTenantId();
        
        logger.info()
            .message("User role assignment approval submission endpoint called")
            .detail("tenantId", tenantId)
            .detail("roleId", request.getRoleId().toString())
            .detail("userCount", String.valueOf(request.getUserIds().size()))
            .detail("workflowId", request.getWorkflowId())
            .objectType("UserRoleApprovalSubmissionEndpoint")
            .log();
        
        Map<String, Object> response = userRoleService.submitUserRoleAssignmentForApproval(request);
        
        logger.info()
            .message("User role assignment approval submission completed")
            .detail("tenantId", tenantId)
            .detail("roleId", request.getRoleId().toString())
            .detail("userCount", String.valueOf(request.getUserIds().size()))
            .detail("success", response.get("success").toString())
            .objectType("UserRoleApprovalSubmissionEndpoint")
            .log();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Submits user role revocation request for approval workflow.
     * 
     * @param request the approval submission request
     * @return success status and message
     */
    @Operation(
        summary = "Submit user role revocation for approval",
        description = "Submits a user role revocation request through an approval workflow. The actual revocation will occur after workflow approval."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Approval request submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Role not found or does not belong to tenant")
    })
    @PostMapping("/submit-revocation")
    public ResponseEntity<Map<String, Object>> submitUserRoleRevocationForApproval(
            @Valid @RequestBody SubmitUserRoleRevocationForApprovalRequest request) {
        
        String tenantId = tenantContextService.getCurrentTenantId();
        
        logger.info()
            .message("User role revocation approval submission endpoint called")
            .detail("tenantId", tenantId)
            .detail("roleId", request.getRoleId().toString())
            .detail("userCount", String.valueOf(request.getUserIds().size()))
            .detail("workflowId", request.getWorkflowId())
            .objectType("UserRoleRevocationApprovalSubmissionEndpoint")
            .log();
        
        Map<String, Object> response = userRoleService.submitUserRoleRevocationForApproval(request);
        
        logger.info()
            .message("User role revocation approval submission completed")
            .detail("tenantId", tenantId)
            .detail("roleId", request.getRoleId().toString())
            .detail("userCount", String.valueOf(request.getUserIds().size()))
            .detail("success", response.get("success").toString())
            .objectType("UserRoleRevocationApprovalSubmissionEndpoint")
            .log();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Assigns multiple roles to multiple users from access portal cart.
     *
     * <p>This endpoint supports cart-based role assignments where each role can have:
     * <ul>
     *   <li>Different users assigned to it</li>
     *   <li>Role-specific justification (or use overall justification)</li>
     *   <li>Role-specific validity period (or use overall validity)</li>
     * </ul>
     * </p>
     *
     * <p><b>Processing Strategy:</b></p>
     * <ul>
     *   <li>Small batches (<1000 assignments): Synchronous processing with immediate results</li>
     *   <li>Large batches (≥1000 assignments): Asynchronous processing (TODO: NOT IMPLEMENTED YET)</li>
     * </ul>
     *
     * <p><b>Example Use Case - Access Portal Cart:</b></p>
     * <pre>
     * User adds multiple roles to cart:
     * - AdminRole for [User1, User2, User3] - "Need admin access", 30 days
     * - ViewerRole for [User1, User4] - "Read-only access", 60 days
     * - EditorRole for [User5] - "Content editing", 90 days
     *
     * Then submits the cart for approval or immediate assignment.
     * </pre>
     *
     * @param request the cart-based assignment request containing role-user mappings with details
     * @return ResponseEntity with assignment results (successful, skipped, failed)
     */
    @Operation(
            summary = "Assign roles from access portal cart",
            description = "Assigns multiple roles to multiple users based on cart selections from access portal. " +
                    "Each role can have different users, different justifications, and different validity periods. " +
                    "Processes synchronously for batches with <1000 assignments.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Cart-based role assignment request with per-role details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AssignRolesFromCartRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Simple Cart Assignment",
                                            value = """
                        {
                            "roleUserMap": {
                                "64aedcba-f440-4b4c-b549-1083ed12070b": {
                                    "userIds": [
                                        "123e4567-e89b-12d3-a456-426614174000",
                                        "234e5678-e89b-12d3-a456-426614174001"
                                    ],
                                    "justification": "Need admin access for Q1 projects",
                                    "validityDays": 30
                                },
                                "75bfedcb-f550-5c5d-c660-2194fe23181c": {
                                    "userIds": [
                                        "123e4567-e89b-12d3-a456-426614174000",
                                        "345e6789-e89b-12d3-a456-426614174002"
                                    ],
                                    "justification": "Read-only access for reporting",
                                    "validityDays": 60
                                }
                            },
                            "overallJustification": "Q1 team setup",
                            "overallValidityDays": 90,
                            "shouldSendNotification": true
                        }
                        """
                                    ),
                                    @ExampleObject(
                                            name = "Cart with Overall Fallbacks",
                                            value = """
                        {
                            "roleUserMap": {
                                "64aedcba-f440-4b4c-b549-1083ed12070b": {
                                    "userIds": [
                                        "123e4567-e89b-12d3-a456-426614174000"
                                    ]
                                },
                                "75bfedcb-f550-5c5d-c660-2194fe23181c": {
                                    "userIds": [
                                        "234e5678-e89b-12d3-a456-426614174001",
                                        "345e6789-e89b-12d3-a456-426614174002"
                                    ],
                                    "justification": "Specific justification for this role"
                                }
                            },
                            "overallJustification": "Default justification for all roles",
                            "overallValidityDays": 90,
                            "shouldSendNotification": false
                        }
                        """
                                    ),
                                    @ExampleObject(
                                            name = "Multi-Role Cart",
                                            value = """
                        {
                            "roleUserMap": {
                                "role-1-uuid": {
                                    "userIds": ["user-1", "user-2", "user-3"],
                                    "justification": "Admin team access",
                                    "validityDays": 30
                                },
                                "role-2-uuid": {
                                    "userIds": ["user-1", "user-4"],
                                    "validityDays": 60
                                },
                                "role-3-uuid": {
                                    "userIds": ["user-5"],
                                    "justification": "Temporary contractor access"
                                }
                            },
                            "overallJustification": "Q1 2024 team structure",
                            "overallValidityDays": 90,
                            "shouldSendNotification": true
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
                    description = "Cart assignment completed successfully (may include skipped/failed items)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CartBasedAssignmentResponse.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                        "success": true,
                        "message": "Cart-based role assignments processed successfully",
                        "status": "OK",
                        "data": {
                            "batchId": null,
                            "status": "COMPLETED",
                            "totalAssignments": 5,
                            "successfulAssignments": 4,
                            "skippedAssignments": 1,
                            "failedAssignments": 0,
                            "successByRole": {
                                "64aedcba-f440-4b4c-b549-1083ed12070b": 2,
                                "75bfedcb-f550-5c5d-c660-2194fe23181c": 2
                            },
                            "failuresByRole": {},
                            "processedAt": "2024-01-23T10:30:00",
                            "estimatedCompletionTime": null,
                            "trackingUrl": null
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
                                    value = """
                    {
                        "success": false,
                        "message": "Role-user map cannot be empty",
                        "status": "BAD_REQUEST",
                        "timestamp": "2024-01-23T10:30:00"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "One or more roles not found or do not belong to tenant",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                        "success": false,
                        "message": "Roles not found: [64aedcba-f440-4b4c-b549-1083ed12070b]",
                        "status": "NOT_FOUND",
                        "timestamp": "2024-01-23T10:30:00"
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
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                        "success": false,
                        "message": "Failed to process cart-based role assignments",
                        "error": "Database connection error",
                        "status": "INTERNAL_SERVER_ERROR",
                        "timestamp": "2024-01-23T10:30:00"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "501",
                    description = "Async processing not yet implemented (for batches ≥1000)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                        "success": false,
                        "message": "Async processing for batches ≥1000 assignments is not yet implemented",
                        "status": "NOT_IMPLEMENTED",
                        "timestamp": "2024-01-23T10:30:00"
                    }
                    """
                            )
                    )
            )
    })
    @PostMapping("/assign-from-cart")
    public ResponseEntity<Map<String, Object>> assignRolesFromCart(
            @Valid @RequestBody AssignRolesFromCartRequest request) {

        logger.trace()
                .message(AppConstants.LOG_TRACE_ENTERING_ASSIGN_ROLE_METHOD)
                .detail(AppConstants.DETAIL_KEY_METHOD_NAME, "assignRolesFromCart")
                .objectType("CartBasedAssignmentEndpoint")
                .log();

        logger.trace()
                .message(AppConstants.LOG_TRACE_REQUEST_PARAMETERS)
                .detail("roleCount", String.valueOf(request.getRoleCount()))
                .detail("uniqueUserCount", String.valueOf(request.getUniqueUserCount()))
                .detail("totalAssignments", String.valueOf(request.getTotalAssignments()))
                .detail("shouldSendNotification", String.valueOf(request.shouldSendNotification()))
                .objectType("CartBasedAssignmentEndpoint")
                .log();

        try {
            // Extract tenant ID from JWT token
            String tenantId = tenantContextService.getCurrentTenantId();

            logger.debug()
                    .message("Starting cart-based role assignment")
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail("roleCount", String.valueOf(request.getRoleCount()))
                    .detail("uniqueUserCount", String.valueOf(request.getUniqueUserCount()))
                    .detail("totalAssignments", String.valueOf(request.getTotalAssignments()))
                    .objectType("CartBasedAssignmentEndpoint")
                    .log();

            logger.debug()
                    .message("Validating cart-based assignment request")
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType("CartBasedAssignmentEndpoint")
                    .log();

            logger.debug()
                    .message(AppConstants.LOG_DEBUG_TENANT_CONTEXT_EXTRACTED)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType("CartBasedAssignmentEndpoint")
                    .log();

            logger.info()
                    .message("Cart-based role assignment endpoint called")
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail("roleCount", String.valueOf(request.getRoleCount()))
                    .detail("uniqueUserCount", String.valueOf(request.getUniqueUserCount()))
                    .detail("totalAssignments", String.valueOf(request.getTotalAssignments()))
                    .detail("overallJustification", request.overallJustification())
                    .objectType("CartBasedAssignmentEndpoint")
                    .log();

            logger.debug()
                    .message(AppConstants.LOG_DEBUG_CALLING_SERVICE_METHOD)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .objectType("CartBasedAssignmentEndpoint")
                    .log();

            // Call service layer
            CartBasedAssignmentResponse response = userRoleService.assignRolesFromCart(request);

            logger.debug()
                    .message(AppConstants.LOG_DEBUG_SERVICE_METHOD_COMPLETED)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail("status", response.status())
                    .detail("successfulAssignments", String.valueOf(response.successfulAssignments()))
                    .detail("skippedAssignments", String.valueOf(response.skippedAssignments()))
                    .detail("failedAssignments", String.valueOf(response.failedAssignments()))
                    .objectType("CartBasedAssignmentSuccess")
                    .log();

            // Send notifications if requested and there are successful assignments
            if (request.shouldSendNotification() && response.successfulAssignments() > 0) {
                try {
                    // TODO: Implement notification logic for cart-based assignments
                    // For now, just log that notifications should be sent
                    logger.debug()
                            .message("Notifications requested for cart-based assignments")
                            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                            .detail("successfulAssignments", String.valueOf(response.successfulAssignments()))
                            .detail("notificationNote", "TODO: Implement per-role notification logic")
                            .objectType("CartBasedAssignmentEndpoint")
                            .log();

                    // When implementing notifications, iterate through successByRole
                    // and send notifications per role with role-specific details

                } catch (Exception notificationException) {
                    // Log notification failure but don't fail the main operation
                    logger.warn()
                            .message("Failed to send notifications for cart-based assignments")
                            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                            .detail("error", notificationException.getMessage())
                            .objectType("CartBasedAssignmentEndpoint")
                            .log();
                }
            }

            // Build success response
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Cart-based role assignments processed successfully");
            successResponse.put("status", HttpStatus.OK.name());
            successResponse.put("data", response);

            logger.trace()
                    .message(AppConstants.LOG_TRACE_RESPONSE_DATA)
                    .detail("response", successResponse.toString())
                    .objectType("CartBasedAssignmentSuccess")
                    .log();

            logger.info()
                    .message("Cart-based role assignment completed successfully")
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail("totalAssignments", String.valueOf(response.totalAssignments()))
                    .detail("successfulAssignments", String.valueOf(response.successfulAssignments()))
                    .detail("skippedAssignments", String.valueOf(response.skippedAssignments()))
                    .detail("failedAssignments", String.valueOf(response.failedAssignments()))
                    .objectType("CartBasedAssignmentSuccess")
                    .log();

            logger.trace()
                    .message(AppConstants.LOG_TRACE_EXITING_ASSIGN_ROLE_METHOD)
                    .detail(AppConstants.DETAIL_KEY_METHOD_NAME, "assignRolesFromCart")
                    .objectType("CartBasedAssignmentSuccess")
                    .log();

            return ResponseEntity.ok(successResponse);

        } catch (UnsupportedOperationException e) {
            // Handle async not implemented error
            logger.warn()
                    .message("Async processing requested but not yet implemented")
                    .detail("totalAssignments", String.valueOf(request.getTotalAssignments()))
                    .detail("error", e.getMessage())
                    .objectType("CartBasedAssignmentNotImplemented")
                    .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", HttpStatus.NOT_IMPLEMENTED.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);

        } catch (IllegalArgumentException e) {
            // Handle validation errors
            logger.debug()
                    .message("Validation error in cart-based assignment")
                    .detail("error", e.getMessage())
                    .objectType("CartBasedAssignmentValidationError")
                    .log();

            logger.error()
                    .message("Cart-based role assignment failed - validation error")
                    .detail("roleCount", String.valueOf(request.getRoleCount()))
                    .detail("error", e.getMessage())
                    .objectType("CartBasedAssignmentValidationError")
                    .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", HttpStatus.BAD_REQUEST.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            // Handle unexpected errors
            String errorMessage = e.getMessage() != null ? e.getMessage() :
                    "Exception thrown without message: " + e.getClass().getSimpleName();

            logger.debug()
                    .message("Exception caught in cart-based assignment")
                    .detail("error", errorMessage)
                    .detail("exceptionType", e.getClass().getSimpleName())
                    .objectType("CartBasedAssignmentError")
                    .log();

            logger.trace()
                    .message("Exception details")
                    .detail("error", errorMessage)
                    .detail("exceptionType", e.getClass().getSimpleName())
                    .detail("stackTrace", java.util.Arrays.toString(e.getStackTrace()))
                    .objectType("CartBasedAssignmentError")
                    .log();

            logger.error()
                    .message("Cart-based role assignment failed")
                    .detail("roleCount", String.valueOf(request.getRoleCount()))
                    .detail("error", errorMessage)
                    .detail("exceptionType", e.getClass().getSimpleName())
                    .objectType("CartBasedAssignmentError")
                    .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to process cart-based role assignments");
            errorResponse.put("error", errorMessage);
            errorResponse.put("exceptionType", e.getClass().getSimpleName());
            errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.name());
            errorResponse.put(AppConstants.USER_ROLE_RESPONSE_KEY_TIMESTAMP, LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Assigns multiple roles to multiple users (M×N assignment).
     *
     * <p><b>Example Request:</b></p>
     * <pre>
     * {
     *   "roleIds": ["role-1", "role-2", "role-3"],
     *   "userIds": ["user-1", "user-2", "user-3"],
     *   "justification": "Q1 team setup",
     *   "validityDays": 90,
     *   "processAsync": true
     * }
     * </pre>
     *
     * @param request the bulk multi-role assignment request
     * @return ResponseEntity with batch job details
     */
    @PostMapping("/bulk-multi-assign")
    public ResponseEntity<Map<String, Object>> assignMultipleRolesToMultipleUsers(
            @Valid @RequestBody BulkMultiRoleAssignmentRequest request) {

        logger.info()
                .message("Bulk multi-role assignment endpoint called")
                .detail("roleCount", String.valueOf(request.roleIds().size()))
                .detail("userCount", String.valueOf(request.userIds().size()))
                .detail("totalAssignments", String.valueOf(request.getTotalAssignments()))
                .objectType("BulkMultiRoleAssignmentEndpoint")
                .log();

        try {
            BulkMultiRoleAssignmentResponse response =
                    userRoleService.assignMultipleRolesToMultipleUsers(request);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Batch assignment initiated successfully");
            successResponse.put("data", response);

            return ResponseEntity.accepted().body(successResponse);

        } catch (IllegalArgumentException e) {
            logger.error()
                    .message("Validation error in bulk multi-role assignment")
                    .detail("error", e.getMessage())
                    .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to process bulk multi-role assignment")
                    .detail("error", e.getMessage())
                    .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Gets the status of a batch assignment job.
     *
     * @param batchId the batch job UUID
     * @return ResponseEntity with batch job status
     */
    @GetMapping("/batch-status/{batchId}")
    public ResponseEntity<Map<String, Object>> getBatchJobStatus(@PathVariable UUID batchId) {

        logger.debug()
                .message("Fetching batch job status")
                .detail("batchId", batchId.toString())
                .log();

        try {
            BatchJobResponse response = userRoleService.getBatchJobStatus(batchId);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("data", response);

            return ResponseEntity.ok(successResponse);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Batch job not found");

            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to fetch batch job status")
                    .detail("batchId", batchId.toString())
                    .log();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}