package com.aidentx.authservice.grpc.service;

import com.aidentx.auth.grpc.GetIdentityIdsRequest;
import com.aidentx.auth.grpc.GetIdentityIdsResponse;
import com.aidentx.auth.grpc.UserIdentityLookupServiceGrpc;
import com.aidentx.auth.grpc.UserIdentityMapping;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * gRPC Service implementation for looking up identity IDs by user IDs.
 * Provides efficient bulk lookup of identity IDs for multiple users.
 */
@GrpcService
@Transactional(readOnly = true)
public class UserIdentityLookupServiceImpl
        extends UserIdentityLookupServiceGrpc.UserIdentityLookupServiceImplBase {

    private final UserRepository userRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public UserIdentityLookupServiceImpl(
            UserRepository userRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.userRepository = userRepository;
        this.logger = loggerFactory.getLogger("UserIdentityLookupService", "USER_IDENTITY_LOOKUP_SERVICE");
    }

    /**
     * Retrieves identity IDs for a list of user IDs in a single request.
     *
     * @param request the gRPC request containing tenant ID and user IDs
     * @param responseObserver the response observer to send results
     */
    @Override
    public void getIdentityIdsByUserIds(
            GetIdentityIdsRequest request,
            StreamObserver<GetIdentityIdsResponse> responseObserver) {

        String tenantId = request.getTenantId();
        List<String> userIds = request.getUserIdsList();

        logger.info()
                .message("gRPC request received: getIdentityIdsByUserIds")
                .details(Map.of(
                        "tenantId", tenantId,
                        "userIdsCount", userIds.size(),
                        "userIds", userIds
                ))
                .log();

        try {
            // Validate request
            validateRequest(tenantId, userIds);

            // Convert string UUIDs to UUID objects
            List<UUID> userUuids;
            try {
                userUuids = userIds.stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                logger.warn()
                        .message("Invalid UUID format in user IDs")
                        .details(Map.of("error", e.getMessage()))
                        .log();

                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("One or more user IDs have invalid UUID format")
                        .asRuntimeException());
                return;
            }

            // Query database for users
            List<User> users = userRepository.findByIdInAndTenantId(userUuids, tenantId);

            logger.info()
                    .message("Retrieved users from database")
                    .details(Map.of(
                            "tenantId", tenantId,
                            "requestedUserCount", userUuids.size(),
                            "foundUserCount", users.size()
                    ))
                    .log();

            // Build response
            GetIdentityIdsResponse response = buildResponse(users, userUuids);

            logger.info()
                    .message("Successfully processed getIdentityIdsByUserIds request")
                    .details(Map.of(
                            "tenantId", tenantId,
                            "mappingsCount", response.getMappingsCount()
                    ))
                    .log();

            // Send response
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error(e)
                    .message("Error processing getIdentityIdsByUserIds request")
                    .details(Map.of(
                            "tenantId", tenantId,
                            "userIdsCount", userIds.size(),
                            "error", e.getMessage()
                    ))
                    .log();

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error while retrieving identity IDs: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    /**
     * Validates the incoming request parameters.
     */
    private void validateRequest(String tenantId, List<String> userIds) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID is required and cannot be empty");
        }

        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("User IDs list cannot be null or empty");
        }

        // Optional: Limit the maximum number of user IDs in a single request
        final int MAX_USER_IDS = 1000;
        if (userIds.size() > MAX_USER_IDS) {
            throw new IllegalArgumentException(
                    String.format("Maximum number of user IDs exceeded. Max: %d, Received: %d",
                            MAX_USER_IDS, userIds.size()));
        }
    }

    /**
     * Builds the gRPC response from the retrieved users.
     */
    private GetIdentityIdsResponse buildResponse(List<User> users, List<UUID> requestedUserUuids) {
        // Create a map for quick lookup
        Map<UUID, UUID> userToIdentityMap = users.stream()
                .filter(user -> user.getIdentityId() != null)
                .collect(Collectors.toMap(
                        User::getId,
                        User::getIdentityId
                ));

        // Build mappings for all requested users
        // If user not found or has no identity ID, leave identityId as empty string
        List<UserIdentityMapping> mappings = requestedUserUuids.stream()
                .map(userId -> {
                    UUID identityId = userToIdentityMap.get(userId);

                    return UserIdentityMapping.newBuilder()
                            .setUserId(userId.toString())
                            .setIdentityId(identityId != null ? identityId.toString() : "")
                            .build();
                })
                .collect(Collectors.toList());

        return GetIdentityIdsResponse.newBuilder()
                .addAllMappings(mappings)
                .build();
    }
}