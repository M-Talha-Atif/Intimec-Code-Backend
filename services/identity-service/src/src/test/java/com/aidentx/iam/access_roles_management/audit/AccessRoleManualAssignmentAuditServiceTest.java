package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.dto.response.ManualRoleMemberGrantResultDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import com.aidentx.iam.access_roles_management.service.AccessRoleAuditLogService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleManualAssignmentAuditServiceTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessRoleManualAssignmentAuditPublisher publisher;

    @Mock
    private AccessRoleAuditLogService auditLogService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock
    private IUnifiedLogger logger;

    private AccessRoleManualAssignmentAuditService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleManualAssignmentAuditService.class), anyString())).thenReturn(logger);
        service = new AccessRoleManualAssignmentAuditService(publisher, auditLogService, objectMapper, loggerFactory);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("john.doe", "pwd"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishManualGrantAuditPublishesSummaryEventAndPersistsRoleAuditLog() {
        UUID roleId = UUID.randomUUID();
        UUID addedIdentityId = UUID.randomUUID();
        UUID failedIdentityId = UUID.randomUUID();

        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("Finance Manager");
        role.setType("business");
        role.setRiskLevel("high");

        ManualRoleMemberGrantResultDto result = new ManualRoleMemberGrantResultDto(
                List.of(new ManualRoleMemberGrantResultDto.GrantedIdentity(addedIdentityId, "Jane Doe", "EMPLOYEE")),
                List.of(new ManualRoleMemberGrantResultDto.GrantedIdentity(UUID.randomUUID(), "Payroll Bot", "NON_HUMAN")),
                List.of(new ManualRoleMemberGrantResultDto.FailedIdentity(failedIdentityId, null, null, "Identity not found for tenant")));

        service.publishManualGrantAudit(TENANT_ID, role, result);

        ArgumentCaptor<AccessRoleManualAssignmentAuditEvent> eventCaptor = ArgumentCaptor.forClass(AccessRoleManualAssignmentAuditEvent.class);
        ArgumentCaptor<AccessRoleAuditLog> logCaptor = ArgumentCaptor.forClass(AccessRoleAuditLog.class);

        verify(publisher).publish(eventCaptor.capture());
        verify(auditLogService).persist(logCaptor.capture());

        AccessRoleManualAssignmentAuditEvent event = eventCaptor.getValue();
        assertThat(event.action()).isEqualTo(AccessRoleManualAssignmentAuditConstants.ACTION_MEMBERS_MANUALLY_GRANTED);
        assertThat(event.actorUsername()).isEqualTo("john.doe");
        assertThat(event.roleId()).isEqualTo(roleId.toString());
        assertThat(event.requestedCount()).isEqualTo(3);
        assertThat(event.addedCount()).isEqualTo(1);
        assertThat(event.skippedCount()).isEqualTo(1);
        assertThat(event.failedCount()).isEqualTo(1);
        assertThat(event.stateAfter().get(AccessRoleManualAssignmentAuditConstants.STATE_ADDED_IDENTITIES)).hasSize(1);
        assertThat(event.stateAfter().get(AccessRoleManualAssignmentAuditConstants.STATE_FAILED_IDENTITIES)).hasSize(1);

        AccessRoleAuditLog auditLog = logCaptor.getValue();
        assertThat(auditLog.getAction()).isEqualTo(AccessRoleManualAssignmentAuditConstants.ACTION_MEMBERS_MANUALLY_GRANTED);
        assertThat(auditLog.getActivityLabel()).isEqualTo("Business Role Members Manually Granted");
        assertThat(auditLog.getRoleId()).isEqualTo(roleId);
        assertThat(auditLog.getStateAfter()).isEqualTo(event.stateAfter());
    }
}

