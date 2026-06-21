package com.aidentx.taskmanagement.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.aidentx.common.protos.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.config.TaskManagementConstants;
import com.aidentx.taskmanagement.model.dto.UserReference;

import io.grpc.StatusRuntimeException;

/**
 * Service for fetching user information from the identity-service via gRPC. Provides resilient
 * methods to resolve user IDs to names, with fallback mechanisms.
 */
@Service
public class UserIdentityService {

  private final UserIdentityServiceGrpc.UserIdentityServiceBlockingStub userIdentityStub;
  private final IUnifiedLogger logger;

  public UserIdentityService(
      UserIdentityServiceGrpc.UserIdentityServiceBlockingStub userIdentityStub,
      UnifiedLoggerFactory loggerFactory) {
    this.userIdentityStub = userIdentityStub;
    this.logger = loggerFactory.getLogger(UserIdentityService.class);
  }

  /**
   * Fetches a single user by ID.
   *
   * @param tenantId The tenant identifier.
   * @param id The user UUID.
   * @return A UserReference with name and ID, or a fallback reference if not found or on error.
   */
  public UserReference getUserById(String tenantId, UUID id) {
    if (id == null) {
      return null;
    }
    Map<String, UserReference> users = getUsersByIdsMap(tenantId, Collections.singletonList(id));
    // Return the resolved user or a fallback if it wasn't found
    return users.getOrDefault(id.toString(), new UserReference(id.toString(), id.toString()));
  }

  /**
   * Fetches a list of users by their IDs.
   *
   * @param tenantId The tenant identifier.
   * @param ids A list of user UUIDs.
   * @return A list of UserReferences. Users not found will have their ID as their name.
   */
  public List<UserReference> getUsersByIds(String tenantId, List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String, UserReference> userMap = getUsersByIdsMap(tenantId, ids);
    // Map each requested ID to its resolved reference or a fallback
    return ids.stream()
        .map(
            id ->
                userMap.getOrDefault(
                    id.toString(), new UserReference(id.toString(), id.toString())))
        .collect(Collectors.toList());
  }

  /** Internal method to fetch users and return a map for efficient lookups. */
  private Map<String, UserReference> getUsersByIdsMap(String tenantId, List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return Collections.emptyMap();
    }
    try {
      List<String> idStrings = ids.stream().map(UUID::toString).collect(Collectors.toList());
      GetUserIdentitiesByIdsRequest request =
          GetUserIdentitiesByIdsRequest.newBuilder()
              .setTenantId(tenantId)
              .addAllIds(idStrings)
              .build();

      GetUserIdentitiesByIdsResponse response = userIdentityStub.getUserIdentitiesByIds(request);
      return response.getItemsList().stream()
          .collect(
              Collectors.toMap(
                  UserIdentityInfo::getId,
                  info -> new UserReference(info.getId(), info.getName())));
    } catch (StatusRuntimeException e) {
      logger
          .warn()
          .message(TaskManagementConstants.MSG_GRPC_ERROR_FETCHING_USERS)
          .detail("status", e.getStatus())
          .log();
      return Collections.emptyMap(); // Return empty map, caller will handle fallbacks
    } catch (Exception e) {
      logger
          .warn()
          .message(TaskManagementConstants.MSG_UNEXPECTED_ERROR_FETCHING_USERS)
          .failureReason(e.getLocalizedMessage())
          .log();
      return Collections.emptyMap();
    }
  }
}
