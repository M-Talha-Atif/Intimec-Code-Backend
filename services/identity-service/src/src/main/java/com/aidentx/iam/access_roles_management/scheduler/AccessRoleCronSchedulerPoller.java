package com.aidentx.iam.access_roles_management.scheduler;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleScanService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fixed-interval poller that runs due CRON access-role assignment rules.
 *
 * <p>This mirrors the entitlement scheduler pattern: every minute it discovers
 * tenants with enabled CRON rules and delegates execution to the existing
 * access-role scan/execution flow. Rule-level locking remains enforced by the
 * repository query used inside {@code executeCronRules(..)}.</p>
 */
@Component
public class AccessRoleCronSchedulerPoller {

    private final AccessRoleAssignmentRuleRepository ruleRepository;
    private final AccessRoleAssignmentRuleScanService scanService;
    private final IUnifiedLogger logger;

    public AccessRoleCronSchedulerPoller(AccessRoleAssignmentRuleRepository ruleRepository,
                                         AccessRoleAssignmentRuleScanService scanService,
                                         UnifiedLoggerFactory loggerFactory) {
        this.ruleRepository = ruleRepository;
        this.scanService = scanService;
        this.logger = loggerFactory.getLogger(getClass(), "AccessRoleCronSchedulerPoller");
    }

    /**
     * Main poller method. Runs every 60 seconds after a short startup delay.
     */
    @Scheduled(fixedRate = 60_000, initialDelay = 30_000)
    public void pollDueRules() {
        try {
            List<String> tenantIds = ruleRepository.findTenantsWithCronRules();
            if (tenantIds.isEmpty()) {
                return;
            }

            for (String tenantId : tenantIds) {
                processTenant(tenantId);
            }
        } catch (Exception ex) {
            logger.error()
                    .message("Access role cron poller tick failed")
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_ERROR, ex.getMessage())
                    .log();
        }
    }

    /**
     * Runs due CRON rules for one tenant using the existing scan orchestration.
     *
     * @param tenantId tenant identifier
     */
    public void processTenant(String tenantId) {
        scanService.scanDueCronRules(tenantId, AccessRoleAssignmentRuleConstants.SYSTEM_TRIGGER);
    }
}

