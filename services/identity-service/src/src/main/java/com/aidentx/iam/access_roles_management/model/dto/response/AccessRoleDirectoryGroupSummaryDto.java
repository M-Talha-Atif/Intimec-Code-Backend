package com.aidentx.iam.access_roles_management.model.dto.response;

/**
 * Summary DTO representing a single role-type group inside a business unit
 * for the role directory navigator.
 */
public class AccessRoleDirectoryGroupSummaryDto {

    /**
     * Stored role type key, for example: functional, technical, application.
     */
    private String groupKey;

    /**
     * Number of roles in this group.
     */
    private long count;

    public AccessRoleDirectoryGroupSummaryDto() {
    }

    public AccessRoleDirectoryGroupSummaryDto(String groupKey, long count) {
        this.groupKey = groupKey;
        this.count = count;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}