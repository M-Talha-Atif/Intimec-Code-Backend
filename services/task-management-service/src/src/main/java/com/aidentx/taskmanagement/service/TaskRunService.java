package com.aidentx.taskmanagement.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidentx.taskmanagement.config.TaskManagementConstants;
import com.aidentx.taskmanagement.dto.TaskRunResponse;
import com.aidentx.taskmanagement.model.entity.Task;
import com.aidentx.taskmanagement.model.entity.TaskRun;
import com.aidentx.taskmanagement.repository.TaskRepository;
import com.aidentx.taskmanagement.repository.TaskRunRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskRunService {

  private final TaskRunRepository taskRunRepository;
  private final TaskRepository taskRepository;

  /**
   * Retrieves a paginated history of all runs for a specific task.
   *
   * @param taskId The ID of the task.
   * @param tenantId The tenant identifier.
   * @param pageable The pagination information (page number, size, and sorting) provided by the
   *     client.
   * @return A Page of TaskRunResponse DTOs, containing the list of runs for the page and pagination
   *     details.
   */
  @Transactional(readOnly = true)
  public Page<TaskRunResponse> getTaskRunHistory(UUID taskId, String tenantId, Pageable pageable) {
    // The Pageable object is passed directly to the repository query.
    Page<TaskRun> runsPage =
        taskRunRepository.findByTaskIdAndTenantIdOrderByCreatedAtDesc(taskId, tenantId, pageable);

    // The .map() function of the Page object is used to convert the Page of
    // entities (TaskRun)
    // into a Page of DTOs (TaskRunResponse), preserving all pagination metadata.
    String category = taskRepository.findById(taskId).map(Task::getCategory).orElse(null);
    return runsPage.map(run -> TaskRunResponse.fromEntity(run, category));
  }

  /**
   * Retrieves a specific task run by its execution ID.
   *
   * @param executionId The unique ID of the execution run.
   * @param taskId The ID of the task this run belongs to, for validation.
   * @param tenantId The tenant identifier.
   * @return The details of the specified task run.
   * @throws IllegalArgumentException if the task run is not found.
   */
  @Transactional(readOnly = true)
  public TaskRunResponse getTaskRun(String executionId, UUID taskId, String tenantId) {
    TaskRun run =
        taskRunRepository
            .findByExecutionIdAndTaskIdAndTenantId(executionId, taskId, tenantId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        String.format(
                            TaskManagementConstants.ERR_TASK_RUN_NOT_FOUND, executionId)));
    String category = taskRepository.findById(taskId).map(Task::getCategory).orElse(null);
    return TaskRunResponse.fromEntity(run, category);
  }
}
