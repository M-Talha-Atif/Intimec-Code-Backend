package com.aidentx.iam.access_roles_management.event;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleAssignmentRuleEvaluationEventListenerTest {

    @Mock
    private AccessRoleAssignmentRuleEvaluationRunner evaluationRunner;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private AccessRoleAssignmentRuleEvaluationEventListener listener;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleAssignmentRuleEvaluationEventListener.class), anyString())).thenReturn(logger);
        listener = new AccessRoleAssignmentRuleEvaluationEventListener(evaluationRunner, loggerFactory);
    }

    @Test
    void handleRuleEvaluationDelegatesToEvaluationRunner() {
        UUID ruleId = UUID.randomUUID();
        AccessRoleAssignmentRuleEvaluationEvent event = new AccessRoleAssignmentRuleEvaluationEvent(
                "tenant-1",
                ruleId,
                AccessRoleAssignmentExecution.TriggerType.ACCESS_ROLE_RULE_UPDATED,
                "admin@example.com",
                "MANUAL");

        listener.handleRuleEvaluation(event);

        verify(evaluationRunner).run(event);
    }
}

