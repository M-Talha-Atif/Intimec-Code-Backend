package com.aidentx.configuration.audit.aspect;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.configuration.audit.AuditEventPublisher;
import com.aidentx.configuration.audit.AuditUser;
import com.aidentx.configuration.audit.SecurityContextService;
import com.aidentx.configuration.model.entity.Artifact;
import com.aidentx.configuration.model.entity.Configuration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Aspect
@Component
public class ArtifactServiceAuditAspect {

    private final AuditEventPublisher eventPublisher;
    private final SecurityContextService securityContextService;
    private final ObjectMapper objectMapper;

    public ArtifactServiceAuditAspect(AuditEventPublisher eventPublisher, SecurityContextService securityContextService,
            ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.securityContextService = securityContextService;
        this.objectMapper = objectMapper;
    }

    @Pointcut("execution(* com.aidentx.configuration.service.ArtifactService.save*(..))")
    public void savePointcut() {
    }

    @Pointcut("execution(* com.aidentx.configuration.service.ArtifactService.deleteArtifact(..))")
    public void deletePointcut() {
    }

    @Around("savePointcut()")
    public Object auditSave(ProceedingJoinPoint pjp) throws Throwable {
        Configuration config = (Configuration) pjp.getArgs()[0];
        String artifactType = (String) pjp.getArgs()[1];

        Optional<Artifact> beforeState = config.getArtifacts().stream()
                .filter(a -> artifactType.equals(a.getArtifactType()))
                .findFirst();
        JsonNode beforeStateJson = beforeState.map(artifact -> (JsonNode) objectMapper.valueToTree(artifact))
                .orElse(null);

        Object result = pjp.proceed();

        Artifact afterState = (Artifact) result;
        JsonNode afterStateJson = objectMapper.valueToTree(afterState);
        String action = beforeState.isPresent() ? "update" : "create";

        publishAuditEvent(action, afterState.getId(), config.getConfigKey(), artifactType, beforeStateJson,
                afterStateJson);

        return result;
    }

    @Around("deletePointcut()")
    public Object auditDelete(ProceedingJoinPoint pjp) throws Throwable {
        Configuration config = (Configuration) pjp.getArgs()[0];
        String artifactType = (String) pjp.getArgs()[1];

        Optional<Artifact> beforeState = config.getArtifacts().stream()
                .filter(a -> artifactType.equals(a.getArtifactType()))
                .findFirst();
        JsonNode beforeStateJson = beforeState.map(artifact -> (JsonNode) objectMapper.valueToTree(artifact))
                .orElse(null);

        Object result = pjp.proceed();

        if (beforeState.isPresent() && (Boolean) result) {
            Artifact deletedArtifact = beforeState.get();
            publishAuditEvent("delete", deletedArtifact.getId(), config.getConfigKey(), artifactType,
                    beforeStateJson, null);
        }

        return result;
    }

        private void publishAuditEvent(String action, UUID objectId, String parentName, String objectType,
                        JsonNode stateBefore, JsonNode stateAfter) {
                AuditUser user;
                try {
                        user = securityContextService.getAuditUser();
                } catch (IllegalStateException ex) {
                        // Graceful degradation: log and skip audit instead of aborting business operation
                        // (Saga threads may lack HTTP/Security context when invoked asynchronously.)
                        return; // silently skip publishing to avoid rollback
                }

                try {
                        AuditLogKafkaEvent event = new AuditLogKafkaEvent(
                                        UUID.fromString(user.id()),
                                        "user",
                                        user.username(),
                                user.tenantId(),
                                        objectId,
                                        objectType,
                                        action,
                                        "success",
                                        "low",
                                        Instant.now(),
                                        null,
                                        stateBefore,
                                        stateAfter,
                                        stateAfter, null, null, null);
                        eventPublisher.publishEvent(event);
                } catch (Exception e) {
                        // Swallow any audit publish exception to protect main flow
                }
        }
}