package com.aidentx.workflowsservice.scheduler;

import com.aidentx.workflowsservice.dto.request.ExecuteWorkflowRequest;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import com.aidentx.workflowsservice.service.WorkflowExecutionService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Quartz job for executing time-based workflows
 */
@Component
public class ScheduledWorkflowJob implements Job {

    private final IUnifiedLogger logger;
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionService workflowExecutionService;

    public ScheduledWorkflowJob(
            WorkflowRepository workflowRepository,
            WorkflowExecutionService workflowExecutionService,
            UnifiedLoggerFactory loggerFactory) {
        this.workflowRepository = workflowRepository;
        this.workflowExecutionService = workflowExecutionService;
        this.logger = loggerFactory.getLogger(ScheduledWorkflowJob.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        UUID workflowId = UUID.fromString(dataMap.getString("workflowId"));

        logger.info()
            .message(LOG_EXECUTING_SCHEDULED_WORKFLOW)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(workflowId.toString())
            .log();

        try {
            // Fetch workflow from database
            Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new JobExecutionException("Workflow not found: " + workflowId));

            // Verify workflow is still active and time-based
            if (workflow.getStatus() != Workflow.WorkflowStatus.ACTIVE) {
                logger.warn()
                    .message(LOG_WORKFLOW_NOT_ACTIVE_SKIPPING)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("workflowId", workflowId.toString())
                    .detail("status", workflow.getStatus().toString())
                    .log();
                return;
            }

            if (workflow.getTriggerType() != Workflow.TriggerType.TIME_BASED) {
                logger.warn()
                    .message(LOG_WORKFLOW_NOT_TIME_BASED_SKIPPING)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("workflowId", workflowId.toString())
                    .detail("triggerType", workflow.getTriggerType().toString())
                    .log();
                return;
            }

            // Build execution context
            Map<String, Object> executionContext = new HashMap<>();
            executionContext.put(CONTEXT_KEY_TRIGGER_TYPE, DEFAULT_TRIGGER_TYPE_TIME);
            executionContext.put(CONTEXT_KEY_SCHEDULED_TIME, OffsetDateTime.now());
            executionContext.put(CONTEXT_KEY_CRON_EXPRESSION, workflow.getCronSchedule());
            executionContext.put(CONTEXT_KEY_NEXT_FIRE_TIME, context.getNextFireTime());

            // Create execution request
            ExecuteWorkflowRequest request = new ExecuteWorkflowRequest();
            request.setContext(executionContext);
            request.setDryRun(false);

            // Execute workflow - use system UUID for time-triggered workflows
            UUID systemUser = UUID.fromString("00000000-0000-0000-0000-000000000000");
            workflowExecutionService.executeWorkflow(workflow.getId(), request, systemUser);

            logger.info()
                .message(LOG_SUCCESSFULLY_EXECUTED_SCHEDULED_WORKFLOW)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(workflowId.toString())
                .log();

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_EXECUTE_SCHEDULED_WORKFLOW)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("workflowId", workflowId.toString())
                .detail("error", e.getMessage())
                .detail("errorType", e.getClass().getSimpleName())
                .log();
            throw new JobExecutionException("Workflow execution failed: " + e.getMessage(), e);
        }
    }
}
