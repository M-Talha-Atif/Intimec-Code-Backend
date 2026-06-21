package com.aidentx.applicationpermissions.roles_management.service;

import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;

/**
 * Service for evaluating conditional rules against user data.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public interface ConditionalRuleEvaluationService {
    
    /**
     * Evaluates a conditional script against user data.
     * 
     * @param conditionScript the condition script to evaluate (e.g., "user.email == 'test@example.com'")
     * @param user the user data to evaluate against
     * @return true if the condition evaluates to true, false otherwise
     */
    boolean evaluateCondition(String conditionScript, UserDto user);
    
    /**
     * Validates if a conditional script is syntactically correct.
     * 
     * @param conditionScript the condition script to validate
     * @return true if the script is valid, false otherwise
     */
    boolean isValidConditionScript(String conditionScript);
}