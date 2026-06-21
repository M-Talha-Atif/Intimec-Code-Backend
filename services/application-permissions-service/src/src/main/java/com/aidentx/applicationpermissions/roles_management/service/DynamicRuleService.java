package com.aidentx.applicationpermissions.roles_management.service;

import com.aidentx.applicationpermissions.roles_management.model.dto.CreateDynamicRuleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.DynamicRuleExecutionResult;
import com.aidentx.applicationpermissions.roles_management.model.dto.DynamicRuleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.UpdateDynamicRuleRequest;

/**
 * Service interface for Dynamic Rule operations.
 * 
 * <p>Provides methods for managing dynamic rules that control
 * role assignment behavior, including creation, activation,
 * and scheduling functionality.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public interface DynamicRuleService {
    
    /**
     * Creates a new dynamic rule.
     * 
     * @param request the dynamic rule creation request
     * @return the created dynamic rule response
     * @throws IllegalArgumentException if the role doesn't exist or doesn't belong to the tenant
     */
    DynamicRuleResponse createDynamicRule(CreateDynamicRuleRequest request);
    
    /**
     * Updates an existing dynamic rule identified by role ID.
     * If the schedule changes from DAILY to IMMEDIATE, the rule will be executed instantly.
     * 
     * @param request the dynamic rule update request containing roleId and new properties
     * @return the updated dynamic rule response
     * @throws IllegalArgumentException if the role or dynamic rule doesn't exist or doesn't belong to the tenant
     */
    DynamicRuleResponse updateDynamicRule(UpdateDynamicRuleRequest request);
    
    /**
     * Executes a dynamic rule by evaluating the condition script against tenant users
     * and assigning the role to users who match the conditions.
     * 
     * @param dynamicRuleId the ID of the dynamic rule to execute
     * @return execution result containing assignment statistics
     * @throws IllegalArgumentException if the dynamic rule doesn't exist or doesn't belong to the tenant
     */
    DynamicRuleExecutionResult executeDynamicRule(java.util.UUID dynamicRuleId);

    /**
     * Executes a dynamic rule with a specific tenant context for scheduled executions.
     * This method is used by the scheduler when there's no HTTP request context available.
     * 
     * @param dynamicRuleId the ID of the dynamic rule to execute
     * @param tenantId the tenant ID to execute the rule for
     * @return execution result containing assignment statistics
     * @throws IllegalArgumentException if the dynamic rule doesn't exist or doesn't belong to the tenant
     */
    DynamicRuleExecutionResult executeDynamicRuleWithTenant(java.util.UUID dynamicRuleId, String tenantId);
    
    /**
     * Retrieves a dynamic rule by role ID.
     * 
     * @param roleId the ID of the role to get the dynamic rule for
     * @return the dynamic rule response, or null if no dynamic rule exists for this role
     * @throws IllegalArgumentException if the role doesn't exist or doesn't belong to the tenant
     */
    DynamicRuleResponse getDynamicRuleByRoleId(java.util.UUID roleId);
}