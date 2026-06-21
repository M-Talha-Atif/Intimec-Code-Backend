package com.aidentx.workflowsservice.async;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import com.aidentx.workflowsservice.service.WorkflowExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Thread pool-based implementation of async workflow execution.
 * 
 * This is the default production implementation that uses a configurable
 * thread pool to execute workflows in the background.
 * 
 * Key design principles:
 * - Stateless: All state is read from/written to database
 * - Non-blocking: API calls return immediately
 * - Scalable: Thread pool handles concurrent executions
 * - Crash-safe: Execution can be resumed from DB state
 * 
 * Performance characteristics:
 * - Core pool size: 10 threads (configurable)
 * - Max pool size: 20 threads (configurable)
 * - Queue capacity: 500 executions (configurable)
 * - Rejected policy: CallerRunsPolicy (graceful degradation)
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
@Primary
public class ThreadPoolExecutionDispatcher implements AsyncExecutionDispatcher {
    
    private final IUnifiedLogger logger;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final WorkflowExecutor workflowExecutor;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowRepository workflowRepository;
    
    public ThreadPoolExecutionDispatcher(
            @Qualifier("workflowExecutorThreadPool") ThreadPoolTaskExecutor threadPoolTaskExecutor,
            WorkflowExecutor workflowExecutor,
            WorkflowExecutionRepository executionRepository,
            WorkflowRepository workflowRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.workflowExecutor = workflowExecutor;
        this.executionRepository = executionRepository;
        this.workflowRepository = workflowRepository;
        this.logger = loggerFactory.getLogger(ThreadPoolExecutionDispatcher.class, LOG_CATEGORY_WORKFLOW_EXECUTION);
    }
    
    @Override
    public CompletableFuture<Void> dispatch(UUID executionId) {
        logger.info()
            .message(LOG_DISPATCHING_TO_THREAD_POOL)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowExecution")
            .objectId(executionId.toString())
            .detail("executionId", executionId)
            .log();
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Stateless: Fetch everything from database
                WorkflowExecution execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalStateException(
                        "Execution not found: " + executionId));
                
                Workflow workflow = workflowRepository.findById(execution.getWorkflowId())
                    .orElseThrow(() -> new IllegalStateException(
                        "Workflow not found: " + execution.getWorkflowId()));
                
                logger.info()
                    .message(LOG_STARTING_WORKFLOW_IN_BACKGROUND)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectType("WorkflowExecution")
                    .objectId(executionId.toString())
                    .detail("workflowId", workflow.getId())
                    .detail("workflowName", workflow.getName())
                    .log();
                
                // Execute workflow (all state updates written to DB)
                workflowExecutor.execute(workflow, execution);
                
                logger.info()
                    .message(LOG_WORKFLOW_COMPLETED_SUCCESSFULLY)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectType("WorkflowExecution")
                    .objectId(executionId.toString())
                    .detail("executionId", executionId)
                    .log();
                
            } catch (Exception e) {
                logger.error()
                    .message(LOG_WORKFLOW_FAILED_IN_BACKGROUND)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectType("WorkflowExecution")
                    .objectId(executionId.toString())
                    .detail("error", e.getMessage())
                    .log();
                
                // Update execution status to FAILED
                try {
                    WorkflowExecution execution = executionRepository.findById(executionId)
                        .orElseThrow();
                    
                    execution.setStatus(WorkflowExecution.ExecutionStatus.FAILED);
                    execution.setErrorMessage("Background execution failed: " + e.getMessage());
                    execution.setCompletedAt(OffsetDateTime.now());
                    
                    if (execution.getStartedAt() != null) {
                        long duration = java.time.Duration.between(
                            execution.getStartedAt(), 
                            execution.getCompletedAt()
                        ).toMillis();
                        execution.setDurationMs((int) duration);
                    }
                    
                    executionRepository.save(execution);
                } catch (Exception saveException) {
                    logger.error()
                        .message(LOG_FAILED_SAVE_ERROR_STATE)
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .objectType("WorkflowExecution")
                        .objectId(executionId.toString())
                        .detail("executionId", executionId)
                        .detail("error", saveException.getMessage())
                        .log();
                }
                
                throw new RuntimeException("Workflow execution failed", e);
            }
        }, threadPoolTaskExecutor.getThreadPoolExecutor());
    }
    
    @Override
    public CompletableFuture<Void> resume(UUID executionId) {
        logger.info()
            .message(LOG_RESUMING_PAUSED_WORKFLOW)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowExecution")
            .objectId(executionId.toString())
            .detail("executionId", executionId)
            .log();
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Fetch execution and workflow from database
                WorkflowExecution execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalStateException(
                        "Execution not found: " + executionId));
                
                if (execution.getStatus() != WorkflowExecution.ExecutionStatus.PAUSED) {
                    throw new IllegalStateException(
                        "Cannot resume execution that is not PAUSED. Current status: " + 
                        execution.getStatus());
                }
                
                Workflow workflow = workflowRepository.findById(execution.getWorkflowId())
                    .orElseThrow(() -> new IllegalStateException(
                        "Workflow not found: " + execution.getWorkflowId()));
                
                logger.info()
                    .message(LOG_RESUMING_FROM_NODE_ASYNC)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectType("WorkflowExecution")
                    .objectId(executionId.toString())
                    .detail("nodeId", execution.getCurrentNodeId())
                    .log();
                
                // Call the resume method on WorkflowExecutor
                workflowExecutor.resume(workflow, execution);
                
                logger.info()
                    .message(LOG_WORKFLOW_RESUMED_COMPLETED)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectType("WorkflowExecution")
                    .objectId(executionId.toString())
                    .detail("executionId", executionId)
                    .log();
                
            } catch (Exception e) {
                logger.error()
                    .message(LOG_WORKFLOW_RESUME_FAILED_BACKGROUND)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectType("WorkflowExecution")
                    .objectId(executionId.toString())
                    .detail("error", e.getMessage())
                    .log();
                
                // Update execution status to FAILED
                try {
                    WorkflowExecution execution = executionRepository.findById(executionId)
                        .orElseThrow();
                    
                    execution.setStatus(WorkflowExecution.ExecutionStatus.FAILED);
                    execution.setErrorMessage("Resume failed: " + e.getMessage());
                    execution.setCompletedAt(OffsetDateTime.now());
                    
                    if (execution.getStartedAt() != null) {
                        long duration = java.time.Duration.between(
                            execution.getStartedAt(), 
                            execution.getCompletedAt()
                        ).toMillis();
                        execution.setDurationMs((int) duration);
                    }
                    
                    executionRepository.save(execution);
                } catch (Exception saveError) {
                    logger.error()
                        .message(LOG_FAILED_UPDATE_STATUS_AFTER_RESUME_ERROR)
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .detail("error", saveError.getMessage())
                        .log();
                }
                
                throw new RuntimeException("Workflow resume failed", e);
            }
        }, threadPoolTaskExecutor);
    }
    
    @Override
    public void cancel(UUID executionId) {
        logger.info()
            .message(LOG_CANCELLING_WORKFLOW_EXECUTION)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowExecution")
            .objectId(executionId.toString())
            .detail("executionId", executionId)
            .log();
        
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new IllegalStateException(
                "Execution not found: " + executionId));
        
        // Update status to CANCELLED
        execution.setStatus(WorkflowExecution.ExecutionStatus.CANCELLED);
        execution.setCompletedAt(OffsetDateTime.now());
        execution.setErrorMessage("Execution cancelled by user");
        
        if (execution.getStartedAt() != null) {
            long duration = java.time.Duration.between(
                execution.getStartedAt(), 
                execution.getCompletedAt()
            ).toMillis();
            execution.setDurationMs((int) duration);
        }
        
        executionRepository.save(execution);
        
        logger.info()
            .message(LOG_EXECUTION_CANCELLED_SUCCESSFULLY)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowExecution")
            .objectId(executionId.toString())
            .detail("executionId", executionId)
            .log();
        
        // Note: We can't forcefully stop threads in Java safely
        // The execution will check status periodically and stop gracefully
    }
    
    @Override
    public boolean isHealthy() {
        if (threadPoolTaskExecutor == null) {
            return false;
        }
        
        int activeCount = threadPoolTaskExecutor.getActiveCount();
        int poolSize = threadPoolTaskExecutor.getPoolSize();
        int maxPoolSize = threadPoolTaskExecutor.getMaxPoolSize();
        int queueSize = threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().size();
        
        logger.debug()
            .message(LOG_THREAD_POOL_STATUS)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .detail("activeCount", activeCount)
            .detail("poolSize", poolSize)
            .detail("maxPoolSize", maxPoolSize)
            .detail("queueSize", queueSize)
            .log();
        
        // Healthy if not completely saturated
        return activeCount < maxPoolSize || queueSize < 450; // 90% of 500 capacity
    }
}
