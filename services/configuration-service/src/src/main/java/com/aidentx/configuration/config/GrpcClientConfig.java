package com.aidentx.configuration.config;

import com.aidentx.configuration.grpc.UserContextClientInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    /**
     * Global client interceptor that will be applied to all gRPC clients,
     * including the @GrpcClient("self") one.
     */
    @Bean
    @GrpcGlobalClientInterceptor
    public UserContextClientInterceptor userContextClientInterceptor() {
        return new UserContextClientInterceptor();
    }
}