package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditEvent;
import com.aidentx.iam.access_roles_management.model.dto.AccessRoleAuditHistoryDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for persistent access role audit log operations.
 *
 * <p>Provides methods for persisting DB-backed audit history in parallel
 * with Kafka publishing, and for querying that history directly for UI use.</p>
 */
public interface AccessRoleAuditLogService {

    /**
     * Persists an access role audit event as a DB audit log record.
     *
     * @param event the in-memory audit event
     */
    void persist(AccessRoleAuditEvent event);

    /**
     * Persists a prebuilt access role audit log entity.
     *
     * <p>This overload is used by audit-package components that already resolve
     * their own DB audit shape before delegating the final save here.</p>
     *
     * @param auditLog persistent audit log entity
     */
    void persist(AccessRoleAuditLog auditLog);

    /**
     * Finds all audit records for a given role.
     *
     * @param tenantId tenant identifier
     * @param roleId role UUID
     * @param pageable pagination and sorting
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByRoleId(String tenantId, UUID roleId, Pageable pageable);

    /**
     * Finds all audit records for a given assignment rule.
     *
     * @param tenantId tenant identifier
     * @param ruleId assignment-rule UUID
     * @param pageable pagination and sorting
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByRuleId(String tenantId, UUID ruleId, Pageable pageable);

    /**
     * Finds all audit records for a given action within a tenant.
     *
     * @param tenantId tenant identifier
     * @param action action type
     * @param pageable pagination and sorting
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByAction(String tenantId, String action, Pageable pageable);

    /**
     * Finds all audit records created by a specific actor within a tenant.
     *
     * @param tenantId tenant identifier
     * @param actorUsername actor username
     * @param pageable pagination and sorting
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByActor(String tenantId, String actorUsername, Pageable pageable);

    /**
     * Finds all audit records within a date range for a tenant.
     *
     * @param tenantId tenant identifier
     * @param from inclusive start epoch millis
     * @param to inclusive end epoch millis
     * @param pageable pagination and sorting
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByDateRange(String tenantId, Long from, Long to, Pageable pageable);

    /**
     * Finds all audit records for a role and action within a tenant.
     *
     * @param tenantId tenant identifier
     * @param roleId role UUID
     * @param action audit action
     * @param pageable pagination and sorting
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByRoleIdAndAction(String tenantId, UUID roleId, String action, Pageable pageable);

    /**
     * Finds all audit records for an assignment rule and action within a tenant.
     *
     * @param tenantId tenant identifier
     * @param ruleId assignment-rule UUID
     * @param action audit action
     * @param pageable pagination and sorting
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByRuleIdAndAction(String tenantId, UUID ruleId, String action, Pageable pageable);

    /**
     * Returns UI-friendly audit history for a specific role.
     *
     * @param tenantId tenant identifier
     * @param roleId role UUID
     * @param action optional action filter, nullable
     * @param search optional actor username/id search text, nullable
     * @param pageable pagination and sorting
     * @return page of history DTOs
     */
    Page<AccessRoleAuditHistoryDto> findAuditHistory(String tenantId,
                                                     UUID roleId,
                                                     String action,
                                                     String search,
                                                     Pageable pageable);

    /**
     * Returns UI-friendly audit history for a specific assignment rule.
     *
     * @param tenantId tenant identifier
     * @param ruleId assignment-rule UUID
     * @param action optional action filter, nullable
     * @param search optional actor username/id search text, nullable
     * @param pageable pagination and sorting
     * @return page of history DTOs
     */
    Page<AccessRoleAuditHistoryDto> findRuleAuditHistory(String tenantId,
                                                         UUID ruleId,
                                                         String action,
                                                         String search,
                                                         Pageable pageable);
}

