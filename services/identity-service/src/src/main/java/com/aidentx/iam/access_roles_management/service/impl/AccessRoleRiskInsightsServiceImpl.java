package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleConstants;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleRiskInsightsConstants;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRiskInsightsResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.EntitlementDepthCountDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleRiskInsightsService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link AccessRoleRiskInsightsService}.
 *
 * <p>All counts derive from the materialized
 * {@code access_role_entitlement_closure} table joined to {@code entitlements},
 * so no recursive composition traversal happens at request time.</p>
 */
@Service
public class AccessRoleRiskInsightsServiceImpl implements AccessRoleRiskInsightsService {

    private final AccessRoleRepository accessRoleRepository;
    private final RoleEntitlementClosureRepository closureRepository;
    private final IUnifiedLogger logger;
    private final int maxDepth;

    public AccessRoleRiskInsightsServiceImpl(
            AccessRoleRepository accessRoleRepository,
            RoleEntitlementClosureRepository closureRepository,
            UnifiedLoggerFactory loggerFactory,
            @Value("${" + AccessRoleRiskInsightsConstants.CONFIG_KEY_MAX_DEPTH
                    + ":" + AccessRoleRiskInsightsConstants.MAX_DEPTH_DEFAULT + "}") int maxDepth) {
        this.accessRoleRepository = accessRoleRepository;
        this.closureRepository = closureRepository;
        this.logger = loggerFactory.getLogger(getClass(),
                AccessRoleRiskInsightsConstants.SERVICE_LOG_CATEGORY);
        this.maxDepth = maxDepth;
    }

    @Override
    @Transactional(readOnly = true)
    public AccessRoleRiskInsightsResponse getRiskInsights(String tenantId, UUID roleId) {
        logger.info()
                .category(AccessRoleRiskInsightsConstants.SERVICE_LOG_CATEGORY)
                .message(AccessRoleRiskInsightsConstants.LOG_SERVICE_COMPUTE)
                .details(Map.of(
                        AccessRoleConstants.LOG_DETAIL_TENANT_ID, tenantId,
                        AccessRoleConstants.LOG_DETAIL_ROLE_ID, roleId.toString()
                ))
                .log();

        AccessRole role = accessRoleRepository.findById(roleId)
                .filter(r -> tenantId != null && tenantId.equals(r.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(AccessRoleRiskInsightsConstants.ERROR_ROLE_NOT_FOUND, roleId)));

        long highRiskCount = closureRepository.countDistinctEntitlementsByRiskLevel(
                tenantId, roleId,
                AccessRoleRiskInsightsConstants.RISK_LEVEL_HIGH, maxDepth);

        long adminCount = closureRepository.countDistinctEntitlementsByPrivilegeLevel(
                tenantId, roleId,
                AccessRoleRiskInsightsConstants.PRIVILEGE_LEVEL_ADMIN, maxDepth);

        List<UUID> overlapIds = closureRepository.findOverlapEntitlementIds(
                tenantId, roleId, maxDepth);
        long overLapConflicts = overlapIds.size();

        List<EntitlementDepthCountDto> depthCounts;
        int observedMaxDepth;
        if (overlapIds.isEmpty()) {
            depthCounts = Collections.emptyList();
            observedMaxDepth = 0;
        } else {
            List<Object[]> rows = closureRepository.countOverlapDepthDistribution(
                    tenantId, roleId, overlapIds, maxDepth);
            depthCounts = rows.stream()
                    .map(r -> new EntitlementDepthCountDto(
                            ((Number) r[0]).intValue(),
                            ((Number) r[1]).longValue()))
                    .toList();
            Integer max = closureRepository.findMaxOverlapDepth(
                    tenantId, roleId, overlapIds, maxDepth);
            observedMaxDepth = max == null ? 0 : max;
        }

        long riskScore = computeRiskScore(highRiskCount, adminCount, overLapConflicts,
                closureRepository.countDistinctEntitlementsByTenantIdAndRoleIdAndMaxDepth(
                        tenantId, roleId, maxDepth));

        AccessRoleRiskInsightsResponse response = new AccessRoleRiskInsightsResponse();
        response.setRoleId(role.getId());
        response.setRoleName(role.getName());
        response.setMaxDepth(observedMaxDepth);
        response.setDepthCounts(depthCounts);
        response.setHighRiskEntitlementsCount(highRiskCount);
        response.setAdminPrivilegeEntitlementsCount(adminCount);
        response.setOverLapConflicts(overLapConflicts);
        response.setRiskScore(riskScore);
        response.setComputedAt(Instant.now());
        return response;
    }

    /**
     * Weighted percentage risk score (0–100).
     * <pre>
     * riskScore = round(
     *   (highRiskCount * 3 + adminCount * 2 + overlapCount * 1)
     *   / max(1, totalEntitlements * 3)
     *   * 100
     * )
     * </pre>
     */
    private static long computeRiskScore(long highRiskCount, long adminCount,
                                         long overlapCount, long totalEntitlements) {
        long numerator = highRiskCount * 3 + adminCount * 2 + overlapCount;
        long denominator = Math.max(1, totalEntitlements * 3);
        return Math.min(100, Math.round((double) numerator / denominator * 100));
    }
}

