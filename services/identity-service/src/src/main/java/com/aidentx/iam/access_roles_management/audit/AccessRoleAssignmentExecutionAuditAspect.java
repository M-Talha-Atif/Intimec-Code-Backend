package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.service.AccessRoleAuditLogService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AOP aspect for auditing access-role assignment execution operations.
 */
@Aspect
@Component
public class AccessRoleAssignmentExecutionAuditAspect {

    private final AccessRoleAssignmentExecutionAuditService executionAuditService;
    private final AccessRoleAssignmentExecutionAuditStateResolver stateResolver;
    private final AccessRoleAssignmentExecutionAuditPublisher auditPublisher;
    private final AccessRoleAuditLogService auditLogService;
    private final IUnifiedLogger logger;

    public AccessRoleAssignmentExecutionAuditAspect(AccessRoleAssignmentExecutionAuditService executionAuditService,
                                                    AccessRoleAssignmentExecutionAuditStateResolver stateResolver,
                                                    AccessRoleAssignmentExecutionAuditPublisher auditPublisher,
                                                    AccessRoleAuditLogService auditLogService,
                                                    UnifiedLoggerFactory loggerFactory) {
        this.executionAuditService = executionAuditService;
        this.stateResolver = stateResolver;
        this.auditPublisher = auditPublisher;
        this.auditLogService = auditLogService;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAssignmentExecutionAuditConstants.LOG_CATEGORY);
    }

    @Pointcut("execution(public * com.aidentx.iam.access_roles_management.service.impl.AccessRoleAssignmentExecutionServiceImpl.executeRule(..))")
    public void executeRulePointcut() {
    }

    @Pointcut("execution(public * com.aidentx.iam.access_roles_management.service.impl.AccessRoleAssignmentExecutionServiceImpl.executeAllRules(..))")
    public void executeAllRulesPointcut() {
    }

    @Around("executeRulePointcut()")
    public Object auditExecuteRule(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];
        UUID ruleId = (UUID) pjp.getArgs()[1];
        AccessRoleAssignmentExecution.TriggerType triggerType =
                (AccessRoleAssignmentExecution.TriggerType) pjp.getArgs()[2];
        String fallbackActor = pjp.getArgs().length > 3 ? (String) pjp.getArgs()[3] : null;

        Object result = pjp.proceed();

        if (AccessRoleAssignmentExecution.TriggerType.CRON == triggerType) {
            return result;
        }

        try {
            AccessRoleAssignmentExecution execution = (AccessRoleAssignmentExecution) result;
            executionAuditService.publishSingleExecutionAudit(tenantId, execution, ruleId, fallbackActor);
        } catch (Exception e) {
            logAuditFailure(e, AccessRoleAssignmentExecutionAuditConstants.LOG_EXECUTE_RULE_AUDIT_FAILED, ruleId, tenantId);
        }

        return result;
    }

    @Around("executeAllRulesPointcut()")
    public Object auditExecuteAllRules(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];

        Object result = pjp.proceed();

        try {
            @SuppressWarnings("unchecked")
            List<AccessRoleAssignmentExecution> executions = (List<AccessRoleAssignmentExecution>) result;
            String fallbackActor = pjp.getArgs().length > 1 ? (String) pjp.getArgs()[1] : null;
            for (AccessRoleAssignmentExecution execution : executions) {
                executionAuditService.publishSingleExecutionAudit(
                        tenantId,
                        execution,
                        execution != null ? execution.getRuleId() : null,
                        fallbackActor);
            }
            var context = stateResolver.buildBulkExecutionContext(tenantId, executions);
            publishAndPersist(buildBulkExecutionEvent(tenantId, executions, context, fallbackActor));
        } catch (Exception e) {
            logAuditFailure(e, AccessRoleAssignmentExecutionAuditConstants.LOG_EXECUTE_ALL_AUDIT_FAILED, null, tenantId);
        }

        return result;
    }

    private AccessRoleAssignmentExecutionAuditEvent buildBulkExecutionEvent(
            String tenantId,
            List<AccessRoleAssignmentExecution> executions,
            AccessRoleAssignmentExecutionAuditStateResolver.BulkExecutionAuditContext context,
            String fallbackActor) {
        List<AccessRoleAssignmentExecution> safeExecutions = executions != null ? executions : List.of();
        return AccessRoleAssignmentExecutionAuditEvent.allExecutions(
                resolveActorId(fallbackActor),
                resolveActorUsername(fallbackActor),
                tenantId,
                context.highestRiskLevel(),
                safeExecutions.size(),
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getIdentitiesMatched).sum(),
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getRolesAssigned).sum(),
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getRolesReactivated).sum(),
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getRolesRevoked).sum(),
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getSkippedAlreadyAssigned).sum(),
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getSkippedNotMatched).sum(),
                safeExecutions.stream().mapToInt(AccessRoleAssignmentExecution::getErrorCount).sum(),
                context.stateAfter());
    }

    private void publishAndPersist(AccessRoleAssignmentExecutionAuditEvent event) {
        auditPublisher.publish(event);
        auditLogService.persist(event.toAuditLog());
    }

    private String resolveActorId(String fallbackActor) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        if (fallbackActor != null && !fallbackActor.isBlank()) {
            return fallbackActor;
        }
        return com.aidentx.iam.config.AppConstants.AUDIT_ACTOR_SYSTEM_ID;
    }

    private String resolveActorUsername(String fallbackActor) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        if (fallbackActor != null && !fallbackActor.isBlank()) {
            return fallbackActor;
        }
        return com.aidentx.iam.config.AppConstants.AUDIT_ACTOR_SYSTEM_USERNAME;
    }


    private void logAuditFailure(Exception e, String message, UUID ruleId, String tenantId) {
        logger.error()
                .category(AccessRoleAssignmentExecutionAuditConstants.LOG_CATEGORY)
                .message(message)
                .details(Map.of(
                        AccessRoleAssignmentExecutionAuditConstants.DETAIL_TENANT_ID,
                        tenantId != null ? tenantId : AccessRoleAssignmentExecutionAuditConstants.NOT_AVAILABLE,
                        AccessRoleAssignmentExecutionAuditConstants.DETAIL_RULE_ID,
                        ruleId != null ? ruleId.toString() : AccessRoleAssignmentExecutionAuditConstants.NOT_AVAILABLE,
                        AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXCEPTION_TYPE,
                        e.getClass().getSimpleName()
                ))
                .log();
    }
}

