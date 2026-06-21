package com.aidentx.core.responsibilities.service;

import com.aidentx.core.responsibilities.dto.ResponsibilitiesKpisResponse;

import java.util.UUID;

public interface ResponsibilitiesAnalyticsService {

    /**
     * Computes the three real KPI values for the Responsibilities Analytics Dashboard.
     *
     * <p>Aggregates:
     * <ul>
     *   <li>workflows-service    -- pending + processed task counts</li>
     *   <li>application-permissions-service -- owned roles count + member totals</li>
     * </ul>
     *
     * <p>Formula matching the frontend:
     * <pre>
     *   totalResponsibilities = allTasksCount + ownedRoles
     *   allTasksCount         = pendingCount + processedCount
     *   pendingTasks          = pendingCount
     *   ownedRoles            = owned roles list size
     *   ownedRolesTotalMembers = sum of userCount across owned roles
     * </pre>
     *
     * @param userId    current user UUID from JWT
     * @param tenantId  current tenant ID from JWT
     * @param authToken JWT Authorization header value for service-to-service calls
     * @return populated KPI response; individual fields default to 0 on downstream failure
     */
    ResponsibilitiesKpisResponse computeKpis(UUID userId, String tenantId, String authToken);
}