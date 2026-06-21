package com.aidentx.taskmanagement.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aidentx.taskmanagement.model.entity.TaskRunAuditEvent;

/**
 * Repository for the durable PostgreSQL audit trail of task run milestone events.
 * This table is append-only — no updates or deletes should ever be issued through this
 * repository.
 */
@Repository
public interface TaskRunAuditEventRepository extends JpaRepository<TaskRunAuditEvent, UUID> {

    /** All audit events for a given execution run, ordered chronologically. */
    List<TaskRunAuditEvent> findByExecutionIdOrderByEventTimeAsc(String executionId);

    /** All audit events for a tenant, most recent first — used for compliance reporting. */
    List<TaskRunAuditEvent> findByTenantIdOrderByEventTimeDesc(String tenantId);
}
