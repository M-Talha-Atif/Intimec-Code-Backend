package com.aidentx.iam.identity_management.grpc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.aidentx.dal.protos.identity.CreateIdentityFromAuthoritativeSourceRequest;
import com.aidentx.dal.protos.identity.DeleteIdentityRequest;
import com.aidentx.dal.protos.identity.DeleteIdentityResponse;
import com.aidentx.dal.protos.identity.FindIdentityByAttributeRequest;
import com.aidentx.dal.protos.identity.FindIdentityByAttributeResponse;
import com.aidentx.dal.protos.identity.GetIdentityByIdRequest;
import com.aidentx.dal.protos.identity.IdentityExistsRequest;
import com.aidentx.dal.protos.identity.IdentityExistsResponse;
import com.aidentx.dal.protos.identity.IdentityMessage;
import com.aidentx.dal.protos.identity.IdentityServiceGrpc;
import com.aidentx.dal.protos.identity.PushAuthoritativeDataRequest;
import com.aidentx.dal.protos.identity.PushDataShardRequest;
import com.aidentx.iam.identity_management.mapper.IdentityGrpcMapper;
import com.aidentx.iam.identity_management.model.dto.IdentityResponseDto;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.service.IdentityService;
import com.google.protobuf.Empty;
import com.google.protobuf.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class IdentityGrpcServiceImpl extends IdentityServiceGrpc.IdentityServiceImplBase {

    private final IdentityService identityService;
    private final IdentityGrpcMapper identityGrpcMapper;

    // CORRECTED: Removed unnecessary repository and objectMapper dependencies.
    public IdentityGrpcServiceImpl(IdentityService identityService,
            IdentityGrpcMapper identityGrpcMapper) {
        this.identityService = identityService;
        this.identityGrpcMapper = identityGrpcMapper;
    }

    /**
     * Retrieves a complete identity by its ID. This method is fully compatible
     * with the new, structured data model.
     */
    @Override
    public void getIdentityById(GetIdentityByIdRequest request, StreamObserver<IdentityMessage> responseObserver) {
        try {
            String tenantId = request.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("tenant_id is required")
                        .asRuntimeException());
                return;
            }

            UUID identityId = UUID.fromString(request.getId());
            Optional<IdentityResponseDto> identityOpt = identityService.findByTenantIdAndId(tenantId, identityId);

            if (identityOpt.isPresent()) {
                // CORRECTED: The mapper now correctly converts the DTO to the Protobuf message.
                IdentityMessage responseMessage = identityGrpcMapper.toIdentityMessage(identityOpt.get());
                responseObserver.onNext(responseMessage);
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Identity not found with ID: " + request.getId())
                        .asRuntimeException());
            }
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format for ID: " + request.getId())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to retrieve identity: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    /**
     * Checks if an identity exists by its UUID.
     */
    @Override
    public void identityExists(IdentityExistsRequest request, StreamObserver<IdentityExistsResponse> responseObserver) {
        try {
            String tenantId = request.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("tenant_id is required")
                        .asRuntimeException());
                return;
            }
            UUID identityId = UUID.fromString(request.getIdentityId());
            boolean exists = identityService.findByTenantIdAndId(tenantId, identityId).isPresent();
            IdentityExistsResponse response = IdentityExistsResponse.newBuilder()
                    .setExists(exists)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to check identity existence: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * NOTE: The following methods require significant new business logic in the
     * service
     * layer before they can be implemented. They are correctly marked as
     * unimplemented.
     */

    @Override
    public void deleteIdentity(DeleteIdentityRequest request, StreamObserver<DeleteIdentityResponse> responseObserver) {
        try {
            String tenantId = request.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("tenant_id is required")
                        .asRuntimeException());
                return;
            }
            UUID identityId = UUID.fromString(request.getId());
            identityService.deleteIdentity(tenantId, identityId);
            DeleteIdentityResponse response = DeleteIdentityResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Identity deleted")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format for ID: " + request.getId())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to delete identity: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void findIdentityByAttribute(FindIdentityByAttributeRequest request,
            StreamObserver<FindIdentityByAttributeResponse> responseObserver) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("tenant_id is required")
                    .asRuntimeException());
            return;
        }
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("findIdentityByAttribute is not yet implemented.")
                .asRuntimeException());
    }

    // @Override
    // public void pushAuthoritativeData(PushAuthoritativeDataRequest request,
    // StreamObserver<Empty> responseObserver) {

    // if (request.getTenantId() == null || request.getTenantId().isBlank()) {
    // responseObserver.onError(Status.INVALID_ARGUMENT
    // .withDescription("tenant_id is required")
    // .asRuntimeException());
    // return;
    // }
    // try {
    // Map<String, Object> attributes = new HashMap<>(request.getAttributesMap());
    // for (Map.Entry<String, Value> entry : request.getAttributesMap().entrySet())
    // {
    // attributes.put(entry.getKey(), convertProtoValue(entry.getValue()));
    // }
    // UUID resourceId;

    // if (request.getResourceId() != null && !request.getResourceId().isBlank()) {
    // resourceId = UUID.fromString(request.getResourceId());
    // } else {
    // throw new IllegalArgumentException("resource_id is required and must be a
    // valid UUID");
    // }

    // IdentityResponseDto identity =
    // identityService.processAuthoritativeData(request.getTenantId(),
    // request.getResourceId(),
    // request.getSourceName(), attributes, IdentityType.EMPLOYEE); // employee for
    // now then we generalize

    // // IdentityMessage responseMessage =
    // // identityGrpcMapper.toIdentityMessage(identity);
    // responseObserver.onNext(Empty.getDefaultInstance()); // i think it's more
    // logical to return a message
    // // representing the created/updated identity
    // responseObserver.onCompleted();
    // return;
    // } catch (IllegalArgumentException e) {
    // responseObserver.onError(Status.INVALID_ARGUMENT
    // .withDescription(e.getMessage())
    // .asRuntimeException());
    // return;
    // } catch (Exception e) {
    // // StringBuilder sb = new StringBuilder();
    // // for (StackTraceElement element : e.getStackTrace()) {
    // // sb.append("\n\tat ").append(element.toString());
    // // }
    // responseObserver.onError(Status.INTERNAL
    // .withDescription("Failed to process authoritative data: " + e.getMessage())
    // .withCause(e)
    // .asRuntimeException());
    // return;
    // }
    // }

    private Object convertProtoValue(Value val) {
        switch (val.getKindCase()) {
            case STRING_VALUE:
                return val.getStringValue();
            case NUMBER_VALUE:
                return val.getNumberValue();
            case BOOL_VALUE:
                return val.getBoolValue();
            case LIST_VALUE:
                return val.getListValue().getValuesList().stream().map(this::convertProtoValue).toList();
            case STRUCT_VALUE:
                Map<String, Object> map = new HashMap<>();
                val.getStructValue().getFieldsMap().forEach((k, v) -> map.put(k, convertProtoValue(v)));
                return map;
            case KIND_NOT_SET:
            default:
                return null;
        }
    }

    @Override
    public void createIdentityFromAuthoritativeSource(CreateIdentityFromAuthoritativeSourceRequest request,
            StreamObserver<IdentityMessage> responseObserver) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("tenant_id is required")
                    .asRuntimeException());
            return;
        }
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("createIdentityFromAuthoritativeSource is not yet implemented.")
                .asRuntimeException());
    }

    @Override
    public void pushDataShard(PushDataShardRequest request, StreamObserver<Empty> responseObserver) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("tenant_id is required")
                    .asRuntimeException());
            return;
        }
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("pushDataShard is not yet implemented.")
                .asRuntimeException());
    }
}