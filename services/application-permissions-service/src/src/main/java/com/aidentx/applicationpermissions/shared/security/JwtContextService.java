package com.aidentx.applicationpermissions.shared.security;

import org.springframework.stereotype.Service;

/**
 * Service for managing JWT context and token operations.
 * This service provides centralized JWT context management
 * for the application-permissions-service.
 */
@Service
public class JwtContextService {

    private final JwtUtil jwtUtil;
    private final TenantContextService tenantContextService;

    public JwtContextService(JwtUtil jwtUtil, TenantContextService tenantContextService) {
        this.jwtUtil = jwtUtil;
        this.tenantContextService = tenantContextService;
    }

    /**
     * Gets the current user ID from the JWT context.
     * 
     * @return the current user ID, or null if not available
     */
    public String getCurrentUserId() {
        UserContext userContext = UserContextHolder.getContext();
        return userContext != null ? userContext.userId() : null;
    }

    /**
     * Gets the current username from the JWT context.
     * 
     * @return the current username, or null if not available
     */
    public String getCurrentUsername() {
        UserContext userContext = UserContextHolder.getContext();
        return userContext != null ? userContext.username() : null;
    }

    /**
     * Gets the current tenant ID from the JWT context.
     * 
     * @return the current tenant ID, or null if not available
     */
    public String getCurrentTenantId() {
        return tenantContextService.getCurrentTenantId();
    }

    /**
     * Checks if the current user has access to the specified tenant.
     * 
     * @param tenantId the tenant ID to check
     * @return true if the user has access, false otherwise
     */
    public boolean hasAccessToTenant(String tenantId) {
        String currentTenantId = getCurrentTenantId();
        return currentTenantId != null && currentTenantId.equals(tenantId);
    }

    /**
     * Validates if a user context is currently set.
     * 
     * @return true if user context is available, false otherwise
     */
    public boolean isUserContextAvailable() {
        return UserContextHolder.getContext() != null;
    }
}