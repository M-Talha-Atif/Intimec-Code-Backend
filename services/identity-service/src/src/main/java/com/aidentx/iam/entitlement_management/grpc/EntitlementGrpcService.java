package com.aidentx.iam.entitlement_management.grpc;

import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementResponseDto;
import com.aidentx.iam.entitlement_management.service.EntitlementService;
import com.aidentx.iam.grpc.EntitlementLookupServiceGrpc;
import com.aidentx.iam.grpc.GetEntitlementByIdRequest;
import com.aidentx.iam.grpc.GetEntitlementByIdResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Optional;
import java.util.UUID;

/**
 * gRPC server implementation for entitlement lookups.
 *
 * Think of this class as the equivalent of your @RestController —
 * it receives requests, calls your existing EntitlementService,
 * and sends back responses.
 *
 * @GrpcService is from grpc-spring-boot-starter (net.devh).
 * It registers this class with the gRPC server automatically,
 * just like @RestController registers with Spring MVC.
 */
@GrpcService
public class EntitlementGrpcService
        extends EntitlementLookupServiceGrpc.EntitlementLookupServiceImplBase {

    private final EntitlementService entitlementService;
    private final ObjectMapper objectMapper;

    public EntitlementGrpcService(EntitlementService entitlementService,
                                  ObjectMapper objectMapper) {
        this.entitlementService = entitlementService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles the GetEntitlementById RPC call.
     *
     * In REST, this would be:
     *   @GetMapping("/entitlements/{id}")
     *   public ResponseEntity<EntitlementResponseDto> getById(@PathVariable UUID id, ...)
     *
     * In gRPC, the pattern is:
     *   1. Read fields from the request message
     *   2. Call your existing service layer (same as REST!)
     *   3. Build and send the response via StreamObserver
     *
     * StreamObserver is gRPC's way of sending responses:
     *   - onNext(response)    → send the response body
     *   - onCompleted()       → close the connection (like returning 200)
     *   - onError(exception)  → send an error status (like returning 404/500)
     */
    @Override
    public void getEntitlementById(GetEntitlementByIdRequest request,
                                   StreamObserver<GetEntitlementByIdResponse> responseObserver) {

        // 1. Parse request fields (like @PathVariable / @RequestHeader in REST)
        UUID entitlementId;
        try {
            entitlementId = UUID.fromString(request.getEntitlementId());
        } catch (IllegalArgumentException e) {
            // gRPC equivalent of HTTP 400 Bad Request
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid entitlement ID format: " + request.getEntitlementId())
                            .asRuntimeException()
            );
            return;
        }

        // 2. Call your existing service — SAME service layer as your REST controller uses
        Optional<EntitlementResponseDto> result = entitlementService.findById(entitlementId);

        if (result.isEmpty()) {
            // gRPC equivalent of HTTP 404 Not Found
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Entitlement not found: " + entitlementId)
                            .asRuntimeException()
            );
            return;
        }

        // 3. Map DTO → protobuf response (like Jackson serializing to JSON, but manual)
        EntitlementResponseDto dto = result.get();
        GetEntitlementByIdResponse response = mapToProto(dto);

        // 4. Send response (like returning ResponseEntity.ok(dto) in REST)
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Maps your existing EntitlementResponseDto to the protobuf response.
     * This is the gRPC equivalent of Jackson's JSON serialization —
     * but explicit and type-safe instead of reflection-based.
     */
    private GetEntitlementByIdResponse mapToProto(EntitlementResponseDto dto) {
        GetEntitlementByIdResponse.Builder builder = GetEntitlementByIdResponse.newBuilder()
                .setId(str(dto.id()))
                .setTenantId(str(dto.tenantId()))
                .setResourceId(str(dto.resourceId()))
                .setSourceUid(nullSafe(dto.sourceUid()))
                .setApplicationName(nullSafe(dto.resourceName()))
                .setDisplayName(nullSafe(dto.displayName()))
                .setDescription(nullSafe(dto.description()))
                .setObjectClass(nullSafe(dto.objectClass()))
                .setKind(nullSafe(dto.kind()))
                .setLifecycle(nullSafe(dto.lifecycle()))
                .setRiskLevel(nullSafe(dto.riskLevel()))
                .setRiskReason(nullSafe(dto.riskReason()))
                .setRequestable(dto.requestable())
                .setIdentityType(nullSafe(dto.identityType()))
                .setPrivilegeLevel(nullSafe(dto.privilegeLevel()))
                .setOwnerId(str(dto.ownerId()))
                .setRequireApproval(dto.requireApproval())
                .setAllowedAutomatedGrant(dto.allowedAutomatedGrant())
                .setAssignedUsersCount(dto.assignedUsersCount());

        // Nullable int64 fields — 0 means "not set" in protobuf
        if (dto.maxDurationMs() != null) builder.setMaxDurationMs(dto.maxDurationMs());
        if (dto.autoExpireDurationMs() != null) builder.setAutoExpireDurationMs(dto.autoExpireDurationMs());
        if (dto.createdAt() != null) builder.setCreatedAt(dto.createdAt());
        if (dto.updatedAt() != null) builder.setUpdatedAt(dto.updatedAt());

        // Classification list
        if (dto.classification() != null) {
            builder.addAllClassification(dto.classification());
        }

        // Attributes map → JSON string
        if (dto.attributes() != null) {
            try {
                builder.setAttributesJson(objectMapper.writeValueAsString(dto.attributes()));
            } catch (Exception e) {
                builder.setAttributesJson("{}");
            }
        }

        return builder.build();
    }

    /** UUID.toString() with null safety */
    private String str(Object value) {
        return value != null ? value.toString() : "";
    }

    /** Null-safe string for protobuf (protobuf doesn't allow null strings) */
    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}