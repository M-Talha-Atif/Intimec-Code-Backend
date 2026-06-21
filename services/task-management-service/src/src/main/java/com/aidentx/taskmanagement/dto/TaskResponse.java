package com.aidentx.taskmanagement.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.aidentx.taskmanagement.model.dto.UserReference;
import com.aidentx.taskmanagement.model.enums.*;

import lombok.Data;

/**
 * Response DTO for task retrieval operations. Contains complete task information including metadata
 * and execution status.
 */
@Data
public class TaskResponse {

  private UUID id;
  private String name;
  private String description;
  private String taskType;
  private String category;
  private Integer priority;

  // Ownership
  private UserReference owner;
  private List<UserReference> assignedTo = new ArrayList<>();
  private String tenantId;
  private ApplicationReference application;

  // Status
  private PersistenceStatus persistenceStatus;
  private ExecutionStatus executionStatus;
  private String businessStatus;

  // Configuration
  private Map<String, Object> taskConfig;
  private String executionConfig;

  // Scheduling
  private ScheduleType scheduleType;
  private String cronExpression;
  private Integer intervalSeconds;
  private BindingType bindingType;

  private Instant earliestStartTime;

  private Instant latestStartTime;

  private Instant latestFinishTime;

  private Instant nextExecution;

  private Instant lastExecution;

  // Misfire and resilience
  private MisfireAction misfireAction;
  private ThreadStopAction threadStopAction;
  private Boolean isResilient;

  // Execution tracking

  private Instant lastRunStartTimestamp;

  private Instant lastRunFinishTimestamp;

  private String executionNodeId;

  // Metadata

  private Instant createdAt;

  private UserReference createdBy;

  private Instant updatedAt;

  private UserReference updatedBy;
  private Integer version;
}
