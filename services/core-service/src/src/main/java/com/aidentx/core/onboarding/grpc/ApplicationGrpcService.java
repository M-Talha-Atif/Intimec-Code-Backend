package com.aidentx.core.onboarding.grpc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aidentx.core.onboarding.model.entity.Application;
import com.aidentx.core.onboarding.repository.ApplicationRepository;
import com.aidentx.dal.protos.application.ApplicationDTO;
import com.aidentx.dal.protos.application.ApplicationSchemaAttributeDTO;
import com.aidentx.dal.protos.application.ApplicationServiceGrpc;
import com.aidentx.dal.protos.application.GetApplicationRequest;
import com.aidentx.dal.protos.application.GetApplicationResponse;
import com.aidentx.dal.protos.application.GetApplicationSchemaAttributesRequest;
import com.aidentx.dal.protos.application.GetApplicationSchemaAttributesResponse;
import com.aidentx.dal.protos.application.GetApplicationsByTenantRequest;
import com.aidentx.dal.protos.application.GetApplicationsByTenantResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * gRPC service implementation for application management.
 * Provides internal service-to-service communication for fetching application
 * configurations.
 */
@GrpcService
public class ApplicationGrpcService extends ApplicationServiceGrpc.ApplicationServiceImplBase {

    private final ApplicationRepository applicationRepository;
    private final IUnifiedLogger logger;

    public ApplicationGrpcService(
            ApplicationRepository applicationRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.applicationRepository = applicationRepository;
        this.logger = loggerFactory.getLogger(ApplicationGrpcService.class);
    }

    @Override
    public void getApplication(GetApplicationRequest request, StreamObserver<GetApplicationResponse> responseObserver) {
        try {
            logger.info().message("gRPC GetApplication called")
                    .detail("tenantId", request.getTenantId())
                    .detail("applicationId", request.getApplicationId())
                    .log();

            UUID applicationId = UUID.fromString(request.getApplicationId());
            Optional<Application> applicationOpt = applicationRepository.findByIdAndTenantId(
                    applicationId, request.getTenantId());

            if (applicationOpt.isEmpty()) {
                logger.warn().message("Application not found")
                        .detail("tenantId", request.getTenantId())
                        .detail("applicationId", request.getApplicationId())
                        .log();

                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Application not found: " + request.getApplicationId())
                        .asRuntimeException());
                return;
            }

            Application application = applicationOpt.get();
            ApplicationDTO dto = convertToDto(application);

            GetApplicationResponse response = GetApplicationResponse.newBuilder()
                    .setApplication(dto)
                    .build();

            logger.info().message("Successfully retrieved application")
                    .detail("tenantId", request.getTenantId())
                    .detail("applicationId", request.getApplicationId())
                    .detail("applicationName", application.getName())
                    .log();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            logger.error(e).message("Invalid application ID format")
                    .detail("applicationId", request.getApplicationId())
                    .log();

            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid application ID format")
                    .withCause(e)
                    .asRuntimeException());

        } catch (Exception e) {
            logger.error(e).message("Error retrieving application")
                    .detail("tenantId", request.getTenantId())
                    .detail("applicationId", request.getApplicationId())
                    .log();

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error retrieving application")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getApplicationsByTenant(GetApplicationsByTenantRequest request,
            StreamObserver<GetApplicationsByTenantResponse> responseObserver) {
        try {
            logger.info().message("gRPC GetApplicationsByTenant called")
                    .detail("tenantId", request.getTenantId())
                    .detail("pageSize", request.getPageSize())
                    .detail("pageOffset", request.getPageOffset())
                    .log();

            List<Application> applications = applicationRepository.findByTenantId(request.getTenantId());

            // Convert to DTOs
            List<ApplicationDTO> dtos = applications.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            GetApplicationsByTenantResponse response = GetApplicationsByTenantResponse.newBuilder()
                    .addAllApplications(dtos)
                    .setTotalCount(dtos.size())
                    .build();

            logger.info().message("Successfully retrieved applications by tenant")
                    .detail("tenantId", request.getTenantId())
                    .detail("count", dtos.size())
                    .log();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error(e).message("Error retrieving applications by tenant")
                    .detail("tenantId", request.getTenantId())
                    .log();

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error retrieving applications")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getApplicationSchemaAttributes(GetApplicationSchemaAttributesRequest request,
            StreamObserver<GetApplicationSchemaAttributesResponse> responseObserver) {
        try {
            logger.info().message("gRPC GetApplicationSchemaAttributes called")
                    .detail("tenantId", request.getTenantId())
                    .detail("applicationId", request.getApplicationId())
                    .log();

            UUID applicationId = UUID.fromString(request.getApplicationId());
            Optional<Application> applicationOpt = applicationRepository.findByIdAndTenantId(
                    applicationId, request.getTenantId());

            if (applicationOpt.isEmpty()) {
                logger.warn().message("Application not found for schema attributes")
                        .detail("tenantId", request.getTenantId())
                        .detail("applicationId", request.getApplicationId())
                        .log();

                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Application not found: " + request.getApplicationId())
                        .asRuntimeException());
                return;
            }

            Application application = applicationOpt.get();

            // Eagerly fetch schema attributes
            application.getSchemaAttributes().size();

                // Convert to DTOs. Skip malformed rows instead of failing the entire call.
                List<com.aidentx.core.onboarding.model.entity.ApplicationSchemaAttribute> schemaAttrs =
                    application.getSchemaAttributes();

                long malformedCount = schemaAttrs.stream()
                    .filter(attr -> !StringUtils.hasText(attr.getName())
                        || !StringUtils.hasText(attr.getType())
                        || !StringUtils.hasText(attr.getObjectType()))
                    .count();

                List<ApplicationSchemaAttributeDTO> attributeDtos = schemaAttrs.stream()
                    .filter(attr -> StringUtils.hasText(attr.getName())
                        && StringUtils.hasText(attr.getType())
                        && StringUtils.hasText(attr.getObjectType()))
                    .map(attr -> {
                        ApplicationSchemaAttributeDTO.Builder builder = ApplicationSchemaAttributeDTO.newBuilder()
                            .setId(attr.getId().toString())
                            .setApplicationId(application.getId().toString())
                            .setName(attr.getName())
                            .setType(attr.getType())
                            .setObjectType(attr.getObjectType())
                            .setIsDisplayAttribute(attr.isDisplayAttribute())
                            .setIsDescriptionAttribute(attr.isDescriptionAttribute())
                            .setFlags(attr.getFlags() != null ? jsonNodeToStruct(attr.getFlags())
                                    : Struct.getDefaultInstance());
                        if (attr.getObjectTypeKind() != null) {
                            builder.setObjectTypeKind(attr.getObjectTypeKind());
                        }
                        if (attr.getCategory() != null) {
                            builder.setCategory(attr.getCategory());
                        }
                        if (attr.getDescription() != null) {
                            builder.setDescription(attr.getDescription());
                        }
                        return builder.build();
                    })
                    .collect(Collectors.toList());

                    if (malformedCount > 0) {
                    logger.warn().message("Skipped malformed schema attributes while building gRPC response")
                        .detail("tenantId", request.getTenantId())
                        .detail("applicationId", request.getApplicationId())
                        .detail("skippedCount", malformedCount)
                        .log();
                    }

            GetApplicationSchemaAttributesResponse response = GetApplicationSchemaAttributesResponse.newBuilder()
                    .addAllAttributes(attributeDtos)
                    .build();

            logger.info().message("Successfully retrieved application schema attributes")
                    .detail("tenantId", request.getTenantId())
                    .detail("applicationId", request.getApplicationId())
                    .detail("attributeCount", attributeDtos.size())
                    .log();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            logger.error(e).message("Invalid application ID format")
                    .detail("applicationId", request.getApplicationId())
                    .log();

            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid application ID format")
                    .withCause(e)
                    .asRuntimeException());

        } catch (Exception e) {
            logger.error(e).message("Error retrieving application schema attributes")
                    .detail("tenantId", request.getTenantId())
                    .detail("applicationId", request.getApplicationId())
                    .log();

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error retrieving schema attributes")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    /**
     * Converts Application entity to ApplicationDTO protobuf message.
     */
    private ApplicationDTO convertToDto(Application application) {
        ApplicationDTO.Builder builder = ApplicationDTO.newBuilder()
                .setId(application.getId().toString())
                .setTenantId(application.getTenantId())
                .setName(application.getName())
                .setIsAuthoritative(application.isAuthoritative());

        // Optional fields
        if (application.getDescription() != null) {
            builder.setDescription(application.getDescription());
        }
        if (application.getAssociatedSchemaId() != null) {
            builder.setAssociatedSchemaId(application.getAssociatedSchemaId().toString());
        }
        if (application.getLifecycleStatus() != null) {
            builder.setLifecycleStatus(application.getLifecycleStatus().name());
        }
        if (application.getOperationalStatus() != null) {
            builder.setOperationalStatus(application.getOperationalStatus().name());
        }
        if (application.getLastStatusDetails() != null) {
            builder.setLastStatusDetails(application.getLastStatusDetails());
        }
        if (application.getIdentityType() != null) {
            builder.setIdentityType(application.getIdentityType());
        }
        if (application.getRiskLevel() != null) {
            builder.setRiskLevel(application.getRiskLevel());
        }
        if (application.getIconPath() != null) {
            builder.setIconPath(application.getIconPath());
        }

        // Convert JSONB fields to Struct
        if (application.getConnectorConfiguration() != null) {
            builder.setConnectorConfiguration(jsonNodeToStruct(application.getConnectorConfiguration()));
        }
        if (application.getTechnicalOwners() != null) {
            builder.setTechnicalOwners(jsonNodeToStruct(application.getTechnicalOwners()));
        }
        if (application.getBusinessOwners() != null) {
            builder.setBusinessOwners(jsonNodeToStruct(application.getBusinessOwners()));
        }
        if (application.getCategories() != null) {
            builder.setCategories(jsonNodeToStruct(application.getCategories()));
        }
        if (application.getComplianceLabels() != null) {
            builder.setComplianceLabels(jsonNodeToStruct(application.getComplianceLabels()));
        }
        if (application.getScopes() != null) {
            builder.setScopes(jsonNodeToStruct(application.getScopes()));
        }
        if (application.getDepartment() != null) {
            builder.setDepartment(jsonNodeToStruct(application.getDepartment()));
        }
        if (application.getAccessPolicy() != null) {
            builder.setAccessPolicy(jsonNodeToStruct(application.getAccessPolicy()));
        }

        // Timestamps
        if (application.getCreatedAt() != null) {
            builder.setCreatedAt(application.getCreatedAt().toEpochMilli());
        }
        if (application.getUpdatedAt() != null) {
            builder.setUpdatedAt(application.getUpdatedAt().toEpochMilli());
        }

        return builder.build();
    }

    /**
     * Converts JsonNode to protobuf Struct.
     */
    private Struct jsonNodeToStruct(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return Struct.getDefaultInstance();
        }

        Struct.Builder structBuilder = Struct.newBuilder();

        if (jsonNode.isObject()) {
            jsonNode.fields().forEachRemaining(entry -> {
                structBuilder.putFields(entry.getKey(), jsonNodeToValue(entry.getValue()));
            });
        }

        return structBuilder.build();
    }

    /**
     * Converts JsonNode to protobuf Value.
     */
    private Value jsonNodeToValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
        }

        if (node.isBoolean()) {
            return Value.newBuilder().setBoolValue(node.asBoolean()).build();
        }

        if (node.isNumber()) {
            return Value.newBuilder().setNumberValue(node.asDouble()).build();
        }

        if (node.isTextual()) {
            return Value.newBuilder().setStringValue(node.asText()).build();
        }

        if (node.isArray()) {
            com.google.protobuf.ListValue.Builder listBuilder = com.google.protobuf.ListValue.newBuilder();
            node.elements().forEachRemaining(element -> {
                listBuilder.addValues(jsonNodeToValue(element));
            });
            return Value.newBuilder().setListValue(listBuilder).build();
        }

        if (node.isObject()) {
            return Value.newBuilder().setStructValue(jsonNodeToStruct(node)).build();
        }

        // Fallback to string
        return Value.newBuilder().setStringValue(node.toString()).build();
    }
}
