package com.aidentx.core.log_management.controller;

import com.aidentx.core.log_management.config.AppConstants;
import com.aidentx.core.log_management.exception.DuplicateDataException;
import com.aidentx.core.log_management.exception.LogNotFoundException;
import com.aidentx.core.log_management.model.dto.CreatePinnedLogRequest;
import com.aidentx.core.log_management.model.dto.PinnedLog;
import com.aidentx.core.log_management.model.dto.UpdatePinnedLogRequest;
import com.aidentx.core.log_management.service.LogPinningService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing pinned logger entries.
 * Provides endpoints for pinning logs, and deleting pinned logs.
 */
@RestController
@RequestMapping("/api/v1/logs")
public class LogsPinningController {

    private final IUnifiedLogger logger;
    private final LogPinningService pinnedLogService;

    /**
     * Constructor with dependency injection.
     *
     * @param pinnedLogService the pinned log service
     * @param loggerFactory    the factory to create a logger instance
     */
    public LogsPinningController(LogPinningService pinnedLogService,
            UnifiedLoggerFactory loggerFactory) {
        this.pinnedLogService = pinnedLogService;
        this.logger = loggerFactory.getLogger("LogsController", AppConstants.LOG_CATEGORY_PINNED_LOGS);
    }

    /**
     * Creates a new pinned log entry.
     *
     * @param request the request containing log ID, name, description, and data for
     *                the pinned log
     * @return the created pinned logger entry with 201 status
     */
    @PostMapping("/pin")
    public ResponseEntity<PinnedLog> pinLog(@RequestBody CreatePinnedLogRequest request) {
        logger.trace().message(AppConstants.LOG_PIN_ATTEMPT).detail("request", request).log();
        // Convert DTO to entity
        PinnedLog pinnedLog = new PinnedLog(request.getDocumentId(), request.getName(),
                request.getDescription());
        if (request.getDocumentId() == null || request.getDocumentId().trim().isEmpty())
            return ResponseEntity.badRequest().body(null);
        PinnedLog savedPinnedLog = pinnedLogService.pinLog(pinnedLog);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPinnedLog);
    }

    /**
     * Unpins a log by removing its pinned status.
     * This sets isPinned to false and clears pinned metadata along with resolved
     * metadata.
     *
     * @param id the ID of the log to unpin
     * @return 204 No Content if successful, 404 if not found
     */
    @DeleteMapping("/pin/{documentId}")
    public ResponseEntity<Void> deletePinnedLog(@PathVariable String documentId) {
        logger.trace().message(AppConstants.LOG_DELETE_ATTEMPT).detail("documentId", documentId).log();
        boolean deleted = pinnedLogService.deletePinnedLogByDocumentId(documentId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Retrieves all pinned log entries sorted from newest to oldest by creation
     * time.
     *
     * @return list of pinned logger entries sorted by creation time (newest first)
     */
    @GetMapping("/pin")
    public ResponseEntity<List<PinnedLog>> getPinnedLogs() {
        logger.trace().message(AppConstants.LOG_GET_ALL_ATTEMPT).log();
        List<PinnedLog> pinnedLogs = pinnedLogService.getPinnedLogs();
        return ResponseEntity.ok(pinnedLogs);
    }

    /**
     * Retrieves all resolved pinned log entries sorted from newest to oldest by
     * resolution time.
     * Returns only logs that have been marked as resolved with resolver
     * information.
     *
     * @return list of resolved pinned logger entries with resolver information
     */
    @GetMapping("/pin/resolved")
    public ResponseEntity<List<PinnedLog>> getResolvedPinnedLogs() {
        logger.trace().message(AppConstants.LOG_GET_ALL_ATTEMPT).detail("resolvedOnly", true).log();
        List<PinnedLog> resolvedPinnedLogs = pinnedLogService.getResolvedPinnedLogs();
        return ResponseEntity.ok(resolvedPinnedLogs);
    }

    /**
     * Updates a pinned log entry with partial field updates.
     * Supports updating name, description, and data fields individually or in
     * combination.
     * Only provided fields will be updated, allowing for flexible partial updates.
     *
     * @param id            the UUID of the pinned log to update
     * @param updateRequest the request body containing fields to update (all fields
     *                      optional)
     * @return the updated pinned logger entry with 200 status, or error responses
     *         for validation failures
     */
    @PatchMapping("/pin/{documentId}")
    public ResponseEntity<?> updatePinnedLog(@PathVariable String documentId,
            @RequestBody UpdatePinnedLogRequest updateRequest) {
        logger.trace().message(AppConstants.LOG_UPDATE_ATTEMPT)
                .detail("documentId", documentId)
                .detail("updateRequest", updateRequest)
                .log();

        try {
            PinnedLog updatedLog = pinnedLogService.updatePinnedLog(documentId, updateRequest);
            return ResponseEntity.ok(updatedLog);
        } catch (IllegalArgumentException e) {
            logger.warn().message("Invalid update request")
                    .detail("documentId", documentId)
                    .detail("error", e.getMessage())
                    .log();
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (LogNotFoundException e) {
            logger.warn().message("Pinned log not found for update")
                    .detail("documentId", documentId)
                    .log();
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error(e).message("Unexpected error during pinned log update")
                    .detail("documentId", documentId)
                    .log();
            return ResponseEntity.internalServerError().body(AppConstants.MSG_FAILED_TO_UPDATE_PINNED_LOG);
        }
    }

    /**
     * Exception handler for DuplicateDataException.
     * Returns 409 Conflict when trying to create a pinned log with duplicate data.
     *
     * @param ex the DuplicateDataException
     * @return error response with 409 status
     */
    @ExceptionHandler(DuplicateDataException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateDataException(DuplicateDataException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Exception handler for LogNotFoundException.
     * Returns 404 Not Found when trying to pin a log that doesn't exist in
     * OpenSearch.
     *
     * @param ex the LogNotFoundException
     * @return error response with 404 status
     */
    @ExceptionHandler(LogNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleLogNotFoundException(LogNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Exception handler for IllegalArgumentException.
     * Returns 400 Bad Request for invalid input data.
     *
     * @param ex the IllegalArgumentException
     * @return error response with 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    /**
     * Marks a log as resolved with the current user's ID from JWT.
     *
     * @param documentId the documentId of the log to resolve
     * @return 200 if resolved successfully, 404 if log not found, 401 if invalid
     *         token
     */
    @PostMapping("/{documentId}/resolve")
    public ResponseEntity<?> resolveLog(@PathVariable String documentId) {
        logger.trace().message(AppConstants.RESOLVER_ATTEMPTING_TO_RESOLVE_LOG)
                .detail("documentId", documentId).log();

        Map<String, Object> result = pinnedLogService.resolveLogByDocumentId(documentId);
        int status = (Integer) result.get("status");

        if (status == 200) {
            Map<String, Object> response = new HashMap<>();
            response.put("documentId", result.get("documentId"));
            response.put("resolved", result.get("resolved"));
            response.put("resolverId", result.get("resolverId"));
            response.put("resolvedAt", result.get("resolvedAt"));
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", result.get("error"));
            return ResponseEntity.status(status).body(errorResponse);
        }
    }

    /**
     * Marks a log as unresolved by removing resolver fields.
     *
     * @param id the ID of the log to unresolve
     * @return 200 if unresolved successfully, 404 if log not found, 401 if invalid
     *         token
     */
    @DeleteMapping("/{documentId}/resolve")
    public ResponseEntity<?> unresolveLog(@PathVariable String documentId) {
        logger.trace().message(AppConstants.RESOLVER_ATTEMPTING_TO_UNRESOLVE_LOG)
                .detail("documentId", documentId).log();

        Map<String, Object> result = pinnedLogService.unresolveLogByDocumentId(documentId);
        int status = (Integer) result.get("status");

        if (status == 200) {
            Map<String, Object> response = new HashMap<>();
            response.put("documentId", result.get("documentId"));
            response.put("resolved", result.get("resolved"));
            response.put("unresolvedBy", result.get("unresolvedBy"));
            response.put("unresolvedAt", result.get("unresolvedAt"));
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", result.get("error"));
            return ResponseEntity.status(status).body(errorResponse);
        }
    }
}