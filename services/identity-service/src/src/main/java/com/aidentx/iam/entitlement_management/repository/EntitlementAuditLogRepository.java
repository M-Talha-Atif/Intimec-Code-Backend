package com.aidentx.iam.entitlement_management.repository;


import com.aidentx.iam.entitlement_management.model.entity.EntitlementAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for querying the entitlement audit log.
 *
 * <p>Provides tenant-scoped, paginated access to audit records
 * filtered by entitlement, action type, actor, rule, or time range.
 */
@Repository
public interface EntitlementAuditLogRepository extends JpaRepository<EntitlementAuditLog, UUID> {

    /**
     * Finds all audit records for a specific entitlement, ordered by most recent first.
     *
     * @param tenantId       the tenant identifier
     * @param entitlementId  the entitlement UUID
     * @param pageable       pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByTenantIdAndEntitlementIdOrderByCreatedAtDesc(
            String tenantId, UUID entitlementId, Pageable pageable);

    /**
     * Finds all audit records for a specific action type within a tenant.
     *
     * @param tenantId  the tenant identifier
     * @param action    the action type (e.g. GOVERNANCE_UPDATED, RULE_EVALUATED)
     * @param pageable  pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByTenantIdAndActionOrderByCreatedAtDesc(
            String tenantId, String action, Pageable pageable);

    /**
     * Finds all audit records performed by a specific actor within a tenant.
     *
     * @param tenantId       the tenant identifier
     * @param actorUsername  the actor's username
     * @param pageable       pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByTenantIdAndActorUsernameOrderByCreatedAtDesc(
            String tenantId, String actorUsername, Pageable pageable);

    /**
     * Finds all audit records for a specific rule within a tenant.
     *
     * @param tenantId  the tenant identifier
     * @param ruleId    the rule UUID
     * @param pageable  pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByTenantIdAndRuleIdOrderByCreatedAtDesc(
            String tenantId, UUID ruleId, Pageable pageable);

    /**
     * Finds all audit records within a time range for a tenant.
     *
     * @param tenantId  the tenant identifier
     * @param from      start time (epoch millis, inclusive)
     * @param to        end time (epoch millis, inclusive)
     * @param pageable  pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String tenantId, Long from, Long to, Pageable pageable);

    /**
     * Finds all audit records for a specific entitlement and action type.
     *
     * @param tenantId       the tenant identifier
     * @param entitlementId  the entitlement UUID
     * @param action         the action type
     * @param pageable       pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByTenantIdAndEntitlementIdAndActionOrderByCreatedAtDesc(
            String tenantId, UUID entitlementId, String action, Pageable pageable);
}
