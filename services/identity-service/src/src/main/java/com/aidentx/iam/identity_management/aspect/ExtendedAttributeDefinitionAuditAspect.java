package com.aidentx.iam.identity_management.aspect;

import com.aidentx.iam.config.AppConstants;
import com.aidentx.iam.config.audit.AuditEventPublisher;
import com.aidentx.iam.config.audit.AuditUser;
import com.aidentx.iam.config.audit.IdentityAuditEvent;
import com.aidentx.iam.config.audit.SecurityContextService;
import com.aidentx.iam.identity_management.model.dto.ExtendedAttributeDtos.DefinitionCreateWithoutTypeDto;
import com.aidentx.iam.identity_management.model.dto.ExtendedAttributeDtos.DefinitionResponseDto;
import com.aidentx.iam.identity_management.model.dto.ExtendedAttributeDtos.DefinitionUpdateDto;
import com.aidentx.iam.identity_management.model.entity.ExtendedAttributeDefinition;
import com.aidentx.iam.identity_management.repository.ExtendedAttributeDefinitionRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect for auditing changes to extended attribute definitions.
 * This aspect intercepts all operations on the
 * ExtendedAttributeDefinitionService
 * to provide comprehensive audit logging and event publishing for schema
 * changes.
 *
 * <p>
 * This is critical for compliance and tracking who makes what changes to
 * the identity schema, which can affect data collection and validation.
 * </p>
 *
 * <p>
 * The aspect publishes structured audit events to Kafka for downstream
 * processing, monitoring, and compliance reporting.
 * </p>
 *
 * @since 1.0
 */
@Aspect
@Component
public class ExtendedAttributeDefinitionAuditAspect {

    private final IUnifiedLogger logger;
    private final SecurityContextService securityContextService;
    private final AuditEventPublisher auditPublisher;
    private final ExtendedAttributeDefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper;

    public ExtendedAttributeDefinitionAuditAspect(UnifiedLoggerFactory loggerFactory,
            SecurityContextService securityContextService,
            AuditEventPublisher auditPublisher,
            ExtendedAttributeDefinitionRepository definitionRepository,
            ObjectMapper objectMapper) {
        this.logger = loggerFactory.getLogger(ExtendedAttributeDefinitionAuditAspect.class);
        this.securityContextService = securityContextService;
        this.auditPublisher = auditPublisher;
        this.definitionRepository = definitionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Pointcut for all methods in ExtendedAttributeDefinitionService
     */
    @Pointcut("execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.*(..))")
    public void definitionServiceMethods() {
    }

    /**
     * Pointcut for write operations (create, update, delete)
     */
    @Pointcut("execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.createEmployeeDefinition(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.createNonEmployeeDefinition(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.createNonHumanDefinition(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.updateEmployeeDefinition(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.updateNonEmployeeDefinition(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.updateNonHumanDefinition(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.deleteEmployeeDefinitionBySystemKey(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.deleteNonEmployeeDefinitionBySystemKey(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.deleteNonHumanDefinitionBySystemKey(..))")
    public void definitionWriteOperations() {
    }

    /**
     * Pointcut for read operations
     */
    @Pointcut("execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.findEmployeeDefinitions(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.findNonEmployeeDefinitions(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.findNonHumanDefinitions(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.getEmployeeSchemaStatistics(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.getNonEmployeeSchemaStatistics(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.getNonHumanSchemaStatistics(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.getEmployeeDefinitionBySystemKey(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.getNonEmployeeDefinitionBySystemKey(..)) || "
            +
            "execution(* com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService.getNonHumanDefinitionBySystemKey(..))")
    public void definitionReadOperations() {
    }

    /**
     * Around advice for all definition service methods to provide comprehensive
     * auditing
     */
    @Around("definitionServiceMethods()")
    public Object auditDefinitionOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // Extract common information
        String tenantId = extractTenantId(args);
        String operationType = determineOperationType(methodName);

        // Capture before state for write operations BEFORE executing the method
        JsonNode stateBefore = null;
        if (isWriteOperation(methodName)) {
            stateBefore = captureEntityStateBefore(tenantId, methodName, args);
        }

        // Log operation start
        logger.info().message("Extended attribute definition operation started")
                .detail("operation", operationType)
                .detail("method", methodName)
                .detail("tenantId", tenantId)
                .detail("parameters", extractOperationDetails(methodName, args))
                .log();

        long startTime = System.currentTimeMillis();
        Object result = null;

        try {
            // Execute the method
            result = joinPoint.proceed();

            // Log successful operation
            long duration = System.currentTimeMillis() - startTime;
            logger.info().message("Extended attribute definition operation completed successfully")
                    .detail("operation", operationType)
                    .detail("method", methodName)
                    .detail("tenantId", tenantId)
                    .detail("durationMs", duration)
                    .detail("result", extractResultSummary(methodName, result))
                    .log();

            // Publish audit event for write operations
            if (isWriteOperation(methodName)) {
                publishAuditEvent(methodName, args, result, tenantId, stateBefore);
            }

            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;

            // Log failed operation
            logger.error().message("Extended attribute definition operation failed")
                    .detail("operation", operationType)
                    .detail("method", methodName)
                    .detail("tenantId", tenantId)
                    .detail("durationMs", duration)
                    .detail("error", ex.getMessage())
                    .detail("errorType", ex.getClass().getSimpleName())
                    .log();

            throw ex;
        }
    }

    /**
     * After returning advice for write operations to log detailed changes
     */
    @AfterReturning(pointcut = "definitionWriteOperations()", returning = "result")
    public void auditWriteOperationDetails(Object result) {
        // Additional detailed logging for write operations can be added here
        // This could include before/after comparisons, change tracking, etc.
    }

    /**
     * Extracts tenant ID from method arguments
     */
    private String extractTenantId(Object[] args) {
        if (args != null && args.length > 0) {
            // Tenant ID is typically the first parameter
            if (args[0] instanceof String) {
                return (String) args[0];
            }
        }
        return "unknown";
    }

    /**
     * Determines the operation type based on method name
     */
    private String determineOperationType(String methodName) {
        return switch (methodName) {
            case "createEmployeeDefinition", "createNonEmployeeDefinition", "createNonHumanDefinition" ->
                "CREATE_SCHEMA_DEFINITION";
            case "updateEmployeeDefinition", "updateNonEmployeeDefinition", "updateNonHumanDefinition" ->
                "UPDATE_SCHEMA_DEFINITION";
            case "deleteEmployeeDefinitionBySystemKey", "deleteNonEmployeeDefinitionBySystemKey",
                    "deleteNonHumanDefinitionBySystemKey" ->
                "DELETE_SCHEMA_DEFINITION";
            case "findEmployeeDefinitions", "findNonEmployeeDefinitions", "findNonHumanDefinitions" ->
                "READ_SCHEMA_DEFINITIONS";
            case "getEmployeeSchemaStatistics", "getNonEmployeeSchemaStatistics", "getNonHumanSchemaStatistics" ->
                "READ_SCHEMA_STATISTICS";
            case "getEmployeeDefinitionBySystemKey", "getNonEmployeeDefinitionBySystemKey",
                    "getNonHumanDefinitionBySystemKey" ->
                "READ_SCHEMA_DEFINITION";
            default -> "UNKNOWN_SCHEMA_OPERATION";
        };
    }

    /**
     * Checks if the operation is a write operation (create, update, delete)
     */
    private boolean isWriteOperation(String methodName) {
        return switch (methodName) {
            case "createEmployeeDefinition", "createNonEmployeeDefinition", "createNonHumanDefinition",
                    "updateEmployeeDefinition", "updateNonEmployeeDefinition", "updateNonHumanDefinition",
                    "deleteEmployeeDefinitionBySystemKey", "deleteNonEmployeeDefinitionBySystemKey",
                    "deleteNonHumanDefinitionBySystemKey" ->
                true;
            default -> false;
        };
    }

    /**
     * Extracts operation-specific details from method arguments
     */
    private String extractOperationDetails(String methodName, Object[] args) {
        if (args == null || args.length == 0) {
            return "{}";
        }

        return switch (methodName) {
            case "createEmployeeDefinition", "createNonEmployeeDefinition", "createNonHumanDefinition" ->
                extractCreateDetails(methodName, args);
            case "updateEmployeeDefinition", "updateNonEmployeeDefinition", "updateNonHumanDefinition" ->
                extractUpdateDetails(methodName, args);
            case "deleteEmployeeDefinitionBySystemKey", "deleteNonEmployeeDefinitionBySystemKey",
                    "deleteNonHumanDefinitionBySystemKey" ->
                extractDeleteDetails(methodName, args);
            case "findEmployeeDefinitions", "findNonEmployeeDefinitions", "findNonHumanDefinitions" ->
                extractReadDefinitionsDetails(methodName, args);
            case "getEmployeeSchemaStatistics", "getNonEmployeeSchemaStatistics", "getNonHumanSchemaStatistics" ->
                extractReadStatisticsDetails(methodName, args);
            case "getEmployeeDefinitionBySystemKey", "getNonEmployeeDefinitionBySystemKey",
                    "getNonHumanDefinitionBySystemKey" ->
                extractReadDefinitionDetails(methodName, args);
            default -> "Unknown operation details";
        };
    }

    private String extractCreateDetails(String methodName, Object[] args) {
        if (args.length >= 2 && args[1] instanceof DefinitionCreateWithoutTypeDto) {
            DefinitionCreateWithoutTypeDto dto = (DefinitionCreateWithoutTypeDto) args[1];
            String identityType = determineIdentityTypeFromMethod(methodName);
            return String.format(
                    "{\"systemKey\":\"%s\",\"displayName\":\"%s\",\"dataType\":\"%s\",\"identityType\":\"%s\",\"isRequired\":%s}",
                    dto.systemKey(), dto.displayName(), dto.dataType(), identityType, dto.isRequired());
        }
        return "Invalid create parameters";
    }

    private String determineIdentityTypeFromMethod(String methodName) {
        if (methodName.contains("Employee"))
            return "EMPLOYEE";
        if (methodName.contains("NonEmployee"))
            return "NON_EMPLOYEE";
        if (methodName.contains("NonHuman"))
            return "NON_HUMAN";
        return "UNKNOWN";
    }

    private String extractUpdateDetails(String methodName, Object[] args) {
        if (args.length >= 3) {
            String systemKey = args[1] instanceof String ? (String) args[1] : "unknown";
            if (args[2] instanceof DefinitionUpdateDto) {
                DefinitionUpdateDto dto = (DefinitionUpdateDto) args[2];
                String identityType = determineIdentityTypeFromMethod(methodName);
                return String.format(
                        "{\"systemKey\":\"%s\",\"displayName\":\"%s\",\"isRequired\":%s,\"identityType\":\"%s\"}",
                        systemKey, dto.displayName() != null ? dto.displayName() : "unchanged",
                        dto.isRequired() != null ? dto.isRequired().toString() : "unchanged", identityType);
            }
        }
        return "Invalid update parameters";
    }

    private String extractDeleteDetails(String methodName, Object[] args) {
        if (args.length >= 2 && args[1] instanceof String) {
            String systemKey = (String) args[1];
            String identityType = determineIdentityTypeFromMethod(methodName);
            return String.format("{\"systemKey\":\"%s\",\"identityType\":\"%s\"}", systemKey, identityType);
        }
        return "Invalid delete parameters";
    }

    private String extractReadDefinitionsDetails(String methodName, Object[] args) {
        String identityType = determineIdentityTypeFromMethod(methodName);
        return String.format("{\"operation\":\"LIST_DEFINITIONS\",\"identityType\":\"%s\"}", identityType);
    }

    private String extractReadStatisticsDetails(String methodName, Object[] args) {
        String identityType = determineIdentityTypeFromMethod(methodName);
        return String.format("{\"operation\":\"GET_STATISTICS\",\"identityType\":\"%s\"}", identityType);
    }

    private String extractReadDefinitionDetails(String methodName, Object[] args) {
        if (args.length >= 2 && args[1] instanceof String) {
            String systemKey = (String) args[1];
            String identityType = determineIdentityTypeFromMethod(methodName);
            return String.format("{\"operation\":\"GET_DEFINITION\",\"systemKey\":\"%s\",\"identityType\":\"%s\"}",
                    systemKey, identityType);
        }
        return "Invalid read definition parameters";
    }

    /**
     * Extracts summary information from operation results
     */
    private String extractResultSummary(String methodName, Object result) {
        if (result == null) {
            return "null";
        }

        return switch (methodName) {
            case "createDefinition", "updateDefinition" ->
                result.toString().length() > 200 ? "Definition updated successfully" : result.toString();
            case "deleteDefinition" -> "Definition deleted successfully";
            case "findByTenantAndIdentityType" ->
                result instanceof java.util.List ? "Found " + ((java.util.List<?>) result).size() + " definitions"
                        : "Definitions retrieved";
            default -> "Operation completed";
        };
    }

    private JsonNode captureEntityStateBefore(String tenantId, String methodName, Object[] args) {
        try {
            // For delete and update operations, we need to find the entity by systemKey
            if (isWriteOperation(methodName) && args.length >= 2 && args[1] instanceof String) {
                String systemKey = (String) args[1];
                ExtendedAttributeDefinition beforeDef = definitionRepository
                        .findByTenantIdAndSystemKey(tenantId, systemKey)
                        .orElse(null);
                if (beforeDef != null) {
                    return objectMapper.valueToTree(beforeDef);
                }
            }
        } catch (Exception e) {
            logger.warn().message("Could not capture entity state before operation")
                    .detail("method", methodName)
                    .detail("tenantId", tenantId)
                    .detail("error", e.getMessage())
                    .log();
        }
        return null;
    }

    /**
     * Publishes audit events for write operations
     */
    private void publishAuditEvent(String methodName, Object[] args, Object result, String tenantId,
            JsonNode stateBefore) {
        try {
            AuditUser auditUser = securityContextService.getAuditUser();

            switch (methodName) {
                case "createEmployeeDefinition", "createNonEmployeeDefinition", "createNonHumanDefinition" ->
                    publishCreateEvent(auditUser, args, (DefinitionResponseDto) result, tenantId);
                case "updateEmployeeDefinition", "updateNonEmployeeDefinition", "updateNonHumanDefinition" ->
                    publishUpdateEvent(auditUser, args, (DefinitionResponseDto) result, tenantId, stateBefore);
                case "deleteEmployeeDefinitionBySystemKey", "deleteNonEmployeeDefinitionBySystemKey",
                        "deleteNonHumanDefinitionBySystemKey" ->
                    publishDeleteEvent(auditUser, args, tenantId, stateBefore);
                default -> logger.debug().message("No audit event published for non-write operation")
                        .detail("method", methodName)
                        .detail("tenantId", tenantId)
                        .log();
            }
        } catch (Exception e) {
            logger.error().message("Failed to publish audit event")
                    .detail("method", methodName)
                    .detail("tenantId", tenantId)
                    .detail("error", e.getMessage())
                    .log();
        }
    }

    private void publishCreateEvent(AuditUser auditUser, Object[] args, DefinitionResponseDto result, String tenantId) {
        JsonNode stateAfter = objectMapper.valueToTree(result);

        IdentityAuditEvent event = IdentityAuditEvent.create(
                auditUser.id(),
                auditUser.username(),
                auditUser.ipAddress(),
                AppConstants.AUDIT_EVENT_CREATE,
                AppConstants.AUDIT_ENTITY_TYPE_EXTENDED_ATTRIBUTE_DEFINITION,
                result.id().toString(),
                result.displayName(),
                tenantId,
                null, // No before state for create
                stateAfter);

        auditPublisher.publishEvent(event);
        logger.info().message("Extended attribute definition creation audit event published")
                .detail("tenantId", tenantId)
                .detail("definitionId", result.id())
                .log();
    }

    private void publishUpdateEvent(AuditUser auditUser, Object[] args, DefinitionResponseDto result, String tenantId,
            JsonNode stateBefore) {
        JsonNode stateAfter = objectMapper.valueToTree(result);

        IdentityAuditEvent event = IdentityAuditEvent.create(
                auditUser.id(),
                auditUser.username(),
                auditUser.ipAddress(),
                AppConstants.AUDIT_EVENT_UPDATE,
                AppConstants.AUDIT_ENTITY_TYPE_EXTENDED_ATTRIBUTE_DEFINITION,
                result.id().toString(),
                result.displayName(),
                tenantId,
                stateBefore,
                stateAfter);

        auditPublisher.publishEvent(event);
        logger.info().message("Extended attribute definition update audit event published")
                .detail("tenantId", tenantId)
                .detail("definitionId", result.id())
                .log();
    }

    private void publishDeleteEvent(AuditUser auditUser, Object[] args, String tenantId, JsonNode stateBefore) {
        String systemKey = (String) args[1]; // Delete methods use systemKey as identifier

        // Extract the definition ID from the stateBefore if available
        String entityId = systemKey; // Default to systemKey
        if (stateBefore != null && stateBefore.has("id")) {
            entityId = stateBefore.get("id").asText();
        }

        IdentityAuditEvent event = IdentityAuditEvent.create(
                auditUser.id(),
                auditUser.username(),
                auditUser.ipAddress(),
                AppConstants.AUDIT_EVENT_DELETE,
                AppConstants.AUDIT_ENTITY_TYPE_EXTENDED_ATTRIBUTE_DEFINITION,
                entityId,
                stateBefore.get("displayName") != null ? stateBefore.get("displayName").asText() : "Deleted Definition",
                tenantId,
                stateBefore,
                null // No after state for delete
        );

        auditPublisher.publishEvent(event);
        logger.info().message("Extended attribute definition deletion audit event published")
                .detail("tenantId", tenantId)
                .detail("systemKey", systemKey)
                .detail("definitionId", entityId)
                .log();
    }
}