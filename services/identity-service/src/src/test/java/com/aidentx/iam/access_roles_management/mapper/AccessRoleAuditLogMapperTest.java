package com.aidentx.iam.access_roles_management.mapper;

import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditConstants;
import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditEvent;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccessRoleAuditLogMapperTest {

    private final AccessRoleAuditLogMapper mapper = Mappers.getMapper(AccessRoleAuditLogMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void eventToEntityUsesImportDescriptionAndTriggerForImportCreatedRole() {
        UUID roleId = UUID.randomUUID();
        ObjectNode stateAfter = objectMapper.createObjectNode();
        stateAfter.put(AccessRoleAuditConstants.STATE_FIELD_CREATED_THROUGH, AccessRoleAuditConstants.TRIGGER_IMPORT);

        AccessRoleAuditEvent event = AccessRoleAuditEvent.roleCreated(
                "actor-1",
                "alice.admin",
                "tenant_001",
                roleId.toString(),
                "Imported Role",
                "No type",
                "Enabled",
                null,
                "No business unit",
                null,
                stateAfter);

        AccessRoleAuditLog entity = mapper.eventToEntity(event);

        assertThat(entity.getTriggerSource()).isEqualTo(AccessRoleAuditConstants.TRIGGER_IMPORT);
        assertThat(entity.getDescription()).isEqualTo(AccessRoleAuditConstants.DESC_ROLE_CREATED_THROUGH_IMPORT);
    }
}
