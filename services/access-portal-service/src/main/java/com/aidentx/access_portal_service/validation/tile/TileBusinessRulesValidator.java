package com.aidentx.access_portal_service.validation.tile;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.request.tile.TileVisualFilterRuleDTO;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validator for tile business rules and filter validations
 */
@Component
public class TileBusinessRulesValidator {

    private final IUnifiedLogger logger;

    /**
     * Constructor with logger factory injection
     */
    @Autowired
    public TileBusinessRulesValidator(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(
                AccessPortalConstants.TILE_BUSINESS_RULES_VALIDATOR_LOGGER_NAME,
                AccessPortalConstants.LOG_CATEGORY_BUSINESS_RULES
        );
    }

    /**
     * Validates visual filter rules
     *
     * @param rules The list of visual filter rules to validate
     * @throws IllegalArgumentException if rules are invalid
     */
    public void validateVisualFilterRules(List<TileVisualFilterRuleDTO> rules) {
        logger.debug()
                .message("Validating visual filter rules")
                .detail("ruleCount", rules != null ? String.valueOf(rules.size()) : "0")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VISUAL_FILTER_RULES_VALIDATION)
                .log();

        if (rules == null || rules.isEmpty()) {
            logger.trace()
                    .message("No visual filter rules to validate")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VALIDATION_SKIPPED)
                    .log();
            return;
        }

        for (int i = 0; i < rules.size(); i++) {
            TileVisualFilterRuleDTO rule = rules.get(i);

            logger.trace()
                    .message("Validating individual filter rule")
                    .detail("ruleIndex", String.valueOf(i))
                    .detail("fieldName", rule.getFieldName())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_INDIVIDUAL_RULE_VALIDATION)
                    .log();

            validateRuleFieldName(rule.getFieldName());
            validateOperator(rule.getOperator());
            validateRuleValue(rule.getValue(), rule.getOperator());

            // logical operator validation ONLY for rules after the first
            if (i > 0) {
                validateLogicalOperator(rule.getLogicalOperator());
            } else {
                // First rule MUST NOT have logical operator
                if (rule.getLogicalOperator() != null) {
                    logger.error()
                            .message("Logical operator is not allowed for the first filter rule")
                            .detail("ruleIndex", "0")
                            .detail("logicalOperator", rule.getLogicalOperator())
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_FIRST_RULE_VALIDATION_ERROR)
                            .log();
                    throw new IllegalArgumentException(
                            "Logical operator is not allowed for the first filter rule"
                    );
                }
            }

            logger.trace()
                    .message("Individual rule validation passed")
                    .detail("ruleIndex", String.valueOf(i))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_INDIVIDUAL_RULE_VALIDATION_SUCCESS)
                    .log();
        }

        logger.info()
                .message("Visual filter rules validation completed successfully")
                .detail("totalRules", String.valueOf(rules.size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VISUAL_FILTER_RULES_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Validates field name for filter rules
     *
     * @param fieldName The field name to validate
     * @throws IllegalArgumentException if field name is invalid
     */
    private void validateRuleFieldName(String fieldName) {
        logger.trace()
                .message("Validating rule field name")
                .detail("fieldName", fieldName != null ? fieldName : "null")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_FIELD_NAME_VALIDATION)
                .log();

        if (fieldName == null || fieldName.trim().isEmpty()) {
            logger.error()
                    .message("Field name is required for filter rules")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_FIELD_NAME_VALIDATION_ERROR)
                    .log();
            throw new IllegalArgumentException("Field name is required for filter rules");
        }

        String[] allowedFields = AccessPortalConstants.ALLOWED_FILTER_FIELDS;
        boolean isValid = false;
        for (String allowedField : allowedFields) {
            if (allowedField.equalsIgnoreCase(fieldName.trim())) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            logger.error()
                    .message("Invalid field name for filter rule")
                    .detail("fieldName", fieldName)
                    .detail("allowedFields", String.join(", ", allowedFields))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_INVALID_FIELD_NAME)
                    .log();
            throw new IllegalArgumentException(
                    "Invalid field name. Allowed fields: " + String.join(", ", allowedFields)
            );
        }

        logger.trace()
                .message("Field name validation passed")
                .detail("fieldName", fieldName)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_FIELD_NAME_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Validates logical operator
     *
     * @param logicalOperator The logical operator to validate
     * @throws IllegalArgumentException if logical operator is invalid
     */
    private void validateLogicalOperator(String logicalOperator) {
        logger.trace()
                .message("Validating logical operator")
                .detail("logicalOperator", logicalOperator != null ? logicalOperator : "null")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_LOGICAL_OPERATOR_VALIDATION)
                .log();

        if (logicalOperator == null || logicalOperator.trim().isEmpty()) {
            logger.error()
                    .message("Logical operator is required for filter rules")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_LOGICAL_OPERATOR_VALIDATION_ERROR)
                    .log();
            throw new IllegalArgumentException("Logical operator is required for filter rules");
        }

        String[] allowedOperators = AccessPortalConstants.LOGICAL_OPERATORS;
        boolean isValid = false;

        for (String allowedOp : allowedOperators) {
            if (allowedOp.equalsIgnoreCase(logicalOperator.trim())) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            logger.error()
                    .message("Invalid logical operator")
                    .detail("logicalOperator", logicalOperator)
                    .detail("allowedOperators", String.join(", ", allowedOperators))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_INVALID_LOGICAL_OPERATOR)
                    .log();
            throw new IllegalArgumentException(
                    "Invalid logical operator. Must be one of: " + String.join(", ", allowedOperators)
            );
        }

        logger.trace()
                .message("Logical operator validation passed")
                .detail("logicalOperator", logicalOperator)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_LOGICAL_OPERATOR_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Validates comparison operator
     *
     * @param operator The operator to validate
     * @throws IllegalArgumentException if operator is invalid
     */
    private void validateOperator(String operator) {
        logger.trace()
                .message("Validating comparison operator")
                .detail("operator", operator != null ? operator : "null")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_COMPARISON_OPERATOR_VALIDATION)
                .log();

        if (operator == null || operator.trim().isEmpty()) {
            logger.error()
                    .message("Operator is required for filter rules")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_COMPARISON_OPERATOR_VALIDATION_ERROR)
                    .log();
            throw new IllegalArgumentException("Operator is required for filter rules");
        }

        String[] allowedOperators = AccessPortalConstants.COMPARISON_OPERATORS;
        boolean isValid = false;

        for (String allowedOp : allowedOperators) {
            if (allowedOp.equalsIgnoreCase(operator.trim())) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            logger.error()
                    .message("Invalid comparison operator")
                    .detail("operator", operator)
                    .detail("allowedOperators", String.join(", ", allowedOperators))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_INVALID_COMPARISON_OPERATOR)
                    .log();
            throw new IllegalArgumentException(
                    "Invalid operator. Must be one of: " + String.join(", ", allowedOperators)
            );
        }

        logger.trace()
                .message("Comparison operator validation passed")
                .detail("operator", operator)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_COMPARISON_OPERATOR_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Validates rule value based on operator
     *
     * @param value The value to validate
     * @param operator The operator used
     * @throws IllegalArgumentException if value is invalid
     */
    private void validateRuleValue(String value, String operator) {
        logger.trace()
                .message("Validating rule value")
                .detail("value", value != null ? value : "null")
                .detail("operator", operator)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_RULE_VALUE_VALIDATION)
                .log();

        if (value == null || value.trim().isEmpty()) {
            logger.error()
                    .message("Value is required for filter rules")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_RULE_VALUE_VALIDATION_ERROR)
                    .log();
            throw new IllegalArgumentException("Value is required for filter rules");
        }

        // For IN and NOT_IN operators, value should be a comma-separated list or JSON array
        if ("IN".equalsIgnoreCase(operator) || "NOT_IN".equalsIgnoreCase(operator)) {
            // Basic validation for list format
            if (!value.contains(",") && !value.startsWith("[") && !value.endsWith("]")) {
                logger.error()
                        .message("IN/NOT_IN operator requires comma-separated list or JSON array")
                        .detail("operator", operator)
                        .detail("value", value)
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_INVALID_LIST_VALUE)
                        .log();
                throw new IllegalArgumentException(
                        "For IN and NOT_IN operators, value must be a comma-separated list or JSON array"
                );
            }
        }

        // Add additional validations based on your business rules
        if (value.length() > AccessPortalConstants.MAX_FILTER_VALUE_LENGTH) {
            logger.error()
                    .message("Filter rule value exceeds maximum length")
                    .detail("valueLength", String.valueOf(value.length()))
                    .detail("maxLength", String.valueOf(AccessPortalConstants.MAX_FILTER_VALUE_LENGTH))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VALUE_LENGTH_EXCEEDED)
                    .log();
            throw new IllegalArgumentException(
                    "Value cannot exceed " + AccessPortalConstants.MAX_FILTER_VALUE_LENGTH + " characters"
            );
        }

        logger.trace()
                .message("Rule value validation passed")
                .detail("value", value)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_RULE_VALUE_VALIDATION_SUCCESS)
                .log();
    }
}