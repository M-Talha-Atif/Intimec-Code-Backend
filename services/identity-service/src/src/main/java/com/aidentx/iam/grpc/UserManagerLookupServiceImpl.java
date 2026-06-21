package com.aidentx.iam.grpc;

import com.aidentx.iam.grpc.GetManagerIdsByUserIdentityRequest;
import com.aidentx.iam.grpc.GetManagerIdsByUserIdentityResponse;
import com.aidentx.iam.grpc.UserIdentityMapping;
import com.aidentx.iam.grpc.UserManagerLookupServiceGrpc;
import com.aidentx.iam.grpc.UserManagerMapping;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.repository.EmployeeRepository;
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
 * gRPC Service implementation for transforming user-identity pairs to user-manager pairs.
 * This service:
 * 1. Accepts userId-identityId pairs (output from auth service)
 * 2. Looks up manager for each identity ID
 * 3. Returns userId-managerId pairs (replaces identityId with managerId)
 *
 * Input format matches GetIdentityIdsResponse from UserIdentityLookupService
 * Output format provides direct user→manager mapping
 */
@GrpcService
@Transactional(readOnly = true)
public class UserManagerLookupServiceImpl
        extends UserManagerLookupServiceGrpc.UserManagerLookupServiceImplBase {

    private final EmployeeRepository employeeRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public UserManagerLookupServiceImpl(
            EmployeeRepository employeeRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.employeeRepository = employeeRepository;
        this.logger = loggerFactory.getLogger("UserManagerLookupService", "USER_MANAGER_LOOKUP_SERVICE");
    }

    /**
     * Transforms user-identity pairs to user-manager pairs.
     * Processes the exact output format from UserIdentityLookupService.
     *
     * @param request Contains tenant ID and user-identity mappings
     * @param responseObserver Stream observer for sending response
     */
    @Override
    public void getManagerIdsByUserIdentityPairs(
            GetManagerIdsByUserIdentityRequest request,
            StreamObserver<GetManagerIdsByUserIdentityResponse> responseObserver) {

        String tenantId = request.getTenantId();
        List<UserIdentityMapping> inputMappings = request.getMappingsList();

        logger.info()
                .message("gRPC request received: getManagerIdsByUserIdentityPairs")
                .details(Map.of(
                        "tenantId", tenantId,
                        "inputMappingsCount", inputMappings.size()
                ))
                .log();

        try {
            // Validate request
            validateRequest(tenantId, inputMappings);

            // Process mappings: transform identity IDs to manager IDs
            ProcessedResult result = processUserIdentityMappings(inputMappings, tenantId);

            // Build and send response
            GetManagerIdsByUserIdentityResponse response = buildResponse(result);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info()
                    .message("Successfully processed user-manager lookup request")
                    .details(Map.of(
                            "tenantId", tenantId,
                            "totalProcessed", result.totalProcessed,
                            "withManager", result.withManager,
                            "withoutManager", result.withoutManager
                    ))
                    .log();

        } catch (Exception e) {
            handleError(e, tenantId, inputMappings.size(), responseObserver);
        }
    }

    /**
     * Validates the incoming request parameters.
     * Ensures tenant ID is provided and input mappings are valid.
     *
     * @param tenantId Tenant ID for data isolation
     * @param inputMappings List of user-identity pairs to process
     * @throws IllegalArgumentException if validation fails
     */
    private void validateRequest(String tenantId, List<UserIdentityMapping> inputMappings) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID is required and cannot be empty");
        }

        if (inputMappings == null || inputMappings.isEmpty()) {
            throw new IllegalArgumentException("User-identity mappings list cannot be null or empty");
        }

        final int MAX_MAPPINGS = 1000;
        if (inputMappings.size() > MAX_MAPPINGS) {
            throw new IllegalArgumentException(
                    String.format("Maximum number of mappings exceeded. Max: %d, Received: %d",
                            MAX_MAPPINGS, inputMappings.size()));
        }
    }

    /**
     * Processes user-identity mappings to find manager IDs.
     * For each mapping:
     * 1. Extracts identity ID (if present and valid)
     * 2. Looks up manager for that identity
     * 3. Creates user-manager mapping
     *
     * @param inputMappings List of user-identity pairs from auth service
     * @param tenantId Tenant ID for data isolation
     * @return ProcessedResult containing all user-manager mappings
     */
    private ProcessedResult processUserIdentityMappings(
            List<UserIdentityMapping> inputMappings,
            String tenantId) {

        // Step 1: Extract valid identity IDs (non-empty UUIDs)
        List<UUID> validIdentityIds = new ArrayList<>();
        Map<UUID, String> identityToUserId = new HashMap<>();
        Map<String, String> userIdToOriginal = new HashMap<>();

        for (UserIdentityMapping mapping : inputMappings) {
            String userId = mapping.getUserId();
            String identityIdStr = mapping.getIdentityId();

            userIdToOriginal.put(userId, identityIdStr);

            // Only process if identity ID is not empty
            if (!identityIdStr.isEmpty()) {
                try {
                    UUID identityId = UUID.fromString(identityIdStr);
                    validIdentityIds.add(identityId);
                    identityToUserId.put(identityId, userId);
                } catch (IllegalArgumentException e) {
                    logger.warn()
                            .message("Invalid UUID format for identity ID")
                            .details(Map.of(
                                    "userId", userId,
                                    "identityId", identityIdStr
                            ))
                            .log();
                }
            }
        }

        logger.debug()
                .message("Extracted identity IDs for processing")
                .details(Map.of(
                        "totalInputMappings", inputMappings.size(),
                        "validIdentityIds", validIdentityIds.size()
                ))
                .log();

        // Step 2: Batch fetch employees with their managers
        Map<UUID, UUID> identityToManagerIdentityId = new HashMap<>();
        Set<UUID> managerIdentityIds = new HashSet<>();

        if (!validIdentityIds.isEmpty()) {
            List<Employee> employees = employeeRepository.findAllByIdWithManager(validIdentityIds);

            // Filter by tenant and active status, extract manager identity IDs
            for (Employee employee : employees) {
                if (tenantId.equals(employee.getTenantId()) &&
                        employee.getStatus() == com.aidentx.iam.identity_management.model.enums.IdentityStatus.ACTIVE) {

                    // Get manager identity ID if exists
                    UUID managerIdentityId = null;
                    Identity manager = employee.getManager();
                    if (manager != null &&
                            manager.getStatus() == com.aidentx.iam.identity_management.model.enums.IdentityStatus.ACTIVE) {
                        managerIdentityId = manager.getId();
                        managerIdentityIds.add(managerIdentityId);
                    }

                    identityToManagerIdentityId.put(employee.getId(), managerIdentityId);
                }
            }
        }

        // Step 3: Resolve manager identity IDs to user IDs
        Map<UUID, UUID> managerIdentityIdToUserId = new HashMap<>();
        if (!managerIdentityIds.isEmpty()) {
            List<Object[]> userIdMappings = employeeRepository.findUserIdsByIdentityIds(new ArrayList<>(managerIdentityIds));
            for (Object[] mapping : userIdMappings) {
                UUID identityId = (UUID) mapping[0];
                UUID userId = (UUID) mapping[1];
                managerIdentityIdToUserId.put(identityId, userId);
            }
        }

        logger.debug()
                .message("Fetched manager information and resolved to user IDs")
                .details(Map.of(
                        "validEmployeesFound", identityToManagerIdentityId.size(),
                        "employeesWithManager", identityToManagerIdentityId.values().stream()
                                .filter(Objects::nonNull)
                                .count(),
                        "managersResolvedToUserId", managerIdentityIdToUserId.size()
                ))
                .log();

        // Step 4: Build user-manager mappings for all input users (using manager's user_id)
        List<UserManagerMapping> outputMappings = new ArrayList<>();
        int withManager = 0;
        int withoutManager = 0;

        for (UserIdentityMapping inputMapping : inputMappings) {
            String userId = inputMapping.getUserId();
            String identityIdStr = inputMapping.getIdentityId();

            String managerUserId = "";

            // Only try to find manager if we have a valid identity ID
            if (!identityIdStr.isEmpty()) {
                try {
                    UUID identityId = UUID.fromString(identityIdStr);
                    UUID managerIdentityId = identityToManagerIdentityId.get(identityId);

                    if (managerIdentityId != null) {
                        // Resolve manager's identity_id to their user_id
                        UUID resolvedManagerUserId = managerIdentityIdToUserId.get(managerIdentityId);
                        if (resolvedManagerUserId != null) {
                            managerUserId = resolvedManagerUserId.toString();
                            withManager++;
                        } else {
                            // Manager identity found but no corresponding user account
                            logger.warn()
                                    .message("Manager has no user account")
                                    .details(Map.of(
                                            "userIdentityId", identityIdStr,
                                            "managerIdentityId", managerIdentityId.toString()
                                    ))
                                    .log();
                            withoutManager++;
                        }
                    } else {
                        // Identity found but no manager
                        withoutManager++;
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid identity ID format
                    withoutManager++;
                }
            } else {
                // Empty identity ID in input
                withoutManager++;
            }

            // Build output mapping (preserves same userId, but managerId is now user_id not identity_id)
            outputMappings.add(UserManagerMapping.newBuilder()
                    .setUserId(userId)
                    .setManagerId(managerUserId)
                    .build());
        }

        return new ProcessedResult(
                outputMappings,
                inputMappings.size(),
                withManager,
                withoutManager
        );
    }

    /**
     * Builds the gRPC response from processed results.
     * Maintains same order as input mappings.
     *
     * @param result Processed result containing all mappings
     * @return Formatted gRPC response
     */
    private GetManagerIdsByUserIdentityResponse buildResponse(ProcessedResult result) {
        return GetManagerIdsByUserIdentityResponse.newBuilder()
                .addAllMappings(result.outputMappings)
                .setTotalProcessed(result.totalProcessed)
                .setWithManager(result.withManager)
                .build();
    }

    /**
     * Handles errors and sends appropriate gRPC error response.
     * Differentiates between client errors (invalid arguments) and server errors.
     *
     * @param e Exception that occurred
     * @param tenantId Tenant ID for logging
     * @param mappingCount Number of mappings being processed
     * @param responseObserver Stream observer for error response
     */
    private void handleError(
            Exception e,
            String tenantId,
            int mappingCount,
            StreamObserver<GetManagerIdsByUserIdentityResponse> responseObserver) {

        logger.error(e)
                .message("Error processing user-manager lookup request")
                .details(Map.of(
                        "tenantId", tenantId,
                        "mappingCount", mappingCount,
                        "errorType", e.getClass().getSimpleName(),
                        "error", e.getMessage()
                ))
                .log();

        Status status;
        if (e instanceof IllegalArgumentException) {
            status = Status.INVALID_ARGUMENT.withDescription(e.getMessage());
        } else {
            status = Status.INTERNAL
                    .withDescription("Internal error while retrieving manager IDs: " + e.getMessage())
                    .withCause(e);
        }

        responseObserver.onError(status.asRuntimeException());
    }

    /**
     * Helper class to hold processed results.
     * Contains all output mappings with statistics.
     */
    private static class ProcessedResult {
        final List<UserManagerMapping> outputMappings;
        final int totalProcessed;
        final int withManager;
        final int withoutManager;

        ProcessedResult(List<UserManagerMapping> outputMappings,
                        int totalProcessed,
                        int withManager,
                        int withoutManager) {
            this.outputMappings = outputMappings;
            this.totalProcessed = totalProcessed;
            this.withManager = withManager;
            this.withoutManager = withoutManager;
        }
    }
}