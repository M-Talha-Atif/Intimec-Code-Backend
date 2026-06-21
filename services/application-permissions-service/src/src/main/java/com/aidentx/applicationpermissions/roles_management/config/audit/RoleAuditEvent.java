package com.aidentx.applicationpermissions.roles_management.config.audit;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Domain-specific audit event for role management operations.
 * <p>
 * This record represents audit events for role management entities
 * such as role creation, updates, and deletions. It captures
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
 * @param riskLevel the risk level of the role (low, medium, high)
 * @param timestamp when the audit event occurred
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-01-21
 */
public record RoleAuditEvent(
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
        String riskLevel,
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
     * @param riskLevel the risk level of the role (low, medium, high)
     * @return a new RoleAuditEvent instance
     */
    public static RoleAuditEvent create(
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
            String riskLevel) {
        return new RoleAuditEvent(
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
                riskLevel,
                Instant.now()
        );
    }
}