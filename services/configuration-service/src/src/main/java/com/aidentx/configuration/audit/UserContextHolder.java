package com.aidentx.configuration.audit;

import com.aidentx.configuration.security.SecurityConstants;
import com.aidentx.configuration.service.util.RequestUtils;
import com.aidentx.configuration.util.JwtUtil;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Thread-local holder for user context information in audit operations.
 */
public class UserContextHolder {

    private static final ThreadLocal<AuditUser> userContext = new ThreadLocal<>();

    public static void setCurrentUser(AuditUser user) {
        userContext.set(user);
    }

    public static AuditUser getCurrentUser() {
        return userContext.get();
    }

    public static void clear() {
        userContext.remove();
    }

    /**
     * Sets the user context from the request using multiple fallback strategies.
     * This ensures that audit context is properly established regardless of the
     * authentication method.
     * 
     * @param request The HTTP request containing user information
     * @throws IllegalStateException if no user context can be established
     */
    public static void setUserContextFromRequest(HttpServletRequest request) {
        JwtUtil jwtUtil = new JwtUtil(new UnifiedLoggerFactory(new ObjectMapper(), "Configuration"));
        String clientIP = RequestUtils.getClientIP(request);

        // Strategy 1: Try to use Spring Security Authentication context
        try {
            UserContextHolder.setUserContextFromAuthentication(clientIP);
            return;
        } catch (IllegalStateException e) {

        }

        // Strategy 2: Try to extract from HTTP headers (API Gateway pre-authentication)
        String userIdFromHeader = request.getHeader(SecurityConstants.HEADER_USER_ID);
        String usernameFromHeader = request.getHeader(SecurityConstants.HEADER_USERNAME);

        if (userIdFromHeader != null && !userIdFromHeader.isBlank() &&
                usernameFromHeader != null && !usernameFromHeader.isBlank()) {
            AuditUser user = new AuditUser(userIdFromHeader, usernameFromHeader, clientIP, "tenant_001");
            UserContextHolder.setCurrentUser(user);
            return;
        }

        // Strategy 3: Try to extract from JWT token directly
        final String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
            String userIdFromJwt = jwtUtil.extractUserId(token);
            String usernameFromJwt = jwtUtil.extractUsername(token);

            if (userIdFromJwt != null && usernameFromJwt != null) {
                AuditUser user = new AuditUser(userIdFromJwt, usernameFromJwt, clientIP, "tenant_001");
                UserContextHolder.setCurrentUser(user);
                return;
            }
        }

        // If all strategies fail, throw an exception
        throw new IllegalStateException(
                "Cannot establish user context: No valid authentication found in Spring Security context, "
                        +
                        "HTTP headers, or JWT token. This is required for audit operations.");
    }

    public static void setUserContextFromAuthentication(String clientIP) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException(
                    "Cannot set user context: No authentication found in Security Context.");
        }

        if (!authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "Cannot set user context: Authentication is not authenticated.");
        }

        if (!(authentication.getPrincipal() instanceof AuditPrincipal)) {
            throw new IllegalStateException(
                    "Cannot set user context: Authentication principal is not an AuditPrincipal but "
                            + (authentication.getPrincipal() != null
                                    ? authentication.getPrincipal().getClass().getSimpleName()
                                    : "null"));
        }

        AuditPrincipal principal = (AuditPrincipal) authentication.getPrincipal();
        AuditUser auditUser = new AuditUser(principal.id(), principal.username(), clientIP, "tenant_001");
        userContext.set(auditUser);
    }
}