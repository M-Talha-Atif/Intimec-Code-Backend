package com.aidentx.iam.entitlement_management.audit;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.aidentx.iam.config.AppConstants;
import com.aidentx.iam.entitlement_management.config.constants.EntitlementAuditConstants;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementResponseDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementUpdateDto;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.entitlement_management.service.EntitlementAuditLogService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP aspect for auditing entitlement governance metadata updates.
 *
 * <p>Publishes audit events to Kafka via {@link EntitlementGovernanceAuditPublisher}
 * and persists audit records to the database via {@link EntitlementAuditLogService}.
 */
@Aspect
@Component
public class EntitlementGovernanceAuditAspect {

    private final EntitlementRepository entitlementRepository;
    private final EntitlementGovernanceAuditPublisher auditPublisher;
    private final EntitlementAuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    public EntitlementGovernanceAuditAspect(
            EntitlementRepository entitlementRepository,
            EntitlementGovernanceAuditPublisher auditPublisher,
            EntitlementAuditLogService auditLogService,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.entitlementRepository = entitlementRepository;
        this.auditPublisher = auditPublisher;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AppConstants.AUDIT_LOG_CATEGORY);
    }

    @Around("@annotation(auditAnnotation)")
    public Object auditGovernanceUpdate(
            ProceedingJoinPoint joinPoint,
            AuditEntitlementGovernance auditAnnotation) throws Throwable {

        Object[] args = joinPoint.getArgs();
        UUID entitlementId = (UUID) args[auditAnnotation.idParamIndex()];
        String tenantId = (String) args[auditAnnotation.tenantIdParamIndex()];
        EntitlementUpdateDto updateDto = (EntitlementUpdateDto) args[auditAnnotation.updateDtoParamIndex()];

        // ---------------------------------------------------------------
        // 1. Capture state BEFORE  read directly from DB (pre-mutation)
        // ---------------------------------------------------------------
        JsonNode stateBefore = captureStateBefore(entitlementId, tenantId);

        // ---------------------------------------------------------------
        // 2. Proceed with the actual business method
        // ---------------------------------------------------------------
        Object result = joinPoint.proceed();

        // ---------------------------------------------------------------
        // 3. Capture state AFTER + publish audit (never break the caller)
        // ---------------------------------------------------------------
        try {
            EntitlementResponseDto responseDto = (EntitlementResponseDto) result;
            JsonNode stateAfter = objectMapper.valueToTree(responseDto);

            String actorId = resolveActorId();
            String actorUsername = resolveActorUsername();

            EntitlementGovernanceAuditEvent auditEvent =
                    EntitlementGovernanceAuditEvent.governanceUpdatedFull(
                            actorId, actorUsername, tenantId,
                            responseDto.id() != null ? responseDto.id().toString() : entitlementId.toString(),
                            responseDto.displayName(),
                            responseDto.resourceName(),
                            updateDto.lifecycle(),
                            updateDto.riskLevel(),
                            updateDto.kind(),
                            updateDto.riskReason(),
                            updateDto.requestable(),
                            updateDto.privilegeLevel(),
                            updateDto.ownerId() != null ? updateDto.ownerId().toString() : null,
                            updateDto.requireApproval(),
                            updateDto.allowedAutomatedGrant(),
                            updateDto.maxDurationMs(),
                            updateDto.autoExpireDurationMs(),
                            updateDto.identityType(),
                            stateBefore, stateAfter
                    );

            // Publish to Kafka
            auditPublisher.publish(auditEvent);

            // Persist to DB
            String description = EntitlementAuditConstants.DESC_GOVERNANCE_UPDATED
                    + " \"" + responseDto.displayName() + "\""
                    + " by " + actorUsername;

            auditLogService.logGovernanceUpdate(
                    tenantId,
                    entitlementId,
                    responseDto.displayName(),
                    responseDto.resourceId(),
                    responseDto.resourceName(),
                    actorId,
                    actorUsername,
                    description,
                    stateBefore,
                    stateAfter
            );

        } catch (Exception e) {
            logger.error()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message("Failed to publish governance audit event from aspect")
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_ENTITLEMENT_ID, entitlementId.toString(),
                            AppConstants.AUDIT_DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName(),
                            "exceptionMessage", e.getMessage() != null ? e.getMessage() : "null"
                    ))
                    .log();
        }

        return result;
    }

    // ===================================================================
    //  Helpers
    // ===================================================================

    private JsonNode captureStateBefore(UUID entitlementId, String tenantId) {
        try {
            Optional<Entitlement> existing = entitlementRepository.findById(entitlementId)
                    .filter(e -> e.getTenantId().equals(tenantId));
            if (existing.isPresent()) {
                Entitlement entity = existing.get();
                // Convert to ResponseDto so before/after have the same shape
                EntitlementResponseDto dto = new EntitlementResponseDto(
                        entity.getId(), entity.getTenantId(), entity.getResourceId(),
                        entity.getSourceUid(), "",
                        entity.getDisplayName(), entity.getDescription(),
                        entity.getObjectClass(), entity.getKind(),
                    entity.getAttributes(), Collections.emptyMap(), entity.getLifecycle(),
                        entity.getRiskLevel(), entity.getRiskReason(),
                        entity.isRequestable(), entity.getClassification(),
                        entity.getIdentityType(), entity.getPrivilegeLevel(),
                        entity.getOwnerId(), entity.isRequireApproval(),
                        entity.isAllowedAutomatedGrant(), entity.getMaxDurationMs(),
                        entity.getAutoExpireDurationMs(), 0L,
                        entity.getCreatedAt(), entity.getUpdatedAt()
                );
                return objectMapper.valueToTree(dto);
            }
        } catch (Exception e) {
            logger.warn()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message("Could not capture pre-update state for audit")
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_ENTITLEMENT_ID, entitlementId.toString(),
                            AppConstants.AUDIT_DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName()
                    ))
                    .log();
        }
        return null;
    }

    private String resolveActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            return auth.getName();
        }
        return AppConstants.AUDIT_ACTOR_SYSTEM_ID;
    }

    private String resolveActorUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            return auth.getName();
        }
        return AppConstants.AUDIT_ACTOR_SYSTEM_ID;
    }
}