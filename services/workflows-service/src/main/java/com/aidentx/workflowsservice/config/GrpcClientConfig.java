package com.aidentx.workflowsservice.config;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.security.UserContext;
import com.aidentx.workflowsservice.security.UserContextHolder;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Configuration;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * gRPC client configuration for workflows-service.
 * Provides global interceptor to inject user context into gRPC calls.
 */
@Configuration
public class GrpcClientConfig {
    
    private static IUnifiedLogger staticLogger;
    
    public GrpcClientConfig(UnifiedLoggerFactory loggerFactory) {
        staticLogger = loggerFactory.getLogger(GrpcClientConfig.class, LOG_CATEGORY_WORKFLOW_CONFIG);
    }

    /**
     * Global client interceptor to add user context to gRPC calls.
     * Extracts user information from UserContextHolder and adds it to gRPC metadata.
     */
    @GrpcGlobalClientInterceptor
    public ClientInterceptor userContextInterceptor() {
        return new UserContextClientInterceptor();
    }

    /**
     * Custom client interceptor that propagates user context to gRPC services.
     */
    private static class UserContextClientInterceptor implements ClientInterceptor {

        private static final Metadata.Key<String> TENANT_ID_KEY = 
            Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);
        private static final Metadata.Key<String> USER_ID_KEY = 
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);
        private static final Metadata.Key<String> USERNAME_KEY = 
            Metadata.Key.of("x-username", Metadata.ASCII_STRING_MARSHALLER);
        private static final Metadata.Key<String> IP_ADDRESS_KEY = 
            Metadata.Key.of("x-ip-address", Metadata.ASCII_STRING_MARSHALLER);

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method, 
                CallOptions callOptions, 
                Channel next) {

            staticLogger.debug()
                .message(LOG_GRPC_INTERCEPTING_CALL)
                .category(LOG_CATEGORY_WORKFLOW_CONFIG)
                .detail("methodName", method.getFullMethodName())
                .log();

            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {

                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    // Get user context from thread local
                    UserContext context = UserContextHolder.getContext();
                    
                    if (context != null) {
                        // Add user context to gRPC metadata
                        if (context.tenantId() != null && !context.tenantId().isBlank()) {
                            headers.put(TENANT_ID_KEY, context.tenantId());
                        }
                        if (context.userId() != null && !context.userId().isBlank()) {
                            headers.put(USER_ID_KEY, context.userId());
                        }
                        if (context.username() != null && !context.username().isBlank()) {
                            headers.put(USERNAME_KEY, context.username());
                        }
                        if (context.ipAddress() != null && !context.ipAddress().isBlank()) {
                            headers.put(IP_ADDRESS_KEY, context.ipAddress());
                        }

                        staticLogger.debug()
                            .message(LOG_GRPC_ADDED_USER_CONTEXT)
                            .category(LOG_CATEGORY_WORKFLOW_CONFIG)
                            .detail("tenantId", context.tenantId())
                            .detail("userId", context.userId())
                            .detail("username", context.username())
                            .log();
                    } else {
                        staticLogger.warn()
                            .message(LOG_GRPC_NO_USER_CONTEXT)
                            .category(LOG_CATEGORY_WORKFLOW_CONFIG)
                            .detail("methodName", method.getFullMethodName())
                            .log();
                    }

                    super.start(responseListener, headers);
                }
            };
        }
    }
}
