package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleResponse;
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
import java.util.Optional;
import java.util.UUID;

/**
 * AOP aspect for access-role assignment rule CRUD auditing.
 *
 * <p>The aspect keeps rule CRUD services free from audit code by intercepting
 * stable service methods, resolving before/after state through a transactional
 * state resolver, publishing Kafka audit events, and persisting DB history via
 * {@link AccessRoleAuditLogService}.</p>
 */
@Aspect
@Component
public class AccessRoleAssignmentRuleAuditAspect {

    private final AccessRoleAssignmentRuleAuditStateResolver stateResolver;
    private final AccessRoleAssignmentRuleAuditPublisher auditPublisher;
    private final AccessRoleAuditLogService auditLogService;
    private final IUnifiedLogger logger;

    public AccessRoleAssignmentRuleAuditAspect(AccessRoleAssignmentRuleAuditStateResolver stateResolver,
                                               AccessRoleAssignmentRuleAuditPublisher auditPublisher,
                                               AccessRoleAuditLogService auditLogService,
                                               UnifiedLoggerFactory loggerFactory) {
        this.stateResolver = stateResolver;
        this.auditPublisher = auditPublisher;
        this.auditLogService = auditLogService;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAssignmentRuleAuditConstants.LOG_CATEGORY);
    }

    @Pointcut("execution(public * com.aidentx.iam.access_roles_management.service.impl.AccessRoleAssignmentRuleServiceImpl.createRule(..))")
    public void createRulePointcut() {
    }

    @Pointcut("execution(public * com.aidentx.iam.access_roles_management.service.impl.AccessRoleAssignmentRuleServiceImpl.updateRule(..))")
    public void updateRulePointcut() {
    }

    @Pointcut("execution(public * com.aidentx.iam.access_roles_management.service.impl.AccessRoleAssignmentRuleServiceImpl.deleteRule(..))")
    public void deleteRulePointcut() {
    }

    /**
     * Audits assignment rule creation.
     */
    @Around("createRulePointcut()")
    public Object auditCreateRule(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];

        Object result = pjp.proceed();

        try {
            AccessRoleAssignmentRuleResponse response = (AccessRoleAssignmentRuleResponse) result;
            Optional<AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot> afterSnapshot =
                    response.getId() != null
                            ? stateResolver.captureRuleSnapshot(tenantId, response.getId())
                            : Optional.empty();

            AccessRoleAssignmentRuleAuditEvent event = AccessRoleAssignmentRuleAuditEvent.ruleCreated(
                    resolveActorId(),
                    resolveActorUsername(),
                    tenantId,
                    stringify(response.getId()),
                    response.getName(),
                    stringify(response.getRoleId()),
                    response.getRoleName(),
                    response.getRoleType(),
                    response.getRoleStatus(),
                    afterSnapshot.map(snapshot -> snapshot.role() != null ? snapshot.role().getRiskLevel() : null).orElse(null),
                    response.isEnabled(),
                    response.getScheduleType(),
                    response.getPriority(),
                    response.getConditionGroups() != null ? response.getConditionGroups().size() : 0,
                    response.getIdentityTypes(),
                    afterSnapshot.map(AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot::state)
                            .orElseGet(() -> stateResolver.buildRuleState(response))
            );
            publishAndPersist(event);
        } catch (Exception e) {
            logAuditFailure(e, AccessRoleAssignmentRuleAuditConstants.LOG_CREATE_RULE_AUDIT_FAILED, null, tenantId);
        }

        return result;
    }

    /**
     * Audits assignment rule updates with diff-only before/after state.
     */
    @Around("updateRulePointcut()")
    public Object auditUpdateRule(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];
        UUID ruleId = (UUID) pjp.getArgs()[1];

        Optional<AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot> beforeSnapshot =
                stateResolver.captureRuleSnapshot(tenantId, ruleId);

        if (beforeSnapshot.isEmpty()) {
            logger.warn()
                    .category(AccessRoleAssignmentRuleAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentRuleAuditConstants.LOG_RULE_NOT_FOUND_FOR_AUDIT)
                    .details(Map.of(
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_TENANT_ID, tenantId,
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_RULE_ID, ruleId != null ? ruleId.toString() : AccessRoleAssignmentRuleAuditConstants.NOT_AVAILABLE
                    ))
                    .log();
        }

        Object result = pjp.proceed();

        try {
            AccessRoleAssignmentRuleResponse response = (AccessRoleAssignmentRuleResponse) result;
            Optional<AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot> afterSnapshot =
                    response.getId() != null
                            ? stateResolver.captureRuleSnapshot(tenantId, response.getId())
                            : Optional.empty();
            var afterState = afterSnapshot.map(AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot::state)
                    .orElseGet(() -> stateResolver.buildRuleState(response));
            var diff = stateResolver.buildDiff(beforeSnapshot.map(AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot::state).orElse(null), afterState);

            AccessRoleAssignmentRuleAuditEvent event = AccessRoleAssignmentRuleAuditEvent.ruleUpdated(
                    resolveActorId(),
                    resolveActorUsername(),
                    tenantId,
                    stringify(response.getId()),
                    response.getName(),
                    stringify(response.getRoleId()),
                    response.getRoleName(),
                    response.getRoleType(),
                    response.getRoleStatus(),
                    afterSnapshot.map(snapshot -> snapshot.role() != null ? snapshot.role().getRiskLevel() : null)
                            .orElseGet(() -> beforeSnapshot.map(snapshot -> snapshot.role() != null ? snapshot.role().getRiskLevel() : null).orElse(null)),
                    response.isEnabled(),
                    response.getScheduleType(),
                    response.getPriority(),
                    response.getConditionGroups() != null ? response.getConditionGroups().size() : 0,
                    response.getIdentityTypes(),
                    diff.before(),
                    diff.after()
            );
            publishAndPersist(event);
        } catch (Exception e) {
            logAuditFailure(e, AccessRoleAssignmentRuleAuditConstants.LOG_UPDATE_RULE_AUDIT_FAILED, ruleId, tenantId);
        }

        return result;
    }

    /**
     * Audits assignment rule deletion using the pre-delete snapshot.
     */
    @Around("deleteRulePointcut()")
    public Object auditDeleteRule(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];
        UUID ruleId = (UUID) pjp.getArgs()[1];

        Optional<AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot> beforeSnapshot =
                stateResolver.captureRuleSnapshot(tenantId, ruleId);

        Object result = pjp.proceed();

        try {
            AccessRoleAssignmentRuleAuditEvent event = AccessRoleAssignmentRuleAuditEvent.ruleDeleted(
                    resolveActorId(),
                    resolveActorUsername(),
                    tenantId,
                    beforeSnapshot.map(snapshot -> stringify(snapshot.rule().getId())).orElse(stringify(ruleId)),
                    beforeSnapshot.map(snapshot -> snapshot.rule().getName()).orElse(AccessRoleAssignmentRuleAuditConstants.UNKNOWN),
                    beforeSnapshot.map(snapshot -> stringify(snapshot.rule().getRoleId())).orElse(null),
                    beforeSnapshot.map(snapshot -> snapshot.role() != null ? snapshot.role().getName() : null).orElse(AccessRoleAssignmentRuleAuditConstants.UNKNOWN),
                    beforeSnapshot.map(snapshot -> snapshot.role() != null ? snapshot.role().getType() : null).orElse(null),
                    beforeSnapshot.map(snapshot -> snapshot.role() != null ? snapshot.role().getStatus() : null).orElse(null),
                    beforeSnapshot.map(snapshot -> snapshot.role() != null ? snapshot.role().getRiskLevel() : null).orElse(null),
                    beforeSnapshot.map(snapshot -> snapshot.rule().isEnabled()).orElse(false),
                    beforeSnapshot.map(snapshot -> snapshot.rule().getScheduleType() != null ? snapshot.rule().getScheduleType().name() : null).orElse(null),
                    beforeSnapshot.map(snapshot -> snapshot.rule().getPriority()).orElse(0),
                    beforeSnapshot.map(AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot::conditionGroupCount).orElse(0),
                    beforeSnapshot.map(AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot::identityTypes).orElse(List.of()),
                    beforeSnapshot.map(AccessRoleAssignmentRuleAuditStateResolver.RuleAuditSnapshot::state).orElse(null)
            );
            publishAndPersist(event);
        } catch (Exception e) {
            logAuditFailure(e, AccessRoleAssignmentRuleAuditConstants.LOG_DELETE_RULE_AUDIT_FAILED, ruleId, tenantId);
        }

        return result;
    }

    private void publishAndPersist(AccessRoleAssignmentRuleAuditEvent event) {
        auditPublisher.publish(event);
        auditLogService.persist(event.toAuditLog());
    }

    private String resolveActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return com.aidentx.iam.config.AppConstants.AUDIT_ACTOR_SYSTEM_ID;
    }

    private String resolveActorUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return com.aidentx.iam.config.AppConstants.AUDIT_ACTOR_SYSTEM_USERNAME;
    }

    private String stringify(UUID value) {
        return value != null ? value.toString() : null;
    }

    private void logAuditFailure(Exception e, String message, UUID ruleId, String tenantId) {
        logger.error()
                .category(AccessRoleAssignmentRuleAuditConstants.LOG_CATEGORY)
                .message(message)
                .details(Map.of(
                        AccessRoleAssignmentRuleAuditConstants.DETAIL_TENANT_ID,
                        tenantId != null ? tenantId : AccessRoleAssignmentRuleAuditConstants.NOT_AVAILABLE,
                        AccessRoleAssignmentRuleAuditConstants.DETAIL_RULE_ID,
                        ruleId != null ? ruleId.toString() : AccessRoleAssignmentRuleAuditConstants.NOT_AVAILABLE,
                        AccessRoleAssignmentRuleAuditConstants.DETAIL_EXCEPTION_TYPE,
                        e.getClass().getSimpleName(),
                        AccessRoleAssignmentRuleAuditConstants.DETAIL_EXCEPTION_MESSAGE,
                        e.getMessage() != null ? e.getMessage() : AccessRoleAssignmentRuleAuditConstants.NOT_AVAILABLE
                ))
                .log();
    }
}
