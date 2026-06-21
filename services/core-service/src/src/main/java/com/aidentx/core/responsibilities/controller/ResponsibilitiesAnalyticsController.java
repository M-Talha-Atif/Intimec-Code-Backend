package com.aidentx.core.responsibilities.controller;

import com.aidentx.core.responsibilities.config.constants.analytics.ResponsibilitiesAnalyticsConstants;
import com.aidentx.core.responsibilities.dto.ResponsibilitiesKpisResponse;
import com.aidentx.core.responsibilities.service.ResponsibilitiesAnalyticsService;
import com.aidentx.core.shared.security.TenantContextService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for Responsibilities Analytics endpoints.
 *
 * <p>Separate from {@link ResponsibilitiesController} to keep analytics
 * endpoints isolated from the task and team management endpoints.
 * All endpoints are under /api/v1/responsibilities/analytics.
 *
 * @author aidentx Development Team
 * @version 1.0
 */
@RestController
@RequestMapping(ResponsibilitiesAnalyticsConstants.BASE_PATH)
public class ResponsibilitiesAnalyticsController {

    private final ResponsibilitiesAnalyticsService analyticsService;
    private final TenantContextService tenantContextService;
    private final IUnifiedLogger logger;

    public ResponsibilitiesAnalyticsController(
            ResponsibilitiesAnalyticsService analyticsService,
            TenantContextService tenantContextService,
            UnifiedLoggerFactory loggerFactory) {
        this.analyticsService     = analyticsService;
        this.tenantContextService = tenantContextService;
        this.logger = loggerFactory.getLogger(
                ResponsibilitiesAnalyticsController.class,
                "ResponsibilitiesAnalyticsController");
    }

    /**
     * Returns the three real KPI values for the Responsibilities Analytics Dashboard.
     *
     * <p>Aggregates data from workflows-service (task counts) and
     * application-permissions-service (owned roles counts) into a single response.
     * The frontend uses this to populate the Total Responsibilities, Pending Tasks,
     * and Owned Roles KPI cards without making three separate calls.
     *
     * <p>All other KPI cards on the dashboard (Overdue Approvals, Active Certifications,
     * Team Risk Exposure, SoD Violations, Unreviewed Items, Owned Applications)
     * remain computed on the frontend from data already fetched by the individual
     * existing endpoints.
     *
     * <p>Example:
     * <pre>
     *   GET /api/v1/responsibilities/analytics/kpis
     *   Authorization: Bearer eyJ...
     *
     *   Response:
     *   {
     *     "totalResponsibilities": 18,
     *     "pendingTasks":           4,
     *     "ownedRoles":             6,
     *     "ownedRolesTotalMembers": 84
     *   }
     * </pre>
     *
     * <p>Postman test:
     * <pre>
     *   GET http://localhost:8080/api/v1/responsibilities/analytics/kpis
     *   Header: Authorization: Bearer {token}
     * </pre>
     *
     * @param request HTTP request for JWT and Authorization header extraction
     * @return HTTP 200 with {@link ResponsibilitiesKpisResponse}
     */
    @GetMapping(ResponsibilitiesAnalyticsConstants.PATH_KPIS)
    public ResponseEntity<ResponsibilitiesKpisResponse> getKpis(
            HttpServletRequest request) {

        UUID   userId    = UUID.fromString(tenantContextService.getCurrentUserId());
        String tenantId  = tenantContextService.getCurrentTenantId();
        String authToken = request.getHeader(
                ResponsibilitiesAnalyticsConstants.HEADER_AUTHORIZATION);

        logger.info()
                .message(ResponsibilitiesAnalyticsConstants.LOG_KPI_REQUEST_RECEIVED)
                .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_USER_ID,  userId.toString())
                .detail(ResponsibilitiesAnalyticsConstants.LOG_KEY_TENANT_ID, tenantId)
                .log();

        return ResponseEntity.ok(
                analyticsService.computeKpis(userId, tenantId, authToken));
    }
}