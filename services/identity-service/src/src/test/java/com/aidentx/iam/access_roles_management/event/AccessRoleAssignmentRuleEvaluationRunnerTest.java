package com.aidentx.iam.access_roles_management.event;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessRoleAssignmentRuleEvaluationRunnerTest {

    @Mock
    private AccessRoleAssignmentExecutionService executionService;

    @Test
    void runDelegatesToExecutionService() {
        AccessRoleAssignmentRuleEvaluationRunner runner = new AccessRoleAssignmentRuleEvaluationRunner(executionService);
        UUID ruleId = UUID.randomUUID();
        AccessRoleAssignmentRuleEvaluationEvent event = new AccessRoleAssignmentRuleEvaluationEvent(
                "tenant-1",
                ruleId,
                AccessRoleAssignmentExecution.TriggerType.ACCESS_ROLE_RULE_CREATED,
                "imadmin",
                "CRON");

        runner.run(event);

        verify(executionService).executeRule(
                "tenant-1",
                ruleId,
                AccessRoleAssignmentExecution.TriggerType.ACCESS_ROLE_RULE_CREATED,
                "imadmin");
    }

    @Test
    void runStartsRequiresNewTransaction() throws Exception {
        Method runMethod = AccessRoleAssignmentRuleEvaluationRunner.class
                .getMethod("run", AccessRoleAssignmentRuleEvaluationEvent.class);

        Transactional transactional = runMethod.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}

