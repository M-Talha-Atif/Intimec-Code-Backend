package com.aidentx.core.log_management.service.Helpers;

import com.aidentx.core.log_management.config.AppConstants;
import com.aidentx.core.opensearch.service.OpenSearchService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LogPinningHelper {
    private final IUnifiedLogger logger;
    private final OpenSearchService openSearchService;

    public LogPinningHelper(UnifiedLoggerFactory loggerFactory,
            OpenSearchService openSearchService) {
        this.logger = loggerFactory.getLogger("LogPinningService", AppConstants.LOG_CATEGORY_PINNED_LOGS);
        this.openSearchService = openSearchService;
    }

    /**
     * Checks if a log document exists and is already pinned.
     *
     * @param documentId the documentId of the log to check
     * @return true if the log exists and is pinned, false otherwise
     */
    public boolean isLogAlreadyPinned(String documentId) {
        logger.trace().message(AppConstants.LOG_CHECKING_IF_LOG_ALREADY_PINNED)
                .detail("documentId", documentId)
                .log();

        try {
            // Search for the specific document ID with isPinned = true
            Map<String, Object> searchQuery = new HashMap<>();
            Map<String, Object> boolQuery = new HashMap<>();

            // Use ids query to find the specific document by _id (more efficient)
            Map<String, Object> idsQuery = new HashMap<>();
            idsQuery.put("values", new String[] { documentId });

            // Use term query for isPinned field
            Map<String, Object> termPinned = new HashMap<>();
            termPinned.put("isPinned", true);

            boolQuery.put("must", new Object[] {
                    Map.of("ids", idsQuery),
                    Map.of("term", termPinned)
            });
            searchQuery.put("query", Map.of("bool", boolQuery));
            searchQuery.put("size", 1);

            JsonNode result = openSearchService.searchDocuments("main-logs", searchQuery);

            if (result != null && result.has("hits")) {
                int totalHits = result.path("hits").path("total").path("value").asInt();
                return totalHits > 0;
            }

            return false;

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_CHECK_IF_LOG_IS_PINNED)
                    .detail("documentId", documentId)
                    .log();
            return false;
        }
    }

    /**
     * Pins a log document using direct document ID access (optimized).
     * Instead of searching for the document, uses the OpenSearch document _id
     * directly.
     *
     * @param documentId  the OpenSearch document _id
     * @param pinnerId    the ID of the user who is pinning the log
     * @param name        the name/title for the pinned log
     * @param description the description for the pinned log
     * @return true if the operation was successful, false otherwise
     */
    public boolean pinLogDocumentById(String documentId, String pinnerId, String name, String description) {
        logger.trace().message(AppConstants.LOG_ATTEMPTING_TO_PIN_LOG_USING_DOCUMENT_ID)
                .detail("documentId", documentId)
                .detail("pinnerId", pinnerId)
                .log();

        try {
            // Prepare the update document
            Map<String, Object> updateDoc = new HashMap<>();
            updateDoc.put("isPinned", true);
            updateDoc.put("pinnedAt", System.currentTimeMillis());
            updateDoc.put("pinnerId", pinnerId);
            updateDoc.put("pinned.name", name);
            updateDoc.put("pinned.description", description);
            updateDoc.put("pinnedAt", System.currentTimeMillis());
            updateDoc.put("updatedAt", System.currentTimeMillis());

            // Use OpenSearchService to update the document
            boolean success = openSearchService.updateLogDocument(documentId, updateDoc);

            if (success) {
                logger.info().message(AppConstants.LOG_SUCCESSFULLY_PINNED_LOG_USING_DOCUMENT_ID)
                        .detail("documentId", documentId)
                        .log();
                return true;
            } else {
                logger.error().message(AppConstants.LOG_FAILED_TO_PIN_LOG_USING_DOCUMENT_ID)
                        .detail("documentId", documentId)
                        .log();
                return false;
            }

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_EXCEPTION_WHILE_PINNING_LOG_USING_DOCUMENT_ID)
                    .detail("documentId", documentId)
                    .detail("pinnerId", pinnerId)
                    .log();
            return false;
        }
    }

    /**
     * Unpins a log document using direct document ID access (optimized).
     * Instead of searching for the document, uses the OpenSearch document _id
     * directly.
     *
     * @param documentId the OpenSearch document _id
     * @param unpinnerId the ID of the user who is unpinning the log
     * @return true if the operation was successful, false otherwise
     */
    public boolean unpinLogDocumentById(String documentId, String unpinnerId) {
        logger.trace().message(AppConstants.LOG_ATTEMPTING_TO_UNPIN_LOG_USING_DOCUMENT_ID)
                .detail("documentId", documentId)
                .detail("unpinnerId", unpinnerId)
                .log();

        try {
            // Prepare the update document
            Map<String, Object> updateDoc = new HashMap<>();
            updateDoc.put("isPinned", false);
            updateDoc.put("pinnedAt", null);
            updateDoc.put("pinnerId", null);
            updateDoc.put("pinned.name", null);
            updateDoc.put("pinned.description", null);

            updateDoc.put("isResolved", false);
            updateDoc.put("resolvedAt", null);
            updateDoc.put("resolverId", null);
            updateDoc.put("updatedAt", null);
            updateDoc.put("updatedBy", null);

            // Use OpenSearchService to update the document
            boolean success = openSearchService.updateLogDocument(documentId, updateDoc);

            if (success) {
                logger.info().message(AppConstants.LOG_SUCCESSFULLY_UNPINNED_LOG_USING_DOCUMENT_ID)
                        .detail("documentId", documentId)
                        .log();
                return true;
            } else {
                logger.error().message(AppConstants.LOG_FAILED_TO_UNPIN_LOG_USING_DOCUMENT_ID)
                        .detail("documentId", documentId)
                        .log();
                return false;
            }

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_EXCEPTION_WHILE_UNPINNING_LOG_USING_DOCUMENT_ID)
                    .detail("documentId", documentId)
                    .detail("unpinnerId", unpinnerId)
                    .log();
            return false;
        }
    }

    /**
     * Checks if a document is pinned using direct document ID access (optimized).
     * Instead of searching, gets the document directly by its _id.
     *
     * @param documentId the OpenSearch document _id
     * @return true if the document exists and is pinned, false otherwise
     */
    public boolean isDocumentPinnedById(String documentId) {
        logger.trace().message(AppConstants.LOG_CHECKING_IF_DOCUMENT_IS_PINNED_USING_DOCUMENT_ID)
                .detail("documentId", documentId)
                .log();

        try {
            // Search for the document by its _id using ids query (recommended for _id
            // searches)
            Map<String, Object> searchQuery = new HashMap<>();
            Map<String, Object> idsQuery = new HashMap<>();
            idsQuery.put("values", new String[] { documentId });
            searchQuery.put("query", Map.of("ids", idsQuery));
            searchQuery.put("size", 1);

            JsonNode searchResult = openSearchService.searchDocuments("main-logs", searchQuery);

            if (searchResult != null && searchResult.has("hits")) {
                JsonNode hits = searchResult.path("hits").path("hits");
                if (hits.size() > 0) {
                    JsonNode source = hits.get(0).path("_source");
                    boolean isPinned = source.path("isPinned").asBoolean(false);

                    logger.debug().message(AppConstants.LOG_DOCUMENT_PINNED_STATUS_CHECKED)
                            .detail("documentId", documentId)
                            .detail("isPinned", isPinned)
                            .log();

                    return isPinned;
                }
            }

            logger.debug().message(AppConstants.LOG_DOCUMENT_NOT_FOUND)
                    .detail("documentId", documentId)
                    .log();
            return false;

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_EXCEPTION_WHILE_CHECKING_IF_DOCUMENT_IS_PINNED)
                    .detail("documentId", documentId)
                    .log();
            return false;
        }
    }

    /**
     * Searches for pinned logs in OpenSearch and includes document IDs.
     * This optimized version returns documents with their _id field for direct
     * operations.
     *
     * @return JsonNode containing the search results with document IDs
     */
    public JsonNode searchPinnedLogsWithDocumentIds() {
        logger.trace().message(AppConstants.LOG_SEARCHING_FOR_PINNED_LOGS_WITH_DOCUMENT_IDS).log();

        try {
            // Build search query for pinned logs
            Map<String, Object> searchQuery = new HashMap<>();
            Map<String, Object> query = new HashMap<>();
            Map<String, Object> termQuery = new HashMap<>();
            termQuery.put("isPinned", true);
            query.put("term", termQuery);
            searchQuery.put("query", query);

            // Sort by pinnedAt descending
            Map<String, Object> sort = new HashMap<>();
            sort.put("pinnedAt", Map.of("order", "desc"));
            searchQuery.put("sort", new Object[] { sort });

            // Include document ID in response
            searchQuery.put("_source", Map.of("includes", new String[] { "*" }));

            JsonNode result = openSearchService.searchDocuments("main-logs", searchQuery);

            if (result != null) {
                logger.info().message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_PINNED_LOGS_WITH_DOCUMENT_IDS).log();
                return result;
            } else {
                logger.error().message(AppConstants.LOG_FAILED_TO_SEARCH_PINNED_LOGS_WITH_DOCUMENT_IDS).log();
                return null;
            }

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_EXCEPTION_WHILE_SEARCHING_PINNED_LOGS_WITH_DOCUMENT_IDS).log();
            return null;
        }
    }
}
