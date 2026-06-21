package com.aidentx.iam.access_roles_management.controller;

import com.aidentx.iam.access_management.config.constants.AccessKpiConstants;
import com.aidentx.iam.access_management.model.dto.response.AccessKpiResponseDto;
import com.aidentx.iam.access_management.service.AccessKpiService;
import com.aidentx.iam.access_roles_management.model.dto.response.ApiResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for access KPI analytics.
 *
 * <p>The controller is intentionally kept in the existing access-role package for
 * minimal API/package disruption, while cross-domain KPI formulas live in
 * {@link AccessKpiService}. The controller can later move to an
 * {@code access_management.controller} package without changing service logic.</p>
 */
@RestController
@RequestMapping(AccessKpiConstants.ACCESS_ANALYTICS_BASE_PATH)
public class AccessKpiController {

    private final AccessKpiService accessKpiService;
    private final IUnifiedLogger logger;

    @Autowired
    public AccessKpiController(AccessKpiService accessKpiService,
                               UnifiedLoggerFactory unifiedLoggerFactory) {
        this.accessKpiService = accessKpiService;
        this.logger = unifiedLoggerFactory.getLogger(getClass(), "access-management");
    }

    /**
     * Returns live access KPI cards for the tenant.
     *
     * @param tenantId tenant identifier propagated through {@code X-Tenant-Id}
     * @return KPI payload wrapped in the local API response envelope
     */
    @GetMapping(AccessKpiConstants.ACCESS_KPIS_PATH)
    public ApiResponse<AccessKpiResponseDto> getAccessKpis(
            @RequestHeader(AccessKpiConstants.HEADER_TENANT_ID) String tenantId) {
        logger.info()
                .message(AccessKpiConstants.LOG_CONTROLLER_ACCESS_KPIS_REQUEST)
                .detail(AccessKpiConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        AccessKpiResponseDto response = accessKpiService.getAccessKpis(tenantId);

        logger.info()
                .message(AccessKpiConstants.LOG_CONTROLLER_ACCESS_KPIS_RESPONSE)
                .detail(AccessKpiConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessKpiConstants.KPI_ROLE_COVERAGE_RATIO, response.getRoleCoverageRatio().getValue().toPlainString())
                .detail(AccessKpiConstants.KPI_DIRECT_ACCESS_EXPOSURE, response.getDirectAccessExposure().getValue().toPlainString())
                .detail(AccessKpiConstants.KPI_ROLE_COMPLEXITY_INDEX, response.getRoleComplexityIndex().getValue().toPlainString())
                .detail(AccessKpiConstants.KPI_ORPHANED_ACCESS_OBJECTS, response.getOrphanedAccessObjects().getValue().toPlainString())
                .detail(AccessKpiConstants.KPI_ACCESS_REUSE_SCORE, response.getAccessReuseScore().getValue().toPlainString())
                .log();

        return ApiResponse.success(response, AccessKpiConstants.MESSAGE_ACCESS_KPIS_FETCHED);
    }
}

