package com.aidentx.taskmanagement.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.model.entity.TaskRunAuditEvent;
import com.aidentx.taskmanagement.model.enums.TaskAuditEventType;
import com.aidentx.taskmanagement.repository.TaskRunAuditEventRepository;

/**
 * Writes immutable milestone events to the durable PostgreSQL audit trail.
 * <p>
 * Each write is performed in its own independent transaction
 * ({@link Propagation#REQUIRES_NEW}) so that an audit event is committed to the
 * database immediately, even if the calling transaction later rolls back or the
 * task fails mid-execution. This guarantees the audit record survives failures.
 * <p>
 * All methods are fire-and-forget from the caller's perspective — failures are
 * swallowed with a warning log so that an audit write failure never aborts the
 * actual task execution.
 */
@Service
public class TaskRunAuditService {

    private final TaskRunAuditEventRepository repository;
    private final IUnifiedLogger log;

    public TaskRunAuditService(
            TaskRunAuditEventRepository repository,
            UnifiedLoggerFactory loggerFactory) {
        this.repository = repository;
        this.log = loggerFactory.getLogger(TaskRunAuditService.class);
    }

    // ── Write helpers ─────────────────────────────────────────────────────────

    public void info(String executionId, UUID taskId, String tenantId,
                     TaskAuditEventType eventType, String message) {
        persist(executionId, taskId, tenantId, eventType, "INFO", message, null);
    }

    public void info(String executionId, UUID taskId, String tenantId,
                     TaskAuditEventType eventType, String message, String metadata) {
        persist(executionId, taskId, tenantId, eventType, "INFO", message, metadata);
    }

    public void warn(String executionId, UUID taskId, String tenantId,
                     TaskAuditEventType eventType, String message) {
        persist(executionId, taskId, tenantId, eventType, "WARN", message, null);
    }

    public void error(String executionId, UUID taskId, String tenantId,
                      TaskAuditEventType eventType, String message) {
        persist(executionId, taskId, tenantId, eventType, "ERROR", message, null);
    }

    public void error(String executionId, UUID taskId, String tenantId,
                      TaskAuditEventType eventType, String message, String metadata) {
        persist(executionId, taskId, tenantId, eventType, "ERROR", message, metadata);
    }

    // ── Query ──────────────────────────────────────────────────────────────────

    public List<TaskRunAuditEvent> getEventsForExecution(String executionId) {
        return repository.findByExecutionIdOrderByEventTimeAsc(executionId);
    }

    public List<TaskRunAuditEvent> getEventsForTenant(String tenantId) {
        return repository.findByTenantIdOrderByEventTimeDesc(tenantId);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Persists the audit event in a new, independent transaction.
     * The {@link Propagation#REQUIRES_NEW} ensures the row is committed immediately
     * and is not rolled back if the outer task transaction fails.
     * Failures are logged as warnings — they must never propagate to the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(String executionId, UUID taskId, String tenantId,
                        TaskAuditEventType eventType, String level,
                        String message, String metadata) {
        try {
            TaskRunAuditEvent event = TaskRunAuditEvent.builder()
                    .executionId(executionId)
                    .taskId(taskId)
                    .tenantId(tenantId)
                    .eventType(eventType)
                    .level(level)
                    .message(message)
                    .metadata(metadata)
                    .build();
            repository.save(event);
        } catch (Exception e) {
            // Never let an audit write failure abort the task execution
            log.warn().message(String.format(
                "Failed to write audit event [%s] for execution %s: %s",
                eventType, executionId, e.getMessage())).log();
        }
    }
}
