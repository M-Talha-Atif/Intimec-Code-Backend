package com.aidentx.iam.config;

import com.aidentx.configuration.grpc.ConfigurationGrpcServiceGrpc;
import com.aidentx.auth.grpc.UserGrpcServiceGrpc;
import com.aidentx.dal.protos.identity.IdentityServiceGrpc;
import com.aidentx.dal.protos.runtime.ConnectorRuntimeServiceGrpc;
import io.grpc.Channel;
import net.devh.boot.grpc.client.inject.GrpcClient;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    private final IUnifiedLogger log;

    @GrpcClient("connector-runtime-service")
    private Channel runtimeChannel;

    @GrpcClient("configuration-service")
    private Channel configurationChannel;

    @GrpcClient("identities-service")
    private Channel identitiesChannel;

    @GrpcClient("identity-service")
    private Channel identitySchemaChannel;

    @GrpcClient("auth-service")
    private Channel authChannel;

    public GrpcClientConfig(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.log = unifiedLoggerFactory.getLogger(GrpcClientConfig.class);
    }

    @Bean
    public ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub connectorRuntimeClient() {
        if (runtimeChannel == null) {
            log.error().message(">>>> FAILED: runtimeChannel is null!").log();
        }
        return ConnectorRuntimeServiceGrpc.newBlockingStub(runtimeChannel);
    }

    @Bean
    public ConfigurationGrpcServiceGrpc.ConfigurationGrpcServiceBlockingStub configurationClient() {
        if (configurationChannel == null) {
            log.error().message(">>>> FAILED: configurationChannel is null!").log();
        }
        return ConfigurationGrpcServiceGrpc.newBlockingStub(configurationChannel);
    }

    @Bean
    public IdentityServiceGrpc.IdentityServiceBlockingStub identityServiceClient() {
        if (identitiesChannel == null) {
            log.error().message(">>>> FAILED: identitiesChannel is null!").log();
        }
        return IdentityServiceGrpc.newBlockingStub(identitiesChannel);
    }

    @Bean
    public UserGrpcServiceGrpc.UserGrpcServiceBlockingStub authUserClient() {
        if (authChannel == null) {
            log.error().message(">>>> FAILED: authChannel is null!").log();
        }
        return UserGrpcServiceGrpc.newBlockingStub(authChannel);
    }
}