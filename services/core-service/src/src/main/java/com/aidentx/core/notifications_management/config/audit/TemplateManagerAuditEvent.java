package com.aidentx.core.notifications_management.config.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Record representing an audit event for template manager operations.
 * <p>
 * This record captures all relevant information about template manager
 * create, update, and delete operations for audit trail purposes.
 * </p>
 * 
 * @param templateManagerId the unique identifier of the template manager
 * @param action the audit action performed (CREATE, UPDATE, DELETE)
 * @param userId the unique identifier of the user performing the action
 * @param username the username of the user performing the action
 * @param tenantId the tenant identifier associated with the template manager
 * @param templateData the template manager data (sanitized for sensitive information)
 * @param timestamp the timestamp when the action occurred
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-22
 */
public record TemplateManagerAuditEvent(
    UUID templateManagerId,
    String action,
    UUID userId,
    String username,
    String tenantId,
    Map<String, Object> templateData,
    Map<String, Object> stateBefore,
    Map<String, Object> stateAfter,
    Instant timestamp
) {
    
    /**
     * Creates a new template manager audit event with the current timestamp.
     * 
     * @param templateManagerId the unique identifier of the template manager
     * @param action the audit action performed
     * @param userId the unique identifier of the user
     * @param username the username of the user
     * @param tenantId the tenant identifier
     * @param templateData the template manager data
     * @return a new TemplateManagerAuditEvent instance
     */
    public static TemplateManagerAuditEvent create(
            UUID templateManagerId,
            String action,
            UUID userId,
            String username,
            String tenantId,
            Map<String, Object> templateData) {
        return new TemplateManagerAuditEvent(
            templateManagerId,
            action,
            userId,
            username,
            tenantId,
            templateData,
            null, // stateBefore
            null, // stateAfter
            Instant.now()
        );
    }
    
    /**
     * Creates a new template manager audit event with stateBefore and stateAfter.
     * 
     * @param templateManagerId the unique identifier of the template manager
     * @param action the audit action performed
     * @param userId the unique identifier of the user
     * @param username the username of the user
     * @param tenantId the tenant identifier
     * @param stateBefore the state before the operation
     * @param stateAfter the state after the operation
     * @return a new TemplateManagerAuditEvent instance
     */
    public static TemplateManagerAuditEvent createWithStates(
            UUID templateManagerId,
            String action,
            UUID userId,
            String username,
            String tenantId,
            Map<String, Object> stateBefore,
            Map<String, Object> stateAfter) {
        return new TemplateManagerAuditEvent(
            templateManagerId,
            action,
            userId,
            username,
            tenantId,
            null, // templateData (will use stateBefore/stateAfter instead)
            stateBefore,
            stateAfter,
            Instant.now()
        );
    }
}
