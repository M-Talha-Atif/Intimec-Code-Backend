package com.aidentx.workflowsservice.service;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

import com.aidentx.workflowsservice.client.IdentityGrpcClient;
import com.aidentx.workflowsservice.exception.ResourceNotFoundException;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service for dynamically resolving workflow approvers.
 *
 * Supports multiple resolution strategies:
 * - manager: Resolve to the user's direct manager
 * - role: Resolve to first user with specified role
 * - department_head: Resolve to department head
 * - custom: Custom resolution logic (extensible)
 *
 * This enables workflows to automatically determine approvers based on
 * organizational structure rather than hardcoding approver IDs.
 *
 * Example Use Cases:
 * - Access request approval by user's manager
 * - Compliance certification by department head
 * - Security review by user with "Security Admin" role
 *
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
public class ApproverResolver {

    private final IUnifiedLogger logger;
    private final IdentityGrpcClient identityClient;

    public ApproverResolver(IdentityGrpcClient identityClient, UnifiedLoggerFactory loggerFactory) {
        this.identityClient = identityClient;
        this.logger = loggerFactory.getLogger(ApproverResolver.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Resolve approver ID based on strategy and context.
     *
     * @param strategy Resolution strategy (manager, role, department_head, custom)
     * @param context Execution context containing user info, department, etc.
     * @return UUID of the resolved approver
     * @throws IllegalArgumentException if strategy is unknown or required context is missing
     * @throws ResourceNotFoundException if no approver found for the strategy
     */
    public UUID resolveApprover(String strategy, Map<String, Object> context) {
        logger.info()
            .message(LOG_RESOLVING_APPROVER)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("strategy", strategy)
            .log();

        if (strategy == null || strategy.isBlank()) {
            throw new IllegalArgumentException(ERROR_APPROVER_STRATEGY_UNKNOWN + strategy);
        }

        UUID approverId = switch (strategy) {
            case APPROVER_STRATEGY_MANAGER -> resolveManager(context);
            case APPROVER_STRATEGY_ROLE -> resolveByRole(context);
            case APPROVER_STRATEGY_DEPARTMENT_HEAD -> resolveDepartmentHead(context);
            case APPROVER_STRATEGY_CUSTOM -> resolveCustom(context);
            default -> throw new IllegalArgumentException(ERROR_APPROVER_STRATEGY_UNKNOWN + strategy);
        };

        if (approverId == null) {
            throw new ResourceNotFoundException(ERROR_APPROVER_NOT_FOUND + strategy);
        }

        logger.info()
            .message(LOG_APPROVER_RESOLVED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("strategy", strategy)
            .detail("approverId", approverId.toString())
            .log();
        return approverId;
    }

    /**
     * Resolve to the user's direct manager.
     *
     * @param context Must contain "userId" key
     * @return Manager's UUID
     */
    private UUID resolveManager(Map<String, Object> context) {
        Object userIdObj = context.get(CONTEXT_KEY_USER_ID);

        if (userIdObj == null) {
            throw new IllegalArgumentException(ERROR_APPROVER_USER_ID_REQUIRED);
        }

        UUID userId = userIdObj instanceof UUID ? (UUID) userIdObj : UUID.fromString(userIdObj.toString());

        logger.info()
            .message(LOG_RESOLVING_MANAGER)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("userId", userId.toString())
            .log();

        // TODO: Phase 6b - Call identity-service to get user's manager
        // For now, return a mock manager ID
        // In production, this would be:
        // Identity identity = identityClient.getIdentity(userId);
        // UUID managerId = identity.getManagerId();

        UUID managerId = UUID.fromString(DEFAULT_USER_ID);

        logger.info()
            .message(LOG_MANAGER_RESOLVED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("userId", userId.toString())
            .detail("managerId", managerId.toString())
            .log();
        return managerId;
    }

    /**
     * Resolve to first user with the specified role.
     *
     * @param context Must contain "requiredApproverRole" key
     * @return UUID of user with that role
     */
    private UUID resolveByRole(Map<String, Object> context) {
        Object roleObj = context.get(CONTEXT_KEY_REQUIRED_APPROVER_ROLE);

        if (roleObj == null) {
            throw new IllegalArgumentException(ERROR_APPROVER_ROLE_REQUIRED);
        }

        String role = roleObj.toString();

        logger.info()
            .message(LOG_RESOLVING_BY_ROLE)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("role", role)
            .log();

        // TODO: Phase 6b - Call identity-service to find user with role
        // For now, return a mock approver
        // In production, this would be:
        // List<Identity> usersWithRole = identityClient.findByRole(role);
        // if (usersWithRole.isEmpty()) throw new ResourceNotFoundException(...)
        // UUID approverId = usersWithRole.get(0).getId();

        UUID approverId = UUID.fromString(DEFAULT_USER_ID);

        logger.info()
            .message(LOG_ROLE_APPROVER_RESOLVED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("role", role)
            .detail("approverId", approverId.toString())
            .log();
        return approverId;
    }

    /**
     * Resolve to the department head.
     *
     * @param context Must contain "department" key
     * @return Department head's UUID
     */
    private UUID resolveDepartmentHead(Map<String, Object> context) {
        Object deptObj = context.get(CONTEXT_KEY_DEPARTMENT);

        if (deptObj == null) {
            throw new IllegalArgumentException(ERROR_APPROVER_DEPARTMENT_REQUIRED);
        }

        String department = deptObj.toString();

        logger.info()
            .message(LOG_RESOLVING_DEPARTMENT_HEAD)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("department", department)
            .log();

        // TODO: Phase 6b - Call identity-service to find department head
        // For now, return a mock department head
        // In production, this would be:
        // Identity deptHead = identityClient.findDepartmentHead(department);
        // UUID headId = deptHead.getId();

        UUID headId = UUID.fromString(DEFAULT_USER_ID);

        logger.info()
            .message(LOG_DEPARTMENT_HEAD_RESOLVED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("department", department)
            .detail("headId", headId.toString())
            .log();
        return headId;
    }

    /**
     * Custom approver resolution logic.
     * Extensible for organization-specific requirements.
     *
     * @param context Execution context with custom fields
     * @return UUID of resolved approver
     */
    private UUID resolveCustom(Map<String, Object> context) {
        // TODO: Phase 6b - Implement custom resolution logic
        // This is a placeholder for organization-specific approver resolution
        // Examples:
        // - Approval by cost center owner
        // - Approval by application owner
        // - Escalation chain (try manager, fallback to dept head)

        logger.warn()
            .message(LOG_CUSTOM_APPROVER_NOT_IMPLEMENTED)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .log();
        return UUID.fromString(DEFAULT_USER_ID);
    }
}