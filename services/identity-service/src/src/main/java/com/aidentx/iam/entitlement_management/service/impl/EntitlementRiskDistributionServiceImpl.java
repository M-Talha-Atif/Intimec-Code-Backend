package com.aidentx.iam.entitlement_management.service.impl;

import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementRiskDistributionConstants;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementRiskDistributionResponse;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.entitlement_management.service.EntitlementRiskDistributionService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Entitlement Risk Distribution Analytics.
 * <p>
 * This service calculates entitlement counts grouped by risk level within a tenant,
 * specifically HIGH, MEDIUM, and LOW risk entitlements.
 * <p>
 * All queries use read-only transactions for consistency and performance.
 */
@Service
@Transactional(readOnly = true)
public class EntitlementRiskDistributionServiceImpl implements EntitlementRiskDistributionService {

    private final EntitlementRepository entitlementRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public EntitlementRiskDistributionServiceImpl(
            EntitlementRepository entitlementRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.entitlementRepository = entitlementRepository;
        this.logger = loggerFactory.getLogger(getClass(), EntitlementRiskDistributionConstants.LOGGER_MODULE_NAME);
    }

    @Override
    public EntitlementRiskDistributionResponse getRiskDistribution(String tenantId) {
        logger.info()
                .message(EntitlementRiskDistributionConstants.LOG_FETCHING_RISK_DISTRIBUTION)
                .detail(EntitlementRiskDistributionConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        validateTenantId(tenantId);

        EntitlementRiskDistributionResponse response = new EntitlementRiskDistributionResponse(tenantId);

        logger.debug()
                .message(EntitlementRiskDistributionConstants.LOG_DEBUG_CALCULATING_HIGH_RISK)
                .detail(EntitlementRiskDistributionConstants.DETAIL_TENANT_ID, tenantId)
                .log();
        Long highRiskCount = entitlementRepository.countByTenantIdAndRiskLevel(
                tenantId, EntitlementRiskDistributionConstants.RISK_LEVEL_HIGH);
        response.setHighRiskEntitlements(highRiskCount != null ? highRiskCount : 0L);

        logger.debug()
                .message(EntitlementRiskDistributionConstants.LOG_DEBUG_CALCULATING_MEDIUM_RISK)
                .detail(EntitlementRiskDistributionConstants.DETAIL_TENANT_ID, tenantId)
                .log();
        Long mediumRiskCount = entitlementRepository.countByTenantIdAndRiskLevel(
                tenantId, EntitlementRiskDistributionConstants.RISK_LEVEL_MEDIUM);
        response.setMediumRiskEntitlements(mediumRiskCount != null ? mediumRiskCount : 0L);

        logger.debug()
                .message(EntitlementRiskDistributionConstants.LOG_DEBUG_CALCULATING_LOW_RISK)
                .detail(EntitlementRiskDistributionConstants.DETAIL_TENANT_ID, tenantId)
                .log();
        Long lowRiskCount = entitlementRepository.countByTenantIdAndRiskLevel(
                tenantId, EntitlementRiskDistributionConstants.RISK_LEVEL_LOW);
        response.setLowRiskEntitlements(lowRiskCount != null ? lowRiskCount : 0L);

        logger.info()
                .message(EntitlementRiskDistributionConstants.LOG_RISK_DISTRIBUTION_CALCULATION_SUCCESS)
                .detail(EntitlementRiskDistributionConstants.DETAIL_HIGH_RISK_ENTITLEMENTS,
                        String.valueOf(response.getHighRiskEntitlements()))
                .detail(EntitlementRiskDistributionConstants.DETAIL_MEDIUM_RISK_ENTITLEMENTS,
                        String.valueOf(response.getMediumRiskEntitlements()))
                .detail(EntitlementRiskDistributionConstants.DETAIL_LOW_RISK_ENTITLEMENTS,
                        String.valueOf(response.getLowRiskEntitlements()))
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
            throw new IllegalArgumentException(EntitlementRiskDistributionConstants.ERROR_TENANT_ID_BLANK);
        }
    }
}