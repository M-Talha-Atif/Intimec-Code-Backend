package com.aidentx.authservice.service;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;

/**
 * Service interface for building and sending audit log events in a professional, production-grade way.
 */
public interface AuditLogEventService {

    /**
     * Sends a general audit log event.
     *
     * @param event The audit log event to send
     */
    void sendAuditEvent(AuditLogKafkaEvent event);
}
