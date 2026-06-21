package com.aidentx.iam.access_roles_management.model.dto.response;

/**
 * DTO representing the count of roles grouped by role type.
 */
public class AccessRoleTypeDistributionDto {

    /**
     * Role type label.
     */
    private String type;

    /**
     * Number of roles for the given type.
     */
    private long count;

    public AccessRoleTypeDistributionDto() {
    }

    public AccessRoleTypeDistributionDto(String type, long count) {
        this.type = type;
        this.count = count;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}