package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccessRoleAssignmentRuleAuditEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ruleUpdatedBuildsUserFriendlyDbAuditLog() {
        UUID ruleId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        ObjectNode before = objectMapper.createObjectNode();
        before.put(AccessRoleAssignmentRuleAuditConstants.STATE_ENABLED, false);
        before.put(AccessRoleAssignmentRuleAuditConstants.STATE_PRIORITY, 1);

        ObjectNode after = objectMapper.createObjectNode();
        after.put(AccessRoleAssignmentRuleAuditConstants.STATE_ENABLED, true);
        after.put(AccessRoleAssignmentRuleAuditConstants.STATE_PRIORITY, 2);

        AccessRoleAssignmentRuleAuditEvent event = AccessRoleAssignmentRuleAuditEvent.ruleUpdated(
                "john.doe",
                "john.doe",
                "tenant-1",
                ruleId.toString(),
                "Dynamic Rule - Finance Manager",
                roleId.toString(),
                "Finance Manager",
                "business",
                "ACTIVE",
                "high",
                true,
                "MANUAL",
                2,
                1,
                List.of("EMPLOYEE"),
                before,
                after
        );

        AccessRoleAuditLog auditLog = event.toAuditLog();

        assertThat(auditLog.getAction()).isEqualTo(AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_UPDATED);
        assertThat(auditLog.getEntityType()).isEqualTo("Business Role Assignment Rule");
        assertThat(auditLog.getActivityLabel()).isEqualTo("Business Role Assignment Rule Updated");
        assertThat(auditLog.getRuleId()).isEqualTo(ruleId);
        assertThat(auditLog.getRuleName()).isEqualTo("Dynamic Rule - Finance Manager");
        assertThat(auditLog.getRoleId()).isEqualTo(roleId);
        assertThat(auditLog.getRoleName()).isEqualTo("Finance Manager");
        assertThat(event.riskLevel()).isEqualTo("high");
        assertThat(auditLog.getDescription())
                .isEqualTo("A business role assignment rule \"Dynamic Rule - Finance Manager\" was updated for role \"Finance Manager\" by john.doe");
        assertThat(auditLog.getChangedFields())
                .containsExactlyInAnyOrder(
                        AccessRoleAssignmentRuleAuditConstants.STATE_ENABLED,
                        AccessRoleAssignmentRuleAuditConstants.STATE_PRIORITY);
        assertThat(auditLog.getStateBefore()).isEqualTo(before);
        assertThat(auditLog.getStateAfter()).isEqualTo(after);
    }

    @Test
    void ruleCreatedUsesFriendlyDescriptionAndEmptyChangedFields() {
        UUID ruleId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        AccessRoleAssignmentRuleAuditEvent event = AccessRoleAssignmentRuleAuditEvent.ruleCreated(
                "alice",
                "alice",
                "tenant-1",
                ruleId.toString(),
                "Dynamic Rule - Finance Analyst",
                roleId.toString(),
                "Finance Analyst",
                "technical",
                "ACTIVE",
                "medium",
                true,
                "CRON",
                5,
                2,
                List.of("EMPLOYEE", "NON_EMPLOYEE"),
                objectMapper.createObjectNode()
        );

        assertThat(event.description())
                .isEqualTo("A technical role assignment rule \"Dynamic Rule - Finance Analyst\" was created for role \"Finance Analyst\" by alice");
        assertThat(event.riskLevel()).isEqualTo("medium");
        assertThat(event.computeChangedFields()).isEmpty();
    }
}
