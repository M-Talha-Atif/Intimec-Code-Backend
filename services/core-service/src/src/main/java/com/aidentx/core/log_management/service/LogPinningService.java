package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.PinnedLog;
import com.aidentx.core.log_management.model.dto.UpdatePinnedLogRequest;

import java.util.List;
import java.util.Map;

/**
 * Service interface for PinnedLog operations.
 * Defines business logic methods for managing pinned log entries.
 */
public interface LogPinningService {
    
    /**
     * Creates and saves a new pinned log entry.
     * 
     * @param pinnedLog the pinned log entry to save
     * @return the saved pinned log entry
     */
    PinnedLog pinLog(PinnedLog pinnedLog);
  
    /**
     * Deletes a pinned log entry by OpenSearch document ID (optimized).
     * Uses direct document access instead of searching.
     * 
     * @param documentId the OpenSearch document _id of the pinned log entry to delete
     * @return true if deleted successfully, false if not found
     */
    boolean deletePinnedLogByDocumentId(String documentId);
    
    /**
     * Updates a pinned log entry by document ID.
     * Uses direct document access to update pinned log information.
     * 
     * @param documentId the OpenSearch document _id of the pinned log entry to update
     * @param updateRequest the update request containing the new values
     * @return the updated pinned log entry
     */
    PinnedLog updatePinnedLog(String documentId, UpdatePinnedLogRequest updateRequest);
    
    /**
     * Retrieves only unresolved pinned log entries sorted from newest to oldest.
     * Excludes any logs that have been marked as resolved.
     * 
     * @return list of unresolved pinned logger entries sorted by creation order
     */
    List<PinnedLog> getPinnedLogs();

    /**
     * Retrieves only resolved pinned log entries sorted from newest to oldest.
     * Includes only logs that have been marked as resolved with resolver information.
     * 
     * @return list of resolved pinned logger entries sorted by resolution date
     */
    List<PinnedLog> getResolvedPinnedLogs();

    /**
     * Checks if a log document exists in OpenSearch by document ID.
     * Debug method for troubleshooting query issues.
     * 
     * @param documentId the document ID to check
     * @return true if document exists, false otherwise
     */
    boolean checkDocumentExists(String documentId);

    // ================== RESOLVER OPERATIONS ==================

    /**
     * Marks a log as resolved with the current user's ID from JWT.
     * 
     * @param id the ID of the log to resolve
     * @return a map containing the result of the resolve operation
     */
    Map<String, Object> resolveLogByDocumentId(String id);

    /**
     * Marks a log as unresolved by removing resolver fields.
     * 
     * @param id the ID of the log to unresolve
     * @return a map containing the result of the unresolve operation
     */
    Map<String, Object> unresolveLogByDocumentId(String id);
}
