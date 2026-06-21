package com.aidentx.iam.entitlement_management.model.dto;

import java.util.List;

/**
 * DTO representing a single condition within a condition group.
 *
 * <p>Each condition specifies a field-operator-value comparison against
 * an identity attribute. Conditions within a group are combined using
 * the group's {@code groupOperator} — individual conditions do not
 * carry their own logical operator or negation.
 *
 * <p>For {@code IN} and {@code NOT_IN} operators, use the {@code values}
 * list instead of the single {@code value} field.
 *
 * <p>Field names are resolved by {@link com.aidentx.iam.entitlement_management.service.IdentityFieldRegistry}
 * to their exact JPA entity attribute names (case-insensitive matching on input).
 */
public class RuleConditionDto {

    /**
     * The identity attribute field name to evaluate.
     *
     * <p>Accepts case-insensitive input. The service normalizes it to
     * the exact camelCase attribute name on the JPA entity.
     *
     * <p>Examples: {@code "department"}, {@code "jobTitle"}, {@code "email"}.
     */
    private String field;

    /**
     * The comparison operator.
     *
     * <p>Supported values: {@code EQUALS}, {@code NOT_EQUALS}, {@code CONTAINS},
     * {@code STARTS_WITH}, {@code ENDS_WITH}, {@code IN}, {@code NOT_IN}.
     *
     * <p>Normalized to uppercase by the service layer.
     */
    private String operator;

    /**
     * The comparison value for single-value operators.
     *
     * <p>Used with: {@code EQUALS}, {@code NOT_EQUALS}, {@code CONTAINS},
     * {@code STARTS_WITH}, {@code ENDS_WITH}.
     *
     * <p>For {@code IN} / {@code NOT_IN}, use {@code values} instead.
     * If both are provided, {@code values} takes precedence.
     */
    private String value;

    /**
     * The list of comparison values for multi-value operators.
     *
     * <p>Used with: {@code IN}, {@code NOT_IN}.
     *
     * <p>If a comma-separated string is provided in {@code value} instead,
     * the service will split it into a list automatically.
     */
    private List<String> values;

    public RuleConditionDto() {}

    public RuleConditionDto(String field, String operator, String value, List<String> values) {
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