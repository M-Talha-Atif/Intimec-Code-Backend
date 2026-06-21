package com.aidentx.iam.access_roles_management.mapper;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleResponse;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting access-role assignment-rule entities to API responses.
 *
 * <p>This mapper is intentionally implemented manually because rule condition
 * groups are stored as JSON-compatible maps and role display metadata is resolved
 * separately by the service layer.</p>
 */
@Component
public class AccessRoleAssignmentRuleMapper {

    /**
     * Converts a rule entity into a response DTO and enriches it with optional
     * role display metadata.
     *
     * @param entity rule entity
     * @param role target access role, may be null if unavailable
     * @return response DTO
     */
    public AccessRoleAssignmentRuleResponse toResponse(AccessRoleAssignmentRule entity, AccessRole role) {
        AccessRoleAssignmentRuleResponse response = new AccessRoleAssignmentRuleResponse();
        response.setId(entity.getId());
        response.setTenantId(entity.getTenantId());
        response.setName(entity.getName());
        response.setRoleId(entity.getRoleId());
        response.setConditionGroups(entity.getConditionGroups());
        response.setEnabled(entity.isEnabled());
        response.setScheduleType(entity.getScheduleType() != null ? entity.getScheduleType().name() : null);
        response.setCronExpression(entity.getCronExpression());
        response.setScheduleTime(entity.getScheduleTime());
        response.setScheduleDate(entity.getScheduleDate());
        response.setScheduleTimezone(entity.getScheduleTimezone());
        response.setLastEvaluatedAt(entity.getLastEvaluatedAt());
        response.setNextExecutionAt(entity.getNextExecutionAt());
        response.setPriority(entity.getPriority());
        response.setIdentityTypes(entity.getIdentityTypes());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        if (role != null) {
            response.setRoleName(role.getDisplayName() != null && !role.getDisplayName().isBlank()
                    ? role.getDisplayName()
                    : role.getName());
            response.setRoleType(role.getType());
            response.setRoleStatus(role.getStatus());
        }

        return response;
    }
}

