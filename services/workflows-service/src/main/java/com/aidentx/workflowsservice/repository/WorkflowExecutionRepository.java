package com.aidentx.workflowsservice.repository;

import com.aidentx.workflowsservice.entity.WorkflowExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkflowExecution entity operations
 */
@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    List<WorkflowExecution> findByWorkflowId(UUID workflowId);

    Page<WorkflowExecution> findByWorkflowId(UUID workflowId, Pageable pageable);

    Page<WorkflowExecution> findByWorkflowIdAndStatus(UUID workflowId, WorkflowExecution.ExecutionStatus status, Pageable pageable);

    List<WorkflowExecution> findByStatus(WorkflowExecution.ExecutionStatus status);

    Page<WorkflowExecution> findByStatus(WorkflowExecution.ExecutionStatus status, Pageable pageable);

    List<WorkflowExecution> findByTriggeredBy(UUID triggeredBy);

    Optional<WorkflowExecution> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT e FROM WorkflowExecution e WHERE e.workflowId = :workflowId ORDER BY e.startedAt DESC")
    List<WorkflowExecution> findExecutionHistory(@Param("workflowId") UUID workflowId);

    @Query("SELECT e FROM WorkflowExecution e WHERE e.status = :status AND e.timeoutAt < :now")
    List<WorkflowExecution> findTimedOutExecutions(
            @Param("status") WorkflowExecution.ExecutionStatus status,
            @Param("now") OffsetDateTime now
    );

    @Query("SELECT e FROM WorkflowExecution e WHERE e.triggeredBy = :userId ORDER BY e.startedAt DESC")
    List<WorkflowExecution> findUserExecutions(@Param("userId") UUID userId);

    @Query("SELECT COUNT(e) FROM WorkflowExecution e WHERE e.workflowId = :workflowId AND e.status = :status")
    Long countByWorkflowIdAndStatus(
            @Param("workflowId") UUID workflowId,
            @Param("status") WorkflowExecution.ExecutionStatus status
    );

    @Query("SELECT COUNT(e) FROM WorkflowExecution e WHERE e.status = :status")
    Long countByStatus(@Param("status") WorkflowExecution.ExecutionStatus status);

    @Query("SELECT AVG(e.durationMs) FROM WorkflowExecution e WHERE e.status = 'COMPLETED' AND e.durationMs IS NOT NULL")
    Double getAverageDuration();

    // Get total execution count for a workflow
    @Query("SELECT COUNT(e) FROM WorkflowExecution e WHERE e.workflowId = :workflowId")
    Long countByWorkflowId(@Param("workflowId") UUID workflowId);

    // Get average duration for a specific workflow (completed executions only)
    @Query("SELECT AVG(e.durationMs) FROM WorkflowExecution e WHERE e.workflowId = :workflowId AND e.status = 'COMPLETED' AND e.durationMs IS NOT NULL")
    Double getAverageDurationByWorkflowId(@Param("workflowId") UUID workflowId);

    // Get latest execution time for a workflow
    @Query("SELECT MAX(e.startedAt) FROM WorkflowExecution e WHERE e.workflowId = :workflowId")
    OffsetDateTime getLastExecutionTime(@Param("workflowId") UUID workflowId);

    /**
     * Count executions started after a specific date
     *
     * @param startDate Start date filter
     * @return Count of executions
     */
    @Query("SELECT COUNT(e) FROM WorkflowExecution e WHERE e.startedAt >= :startDate")
    Long countByStartedAtAfter(@Param("startDate") OffsetDateTime startDate);

    /**
     * Count executions by status started after a specific date
     *
     * @param status    Execution status
     * @param startDate Start date filter
     * @return Count of executions
     */
    @Query("SELECT COUNT(e) FROM WorkflowExecution e WHERE e.status = :status AND e.startedAt >= :startDate")
    Long countByStatusAndStartedAtAfter(
            @Param("status") WorkflowExecution.ExecutionStatus status,
            @Param("startDate") OffsetDateTime startDate
    );

    /**
     * Get average duration for executions started after a specific date
     *
     * @param startDate Start date filter
     * @return Average duration in milliseconds
     */
    @Query("SELECT AVG(e.durationMs) FROM WorkflowExecution e WHERE e.status = 'COMPLETED' AND e.durationMs IS NOT NULL AND e.startedAt >= :startDate")
    Double getAverageDurationAfter(@Param("startDate") OffsetDateTime startDate);

    /**
     * Count executions within a date range
     *
     * @param startDate Start of date range (inclusive)
     * @param endDate   End of date range (inclusive)
     * @return Count of executions
     */
    @Query("SELECT COUNT(e) FROM WorkflowExecution e WHERE e.startedAt >= :startDate AND e.startedAt <= :endDate")
    Long countByStartedAtBetween(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Count executions by status within a date range
     *
     * @param status    Execution status
     * @param startDate Start of date range (inclusive)
     * @param endDate   End of date range (inclusive)
     * @return Count of executions
     */
    @Query("SELECT COUNT(e) FROM WorkflowExecution e WHERE e.status = :status AND e.startedAt >= :startDate AND e.startedAt <= :endDate")
    Long countByStatusAndStartedAtBetween(
            @Param("status") WorkflowExecution.ExecutionStatus status,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Get average execution duration for a specific workflow
     *
     * @param workflowId Workflow ID
     * @return Average duration in milliseconds
     */
    @Query("SELECT AVG(e.durationMs) FROM WorkflowExecution e WHERE e.workflowId = :workflowId AND e.status = 'COMPLETED'")
    Double getAverageExecutionTimeByWorkflowId(@Param("workflowId") UUID workflowId);

}
