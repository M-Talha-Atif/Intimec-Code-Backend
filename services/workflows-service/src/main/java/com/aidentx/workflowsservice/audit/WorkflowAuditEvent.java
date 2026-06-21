package com.aidentx.workflowsservice.audit;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Record representing a workflow audit event.
 * Used to capture workflow-related actions for audit logging.
 *
 * @param actorId The ID of the user performing the action
 * @param actorType The type of actor (user, system)
 * @param actorName The name/username of the actor
 * @param tenantId The tenant ID
 * @param entityId The workflow/execution/approval ID
 * @param entityType The type of entity (workflow, execution, approval)
 * @param action The action performed (created, updated, executed, approved, etc.)
 * @param stateBefore The state before the action
 * @param stateAfter The state after the action
 * @param clientIp The client IP address
 * @param userAgent The user agent string
 */
public record WorkflowAuditEvent(
    UUID actorId,
    String actorType,
    String actorName,
    String tenantId,
    UUID entityId,
    String entityType,
    String action,
    JsonNode stateBefore,
    JsonNode stateAfter,
    String clientIp,
    String userAgent
) {
    /**
     * Creates a workflow audit event with system actor.
     */
    public static WorkflowAuditEvent system(
            String tenantId,
            UUID entityId,
            String entityType,
            String action,
            JsonNode stateBefore,
            JsonNode stateAfter) {
        return new WorkflowAuditEvent(
            null,
            "system",
            "System",
            tenantId,
            entityId,
            entityType,
            action,
            stateBefore,
            stateAfter,
            null,
            null
        );
    }

    /**
     * Creates a workflow audit event with user actor.
     */
    public static WorkflowAuditEvent user(
            UUID actorId,
            String actorName,
            String tenantId,
            UUID entityId,
            String entityType,
            String action,
            JsonNode stateBefore,
            JsonNode stateAfter,
            String clientIp,
            String userAgent) {
        return new WorkflowAuditEvent(
            actorId,
            "user",
            actorName,
            tenantId,
            entityId,
            entityType,
            action,
            stateBefore,
            stateAfter,
            clientIp,
            userAgent
        );
    }
}
