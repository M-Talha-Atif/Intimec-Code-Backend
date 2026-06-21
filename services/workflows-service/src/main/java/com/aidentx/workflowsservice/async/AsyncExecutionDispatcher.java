package com.aidentx.workflowsservice.async;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Dispatcher interface for asynchronous workflow execution.
 * 
 * This abstraction allows easy switching between different execution strategies:
 * - ThreadPoolExecutionDispatcher (default, production)
 * - KafkaExecutionDispatcher (future enhancement)
 * - SynchronousExecutionDispatcher (testing only)
 * 
 * The dispatcher pattern enables:
 * - Non-blocking API responses
 * - Horizontal scalability
 * - Pause/resume capability
 * - Queue-based execution (future)
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
public interface AsyncExecutionDispatcher {
    
    /**
     * Dispatch a workflow execution to run asynchronously.
     * 
     * The dispatcher will:
     * 1. Fetch the execution and workflow from database (stateless)
     * 2. Execute the workflow in a background thread/process
     * 3. Update execution status in database as it progresses
     * 
     * @param executionId The ID of the workflow execution to dispatch
     * @return CompletableFuture that completes when execution finishes
     */
    CompletableFuture<Void> dispatch(UUID executionId);
    
    /**
     * Resume a paused workflow execution.
     * 
     * Used when:
     * - Approval is granted/rejected
     * - Scheduled delay expires
     * - Manual intervention triggers resume
     * 
     * @param executionId The ID of the paused execution to resume
     * @return CompletableFuture that completes when execution finishes
     * @throws IllegalStateException if execution is not in PAUSED status
     */
    CompletableFuture<Void> resume(UUID executionId);
    
    /**
     * Cancel a running or paused workflow execution.
     * 
     * This will:
     * 1. Update execution status to CANCELLED
     * 2. Stop any running threads (best effort)
     * 3. Clean up resources
     * 
     * @param executionId The ID of the execution to cancel
     */
    void cancel(UUID executionId);
    
    /**
     * Check if the dispatcher is healthy and ready to accept work.
     * 
     * Used by:
     * - Health check endpoints
     * - Load balancers
     * - Monitoring systems
     * 
     * @return true if dispatcher can accept new executions
     */
    boolean isHealthy();
}
