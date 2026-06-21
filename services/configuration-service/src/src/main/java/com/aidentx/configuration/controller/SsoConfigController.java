package com.aidentx.configuration.controller;

import com.aidentx.configuration.config.AppConstants;
import com.aidentx.configuration.grpc.ConfigurationGrpcServiceGrpc;
import com.aidentx.configuration.grpc.DeleteConfigurationRequest;
import com.aidentx.configuration.grpc.DeleteConfigurationResponse;
import com.aidentx.configuration.grpc.GetConfigurationByTenantRequest;
import com.aidentx.configuration.grpc.GetConfigurationResponse;
import com.aidentx.configuration.grpc.PartialUpdateConfigurationRequest;
import com.aidentx.configuration.grpc.PartialUpdateConfigurationResponse;
import com.aidentx.configuration.grpc.SaveConfigurationRequest;
import com.aidentx.configuration.util.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing SSO (Single Sign-On) provider configurations
 * (LDAP, SAML, OAuth2).
 * <p>
 * This high-level controller provides a business-aware facade, primarily for
 * frontend interactions
 * that are routed through an API Gateway. It translates domain-specific REST
 * requests into
 * generic gRPC calls to this service's own internal gRPC endpoint. This
 * architectural pattern ensures
 * that all modifications trigger the appropriate, centralized business logic,
 * auditing, and
 * system-wide refresh events consistently and reliably.
 * </p>
 */
@RestController
@RequestMapping(AppConstants.API_V1_ADMIN_SSO_BASE_PATH)
public class SsoConfigController {

        private final IUnifiedLogger logger;
        private final ObjectMapper objectMapper;

        @GrpcClient("self") // Injects the gRPC client stub configured to call this service's own gRPC port
        private ConfigurationGrpcServiceGrpc.ConfigurationGrpcServiceBlockingStub grpcStub;

        /**
         * Constructs a new SsoConfigController.
         *
         * @param objectMapper         The Jackson ObjectMapper for JSON conversions.
         * @param unifiedLoggerFactory The factory for the unified logger.
         * @param jwtUtil              Utility for JWT operations.
         */
        public SsoConfigController(ObjectMapper objectMapper, UnifiedLoggerFactory unifiedLoggerFactory,
                        JwtUtil jwtUtil) {
                this.objectMapper = objectMapper;
                this.logger = unifiedLoggerFactory.getLogger(
                                AppConstants.LOGGER_NAME_SSO_CONFIG_CONTROLLER,
                                AppConstants.LOG_CATEGORY_SSO_CONFIG_CONTROLLER);
        }

        /**
         * Retrieves all SSO provider configurations by making individual gRPC calls for
         * each type.
         * This method aggregates the results into a single, convenient map for the
         * frontend. If a specific
         * configuration is not found, its entry will be omitted from the map.
         *
         * @return A {@link ResponseEntity} containing a map of all found SSO
         *         configurations.
         */
        @GetMapping(AppConstants.API_SSO_ALL_CONFIGS)
        public ResponseEntity<Map<String, Object>> getAllSsoConfigurations(
                        @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
                logger.trace().message(AppConstants.LOG_MSG_GET_ALL_SSO_CONFIGS).log();
                Map<String, Object> configs = new HashMap<>();
                try {

                        getProviderConfigData(AppConstants.CONFIG_KEY_LDAP, tenantId)
                                        .ifPresent(data -> configs.put(AppConstants.PROVIDER_LDAP, data));
                        getProviderConfigData(AppConstants.CONFIG_KEY_SAML, tenantId)
                                        .ifPresent(data -> configs.put(AppConstants.PROVIDER_SAML, data));
                        getProviderConfigData(AppConstants.CONFIG_KEY_OAUTH2, tenantId)
                                        .ifPresent(data -> configs.put(AppConstants.PROVIDER_OAUTH2, data));
                        logger.trace().message(AppConstants.LOG_MSG_GET_ALL_SSO_CONFIGS_SUCCESS).log();
                        return ResponseEntity.ok(configs);
                } catch (StatusRuntimeException e) {
                        throw e;
                } catch (Exception e) {
                        logger.error(e).message(AppConstants.LOG_MSG_GET_ALL_SSO_CONFIGS_ERROR).log();
                        throw new RuntimeException(e);
                }
        }

        /**
         * Retrieves the configuration for a specific SSO provider by delegating to an
         * internal gRPC call.
         *
         * @param provider The SSO provider identifier (ldap, saml, oauth2).
         * @return A {@link ResponseEntity} containing the configuration object as JSON,
         *         or 404 if not found.
         */
        @GetMapping(AppConstants.API_SSO_BY_PROVIDER)
        public ResponseEntity<Map<String, Object>> getSsoConfiguration(@PathVariable String provider,
                        @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
                logger.trace().message(AppConstants.LOG_MSG_GET_SSO_CONFIG)
                                .detail(AppConstants.JSON_KEY_PROVIDER, provider)
                                .log();
                String configKey = getConfigKeyForProvider(provider);
                if (configKey == null) {
                        logger.warn().message(AppConstants.LOG_MSG_GET_UNKNOWN_PROVIDER)
                                        .detail(AppConstants.JSON_KEY_PROVIDER, provider)
                                        .log();
                        return ResponseEntity.badRequest().build();
                }
                return getProviderConfigData(configKey, tenantId)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        /**
         * Saves or updates the configuration for a specific SSO provider by calling the
         * gRPC endpoint.
         * The underlying gRPC endpoint is responsible for all business logic, including
         * triggering the context refresh event.
         *
         * @param provider    The SSO provider identifier (ldap, saml, oauth2).
         * @param requestBody The configuration data as a JSON object.
         * @return A {@link ResponseEntity} with a status message indicating success or
         *         failure.
         */
        @PutMapping(AppConstants.API_SSO_BY_PROVIDER)
        public ResponseEntity<Map<String, String>> saveSsoConfiguration(
                        @PathVariable String provider,
                        @RequestBody(required = false) JsonNode requestBody,
                        HttpServletRequest request,
                        @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
                logger.trace().message(AppConstants.LOG_MSG_SAVE_SSO_CONFIG)
                                .detail(AppConstants.JSON_KEY_PROVIDER, provider)
                                .log();
                if (requestBody == null || !requestBody.isObject() || requestBody.isEmpty()) {
                        logger.warn().message(AppConstants.LOG_MSG_SAVE_SSO_INVALID_BODY)
                                        .detail(AppConstants.JSON_KEY_PROVIDER, provider).log();
                        return ResponseEntity.badRequest()
                                        .body(Map.of(AppConstants.JSON_KEY_STATUS, AppConstants.STATUS_ERROR,
                                                        AppConstants.JSON_KEY_MESSAGE,
                                                        AppConstants.ERROR_MSG_CONFIG_DATA_REQUIRED));
                }
                String configKey = getConfigKeyForProvider(provider);
                if (configKey == null) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of(AppConstants.JSON_KEY_STATUS, AppConstants.STATUS_ERROR,
                                                        AppConstants.JSON_KEY_MESSAGE,
                                                        AppConstants.ERROR_MSG_UNKNOWN_PROVIDER));
                }

                try {

                        boolean encrypt = requestBody.path(AppConstants.JSON_KEY_ENCRYPT_ENTIRE_CONFIG)
                                        .asBoolean(false);
                        Struct.Builder structBuilder = Struct.newBuilder();
                        JsonFormat.parser().merge(objectMapper.writeValueAsString(requestBody), structBuilder);

                        SaveConfigurationRequest grpcRequest = SaveConfigurationRequest.newBuilder()
                                        .setConfigType(AppConstants.CONFIG_TYPE_SSO)
                                        .setConfigKey(configKey)
                                        .setTenantId(tenantId)
                                        .setJsonData(structBuilder.build())
                                        .setEncryptEntireConfig(encrypt)
                                        .build();

                        grpcStub.saveConfiguration(grpcRequest);
                        logger.info().message(AppConstants.LOG_MSG_SAVE_SSO_SUCCESS)
                                        .detail(AppConstants.JSON_KEY_PROVIDER, provider).log();
                        String successMessage = String.format(AppConstants.MSG_FORMAT_CONFIG_UPDATED, provider);
                        return ResponseEntity.ok(Map.of(AppConstants.JSON_KEY_STATUS, AppConstants.STATUS_SUCCESS,
                                        AppConstants.JSON_KEY_MESSAGE, successMessage));
                } catch (IllegalStateException e) {
                        // Specific handling for authentication context issues
                        logger.error(e).message("Failed to set user context for SSO configuration save")
                                        .detail(AppConstants.JSON_KEY_PROVIDER, provider)
                                        .detail("error", e.getMessage())
                                        .log();
                        throw new RuntimeException("Authentication context could not be established: "
                                        + e.getMessage());
                } catch (StatusRuntimeException e) {
                        throw e;
                } catch (Exception e) {
                        logger.error(e).message(AppConstants.LOG_MSG_SAVE_SSO_FAILED)
                                        .detail(AppConstants.JSON_KEY_PROVIDER, provider).log();
                        throw new RuntimeException("Failed to save SSO configuration: " + e.getMessage());
                }
        }

        /**
         * A helper method to make a gRPC call to get configuration data and convert the
         * response to a Map.
         * This encapsulates the gRPC client logic and error handling.
         *
         * @param configKey The specific configuration key to fetch (e.g.,
         *                  "ldap_config").
         * @return An {@link Optional} containing the configuration data as a Map, or
         *         empty if not found or an error occurs.
         */
        private Optional<Map<String, Object>> getProviderConfigData(String configKey, String tenantId) {
                try {
                        GetConfigurationByTenantRequest grpcRequest = GetConfigurationByTenantRequest.newBuilder()
                                        .setConfigType(AppConstants.CONFIG_TYPE_SSO)
                                        .setTenantId(tenantId)
                                        .setConfigKey(configKey).build();
                        GetConfigurationResponse grpcResponse = grpcStub.getConfigurationByTenant(grpcRequest);
                        if (!grpcResponse.getFound()) {
                                return Optional.empty();
                        }
                        String responseJson = JsonFormat.printer().print(grpcResponse.getJsonData());
                        return Optional.of(objectMapper.readValue(responseJson, new TypeReference<>() {
                        }));
                } catch (StatusRuntimeException e) {
                        logger.warn().message(AppConstants.LOG_MSG_GET_PROVIDER_DATA_GRPC_FAILED)
                                        .detail(AppConstants.LOG_DETAIL_CONFIG_KEY, configKey)
                                        .detail(AppConstants.LOG_DETAIL_GRPC_STATUS, e.getStatus()).log();
                        return Optional.empty();
                } catch (Exception e) {
                        logger.error(e).message(AppConstants.LOG_MSG_GET_PROVIDER_DATA_PARSE_FAILED)
                                        .detail(AppConstants.LOG_DETAIL_CONFIG_KEY, configKey)
                                        .log();
                        return Optional.empty();
                }
        }

        /**
         * A helper method to resolve a provider name (e.g., "ldap") to its standardized
         * configuration key.
         * This mapping is internal to the business logic of this facade.
         *
         * @param provider The provider name from the REST API path.
         * @return The corresponding configuration key, or null if the provider is not
         *         recognized.
         */
        private String getConfigKeyForProvider(String provider) {
                return switch (provider.toLowerCase()) {
                        case AppConstants.PROVIDER_LDAP -> AppConstants.CONFIG_KEY_LDAP;
                        case AppConstants.PROVIDER_SAML -> AppConstants.CONFIG_KEY_SAML;
                        case AppConstants.PROVIDER_OAUTH2 -> AppConstants.CONFIG_KEY_OAUTH2;
                        default -> null;
                };
        }

        /**
         * Partially updates the configuration for a specific SSO provider.
         * This is ideal for toggling flags (like 'enabled') or updating single fields
         * without sending the entire configuration object.
         *
         * @param provider    The SSO provider identifier (ldap, saml, oauth2).
         * @param requestBody A JSON object containing only the fields to be changed.
         * @return A {@link ResponseEntity} containing the fully updated configuration
         *         object.
         */
        @PatchMapping(AppConstants.API_SSO_BY_PROVIDER)
        public ResponseEntity<Object> patchSsoConfiguration(
                        @PathVariable String provider, @RequestBody JsonNode requestBody,
                        HttpServletRequest request,
                        @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
                logger.trace().message(AppConstants.LOG_MSG_PATCH_SSO_CONFIG)
                                .detail(AppConstants.JSON_KEY_PROVIDER, provider)
                                .log();
                String configKey = getConfigKeyForProvider(provider);
                if (configKey == null) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of(AppConstants.JSON_KEY_STATUS, AppConstants.STATUS_ERROR,
                                                        AppConstants.JSON_KEY_MESSAGE,
                                                        AppConstants.ERROR_MSG_UNKNOWN_PROVIDER));
                }
                try {

                        Struct.Builder structBuilder = Struct.newBuilder();
                        JsonFormat.parser().merge(objectMapper.writeValueAsString(requestBody), structBuilder);

                        PartialUpdateConfigurationRequest grpcRequest = PartialUpdateConfigurationRequest.newBuilder()
                                        .setConfigType(AppConstants.CONFIG_TYPE_SSO)
                                        .setTenantId(tenantId)
                                        .setConfigKey(configKey)
                                        .setPatchData(structBuilder.build())
                                        .build();

                        PartialUpdateConfigurationResponse grpcResponse = grpcStub
                                        .partialUpdateConfiguration(grpcRequest);

                        String responseJson = JsonFormat.printer().print(grpcResponse.getUpdatedConfiguration());
                        Object responseObject = objectMapper.readValue(responseJson, Object.class);
                        return ResponseEntity.ok(responseObject);
                } catch (StatusRuntimeException e) {
                        logger.error(e).message(AppConstants.LOG_MSG_PATCH_SSO_CONFIG_FAILED)
                                        .detail(AppConstants.JSON_KEY_PROVIDER, provider)
                                        .log();
                        throw e;
                } catch (Exception e) {
                        logger.error(e).message(AppConstants.LOG_MSG_PATCH_SSO_CONFIG_FAILED)
                                        .detail(AppConstants.JSON_KEY_PROVIDER, provider)
                                        .log();
                        throw new RuntimeException(e);
                }
        }

        /**
         * Deletes the configuration for a specific SSO provider.
         * The underlying gRPC endpoint triggers the context refresh event.
         *
         * @param provider The SSO provider identifier (ldap, saml, oauth2).
         * @return A {@link ResponseEntity} with a status message, or 404 if not found.
         */
        @DeleteMapping(AppConstants.API_SSO_BY_PROVIDER)
        public ResponseEntity<Map<String, String>> deleteProviderConfig(@PathVariable String provider,
                        HttpServletRequest request,
                        @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
                logger.trace().message(AppConstants.LOG_MSG_DELETE_SSO_CONFIG)
                                .detail(AppConstants.JSON_KEY_PROVIDER, provider)
                                .log();

                String configKey = getConfigKeyForProvider(provider);
                if (configKey == null) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of(AppConstants.JSON_KEY_STATUS, AppConstants.STATUS_ERROR,
                                                        AppConstants.JSON_KEY_MESSAGE,
                                                        AppConstants.ERROR_MSG_UNKNOWN_PROVIDER));
                }
                DeleteConfigurationRequest grpcRequest = DeleteConfigurationRequest.newBuilder()
                                .setConfigType(AppConstants.CONFIG_TYPE_SSO)
                                .setTenantId(tenantId)
                                .setConfigKey(configKey).build();
                DeleteConfigurationResponse grpcResponse = grpcStub.deleteConfiguration(grpcRequest);
                if (grpcResponse.getDeleted()) {
                        logger.info().message(AppConstants.LOG_MSG_DELETE_SSO_SUCCESS)
                                        .detail(AppConstants.JSON_KEY_PROVIDER, provider).log();
                        String successMessage = String.format(AppConstants.MSG_FORMAT_CONFIG_DELETED, provider);
                        return ResponseEntity.ok(Map.of(AppConstants.JSON_KEY_STATUS, AppConstants.STATUS_SUCCESS,
                                        AppConstants.JSON_KEY_MESSAGE, successMessage));
                } else {
                        logger.warn().message(AppConstants.LOG_MSG_DELETE_SSO_NOT_FOUND)
                                        .detail(AppConstants.JSON_KEY_PROVIDER, provider).log();
                        return ResponseEntity.notFound().build();
                }
        }
}