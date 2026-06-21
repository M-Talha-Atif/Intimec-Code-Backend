package com.aidentx.iam.entitlement_management.service.impl;

import com.aidentx.iam.entitlement_management.client.ApplicationClient;
import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementApplicationCountConstants;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementApplicationCountResponse;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.entitlement_management.service.EntitlementApplicationCountService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service implementation for Entitlement Application Count Analytics.
 *
 * This service calculates entitlement counts for each application within a tenant.
 * Application names are fetched from the Application Client service.
 * Entitlement counts are aggregated across all resources per application.
 *
 * All queries use read-only transactions for consistency and performance.
 */
@Service
@Transactional(readOnly = true)
public class EntitlementApplicationCountServiceImpl implements EntitlementApplicationCountService {

    private final EntitlementRepository entitlementRepository;
    private final ApplicationClient applicationClient;
    private final IUnifiedLogger logger;

    @Autowired
    public EntitlementApplicationCountServiceImpl(
            EntitlementRepository entitlementRepository,
            ApplicationClient applicationClient,
            UnifiedLoggerFactory loggerFactory) {
        this.entitlementRepository = entitlementRepository;
        this.applicationClient = applicationClient;
        this.logger = loggerFactory.getLogger(getClass(), EntitlementApplicationCountConstants.LOGGER_MODULE_NAME);
    }

    @Override
    public EntitlementApplicationCountResponse getApplicationEntitlementCounts(String tenantId) {
        logger.info()
                .message(EntitlementApplicationCountConstants.LOG_FETCHING_APPLICATION_COUNTS)
                .detail(EntitlementApplicationCountConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        validateTenantId(tenantId);

        EntitlementApplicationCountResponse response = new EntitlementApplicationCountResponse(tenantId);

        // Fetch all unique resource IDs (application IDs) for the tenant
        List<UUID> applicationIds = entitlementRepository.findDistinctResourceIdsByTenantId(tenantId);

        if (applicationIds == null || applicationIds.isEmpty()) {
            logger.debug()
                    .message(EntitlementApplicationCountConstants.LOG_DEBUG_NO_APPLICATIONS)
                    .detail(EntitlementApplicationCountConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            logger.info()
                    .message(EntitlementApplicationCountConstants.LOG_APPLICATION_COUNT_RETURNED)
                    .detail(EntitlementApplicationCountConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementApplicationCountConstants.DETAIL_TOTAL_APPLICATIONS, String.valueOf(EntitlementApplicationCountConstants.ZERO_COUNT))
                    .log();

            return response;
        }

        // Fetch application names from Application Client
        Map<UUID, String> applicationNames = applicationClient.getApplicationNames(tenantId, applicationIds);

        // Calculate entitlement count for each application
        for (UUID applicationId : applicationIds) {
            logger.debug()
                    .message(EntitlementApplicationCountConstants.LOG_DEBUG_PROCESSING_APPLICATION)
                    .detail(EntitlementApplicationCountConstants.DETAIL_APPLICATION_ID, applicationId.toString())
                    .log();

            long entitlementCount = entitlementRepository.countByResourceIdAndTenantId(applicationId, tenantId);
            String applicationName = applicationNames.getOrDefault(applicationId, applicationId.toString());

            EntitlementApplicationCountResponse.ApplicationEntitlementCount appCount =
                    new EntitlementApplicationCountResponse.ApplicationEntitlementCount(
                            applicationId.toString(),
                            applicationName,
                            entitlementCount
                    );

            response.getApplications().add(appCount);
        }

        logger.info()
                .message(EntitlementApplicationCountConstants.LOG_APPLICATION_COUNT_SUCCESS)
                .detail(EntitlementApplicationCountConstants.DETAIL_TENANT_ID, tenantId)
                .detail(EntitlementApplicationCountConstants.DETAIL_TOTAL_APPLICATIONS, String.valueOf(response.getApplications().size()))
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
            throw new IllegalArgumentException(EntitlementApplicationCountConstants.ERROR_TENANT_ID_BLANK);
        }
    }
}

