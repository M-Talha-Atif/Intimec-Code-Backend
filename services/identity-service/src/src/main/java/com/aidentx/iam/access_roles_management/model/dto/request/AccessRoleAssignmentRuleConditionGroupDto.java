package com.aidentx.iam.access_roles_management.model.dto.request;

import java.util.List;

/**
 * DTO representing a grouped condition block for access-role assignment rules.
 *
 * <p>Groups support recursive nesting through {@code conditionGroups}. A group
 * may contain leaf {@code conditions}, nested {@code conditionGroups}, or both.
 * At least one must be supplied.</p>
 */
public class AccessRoleAssignmentRuleConditionGroupDto {

    /** Combines conditions and nested groups within this group. Defaults to AND. */
    private String groupOperator;

    /** Connects this group to the next sibling group. Defaults to AND. */
    private String interGroupOperator;

    /** When true, the whole group result is wrapped in NOT(...). Defaults to false. */
    private Boolean negated;

    /** Leaf identity-attribute conditions within this group. */
    private List<AccessRoleAssignmentRuleConditionDto> conditions;

    /** Nested child condition groups evaluated recursively. */
    private List<AccessRoleAssignmentRuleConditionGroupDto> conditionGroups;

    public AccessRoleAssignmentRuleConditionGroupDto() {
    }

    public AccessRoleAssignmentRuleConditionGroupDto(String groupOperator,
                                                     String interGroupOperator,
                                                     Boolean negated,
                                                     List<AccessRoleAssignmentRuleConditionDto> conditions,
                                                     List<AccessRoleAssignmentRuleConditionGroupDto> conditionGroups) {
        this.groupOperator = groupOperator;
        this.interGroupOperator = interGroupOperator;
        this.negated = negated;
        this.conditions = conditions;
        this.conditionGroups = conditionGroups;
    }

    public String getGroupOperator() { return groupOperator; }
    public void setGroupOperator(String groupOperator) { this.groupOperator = groupOperator; }

    public String getInterGroupOperator() { return interGroupOperator; }
    public void setInterGroupOperator(String interGroupOperator) { this.interGroupOperator = interGroupOperator; }

    public Boolean getNegated() { return negated; }
    public void setNegated(Boolean negated) { this.negated = negated; }

    public List<AccessRoleAssignmentRuleConditionDto> getConditions() { return conditions; }
    public void setConditions(List<AccessRoleAssignmentRuleConditionDto> conditions) { this.conditions = conditions; }

    public List<AccessRoleAssignmentRuleConditionGroupDto> getConditionGroups() { return conditionGroups; }
    public void setConditionGroups(List<AccessRoleAssignmentRuleConditionGroupDto> conditionGroups) { this.conditionGroups = conditionGroups; }
}

