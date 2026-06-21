package com.aidentx.iam.entitlement_management.service.impl;

import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementRiskKpiConstants;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementRiskKpiResponse;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.entitlement_management.service.EntitlementRiskKpiService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Entitlement Risk KPI Analytics.
 * <p>
 * This service calculates risk metrics for entitlements within a tenant,
 * including high-risk entitlement count and placeholder KPI values for
 * SoD exposure, compliance tagged entitlements, and data sensitive entitlements.
 * <p>
 * This is specifically for Risk KPI analytics - differentiated from other KPI types.
 * <p>
 * All queries use read-only transactions for consistency and performance.
 */
@Service
@Transactional(readOnly = true)
public class EntitlementRiskKpiServiceImpl implements EntitlementRiskKpiService {

    private final EntitlementRepository entitlementRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public EntitlementRiskKpiServiceImpl(
            EntitlementRepository entitlementRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.entitlementRepository = entitlementRepository;
        this.logger = loggerFactory.getLogger(getClass(), EntitlementRiskKpiConstants.LOGGER_MODULE_NAME);
    }

    @Override
    public EntitlementRiskKpiResponse getRiskKpis(String tenantId) {
        logger.info()
                .message(EntitlementRiskKpiConstants.LOG_FETCHING_RISK_KPI)
                .detail(EntitlementRiskKpiConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        validateTenantId(tenantId);

        EntitlementRiskKpiResponse response = new EntitlementRiskKpiResponse(tenantId);

        // ===================================================================
        // Real KPI (actual calculation)
        // ===================================================================

        logger.debug()
                .message(EntitlementRiskKpiConstants.LOG_DEBUG_CALCULATING_HIGH_RISK_ENTITLEMENTS)
                .detail(EntitlementRiskKpiConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        Long highRiskEntitlementsCount = entitlementRepository.countByTenantIdAndRiskLevel(
                tenantId,
                EntitlementRiskKpiConstants.RISK_LEVEL_HIGH
        );
        response.setHighRiskEntitlements(
                highRiskEntitlementsCount != null ? highRiskEntitlementsCount : 0L
        );

        // ===================================================================
        // Mock KPIs (return 0 as per requirements)
        // ===================================================================

        response.setSodExposure(EntitlementRiskKpiConstants.ZERO_COUNT);
        response.setComplianceTagged(EntitlementRiskKpiConstants.ZERO_COUNT);
        response.setDataSensitive(EntitlementRiskKpiConstants.ZERO_COUNT);

        logger.info()
                .message(EntitlementRiskKpiConstants.LOG_RISK_KPI_CALCULATION_SUCCESS)
                .detail(EntitlementRiskKpiConstants.DETAIL_HIGH_RISK_ENTITLEMENTS,
                        String.valueOf(response.getHighRiskEntitlements()))
                .detail(EntitlementRiskKpiConstants.DETAIL_SOD_EXPOSURE,
                        String.valueOf(response.getSodExposure()))
                .detail(EntitlementRiskKpiConstants.DETAIL_COMPLIANCE_TAGGED,
                        String.valueOf(response.getComplianceTagged()))
                .detail(EntitlementRiskKpiConstants.DETAIL_DATA_SENSITIVE,
                        String.valueOf(response.getDataSensitive()))
                .log();

        return response;
    }

    /**
     * Validates the tenant ID parameter.
     *
     * @param tenantId the tenant ID from header
     * @throws IllegalArgumentException if tenantId is invalid
     */
    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(EntitlementRiskKpiConstants.ERROR_TENANT_ID_BLANK);
        }
    }
}