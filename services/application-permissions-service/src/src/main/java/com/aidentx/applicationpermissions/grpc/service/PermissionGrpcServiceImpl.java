package com.aidentx.applicationpermissions.grpc.service;

import com.aidentx.applicationpermissions.grpc.PermissionCheckRequest;
import com.aidentx.applicationpermissions.grpc.PermissionCheckResponse;
import com.aidentx.applicationpermissions.grpc.EndpointCheckRequest;
import com.aidentx.applicationpermissions.grpc.EndpointCheckResponse;
import com.aidentx.applicationpermissions.grpc.ActivateRoleRequest;
import com.aidentx.applicationpermissions.grpc.ActivateRoleResponse;
import com.aidentx.applicationpermissions.grpc.PermissionServiceGrpc;
import com.aidentx.applicationpermissions.roles_management.service.UserPermissionService;
import com.aidentx.applicationpermissions.roles_management.repository.RoleRepository;
import com.aidentx.applicationpermissions.shared.SharedAppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import io.grpc.stub.StreamObserver;
import io.grpc.Status;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * gRPC service implementation for permission checking operations.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@GrpcService
public class PermissionGrpcServiceImpl extends PermissionServiceGrpc.PermissionServiceImplBase {
    
    private final UserPermissionService userPermissionService;
    private final RoleRepository roleRepository;
    private final IUnifiedLogger logger;
    
    @Autowired
    public PermissionGrpcServiceImpl(UserPermissionService userPermissionService,
                                   RoleRepository roleRepository,
                                   UnifiedLoggerFactory loggerFactory) {
        this.userPermissionService = userPermissionService;
        this.roleRepository = roleRepository;
        this.logger = loggerFactory.getLogger("PermissionGrpcService", "GRPC_SERVICE");
    }
    
    @Override
    public void checkPermission(PermissionCheckRequest request, 
                               StreamObserver<PermissionCheckResponse> responseObserver) {
        
        logger.info()
            .message(SharedAppConstants.LOG_GRPC_PERMISSION_CHECK_RECEIVED)
            .detail(SharedAppConstants.DETAIL_USER_ID, request.getUserId())
            .detail(SharedAppConstants.DETAIL_TENANT_ID, request.getTenantId())
            .detail(SharedAppConstants.DETAIL_API_ENDPOINT, request.getApiEndpoint())
            .detail(SharedAppConstants.DETAIL_HTTP_METHOD, request.getHttpMethod())
            .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
            .log();
        
        try {
            logger.trace()
                .message(SharedAppConstants.CALLING_PERMISSION_SERVICE)
                .detail(SharedAppConstants.DETAIL_USER_ID, request.getUserId())
                .detail(SharedAppConstants.DETAIL_TENANT_ID, request.getTenantId())
                .detail(SharedAppConstants.DETAIL_API_ENDPOINT, request.getApiEndpoint())
                .detail(SharedAppConstants.DETAIL_HTTP_METHOD, request.getHttpMethod())
                .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                .log();
                
            boolean hasPermission = userPermissionService.hasPermissionForEndpoint(
                request.getUserId(), 
                request.getTenantId(), 
                request.getApiEndpoint(), 
                request.getHttpMethod()
            );
            
            logger.debug()
                .message(SharedAppConstants.BUILDING_PERMISSION_RESPONSE)
                .detail(SharedAppConstants.DETAIL_HAS_PERMISSION, String.valueOf(hasPermission))
                .detail(SharedAppConstants.DETAIL_USER_ID, request.getUserId())
                .detail(SharedAppConstants.DETAIL_API_ENDPOINT, request.getApiEndpoint())
                .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                .log();
            
            PermissionCheckResponse response = PermissionCheckResponse.newBuilder()
                .setHasPermission(hasPermission)
                .setUserId(request.getUserId())
                .setTenantId(request.getTenantId())
                .setApiEndpoint(request.getApiEndpoint())
                .setHttpMethod(request.getHttpMethod())
                .build();
            
            logger.debug()
                .message(SharedAppConstants.LOG_GRPC_PERMISSION_CHECK_COMPLETED)
                .detail(SharedAppConstants.DETAIL_HAS_PERMISSION, String.valueOf(hasPermission))
                .detail(SharedAppConstants.DETAIL_USER_ID, request.getUserId())
                .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                .log();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            logger.error(e)
                .message(SharedAppConstants.LOG_GRPC_PERMISSION_CHECK_ERROR)
                .detail(SharedAppConstants.DETAIL_USER_ID, request.getUserId())
                .detail(SharedAppConstants.DETAIL_TENANT_ID, request.getTenantId())
                .detail(SharedAppConstants.DETAIL_API_ENDPOINT, request.getApiEndpoint())
                .detail(SharedAppConstants.DETAIL_HTTP_METHOD, request.getHttpMethod())
                .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                .log();
            
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void checkEndpointExists(EndpointCheckRequest request, 
                                   StreamObserver<EndpointCheckResponse> responseObserver) {
        
        logger.debug()
            .message(SharedAppConstants.LOG_GRPC_ENDPOINT_CHECK_RECEIVED)
            .detail(SharedAppConstants.DETAIL_ENDPOINT, request.getEndpoint())
            .detail(SharedAppConstants.DETAIL_HTTP_METHOD, request.getHttpMethod())
            .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
            .log();
        
        try {
            logger.trace()
                .message(SharedAppConstants.CALLING_ENDPOINT_SERVICE)
                .detail(SharedAppConstants.DETAIL_ENDPOINT, request.getEndpoint())
                .detail(SharedAppConstants.DETAIL_HTTP_METHOD, request.getHttpMethod())
                .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                .log();
                
            boolean isManaged = userPermissionService.isEndpointManaged(
                request.getEndpoint(), 
                request.getHttpMethod()
            );
            
            logger.debug()
                .message(SharedAppConstants.BUILDING_ENDPOINT_RESPONSE)
                .detail(SharedAppConstants.DETAIL_IS_MANAGED, String.valueOf(isManaged))
                .detail(SharedAppConstants.DETAIL_ENDPOINT, request.getEndpoint())
                .detail(SharedAppConstants.DETAIL_HTTP_METHOD, request.getHttpMethod())
                .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                .log();
            
            EndpointCheckResponse response = EndpointCheckResponse.newBuilder()
                .setIsManaged(isManaged)
                .setEndpoint(request.getEndpoint())
                .setHttpMethod(request.getHttpMethod())
                .build();
            
            logger.debug()
                .message(SharedAppConstants.LOG_GRPC_ENDPOINT_CHECK_COMPLETED)
                .detail(SharedAppConstants.DETAIL_IS_MANAGED, String.valueOf(isManaged))
                .detail(SharedAppConstants.DETAIL_ENDPOINT, request.getEndpoint())
                .detail(SharedAppConstants.DETAIL_HTTP_METHOD, request.getHttpMethod())
                .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                .log();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            logger.error(e)
                .message(SharedAppConstants.LOG_GRPC_ENDPOINT_CHECK_ERROR)
                .detail(SharedAppConstants.DETAIL_ENDPOINT, request.getEndpoint())
                .detail(SharedAppConstants.DETAIL_HTTP_METHOD, request.getHttpMethod())
                .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                .log();
            
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void activateRole(ActivateRoleRequest request, 
                            StreamObserver<ActivateRoleResponse> responseObserver) {
        
        logger.info()
            .message("gRPC role activation request received")
            .detail("roleId", request.getRoleId())
            .detail("tenantId", request.getTenantId())
            .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
            .log();
        
        try {
            UUID roleId = UUID.fromString(request.getRoleId());
            String tenantId = request.getTenantId();
            
            // Get role and verify tenant ownership
            com.aidentx.applicationpermissions.roles_management.model.entity.Role role = 
                roleRepository.findById(roleId).orElse(null);
            
            if (role == null || !role.getTenantId().equals(tenantId)) {
                logger.warn()
                    .message("Role not found or access denied")
                    .detail("roleId", request.getRoleId())
                    .detail("tenantId", tenantId)
                    .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                    .log();
                
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("Role not found or access denied")
                        .asRuntimeException()
                );
                return;
            }
            
            // Activate role
            role.setIsActive(true);
            roleRepository.save(role);
            
            logger.info()
                .message("Role activated successfully via gRPC")
                .detail("roleId", request.getRoleId())
                .detail("roleName", role.getRoleName())
                .detail("tenantId", tenantId)
                .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                .log();
            
            ActivateRoleResponse response = ActivateRoleResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Role activated successfully")
                .setRoleId(request.getRoleId())
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (IllegalArgumentException e) {
            logger.error(e)
                .message("Invalid role ID format")
                .detail("roleId", request.getRoleId())
                .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                .log();
            
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("Invalid role ID format: " + e.getMessage())
                    .asRuntimeException()
            );
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to activate role via gRPC")
                .detail("roleId", request.getRoleId())
                .detail("tenantId", request.getTenantId())
                .objectType(SharedAppConstants.OBJECT_TYPE_PERMISSION_GRPC_SERVICE)
                .log();
            
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Failed to activate role: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException()
            );
        }
    }
}