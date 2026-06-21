package com.aidentx.iam.access_roles_management.model.dto.request;

import java.util.List;

/**
 * DTO representing a single identity-attribute condition inside an access-role
 * assignment-rule condition group.
 *
 * <p>Conditions are grouped by access-role assignment-rule condition groups;
 * individual conditions do not carry their own logical operator.</p>
 */
public class AccessRoleAssignmentRuleConditionDto {

    /** Identity attribute field name, resolved case-insensitively by the service layer. */
    private String field;

    /** Comparison operator such as EQUALS, NOT_EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH, IN, or NOT_IN. */
    private String operator;

    /** Single comparison value for single-value operators. */
    private String value;

    /** Multi-value comparison list for IN and NOT_IN operators. */
    private List<String> values;

    public AccessRoleAssignmentRuleConditionDto() {
    }

    public AccessRoleAssignmentRuleConditionDto(String field, String operator, String value, List<String> values) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.values = values;
    }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public List<String> getValues() { return values; }
    public void setValues(List<String> values) { this.values = values; }
}

