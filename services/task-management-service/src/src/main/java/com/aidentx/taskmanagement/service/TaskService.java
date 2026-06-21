package com.aidentx.taskmanagement.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aidentx.taskmanagement.dto.TaskCreationRequest;
import com.aidentx.taskmanagement.dto.TaskCreationResponse;
import com.aidentx.taskmanagement.dto.TaskExecutionResponse;
import com.aidentx.taskmanagement.dto.TaskResponse;
import com.aidentx.taskmanagement.dto.TaskUpdateRequest;

/**
 * Service interface for task management operations. Handles the business logic for creating,
 * scheduling, and managing the lifecycle of tasks.
 */
public interface TaskService {

  /**
   * Creates a new task.
   *
   * @param request The task creation request.
   * @param applicationId The ID of the application this task belongs to.
   * @param tenantId The tenant identifier.
   * @param userId The ID of the user creating the task.
   * @return A response containing the created task's ID.
   */
  TaskCreationResponse createTask(
      TaskCreationRequest request, UUID applicationId, String tenantId, UUID userId);

  /**
   * Retrieves a task by its ID and application ID, ensuring it belongs to the correct application.
   *
   * @param taskId The UUID of the task.
   * @param applicationId The UUID of the application.
   * @param tenantId The tenant identifier.
   * @return The complete task information.
   * @throws IllegalArgumentException if the task is not found or does not belong to the
   *     application.
   */
  TaskResponse getTaskByIdAndApplicationId(UUID taskId, UUID applicationId, String tenantId);

  /**
   * Retrieves a task by its ID, without application scope.
   *
   * @param taskId The UUID of the task.
   * @param tenantId The tenant identifier.
   * @return The complete task information.
   * @throws IllegalArgumentException if the task is not found.
   */
  TaskResponse getTaskById(UUID taskId, String tenantId);

  /**
   * Retrieves a paginated list of all tasks for a tenant.
   *
   * @param tenantId The tenant identifier.
   * @param pageable Pagination parameters.
   * @return A page of tasks.
   */
  Page<TaskResponse> getAllTasks(String tenantId, Pageable pageable);

  /**
   * Retrieves a paginated list of tasks for a specific application.
   *
   * @param applicationId The application UUID.
   * @param tenantId The tenant identifier.
   * @param pageable Pagination parameters.
   * @return A page of tasks for the application.
   */
  Page<TaskResponse> getTasksByApplicationId(
      UUID applicationId, String tenantId, Pageable pageable);

  /**
   * Updates an existing task.
   *
   * @param taskId The UUID of the task to update.
   * @param applicationId The UUID of the application the task must belong to.
   * @param request The update request with partial data.
   * @param tenantId The tenant identifier.
   * @param userId The ID of the user performing the update.
   * @return The updated task information.
   * @throws IllegalArgumentException if the task is not found or does not belong to the
   *     application.
   */
  TaskResponse updateTask(
      UUID taskId, UUID applicationId, TaskUpdateRequest request, String tenantId, UUID userId);

  /**
   * Deletes a task.
   *
   * @param taskId The UUID of the task to delete.
   * @param applicationId The UUID of the application the task must belong to.
   * @param tenantId The tenant identifier.
   * @throws IllegalArgumentException if the task is not found or does not belong to the
   *     application.
   */
  void deleteTask(UUID taskId, UUID applicationId, String tenantId);

  /**
   * Deletes all tasks belonging to an application (used during application cascade delete).
   *
   * @param applicationId The UUID of the application whose tasks should be deleted.
   * @param tenantId The tenant identifier.
   */
  void deleteAllTasksByApplicationId(UUID applicationId, String tenantId);

  /**
   * Triggers the immediate execution of a task.
   *
   * @param taskId The UUID of the task to execute.
   * @param tenantId The tenant identifier.
   * @return A response containing the execution ID for this run.
   */
  TaskExecutionResponse executeTask(UUID taskId, String tenantId);

  /**
   * Triggers an async dry-run preview of a task via the Quartz pipeline.
   * Creates a TaskRun(PENDING, isPreview=true) and triggers the job with dryRun=true.
   */
  TaskExecutionResponse executePreview(UUID taskId, String tenantId);

  /**
   * Pauses a scheduled task.
   *
   * @param taskId The UUID of the task to pause.
   * @param tenantId The tenant identifier.
   */
  void pauseTask(UUID taskId, String tenantId);

  /**
   * Resumes a paused task.
   *
   * @param taskId The UUID of the task to resume.
   * @param tenantId The tenant identifier.
   */
  void resumeTask(UUID taskId, String tenantId);

  /**
   * Terminates a scheduled or running task.
   *
   * @param taskId The UUID of the task to terminate.
   * @param tenantId The tenant identifier.
   */
  void terminateTask(UUID taskId, String tenantId);
}
