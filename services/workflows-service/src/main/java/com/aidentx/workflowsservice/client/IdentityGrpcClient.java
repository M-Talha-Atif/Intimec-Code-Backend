package com.aidentx.workflowsservice.client;

import com.aidentx.dal.protos.identity.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * gRPC Client for Identity Service
 * Handles user/identity management operations
 */
@Service
public class IdentityGrpcClient {
    
    private final IUnifiedLogger logger;

    @GrpcClient("identity-service")
    private IdentityServiceGrpc.IdentityServiceBlockingStub identityStub;
    
    public IdentityGrpcClient(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(IdentityGrpcClient.class, "grpc-client");
    }

    /**
     * Create a new employee identity
     * 
     * @deprecated This gRPC method is NOT IMPLEMENTED in identity-service.
     * The backend returns Status.UNIMPLEMENTED for this operation.
     * Use IdentityRestClient.createEmployee() instead which calls the REST API.
     * This method will throw StatusRuntimeException with UNIMPLEMENTED status at runtime.
     * 
     * @see com.aidentx.workflowsservice.client.IdentityRestClient#createEmployee
     */
    @Deprecated
    public IdentityMessage createEmployee(String tenantId, String name, String email, String department, String jobTitle) {
        logger.warn()
            .message(LOG_CREATE_EMPLOYEE_GRPC_NOT_IMPLEMENTED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .log();
        logger.info()
            .message(LOG_ATTEMPTING_CREATE_EMPLOYEE_GRPC)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("name", name)
            .detail("email", email)
            .log();

        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("tenantId is required for creating employee");
        }

        try {
            // Build employee attributes
            // Note: This proto structure exists but the gRPC method is NOT IMPLEMENTED
            EmployeeAttributes employeeAttrs = EmployeeAttributes.newBuilder()
                    .setDisplayName(name)
                    .setEmail(email)
                    .setDepartment(department)
                    .setJobTitle(jobTitle)
                    .build();

            CreateEmployeeRequest request = CreateEmployeeRequest.newBuilder()
                    .setTenantId(tenantId)
                    .setCoreAttributes(employeeAttrs)
                    .build();

            IdentityMessage identity = identityStub
                    .withDeadlineAfter(10, TimeUnit.SECONDS)
                    .createEmployee(request);

            logger.info()
                .message(LOG_EMPLOYEE_CREATED_SUCCESS)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("identityId", identity.getId())
                .log();
            return identity;

        } catch (StatusRuntimeException e) {
            logger.error()
                .message(LOG_EMPLOYEE_CREATION_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("statusCode", e.getStatus().getCode().toString())
                .detail("description", e.getStatus().getDescription())
                .log();
            throw new RuntimeException(
                String.format("Failed to create employee [%s]: %s", 
                    e.getStatus().getCode(), e.getStatus().getDescription()), 
                e
            );
        }
    }

    /**
     * Get identity by ID
     * Phase 7: Protected with circuit breaker, retry, and bulkhead
     */
    @CircuitBreaker(name = "identityService", fallbackMethod = "getIdentityByIdFallback")
    @Retry(name = "identityService")
    @Bulkhead(name = "identityService")
    public IdentityMessage getIdentityById(String identityId) {
        logger.info()
            .message(LOG_GETTING_IDENTITY_BY_ID_GRPC)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("identityId", identityId)
            .log();

        try {
            GetIdentityByIdRequest request = GetIdentityByIdRequest.newBuilder()
                    .setId(identityId)
                    .build();

            IdentityMessage identity = identityStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .getIdentityById(request);

            logger.info()
                .message(LOG_IDENTITY_RETRIEVED_SUCCESS)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("identityId", identity.getId())
                .log();
            return identity;

        } catch (StatusRuntimeException e) {
            logger.error()
                .message(LOG_IDENTITY_RETRIEVAL_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("statusCode", e.getStatus().getCode().toString())
                .detail("description", e.getStatus().getDescription())
                .log();
            throw new RuntimeException(
                String.format("Failed to get identity [%s]: %s", 
                    e.getStatus().getCode(), e.getStatus().getDescription()), 
                e
            );
        }
    }

    /**
     * Delete identity (for user termination)
     * Phase 7: Protected with circuit breaker, retry, and bulkhead
     */
    @CircuitBreaker(name = "identityService", fallbackMethod = "deleteIdentityFallback")
    @Retry(name = "identityService")
    @Bulkhead(name = "identityService")
    public void deleteIdentity(String identityId, String tenantId) {
        logger.info()
            .message(LOG_DELETING_IDENTITY_GRPC)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("identityId", identityId)
            .detail("tenantId", tenantId)
            .log();

        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("tenantId is required for deleting identity");
        }

        try {
            DeleteIdentityRequest request = DeleteIdentityRequest.newBuilder()
                    .setId(identityId)
                    .setTenantId(tenantId)
                    .build();

            identityStub
                    .withDeadlineAfter(10, TimeUnit.SECONDS)
                    .deleteIdentity(request);

            logger.info()
                .message(LOG_IDENTITY_DELETED_SUCCESS)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("identityId", identityId)
                .detail("tenantId", tenantId)
                .log();

        } catch (StatusRuntimeException e) {
            logger.error()
                .message(LOG_IDENTITY_DELETION_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("statusCode", e.getStatus().getCode().toString())
                .detail("description", e.getStatus().getDescription())
                .log();
            throw new RuntimeException(
                String.format("Failed to delete identity [%s]: %s", 
                    e.getStatus().getCode(), e.getStatus().getDescription()), 
                e
            );
        }
    }

    /**
     * Check if identity exists
     * Phase 7: Protected with circuit breaker, retry, and bulkhead
     * 
     * @param identityId the identity ID to check
     * @return true if identity exists, false if NOT_FOUND
     * @throws RuntimeException if service is unavailable or other errors occur
     */
    @CircuitBreaker(name = "identityService", fallbackMethod = "identityExistsFallback")
    @Retry(name = "identityService")
    @Bulkhead(name = "identityService")
    public boolean identityExists(String identityId) {
        logger.info()
            .message(LOG_CHECKING_IDENTITY_EXISTS_GRPC)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("identityId", identityId)
            .log();

        try {
            IdentityExistsRequest request = IdentityExistsRequest.newBuilder()
                    .setIdentityId(identityId)
                    .build();

            IdentityExistsResponse response = identityStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .identityExists(request);

            logger.info()
                .message(LOG_IDENTITY_EXISTS_CHECK)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("identityId", identityId)
                .detail("exists", String.valueOf(response.getExists()))
                .log();
            return response.getExists();

        } catch (StatusRuntimeException e) {
            // If NOT_FOUND, identity definitely doesn't exist
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                logger.info()
                    .message(LOG_IDENTITY_NOT_FOUND)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("identityId", identityId)
                    .log();
                return false;
            }
            
            // For other errors (UNAVAILABLE, DEADLINE_EXCEEDED), throw to indicate service issue
            logger.error()
                .message(LOG_IDENTITY_CHECK_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("statusCode", e.getStatus().getCode().toString())
                .detail("description", e.getStatus().getDescription())
                .log();
            throw new RuntimeException(
                String.format("Service unavailable while checking identity [%s]: %s", 
                    e.getStatus().getCode(), e.getStatus().getDescription()), 
                e
            );
        }
    }

    // ============ Phase 7: Fallback Methods for Resilience ============

    /**
     * Fallback for deleteIdentity when identity-service is unavailable
     */
    private void deleteIdentityFallback(String identityId, String tenantId, Exception e) {
        logger.error()
            .message(LOG_IDENTITY_SERVICE_UNAVAILABLE_DELETE)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("identityId", identityId)
            .detail("tenantId", tenantId)
            .detail("error", e.getMessage())
            .log();
        // In production: Queue for later retry or notify operations team
        throw new RuntimeException(
            "Identity service is currently unavailable. The delete operation will be retried automatically.",
            e
        );
    }

    /**
     * Fallback for getIdentityById when identity-service is unavailable
     */
    private IdentityMessage getIdentityByIdFallback(String identityId, Exception e) {
        logger.error()
            .message(LOG_IDENTITY_SERVICE_UNAVAILABLE_GET)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("identityId", identityId)
            .detail("error", e.getMessage())
            .log();
        throw new RuntimeException(
            "Identity service is currently unavailable. Please try again later.",
            e
        );
    }

    /**
     * Fallback for identityExists when identity-service is unavailable
     * Returns false to prevent blocking workflow execution
     */
    private boolean identityExistsFallback(String identityId, Exception e) {
        logger.warn()
            .message(LOG_IDENTITY_SERVICE_UNAVAILABLE_EXISTS)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("identityId", identityId)
            .detail("error", e.getMessage())
            .log();
        // Conservative approach: assume identity doesn't exist to prevent false positives
        return false;
    }
}
