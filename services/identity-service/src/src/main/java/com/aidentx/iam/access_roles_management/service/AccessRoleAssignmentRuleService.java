package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleAssignmentRuleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleGlobalScheduleUpdateRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.UpdateAccessRoleAssignmentRuleRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleGlobalScheduleConfigResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleGlobalScheduleUpdateResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleResponse;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

/**
 * Service contract for CRUD operations on dynamic access-role assignment rules.
 *
 * <p>Each rule targets one access role and stores only grouped identity
 * conditions via {@code conditionGroups}. Evaluation and role entitlement
 * propagation are intentionally separate from this CRUD contract.</p>
 */
public interface AccessRoleAssignmentRuleService {

    /**
     * Creates a tenant-scoped assignment rule for an access role.
     *
     * @param tenantId tenant identifier
     * @param request create request
     * @param createdBy authenticated actor name
     * @return created rule response
     */
    AccessRoleAssignmentRuleResponse createRule(String tenantId,
                                                CreateAccessRoleAssignmentRuleRequest request,
                                                String createdBy);

    /**
     * Updates mutable fields of an existing access-role assignment rule.
     *
     * @param tenantId tenant identifier
     * @param ruleId rule identifier
     * @param request update request
     * @param updatedBy authenticated actor name
     * @return updated rule response
     */
    AccessRoleAssignmentRuleResponse updateRule(String tenantId,
                                                UUID ruleId,
                                                UpdateAccessRoleAssignmentRuleRequest request,
                                                String updatedBy);

    /**
     * Retrieves a rule by ID within a tenant.
     *
     * @param tenantId tenant identifier
     * @param ruleId rule identifier
     * @return matching rule response
     */
    AccessRoleAssignmentRuleResponse getRule(String tenantId, UUID ruleId);

    /**
     * Retrieves the rule targeting a specific role.
     *
     * @param tenantId tenant identifier
     * @param roleId role identifier
     * @return optional rule response
     */
    Optional<AccessRoleAssignmentRuleResponse> getRuleByRole(String tenantId, UUID roleId);

    /**
     * Lists rules for a tenant.
     *
     * @param tenantId tenant identifier
     * @param page zero-based page number
     * @param size page size
     * @param sort optional sort property
     * @return page of rule responses
     */
    Page<AccessRoleAssignmentRuleResponse> listRules(String tenantId, int page, int size, String sort);

    /**
     * Updates the tenant-level schedule configuration and applies it to all
     * access-role assignment rules.
     *
     * @param tenantId tenant identifier
     * @param request global schedule update request
     * @return updated schedule summary
     */
    AccessRoleAssignmentRuleGlobalScheduleUpdateResponse updateGlobalScheduleType(
            String tenantId,
            AccessRoleAssignmentRuleGlobalScheduleUpdateRequest request);

    /**
     * Returns the persisted tenant-level global schedule config.
     *
     * @param tenantId tenant identifier
     * @return current global schedule config, or the default config when none exists
     */
    AccessRoleAssignmentRuleGlobalScheduleConfigResponse getGlobalScheduleConfig(String tenantId);

    /**
     * Deletes a rule by ID within a tenant.
     *
     * @param tenantId tenant identifier
     * @param ruleId rule identifier
     */
    void deleteRule(String tenantId, UUID ruleId);
}

