package com.aidentx.configuration.audit.aspect;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.configuration.audit.AuditEventPublisher;
import com.aidentx.configuration.audit.AuditUser;
import com.aidentx.configuration.audit.SecurityContextService;
import com.aidentx.configuration.config.ConfigurationType;
import com.aidentx.configuration.model.entity.Configuration;
import com.aidentx.configuration.service.ConfigurationService;
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
public class ConfigurationServiceAuditAspect {

    private final AuditEventPublisher eventPublisher;
    private final SecurityContextService securityContextService;
    private final ConfigurationService configurationService;
    private final ObjectMapper objectMapper;

    public ConfigurationServiceAuditAspect(AuditEventPublisher eventPublisher,
            SecurityContextService securityContextService,
            ConfigurationService configurationService,
            ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.securityContextService = securityContextService;
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
    }

    @Pointcut("execution(* com.aidentx.configuration.service.ConfigurationService.saveConfiguration(..)) || " +
            "execution(* com.aidentx.configuration.service.ConfigurationService.partialUpdateConfiguration(..))")
    public void saveOrUpdatePointcut() {
    }

    @Pointcut("execution(* com.aidentx.configuration.service.ConfigurationService.deleteConfiguration(..))")
    public void deletePointcut() {
    }

    @Around("saveOrUpdatePointcut()")
    public Object auditSaveOrUpdate(ProceedingJoinPoint pjp) throws Throwable {
        ConfigurationType configType = (ConfigurationType) pjp.getArgs()[0];
        String configKey = (String) pjp.getArgs()[1];
    String tenantId = (String) pjp.getArgs()[2];

        // Capture state BEFORE the method executes
    Optional<Configuration> beforeState = configurationService.getConfiguration(configType, configKey, tenantId);
        JsonNode beforeStateJson = beforeState.map(config -> (JsonNode) objectMapper.valueToTree(config)).orElse(null);

        // Execute the actual service method
        Object result = pjp.proceed();
        Configuration afterState = (Configuration) result;
        JsonNode afterStateJson = objectMapper.valueToTree(afterState);

        String action = beforeState.isPresent() ? "update" : "create";

    publishAuditEvent(action, afterState.getId(), configType.getValue(), configKey, beforeStateJson,
                afterStateJson);

        return result;
    }

    @Around("deletePointcut()")
    public Object auditDelete(ProceedingJoinPoint pjp) throws Throwable {
        ConfigurationType configType = (ConfigurationType) pjp.getArgs()[0];
        String configKey = (String) pjp.getArgs()[1];
    String tenantId = (String) pjp.getArgs()[2];

    Optional<Configuration> beforeState = configurationService.getConfiguration(configType, configKey, tenantId);
        JsonNode beforeStateJson = beforeState.map(config -> (JsonNode) objectMapper.valueToTree(config)).orElse(null);

        Object result = pjp.proceed(); // result is a boolean

        if (beforeState.isPresent() && (Boolean) result) {
            Configuration deletedConfig = beforeState.get();
            publishAuditEvent("delete", deletedConfig.getId(), configType.getValue(), configKey, beforeStateJson, null);
        }

        return result;
    }

    private void publishAuditEvent(String action, UUID objectId, String objectType, String objectName,
            JsonNode stateBefore, JsonNode stateAfter) {
        AuditUser user = securityContextService.getAuditUser();

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
    }
}