package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves role context and summary state for execution audit events.
 */
@Service
@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
public class AccessRoleAssignmentExecutionAuditStateResolver {

    private final AccessRoleRepository roleRepository;
    private final ObjectMapper objectMapper;
    private final AccessRoleExecutionAuditRiskResolver riskResolver;

    public AccessRoleAssignmentExecutionAuditStateResolver(AccessRoleRepository roleRepository,
                                                           ObjectMapper objectMapper,
                                                           AccessRoleExecutionAuditRiskResolver riskResolver) {
        this.roleRepository = roleRepository;
        this.objectMapper = objectMapper;
        this.riskResolver = riskResolver;
    }

    public SingleExecutionAuditContext buildSingleExecutionContext(String tenantId,
                                                                   AccessRoleAssignmentExecution execution) {
        AccessRole role = null;
        if (execution != null && execution.getRoleId() != null) {
            role = roleRepository.findById(execution.getRoleId())
                    .filter(candidate -> tenantId.equals(candidate.getTenantId()))
                    .orElse(null);
        }
        return new SingleExecutionAuditContext(role, buildSingleState(execution, role),
                role != null ? riskResolver.normalize(role.getRiskLevel()) : null);
    }

    public BulkExecutionAuditContext buildBulkExecutionContext(String tenantId,
                                                               List<AccessRoleAssignmentExecution> executions) {
        List<UUID> roleIds = executions == null ? List.of() : executions.stream()
                .map(AccessRoleAssignmentExecution::getRoleId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, AccessRole> rolesById = new LinkedHashMap<>();
        if (!roleIds.isEmpty()) {
            for (AccessRole role : roleRepository.findByIdIn(roleIds)) {
                if (tenantId.equals(role.getTenantId())) {
                    rolesById.put(role.getId(), role);
                }
            }
        }

        String highestRisk = riskResolver.highestOf(rolesById.values().stream()
                .map(AccessRole::getRiskLevel)
                .toList());
        return new BulkExecutionAuditContext(highestRisk, buildBulkState(executions, rolesById));
    }

    private JsonNode buildSingleState(AccessRoleAssignmentExecution execution, AccessRole role) {
        ObjectNode node = objectMapper.createObjectNode();
        if (execution == null) {
            return node;
        }
        putIfNotNull(node, AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXECUTION_ID, execution.getId());
        putIfNotNull(node, AccessRoleAssignmentExecutionAuditConstants.DETAIL_RULE_ID, execution.getRuleId());
        putIfNotNull(node, AccessRoleAssignmentExecutionAuditConstants.DETAIL_RULE_NAME, execution.getRuleName());
        putIfNotNull(node, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLE_ID, execution.getRoleId());
        putIfNotNull(node, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLE_NAME,
                role != null ? role.getName() : execution.getRoleName());
        putIfNotNull(node, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLE_TYPE,
                role != null ? role.getType() : execution.getRoleType());
        putIfNotNull(node, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLE_STATUS,
                role != null ? role.getStatus() : null);
        putIfNotNull(node, AccessRoleAssignmentExecutionAuditConstants.DETAIL_RISK_LEVEL,
                role != null ? riskResolver.normalize(role.getRiskLevel()) : null);
        putIfNotNull(node, AccessRoleAssignmentExecutionAuditConstants.DETAIL_TRIGGER_TYPE,
                execution.getTriggerType() != null ? execution.getTriggerType().name() : null);
        putIfNotNull(node, AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXECUTION_STATUS,
                execution.getStatus() != null ? execution.getStatus().name() : null);
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_IDENTITIES_MATCHED, execution.getIdentitiesMatched());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_ASSIGNED, execution.getRolesAssigned());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_REACTIVATED, execution.getRolesReactivated());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_REVOKED, execution.getRolesRevoked());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_SKIPPED_ALREADY_ASSIGNED, execution.getSkippedAlreadyAssigned());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_SKIPPED_NOT_MATCHED, execution.getSkippedNotMatched());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ERROR_COUNT, execution.getErrorCount());
        return node;
    }

    private JsonNode buildBulkState(List<AccessRoleAssignmentExecution> executions, Map<UUID, AccessRole> rolesById) {
        ObjectNode node = objectMapper.createObjectNode();
        List<AccessRoleAssignmentExecution> safeExecutions = executions != null ? executions : List.of();
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXECUTION_COUNT, safeExecutions.size());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_IDENTITIES_MATCHED,
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getIdentitiesMatched).sum());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_ASSIGNED,
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getRolesAssigned).sum());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_REACTIVATED,
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getRolesReactivated).sum());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_REVOKED,
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getRolesRevoked).sum());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_SKIPPED_ALREADY_ASSIGNED,
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getSkippedAlreadyAssigned).sum());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_SKIPPED_NOT_MATCHED,
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getSkippedNotMatched).sum());
        node.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ERROR_COUNT,
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getErrorCount).sum());
        putIfNotNull(node, AccessRoleAssignmentExecutionAuditConstants.DETAIL_HIGHEST_RISK_LEVEL,
                riskResolver.highestOf(rolesById.values().stream().map(AccessRole::getRiskLevel).toList()));

        ArrayNode evaluatedRoles = node.putArray("evaluatedRoles");
        for (AccessRoleAssignmentExecution execution : safeExecutions) {
            ObjectNode roleNode = objectMapper.createObjectNode();
            putIfNotNull(roleNode, AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXECUTION_ID, execution.getId());
            putIfNotNull(roleNode, AccessRoleAssignmentExecutionAuditConstants.DETAIL_RULE_ID, execution.getRuleId());
            putIfNotNull(roleNode, AccessRoleAssignmentExecutionAuditConstants.DETAIL_RULE_NAME, execution.getRuleName());
            putIfNotNull(roleNode, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLE_ID, execution.getRoleId());
            AccessRole role = execution.getRoleId() != null ? rolesById.get(execution.getRoleId()) : null;
            putIfNotNull(roleNode, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLE_NAME,
                    role != null ? role.getName() : execution.getRoleName());
            putIfNotNull(roleNode, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLE_TYPE,
                    role != null ? role.getType() : execution.getRoleType());
            putIfNotNull(roleNode, AccessRoleAssignmentExecutionAuditConstants.DETAIL_RISK_LEVEL,
                    role != null ? riskResolver.normalize(role.getRiskLevel()) : null);
            putIfNotNull(roleNode, AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXECUTION_STATUS,
                    execution.getStatus() != null ? execution.getStatus().name() : null);
            roleNode.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_IDENTITIES_MATCHED, execution.getIdentitiesMatched());
            roleNode.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_ASSIGNED, execution.getRolesAssigned());
            roleNode.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_REACTIVATED, execution.getRolesReactivated());
            roleNode.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_REVOKED, execution.getRolesRevoked());
            roleNode.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_ERROR_COUNT, execution.getErrorCount());
            evaluatedRoles.add(roleNode);
        }
        return node;
    }

    private void putIfNotNull(ObjectNode node, String key, UUID value) {
        if (value != null) {
            node.put(key, value.toString());
        }
    }

    private void putIfNotNull(ObjectNode node, String key, String value) {
        if (value != null) {
            node.put(key, value);
        }
    }

    public record SingleExecutionAuditContext(AccessRole role, JsonNode stateAfter, String normalizedRiskLevel) {
    }

    public record BulkExecutionAuditContext(String highestRiskLevel, JsonNode stateAfter) {
    }
}

