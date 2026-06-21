package com.aidentx.api_gateaway.filter;

import com.aidentx.api_gateaway.config.AppConstants;
import com.aidentx.api_gateaway.util.JwtUtil;
import com.aidentx.auth.grpc.SessionActivityServiceGrpc;
import com.aidentx.auth.grpc.UpdateLastActivityRequest;
import com.aidentx.auth.grpc.UpdateLastActivityResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Gateway filter for JWT authentication.
 * This filter is applied to protected routes via the gateway configuration.
 * It validates the JWT token in the Authorization header and enriches the
 * request with the username for downstream services.
 */
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    // Conventional headers the API Gateway would add after validating
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_IP_ADDRESS = "X-IP-Address";
    private static final String HEADER_TENANT = "X-Tenant-Id";
    private static final String DEFAULT_TENANT = "tenant_001";

    private final IUnifiedLogger logger;
    private final JwtUtil jwtUtil;
    private SessionActivityServiceGrpc.SessionActivityServiceBlockingStub sessionActivityStub;

    public AuthFilter(JwtUtil jwtUtil, UnifiedLoggerFactory loggerFactory) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.logger = loggerFactory.getLogger("GatewayAuthFilter", AppConstants.LOG_CATEGORY_AUTH_FILTER);
    }

    @GrpcClient("auth-service")
    public void setSessionActivityStub(SessionActivityServiceGrpc.SessionActivityServiceBlockingStub sessionActivityStub) {
        this.sessionActivityStub = sessionActivityStub;
        logger.info().message("gRPC SessionActivity stub injected successfully").log();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            logger.trace().message(AppConstants.LOG_SECURED_ROUTE_ACCESS).detail("uri", request.getURI()).log();

            // Check for Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, AppConstants.MSG_MISSING_AUTHORIZATION_HEADER, HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // Check 'Bearer ' prefix
            if (authHeader == null || !authHeader.startsWith(AppConstants.BEARER_TOKEN_PREFIX)) {
                return onError(exchange, AppConstants.MSG_INVALID_AUTHORIZATION_HEADER_FORMAT, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(AppConstants.BEARER_TOKEN_PREFIX_LENGTH);

            // Validate the token
            try {
                if (!jwtUtil.validateToken(token)) {
                    return onError(exchange, AppConstants.MSG_INVALID_OR_EXPIRED_TOKEN, HttpStatus.UNAUTHORIZED);
                }

                // If valid, extract username and user ID, then add them to request headers for
                // downstream services
                String username = jwtUtil.extractUsername(token);
                String userId = jwtUtil.extractUserId(token);
                String tenantId = jwtUtil.extractTenantId(token);
                String clientIpAddress = getClientIpAddress(request);

                // Build the request with extracted headers
                ServerHttpRequest.Builder mutator = request.mutate()
                        .header(HEADER_USERNAME, username)
                        .header(HEADER_USER_ID, userId)
                        .header(HEADER_IP_ADDRESS, clientIpAddress)
                        .header(AppConstants.HEADER_X_USER, username); // Keep for backward compatibility

                // Add tenant ID header if extracted from JWT
                if (tenantId != null && !tenantId.trim().isEmpty()) {
                    mutator.header(HEADER_TENANT, tenantId);
                    logger.debug().message("Tenant ID forwarded to downstream service")
                            .detail("tenantId", tenantId)
                            .detail("username", username).log();
                } else {
                    // Use default tenant if none found in JWT
                    mutator.header(HEADER_TENANT, DEFAULT_TENANT);
                    logger.debug().message("No tenant ID in JWT, using default")
                            .detail("defaultTenant", DEFAULT_TENANT)
                            .detail("username", username).log();
                }

                ServerHttpRequest modifiedRequest = mutator.build();

                // Update last activity asynchronously
                updateLastActivity(request);

                // Pass the modified request to the next filter in the chain
                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                logger.error(e).message(AppConstants.LOG_TOKEN_VALIDATION_FAILURE).log();
                return onError(exchange, AppConstants.MSG_TOKEN_VALIDATION_FAILED, HttpStatus.UNAUTHORIZED);
            }
        };
    }

    /**
     * Updates the last activity timestamp for the session asynchronously.
     * Reads sessionId from X-Session-Id header sent by the frontend.
     */
    private void updateLastActivity(ServerHttpRequest request) {
        try {
            // Extract sessionId from X-Session-Id header
            String sessionId = request.getHeaders().getFirst("X-Session-Id");
            
            logger.debug()
                    .message("Attempting to update last activity")
                    .detail("hasSessionId", sessionId != null)
                    .detail("hasStub", sessionActivityStub != null)
                    .log();
            
            if (sessionId != null && !sessionId.trim().isEmpty()) {
                if (sessionActivityStub == null) {
                    logger.warn()
                            .message("SessionActivity gRPC stub is null, cannot update activity")
                            .log();
                    return;
                }
                
                // Call gRPC service asynchronously (non-blocking)
                CompletableFuture.runAsync(() -> {
                    try {
                        logger.debug()
                                .message("Calling gRPC updateLastActivity")
                                .detail("sessionId", sessionId)
                                .log();
                        
                        UpdateLastActivityRequest grpcRequest = UpdateLastActivityRequest.newBuilder()
                                .setSessionId(sessionId)
                                .build();
                        
                        UpdateLastActivityResponse response = sessionActivityStub.updateLastActivity(grpcRequest);
                        
                        if (response.getSuccess()) {
                            logger.debug()
                                    .message("Successfully updated last activity")
                                    .detail("sessionId", sessionId)
                                    .log();
                        } else {
                            logger.debug()
                                    .message("Failed to update last activity")
                                    .detail("message", response.getMessage())
                                    .detail("sessionId", sessionId)
                                    .log();
                        }
                    } catch (Exception e) {
                        logger.error(e)
                                .message("Error updating last activity via gRPC")
                                .detail("sessionId", sessionId)
                                .log();
                    }
                });
            } else {
                logger.debug()
                        .message("No X-Session-Id header found for activity update")
                        .log();
            }
        } catch (Exception e) {
            // Log but don't fail the request
            logger.error(e)
                    .message("Error extracting sessionId for activity update")
                    .log();
        }
    }

    /**
     * Gets the client IP address from the request, handling proxy headers.
     */
    private String getClientIpAddress(ServerHttpRequest request) {
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
            String ip = request.getHeaders().getFirst(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null) {
            return "unknown";
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, AppConstants.CONTENT_TYPE_JSON);

        String responseBody = String.format(
                AppConstants.ERROR_RESPONSE_TEMPLATE,
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                errorMessage);

        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}