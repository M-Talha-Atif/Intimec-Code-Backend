package com.aidentx.iam.access_roles_management.service.analytics.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleIntelligenceConstants;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleBusinessUnitAnalyticsItemDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleIntelligenceKpisDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleTypeDistributionDto;
import com.aidentx.iam.access_roles_management.repository.AccessRoleEntitlementRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.service.analytics.AccessRoleIntelligenceAnalyticsService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link AccessRoleIntelligenceAnalyticsService}.
 *
 * <p>Calculates intelligence KPIs and chart datasets for the Access Role
 * Intelligence tab using access-role repository aggregates.</p>
 */
@Service
@Transactional(readOnly = true)
public class AccessRoleIntelligenceAnalyticsServiceImpl implements AccessRoleIntelligenceAnalyticsService {

    private final AccessRoleRepository accessRoleRepository;
    private final AccessRoleEntitlementRepository accessRoleEntitlementRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public AccessRoleIntelligenceAnalyticsServiceImpl(
            AccessRoleRepository accessRoleRepository,
            AccessRoleEntitlementRepository accessRoleEntitlementRepository,
            UnifiedLoggerFactory unifiedLoggerFactory
    ) {
        this.accessRoleRepository = accessRoleRepository;
        this.accessRoleEntitlementRepository = accessRoleEntitlementRepository;
        this.logger = unifiedLoggerFactory.getLogger(getClass(), "access-roles-management");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessRoleIntelligenceKpisDto getIntelligenceKpis(String tenantId) {
        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_SERVICE_KPIS_START)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        long totalRoles = accessRoleRepository.countByTenantId(tenantId);
        long activeRoles = accessRoleRepository.countByTenantIdAndStatus(
                tenantId, AccessRoleIntelligenceConstants.ROLE_STATUS_ACTIVE);
        long disabledRoles = accessRoleRepository.countByTenantIdAndStatus(
                tenantId, AccessRoleIntelligenceConstants.ROLE_STATUS_DISABLED);
        long highRiskRoles = accessRoleRepository.countByTenantIdAndRiskLevelIgnoreCase(tenantId, "high");

        logger.debug()
                .message("Fetched raw KPI aggregates for access role intelligence")
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TOTAL_ROLES, String.valueOf(totalRoles))
                .detail(AccessRoleIntelligenceConstants.DETAIL_ACTIVE_ROLES, String.valueOf(activeRoles))
                .detail(AccessRoleIntelligenceConstants.DETAIL_DISABLED_ROLES, String.valueOf(disabledRoles))
                .detail(AccessRoleIntelligenceConstants.DETAIL_HIGH_RISK_ROLES, String.valueOf(highRiskRoles))
                .log();

        double averageEntitlementsPerRole = AccessRoleIntelligenceConstants.DEFAULT_AVERAGE_ENTITLEMENTS_PER_ROLE;
        if (totalRoles > 0) {
            long totalRoleEntitlementLinks = accessRoleEntitlementRepository.count();
            averageEntitlementsPerRole = (double) totalRoleEntitlementLinks / totalRoles;
        }

        AccessRoleIntelligenceKpisDto response = new AccessRoleIntelligenceKpisDto();
        response.setTotalRoles(totalRoles);
        response.setActiveRoles(activeRoles);
        response.setDisabledRoles(disabledRoles);
        response.setOrphanedRoles(AccessRoleIntelligenceConstants.MOCK_ORPHANED_ROLES);
        response.setAverageEntitlementsPerRole(averageEntitlementsPerRole);
        response.setHighRiskRoles(highRiskRoles);

        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_SERVICE_KPIS_COMPLETE)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TOTAL_ROLES, String.valueOf(response.getTotalRoles()))
                .detail(AccessRoleIntelligenceConstants.DETAIL_ACTIVE_ROLES, String.valueOf(response.getActiveRoles()))
                .detail(AccessRoleIntelligenceConstants.DETAIL_DISABLED_ROLES, String.valueOf(response.getDisabledRoles()))
                .detail(AccessRoleIntelligenceConstants.DETAIL_ORPHANED_ROLES, String.valueOf(response.getOrphanedRoles()))
                .detail(AccessRoleIntelligenceConstants.DETAIL_AVG_ENTITLEMENTS_PER_ROLE,
                        String.valueOf(response.getAverageEntitlementsPerRole()))
                .detail(AccessRoleIntelligenceConstants.DETAIL_HIGH_RISK_ROLES,
                        String.valueOf(response.getHighRiskRoles()))
                .log();

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AccessRoleTypeDistributionDto> getRoleDistributionByType(String tenantId) {
        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_SERVICE_ROLE_DISTRIBUTION_START)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        List<AccessRoleTypeDistributionDto> response = accessRoleRepository.findRoleDistributionByType(tenantId)
                .stream()
                .map(row -> new AccessRoleTypeDistributionDto(
                        row.getType(),
                        row.getCount()
                ))
                .toList();

        logger.debug()
                .message("Fetched role distribution by type rows")
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleIntelligenceConstants.DETAIL_ROW_COUNT, String.valueOf(response.size()))
                .log();

        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_SERVICE_ROLE_DISTRIBUTION_COMPLETE)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleIntelligenceConstants.DETAIL_ROW_COUNT, String.valueOf(response.size()))
                .log();

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AccessRoleBusinessUnitAnalyticsItemDto> getRolesUsersByBusinessUnit(String tenantId) {
        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_SERVICE_BUSINESS_UNIT_START)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        List<AccessRoleBusinessUnitAnalyticsItemDto> response = accessRoleRepository.findRoleCountsByBusinessUnit(tenantId)
                .stream()
                .map(row -> new AccessRoleBusinessUnitAnalyticsItemDto(
                        row.getBusinessUnit() == null || row.getBusinessUnit().isBlank()
                                ? AccessRoleIntelligenceConstants.BUSINESS_UNIT_UNSPECIFIED
                                : row.getBusinessUnit(),
                        row.getRoles(),
                        AccessRoleIntelligenceConstants.MOCK_USERS_COUNT
                ))
                .toList();

        logger.debug()
                .message("Fetched roles and users by business unit rows")
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleIntelligenceConstants.DETAIL_ROW_COUNT, String.valueOf(response.size()))
                .log();

        logger.info()
                .message(AccessRoleIntelligenceConstants.LOG_SERVICE_BUSINESS_UNIT_COMPLETE)
                .detail(AccessRoleIntelligenceConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleIntelligenceConstants.DETAIL_ROW_COUNT, String.valueOf(response.size()))
                .log();

        return response;
    }

    /**
     * Projection for role distribution by type.
     */
    public interface RoleTypeDistributionProjection {

        /**
         * Returns the role type.
         *
         * @return role type
         */
        String getType();

        /**
         * Returns the number of roles for this type.
         *
         * @return role count
         */
        long getCount();
    }

    /**
     * Projection for role counts by business unit.
     */
    public interface RoleBusinessUnitCountProjection {

        /**
         * Returns the business unit.
         *
         * @return business unit
         */
        String getBusinessUnit();

        /**
         * Returns the number of roles for this business unit.
         *
         * @return role count
         */
        long getRoles();
    }
}