package com.aidentx.taskmanagement.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified pagination response for task listings. Contains only essential pagination information
 * to reduce response size.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Simplified pagination response for tasks")
public class PagedTaskResponse {

  @Schema(description = "List of tasks for the current page")
  private List<TaskResponse> tasks;

  @Schema(description = "Current page number (0-based)", example = "0")
  private int pageNumber;

  @Schema(description = "Number of items per page", example = "10")
  private int pageSize;

  @Schema(description = "Total number of pages", example = "5")
  private int totalPages;

  @Schema(description = "Total number of items across all pages", example = "42")
  private long totalElements;

  @Schema(description = "Whether this is the first page", example = "true")
  private boolean first;

  @Schema(description = "Whether this is the last page", example = "false")
  private boolean last;

  @Schema(description = "Number of items in the current page", example = "10")
  private int numberOfElements;

  @Schema(description = "Whether the result set is empty", example = "false")
  private boolean empty;
}
