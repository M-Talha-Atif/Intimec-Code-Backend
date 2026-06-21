package com.aidentx.iam.config.audit;

import com.aidentx.iam.config.AppConstants;
import com.aidentx.iam.security.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SecurityContextService {

    private final IUnifiedLogger logger;
    private final JwtUtil jwtUtil;
    private static final String LOG_CATEGORY = "security-context";

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public SecurityContextService(UnifiedLoggerFactory unifiedLoggerFactory, JwtUtil jwtUtil) {
        this.logger = unifiedLoggerFactory.getLogger("SecurityContextService", LOG_CATEGORY);
        this.jwtUtil = jwtUtil;
    }

    /**
     * Extracts user and request details from the current security context.
     * First tries to get user info from JWT token in Authorization header,
     * then falls back to headers populated by upstream gateway or security filter.
     *
     * @return An {@link AuditUser} object. Returns a SYSTEM user if no context is found.
     */
    public AuditUser getAuditUser() {
        HttpServletRequest request = getCurrentHttpRequest();

        if (request == null) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.SECURITY_LOG_NO_HTTP_REQUEST_FOUND)
                    .log();
            return AuditUser.SYSTEM;
        }

        String ipAddress = getClientIpAddress(request);

        // First, try to extract user info from JWT token
        AuditUser jwtUser = extractUserFromJwtToken(request, ipAddress);
        if (jwtUser != null) {
            return jwtUser;
        }

        // Second, try to extract from Spring Security Context
        AuditUser springSecurityUser = extractUserFromSpringSecurityContext(ipAddress);
        if (springSecurityUser != null) {
            return springSecurityUser;
        }

        // Third, fallback to headers set by API Gateway
        String userId = request.getHeader(HEADER_USER_ID);
        String username = request.getHeader(HEADER_USERNAME);

        if (userId != null && username != null) {
            logger.debug()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.SECURITY_LOG_USING_USER_INFO_FROM_GATEWAY_HEADERS)
                    .details(java.util.Map.of("userId", userId, "username", username))
                    .log();
            return new AuditUser(userId, username, ipAddress);
        }

        // If all else fails, return system user
        logger.debug()
                .category(LOG_CATEGORY)
                .message(AppConstants.SECURITY_LOG_NO_USER_CONTEXT_FOUND)
                .log();
        return AuditUser.SYSTEM;
    }

    /**
     * Extracts user information from JWT token in the Authorization header.
     */
    private AuditUser extractUserFromJwtToken(HttpServletRequest request, String ipAddress) {
        try {
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length());
                
                if (jwtUtil.validateToken(token)) {
                    String userId = jwtUtil.extractUserId(token);  // This extracts 'sub' from JWT
                    String username = jwtUtil.extractUsername(token);  // This extracts 'username' from JWT
                    
                    logger.debug()
                            .category(LOG_CATEGORY)
                            .message(AppConstants.SECURITY_LOG_SUCCESSFULLY_EXTRACTED_USER_FROM_JWT)
                            .details(java.util.Map.of("userId", userId, "username", username))
                            .log();
                    
                    return new AuditUser(userId, username, ipAddress);
                }
            }
        } catch (Exception e) {
            logger.debug()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.SECURITY_LOG_FAILED_TO_EXTRACT_USER_FROM_JWT)
                    .detail("error", e.getMessage())
                    .log();
        }
        return null;
    }

    /**
     * Extracts user information from Spring Security Context.
     */
    private AuditUser extractUserFromSpringSecurityContext(String ipAddress) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getPrincipal())) {
                
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetails userDetails) {
                    String username = userDetails.getUsername();
                    
                    logger.debug()
                            .category(LOG_CATEGORY)
                            .message(AppConstants.SECURITY_LOG_USING_USER_INFO_FROM_SPRING_SECURITY)
                            .details(java.util.Map.of("username", username))
                            .log();
                    
                    // For Spring Security context, username might be the userId
                    return new AuditUser(username, username, ipAddress);
                }
            }
        } catch (Exception e) {
            logger.debug()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.SECURITY_LOG_FAILED_TO_EXTRACT_USER_FROM_SPRING_SECURITY)
                    .detail("error", e.getMessage())
                    .log();
        }
        return null;
    }

    /**
     * Gets the client IP address from the request, handling proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.SECURITY_LOG_COULD_NOT_RETRIEVE_HTTP_REQUEST)
                    .detail("exceptionType", e.getClass().getSimpleName())
                    .log();
            return null;
        }
    }
}