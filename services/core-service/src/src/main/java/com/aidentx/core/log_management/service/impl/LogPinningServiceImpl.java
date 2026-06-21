package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.AppConstants;
import com.aidentx.core.log_management.exception.DuplicateDataException;
import com.aidentx.core.log_management.exception.LogNotFoundException;
import com.aidentx.core.log_management.model.dto.PinnedLog;
import com.aidentx.core.log_management.model.dto.UpdatePinnedLogRequest;
import com.aidentx.core.log_management.service.Helpers.LogPinningHelper;
import com.aidentx.core.log_management.service.LogPinningService;
import com.aidentx.core.opensearch.service.OpenSearchService;
import com.aidentx.core.shared.security.JwtUtil;
import com.aidentx.core.shared.security.SecurityConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

/**
 * Service implementation for logPinning operations using OpenSearch.
 * Implements business logic for managing pinned log entries directly in
 * OpenSearch.
 */
@Service
public class LogPinningServiceImpl implements LogPinningService {

    private final IUnifiedLogger logger;
    private final OpenSearchService openSearchService;
    private final LogPinningHelper logPinningHelper;
    private final JwtUtil jwtUtil;

    // Set of fields to exclude from the pinned log data entry
    private Set<String> excludedFields = Set.of(
            "pinned.name", "pinned.description", "pinnerId", "pinnedAt",
            "resolverId", "resolvedAt", "updatedBy", "updatedAt", "createdAt",
            "isPinned", "isResolved");

    /**
     * Constructor with dependency injection.
     *
     * @param openSearchService the OpenSearch service for document operations
     * @param logPinningHelper  the helper for pinning-specific operations
     * @param loggerFactory     the factory to create a logger instance
     * @param jwtUtil           the JWT utility for token operations
     */
    public LogPinningServiceImpl(OpenSearchService openSearchService,
            LogPinningHelper logPinningHelper,
            UnifiedLoggerFactory loggerFactory,
            JwtUtil jwtUtil) {
        this.openSearchService = openSearchService;
        this.logPinningHelper = logPinningHelper;
        this.logger = loggerFactory.getLogger("LogPinningService", AppConstants.LOG_CATEGORY_PINNED_LOGS);
        this.jwtUtil = jwtUtil;
    }

    /**
     * Pins a log entry by updating the OpenSearch document.
     * Prevents duplicate entries by checking if the log is already pinned.
     *
     * @param pinnedLog the pinned logger entry containing log ID, name, and
     *                  description
     * @return the updated pinned logger entry
     * @throws IllegalArgumentException if pinnedLog is null or data is invalid
     * @throws DuplicateDataException   if log is already pinned
     * @throws RuntimeException         if pinning fails
     */
    @Override
    public PinnedLog pinLog(PinnedLog pinnedLog) {
        logger.trace().message(AppConstants.LOG_PIN_ATTEMPT).detail("pinnedLog", pinnedLog).log();
        try {
            if (pinnedLog == null) {
                logger.error().message(AppConstants.LOG_ATTEMPTED_SAVE_NULL_LOGGER).log();
                throw new IllegalArgumentException(AppConstants.MSG_INVALID_REQUEST_DATA);
            }

            if (pinnedLog.getName() == null || pinnedLog.getName().trim().isEmpty()) {
                logger.error().message(AppConstants.LOG_INVALID_DATA_PINNED_LOG_CREATION)
                        .failureReason(AppConstants.FAILURE_REASON_NAME_NULL_OR_EMPTY).log();
                throw new IllegalArgumentException(AppConstants.VALIDATION_NAME_REQUIRED);
            }

            logger.info().message(AppConstants.LOG_CHECKING_IF_DOCUMENT_ALREADY_PINNED)
                    .detail("documentId", pinnedLog.getDocumentId())
                    .log();
            boolean isAlreadyPinned = logPinningHelper.isDocumentPinnedById(pinnedLog.getDocumentId());

            if (isAlreadyPinned) {
                logger.warn().message(AppConstants.LOG_DUPLICATE_DATA_REJECTED)
                        .detail("documentId", pinnedLog.getDocumentId())
                        .objectType(AppConstants.OBJECT_TYPE_PINNED_LOG).log();
                throw new DuplicateDataException(
                        String.format(AppConstants.MSG_DUPLICATE_PINNED_LOG_ID_FORMAT, pinnedLog.getDocumentId()));
            }

            // Get current user ID from security context
            String currentUserId = extractUserIdFromCurrentRequest();

            // Pin the log in OpenSearch using document ID
            logger.info().message(AppConstants.LOG_PINNING_LOG_USING_DOCUMENT_ID)
                    .detail("documentId", pinnedLog.getDocumentId())
                    .log();
            boolean success = logPinningHelper.pinLogDocumentById(
                    pinnedLog.getDocumentId(),
                    currentUserId,
                    pinnedLog.getName(),
                    pinnedLog.getDescription());

            if (success) {
                logger.info().message(AppConstants.LOG_SUCCESSFULLY_CREATED_PINNED_LOG)
                        .detail("documentId", pinnedLog.getDocumentId()).objectType(AppConstants.OBJECT_TYPE_PINNED_LOG)
                        .log();
                // Return the pinned logger with updated timestamp information
                long currentTimeMillis = System.currentTimeMillis();
                pinnedLog.setUpdatedAt(currentTimeMillis);
                pinnedLog.setPinnedAt(currentTimeMillis);
                pinnedLog.setPinned(true);
                return pinnedLog;
            } else {
                logger.error().message(AppConstants.LOG_FAILED_TO_PIN_LOG_OPENSEARCH)
                        .detail("documentId", pinnedLog.getDocumentId()).log();
                throw new LogNotFoundException(
                        String.format(AppConstants.MSG_LOG_NOT_FOUND_IN_OPENSEARCH_FORMAT, pinnedLog.getDocumentId()));
            }

        } catch (IllegalArgumentException | DuplicateDataException e) {
            logger.error(e).message(AppConstants.LOG_INVALID_DATA_PINNED_LOG_CREATION).log();
            throw e;
        } catch (LogNotFoundException e) {
            logger.error(e).message(AppConstants.LOG_PINNING_LOG_NOT_FOUND)
                    .detail("documentId", pinnedLog.getDocumentId())
                    .log();
            throw e;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_CREATE_PINNED_LOG).log();
            throw new RuntimeException(AppConstants.MSG_FAILED_TO_CREATE_PINNED_LOG, e);
        }
    }

    /**
     * Deletes a pinned log entry by OpenSearch document ID.
     * Uses direct document access instead of searching by log ID.
     *
     * @param documentId the OpenSearch document _id of the log to unpin
     * @return true if unpinned successfully, false if not found or not pinned
     * @throws RuntimeException if unpinning fails
     */
    @Override
    public boolean deletePinnedLogByDocumentId(String documentId) {
        logger.trace().message("Delete attempt using document ID")
                .detail("documentId", documentId != null ? documentId : "null")
                .objectType(AppConstants.OBJECT_TYPE_PINNED_LOG).log();
        try {
            if (documentId == null || documentId.trim().isEmpty()) {
                logger.error().message(AppConstants.LOG_ATTEMPTED_DELETE_NULL_OR_EMPTY_DOCUMENT_ID).log();
                throw new IllegalArgumentException("Document ID is required");
            }

            // Check if document is currently pinned using optimized method
            if (!logPinningHelper.isDocumentPinnedById(documentId)) {
                logger.warn().message(AppConstants.LOG_ATTEMPTED_DELETE_NON_EXISTENT_OR_NON_PINNED_DOCUMENT)
                        .detail("documentId", documentId)
                        .objectType(AppConstants.OBJECT_TYPE_PINNED_LOG).log();
                return false;
            }

            // Get current user ID from security context
            String currentUserId = extractUserIdFromCurrentRequest();

            // Unpin the log using optimized method
            boolean success = logPinningHelper.unpinLogDocumentById(documentId, currentUserId);

            if (success) {
                logger.info().message(AppConstants.LOG_SUCCESSFULLY_DELETED_PINNED_LOG_USING_DOCUMENT_ID)
                        .detail("documentId", documentId)
                        .objectType(AppConstants.OBJECT_TYPE_PINNED_LOG).log();
                return true;
            } else {
                logger.error().message(AppConstants.LOG_FAILED_TO_UNPIN_LOG_USING_DOCUMENT_ID)
                        .detail("documentId", documentId).log();
                return false;
            }

        } catch (IllegalArgumentException e) {
            logger.error(e).message(AppConstants.LOG_INVALID_DOCUMENT_ID_FOR_PINNED_LOG_DELETION).log();
            throw e;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_DELETE_PINNED_LOG_USING_DOCUMENT_ID)
                    .detail("documentId", documentId)
                    .objectType(AppConstants.OBJECT_TYPE_PINNED_LOG).log();
            throw new RuntimeException("Failed to delete pinned log", e);
        }
    }

    /**
     * Updates a pinned log entry by document ID.
     * Uses direct document access to update pinned log information.
     *
     * @param documentId    the OpenSearch document _id of the pinned log entry to
     *                      update
     * @param updateRequest the update request containing the new values
     * @return the updated pinned log entry
     * @throws IllegalArgumentException if documentId is null or empty
     * @throws LogNotFoundException     if the log is not found or not pinned
     * @throws RuntimeException         if the update fails
     */
    @Override
    public PinnedLog updatePinnedLog(String documentId, UpdatePinnedLogRequest updateRequest) {
        logger.trace().message(AppConstants.LOG_UPDATE_ATTEMPT_USING_DOCUMENT_ID)
                .detail("documentId", documentId != null ? documentId : "null")
                .objectType(AppConstants.OBJECT_TYPE_PINNED_LOG).log();
        try {
            if (documentId == null || documentId.trim().isEmpty()) {
                logger.error().message(AppConstants.LOG_ATTEMPTED_UPDATE_NULL_OR_EMPTY_DOCUMENT_ID).log();
                throw new IllegalArgumentException("Document ID is required");
            }

            if (updateRequest == null) {
                logger.error().message(AppConstants.LOG_ATTEMPTED_UPDATE_NULL_UPDATE_REQUEST).log();
                throw new IllegalArgumentException("Update request is required");
            }

            // Check if document exists and is pinned using optimized method
            if (!logPinningHelper.isDocumentPinnedById(documentId)) {
                logger.warn().message(AppConstants.LOG_ATTEMPTED_UPDATE_NON_EXISTENT_OR_NON_PINNED_DOCUMENT)
                        .detail("documentId", documentId)
                        .objectType(AppConstants.OBJECT_TYPE_PINNED_LOG).log();
                throw new LogNotFoundException("Pinned log not found with document ID: " + documentId);
            }

            // Get current user ID from security context
            String currentUserId = extractUserIdFromCurrentRequest();

            // Build update map with only non-null fields from the request
            Map<String, Object> updates = new HashMap<>();
            updates.put("updatedAt", System.currentTimeMillis());
            updates.put("updatedBy", currentUserId);

            // Update name if provided
            if (updateRequest.getName() != null) {
                if (updateRequest.getName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Name cannot be empty");
                }
                updates.put("pinned.name", updateRequest.getName().trim());
            }

            // Update description if provided (can be null to remove)
            if (updateRequest.getDescription() != null) {
                updates.put("pinned.description", updateRequest.getDescription());
            }

            // Update the document using direct document ID
            boolean success = openSearchService.updateDocumentById(documentId, "main-logs", updates);

            if (success) {
                logger.info().message(AppConstants.LOG_SUCCESSFULLY_UPDATED_PINNED_LOG_USING_DOCUMENT_ID)
                        .detail("documentId", documentId)
                        .objectType(AppConstants.OBJECT_TYPE_PINNED_LOG).log();

                // Get the updated document to return
                JsonNode updatedDocument = openSearchService.getDocumentById(documentId, "main-logs");
                if (updatedDocument != null) {
                    JsonNode source = updatedDocument.path("_source");
                    PinnedLog updatedPinnedLog = convertToPinnedLog(source);
                    if (updatedPinnedLog != null) {
                        updatedPinnedLog.setDocumentId(documentId);
                        return updatedPinnedLog;
                    }
                }

                // Fallback: return a minimal PinnedLog object if conversion fails
                PinnedLog result = new PinnedLog();
                result.setDocumentId(documentId);
                return result;
            } else {
                logger.error().message(AppConstants.LOG_FAILED_TO_UPDATE_PINNED_LOG_USING_DOCUMENT_ID)
                        .detail("documentId", documentId).log();
                throw new RuntimeException("Failed to update pinned log");
            }

        } catch (IllegalArgumentException | LogNotFoundException e) {
            logger.error(e).message(AppConstants.LOG_INVALID_REQUEST_FOR_PINNED_LOG_UPDATE).log();
            throw e;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_UPDATE_PINNED_LOG_USING_DOCUMENT_ID)
                    .detail("documentId", documentId)
                    .objectType(AppConstants.OBJECT_TYPE_PINNED_LOG).log();
            throw new RuntimeException("Failed to update pinned log", e);
        }
    }

    /**
     * Retrieves only unresolved pinned log entries from OpenSearch sorted from
     * newest to oldest.
     * Filters out any logs that have been marked as resolved.
     *
     * @return list of unresolved pinned logger entries converted from OpenSearch
     *         documents
     * @throws RuntimeException if retrieval fails
     */
    @Override
    public List<PinnedLog> getPinnedLogs() {
        logger.trace().message(AppConstants.LOG_GET_ALL_ATTEMPT).log();
        try {
            JsonNode searchResults = logPinningHelper.searchPinnedLogsWithDocumentIds();
            List<PinnedLog> pinnedLogs = new ArrayList<>();

            if (searchResults != null && searchResults.has(AppConstants.OPENSEARCH_FIELD_HITS)) {
                JsonNode hits = searchResults.get(AppConstants.OPENSEARCH_FIELD_HITS)
                        .get(AppConstants.OPENSEARCH_FIELD_HITS);

                for (JsonNode hit : hits) {
                    JsonNode source = hit.get(AppConstants.OPENSEARCH_FIELD_SOURCE);

                    // Check if the log is resolved - skip if it is
                    boolean isResolved = source.path(AppConstants.OPENSEARCH_FIELD_IS_RESOLVED).asBoolean(false);
                    if (isResolved) {
                        continue; // Skip resolved logs
                    }

                    PinnedLog pinnedLog = convertToPinnedLog(source);
                    if (pinnedLog != null) {
                        // Extract and set the document ID from the hit
                        String documentId = hit.path("_id").asText(null);
                        pinnedLog.setDocumentId(documentId);

                        pinnedLogs.add(pinnedLog);
                    }
                }
            }

            logger.info().message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_PINNED_LOGS)
                    .detail("totalCount", pinnedLogs.size())
                    .detail("unresolvedOnly", true)
                    .log();
            return pinnedLogs;

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_RETRIEVE_PINNED_LOGS).log();
            throw new RuntimeException(AppConstants.MSG_FAILED_TO_RETRIEVE_PINNED_LOGS, e);
        }
    }

    /**
     * Retrieves only resolved pinned log entries from OpenSearch sorted from newest
     * to oldest.
     * Includes only logs that have been marked as resolved with resolver
     * information.
     *
     * @return list of resolved pinned logger entries converted from OpenSearch
     *         documents
     * @throws RuntimeException if retrieval fails
     */
    @Override
    public List<PinnedLog> getResolvedPinnedLogs() {
        logger.trace().message(AppConstants.LOG_GET_ALL_ATTEMPT).detail("resolvedOnly", true).log();
        try {
            JsonNode searchResults = logPinningHelper.searchPinnedLogsWithDocumentIds();
            List<PinnedLog> resolvedPinnedLogs = new ArrayList<>();

            if (searchResults != null && searchResults.has(AppConstants.OPENSEARCH_FIELD_HITS)) {
                JsonNode hits = searchResults.get(AppConstants.OPENSEARCH_FIELD_HITS)
                        .get(AppConstants.OPENSEARCH_FIELD_HITS);

                for (JsonNode hit : hits) {
                    JsonNode source = hit.get(AppConstants.OPENSEARCH_FIELD_SOURCE);

                    // Check if the log is resolved - include only if it is
                    boolean isResolved = source.path(AppConstants.OPENSEARCH_FIELD_IS_RESOLVED).asBoolean(false);
                    if (!isResolved) {
                        continue;
                    }

                    PinnedLog pinnedLog = convertToPinnedLog(source);
                    if (pinnedLog != null) {
                        // Extract and set the document ID from the hit
                        String documentId = hit.path("_id").asText(null);
                        pinnedLog.setDocumentId(documentId);

                        // Add resolver information to the pinned log
                        addResolverInformation(pinnedLog, source);
                        resolvedPinnedLogs.add(pinnedLog);
                    }
                }
            }

            logger.info().message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_PINNED_LOGS)
                    .detail("totalCount", resolvedPinnedLogs.size())
                    .detail("resolvedOnly", true)
                    .log();
            return resolvedPinnedLogs;

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_RETRIEVE_PINNED_LOGS).detail("resolvedOnly", true).log();
            throw new RuntimeException(AppConstants.MSG_FAILED_TO_RETRIEVE_PINNED_LOGS, e);
        }
    }

    /**
     * Converts an OpenSearch document to a PinnedLog entity.
     * Creates a clean data structure with only essential log information.
     */
    private PinnedLog convertToPinnedLog(JsonNode source) {
        try {
            PinnedLog pinnedLog = new PinnedLog();

            // Set basic fields from pinned metadata
            pinnedLog.setName(source.path(AppConstants.OPENSEARCH_FIELD_PINNED_NAME).asText(""));
            pinnedLog.setDescription(source.path(AppConstants.OPENSEARCH_FIELD_PINNED_DESCRIPTION).asText(""));
            pinnedLog.setPinned(source.path(AppConstants.OPENSEARCH_FIELD_IS_PINNED).asBoolean(false));
            pinnedLog.setPinnerId(source.path(AppConstants.OPENSEARCH_FIELD_PINNER_ID).asText(""));
            pinnedLog.setPinnedAt(source.path(AppConstants.OPENSEARCH_FIELD_PINNED_AT).asLong(0));
            pinnedLog.setUpdatedAt(source.path(AppConstants.OPENSEARCH_FIELD_UPDATED_AT).asLong(0));
            pinnedLog.setUpdatedBy(source.path(AppConstants.OPENSEARCH_FIELD_UPDATED_BY).asText(""));
            pinnedLog.setResolvedAt(source.path(AppConstants.OPENSEARCH_FIELD_RESOLVED_AT).asLong(0));

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode cleanData = mapper.createObjectNode();
            // Copy all fields except the excluded ones
            java.util.Iterator<String> fieldNames = source.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = source.get(fieldName);

                if (!excludedFields.contains(fieldName)) {
                    if ("@timestamp".equals(fieldName)) {
                        cleanData.set("timestamp", fieldValue);
                    } else {
                        cleanData.set(fieldName, fieldValue);
                    }
                }
            }
            pinnedLog.setData(cleanData);

            return pinnedLog;

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_CONVERT_DOCUMENT).log();
            return null;
        }
    }

    /**
     * Adds resolver information to a PinnedLog from the OpenSearch source document.
     * This includes resolver ID and resolved timestamp for resolved logs.
     */
    private void addResolverInformation(PinnedLog pinnedLog, JsonNode source) {
        try {
            if (pinnedLog.getData() != null && pinnedLog.getData().isObject()) {
                pinnedLog
                        .setResolverId(source.path(AppConstants.OPENSEARCH_FIELD_RESOLVER_ID).asText(""));
                pinnedLog.setResolvedAt(source.path(AppConstants.OPENSEARCH_FIELD_RESOLVED_AT).asLong(0));
                pinnedLog.setResolved(true);
            }
        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_FAILED_TO_ADD_RESOLVER_INFORMATION_TO_PINNED_LOG)
                    .detail("pinned Document ID", pinnedLog.getDocumentId())
                    .log();
        }
    }

    /**
     * Checks if a log document exists in OpenSearch by document ID.
     * Debug method for troubleshooting query issues.
     * 
     * @param documentId the document ID to check
     * @return true if document exists, false otherwise
     */
    @Override
    public boolean checkDocumentExists(String documentId) {
        logger.info().message(AppConstants.LOG_CHECKING_DOCUMENT_EXISTS)
                .detail("documentId", documentId)
                .log();

        return openSearchService.documentExistsById(documentId, "main-logs");
    }

    // ================== RESOLVER OPERATIONS ==================

    /**
     * Marks a log as resolved with the current user's ID from JWT.
     * 
     * @param documentId the ID of the log to resolve
     * @return a map containing the result of the resolve operation
     */
    @Override
    public Map<String, Object> resolveLogByDocumentId(String documentId) {
        logger.trace().message(AppConstants.RESOLVER_ATTEMPTING_TO_RESOLVE_LOG).detail("documentId", documentId).log();

        try {
            // Extract user ID from JWT token
            String userId = extractUserIdFromCurrentRequest();
            if (userId == null || userId.isEmpty()) {
                logger.warn().message(AppConstants.RESOLVER_NO_VALID_USER_ID_FOR_RESOLVE).log();
                return Map.of(AppConstants.HTTP_ERROR_FIELD, AppConstants.RESOLVER_AUTHENTICATION_REQUIRED,
                        AppConstants.HTTP_STATUS_FIELD, 401);
            }

            // Check if log exists and get current state
            JsonNode document = openSearchService.getDocumentById(documentId, "main-logs");
            if (document == null) {
                logger.warn().message(AppConstants.RESOLVER_ATTEMPTED_TO_RESOLVE_NON_EXISTENT_LOG)
                        .detail("documentId", documentId)
                        .log();
                return Map.of(AppConstants.HTTP_ERROR_FIELD, AppConstants.RESOLVER_LOG_NOT_FOUND,
                        AppConstants.HTTP_STATUS_FIELD, 404);
            }

            // Check if log is already resolved by checking the document directly
            JsonNode source = document.path("_source");
            boolean isAlreadyResolved = source.path(AppConstants.OPENSEARCH_FIELD_IS_RESOLVED).asBoolean(false);
            if (isAlreadyResolved) {
                String existingResolverId = source.path(AppConstants.OPENSEARCH_FIELD_RESOLVER_ID).asText("");
                long existingResolvedAt = source.path(AppConstants.OPENSEARCH_FIELD_RESOLVED_AT).asLong(0);

                logger.warn().message(AppConstants.RESOLVER_ATTEMPTED_TO_RESOLVE_ALREADY_RESOLVED_LOG)
                        .detail("id", documentId)
                        .detail("existingResolverId", existingResolverId)
                        .detail("existingResolvedAt", existingResolvedAt)
                        .log();

                return Map.of(
                        "error", AppConstants.RESOLVER_LOG_ALREADY_RESOLVED,
                        "id", documentId,
                        "resolverId", existingResolverId,
                        "resolvedAt", existingResolvedAt,
                        "status", 409 // Conflict status
                );
            }

            // Update the document with resolver fields using direct document ID
            long resolvedAtMillis = System.currentTimeMillis();
            Map<String, Object> updates = Map.of(
                    AppConstants.RESPONSE_FIELD_IS_RESOLVED, true,
                    "updatedBy", userId,
                    "updatedAt", System.currentTimeMillis(),
                    AppConstants.RESPONSE_FIELD_RESOLVER_ID, userId,
                    AppConstants.RESPONSE_FIELD_RESOLVED_AT, resolvedAtMillis);

            boolean success = openSearchService.updateDocumentById(documentId, "main-logs", updates);
            if (success) {
                logger.info().message(AppConstants.RESOLVER_LOG_RESOLVED_SUCCESSFULLY).detail("documentId", documentId)
                        .detail("resolverId", userId).log();
                return Map.of(
                        "documentId", documentId,
                        "resolved", true,
                        "resolverId", userId,
                        "resolvedAt", resolvedAtMillis,
                        "status", 200);
            } else {
                logger.error().message(AppConstants.RESOLVER_FAILED_TO_RESOLVE_LOG_IN_OPENSEARCH)
                        .detail("documentId", documentId)
                        .log();
                return Map.of("error", AppConstants.RESOLVER_FAILED_TO_RESOLVE_LOG,
                        "status", 500);
            }
        } catch (Exception e) {
            logger.error(e).message(AppConstants.RESOLVER_ERROR_RESOLVING_LOG).detail("documentId", documentId).log();
            return Map.of("error",
                    AppConstants.RESOLVER_INTERNAL_SERVER_ERROR_PREFIX + e.getMessage(), "status",
                    500);
        }
    }

    /**
     * Marks a log as unresolved by removing resolver fields.
     * 
     * @param documentId the ID of the log to unresolve
     * @return a map containing the result of the unresolve operation
     */
    @Override
    public Map<String, Object> unresolveLogByDocumentId(String documentId) {
        logger.trace().message(AppConstants.RESOLVER_ATTEMPTING_TO_UNRESOLVE_LOG).detail("documentId", documentId)
                .log();

        try {
            // Extract user ID from JWT token for audit purposes
            String userId = extractUserIdFromCurrentRequest();
            if (userId == null || userId.isEmpty()) {
                logger.warn().message(AppConstants.RESOLVER_NO_VALID_USER_ID_FOR_UNRESOLVE).log();
                return Map.of(AppConstants.HTTP_ERROR_FIELD, AppConstants.RESOLVER_AUTHENTICATION_REQUIRED,
                        AppConstants.HTTP_STATUS_FIELD, 401);
            }

            // Check if log exists and get current state
            JsonNode document = openSearchService.getDocumentById(documentId, "main-logs");
            if (document == null) {
                logger.warn().message(AppConstants.RESOLVER_ATTEMPTED_TO_UNRESOLVE_NON_EXISTENT_LOG)
                        .detail("id", documentId)
                        .log();
                return Map.of(AppConstants.HTTP_ERROR_FIELD, AppConstants.RESOLVER_LOG_NOT_FOUND,
                        AppConstants.HTTP_STATUS_FIELD, 404);
            }

            // Update the document to remove resolver fields using direct document ID
            Map<String, Object> updates = new HashMap<>();
            updates.put(AppConstants.RESPONSE_FIELD_IS_RESOLVED, false);
            updates.put("updatedBy", userId);
            updates.put("updatedAt", System.currentTimeMillis());
            updates.put(AppConstants.RESPONSE_FIELD_RESOLVER_ID, null);
            updates.put(AppConstants.RESPONSE_FIELD_RESOLVED_AT, null);

            boolean success = openSearchService.updateDocumentById(documentId, "main-logs", updates);
            if (success) {
                logger.info().message(AppConstants.RESOLVER_LOG_UNRESOLVED_SUCCESSFULLY)
                        .detail("documentId", documentId)
                        .detail("userId", userId).log();
                return Map.of(
                        "documentId", documentId,
                        "resolved", false,
                        "unresolvedAt", System.currentTimeMillis(),
                        "status", 200);
            } else {
                logger.error().message(AppConstants.RESOLVER_FAILED_TO_UNRESOLVE_LOG_IN_OPENSEARCH)
                        .detail("documentId", documentId)
                        .log();
                return Map.of("error", AppConstants.RESOLVER_FAILED_TO_UNRESOLVE_LOG,
                        "status", 500);
            }
        } catch (Exception e) {
            logger.error(e).message(AppConstants.RESOLVER_ERROR_UNRESOLVING_LOG).detail("documentId", documentId).log();
            return Map.of("error",
                    AppConstants.RESOLVER_INTERNAL_SERVER_ERROR_PREFIX + e.getMessage(), "status",
                    500);
        }
    }

    // ================== DEBUG OPERATIONS ==================

    /**
     * Extracts user ID from the current HTTP request JWT token.
     * 
     * @return the user ID from the JWT token, or null if not available
     */
    private String extractUserIdFromCurrentRequest() {
        try {
            // Get authentication from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    !AppConstants.SECURITY_ANONYMOUS_USER.equals(authentication.getPrincipal())) {

                // Try to get JWT token from current request
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();
                if (requestAttributes != null) {
                    String authHeader = requestAttributes.getRequest()
                            .getHeader(SecurityConstants.AUTHORIZATION_HEADER);
                    if (authHeader != null && authHeader.startsWith(AppConstants.SECURITY_BEARER_PREFIX)) {
                        String token = authHeader.substring(AppConstants.BEARER_PREFIX_LENGTH); // Remove "Bearer "
                                                                                                // prefix

                        // Extract user ID from JWT token using JwtUtil
                        return jwtUtil.extractUserId(token);
                    }
                }

                // Fallback to authentication name if JWT extraction fails
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.warn().message(AppConstants.RESOLVER_FAILED_TO_EXTRACT_USER_ID).log();
        }
        return null;
    }
}