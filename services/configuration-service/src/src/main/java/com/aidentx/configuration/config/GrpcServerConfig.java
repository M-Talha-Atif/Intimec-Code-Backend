package com.aidentx.configuration.config;

import com.aidentx.configuration.audit.UserContextInterceptor;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gRPC server interceptors.
 * Registers global interceptors for all gRPC services.
 */
@Configuration
public class GrpcServerConfig {

    private final UnifiedLoggerFactory unifiedLoggerFactory;

    public GrpcServerConfig(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.unifiedLoggerFactory = unifiedLoggerFactory;
    }

    /**
     * Registers the user context interceptor globally for all gRPC services.
     * This interceptor extracts user information from gRPC metadata and sets it
     * in thread-local context for audit purposes.
     */
    @Bean
    @GrpcGlobalServerInterceptor
    public UserContextInterceptor userContextInterceptor() {
        return new UserContextInterceptor(unifiedLoggerFactory);
    }
}
