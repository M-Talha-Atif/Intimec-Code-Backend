package com.aidentx.iam.access_management.service.impl;

import com.aidentx.iam.access_management.config.constants.AccessKpiConstants;
import com.aidentx.iam.access_management.model.dto.response.AccessKpiMetricDto;
import com.aidentx.iam.access_management.model.dto.response.AccessKpiResponseDto;
import com.aidentx.iam.access_management.service.AccessKpiService;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Default live-computation implementation of {@link AccessKpiService}.
 *
 * <p>This service intentionally owns cross-domain access KPI formulas while
 * controllers remain in existing feature packages. It reads current-state
 * repository aggregates only; historical deltas can be added later by reading a
 * snapshot table without changing this service contract or response DTO.</p>
 */
@Service
@Transactional(readOnly = true)
public class AccessKpiServiceImpl implements AccessKpiService {

    private final EntitlementAssignmentRepository entitlementAssignmentRepository;
    private final AccessRoleRepository accessRoleRepository;
    private final IdentityRoleAssignmentRepository identityRoleAssignmentRepository;
    private final RoleEntitlementClosureRepository roleEntitlementClosureRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public AccessKpiServiceImpl(EntitlementAssignmentRepository entitlementAssignmentRepository,
                                AccessRoleRepository accessRoleRepository,
                                IdentityRoleAssignmentRepository identityRoleAssignmentRepository,
                                RoleEntitlementClosureRepository roleEntitlementClosureRepository,
                                UnifiedLoggerFactory unifiedLoggerFactory) {
        this.entitlementAssignmentRepository = entitlementAssignmentRepository;
        this.accessRoleRepository = accessRoleRepository;
        this.identityRoleAssignmentRepository = identityRoleAssignmentRepository;
        this.roleEntitlementClosureRepository = roleEntitlementClosureRepository;
        this.logger = unifiedLoggerFactory.getLogger(getClass(), "access-management");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessKpiResponseDto getAccessKpis(String tenantId) {
        logger.info()
                .message(AccessKpiConstants.LOG_SERVICE_ACCESS_KPIS_START)
                .detail(AccessKpiConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        long totalEntitlementAssignments = entitlementAssignmentRepository.countByTenantId(tenantId);
        long roleDerivedAssignments = entitlementAssignmentRepository.countByTenantIdAndSourceIgnoreCase(
                tenantId, AccessKpiConstants.SOURCE_ROLE_DERIVED);
        long directAssignments = entitlementAssignmentRepository.countStandaloneAssignmentsByTenantIdAndSourceIn(
                tenantId,
                List.of(
                        AccessKpiConstants.SOURCE_DIRECT,
                        AccessKpiConstants.SOURCE_MANUAL,
                        AccessKpiConstants.SOURCE_IMPORT));
        long totalRoles = accessRoleRepository.countByTenantId(tenantId);
        long activeRoles = accessRoleRepository.countByTenantIdAndStatusIgnoreCase(
                tenantId, AccessKpiConstants.ROLE_STATUS_ACTIVE);
        long roleEntitlementMemberships = roleEntitlementClosureRepository
                .sumDistinctEntitlementMembershipsForRolesByTenantIdAndStatus(
                        tenantId, AccessKpiConstants.ROLE_STATUS_ACTIVE);
        long orphanedEntitlements = entitlementAssignmentRepository.countEntitlementsWithoutAssignments(tenantId);
        long orphanedRoles = accessRoleRepository.countRolesWithoutActiveMembers(
                tenantId, IdentityRoleAssignment.Status.ACTIVE);
        long reusedRoles = identityRoleAssignmentRepository.countRolesWithAtLeastDistinctIdentityMembers(
                tenantId,
                IdentityRoleAssignment.Status.ACTIVE.name(),
                AccessKpiConstants.REUSED_ROLE_MIN_MEMBER_COUNT);

        logger.debug()
                .message(AccessKpiConstants.LOG_SERVICE_ACCESS_KPIS_AGGREGATES)
                .detail(AccessKpiConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessKpiConstants.DETAIL_TOTAL_ENTITLEMENT_ASSIGNMENTS, String.valueOf(totalEntitlementAssignments))
                .detail(AccessKpiConstants.DETAIL_ROLE_DERIVED_ASSIGNMENTS, String.valueOf(roleDerivedAssignments))
                .detail(AccessKpiConstants.DETAIL_DIRECT_ASSIGNMENTS, String.valueOf(directAssignments))
                .detail(AccessKpiConstants.DETAIL_TOTAL_ROLES, String.valueOf(totalRoles))
                .detail(AccessKpiConstants.DETAIL_ACTIVE_ROLES, String.valueOf(activeRoles))
                .detail(AccessKpiConstants.DETAIL_ROLE_ENTITLEMENT_MEMBERSHIPS, String.valueOf(roleEntitlementMemberships))
                .detail(AccessKpiConstants.DETAIL_ORPHANED_ENTITLEMENTS, String.valueOf(orphanedEntitlements))
                .detail(AccessKpiConstants.DETAIL_ORPHANED_ROLES, String.valueOf(orphanedRoles))
                .detail(AccessKpiConstants.DETAIL_REUSED_ROLES, String.valueOf(reusedRoles))
                .log();

        AccessKpiResponseDto response = new AccessKpiResponseDto();
        response.setRoleCoverageRatio(percent(roleDerivedAssignments, totalEntitlementAssignments));
        response.setDirectAccessExposure(count(directAssignments));
        response.setRoleComplexityIndex(average(roleEntitlementMemberships, activeRoles));
        response.setOrphanedAccessObjects(count(orphanedEntitlements + orphanedRoles));
        response.setAccessReuseScore(percent(reusedRoles, totalRoles));

        logger.info()
                .message(AccessKpiConstants.LOG_SERVICE_ACCESS_KPIS_COMPLETE)
                .detail(AccessKpiConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessKpiConstants.KPI_ROLE_COVERAGE_RATIO, response.getRoleCoverageRatio().getValue().toPlainString())
                .detail(AccessKpiConstants.KPI_DIRECT_ACCESS_EXPOSURE, response.getDirectAccessExposure().getValue().toPlainString())
                .detail(AccessKpiConstants.KPI_ROLE_COMPLEXITY_INDEX, response.getRoleComplexityIndex().getValue().toPlainString())
                .detail(AccessKpiConstants.KPI_ORPHANED_ACCESS_OBJECTS, response.getOrphanedAccessObjects().getValue().toPlainString())
                .detail(AccessKpiConstants.KPI_ACCESS_REUSE_SCORE, response.getAccessReuseScore().getValue().toPlainString())
                .log();

        return response;
    }

    /**
     * Calculates a percentage KPI with zero-denominator protection.
     *
     * @param numerator   numerator count
     * @param denominator denominator count
     * @return metric card using percent units
     */
    private AccessKpiMetricDto percent(long numerator, long denominator) {
        BigDecimal value = BigDecimal.ZERO;
        if (denominator > 0) {
            value = BigDecimal.valueOf(numerator)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(denominator), AccessKpiConstants.KPI_VALUE_SCALE, RoundingMode.HALF_UP);
        }
        return metric(value, AccessKpiConstants.UNIT_PERCENT);
    }

    /**
     * Calculates an average KPI with zero-denominator protection.
     *
     * @param numerator   summed count
     * @param denominator item count
     * @return metric card using average units
     */
    private AccessKpiMetricDto average(long numerator, long denominator) {
        BigDecimal value = BigDecimal.ZERO;
        if (denominator > 0) {
            value = BigDecimal.valueOf(numerator)
                    .divide(BigDecimal.valueOf(denominator), AccessKpiConstants.KPI_VALUE_SCALE, RoundingMode.HALF_UP);
        }
        return metric(value, AccessKpiConstants.UNIT_AVERAGE);
    }

    /**
     * Builds a count KPI.
     *
     * @param value current count
     * @return metric card using count units
     */
    private AccessKpiMetricDto count(long value) {
        return metric(BigDecimal.valueOf(value), AccessKpiConstants.UNIT_COUNT);
    }

    /**
     * Builds a metric card with current-period delta placeholders.
     *
     * @param value current KPI value
     * @param unit  display unit
     * @return access KPI metric card
     */
    private AccessKpiMetricDto metric(BigDecimal value, String unit) {
        return new AccessKpiMetricDto(
                value,
                unit,
                null,
                AccessKpiConstants.DELTA_DIRECTION_NONE,
                AccessKpiConstants.PERIOD_LABEL_CURRENT);
    }
}

