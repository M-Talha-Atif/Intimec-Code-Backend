package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.enums.ScheduleType;
import com.aidentx.applicationpermissions.roles_management.model.entity.DynamicRuleExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link DynamicRuleExecutionHistory} entity operations.
 * 
 * <p>This repository provides data access methods for tracking and querying
 * dynamic rule execution history. It includes specialized methods for:</p>
 * <ul>
 *   <li>Checking if rules were executed within specific time periods</li>
 *   <li>Finding latest executions for schedule period calculations</li>
 *   <li>Retrieving execution statistics for monitoring</li>
 *   <li>Tenant-specific execution history queries</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface DynamicRuleExecutionHistoryRepository extends JpaRepository<DynamicRuleExecutionHistory, UUID> {

    /**
     * Find the latest execution for a specific rule.
     * Used to determine the last execution time for schedule period calculations.
     */
    @Query("SELECT h FROM DynamicRuleExecutionHistory h WHERE h.ruleId = :ruleId " +
           "ORDER BY h.executionDate DESC LIMIT 1")
    Optional<DynamicRuleExecutionHistory> findLatestExecutionByRuleId(@Param("ruleId") UUID ruleId);

    /**
     * Check if a rule was executed within a specific time range.
     * Critical for preventing duplicate executions within schedule periods.
     */
    @Query("SELECT COUNT(h) > 0 FROM DynamicRuleExecutionHistory h " +
           "WHERE h.ruleId = :ruleId AND h.executionDate BETWEEN :startTime AND :endTime")
    boolean existsByRuleIdAndExecutionDateBetween(@Param("ruleId") UUID ruleId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * Find all executions for a rule within a specific time range.
     * Useful for debugging and monitoring rule execution patterns.
     */
    @Query("SELECT h FROM DynamicRuleExecutionHistory h " +
           "WHERE h.ruleId = :ruleId AND h.executionDate BETWEEN :startTime AND :endTime " +
           "ORDER BY h.executionDate DESC")
    List<DynamicRuleExecutionHistory> findByRuleIdAndExecutionDateBetween(@Param("ruleId") UUID ruleId,
                                                                          @Param("startTime") LocalDateTime startTime,
                                                                          @Param("endTime") LocalDateTime endTime);

    /**
     * Find executions by tenant within a time range.
     * Essential for tenant-specific monitoring and compliance reporting.
     */
    @Query("SELECT h FROM DynamicRuleExecutionHistory h " +
           "WHERE h.tenantId = :tenantId AND h.executionDate BETWEEN :startTime AND :endTime " +
           "ORDER BY h.executionDate DESC")
    List<DynamicRuleExecutionHistory> findByTenantIdAndExecutionDateBetween(@Param("tenantId") String tenantId,
                                                                            @Param("startTime") LocalDateTime startTime,
                                                                            @Param("endTime") LocalDateTime endTime);

    /**
     * Find executions by schedule type within a time range.
     * Useful for analyzing performance patterns by schedule type.
     */
    @Query("SELECT h FROM DynamicRuleExecutionHistory h " +
           "WHERE h.scheduleType = :scheduleType AND h.executionDate BETWEEN :startTime AND :endTime " +
           "ORDER BY h.executionDate DESC")
    List<DynamicRuleExecutionHistory> findByScheduleTypeAndExecutionDateBetween(@Param("scheduleType") ScheduleType scheduleType,
                                                                                @Param("startTime") LocalDateTime startTime,
                                                                                @Param("endTime") LocalDateTime endTime);

    /**
     * Find failed executions for alerting and monitoring.
     */
    @Query("SELECT h FROM DynamicRuleExecutionHistory h " +
           "WHERE h.executionStatus = 'FAILED' AND h.executionDate >= :since " +
           "ORDER BY h.executionDate DESC")
    List<DynamicRuleExecutionHistory> findFailedExecutionsSince(@Param("since") LocalDateTime since);

    /**
     * Get execution statistics for a tenant within a time range.
     * Provides aggregated metrics for monitoring dashboards.
     */
    @Query("SELECT " +
           "COUNT(h) as totalExecutions, " +
           "SUM(h.totalUsersEvaluated) as totalUsersEvaluated, " +
           "SUM(h.successfulAssignments) as totalSuccessfulAssignments, " +
           "SUM(h.failedAssignments) as totalFailedAssignments, " +
           "AVG(h.executionDurationMs) as avgExecutionTimeMs " +
           "FROM DynamicRuleExecutionHistory h " +
           "WHERE h.tenantId = :tenantId AND h.executionDate BETWEEN :startTime AND :endTime")
    Object[] getExecutionStatsByTenant(@Param("tenantId") String tenantId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Find all rules that should be executed based on schedule and last execution time.
     * This is a complex query that helps identify which rules are due for execution.
     */
    @Query("SELECT DISTINCT h.ruleId FROM DynamicRuleExecutionHistory h " +
           "WHERE h.scheduleType = :scheduleType " +
           "AND h.executionDate = (" +
           "    SELECT MAX(h2.executionDate) FROM DynamicRuleExecutionHistory h2 " +
           "    WHERE h2.ruleId = h.ruleId" +
           ") " +
           "AND h.executionDate < :cutoffTime")
    List<UUID> findRulesReadyForExecution(@Param("scheduleType") ScheduleType scheduleType,
                                         @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Delete old execution history records for data retention.
     * Important for maintaining database performance and compliance with data retention policies.
     */
    @Query("DELETE FROM DynamicRuleExecutionHistory h WHERE h.executionDate < :cutoffDate")
    void deleteExecutionsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count executions older than specified date for cleanup operations.
     */
    @Query("SELECT COUNT(h) FROM DynamicRuleExecutionHistory h WHERE h.executionDate < :cutoffDate")
    Long countExecutionsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count executions since a specific date for monitoring.
     */
    @Query("SELECT COUNT(h) FROM DynamicRuleExecutionHistory h WHERE h.executionDate >= :since")
    Long countExecutionsSince(@Param("since") LocalDateTime since);

    /**
     * Find executions within a date range for monitoring and reporting.
     */
    @Query("SELECT h FROM DynamicRuleExecutionHistory h " +
           "WHERE h.executionDate BETWEEN :startTime AND :endTime " +
           "ORDER BY h.executionDate DESC")
    List<DynamicRuleExecutionHistory> findByExecutionDateBetween(@Param("startTime") LocalDateTime startTime,
                                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * Count executions by tenant for resource usage monitoring.
     */
    @Query("SELECT COUNT(h) FROM DynamicRuleExecutionHistory h " +
           "WHERE h.tenantId = :tenantId AND h.executionDate >= :since")
    Long countExecutionsByTenantSince(@Param("tenantId") String tenantId,
                                      @Param("since") LocalDateTime since);

    /**
     * Find long-running executions for performance monitoring.
     */
    @Query("SELECT h FROM DynamicRuleExecutionHistory h " +
           "WHERE h.executionDurationMs > :thresholdMs AND h.executionDate >= :since " +
           "ORDER BY h.executionDurationMs DESC")
    List<DynamicRuleExecutionHistory> findLongRunningExecutions(@Param("thresholdMs") Long thresholdMs,
                                                               @Param("since") LocalDateTime since);
}