package com.aidentx.workflowsservice.event;

import com.aidentx.workflowsservice.dto.request.ExecuteWorkflowRequest;
import com.aidentx.workflowsservice.entity.ProcessedEvent;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.repository.ProcessedEventRepository;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import com.aidentx.workflowsservice.service.WorkflowExecutionService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer that listens for events from various services
 * and triggers workflows based on event configuration.
 * 
 * Implements event-level deduplication to prevent duplicate processing
 * when Kafka replays messages due to consumer rebalancing or service restarts.
 */
@Service
public class WorkflowEventConsumer {

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionService workflowExecutionService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;
    
    public WorkflowEventConsumer(
            WorkflowRepository workflowRepository,
            WorkflowExecutionService workflowExecutionService,
            ProcessedEventRepository processedEventRepository,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.workflowRepository = workflowRepository;
        this.workflowExecutionService = workflowExecutionService;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(WorkflowEventConsumer.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Listen to identity service events
     */
    @KafkaListener(
        topics = "identity-events",
        groupId = "workflow-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleIdentityEvent(String message) {
        processEvent(message, "identity-service");
    }

    /**
     * Listen to role service events
     */
    @KafkaListener(
        topics = "role-events",
        groupId = "workflow-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRoleEvent(String message) {
        processEvent(message, "role-service");
    }

    /**
     * Listen to access service events
     */
    @KafkaListener(
        topics = "access-events",
        groupId = "workflow-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAccessEvent(String message) {
        processEvent(message, "access-service");
    }

    /**
     * Listen to application service events
     */
    @KafkaListener(
        topics = "application-events",
        groupId = "workflow-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleApplicationEvent(String message) {
        processEvent(message, "application-service");
    }

    /**
     * Listen to certification service events
     */
    @KafkaListener(
        topics = "certification-events",
        groupId = "workflow-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCertificationEvent(String message) {
        processEvent(message, "certification-service");
    }

    /**
     * Listen to policy service events
     */
    @KafkaListener(
        topics = "policy-events",
        groupId = "workflow-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePolicyEvent(String message) {
        processEvent(message, "policy-service");
    }

    /**
     * Process incoming event and trigger matching workflows.
     * Implements event-level deduplication to prevent duplicate processing.
     */
    private void processEvent(String message, String sourceService) {
        try {
            // Parse event
            WorkflowEvent event = objectMapper.readValue(message, WorkflowEvent.class);
            event.setSourceService(sourceService);
            
            logger.info()
                .message(LOG_RECEIVED_WORKFLOW_EVENT)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType("WorkflowEvent")
                .detail("eventType", event.getEventType())
                .detail("sourceService", sourceService)
                .detail("eventId", event.getEventId())
                .log();
            
            // EVENT-LEVEL DEDUPLICATION: Check if this event has already been processed
            // This is the FIRST LINE OF DEFENSE against Kafka message replays
            if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
                logger.info()
                    .message(LOG_EVENT_ALREADY_PROCESSED_SKIPPING)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType("WorkflowEvent")
                    .detail("eventId", event.getEventId())
                    .detail("eventType", event.getEventType())
                    .log();
                return; // Skip entire processing - saves DB queries, workflow lookups, execution attempts
            }
            
            // Debug: Log payload keys to diagnose workflowId filtering issue
            if (event.getPayload() != null) {
                logger.trace()
                    .message(LOG_EVENT_PAYLOAD_KEYS)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("payloadKeys", event.getPayload().keySet().toString())
                    .log();
                if (event.getPayload().containsKey("workflowId")) {
                    Object workflowIdObj = event.getPayload().get("workflowId");
                    logger.trace()
                        .message(LOG_EVENT_CONTAINS_WORKFLOW_ID)
                        .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                        .detail("workflowId", workflowIdObj != null ? workflowIdObj.toString() : "null")
                        .log();
                }
            } else {
                logger.warn()
                    .message(LOG_EVENT_PAYLOAD_IS_NULL)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType("WorkflowEvent")
                    .detail("eventType", event.getEventType())
                    .log();
            }

            // LOOP GUARD: If an event was emitted as a side-effect of a running workflow execution,
            // do not trigger workflows again from it (prevents infinite loops with new eventIds).
            // Producers should set metadata.workflowExecutionId when receiving HEADER_WORKFLOW_EXECUTION_ID.
            if (event.getMetadata() != null) {
                Object workflowExecutionId = event.getMetadata().get(METADATA_KEY_WORKFLOW_EXECUTION_ID);
                if (workflowExecutionId != null && !workflowExecutionId.toString().trim().isEmpty()) {
                    logger.info()
                        .message("Skipping workflow trigger for workflow-origin event")
                        .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                        .objectType("WorkflowEvent")
                        .detail("eventId", event.getEventId())
                        .detail("eventType", event.getEventType())
                        .detail(METADATA_KEY_WORKFLOW_EXECUTION_ID, workflowExecutionId.toString())
                        .log();

                    if (event.getEventId() != null) {
                        markEventAsProcessed(event, 0);
                    }
                    return;
                }
            }

            // Find workflows subscribed to this event type
            List<Workflow> matchingWorkflows = findWorkflowsForEvent(event);
            
            if (matchingWorkflows.isEmpty()) {
                logger.debug()
                    .message(LOG_NO_WORKFLOWS_SUBSCRIBED_TO_EVENT)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType("WorkflowEvent")
                    .detail("eventType", event.getEventType())
                    .log();
                
                // Even if no workflows match, mark event as processed to avoid reprocessing
                if (event.getEventId() != null) {
                    markEventAsProcessed(event, 0);
                }
                return;
            }

            logger.info()
                .message(LOG_FOUND_WORKFLOWS_TO_EXECUTE_FOR_EVENT)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType("WorkflowEvent")
                .detail("workflowCount", matchingWorkflows.size())
                .detail("eventType", event.getEventType())
                .log();

            // Execute each matching workflow
            for (Workflow workflow : matchingWorkflows) {
                executeWorkflowForEvent(workflow, event);
            }
            
            // Mark event as successfully processed AFTER all workflows have been triggered
            // This ensures we only mark as processed if everything succeeded
            if (event.getEventId() != null) {
                markEventAsProcessed(event, matchingWorkflows.size());
            }

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_TO_PROCESS_EVENT)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("sourceService", sourceService)
                .detail("error", e.getMessage())
                .detail("errorType", e.getClass().getSimpleName())
                .log();
            // NOTE: We do NOT mark as processed on error - the event will be retried
            // The workflow-level idempotency (workflowId-eventId) will prevent duplicates
            // if the event was partially processed before the error
        }
    }
    
    /**
     * Mark an event as processed to prevent duplicate processing.
     * Called after successfully triggering all workflows for an event.
     */
    private void markEventAsProcessed(WorkflowEvent event, int workflowCount) {
        try {
            ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .sourceService(event.getSourceService())
                .tenantId(event.getTenantId())
                .correlationId(event.getCorrelationId())
                .workflowCount(workflowCount)
                .processedAt(OffsetDateTime.now())
                .build();
            
            processedEventRepository.save(processedEvent);
            
            logger.debug()
                .message(LOG_MARKED_EVENT_AS_PROCESSED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType("ProcessedEvent")
                .objectId(event.getEventId().toString())
                .detail("workflowCount", workflowCount)
                .log();
                
        } catch (Exception e) {
            // Log but don't fail - the workflow-level idempotency will prevent duplicates
            logger.error()
                .message(LOG_FAILED_TO_MARK_EVENT_AS_PROCESSED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("eventId", event.getEventId())
                .detail("error", e.getMessage())
                .log();
        }
    }

    /**
     * Find all active workflows subscribed to the given event
     * If event payload contains workflowId, only return that specific workflow
     */
    @SuppressWarnings("unchecked")
    private List<Workflow> findWorkflowsForEvent(WorkflowEvent event) {
        // Check if event payload specifies a specific workflow to execute
        if (event.getPayload() != null && event.getPayload().containsKey("workflowId")) {
            String workflowId = (String) event.getPayload().get("workflowId");
            if (workflowId != null && !workflowId.isEmpty()) {
                logger.info()
                    .message(LOG_EVENT_SPECIFIES_WORKFLOW_ID_EXECUTING_ONLY_THAT)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType("Workflow")
                    .objectId(workflowId)
                    .log();
                try {
                    UUID workflowUuid = UUID.fromString(workflowId);
                    Workflow workflow = workflowRepository.findById(workflowUuid).orElse(null);
                    
                    // Verify workflow is active and matches event type
                    if (workflow != null && 
                        workflow.getStatus() == Workflow.WorkflowStatus.ACTIVE &&
                        workflow.getTriggerType() == Workflow.TriggerType.EVENT_BASED) {
                        
                        Map<String, Object> eventConfig = workflow.getEventConfig();
                        if (eventConfig != null) {
                            List<String> configuredEvents = (List<String>) eventConfig.get("eventTypes");
                            if (configuredEvents != null && configuredEvents.contains(event.getEventType())) {
                                return List.of(workflow);
                            }
                        }
                    }
                    logger.warn()
                        .message(LOG_SPECIFIED_WORKFLOW_ID_NOT_FOUND)
                        .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                        .objectType("Workflow")
                        .objectId(workflowId)
                        .detail("eventType", event.getEventType())
                        .log();
                } catch (IllegalArgumentException e) {
                    logger.error()
                        .message(ERROR_INVALID_UUID)
                        .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                        .detail("workflowId", workflowId)
                        .log();
                }
            }
        }
        
        // Fallback: Find all active EVENT_BASED workflows subscribed to this event type
        List<Workflow> eventBasedWorkflows = workflowRepository.findByTriggerTypeAndStatus(
            Workflow.TriggerType.EVENT_BASED,
            Workflow.WorkflowStatus.ACTIVE
        );

        // Filter workflows that are subscribed to this specific event type
        return eventBasedWorkflows.stream()
            .filter(workflow -> {
                Map<String, Object> eventConfig = workflow.getEventConfig();
                if (eventConfig == null) {
                    return false;
                }

                // Get configured event types
                List<String> configuredEvents = (List<String>) eventConfig.get("eventTypes");
                if (configuredEvents == null || configuredEvents.isEmpty()) {
                    return false;
                }

                // Check if workflow is subscribed to this event type
                return configuredEvents.contains(event.getEventType());
            })
            .toList();
    }

    /**
     * Execute a workflow in response to an event
     */
    private void executeWorkflowForEvent(Workflow workflow, WorkflowEvent event) {
        try {
            logger.info()
                .message(LOG_EXECUTING_WORKFLOW)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(workflow.getId().toString())
                .detail("workflowName", workflow.getName())
                .detail("eventType", event.getEventType())
                .log();

            // Prepare execution context with event data
            Map<String, Object> executionContext = new HashMap<>();
            executionContext.put(CONTEXT_KEY_TRIGGER, DEFAULT_TRIGGER_TYPE_EVENT);
            executionContext.put(CONTEXT_KEY_EVENT_TYPE, event.getEventType());
            executionContext.put(CONTEXT_KEY_EVENT_ID, event.getEventId());
            executionContext.put(CONTEXT_KEY_SOURCE_SERVICE, event.getSourceService());
            executionContext.put(CONTEXT_KEY_TENANT_ID, event.getTenantId());
            executionContext.put(CONTEXT_KEY_TIMESTAMP, event.getTimestamp());
            executionContext.put(CONTEXT_KEY_CORRELATION_ID, event.getCorrelationId());
            
            // Merge event payload into context
            if (event.getPayload() != null) {
                executionContext.putAll(event.getPayload());
            }

            // Create execution request with idempotency key to prevent duplicate executions
            ExecuteWorkflowRequest request = new ExecuteWorkflowRequest();
            request.setContext(executionContext);
            request.setDryRun(false);
            request.setTriggerType("EVENT_BASED"); // Set trigger type for event-triggered workflows
            // Set idempotency key as: workflowId + eventId to prevent duplicate executions when Kafka replays messages
            request.setIdempotencyKey(workflow.getId() + "-" + event.getEventId());

            // Extract triggeredBy from event payload (createdById, userId, etc.)
            UUID triggeredBy = extractUserFromEvent(event);
            
            workflowExecutionService.executeWorkflow(workflow.getId(), request, triggeredBy);

            logger.info()
                .message(SUCCESS_WORKFLOW_EXECUTION_STARTED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(workflow.getId().toString())
                .detail("eventId", event.getEventId())
                .log();

        } catch (Exception e) {
            logger.error()
                .message(ERROR_WORKFLOW_EXECUTION_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(workflow.getId().toString())
                .detail("eventId", event.getEventId())
                .detail("error", e.getMessage())
                .detail("errorType", e.getClass().getSimpleName())
                .log();
            // TODO: Record failed execution in audit log
        }
    }
    
    /**
     * Extract the user UUID from event payload.
     * Tries multiple common field names: createdById, userId, requestedBy, etc.
     * Throws exception if no user found - all workflows must be triggered by a real user.
     */
    private UUID extractUserFromEvent(WorkflowEvent event) {
        if (event.getPayload() == null) {
            logger.error()
                .message(LOG_EVENT_HAS_NO_PAYLOAD_CANNOT_EXTRACT_USER)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType("WorkflowEvent")
                .detail("eventType", event.getEventType())
                .log();
            throw new IllegalStateException("Workflow event must have a payload with user information");
        }
        
        Map<String, Object> payload = event.getPayload();
        
        // DEBUG: Log the entire payload to diagnose the issue
        logger.info()
            .message("DEBUG: Extracting user from event payload")
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("payloadKeys", payload.keySet().toString())
            .detail("payloadSize", payload.size())
            .log();
        
        // Try common field names for user identification
        String[] userFields = {"createdById", "userId", "requestedBy", "triggeredBy", "actorId", "performedBy"};
        
        for (String field : userFields) {
            Object value = payload.get(field);
            logger.info()
                .message("DEBUG: Checking field for user ID")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("fieldName", field)
                .detail("valueIsNull", value == null)
                .detail("valueType", value != null ? value.getClass().getName() : "null")
                .detail("valueString", value != null ? value.toString() : "null")
                .log();
                
            if (value != null) {
                try {
                    UUID userId = value instanceof UUID ? (UUID) value : UUID.fromString(value.toString());
                    logger.info()
                        .message(LOG_EXTRACTED_USER_FROM_EVENT_PAYLOAD)
                        .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                        .detail("fieldName", field)
                        .detail("userId", userId.toString())
                        .log();
                    return userId;
                } catch (IllegalArgumentException e) {
                    logger.warn()
                        .message(ERROR_INVALID_UUID)
                        .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                        .detail("fieldName", field)
                        .detail("value", value.toString())
                        .detail("error", e.getMessage())
                        .log();
                }
            }
        }
        
        // No fallback - throw exception to enforce user requirement
        logger.error()
            .message(LOG_NO_VALID_USER_FOUND_IN_EVENT_PAYLOAD)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType("WorkflowEvent")
            .detail("triedFields", String.join(", ", userFields))
            .detail("actualKeys", payload.keySet().toString())
            .log();
        throw new IllegalStateException("Workflow event must contain a valid user ID in one of these fields: " + String.join(", ", userFields));
    }
}
