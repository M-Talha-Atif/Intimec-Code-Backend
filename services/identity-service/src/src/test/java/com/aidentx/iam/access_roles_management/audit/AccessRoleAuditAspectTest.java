package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.service.AccessRoleAuditLogService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleAuditAspectTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessRoleAuditStateResolver stateResolver;

    @Mock
    private AccessRoleAuditPublisher auditPublisher;

    @Mock
    private AccessRoleAuditLogService auditLogService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private AuditAccessRoleChange auditAnnotation;

    private AccessRoleAuditAspect aspect;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleAuditAspect.class), anyString())).thenReturn(logger);
        aspect = new AccessRoleAuditAspect(stateResolver, auditPublisher, auditLogService, objectMapper, loggerFactory);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("john.doe", "pwd"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteRoleAuditUsesDeletedRoleRiskLevel() throws Throwable {
        UUID roleId = UUID.randomUUID();
        ObjectNode stateBefore = objectMapper.createObjectNode();
        stateBefore.put("id", roleId.toString());
        stateBefore.put("riskLevel", "low");

        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("Finance");
        role.setType("business");
        role.setRiskLevel("low");

        when(auditAnnotation.action()).thenReturn(AccessRoleAuditAction.ROLE_DELETED);
        when(auditAnnotation.tenantIdParamIndex()).thenReturn(0);
        when(auditAnnotation.roleIdParamIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(new Object[]{TENANT_ID, roleId});
        when(stateResolver.captureRoleState(roleId)).thenReturn(stateBefore);
        when(stateResolver.findRole(roleId)).thenReturn(Optional.of(role));
        when(joinPoint.proceed()).thenReturn(null);

        aspect.auditRoleChange(joinPoint, auditAnnotation);

        ArgumentCaptor<AccessRoleAuditEvent> eventCaptor = ArgumentCaptor.forClass(AccessRoleAuditEvent.class);
        verify(auditPublisher).publish(eventCaptor.capture());
        verify(auditLogService).persist(eventCaptor.getValue());

        AccessRoleAuditEvent event = eventCaptor.getValue();
        assertThat(event.action()).isEqualTo(AccessRoleAuditConstants.ACTION_ROLE_DELETED);
        assertThat(event.riskLevel()).isEqualTo("low");
        assertThat(event.stateBefore()).isEqualTo(stateBefore);
        assertThat(event.entityName()).isEqualTo("Finance");
        assertThat(event.roleType()).isEqualTo("business");
    }
}

