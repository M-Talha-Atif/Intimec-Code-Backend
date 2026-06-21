package com.aidentx.applicationpermissions.shared.grpc.client;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.auth.grpc.UserGrpcServiceGrpc;
import com.aidentx.auth.grpc.GetUsersByTenantRequest;
import com.aidentx.auth.grpc.GetUsersByTenantResponse;
import com.aidentx.auth.grpc.UserInfo;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Client Service for user operations.
 * Provides methods to call the auth-service via gRPC for user-related operations.
 */
@Service
public class UserGrpcClient {

    private final IUnifiedLogger logger;

    @GrpcClient("auth-service")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userServiceStub;

    public UserGrpcClient(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(AppConstants.USER_GRPC_CLIENT_LOGGER_NAME, AppConstants.LOG_CATEGORY_USER_GRPC_CLIENT);
    }

    /**
     * Retrieves all users belonging to a specific tenant via gRPC call to auth-service.
     * 
     * @param tenantId the tenant ID to filter users
     * @return GetUsersByTenantResponse containing the list of users and metadata
     * @throws RuntimeException if the gRPC call fails
     */
    public GetUsersByTenantResponse getUsersByTenant(String tenantId) {
        logger.info()
            .message(AppConstants.LOG_INITIATING_GRPC_CALL_RETRIEVE_USERS)
            .details(Map.of(AppConstants.DETAIL_KEY_TENANT_ID, tenantId))
            .log();

        try {
            // Build the request
            GetUsersByTenantRequest request = GetUsersByTenantRequest.newBuilder()
                .setTenantId(tenantId)
                .build();

            logger.debug()
                .message(AppConstants.LOG_SENDING_GRPC_REQUEST_TO_AUTH_SERVICE)
                .details(Map.of(
                    AppConstants.DETAIL_KEY_SERVICE, AppConstants.VALUE_USER_GRPC_SERVICE,
                    AppConstants.DETAIL_KEY_METHOD, AppConstants.VALUE_GET_USERS_BY_TENANT_METHOD,
                    AppConstants.DETAIL_KEY_TENANT_ID, tenantId
                ))
                .log();

            // Make the gRPC call with timeout
            GetUsersByTenantResponse response = userServiceStub
                .withDeadlineAfter(30, TimeUnit.SECONDS)
                .getUsersByTenant(request);

            logger.info()
                .message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_USERS_FROM_AUTH_SERVICE)
                .details(Map.of(
                    AppConstants.DETAIL_KEY_TENANT_ID, tenantId,
                    AppConstants.DETAIL_KEY_USER_COUNT, response.getTotalCount(),
                    AppConstants.DETAIL_KEY_RESPONSE_TIME, AppConstants.VALUE_SUCCESS
                ))
                .log();

            return response;

        } catch (StatusRuntimeException e) {
            logger.error(e)
                .message(AppConstants.LOG_GRPC_CALL_FAILED_WITH_STATUS_ERROR)
                .details(Map.of(
                    AppConstants.DETAIL_KEY_TENANT_ID, tenantId,
                    AppConstants.DETAIL_KEY_STATUS, e.getStatus().getCode().toString(),
                    AppConstants.DETAIL_KEY_DESCRIPTION, e.getStatus().getDescription() != null ? e.getStatus().getDescription() : AppConstants.VALUE_NO_DESCRIPTION
                ))
                .log();

            throw new RuntimeException(AppConstants.ERROR_FAILED_TO_RETRIEVE_USERS_FROM_AUTH_SERVICE + ": " + e.getStatus().getDescription(), e);

        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_UNEXPECTED_ERROR_DURING_GRPC_CALL)
                .details(Map.of(
                    AppConstants.DETAIL_KEY_TENANT_ID, tenantId,
                    AppConstants.DETAIL_KEY_ERROR, e.getMessage()
                ))
                .log();

            throw new RuntimeException(AppConstants.ERROR_UNEXPECTED_ERROR_WHILE_RETRIEVING_USERS + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves users by tenant and returns them as a simple list for easier consumption.
     * 
     * @param tenantId the tenant ID to filter users
     * @return List of UserInfo objects
     * @throws RuntimeException if the gRPC call fails
     */
    public List<UserInfo> getUserListByTenant(String tenantId) {
        logger.debug()
            .message(AppConstants.LOG_RETRIEVING_USER_LIST_BY_TENANT)
            .details(Map.of(AppConstants.DETAIL_KEY_TENANT_ID, tenantId))
            .log();

        try {
            GetUsersByTenantResponse response = getUsersByTenant(tenantId);
            List<UserInfo> users = response.getUsersList();

            logger.debug()
                .message(AppConstants.LOG_CONVERTED_GRPC_RESPONSE_TO_USER_LIST)
                .details(Map.of(
                    AppConstants.DETAIL_KEY_TENANT_ID, tenantId,
                    AppConstants.DETAIL_KEY_USER_COUNT, users.size()
                ))
                .log();

            return users;

        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ERROR_CONVERTING_GRPC_RESPONSE_TO_USER_LIST)
                .details(Map.of(AppConstants.DETAIL_KEY_TENANT_ID, tenantId))
                .log();

            throw e; // Re-throw the original exception
        }
    }

    /**
     * Health check method to test connectivity to auth-service.
     * This is useful for monitoring and debugging connectivity issues.
     * 
     * @return true if the service is reachable, false otherwise
     */
    public boolean isAuthServiceReachable() {
        try {
            logger.debug()
                .message(AppConstants.LOG_TESTING_CONNECTIVITY_TO_AUTH_SERVICE)
                .log();

            // Try to make a call with an empty tenant ID to test connectivity
            // The service might return an error, but if we get a response, it's reachable
            getUsersByTenant("");
            return true;

        } catch (StatusRuntimeException e) {
            // If we get a status error, the service is reachable but might have validation issues
            if (e.getStatus().getCode() == io.grpc.Status.Code.INVALID_ARGUMENT) {
                logger.debug()
                    .message(AppConstants.LOG_AUTH_SERVICE_REACHABLE_VALIDATION_ERROR)
                    .log();
                return true;
            }

            logger.warn()
                .message(AppConstants.LOG_AUTH_SERVICE_CONNECTIVITY_TEST_FAILED)
                .details(Map.of(AppConstants.DETAIL_KEY_STATUS, e.getStatus().getCode().toString()))
                .log();
            return false;

        } catch (Exception e) {
            logger.warn()
                .message(AppConstants.LOG_AUTH_SERVICE_CONNECTIVITY_TEST_FAILED)
                .details(Map.of(AppConstants.DETAIL_KEY_ERROR, e.getMessage()))
                .log();
            return false;
        }
    }
}