package com.aidentx.iam.entitlement_management.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignmentRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link EntitlementAssignmentRule} entities.
 *
 * <p>Provides standard CRUD, tenant-scoped queries, schedule management,
 * and cron poller support with distributed locking.
 */
@Repository
public interface EntitlementAssignmentRuleRepository extends JpaRepository<EntitlementAssignmentRule, UUID> {

    /**
     * Lists all rules for a tenant with pagination.
     *
     * @param tenantId the tenant identifier
     * @param pageable pagination and sorting parameters
     * @return page of rules
     */
    Page<EntitlementAssignmentRule> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Finds a rule by its ID and tenant ID.
     *
     * @param id       the rule UUID
     * @param tenantId the tenant identifier
     * @return optional containing the rule, or empty
     */
    Optional<EntitlementAssignmentRule> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Finds the assignment rule for a specific entitlement within a tenant.
     *
     * <p>Since one rule per entitlement is enforced, this returns at most one result.
     * Used by the service layer for duplicate detection on create and by the
     * frontend's GET-by-entitlement flow.
     *
     * @param tenantId      the tenant identifier
     * @param entitlementId the entitlement UUID
     * @return optional containing the rule, or empty if no rule exists
     */
    Optional<EntitlementAssignmentRule> findByTenantIdAndEntitlementId(String tenantId, UUID entitlementId);

    /**
     * Finds all enabled rules for a tenant, ordered by priority.
     *
     * @param tenantId the tenant identifier
     * @return list of enabled rules sorted by priority ascending
     */
    List<EntitlementAssignmentRule> findByTenantIdAndEnabledTrueOrderByPriorityAsc(String tenantId);

    /**
     * Finds all enabled rules for a tenant and entitlement, ordered by priority.
     * Used during entitlement-scoped dynamic revocation so an assignment is kept
     * when any enabled rule for that entitlement still matches the identity.
     */
    List<EntitlementAssignmentRule> findByTenantIdAndEntitlementIdAndEnabledTrueOrderByPriorityAsc(
            String tenantId, UUID entitlementId);

    /**
     * Checks if a rule already exists for an entitlement within a tenant.
     *
     * @param tenantId      the tenant identifier
     * @param entitlementId the entitlement UUID
     * @return true if a rule exists
     */
    boolean existsByTenantIdAndEntitlementId(String tenantId, UUID entitlementId);

    /**
     * Find all enabled IMMEDIATE rules for continuous reconciliation.
     *
     * @param tenantId the tenant identifier
     * @return list of IMMEDIATE rules sorted by priority
     */
    @Query("SELECT r FROM EntitlementAssignmentRule r WHERE r.tenantId = :tenantId " +
            "AND r.enabled = true AND r.scheduleType = 'IMMEDIATE' ORDER BY r.priority ASC")
    List<EntitlementAssignmentRule> findImmediateRules(@Param("tenantId") String tenantId);

    /**
     * Find all enabled CRON rules for scheduled evaluation.
     *
     * @param tenantId the tenant identifier
     * @return list of CRON rules sorted by priority
     */
    @Query("SELECT r FROM EntitlementAssignmentRule r WHERE r.tenantId = :tenantId " +
            "AND r.enabled = true AND r.scheduleType = 'CRON' ORDER BY r.priority ASC")
    List<EntitlementAssignmentRule> findCronRules(@Param("tenantId") String tenantId);

    /**
     * Find all enabled rules for a tenant, regardless of schedule type.
     *
     * <p>Used by {@code evaluateAllRules()} to evaluate all rules including
     * MANUAL rules that would not otherwise be auto-evaluated.
     *
     * @param tenantId the tenant identifier
     * @return list of all enabled rules sorted by priority
     */
    @Query("SELECT r FROM EntitlementAssignmentRule r WHERE r.tenantId = :tenantId " +
            "AND r.enabled = true ORDER BY r.priority ASC")
    List<EntitlementAssignmentRule> findAllEnabledRules(@Param("tenantId") String tenantId);

    /**
     * Bulk update schedule type and cron configuration for all enabled rules in a tenant.
     *
     * @param tenantId       the tenant identifier
     * @param scheduleType   the new schedule type
     * @param cronExpression the new cron expression (null to clear)
     * @param scheduleTime   the new schedule time (null to clear)
     * @param scheduleDate   the new schedule date (null to clear)
     * @param scheduleTimezone the new timezone (null to clear)
     * @param now            current epoch millis for updatedAt
     * @return the number of rules updated
     */
    @Modifying
    @Query("UPDATE EntitlementAssignmentRule r SET r.scheduleType = :scheduleType, " +
            "r.cronExpression = :cronExpression, r.scheduleTime = :scheduleTime, " +
            "r.scheduleDate = :scheduleDate, r.scheduleTimezone = :scheduleTimezone, " +
            "r.updatedAt = :now WHERE r.tenantId = :tenantId AND r.enabled = true")
    int updateGlobalScheduleForAllEnabled(
            @Param("tenantId") String tenantId,
            @Param("scheduleType") EntitlementAssignmentRule.ScheduleType scheduleType,
            @Param("cronExpression") String cronExpression,
            @Param("scheduleTime") String scheduleTime,
            @Param("scheduleDate") String scheduleDate,
            @Param("scheduleTimezone") String scheduleTimezone,
            @Param("now") Long now);

    /**
     * Counts enabled rules for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return the count of enabled rules
     */
    @Query("SELECT COUNT(r) FROM EntitlementAssignmentRule r WHERE r.tenantId = :tenantId AND r.enabled = true")
    long countEnabledRules(@Param("tenantId") String tenantId);

    /**
     * Finds all CRON rules that are due for execution.
     *
     * <p>A rule is "due" when its {@code nextExecutionAt} is less than or equal
     * to the current time. Uses native query with {@code FOR UPDATE SKIP LOCKED}
     * to ensure only one app instance evaluates each rule (distributed safety).
     *
     * <p>{@code SKIP LOCKED} means: if another instance already locked this row,
     * skip it instead of waiting. This prevents duplicate evaluations and
     * eliminates blocking between instances.
     *
     * @param tenantId the tenant identifier
     * @param now      the current time in epoch millis
     * @return list of rules due for execution (locked for this transaction)
     */
    @Query(value = "SELECT * FROM entitlement_assignment_rules " +
            "WHERE tenant_id = :tenantId " +
            "AND schedule_type = 'CRON' " +
            "AND enabled = true " +
            "AND next_execution_at IS NOT NULL " +
            "AND next_execution_at <= :now " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<EntitlementAssignmentRule> findDueCronRules(
            @Param("tenantId") String tenantId,
            @Param("now") Long now);

    /**
     * Finds all distinct tenant IDs that have enabled CRON rules.
     *
     * <p>Used by the cron poller to know which tenants to poll.
     * Avoids hardcoding tenant IDs in configuration.
     *
     * @return list of unique tenant IDs
     */
    @Query("SELECT DISTINCT r.tenantId FROM EntitlementAssignmentRule r " +
            "WHERE r.scheduleType = 'CRON' AND r.enabled = true")
    List<String> findTenantsWithCronRules();

    /**
     * Bulk updates next_execution_at for all enabled CRON rules of a tenant.
     *
     * <p>Used when global schedule switches to IMMEDIATE or MANUAL (set null)
     * or when the calculator needs to be re-run for all rules.
     *
     * @param tenantId        the tenant identifier
     * @param nextExecutionAt the new value (null to clear)
     * @param now             current epoch millis for updatedAt
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE EntitlementAssignmentRule r SET r.nextExecutionAt = :nextExecutionAt, " +
            "r.updatedAt = :now " +
            "WHERE r.tenantId = :tenantId AND r.scheduleType = 'CRON' AND r.enabled = true")
    int bulkUpdateNextExecutionAt(
            @Param("tenantId") String tenantId,
            @Param("nextExecutionAt") Long nextExecutionAt,
            @Param("now") Long now);

    /** Delete all assignment rules for a resource within a tenant (cascade delete on application removal). */
    @Modifying
    @Query("DELETE FROM EntitlementAssignmentRule r WHERE r.resourceId = :resourceId AND r.tenantId = :tenantId")
    void deleteAllByResourceIdAndTenantId(@Param("resourceId") UUID resourceId, @Param("tenantId") String tenantId);
}