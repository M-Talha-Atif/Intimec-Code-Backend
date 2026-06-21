package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
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
class AccessRoleAssignmentExecutionAuditAspectTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessRoleAssignmentExecutionAuditService executionAuditService;

    @Mock
    private AccessRoleAssignmentExecutionAuditStateResolver stateResolver;

    @Mock
    private AccessRoleAssignmentExecutionAuditPublisher publisher;

    @Mock
    private AccessRoleAuditLogService auditLogService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock
    private IUnifiedLogger logger;

    @Mock
    private ProceedingJoinPoint pjp;

    private AccessRoleAssignmentExecutionAuditAspect aspect;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleAssignmentExecutionAuditAspect.class), anyString())).thenReturn(logger);
        aspect = new AccessRoleAssignmentExecutionAuditAspect(executionAuditService, stateResolver, publisher, auditLogService, loggerFactory);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("john.doe", "pwd"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditExecuteRuleUsesRoleRiskLevel() throws Throwable {
        UUID executionId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        AccessRoleAssignmentExecution execution = execution(executionId, ruleId, roleId);
        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("Finance Manager");
        role.setType("business");
        role.setStatus("ACTIVE");
        role.setRiskLevel("high");
        ObjectNode stateAfter = objectMapper.createObjectNode();
        stateAfter.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXECUTION_ID, executionId.toString());

        when(pjp.getArgs()).thenReturn(new Object[]{TENANT_ID, ruleId, AccessRoleAssignmentExecution.TriggerType.MANUAL, "john.doe"});
        when(pjp.proceed()).thenReturn(execution);
        aspect.auditExecuteRule(pjp);

        verify(executionAuditService).publishSingleExecutionAudit(TENANT_ID, execution, ruleId, "john.doe");
    }

    @Test
    void auditExecuteAllUsesHighestEvaluatedRoleRiskLevel() throws Throwable {
        AccessRoleAssignmentExecution highRiskExecution = execution(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        highRiskExecution.setRolesAssigned(2);
        highRiskExecution.setIdentitiesMatched(4);

        AccessRoleAssignmentExecution lowRiskExecution = execution(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        lowRiskExecution.setRolesAssigned(1);
        lowRiskExecution.setIdentitiesMatched(2);

        ObjectNode stateAfter = objectMapper.createObjectNode();
        stateAfter.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_HIGHEST_RISK_LEVEL, "high");
        stateAfter.put(AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXECUTION_COUNT, 2);

        when(pjp.getArgs()).thenReturn(new Object[]{TENANT_ID, "john.doe"});
        when(pjp.proceed()).thenReturn(List.of(highRiskExecution, lowRiskExecution));
        when(stateResolver.buildBulkExecutionContext(TENANT_ID, List.of(highRiskExecution, lowRiskExecution)))
                .thenReturn(new AccessRoleAssignmentExecutionAuditStateResolver.BulkExecutionAuditContext("high", stateAfter));

        aspect.auditExecuteAllRules(pjp);

        verify(executionAuditService).publishSingleExecutionAudit(
                TENANT_ID,
                highRiskExecution,
                highRiskExecution.getRuleId(),
                "john.doe");
        verify(executionAuditService).publishSingleExecutionAudit(
                TENANT_ID,
                lowRiskExecution,
                lowRiskExecution.getRuleId(),
                "john.doe");

        ArgumentCaptor<AccessRoleAssignmentExecutionAuditEvent> eventCaptor = ArgumentCaptor.forClass(AccessRoleAssignmentExecutionAuditEvent.class);
        verify(publisher).publish(eventCaptor.capture());

        assertThat(eventCaptor.getValue().action()).isEqualTo(AccessRoleAssignmentExecutionAuditConstants.ACTION_RULES_EXECUTED);
        assertThat(eventCaptor.getValue().riskLevel()).isEqualTo("high");
        assertThat(eventCaptor.getValue().executionCount()).isEqualTo(2);
        assertThat(eventCaptor.getValue().rolesAssigned()).isEqualTo(3);
        assertThat(eventCaptor.getValue().identitiesMatched()).isEqualTo(6);
    }

    @Test
    void auditExecuteRuleSkipsCronBecauseServiceOwnsThatAuditFlow() throws Throwable {
        UUID executionId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        AccessRoleAssignmentExecution execution = execution(executionId, ruleId, roleId);
        execution.setTriggerType(AccessRoleAssignmentExecution.TriggerType.CRON);

        when(pjp.getArgs()).thenReturn(new Object[]{TENANT_ID, ruleId, AccessRoleAssignmentExecution.TriggerType.CRON, "System"});
        when(pjp.proceed()).thenReturn(execution);

        Object result = aspect.auditExecuteRule(pjp);

        assertThat(result).isSameAs(execution);
        org.mockito.Mockito.verifyNoInteractions(executionAuditService);
    }

    private AccessRoleAssignmentExecution execution(UUID executionId, UUID ruleId, UUID roleId) {
        AccessRoleAssignmentExecution execution = new AccessRoleAssignmentExecution();
        execution.setId(executionId);
        execution.setTenantId(TENANT_ID);
        execution.setRuleId(ruleId);
        execution.setRuleName("Dynamic Rule - Finance Manager");
        execution.setRoleId(roleId);
        execution.setRoleName("Finance Manager");
        execution.setRoleType("business");
        execution.setTriggerType(AccessRoleAssignmentExecution.TriggerType.MANUAL);
        execution.setTriggeredBy("john.doe");
        execution.setStatus(AccessRoleAssignmentExecution.ExecutionStatus.SUCCESS);
        execution.setIdentitiesMatched(3);
        execution.setRolesAssigned(1);
        execution.setRolesReactivated(0);
        execution.setRolesRevoked(0);
        execution.setSkippedAlreadyAssigned(0);
        execution.setSkippedNotMatched(2);
        execution.setErrorCount(0);
        return execution;
    }
}

