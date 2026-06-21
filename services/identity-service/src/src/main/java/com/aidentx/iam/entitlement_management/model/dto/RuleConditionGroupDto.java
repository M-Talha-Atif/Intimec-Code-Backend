package com.aidentx.iam.entitlement_management.model.dto;

import java.util.List;

/**
 * DTO representing a group of conditions in a dynamic entitlement assignment rule.
 *
 * <p>Supports recursive nesting: a group can contain sub-groups via {@code conditionGroups}.
 * A group may have conditions, sub-groups, or both. At least one must be present.
 *
 * <p>Two operators per group:
 * <ul>
 *   <li>{@code groupOperator} — combines conditions and sub-groups <b>within</b> this group (AND/OR)</li>
 *   <li>{@code interGroupOperator} — connects this group to the <b>next sibling</b> group (AND/OR)</li>
 * </ul>
 *
 * <p>The {@code negated} flag wraps the entire group's combined result in {@code NOT(...)}.
 *
 * <p><b>Nested example:</b>
 * <pre>
 *   {
 *     "groupOperator": "AND",
 *     "interGroupOperator": "AND",
 *     "negated": false,
 *     "conditions": [
 *       { "field": "department", "operator": "EQUALS", "value": "Engineering" }
 *     ],
 *     "conditionGroups": [
 *       {
 *         "groupOperator": "OR",
 *         "negated": false,
 *         "conditions": [
 *           { "field": "email", "operator": "ENDS_WITH", "value": "@nyc.company.com" },
 *           { "field": "email", "operator": "ENDS_WITH", "value": "@london.company.com" }
 *         ]
 *       }
 *     ]
 *   }
 *
 *   Evaluates as: (department=Engineering AND (email=@nyc OR email=@london))
 * </pre>
 */
public class RuleConditionGroupDto {

    /**
     * Combines conditions and sub-groups <b>within</b> this group (AND/OR).
     * Defaults to "AND" if null or blank.
     */
    private String groupOperator;

    /**
     * Connects this group to the <b>next sibling</b> group (AND/OR).
     * Defaults to "AND" if null or blank.
     * Ignored for the last group in a sibling list.
     */
    private String interGroupOperator;

    /**
     * When true, wraps the entire group's result in NOT(...).
     * Defaults to false if null.
     */
    private Boolean negated;

    /**
     * Leaf conditions within this group.
     * Combined using {@code groupOperator}.
     * May be null/empty if {@code conditionGroups} is provided.
     */
    private List<RuleConditionDto> conditions;

    /**
     * Nested sub-groups within this group.
     * Each sub-group is evaluated recursively and its result is combined
     * with this group's conditions using {@code groupOperator}.
     * May be null/empty if {@code conditions} is provided.
     */
    private List<RuleConditionGroupDto> conditionGroups;

    public RuleConditionGroupDto() {}

    public RuleConditionGroupDto(String groupOperator, String interGroupOperator,
                                  Boolean negated, List<RuleConditionDto> conditions,
                                  List<RuleConditionGroupDto> conditionGroups) {
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

    public List<RuleConditionDto> getConditions() { return conditions; }
    public void setConditions(List<RuleConditionDto> conditions) { this.conditions = conditions; }

    public List<RuleConditionGroupDto> getConditionGroups() { return conditionGroups; }
    public void setConditionGroups(List<RuleConditionGroupDto> conditionGroups) { this.conditionGroups = conditionGroups; }
}