package com.aidentx.taskmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for streaming task execution logs via SSE. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskLogStreamResponse {

  /** Timestamp of the log entry */
  private Long timestamp;

  /** Log level */
  private String level;

  /** Log message */
  private String message;

  /** Task execution status */
  private String status;

  /** Exception message if error occurred */
  private String exception;

  /** Flag indicating if this is the last message (task completed/failed) */
  private Boolean isLastMessage;
}
