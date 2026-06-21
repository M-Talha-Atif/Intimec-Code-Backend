package com.aidentx.core.notifications_management.config.audit;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Domain-specific audit event for notification management operations.
 * <p>
 * This record represents audit events for notification management entities
 * such as email configurations, templates, and notifications. It captures
 * the state before and after operations for compliance and monitoring.
 * </p>
 * 
 * @param userId the ID of the user performing the action
 * @param username the username of the user performing the action
 * @param userIp the IP address of the user
 * @param action the action being performed (CREATE, UPDATE, DELETE, etc.)
 * @param entityType the type of entity being operated on
 * @param entityId the unique identifier of the entity
 * @param entityName the name/description of the entity
 * @param tenantId the tenant identifier
 * @param stateBefore the state of the entity before the operation (null for CREATE)
 * @param stateAfter the state of the entity after the operation (null for DELETE)
 * @param timestamp when the audit event occurred
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-22
 */
public record NotificationAuditEvent(
        String userId,
        String username,
        String userIp,
        String action,
        String entityType,
        String entityId,
        String entityName,
        String tenantId,
        JsonNode stateBefore,
        JsonNode stateAfter,
        Instant timestamp
) {
    
    /**
     * Factory method to create a new audit event.
     * 
     * @param userId the ID of the user performing the action
     * @param username the username of the user performing the action
     * @param userIp the IP address of the user
     * @param action the action being performed
     * @param entityType the type of entity being operated on
     * @param entityId the unique identifier of the entity
     * @param entityName the name/description of the entity
     * @param tenantId the tenant identifier
     * @param stateBefore the state before the operation
     * @param stateAfter the state after the operation
     * @return a new NotificationAuditEvent instance
     */
    public static NotificationAuditEvent create(
            String userId,
            String username,
            String userIp,
            String action,
            String entityType,
            String entityId,
            String entityName,
            String tenantId,
            JsonNode stateBefore,
            JsonNode stateAfter) {
        return new NotificationAuditEvent(
                userId,
                username,
                userIp,
                action,
                entityType,
                entityId,
                entityName,
                tenantId,
                stateBefore,
                stateAfter,
                Instant.now()
        );
    }
}
