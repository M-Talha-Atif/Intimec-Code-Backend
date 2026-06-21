package com.aidentx.iam.security;

import com.aidentx.iam.config.AppConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication entry point for handling unauthorized access attempts.
 * 
 * <p>This component provides a standardized JSON response for authentication failures
 * instead of the default Spring Security response. It includes comprehensive
 * security logging and client IP detection for audit trails.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Standardized JSON error response format</li>
 *   <li>Client IP address extraction with proxy header support</li>
 *   <li>Comprehensive security audit logging</li>
 *   <li>Consistent error response structure across all unauthorized access attempts</li>
 * </ul>
 * 
 * <p>The entry point is automatically invoked by Spring Security when an
 * unauthenticated request attempts to access a protected resource.</p>
 *
 * @author aidentx Development Team
 * @since 1.0
 * @see AuthenticationEntryPoint
 * @see SecurityConstants
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationEntryPoint(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.logger = unifiedLoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    }

    /**
     * Handles unauthorized access attempts by generating a standardized error response.
     * 
     * <p>This method is automatically called by Spring Security when an authentication
     * failure occurs. It logs the security event with comprehensive details and
     * returns a structured JSON error response to the client.</p>
     * 
     * @param request the HTTP request that resulted in authentication failure
     * @param response the HTTP response to write the error information to
     * @param authException the authentication exception that triggered this entry point
     * @throws IOException if an error occurs while writing the response
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        logger.debug().message(AppConstants.LOG_DEBUG_METHOD_ENTRY + ": commence").log();
        
        final String clientIp = getClientIpAddress(request);
        logger.warn().message(String.format("%s - URI: %s, Method: %s, Client IP: %s, Exception: %s", 
                AppConstants.LOG_JWT_UNAUTHORIZED_ACCESS_ATTEMPT, request.getRequestURI(), 
                request.getMethod(), clientIp, authException.getMessage())).log();

        // Set response status and content type
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        logger.debug().message(AppConstants.LOG_DEBUG_PROCESSING_STEP + ": Response headers set").log();

        // Create error response body
        Map<String, Object> errorResponse = createErrorResponse(request);
        logger.debug().message(AppConstants.LOG_JWT_ERROR_RESPONSE_CREATED).log();

        // Write JSON response
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        logger.debug().message(AppConstants.LOG_JWT_ERROR_RESPONSE_WRITTEN).log();
        logger.debug().message(AppConstants.LOG_JWT_UNAUTHORIZED_RESPONSE_GENERATED).log();
    }

    /**
     * Creates a standardized error response object for unauthorized access attempts.
     * 
     * <p>Generates a consistent error response structure that includes timestamp,
     * HTTP status, error type, message, and request path information.</p>
     * 
     * @param request the HTTP request that failed authentication
     * @return a map containing the structured error response
     */
    private Map<String, Object> createErrorResponse(HttpServletRequest request) {
        logger.debug().message(AppConstants.LOG_DEBUG_METHOD_ENTRY + ": createErrorResponse").log();
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", SecurityConstants.MSG_AUTHENTICATION_REQUIRED);
        errorResponse.put("path", request.getRequestURI());
        
        logger.debug().message(AppConstants.LOG_DEBUG_METHOD_EXIT + ": createErrorResponse").log();
        return errorResponse;
    }

    /**
     * Extracts the client IP address from the request with comprehensive proxy header support.
     * 
     * <p>Implements a robust IP detection mechanism that handles various proxy configurations:</p>
     * <ol>
     *   <li>X-Forwarded-For header (handles comma-separated IPs, takes first one)</li>
     *   <li>X-Real-IP header</li>
     *   <li>X-Client-IP header</li>
     *   <li>Remote address as fallback</li>
     * </ol>
     * 
     * <p>This method is essential for security auditing as it provides accurate
     * client identification even in load-balanced or proxied environments.</p>
     * 
     * @param request the HTTP request to extract IP address from
     * @return the client IP address, or remote address if proxy headers are not available
     */
    private String getClientIpAddress(HttpServletRequest request) {
        logger.debug().message(AppConstants.LOG_DEBUG_METHOD_ENTRY + ": getClientIpAddress").log();
        
        try {
            // Check common proxy headers first
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                String clientIp = xForwardedFor.split(",")[0].trim();
                logger.debug().message(AppConstants.LOG_JWT_CLIENT_IP_EXTRACTED + " from X-Forwarded-For: " + clientIp).log();
                return clientIp;
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                logger.debug().message(AppConstants.LOG_JWT_CLIENT_IP_EXTRACTED + " from X-Real-IP: " + xRealIp).log();
                return xRealIp;
            }

            String xClientIp = request.getHeader("X-Client-IP");
            if (xClientIp != null && !xClientIp.isEmpty() && !"unknown".equalsIgnoreCase(xClientIp)) {
                logger.debug().message(AppConstants.LOG_JWT_CLIENT_IP_EXTRACTED + " from X-Client-IP: " + xClientIp).log();
                return xClientIp;
            }

            // Fall back to remote address
            String remoteAddr = request.getRemoteAddr();
            logger.debug().message(AppConstants.LOG_JWT_CLIENT_IP_EXTRACTED + " from remote address: " + remoteAddr).log();
            return remoteAddr;
            
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_JWT_CLIENT_IP_EXTRACTION_FAILED).log();
            return request.getRemoteAddr();
        }
    }
}
