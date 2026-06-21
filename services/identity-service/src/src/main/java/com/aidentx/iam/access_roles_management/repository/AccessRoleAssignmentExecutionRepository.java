package com.aidentx.iam.access_roles_management.repository;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for run-level access-role assignment-rule execution history.
 */
@Repository
public interface AccessRoleAssignmentExecutionRepository extends JpaRepository<AccessRoleAssignmentExecution, UUID>,
        JpaSpecificationExecutor<AccessRoleAssignmentExecution> {

    /** Lists executions for a tenant, newest first. */
    Page<AccessRoleAssignmentExecution> findByTenantIdOrderByStartedAtDesc(String tenantId, Pageable pageable);

    /** Lists executions for one assignment rule, newest first. */
    Page<AccessRoleAssignmentExecution> findByTenantIdAndRuleIdOrderByStartedAtDesc(
            String tenantId,
            UUID ruleId,
            Pageable pageable);

    /** Lists executions by status, newest first. */
    Page<AccessRoleAssignmentExecution> findByTenantIdAndStatusOrderByStartedAtDesc(
            String tenantId,
            AccessRoleAssignmentExecution.ExecutionStatus status,
            Pageable pageable);

    /** Counts execution runs referencing one role in a tenant. */
    long countByTenantIdAndRoleId(String tenantId, UUID roleId);

    /** Finds failed or partially failed executions since a timestamp. */
    @Query("SELECT e FROM AccessRoleAssignmentExecution e WHERE e.tenantId = :tenantId " +
            "AND e.status IN ('FAILURE', 'PARTIAL_FAILURE') AND e.startedAt >= :since ORDER BY e.startedAt DESC")
    List<AccessRoleAssignmentExecution> findFailuresSince(@Param("tenantId") String tenantId,
                                                          @Param("since") Long since);

    /** Finds the latest execution for each rule in a tenant. */
    @Query("SELECT e FROM AccessRoleAssignmentExecution e WHERE e.tenantId = :tenantId " +
            "AND e.startedAt = (SELECT MAX(e2.startedAt) FROM AccessRoleAssignmentExecution e2 " +
            "WHERE e2.tenantId = e.tenantId AND e2.ruleId = e.ruleId)")
    List<AccessRoleAssignmentExecution> findLatestPerRule(@Param("tenantId") String tenantId);
}

