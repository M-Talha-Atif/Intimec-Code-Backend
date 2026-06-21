package com.aidentx.taskmanagement.audit;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * Domain-specific audit event for IGA task management operations.
 * <p>
 * Covers task CRUD plus execution lifecycle events so they can be forwarded
 * to the centralized Kafka {@code audit-log} topic.
 * </p>
 *
 * @param userId       UUID of the acting user (null resolved to system UUID by the publisher)
 * @param userIp       Client IP address extracted from the HTTP request
 * @param action       Kafka event name, e.g. {@code "task_created"}, {@code "task_executed"}
 * @param taskId       UUID of the target task
 * @param taskName     Display name of the target task
 * @param applicationId UUID of the application that owns the task
 * @param riskLevel    Risk classification: low / medium / high
 * @param tenantId     Tenant identifier
 * @param stateBefore  Entity snapshot before the operation (null for CREATE)
 * @param stateAfter   Entity snapshot after the operation (null for DELETE / TERMINATE)
 * @param timestamp    When the event occurred
 */
public record IgaTaskAuditEvent(
        String userId,
        String userIp,
        String action,
        String taskId,
        String taskName,
        String applicationId,
        String riskLevel,
        String tenantId,
        JsonNode stateBefore,
        JsonNode stateAfter,
        Instant timestamp
) {}
