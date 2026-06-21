package com.aidentx.authservice.grpc.service;

import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.common.protos.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * gRPC service implementation for user and identity lookup operations.
 * Provides search with pagination and batch lookup by IDs.
 */
@GrpcService
public class UserIdentityGrpcServiceImpl extends UserIdentityServiceGrpc.UserIdentityServiceImplBase {

    private final UserRepository userRepository;
    private final IUnifiedLogger logger;

    public UserIdentityGrpcServiceImpl(
            UserRepository userRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.userRepository = userRepository;
        this.logger = loggerFactory.getLogger("UserIdentityGrpcService", "USER_IDENTITY_GRPC_SERVICE");
    }

    @Override
    public void searchUsersAndIdentities(
            SearchUsersAndIdentitiesRequest request,
            StreamObserver<SearchUsersAndIdentitiesResponse> responseObserver) {

        try {
            logger.info()
                    .message("gRPC request received: searchUsersAndIdentities")
                    .details(Map.of(
                            "tenantId", request.getTenantId(),
                            "searchQuery", request.getSearchQuery(),
                            "includeUsers", request.getIncludeUsers(),
                            "page", request.getPage(),
                            "size", request.getSize()))
                    .log();

            String tenantId = request.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Tenant ID is required")
                        .asRuntimeException());
                return;
            }

            // Only process if includeUsers is true (identities handled by identity-service)
            if (!request.getIncludeUsers()) {
                SearchUsersAndIdentitiesResponse response = SearchUsersAndIdentitiesResponse.newBuilder()
                        .setPage(request.getPage())
                        .setSize(request.getSize())
                        .setTotalElements(0)
                        .setTotalPages(0)
                        .setFirst(true)
                        .setLast(true)
                        .setNumberOfElements(0)
                        .setEmpty(true)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Pagination setup
            int page = Math.max(0, request.getPage());
            int size = Math.min(Math.max(1, request.getSize()), 100); // Max 100 per page

            Sort sort = Sort.by(Sort.Direction.ASC, "username"); // Default sort
            if (request.getSortBy() != null && !request.getSortBy().isBlank()) {
                Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDirection())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
                sort = Sort.by(direction, request.getSortBy());
            }

            Pageable pageable = PageRequest.of(page, size, sort);

            // Search users
            Page<User> usersPage;
            String searchQuery = request.getSearchQuery();

            if (searchQuery != null && !searchQuery.isBlank()) {
                // Search by username or email
                usersPage = userRepository.searchByTenantIdAndTerm(
                        tenantId, searchQuery, pageable);
            } else {
                // Get all users for tenant
                usersPage = userRepository.findByTenantId(tenantId, pageable);
            }

            // Map to protobuf
            List<UserIdentityInfo> userInfoList = usersPage.getContent().stream()
                    .map(this::mapUserToUserIdentityInfo)
                    .collect(Collectors.toList());

            SearchUsersAndIdentitiesResponse response = SearchUsersAndIdentitiesResponse.newBuilder()
                    .addAllContent(userInfoList)
                    .setPage(page)
                    .setSize(size)
                    .setTotalElements((int) usersPage.getTotalElements())
                    .setTotalPages(usersPage.getTotalPages())
                    .setFirst(usersPage.isFirst())
                    .setLast(usersPage.isLast())
                    .setNumberOfElements(usersPage.getNumberOfElements())
                    .setEmpty(usersPage.isEmpty())
                    .build();

            logger.info()
                    .message("Successfully processed searchUsersAndIdentities")
                    .details(Map.of("resultCount", userInfoList.size()))
                    .log();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error()
                    .message("Error processing searchUsersAndIdentities request")
                    .details(Map.of("error", e.getMessage()))
                    .log();

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error while searching: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserIdentitiesByIds(
            GetUserIdentitiesByIdsRequest request,
            StreamObserver<GetUserIdentitiesByIdsResponse> responseObserver) {

        try {
            logger.info()
                    .message("gRPC request received: getUserIdentitiesByIds")
                    .details(Map.of(
                            "tenantId", request.getTenantId(),
                            "idsCount", request.getIdsList().size(),
                            "includeUsers", request.getIncludeUsers()))
                    .log();

            String tenantId = request.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Tenant ID is required")
                        .asRuntimeException());
                return;
            }

            List<UserIdentityInfo> results = new java.util.ArrayList<>();

            // Only process if includeUsers is true
            if (request.getIncludeUsers()) {
                // Convert string IDs to UUIDs
                List<UUID> userIds = request.getIdsList().stream()
                        .map(id -> {
                            try {
                                return UUID.fromString(id);
                            } catch (IllegalArgumentException e) {
                                logger.warn()
                                        .message("Invalid UUID format")
                                        .details(Map.of("id", id))
                                        .log();
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                // Batch fetch users
                List<User> users = userRepository.findByIdInAndTenantId(userIds, tenantId);

                results.addAll(users.stream()
                        .map(this::mapUserToUserIdentityInfo)
                        .collect(Collectors.toList()));
            }

            GetUserIdentitiesByIdsResponse response = GetUserIdentitiesByIdsResponse.newBuilder()
                    .addAllItems(results)
                    .build();

            logger.info()
                    .message("Successfully processed getUserIdentitiesByIds")
                    .details(Map.of("foundCount", results.size()))
                    .log();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error()
                    .message("Error processing getUserIdentitiesByIds request")
                    .details(Map.of("error", e.getMessage()))
                    .log();

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error while fetching by IDs: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    private UserIdentityInfo mapUserToUserIdentityInfo(User user) {
        return UserIdentityInfo.newBuilder()
                .setId(user.getId().toString())
                .setName(user.getUsername())
                .setType("USER")
                .setEmail(user.getEmail() != null ? user.getEmail() : "")
                .setIsActive(user.getStatus() == com.aidentx.authservice.enums.UserStatus.ACTIVE)
                .setTenantId(user.getTenantId())
                .setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toEpochMilli() : 0)
                .build();
    }
}
