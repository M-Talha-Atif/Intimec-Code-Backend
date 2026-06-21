package com.aidentx.iam.entitlement_management.service.impl;

import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementIntelligenceTabKpiConstants;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementIntelligenceTabKpiResponse;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.entitlement_management.service.EntitlementIntelligenceTabKpiService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service implementation for Entitlement Intelligence Tab KPI Analytics.
 *
 * This service calculates comprehensive KPI metrics for entitlement governance,
 * including counts of active, orphaned, unclassified, and high-risk entitlements,
 * as well as user assignment statistics across all resources within a tenant.
 *
 * This is specifically for Intelligence Tab KPI analytics - differentiated from other KPI types.
 * 
 * All queries use read-only transactions for consistency and performance.
 */
@Service
@Transactional(readOnly = true)
public class EntitlementIntelligenceTabKpiServiceImpl implements EntitlementIntelligenceTabKpiService {

    private final EntitlementRepository entitlementRepository;
    private final EntitlementAssignmentRepository entitlementAssignmentRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public EntitlementIntelligenceTabKpiServiceImpl(
            EntitlementRepository entitlementRepository,
            EntitlementAssignmentRepository entitlementAssignmentRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.entitlementRepository = entitlementRepository;
        this.entitlementAssignmentRepository = entitlementAssignmentRepository;
        this.logger = loggerFactory.getLogger(getClass(), EntitlementIntelligenceTabKpiConstants.LOGGER_MODULE_NAME);
    }

    @Override
    public EntitlementIntelligenceTabKpiResponse getIntelligenceTabKpis(String tenantId) {
        logger.info()
                .message(EntitlementIntelligenceTabKpiConstants.LOG_FETCHING_KPI)
                .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        validateTenantId(tenantId);

        EntitlementIntelligenceTabKpiResponse response = new EntitlementIntelligenceTabKpiResponse(tenantId);

        // Fetch total entitlements count for tenant (all resources)
        long totalEntitlements = entitlementRepository.countByTenantId(tenantId);
        response.setTotalEntitlements(totalEntitlements);

        // Fetch active entitlements count
        long activeEntitlements = entitlementRepository.countByTenantIdAndLifecycle(
                tenantId,
                EntitlementIntelligenceTabKpiConstants.LIFECYCLE_ACTIVE
        );
        response.setActiveEntitlements(activeEntitlements);

        // Fetch orphaned entitlements count
        long orphanedEntitlements = entitlementRepository.countOrphanedEntitlementsByTenantId(tenantId);
        response.setOrphanedEntitlements(orphanedEntitlements);

        // Fetch unclassified entitlements count
        long unclassifiedEntitlements = entitlementRepository.countUnclassifiedEntitlementsByTenantId(tenantId);
        response.setUnclassifiedEntitlements(unclassifiedEntitlements);

        // Fetch high-risk entitlements count
        long highRiskEntitlements = entitlementRepository.countByTenantIdAndRiskLevel(
                tenantId,
                EntitlementIntelligenceTabKpiConstants.RISK_LEVEL_HIGH
        );
        response.setHighRiskEntitlements(highRiskEntitlements);

        // Calculate average users per entitlement
        double averageUsersPerEntitlement = calculateAverageUsersPerEntitlement(tenantId, totalEntitlements);
        response.setAverageUsersPerEntitlement(averageUsersPerEntitlement);

        logger.info()
                .message(EntitlementIntelligenceTabKpiConstants.LOG_KPI_CALCULATION_SUCCESS)
                .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_TENANT_ID, tenantId)
                .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_TOTAL_ENTITLEMENTS, String.valueOf(totalEntitlements))
                .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_ACTIVE_ENTITLEMENTS, String.valueOf(activeEntitlements))
                .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_ORPHANED_ENTITLEMENTS, String.valueOf(orphanedEntitlements))
                .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_UNCLASSIFIED_ENTITLEMENTS, String.valueOf(unclassifiedEntitlements))
                .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_HIGH_RISK_ENTITLEMENTS, String.valueOf(highRiskEntitlements))
                .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_AVERAGE_USERS_PER_ENTITLEMENT, String.valueOf(averageUsersPerEntitlement))
                .log();

        return response;
    }

    /**
     * Calculates the average number of users assigned per entitlement.
     *
     * Formula: totalAssignments / totalEntitlements
     *
     * @param tenantId the tenant ID to filter by
     * @param totalEntitlements the total count of entitlements (prevents division by zero)
     * @return average users per entitlement, rounded to 2 decimal places, or 0.0 if no entitlements
     */
    private double calculateAverageUsersPerEntitlement(String tenantId, long totalEntitlements) {
        if (totalEntitlements == EntitlementIntelligenceTabKpiConstants.ZERO_COUNT) {
            logger.debug()
                    .message(EntitlementIntelligenceTabKpiConstants.LOG_DEBUG_NO_ENTITLEMENTS)
                    .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .log();
            return EntitlementIntelligenceTabKpiConstants.ZERO_DOUBLE;
        }

        long totalAssignments = entitlementAssignmentRepository.countByTenantId(tenantId);

        BigDecimal assignments = new BigDecimal(totalAssignments);
        BigDecimal entitlements = new BigDecimal(totalEntitlements);
        BigDecimal average = assignments.divide(
                entitlements,
                EntitlementIntelligenceTabKpiConstants.DECIMAL_PRECISION,
                RoundingMode.HALF_UP
        );

        return average.doubleValue();
    }

    /**
     * Validates the tenant ID parameter.
     *
     * @param tenantId the tenant ID from header
     * @throws IllegalArgumentException if tenantId is invalid
     */
    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(EntitlementIntelligenceTabKpiConstants.ERROR_TENANT_ID_BLANK);
        }
    }
}



