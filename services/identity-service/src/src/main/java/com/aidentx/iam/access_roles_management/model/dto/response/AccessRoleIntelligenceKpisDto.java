package com.aidentx.iam.access_roles_management.model.dto.response;

/**
 * DTO representing KPI values for the Access Role Intelligence tab.
 */
public class AccessRoleIntelligenceKpisDto {

    /**
     * Total number of roles for the tenant.
     */
    private long totalRoles;

    /**
     * Total number of active roles for the tenant.
     */
    private long activeRoles;

    /**
     * Total number of disabled roles for the tenant.
     */
    private long disabledRoles;

    /**
     * Total number of orphaned roles for the tenant.
     *
     * <p>Currently mocked as zero until orphan detection is implemented.</p>
     */
    private long orphanedRoles;

    /**
     * Average number of entitlements attached per role.
     */
    private double averageEntitlementsPerRole;

    /**
     * Total number of high-risk roles for the tenant.
     */
    private long highRiskRoles;

    public long getTotalRoles() {
        return totalRoles;
    }

    public void setTotalRoles(long totalRoles) {
        this.totalRoles = totalRoles;
    }

    public long getActiveRoles() {
        return activeRoles;
    }

    public void setActiveRoles(long activeRoles) {
        this.activeRoles = activeRoles;
    }

    public long getDisabledRoles() {
        return disabledRoles;
    }

    public void setDisabledRoles(long disabledRoles) {
        this.disabledRoles = disabledRoles;
    }

    public long getOrphanedRoles() {
        return orphanedRoles;
    }

    public void setOrphanedRoles(long orphanedRoles) {
        this.orphanedRoles = orphanedRoles;
    }

    public double getAverageEntitlementsPerRole() {
        return averageEntitlementsPerRole;
    }

    public void setAverageEntitlementsPerRole(double averageEntitlementsPerRole) {
        this.averageEntitlementsPerRole = averageEntitlementsPerRole;
    }

    public long getHighRiskRoles() {
        return highRiskRoles;
    }

    public void setHighRiskRoles(long highRiskRoles) {
        this.highRiskRoles = highRiskRoles;
    }
}