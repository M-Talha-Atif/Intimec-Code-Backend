package com.aidentx.iam.access_roles_management.model.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Risk insights for a single access role.
 *
 * <p>Counts are computed across the role's effective entitlement closure up to
 * the configured maximum depth. {@code overLapConflicts} counts distinct
 * entitlements that are contributed by more than one source role within this
 * role's own descendant tree. {@code depthCounts} buckets those overlapping
 * entitlements by their closure depth. {@code riskScore} is a simple sum of
 * the three count fields.</p>
 */
public class AccessRoleRiskInsightsResponse {

    private UUID roleId;
    private String roleName;
    private int maxDepth;
    private List<EntitlementDepthCountDto> depthCounts;
    private long highRiskEntitlementsCount;
    private long adminPrivilegeEntitlementsCount;
    private long overLapConflicts;
    private long riskScore;
    private Instant computedAt;

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public List<EntitlementDepthCountDto> getDepthCounts() {
        return depthCounts;
    }

    public void setDepthCounts(List<EntitlementDepthCountDto> depthCounts) {
        this.depthCounts = depthCounts;
    }

    public long getHighRiskEntitlementsCount() {
        return highRiskEntitlementsCount;
    }

    public void setHighRiskEntitlementsCount(long highRiskEntitlementsCount) {
        this.highRiskEntitlementsCount = highRiskEntitlementsCount;
    }

    public long getAdminPrivilegeEntitlementsCount() {
        return adminPrivilegeEntitlementsCount;
    }

    public void setAdminPrivilegeEntitlementsCount(long adminPrivilegeEntitlementsCount) {
        this.adminPrivilegeEntitlementsCount = adminPrivilegeEntitlementsCount;
    }

    public long getOverLapConflicts() {
        return overLapConflicts;
    }

    public void setOverLapConflicts(long overLapConflicts) {
        this.overLapConflicts = overLapConflicts;
    }

    public long getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(long riskScore) {
        this.riskScore = riskScore;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }
}

