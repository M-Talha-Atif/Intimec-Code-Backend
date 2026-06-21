package com.aidentx.taskmanagement.config;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.model.entity.TaskRun;
import com.aidentx.taskmanagement.model.enums.ExecutionStatus;
import com.aidentx.taskmanagement.repository.TaskRepository;
import com.aidentx.taskmanagement.repository.TaskRunRepository;

/**
 * On every service startup, scans for TaskRun records that were left in a non-terminal
 * state (RUNNING or SUSPENDED) by a previous JVM crash or Docker restart. Those runs
 * never had a chance to call handleFailure(), so they would otherwise stay "running"
 * forever in the database and cause the frontend to show Pause/Stop buttons for a
 * task that is no longer executing.
 *
 * <p>Recovery logic:
 * <ul>
 *   <li>Mark orphaned TaskRuns as FAILED with an explanatory message.</li>
 *   <li>Mark the parent Task entity as FAILED so the Task-level status is also correct.</li>
 * </ul>
 *
 * <p>This is safe to run on every startup because only non-terminal statuses are touched.
 * Runs that already reached COMPLETED/FAILED/SKIPPED are left untouched.
 */
@Component
@Order(1) // Run before OpenSearchRetentionConfig (order 2 by default)
public class StartupRecoveryRunner implements ApplicationRunner {

    private static final String INTERRUPTED_MESSAGE =
            "Execution interrupted — service restarted before task could finish.";

    private final TaskRunRepository taskRunRepository;
    private final TaskRepository taskRepository;
    private final IUnifiedLogger log;

    public StartupRecoveryRunner(
            TaskRunRepository taskRunRepository,
            TaskRepository taskRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.taskRunRepository = taskRunRepository;
        this.taskRepository = taskRepository;
        this.log = loggerFactory.getLogger(StartupRecoveryRunner.class);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            recoverOrphanedRuns();
        } catch (Exception e) {
            // Recovery failure must never block startup.
            log.warn()
                    .message(
                            "Startup recovery of orphaned task runs failed — some tasks may still "
                                    + "show as RUNNING in the UI. Error: "
                                    + e.getMessage())
                    .log();
        }
    }

    private void recoverOrphanedRuns() {
        List<TaskRun> orphaned =
                taskRunRepository.findByStatusIn(
                        List.of(ExecutionStatus.RUNNING, ExecutionStatus.SUSPENDED));

        if (orphaned.isEmpty()) {
            log.info().message("Startup recovery: no orphaned task runs found.").log();
            return;
        }

        log.warn()
                .message(
                        String.format(
                                "Startup recovery: found %d orphaned task run(s) in RUNNING/SUSPENDED "
                                        + "state. Marking as FAILED due to service restart.",
                                orphaned.size()))
                .log();

        for (TaskRun run : orphaned) {
            log.warn()
                    .message(
                            String.format(
                                    "Startup recovery: marking run executionId=%s taskId=%s as FAILED.",
                                    run.getExecutionId(), run.getTaskId()))
                    .log();

            run.setStatus(ExecutionStatus.FAILED);
            run.setErrorMessage(INTERRUPTED_MESSAGE);
            run.setEndTime(Instant.now());
            taskRunRepository.save(run);

            // Also fix the Task entity so the task-level status is consistent.
            taskRepository
                    .findById(run.getTaskId())
                    .ifPresent(
                            task -> {
                                // Only override if the task entity itself is in a non-terminal state.
                                if (task.getStatus() == ExecutionStatus.RUNNING
                                        || task.getStatus() == ExecutionStatus.SUSPENDED) {
                                    task.setStatus(ExecutionStatus.FAILED);
                                    task.setLastRunFinishTimestamp(Instant.now());
                                    task.setLastExecution(Instant.now());
                                    taskRepository.save(task);
                                }
                            });
        }
    }
}
