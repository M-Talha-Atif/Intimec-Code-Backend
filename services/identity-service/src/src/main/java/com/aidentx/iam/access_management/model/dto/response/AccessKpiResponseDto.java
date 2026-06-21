package com.aidentx.iam.access_management.model.dto.response;

/**
 * Cross-domain access KPI response payload.
 *
 * <p>The payload aggregates governance metrics spanning both direct entitlement
 * assignments and access-role assignments.</p>
 */
public class AccessKpiResponseDto {

    private AccessKpiMetricDto roleCoverageRatio;
    private AccessKpiMetricDto directAccessExposure;
    private AccessKpiMetricDto roleComplexityIndex;
    private AccessKpiMetricDto orphanedAccessObjects;
    private AccessKpiMetricDto accessReuseScore;

    public AccessKpiMetricDto getRoleCoverageRatio() {
        return roleCoverageRatio;
    }

    public void setRoleCoverageRatio(AccessKpiMetricDto roleCoverageRatio) {
        this.roleCoverageRatio = roleCoverageRatio;
    }

    public AccessKpiMetricDto getDirectAccessExposure() {
        return directAccessExposure;
    }

    public void setDirectAccessExposure(AccessKpiMetricDto directAccessExposure) {
        this.directAccessExposure = directAccessExposure;
    }

    public AccessKpiMetricDto getRoleComplexityIndex() {
        return roleComplexityIndex;
    }

    public void setRoleComplexityIndex(AccessKpiMetricDto roleComplexityIndex) {
        this.roleComplexityIndex = roleComplexityIndex;
    }

    public AccessKpiMetricDto getOrphanedAccessObjects() {
        return orphanedAccessObjects;
    }

    public void setOrphanedAccessObjects(AccessKpiMetricDto orphanedAccessObjects) {
        this.orphanedAccessObjects = orphanedAccessObjects;
    }

    public AccessKpiMetricDto getAccessReuseScore() {
        return accessReuseScore;
    }

    public void setAccessReuseScore(AccessKpiMetricDto accessReuseScore) {
        this.accessReuseScore = accessReuseScore;
    }
}
