package com.aidentx.iam.access_roles_management.event;

import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentExecutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs create/update-triggered access-role assignment rule evaluation in a new transaction.
 *
 * <p>The event listener is invoked after the rule create/update transaction commits. Starting
 * a fresh transaction here ensures execution history rows and execution audit-log persistence
 * are committed independently from the already-finished request transaction.</p>
 */
@Service
public class AccessRoleAssignmentRuleEvaluationRunner {

    private final AccessRoleAssignmentExecutionService executionService;

    public AccessRoleAssignmentRuleEvaluationRunner(AccessRoleAssignmentExecutionService executionService) {
        this.executionService = executionService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void run(AccessRoleAssignmentRuleEvaluationEvent event) {
        executionService.executeRule(
                event.getTenantId(),
                event.getRuleId(),
                event.getTriggerType(),
                event.getTriggeredBy());
    }
}

