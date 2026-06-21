package com.aidentx.access_portal_service.dto.response.access_request;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for an audit log entry.
 */
@Getter
@Setter
public class AuditLogEntryDTO {
    private UUID logId;
    private String action;

    // CHANGED: Single performedBy field (matches entity)
    private String performedBy;

    // CHANGED: performedAt -> timestamp (matches entity field name)
    private OffsetDateTime performedAt;

    private String previousStatus;
    private String newStatus;
    private String details;
}