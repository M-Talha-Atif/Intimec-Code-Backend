package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleResponse;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleAssignmentRuleAuditAspectTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessRoleAssignmentRuleAuditStateResolver stateResolver;

    @Mock
    private AccessRoleAssignmentRuleAuditPublisher publisher;

    @Mock
    private AccessRoleAuditLogService auditLogService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock
    private IUnifiedLogger logger;

    @Mock
    private ProceedingJoinPoint pjp;

    private AccessRoleAssignmentRuleAuditAspect aspect;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleAssignmentRuleAuditAspect.class), anyString())).thenReturn(logger);
        aspect = new AccessRoleAssignmentRuleAuditAspect(stateResolver, publisher, auditLogService, loggerFactory);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("john.doe", "pwd"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditCreateRulePublishesAndPersistsFriendlyEvent() throws Throwable {
        UUID ruleId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        AccessRoleAssignmentRuleResponse response = response(ruleId, roleId);
        ObjectNode stateAfter = objectMapper.createObjectNode();
        stateAfter.put(AccessRoleAssignmentRuleAuditConstants.STATE_RULE_ID, ruleId.toString());
        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("Finance Manager");
        role.setType("business");
        role.setStatus("ACTIVE");
        role.setRiskLevel("high");
        AccessRoleAssignmentRule rule = new AccessRoleAssignmentRule();
        rule.setId(ruleId);
        rule.setRoleId(roleId);

        when(pjp.getArgs()).thenReturn(new Object[]{TENANT_ID, new Object(), "john.doe"});
        when(pjp.proceed()).thenReturn(response);
        when(stateResolver.captureRuleSnapshot(TENANT_ID, ruleId))
                .thenReturn(Optional.of(new AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot(rule, role, stateAfter)));

        aspect.auditCreateRule(pjp);

        ArgumentCaptor<AccessRoleAssignmentRuleAuditEvent> eventCaptor = ArgumentCaptor.forClass(AccessRoleAssignmentRuleAuditEvent.class);
        ArgumentCaptor<AccessRoleAuditLog> logCaptor = ArgumentCaptor.forClass(AccessRoleAuditLog.class);

        verify(publisher).publish(eventCaptor.capture());
        verify(auditLogService).persist(logCaptor.capture());

        assertThat(eventCaptor.getValue().action()).isEqualTo(AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_CREATED);
        assertThat(eventCaptor.getValue().actorUsername()).isEqualTo("john.doe");
        assertThat(eventCaptor.getValue().riskLevel()).isEqualTo("high");
        assertThat(logCaptor.getValue().getActivityLabel()).isEqualTo("Business Role Assignment Rule Created");
        assertThat(logCaptor.getValue().getRuleId()).isEqualTo(ruleId);
        assertThat(logCaptor.getValue().getRuleName()).isEqualTo("Dynamic Rule - Finance Manager");
    }

    @Test
    void auditUpdateRuleUsesDiffBeforePublishing() throws Throwable {
        UUID ruleId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        AccessRoleAssignmentRule rule = new AccessRoleAssignmentRule();
        rule.setId(ruleId);
        rule.setRoleId(roleId);
        rule.setName("Dynamic Rule - Finance Manager");
        rule.setEnabled(false);
        rule.setPriority(1);
        rule.setIdentityTypes(List.of("EMPLOYEE"));

        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("Finance Manager");
        role.setType("business");
        role.setStatus("ACTIVE");
        role.setRiskLevel("medium");

        ObjectNode beforeState = objectMapper.createObjectNode();
        beforeState.put(AccessRoleAssignmentRuleAuditConstants.STATE_ENABLED, false);
        ObjectNode beforeDiff = objectMapper.createObjectNode();
        beforeDiff.put(AccessRoleAssignmentRuleAuditConstants.STATE_ENABLED, false);
        ObjectNode afterState = objectMapper.createObjectNode();
        afterState.put(AccessRoleAssignmentRuleAuditConstants.STATE_ENABLED, true);
        ObjectNode afterDiff = objectMapper.createObjectNode();
        afterDiff.put(AccessRoleAssignmentRuleAuditConstants.STATE_ENABLED, true);
        ObjectNode afterStateFull = objectMapper.createObjectNode();
        afterStateFull.put(AccessRoleAssignmentRuleAuditConstants.STATE_ENABLED, true);
        afterStateFull.put(AccessRoleAssignmentRuleAuditConstants.STATE_ROLE_RISK_LEVEL, "medium");

        AccessRoleAssignmentRuleResponse response = response(ruleId, roleId);

        when(pjp.getArgs()).thenReturn(new Object[]{TENANT_ID, ruleId, new Object(), "john.doe"});
        when(pjp.proceed()).thenReturn(response);
        when(stateResolver.captureRuleSnapshot(TENANT_ID, ruleId))
                .thenReturn(Optional.of(new AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot(rule, role, beforeState)))
                .thenReturn(Optional.of(new AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot(rule, role, afterStateFull)));
        when(stateResolver.buildDiff(beforeState, afterStateFull))
                .thenReturn(new AccessRoleAssignmentRuleAuditStateResolver.StateDiff(beforeDiff, afterDiff));

        aspect.auditUpdateRule(pjp);

        ArgumentCaptor<AccessRoleAssignmentRuleAuditEvent> eventCaptor = ArgumentCaptor.forClass(AccessRoleAssignmentRuleAuditEvent.class);
        verify(publisher).publish(eventCaptor.capture());

        assertThat(eventCaptor.getValue().action()).isEqualTo(AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_UPDATED);
        assertThat(eventCaptor.getValue().riskLevel()).isEqualTo("medium");
        assertThat(eventCaptor.getValue().stateBefore()).isEqualTo(beforeDiff);
        assertThat(eventCaptor.getValue().stateAfter()).isEqualTo(afterDiff);
    }

    @Test
    void auditDeleteRulePublishesUsingPreDeleteSnapshot() throws Throwable {
        UUID ruleId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        AccessRoleAssignmentRule rule = new AccessRoleAssignmentRule();
        rule.setId(ruleId);
        rule.setRoleId(roleId);
        rule.setName("Dynamic Rule - Finance Manager");
        rule.setEnabled(true);
        rule.setPriority(3);
        rule.setIdentityTypes(List.of("EMPLOYEE"));
        rule.setScheduleType(AccessRoleAssignmentRule.ScheduleType.MANUAL);

        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("Finance Manager");
        role.setType("business");
        role.setStatus("ACTIVE");
        role.setRiskLevel("low");

        ObjectNode beforeState = objectMapper.createObjectNode();
        beforeState.put(AccessRoleAssignmentRuleAuditConstants.STATE_RULE_ID, ruleId.toString());

        when(pjp.getArgs()).thenReturn(new Object[]{TENANT_ID, ruleId});
        when(pjp.proceed()).thenReturn(null);
        when(stateResolver.captureRuleSnapshot(TENANT_ID, ruleId))
                .thenReturn(Optional.of(new AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot(rule, role, beforeState)));

        aspect.auditDeleteRule(pjp);

        ArgumentCaptor<AccessRoleAssignmentRuleAuditEvent> eventCaptor = ArgumentCaptor.forClass(AccessRoleAssignmentRuleAuditEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().action()).isEqualTo(AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_DELETED);
        assertThat(eventCaptor.getValue().riskLevel()).isEqualTo("low");
        assertThat(eventCaptor.getValue().stateBefore()).isEqualTo(beforeState);
        assertThat(eventCaptor.getValue().description()).contains("was deleted for role \"Finance Manager\" by john.doe");
    }

    private AccessRoleAssignmentRuleResponse response(UUID ruleId, UUID roleId) {
        AccessRoleAssignmentRuleResponse response = new AccessRoleAssignmentRuleResponse();
        response.setId(ruleId);
        response.setName("Dynamic Rule - Finance Manager");
        response.setRoleId(roleId);
        response.setRoleName("Finance Manager");
        response.setRoleType("business");
        response.setRoleStatus("ACTIVE");
        response.setEnabled(true);
        response.setScheduleType("MANUAL");
        response.setPriority(2);
        response.setIdentityTypes(List.of("EMPLOYEE"));
        response.setConditionGroups(List.of(Map.of("field", "department", "operator", "EQUALS", "value", "Finance")));
        return response;
    }
}

