package com.aidentx.iam.access_roles_management.repository;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for querying persistent access role audit logs.
 */
@Repository
public interface AccessRoleAuditLogRepository extends JpaRepository<AccessRoleAuditLog, UUID> {

    /**
     * Finds all audit logs for a specific role, newest first when pageable sort is provided.
     *
     * @param tenantId the tenant identifier
     * @param roleId the role UUID
     * @param pageable page and sort information
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByTenantIdAndRoleId(String tenantId, UUID roleId, Pageable pageable);

    /** Searches role audit logs by actor username or actor id. */
    @Query("""
            SELECT a
            FROM AccessRoleAuditLog a
            WHERE a.tenantId = :tenantId
              AND a.roleId = :roleId
              AND (
                    LOWER(COALESCE(a.actorUsername, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(COALESCE(a.actorId, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<AccessRoleAuditLog> searchByTenantIdAndRoleIdAndActor(@Param("tenantId") String tenantId,
                                                               @Param("roleId") UUID roleId,
                                                               @Param("search") String search,
                                                               Pageable pageable);

    /** Counts audit records referencing one role in a tenant. */
    long countByTenantIdAndRoleId(String tenantId, UUID roleId);

    /**
     * Finds all audit logs for a specific assignment rule, newest first when pageable sort is provided.
     *
     * @param tenantId the tenant identifier
     * @param ruleId the assignment-rule UUID
     * @param pageable page and sort information
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByTenantIdAndRuleId(String tenantId, UUID ruleId, Pageable pageable);

    /** Searches assignment-rule audit logs by actor username or actor id. */
    @Query("""
            SELECT a
            FROM AccessRoleAuditLog a
            WHERE a.tenantId = :tenantId
              AND a.ruleId = :ruleId
              AND (
                    LOWER(COALESCE(a.actorUsername, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(COALESCE(a.actorId, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<AccessRoleAuditLog> searchByTenantIdAndRuleIdAndActor(@Param("tenantId") String tenantId,
                                                               @Param("ruleId") UUID ruleId,
                                                               @Param("search") String search,
                                                               Pageable pageable);

    /**
     * Finds all audit logs for a specific action within a tenant.
     *
     * @param tenantId the tenant identifier
     * @param action the audit action
     * @param pageable page and sort information
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByTenantIdAndAction(String tenantId, String action, Pageable pageable);

    /**
     * Finds all audit logs created by a specific actor within a tenant.
     *
     * @param tenantId the tenant identifier
     * @param actorUsername actor username
     * @param pageable page and sort information
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByTenantIdAndActorUsername(String tenantId, String actorUsername, Pageable pageable);

    /**
     * Finds all audit logs for a role and action within a tenant.
     *
     * @param tenantId the tenant identifier
     * @param roleId role UUID
     * @param action audit action
     * @param pageable page and sort information
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByTenantIdAndRoleIdAndAction(String tenantId, UUID roleId, String action, Pageable pageable);

    /** Searches role audit logs by action and actor username or actor id. */
    @Query("""
            SELECT a
            FROM AccessRoleAuditLog a
            WHERE a.tenantId = :tenantId
              AND a.roleId = :roleId
              AND a.action = :action
              AND (
                    LOWER(COALESCE(a.actorUsername, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(COALESCE(a.actorId, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<AccessRoleAuditLog> searchByTenantIdAndRoleIdAndActionAndActor(@Param("tenantId") String tenantId,
                                                                        @Param("roleId") UUID roleId,
                                                                        @Param("action") String action,
                                                                        @Param("search") String search,
                                                                        Pageable pageable);

    /**
     * Finds all audit logs for an assignment rule and action within a tenant.
     *
     * @param tenantId the tenant identifier
     * @param ruleId assignment-rule UUID
     * @param action audit action
     * @param pageable page and sort information
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByTenantIdAndRuleIdAndAction(String tenantId, UUID ruleId, String action, Pageable pageable);

    /** Searches assignment-rule audit logs by action and actor username or actor id. */
    @Query("""
            SELECT a
            FROM AccessRoleAuditLog a
            WHERE a.tenantId = :tenantId
              AND a.ruleId = :ruleId
              AND a.action = :action
              AND (
                    LOWER(COALESCE(a.actorUsername, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(COALESCE(a.actorId, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<AccessRoleAuditLog> searchByTenantIdAndRuleIdAndActionAndActor(@Param("tenantId") String tenantId,
                                                                        @Param("ruleId") UUID ruleId,
                                                                        @Param("action") String action,
                                                                        @Param("search") String search,
                                                                        Pageable pageable);

    /**
     * Finds all audit logs within an inclusive timestamp range for a tenant.
     *
     * @param tenantId the tenant identifier
     * @param from inclusive start epoch millis
     * @param to inclusive end epoch millis
     * @param pageable page and sort information
     * @return page of audit log records
     */
    Page<AccessRoleAuditLog> findByTenantIdAndCreatedAtBetween(String tenantId, Long from, Long to, Pageable pageable);
}

