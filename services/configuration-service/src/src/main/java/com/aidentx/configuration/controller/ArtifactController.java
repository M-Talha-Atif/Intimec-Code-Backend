package com.aidentx.configuration.controller;

import com.aidentx.configuration.grpc.ConfigurationGrpcServiceGrpc;
import com.aidentx.configuration.grpc.DeleteArtifactRequest;
import com.aidentx.configuration.grpc.DeleteArtifactResponse;
import com.aidentx.configuration.grpc.GetArtifactRequest;
import com.aidentx.configuration.grpc.GetArtifactResponse;
import com.aidentx.configuration.grpc.GetRelatedArtifactsRequest;
import com.aidentx.configuration.grpc.GetRelatedArtifactsResponse;
import com.aidentx.configuration.grpc.SaveArtifactRequest;
import com.aidentx.configuration.grpc.SaveArtifactResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing configuration artifacts (such as certificates,
 * metadata, etc.).
 * <p>
 * This controller provides a public REST API for frontend interactions related
 * to file-based artifacts.
 * It acts as a facade, translating all HTTP requests into internal gRPC calls.
 * This ensures that
 * all modifications, including artifact updates, consistently trigger
 * system-wide refresh events
 * and follow the centralized business logic defined in the gRPC endpoint.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/artifacts")
public class ArtifactController {

        private final IUnifiedLogger logger;
        private final ObjectMapper objectMapper;
        @GrpcClient("self")
        private ConfigurationGrpcServiceGrpc.ConfigurationGrpcServiceBlockingStub grpcStub;

        public ArtifactController(ObjectMapper objectMapper, UnifiedLoggerFactory unifiedLoggerFactory) {
                this.objectMapper = objectMapper;
                this.logger = unifiedLoggerFactory.getLogger("ArtifactController", "config-artifact");
        }

        /**
         * Retrieves all related artifacts for a given configuration via a gRPC call.
         * <p>
         * If no artifacts are found for the given configuration, this endpoint
         * correctly returns a
         * 200 OK response with an empty JSON object {@code {}}.
         * </p>
         *
         * @param configType The type/category of the configuration (e.g., SSO).
         * @param configKey  The unique key identifying the configuration.
         * @return A {@link ResponseEntity} containing a map of artifacts.
         */
        @GetMapping("/{configType}/{configKey}/artifacts")
        public ResponseEntity<Map<String, Object>> getRelatedArtifacts(
                        @PathVariable String configType, @PathVariable String configKey,
                        @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
                logger.trace().message("Entering REST GET .../artifacts").detail("configType", configType)
                                .detail("configKey", configKey).log();
                try {
                        GetRelatedArtifactsRequest grpcRequest = GetRelatedArtifactsRequest.newBuilder()
                                        .setConfigType(configType)
                                        .setTenantId(tenantId)
                                        .setConfigKey(configKey)
                                        .build();
                        GetRelatedArtifactsResponse grpcResponse = grpcStub.getRelatedArtifacts(grpcRequest);
                        String responseJson = JsonFormat.printer().print(grpcResponse.getArtifactsMap());
                        Map<String, Object> artifactsMap = objectMapper.readValue(responseJson, new TypeReference<>() {
                        });

                        // Correctly return 200 OK with the (potentially empty) map.
                        return ResponseEntity.ok(artifactsMap);
                } catch (StatusRuntimeException e) {
                        throw e;
                } catch (Exception e) {
                        logger.error(e).message("Failed to get related artifacts via gRPC.").log();
                        throw new RuntimeException(e);
                }
        }

        /**
         * Retrieves the decrypted content of a specific artifact for a given
         * configuration via a gRPC call.
         * <p>
         * As this endpoint targets a single, specific resource, it correctly returns a
         * 404 Not Found
         * if the requested artifact does not exist.
         * </p>
         *
         * @param configType   The type/category of the configuration.
         * @param configKey    The unique key identifying the configuration.
         * @param artifactType The type of artifact to retrieve (e.g., "certificate").
         * @return A {@link ResponseEntity} containing the artifact content as a string,
         *         or 404 if not found.
         */
        @GetMapping("/{configType}/{configKey}/artifacts/{artifactType}")
        public ResponseEntity<String> getConfigurationArtifact(
                        @PathVariable String configType, @PathVariable String configKey,
                        @PathVariable String artifactType,
                        @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
                logger.trace().message("Entering REST GET .../artifacts/{artifactType}")
                                .detail("configType", configType)
                                .detail("configKey", configKey).detail("artifactType", artifactType).log();

                GetArtifactRequest grpcRequest = GetArtifactRequest.newBuilder()
                                .setConfigType(configType)
                                .setTenantId(tenantId)
                                .setConfigKey(configKey)
                                .setArtifactType(artifactType)
                                .build();
                GetArtifactResponse grpcResponse = grpcStub.getArtifact(grpcRequest);

                return grpcResponse.getFound()
                                ? ResponseEntity.ok(grpcResponse.getContent())
                                : ResponseEntity.notFound().build();
        }

        /**
         * Retrieves the raw binary content of a specific artifact.
         * Returns 404 if not found or not a file artifact.
         */
        @GetMapping(value = "/{configType}/{configKey}/artifacts/{artifactType}/file")
        public ResponseEntity<byte[]> getBinaryConfigurationArtifact(
                        @PathVariable String configType, @PathVariable String configKey,
                        @PathVariable String artifactType,
                        @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
                logger.trace().message("Entering REST GET binary .../artifacts/{artifactType}/file")
                                .detail("configType", configType)
                                .detail("configKey", configKey).detail("artifactType", artifactType).log();
                try {
                        // Reuse existing gRPC getArtifact to fetch content as string; if encrypted binary it will be base64
                        GetArtifactRequest grpcRequest = GetArtifactRequest.newBuilder()
                                        .setConfigType(configType)
                                        .setTenantId(tenantId)
                                        .setConfigKey(configKey)
                                        .setArtifactType(artifactType)
                                        .build();
                        GetArtifactResponse grpcResponse = grpcStub.getArtifact(grpcRequest);
                        if (!grpcResponse.getFound()) {
                                return ResponseEntity.notFound().build();
                        }
                        String content = grpcResponse.getContent();
                        // Try Base64 decode; if fails, treat as UTF-8 bytes
                        byte[] bytes;
                        try {
                                bytes = java.util.Base64.getDecoder().decode(content);
                                // Heuristic: if decoding yields very small or similar length, fallback
                                if (bytes.length < 4 && content.length() > 0) {
                                        bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                                }
                        } catch (IllegalArgumentException ex) {
                                bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        }
                        return ResponseEntity.ok()
                                        .header("Content-Disposition", "attachment; filename=" + artifactType)
                                        .body(bytes);
                } catch (StatusRuntimeException e) {
                        throw e;
                } catch (Exception e) {
                        logger.error(e).message("Failed to get binary artifact via gRPC.").log();
                        throw new RuntimeException(e);
                }
        }

        /**
         * Adds or updates a specific artifact for a configuration using a JSON payload.
         * <p>
         * This method expects a JSON object containing the raw artifact content. It
         * extracts this content
         * and forwards it to the gRPC endpoint, which handles the file-saving logic and
         * triggers a
         * context refresh.
         * </p>
         *
         * @param configType   The type/category of the configuration.
         * @param configKey    The unique key identifying the configuration.
         * @param artifactType The type of artifact to update (e.g., "certificate").
         * @param requestBody  A {@link JsonNode} containing the artifact content.
         * @return A {@link ResponseEntity} containing the full, updated configuration
         *         entity as JSON.
         */
        @PutMapping(value = "/{configType}/{configKey}/artifacts/{artifactType}", consumes = "application/json")
        public ResponseEntity<Object> updateInlineConfigurationArtifact(
                        @PathVariable String configType, @PathVariable String configKey,
                        @PathVariable String artifactType,
                        @RequestBody JsonNode requestBody,
                        @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
                logger.trace().message("Entering REST PUT (inline JSON) .../artifacts/{artifactType}")
                                .detail("configType", configType)
                                .detail("configKey", configKey).detail("artifactType", artifactType).log();
                try {
                        if (requestBody == null || !requestBody.isObject()) {
                                return ResponseEntity.badRequest().body(Map.of("error", "Body must be a JSON object"));
                        }
                        // Extract and strip encryption flag if present (supports two possible keys)
                        boolean encryptEntirely = false;
                        if (requestBody.has("encryptEntirely")) {
                                encryptEntirely = requestBody.get("encryptEntirely").asBoolean();
                                ((com.fasterxml.jackson.databind.node.ObjectNode) requestBody)
                                                .remove("encryptEntirely");
                        } else if (requestBody.has("encryptEntireConfig")) {
                                encryptEntirely = requestBody.get("encryptEntireConfig").asBoolean();
                                ((com.fasterxml.jackson.databind.node.ObjectNode) requestBody)
                                                .remove("encryptEntireConfig");
                        }
                        com.google.protobuf.Struct.Builder structBuilder = com.google.protobuf.Struct.newBuilder();
                        JsonFormat.parser().merge(objectMapper.writeValueAsString(requestBody), structBuilder);
                        SaveArtifactRequest grpcRequest = SaveArtifactRequest.newBuilder()
                                        .setConfigType(configType)
                                        .setTenantId(tenantId)
                                        .setConfigKey(configKey)
                                        .setArtifactType(artifactType)
                                        .setJsonData(structBuilder.build())
                                        .setEncryptEntirely(encryptEntirely)
                                        .build();
                        SaveArtifactResponse grpcResponse = grpcStub.saveArtifact(grpcRequest);
                        String responseJson = JsonFormat.printer().print(grpcResponse.getUpdatedConfiguration());
                        Object responseObject = objectMapper.readValue(responseJson, Object.class);
                        return ResponseEntity.ok(responseObject);
                } catch (StatusRuntimeException e) {
                        throw e;
                } catch (Exception e) {
                        logger.error(e).message("Failed to update inline artifact via gRPC.").log();
                        throw new RuntimeException(e);
                }
        }

        /**
         * Adds or updates a binary (raw file) artifact. Body is expected to be raw
         * bytes
         * with Content-Type: application/octet-stream
         */
        @PutMapping(value = "/{configType}/{configKey}/artifacts/{artifactType}/file", consumes = "application/octet-stream")
        public ResponseEntity<Object> updateBinaryConfigurationArtifact(
                        @PathVariable String configType, @PathVariable String configKey,
                        @PathVariable String artifactType,
                        @RequestBody byte[] body,
                        @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
                logger.trace().message("Entering REST PUT (binary file) .../artifacts/{artifactType}/file")
                                .detail("configType", configType)
                                .detail("configKey", configKey).detail("artifactType", artifactType)
                                .detail("size", body == null ? 0 : body.length).log();
                try {
                        if (body == null || body.length == 0) {
                                return ResponseEntity.badRequest().body(Map.of("error", "Empty body"));
                        }
                        SaveArtifactRequest grpcRequest = SaveArtifactRequest.newBuilder()
                                        .setConfigType(configType)
                                        .setTenantId(tenantId)
                                        .setConfigKey(configKey)
                                        .setArtifactType(artifactType)
                                        .setFileBytes(com.google.protobuf.ByteString.copyFrom(body))
                                        .build();
                        SaveArtifactResponse grpcResponse = grpcStub.saveArtifact(grpcRequest);
                        String responseJson = JsonFormat.printer().print(grpcResponse.getUpdatedConfiguration());
                        Object responseObject = objectMapper.readValue(responseJson, Object.class);
                        return ResponseEntity.ok(responseObject);
                } catch (StatusRuntimeException e) {
                        throw e;
                } catch (Exception e) {
                        logger.error(e).message("Failed to update binary artifact via gRPC.").log();
                        throw new RuntimeException(e);
                }
        }

        /**
         * Deletes a specific artifact for a configuration via a gRPC call.
         *
         * @param configType   The type/category of the configuration.
         * @param configKey    The unique key identifying the configuration.
         * @param artifactType The type of artifact to delete.
         * @return A {@link ResponseEntity} with HTTP 204 (No Content) if deleted, or
         *         404 if not found.
         */
        @DeleteMapping("/{configType}/{configKey}/artifacts/{artifactType}")
        public ResponseEntity<Void> deleteConfigurationArtifact(
                        @PathVariable String configType, @PathVariable String configKey,
                        @PathVariable String artifactType,
                        HttpServletRequest request,
                        @RequestHeader(name = "X-Tenant-Id", required = false, defaultValue = "tenant_001") String tenantId) {
                logger.trace().message("Entering REST DELETE .../artifacts/{artifactType}")
                                .detail("configType", configType)
                                .detail("configKey", configKey).detail("artifactType", artifactType).log();

                DeleteArtifactRequest grpcRequest = DeleteArtifactRequest.newBuilder()
                                .setConfigType(configType)
                                .setTenantId(tenantId)
                                .setConfigKey(configKey)
                                .setArtifactType(artifactType)
                                .build();
                DeleteArtifactResponse grpcResponse = grpcStub.deleteArtifact(grpcRequest);
                return grpcResponse.getDeleted()
                                ? ResponseEntity.noContent().build()
                                : ResponseEntity.notFound().build();
        }

}