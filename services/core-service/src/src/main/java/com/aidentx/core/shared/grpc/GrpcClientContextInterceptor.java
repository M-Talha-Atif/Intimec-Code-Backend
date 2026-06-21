package com.aidentx.core.shared.grpc;

import com.aidentx.core.shared.security.UserContext;
import com.aidentx.core.shared.security.UserContextHolder;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import com.aidentx.core.shared.security.SecurityConstants;

/**
 * A gRPC client interceptor that injects the user context from the ThreadLocal
 * into the metadata of every outgoing gRPC call.
 */
public class GrpcClientContextInterceptor implements ClientInterceptor {

    private final IUnifiedLogger logger;

    public GrpcClientContextInterceptor(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.logger = unifiedLoggerFactory.getLogger(GrpcClientContextInterceptor.class);
        logger.info().message(SecurityConstants.GRPC_INTERCEPTOR_INSTANTIATED).log();
    }

    // These keys MUST EXACTLY MATCH the keys the configuration service is
    // expecting.
    // Use lowercase as is standard gRPC convention.
    public static final Metadata.Key<String> USER_ID_KEY = Metadata.Key.of("x-user-id",
            Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> USERNAME_KEY = Metadata.Key.of("x-username",
            Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> IP_ADDRESS_KEY = Metadata.Key.of("x-ip-address",
            Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        logger.info().message(SecurityConstants.GRPC_INTERCEPT_CALL + method.getFullMethodName()).log();

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                logger.info().message(SecurityConstants.GRPC_START_CALL + method.getFullMethodName()).log();

                // Read the context from the ThreadLocal holder
                UserContext context = UserContextHolder.getContext();

                logger.info().message(SecurityConstants.GRPC_USER_CONTEXT + context).log();

                if (context != null) {
                    // Attach the user's identity to the outgoing call's metadata (skip
                    // nulls/blanks)
                    String userId = context.userId();
                    String username = context.username();
                    String ipAddress = context.ipAddress();

                    if (userId != null && !userId.isBlank()) {
                        headers.put(USER_ID_KEY, userId);
                    } else {
                        logger.debug().message(SecurityConstants.GRPC_USER_ID_MISSING).log();
                    }

                    if (username != null && !username.isBlank()) {
                        headers.put(USERNAME_KEY, username);
                    } else {
                        logger.debug().message(SecurityConstants.GRPC_USERNAME_MISSING).log();
                    }

                    if (ipAddress != null && !ipAddress.isBlank()) {
                        headers.put(IP_ADDRESS_KEY, ipAddress);
                    } else {
                        logger.debug().message(SecurityConstants.GRPC_IP_MISSING).log();
                    }

                    logger.info()
                            .message(String.format(SecurityConstants.GRPC_HEADERS_ADDED, userId, username, ipAddress))
                            .log();
                } else {
                    logger.warn().message(SecurityConstants.GRPC_NO_USER_CONTEXT).log();
                }

                super.start(responseListener, headers);
            }
        };
    }
}