// File: UserManagerLookupGrpcClient.java
package com.aidentx.access_portal_service.client.grpc;

import com.aidentx.iam.grpc.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * gRPC client for transforming user-identity pairs to user-manager pairs.
 *
 * SERVICE PURPOSE:
 * This client bridges the gap between identity service and manager lookup.
 * It takes the output from UserIdentityLookupService (userId->identityId)
 * and transforms it into userId->managerId for workflow approvals.
 *
 * HOW IT WORKS:
 * 1. Auth service returns: {"user1": "identity123", "user2": "identity456"}
 * 2. This client transforms to: {"user1": "manager789", "user2": "manager999"}
 * 3. If no manager found for an identity, returns empty string ""
 *
 * USAGE SCENARIO:
 * - After fetching identity IDs from UserIdentitiesGrpcClient
 * - Before starting workflow approvals (need manager for approval chain)
 * - Batch processing multiple users efficiently
 *
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
public class UserManagerLookupGrpcClient {

    // Logger category constants
    private static final String LOG_CATEGORY_MANAGER_LOOKUP = "MANAGER_LOOKUP_SERVICE";
    private static final String LOG_TRANSFORMING_TO_MANAGERS = "Transforming user-identity to user-manager mappings";
    private static final String LOG_SUCCESS_TRANSFORMED_MANAGERS = "Successfully transformed to manager mappings";
    private static final String LOG_GRPC_ERROR_TRANSFORMING_MANAGERS = "gRPC error transforming to manager mappings";
    private static final String LOG_UNEXPECTED_ERROR_TRANSFORMING = "Unexpected error transforming to manager mappings";
    private static final String LOG_MANAGER_LOOKUP_HEALTH_CHECK_FAILED = "Manager lookup service health check failed";

    private final IUnifiedLogger logger;

    /**
     * gRPC Stub injection for UserManagerLookupService
     * - "identity-service" refers to the gRPC server name configured in application.yml
     * - This stub is automatically created by Spring Boot gRPC starter
     * - BlockingStub: synchronous call, thread waits for response
     */
    @GrpcClient("identity-service")
    private UserManagerLookupServiceGrpc.UserManagerLookupServiceBlockingStub managerLookupStub;

    public UserManagerLookupGrpcClient(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(UserManagerLookupGrpcClient.class, LOG_CATEGORY_MANAGER_LOOKUP);
    }

    /**
     * Transforms user-identity mappings to user-manager mappings.
     * This is the core method that matches the gRPC service contract.
     *
     * INPUT FORMAT:
     * Map<String, String> where:
     *   - Key: userId (UUID as string)
     *   - Value: identityId (UUID as string, may be empty if not found)
     *
     * OUTPUT FORMAT:
     * Map<String, String> where:
     *   - Key: userId (same as input)
     *   - Value: managerId (UUID as string, empty if no manager)
     *
     * HOW THE TRANSFORMATION HAPPENS:
     * 1. Auth service provides: user -> identity
     * 2. This service looks up: identity -> manager
     * 3. Result: user -> manager (through identity)
     *
     * Example transformation:
     * Input:  {"user1": "identity123", "user2": "identity456"}
     * Output: {"user1": "manager789", "user2": ""} (user2 has no manager)
     *
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param userIdentityMap Map of userId -> identityId (from UserIdentitiesGrpcClient)
     * @return Map of userId -> managerId (empty string if no manager found)
     * @throws IllegalArgumentException if tenantId is null/empty or map is null
     */
    public Map<String, String> transformToManagerMappings(
            String tenantId,
            Map<String, String> userIdentityMap) {

        // Validation
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }

        if (userIdentityMap == null) {
            throw new IllegalArgumentException("User-identity map cannot be null");
        }

        if (userIdentityMap.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // Step 1: Convert Map to gRPC UserIdentityMapping list
            List<UserIdentityMapping> mappings = buildUserIdentityMappings(userIdentityMap);

            // Step 2: Build gRPC request
            // The proto expects UserIdentityMapping objects, not a simple Map
            GetManagerIdsByUserIdentityRequest request = GetManagerIdsByUserIdentityRequest.newBuilder()
                    .setTenantId(tenantId)
                    .addAllMappings(mappings)  // Critical: addAllMappings not addAllUserIds
                    .build();

            logger.debug()
                    .message(LOG_TRANSFORMING_TO_MANAGERS)
                    .category(LOG_CATEGORY_MANAGER_LOOKUP)
                    .detail("tenantId", tenantId)
                    .detail("userCount", userIdentityMap.size())
                    .detail("mappingsWithIdentity",
                            userIdentityMap.values().stream().filter(id -> !id.isEmpty()).count())
                    .log();

            // Step 3: Execute gRPC call with timeout
            // The service processes each identity to find its manager
            GetManagerIdsByUserIdentityResponse response = managerLookupStub
                    .withDeadlineAfter(30, TimeUnit.SECONDS)
                    .getManagerIdsByUserIdentityPairs(request);

            // Step 4: Process response - response contains UserManagerMapping objects
            Map<String, String> result = new HashMap<>();
            for (UserManagerMapping mapping : response.getMappingsList()) {
                // mapping.getUserId() is same as input userId
                // mapping.getManagerId() may be empty if no manager
                result.put(mapping.getUserId(), mapping.getManagerId());
            }

            // Step 5: Log detailed results
            long managersFound = result.values().stream().filter(id -> !id.isEmpty()).count();
            logger.info()
                    .message(LOG_SUCCESS_TRANSFORMED_MANAGERS)
                    .category(LOG_CATEGORY_MANAGER_LOOKUP)
                    .detail("tenantId", tenantId)
                    .detail("totalUsers", userIdentityMap.size())
                    .detail("managersFound", managersFound)
                    .detail("usersWithoutManager", userIdentityMap.size() - managersFound)
                    .detail("serviceStats", String.format("Processed: %d, WithManager: %d",
                            response.getTotalProcessed(), response.getWithManager()))
                    .log();

            return result;

        } catch (StatusRuntimeException e) {
            // gRPC-specific exception handling
            logger.error()
                    .message(LOG_GRPC_ERROR_TRANSFORMING_MANAGERS)
                    .category(LOG_CATEGORY_MANAGER_LOOKUP)
                    .detail("tenantId", tenantId)
                    .detail("userCount", userIdentityMap.size())
                    .detail("statusCode", e.getStatus().getCode().name())
                    .detail("statusDescription", e.getStatus().getDescription())
                    .log();

            // Return fallback - all users get empty manager
            Map<String, String> fallbackResult = new HashMap<>();
            for (String userId : userIdentityMap.keySet()) {
                fallbackResult.put(userId, "");
            }
            return fallbackResult;

        } catch (Exception e) {
            // Catch any other unexpected exceptions
            logger.error()
                    .message(LOG_UNEXPECTED_ERROR_TRANSFORMING)
                    .category(LOG_CATEGORY_MANAGER_LOOKUP)
                    .detail("tenantId", tenantId)
                    .detail("userCount", userIdentityMap.size())
                    .detail("error", e.getMessage())
                    .log();

            // Return fallback
            Map<String, String> fallbackResult = new HashMap<>();
            for (String userId : userIdentityMap.keySet()) {
                fallbackResult.put(userId, "");
            }
            return fallbackResult;
        }
    }

    /**
     * Helper method to build UserIdentityMapping list from Map.
     * This converts the simple Map format to proto message format.
     *
     * @param userIdentityMap Map of userId -> identityId
     * @return List of UserIdentityMapping proto messages
     */
    private List<UserIdentityMapping> buildUserIdentityMappings(Map<String, String> userIdentityMap) {
        List<UserIdentityMapping> mappings = new ArrayList<>();

        for (Map.Entry<String, String> entry : userIdentityMap.entrySet()) {
            UserIdentityMapping mapping = UserIdentityMapping.newBuilder()
                    .setUserId(entry.getKey())
                    .setIdentityId(entry.getValue() != null ? entry.getValue() : "")
                    .build();
            mappings.add(mapping);
        }

        return mappings;
    }

    /**
     * Convenience method: Transform for single user.
     *
     * @param tenantId Tenant ID
     * @param userId User ID
     * @param identityId Identity ID (may be empty)
     * @return Manager ID or empty string
     */
    public String getManagerForUser(String tenantId, String userId, String identityId) {
        Map<String, String> singleMapping = new HashMap<>();
        singleMapping.put(userId, identityId != null ? identityId : "");

        Map<String, String> result = transformToManagerMappings(tenantId, singleMapping);
        return result.getOrDefault(userId, "");
    }

    /**
     * Batch transformation with identity map and user list.
     * Filters the identity map to only include requested users.
     *
     * @param tenantId Tenant ID
     * @param userIds List of user IDs to process
     * @param fullIdentityMap Complete identity map (from UserIdentitiesGrpcClient)
     * @return Map of userId -> managerId only for requested users
     */
    public Map<String, String> getManagersForUsers(
            String tenantId,
            List<String> userIds,
            Map<String, String> fullIdentityMap) {

        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        // Filter identity map to only include requested users
        Map<String, String> filteredMap = new HashMap<>();
        for (String userId : userIds) {
            String identityId = fullIdentityMap.getOrDefault(userId, "");
            filteredMap.put(userId, identityId);
        }

        return transformToManagerMappings(tenantId, filteredMap);
    }

    /**
     * Combined operation: Get identities and transform to managers in one flow.
     * This is the most common use case for access portal.
     *
     * FLOW:
     * 1. Call identity service to get userId->identityId
     * 2. Call manager lookup to transform to userId->managerId
     * 3. Return final manager mappings
     *
     * @param tenantId Tenant ID
     * @param userIds List of user IDs
     * @param identityClient Pre-injected UserIdentitiesGrpcClient
     * @return Map of userId -> managerId
     */
    public Map<String, String> getIdentityAndManagerMappings(
            String tenantId,
            List<String> userIds,
            UserIdentitiesGrpcClient identityClient) {

        // Step 1: Get identity mappings
        Map<String, String> identityMap = identityClient.getIdentityIdsByUserIds(tenantId, userIds);

        // Step 2: Transform to manager mappings
        return transformToManagerMappings(tenantId, identityMap);
    }

    /**
     * Health check method to verify manager lookup service connectivity.
     *
     * @return true if service is reachable, false otherwise
     */
    public boolean isManagerLookupServiceReachable() {
        try {
            // Create minimal request with test data
            UserIdentityMapping testMapping = UserIdentityMapping.newBuilder()
                    .setUserId("test-user-id")
                    .setIdentityId("test-identity-id")
                    .build();

            GetManagerIdsByUserIdentityRequest request = GetManagerIdsByUserIdentityRequest.newBuilder()
                    .setTenantId("health-check")
                    .addMappings(testMapping)
                    .build();

            // Try call with short timeout
            managerLookupStub
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .getManagerIdsByUserIdentityPairs(request);

            return true;
        } catch (Exception e) {
            logger.warn()
                    .message(LOG_MANAGER_LOOKUP_HEALTH_CHECK_FAILED)
                    .category(LOG_CATEGORY_MANAGER_LOOKUP)
                    .detail("error", e.getMessage())
                    .log();
            return false;
        }
    }
}