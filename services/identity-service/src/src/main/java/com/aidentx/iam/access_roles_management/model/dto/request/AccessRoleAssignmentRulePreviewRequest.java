package com.aidentx.iam.access_roles_management.model.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for read-only access-role assignment-rule preview/dry-run.
 */
public class AccessRoleAssignmentRulePreviewRequest {

    @NotNull
    private UUID roleId;
    private UUID ruleId;
    private String expression;
    private List<AccessRoleAssignmentRuleConditionGroupDto> conditionGroups;
    private List<String> identityTypes;
    private Boolean includeEntitlements = true;
    private Integer page = 0;
    private Integer size = 20;

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public List<AccessRoleAssignmentRuleConditionGroupDto> getConditionGroups() { return conditionGroups; }
    public void setConditionGroups(List<AccessRoleAssignmentRuleConditionGroupDto> conditionGroups) { this.conditionGroups = conditionGroups; }

    public List<String> getIdentityTypes() { return identityTypes; }
    public void setIdentityTypes(List<String> identityTypes) { this.identityTypes = identityTypes; }

    public Boolean getIncludeEntitlements() { return includeEntitlements; }
    public void setIncludeEntitlements(Boolean includeEntitlements) { this.includeEntitlements = includeEntitlements; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}

