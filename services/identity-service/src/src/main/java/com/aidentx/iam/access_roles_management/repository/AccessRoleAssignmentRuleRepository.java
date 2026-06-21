package com.aidentx.iam.access_roles_management.repository;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for tenant-scoped {@link AccessRoleAssignmentRule} CRUD and
 * schedule lookup operations.
 */
@Repository
public interface AccessRoleAssignmentRuleRepository extends JpaRepository<AccessRoleAssignmentRule, UUID> {

    /**
     * Lists all access-role assignment rules for a tenant.
     *
     * @param tenantId tenant identifier
     * @param pageable pagination and sorting metadata
     * @return page of matching rules
     */
    Page<AccessRoleAssignmentRule> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Finds a rule by ID within a tenant.
     *
     * @param id rule identifier
     * @param tenantId tenant identifier
     * @return matching rule, if present
     */
    Optional<AccessRoleAssignmentRule> findByIdAndTenantId(UUID id, String tenantId);

    /** Batch-loads rules by ID within one tenant. */
    List<AccessRoleAssignmentRule> findByTenantIdAndIdIn(String tenantId, Collection<UUID> ids);

    /**
     * Finds the assignment rule targeting a specific role within a tenant.
     *
     * @param tenantId tenant identifier
     * @param roleId access role identifier
     * @return matching rule, if present
     */
    Optional<AccessRoleAssignmentRule> findByTenantIdAndRoleId(String tenantId, UUID roleId);

    /** Counts assignment rules targeting one role in a tenant. */
    long countByTenantIdAndRoleId(String tenantId, UUID roleId);

    /** Counts enabled assignment rules targeting one role in a tenant. */
    long countByTenantIdAndRoleIdAndEnabledTrue(String tenantId, UUID roleId);

    /**
     * Checks whether a tenant already has a rule for the given role.
     *
     * @param tenantId tenant identifier
     * @param roleId access role identifier
     * @return true when a rule already exists
     */
    boolean existsByTenantIdAndRoleId(String tenantId, UUID roleId);

    /**
     * Finds all enabled rules for a tenant in deterministic priority order.
     *
     * @param tenantId tenant identifier
     * @return enabled rules ordered by priority
     */
    List<AccessRoleAssignmentRule> findByTenantIdAndEnabledTrueOrderByPriorityAsc(String tenantId);

    /**
     * Finds enabled rules for a tenant and schedule type.
     *
     * @param tenantId tenant identifier
     * @param scheduleType schedule type to match
     * @return enabled rules ordered by priority
     */
    List<AccessRoleAssignmentRule> findByTenantIdAndScheduleTypeAndEnabledTrueOrderByPriorityAsc(
            String tenantId,
            AccessRoleAssignmentRule.ScheduleType scheduleType);

    /**
     * Finds all rules for a tenant by schedule type in deterministic priority order.
     *
     * @param tenantId tenant identifier
     * @param scheduleType schedule type to match
     * @return matching rules ordered by priority
     */
    List<AccessRoleAssignmentRule> findByTenantIdAndScheduleTypeOrderByPriorityAsc(
            String tenantId,
            AccessRoleAssignmentRule.ScheduleType scheduleType);

    /**
     * Bulk-updates schedule fields for all rules of a tenant.
     *
     * @param tenantId tenant identifier
     * @param scheduleType new schedule type
     * @param cronExpression cron expression to persist, or null
     * @param scheduleTime schedule time to persist, or null
     * @param scheduleDate schedule date to persist, or null
     * @param scheduleTimezone schedule timezone to persist, or null
     * @param now current epoch millis for updatedAt
     * @return number of tenant rules updated
     */
    @Modifying
    @Query("UPDATE AccessRoleAssignmentRule r SET r.scheduleType = :scheduleType, " +
            "r.cronExpression = :cronExpression, r.scheduleTime = :scheduleTime, " +
            "r.scheduleDate = :scheduleDate, r.scheduleTimezone = :scheduleTimezone, " +
            "r.updatedAt = :now WHERE r.tenantId = :tenantId")
    int updateGlobalScheduleForAllRules(
            @Param("tenantId") String tenantId,
            @Param("scheduleType") AccessRoleAssignmentRule.ScheduleType scheduleType,
            @Param("cronExpression") String cronExpression,
            @Param("scheduleTime") String scheduleTime,
            @Param("scheduleDate") String scheduleDate,
            @Param("scheduleTimezone") String scheduleTimezone,
            @Param("now") Long now);

    /**
     * Bulk-updates nextExecutionAt for all rules of a tenant.
     *
     * @param tenantId tenant identifier
     * @param nextExecutionAt new next execution value, or null to clear
     * @param now current epoch millis for updatedAt
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE AccessRoleAssignmentRule r SET r.nextExecutionAt = :nextExecutionAt, r.updatedAt = :now " +
            "WHERE r.tenantId = :tenantId")
    int bulkUpdateNextExecutionAt(
            @Param("tenantId") String tenantId,
            @Param("nextExecutionAt") Long nextExecutionAt,
            @Param("now") Long now);

    /**
     * Finds CRON rules due for execution. This is provided for future evaluation
     * workers and uses row locking to avoid duplicate processing across nodes.
     *
     * @param tenantId tenant identifier
     * @param now current epoch milliseconds
     * @return locked due CRON rules
     */
    @Query(value = "SELECT * FROM access_role_assignment_rules " +
            "WHERE tenant_id = :tenantId " +
            "AND schedule_type = 'CRON' " +
            "AND enabled = true " +
            "AND next_execution_at IS NOT NULL " +
            "AND next_execution_at <= :now " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<AccessRoleAssignmentRule> findDueCronRules(@Param("tenantId") String tenantId,
                                                    @Param("now") Long now);

    /**
     * Returns tenant IDs that currently have enabled CRON access-role rules.
     *
     * @return distinct tenant identifiers
     */
    @Query("SELECT DISTINCT r.tenantId FROM AccessRoleAssignmentRule r " +
            "WHERE r.scheduleType = 'CRON' AND r.enabled = true")
    List<String> findTenantsWithCronRules();
}

