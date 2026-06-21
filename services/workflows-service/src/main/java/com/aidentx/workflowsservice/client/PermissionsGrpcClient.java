package com.aidentx.workflowsservice.client;

import com.aidentx.applicationpermissions.grpc.PermissionCheckRequest;
import com.aidentx.applicationpermissions.grpc.PermissionCheckResponse;
import com.aidentx.applicationpermissions.grpc.PermissionServiceGrpc;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * gRPC client for permission checking operations.
 * Wraps the PermissionService gRPC stub for convenient permission checks.
 *
 * @author aidentx Development Team
 * @version 1.0
 */
import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

@Service
public class PermissionsGrpcClient {

    private final IUnifiedLogger logger;

    @GrpcClient("application-permissions-service")
    private PermissionServiceGrpc.PermissionServiceBlockingStub permissionStub;

    public PermissionsGrpcClient(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(PermissionsGrpcClient.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Checks if a user has permission to access a specific endpoint.
     *
     * @param userId the user ID to check permissions for
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param apiEndpoint the API endpoint (e.g., "/api/v1/workflows")
     * @param httpMethod the HTTP method (e.g., "GET", "POST", "PUT", "DELETE")
     * @return true if user has permission, false otherwise
     */
    public boolean checkPermission(String userId, String tenantId, String apiEndpoint, String httpMethod) {
        try {
            PermissionCheckRequest request = PermissionCheckRequest.newBuilder()
                    .setUserId(userId)
                    .setTenantId(tenantId)
                    .setApiEndpoint(apiEndpoint)
                    .setHttpMethod(httpMethod)
                    .build();

            PermissionCheckResponse response = permissionStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .checkPermission(request);

            logger.trace()
                .message(LOG_PERMISSION_CHECK_RESULT)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("userId", userId)
                .detail("endpoint", apiEndpoint)
                .detail("method", httpMethod)
                .detail("hasPermission", response.getHasPermission())
                .log();

            return response.getHasPermission();

        } catch (StatusRuntimeException e) {
            logger.error()
                .message(LOG_GRPC_ERROR_CHECKING_PERMISSION)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("userId", userId)
                .detail("endpoint", apiEndpoint)
                .detail("method", httpMethod)
                .detail("status", e.getStatus().toString())
                .log();
            // On error, default to denying access for security
            return false;
        } catch (Exception e) {
            logger.error()
                .message(LOG_UNEXPECTED_ERROR_CHECKING_PERMISSION)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("userId", userId)
                .detail("endpoint", apiEndpoint)
                .detail("method", httpMethod)
                .detail("error", e.getMessage())
                .log();
            return false;
        }
    }

    /**
     * Checks if a user can execute workflows (POST /api/v1/workflows/execute).
     *
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return true if user can execute workflows
     */
    public boolean canExecuteWorkflows(String userId, String tenantId) {
        return checkPermission(userId, tenantId, "/api/v1/workflows/execute", "POST");
    }

    /**
     * Checks if a user can create workflow definitions (POST /api/v1/workflows).
     *
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return true if user can create workflows
     */
    public boolean canCreateWorkflows(String userId, String tenantId) {
        return checkPermission(userId, tenantId, "/api/v1/workflows", "POST");
    }

    /**
     * Checks if a user can approve workflow steps.
     *
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return true if user can approve workflows
     */
    public boolean canApproveWorkflows(String userId, String tenantId) {
        return checkPermission(userId, tenantId, "/api/v1/workflows/approvals", "PUT");
    }

    /**
     * Checks if a user can view workflow definitions (GET /api/v1/workflows).
     *
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return true if user can view workflows
     */
    public boolean canViewWorkflows(String userId, String tenantId) {
        return checkPermission(userId, tenantId, "/api/v1/workflows", "GET");
    }

    /**
     * Health check method to verify permissions service connectivity.
     *
     * @return true if service is reachable
     */
    public boolean isPermissionsServiceReachable() {
        try {
            // Try a dummy permission check with timeout
            permissionStub
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .checkPermission(PermissionCheckRequest.newBuilder()
                            .setUserId("health-check")
                            .setTenantId("default")
                            .setApiEndpoint("/health")
                            .setHttpMethod("GET")
                            .build());
            return true;
        } catch (Exception e) {
            logger.warn()
                .message(LOG_PERMISSIONS_HEALTH_CHECK_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
            return false;
        }
    }
}
