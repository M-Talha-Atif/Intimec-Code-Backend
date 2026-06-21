package com.aidentx.taskmanagement.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aidentx.taskmanagement.model.enums.ExecutionStatus;

import com.aidentx.taskmanagement.model.entity.TaskRun;

/** Repository interface for TaskRun entity. Provides methods to query task execution history. */
@Repository
public interface TaskRunRepository extends JpaRepository<TaskRun, String> {

  /** Find all runs for a specific task. */
  Page<TaskRun> findByTaskIdAndTenantIdOrderByCreatedAtDesc(
      UUID taskId, String tenantId, Pageable pageable);

  /** Find a specific run by execution ID, task ID, and tenant ID. */
  Optional<TaskRun> findByExecutionIdAndTaskIdAndTenantId(
      String executionId, UUID taskId, String tenantId);

  /** Find a run by execution ID and tenant ID. */
  Optional<TaskRun> findByExecutionIdAndTenantId(String executionId, String tenantId);

  /** Find all runs for a tenant. */
  Page<TaskRun> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

  /**
   * Find the most recent run for a task — used to update status on pause/resume/cancel
   * without requiring the caller to know the executionId.
   */
  Optional<TaskRun> findTopByTaskIdAndTenantIdOrderByCreatedAtDesc(UUID taskId, String tenantId);

  /**
   * Find all TaskRun records whose status is in the given set.
   * Used by StartupRecoveryRunner to find orphaned RUNNING/SUSPENDED runs after a crash.
   */
  List<TaskRun> findByStatusIn(List<ExecutionStatus> statuses);

  /** Find the most recent completed (non-preview) run for a task — used to estimate progress. */
  Optional<TaskRun> findTopByTaskIdAndTenantIdAndStatusAndPreviewFalseOrderByEndTimeDesc(
      UUID taskId, String tenantId, ExecutionStatus status);

  /** Delete all runs belonging to an application — used during cascade application deletion. */
  @Modifying
  @Query("DELETE FROM TaskRun r WHERE r.applicationId = :applicationId AND r.tenantId = :tenantId")
  void deleteAllByApplicationIdAndTenantId(
      @Param("applicationId") UUID applicationId, @Param("tenantId") String tenantId);
}
