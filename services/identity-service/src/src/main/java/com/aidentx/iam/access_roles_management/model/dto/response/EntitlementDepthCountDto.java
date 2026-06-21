package com.aidentx.iam.access_roles_management.model.dto.response;

/**
 * Per-depth count of overlapping entitlements in a role's closure.
 */
public class EntitlementDepthCountDto {

    private int depth;
    private long entitlementCount;

    public EntitlementDepthCountDto() {
    }

    public EntitlementDepthCountDto(int depth, long entitlementCount) {
        this.depth = depth;
        this.entitlementCount = entitlementCount;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public long getEntitlementCount() {
        return entitlementCount;
    }

    public void setEntitlementCount(long entitlementCount) {
        this.entitlementCount = entitlementCount;
    }
}

