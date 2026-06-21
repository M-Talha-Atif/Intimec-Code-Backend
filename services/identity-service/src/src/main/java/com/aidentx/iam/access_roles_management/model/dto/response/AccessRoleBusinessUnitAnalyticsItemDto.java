package com.aidentx.iam.access_roles_management.model.dto.response;

/**
 * DTO representing business-unit analytics for access roles.
 */
public class AccessRoleBusinessUnitAnalyticsItemDto {

    /**
     * Business unit label.
     */
    private String businessUnit;

    /**
     * Number of roles in the business unit.
     */
    private long roles;

    /**
     * Number of users in the business unit.
     *
     * <p>Currently mocked as zero until role-user assignment analytics are implemented.</p>
     */
    private long users;

    public AccessRoleBusinessUnitAnalyticsItemDto() {
    }

    public AccessRoleBusinessUnitAnalyticsItemDto(String businessUnit, long roles, long users) {
        this.businessUnit = businessUnit;
        this.roles = roles;
        this.users = users;
    }

    public String getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    public long getRoles() {
        return roles;
    }

    public void setRoles(long roles) {
        this.roles = roles;
    }

    public long getUsers() {
        return users;
    }

    public void setUsers(long users) {
        this.users = users;
    }
}