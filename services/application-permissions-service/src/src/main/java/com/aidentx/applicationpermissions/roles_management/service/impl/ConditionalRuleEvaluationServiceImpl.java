package com.aidentx.applicationpermissions.roles_management.service.impl;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.service.ConditionalRuleEvaluationService;
import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

/**
 * Implementation of ConditionalRuleEvaluationService using Spring Expression Language (SpEL).
 * 
 * This service provides dynamic conditional rule evaluation against user data.
 * Scripts must reference user fields using the 'user.' prefix:
 * 
 * Examples:
 * - user.email == 'user@example.com'
 * - user.status == 'ACTIVE'
 * - user.username == 'admin'
 * - user.failedLoginAttempts < 3
 * - user.mfaEnabled == true
 * - user.tenantId == 'tenant_001'
 * - user.firstName == 'John' && user.lastName == 'Doe'
 * - user.email.endsWith('@company.com') && user.status == 'ACTIVE'
 * 
 * The system requires explicit 'user.' prefix for all user property access.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class ConditionalRuleEvaluationServiceImpl implements ConditionalRuleEvaluationService {
    
    private final IUnifiedLogger logger;
    private final ExpressionParser parser;
    
    public ConditionalRuleEvaluationServiceImpl(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(AppConstants.CONDITIONAL_RULE_EVALUATION_SERVICE_LOGGER_NAME, AppConstants.LOG_CATEGORY_CONDITIONAL_RULE_EVALUATION);
        this.parser = new SpelExpressionParser();
    }
    
    @Override
    public boolean evaluateCondition(String conditionScript, UserDto user) {
        if (conditionScript == null || conditionScript.trim().isEmpty()) {
            logger.debug()
                .message(AppConstants.LOG_NO_CONDITION_SCRIPT_PROVIDED)
                .detail(AppConstants.DETAIL_KEY_USER_ID, user.getId())
                .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                .log();
            return true; // No condition means no restriction
        }
        
        try {
            logger.info()
                .message(AppConstants.LOG_STARTING_CONDITIONAL_RULE_EVALUATION)
                .detail(AppConstants.DETAIL_KEY_CONDITION_SCRIPT, conditionScript)
                .detail(AppConstants.DETAIL_KEY_USER_ID, user.getId())
                .detail(AppConstants.DETAIL_KEY_EVALUATION_TYPE, AppConstants.EVALUATION_TYPE_DYNAMIC_SPEL)
                .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                .log();
            
            // Create evaluation context with a wrapper object that provides 'user' property
            // This allows scripts to use: user.email, user.status, user.firstName, etc.
            UserWrapper wrapper = new UserWrapper(user);
            StandardEvaluationContext context = new StandardEvaluationContext(wrapper);
            
            logger.debug()
                .message(AppConstants.LOG_CREATED_SPEL_EVALUATION_CONTEXT)
                .detail(AppConstants.DETAIL_KEY_CONDITION_SCRIPT, conditionScript)
                .detail(AppConstants.DETAIL_KEY_USER_ID, user.getId())
                .detail(AppConstants.DETAIL_KEY_CONTEXT_INFO, "User object available via 'user' property - supports user.property syntax")
                .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                .log();
            
            // Parse and evaluate the expression
            Expression expression = parser.parseExpression(conditionScript);
            Object result = expression.getValue(context);
            
            // Convert result to boolean - handle various result types
            boolean conditionMet = false;
            if (result instanceof Boolean) {
                conditionMet = (Boolean) result;
            } else if (result != null) {
                // For non-boolean results, check if they represent "truthy" values
                String resultStr = result.toString().toLowerCase();
                conditionMet = "true".equals(resultStr) || "1".equals(resultStr);
            }
            
            logger.info()
                .message(AppConstants.LOG_CONDITIONAL_RULE_EVALUATION_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_CONDITION_SCRIPT, conditionScript)
                .detail(AppConstants.DETAIL_KEY_USER_ID, user.getId())
                .detail(AppConstants.DETAIL_KEY_CONDITION_MET, String.valueOf(conditionMet))
                .detail(AppConstants.DETAIL_KEY_EVALUATION_RESULT, String.valueOf(result))
                .detail(AppConstants.DETAIL_KEY_RESULT_TYPE, result != null ? result.getClass().getSimpleName() : "null")
                .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                .log();
            
            return conditionMet;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ERROR_EVALUATING_CONDITIONAL_RULE)
                .detail(AppConstants.DETAIL_KEY_CONDITION_SCRIPT, conditionScript)
                .detail(AppConstants.DETAIL_KEY_USER_ID, user.getId())
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                .log();
            
            // On error, deny access for security
            return false;
        }
    }
    
    @Override
    public boolean isValidConditionScript(String conditionScript) {
        if (conditionScript == null || conditionScript.trim().isEmpty()) {
            return true; // Empty condition is valid
        }
        
        try {
            logger.trace()
                .message(AppConstants.LOG_VALIDATING_CONDITION_SCRIPT_SYNTAX)
                .detail(AppConstants.DETAIL_KEY_CONDITION_SCRIPT, conditionScript)
                .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                .log();
            
            // Try to parse the expression
            parser.parseExpression(conditionScript);
            
            logger.debug()
                .message(AppConstants.LOG_CONDITION_SCRIPT_VALIDATION_SUCCESSFUL)
                .detail(AppConstants.DETAIL_KEY_CONDITION_SCRIPT, conditionScript)
                .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                .log();
            
            return true;
            
        } catch (Exception e) {
            logger.warn()
                .message(AppConstants.LOG_INVALID_CONDITION_SCRIPT_SYNTAX)
                .detail(AppConstants.DETAIL_KEY_CONDITION_SCRIPT, conditionScript)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_CONDITIONAL_RULE)
                .log();

            return false;
        }
    }
}