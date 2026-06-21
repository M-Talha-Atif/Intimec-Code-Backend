package com.aidentx.workflowsservice.client;

/*
 * TEMPORARILY DISABLED: Auth service protos are not in common-protos yet.
 * This client will be re-enabled once com.aidentx.auth.grpc protos are added to common-protos.
 * 
 * For now, user lookups are not critical for Phase 5 (identity provisioning).
 * Future phases will need this for approval workflow user validation.
 * 
 * TODO: Add auth-service user.proto to common-protos and re-enable this client
 */

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * gRPC Client for Auth Service
 * Handles user authentication and user data retrieval
 * 
 * @deprecated TEMPORARILY DISABLED until auth service protos are added to common-protos
 */
@Service
@Deprecated
public class UserGrpcClient {
    
    private final IUnifiedLogger logger;
    
    public UserGrpcClient(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(UserGrpcClient.class, "grpc-client");
    }

    /**
     * Get all users by tenant ID
     * 
     * @deprecated Not implemented - auth protos not in common-protos
     */
    @Deprecated
    public List<Object> getUsersByTenant(String tenantId) {
        logger.warn()
            .message(LOG_USER_GRPC_DISABLED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .log();
        logger.warn()
            .message(LOG_RETURNING_EMPTY_LIST_TENANT)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("tenantId", tenantId)
            .log();
        return Collections.emptyList();
    }

    /**
     * Get user by ID (using getUsersByTenant and filtering)
     * 
     * @deprecated Not implemented - auth protos not in common-protos
     */
    @Deprecated
    public Object getUserById(String userId, String tenantId) {
        logger.warn()
            .message(LOG_USER_GET_BY_ID_DISABLED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .log();
        throw new UnsupportedOperationException("UserGrpcClient temporarily disabled - auth protos not in common-protos");
    }

    /**
     * Check if auth service is reachable
     * 
     * @return always returns false (service disabled)
     * @deprecated Not implemented - auth protos not in common-protos
     */
    @Deprecated
    public boolean isAuthServiceReachable() {
        logger.warn()
            .message(LOG_USER_REACHABLE_DISABLED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .log();
        return false;
    }
}
