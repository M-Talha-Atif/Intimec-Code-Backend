package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.config.WorkflowConstants;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.client.IdentityGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.aidentx.workflowsservice.config.ApprovalCacheConfig.*;
import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * ApproverResolutionService - Flexible approver resolution for multi-approval nodes
 * Phase 1 (Week 1-2) implementation with 5 resolution strategies
 *
 * Strategies:
 * - CONTEXT: Extract approver IDs from workflow context using configurable key
 * - STATIC: Use hardcoded list of approver IDs from node configuration
 * - ROLE: Query users by role names (e.g., FINANCE_APPROVER, SECURITY_OFFICER)
 * - HIERARCHY: Resolve manager from org chart via identity service
 * - COMPOSITE: Combine multiple strategies (union or intersection)
 *
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApproverResolutionService {

    private final IdentityGrpcClient identityClient;
    private final ApproverResolver approverResolver;

    /**
     * Main entry point: Resolve approver IDs using configured strategy
     *
     * @param approverResolution Configuration map from node data
     * @param context Workflow execution context
     * @param execution Workflow execution entity (for metadata)
     * @return List of approver UUIDs (as strings)
     * @throws IllegalArgumentException if strategy unknown or config invalid
     */
    public List<String> resolveApprovers(
            Map<String, Object> approverResolution,
            Map<String, Object> context,
            WorkflowExecution execution) {

        String strategy = (String) approverResolution.get(CONFIG_KEY_STRATEGY);
        log.info("{}: {} for execution {}",
            LOG_RESOLVING_APPROVERS, strategy, execution.getId());

        return switch (strategy) {
            case RESOLUTION_STRATEGY_CONTEXT -> resolveFromContext(approverResolution, context);
            case RESOLUTION_STRATEGY_STATIC -> resolveFromStatic(approverResolution);
            case RESOLUTION_STRATEGY_ROLE -> resolveFromRole(approverResolution, context);
            case RESOLUTION_STRATEGY_HIERARCHY -> resolveFromHierarchy(approverResolution, context);
            case RESOLUTION_STRATEGY_COMPOSITE -> resolveFromComposite(approverResolution, context, execution);
            default -> throw new IllegalArgumentException(ERROR_RESOLUTION_STRATEGY_UNKNOWN + strategy);
        };
    }

    /**
     * CONTEXT Strategy: Extract approver IDs from workflow context
     * Example: context.get("managerIds") or context.get("financeTeamIds")
     * Replaces hardcoded "ownerIds" limitation
     *
     * Config example:
     * {
     *   "strategy": "context",
     *   "contextKey": "managerIds",
     *   "limit": 5
     * }
     */
    private List<String> resolveFromContext(
            Map<String, Object> config,
            Map<String, Object> context) {

        String contextKey = (String) config.get(CONFIG_KEY_CONTEXT_KEY);
        if (contextKey == null || contextKey.isEmpty()) {
            throw new IllegalArgumentException(ERROR_CONTEXT_KEY_REQUIRED);
        }

        Object contextValue = context.get(contextKey);
        if (contextValue == null) {
            log.warn("{}: {}", ERROR_CONTEXT_KEY_NOT_FOUND, contextKey);
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<String> approverIds = (List<String>) contextValue;

        Integer limit = (Integer) config.get(CONFIG_KEY_LIMIT);
        if (limit != null && limit > 0 && approverIds.size() > limit) {
            log.info(LOG_APPROVERS_RESOLVED_LIMITED, approverIds.size(), limit);
            return approverIds.subList(0, limit);
        }

        log.info(LOG_APPROVERS_RESOLVED_FROM_CONTEXT, approverIds.size(), contextKey);
        return approverIds;
    }

    /**
     * STATIC Strategy: Use hardcoded list from node configuration
     * Backward compatible with existing static approver lists
     *
     * Config example:
     * {
     *   "strategy": "static",
     *   "approverIds": ["uuid1", "uuid2", "uuid3"]
     * }
     */
    private List<String> resolveFromStatic(Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        List<String> approverIds = (List<String>) config.get(CONFIG_KEY_APPROVER_IDS);

        if (approverIds == null || approverIds.isEmpty()) {
            throw new IllegalArgumentException(ERROR_STATIC_APPROVERS_EMPTY);
        }

        log.info(LOG_APPROVERS_RESOLVED_STATIC, approverIds.size());
        return approverIds;
    }

    /**
     * ROLE Strategy: Query users by role names
     * Example: All users with "FINANCE_APPROVER" or "SECURITY_OFFICER" role
     *
     * Config example:
     * {
     *   "strategy": "role",
     *   "roleNames": ["FINANCE_APPROVER", "SECURITY_OFFICER"],
     *   "department": "FINANCE",  // optional filter
     *   "limit": 10
     * }
     *
     * Note: This implementation uses the CONTEXT strategy as a fallback,
     * expecting role-based user IDs to be passed in context under a key
     * matching the role name. For full role-based resolution, the identity
     * service gRPC API needs to be extended with a findUsersByRoles method.
     *
     * Phase 5: Role resolution results are cached for 5 minutes to reduce
     * database/gRPC calls for frequently used roles.
     */
    @Cacheable(value = CACHE_APPROVERS_BY_ROLE,
               key = "#config.get('roleNames').toString()",
               condition = "#config.get('roleNames') != null")
    private List<String> resolveFromRole(
            Map<String, Object> config,
            Map<String, Object> context) {

        @SuppressWarnings("unchecked")
        List<String> roleNames = (List<String>) config.get(CONFIG_KEY_ROLE_NAMES);

        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException(ERROR_ROLE_NAMES_REQUIRED);
        }

        // TODO: When identity-service gRPC API supports findUsersByRoles, use:
        // List<UUID> userIds = identityClient.findUserIdsByRoles(roleNames, tenantId);

        // For now, look for role-based user IDs in context
        // Convention: context should contain keys like "FINANCE_APPROVER_users", "SECURITY_OFFICER_users"
        List<String> approverIds = new ArrayList<>();
        for (String roleName : roleNames) {
            String contextKey = roleName + "_users";
            Object usersForRole = context.get(contextKey);
            if (usersForRole instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> roleUserIds = (List<String>) usersForRole;
                approverIds.addAll(roleUserIds);
            }
        }

        // Deduplicate
        approverIds = approverIds.stream().distinct().collect(Collectors.toList());

        if (approverIds.isEmpty()) {
            log.warn("{}: {}", ERROR_NO_USERS_WITH_ROLES, roleNames);
            return Collections.emptyList();
        }

        // Apply optional limit
        Integer limit = (Integer) config.get(CONFIG_KEY_LIMIT);
        if (limit != null && limit > 0 && approverIds.size() > limit) {
            log.info(LOG_APPROVERS_RESOLVED_LIMITED, approverIds.size(), limit);
            return approverIds.subList(0, limit);
        }

        log.info(LOG_APPROVERS_RESOLVED_FROM_ROLES, approverIds.size(), roleNames);
        return approverIds;
    }

    /**
     * HIERARCHY Strategy: Resolve manager from org chart
     * Uses ApproverResolver to traverse hierarchy via identity-service
     *
     * Config example:
     * {
     *   "strategy": "hierarchy",
     *   "hierarchyType": "manager",       // manager, department_head
     *   "contextKey": "userId"            // who to find manager for (defaults to userId)
     * }
     *
     * Phase 5: Manager hierarchy lookups are cached for 10 minutes since
     * org structure changes infrequently. Cache key is the user ID.
     */
    @Cacheable(value = CACHE_USER_MANAGERS,
               key = "#context.get(#config.getOrDefault('contextKey', 'userId'))",
               condition = "#context != null")
    private List<String> resolveFromHierarchy(
            Map<String, Object> config,
            Map<String, Object> context) {

        String hierarchyType = (String) config.get(CONFIG_KEY_HIERARCHY_TYPE);
        if (hierarchyType == null || hierarchyType.isEmpty()) {
            hierarchyType = HIERARCHY_TYPE_MANAGER; // Default to manager
        }

        String contextKey = (String) config.get(CONFIG_KEY_CONTEXT_KEY);
        if (contextKey == null || contextKey.isEmpty()) {
            contextKey = CONTEXT_KEY_USER_ID; // Default to userId
        }

        Object userIdObj = context.get(contextKey);
        if (userIdObj == null) {
            log.warn("{}: {}", ERROR_CONTEXT_KEY_NOT_FOUND, contextKey);
            return Collections.emptyList();
        }

        // Prepare context for ApproverResolver
        Map<String, Object> resolverContext = Map.of(
            CONTEXT_KEY_USER_ID, userIdObj,
            CONTEXT_KEY_DEPARTMENT, context.getOrDefault(CONTEXT_KEY_DEPARTMENT, "")
        );

        try {
            // Map hierarchyType to ApproverResolver strategy
            String resolverStrategy = switch (hierarchyType) {
                case HIERARCHY_TYPE_MANAGER -> APPROVER_STRATEGY_MANAGER;
                case HIERARCHY_TYPE_DEPARTMENT_HEAD -> APPROVER_STRATEGY_DEPARTMENT_HEAD;
                default -> {
                    log.warn("{}: {}", ERROR_HIERARCHY_TYPE_UNKNOWN, hierarchyType);
                    yield APPROVER_STRATEGY_MANAGER; // Fallback
                }
            };

            UUID approverId = approverResolver.resolveApprover(resolverStrategy, resolverContext);
            log.info(LOG_APPROVERS_RESOLVED_FROM_HIERARCHY, 1, hierarchyType);
            return List.of(approverId.toString());

        } catch (Exception e) {
            log.error("{}: {}", ERROR_HIERARCHY_RESOLUTION_FAILED, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * COMPOSITE Strategy: Combine multiple strategies
     * Supports union (OR) or intersection (AND) operations
     *
     * Config example:
     * {
     *   "strategy": "composite",
     *   "operation": "union",  // or "intersection"
     *   "strategies": [
     *     {"strategy": "context", "contextKey": "managerIds"},
     *     {"strategy": "role", "roleNames": ["FINANCE_APPROVER"]}
     *   ],
     *   "limit": 10
     * }
     */
    private List<String> resolveFromComposite(
            Map<String, Object> config,
            Map<String, Object> context,
            WorkflowExecution execution) {

        String operation = (String) config.get(CONFIG_KEY_OPERATION);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> strategies =
            (List<Map<String, Object>>) config.get(CONFIG_KEY_STRATEGIES);

        if (strategies == null || strategies.isEmpty()) {
            throw new IllegalArgumentException(ERROR_COMPOSITE_STRATEGIES_REQUIRED);
        }

        // Resolve each strategy
        List<Set<String>> resolvedSets = strategies.stream()
                .map(strategyConfig -> new HashSet<>(resolveApprovers(strategyConfig, context, execution)))
                .collect(Collectors.toList());

        // Combine using operation
        Set<String> result;
        if (COMPOSITE_OPERATION_INTERSECTION.equals(operation)) {
            // Intersection: Keep only IDs present in ALL strategies
            result = resolvedSets.get(0);
            for (int i = 1; i < resolvedSets.size(); i++) {
                result.retainAll(resolvedSets.get(i));
            }
            log.info(LOG_APPROVERS_RESOLVED_COMPOSITE_INTERSECTION, result.size());
        } else {
            // Union (default): Combine all IDs from all strategies
            result = new HashSet<>();
            for (Set<String> set : resolvedSets) {
                result.addAll(set);
            }
            log.info(LOG_APPROVERS_RESOLVED_COMPOSITE_UNION, result.size());
        }

        List<String> approverIds = new ArrayList<>(result);

        // Apply optional limit
        Integer limit = (Integer) config.get(CONFIG_KEY_LIMIT);
        if (limit != null && limit > 0 && approverIds.size() > limit) {
            log.info(LOG_APPROVERS_RESOLVED_LIMITED, approverIds.size(), limit);
            return approverIds.subList(0, limit);
        }

        return approverIds;
    }
}