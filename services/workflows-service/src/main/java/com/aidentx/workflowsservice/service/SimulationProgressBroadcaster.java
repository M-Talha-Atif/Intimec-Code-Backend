package com.aidentx.workflowsservice.service;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.entity.WorkflowNodeExecution;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowNodeExecutionRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Broadcasts real-time workflow simulation progress updates via WebSocket.
 *
 * <p>Sends node execution updates to connected clients during simulation runs,
 * enabling real-time UI updates showing execution progress.</p>
 *
 * @author AidentX Development Team
 * @since Phase 7
 */
@Component
public class SimulationProgressBroadcaster {

    private final IUnifiedLogger logger;
    private final SimpMessagingTemplate messagingTemplate;
    private final WorkflowNodeExecutionRepository nodeExecutionRepository;
    private final WorkflowExecutionRepository executionRepository;

    public SimulationProgressBroadcaster(
            SimpMessagingTemplate messagingTemplate,
            WorkflowNodeExecutionRepository nodeExecutionRepository,
            WorkflowExecutionRepository executionRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.messagingTemplate = messagingTemplate;
        this.nodeExecutionRepository = nodeExecutionRepository;
        this.executionRepository = executionRepository;
        this.logger = loggerFactory.getLogger(SimulationProgressBroadcaster.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Broadcast that a node has started executing
     */
    public void broadcastNodeStart(UUID executionId, String nodeId, String nodeName, String nodeType) {
        Map<String, Object> update = new HashMap<>();
        update.put("event", "node_start");
        update.put("executionId", executionId.toString());
        update.put("nodeId", nodeId);
        update.put("nodeName", nodeName);
        update.put("nodeType", nodeType);
        update.put("timestamp", System.currentTimeMillis());

        sendUpdate(executionId, update);

        logger.debug()
            .message("Broadcasting node start")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("executionId", executionId.toString())
            .detail("nodeType", nodeType)
            .log();
    }

    /**
     * Broadcast that a node has completed successfully
     */
    public void broadcastNodeComplete(
            UUID executionId,
            String nodeId,
            String nodeName,
            String nodeType,
            long duration,
            Map<String, Object> output) {

        Map<String, Object> update = new HashMap<>();
        update.put("event", "node_complete");
        update.put("executionId", executionId.toString());
        update.put("nodeId", nodeId);
        update.put("nodeName", nodeName);
        update.put("nodeType", nodeType);
        update.put("duration", duration);
        update.put("output", output);
        update.put("status", "completed");
        update.put("timestamp", System.currentTimeMillis());

        sendUpdate(executionId, update);

        logger.debug()
            .message("Broadcasting node completion")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("executionId", executionId.toString())
            .detail("duration", duration)
            .log();
    }

    /**
     * Broadcast that a node has failed
     */
    public void broadcastNodeError(
            UUID executionId,
            String nodeId,
            String nodeName,
            String nodeType,
            long duration,
            String errorMessage) {

        Map<String, Object> update = new HashMap<>();
        update.put("event", "node_error");
        update.put("executionId", executionId.toString());
        update.put("nodeId", nodeId);
        update.put("nodeName", nodeName);
        update.put("nodeType", nodeType);
        update.put("duration", duration);
        update.put("error", errorMessage);
        update.put("status", "failed");
        update.put("timestamp", System.currentTimeMillis());

        sendUpdate(executionId, update);

        logger.warn()
            .message("Broadcasting node error")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("executionId", executionId.toString())
            .detail("error", errorMessage)
            .log();
    }

    /**
     * Broadcast that a node is being skipped
     */
    public void broadcastNodeSkipped(
            UUID executionId,
            String nodeId,
            String nodeName,
            String nodeType,
            String reason) {

        Map<String, Object> update = new HashMap<>();
        update.put("event", "node_skipped");
        update.put("executionId", executionId.toString());
        update.put("nodeId", nodeId);
        update.put("nodeName", nodeName);
        update.put("nodeType", nodeType);
        update.put("reason", reason);
        update.put("status", "skipped");
        update.put("timestamp", System.currentTimeMillis());

        sendUpdate(executionId, update);

        logger.debug()
            .message("Broadcasting node skip")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("executionId", executionId.toString())
            .detail("reason", reason)
            .log();
    }

    /**
     * Broadcast that a node is waiting for approval
     */
    public void broadcastNodeAwaitingApproval(
            UUID executionId,
            String nodeId,
            String nodeName,
            String nodeType,
            Map<String, Object> approvalData) {

        Map<String, Object> update = new HashMap<>();
        update.put("event", "node_awaiting_approval");
        update.put("executionId", executionId.toString());
        update.put("nodeId", nodeId);
        update.put("nodeName", nodeName);
        update.put("nodeType", nodeType);
        update.put("approvalData", approvalData);
        update.put("status", "paused");
        update.put("timestamp", System.currentTimeMillis());

        logger.info()
            .message("=== WEBSOCKET MESSAGE DEBUG ===")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("fullUpdate", update.toString())
            .detail("approvalData", approvalData)
            .log();

        sendUpdate(executionId, update);

        logger.info()
            .message("Broadcasting approval required")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowNode")
            .objectId(nodeId)
            .detail("executionId", executionId.toString())
            .log();
    }

    /**
     * Broadcast simulation complete
     */
    public void broadcastSimulationComplete(UUID executionId, String status, Map<String, Object> summary) {
        Map<String, Object> update = new HashMap<>();
        update.put("event", "simulation_complete");
        update.put("executionId", executionId.toString());
        update.put("status", status);
        update.put("summary", summary);
        update.put("timestamp", System.currentTimeMillis());

        sendUpdate(executionId, update);

        logger.info()
            .message("Broadcasting simulation completion")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowExecution")
            .objectId(executionId.toString())
            .detail("status", status)
            .log();
    }

    /**
     * Send update to topic for given execution ID
     */
    private void sendUpdate(UUID executionId, Map<String, Object> update) {
        try {
            String destination = "/topic/simulation/" + executionId.toString();
            messagingTemplate.convertAndSend(destination, update);

            logger.trace()
                .message("Sent WebSocket update")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("destination", destination)
                .detail("event", update.get("event"))
                .log();
        } catch (Exception e) {
            logger.error()
                .message("Failed to broadcast simulation update")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("executionId", executionId.toString())
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Replay execution state when a client subscribes late.
     * Sends all completed node executions so the UI can catch up.
     */
    public void replayExecutionState(UUID executionId) {
        try {
            logger.info()
                .message("Replaying execution state for late subscriber")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("executionId", executionId.toString())
                .log();

            // Get the execution to check it exists
            WorkflowExecution execution = executionRepository.findById(executionId).orElse(null);
            if (execution == null) {
                logger.warn()
                    .message("Execution not found for replay")
                    .detail("executionId", executionId.toString())
                    .log();
                return;
            }

            // Get all node executions for this execution, ordered by start time
            List<WorkflowNodeExecution> nodeExecutions = nodeExecutionRepository.findByExecutionId(executionId);

            logger.info()
                .message("Found node executions to replay")
                .detail("executionId", executionId.toString())
                .detail("nodeCount", nodeExecutions.size())
                .log();

            // Send each node's state with small delays for UI animation
            for (int i = 0; i < nodeExecutions.size(); i++) {
                WorkflowNodeExecution nodeExec = nodeExecutions.get(i);

                // Add small delay between nodes for UI animation (except first node)
                if (i > 0) {
                    try {
                        Thread.sleep(150); // 150ms delay between nodes
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // Send node_start event
                Map<String, Object> startUpdate = new HashMap<>();
                startUpdate.put("event", "node_start");
                startUpdate.put("executionId", executionId.toString());
                startUpdate.put("nodeId", nodeExec.getNodeId());
                startUpdate.put("nodeName", nodeExec.getStepType()); // Using stepType as name
                startUpdate.put("nodeType", nodeExec.getStepType());
                startUpdate.put("timestamp", nodeExec.getStartedAt() != null ?
                    nodeExec.getStartedAt().toInstant().toEpochMilli() : System.currentTimeMillis());
                startUpdate.put("replay", true);
                sendUpdate(executionId, startUpdate);

                // Send completion/error event if node is not running
                WorkflowNodeExecution.NodeExecutionStatus status = nodeExec.getStatus();
                if (status != null && status != WorkflowNodeExecution.NodeExecutionStatus.RUNNING) {
                    Map<String, Object> completeUpdate = new HashMap<>();

                    if (status == WorkflowNodeExecution.NodeExecutionStatus.COMPLETED) {
                        completeUpdate.put("event", "node_complete");
                        completeUpdate.put("status", "completed");
                        completeUpdate.put("output", nodeExec.getOutputData());
                    } else if (status == WorkflowNodeExecution.NodeExecutionStatus.FAILED) {
                        completeUpdate.put("event", "node_error");
                        completeUpdate.put("status", "failed");
                        completeUpdate.put("error", nodeExec.getErrorDetails());
                    } else if (status == WorkflowNodeExecution.NodeExecutionStatus.PAUSED) {
                        // For paused approval nodes, send awaiting_approval event
                        completeUpdate.put("event", "node_awaiting_approval");
                        completeUpdate.put("status", "paused");

                        // Build approval data from node context
                        Map<String, Object> approvalData = new HashMap<>();
                        approvalData.put("reason", "Waiting for approval");
                        approvalData.put("approvalType", nodeExec.getStepType());

                        // Extract approverIds from output data (set during execution)
                        Map<String, Object> outputData = nodeExec.getOutputData();

                        logger.info()
                            .message("=== REPLAY APPROVAL DATA DEBUG ===")
                            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                            .detail("nodeId", nodeExec.getNodeId())
                            .detail("outputData", outputData != null ? outputData.toString() : "null")
                            .detail("hasApproverIds", outputData != null && outputData.containsKey("approverIds"))
                            .log();

                        if (outputData != null && outputData.containsKey("approverIds")) {
                            approvalData.put("approverIds", outputData.get("approverIds"));
                        } else {
                            // Fallback: try to get from input data
                            Map<String, Object> inputData = nodeExec.getInputData();
                            if (inputData != null && inputData.containsKey("approverIds")) {
                                approvalData.put("approverIds", inputData.get("approverIds"));
                            } else {
                                // Last fallback: empty list
                                approvalData.put("approverIds", List.of());
                            }
                        }

                        logger.info()
                            .message("Replay approval data prepared")
                            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                            .detail("approverIds", approvalData.get("approverIds"))
                            .log();

                        completeUpdate.put("approvalData", approvalData);
                    }

                    completeUpdate.put("executionId", executionId.toString());
                    completeUpdate.put("nodeId", nodeExec.getNodeId());
                    completeUpdate.put("nodeName", nodeExec.getStepType());
                    completeUpdate.put("nodeType", nodeExec.getStepType());
                    completeUpdate.put("duration", nodeExec.getDurationMs() != null ? nodeExec.getDurationMs() : 0);
                    completeUpdate.put("timestamp", nodeExec.getCompletedAt() != null ?
                        nodeExec.getCompletedAt().toInstant().toEpochMilli() : System.currentTimeMillis());
                    completeUpdate.put("replay", true);

                    sendUpdate(executionId, completeUpdate);
                }
            }

            logger.info()
                .message("Execution state replay completed")
                .detail("executionId", executionId.toString())
                .detail("nodesReplayed", nodeExecutions.size())
                .log();

        } catch (Exception e) {
            logger.error()
                .message("Failed to replay execution state")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("executionId", executionId.toString())
                .detail("error", e.getMessage())
                .log();
        }
    }
}