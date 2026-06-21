package com.aidentx.iam.config.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Audit event record for identity operations.
 * 
 * This record represents the audit event that will be sent to Kafka for tracking
 * all CRUD operations performed on identities. It captures:
 * - User information (who performed the action)
 * - Action details (what was done)
 * - Identity information (what entity was affected)
 * - State changes (before and after data)
 * - Timestamp metadata
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // Omit null fields from JSON
public record IdentityAuditEvent(
        // Who performed the action
        String userId,
        String username,
        String userIp,

        // What happened
        String action, // e.g., "create", "update", "delete"
        String entityType, // e.g., "IDENTITY"
        String entityId, // Identity ID
        String identityName, // For easier identification in audit logs (could be email, username, etc.)
        String tenantId, // For tenant-specific auditing

        // The data payload
        JsonNode stateBefore,
        JsonNode stateAfter,

        // Event metadata
        Instant timestamp) {

    /**
     * Creates a new audit event for identity operations.
     * 
     * @param userId the ID of the user who performed the action
     * @param username the username of the user who performed the action
     * @param userIp the IP address from where the action was performed
     * @param action the type of action performed (create, update, delete)
     * @param entityType the type of entity being audited (IDENTITY)
     * @param entityId the unique identifier of the entity
     * @param identityName the identity name for easier identification in audit logs
     * @param tenantId the tenant ID for tenant-specific auditing
     * @param stateBefore the state of the entity before the operation (null for create)
     * @param stateAfter the state of the entity after the operation (null for delete)
     * @return a new IdentityAuditEvent with the current timestamp
     */
    public static IdentityAuditEvent create(String userId, String username, String userIp, 
            String action, String entityType, String entityId, String identityName, String tenantId,
            JsonNode stateBefore, JsonNode stateAfter) {
        return new IdentityAuditEvent(userId, username, userIp, action, entityType, 
                entityId, identityName, tenantId, stateBefore, stateAfter, Instant.now());
    }
}
