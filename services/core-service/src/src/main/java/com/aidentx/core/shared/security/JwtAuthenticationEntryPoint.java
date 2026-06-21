package com.aidentx.core.shared.security;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * This component provides a standardized JSON response for authentication
 * failures
 * instead of the default Spring Security response.
 * 
 * Production-ready with proper logging and structured error responses.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationEntryPoint(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger("JwtAuthenticationEntryPoint",
                SecurityConstants.LOG_CATEGORY_SECURITY_AUTH);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        final String clientIp = getClientIpAddress(request);
        logger.warn().message(SecurityConstants.LOG_UNAUTHORIZED_ACCESS)
                .details(Map.of("uri", request.getRequestURI(),
                        "method", request.getMethod(),
                        "clientIp", clientIp))
                .detail("exception", authException.getMessage())
                .log();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> errorResponse = createErrorResponse(request);

        response.getOutputStream().println(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Creates a standardized error response for authentication failures.
     * 
     * @param request the HTTP request
     * @return a map containing error details
     */
    private Map<String, Object> createErrorResponse(HttpServletRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
        errorResponse.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        errorResponse.put("message", SecurityConstants.MSG_AUTHENTICATION_REQUIRED);
        errorResponse.put("path", request.getRequestURI());

        return errorResponse;
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
}