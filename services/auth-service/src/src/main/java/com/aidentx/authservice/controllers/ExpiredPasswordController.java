package com.aidentx.authservice.controllers;

import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.authservice.dto.request.UpdateExpiredPasswordRequest;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.service.UpdateExpiredPasswordService;
import com.aidentx.authservice.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for expired password management endpoints.
 * <p>
 * This controller handles operations specifically related to updating expired passwords,
 * including dynamic validation based on tenant password policies and proper tracking
 * of password expiration fields.
 * </p>
 *
 * @author Aidentx Development Team
 * @version 1.0
 * @since 2025-09-11
 */
@RestController
@RequestMapping("/api/v1/auth/expired-password")
public class ExpiredPasswordController {

    private final UpdateExpiredPasswordService updateExpiredPasswordService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public ExpiredPasswordController(UpdateExpiredPasswordService updateExpiredPasswordService,
                                   JwtUtil jwtUtil,
                                   UserRepository userRepository) {
        this.updateExpiredPasswordService = updateExpiredPasswordService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    /**
     * Updates an expired password for the authenticated user.
     * <p>
     * This endpoint specifically handles password updates for users with expired passwords.
     * It validates the new password against the tenant's current password policy.
     * </p>
     *
     * @param request the password update request
     * @param httpRequest the HTTP request to extract JWT token
     * @return ResponseEntity containing validation results and success/error messages
     */
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateExpiredPassword(
            @RequestBody @Valid UpdateExpiredPasswordRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Extract user ID and tenant ID from JWT token
            String token = extractTokenFromRequest(httpRequest);
            if (token == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", AppConstants.EXPIRED_PASSWORD_INVALID_TOKEN_MSG);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String userId = jwtUtil.extractUserId(token);
            String tenantId = jwtUtil.extractTenantId(token);
            
            if (userId == null || tenantId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", AppConstants.EXPIRED_PASSWORD_TOKEN_EXTRACTION_ERROR_MSG);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Update expired password with dynamic validation
            Map<String, Object> result = updateExpiredPasswordService.updateExpiredPassword(
                userId, 
                tenantId, 
                request.getCurrentPassword(), 
                request.getNewPassword(), 
                request.getConfirmPassword()
            );
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", AppConstants.EXPIRED_PASSWORD_INTERNAL_ERROR_MSG);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Checks if passwords are expired for the authenticated user's tenant.
     * <p>
     * This endpoint extracts the tenant ID from JWT and checks if passwords
     * are expired based on the password policy's policyExpiresAt field.
     * </p>
     *
     * @param httpRequest the HTTP request to extract JWT token
     * @return ResponseEntity containing expiration status
     */
    @GetMapping("/check-expiration")
    public ResponseEntity<?> checkPasswordExpiration(HttpServletRequest httpRequest) {
        try {
            // Extract tenant ID and user ID from JWT token
            String token = extractTokenFromRequest(httpRequest);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", AppConstants.EXPIRED_PASSWORD_INVALID_TOKEN_MSG));
            }

            String tenantId = jwtUtil.extractTenantId(token);
            String userId = jwtUtil.extractUserId(token);
            
            if (tenantId == null || userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", AppConstants.EXPIRED_PASSWORD_TOKEN_EXTRACTION_ERROR_MSG));
            }

            // Get the user and check their individual password expiration status
            Optional<User> userOpt = userRepository.findById(UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", AppConstants.EXPIRED_PASSWORD_USER_NOT_FOUND_MSG));
            }
            
            User user = userOpt.get();
            boolean isExpired = user.getIsPasswordExpired() != null && user.getIsPasswordExpired();
            
            return ResponseEntity.ok(Map.of(
                "isExpired", isExpired,
                "userId", userId,
                "tenantId", tenantId
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", AppConstants.EXPIRED_PASSWORD_INTERNAL_ERROR_MSG));
        }
    }

    /**
     * Gets detailed password expiration information for debugging.
     * <p>
     * This endpoint provides comprehensive information about password expiration
     * including timestamps, policy information, and calculation details for the tenant.
     * </p>
     *
     * @param httpRequest the HTTP request to extract JWT token
     * @return ResponseEntity containing detailed expiration information
     */
    @GetMapping("/debug-expiration")
    public ResponseEntity<?> debugPasswordExpiration(HttpServletRequest httpRequest) {
        try {
            // Extract tenant ID from JWT token
            String token = extractTokenFromRequest(httpRequest);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", AppConstants.EXPIRED_PASSWORD_INVALID_TOKEN_MSG));
            }

            String tenantId = jwtUtil.extractTenantId(token);
            if (tenantId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", AppConstants.EXPIRED_PASSWORD_TOKEN_EXTRACTION_ERROR_MSG));
            }

            // Get comprehensive expiration information for tenant
            Map<String, Object> expirationInfo = updateExpiredPasswordService.getPasswordExpirationInfo(tenantId);
            
            return ResponseEntity.ok(expirationInfo);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", AppConstants.EXPIRED_PASSWORD_INTERNAL_ERROR_MSG, "details", e.getMessage()));
        }
    }

    /**
     * Extracts JWT token from the Authorization header.
     *
     * @param request the HTTP request
     * @return the JWT token without "Bearer " prefix, or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
