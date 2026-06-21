package com.aidentx.workflowsservice.scheduler;

import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for managing scheduled workflow jobs using Quartz
 */
import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

@Service
public class WorkflowSchedulerService {

    private final IUnifiedLogger logger;
    private final Scheduler scheduler;

    public WorkflowSchedulerService(Scheduler scheduler, UnifiedLoggerFactory loggerFactory) {
        this.scheduler = scheduler;
        this.logger = loggerFactory.getLogger(WorkflowSchedulerService.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Schedule a time-based workflow
     */
    public void scheduleWorkflow(Workflow workflow) {
        if (workflow.getTriggerType() != Workflow.TriggerType.TIME_BASED) {
            logger.warn()
                .message(LOG_CANNOT_SCHEDULE_NON_TIME_BASED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("workflowId", workflow.getId().toString())
                .log();
            return;
        }

        if (workflow.getCronSchedule() == null || workflow.getCronSchedule().isBlank()) {
            logger.warn()
                .message(LOG_CANNOT_SCHEDULE_NO_CRON)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("workflowId", workflow.getId().toString())
                .log();
            return;
        }

        try {
            JobKey jobKey = createJobKey(workflow.getId());
            TriggerKey triggerKey = createTriggerKey(workflow.getId());

            // Check if job already exists and delete it
            if (scheduler.checkExists(jobKey)) {
                logger.info()
                    .message(LOG_REMOVING_EXISTING_SCHEDULED_JOB)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType(OBJECT_TYPE_WORKFLOW)
                    .objectId(workflow.getId().toString())
                    .log();
                scheduler.deleteJob(jobKey);
            }

            // Build job detail
            JobDetail jobDetail = JobBuilder.newJob(ScheduledWorkflowJob.class)
                .withIdentity(jobKey)
                .withDescription("Scheduled execution for workflow: " + workflow.getName())
                .usingJobData("workflowId", workflow.getId().toString())
                .storeDurably(false)
                .build();

            // Build cron trigger
            CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withDescription("Cron trigger for workflow: " + workflow.getName())
                .withSchedule(CronScheduleBuilder.cronSchedule(workflow.getCronSchedule()))
                .build();

            // Schedule the job
            scheduler.scheduleJob(jobDetail, trigger);

            logger.info()
                .message(LOG_SUCCESSFULLY_SCHEDULED_WORKFLOW)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(workflow.getId().toString())
                .detail("cronSchedule", workflow.getCronSchedule())
                .log();

        } catch (SchedulerException e) {
            logger.error()
                .message(LOG_FAILED_SCHEDULE_WORKFLOW)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("workflowId", workflow.getId().toString())
                .detail("error", e.getMessage())
                .log();
            throw new RuntimeException("Failed to schedule workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Unschedule a workflow
     */
    public void unscheduleWorkflow(UUID workflowId) {
        try {
            JobKey jobKey = createJobKey(workflowId);
            
            if (scheduler.checkExists(jobKey)) {
                boolean deleted = scheduler.deleteJob(jobKey);
                if (deleted) {
                    logger.info()
                        .message(LOG_WORKFLOW_UNSCHEDULED)
                        .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                        .objectType(OBJECT_TYPE_WORKFLOW)
                        .objectId(workflowId.toString())
                        .log();
                } else {
                    logger.warn()
                        .message(LOG_FAILED_UNSCHEDULE_WORKFLOW_INNER)
                        .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                        .objectType(OBJECT_TYPE_WORKFLOW)
                        .objectId(workflowId.toString())
                        .log();
                }
            } else {
                logger.trace()
                    .message(LOG_NO_SCHEDULED_JOB_FOUND_FOR_WORKFLOW)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .objectType(OBJECT_TYPE_WORKFLOW)
                    .objectId(workflowId.toString())
                    .log();
            }
        } catch (SchedulerException e) {
            logger.error()
                .message(LOG_FAILED_UNSCHEDULE_WORKFLOW_INNER)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(workflowId.toString())
                .detail("error", e.getMessage())
                .log();
            throw new RuntimeException("Failed to unschedule workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Reschedule a workflow (when cron expression changes)
     */
    public void rescheduleWorkflow(Workflow workflow) {
        logger.info()
            .message(LOG_RESCHEDULING_WORKFLOW)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .objectType(OBJECT_TYPE_WORKFLOW)
            .objectId(workflow.getId().toString())
            .log();
        unscheduleWorkflow(workflow.getId());
        scheduleWorkflow(workflow);
    }

    /**
     * Check if a workflow is scheduled
     */
    public boolean isScheduled(UUID workflowId) {
        try {
            JobKey jobKey = createJobKey(workflowId);
            return scheduler.checkExists(jobKey);
        } catch (SchedulerException e) {
            logger.error()
                .message(LOG_FAILED_CHECK_IF_WORKFLOW_SCHEDULED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .objectType(OBJECT_TYPE_WORKFLOW)
                .objectId(workflowId.toString())
                .detail("error", e.getMessage())
                .log();
            return false;
        }
    }

    /**
     * Validate cron expression
     */
    public boolean isValidCronExpression(String cronExpression) {
        try {
            CronScheduleBuilder.cronSchedule(cronExpression);
            return true;
        } catch (Exception e) {
            logger.warn()
                .message(LOG_INVALID_CRON_EXPRESSION)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("cronExpression", cronExpression)
                .detail("error", e.getMessage())
                .log();
            return false;
        }
    }

    private JobKey createJobKey(UUID workflowId) {
        return JobKey.jobKey("workflow-" + workflowId, "workflows");
    }

    private TriggerKey createTriggerKey(UUID workflowId) {
        return TriggerKey.triggerKey("workflow-trigger-" + workflowId, "workflows");
    }
}
