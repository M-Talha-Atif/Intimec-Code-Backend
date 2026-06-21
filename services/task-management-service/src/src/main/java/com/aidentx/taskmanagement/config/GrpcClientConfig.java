package com.aidentx.taskmanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aidentx.common.protos.UserIdentityServiceGrpc;
import com.aidentx.dal.protos.application.ApplicationServiceGrpc;
import com.aidentx.dal.protos.runtime.ConnectorRuntimeServiceGrpc;
import com.aidentx.dal.protos.shadow.ShadowSyncServiceGrpc;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for gRPC clients used by task management service. Configures connections to
 * core-service and connector-runtime-service.
 */
@Configuration
public class GrpcClientConfig {

  private final IUnifiedLogger logger;

  @Value("${core-service.grpc.host:localhost}")
  private String coreServiceHost;

  @Value("${core-service.grpc.port:9095}")
  private int coreServicePort;

  @Value("${connector-runtime-service.grpc.host:localhost}")
  private String connectorRuntimeHost;

  @Value("${connector-runtime-service.grpc.port:9094}")
  private int connectorRuntimePort;

  @Value("${identity-service.grpc.host:localhost}")
  private String identityServiceHost;

  @Value("${identity-service.grpc.port:9096}")
  private int identityServicePort;

  @Value("${auth-service.grpc.host:localhost}")
  private String authServiceHost;

  @Value("${auth-service.grpc.port:8002}")
  private int authServicePort;

  public GrpcClientConfig(UnifiedLoggerFactory loggerFactory) {
    this.logger = loggerFactory.getLogger(GrpcClientConfig.class);
  }

  /** Creates a ManagedChannel for core-service gRPC communication. */
  @Bean
  public ManagedChannel coreServiceChannel() {
    logger
        .info()
        .message("Creating gRPC channel for core-service")
        .detail("host", coreServiceHost)
        .detail("port", coreServicePort)
        .log();

    return ManagedChannelBuilder.forTarget("dns:///" + coreServiceHost + ":" + coreServicePort)
        .usePlaintext()
        .defaultLoadBalancingPolicy("round_robin")
        .keepAliveTime(5, TimeUnit.MINUTES)
        .keepAliveTimeout(20, TimeUnit.SECONDS)
        .keepAliveWithoutCalls(false)
        .idleTimeout(5, TimeUnit.MINUTES)
        .build();
  }

  /**
   * Creates a blocking stub for ApplicationService. Used to fetch application configurations from
   * core-service.
   */
  @Bean
  public ApplicationServiceGrpc.ApplicationServiceBlockingStub applicationServiceStub(
      ManagedChannel coreServiceChannel) {
    logger.info().message("Creating gRPC blocking stub for ApplicationService").log();
    return ApplicationServiceGrpc.newBlockingStub(coreServiceChannel);
  }

  /** Creates a ManagedChannel for connector-runtime-service gRPC communication. */
  @Bean
  public ManagedChannel connectorRuntimeChannel() {
    logger
        .info()
        .message("Creating gRPC channel for connector-runtime-service")
        .detail("host", connectorRuntimeHost)
        .detail("port", connectorRuntimePort)
        .log();

    return ManagedChannelBuilder.forTarget("dns:///" + connectorRuntimeHost + ":" + connectorRuntimePort)
        .usePlaintext()
        .defaultLoadBalancingPolicy("round_robin")
        .keepAliveTime(5, TimeUnit.MINUTES)
        .keepAliveTimeout(20, TimeUnit.SECONDS)
        .keepAliveWithoutCalls(false)
        .idleTimeout(5, TimeUnit.MINUTES)
        .build();
  }

  /**
   * Creates a blocking stub for ConnectorRuntimeService. Used to make synchronous gRPC calls to
   * connector operations.
   */
  @Bean
  public ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub connectorRuntimeStub(
      ManagedChannel connectorRuntimeChannel) {
    logger.info().message("Creating gRPC blocking stub for ConnectorRuntimeService").log();
    return ConnectorRuntimeServiceGrpc.newBlockingStub(connectorRuntimeChannel);
  }

  /** Creates a ManagedChannel for identity-service gRPC communication. */
  @Bean
  public ManagedChannel identityServiceChannel() {
    logger
        .info()
        .message("Creating gRPC channel for identity-service")
        .detail("host", identityServiceHost)
        .detail("port", identityServicePort)
        .log();

    return ManagedChannelBuilder.forTarget("dns:///" + identityServiceHost + ":" + identityServicePort)
        .usePlaintext()
        .defaultLoadBalancingPolicy("round_robin")
        .keepAliveTime(5, TimeUnit.MINUTES)
        .keepAliveTimeout(20, TimeUnit.SECONDS)
        .keepAliveWithoutCalls(false)
        .idleTimeout(5, TimeUnit.MINUTES)
        .build();
  }

  /** Creates a ManagedChannel for auth-service gRPC communication (UserIdentityService). */
  @Bean
  public ManagedChannel authServiceChannel() {
    logger
        .info()
        .message("Creating gRPC channel for auth-service")
        .detail("host", authServiceHost)
        .detail("port", authServicePort)
        .log();

    return ManagedChannelBuilder.forTarget("dns:///" + authServiceHost + ":" + authServicePort)
        .usePlaintext()
        .defaultLoadBalancingPolicy("round_robin")
        .keepAliveTime(5, TimeUnit.MINUTES)
        .keepAliveTimeout(20, TimeUnit.SECONDS)
        .keepAliveWithoutCalls(false)
        .idleTimeout(5, TimeUnit.MINUTES)
        .build();
  }

  /**
   * Creates a blocking stub for UserIdentityService. Used to fetch user/identity names by ID.
   * Connects to auth-service which implements UserIdentityService/GetUserIdentitiesByIds.
   */
  @Bean
  public UserIdentityServiceGrpc.UserIdentityServiceBlockingStub userIdentityServiceStub(
      ManagedChannel authServiceChannel) {
    logger.info().message("Creating gRPC blocking stub for UserIdentityService (auth-service)").log();
    return UserIdentityServiceGrpc.newBlockingStub(authServiceChannel);
  }

  /**
   * Creates a blocking stub for ShadowSyncService. Used to process imported objects
   * during sync tasks: create/update shadows, run correlation, and link identities.
   */
  @Bean
  public ShadowSyncServiceGrpc.ShadowSyncServiceBlockingStub shadowSyncServiceStub(
      ManagedChannel identityServiceChannel) {
    logger.info().message("Creating gRPC blocking stub for ShadowSyncService").log();
    return ShadowSyncServiceGrpc.newBlockingStub(identityServiceChannel);
  }
}
