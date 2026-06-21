package com.aidentx.iam.config.audit;

import com.aidentx.iam.config.AppConstants;
import com.aidentx.iam.identity_management.model.dto.IdentityResponseDto;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Aspect for auditing Identity operations.
 *
 * This aspect intercepts CRUD operations on Identities to capture audit
 * information and publish structured events to Kafka for compliance and
 * monitoring purposes.
 */
@Aspect
@Component
public class IdentityAuditAspect {

    private final IUnifiedLogger logger;
    private final SecurityContextService securityContextService;
    private final AuditEventPublisher auditPublisher;
    private final IdentityRepository identityRepository;
    private final ObjectMapper objectMapper;

    public IdentityAuditAspect(UnifiedLoggerFactory unifiedLoggerFactory,
                              SecurityContextService securityContextService,
                              AuditEventPublisher auditPublisher,
                              IdentityRepository identityRepository,
                              ObjectMapper objectMapper) {
        this.logger = unifiedLoggerFactory.getLogger(IdentityAuditAspect.class);
        this.securityContextService = securityContextService;
        this.auditPublisher = auditPublisher;
        this.identityRepository = identityRepository;
        this.objectMapper = objectMapper;
    }

    // ==================== IDENTITY POINTCUTS ====================

    @Pointcut("execution(public * com.aidentx.iam.identity_management.service.impl.IdentityServiceImpl.createEmployee(..))")
    public void createEmployeePointcut() {}

    @Pointcut("execution(public * com.aidentx.iam.identity_management.service.impl.IdentityServiceImpl.createNonEmployee(..))")
    public void createNonEmployeePointcut() {}

    @Pointcut("execution(public * com.aidentx.iam.identity_management.service.impl.IdentityServiceImpl.createNonHumanIdentity(..))")
    public void createNonHumanIdentityPointcut() {}

    @Pointcut("execution(public * com.aidentx.iam.identity_management.service.impl.IdentityServiceImpl.updateIdentity(..))")
    public void updateIdentityPointcut() {}

    @Pointcut("execution(public * com.aidentx.iam.identity_management.service.impl.IdentityServiceImpl.deleteIdentity(..))")
    public void deleteIdentityPointcut() {}

    // ==================== IDENTITY AUDIT METHODS ====================

    @Around("createEmployeePointcut()")
    public Object auditCreateEmployee(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];
        logger.info().message("Auditing employee creation").detail("tenantId", tenantId).log();

        // Execute the method
        Object result = pjp.proceed();

        // Get user context and publish audit event
        IdentityResponseDto createdIdentity = (IdentityResponseDto) result;
        AuditUser auditUser = securityContextService.getAuditUser();

        JsonNode stateAfter = objectMapper.valueToTree(createdIdentity);

        IdentityAuditEvent event = IdentityAuditEvent.create(
            auditUser.id(),
            auditUser.username(),
            auditUser.ipAddress(),
            AppConstants.AUDIT_EVENT_CREATE,
            AppConstants.AUDIT_ENTITY_TYPE_IDENTITY,
            createdIdentity.id().toString(),
            getIdentityDisplayName(createdIdentity),
            tenantId,
            null, // No before state for create
            stateAfter
        );
        auditPublisher.publishEvent(event);

        logger.info().message("Employee created and audit event published").detail("tenantId", tenantId).log();
        return result;
    }

    @Around("createNonEmployeePointcut()")
    public Object auditCreateNonEmployee(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];
        logger.info().message("Auditing non-employee creation").detail("tenantId", tenantId).log();

        // Execute the method
        Object result = pjp.proceed();

        // Get user context and publish audit event
        IdentityResponseDto createdIdentity = (IdentityResponseDto) result;
        AuditUser auditUser = securityContextService.getAuditUser();

        JsonNode stateAfter = objectMapper.valueToTree(createdIdentity);

        IdentityAuditEvent event = IdentityAuditEvent.create(
            auditUser.id(),
            auditUser.username(),
            auditUser.ipAddress(),
            AppConstants.AUDIT_EVENT_CREATE,
            AppConstants.AUDIT_ENTITY_TYPE_IDENTITY,
            createdIdentity.id().toString(),
            getIdentityDisplayName(createdIdentity),
            tenantId,
            null, // No before state for create
            stateAfter
        );
        auditPublisher.publishEvent(event);

        logger.info().message("Non-employee created and audit event published").detail("tenantId", tenantId).log();
        return result;
    }

    @Around("createNonHumanIdentityPointcut()")
    public Object auditCreateNonHumanIdentity(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];
        logger.info().message("Auditing non-human identity creation").detail("tenantId", tenantId).log();

        // Execute the method
        Object result = pjp.proceed();

        // Get user context and publish audit event
        IdentityResponseDto createdIdentity = (IdentityResponseDto) result;
        AuditUser auditUser = securityContextService.getAuditUser();

        JsonNode stateAfter = objectMapper.valueToTree(createdIdentity);

        IdentityAuditEvent event = IdentityAuditEvent.create(
            auditUser.id(),
            auditUser.username(),
            auditUser.ipAddress(),
            AppConstants.AUDIT_EVENT_CREATE,
            AppConstants.AUDIT_ENTITY_TYPE_IDENTITY,
            createdIdentity.id().toString(),
            getIdentityDisplayName(createdIdentity),
            tenantId,
            null, // No before state for create
            stateAfter
        );
        auditPublisher.publishEvent(event);

        logger.info().message("Non-human identity created and audit event published").detail("tenantId", tenantId).log();
        return result;
    }

    @Around("updateIdentityPointcut()")
    public Object auditUpdateIdentity(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];
        UUID identityId = (UUID) pjp.getArgs()[1];
        logger.info().message("Auditing identity update").detail("tenantId", tenantId).detail("identityId", identityId).log();

        // Get "before" state
        JsonNode stateBefore = null;
        try {
            Identity beforeIdentity = identityRepository.findByTenantIdAndId(tenantId, identityId).orElse(null);
            if (beforeIdentity != null) {
                stateBefore = objectMapper.valueToTree(beforeIdentity);
            }
        } catch (Exception e) {
            logger.warn().message("Could not serialize identity before state").detail("error", e.getMessage()).log();
        }

        // Execute the method
        Object result = pjp.proceed();

        // Get user context and publish audit event
        IdentityResponseDto updatedIdentity = (IdentityResponseDto) result;
        AuditUser auditUser = securityContextService.getAuditUser();

        JsonNode stateAfter = objectMapper.valueToTree(updatedIdentity);

        IdentityAuditEvent event = IdentityAuditEvent.create(
            auditUser.id(),
            auditUser.username(),
            auditUser.ipAddress(),
            AppConstants.AUDIT_EVENT_UPDATE,
            AppConstants.AUDIT_ENTITY_TYPE_IDENTITY,
            updatedIdentity.id().toString(),
            getIdentityDisplayName(updatedIdentity),
            tenantId,
            stateBefore,
            stateAfter
        );
        auditPublisher.publishEvent(event);

        logger.info().message("Identity updated and audit event published").detail("tenantId", tenantId).detail("identityId", identityId).log();
        return result;
    }

    @Around("deleteIdentityPointcut()")
    public Object auditDeleteIdentity(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];
        UUID identityId = (UUID) pjp.getArgs()[1];
        logger.info().message("Auditing identity deletion").detail("tenantId", tenantId).detail("identityId", identityId).log();

        // Get "before" state
        JsonNode stateBefore = null;
        try {
            Identity beforeIdentity = identityRepository.findByTenantIdAndId(tenantId, identityId).orElse(null);
            if (beforeIdentity != null) {
                stateBefore = objectMapper.valueToTree(beforeIdentity);
            }
        } catch (Exception e) {
            logger.warn().message("Could not serialize identity before state").detail("error", e.getMessage()).log();
        }

        // Execute the method
        Object result = pjp.proceed();

        // Get user context and publish audit event
        AuditUser auditUser = securityContextService.getAuditUser();

        IdentityAuditEvent event = IdentityAuditEvent.create(
            auditUser.id(),
            auditUser.username(),
            auditUser.ipAddress(),
            AppConstants.AUDIT_EVENT_DELETE,
            AppConstants.AUDIT_ENTITY_TYPE_IDENTITY,
            identityId.toString(),
            "Deleted Identity", // Display name not available after deletion
            tenantId,
            stateBefore,
            null // No after state for delete
        );
        auditPublisher.publishEvent(event);

        logger.info().message("Identity deleted and audit event published").detail("tenantId", tenantId).detail("identityId", identityId).log();
        return result;
    }

    /**
     * Helper method to get a consistent display name for an identity for audit purposes.
     */
    private String getIdentityDisplayName(IdentityResponseDto identity) {
        if (identity == null) {
            return "Unknown";
        }

        // Try to get a meaningful name based on identity type
        Map<String, Object> coreAttributes = identity.coreAttributes();
        if (coreAttributes != null) {
            switch (identity.identityType()) {
                case "EMPLOYEE":
                    String firstName = (String) coreAttributes.get("firstName");
                    String lastName = (String) coreAttributes.get("lastName");
                    if (firstName != null && lastName != null) {
                        return firstName + " " + lastName;
                    }
                    return (String) coreAttributes.getOrDefault("username", "Unknown Employee");

                case "NON_EMPLOYEE":
                    String neFirstName = (String) coreAttributes.get("firstName");
                    String neLastName = (String) coreAttributes.get("lastName");
                    if (neFirstName != null && neLastName != null) {
                        return neFirstName + " " + neLastName;
                    }
                    return (String) coreAttributes.getOrDefault("username", "Unknown Non-Employee");

                case "NON_HUMAN":
                    return (String) coreAttributes.getOrDefault("serviceName", "Unknown Service");

                default:
                    return "Unknown Identity";
            }
        }

        return "Unknown Identity";
    }
}
