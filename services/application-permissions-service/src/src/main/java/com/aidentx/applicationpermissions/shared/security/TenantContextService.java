package com.aidentx.applicationpermissions.shared.security;

import com.aidentx.applicationpermissions.shared.SharedAppConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service for extracting tenant context from JWT tokens in HTTP requests.
 * Uses the existing JwtUtil to parse JWT tokens from Authorization headers.
 * 
 * @author Aidentx Development Team
 * @version 1.0
 * @since 2025-09-01
 */
@Service
public class TenantContextService {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public TenantContextService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Extracts the JWT token from the current HTTP request.
     * 
     * @return the JWT token without the "Bearer " prefix
     * @throws RuntimeException if no token is found or request context is not
     *                          available
     */
    private String extractTokenFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new RuntimeException(SharedAppConstants.MSG_NO_REQUEST_CONTEXT);
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new RuntimeException(SharedAppConstants.MSG_NO_JWT_TOKEN);
        }

        return authHeader.substring(BEARER_PREFIX.length());
    }

    /**
     * Gets the current user's tenant ID from the JWT token.
     * 
     * @return the tenant ID from the current JWT token
     * @throws RuntimeException if token parsing fails or tenantId is not found
     */
    public String getCurrentTenantId() {
        String token = extractTokenFromRequest();
        return jwtUtil.extractTenantId(token);
    }

    /**
     * Gets the current user's ID from the JWT token.
     * 
     * @return the user ID from the current JWT token
     * @throws RuntimeException if token parsing fails
     */
    public String getCurrentUserId() {
        String token = extractTokenFromRequest();
        return jwtUtil.extractUserId(token);
    }

    /**
     * Gets the current user's username from the JWT token.
     * 
     * @return the username from the current JWT token
     * @throws RuntimeException if token parsing fails
     */
    public String getCurrentUsername() {
        String token = extractTokenFromRequest();
        return jwtUtil.extractUsername(token);
    }

    /**
     * Validates that the current user belongs to the specified tenant.
     * 
     * @param requiredTenantId the tenant ID to validate against
     * @throws RuntimeException if the user's tenant doesn't match the required
     *                          tenant
     */
    public void validateTenantAccess(String requiredTenantId) {
        String currentTenantId = getCurrentTenantId();
        if (!currentTenantId.equals(requiredTenantId)) {
            throw new RuntimeException(
                    String.format(SharedAppConstants.MSG_TENANT_ACCESS_DENIED, currentTenantId, requiredTenantId));
        }
    }
}