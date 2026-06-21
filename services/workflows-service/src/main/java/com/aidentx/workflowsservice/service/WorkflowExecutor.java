package com.aidentx.workflowsservice.service;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

import com.aidentx.workflowsservice.client.NotificationKafkaClient;
import com.aidentx.workflowsservice.entity.ParallelBranchContext;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.entity.WorkflowNodeExecution;
import com.aidentx.workflowsservice.repository.ParallelBranchContextRepository;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowNodeExecutionRepository;
import com.aidentx.workflowsservice.util.JwtUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Core workflow execution engine
 * Orchestrates the execution of workflow nodes following the graph structure
 * Phase 6b: Enhanced with parallel branch execution support
 */
@Component
public class WorkflowExecutor {

    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowNodeExecutionRepository nodeExecutionRepository;
    private final NodeExecutor nodeExecutor;
    private final NotificationKafkaClient notificationClient;
    private final ThreadPoolTaskExecutor workflowExecutorThreadPool;
    private final ParallelBranchContextRepository parallelBranchContextRepository;
    private final JwtUtils jwtUtils;
    private final IUnifiedLogger logger;

    public WorkflowExecutor(
            WorkflowExecutionRepository executionRepository,
            WorkflowNodeExecutionRepository nodeExecutionRepository,
            NodeExecutor nodeExecutor,
            NotificationKafkaClient notificationClient,
            @Qualifier("workflowExecutorThreadPool") ThreadPoolTaskExecutor workflowExecutorThreadPool,
            ParallelBranchContextRepository parallelBranchContextRepository,
            JwtUtils jwtUtils,
            UnifiedLoggerFactory loggerFactory) {
        this.executionRepository = executionRepository;
        this.nodeExecutionRepository = nodeExecutionRepository;
        this.nodeExecutor = nodeExecutor;
        this.notificationClient = notificationClient;
        this.workflowExecutorThreadPool = workflowExecutorThreadPool;
        this.parallelBranchContextRepository = parallelBranchContextRepository;
        this.jwtUtils = jwtUtils;
        this.logger = loggerFactory.getLogger(WorkflowExecutor.class, "workflow-execution");
    }

    /**
     * Extract tenant ID from the current HTTP request.
     */
    private String getCurrentTenantId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return jwtUtils.extractTenantId(request);
            }
            logger.error()
                .message(LOG_TENANT_ID_EXTRACTION_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .detail("error", "Missing request context")
                .log();
            throw new IllegalStateException("Cannot extract tenant ID - no HTTP context available");
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_TO_EXTRACT_TENANT_ID)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .detail("error", e.getMessage())
                .log();
            throw new IllegalStateException("Failed to extract tenant ID: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a workflow
     */
    public void execute(Workflow workflow, WorkflowExecution execution) {
        logger.info()
            .message(LOG_STARTING_WORKFLOW_EXECUTION)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowExecution")
            .objectId(execution.getId().toString())
            .detail("workflowId", execution.getWorkflowId().toString())
            .detail("workflowName", workflow.getName())
            .detail("triggerType", execution.getTriggerType())
            .log();

        try {
            // 1. Find start node
            Map<String, Object> startNode = findStartNode(workflow.getNodes());
            if (startNode == null) {
                logger.error()
                    .message(ERROR_NO_START_NODE)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectType("WorkflowExecution")
                    .objectId(execution.getId().toString())
                    .detail("workflowId", workflow.getId().toString())
                    .log();
                throw new IllegalStateException(ERROR_NO_START_NODE);
            }

            logger.debug()
                .message(LOG_FOUND_START_NODE)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .objectType("WorkflowExecution")
                .objectId(execution.getId().toString())
                .detail("startNodeId", startNode.get(FIELD_ID))
                .detail("startNodeType", startNode.get(FIELD_TYPE))
                .log();

            // 2. Build adjacency map for graph traversal
            Map<String, List<String>> adjacencyMap = buildAdjacencyMap(workflow.getEdges());

            // 3. Calculate step numbers for all nodes using BFS
            String startNodeId = (String) startNode.get(FIELD_ID);
            Map<String, Integer> nodeStepNumbers = calculateStepNumbers(startNodeId, adjacencyMap);

            // 4. Initialize execution context with safe defaults
            Map<String, Object> initialContext = execution.getExecutionContext() != null
                ? new HashMap<>(execution.getExecutionContext())
                : new HashMap<>();

            // 5. Execute nodes starting from start node
            executeNode(startNode, workflow, execution, adjacencyMap, initialContext, nodeStepNumbers);

            // 5. Check if execution was paused (don't mark as completed if paused)
            if (execution.getStatus() == WorkflowExecution.ExecutionStatus.PAUSED) {
                logger.info()
                    .message(LOG_WORKFLOW_EXECUTION_PAUSED)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectType("WorkflowExecution")
                    .objectId(execution.getId().toString())
                    .detail("pausedAtNode", execution.getCurrentNodeId())
                    .log();
                // Execution is already saved in executeNode when paused
                return;
            }

            // 6. Mark execution as completed (only if not paused)
            execution.setStatus(WorkflowExecution.ExecutionStatus.COMPLETED);
            execution.setCompletedAt(OffsetDateTime.now());

            // Calculate duration
            if (execution.getStartedAt() != null) {
                long duration = java.time.Duration.between(execution.getStartedAt(), execution.getCompletedAt()).toMillis();
                execution.setDurationMs((int) duration);
            }

            // Clean up any incomplete node executions (mark them as SKIPPED)
            List<WorkflowNodeExecution> nodeExecutions = nodeExecutionRepository.findByExecutionId(execution.getId());
            OffsetDateTime now = OffsetDateTime.now();
            for (WorkflowNodeExecution nodeExec : nodeExecutions) {
                if (nodeExec.getStatus() == WorkflowNodeExecution.NodeExecutionStatus.RUNNING
                    || nodeExec.getStatus() == WorkflowNodeExecution.NodeExecutionStatus.PAUSED
                    || nodeExec.getStatus() == WorkflowNodeExecution.NodeExecutionStatus.PENDING) {
                    nodeExec.setStatus(WorkflowNodeExecution.NodeExecutionStatus.SKIPPED);
                    nodeExec.setCompletedAt(now);
                    if (nodeExec.getStartedAt() != null) {
                        long nodeDuration = java.time.Duration.between(nodeExec.getStartedAt(), now).toMillis();
                        nodeExec.setDurationMs((int) nodeDuration);
                    }
                    nodeExecutionRepository.save(nodeExec);
                    logger.debug()
                        .message(LOG_MARKED_NODE_SKIPPED_COMPLETION)
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .objectType("WorkflowNodeExecution")
                        .objectId(nodeExec.getId().toString())
                        .detail("nodeId", nodeExec.getNodeId())
                        .detail("status", "SKIPPED")
                        .log();
                }
            }

            executionRepository.save(execution);

            logger.info()
                .message(LOG_WORKFLOW_EXECUTION_COMPLETED)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .objectType("WorkflowExecution")
                .objectId(execution.getId().toString())
                .detail("status", "COMPLETED")
                .detail("durationMs", execution.getDurationMs())
                .log();

            // Send workflow completion notification to requester
            if (execution.getTriggeredBy() != null) {
                try {
                    // TODO: Fetch real requester details from identity-service
                    notificationClient.sendWorkflowCompletedNotification(
                        getCurrentTenantId(),
                        execution.getTriggeredBy().toString(),
                        DEFAULT_REQUESTER_EMAIL, // TODO: Get from identity-service
                        DEFAULT_REQUESTER_NAME, // TODO: Get from identity-service
                        workflow.getName(),
                        execution.getId().toString()
                    );

                    logger.info()
                        .message(LOG_WORKFLOW_COMPLETION_NOTIFICATION_SENT)
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .objectType("WorkflowExecution")
                        .objectId(execution.getId().toString())
                        .detail("recipientUserId", execution.getTriggeredBy().toString())
                        .detail("notificationType", "workflow_completed")
                        .log();
                } catch (Exception e) {
                    logger.error()
                        .message(LOG_WORKFLOW_COMPLETION_NOTIFICATION_FAILED)
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
                        .objectId(execution.getId().toString())
                        .detail("error", e.getMessage())
                        .log();
                    // Don't fail the workflow if notification fails
                }
            }

        } catch (Exception e) {
            logger.error()
                .message(LOG_WORKFLOW_EXECUTION_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .objectType("WorkflowExecution")
                .objectId(execution.getId().toString())
                .detail("error", e.getMessage())
                .detail("errorType", e.getClass().getSimpleName())
                .detail("workflowId", workflow.getId().toString())
                .log();
            execution.setStatus(WorkflowExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(OffsetDateTime.now());

            if (execution.getStartedAt() != null) {
                long duration = java.time.Duration.between(execution.getStartedAt(), execution.getCompletedAt()).toMillis();
                execution.setDurationMs((int) duration);
            }

            executionRepository.save(execution);
            throw e;
        }
    }

    /**
     * Resume a paused workflow execution
     * Used when approval is granted or when resuming after manual intervention
     */
    public void resume(Workflow workflow, WorkflowExecution execution) {
        logger.info()
            .message(LOG_RESUMING_WORKFLOW_EXECUTION)
            .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
            .objectType("WorkflowExecution")
            .objectId(execution.getId().toString())
            .detail("currentNodeId", execution.getCurrentNodeId())
            .detail("previousStatus", execution.getStatus().toString())
            .log();

        if (execution.getStatus() != WorkflowExecution.ExecutionStatus.PAUSED) {
            throw new IllegalStateException(
                ERROR_EXECUTION_NOT_PAUSED + execution.getStatus() + ")"
            );
        }

        if (execution.getCurrentNodeId() == null) {
            throw new IllegalStateException(ERROR_EXECUTION_CURRENT_NODE_NULL);
        }

        try {
            // Update status to RUNNING
            execution.setStatus(WorkflowExecution.ExecutionStatus.RUNNING);
            executionRepository.save(execution);

            // Build adjacency map
            Map<String, List<String>> adjacencyMap = buildAdjacencyMap(workflow.getEdges());

            // Calculate step numbers for proper step ordering
            Map<String, Object> startNode = findStartNode(workflow.getNodes());
            String startNodeId = startNode != null ? (String) startNode.get(FIELD_ID) : null;
            Map<String, Integer> nodeStepNumbers = startNodeId != null
                ? calculateStepNumbers(startNodeId, adjacencyMap)
                : new HashMap<>();

            // Retrieve context from execution
            Map<String, Object> context = execution.getExecutionContext() != null
                ? new HashMap<>(execution.getExecutionContext())
                : new HashMap<>();

            // Find the paused node
            Map<String, Object> pausedNode = findNodeById(workflow.getNodes(), execution.getCurrentNodeId());
            if (pausedNode == null) {
                throw new IllegalStateException(ERROR_PAUSED_NODE_NOT_FOUND + execution.getCurrentNodeId());
            }

            // Get next nodes after the paused node
            List<String> nextNodeIds = adjacencyMap.getOrDefault(execution.getCurrentNodeId(), List.of());

            logger.info()
                .message(LOG_RESUMING_FROM_NODE)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .objectType("WorkflowExecution")
                .objectId(execution.getId().toString())
                .detail("pausedNodeId", execution.getCurrentNodeId())
                .detail("nextNodesCount", nextNodeIds.size())
                .log();

            // Mark the paused node execution as COMPLETED (it was approved/resumed)
            List<WorkflowNodeExecution> pausedNodeExecutions = nodeExecutionRepository
                .findByExecutionId(execution.getId()).stream()
                .filter(ne -> pausedNode.get("id").equals(ne.getNodeId())
                    && ne.getStatus() == WorkflowNodeExecution.NodeExecutionStatus.PAUSED)
                .toList();

            for (WorkflowNodeExecution nodeExec : pausedNodeExecutions) {
                // CRITICAL: Reload from database to get fresh approval/rejection decision data
                // The approval service just updated output_data, but JPA might have stale cache
                WorkflowNodeExecution freshNodeExec = nodeExecutionRepository.findById(nodeExec.getId())
                    .orElse(nodeExec);

                // CRITICAL: Merge node output data (approval/rejection decision) into execution context
                // This ensures the decision data flows to subsequent nodes
                logger.debug()
                    .message(LOG_CHECKING_NODE_OUTPUT_DATA)
                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                    .objectType("WorkflowNodeExecution")
                    .objectId(freshNodeExec.getId().toString())
                    .detail("hasOutputData", freshNodeExec.getOutputData() != null)
                    .detail("outputDataEmpty", freshNodeExec.getOutputData() != null && freshNodeExec.getOutputData().isEmpty())
                    .log();

                if (freshNodeExec.getOutputData() != null && !freshNodeExec.getOutputData().isEmpty()) {
                    logger.debug()
                        .message(LOG_MERGING_NODE_OUTPUT_DATA)
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .objectType("WorkflowNodeExecution")
                        .objectId(freshNodeExec.getId().toString())
                        .detail("outputDataKeys", freshNodeExec.getOutputData().keySet().toString())
                        .detail("keyCount", freshNodeExec.getOutputData().size())
                        .log();
                    context.putAll(freshNodeExec.getOutputData());
                } else {
                    logger.warn()
                        .message(LOG_NODE_NO_OUTPUT_DATA)
                        .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                        .objectType("NodeExecution")
                        .objectId(freshNodeExec.getId().toString())
                        .log();
                }

                freshNodeExec.setStatus(WorkflowNodeExecution.NodeExecutionStatus.COMPLETED);
                freshNodeExec.setCompletedAt(OffsetDateTime.now());
                if (freshNodeExec.getStartedAt() != null) {
                    long nodeDuration = java.time.Duration.between(freshNodeExec.getStartedAt(), freshNodeExec.getCompletedAt()).toMillis();
                    freshNodeExec.setDurationMs((int) nodeDuration);
                }
                // Note: Save with approval/rejection decision data intact
                nodeExecutionRepository.save(freshNodeExec);
                logger.info()
                    .message(LOG_MARKED_PAUSED_NODE_COMPLETED)
                    .category("workflow-execution")
                    .objectType("WorkflowNodeExecution")
                    .objectId(freshNodeExec.getId().toString())
                    .detail("nodeId", freshNodeExec.getNodeId())
                    .detail("hasDecisionData", freshNodeExec.getOutputData() != null && !freshNodeExec.getOutputData().isEmpty())
                    .log();
            }

            // Clear current node ID and save updated context
            execution.setCurrentNodeId(null);
            execution.setExecutionContext(context); // Save merged context with approval/rejection data
            executionRepository.save(execution);
            logger.debug()
                .message(LOG_UPDATED_CONTEXT_AFTER_RESUME)
                .category("workflow-execution")
                .objectType("WorkflowExecution")
                .objectId(execution.getId().toString())
                .detail("contextKeyCount", context.size())
                .log();

            // Execute next nodes
            for (String nextNodeId : nextNodeIds) {
                Map<String, Object> nextNode = findNodeById(workflow.getNodes(), nextNodeId);
                if (nextNode != null) {
                    if (shouldExecuteNode(pausedNode, nextNode, workflow.getEdges(), context)) {
                        executeNode(nextNode, workflow, execution, adjacencyMap, new HashMap<>(context), nodeStepNumbers);
                    } else {
                        logger.debug()
                            .message(LOG_SKIPPING_NODE_DUE_TO_CONDITION)
                            .category("workflow-execution")
                            .objectType("WorkflowNode")
                            .objectId(nextNodeId)
                            .detail("executionId", execution.getId().toString())
                            .detail("reason", "condition_not_met")
                            .log();
                    }
                }
            }

            // Only mark as completed if execution is still RUNNING (not paused again or failed)
            // Refresh execution state from DB to get latest status
            WorkflowExecution refreshedExecution = executionRepository.findById(execution.getId())
                .orElseThrow(() -> new IllegalStateException("Execution not found: " + execution.getId()));

            if (refreshedExecution.getStatus() == WorkflowExecution.ExecutionStatus.RUNNING) {
                // Mark execution as completed
                refreshedExecution.setStatus(WorkflowExecution.ExecutionStatus.COMPLETED);
                refreshedExecution.setCompletedAt(OffsetDateTime.now());

                // Calculate duration
                if (refreshedExecution.getStartedAt() != null) {
                    long duration = java.time.Duration.between(refreshedExecution.getStartedAt(), refreshedExecution.getCompletedAt()).toMillis();
                    refreshedExecution.setDurationMs((int) duration);
                }

                executionRepository.save(refreshedExecution);

                logger.info()
                    .message(LOG_WORKFLOW_RESUMED_AND_COMPLETED)
                    .category("workflow-execution")
                    .objectType("WorkflowExecution")
                    .objectId(refreshedExecution.getId().toString())
                    .detail("status", "COMPLETED")
                    .detail("durationMs", refreshedExecution.getDurationMs())
                    .log();
            } else {
                logger.debug()
                    .message(LOG_WORKFLOW_NOT_MARKED_COMPLETED)
                    .category("workflow-execution")
                    .objectType("WorkflowExecution")
                    .objectId(refreshedExecution.getId().toString())
                    .detail("currentStatus", refreshedExecution.getStatus().toString())
                    .detail("reason", "status_not_running")
                    .log();
            }

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_RESUME_WORKFLOW_EXECUTION)
                .category("workflow-execution")
                .objectType("WorkflowExecution")
                .objectId(execution.getId().toString())
                .detail("error", e.getMessage())
                .detail("errorType", e.getClass().getSimpleName())
                .log();
            execution.setStatus(WorkflowExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(OffsetDateTime.now());

            if (execution.getStartedAt() != null) {
                long duration = java.time.Duration.between(execution.getStartedAt(), execution.getCompletedAt()).toMillis();
                execution.setDurationMs((int) duration);
            }

            executionRepository.save(execution);
            throw e;
        }
    }

    /**
     * Execute a single node and its descendants
     */
    private Map<String, Object> executeNode(
        Map<String, Object> node,
        Workflow workflow,
        WorkflowExecution execution,
        Map<String, List<String>> adjacencyMap,
        Map<String, Object> context,
        Map<String, Integer> nodeStepNumbers
    ) {
        String nodeId = (String) node.get(FIELD_ID);
        String nodeType = (String) node.get(FIELD_TYPE);

        logger.debug()
            .message(LOG_EXECUTING_WORKFLOW_NODE)
            .category("workflow-execution")
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("nodeType", nodeType)
            .detail("executionId", execution.getId().toString())
            .log();

        // Add to path trace - store as simple Map entries
        List<Map<String, Object>> pathTrace = execution.getPathTrace();
        if (pathTrace == null) {
            pathTrace = new ArrayList<>();
        }

        // Create a mutable map for the trace entry
        Map<String, Object> traceEntry = new HashMap<>();
        traceEntry.put(CONTEXT_KEY_NODE_ID, nodeId);
        traceEntry.put(CONTEXT_KEY_NODE_TYPE, nodeType);
        traceEntry.put(CONTEXT_KEY_TIMESTAMP, System.currentTimeMillis());

        pathTrace.add(traceEntry);
        execution.setPathTrace(pathTrace);
        executionRepository.save(execution);

        // Create node execution record
        Integer stepNumber = nodeStepNumbers.getOrDefault(nodeId, 0);
        WorkflowNodeExecution nodeExecution = WorkflowNodeExecution.builder()
            .executionId(execution.getId())
            .nodeId(nodeId)
            .stepType(nodeType)
            .stepNumber(stepNumber)
            .status(WorkflowNodeExecution.NodeExecutionStatus.RUNNING)
            .startedAt(OffsetDateTime.now())
            .inputData(context)
            .build();

        nodeExecution = nodeExecutionRepository.save(nodeExecution);

        try {
            // Execute the node
            Map<String, Object> nodeOutput = nodeExecutor.execute(node, context, execution);

            // Check if execution should pause (approval nodes or parallel join waiting)
            if (nodeOutput.containsKey(CONTEXT_KEY_PAUSE_REQUIRED) && Boolean.TRUE.equals(nodeOutput.get(CONTEXT_KEY_PAUSE_REQUIRED))) {
                logger.info()
                    .message(LOG_PAUSING_WORKFLOW_AT_NODE)
                    .category("workflow-execution")
                    .objectType("WorkflowNode")
                    .objectId(nodeId)
                    .detail("executionId", execution.getId().toString())
                    .detail("reason", nodeOutput.get(CONTEXT_KEY_REASON).toString())
                    .log();

                // Merge node output into context before pausing
                context.putAll(nodeOutput);

                // Update execution status to PAUSED and save context
                execution.setStatus(WorkflowExecution.ExecutionStatus.PAUSED);
                execution.setCurrentNodeId(nodeId);
                execution.setExecutionContext(new HashMap<>(context));
                executionRepository.save(execution);
                logger.debug()
                    .message(LOG_SAVED_CONTEXT_BEFORE_PAUSING)
                    .category("workflow-execution")
                    .objectType("WorkflowExecution")
                    .objectId(execution.getId().toString())
                    .detail("contextKeyCount", context.size())
                    .log();

                // Update node execution to PAUSED
                nodeExecution.setStatus(WorkflowNodeExecution.NodeExecutionStatus.PAUSED);
                nodeExecution.setOutputData(nodeOutput);
                nodeExecutionRepository.save(nodeExecution);

                logger.info()
                    .message(LOG_EXECUTION_PAUSED_SUCCESSFULLY)
                    .category("workflow-execution")
                    .objectType("WorkflowExecution")
                    .objectId(execution.getId().toString())
                    .detail("pausedAtNode", nodeId)
                    .detail("pauseReason", nodeOutput.get(CONTEXT_KEY_REASON).toString())
                    .log();

                // Stop execution here (don't continue to next nodes)
                return context;
            }

            // Phase 6b: Check if this is a parallel join that just completed
            boolean isParallelJoinComplete = nodeOutput.containsKey(CONTEXT_KEY_ALL_BRANCHES_COMPLETE) &&
                                            Boolean.TRUE.equals(nodeOutput.get(CONTEXT_KEY_ALL_BRANCHES_COMPLETE));

            if (isParallelJoinComplete) {
                logger.info()
                    .message(LOG_PARALLEL_JOIN_COMPLETED_MERGED_CONTEXT)
                    .category("parallel-execution")
                    .objectType("WorkflowNode")
                    .objectId(nodeId)
                    .detail("executionId", execution.getId().toString())
                    .detail("completedBranches", nodeOutput.get(CONTEXT_KEY_COMPLETED_BRANCHES).toString())
                    .log();

                // Update execution status back to RUNNING if it was paused
                if (execution.getStatus() == WorkflowExecution.ExecutionStatus.PAUSED) {
                    execution.setStatus(WorkflowExecution.ExecutionStatus.RUNNING);
                    execution.setCurrentNodeId(null);
                    executionRepository.save(execution);
                }

                // Replace context with merged context from join node
                context.clear();
                context.putAll(nodeOutput);

                logger.debug()
                    .message(LOG_CONTEXT_REPLACED_PARALLEL_BRANCHES)
                    .category("parallel-execution")
                    .objectType("WorkflowNode")
                    .objectId(nodeId)
                    .detail("mergedBranchCount", nodeOutput.get(CONTEXT_KEY_COMPLETED_BRANCHES).toString())
                    .log();
            }

            // Update node execution record
            nodeExecution.setStatus(WorkflowNodeExecution.NodeExecutionStatus.COMPLETED);
            nodeExecution.setCompletedAt(OffsetDateTime.now());
            nodeExecution.setOutputData(nodeOutput);

            // Calculate duration
            if (nodeExecution.getStartedAt() != null) {
                long duration = java.time.Duration.between(nodeExecution.getStartedAt(), nodeExecution.getCompletedAt()).toMillis();
                nodeExecution.setDurationMs((int) duration);
            }

            nodeExecutionRepository.save(nodeExecution);

            // Merge node output into context (unless already merged for parallel join)
            if (!isParallelJoinComplete) {
                context.putAll(nodeOutput);
            }

            // CRITICAL: Save updated context back to execution entity
            // This ensures every node has access to the complete context from all previous nodes
            execution.setExecutionContext(new HashMap<>(context));
            executionRepository.save(execution);
            logger.trace()
                .message(LOG_UPDATED_CONTEXT_AFTER_NODE_COMPLETION)
                .category("workflow-execution")
                .objectType("WorkflowExecution")
                .objectId(execution.getId().toString())
                .detail("nodeId", nodeId)
                .detail("contextKeyCount", context.size())
                .log();

            // Check if this is an end node
            if (nodeType != null && nodeType.equals(NODE_TYPE_TRIGGER_END)) {
                logger.info()
                    .message(LOG_REACHED_WORKFLOW_END_NODE)
                    .category("workflow-execution")
                    .objectType("WorkflowNode")
                    .objectId(nodeId)
                    .detail("executionId", execution.getId().toString())
                    .detail("finalContextKeys", context.keySet().toString())
                    .detail("contextKeyCount", context.keySet().size())
                    .log();
                return context;
            }

            // Get next nodes
            List<String> nextNodeIds = adjacencyMap.getOrDefault(nodeId, List.of());

            if (nextNodeIds.isEmpty()) {
                logger.warn()
                    .message(LOG_NO_NEXT_NODES_AFTER_CURRENT)
                    .category("workflow-execution")
                    .objectType("WorkflowNode")
                    .objectId(nodeId)
                    .detail("executionId", execution.getId().toString())
                    .detail("nodeType", nodeType)
                    .log();
                return context;
            }

            // Phase 6b: Check if this was a parallel-split node
            boolean isParallelSplit = nodeOutput.containsKey(CONTEXT_KEY_SPLIT_NODE) &&
                                     Boolean.TRUE.equals(nodeOutput.get(CONTEXT_KEY_SPLIT_NODE));

            if (isParallelSplit && nextNodeIds.size() > 1) {
                // Parallel execution: Dispatch each branch to thread pool
                logger.info()
                    .message(LOG_DISPATCHING_PARALLEL_BRANCHES)
                    .category("parallel-execution")
                    .objectType("WorkflowNode")
                    .objectId(nodeId)
                    .detail("branchCount", nextNodeIds.size())
                    .detail("executionId", execution.getId().toString())
                    .log();

                String joinNodeId = (String) nodeOutput.get(CONTEXT_KEY_JOIN_NODE);

                // Create branch context records for each branch
                int branchIndex = 0;
                for (String nextNodeId : nextNodeIds) {
                    Map<String, Object> nextNode = findNodeById(workflow.getNodes(), nextNodeId);
                    if (nextNode != null && shouldExecuteNode(node, nextNode, workflow.getEdges(), context)) {
                        // Execute each branch in separate thread (async)
                        final Map<String, Object> branchContext = new HashMap<>(context);
                        final int currentBranchIndex = branchIndex;

                        // Create and save branch context record
                        ParallelBranchContext branchContextEntity = ParallelBranchContext.builder()
                            .executionId(execution.getId())
                            .splitNodeId(nodeId)
                            .joinNodeId(joinNodeId)
                            .branchIndex(currentBranchIndex)
                            .branchContext(branchContext)
                            .isComplete(false)
                            .build();

                        parallelBranchContextRepository.save(branchContextEntity);

                        logger.trace()
                            .message(LOG_TRACE_PARALLEL_BRANCH_CREATED)
                            .category("parallel-execution")
                            .objectType("ParallelBranch")
                            .objectId(branchContextEntity.getId().toString())
                            .detail("branchIndex", currentBranchIndex)
                            .detail("nextNodeId", nextNodeId)
                            .detail("splitNodeId", nodeId)
                            .log();

                        // Store branch context ID in the branch's execution context
                        branchContext.put("branchContextId", branchContextEntity.getId().toString());
                        branchContext.put("branchIndex", currentBranchIndex);

                        CompletableFuture.runAsync(() -> {
                            try {
                                logger.info()
                                    .message(LOG_PARALLEL_BRANCH_DISPATCHED)
                                    .category("parallel-execution")
                                    .objectType("WorkflowNode")
                                    .objectId(nextNodeId)
                                    .detail("branchIndex", currentBranchIndex)
                                    .detail("executionId", execution.getId().toString())
                                    .detail("splitNodeId", nodeId)
                                    .log();
                                executeNode(nextNode, workflow, execution, adjacencyMap, branchContext, nodeStepNumbers);

                                // Phase 7: Mark branch as successful
                                markBranchCompleted(branchContextEntity.getId(), true, null);
                                logger.info()
                                    .message(LOG_PARALLEL_BRANCH_COMPLETED_SUCCESSFULLY)
                                    .category("parallel-execution")
                                    .objectType("ParallelBranchContext")
                                    .objectId(branchContextEntity.getId().toString())
                                    .detail("branchIndex", currentBranchIndex)
                                    .detail("executionId", execution.getId().toString())
                                    .log();

                            } catch (Exception e) {
                                logger.error()
                                    .message(LOG_PARALLEL_BRANCH_FAILED_EXEC)
                                    .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                                    .objectType(OBJECT_TYPE_WORKFLOW_EXECUTION)
                                    .objectId(execution.getId().toString())
                                    .detail("branchIndex", String.valueOf(currentBranchIndex))
                                    .detail("error", e.getMessage())
                                    .log();

                                // Phase 7: Mark branch as failed
                                markBranchCompleted(branchContextEntity.getId(), false, e.getMessage());

                                // Phase 7: Check failure policy
                                String failurePolicy = (String) nodeOutput.getOrDefault(FIELD_FAILURE_POLICY, DEFAULT_FAILURE_POLICY);

                                if ("FAIL_FAST".equals(failurePolicy)) {
                                    // Fail entire workflow immediately
                                    failWorkflowExecution(execution.getId(), ERROR_PARALLEL_BRANCH_FAILED + ": " + e.getMessage());
                                }
                                // FAIL_SAFE and THRESHOLD: Handle at join node
                            }
                        }, workflowExecutorThreadPool.getThreadPoolExecutor());

                        branchIndex++;
                    }
                }

                // Don't continue sequentially - branches will execute in parallel
                return context;
            }

            // Normal sequential execution (not a parallel split)
            for (String nextNodeId : nextNodeIds) {
                Map<String, Object> nextNode = findNodeById(workflow.getNodes(), nextNodeId);
                if (nextNode != null) {
                    // Check edge conditions (for conditional flows)
                    if (shouldExecuteNode(node, nextNode, workflow.getEdges(), context)) {
                        executeNode(nextNode, workflow, execution, adjacencyMap, new HashMap<>(context), nodeStepNumbers);
                    } else {
                        logger.debug()
                            .message(LOG_SKIPPING_NODE_DUE_TO_CONDITION)
                            .category("workflow-execution")
                            .objectType("WorkflowNode")
                            .objectId(nextNodeId)
                            .detail("executionId", execution.getId().toString())
                            .detail("reason", "condition_not_met")
                            .log();
                    }
                }
            }

            return context;

        } catch (Exception e) {
            logger.error()
                .message(LOG_NODE_EXECUTION_FAILED)
                .category("node-execution")
                .objectType("WorkflowNode")
                .objectId(nodeId)
                .detail("executionId", execution.getId().toString())
                .detail("nodeType", nodeType)
                .detail("error", e.getMessage())
                .detail("errorType", e.getClass().getSimpleName())
                .log();

            nodeExecution.setStatus(WorkflowNodeExecution.NodeExecutionStatus.FAILED);
            nodeExecution.setCompletedAt(OffsetDateTime.now());

            // Create mutable map for error details
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put(CONTEXT_KEY_ERROR, e.getMessage());
            errorDetails.put(CONTEXT_KEY_NODE_ID, nodeId);
            errorDetails.put(CONTEXT_KEY_STACK_TRACE, e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : DEFAULT_STACK_TRACE_UNKNOWN);
            nodeExecution.setErrorDetails(errorDetails);

            if (nodeExecution.getStartedAt() != null) {
                long duration = java.time.Duration.between(nodeExecution.getStartedAt(), nodeExecution.getCompletedAt()).toMillis();
                nodeExecution.setDurationMs((int) duration);
            }

            nodeExecutionRepository.save(nodeExecution);

            throw new RuntimeException(ERROR_NODE_EXECUTION_FAILED + nodeId + " - " + e.getMessage(), e);
        }
    }

    /**
     * Find start node (type: trigger.start)
     */
    private Map<String, Object> findStartNode(List<Map<String, Object>> nodes) {
        return nodes.stream()
            .filter(node -> {
                String type = (String) node.get(FIELD_TYPE);
                return type != null && type.equals(NODE_TYPE_TRIGGER_START);
            })
            .findFirst()
            .orElse(null);
    }

    /**
     * Find node by ID
     */
    private Map<String, Object> findNodeById(List<Map<String, Object>> nodes, String nodeId) {
        return nodes.stream()
            .filter(node -> nodeId.equals(node.get(FIELD_ID)))
            .findFirst()
            .orElse(null);
    }

    /**
     * Build adjacency map from edges for graph traversal
     * Returns map of: sourceNodeId -> [targetNodeId1, targetNodeId2, ...]
     */
    private Map<String, List<String>> buildAdjacencyMap(List<Map<String, Object>> edges) {
        Map<String, List<String>> adjacencyMap = new HashMap<>();

        for (Map<String, Object> edge : edges) {
            String source = (String) edge.get(FIELD_SOURCE);
            String target = (String) edge.get(FIELD_TARGET);

            if (source != null && target != null) {
                adjacencyMap.computeIfAbsent(source, k -> new ArrayList<>()).add(target);
            }
        }

        return adjacencyMap;
    }

    /**
     * Calculate step numbers for all nodes using BFS from start node
     * This provides logical step numbers based on graph distance, not execution order
     * Returns map of: nodeId -> stepNumber (1-indexed)
     */
    private Map<String, Integer> calculateStepNumbers(String startNodeId, Map<String, List<String>> adjacencyMap) {
        Map<String, Integer> stepNumbers = new HashMap<>();
        Queue<String> queue = new LinkedList<>();

        // Start node is step 1
        queue.offer(startNodeId);
        stepNumbers.put(startNodeId, 1);

        while (!queue.isEmpty()) {
            String currentNodeId = queue.poll();
            Integer currentStep = stepNumbers.get(currentNodeId);

            List<String> nextNodes = adjacencyMap.getOrDefault(currentNodeId, List.of());
            for (String nextNodeId : nextNodes) {
                // Only assign step number if not already visited
                if (!stepNumbers.containsKey(nextNodeId)) {
                    stepNumbers.put(nextNodeId, currentStep + 1);
                    queue.offer(nextNodeId);
                }
            }
        }

        logger.trace()
            .message(LOG_CALCULATED_STEP_NUMBERS)
            .category("workflow-execution")
            .detail("nodeCount", stepNumbers.size())
            .detail("stepNumbers", stepNumbers.toString())
            .log();
        return stepNumbers;
    }

    /**
     * Check if a node should be executed based on edge conditions
     * Evaluates conditional branching from condition.if nodes
     */
    private boolean shouldExecuteNode(
        Map<String, Object> sourceNode,
        Map<String, Object> targetNode,
        List<Map<String, Object>> edges,
        Map<String, Object> context
    ) {
        String sourceId = (String) sourceNode.get(FIELD_ID);
        String targetId = (String) targetNode.get(FIELD_ID);
        String sourceType = (String) sourceNode.get(FIELD_TYPE);

        // Find edge between source and target
        Optional<Map<String, Object>> edge = edges.stream()
            .filter(e -> sourceId.equals(e.get(FIELD_SOURCE)) && targetId.equals(e.get(FIELD_TARGET)))
            .findFirst();

        if (edge.isEmpty()) {
            return true; // No edge found, allow execution
        }

        Map<String, Object> edgeData = edge.get();

        // Check if source is a condition.if node
        if ("condition.if".equals(sourceType)) {
            logger.trace()
                .message(LOG_TRACE_EVALUATING_CONDITION)
                .category("workflow-execution")
                .objectType("WorkflowNode")
                .objectId(sourceId)
                .detail("targetNodeId", targetId)
                .detail("contextSize", context.size())
                .log();

            // Get the sourceHandle (which branch: "true" or "false")
            String sourceHandle = (String) edgeData.get("sourceHandle");

            if (sourceHandle == null) {
                logger.warn()
                    .message(LOG_CONDITION_NODE_EDGE_NO_SOURCE_HANDLE)
                    .category("workflow-execution")
                    .objectType("WorkflowNode")
                    .objectId(sourceId)
                    .detail("targetNodeId", targetId)
                    .detail("action", "allowing_execution")
                    .log();
                return true;
            }

            // Get condition result from context (stored by executeConditionIf)
            Boolean conditionMet = (Boolean) context.get("conditionMet");

            if (conditionMet == null) {
                logger.warn()
                    .message(LOG_CONDITION_RESULT_NOT_IN_CONTEXT)
                    .category("workflow-execution")
                    .objectType("WorkflowNode")
                    .objectId(sourceId)
                    .detail("defaultValue", false)
                    .log();
                conditionMet = false;
            }

            // Execute node only if sourceHandle matches condition result
            boolean shouldExecute = (sourceHandle.equalsIgnoreCase("true") && conditionMet) ||
                                   (sourceHandle.equalsIgnoreCase("false") && !conditionMet);

            logger.debug()
                .message(LOG_CONDITIONAL_BRANCH_EVALUATION)
                .category("condition-evaluation")
                .objectType("WorkflowNode")
                .objectId(sourceId)
                .detail("sourceHandle", sourceHandle)
                .detail("conditionMet", conditionMet)
                .detail("shouldExecute", shouldExecute)
                .log();

            return shouldExecute;
        }

        // Check if source is a condition.switch node
        if ("condition.switch".equals(sourceType)) {
            String sourceHandle = (String) edgeData.get("sourceHandle");
            String selectedBranch = (String) context.get("selectedBranch");

            if (sourceHandle == null || selectedBranch == null) {
                logger.warn()
                    .message(LOG_SWITCH_NODE_MISSING_SOURCE_HANDLE)
                    .category("condition-evaluation")
                    .objectType("WorkflowNode")
                    .objectId(sourceId)
                    .detail("action", "allowing_execution")
                    .log();
                return true;
            }

            boolean shouldExecute = sourceHandle.equalsIgnoreCase(selectedBranch);

            logger.debug()
                .message(LOG_SWITCH_BRANCH_EVALUATION)
                .category("condition-evaluation")
                .objectType("WorkflowNode")
                .objectId(sourceId)
                .detail("sourceHandle", sourceHandle)
                .detail("selectedBranch", selectedBranch)
                .detail("shouldExecute", shouldExecute)
                .log();

            return shouldExecute;
        }

        // For non-condition nodes, check if edge has explicit condition
        Object conditionObj = edgeData.get(FIELD_CONDITION);
        if (conditionObj != null) {
            logger.debug()
                .message(LOG_EDGE_EXPLICIT_CONDITION_NOT_IMPLEMENTED)
                .category("workflow-execution")
                .objectType("WorkflowEdge")
                .detail("sourceId", sourceId)
                .detail("targetId", targetId)
                .detail("condition", conditionObj.toString())
                .log();
            // TODO: Implement explicit edge condition evaluation
        }

        return true; // Default: allow execution
    }

    // ============ Phase 7: Parallel Branch Failure Handling ============

    /**
     * Mark a parallel branch as completed (success or failure)
     * Phase 7: Updates ParallelBranchContext with completion status
     */
    private void markBranchCompleted(UUID branchContextId, boolean successful, String errorMessage) {
        try {
            ParallelBranchContext branchContext = parallelBranchContextRepository.findById(branchContextId)
                .orElseThrow(() -> new IllegalArgumentException("Branch context not found: " + branchContextId));

            branchContext.setIsComplete(true);
            branchContext.setIsSuccessful(successful);
            branchContext.setErrorMessage(errorMessage);
            branchContext.setCompletedAt(OffsetDateTime.now());

            parallelBranchContextRepository.save(branchContext);

            logger.info()
                .message(LOG_PARALLEL_BRANCH_MARKED_COMPLETED)
                .category("parallel-execution")
                .objectType("ParallelBranchContext")
                .objectId(branchContextId.toString())
                .detail("successful", successful)
                .detail("errorMessage", errorMessage != null ? errorMessage : "none")
                .log();

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_MARK_BRANCH_COMPLETED)
                .category(LOG_CATEGORY_WORKFLOW_EXECUTION)
                .objectType("ParallelBranchContext")
                .objectId(branchContextId.toString())
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Fail workflow execution immediately
     * Phase 7: Called when FAIL_FAST policy is triggered
     */
    private void failWorkflowExecution(UUID executionId, String reason) {
        try {
            WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

            execution.setStatus(WorkflowExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(reason);
            execution.setCompletedAt(OffsetDateTime.now());

            // Calculate duration
            if (execution.getStartedAt() != null) {
                long duration = java.time.Duration.between(
                    execution.getStartedAt(),
                    execution.getCompletedAt()
                ).toMillis();
                execution.setDurationMs((int) duration);
            }

            executionRepository.save(execution);
            logger.error()
                .message(LOG_WORKFLOW_EXECUTION_FAILED_STATUS)
                .category("parallel-execution")
                .objectType("WorkflowExecution")
                .objectId(executionId.toString())
                .detail("failureReason", reason)
                .detail("failurePolicy", "FAIL_FAST")
                .log();

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_UPDATE_STATUS_TO_FAILED)
                .category("workflow-execution")
                .objectType("WorkflowExecution")
                .objectId(executionId.toString())
                .detail("error", e.getMessage())
                .log();
        }
    }
}
