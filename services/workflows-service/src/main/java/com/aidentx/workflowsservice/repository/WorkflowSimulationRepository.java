package com.aidentx.workflowsservice.repository;

import com.aidentx.workflowsservice.entity.WorkflowSimulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for WorkflowSimulation entity operations
 */
@Repository
public interface WorkflowSimulationRepository extends JpaRepository<WorkflowSimulation, UUID> {

    List<WorkflowSimulation> findByWorkflowId(UUID workflowId);

    List<WorkflowSimulation> findByCreatedBy(UUID createdBy);

    @Query("SELECT s FROM WorkflowSimulation s WHERE s.workflowId = :workflowId ORDER BY s.createdAt DESC")
    List<WorkflowSimulation> findSimulationHistory(@Param("workflowId") UUID workflowId);
}
