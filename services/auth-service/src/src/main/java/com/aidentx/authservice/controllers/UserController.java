package com.aidentx.authservice.controllers;

import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for user management endpoints.
 * Provides endpoints for retrieving user information.
 *
 * @author aidentx Development Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;
    private final com.aidentx.authservice.repository.UserSessionRepository userSessionRepository;

    public UserController(UserRepository userRepository, 
                         com.aidentx.authservice.repository.UserSessionRepository userSessionRepository) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
    }

    /**
     * Get user by user ID.
     * Used by other services to fetch user details.
     *
     * @param userId User ID
     * @return Response containing user details
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "User not found for user_id: " + userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("identityId", user.getIdentityId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("displayName", user.getDisplayName());
        response.put("userStatus", user.getStatus() != null ? user.getStatus().name() : null);
        response.put("lastLoginAt", user.getLastLoginAt());
        response.put("mfaEnabled", user.getMfaEnabled() != null ? user.getMfaEnabled() : false);
        response.put("failedLoginAttempts", user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0);
        response.put("createdAt", user.getCreatedAt());
        response.put("tenantId", user.getTenantId());
        response.put("status", "OK");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get user ID by identity ID.
     * Used by other services to map identity_id to user_id.
     *
     * @param identityId Identity ID from identity-service
     * @return Response containing user_id and identity_id
     */
    @GetMapping("/by-identity/{identityId}")
    public ResponseEntity<Map<String, Object>> getUserByIdentityId(@PathVariable UUID identityId) {
        Optional<User> userOpt = userRepository.findByIdentityId(identityId);
        
        if (userOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "User not found for identity_id: " + identityId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("identityId", user.getIdentityId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("displayName", user.getDisplayName());
        response.put("userStatus", user.getStatus() != null ? user.getStatus().name() : null);
        response.put("lastLoginAt", user.getLastLoginAt());
        response.put("mfaEnabled", user.getMfaEnabled() != null ? user.getMfaEnabled() : false);
        response.put("failedLoginAttempts", user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0);
        response.put("createdAt", user.getCreatedAt());
        
        // Count active sessions
        int activeSessions = userSessionRepository.findByUserId(user.getId()).size();
        response.put("activeSessions", activeSessions);
        response.put("status", "OK");
        
        return ResponseEntity.ok(response);
    }
}
