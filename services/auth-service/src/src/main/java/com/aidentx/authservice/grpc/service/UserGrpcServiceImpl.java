package com.aidentx.authservice.grpc.service;

import com.aidentx.auth.grpc.UserGrpcServiceGrpc;
import com.aidentx.auth.grpc.GetUsersByIdentityIdsRequest;
import com.aidentx.auth.grpc.GetUsersByIdentityIdsResponse;
import com.aidentx.auth.grpc.GetUsersByTenantRequest;
import com.aidentx.auth.grpc.GetUsersByTenantResponse;
import com.aidentx.auth.grpc.UserInfo;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * gRPC Service implementation for user operations.
 * Provides methods to retrieve users by various criteria.
 */
@GrpcService
public class UserGrpcServiceImpl extends UserGrpcServiceGrpc.UserGrpcServiceImplBase {

    private final UserRepository userRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public UserGrpcServiceImpl(UserRepository userRepository, UnifiedLoggerFactory loggerFactory) {
        this.userRepository = userRepository;
        this.logger = loggerFactory.getLogger("UserGrpcService", "USER_GRPC_SERVICE");
    }

    /**
     * Retrieves all users belonging to a specific tenant.
     * 
     * @param request the gRPC request containing tenant ID
     * @param responseObserver the response observer to send results
     */
    @Override
    public void getUsersByTenant(GetUsersByTenantRequest request, StreamObserver<GetUsersByTenantResponse> responseObserver) {
        String tenantId = request.getTenantId();
        
        logger.info()
            .message("gRPC request received: getUsersByTenant")
            .details(Map.of("tenantId", tenantId))
            .log();

        try {
            // Validate tenant ID
            if (tenantId == null || tenantId.trim().isEmpty()) {
                logger.warn()
                    .message("Invalid tenant ID provided")
                    .details(Map.of("tenantId", tenantId))
                    .log();

                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Tenant ID is required and cannot be empty")
                    .asRuntimeException());
                return;
            }

            // Retrieve users from database
            List<User> users = userRepository.findByTenantId(tenantId);

            logger.info()
                .message("Retrieved users from database")
                .details(Map.of(
                    "tenantId", tenantId,
                    "userCount", users.size()
                ))
                .log();

            // Convert to gRPC UserInfo objects
            List<UserInfo> userInfoList = users.stream()
                .map(this::convertToUserInfo)
                .collect(Collectors.toList());

            // Build response
            GetUsersByTenantResponse response = GetUsersByTenantResponse.newBuilder()
                .addAllUsers(userInfoList)
                .setTotalCount(userInfoList.size())
                .setTenantId(tenantId)
                .build();

            logger.info()
                .message("Successfully processed getUsersByTenant request")
                .details(Map.of(
                    "tenantId", tenantId,
                    "userCount", userInfoList.size()
                ))
                .log();

            // Send response
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error(e)
                .message("Error processing getUsersByTenant request")
                .details(Map.of(
                    "tenantId", tenantId,
                    "error", e.getMessage()
                ))
                .log();

            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal error while retrieving users: " + e.getMessage())
                .withCause(e)
                .asRuntimeException());
        }
    }

    /**
     * Retrieves users belonging to a tenant and matching Identity-service identity IDs.
     *
     * @param request the gRPC request containing tenant ID and identity IDs
     * @param responseObserver the response observer to send results
     */
    @Override
    public void getUsersByIdentityIds(GetUsersByIdentityIdsRequest request,
                                      StreamObserver<GetUsersByIdentityIdsResponse> responseObserver) {
        String tenantId = request.getTenantId();

        logger.info()
            .message("gRPC request received: getUsersByIdentityIds")
            .details(Map.of(
                "tenantId", tenantId,
                "identityIdCount", request.getIdentityIdsCount()
            ))
            .log();

        try {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Tenant ID is required and cannot be empty")
                    .asRuntimeException());
                return;
            }

            List<UUID> identityIds = request.getIdentityIdsList().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(UUID::fromString)
                .distinct()
                .toList();

            if (identityIds.isEmpty()) {
                responseObserver.onNext(GetUsersByIdentityIdsResponse.newBuilder()
                    .setTenantId(tenantId)
                    .setTotalCount(0)
                    .build());
                responseObserver.onCompleted();
                return;
            }

            List<UserInfo> userInfoList = userRepository.findByIdentityIdInAndTenantId(identityIds, tenantId).stream()
                .map(this::convertToUserInfo)
                .collect(Collectors.toList());

            GetUsersByIdentityIdsResponse response = GetUsersByIdentityIdsResponse.newBuilder()
                .addAllUsers(userInfoList)
                .setTotalCount(userInfoList.size())
                .setTenantId(tenantId)
                .build();

            logger.info()
                .message("Successfully processed getUsersByIdentityIds request")
                .details(Map.of(
                    "tenantId", tenantId,
                    "requestedIdentityCount", identityIds.size(),
                    "userCount", userInfoList.size()
                ))
                .log();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("One or more identityIds are not valid UUID values")
                .withCause(e)
                .asRuntimeException());
        } catch (Exception e) {
            logger.error(e)
                .message("Error processing getUsersByIdentityIds request")
                .details(Map.of(
                    "tenantId", tenantId,
                    "error", e.getMessage()
                ))
                .log();

            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal error while retrieving users by identity IDs: " + e.getMessage())
                .withCause(e)
                .asRuntimeException());
        }
    }

    /**
     * Converts a User entity to UserInfo gRPC message.
     * 
     * @param user the User entity
     * @return UserInfo gRPC message
     */
    private UserInfo convertToUserInfo(User user) {
        UserInfo.Builder builder = UserInfo.newBuilder()
            .setId(user.getId().toString())
            .setIsActive(user.getStatus() == com.aidentx.authservice.enums.UserStatus.ACTIVE);

        // Set tenant ID (required field)
        if (user.getTenantId() != null) {
            builder.setTenantId(user.getTenantId());
        }

        if (user.getIdentityId() != null) {
            builder.setIdentityId(user.getIdentityId().toString());
        }

        // Handle nullable string fields safely
        if (user.getUsername() != null) {
            builder.setUsername(user.getUsername());
        }
        
        if (user.getEmail() != null) {
            builder.setEmail(user.getEmail());
        }
        
        if (user.getFirstName() != null) {
            builder.setFirstName(user.getFirstName());
        }
        
        if (user.getLastName() != null) {
            builder.setLastName(user.getLastName());
        }

        if (user.getDisplayName() != null) {
            builder.setDisplayName(user.getDisplayName());
        }

        // Set status as string - this is a required field so we should always set it
        if (user.getStatus() != null) {
            builder.setStatus(user.getStatus().toString());
        } else {
            // Default to ACTIVE if status is null
            builder.setStatus("ACTIVE");
        }

        // Handle optional integer fields
        if (user.getFailedLoginAttempts() != null) {
            builder.setFailedLoginAttempts(user.getFailedLoginAttempts());
        } else {
            builder.setFailedLoginAttempts(0);
        }

        // Handle optional boolean fields with proper defaults
        if (user.getMfaEnabled() != null) {
            builder.setMfaEnabled(user.getMfaEnabled());
        } else {
            builder.setMfaEnabled(false);
        }

        if (user.getIsPasswordExpired() != null) {
            builder.setIsPasswordExpired(user.getIsPasswordExpired());
        } else {
            builder.setIsPasswordExpired(false);
        }

        // Handle profile data - convert JsonNode to string
        if (user.getProfileData() != null) {
            builder.setProfileData(user.getProfileData().toString());
        } else {
            builder.setProfileData("{}");
        }

        // Handle audit fields - convert UUIDs to strings
        if (user.getCreatedBy() != null) {
            builder.setCreatedBy(user.getCreatedBy().toString());
        } else {
            builder.setCreatedBy("");
        }

        if (user.getUpdatedBy() != null) {
            builder.setUpdatedBy(user.getUpdatedBy().toString());
        } else {
            builder.setUpdatedBy("");
        }

        // Handle timestamps - convert to epoch millis
        if (user.getCreatedAt() != null) {
            builder.setCreatedAt(user.getCreatedAt().toEpochMilli());
        } else {
            builder.setCreatedAt(0);
        }
        
        if (user.getUpdatedAt() != null) {
            builder.setUpdatedAt(user.getUpdatedAt().toEpochMilli());
        } else {
            builder.setUpdatedAt(0);
        }

        if (user.getLastLoginAt() != null) {
            builder.setLastLoginAt(user.getLastLoginAt().toEpochMilli());
        } else {
            builder.setLastLoginAt(0);
        }

        if (user.getPasswordChangedAt() != null) {
            builder.setPasswordChangedAt(user.getPasswordChangedAt().toEpochMilli());
        } else {
            builder.setPasswordChangedAt(0);
        }

        if (user.getLockedUntil() != null) {
            builder.setLockedUntil(user.getLockedUntil().toEpochMilli());
        } else {
            builder.setLockedUntil(0);
        }

        if (user.getEmailVerifiedAt() != null) {
            builder.setEmailVerifiedAt(user.getEmailVerifiedAt().toEpochMilli());
        } else {
            builder.setEmailVerifiedAt(0);
        }

        return builder.build();
    }
}