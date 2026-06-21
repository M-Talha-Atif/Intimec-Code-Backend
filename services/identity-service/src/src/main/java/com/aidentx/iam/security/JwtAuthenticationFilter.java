package com.aidentx.iam.security;

import com.aidentx.iam.config.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT Authentication Filter for validating JWT tokens in incoming requests.
 * 
 * <p>This filter runs once per request and provides comprehensive JWT authentication
 * by validating tokens from multiple sources and setting up Spring Security context
 * for authenticated users.</p>
 * 
 * <p>Authentication flow:</p>
 * <ol>
 *   <li>Checks for pre-authenticated user from API Gateway via X-User header</li>
 *   <li>Falls back to JWT token validation from Authorization header</li>
 *   <li>Validates token signature, structure, and expiration</li>
 *   <li>Extracts user identity and creates Spring Security authentication</li>
 *   <li>Sets security context for the current request</li>
 * </ol>
 * 
 * <p>This dual approach supports both API Gateway pre-authentication and
 * direct JWT token validation, providing flexibility in microservice architectures.</p>
 *
 * @author aidentx Development Team
 * @since 1.0
 * @see OncePerRequestFilter
 * @see JwtUtil
 * @see SecurityConstants
 */
@Component(SecurityConstants.JWT_FILTER_BEAN_NAME)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final IUnifiedLogger logger;
    private final JwtUtil jwtUtil;

    /**
     * Constructs a new JWT authentication filter with the specified JWT utility.
     * 
     * @param jwtUtil the JWT utility for token validation and parsing
     * @param unifiedLoggerFactory the unified logger factory
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UnifiedLoggerFactory unifiedLoggerFactory) {
        this.jwtUtil = jwtUtil;
        this.logger = unifiedLoggerFactory.getLogger(JwtAuthenticationFilter.class);
        logger.debug().message(AppConstants.LOG_DEBUG_CONFIGURATION_VALUE + ": JWT authentication filter configured").log();
    }

    /**
     * Processes each HTTP request to perform JWT authentication.
     * 
     * <p>This method is the core of the authentication filter, executing once per request
     * to validate JWT tokens and establish security context. It handles both API Gateway
     * pre-authenticated requests and direct JWT token validation scenarios.</p>
     * 
     * <p>The filter implements a fail-safe approach where authentication failures
     * do not prevent request processing, allowing other security mechanisms to handle
     * unauthorized requests appropriately.</p>
     * 
     * @param request the HTTP request to process
     * @param response the HTTP response 
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        logger.debug().message(AppConstants.LOG_JWT_FILTER_PROCESSING_STARTED + " for URI: " + request.getRequestURI()).log();
        
        try {
            String userId = extractUsernameFromRequest(request);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateUser(userId, request);
                logger.debug().message(AppConstants.LOG_JWT_FILTER_USER_AUTHENTICATED + ": " + userId).log();
            }
            
            logger.debug().message(AppConstants.LOG_JWT_FILTER_PROCESSING_COMPLETED).log();
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_JWT_FILTER_PROCESSING_FAILED).log();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts username from the request using a dual-source approach.
     * 
     * <p>Implements a hierarchical authentication strategy:</p>
     * <ol>
     *   <li><strong>Primary:</strong> API Gateway pre-authentication via X-User header</li>
     *   <li><strong>Fallback:</strong> Direct JWT token validation from Authorization header</li>
     * </ol>
     * 
     * <p>This approach optimizes performance by trusting API Gateway authentication
     * when available, while maintaining the ability to validate tokens directly
     * for requests that bypass the gateway or in development scenarios.</p>
     * 
     * @param request the HTTP request to extract username from
     * @return the username if found and valid, null otherwise
     */
    private String extractUsernameFromRequest(HttpServletRequest request) {
        logger.debug().message(AppConstants.LOG_DEBUG_METHOD_ENTRY + ": extractUsernameFromRequest").log();
        
        // First, check if the API Gateway has already validated the token and passed the username
        String userFromGateway = request.getHeader(SecurityConstants.USER_HEADER);
        if (userFromGateway != null && !userFromGateway.trim().isEmpty()) {
            logger.debug().message(AppConstants.LOG_JWT_FILTER_USER_FROM_GATEWAY + ": " + userFromGateway).log();
            return userFromGateway.trim();
        }

        // Fall back to checking the Authorization header directly
        String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
            logger.debug().message(AppConstants.LOG_JWT_FILTER_TOKEN_FROM_HEADER).log();
            return extractUserIdFromToken(token);
        }

        logger.debug().message(AppConstants.LOG_DEBUG_METHOD_EXIT + ": extractUsernameFromRequest - no authentication found").log();
        return null;
    }

    /**
     * Extracts user ID from JWT token with comprehensive validation.
     * 
     * <p>Performs complete token validation including signature verification,
     * expiration checking, and claims extraction. This method ensures that
     * only valid, non-expired tokens result in user authentication.</p>
     * 
     * @param token the JWT token to validate and extract user ID from
     * @return the user ID if token is valid, null otherwise
     */
    private String extractUserIdFromToken(String token) {
        logger.debug().message(AppConstants.LOG_JWT_FILTER_TOKEN_VALIDATION_STARTED).log();
        try {
            if (jwtUtil.validateToken(token)) {
                String userId = jwtUtil.extractUserId(token);
                logger.debug().message(AppConstants.LOG_JWT_FILTER_TOKEN_VALIDATION_SUCCESSFUL + " - User ID: " + userId).log();
                return userId;
            } else {
                logger.warn().message(AppConstants.LOG_JWT_FILTER_TOKEN_VALIDATION_FAILED).log();
                return null;
            }
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_JWT_FILTER_TOKEN_VALIDATION_FAILED).log();
            return null;
        }
    }

    /**
     * Authenticates the user and establishes Spring Security context.
     * 
     * <p>Creates a complete Spring Security authentication context for the validated user,
     * including UserDetails, authentication token, and security context. This method
     * integrates JWT authentication with Spring Security's standard authentication
     * mechanisms.</p>
     * 
     * <p>The authentication process includes:</p>
     * <ul>
     *   <li>UserDetails creation with username and empty authorities</li>
     *   <li>UsernamePasswordAuthenticationToken creation</li>
     *   <li>Web authentication details attachment</li>
     *   <li>Security context establishment</li>
     * </ul>
     * 
     * @param userId the validated user ID to authenticate
     * @param request the HTTP request for authentication details
     */
    private void authenticateUser(String userId, HttpServletRequest request) {
        logger.debug().message(AppConstants.LOG_DEBUG_METHOD_ENTRY + ": authenticateUser for user: " + userId).log();
        try {
            // Create UserDetails for the authenticated user
            UserDetails userDetails = User.builder()
                    .username(userId)
                    .password("") // Password not needed for JWT authentication
                    .authorities(new ArrayList<>()) // Empty authorities for now
                    .build();
            logger.debug().message(AppConstants.LOG_DEBUG_PROCESSING_STEP + ": UserDetails created").log();

            // Create authentication token
            UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            logger.debug().message(AppConstants.LOG_DEBUG_PROCESSING_STEP + ": Authentication token created").log();
            
            // Set additional details
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            logger.debug().message(AppConstants.LOG_DEBUG_PROCESSING_STEP + ": Authentication details set").log();

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            logger.debug().message(AppConstants.LOG_JWT_FILTER_SECURITY_CONTEXT_SET + ": " + userId).log();
            
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_JWT_FILTER_SECURITY_CONTEXT_FAILED + ": " + userId).log();
        }
    }
}
