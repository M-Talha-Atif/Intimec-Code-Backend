package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Top-level read-only preview/dry-run response for an access-role assignment rule.
 */
public class AccessRoleAssignmentRulePreviewResponse {

    private boolean valid;
    private String validationMessage;
    private UUID roleId;
    private String roleName;
    private UUID ruleId;
    private List<Map<String, Object>> normalizedConditionGroups;
    private List<String> identityTypes;
    private long totalMatched;
    private long totalAffected;
    private long willBeAdded;
    private long willBeRemoved;
    private long alreadyAssigned;
    private int page;
    private int size;
    private long totalPreviewRows;
    private List<AccessRoleAssignmentRulePreviewIdentityResponse> previewPopulation;
    private RoleEntitlementPreviewSummaryResponse entitlementSummary;

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public List<Map<String, Object>> getNormalizedConditionGroups() { return normalizedConditionGroups; }
    public void setNormalizedConditionGroups(List<Map<String, Object>> normalizedConditionGroups) { this.normalizedConditionGroups = normalizedConditionGroups; }

    public List<String> getIdentityTypes() { return identityTypes; }
    public void setIdentityTypes(List<String> identityTypes) { this.identityTypes = identityTypes; }

    public long getTotalMatched() { return totalMatched; }
    public void setTotalMatched(long totalMatched) { this.totalMatched = totalMatched; }

    public long getTotalAffected() { return totalAffected; }
    public void setTotalAffected(long totalAffected) { this.totalAffected = totalAffected; }

    public long getWillBeAdded() { return willBeAdded; }
    public void setWillBeAdded(long willBeAdded) { this.willBeAdded = willBeAdded; }

    public long getWillBeRemoved() { return willBeRemoved; }
    public void setWillBeRemoved(long willBeRemoved) { this.willBeRemoved = willBeRemoved; }

    public long getAlreadyAssigned() { return alreadyAssigned; }
    public void setAlreadyAssigned(long alreadyAssigned) { this.alreadyAssigned = alreadyAssigned; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalPreviewRows() { return totalPreviewRows; }
    public void setTotalPreviewRows(long totalPreviewRows) { this.totalPreviewRows = totalPreviewRows; }

    public List<AccessRoleAssignmentRulePreviewIdentityResponse> getPreviewPopulation() { return previewPopulation; }
    public void setPreviewPopulation(List<AccessRoleAssignmentRulePreviewIdentityResponse> previewPopulation) { this.previewPopulation = previewPopulation; }

    public RoleEntitlementPreviewSummaryResponse getEntitlementSummary() { return entitlementSummary; }
    public void setEntitlementSummary(RoleEntitlementPreviewSummaryResponse entitlementSummary) { this.entitlementSummary = entitlementSummary; }
}

