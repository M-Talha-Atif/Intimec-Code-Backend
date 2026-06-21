package com.aidentx.configuration.controller;

import com.aidentx.configuration.config.AppConstants;
import com.aidentx.configuration.dto.ConfigurationRequest;
import com.aidentx.configuration.grpc.ConfigurationGrpcServiceGrpc;
import com.aidentx.configuration.grpc.DeleteConfigurationRequest;
import com.aidentx.configuration.grpc.GetAllConfigurationsResponse;
import com.aidentx.configuration.grpc.GetConfigurationByTenantRequest;
import com.aidentx.configuration.grpc.GetConfigurationsByTypeRequest;
import com.aidentx.configuration.grpc.GetConfigurationsByTypeResponse;
import com.aidentx.configuration.grpc.PartialUpdateConfigurationRequest;
import com.aidentx.configuration.grpc.PartialUpdateConfigurationResponse;
import com.aidentx.configuration.grpc.SaveConfigurationRequest;
import com.aidentx.configuration.util.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for low-level, generic management of application
 * configurations.
 * <p>
 * This API is typically used for administrative purposes or diagnostics. It
 * acts as a
 * REST facade over the primary gRPC endpoint, ensuring that all modifications
 * trigger
 * the appropriate centralized business logic, auditing, and refresh events.
 * </p>
 */
@RestController
@RequestMapping(AppConstants.API_V1_CONFIGURATIONS_BASE_PATH)
public class ConfigurationController {

    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    @GrpcClient("self")
    private ConfigurationGrpcServiceGrpc.ConfigurationGrpcServiceBlockingStub grpcStub;

    public ConfigurationController(ObjectMapper objectMapper, UnifiedLoggerFactory unifiedLoggerFactory,
            JwtUtil jwtUtil) {
        this.objectMapper = objectMapper;
        this.logger = unifiedLoggerFactory.getLogger(AppConstants.LOGGER_NAME_CONFIGURATION_CONTROLLER,
                AppConstants.LOG_CATEGORY);
    }

    /**
     * Retrieves all configurations in the system.
     * <p>
     * This endpoint fetches a complete list of all stored configuration entities by
     * calling the
     * corresponding gRPC method. If no configurations exist, it correctly returns a
     * 200 OK with an empty list.
     * The response contains the raw configuration entities, including potentially
     * sensitive or encrypted data.
     * </p>
     *
     * @return A {@link ResponseEntity} containing a list of all configuration
     *         entities as JSON objects.
     */
    @GetMapping
    public ResponseEntity<List<JsonNode>> getAllConfigurations(
            @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
        logger.trace().message(AppConstants.LOG_MSG_ENTERING_GET_ALL_CONFIGS).log();
        try {
            GetAllConfigurationsResponse grpcResponse = grpcStub.getAllConfigurations(Empty.getDefaultInstance());
            List<JsonNode> resultList = new ArrayList<>();
            for (Struct configStruct : grpcResponse.getConfigurationsList()) {
                String configJson = JsonFormat.printer().print(configStruct);
                JsonNode node = objectMapper.readTree(configJson);
                if (node.has("tenantId") && tenantId.equals(node.path("tenantId").asText())) {
                    resultList.add(node);
                }
            }
            return ResponseEntity.ok(resultList);
        } catch (StatusRuntimeException e) {
            logger.error(e).message(AppConstants.LOG_MSG_GET_ALL_CONFIGS_FAILED).log();
            throw e;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_MSG_GET_ALL_CONFIGS_FAILED).log();
            throw new RuntimeException(e);
        }
    }

    /**
     * Lists all configuration entities of a given type.
     * <p>
     * This endpoint fetches configurations filtered by their type via a gRPC call.
     * If no configurations
     * of the specified type are found, it correctly returns a 200 OK response with
     * an empty JSON array {@code []}.
     * </p>
     *
     * @param configType The type of configuration to filter by (e.g., "sso",
     *                   "security").
     * @return A {@link ResponseEntity} containing a list of matching configuration
     *         entities as JSON objects.
     */
    @GetMapping(AppConstants.API_CONFIG_BY_TYPE)
    public ResponseEntity<List<JsonNode>> getConfigurationsByType(@PathVariable String configType,
            @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
        logger.trace().message(AppConstants.LOG_MSG_ENTERING_GET_CONFIGS_BY_TYPE)
                .detail(AppConstants.DETAIL_CONFIG_TYPE, configType).log();
        try {
            GetConfigurationsByTypeRequest grpcRequest = GetConfigurationsByTypeRequest.newBuilder()
                    .setConfigType(configType).build();
            GetConfigurationsByTypeResponse grpcResponse = grpcStub.getConfigurationsByType(grpcRequest);
            List<JsonNode> resultList = new ArrayList<>();
            for (Struct configStruct : grpcResponse.getConfigurationsList()) {
                String configJson = JsonFormat.printer().print(configStruct);
                JsonNode node = objectMapper.readTree(configJson);
                if (node.has("tenantId") && tenantId.equals(node.path("tenantId").asText())) {
                    resultList.add(node);
                }
            }
            return ResponseEntity.ok(resultList);
        } catch (StatusRuntimeException e) {
            logger.error(e).message(AppConstants.LOG_MSG_GET_CONFIGS_BY_TYPE_FAILED)
                    .detail(AppConstants.DETAIL_CONFIG_TYPE, configType).log();
            throw e;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_MSG_GET_CONFIGS_BY_TYPE_FAILED)
                    .detail(AppConstants.DETAIL_CONFIG_TYPE, configType).log();
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves a specific configuration's decrypted data based on type and key via
     * a gRPC call.
     *
     * @param configType The configuration type.
     * @param configKey  The configuration key.
     * @return The configuration data as a {@link JsonNode}, or 404 if not found.
     */
    @GetMapping(AppConstants.API_CONFIG_BY_TYPE_AND_KEY)
    public ResponseEntity<JsonNode> getConfiguration(
            @PathVariable String configType, @PathVariable String configKey,
            @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
        logger.trace().message(AppConstants.LOG_MSG_ENTERING_GET_CONFIG)
                .detail(AppConstants.DETAIL_CONFIG_TYPE, configType)
                .detail(AppConstants.DETAIL_CONFIG_KEY, configKey).log();
        try {
        var grpcRequest = GetConfigurationByTenantRequest.newBuilder()
            .setConfigType(configType)
            .setTenantId(tenantId)
            .setConfigKey(configKey)
            .build();
        var grpcResponse = grpcStub.getConfigurationByTenant(grpcRequest);
            if (!grpcResponse.getFound()) {
                return ResponseEntity.notFound().build();
            }
            String responseJson = JsonFormat.printer().print(grpcResponse.getJsonData());
            return ResponseEntity.ok(objectMapper.readTree(responseJson));
        } catch (StatusRuntimeException e) {
            logger.error(e).message(AppConstants.LOG_MSG_GET_CONFIG_FAILED).log();
            throw e;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_MSG_GET_CONFIG_FAILED).log();
            throw new RuntimeException(e);
        }
    }

    /**
     * Partially updates an existing configuration by merging the provided JSON
     * data.
     * This method is ideal for updating specific fields without replacing the
     * entire configuration.
     *
     * @param configType  The type of the configuration (e.g., "sso").
     * @param configKey   The key of the configuration (e.g., "saml_config").
     * @param requestBody A JSON object containing the fields to be updated.
     * @return A {@link ResponseEntity} containing the full, updated configuration
     *         object.
     */
    @PatchMapping(AppConstants.API_CONFIG_BY_TYPE_AND_KEY)
    public ResponseEntity<JsonNode> partialUpdateConfiguration(
            @PathVariable String configType, @PathVariable String configKey,
            @RequestBody JsonNode requestBody,
            @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId,
            HttpServletRequest httpRequest) {
        logger.trace().message(AppConstants.LOG_MSG_ENTERING_PATCH_CONFIG)
                .detail(AppConstants.DETAIL_CONFIG_TYPE, configType)
                .detail(AppConstants.DETAIL_CONFIG_KEY, configKey).log();

        Struct.Builder structBuilder = Struct.newBuilder();
        try {
            JsonFormat.parser().merge(objectMapper.writeValueAsString(requestBody), structBuilder);

        PartialUpdateConfigurationRequest grpcRequest = PartialUpdateConfigurationRequest.newBuilder()
            .setConfigType(configType)
            .setTenantId(tenantId)
            .setConfigKey(configKey)
            .setPatchData(structBuilder.build())
            .build();

            PartialUpdateConfigurationResponse grpcResponse = grpcStub.partialUpdateConfiguration(grpcRequest);

            String responseJson = JsonFormat.printer().print(grpcResponse.getUpdatedConfiguration());
            return ResponseEntity.ok(objectMapper.readTree(responseJson));
        } catch (StatusRuntimeException e) {
            logger.error(e).message(AppConstants.LOG_MSG_PATCH_CONFIG_FAILED).log();
            // Re-throw StatusRuntimeException directly so GlobalExceptionHandler can handle it properly
            throw e;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_MSG_PATCH_CONFIG_FAILED).log();
            // For non-StatusRuntimeException, wrap it
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates or updates a configuration entry via a gRPC call.
     *
     * @param configType The type of the configuration.
     * @param configKey  The key of the configuration.
     * @param request    The configuration data payload.
     * @return The saved configuration data as a JSON object.
     */
    @PutMapping(AppConstants.API_CONFIG_BY_TYPE_AND_KEY)
    public ResponseEntity<JsonNode> saveConfiguration(
            @PathVariable String configType, @PathVariable String configKey,
            @RequestBody ConfigurationRequest request,
            @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId,
            HttpServletRequest httpRequest) {
        logger.trace().message(AppConstants.LOG_MSG_ENTERING_PUT_CONFIG)
                .detail(AppConstants.DETAIL_CONFIG_TYPE, configType)
                .detail(AppConstants.DETAIL_CONFIG_KEY, configKey).log();

        try {
            Struct.Builder structBuilder = Struct.newBuilder();
            JsonFormat.parser().merge(objectMapper.writeValueAsString(request.getData()), structBuilder);

        var grpcRequest = SaveConfigurationRequest.newBuilder()
            .setConfigType(configType)
            .setConfigKey(configKey)
            .setTenantId(tenantId)
            .setJsonData(structBuilder.build())
            .setEncryptEntireConfig(request.isEncryptEntireConfig())
            .build();

            var grpcResponse = grpcStub.saveConfiguration(grpcRequest);
            String responseJson = JsonFormat.printer().print(grpcResponse.getSavedConfigData());
            return ResponseEntity.ok(objectMapper.readTree(responseJson));
        } catch (StatusRuntimeException e) {
            logger.error(e).message(AppConstants.LOG_MSG_SAVE_CONFIG_FAILED).log();
            throw e;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_MSG_SAVE_CONFIG_FAILED).log();
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a configuration via a gRPC call.
     *
     * @param configType The type of the configuration.
     * @param configKey  The key of the configuration.
     * @return HTTP 204 (No Content) if successful, or 404 if not found.
     */
    @DeleteMapping(AppConstants.API_CONFIG_BY_TYPE_AND_KEY)
    public ResponseEntity<Void> deleteConfiguration(
            @PathVariable String configType, @PathVariable String configKey,
            @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId,
            HttpServletRequest httpRequest) {
        logger.trace().message(AppConstants.LOG_MSG_ENTERING_DELETE_CONFIG)
                .details(Map.of(AppConstants.DETAIL_CONFIG_TYPE, configType, AppConstants.DETAIL_CONFIG_KEY, configKey))
                .log();
    var grpcRequest = DeleteConfigurationRequest.newBuilder()
        .setConfigType(configType)
        .setTenantId(tenantId)
        .setConfigKey(configKey)
        .build();
        var grpcResponse = grpcStub.deleteConfiguration(grpcRequest);
        return grpcResponse.getDeleted()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}