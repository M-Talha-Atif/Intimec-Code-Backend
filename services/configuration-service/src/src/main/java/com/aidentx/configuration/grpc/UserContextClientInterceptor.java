package com.aidentx.configuration.grpc;

import com.aidentx.configuration.audit.AuditUser;
import com.aidentx.configuration.audit.UserContextHolder;
import com.aidentx.configuration.security.SecurityConstants;
import io.grpc.*;

/**
 * A gRPC client interceptor that injects the current user context
 * from the UserContextHolder into the outgoing request metadata.
 * This ensures that every internal gRPC call made from a controller
 * carries the necessary audit information.
 */
public class UserContextClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                AuditUser currentUser = UserContextHolder.getCurrentUser();
                if (currentUser != null) {
                    headers.put(Metadata.Key.of(SecurityConstants.HEADER_USER_ID, Metadata.ASCII_STRING_MARSHALLER),
                            currentUser.id());
                    headers.put(Metadata.Key.of(SecurityConstants.HEADER_USERNAME, Metadata.ASCII_STRING_MARSHALLER),
                            currentUser.username());
                    headers.put(Metadata.Key.of(SecurityConstants.HEADER_IP_ADDRESS, Metadata.ASCII_STRING_MARSHALLER),
                            currentUser.ipAddress());
                }
                super.start(responseListener, headers);
            }
        };
    }
}