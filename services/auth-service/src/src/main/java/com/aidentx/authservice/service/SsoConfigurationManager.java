package com.aidentx.authservice.service;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.config.LdapConfig;
import com.aidentx.authservice.config.OAuth2Config;
import com.aidentx.authservice.config.SamlConfig;
import com.aidentx.configuration.grpc.ConfigurationGrpcServiceGrpc;
import com.aidentx.configuration.grpc.GetConfigurationByTenantRequest;
import com.aidentx.configuration.grpc.GetConfigurationResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import jakarta.servlet.http.HttpServletRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Manager for SSO provider configurations that acts as a client to the remote
 * configuration-service.
 * <p>
 * This service is the primary point of interaction for SSO settings within the
 * auth-service.
 * It translates business-specific needs (e.g., "get LDAP config") into gRPC
 * calls to the
 * centralized configuration-service. It is marked with {@link RefreshScope} to
 * ensure it
 * re-fetches configurations when a refresh event is received on the Spring
 * Cloud Bus.
 * </p>
 */
@Service
@RefreshScope
public class SsoConfigurationManager {

    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;

    // gRPC metadata keys for user context
    private static final Metadata.Key<String> USER_ID_KEY = Metadata.Key.of(AppConstants.HEADER_USER_ID,
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> USERNAME_KEY = Metadata.Key.of(AppConstants.HEADER_USERNAME,
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> IP_ADDRESS_KEY = Metadata.Key.of(AppConstants.HEADER_IP_ADDRESS,
            Metadata.ASCII_STRING_MARSHALLER);

    @GrpcClient(AppConstants.GRPC_CLIENT_CONFIGURATION_SERVICE)
    private ConfigurationGrpcServiceGrpc.ConfigurationGrpcServiceBlockingStub configurationClient;

    public SsoConfigurationManager(ObjectMapper objectMapper, UnifiedLoggerFactory unifiedLoggerFactory) {
        this.objectMapper = objectMapper.copy();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.logger = unifiedLoggerFactory.getLogger(
                AppConstants.LOGGER_NAME_SSO_CONFIG_MANAGER,
                AppConstants.LOGGER_TOPIC_CONFIG_SSO);
    }

    /**
     * Retrieves the LDAP configuration.
     */
    public LdapConfig getLdapConfig() {
        logger.trace().message(AppConstants.LOG_MSG_GET_LDAP_CONFIG).log();
        return getConfig(AppConstants.SSO_LDAP_CONFIG, LdapConfig.class);
    }

    /**
     * Retrieves the SAML configuration.
     */
    public SamlConfig getSamlConfig() {
        logger.trace().message(AppConstants.LOG_MSG_GET_SAML_CONFIG).log();
        return getConfig(AppConstants.SSO_SAML_CONFIG, SamlConfig.class);
    }

    /**
     * Retrieves the OAuth2 configuration.
     */
    public OAuth2Config getOAuth2Config() {
        logger.trace().message(AppConstants.LOG_MSG_GET_OAUTH2_CONFIG).log();
        return getConfig(AppConstants.SSO_OAUTH2_CONFIG, OAuth2Config.class);
    }

    /**
     * A generic helper method to retrieve and map configuration data via gRPC.
     */
    private <T> T getConfig(String configKey, Class<T> configClass) {
        logger.trace().message(AppConstants.LOG_MSG_EXECUTE_GRPC_GET_CONFIG)
                .detail(AppConstants.LOG_DETAIL_CONFIG_KEY, configKey).log();
        try {
            Metadata metadata = createUserContextMetadata();
            ConfigurationGrpcServiceGrpc.ConfigurationGrpcServiceBlockingStub clientWithContext = configurationClient
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

        GetConfigurationByTenantRequest grpcRequest = GetConfigurationByTenantRequest.newBuilder()
            .setConfigType(AppConstants.CONFIG_TYPE_SSO)
            .setTenantId("tenant_001")
            .setConfigKey(configKey)
            .build();

        GetConfigurationResponse grpcResponse = clientWithContext.getConfigurationByTenant(grpcRequest);

            if (!grpcResponse.getFound()) {
                logger.warn().message(AppConstants.LOG_MSG_CONFIG_NOT_FOUND_GRPC)
                        .detail(AppConstants.LOG_DETAIL_CONFIG_KEY, configKey).log();
                return null;
            }

            String responseJson = JsonFormat.printer().print(grpcResponse.getJsonData());
            JsonNode responseWrapperNode = objectMapper.readTree(responseJson);
            JsonNode configDataNode = responseWrapperNode.path(AppConstants.JSON_DATA_FIELD);

            if (configDataNode.isMissingNode()) {
                logger.error().message(AppConstants.LOG_MSG_DATA_FIELD_MISSING_IN_RESPONSE)
                        .detail(AppConstants.LOG_DETAIL_CONFIG_KEY, configKey).log();
                return null;
            }

            T configObject = objectMapper.treeToValue(configDataNode, configClass);
            logger.info().message(AppConstants.LOG_MSG_CONFIG_RETRIEVED_SUCCESS)
                    .detail(AppConstants.LOG_DETAIL_CONFIG_KEY, configKey).log();
            return configObject;

        } catch (StatusRuntimeException e) {
            logger.error(e).message(AppConstants.LOG_MSG_GRPC_CALL_FAILED)
                    .detail(AppConstants.LOG_DETAIL_CONFIG_KEY, configKey)
                    .detail(AppConstants.LOG_DETAIL_GRPC_STATUS, e.getStatus()).log();
            return null;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_MSG_PROCESS_GRPC_RESPONSE_FAILED)
                    .detail(AppConstants.LOG_DETAIL_CONFIG_KEY, configKey).log();
            return null;
        }
    }

    /**
     * Creates gRPC metadata with user context.
     */
    private Metadata createUserContextMetadata() {
        Metadata metadata = new Metadata();
        HttpServletRequest request = getCurrentHttpRequest();

        if (request != null) {
            String userId = request.getHeader(AppConstants.HEADER_USER_ID);
            String username = request.getHeader(AppConstants.HEADER_USERNAME);
            String ipAddress = request.getHeader(AppConstants.HEADER_IP_ADDRESS);

            if (userId != null && username != null) {
                String finalIpAddress = ipAddress != null ? ipAddress : AppConstants.UNKNOWN_IP_ADDRESS;
                metadata.put(USER_ID_KEY, userId);
                metadata.put(USERNAME_KEY, username);
                metadata.put(IP_ADDRESS_KEY, finalIpAddress);

                logger.debug().message(AppConstants.LOG_MSG_USER_CONTEXT_FROM_HEADERS)
                        .details(Map.of(
                                AppConstants.LOG_DETAIL_USER_ID, userId,
                                AppConstants.LOG_DETAIL_USERNAME, username,
                                AppConstants.LOG_DETAIL_IP_ADDRESS, finalIpAddress))
                        .log();
                return metadata;
            }
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                String userId = null;
                String username = null;

                if (principal instanceof UserDetails) {
                    username = ((UserDetails) principal).getUsername();
                    userId = username;
                } else if (principal instanceof String) {
                    username = (String) principal;
                    userId = username;
                } else {
                    username = principal.toString();
                    userId = username;
                }

                if (userId != null) {
                    metadata.put(USER_ID_KEY, userId);
                    metadata.put(USERNAME_KEY, username);
                    metadata.put(IP_ADDRESS_KEY, AppConstants.UNKNOWN_IP_ADDRESS);

                    logger.debug().message(AppConstants.LOG_MSG_USER_CONTEXT_FROM_SPRING_SECURITY)
                            .details(Map.of(
                                    AppConstants.LOG_DETAIL_USER_ID, userId,
                                    AppConstants.LOG_DETAIL_USERNAME, username))
                            .log();
                    return metadata;
                }
            }
        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_MSG_EXTRACT_USER_CONTEXT_FAILED)
                    .detail(AppConstants.LOG_DETAIL_ERROR, e.getMessage()).log();
        }

        metadata.put(USER_ID_KEY, AppConstants.SYSTEM_USER_ID);
        metadata.put(USERNAME_KEY, AppConstants.SYSTEM_USERNAME);
        metadata.put(IP_ADDRESS_KEY, AppConstants.UNKNOWN_IP_ADDRESS);
        logger.debug().message(AppConstants.LOG_MSG_USING_SYSTEM_CONTEXT).log();
        return metadata;
    }

    /**
     * Gets the current HTTP servlet request.
     */
    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            logger.debug().message(AppConstants.LOG_MSG_CANNOT_GET_HTTP_REQUEST)
                    .detail(AppConstants.LOG_DETAIL_ERROR, e.getMessage()).log();
            return null;
        }
    }
}