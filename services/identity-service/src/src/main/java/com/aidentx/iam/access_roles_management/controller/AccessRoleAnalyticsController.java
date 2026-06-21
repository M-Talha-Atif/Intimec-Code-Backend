package com.aidentx.iam.access_roles_management.controller;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleIntelligenceConstants;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleBusinessUnitAnalyticsItemDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleIntelligenceKpisDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleTypeDistributionDto;
import com.aidentx.iam.access_roles_management.model.dto.response.ApiResponse;
import com.aidentx.iam.access_roles_management.service.analytics.AccessRoleIntelligenceAnalyticsService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Access Role Intelligence analytics endpoints.
 *
 * <p>Provides separate analytics APIs for the frontend Intelligence tab,
 * including KPI cards and chart datasets.</p>
 */
@RestController
@RequestMapping("/api/v1/access-roles/analytics" + AccessRoleIntelligenceConstants.INTELLIGENCE_BASE_PATH)
public class AccessRoleAnalyticsController {

    private final AccessRoleIntelligenceAnalyticsService accessRoleIntelligenceAnalyticsService;
    private final IUnifiedLogger logger;

    @Autowired
    public AccessRoleAnalyticsController(
            AccessRoleIntelligenceAnalyticsService accessRoleIntelligenceAnalyticsService,
            UnifiedLoggerFactory unifiedLoggerFactory
    ) {
        this.accessRoleIntelligenceAnalyticsService = accessRoleIntelligenceAnalyticsService;
        this.logger = unifiedLoggerFactory.getLogger(getClass(), "access-roles-management");
    }

    /**
     * Returns KPI analytics for the Access Role Intelligence tab.
     *
     * @param request the HTTP servlet request
     * @return KPI dataset wrapped in {@link ApiResponse}
     */
    @GetMapping(AccessRoleIntelligenceConstants.INTELLIGENCE_KPIS_PATH)
    public ApiResponse<AccessRoleIntelligenceKpisDto> getIntelligenceKpis(HttpServletRequest request) {
        String tenantId = request.getHeader("X-Tenant-Id");

        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_CONTROLLER_KPIS_REQUEST)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        AccessRoleIntelligenceKpisDto response =
                accessRoleIntelligenceAnalyticsService.getIntelligenceKpis(tenantId);

        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_CONTROLLER_KPIS_RESPONSE)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TOTAL_ROLES,
                        String.valueOf(response.getTotalRoles()))
                .detail(AccessRoleIntelligenceConstants.DETAIL_ACTIVE_ROLES,
                        String.valueOf(response.getActiveRoles()))
                .detail(AccessRoleIntelligenceConstants.DETAIL_DISABLED_ROLES,
                        String.valueOf(response.getDisabledRoles()))
                .detail(AccessRoleIntelligenceConstants.DETAIL_ORPHANED_ROLES,
                        String.valueOf(response.getOrphanedRoles()))
                .detail(AccessRoleIntelligenceConstants.DETAIL_AVG_ENTITLEMENTS_PER_ROLE,
                        String.valueOf(response.getAverageEntitlementsPerRole()))
                .detail(AccessRoleIntelligenceConstants.DETAIL_HIGH_RISK_ROLES,
                        String.valueOf(response.getHighRiskRoles()))
                .log();

        return ApiResponse.success(
                response,
                AccessRoleIntelligenceConstants.MESSAGE_INTELLIGENCE_KPIS_FETCHED
        );
    }

    /**
     * Returns chart analytics for role distribution by type.
     *
     * @param request the HTTP servlet request
     * @return role-distribution dataset wrapped in {@link ApiResponse}
     */
    @GetMapping(AccessRoleIntelligenceConstants.ROLE_DISTRIBUTION_BY_TYPE_PATH)
    public ApiResponse<List<AccessRoleTypeDistributionDto>> getRoleDistributionByType(
            HttpServletRequest request
    ) {
        String tenantId = request.getHeader("X-Tenant-Id");

        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_CONTROLLER_ROLE_DISTRIBUTION_REQUEST)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        List<AccessRoleTypeDistributionDto> response =
                accessRoleIntelligenceAnalyticsService.getRoleDistributionByType(tenantId);

        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_CONTROLLER_ROLE_DISTRIBUTION_RESPONSE)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleIntelligenceConstants.DETAIL_ROW_COUNT, String.valueOf(response.size()))
                .log();

        return ApiResponse.success(
                response,
                AccessRoleIntelligenceConstants.MESSAGE_ROLE_DISTRIBUTION_BY_TYPE_FETCHED
        );
    }

    /**
     * Returns chart analytics for roles and users by business unit.
     *
     * <p>User counts are currently mocked as zero until role-user assignment
     * analytics are implemented.</p>
     *
     * @param request the HTTP servlet request
     * @return business-unit dataset wrapped in {@link ApiResponse}
     */
    @GetMapping(AccessRoleIntelligenceConstants.ROLES_USERS_BY_BUSINESS_UNIT_PATH)
    public ApiResponse<List<AccessRoleBusinessUnitAnalyticsItemDto>> getRolesUsersByBusinessUnit(
            HttpServletRequest request
    ) {
        String tenantId = request.getHeader("X-Tenant-Id");

        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_CONTROLLER_BUSINESS_UNIT_REQUEST)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        List<AccessRoleBusinessUnitAnalyticsItemDto> response =
                accessRoleIntelligenceAnalyticsService.getRolesUsersByBusinessUnit(tenantId);

        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_CONTROLLER_BUSINESS_UNIT_RESPONSE)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleIntelligenceConstants.DETAIL_ROW_COUNT, String.valueOf(response.size()))
                .log();

        return ApiResponse.success(
                response,
                AccessRoleIntelligenceConstants.MESSAGE_ROLES_USERS_BY_BUSINESS_UNIT_FETCHED
        );
    }
}