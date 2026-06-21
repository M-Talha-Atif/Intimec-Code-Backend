package com.aidentx.taskmanagement.controller;

import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.config.TaskManagementConstants;

/**
 * Global exception handler for all REST controllers. This class centralizes exception handling,
 * ensuring consistent error responses and reducing boilerplate code in controllers.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private final IUnifiedLogger log;

  public GlobalExceptionHandler(UnifiedLoggerFactory unifiedLoggerFactory) {
    this.log = unifiedLoggerFactory.getLogger(GlobalExceptionHandler.class);
  }

  /**
   * Handles resource not found exceptions. Catches {@link IllegalArgumentException} which we use in
   * services to indicate a missing entity.
   *
   * @param ex The exception.
   * @param request The HTTP request.
   * @return A ResponseEntity with HTTP status 404 (Not Found).
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.warn().message(TaskManagementConstants.MSG_RESOURCE_NOT_FOUND + ex.getMessage()).log();
    ApiErrorResponse errorResponse =
        new ApiErrorResponse(
            HttpStatus.NOT_FOUND.value(), ex.getMessage(), Instant.now(), request.getRequestURI());
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  /**
   * Handles validation errors for request bodies. Catches {@link MethodArgumentNotValidException}
   * from @Valid annotation.
   *
   * @param ex The exception.
   * @param request The HTTP request.
   * @return A ResponseEntity with HTTP status 400 (Bad Request).
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
    log.warn().message(TaskManagementConstants.MSG_VALIDATION_ERROR + errors).log();
    ApiErrorResponse errorResponse =
        new ApiErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            TaskManagementConstants.MSG_VALIDATION_FAILED + errors,
            Instant.now(),
            request.getRequestURI());
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles all other unexpected exceptions.
   *
   * @param ex The exception.
   * @param request The HTTP request.
   * @return A ResponseEntity with HTTP status 500 (Internal Server Error).
   */
  /**
   * Ignores SSE stream timeouts — the response Content-Type is text/event-stream
   * and cannot carry a JSON ApiErrorResponse body. Spring logs a WARN automatically.
   */
  @ExceptionHandler(AsyncRequestTimeoutException.class)
  public ResponseEntity<Void> handleAsyncTimeout(AsyncRequestTimeoutException ex) {
    // SSE connections time out normally — no JSON body can be written on an event-stream
    return ResponseEntity.noContent().build();
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGlobalException(
      Exception ex, HttpServletRequest request) {
    log.error(ex).message(TaskManagementConstants.MSG_UNEXPECTED_ERROR_OCCURRED).log();
    ApiErrorResponse errorResponse =
        new ApiErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            TaskManagementConstants.MSG_UNEXPECTED_INTERNAL_ERROR,
            Instant.now(),
            request.getRequestURI());
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
