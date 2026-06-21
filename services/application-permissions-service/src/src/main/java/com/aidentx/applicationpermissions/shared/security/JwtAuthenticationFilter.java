package com.aidentx.applicationpermissions.shared.security;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Shared JWT Authentication Filter for validating JWT tokens in incoming
 * requests.
 * This filter runs once per request and validates the JWT token from the
 * Authorization header
 * or the X-User header set by the API Gateway.
 * 
 * This is a shared component used across application-permissions-service
 * to ensure consistent authentication behavior.
 */
@Component(SecurityConstants.JWT_FILTER_BEAN_NAME)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final IUnifiedLogger logger;
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UnifiedLoggerFactory loggerFactory) {
        this.jwtUtil = jwtUtil;
        this.logger = loggerFactory.getLogger("JwtAuthenticationFilter",
                SecurityConstants.LOG_CATEGORY_SECURITY_AUTH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String userId = extractUsernameFromRequest(request);

            if (userId != null) {
                // Always set the user context for gRPC calls when we have a valid user
                setUserContext(userId, request);
                
                // Only set Spring Security authentication if not already set
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    authenticateUser(userId, request);
                    logger.debug().message(SecurityConstants.LOG_AUTHENTICATION_SET).detail("userId", userId).log();
                }
            }
        } catch (Exception e) {
            logger.error(e).message(SecurityConstants.LOG_AUTHENTICATION_FILTER_ERROR).log();
        }

        try {
            // Continue the request processing
            filterChain.doFilter(request, response);
        } finally {
            // Clear the user context after the request is completely finished
            // This ensures that gRPC calls within the request have access to the context
            logger.debug().message(SecurityConstants.LOG_CLEARING_USER_CONTEXT).log();
            UserContextHolder.clear();
        }
    }

    /**
     * Extracts username from the request, either from API Gateway header or JWT
     * token.
     * 
     * @param request the HTTP request
     * @return the username if found and valid, null otherwise
     */
    private String extractUsernameFromRequest(HttpServletRequest request) {
        // First, check if the API Gateway has already validated the token and passed
        // the username
        String userFromGateway = request.getHeader(SecurityConstants.USER_HEADER);
        if (userFromGateway != null && !userFromGateway.trim().isEmpty()) {
            return userFromGateway.trim();
        }

        // Fall back to checking the Authorization header directly
        String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
            return extractUserIdFromToken(token);
        }

        return null;
    }

    /**
     * Extracts user ID from JWT token.
     * 
     * @param token the JWT token
     * @return the user ID if token is valid, null otherwise
     */
    private String extractUserIdFromToken(String token) {
        try {
            logger.debug().message(SecurityConstants.LOG_JWT_ATTEMPT_VALIDATION).log();
            if (jwtUtil.validateToken(token)) {
                String userId = jwtUtil.extractUserId(token);
                logger.debug().message(SecurityConstants.LOG_JWT_VALIDATION_SUCCESS).detail("userId", userId).log();
                return userId;
            } else {
                logger.warn().message(SecurityConstants.LOG_JWT_VALIDATION_FAILED).log();
            }
        } catch (Exception e) {
            logger.warn().message(SecurityConstants.LOG_TOKEN_VALIDATION_FAILED + " " + e.getMessage()).log();
        }
        return null;
    }

    /**
     * Creates and sets authentication in security context.
     * 
     * @param userId  the authenticated user ID
     * @param request the HTTP request
     */
    private void authenticateUser(String userId, HttpServletRequest request) {
        UserDetails userDetails = User.builder()
                .username(userId) // Using userId as username for Spring Security
                .password("") // Password not needed for JWT-based auth
                .authorities(new ArrayList<>()) // Add roles/authorities as needed
                .build();

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        logger.debug().message(SecurityConstants.LOG_USER_AUTHENTICATED_VIA_TOKEN).detail("userId", userId).log();
    }

    /**
     * Sets the user context for gRPC calls and other thread-local operations.
     * 
     * @param userId  the authenticated user ID
     * @param request the HTTP request
     */
    private void setUserContext(String userId, HttpServletRequest request) {
        try {
            // Extract username from token if available
            String username = extractUsernameFromToken(request);
            if (username == null) {
                username = userId; // Fallback to userId if username not available
            }

            // Extract tenant ID from token
            String tenantId = extractTenantIdFromToken(request);
            if (tenantId == null) {
                tenantId = "unknown"; // Fallback value
            }

            // Extract client IP address
            String ipAddress = RequestUtils.getClientIP(request);

            // Set the user context
            UserContext userContext = new UserContext(userId, username, tenantId, ipAddress);
            UserContextHolder.setContext(userContext);

            logger.info().message(SecurityConstants.USER_CONTEXT_SET)
                    .detail("userId", userId)
                    .detail("username", username)
                    .detail("tenantId", tenantId)
                    .detail("ipAddress", ipAddress)
                    .log();
                    
            // Verify the context was set correctly
            UserContext verifyContext = UserContextHolder.getContext();
            if (verifyContext != null) {
                logger.info().message(SecurityConstants.USER_CONTEXT_VERIFIED)
                        .detail("verifiedUserId", verifyContext.userId())
                        .log();
            } else {
                logger.error().message(SecurityConstants.USER_CONTEXT_FAILED).log();
            }
        } catch (Exception e) {
            logger.error(e).message(SecurityConstants.USER_CONTEXT_SET_FAILED + e.getMessage()).log();
        }
    }

    /**
     * Extracts username from the JWT token in the request.
     * 
     * @param request the HTTP request
     * @return the username if found, null otherwise
     */
    private String extractUsernameFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
            return jwtUtil.extractUsername(token);
        }
        return null;
    }

    /**
     * Extracts tenant ID from the JWT token in the request.
     * 
     * @param request the HTTP request
     * @return the tenant ID if found, null otherwise
     */
    private String extractTenantIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
            try {
                return jwtUtil.extractTenantId(token);
            } catch (Exception e) {
                logger.debug().message("Failed to extract tenant ID from token").detail("error", e.getMessage()).log();
                return null;
            }
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String endpoint : SecurityConstants.PUBLIC_ENDPOINTS) {
            if (path.startsWith(endpoint.replace("/**", ""))) {
                return true;
            }
        }
        return false;
    }
}