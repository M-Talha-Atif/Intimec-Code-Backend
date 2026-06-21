package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentExecutionService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleAssignmentRuleScanServiceImplTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String TRIGGERED_BY = "admin@example.com";

    @Mock
    private AccessRoleAssignmentExecutionService executionService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private AccessRoleAssignmentRuleScanServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleAssignmentRuleScanServiceImpl.class), anyString())).thenReturn(logger);
        service = new AccessRoleAssignmentRuleScanServiceImpl(executionService, loggerFactory);
    }

    @Test
    void scanRuleRunsManualRuleScanAndReturnsAssignmentOutcome() {
        UUID ruleId = UUID.randomUUID();
        AccessRoleAssignmentExecution expected = execution(ruleId, 1, 0, 0);
        when(executionService.executeRule(TENANT_ID, ruleId, AccessRoleAssignmentExecution.TriggerType.MANUAL, TRIGGERED_BY))
                .thenReturn(expected);

        AccessRoleAssignmentExecution actual = service.scanRule(TENANT_ID, ruleId, TRIGGERED_BY);

        assertThat(actual).isSameAs(expected);
        assertThat(actual.getRolesAssigned()).isEqualTo(1);
        verify(executionService).executeRule(TENANT_ID, ruleId, AccessRoleAssignmentExecution.TriggerType.MANUAL, TRIGGERED_BY);
        verify(logger, atLeastOnce()).info();
    }

    @Test
    void scanRuleDefaultsTriggeredByToSystem() {
        UUID ruleId = UUID.randomUUID();
        AccessRoleAssignmentExecution expected = execution(ruleId, 0, 1, 0);
        when(executionService.executeRule(
                TENANT_ID,
                ruleId,
                AccessRoleAssignmentExecution.TriggerType.MANUAL,
                AccessRoleAssignmentRuleConstants.SYSTEM_TRIGGER))
                .thenReturn(expected);

        AccessRoleAssignmentExecution actual = service.scanRule(TENANT_ID, ruleId, " ");

        assertThat(actual.getSkippedAlreadyAssigned()).isEqualTo(1);
        verify(executionService).executeRule(
                TENANT_ID,
                ruleId,
                AccessRoleAssignmentExecution.TriggerType.MANUAL,
                AccessRoleAssignmentRuleConstants.SYSTEM_TRIGGER);
    }

    @Test
    void scanAllRulesRunsFullScanAndAggregatesOutcomesForLogging() {
        AccessRoleAssignmentExecution assigned = execution(UUID.randomUUID(), 1, 0, 0);
        AccessRoleAssignmentExecution skipped = execution(UUID.randomUUID(), 0, 1, 0);
        when(executionService.executeAllRules(TENANT_ID, TRIGGERED_BY)).thenReturn(List.of(assigned, skipped));

        List<AccessRoleAssignmentExecution> executions = service.scanAllRules(TENANT_ID, TRIGGERED_BY);

        assertThat(executions).containsExactly(assigned, skipped);
        assertThat(executions).extracting(AccessRoleAssignmentExecution::getRolesAssigned).containsExactly(1, 0);
        assertThat(executions).extracting(AccessRoleAssignmentExecution::getSkippedAlreadyAssigned).containsExactly(0, 1);
        verify(executionService).executeAllRules(TENANT_ID, TRIGGERED_BY);
        verify(logger, atLeastOnce()).info();
    }

    @Test
    void scanImmediateRulesDelegatesToExecutionService() {
        AccessRoleAssignmentExecution execution = execution(UUID.randomUUID(), 1, 0, 0);
        when(executionService.executeImmediateRules(TENANT_ID, TRIGGERED_BY)).thenReturn(List.of(execution));

        List<AccessRoleAssignmentExecution> executions = service.scanImmediateRules(TENANT_ID, TRIGGERED_BY);

        assertThat(executions).containsExactly(execution);
        verify(executionService).executeImmediateRules(TENANT_ID, TRIGGERED_BY);
        verify(logger, atLeastOnce()).debug();
    }

    @Test
    void scanDueCronRulesDelegatesToExecutionService() {
        AccessRoleAssignmentExecution execution = execution(UUID.randomUUID(), 0, 0, 1);
        when(executionService.executeCronRules(TENANT_ID, TRIGGERED_BY)).thenReturn(List.of(execution));

        List<AccessRoleAssignmentExecution> executions = service.scanDueCronRules(TENANT_ID, TRIGGERED_BY);

        assertThat(executions).containsExactly(execution);
        assertThat(executions.get(0).getErrorCount()).isEqualTo(1);
        verify(executionService).executeCronRules(TENANT_ID, TRIGGERED_BY);
        verify(logger, atLeastOnce()).debug();
    }

    @Test
    void scanRuleRejectsBlankTenant() {
        assertThatThrownBy(() -> service.scanRule(" ", UUID.randomUUID(), TRIGGERED_BY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(AccessRoleAssignmentRuleConstants.ERROR_TENANT_REQUIRED);
    }

    private AccessRoleAssignmentExecution execution(UUID ruleId, int assigned, int skippedAlreadyAssigned, int errors) {
        AccessRoleAssignmentExecution execution = new AccessRoleAssignmentExecution();
        execution.setId(UUID.randomUUID());
        execution.setTenantId(TENANT_ID);
        execution.setRuleId(ruleId);
        execution.setRoleId(UUID.randomUUID());
        execution.setRuleName("rule-" + ruleId);
        execution.setTriggerType(AccessRoleAssignmentExecution.TriggerType.MANUAL);
        execution.setStatus(errors > 0
                ? AccessRoleAssignmentExecution.ExecutionStatus.PARTIAL_FAILURE
                : AccessRoleAssignmentExecution.ExecutionStatus.SUCCESS);
        execution.setIdentitiesMatched(assigned + skippedAlreadyAssigned);
        execution.setRolesAssigned(assigned);
        execution.setSkippedAlreadyAssigned(skippedAlreadyAssigned);
        execution.setErrorCount(errors);
        return execution;
    }
}

