package com.aidentx.workflowsservice.service;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

import com.aidentx.workflowsservice.async.AsyncExecutionDispatcher;
import com.aidentx.workflowsservice.dto.request.ExecuteWorkflowRequest;
import com.aidentx.workflowsservice.dto.response.WorkflowExecutionDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowNodeExecutionDTO;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.entity.WorkflowNodeExecution;
import com.aidentx.workflowsservice.exception.ResourceNotFoundException;
import com.aidentx.workflowsservice.exception.ValidationException;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowNodeExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for workflow execution operations
 */
@Service
public class WorkflowExecutionService {

    private final IUnifiedLogger logger;
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowNodeExecutionRepository nodeExecutionRepository;
    private final AsyncExecutionDispatcher asyncExecutionDispatcher;
    private final EntityManager entityManager;

    public WorkflowExecutionService(
            WorkflowRepository workflowRepository,
            WorkflowExecutionRepository executionRepository,
            WorkflowNodeExecutionRepository nodeExecutionRepository,
            AsyncExecutionDispatcher asyncExecutionDispatcher,
            EntityManager entityManager,
            UnifiedLoggerFactory loggerFactory) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.nodeExecutionRepository = nodeExecutionRepository;
        this.asyncExecutionDispatcher = asyncExecutionDispatcher;
        this.entityManager = entityManager;
        this.logger = loggerFactory.getLogger(WorkflowExecutionService.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    private void attachStepCounts(WorkflowExecutionDTO dto, UUID executionId) {
        if (dto == null || executionId == null) return;

        long total = nodeExecutionRepository.countByExecutionId(executionId);

        long completed = nodeExecutionRepository.countByExecutionIdAndStatusIn(
            executionId,
            java.util.List.of(
                WorkflowNodeExecution.NodeExecutionStatus.COMPLETED,
                WorkflowNodeExecution.NodeExecutionStatus.FAILED,
                WorkflowNodeExecution.NodeExecutionStatus.SKIPPED,
                WorkflowNodeExecution.NodeExecutionStatus.TIMEOUT
            )
        );

        long failed = nodeExecutionRepository.countByExecutionIdAndStatusIn(
            executionId,
            java.util.List.of(
                WorkflowNodeExecution.NodeExecutionStatus.FAILED,
                WorkflowNodeExecution.NodeExecutionStatus.TIMEOUT
            )
        );

        dto.setTotalSteps((int) total);
        dto.setStepsCompleted((int) completed);
        dto.setFailedSteps((int) failed);
    }

    /**
     * Execute a workflow
     * 
     * Uses REQUIRES_NEW to ensure execution record is committed to database
     * before dispatching to background thread (prevents race condition)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WorkflowExecutionDTO executeWorkflow(UUID workflowId, ExecuteWorkflowRequest request, UUID triggeredBy) {
        logger.info()
            .message(LOG_STARTING_EXECUTION)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(workflowId.toString())
            .detail("triggeredBy", triggeredBy != null ? triggeredBy.toString() : "null")
            .log();
        
        // 0. Check for duplicate execution using idempotency key (prevents Kafka message replays)
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isEmpty()) {
            WorkflowExecution existingExecution = executionRepository
                .findByIdempotencyKey(request.getIdempotencyKey())
                .orElse(null);
            
            if (existingExecution != null) {
                logger.info()
                    .message(LOG_DUPLICATE_EXECUTION_PREVENTED)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType("WorkflowExecution")
                    .objectId(existingExecution.getId().toString())
                    .detail("idempotencyKey", request.getIdempotencyKey())
                    .log();
                return WorkflowExecutionDTO.from(existingExecution);
            }
        }
        
        // 1. Fetch workflow
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new ResourceNotFoundException(ERROR_WORKFLOW_NOT_FOUND_WITH_ID + workflowId));
        
        // 2. Validate workflow is executable
        validateWorkflowExecutable(workflow);
        
        // 3. Create execution record
        // Build execution context - handle null inputData gracefully
        Map<String, Object> executionContext = new HashMap<>();
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            executionContext.putAll(request.getContext());
        }
        executionContext.put(CONTEXT_KEY_WORKFLOW_NAME, workflow.getName());
        executionContext.put(CONTEXT_KEY_SIMULATION_MODE, request.getDryRun() != null ? request.getDryRun() : false);
        
        // Handle triggeredBy conversion from String to UUID
        UUID actualTriggeredBy = triggeredBy;
        if (actualTriggeredBy == null && request.getTriggeredBy() != null) {
            try {
                actualTriggeredBy = UUID.fromString(request.getTriggeredBy());
            } catch (IllegalArgumentException e) {
                logger.warn()
                    .message(LOG_INVALID_TRIGGERED_BY_UUID)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("triggeredBy", request.getTriggeredBy())
                    .log();
                // Use a default UUID or throw exception based on requirements
            }
        }
        executionContext.put(CONTEXT_KEY_TRIGGERED_BY, actualTriggeredBy);
        
        WorkflowExecution execution = WorkflowExecution.builder()
            .workflowId(workflowId)
            .workflowVersion(workflow.getVersion())
            .status(WorkflowExecution.ExecutionStatus.RUNNING)
            .triggeredBy(actualTriggeredBy)
            .triggerType(request.getTriggerType() != null ? request.getTriggerType() : DEFAULT_TRIGGER_TYPE_MANUAL)
            .startedAt(OffsetDateTime.now())
            .executionContext(executionContext)
            .pathTrace(new ArrayList<>())
            .idempotencyKey(request.getIdempotencyKey())  // Set idempotency key to prevent duplicates
            .build();
        
        WorkflowExecution savedExecution = executionRepository.save(execution);
        
        // CRITICAL: Flush to ensure INSERT is sent to database BEFORE dispatching to background thread
        // This prevents race condition where background thread tries to read uncommitted data
        entityManager.flush();
        
        // Get the execution ID before dispatching
        UUID executionId = savedExecution.getId();
        WorkflowExecutionDTO result = WorkflowExecutionDTO.from(savedExecution);
        
        // 4. Dispatch workflow execution to background thread AFTER method completes
        // Use TransactionSynchronizationManager to dispatch after transaction commits
        logger.info()
            .message(LOG_DISPATCHING_EXECUTION)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
            .objectId(executionId.toString())
            .log();
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    logger.info()
                        .message(LOG_TRANSACTION_COMMITTED_DISPATCHING)
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
                        .objectId(executionId.toString())
                        .log();
                    asyncExecutionDispatcher.dispatch(executionId);
                }
            }
        );
        
        // 5. Return immediately (execution will be dispatched after transaction commits)
        return result;
    }

    /**
     * Get execution by ID
     */
    public WorkflowExecutionDTO getExecution(UUID executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new ResourceNotFoundException(ERROR_EXECUTION_NOT_FOUND + executionId));
        
        return enrichWithNodeExecutions(execution);
    }

    /**
     * Get execution status ONLY without loading child node executions
     * This is much faster and avoids JSONB deserialization issues
     * Use this for polling/status checks during simulation
     * REQUIRES_NEW ensures we see committed changes from other transactions
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public String getExecutionStatus(UUID executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new ResourceNotFoundException(ERROR_EXECUTION_NOT_FOUND + executionId));
        
        return execution.getStatus().name();
    }

    /**
     * Get node executions for a workflow execution, sorted by step number
     */
    public List<WorkflowNodeExecutionDTO> getNodeExecutions(UUID executionId) {
        logger.info()
            .message(LOG_GETTING_NODE_EXECUTIONS)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
            .objectId(executionId.toString())
            .log();
        
        // Verify execution exists
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new ResourceNotFoundException(ERROR_EXECUTION_NOT_FOUND + executionId));
        
        // Get all node executions for this execution
        List<WorkflowNodeExecution> nodeExecutions = nodeExecutionRepository.findByExecutionId(executionId);
        
        logger.debug()
            .message(LOG_FOUND_NODE_EXECUTIONS)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
            .objectId(executionId.toString())
            .detail("nodeExecutionCount", nodeExecutions.size())
            .log();
        
        // Sort by step number (nulls last) and convert to DTOs
        return nodeExecutions.stream()
            .sorted(Comparator.comparing(WorkflowNodeExecution::getStepNumber, 
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .map(WorkflowNodeExecutionDTO::from)
            .collect(Collectors.toList());
    }

    /**
     * Get execution trace (path through workflow)
     */
    public Map<String, Object> getExecutionTrace(UUID executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new ResourceNotFoundException(ERROR_EXECUTION_NOT_FOUND + executionId));
        
        Map<String, Object> trace = new HashMap<>();
        trace.put(CONTEXT_KEY_EXECUTION_ID, execution.getId());
        trace.put(CONTEXT_KEY_WORKFLOW_ID, execution.getWorkflowId());
        trace.put(CONTEXT_KEY_STATUS, execution.getStatus().name());
        trace.put("pathTrace", execution.getPathTrace());
        trace.put("startedAt", execution.getStartedAt());
        trace.put("completedAt", execution.getCompletedAt());
        
        if (execution.getDurationMs() != null) {
            trace.put("durationMs", execution.getDurationMs());
        }
        
        // Add node execution details sorted by step number
        List<WorkflowNodeExecutionDTO> nodeExecutions = getNodeExecutions(executionId);
        trace.put("nodeExecutions", nodeExecutions);
        
        return trace;
    }

    /**
     * Cancel a running or paused execution
     */
    @Transactional
    public WorkflowExecutionDTO cancelExecution(UUID executionId, UUID cancelledBy, String cancelledByUsername) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new ResourceNotFoundException(ERROR_EXECUTION_NOT_FOUND + executionId));
        
        // Validate execution can be cancelled (must be RUNNING or PAUSED)
        if (execution.getStatus() != WorkflowExecution.ExecutionStatus.RUNNING 
            && execution.getStatus() != WorkflowExecution.ExecutionStatus.PAUSED) {
            throw new ValidationException(ERROR_EXECUTION_CANNOT_CANCEL + execution.getStatus());
        }
        
        // Update all incomplete node executions to SKIPPED
        List<WorkflowNodeExecution> nodeExecutions = nodeExecutionRepository.findByExecutionId(executionId);
        OffsetDateTime now = OffsetDateTime.now();
        for (WorkflowNodeExecution nodeExec : nodeExecutions) {
            if (nodeExec.getStatus() == WorkflowNodeExecution.NodeExecutionStatus.RUNNING 
                || nodeExec.getStatus() == WorkflowNodeExecution.NodeExecutionStatus.PAUSED
                || nodeExec.getStatus() == WorkflowNodeExecution.NodeExecutionStatus.PENDING) {
                nodeExec.setStatus(WorkflowNodeExecution.NodeExecutionStatus.SKIPPED);
                nodeExec.setCompletedAt(now);
                if (nodeExec.getStartedAt() != null) {
                    long duration = java.time.Duration.between(nodeExec.getStartedAt(), now).toMillis();
                    nodeExec.setDurationMs((int) duration);
                }
                nodeExecutionRepository.save(nodeExec);
            }
        }
        
        // Update execution status
        execution.setStatus(WorkflowExecution.ExecutionStatus.CANCELLED);
        execution.setCompletedAt(now);
        execution.setCancelledBy(cancelledBy);
        execution.setCancelledByName(cancelledByUsername != null ? cancelledByUsername : "Unknown");
        // Don't set errorMessage for cancellation - it's not an error
        
        // Calculate duration
        if (execution.getStartedAt() != null) {
            long duration = java.time.Duration.between(execution.getStartedAt(), execution.getCompletedAt()).toMillis();
            execution.setDurationMs((int) duration);
        }
        
        WorkflowExecution cancelled = executionRepository.save(execution);
        
        logger.info()
            .message(LOG_EXECUTION_CANCELLED)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
            .objectId(executionId.toString())
            .detail("cancelledBy", cancelledBy.toString())
            .log();
        
        return enrichWithNodeExecutions(cancelled);
    }

    /**
     * Get all executions across all workflows
     */
    public Page<WorkflowExecutionDTO> getAllExecutions(String status, Pageable pageable) {
        return getAllExecutions(status, pageable, false);
    }

    public Page<WorkflowExecutionDTO> getAllExecutions(String status, Pageable pageable, boolean includeDetails) {
        Page<WorkflowExecution> executions;
        
        if (status != null && !status.isEmpty()) {
            try {
                WorkflowExecution.ExecutionStatus executionStatus = WorkflowExecution.ExecutionStatus.valueOf(status.toUpperCase());
                executions = executionRepository.findByStatus(executionStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new ValidationException(ERROR_INVALID_EXECUTION_STATUS + status);
            }
        } else {
            executions = executionRepository.findAll(pageable);
        }

        if (includeDetails) {
            return executions.map(this::enrichWithNodeExecutions);
        }

        return executions.map(execution -> {
            WorkflowExecutionDTO dto = WorkflowExecutionDTO.from(execution);
            attachStepCounts(dto, execution.getId());
            return dto;
        });
    }
    
    /**
     * Helper method to enrich execution DTO with node executions
     */
    private WorkflowExecutionDTO enrichWithNodeExecutions(WorkflowExecution execution) {
        WorkflowExecutionDTO dto = WorkflowExecutionDTO.from(execution);
        
        // Fetch and include node executions with real-time status
        try {
            List<WorkflowNodeExecution> nodeExecutions = nodeExecutionRepository.findByExecutionId(execution.getId());
            List<WorkflowNodeExecutionDTO> nodeExecutionDTOs = nodeExecutions.stream()
                .sorted(Comparator.comparing(WorkflowNodeExecution::getStepNumber, 
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(nodeExec -> {
                    try {
                        return WorkflowNodeExecutionDTO.from(nodeExec);
                    } catch (Exception e) {
                        logger.warn()
                            .message("Failed to map node execution to DTO - skipping")
                            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                            .objectId(nodeExec.getId().toString())
                            .detail("nodeId", nodeExec.getNodeId())
                            .detail("error", e.getMessage())
                            .log();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
            dto.setNodeExecutions(nodeExecutionDTOs);
        } catch (Exception e) {
            logger.error()
                .message("Failed to load node executions")
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
                .objectId(execution.getId().toString())
                .detail("error", e.getMessage())
                .detail("errorClass", e.getClass().getSimpleName())
                .log();
            // Set empty list instead of failing completely
            dto.setNodeExecutions(List.of());
        }
        
        return dto;
    }

    /**
     * Get all executions for a workflow
     */
    public Page<WorkflowExecutionDTO> getWorkflowExecutions(UUID workflowId, String status, Pageable pageable) {
        return getWorkflowExecutions(workflowId, status, pageable, false);
    }

    public Page<WorkflowExecutionDTO> getWorkflowExecutions(UUID workflowId, String status, Pageable pageable, boolean includeDetails) {
        // Verify workflow exists
        workflowRepository.findById(workflowId)
            .orElseThrow(() -> new ResourceNotFoundException(ERROR_WORKFLOW_NOT_FOUND_WITH_ID + workflowId));
        
        Page<WorkflowExecution> executions;
        
        if (status != null && !status.isEmpty()) {
            try {
                WorkflowExecution.ExecutionStatus executionStatus = WorkflowExecution.ExecutionStatus.valueOf(status.toUpperCase());
                executions = executionRepository.findByWorkflowIdAndStatus(workflowId, executionStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new ValidationException(ERROR_INVALID_EXECUTION_STATUS + status);
            }
        } else {
            executions = executionRepository.findByWorkflowId(workflowId, pageable);
        }

        if (includeDetails) {
            return executions.map(this::enrichWithNodeExecutions);
        }

        return executions.map(execution -> {
            WorkflowExecutionDTO dto = WorkflowExecutionDTO.from(execution);
            attachStepCounts(dto, execution.getId());
            return dto;
        });
    }

    /**
     * Retry a failed execution
     */
    @Transactional
    public WorkflowExecutionDTO retryExecution(UUID executionId, UUID retriedBy) {
        WorkflowExecution originalExecution = executionRepository.findById(executionId)
            .orElseThrow(() -> new ResourceNotFoundException(ERROR_EXECUTION_NOT_FOUND + executionId));
        
        // Validate execution can be retried
        if (originalExecution.getStatus() != WorkflowExecution.ExecutionStatus.FAILED) {
            throw new ValidationException(ERROR_EXECUTION_CANNOT_RETRY + originalExecution.getStatus());
        }
        
        // Fetch original workflow
        Workflow workflow = workflowRepository.findById(originalExecution.getWorkflowId())
            .orElseThrow(() -> new ResourceNotFoundException(ERROR_WORKFLOW_NOT_FOUND_WITH_ID + originalExecution.getWorkflowId()));
        
        // Create new execution request
        ExecuteWorkflowRequest retryRequest = new ExecuteWorkflowRequest();
        retryRequest.setTriggeredBy(retriedBy != null ? retriedBy.toString() : null);
        retryRequest.setTriggerType(DEFAULT_TRIGGER_TYPE_RETRY);
        retryRequest.setContext(originalExecution.getExecutionContext());
        Boolean simulationMode = originalExecution.getExecutionContext() != null ? 
            (Boolean) originalExecution.getExecutionContext().get(CONTEXT_KEY_SIMULATION_MODE) : false;
        retryRequest.setDryRun(simulationMode);
        
        logger.info()
            .message(LOG_RETRY_STARTED)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
            .objectId(executionId.toString())
            .log();
        
        // Execute as new execution
        return executeWorkflow(workflow.getId(), retryRequest, retriedBy);
    }

    /**
     * Validate workflow is executable
     */
    private void validateWorkflowExecutable(Workflow workflow) {
        if (workflow.getStatus() != Workflow.WorkflowStatus.ACTIVE) {
            throw new ValidationException(ERROR_WORKFLOW_NOT_ACTIVE + workflow.getStatus());
        }
        
        if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
            throw new ValidationException(ERROR_WORKFLOW_NO_NODES);
        }
        
        if (workflow.getEdges() == null) {
            throw new ValidationException(ERROR_WORKFLOW_NO_EDGES);
        }
        
        // Check if workflow has canExecute permission
        Map<String, Boolean> permissions = workflow.getPermissions();
        if (permissions != null && Boolean.FALSE.equals(permissions.get(FIELD_CAN_EXECUTE))) {
            throw new ValidationException(ERROR_WORKFLOW_NOT_PERMITTED);
        }
    }
    
    /**
     * Get global execution statistics
     */
    public Map<String, Object> getExecutionStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        // Total executions
        long total = executionRepository.count();
        
        // Count by status
        long completed = executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.COMPLETED);
        long failed = executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.FAILED);
        long running = executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.RUNNING);
        long paused = executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.PAUSED);
        long cancelled = executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.CANCELLED);
        
        // Calculate average duration for completed executions
        Double avgDuration = executionRepository.getAverageDuration();
        
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("failed", failed);
        stats.put("running", running);
        stats.put("paused", paused);
        stats.put("cancelled", cancelled);
        stats.put("avgDurationMs", avgDuration != null ? avgDuration.longValue() : 0L);
        
        return stats;
    }
}
