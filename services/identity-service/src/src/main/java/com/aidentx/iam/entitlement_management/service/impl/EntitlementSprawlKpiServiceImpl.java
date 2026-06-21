package com.aidentx.iam.entitlement_management.service.impl;

import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementSprawlKpiConstants;
import com.aidentx.iam.entitlement_management.config.constants.EntitlementAssignmentSourceConstants;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementSprawlKpiResponse;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.entitlement_management.service.EntitlementSprawlKpiService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Entitlement Sprawl KPI Analytics.
 * <p>
 * This service calculates sprawl metrics for entitlements within a tenant,
 * including over-provisioned entitlements and direct assignment tracking.
 * <p>
 * This is specifically for Sprawl KPI analytics - differentiated from other KPI types.
 * <p>
 * All queries use read-only transactions for consistency and performance.
 */
@Service
@Transactional(readOnly = true)
public class EntitlementSprawlKpiServiceImpl implements EntitlementSprawlKpiService {

    private final EntitlementRepository entitlementRepository;
    private final EntitlementAssignmentRepository entitlementAssignmentRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public EntitlementSprawlKpiServiceImpl(
            EntitlementRepository entitlementRepository,
            EntitlementAssignmentRepository entitlementAssignmentRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.entitlementRepository = entitlementRepository;
        this.entitlementAssignmentRepository = entitlementAssignmentRepository;
        this.logger = loggerFactory.getLogger(getClass(), EntitlementSprawlKpiConstants.LOGGER_MODULE_NAME);
    }

    @Override
    public EntitlementSprawlKpiResponse getSprawlKpis(String tenantId) {
        logger.info()
                .message(EntitlementSprawlKpiConstants.LOG_FETCHING_SPRAWL_KPI)
                .detail(EntitlementSprawlKpiConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        validateTenantId(tenantId);

        EntitlementSprawlKpiResponse response = new EntitlementSprawlKpiResponse(tenantId);

        // ===================================================================
        // Mock KPIs (return 0 as per requirements)
        // ===================================================================

        // Mock: Sprawl Index (average entitlements per user) - currently not implementable
        response.setSprawlIndex(EntitlementSprawlKpiConstants.ZERO_DOUBLE);

        // Mock: Stale Entitlements (deprecated/unused since start of year) - currently not implementable
        response.setStaleEntitlements(EntitlementSprawlKpiConstants.ZERO_COUNT);

        // ===================================================================
        // Real KPIs (actual calculation)
        // ===================================================================

        // Real: Over-provisioned entitlements (assigned to 100+ users)
        logger.debug()
                .message(EntitlementSprawlKpiConstants.LOG_DEBUG_CALCULATING_OVER_PROVISIONED)
                .detail(EntitlementSprawlKpiConstants.DETAIL_THRESHOLD,
                        String.valueOf(EntitlementSprawlKpiConstants.OVER_PROVISIONED_THRESHOLD))
                .detail(EntitlementSprawlKpiConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        Long overProvisionedCount = entitlementAssignmentRepository
                .countEntitlementsWithAssignmentCountGreaterThan(
                        tenantId,
                        EntitlementSprawlKpiConstants.OVER_PROVISIONED_THRESHOLD);

        response.setOverProvisionedCount(
                overProvisionedCount != null ? overProvisionedCount : 0L
        );

        // Real: Direct assignment count (entitlements with DIRECT source)
        logger.debug()
                .message(EntitlementSprawlKpiConstants.LOG_DEBUG_CALCULATING_DIRECT_ASSIGNMENTS)
                .detail(EntitlementSprawlKpiConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        long directAssignmentCount = entitlementAssignmentRepository
                .countDistinctEntitlementsByTenantIdAndSource(
                        tenantId, EntitlementAssignmentSourceConstants.SOURCE_DIRECT);
        response.setDirectAssignmentCount(directAssignmentCount);

        logger.info()
                .message(EntitlementSprawlKpiConstants.LOG_SPRAWL_KPI_CALCULATION_SUCCESS)
                .detail(EntitlementSprawlKpiConstants.DETAIL_SPRAWL_INDEX,
                        String.valueOf(response.getSprawlIndex()))
                .detail(EntitlementSprawlKpiConstants.DETAIL_STALE_ENTITLEMENTS,
                        String.valueOf(response.getStaleEntitlements()))
                .detail(EntitlementSprawlKpiConstants.DETAIL_OVER_PROVISIONED_COUNT,
                        String.valueOf(response.getOverProvisionedCount()))
                .detail(EntitlementSprawlKpiConstants.DETAIL_DIRECT_ASSIGNMENT_COUNT,
                        String.valueOf(response.getDirectAssignmentCount()))
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
            throw new IllegalArgumentException(EntitlementSprawlKpiConstants.ERROR_TENANT_ID_BLANK);
        }
    }
}

