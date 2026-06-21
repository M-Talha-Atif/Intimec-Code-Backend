package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level response DTO for the access-role directory summary endpoint.
 *
 * <p>Contains only business units, role-type buckets, and counts.
 * This payload is intended for lightweight navigator rendering and does not
 * include full role rows.</p>
 */
public class AccessRoleDirectorySummaryResponseDto {

    /**
     * Total number of roles across all business units for the tenant.
     */
    private long totalRoles;

    /**
     * Business-unit level summaries.
     */
    private List<AccessRoleDirectoryBusinessUnitSummaryDto> businessUnits = new ArrayList<>();

    public AccessRoleDirectorySummaryResponseDto() {
    }

    public AccessRoleDirectorySummaryResponseDto(
            long totalRoles,
            List<AccessRoleDirectoryBusinessUnitSummaryDto> businessUnits
    ) {
        this.totalRoles = totalRoles;
        this.businessUnits = businessUnits;
    }

    public long getTotalRoles() {
        return totalRoles;
    }

    public void setTotalRoles(long totalRoles) {
        this.totalRoles = totalRoles;
    }

    public List<AccessRoleDirectoryBusinessUnitSummaryDto> getBusinessUnits() {
        return businessUnits;
    }

    public void setBusinessUnits(List<AccessRoleDirectoryBusinessUnitSummaryDto> businessUnits) {
        this.businessUnits = businessUnits;
    }
}