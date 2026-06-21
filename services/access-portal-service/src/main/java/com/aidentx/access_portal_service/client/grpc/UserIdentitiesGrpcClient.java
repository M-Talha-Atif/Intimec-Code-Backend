// File: UserIdentitiesGrpcClient.java
package com.aidentx.access_portal_service.client.grpc;

import com.aidentx.auth.grpc.GetIdentityIdsRequest;
import com.aidentx.auth.grpc.GetIdentityIdsResponse;
import com.aidentx.auth.grpc.UserIdentityLookupServiceGrpc;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client for user identity lookup operations.
 * Wraps the UserIdentityLookupService gRPC stub for convenient identity resolution.
 *
 * USAGE:
 * 1. This client is automatically injected with the gRPC stub via @GrpcClient annotation
 * 2. The stub is created by Spring Boot gRPC starter based on application.yml configuration
 * 3. All calls automatically include user context via the GrpcClientConfig interceptor
 *
 * BLOCKING STUB EXPLANATION:
 * - BlockingStub: Synchronous call, waits for response (used here)
 * - FutureStub: Returns Future for async handling
 * - AsyncStub: Uses callbacks for async handling
 *
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
public class UserIdentitiesGrpcClient {

    // Logger category constants
    private static final String LOG_CATEGORY_IDENTITY_SERVICE = "IDENTITY_SERVICE";
    private static final String LOG_FETCHING_IDENTITY_IDS = "Fetching identity IDs via gRPC";
    private static final String LOG_SUCCESS_FETCHED_IDENTITIES = "Successfully fetched identity IDs";
    private static final String LOG_GRPC_ERROR_FETCHING_IDENTITIES = "gRPC error fetching identity IDs";
    private static final String LOG_UNEXPECTED_ERROR_FETCHING_IDENTITIES = "Unexpected error fetching identity IDs";
    private static final String LOG_IDENTITY_HEALTH_CHECK_FAILED = "Identity service health check failed";

    private final IUnifiedLogger logger;

    /**
     * gRPC Stub injection:
     * - "auth-service" refers to the gRPC server name configured in application.yml
     * - The stub is automatically created and managed by Spring Boot gRPC starter
     * - This is a BlockingStub: each call blocks until response is received or timeout
     */
    @GrpcClient("auth-service")
    private UserIdentityLookupServiceGrpc.UserIdentityLookupServiceBlockingStub identityStub;

    public UserIdentitiesGrpcClient(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(UserIdentitiesGrpcClient.class, LOG_CATEGORY_IDENTITY_SERVICE);
    }

    /**
     * Fetches identity IDs for multiple users in a single gRPC call.
     *
     * HOW IT WORKS:
     * 1. Builds gRPC request with tenantId and list of userIds
     * 2. Calls gRPC server with 5-second timeout
     * 3. Server queries database and returns identity mappings
     * 4. Client processes response into a Map for easy lookup
     *
     * RESPONSE FORMAT:
     * - Returns Map<userId, identityId>
     * - If identity not found for a user, value is empty string ""
     * - All requested userIds are guaranteed to be in the map (with empty string if not found)
     *
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param userIds List of user IDs (UUIDs as strings)
     * @return Map where key=userId, value=identityId (empty string if not found)
     * @throws IllegalArgumentException if tenantId is null/empty or userIds is null
     */
    public Map<String, String> getIdentityIdsByUserIds(String tenantId, List<String> userIds) {
        // Validation
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }

        if (userIds == null) {
            throw new IllegalArgumentException("User IDs list cannot be null");
        }

        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // Build gRPC request - proto messages are immutable, use builder pattern
            GetIdentityIdsRequest request = GetIdentityIdsRequest.newBuilder()
                    .setTenantId(tenantId)
                    .addAllUserIds(userIds)  // Adds all elements from the collection
                    .build();

            logger.debug()
                    .message(LOG_FETCHING_IDENTITY_IDS)
                    .category(LOG_CATEGORY_IDENTITY_SERVICE)
                    .detail("tenantId", tenantId)
                    .detail("userCount", userIds.size())
                    .detail("userIdsSample", userIds.size() > 5 ? userIds.subList(0, 5) + "..." : userIds)
                    .log();

            // Execute gRPC call with timeout
            // withDeadlineAfter() sets maximum wait time, throws DEADLINE_EXCEEDED if exceeded
            GetIdentityIdsResponse response = identityStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)  // 5 second timeout
                    .getIdentityIdsByUserIds(request);       // Actual gRPC call

            // Process response - gRPC returns List<UserIdentityMapping>
            Map<String, String> result = new HashMap<>();
            for (com.aidentx.auth.grpc.UserIdentityMapping mapping : response.getMappingsList()) {
                // mapping.getUserId() and mapping.getIdentityId() are strings from proto
                result.put(mapping.getUserId(), mapping.getIdentityId());
            }

            // Ensure all requested users are in the result map
            // This handles cases where user exists but has no identity ID
            for (String userId : userIds) {
                result.putIfAbsent(userId, "");
            }

            // Log success
            long foundCount = result.values().stream().filter(id -> !id.isEmpty()).count();
            logger.info()
                    .message(LOG_SUCCESS_FETCHED_IDENTITIES)
                    .category(LOG_CATEGORY_IDENTITY_SERVICE)
                    .detail("tenantId", tenantId)
                    .detail("totalRequested", userIds.size())
                    .detail("identitiesFound", foundCount)
                    .detail("identitiesMissing", userIds.size() - foundCount)
                    .log();

            return result;

        } catch (StatusRuntimeException e) {
            // gRPC-specific exception with status codes like DEADLINE_EXCEEDED, UNAVAILABLE, etc.
            logger.error()
                    .message(LOG_GRPC_ERROR_FETCHING_IDENTITIES)
                    .category(LOG_CATEGORY_IDENTITY_SERVICE)
                    .detail("tenantId", tenantId)
                    .detail("userCount", userIds.size())
                    .detail("statusCode", e.getStatus().getCode().name())
                    .detail("statusDescription", e.getStatus().getDescription())
                    .log();

            // Return empty map or throw - depending on your error handling strategy
            // For this use case, return empty map so parent logic can handle missing identities
            Map<String, String> fallbackResult = new HashMap<>();
            for (String userId : userIds) {
                fallbackResult.put(userId, "");
            }
            return fallbackResult;

        } catch (Exception e) {
            // Catch any other unexpected exceptions
            logger.error()
                    .message(LOG_UNEXPECTED_ERROR_FETCHING_IDENTITIES)
                    .category(LOG_CATEGORY_IDENTITY_SERVICE)
                    .detail("tenantId", tenantId)
                    .detail("userCount", userIds.size())
                    .detail("error", e.getMessage())
                    .log();

            // Return fallback result
            Map<String, String> fallbackResult = new HashMap<>();
            for (String userId : userIds) {
                fallbackResult.put(userId, "");
            }
            return fallbackResult;
        }
    }

    /**
     * Fetches identity ID for a single user.
     * Convenience wrapper around the batch method.
     *
     * @param tenantId Tenant ID
     * @param userId Single user ID
     * @return Identity ID as string, or empty string if not found
     */
    public String getIdentityIdByUserId(String tenantId, String userId) {
        Map<String, String> result = getIdentityIdsByUserIds(tenantId, List.of(userId));
        return result.getOrDefault(userId, "");
    }

    /**
     * Health check method to verify identity service connectivity.
     *
     * HOW IT WORKS:
     * 1. Sends a minimal request to the identity service
     * 2. If call succeeds, service is reachable
     * 3. If call fails or times out, service is unreachable
     *
     * @return true if identity service is reachable, false otherwise
     */
    public boolean isIdentityServiceReachable() {
        try {
            // Try a minimal request with short timeout
            identityStub
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .getIdentityIdsByUserIds(GetIdentityIdsRequest.newBuilder()
                            .setTenantId("health-check")
                            .addUserIds("health-check-user")
                            .build());
            return true;
        } catch (Exception e) {
            logger.warn()
                    .message(LOG_IDENTITY_HEALTH_CHECK_FAILED)
                    .category(LOG_CATEGORY_IDENTITY_SERVICE)
                    .detail("error", e.getMessage())
                    .log();
            return false;
        }
    }
}