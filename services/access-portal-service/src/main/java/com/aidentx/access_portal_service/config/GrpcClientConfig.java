// File: GrpcClientConfig.java
package com.aidentx.access_portal_service.config;

import com.aidentx.access_portal_service.security.UserContext;
import com.aidentx.access_portal_service.security.UserContextHolder;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.grpc.*;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC client configuration for access-portal-service.
 * Provides global interceptor to inject user context into gRPC calls.
 */
@Configuration
public class GrpcClientConfig {

    // Logger category constants
    private static final String LOG_CATEGORY_GRPC_CONFIG = "GRPC_CONFIG";
    private static final String LOG_GRPC_INTERCEPTING_CALL = "Intercepting gRPC call";
    private static final String LOG_GRPC_ADDED_USER_CONTEXT = "Added user context to gRPC metadata";
    private static final String LOG_GRPC_NO_USER_CONTEXT = "No user context available for gRPC call";

    private static IUnifiedLogger staticLogger;

    public GrpcClientConfig(UnifiedLoggerFactory loggerFactory) {
        staticLogger = loggerFactory.getLogger(GrpcClientConfig.class, LOG_CATEGORY_GRPC_CONFIG);
    }

    /**
     * Global client interceptor to add user context to gRPC calls.
     * Extracts user information from UserContextHolder and adds it to gRPC metadata.
     * This interceptor runs for ALL gRPC calls made by this service.
     */
    @GrpcGlobalClientInterceptor
    public ClientInterceptor userContextInterceptor() {
        return new UserContextClientInterceptor();
    }

    /**
     * Custom client interceptor that propagates user context to gRPC services.
     * This ensures all gRPC calls carry the authenticated user's information.
     */
    private static class UserContextClientInterceptor implements ClientInterceptor {

        // Metadata keys for headers - these will be available on the server side
        private static final Metadata.Key<String> TENANT_ID_KEY =
                Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);
        private static final Metadata.Key<String> USER_ID_KEY =
                Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);
        private static final Metadata.Key<String> USERNAME_KEY =
                Metadata.Key.of("x-username", Metadata.ASCII_STRING_MARSHALLER);
        private static final Metadata.Key<String> CORRELATION_ID_KEY =
                Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);
        private static final Metadata.Key<String> CLIENT_NAME_KEY =
                Metadata.Key.of("x-client-name", Metadata.ASCII_STRING_MARSHALLER);

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions,
                Channel next) {

            staticLogger.debug()
                    .message(LOG_GRPC_INTERCEPTING_CALL)
                    .category(LOG_CATEGORY_GRPC_CONFIG)
                    .detail("methodName", method.getFullMethodName())
                    .detail("serviceName", method.getServiceName())
                    .log();

            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {

                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    // Get user context from thread local (populated by Spring Security filter)
                    UserContext context = UserContextHolder.getContext();

                    if (context != null) {
                        // Add user context to gRPC metadata headers
                        // These headers will be received by the gRPC server
                        if (context.tenantId() != null && !context.tenantId().isBlank()) {
                            headers.put(TENANT_ID_KEY, context.tenantId());
                        }
                        if (context.userId() != null && !context.userId().isBlank()) {
                            headers.put(USER_ID_KEY, context.userId());
                        }
                        if (context.username() != null && !context.username().isBlank()) {
                            headers.put(USERNAME_KEY, context.username());
                        }

//                        // Add correlation ID for distributed tracing
//                        if (context.correlationId() != null && !context.correlationId().isBlank()) {
//                            headers.put(CORRELATION_ID_KEY, context.correlationId());
//                        }

                        staticLogger.debug()
                                .message(LOG_GRPC_ADDED_USER_CONTEXT)
                                .category(LOG_CATEGORY_GRPC_CONFIG)
                                .detail("tenantId", context.tenantId())
                                .detail("userId", context.userId())
                                .detail("username", context.username())
                                .log();
                    } else {
                        staticLogger.warn()
                                .message(LOG_GRPC_NO_USER_CONTEXT)
                                .category(LOG_CATEGORY_GRPC_CONFIG)
                                .detail("methodName", method.getFullMethodName())
                                .log();
                    }

                    // Always add client identification
                    headers.put(CLIENT_NAME_KEY, "access-portal-service");

                    super.start(responseListener, headers);
                }
            };
        }
    }
}