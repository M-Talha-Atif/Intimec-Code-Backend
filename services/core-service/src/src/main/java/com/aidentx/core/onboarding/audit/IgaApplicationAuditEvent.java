package com.aidentx.core.onboarding.audit;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * Domain-specific audit event for IGA application management operations.
 * <p>
 * Captures CRUD events for applications so they are forwarded to the
 * centralized Kafka audit-log topic for compliance and governance reporting.
 * </p>
 *
 * @param userId        UUID of the acting user (null/system UUID for system operations)
 * @param userIp        IP address of the requesting client
 * @param action        Kafka event name, e.g. {@code "application_created"}
 * @param applicationId UUID of the target application
 * @param applicationName Display name of the target application
 * @param riskLevel     Risk classification: low / medium / high
 * @param tenantId      Tenant identifier
 * @param stateBefore   Entity snapshot before the operation (null for CREATE)
 * @param stateAfter    Entity snapshot after the operation (null for DELETE)
 * @param timestamp     When the event occurred
 */
public record IgaApplicationAuditEvent(
        String userId,
        String userIp,
        String action,
        String applicationId,
        String applicationName,
        String riskLevel,
        String tenantId,
        JsonNode stateBefore,
        JsonNode stateAfter,
        Instant timestamp
) {}
