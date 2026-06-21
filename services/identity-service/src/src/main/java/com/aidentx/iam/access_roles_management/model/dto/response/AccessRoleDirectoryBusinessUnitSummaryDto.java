package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary DTO representing a business-unit section in the role directory navigator.
 */
public class AccessRoleDirectoryBusinessUnitSummaryDto {

    /**
     * Business unit label.
     */
    private String businessUnit;

    /**
     * Total number of roles in this business unit.
     */
    private long totalRoles;

    /**
     * Type-group summaries inside this business unit.
     */
    private List<AccessRoleDirectoryGroupSummaryDto> groups = new ArrayList<>();

    public AccessRoleDirectoryBusinessUnitSummaryDto() {
    }

    public AccessRoleDirectoryBusinessUnitSummaryDto(
            String businessUnit,
            long totalRoles,
            List<AccessRoleDirectoryGroupSummaryDto> groups
    ) {
        this.businessUnit = businessUnit;
        this.totalRoles = totalRoles;
        this.groups = groups;
    }

    public String getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    public long getTotalRoles() {
        return totalRoles;
    }

    public void setTotalRoles(long totalRoles) {
        this.totalRoles = totalRoles;
    }

    public List<AccessRoleDirectoryGroupSummaryDto> getGroups() {
        return groups;
    }

    public void setGroups(List<AccessRoleDirectoryGroupSummaryDto> groups) {
        this.groups = groups;
    }
}