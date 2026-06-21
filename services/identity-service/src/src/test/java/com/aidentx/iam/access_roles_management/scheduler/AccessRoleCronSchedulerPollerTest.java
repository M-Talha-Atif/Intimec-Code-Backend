package com.aidentx.iam.access_roles_management.scheduler;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleScanService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleCronSchedulerPollerTest {

    @Mock
    private AccessRoleAssignmentRuleRepository ruleRepository;

    @Mock
    private AccessRoleAssignmentRuleScanService scanService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private AccessRoleCronSchedulerPoller poller;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleCronSchedulerPoller.class), anyString())).thenReturn(logger);
        poller = new AccessRoleCronSchedulerPoller(ruleRepository, scanService, loggerFactory);
    }

    @Test
    void pollDueRulesProcessesEveryTenantWithCronRules() {
        when(ruleRepository.findTenantsWithCronRules()).thenReturn(List.of("tenant-1", "tenant-2"));

        poller.pollDueRules();

        verify(scanService).scanDueCronRules("tenant-1", AccessRoleAssignmentRuleConstants.SYSTEM_TRIGGER);
        verify(scanService).scanDueCronRules("tenant-2", AccessRoleAssignmentRuleConstants.SYSTEM_TRIGGER);
    }

    @Test
    void pollDueRulesDoesNothingWhenNoTenantsHaveCronRules() {
        when(ruleRepository.findTenantsWithCronRules()).thenReturn(List.of());

        poller.pollDueRules();

        verify(scanService, never()).scanDueCronRules(anyString(), anyString());
    }

    @Test
    void pollDueRulesSwallowsUnexpectedPollerErrors() {
        when(ruleRepository.findTenantsWithCronRules()).thenThrow(new RuntimeException("boom"));

        poller.pollDueRules();

        verify(logger).error();
    }

    @Test
    void processTenantDelegatesUsingSystemActor() {
        poller.processTenant("tenant-42");

        verify(scanService).scanDueCronRules("tenant-42", AccessRoleAssignmentRuleConstants.SYSTEM_TRIGGER);
    }
}

