package com.aidentx.api_gateaway.filter;

import com.aidentx.api_gateaway.config.AppConstants;
import com.aidentx.api_gateaway.util.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.applicationpermissions.grpc.PermissionServiceGrpc;
import com.aidentx.applicationpermissions.grpc.PermissionCheckRequest;
import com.aidentx.applicationpermissions.grpc.PermissionCheckResponse;
import com.aidentx.applicationpermissions.grpc.EndpointCheckRequest;
import com.aidentx.applicationpermissions.grpc.EndpointCheckResponse;
import com.aidentx.auth.grpc.SessionActivityServiceGrpc;
import com.aidentx.auth.grpc.UpdateLastActivityRequest;
import com.aidentx.auth.grpc.UpdateLastActivityResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.concurrent.CompletableFuture;

/**
 * Reactive permission check filter for API Gateway.
 * 
 * <p>This filter validates user permissions for any endpoint by making a gRPC call to
 * the application-permissions-service. It runs in a reactive (non-blocking) manner
 * compatible with Spring Cloud Gateway's WebFlux architecture.</p>
 * 
 * <p>This is a GlobalFilter that automatically applies to all routes, running after
 * authentication but before routing to downstream services.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class GatewayPermissionFilter implements GlobalFilter, Ordered {
    
    private final IUnifiedLogger logger;
    private final JwtUtil jwtUtil;
    
    @GrpcClient("application-permissions-service")
    private PermissionServiceGrpc.PermissionServiceBlockingStub permissionServiceStub;
    
    @GrpcClient("auth-service")
    private com.aidentx.auth.grpc.SessionActivityServiceGrpc.SessionActivityServiceBlockingStub sessionActivityStub;
    
    public GatewayPermissionFilter(UnifiedLoggerFactory loggerFactory, JwtUtil jwtUtil) {
        this.logger = loggerFactory.getLogger("GatewayPermissionFilter", "SECURITY");
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // Get original request path (before any rewrite filters)
        LinkedHashSet<URI> originalUris = exchange.getAttribute(org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        String requestURI = originalUris != null && !originalUris.isEmpty() 
            ? originalUris.iterator().next().getPath() 
            : request.getURI().getPath();
        String method = request.getMethod().name();
        
        logger.trace()
            .message(AppConstants.LOG_PERMISSION_CHECK_STARTED)
            .detail(AppConstants.DETAIL_METHOD, method)
            .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
            .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
            .log();
        
        // Skip permission check for certain paths
        if (shouldSkipPermissionCheck(requestURI)) {
            logger.debug()
                .message(AppConstants.LOG_PERMISSION_CHECK_SKIPPED)
                .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                .log();
            return chain.filter(exchange);
        }
        
        // Extract JWT token from request
        return extractToken(exchange)
            .flatMap(token -> {
                try {
                    // Extract user info from JWT
                    String userId = jwtUtil.extractUserId(token);
                    String tenantId = jwtUtil.extractTenantId(token);
                    
                    logger.trace()
                        .message(AppConstants.USER_INFO_EXTRACTED)
                        .detail(AppConstants.DETAIL_USER_ID, userId)
                        .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                        .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                        .objectType(AppConstants.OBJECT_TYPE_JWT_EXTRACTION)
                        .log();
                    
                    // Update last activity timestamp asynchronously (non-blocking)
                    logger.info()
                        .message("About to call updateLastActivity")
                        .detail("hasCookies", !request.getCookies().isEmpty())
                        .detail("hasSessionStub", sessionActivityStub != null)
                        .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                        .log();
                    updateLastActivity(request);
                    
                    // Check if endpoint is managed by permission system (blocking call in separate thread)
                    return Mono.fromCallable(() -> isEndpointManaged(requestURI, method))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(isManaged -> {
                            if (!isManaged) {
                                logger.debug()
                                    .message(AppConstants.LOG_ENDPOINT_NOT_MANAGED)
                                    .detail(AppConstants.DETAIL_METHOD, method)
                                    .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                                    .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                                    .log();
                                return chain.filter(exchange);
                            }
                            
                            logger.info()
                                .message(AppConstants.LOG_CHECKING_MANAGED_ENDPOINT)
                                .detail(AppConstants.DETAIL_USER_ID, userId)
                                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                                .detail(AppConstants.DETAIL_METHOD, method)
                                .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                                .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                                .log();
                            
                            // Check permission via gRPC (blocking call in separate thread)
                            return Mono.fromCallable(() -> checkPermissionViaService(userId, tenantId, requestURI, method))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(hasPermission -> {
                                    if (!hasPermission) {
                                        logger.error()
                                            .message(AppConstants.LOG_PERMISSION_DENIED)
                                            .detail(AppConstants.DETAIL_USER_ID, userId)
                                            .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                                            .detail(AppConstants.DETAIL_METHOD, method)
                                            .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                                            .detail("reason", "User does not have required role/permissions for this endpoint")
                                            .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                                            .log();
                                        return sendForbiddenResponse(exchange, AppConstants.ERROR_ACCESS_DENIED, userId, requestURI, method);
                                    }
                                    
                                    logger.info()
                                        .message(AppConstants.LOG_PERMISSION_GRANTED)
                                        .detail(AppConstants.DETAIL_USER_ID, userId)
                                        .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                                        .detail(AppConstants.DETAIL_METHOD, method)
                                        .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                                        .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                                        .log();
                                    
                                    return chain.filter(exchange);
                                });
                        });
                } catch (Exception e) {
                    // This catch block handles synchronous errors during JWT extraction
                    logger.error(e)
                        .message(AppConstants.LOG_JWT_TOKEN_PROCESSING_ERROR)
                        .detail(AppConstants.DETAIL_METHOD, method)
                        .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                        .detail(AppConstants.DETAIL_ERROR, e.getMessage())
                        .detail("exceptionType", e.getClass().getSimpleName())
                        .detail("phase", "JWT_EXTRACTION")
                        .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                        .log();
                    return sendErrorResponse(exchange, "Invalid token: " + e.getMessage());
                }
            })
            .onErrorResume(e -> {
                // Handle case where token extraction fails - check if it's a public endpoint
                if (shouldSkipPermissionCheck(requestURI)) {
                    logger.trace()
                        .message(AppConstants.LOG_TOKEN_EXTRACTION_PUBLIC_ENDPOINT)
                        .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                        .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                        .log();
                    return chain.filter(exchange);
                }
                
                // Check if it's an authentication error (missing/invalid token)
                if (e.getMessage() != null && e.getMessage().contains(AppConstants.ERROR_MISSING_AUTH_HEADER)) {
                    logger.warn()
                        .message(AppConstants.LOG_AUTHENTICATION_FAILED)
                        .detail(AppConstants.DETAIL_METHOD, method)
                        .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                        .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                        .log();
                    return sendErrorResponse(exchange, AppConstants.ERROR_MISSING_AUTH_HEADER);
                }
                
                // Log genuine errors (gRPC failures, network issues, etc.)
                logger.error(e)
                    .message(AppConstants.LOG_PERMISSION_CHECK_ERROR)
                    .detail(AppConstants.DETAIL_METHOD, method)
                    .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                    .detail(AppConstants.DETAIL_ERROR, e.getMessage())
                    .detail("exceptionType", e.getClass().getSimpleName())
                    .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                    .log();
                return sendErrorResponse(exchange, AppConstants.ERROR_INTERNAL_PERMISSION_CHECK);
            });
    }
    
    /**
     * Extract JWT token from Authorization header.
     * 
     * @param exchange the server web exchange
     * @return Mono containing the token, or error if missing
     */
    private Mono<String> extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.trace()
                .message(AppConstants.LOG_NO_AUTH_HEADER)
                .detail(AppConstants.DETAIL_REQUEST_URI, exchange.getRequest().getURI().getPath())
                .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                .log();
            return Mono.error(new RuntimeException(AppConstants.ERROR_MISSING_AUTH_HEADER));
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        
        logger.debug()
            .message(AppConstants.LOG_JWT_USER_INFO_EXTRACTED)
            .detail(AppConstants.DETAIL_JWT_TOKEN_LENGTH, String.valueOf(token.length()))
            .objectType(AppConstants.OBJECT_TYPE_JWT_EXTRACTION)
            .log();
        
        return Mono.just(token);
    }
    
    /**
     * Check if permission check should be skipped for this URI.
     * 
     * @param requestURI the request URI
     * @return true if permission check should be skipped
     */
    private boolean shouldSkipPermissionCheck(String requestURI) {
        // Skip permission check for these paths
        return requestURI.startsWith("/actuator/") ||
               requestURI.startsWith("/health") ||
               requestURI.startsWith("/api/v1/auth/") ||
               requestURI.startsWith("/api/v1/workflows/ws") ||
               requestURI.startsWith("/api/v1/artifacts/") ||
               requestURI.startsWith("/api/v1/action-templates") ||
               requestURI.equals("/") ||
               requestURI.startsWith("/swagger") ||
               requestURI.startsWith("/v3/api-docs");
    }
    
    /**
     * Check if an endpoint is managed by the permission system.
     * 
     * @param requestURI the request URI
     * @param method the HTTP method
     * @return true if endpoint is managed by permission system
     */
    private boolean isEndpointManaged(String requestURI, String method) {
        try {
            logger.trace()
                .message(AppConstants.LOG_GRPC_REQUEST_BUILDING)
                .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                .detail(AppConstants.DETAIL_METHOD, method)
                .objectType(AppConstants.OBJECT_TYPE_GRPC_CALL)
                .log();
                
            EndpointCheckRequest request = EndpointCheckRequest.newBuilder()
                .setEndpoint(requestURI)
                .setHttpMethod(method)
                .build();
            
            logger.debug()
                .message(AppConstants.CALLING_GRPC_CHECK_ENDPOINT)
                .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                .detail(AppConstants.DETAIL_METHOD, method)
                .objectType(AppConstants.OBJECT_TYPE_GRPC_CALL)
                .log();
            
            EndpointCheckResponse response = permissionServiceStub.checkEndpointExists(request);
            
            boolean isManaged = response.getIsManaged();
            
            logger.debug()
                .message(AppConstants.LOG_ENDPOINT_MANAGEMENT_CHECK)
                .detail(AppConstants.DETAIL_METHOD, method)
                .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                .detail(AppConstants.DETAIL_IS_MANAGED, String.valueOf(isManaged))
                .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_CHECK)
                .log();
                
            return isManaged;
            
        } catch (Exception e) {
            // If we can't check, assume endpoint is not managed for safety
            logger.warn()
                .message(AppConstants.LOG_GRPC_ENDPOINT_CHECK_FAILED)
                .detail(AppConstants.DETAIL_REQUEST_URI, requestURI)
                .detail(AppConstants.DETAIL_METHOD, method)
                .detail(AppConstants.DETAIL_ERROR, e.getMessage())
                .detail("exceptionType", e.getClass().getSimpleName())
                .detail("phase", "ENDPOINT_CHECK")
                .objectType(AppConstants.OBJECT_TYPE_GRPC_CALL)
                .log();
            return false; // Allow access if permission service is unavailable
        }
    }
    
    /**
     * Call the application-permissions-service via gRPC to check user permission.
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @param apiEndpoint the API endpoint
     * @param httpMethod the HTTP method
     * @return true if user has permission
     */
    private boolean checkPermissionViaService(String userId, String tenantId, 
                                            String apiEndpoint, String httpMethod) {
        try {
            logger.trace()
                .message(AppConstants.LOG_GRPC_PERMISSION_REQUEST_BUILDING)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_GRPC_CALL)
                .log();
                
            PermissionCheckRequest request = PermissionCheckRequest.newBuilder()
                .setUserId(userId)
                .setTenantId(tenantId)
                .setApiEndpoint(apiEndpoint)
                .setHttpMethod(httpMethod)
                .build();
            
            logger.debug()
                .message(AppConstants.LOG_GRPC_PERMISSION_CALL)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .objectType(AppConstants.OBJECT_TYPE_GRPC_CALL)
                .log();
            
            PermissionCheckResponse response = permissionServiceStub.checkPermission(request);
            
            boolean hasPermission = response.getHasPermission();
            
            logger.debug()
                .message(AppConstants.LOG_GRPC_PERMISSION_RESPONSE)
                .detail(AppConstants.DETAIL_HAS_PERMISSION, String.valueOf(hasPermission))
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .objectType(AppConstants.OBJECT_TYPE_GRPC_CALL)
                .log();
                
            return hasPermission;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_GRPC_PERMISSION_CHECK_FAILED)
                .detail(AppConstants.DETAIL_USER_ID, userId)
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
                .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
                .detail(AppConstants.DETAIL_ERROR, e.getMessage())
                .detail("exceptionType", e.getClass().getSimpleName())
                .detail("phase", "PERMISSION_CHECK")
                .objectType(AppConstants.OBJECT_TYPE_GRPC_CALL)
                .log();
            return false; // Deny access on error (fail-secure)
        }
    }
    
    /**
     * Send a forbidden (403) response.
     * 
     * @param exchange the server web exchange
     * @param message the error message
     * @return Mono<Void> representing the completion of the response
     */
    private Mono<Void> sendForbiddenResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        
        String jsonResponse = String.format(
            "{\"error\":\"Forbidden\",\"message\":\"%s\",\"status\":403,\"timestamp\":%d}",
            message, System.currentTimeMillis());
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(jsonResponse.getBytes())));
    }
    
    /**
     * Send a forbidden (403) response with detailed information.
     * 
     * @param exchange the server web exchange
     * @param message the error message
     * @param userId the user ID who was denied access
     * @param requestUri the URI that was requested
     * @param method the HTTP method
     * @return Mono<Void> representing the completion of the response
     */
    private Mono<Void> sendForbiddenResponse(ServerWebExchange exchange, String message, 
                                            String userId, String requestUri, String method) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        
        String jsonResponse = String.format(
            "{\"error\":\"Forbidden\",\"message\":\"%s\",\"status\":403,\"timestamp\":%d," +
            "\"details\":{\"userId\":\"%s\",\"endpoint\":\"%s\",\"method\":\"%s\"," +
            "\"hint\":\"Contact your administrator to request the necessary role or permissions\"}}",
            message, System.currentTimeMillis(), userId, requestUri, method);
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(jsonResponse.getBytes())));
    }
    
    /**
     * Send an internal server error (500) response.
     * 
     * @param exchange the server web exchange
     * @param message the error message
     * @return Mono<Void> representing the completion of the response
     */
    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().add("Content-Type", "application/json");
        
        String jsonResponse = String.format(
            "{\"error\":\"Internal Server Error\",\"message\":\"%s\",\"status\":500,\"timestamp\":%d}",
            message, System.currentTimeMillis());
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(jsonResponse.getBytes())));
    }
    
    @Override
    public int getOrder() {
        // Run after authentication and correlation filters
        // AuthFilter and CorrelationFilter typically run at orders -1, 0, or 1
        // So set this to 10 to ensure it runs after them
        return 10;
    }
    
    /**
     * Updates the last activity timestamp for the session asynchronously.
     * This method extracts the sessionId from X-Session-Id header and calls the auth-service
     * via gRPC to update the session's last activity timestamp.
     * 
     * @param request the server HTTP request containing the session header
     */
    private void updateLastActivity(ServerHttpRequest request) {
        try {
            // Get sessionId from X-Session-Id header (sent by frontend)
            String sessionId = request.getHeaders().getFirst("X-Session-Id");
            
            logger.info()
                    .message("updateLastActivity called")
                    .detail("hasSessionId", sessionId != null)
                    .detail("hasStub", sessionActivityStub != null)
                    .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                    .log();
            
            if (sessionId != null && !sessionId.isEmpty()) {
                final String finalSessionId = sessionId;
                
                if (sessionActivityStub == null) {
                    logger.warn()
                            .message("SessionActivity gRPC stub is null, skipping activity update")
                            .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                            .log();
                    return;
                }
                
                logger.info()
                        .message("Calling gRPC to update last activity")
                        .detail("sessionId", sessionId)
                        .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                        .log();
                
                // Call gRPC service asynchronously (non-blocking)
                CompletableFuture.runAsync(() -> {
                    try {
                        UpdateLastActivityRequest grpcRequest = UpdateLastActivityRequest.newBuilder()
                                .setSessionId(finalSessionId)
                                .build();
                        
                        UpdateLastActivityResponse response = sessionActivityStub.updateLastActivity(grpcRequest);
                        
                        if (response.getSuccess()) {
                            logger.info()
                                    .message("Successfully updated last activity timestamp via gRPC")
                                    .detail("sessionId", finalSessionId)
                                    .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                                    .log();
                        } else {
                            logger.warn()
                                    .message("Failed to update last activity")
                                    .detail("grpcMessage", response.getMessage())
                                    .detail("sessionId", finalSessionId)
                                    .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                                    .log();
                        }
                    } catch (Exception e) {
                        logger.error(e)
                                .message("Error updating last activity via gRPC")
                                .detail("sessionId", finalSessionId)
                                .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                                .log();
                    }
                });
            } else {
                logger.info()
                        .message("No X-Session-Id header found, skipping activity update")
                        .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                        .log();
            }
        } catch (Exception e) {
            // Log but don't fail the request
            logger.error(e)
                    .message("Error extracting sessionId for activity update")
                    .objectType(AppConstants.OBJECT_TYPE_PERMISSION_FILTER)
                    .log();
        }
    }
}
