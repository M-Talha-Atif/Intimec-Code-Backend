package com.aidentx.iam.entitlement_management.repository;

import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionHistory;
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
public interface RuleExecutionHistoryRepository extends JpaRepository<RuleExecutionHistory, UUID> {

    /**
     * All executions for a rule, newest first.
     */
    Page<RuleExecutionHistory> findByTenantIdAndRuleIdOrderByStartedAtDesc(
            String tenantId, UUID ruleId, Pageable pageable);

    /**
     * All executions for an entitlement, newest first.
     */
    Page<RuleExecutionHistory> findByTenantIdAndEntitlementIdOrderByStartedAtDesc(
            String tenantId, UUID entitlementId, Pageable pageable);

    /**
     * All executions for a tenant, newest first.
     */
    Page<RuleExecutionHistory> findByTenantIdOrderByStartedAtDesc(
            String tenantId, Pageable pageable);

    /**
     * Failed executions for a tenant within a time range.
     */
    @Query("SELECT h FROM RuleExecutionHistory h WHERE h.tenantId = :tenantId " +
            "AND h.status != 'SUCCESS' AND h.startedAt >= :since ORDER BY h.startedAt DESC")
    List<RuleExecutionHistory> findFailuresSince(
            @Param("tenantId") String tenantId, @Param("since") Long since);

    /**
     * Latest execution for each rule in a tenant.
     */
    @Query("SELECT h FROM RuleExecutionHistory h WHERE h.tenantId = :tenantId " +
            "AND h.startedAt = (SELECT MAX(h2.startedAt) FROM RuleExecutionHistory h2 " +
            "WHERE h2.tenantId = h.tenantId AND h2.ruleId = h.ruleId)")
    List<RuleExecutionHistory> findLatestPerRule(@Param("tenantId") String tenantId);

    /** Delete all history records for a resource within a tenant (cascade delete on application removal). */
    @Modifying
    @Query("DELETE FROM RuleExecutionHistory h WHERE h.resourceId = :resourceId AND h.tenantId = :tenantId")
    void deleteAllByResourceIdAndTenantId(@Param("resourceId") UUID resourceId, @Param("tenantId") String tenantId);
}