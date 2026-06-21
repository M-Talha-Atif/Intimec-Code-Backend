package com.aidentx.workflowsservice.repository;

import com.aidentx.workflowsservice.entity.WorkflowNodeExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkflowNodeExecution entity operations
 */
@Repository
public interface WorkflowNodeExecutionRepository extends JpaRepository<WorkflowNodeExecution, UUID> {

    List<WorkflowNodeExecution> findByExecutionId(UUID executionId);

    List<WorkflowNodeExecution> findByStatus(WorkflowNodeExecution.NodeExecutionStatus status);

    Optional<WorkflowNodeExecution> findByExecutionIdAndNodeId(UUID executionId, String nodeId);

    @Query("SELECT n FROM WorkflowNodeExecution n WHERE n.executionId = :executionId ORDER BY n.startedAt ASC")
    List<WorkflowNodeExecution> findExecutionTrace(@Param("executionId") UUID executionId);

    @Query("SELECT n FROM WorkflowNodeExecution n WHERE n.executionId = :executionId AND n.status = :status")
    List<WorkflowNodeExecution> findByExecutionIdAndStatus(
        @Param("executionId") UUID executionId,
        @Param("status") WorkflowNodeExecution.NodeExecutionStatus status
    );

    @Query("SELECT n FROM WorkflowNodeExecution n WHERE n.status IN ('PENDING', 'RUNNING')")
    List<WorkflowNodeExecution> findActiveNodeExecutions();

    long countByExecutionId(UUID executionId);

    long countByExecutionIdAndStatusIn(UUID executionId, Collection<WorkflowNodeExecution.NodeExecutionStatus> statuses);
}
