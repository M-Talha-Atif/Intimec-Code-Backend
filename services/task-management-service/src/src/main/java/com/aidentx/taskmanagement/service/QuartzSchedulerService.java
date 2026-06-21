package com.aidentx.taskmanagement.service;

import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.quartz.*;
import org.springframework.stereotype.Service;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.config.TaskManagementConstants;
import com.aidentx.taskmanagement.job.TaskExecutionJob;
import com.aidentx.taskmanagement.model.entity.Task;
import com.aidentx.taskmanagement.model.enums.ScheduleType;

/**
 * Service for scheduling tasks using Quartz Scheduler. Handles the creation, scheduling, and
 * management of jobs and triggers.
 */
@Service
public class QuartzSchedulerService {

  private final Scheduler scheduler;
  private final IUnifiedLogger log;

  public QuartzSchedulerService(Scheduler scheduler, UnifiedLoggerFactory unifiedLoggerFactory) {
    this.scheduler = scheduler;
    this.log = unifiedLoggerFactory.getLogger(QuartzSchedulerService.class);

    // Log scheduler initialization info
    try {
      log.info()
          .message(
              String.format(
                  "QuartzSchedulerService initialized. Scheduler: %s, Started: %s, InStandbyMode: %s",
                  scheduler.getSchedulerName(), scheduler.isStarted(), scheduler.isInStandbyMode()))
          .log();
    } catch (SchedulerException e) {
      log.error(e).message("Failed to get scheduler status during initialization").log();
    }
  }

  public void scheduleOneTimeTask(Task task) throws SchedulerException {
    log.info()
        .message(
            String.format(
                TaskManagementConstants.MSG_SCHEDULING_ONE_TIME_TASK,
                task.getId(),
                task.getNextExecution()))
        .log();

    JobKey jobKey = getJobKey(task.getId());
    TriggerKey triggerKey = getTriggerKey(task.getId());

    // Check if job already exists
    boolean jobExists = scheduler.checkExists(jobKey);

    JobDetail jobDetail = buildJobDetail(task, false); // Not durable for one-time tasks

    // For one-time tasks, use SimpleTrigger with no repeat (repeat count = 0)
    // This ensures the trigger fires exactly once at the specified time
    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .forJob(jobDetail)
            .startAt(Date.from(task.getNextExecution().atZone(ZoneId.systemDefault()).toInstant()))
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withRepeatCount(0) // Fire only once
                    .withMisfireHandlingInstructionFireNow())
            .build();

    if (jobExists) {
      // Job exists - reschedule
      if (scheduler.checkExists(triggerKey)) {
        scheduler.unscheduleJob(triggerKey);
      }
      scheduler.addJob(jobDetail, true);
      scheduler.scheduleJob(trigger);
      log.info().message(String.format("Rescheduled one-time task %s", task.getId())).log();
    } else {
      // New job
      scheduler.scheduleJob(jobDetail, trigger);
      log.info().message(String.format("Scheduled new one-time task %s", task.getId())).log();
    }
  }

  public void scheduleRecurringTask(Task task) throws SchedulerException {
    log.info()
        .message(String.format(TaskManagementConstants.MSG_SCHEDULING_RECURRING_TASK, task.getId()))
        .log();

    JobKey jobKey = getJobKey(task.getId());
    TriggerKey triggerKey = getTriggerKey(task.getId());

    // Check if job already exists - if so, reschedule with new trigger
    boolean jobExists = scheduler.checkExists(jobKey);

    JobDetail jobDetail = buildJobDetail(task, true); // Durable for recurring tasks

    ScheduleBuilder<?> scheduleBuilder;
    if (task.getScheduleType() == ScheduleType.CRON) {
      scheduleBuilder =
          CronScheduleBuilder.cronSchedule(task.getCronExpression())
              .withMisfireHandlingInstructionFireAndProceed();
    } else {
      // INTERVAL type
      scheduleBuilder =
          SimpleScheduleBuilder.simpleSchedule()
              .withIntervalInSeconds(task.getIntervalSeconds())
              .repeatForever()
              .withMisfireHandlingInstructionNextWithExistingCount();
    }

    Date startTime =
        (task.getNextExecution() != null)
            ? Date.from(task.getNextExecution().atZone(ZoneId.systemDefault()).toInstant())
            : new Date();

    log.info()
        .message(
            String.format(
                "Recurring task %s: scheduleType=%s, intervalSeconds=%s, cronExpression=%s, startTime=%s, nextExecution=%s, jobExists=%s",
                task.getId(),
                task.getScheduleType(),
                task.getIntervalSeconds(),
                task.getCronExpression(),
                startTime,
                task.getNextExecution(),
                jobExists))
        .log();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .forJob(jobDetail)
            .startAt(startTime)
            .withSchedule(scheduleBuilder)
            .build();

    if (jobExists) {
      // Job exists - need to reschedule with new trigger
      // First, delete old trigger if it exists
      if (scheduler.checkExists(triggerKey)) {
        scheduler.unscheduleJob(triggerKey);
      }
      // Update job details
      scheduler.addJob(jobDetail, true);
      // Schedule new trigger
      scheduler.scheduleJob(trigger);
      log.info()
          .message(String.format("Rescheduled recurring task %s with new trigger", task.getId()))
          .log();
    } else {
      // New job - schedule job and trigger together
      scheduler.scheduleJob(jobDetail, trigger);
      log.info().message(String.format("Scheduled new recurring task %s", task.getId())).log();
    }
  }

  public void scheduleManualTask(Task task) throws SchedulerException {
    log.info()
        .message(String.format(TaskManagementConstants.MSG_SCHEDULING_MANUAL_TASK, task.getId()))
        .log();
    JobDetail jobDetail = buildJobDetail(task, true); // Durable so it can be triggered on demand
    scheduler.addJob(jobDetail, true); // Add the job without a trigger
  }

  public void cancelTask(UUID taskId) throws SchedulerException {
    scheduler.deleteJob(getJobKey(taskId));
  }

  public void pauseTask(UUID taskId) throws SchedulerException {
    scheduler.pauseJob(getJobKey(taskId));
  }

  public void resumeTask(UUID taskId) throws SchedulerException {
    scheduler.resumeJob(getJobKey(taskId));
  }

  public void triggerTask(UUID taskId, String executionId) throws SchedulerException {
    triggerTask(taskId, executionId, false);
  }

  public void triggerTask(UUID taskId, String executionId, boolean dryRun) throws SchedulerException {
    JobDataMap dataMap = new JobDataMap();
    dataMap.put("executionId", executionId);
    dataMap.put("dryRun", String.valueOf(dryRun));

    // Debug logging
    log.info()
        .message(
            String.format(
                "Triggering job: taskId=%s, executionId=%s, dryRun=%s, schedulerStarted=%s, jobExists=%s",
                taskId,
                executionId,
                dryRun,
                scheduler.isStarted(),
                scheduler.checkExists(getJobKey(taskId))))
        .log();

    scheduler.triggerJob(getJobKey(taskId), dataMap);

    log.info()
        .message(
            String.format(
                "Job triggered successfully: taskId=%s, executionId=%s, dryRun=%s", taskId, executionId, dryRun))
        .log();
  }

  public boolean isTaskScheduled(UUID taskId) throws SchedulerException {
    return scheduler.checkExists(getJobKey(taskId));
  }

  /** Private helper to build a JobDetail object, reducing code duplication. */
  private JobDetail buildJobDetail(Task task, boolean isDurable) {
    return JobBuilder.newJob(TaskExecutionJob.class)
        .withIdentity(getJobKey(task.getId()))
        .withDescription(task.getName())
        .usingJobData("taskId", task.getId().toString())
        .usingJobData("tenantId", task.getTenantId())
        .usingJobData("applicationId", task.getApplicationId().toString())
        .storeDurably(isDurable)
        .build();
  }

  /**
   * Get the next scheduled execution time for a task from Quartz.
   *
   * @param taskId The task ID
   * @return The next fire time as a Date, or null if not scheduled or no next fire time
   */
  public Date getNextFireTime(UUID taskId) {
    try {
      TriggerKey triggerKey = getTriggerKey(taskId);
      Trigger trigger = scheduler.getTrigger(triggerKey);
      if (trigger != null) {
        return trigger.getNextFireTime();
      }
    } catch (SchedulerException e) {
      log.warn()
          .message(
              String.format("Failed to get next fire time for task %s: %s", taskId, e.getMessage()))
          .log();
    }
    return null;
  }

  private JobKey getJobKey(UUID taskId) {
    return JobKey.jobKey(taskId.toString(), "tasks");
  }

  private TriggerKey getTriggerKey(UUID taskId) {
    return TriggerKey.triggerKey(taskId.toString() + "-trigger", "tasks");
  }
}
