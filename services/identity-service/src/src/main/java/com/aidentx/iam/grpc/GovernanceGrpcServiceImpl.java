package com.aidentx.iam.grpc;

import com.aidentx.dal.protos.governance.*;
import com.aidentx.iam.entitlement_management.service.EntitlementService;
import com.aidentx.iam.role_management.service.RoleService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
public class GovernanceGrpcServiceImpl extends GovernanceServiceGrpc.GovernanceServiceImplBase {

    private final RoleService roleService;
    private final EntitlementService entitlementService;

    public GovernanceGrpcServiceImpl(RoleService roleService, EntitlementService entitlementService) {
        this.roleService = roleService;
        this.entitlementService = entitlementService;
    }

    @Override
    public void getRoleById(GetByIdRequest request, StreamObserver<RoleMessage> responseObserver) {
        try {
            UUID roleId = UUID.fromString(request.getId());
            roleService.findRoleWithEntitlements(roleId) // Assumes a new service method
                .ifPresentOrElse(
                    role -> {
                        RoleMessage response = RoleMessage.newBuilder()
                            .setId(role.id().toString())
                            .setTenantId(role.tenantId())
                            .setDisplayName(role.displayName())
                            .addAllIncludedEntitlementIds(
                                role.includedEntitlements().stream()
                                    .map(e -> e.id().toString())
                                    .collect(Collectors.toList())
                            )
                            .build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    },
                    () -> responseObserver.onError(Status.NOT_FOUND.asRuntimeException())
                );
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getEntitlementById(GetByIdRequest request, StreamObserver<EntitlementMessage> responseObserver) {
        try {
            UUID entitlementId = UUID.fromString(request.getId());
            entitlementService.findById(entitlementId)
                .ifPresentOrElse(
                    entitlement -> {
                        EntitlementMessage response = EntitlementMessage.newBuilder()
                            .setId(entitlement.id().toString())
                            .setTenantId(entitlement.tenantId())
                            .setDisplayName(entitlement.displayName())
                            .setResourceId(entitlement.resourceId().toString())
                            .setSourceUid(entitlement.sourceUid())
                            .build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    },
                    () -> responseObserver.onError(Status.NOT_FOUND.asRuntimeException())
                );
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}