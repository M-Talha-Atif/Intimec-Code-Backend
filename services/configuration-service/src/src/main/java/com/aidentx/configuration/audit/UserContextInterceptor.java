package com.aidentx.configuration.audit;

import com.aidentx.configuration.config.AppConstants;
import com.aidentx.configuration.security.SecurityConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.grpc.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * gRPC server interceptor that extracts user context from metadata
 * and sets it in the thread-local context for audit purposes.
 */
@Component
public class UserContextInterceptor implements ServerInterceptor {

    private final IUnifiedLogger logger;

    // Metadata keys for user context
    private static final Metadata.Key<String> USER_ID_KEY = Metadata.Key.of(SecurityConstants.HEADER_USER_ID,
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> USERNAME_KEY = Metadata.Key.of(SecurityConstants.HEADER_USERNAME,
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> IP_ADDRESS_KEY = Metadata.Key.of(SecurityConstants.HEADER_IP_ADDRESS,
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TENANT_ID_KEY = Metadata.Key.of(SecurityConstants.HEADER_TENANT_ID, Metadata.ASCII_STRING_MARSHALLER);

    public UserContextInterceptor(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.logger = unifiedLoggerFactory.getLogger(
                AppConstants.LOGGER_NAME_USER_CONTEXT_INTERCEPTOR,
                AppConstants.LOG_CATEGORY_GRPC_USER_CONTEXT);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // Extract user information from metadata
        String userId = headers.get(USER_ID_KEY);
        String username = headers.get(USERNAME_KEY);
        String ipAddress = headers.get(IP_ADDRESS_KEY);
        String tenantId = headers.get(TENANT_ID_KEY);
        logger.debug()
                .category(AppConstants.LOG_CATEGORY_GRPC_USER_CONTEXT)
                .message("gRPC server interceptor extracting user context")
                .details(Map.of(
                        "userIdPresent", userId != null && !userId.isBlank(),
                        "usernamePresent", username != null && !username.isBlank(),
                        "ipAddressPresent", ipAddress != null && !ipAddress.isBlank(),
                        "methodName", call.getMethodDescriptor().getFullMethodName()))
                .log();

        if (userId != null && !userId.isBlank() && username != null && !username.isBlank()) {
            String finalIpAddress = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress
                    : AppConstants.UNKNOWN_IP_ADDRESS;
            AuditUser user = new AuditUser(userId, username, finalIpAddress, tenantId);
            UserContextHolder.setCurrentUser(user);

            logger.debug()
                    .category(AppConstants.LOG_CATEGORY_GRPC_USER_CONTEXT)
                    .message(AppConstants.LOG_MSG_SET_USER_CONTEXT_FROM_METADATA)
                    .details(Map.of(
                            AppConstants.LOG_DETAIL_USER_ID, userId,
                            AppConstants.LOG_DETAIL_USERNAME, username))
                    .log();
        } else {
            logger.warn()
                    .category(AppConstants.LOG_CATEGORY_GRPC_USER_CONTEXT)
                    .message("No valid user context metadata found in gRPC headers. Proceeding without context.")
                    .details(Map.of(
                            "userId", userId != null ? userId : "null",
                            "username", username != null ? username : "null",
                            "methodName", call.getMethodDescriptor().getFullMethodName()))
                    .log();
        }

        // Create a new context that will be used for the call's execution
        Context context = Context.current();
        // The startCall method returns the next listener in the chain
        ServerCall.Listener<ReqT> delegate = Contexts.interceptCall(context, call, headers, next);

        // Return our custom listener wrapper which ensures cleanup
        return new ContextualizedListener<>(delegate);
    }

    /**
     * A listener wrapper that ensures the UserContextHolder is cleared
     * after the gRPC call is complete or cancelled, preventing context leakage.
     * This correctly extends the standard gRPC ForwardingServerCallListener.
     */
    private static class ContextualizedListener<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        ContextualizedListener(ServerCall.Listener<ReqT> delegate) {
            super(delegate);
        }

        @Override
        public void onCancel() {
            try {
                super.onCancel();
            } finally {
                UserContextHolder.clear();
            }
        }

        @Override
        public void onComplete() {
            try {
                super.onComplete();
            } finally {
                UserContextHolder.clear();
            }
        }
    }
}