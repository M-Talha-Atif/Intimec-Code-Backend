package com.aidentx.workflowsservice.repository;

import com.aidentx.workflowsservice.entity.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Workflow entity operations
 */
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    List<Workflow> findByStatus(Workflow.WorkflowStatus status);

    Page<Workflow> findByStatus(String status, Pageable pageable);

    List<Workflow> findByCategory(Workflow.WorkflowCategory category);

    Page<Workflow> findByCategory(String category, Pageable pageable);

    List<Workflow> findByCreatedBy(UUID createdBy);

    List<Workflow> findBySourceTemplateId(UUID sourceTemplateId);

    Optional<Workflow> findByNameAndVersion(String name, String version);

    boolean existsByName(String name);

    List<Workflow> findByStatusAndCategory(Workflow.WorkflowStatus status, Workflow.WorkflowCategory category);

    Page<Workflow> findByStatusAndCategory(String status, String category, Pageable pageable);

    @Query("SELECT w FROM Workflow w WHERE w.driftDetected = true")
    List<Workflow> findWorkflowsWithDrift();

    @Query("SELECT w FROM Workflow w WHERE w.riskScore >= :minRiskScore")
    List<Workflow> findHighRiskWorkflows(@Param("minRiskScore") Integer minRiskScore);

    List<Workflow> findByTriggerTypeAndStatus(Workflow.TriggerType triggerType, Workflow.WorkflowStatus status);

    /**
     * Count workflows by status
     *
     * @param status Workflow status
     * @return Count of workflows
     */
    Long countByStatus(Workflow.WorkflowStatus status);

    /**
     * Count workflows by trigger type
     *
     * @param triggerType Trigger type
     * @return Count of workflows
     */
    Long countByTriggerType(Workflow.TriggerType triggerType);

    /**
     * Count workflows with trigger types in a given list
     * Used for automated workflows (EVENT_BASED + TIME_BASED)
     *
     * @param triggerTypes List of trigger types
     * @return Count of workflows
     */
    @Query("SELECT COUNT(w) FROM Workflow w WHERE w.triggerType IN :triggerTypes")
    Long countByTriggerTypeIn(@Param("triggerTypes") Workflow.TriggerType... triggerTypes);

    /**
     * Count workflows with risk score greater than or equal to threshold
     *
     * @param riskScore Minimum risk score
     * @return Count of workflows
     */
    Long countByRiskScoreGreaterThanEqual(Integer riskScore);

    /**
     * Count workflows by category
     *
     * @param category Workflow category
     * @return Count of workflows
     */
    Long countByCategory(Workflow.WorkflowCategory category);

    /**
     * Find workflows by trigger type with pagination
     *
     * @param triggerType Trigger type as string
     * @param pageable    Pagination parameters
     * @return Page of workflows
     */
    Page<Workflow> findByTriggerType(String triggerType, Pageable pageable);

    /**
     * Find workflows with trigger types in a list with pagination
     *
     * @param triggerTypes List of trigger types as strings
     * @param pageable     Pagination parameters
     * @return Page of workflows
     */
    @Query("SELECT w FROM Workflow w WHERE w.triggerType IN :triggerTypes")
    Page<Workflow> findByTriggerTypeIn(
            @Param("triggerTypes") List<String> triggerTypes,
            Pageable pageable
    );

    /**
     * Find workflows by risk score threshold with pagination
     *
     * @param riskScore Minimum risk score
     * @param pageable  Pagination parameters
     * @return Page of workflows
     */
    Page<Workflow> findByRiskScoreGreaterThanEqual(Integer riskScore, Pageable pageable);


    /**
     * Find workflows by multiple statuses with pagination
     *
     * @param statuses List of workflow statuses
     * @param pageable Pagination parameters
     * @return Page of workflows
     */
    @Query("SELECT w FROM Workflow w WHERE w.status IN :statuses")
    Page<Workflow> findByStatusIn(@Param("statuses") List<String> statuses, Pageable pageable);

    /**
     * Find workflows by risk score threshold with pagination
     *
     * @param riskScore Minimum risk score
     * @param pageable  Pagination parameters
     * @return Page of workflows
     */
    @Query("SELECT w FROM Workflow w WHERE w.riskScore >= :riskScore")
    Page<Workflow> findByRiskScoreGreaterThanEqual(@Param("riskScore") int riskScore, Pageable pageable);

    /**
     * Find all workflows with at least one execution
     *
     * @param pageable Pagination parameters
     * @return Page of workflows
     */
    @Query("SELECT DISTINCT w FROM Workflow w JOIN WorkflowExecution e ON w.id = e.workflowId")
    Page<Workflow> findAllWithExecutions(Pageable pageable);

    /**
     * Find workflows with failed executions
     *
     * @param pageable Pagination parameters
     * @return Page of workflows
     */
    @Query("SELECT DISTINCT w FROM Workflow w JOIN WorkflowExecution e ON w.id = e.workflowId WHERE e.status IN ('FAILED', 'TIMEOUT', 'CANCELLED')")
    Page<Workflow> findAllWithFailedExecutions(Pageable pageable);


    /**
     * Find workflows by category with pagination
     *
     * @param category Workflow category enum
     * @param pageable Pagination parameters
     * @return Page of workflows
     */
    Page<Workflow> findByCategory(Workflow.WorkflowCategory category, Pageable pageable);

}
