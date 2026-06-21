package com.aidentx.iam.entitlement_management.repository;

import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleExecutionDetailRepository extends JpaRepository<RuleExecutionDetail, UUID> {

    /**
     * All details for a specific execution run.
     */
    Page<RuleExecutionDetail> findByTenantIdAndExecutionIdOrderByProcessedAtDesc(
            String tenantId, UUID executionId, Pageable pageable);

    /**
     * All details for a specific execution run filtered by outcome.
     */
    Page<RuleExecutionDetail> findByTenantIdAndExecutionIdAndOutcomeOrderByProcessedAtDesc(
            String tenantId, UUID executionId, RuleExecutionDetail.Outcome outcome, Pageable pageable);

    /**
     * History for a specific identity across all rule executions.
     * Answers: "What happened to this person over time?"
     */
    Page<RuleExecutionDetail> findByTenantIdAndIdentityIdOrderByProcessedAtDesc(
            String tenantId, UUID identityId, Pageable pageable);

    /**
     * History for a specific identity + rule combination.
     * Answers: "Why does this person have/not have this entitlement?"
     */
    List<RuleExecutionDetail> findByTenantIdAndRuleIdAndIdentityIdOrderByProcessedAtDesc(
            String tenantId, UUID ruleId, UUID identityId);

    /**
     * Count details by outcome for a specific execution.
     */
    @Query("SELECT d.outcome, COUNT(d) FROM RuleExecutionDetail d " +
            "WHERE d.tenantId = :tenantId AND d.executionId = :executionId " +
            "GROUP BY d.outcome")
    List<Object[]> countByOutcome(
            @Param("tenantId") String tenantId, @Param("executionId") UUID executionId);

    /**
     * Delete all execution details whose parent history record belongs to a given resource+tenant.
     * Uses a native subquery because RuleExecutionDetail has no direct resourceId field.
     */
    @Modifying
    @Query(value = "DELETE FROM rule_execution_details WHERE execution_id IN "
            + "(SELECT id FROM rule_execution_history WHERE resource_id = CAST(:resourceId AS uuid) AND tenant_id = :tenantId)",
            nativeQuery = true)
    void deleteAllByResourceIdAndTenantId(@Param("resourceId") String resourceId, @Param("tenantId") String tenantId);
}