package com.aidentx.iam.entitlement_management.model.enums;

import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;

/**
 * Supported operators for rule condition evaluation.
 *
 * <p>These operators are applied against Identity field values
 * during dynamic entitlement assignment rule evaluation.
 *
 * <p>All string comparisons are case-insensitive (UPPER normalization).
 *
 * <ul>
 *   <li><strong>EQUALS / NOT_EQUALS</strong> — Exact match (case-insensitive)</li>
 *   <li><strong>CONTAINS</strong> — Substring match anywhere in the value</li>
 *   <li><strong>STARTS_WITH / ENDS_WITH</strong> — Prefix/suffix match</li>
 *   <li><strong>IN / NOT_IN</strong> — Value is (or is not) in a provided list</li>
 * </ul>
 */
public enum RuleOperator {

    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    IN,
    NOT_IN;

    /**
     * Parse operator from string (case-insensitive).
     *
     * @param value the operator string
     * @return the matching RuleOperator enum value
     * @throws IllegalArgumentException if value is null, blank, or not a valid operator
     */
    public static RuleOperator fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(RuleEvaluationConstants.ERROR_OPERATOR_BLANK);
        }
        return valueOf(value.trim().toUpperCase());
    }
}