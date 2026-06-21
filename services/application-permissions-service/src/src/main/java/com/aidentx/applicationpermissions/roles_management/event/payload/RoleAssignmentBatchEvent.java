package com.aidentx.applicationpermissions.roles_management.event.payload;

import java.util.List;
import java.util.UUID;



public class RoleAssignmentBatchEvent {

    private UUID batchId;
    private String tenantId;
    private List<UUID> roleIds;
    private List<UUID> userIds;
    private String assignmentType;
    private Integer validityDays;
    private String justification;

    public RoleAssignmentBatchEvent() {
        // Default constructor
    }

    public RoleAssignmentBatchEvent(UUID batchId, String tenantId, List<UUID> roleIds, List<UUID> userIds,
                                   String assignmentType, Integer validityDays, String justification) {
        this.batchId = batchId;
        this.tenantId = tenantId;
        this.roleIds = roleIds;
        this.userIds = userIds;
        this.assignmentType = assignmentType;
        this.validityDays = validityDays;
        this.justification = justification;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Integer getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(Integer validityDays) {
        this.validityDays = validityDays;
    }

    public String getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }

    public List<UUID> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<UUID> userIds) {
        this.userIds = userIds;
    }

    public List<UUID> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<UUID> roleIds) {
        this.roleIds = roleIds;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }


}
