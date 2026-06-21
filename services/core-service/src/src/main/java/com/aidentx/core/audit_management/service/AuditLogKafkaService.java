package com.aidentx.core.audit_management.service;

/**
 * Service interface for processing AuditLog messages received from Kafka.
 *
 * Implementations should handle the logic for saving audit logs to the database.
 */

import com.aidentx.auditlogdto.AuditLogKafkaEvent;

public interface AuditLogKafkaService {
    /**
     * Processes an {@link AuditLogKafkaEvent} received from Kafka.
     * <p>
     * Implementations should transform this DTO into a persistable {@code AuditLog}
     * entity
     * and store it in the database.
     *
     * @param event the audit log event received from Kafka
     */
    void processAuditLog(AuditLogKafkaEvent event);
}
