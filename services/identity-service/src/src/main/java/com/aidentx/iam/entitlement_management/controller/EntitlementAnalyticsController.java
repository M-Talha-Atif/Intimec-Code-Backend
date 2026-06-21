package com.aidentx.iam.entitlement_management.controller;

import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementApplicationCountConstants;
import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementClassificationKpiConstants;
import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementIntelligenceTabKpiConstants;
import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementRiskDistributionConstants;
import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementRiskKpiConstants;
import com.aidentx.iam.entitlement_management.config.constants.analytics.EntitlementSprawlKpiConstants;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementApplicationCountResponse;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementClassificationKpiResponse;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementIntelligenceTabKpiResponse;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementSprawlKpiResponse;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementRiskKpiResponse;
import com.aidentx.iam.entitlement_management.model.dto.response.EntitlementRiskDistributionResponse;
import com.aidentx.iam.entitlement_management.service.EntitlementRiskKpiService;
import com.aidentx.iam.entitlement_management.service.EntitlementApplicationCountService;
import com.aidentx.iam.entitlement_management.service.EntitlementClassificationKpiService;
import com.aidentx.iam.entitlement_management.service.EntitlementIntelligenceTabKpiService;
import com.aidentx.iam.entitlement_management.service.EntitlementSprawlKpiService;
import com.aidentx.iam.entitlement_management.service.EntitlementRiskDistributionService;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Entitlement Analytics APIs.
 * Provides endpoints for retrieving entitlement governance metrics and analytics.
 * <p>
 * Base Path: /api/v1/entitlements/analytics
 */
@RestController
@RequestMapping("/api/v1/entitlements/analytics")
public class EntitlementAnalyticsController {

    private final EntitlementIntelligenceTabKpiService entitlementIntelligenceTabKpiService;
    private final EntitlementApplicationCountService entitlementApplicationCountService;
    private final EntitlementSprawlKpiService entitlementSprawlKpiService;
    private final EntitlementClassificationKpiService entitlementClassificationKpiService;
    private final EntitlementRiskKpiService entitlementRiskKpiService;
    private final EntitlementRiskDistributionService entitlementRiskDistributionService;
    private final IUnifiedLogger logger;

    @Autowired
    public EntitlementAnalyticsController(
            EntitlementIntelligenceTabKpiService entitlementIntelligenceTabKpiService,
            EntitlementApplicationCountService entitlementApplicationCountService,
            EntitlementSprawlKpiService entitlementSprawlKpiService,
            EntitlementClassificationKpiService entitlementClassificationKpiService,
            EntitlementRiskKpiService entitlementRiskKpiService,
            EntitlementRiskDistributionService entitlementRiskDistributionService,
            UnifiedLoggerFactory loggerFactory) {
        this.entitlementIntelligenceTabKpiService = entitlementIntelligenceTabKpiService;
        this.entitlementApplicationCountService = entitlementApplicationCountService;
        this.entitlementSprawlKpiService = entitlementSprawlKpiService;
        this.entitlementClassificationKpiService = entitlementClassificationKpiService;
        this.entitlementRiskKpiService = entitlementRiskKpiService;
        this.entitlementRiskDistributionService = entitlementRiskDistributionService;
        this.logger = loggerFactory.getLogger(getClass(), EntitlementIntelligenceTabKpiConstants.LOGGER_MODULE_NAME);
    }

    /**
     * Retrieves comprehensive KPI metrics for entitlement governance (Intelligence Tab).
     * <p>
     * Metrics Calculated:
     * - Total number of entitlements for the tenant (all resources)
     * - Count of active entitlements
     * - Count of orphaned entitlements (no owner)
     * - Count of unclassified entitlements
     * - Count of high-risk entitlements
     * - Average number of users per entitlement
     * <p>
     * Headers:
     * - X-Tenant-ID: Required. The tenant ID to retrieve KPIs for.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return ResponseEntity with EntitlementIntelligenceTabKpiResponse containing all metrics
     */
    @GetMapping("/intelligence-kpi")
    public ResponseEntity<EntitlementIntelligenceTabKpiResponse> getIntelligenceTabKpis(
            @RequestHeader(EntitlementIntelligenceTabKpiConstants.HEADER_TENANT_ID) String tenantId) {

        try {
            logger.info()
                    .message(EntitlementIntelligenceTabKpiConstants.LOG_FETCHING_KPI)
                    .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            EntitlementIntelligenceTabKpiResponse response =
                    entitlementIntelligenceTabKpiService.getIntelligenceTabKpis(tenantId);

            logger.info()
                    .message(EntitlementIntelligenceTabKpiConstants.LOG_KPI_RETURNED_SUCCESS)
                    .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error()
                    .message(EntitlementIntelligenceTabKpiConstants.LOG_ERROR_INVALID_REQUEST)
                    .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error()
                    .message(EntitlementIntelligenceTabKpiConstants.LOG_ERROR_CALCULATING_KPI)
                    .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementIntelligenceTabKpiConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves entitlement count for each application in a tenant.
     * <p>
     * Returns a list of applications with their corresponding entitlement counts.
     * Application names are fetched from the Application Client service.
     * Entitlement counts are aggregated across all resources per application.
     * <p>
     * Headers:
     * - X-Tenant-ID: Required. The tenant ID to retrieve application entitlement counts for.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return ResponseEntity with EntitlementApplicationCountResponse containing applications and counts
     */
    @GetMapping("/application-count")
    public ResponseEntity<EntitlementApplicationCountResponse> getApplicationEntitlementCounts(
            @RequestHeader(EntitlementApplicationCountConstants.HEADER_TENANT_ID) String tenantId) {

        try {
            logger.info()
                    .message(EntitlementApplicationCountConstants.LOG_FETCHING_APPLICATION_COUNTS)
                    .detail(EntitlementApplicationCountConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            EntitlementApplicationCountResponse response =
                    entitlementApplicationCountService.getApplicationEntitlementCounts(tenantId);

            logger.info()
                    .message(EntitlementApplicationCountConstants.LOG_APPLICATION_COUNT_RETURNED)
                    .detail(EntitlementApplicationCountConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementApplicationCountConstants.DETAIL_TOTAL_APPLICATIONS,
                            String.valueOf(response.getApplications().size()))
                    .log();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error()
                    .message(EntitlementApplicationCountConstants.LOG_ERROR_INVALID_REQUEST)
                    .detail(EntitlementApplicationCountConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementApplicationCountConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error()
                    .message(EntitlementApplicationCountConstants.LOG_ERROR_FETCHING_APPLICATION_COUNTS)
                    .detail(EntitlementApplicationCountConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementApplicationCountConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves sprawl KPI metrics for entitlements within a tenant.
     * <p>
     * Metrics Calculated:
     * - Sprawl Index: Average entitlements per user (currently mock - returns 0)
     * - Stale Entitlements: Deprecated/unused entitlements since start of year (currently mock - returns 0)
     * - Over-Provisioned Count: Entitlements assigned to 100+ users (real calculation)
     * - Direct Assignment Count: Entitlements with direct manual assignments (real calculation)
     * <p>
     * Headers:
     * - X-Tenant-ID: Required. The tenant ID to retrieve sprawl KPIs for.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return ResponseEntity with EntitlementSprawlKpiResponse containing sprawl metrics
     */
    @GetMapping("/sprawl-kpi")
    public ResponseEntity<EntitlementSprawlKpiResponse> getSprawlKpis(
            @RequestHeader(EntitlementSprawlKpiConstants.HEADER_TENANT_ID) String tenantId) {

        try {
            logger.info()
                    .message(EntitlementSprawlKpiConstants.LOG_FETCHING_SPRAWL_KPI)
                    .detail(EntitlementSprawlKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            EntitlementSprawlKpiResponse response =
                    entitlementSprawlKpiService.getSprawlKpis(tenantId);

            logger.info()
                    .message(EntitlementSprawlKpiConstants.LOG_SPRAWL_KPI_RETURNED_SUCCESS)
                    .detail(EntitlementSprawlKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error()
                    .message(EntitlementSprawlKpiConstants.LOG_ERROR_INVALID_REQUEST)
                    .detail(EntitlementSprawlKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementSprawlKpiConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error()
                    .message(EntitlementSprawlKpiConstants.LOG_ERROR_CALCULATING_SPRAWL_KPI)
                    .detail(EntitlementSprawlKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementSprawlKpiConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves classification KPI metrics for entitlements within a tenant.
     * <p>
     * Metrics Calculated:
     * - Ownership Gaps: Count of entitlements without owners (real calculation)
     * - Unclassified: Count of unclassified entitlements (currently mock - returns 0)
     * - Requestability Rate: Percentage of requestable entitlements (currently mock - returns 0%)
     * - Requestable Entitlements: Count of entitlements marked as requestable (real calculation)
     * <p>
     * Headers:
     * - X-Tenant-ID: Required. The tenant ID to retrieve classification KPIs for.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return ResponseEntity with EntitlementClassificationKpiResponse containing classification metrics
     */
    @GetMapping("/classification-kpi")
    public ResponseEntity<EntitlementClassificationKpiResponse> getClassificationKpis(
            @RequestHeader(EntitlementClassificationKpiConstants.HEADER_TENANT_ID) String tenantId) {

        try {
            logger.info()
                    .message(EntitlementClassificationKpiConstants.LOG_FETCHING_CLASSIFICATION_KPI)
                    .detail(EntitlementClassificationKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            EntitlementClassificationKpiResponse response =
                    entitlementClassificationKpiService.getClassificationKpis(tenantId);

            logger.info()
                    .message(EntitlementClassificationKpiConstants.LOG_CLASSIFICATION_KPI_RETURNED_SUCCESS)
                    .detail(EntitlementClassificationKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error()
                    .message(EntitlementClassificationKpiConstants.LOG_ERROR_INVALID_REQUEST)
                    .detail(EntitlementClassificationKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementClassificationKpiConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error()
                    .message(EntitlementClassificationKpiConstants.LOG_ERROR_CALCULATING_CLASSIFICATION_KPI)
                    .detail(EntitlementClassificationKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementClassificationKpiConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves risk KPI metrics for entitlements within a tenant.
     * <p>
     * Metrics Calculated:
     * - High Risk Entitlements: Count of entitlements with HIGH risk level (real calculation)
     * - SoD Exposure: Count of SoD exposure entitlements (currently mock - returns 0)
     * - Compliance Tagged: Count of compliance tagged entitlements (currently mock - returns 0)
     * - Data Sensitive: Count of data sensitive entitlements (currently mock - returns 0)
     * <p>
     * Headers:
     * - X-Tenant-ID: Required. The tenant ID to retrieve risk KPIs for.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return ResponseEntity with EntitlementRiskKpiResponse containing risk metrics
     */
    @GetMapping("/risk-kpi")
    public ResponseEntity<EntitlementRiskKpiResponse> getRiskKpis(
            @RequestHeader(EntitlementRiskKpiConstants.HEADER_TENANT_ID) String tenantId) {

        try {
            logger.info()
                    .message(EntitlementRiskKpiConstants.LOG_FETCHING_RISK_KPI)
                    .detail(EntitlementRiskKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            EntitlementRiskKpiResponse response =
                    entitlementRiskKpiService.getRiskKpis(tenantId);

            logger.info()
                    .message(EntitlementRiskKpiConstants.LOG_RISK_KPI_RETURNED_SUCCESS)
                    .detail(EntitlementRiskKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error()
                    .message(EntitlementRiskKpiConstants.LOG_ERROR_INVALID_REQUEST)
                    .detail(EntitlementRiskKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementRiskKpiConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error()
                    .message(EntitlementRiskKpiConstants.LOG_ERROR_CALCULATING_RISK_KPI)
                    .detail(EntitlementRiskKpiConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementRiskKpiConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves risk distribution metrics for entitlements within a tenant.
     * <p>
     * Metrics Calculated:
     * - High Risk Entitlements: Count of entitlements with HIGH risk level
     * - Medium Risk Entitlements: Count of entitlements with MEDIUM risk level
     * - Low Risk Entitlements: Count of entitlements with LOW risk level
     * <p>
     * Headers:
     * - X-Tenant-ID: Required. The tenant ID to retrieve risk distribution for.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return ResponseEntity with EntitlementRiskDistributionResponse containing risk distribution metrics
     */
    @GetMapping("/risk-distribution")
    public ResponseEntity<EntitlementRiskDistributionResponse> getRiskDistribution(
            @RequestHeader(EntitlementRiskDistributionConstants.HEADER_TENANT_ID) String tenantId) {

        try {
            logger.info()
                    .message(EntitlementRiskDistributionConstants.LOG_FETCHING_RISK_DISTRIBUTION)
                    .detail(EntitlementRiskDistributionConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            EntitlementRiskDistributionResponse response =
                    entitlementRiskDistributionService.getRiskDistribution(tenantId);

            logger.info()
                    .message(EntitlementRiskDistributionConstants.LOG_RISK_DISTRIBUTION_RETURNED_SUCCESS)
                    .detail(EntitlementRiskDistributionConstants.DETAIL_TENANT_ID, tenantId)
                    .log();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error()
                    .message(EntitlementRiskDistributionConstants.LOG_ERROR_INVALID_REQUEST)
                    .detail(EntitlementRiskDistributionConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementRiskDistributionConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error()
                    .message(EntitlementRiskDistributionConstants.LOG_ERROR_CALCULATING_RISK_DISTRIBUTION)
                    .detail(EntitlementRiskDistributionConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(EntitlementRiskDistributionConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}






