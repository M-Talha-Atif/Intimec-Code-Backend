package com.aidentx.configuration.audit;

import com.aidentx.configuration.security.SecurityConstants;
import com.aidentx.configuration.util.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SecurityContextService {

    private final IUnifiedLogger logger;
    private static final String LOG_CATEGORY = "security-context";

    private final JwtUtil jwtUtil;

    public SecurityContextService(UnifiedLoggerFactory unifiedLoggerFactory, JwtUtil jwtUtil) {
        this.logger = unifiedLoggerFactory.getLogger("SecurityContextService", LOG_CATEGORY);
        this.jwtUtil = jwtUtil;
    }

    /**
     * Extracts mandatory user and request details from the current security context
     * for auditing purposes.
     * The method checks for user identity in the following order of priority:
     * <ol>
     * <li>HTTP headers (X-User-Id, X-Username, X-Tenant-Id) set by a trusted gateway.</li>
     * <li>JWT token in the 'Authorization: Bearer' header.</li>
     * <li>Thread-local context for internal gRPC calls.</li>
     * </ol>
     *
     * @return A non-null {@link AuditUser} object if a user context is successfully
     *         found.
     * @throws IllegalStateException if a user context cannot be determined from any
     *                               of the available sources,
     *                               as this is considered a critical failure for an
     *                               audited action.
     */
    public AuditUser getAuditUser() {
        // First, try to get from HTTP request context
        HttpServletRequest request = getCurrentHttpRequest();
        if (request != null) {
            String ipAddress = getIpAddressFromRequest(request);

            // Priority 1: Check for API Gateway pre-authentication headers
            String userIdFromHeader = request.getHeader(SecurityConstants.HEADER_USER_ID);
            String usernameFromHeader = request.getHeader(SecurityConstants.HEADER_USERNAME);
            String tenantIdFromHeader = request.getHeader(SecurityConstants.HEADER_TENANT_ID);

            if (userIdFromHeader != null && !userIdFromHeader.isBlank() && usernameFromHeader != null
                    && !usernameFromHeader.isBlank()) {
                logger.debug()
                        .category(LOG_CATEGORY).message("Retrieved user from HTTP headers (Gateway Auth)").details(java.util.Map.of("userId", userIdFromHeader, "username", usernameFromHeader, "tenantId", tenantIdFromHeader != null ? tenantIdFromHeader : "null",
                                "ipAddress", ipAddress))
                        .log();
                return new AuditUser(userIdFromHeader, usernameFromHeader, tenantIdFromHeader, ipAddress);
            }

            // Priority 2: Check for JWT in Authorization header
            final String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
            if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
                String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
                String userIdFromJwt = jwtUtil.extractUserId(token);
                String usernameFromJwt = jwtUtil.extractUsername(token);
                String tenantIdFromJwt = jwtUtil.extractTenantId(token);

                if (userIdFromJwt != null && usernameFromJwt != null) {
                    logger.debug()
                            .category(LOG_CATEGORY).message("Retrieved user from JWT token").details(java.util.Map.of("userId", userIdFromJwt, "username", usernameFromJwt, "tenantId", tenantIdFromJwt != null ? tenantIdFromJwt : "null", "ipAddress", ipAddress))
                            .log();
                    return new AuditUser(userIdFromJwt, usernameFromJwt, tenantIdFromJwt, ipAddress);
                } else {
                    logger.warn()
                            .category(LOG_CATEGORY)
                            .message("JWT token found but could not extract required claims (sub, username).")
                            .log();
                }
            }
        }

        // Priority 3: Check thread-local context (for gRPC calls)
        AuditUser threadLocalUser = UserContextHolder.getCurrentUser();
        if (threadLocalUser != null) {
            // Fallback to default tenant if tenant ID is null or empty
            String resolvedTenantId = (threadLocalUser.tenantId() != null && !threadLocalUser.tenantId().isBlank()) ? threadLocalUser.tenantId() : "tenant_001";

            logger.debug()
                    .category(LOG_CATEGORY).message("Retrieved user from thread-local context (gRPC)").details(java.util.Map.of("userId", threadLocalUser.id(), "username", threadLocalUser.username(), "tenantId", resolvedTenantId, "tenantIdSource", threadLocalUser.tenantId() != null ? "thread_local" : "default_fallback"))
                    .log();

            // If we need to fallback, create a new AuditUser with the resolved tenant ID
            if (!resolvedTenantId.equals(threadLocalUser.tenantId())) {
                return new AuditUser(threadLocalUser.id(), threadLocalUser.username(), resolvedTenantId, threadLocalUser.ipAddress());
            }

            return threadLocalUser;
        }

        // If we reach this point, no user context was found from any source.
        // This is a critical failure for an audited operation.
        logger.error()
                .category(LOG_CATEGORY)
                .message("CRITICAL: Audit user context not found. Unable to proceed with audited operation.").details(java.util.Map.of("httpRequestAvailable", request != null ? "true" : "false", "threadLocalUserAvailable", threadLocalUser != null ? "true" : "false"
                ))
                .log();
        throw new IllegalStateException(
                "Audit user context could not be determined. This is a critical security failure.");
    }

    /**
     * Safely retrieves the current HTTP request from the request context holder.
     *
     * @return The current HttpServletRequest, or null if not in an HTTP context.
     */
    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message("Could not retrieve HTTP request for audit context (not in a request scope).")
                    .detail("exceptionType", e.getClass().getSimpleName())
                    .log();
            return null;
        }
    }

    /**
     * Extracts the client IP address from the request, prioritizing proxy headers.
     *
     * @param request The current HTTP request.
     * @return The resolved client IP address.
     */
    private String getIpAddressFromRequest(HttpServletRequest request) {
        // Prioritize X-Forwarded-For as it is the standard
        String ipFromHeader = request.getHeader("X-Forwarded-For");
        if (ipFromHeader != null && !ipFromHeader.isBlank()) {
            // The header can contain a comma-separated list, the first IP is the client
            return ipFromHeader.split(",")[0].trim();
        }
        // Fallback to X-Real-IP
        ipFromHeader = request.getHeader("X-Real-IP");
        if (ipFromHeader != null && !ipFromHeader.isBlank()) {
            return ipFromHeader.trim();
        }
        // Final fallback to the direct remote address
        return request.getRemoteAddr();
    }
}