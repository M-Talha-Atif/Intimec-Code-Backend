package com.aidentx.taskmanagement.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A standardized, paginated response for lists of task execution runs. This structure provides the
 * client with the data for the current page and the necessary metadata to navigate to other pages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response for task execution run history")
public class PagedTaskRunResponse {

  @Schema(description = "List of task runs for the current page")
  private List<TaskRunResponse> runs;

  @Schema(description = "Current page number (0-based)", example = "0")
  private int pageNumber;

  @Schema(description = "Number of items per page", example = "20")
  private int pageSize;

  @Schema(description = "Total number of pages available", example = "10")
  private int totalPages;

  @Schema(description = "Total number of items across all pages", example = "198")
  private long totalElements;

  @Schema(description = "Indicates if this is the first page", example = "true")
  private boolean first;

  @Schema(description = "Indicates if this is the last page", example = "false")
  private boolean last;

  @Schema(description = "Number of items on the current page", example = "20")
  private int numberOfElements;

  @Schema(description = "Indicates if the current page has no content", example = "false")
  private boolean empty;
}
