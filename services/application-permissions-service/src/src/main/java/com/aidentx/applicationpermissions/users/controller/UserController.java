package com.aidentx.applicationpermissions.users.controller;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.shared.grpc.client.UserGrpcClient;
import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;
import com.aidentx.applicationpermissions.shared.grpc.dto.UsersResponse;
import com.aidentx.applicationpermissions.shared.security.JwtUtil;
import com.aidentx.applicationpermissions.shared.security.UserContextHolder;
import com.aidentx.applicationpermissions.shared.security.UserContext;
import com.aidentx.auth.grpc.UserInfo;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for user-related operations.
 * Provides endpoints to retrieve users from auth-service via gRPC.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "Operations for retrieving user information from auth-service")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserGrpcClient userGrpcClient;
    private final JwtUtil jwtUtil;
    private final IUnifiedLogger logger;

    @Autowired
    public UserController(UserGrpcClient userGrpcClient, JwtUtil jwtUtil, UnifiedLoggerFactory loggerFactory) {
        this.userGrpcClient = userGrpcClient;
        this.jwtUtil = jwtUtil;
        this.logger = loggerFactory.getLogger(AppConstants.USER_CONTROLLER_LOGGER_NAME, AppConstants.LOG_CATEGORY_USER_CONTROLLER);
    }

    /**
     * Retrieves all users belonging to the same tenant as the authenticated user.
     * The tenant ID is extracted from the JWT token automatically.
     * 
     * @param request HTTP servlet request to extract JWT token
     * @return ResponseEntity containing the list of users for the tenant
     */
    @GetMapping("/tenant")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get users by tenant",
        description = "Retrieves all users belonging to the same tenant as the authenticated user. " +
                     "The tenant ID is automatically extracted from the JWT token."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing JWT token"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or expired token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UsersResponse> getUsersByTenant(HttpServletRequest request) {
        logger.info()
            .message(AppConstants.LOG_USER_RETRIEVAL_ENDPOINT_CALLED)
            .details(Map.of(AppConstants.DETAIL_KEY_ENDPOINT, "GET /api/v1/users/tenant"))
            .log();

        try {
            // Extract tenant ID from JWT token
            String tenantId = extractTenantIdFromRequest(request);
            
            if (tenantId == null || tenantId.trim().isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_TENANT_ID_NOT_FOUND_IN_JWT)
                    .log();
                
                return ResponseEntity.badRequest()
                    .body(new UsersResponse(null, AppConstants.TENANT_ID_NOT_FOUND_IN_JWT_TOKEN));
            }

            logger.info()
                .message(AppConstants.LOG_PROCESSING_USER_RETRIEVAL_REQUEST)
                .details(Map.of(AppConstants.DETAIL_KEY_TENANT_ID, tenantId))
                .log();

            // Call gRPC service to get users
            List<UserInfo> grpcUsers = userGrpcClient.getUserListByTenant(tenantId);

            // Convert gRPC responses to DTOs
            List<UserDto> users = grpcUsers.stream()
                .map(this::convertToUserDto)
                .collect(Collectors.toList());

            UsersResponse response = new UsersResponse(users, users.size(), tenantId);

            logger.info()
                .message(AppConstants.LOG_USER_RETRIEVAL_SUCCESSFUL)
                .details(Map.of(
                    AppConstants.DETAIL_KEY_TENANT_ID, tenantId,
                    AppConstants.DETAIL_KEY_USER_COUNT, users.size(),
                    AppConstants.DETAIL_KEY_RESPONSE_STATUS, 200
                ))
                .log();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ERROR_RETRIEVING_USERS_BY_TENANT)
                .details(Map.of(AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                .log();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new UsersResponse(null, AppConstants.ERROR_RETRIEVING_USERS + ": " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint to test gRPC connectivity to auth-service.
     * 
     * @return ResponseEntity with connectivity status
     */
    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRATOR')")
    @Operation(
        summary = "Health check for auth-service connectivity",
        description = "Tests the gRPC connectivity to auth-service. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Auth-service is reachable"),
        @ApiResponse(responseCode = "503", description = "Auth-service is not reachable"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info()
            .message(AppConstants.LOG_HEALTH_CHECK_ENDPOINT_CALLED)
            .details(Map.of(AppConstants.DETAIL_KEY_ENDPOINT, "GET /api/v1/users/health"))
            .log();

        try {
            boolean isReachable = userGrpcClient.isAuthServiceReachable();

            Map<String, Object> response = Map.of(
                "authServiceReachable", isReachable,
                "status", isReachable ? "healthy" : "unhealthy",
                "timestamp", System.currentTimeMillis()
            );

            HttpStatus status = isReachable ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

            logger.info()
                .message(AppConstants.LOG_HEALTH_CHECK_COMPLETED)
                .details(Map.of(AppConstants.DETAIL_KEY_IS_REACHABLE, isReachable, AppConstants.DETAIL_KEY_STATUS, status.value()))
                .log();

            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_HEALTH_CHECK_FAILED)
                .details(Map.of(AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                .log();

            Map<String, Object> response = Map.of(
                "authServiceReachable", false,
                "status", "error",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    /**
     * Extracts tenant ID from the JWT token in the request.
     * 
     * @param request HTTP servlet request
     * @return tenant ID or null if not found
     */
    private String extractTenantIdFromRequest(HttpServletRequest request) {
        try {
            // First try to get from UserContext (set by JWT filter)
            UserContext userContext = UserContextHolder.getContext();
            if (userContext != null && userContext.tenantId() != null) {
                logger.debug()
                    .message(AppConstants.LOG_EXTRACTED_TENANT_ID_FROM_USER_CONTEXT)
                    .details(Map.of(AppConstants.DETAIL_KEY_TENANT_ID, userContext.tenantId()))
                    .log();
                return userContext.tenantId();
            }

            // Fallback: extract from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String tenantId = jwtUtil.extractTenantId(token);
                
                logger.debug()
                    .message(AppConstants.LOG_EXTRACTED_TENANT_ID_FROM_JWT_TOKEN)
                    .details(Map.of(AppConstants.DETAIL_KEY_TENANT_ID, tenantId))
                    .log();
                
                return tenantId;
            }

            logger.warn()
                .message(AppConstants.LOG_NO_AUTHORIZATION_HEADER_FOUND)
                .log();
            
            return null;

        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ERROR_EXTRACTING_TENANT_ID)
                .details(Map.of(AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                .log();
            return null;
        }
    }

    /**
     * Converts gRPC UserInfo to UserDto.
     * 
     * @param userInfo gRPC UserInfo object
     * @return UserDto object
     */
    private UserDto convertToUserDto(UserInfo userInfo) {
        return new UserDto(
            userInfo.getId(),
            userInfo.getUsername(),
            userInfo.getEmail(),
            userInfo.getFirstName(),
            userInfo.getLastName(),
            userInfo.getDisplayName(),
            userInfo.getStatus(),
            userInfo.getTenantId(),
            userInfo.getLastLoginAt(),
            userInfo.getPasswordChangedAt(),
            userInfo.getFailedLoginAttempts(),
            userInfo.getLockedUntil(),
            userInfo.getEmailVerifiedAt(),
            userInfo.getMfaEnabled(),
            userInfo.getIsPasswordExpired(),
            userInfo.getProfileData(),
            userInfo.getCreatedAt(),
            userInfo.getUpdatedAt(),
            userInfo.getCreatedBy(),
            userInfo.getUpdatedBy()
        );
    }
}