package com.aidentx.iam.config.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Audit event record for extended attribute definition operations.
 *
 * This record represents the audit event that will be sent to Kafka for tracking
 * all CRUD operations performed on extended attribute definitions. It captures:
 * - User information (who performed the action)
 * - Action details (what was done)
 * - Definition information (what entity was affected)
 * - State changes (before and after data)
 * - Timestamp metadata
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // Omit null fields from JSON
public record ExtendedAttributeDefinitionAuditEvent(
        // Who performed the action
        String userId,
        String username,
        String userIp,

        // What happened
        String action, // e.g., "create", "update", "delete"
        String entityType, // e.g., "EXTENDED_ATTRIBUTE_DEFINITION"
        String entityId, // Definition ID or system key
        String definitionName, // Display name for easier identification
        String systemKey, // System key of the definition
        String identityType, // The identity type this definition applies to
        String tenantId, // For tenant-specific auditing

        // The data payload
        JsonNode stateBefore,
        JsonNode stateAfter,

        // Event metadata
        Instant timestamp) {

    /**
     * Creates a new audit event for extended attribute definition operations.
     *
     * @param userId the ID of the user who performed the action
     * @param username the username of the user who performed the action
     * @param userIp the IP address from where the action was performed
     * @param action the type of action performed (create, update, delete)
     * @param entityType the type of entity being audited (EXTENDED_ATTRIBUTE_DEFINITION)
     * @param entityId the unique identifier of the entity
     * @param definitionName the display name for easier identification in audit logs
     * @param systemKey the system key of the definition
     * @param identityType the identity type this definition applies to
     * @param tenantId the tenant ID for tenant-specific auditing
     * @param stateBefore the state of the entity before the operation (null for create)
     * @param stateAfter the state of the entity after the operation (null for delete)
     * @return a new ExtendedAttributeDefinitionAuditEvent with the current timestamp
     */
    public static ExtendedAttributeDefinitionAuditEvent create(String userId, String username, String userIp,
            String action, String entityType, String entityId, String definitionName, String systemKey,
            String identityType, String tenantId, JsonNode stateBefore, JsonNode stateAfter) {
        return new ExtendedAttributeDefinitionAuditEvent(userId, username, userIp, action, entityType,
                entityId, definitionName, systemKey, identityType, tenantId, stateBefore, stateAfter, Instant.now());
    }
}