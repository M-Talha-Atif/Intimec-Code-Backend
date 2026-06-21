package com.aidentx.core.responsibilities.service.impl;

import com.aidentx.core.responsibilities.client.ApplicationPermissionsServiceClient;
import com.aidentx.core.responsibilities.client.WorkflowsServiceClient;
import com.aidentx.core.responsibilities.config.constants.analytics.ResponsibilitiesAnalyticsConstants;
import com.aidentx.core.responsibilities.dto.ResponsibilitiesKpisResponse;
import com.aidentx.core.responsibilities.dto.WorkflowApprovalDTO;
import com.aidentx.core.responsibilities.service.ResponsibilitiesAnalyticsService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link ResponsibilitiesAnalyticsService}.
 *
 * <p>Makes three downstream calls and aggregates results:
 * <ol>
 *   <li>workflows-service GET /api/v1/approvals/my-pending   -> pendingCount</li>
 *   <li>workflows-service GET /api/v1/approvals/my-history   -> processedCount</li>
 *   <li>application-permissions-service GET /api/v1/roles/owned-by-me -> ownedRoles + members</li>
 * </ol>
 *
 * <p>Each call is individually guarded with try-catch so a single downstream
 * failure degrades gracefully -- the other KPI values still populate correctly.
 *
 * <p>The owned roles response is typed as {@code Object} in
 * {@link ApplicationPermissionsServiceClient} because the client was written
 * generically. {@link #extractRolesList(Object)} handles the two common response
 * shapes (direct list and wrapped map) at runtime.
 */
@Service
public class ResponsibilitiesAnalyticsServiceImpl
        implements ResponsibilitiesAnalyticsService {

    private final WorkflowsServiceClient workflowsClient;
    private final ApplicationPermissionsServiceClient applicationPermissionsClient;
    private final IUnifiedLogger logger;

    public ResponsibilitiesAnalyticsServiceImpl(
            WorkflowsServiceClient workflowsClient,
            ApplicationPermissionsServiceClient applicationPermissionsClient,
            UnifiedLoggerFactory loggerFactory) {
        this.workflowsClient              = workflowsClient;
        this.applicationPermissionsClient = applicationPermissionsClient;
        this.logger = loggerFactory.getLogger(
                ResponsibilitiesAnalyticsServiceImpl.class,
                "ResponsibilitiesAnalyticsService");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponsibilitiesKpisResponse computeKpis(
            UUID userId, String tenantId, String authToken) {

        logger.info()
                .message(ResponsibilitiesAnalyticsConstants.LOG_COMPUTING_KPIS)
                .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_USER_ID,  userId.toString())
                .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_TENANT_ID, tenantId)
                .log();

        int pendingCount   = fetchPendingTaskCount(userId, tenantId, authToken);
        int processedCount = fetchProcessedTaskCount(userId, tenantId, authToken);
        int allTasksCount  = pendingCount + processedCount;

        int[] ownedRolesCounts     = fetchOwnedRolesCounts(userId, tenantId, authToken);
        int   ownedRoles           = ownedRolesCounts[0];
        int   ownedRolesTotalMembers = ownedRolesCounts[1];

        // Matches frontend: tasks.length + internalRoles.length
        int totalResponsibilities = allTasksCount + ownedRoles;

        logger.info()
                .message(ResponsibilitiesAnalyticsConstants.LOG_KPIS_COMPUTED)
                .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_TOTAL_RESPONSIBILITIES, totalResponsibilities)
                .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_PENDING_TASKS,          pendingCount)
                .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_OWNED_ROLES,            ownedRoles)
                .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_OWNED_ROLES_MEMBERS,    ownedRolesTotalMembers)
                .log();

        return new ResponsibilitiesKpisResponse(
                totalResponsibilities,
                pendingCount,
                ownedRoles,
                ownedRolesTotalMembers);
    }

    /**
     * Fetches pending approval count from workflows-service.
     *
     * <p>Calls {@link WorkflowsServiceClient#getPendingApprovals} which maps to
     * GET /api/v1/approvals/my-pending on workflows-service.
     * Returns 0 when the call fails so remaining KPIs are unaffected.
     *
     * @param userId    current user UUID
     * @param tenantId  current tenant ID
     * @param authToken JWT header value
     * @return pending task count; 0 on error
     */
    private int fetchPendingTaskCount(UUID userId, String tenantId, String authToken) {
        try {
            List<WorkflowApprovalDTO> pending =
                    workflowsClient.getPendingApprovals(userId, tenantId, authToken);
            int count = pending != null ? pending.size() : 0;

            logger.info()
                    .message(ResponsibilitiesAnalyticsConstants.LOG_PENDING_FETCHED)
                    .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_COUNT, count)
                    .log();

            return count;
        } catch (Exception e) {
            logger.error(e)
                    .message(ResponsibilitiesAnalyticsConstants.LOG_PENDING_FAILED)
                    .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_USER_ID, userId.toString())
                    .log();
            return 0;
        }
    }

    /**
     * Fetches processed (historical) approval count from workflows-service.
     *
     * <p>Calls {@link WorkflowsServiceClient#getProcessedApprovals} with
     * decision=null to get all processed approvals regardless of outcome.
     * Returns 0 when the call fails.
     *
     * @param userId    current user UUID
     * @param tenantId  current tenant ID
     * @param authToken JWT header value
     * @return processed task count; 0 on error
     */
    private int fetchProcessedTaskCount(UUID userId, String tenantId, String authToken) {
        try {
            List<WorkflowApprovalDTO> processed =
                    workflowsClient.getProcessedApprovals(userId, tenantId, authToken, null);
            int count = processed != null ? processed.size() : 0;

            logger.info()
                    .message(ResponsibilitiesAnalyticsConstants.LOG_PROCESSED_FETCHED)
                    .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_COUNT, count)
                    .log();

            return count;
        } catch (Exception e) {
            logger.error(e)
                    .message(ResponsibilitiesAnalyticsConstants.LOG_PROCESSED_FAILED)
                    .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_USER_ID, userId.toString())
                    .log();
            return 0;
        }
    }

    /**
     * Fetches owned roles from application-permissions-service and computes
     * role count and total member count.
     *
     * <p>Calls {@link ApplicationPermissionsServiceClient#getOwnedRoles} which
     * maps to GET /api/v1/roles/owned-by-me on application-permissions-service.
     *
     * <p>The response is typed as {@code Object} because the client was written
     * generically. {@link #extractRolesList(Object)} inspects the shape at runtime
     * and handles both a direct list and a wrapped map response.
     *
     * <p>Member count is computed as sum of the
     * {@link ResponsibilitiesAnalyticsConstants#FIELD_USER_COUNT} field across
     * all role entries -- matching the frontend formula:
     * {@code internalRoles.reduce((s, r) => s + (r.userCount ?? 0), 0)}
     *
     * @param userId    current user UUID
     * @param tenantId  current tenant ID
     * @param authToken JWT header value
     * @return int[2]: [0] = role count, [1] = total member count; [0,0] on error
     */
    private int[] fetchOwnedRolesCounts(UUID userId, String tenantId, String authToken) {
        try {
            Object response = applicationPermissionsClient.getOwnedRoles(
                    userId, tenantId, authToken);

            if (response == null) {
                return new int[]{0, 0};
            }

            List<Map<String, Object>> roles = extractRolesList(response);

            int roleCount   = roles.size();
            int memberCount = roles.stream()
                    .mapToInt(role -> {
                        Object userCount =
                                role.get(ResponsibilitiesAnalyticsConstants.FIELD_USER_COUNT);
                        if (userCount == null)           return 0;
                        if (userCount instanceof Number) return ((Number) userCount).intValue();
                        try {
                            return Integer.parseInt(userCount.toString());
                        } catch (NumberFormatException ex) {
                            return 0;
                        }
                    })
                    .sum();

            logger.info()
                    .message(ResponsibilitiesAnalyticsConstants.LOG_OWNED_ROLES_FETCHED)
                    .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_ROLE_COUNT,    roleCount)
                    .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_TOTAL_MEMBERS, memberCount)
                    .log();

            return new int[]{roleCount, memberCount};

        } catch (Exception e) {
            logger.error(e)
                    .message(ResponsibilitiesAnalyticsConstants.LOG_OWNED_ROLES_FAILED)
                    .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_USER_ID, userId.toString())
                    .log();
            return new int[]{0, 0};
        }
    }

    /**
     * Extracts a list of role maps from the generic owned roles response.
     *
     * <p>Tries wrapper keys from
     * {@link ResponsibilitiesAnalyticsConstants#ROLES_WRAPPER_KEYS} in order
     * when the response is a Map. Returns the first value that is itself a List.
     *
     * <p>Handles two common response shapes from application-permissions-service:
     * <ul>
     *   <li>Direct list: {@code [ {role1}, {role2} ]}</li>
     *   <li>Wrapped map: {@code { "roles": [...] }} or {@code { "data": [...] }}
     *       or {@code { "content": [...] }} or {@code { "items": [...] }}</li>
     * </ul>
     *
     * <p>Returns an empty list when the shape is unrecognised so the caller
     * always receives a safe non-null value.
     *
     * @param response raw Object from
     *                 {@link ApplicationPermissionsServiceClient#getOwnedRoles}
     * @return list of role maps; empty on unrecognised shape
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRolesList(Object response) {
        if (response instanceof List) {
            return (List<Map<String, Object>>) response;
        }

        if (response instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) response;
            for (String key : ResponsibilitiesAnalyticsConstants.ROLES_WRAPPER_KEYS) {
                Object value = map.get(key);
                if (value instanceof List) {
                    return (List<Map<String, Object>>) value;
                }
            }
        }

        logger.warn()
                .message(ResponsibilitiesAnalyticsConstants.LOG_UNRECOGNISED_SHAPE)
                .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_RESPONSE_TYPE,
                        response.getClass().getSimpleName())
                .log();

        return Collections.emptyList();
    }
}