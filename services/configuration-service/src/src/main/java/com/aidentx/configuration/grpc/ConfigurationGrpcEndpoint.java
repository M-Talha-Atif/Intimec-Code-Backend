package com.aidentx.configuration.grpc;

import com.aidentx.configuration.config.ConfigurationType;
import com.aidentx.configuration.dto.ConfigurationDataWithMetadata;
import com.aidentx.configuration.dto.ConfigurationPatchRequest;
import com.aidentx.configuration.dto.ConfigurationSummary;
import com.aidentx.configuration.exception.artifact.ArtifactNotFoundException;
import com.aidentx.configuration.exception.artifact.ArtifactValidationException;
import com.aidentx.configuration.exception.configuration.ConfigurationAlreadyExistsException;
import com.aidentx.configuration.exception.configuration.ConfigurationNotFoundException;
import com.aidentx.configuration.exception.configuration.ConfigurationPatchException;
import com.aidentx.configuration.exception.configuration.ConfigurationValidationException;
import com.aidentx.configuration.grpc.SaveArtifactRequest.DataPayloadCase;
import com.aidentx.configuration.model.entity.Artifact;
import com.aidentx.configuration.model.entity.Configuration;
import com.aidentx.configuration.service.ArtifactService;
import com.aidentx.configuration.service.ConfigurationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@GrpcService
public class ConfigurationGrpcEndpoint extends ConfigurationGrpcServiceGrpc.ConfigurationGrpcServiceImplBase {

    private final ConfigurationService configurationService;
    private final ArtifactService artifactService;
    private final ObjectMapper objectMapper;
    private final ContextRefresher contextRefresher;

    public ConfigurationGrpcEndpoint(ConfigurationService configurationService,
            ArtifactService artifactService,
            ObjectMapper objectMapper,
            ContextRefresher contextRefresher) {
        this.configurationService = configurationService;
        this.artifactService = artifactService;
        this.objectMapper = objectMapper;
        this.contextRefresher = contextRefresher;
    }

    @Override
    public void saveConfiguration(SaveConfigurationRequest request,
            StreamObserver<SaveConfigurationResponse> responseObserver) {
        try {
            String dataAsJsonString = JsonFormat.printer().print(request.getJsonData());
            JsonNode dataToSave = objectMapper.readTree(dataAsJsonString);

            configurationService.saveConfiguration(
                    ConfigurationType.fromValue(request.getConfigType()),
                    request.getConfigKey(),
                    request.getTenantId(),
                    dataToSave,
                    request.getEncryptEntireConfig());

            contextRefresher.refresh();

            // SOLUTION: Return an empty struct instead of the full saved data.
            responseObserver.onNext(SaveConfigurationResponse.newBuilder()
                    .setSavedConfigData(Struct.newBuilder().build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e, "Failed to save configuration"));
        }
    }

    @Override
    public void partialUpdateConfiguration(PartialUpdateConfigurationRequest request,
            StreamObserver<PartialUpdateConfigurationResponse> responseObserver) {
        try {
            String patchJson = JsonFormat.printer().print(request.getPatchData());
            JsonNode patchNode = objectMapper.readTree(patchJson);

            ConfigurationPatchRequest patchRequestDto = objectMapper.treeToValue(patchNode,
                    ConfigurationPatchRequest.class);

            configurationService.partialUpdateConfiguration(
                    ConfigurationType.fromValue(request.getConfigType()),
                    request.getConfigKey(),
                    request.getTenantId(),
                    patchRequestDto);

            contextRefresher.refresh();

            // SOLUTION: Return an empty struct instead of the full updated object.
            PartialUpdateConfigurationResponse response = PartialUpdateConfigurationResponse.newBuilder()
                    .setUpdatedConfiguration(Struct.newBuilder().build())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e, "Failed to patch configuration"));
        }
    }

    @Override
    @Transactional
    public void saveArtifact(SaveArtifactRequest request, StreamObserver<SaveArtifactResponse> responseObserver) {
        try {
            Configuration config = configurationService.getConfiguration(
                    ConfigurationType.fromValue(request.getConfigType()), request.getConfigKey(), request.getTenantId())
                    .orElseThrow(() -> new IllegalStateException("Configuration must exist to add an artifact."));
            // Determine which payload was provided
            DataPayloadCase payloadCase = request.getDataPayloadCase();
            if (payloadCase == DataPayloadCase.JSON_DATA) {
                // Convert incoming Struct to JSON string then parse to JsonNode
                String jsonString = JsonFormat.printer().print(request.getJsonData());
                JsonNode jsonNode;
                try {
                    jsonNode = objectMapper.readTree(jsonString);
                } catch (com.fasterxml.jackson.core.JsonProcessingException parseEx) {
                    throw new ArtifactValidationException(
                            request.getConfigType(),
                            request.getConfigKey(),
                            request.getArtifactType(),
                            "Invalid inline JSON artifact payload: " + parseEx.getOriginalMessage(),
                            parseEx);
                } catch (Exception ex) {
                    throw new ArtifactValidationException(
                            request.getConfigType(),
                            request.getConfigKey(),
                            request.getArtifactType(),
                            "Invalid inline JSON artifact payload: " + ex.getMessage(),
                            ex);
                }
                // Persist (let domain/service exceptions spropagate for accurate status
                // mapping)
                artifactService.saveInlineArtifact(config, request.getArtifactType(), jsonNode,
                        request.getEncryptEntirely());
            } else if (payloadCase == DataPayloadCase.FILE_BYTES) {
                artifactService.saveBinaryFileArtifact(config, request.getArtifactType(),
                        request.getFileBytes().toByteArray());
            } else {
                throw new IllegalArgumentException("No artifact payload provided");
            }

            contextRefresher.refresh();

            // SOLUTION: Return an empty struct instead of the large updated configuration
            // object.
            SaveArtifactResponse response = SaveArtifactResponse.newBuilder()
                    .setUpdatedConfiguration(Struct.newBuilder().build())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e, "Failed to save artifact"));
        }
    }

    @Override
    public void deleteConfiguration(DeleteConfigurationRequest request,
            StreamObserver<DeleteConfigurationResponse> responseObserver) {
        boolean deleted = configurationService.deleteConfiguration(
                ConfigurationType.fromValue(request.getConfigType()),
                request.getConfigKey(),
                request.getTenantId());
        if (deleted) {
            contextRefresher.refresh();
        }
        responseObserver.onNext(DeleteConfigurationResponse.newBuilder().setDeleted(deleted).build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void deleteArtifact(DeleteArtifactRequest request, StreamObserver<DeleteArtifactResponse> responseObserver) {
        try {
            Configuration config = configurationService.getConfiguration(
                    ConfigurationType.fromValue(request.getConfigType()), request.getConfigKey(), request.getTenantId())
                    .orElse(null);

            if (config == null) {
                responseObserver.onNext(DeleteArtifactResponse.newBuilder().setDeleted(false).build());
                responseObserver.onCompleted();
                return;
            }

            boolean deleted = artifactService.deleteArtifact(config, request.getArtifactType());
            if (deleted) {
                configurationService.saveConfiguration(
                        ConfigurationType.fromValue(request.getConfigType()),
                        request.getConfigKey(),
                        request.getTenantId(),
                        config.getConfigData(),
                        false);
                contextRefresher.refresh();
            }

            responseObserver.onNext(DeleteArtifactResponse.newBuilder().setDeleted(deleted).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e, "Failed to delete artifact"));
        }
    }

    // Removed unscoped getConfiguration; use getConfigurationByTenant instead.

    @Override
    public void getConfigurationByTenant(GetConfigurationByTenantRequest request,
            StreamObserver<GetConfigurationResponse> responseObserver) {
        try {
            ConfigurationType configType = ConfigurationType.fromValue(request.getConfigType());
            Optional<ConfigurationDataWithMetadata> configOpt = configurationService
                    .getConfigurationDataWithMetadata(configType, request.getConfigKey(), request.getTenantId());

            GetConfigurationResponse.Builder responseBuilder = GetConfigurationResponse.newBuilder();
            if (configOpt.isPresent()) {
                JsonNode responseNode = objectMapper.valueToTree(configOpt.get());
                if (responseNode.isObject() && request.getTenantId() != null && !request.getTenantId().isBlank()) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) responseNode)
                            .put("tenantId", request.getTenantId());
                }
                Struct.Builder structBuilder = Struct.newBuilder();
                JsonFormat.parser().merge(objectMapper.writeValueAsString(responseNode), structBuilder);
                responseBuilder.setFound(true).setJsonData(structBuilder.build());
            } else {
                responseBuilder.setFound(false);
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e, "Failed to get configuration by tenant"));
        }
    }

    @Override
    public void getRelatedArtifacts(GetRelatedArtifactsRequest request,
            StreamObserver<GetRelatedArtifactsResponse> responseObserver) {
        try {
            Map<String, Object> artifacts = configurationService.getRelatedArtifacts(
                    ConfigurationType.fromValue(request.getConfigType()), request.getConfigKey(),
                    request.getTenantId());

            Struct.Builder structBuilder = Struct.newBuilder();
            if (artifacts != null && !artifacts.isEmpty()) {
                JsonFormat.parser().merge(objectMapper.writeValueAsString(artifacts), structBuilder);
            }
            responseObserver
                    .onNext(GetRelatedArtifactsResponse.newBuilder().setArtifactsMap(structBuilder.build()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e, "Failed to get related artifacts"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getArtifact(GetArtifactRequest request, StreamObserver<GetArtifactResponse> responseObserver) {
        try {
            Optional<Configuration> configOpt = configurationService.getConfiguration(
                    ConfigurationType.fromValue(request.getConfigType()), request.getConfigKey(),
                    request.getTenantId());

            if (configOpt.isEmpty()) {
                responseObserver.onNext(GetArtifactResponse.newBuilder().setFound(false).build());
                responseObserver.onCompleted();
                return;
            }

            Optional<Artifact> artifactOpt = configOpt.get().getArtifacts().stream()
                    .filter(a -> request.getArtifactType().equals(a.getArtifactType()))
                    .findFirst();

            if (artifactOpt.isEmpty()) {
                responseObserver.onNext(GetArtifactResponse.newBuilder().setFound(false).build());
                responseObserver.onCompleted();
                return;
            }

            Artifact artifact = artifactOpt.get();
            Optional<String> contentOpt = Optional.empty();

            if (artifact.getStorageType() == Artifact.StorageType.FILE) {
                contentOpt = artifactService.getArtifactContent(artifact);
            } else if (artifact.getStorageType() == Artifact.StorageType.INLINE) {
                contentOpt = artifactService.getArtifactInlineContent(artifact)
                        .map(JsonNode::toString);
            }

            GetArtifactResponse.Builder responseBuilder = GetArtifactResponse.newBuilder();
            contentOpt.ifPresent(content -> responseBuilder.setFound(true).setContent(content));

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e, "Failed to get artifact"));
        }
    }

    @Override
    public void getAllConfigurations(Empty request, StreamObserver<GetAllConfigurationsResponse> responseObserver) {
        try {
            List<ConfigurationSummary> configs = configurationService.getAllConfigurations();
            GetAllConfigurationsResponse.Builder responseBuilder = GetAllConfigurationsResponse.newBuilder();

            for (ConfigurationSummary configDto : configs) {
                String configJson = objectMapper.writeValueAsString(configDto);
                Struct.Builder structBuilder = Struct.newBuilder();
                JsonFormat.parser().merge(configJson, structBuilder);
                responseBuilder.addConfigurations(structBuilder.build());
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e, "Failed to get all configurations"));
        }
    }

    @Override
    public void getConfigurationsByType(GetConfigurationsByTypeRequest request,
            StreamObserver<GetConfigurationsByTypeResponse> responseObserver) {
        try {
            List<ConfigurationSummary> configs = configurationService
                    .getConfigurationsByType(ConfigurationType.fromValue(request.getConfigType()));
            GetConfigurationsByTypeResponse.Builder responseBuilder = GetConfigurationsByTypeResponse.newBuilder();
            for (ConfigurationSummary configDto : configs) {
                String configJson = objectMapper.writeValueAsString(configDto);
                Struct.Builder structBuilder = Struct.newBuilder();
                JsonFormat.parser().merge(configJson, structBuilder);
                responseBuilder.addConfigurations(structBuilder.build());
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e, "Failed to get configurations by type"));
        }
    }

    /**
     * Maps custom service exceptions to appropriate gRPC status codes.
     *
     * @param exception the exception to map
     * @param operation the operation that failed
     * @return a gRPC StatusRuntimeException with appropriate status code and
     *         message
     */
    private RuntimeException mapExceptionToGrpcStatus(Exception exception, String operation) {
        // Ensure we never exceed header size limits due to an overly large exception
        // message
        String safeMessage = truncate(exception.getMessage(), 512);
        String op = truncate(operation, 128);
        if (exception instanceof ConfigurationNotFoundException) {
            return Status.NOT_FOUND
                    .withDescription(op + ": " + safeMessage)
                    .asRuntimeException();
        } else if (exception instanceof ArtifactNotFoundException) {
            return Status.NOT_FOUND
                    .withDescription(op + ": " + safeMessage)
                    .asRuntimeException();
        } else if (exception instanceof ConfigurationAlreadyExistsException) {
            return Status.ALREADY_EXISTS
                    .withDescription(op + ": " + safeMessage)
                    .asRuntimeException();
        } else if (exception instanceof ConfigurationValidationException) {
            return Status.INVALID_ARGUMENT
                    .withDescription(op + ": " + safeMessage)
                    .asRuntimeException();
        } else if (exception instanceof ArtifactValidationException) {
            return Status.INVALID_ARGUMENT
                    .withDescription(op + ": " + safeMessage)
                    .asRuntimeException();
        } else if (exception instanceof ConfigurationPatchException) {
            return Status.INVALID_ARGUMENT
                    .withDescription(op + ": " + safeMessage)
                    .asRuntimeException();
        } else {
            // For any other exception, return INTERNAL status
            return Status.INTERNAL
                    .withDescription(op + ": " + safeMessage)
                    .asRuntimeException();
        }
    }

    private String truncate(String value, int max) {
        if (value == null)
            return "";
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}