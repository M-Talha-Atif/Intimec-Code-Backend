package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.service.AccessRoleAuditLogService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Shared publisher for access-role assignment execution audit events.
 *
 * <p>This service is used by both AOP-based manual execution auditing and
 * service-owned CRON auditing so the event shape stays consistent.</p>
 */
@Service
public class AccessRoleAssignmentExecutionAuditService {

    private final AccessRoleAssignmentExecutionAuditStateResolver stateResolver;
    private final AccessRoleAssignmentExecutionAuditPublisher auditPublisher;
    private final AccessRoleAuditLogService auditLogService;
    private final IUnifiedLogger logger;

    public AccessRoleAssignmentExecutionAuditService(AccessRoleAssignmentExecutionAuditStateResolver stateResolver,
                                                     AccessRoleAssignmentExecutionAuditPublisher auditPublisher,
                                                     AccessRoleAuditLogService auditLogService,
                                                     UnifiedLoggerFactory loggerFactory) {
        this.stateResolver = stateResolver;
        this.auditPublisher = auditPublisher;
        this.auditLogService = auditLogService;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAssignmentExecutionAuditConstants.LOG_CATEGORY);
    }

    public void publishSingleExecutionAudit(String tenantId,
                                            AccessRoleAssignmentExecution execution,
                                            UUID fallbackRuleId,
                                            String fallbackActor) {
        try {
            var context = stateResolver.buildSingleExecutionContext(tenantId, execution);
            AccessRoleAssignmentExecutionAuditEvent event = AccessRoleAssignmentExecutionAuditEvent.singleExecution(
                    resolveActorId(fallbackActor),
                    resolveActorUsername(fallbackActor),
                    tenantId,
                    uuidToString(execution != null ? execution.getId() : null),
                    uuidToString(execution != null && execution.getRuleId() != null ? execution.getRuleId() : fallbackRuleId),
                    execution != null ? execution.getRuleName() : null,
                    uuidToString(execution != null ? execution.getRoleId() : null),
                    resolveRoleName(execution, context),
                    resolveRoleType(execution, context),
                    context.role() != null ? context.role().getStatus() : null,
                    context.normalizedRiskLevel(),
                    execution != null && execution.getTriggerType() != null ? execution.getTriggerType().name() : null,
                    execution != null && execution.getStatus() != null ? execution.getStatus().name() : null,
                    execution != null ? execution.getIdentitiesMatched() : 0,
                    execution != null ? execution.getRolesAssigned() : 0,
                    execution != null ? execution.getRolesReactivated() : 0,
                    execution != null ? execution.getRolesRevoked() : 0,
                    execution != null ? execution.getSkippedAlreadyAssigned() : 0,
                    execution != null ? execution.getSkippedNotMatched() : 0,
                    execution != null ? execution.getErrorCount() : 0,
                    context.stateAfter());
            auditPublisher.publish(event);
            auditLogService.persist(event.toAuditLog());
        } catch (Exception e) {
            logger.error()
                    .category(AccessRoleAssignmentExecutionAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentExecutionAuditConstants.LOG_EXECUTE_RULE_AUDIT_FAILED)
                    .details(Map.of(
                            AccessRoleAssignmentExecutionAuditConstants.DETAIL_TENANT_ID,
                            tenantId != null ? tenantId : AccessRoleAssignmentExecutionAuditConstants.NOT_AVAILABLE,
                            AccessRoleAssignmentExecutionAuditConstants.DETAIL_RULE_ID,
                            execution != null && execution.getRuleId() != null
                                    ? execution.getRuleId().toString()
                                    : (fallbackRuleId != null ? fallbackRuleId.toString() : AccessRoleAssignmentExecutionAuditConstants.NOT_AVAILABLE),
                            AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXCEPTION_TYPE,
                            e.getClass().getSimpleName()
                    ))
                    .log();
        }
    }

    private String resolveRoleName(AccessRoleAssignmentExecution execution,
                                   AccessRoleAssignmentExecutionAuditStateResolver.SingleExecutionAuditContext context) {
        if (context.role() != null) {
            return context.role().getName();
        }
        return execution != null ? execution.getRoleName() : null;
    }

    private String resolveRoleType(AccessRoleAssignmentExecution execution,
                                   AccessRoleAssignmentExecutionAuditStateResolver.SingleExecutionAuditContext context) {
        if (context.role() != null) {
            return context.role().getType();
        }
        return execution != null ? execution.getRoleType() : null;
    }

    private String uuidToString(UUID value) {
        return value != null ? value.toString() : null;
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
}
