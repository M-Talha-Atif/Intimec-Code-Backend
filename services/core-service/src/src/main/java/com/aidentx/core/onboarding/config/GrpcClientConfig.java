package com.aidentx.core.onboarding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aidentx.configuration.grpc.ConfigurationGrpcServiceGrpc;
import com.aidentx.core.shared.grpc.GrpcClientContextInterceptor;
import com.aidentx.dal.protos.identity.IdentityQueryServiceGrpc;
import com.aidentx.dal.protos.identity.IdentityServiceGrpc;
import com.aidentx.dal.protos.identity_schema.IdentitySchemaServiceGrpc;
import com.aidentx.dal.protos.runtime.ConnectorRuntimeServiceGrpc;
import com.aidentx.dal.protos.shadow.ShadowSyncServiceGrpc;
import com.aidentx.iam.contracts.grpc.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Configuration
public class GrpcClientConfig {

    private final IUnifiedLogger logger;

    @GrpcClient("connector-runtime-service")
    private Channel runtimeChannel;

    @GrpcClient("configuration-service")
    private Channel configurationChannel;

    @GrpcClient("identity-service")
    private Channel identitiesChannel;

    @GrpcClient("identity-service")
    private Channel governanceChannel;

    public GrpcClientConfig(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.logger = unifiedLoggerFactory.getLogger(GrpcClientConfig.class);
    }

    @Bean
    public GrpcClientContextInterceptor grpcClientContextInterceptor(UnifiedLoggerFactory unifiedLoggerFactory) {
        return new GrpcClientContextInterceptor(unifiedLoggerFactory);
    }

    @Bean
    public ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub connectorRuntimeClient(
            GrpcClientContextInterceptor interceptor) {
        logger.info().message("Creating connectorRuntimeClient with interceptor").log();
        Channel interceptedChannel = ClientInterceptors.intercept(runtimeChannel, interceptor);
        return ConnectorRuntimeServiceGrpc.newBlockingStub(interceptedChannel);
    }

    @Bean
    public ConfigurationGrpcServiceGrpc.ConfigurationGrpcServiceBlockingStub configurationClient(
            GrpcClientContextInterceptor interceptor) {
        logger.info().message("Creating configurationClient with interceptor").log();
        Channel interceptedChannel = ClientInterceptors.intercept(configurationChannel, interceptor);
        return ConfigurationGrpcServiceGrpc.newBlockingStub(interceptedChannel);
    }

    @Bean
    public IdentityServiceGrpc.IdentityServiceBlockingStub identityServiceClient(
            GrpcClientContextInterceptor interceptor) {
        logger.info().message("Creating identityServiceClient with interceptor").log();
        Channel interceptedChannel = ClientInterceptors.intercept(identitiesChannel, interceptor);
        return IdentityServiceGrpc.newBlockingStub(interceptedChannel);
    }

    @Bean
    public IdentityQueryServiceGrpc.IdentityQueryServiceBlockingStub identityQueryClient(
            GrpcClientContextInterceptor interceptor) {
        logger.info().message("Creating identityQueryClient with interceptor").log();
        Channel interceptedChannel = ClientInterceptors.intercept(identitiesChannel, interceptor);
        return IdentityQueryServiceGrpc.newBlockingStub(interceptedChannel);
    }

    @Bean
    public IdentitySchemaServiceGrpc.IdentitySchemaServiceBlockingStub identitySchemaClient(
            GrpcClientContextInterceptor interceptor) {
        logger.info().message("Creating identitySchemaClient with interceptor").log();
        Channel interceptedChannel = ClientInterceptors.intercept(identitiesChannel, interceptor);
        return IdentitySchemaServiceGrpc.newBlockingStub(interceptedChannel);
    }

    @Bean
    public GovernancePolicyServiceGrpc.GovernancePolicyServiceBlockingStub governancePolicyClient(
            GrpcClientContextInterceptor interceptor) {
        logger.info().message("Creating governancePolicyClient with interceptor").log();
        Channel interceptedChannel = ClientInterceptors.intercept(governanceChannel, interceptor);
        return GovernancePolicyServiceGrpc.newBlockingStub(interceptedChannel);
    }

    @Bean
    public ShadowSyncServiceGrpc.ShadowSyncServiceBlockingStub shadowSyncServiceClient(
            GrpcClientContextInterceptor interceptor) {
        logger.info().message("Creating shadowSyncServiceClient with interceptor").log();
        Channel interceptedChannel = ClientInterceptors.intercept(identitiesChannel, interceptor);
        return ShadowSyncServiceGrpc.newBlockingStub(interceptedChannel);
    }
}