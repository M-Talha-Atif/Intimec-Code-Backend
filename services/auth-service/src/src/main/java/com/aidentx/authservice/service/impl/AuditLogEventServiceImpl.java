package com.aidentx.authservice.service.impl;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.authservice.service.AuditKafkaProducerService;
import com.aidentx.authservice.service.AuditLogEventService;
import org.springframework.stereotype.Service;

/**
 * Implementation of AuditLogEventService for building and sending audit log events.
 */
@Service
public class AuditLogEventServiceImpl implements AuditLogEventService {

    private final AuditKafkaProducerService auditKafkaProducerService;

    public AuditLogEventServiceImpl(AuditKafkaProducerService auditKafkaProducerService) {
        this.auditKafkaProducerService = auditKafkaProducerService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendAuditEvent(AuditLogKafkaEvent event) {
        auditKafkaProducerService.sendAuditLog(event);
    }
}
