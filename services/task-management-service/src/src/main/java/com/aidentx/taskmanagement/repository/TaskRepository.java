package com.aidentx.taskmanagement.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aidentx.taskmanagement.model.entity.Task;

/** Repository interface for the Task entity. */
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

  /** Finds a task by its ID and tenant ID. */
  Optional<Task> findByIdAndTenantId(UUID taskId, String tenantId);

  /**
   * Finds a task by its ID, application ID, and tenant ID. This is a more specific query to ensure
   * a task belongs to an application before operating on it.
   */
  Optional<Task> findByIdAndApplicationIdAndTenantId(
      UUID taskId, UUID applicationId, String tenantId);

  /** Finds all tasks for a specific tenant with pagination. */
  Page<Task> findByTenantId(String tenantId, Pageable pageable);

  /** Finds all tasks for a specific application and tenant with pagination. */
  Page<Task> findByApplicationIdAndTenantId(UUID applicationId, String tenantId, Pageable pageable);

  /** Finds all task IDs for a specific application and tenant (for bulk deletion). */
  java.util.List<Task> findAllByApplicationIdAndTenantId(UUID applicationId, String tenantId);

  /** Deletes all tasks for a specific application and tenant. */
  void deleteAllByApplicationIdAndTenantId(UUID applicationId, String tenantId);
}
