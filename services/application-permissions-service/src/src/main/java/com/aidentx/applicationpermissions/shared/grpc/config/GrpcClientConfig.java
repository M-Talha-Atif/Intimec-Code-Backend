package com.aidentx.applicationpermissions.shared.grpc.config;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;

import com.aidentx.applicationpermissions.shared.security.UserContextHolder;
import com.aidentx.applicationpermissions.shared.security.UserContext;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import java.util.Map;

/**
 * gRPC Client Configuration for application-permissions-service.
 * Configures gRPC clients with proper interceptors for security context propagation.
 */
@Configuration
public class GrpcClientConfig {

    private final IUnifiedLogger logger;

    public GrpcClientConfig(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(AppConstants.GRPC_CLIENT_CONFIG_LOGGER_NAME, AppConstants.LOG_CATEGORY_GRPC_CLIENT);
    }

    /**
     * Global client interceptor to add user context to gRPC calls.
     * This interceptor automatically adds tenant ID, user ID, and username
     * to outgoing gRPC calls for proper authorization and context.
     */
    @GrpcGlobalClientInterceptor
    @Bean
    public ClientInterceptor userContextInterceptor() {
        return new UserContextInterceptor(logger);
    }

    /**
     * Custom client interceptor that propagates user context to gRPC services.
     * Extracts user information from UserContextHolder and adds it to gRPC metadata.
     */
    private static class UserContextInterceptor implements ClientInterceptor {

        private final IUnifiedLogger logger;

        public UserContextInterceptor(IUnifiedLogger logger) {
            this.logger = logger;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method, 
                CallOptions callOptions, 
                Channel next) {

            logger.debug()
                .message(AppConstants.LOG_INTERCEPTING_GRPC_CALL)
                .details(Map.of(AppConstants.DETAIL_KEY_METHOD, method.getFullMethodName()))
                .log();

            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {

                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    try {
                        // Get user context from thread local
                        UserContext userContext = UserContextHolder.getContext();
                        
                        if (userContext != null) {
                            // Add user context to gRPC metadata
                            Metadata.Key<String> tenantIdKey = Metadata.Key.of(AppConstants.GRPC_METADATA_TENANT_ID, Metadata.ASCII_STRING_MARSHALLER);
                            Metadata.Key<String> userIdKey = Metadata.Key.of(AppConstants.GRPC_METADATA_USER_ID, Metadata.ASCII_STRING_MARSHALLER);
                            Metadata.Key<String> usernameKey = Metadata.Key.of(AppConstants.GRPC_METADATA_USERNAME, Metadata.ASCII_STRING_MARSHALLER);

                            headers.put(tenantIdKey, userContext.tenantId());
                            headers.put(userIdKey, userContext.userId());
                            headers.put(usernameKey, userContext.username());

                            logger.debug()
                                .message(AppConstants.LOG_ADDED_USER_CONTEXT_TO_GRPC_HEADERS)
                                .details(Map.of(
                                    AppConstants.DETAIL_KEY_TENANT_ID, userContext.tenantId(),
                                    AppConstants.DETAIL_KEY_USER_ID, userContext.userId(),
                                    AppConstants.DETAIL_KEY_USERNAME, userContext.username(),
                                    AppConstants.DETAIL_KEY_METHOD, method.getFullMethodName()
                                ))
                                .log();
                        } else {
                            logger.warn()
                                .message(AppConstants.LOG_NO_USER_CONTEXT_AVAILABLE_FOR_GRPC_CALL)
                                .details(Map.of(AppConstants.DETAIL_KEY_METHOD, method.getFullMethodName()))
                                .log();
                        }
                    } catch (Exception e) {
                        logger.error(e)
                            .message(AppConstants.LOG_ERROR_ADDING_USER_CONTEXT_TO_GRPC_HEADERS)
                            .details(Map.of(AppConstants.DETAIL_KEY_METHOD, method.getFullMethodName()))
                            .log();
                    }

                    super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                        @Override
                        public void onClose(io.grpc.Status status, Metadata trailers) {
                            logger.debug()
                                .message(AppConstants.LOG_GRPC_CALL_COMPLETED)
                                .details(Map.of(
                                    AppConstants.DETAIL_KEY_METHOD, method.getFullMethodName(),
                                    AppConstants.DETAIL_KEY_STATUS, status.getCode().toString()
                                ))
                                .log();
                            super.onClose(status, trailers);
                        }
                    }, headers);
                }
            };
        }
    }
}