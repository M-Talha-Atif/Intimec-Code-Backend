package com.aidentx.taskmanagement.controller;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standardized error response format for the API. This class provides a consistent structure for
 * returning errors to clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

  /** The HTTP status code. */
  private int status;

  /** A brief, human-readable message explaining the error. */
  private String message;

  /** The timestamp when the error occurred. */
  private Instant timestamp;

  /** The API path where the error occurred. */
  private String path;
}
