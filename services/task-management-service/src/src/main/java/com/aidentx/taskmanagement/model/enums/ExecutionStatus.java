package com.aidentx.taskmanagement.model.enums;

public enum ExecutionStatus {
  PENDING,
  RUNNABLE,
  RUNNING,
  WAITING,
  SUSPENDED,
  CLOSED,
  COMPLETED,
  SKIPPED,  // Run was never attempted: another execution held the advisory lock
  CANCELLED, // Explicitly stopped by an operator before natural completion
  FAILED
}
