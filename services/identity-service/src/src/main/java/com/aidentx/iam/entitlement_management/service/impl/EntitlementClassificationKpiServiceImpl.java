package com.aidentx.iam.entitlement_management.service.impl;

import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementClassificationKpiConstants;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementClassificationKpiResponse;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.entitlement_management.service.EntitlementClassificationKpiService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Entitlement Classification KPI Analytics.
 * <p>
 * This service calculates classification metrics for entitlements within a tenant,
 * including ownership gaps, unclassified entitlements, requestability rate, and
 * requestable entitlements count.
 * <p>
 * This is specifically for Classification KPI analytics - differentiated from other KPI types.
 * <p>
 * All queries use read-only transactions for consistency and performance.
 */
@Service
@Transactional(readOnly = true)
public class EntitlementClassificationKpiServiceImpl implements EntitlementClassificationKpiService {

    private final EntitlementRepository entitlementRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public EntitlementClassificationKpiServiceImpl(
            EntitlementRepository entitlementRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.entitlementRepository = entitlementRepository;
        this.logger = loggerFactory.getLogger(getClass(), EntitlementClassificationKpiConstants.LOGGER_MODULE_NAME);
    }

    @Override
    public EntitlementClassificationKpiResponse getClassificationKpis(String tenantId) {
        logger.info()
                .message(EntitlementClassificationKpiConstants.LOG_FETCHING_CLASSIFICATION_KPI)
                .detail(EntitlementClassificationKpiConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        validateTenantId(tenantId);

        EntitlementClassificationKpiResponse response = new EntitlementClassificationKpiResponse(tenantId);

        // ===================================================================
        // Real KPIs (actual calculation)
        // ===================================================================

        // Real: Ownership gaps (entitlements without owners - ownerId = NULL)
        logger.debug()
                .message(EntitlementClassificationKpiConstants.LOG_DEBUG_CALCULATING_OWNERSHIP_GAPS)
                .detail(EntitlementClassificationKpiConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        Long ownershipGapsCount = entitlementRepository.countEntitlementsWithoutOwner(tenantId);
        response.setOwnershipGaps(
                ownershipGapsCount != null ? ownershipGapsCount : 0L
        );

        // ===================================================================
        // Mock KPIs (return 0 as per requirements)
        // ===================================================================

        // Mock: Unclassified entitlements (no classification labels) - currently not implementable
        response.setUnclassified(EntitlementClassificationKpiConstants.ZERO_COUNT);

        // Mock: Requestability Rate (percentage of requestable entitlements) - currently not implementable
        response.setRequestabilityRate(EntitlementClassificationKpiConstants.ZERO_DOUBLE);

        // ===================================================================
        // Real KPIs (actual calculation)
        // ===================================================================

        // Real: Requestable entitlements count (entitlements marked as requestable)
        logger.debug()
                .message(EntitlementClassificationKpiConstants.LOG_DEBUG_CALCULATING_REQUESTABLE_ENTITLEMENTS)
                .detail(EntitlementClassificationKpiConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        Long requestableEntitlementsCount = entitlementRepository.countRequestableEntitlements(tenantId);
        response.setRequestableEntitlements(
                requestableEntitlementsCount != null ? requestableEntitlementsCount : 0L
        );

        logger.info()
                .message(EntitlementClassificationKpiConstants.LOG_CLASSIFICATION_KPI_CALCULATION_SUCCESS)
                .detail(EntitlementClassificationKpiConstants.DETAIL_OWNERSHIP_GAPS,
                        String.valueOf(response.getOwnershipGaps()))
                .detail(EntitlementClassificationKpiConstants.DETAIL_UNCLASSIFIED,
                        String.valueOf(response.getUnclassified()))
                .detail(EntitlementClassificationKpiConstants.DETAIL_REQUESTABILITY_RATE,
                        String.valueOf(response.getRequestabilityRate()))
                .detail(EntitlementClassificationKpiConstants.DETAIL_REQUESTABLE_ENTITLEMENTS,
                        String.valueOf(response.getRequestableEntitlements()))
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
            throw new IllegalArgumentException(EntitlementClassificationKpiConstants.ERROR_TENANT_ID_BLANK);
        }
    }
}

