package com.aidentx.iam.access_roles_management.repository;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecutionItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for per-identity access-role assignment execution detail rows.
 */
@Repository
public interface AccessRoleAssignmentExecutionItemRepository extends JpaRepository<AccessRoleAssignmentExecutionItem, UUID> {

    /** Lists items for an execution run, newest first. */
    Page<AccessRoleAssignmentExecutionItem> findByTenantIdAndExecutionIdOrderByProcessedAtDesc(
            String tenantId,
            UUID executionId,
            Pageable pageable);

    /** Lists items for an execution run filtered by outcome, newest first. */
    Page<AccessRoleAssignmentExecutionItem> findByTenantIdAndExecutionIdAndOutcomeOrderByProcessedAtDesc(
            String tenantId,
            UUID executionId,
            AccessRoleAssignmentExecutionItem.Outcome outcome,
            Pageable pageable);

    /** Lists role-rule execution history for one identity. */
    Page<AccessRoleAssignmentExecutionItem> findByTenantIdAndIdentityIdOrderByProcessedAtDesc(
            String tenantId,
            UUID identityId,
            Pageable pageable);

    /** Lists execution item history for one identity and rule. */
    List<AccessRoleAssignmentExecutionItem> findByTenantIdAndRuleIdAndIdentityIdOrderByProcessedAtDesc(
            String tenantId,
            UUID ruleId,
            UUID identityId);

    /** Lists execution item history for one identity and role. */
    List<AccessRoleAssignmentExecutionItem> findByTenantIdAndRoleIdAndIdentityIdOrderByProcessedAtDesc(
            String tenantId,
            UUID roleId,
            UUID identityId);

    /** Counts execution detail rows referencing one role in a tenant. */
    long countByTenantIdAndRoleId(String tenantId, UUID roleId);

    /** Counts detail rows by outcome for an execution run. */
    @Query("SELECT i.outcome, COUNT(i) FROM AccessRoleAssignmentExecutionItem i " +
            "WHERE i.tenantId = :tenantId AND i.executionId = :executionId GROUP BY i.outcome")
    List<Object[]> countByOutcome(@Param("tenantId") String tenantId,
                                  @Param("executionId") UUID executionId);

    /** Finds failed identity-level items for a run. */
    List<AccessRoleAssignmentExecutionItem> findByTenantIdAndExecutionIdAndOutcome(
            String tenantId,
            UUID executionId,
            AccessRoleAssignmentExecutionItem.Outcome outcome);
}

