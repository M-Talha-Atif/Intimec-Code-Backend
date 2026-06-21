package com.aidentx.core.opensearch.service;

import com.aidentx.core.log_management.config.AppConstants;
import com.aidentx.core.opensearch.config.OpenSearchConfig;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for performing OpenSearch operations on log documents.
 * Handles pinning and unpinning logs by updating documents in OpenSearch.
 */
@Service
public class OpenSearchService {

    private final RestTemplate restTemplate;
    private final OpenSearchConfig.OpenSearchProperties openSearchProperties;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    public OpenSearchService(RestTemplate restTemplate,
            OpenSearchConfig.OpenSearchProperties openSearchProperties,
            UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.openSearchProperties = openSearchProperties;
        this.objectMapper = new ObjectMapper();
        this.logger = loggerFactory.getLogger("OpenSearchService", "opensearch-operations");
    }

    /**
     * Gets a document by its OpenSearch document ID.
     * This is a general OpenSearch operation.
     *
     * @param documentId the OpenSearch document _id
     * @param index      the index name
     * @return JsonNode containing the document, or null if not found
     */
    public JsonNode getDocumentById(String documentId, String index) {
        logger.trace().message(AppConstants.LOG_GETTING_DOCUMENT_BY_ID)
                .detail("documentId", documentId)
                .detail("index", index)
                .log();

        try {
            String url = openSearchProperties.getUrl() + "/" + index + "/_doc/" + documentId;
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.debug().message(AppConstants.LOG_DOCUMENT_RETRIEVED_SUCCESSFULLY)
                        .detail("documentId", documentId)
                        .detail("index", index)
                        .log();
                return objectMapper.readTree(response.getBody());
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.debug().message(AppConstants.LOG_DOCUMENT_NOT_FOUND_OPENSEARCH)
                        .detail("documentId", documentId)
                        .detail("index", index)
                        .log();
                return null;
            } else {
                logger.error().message(AppConstants.LOG_FAILED_TO_GET_DOCUMENT)
                        .detail("documentId", documentId)
                        .detail("index", index)
                        .detail("statusCode", response.getStatusCode())
                        .log();
                return null;
            }

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_EXCEPTION_WHILE_GETTING_DOCUMENT)
                    .detail("documentId", documentId)
                    .detail("index", index)
                    .log();
            return null;
        }
    }

    /**
     * Updates a document by its OpenSearch document ID.
     * This is a general OpenSearch operation.
     *
     * @param documentId the OpenSearch document _id
     * @param index      the index name
     * @param updates    the update document
     * @return true if the operation was successful, false otherwise
     */
    public boolean updateDocumentById(String documentId, String index, Map<String, Object> updates) {
        logger.trace().message(AppConstants.LOG_UPDATING_DOCUMENT_BY_ID)
                .detail("documentId", documentId)
                .detail("index", index)
                .log();

        try {
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("doc", updates);

            String url = openSearchProperties.getUrl() + "/" + index + "/_update/" + documentId;
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updateRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info().message(AppConstants.LOG_DOCUMENT_UPDATED_SUCCESSFULLY)
                        .detail("documentId", documentId)
                        .detail("index", index)
                        .log();
                return true;
            } else {
                logger.error().message(AppConstants.LOG_FAILED_TO_UPDATE_DOCUMENT)
                        .detail("documentId", documentId)
                        .detail("index", index)
                        .detail("statusCode", response.getStatusCode())
                        .log();
                return false;
            }

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_EXCEPTION_WHILE_UPDATING_DOCUMENT)
                    .detail("documentId", documentId)
                    .detail("index", index)
                    .log();
            return false;
        }
    }

    /**
     * Creates HTTP headers with authentication for OpenSearch requests.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (openSearchProperties.getUsername() != null && openSearchProperties.getPassword() != null) {
            String auth = openSearchProperties.getUsername() + ":" + openSearchProperties.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
        }

        return headers;
    }

    /**
     * Updates a log document in OpenSearch with provided fields.
     *
     * @param id      the ID of the log document to update
     * @param updates the map of field updates to apply
     * @return true if the operation was successful, false otherwise
     */
    public boolean updateLogDocument(String documentId, Map<String, Object> updates) {
        logger.trace().message(AppConstants.LOG_ATTEMPTING_TO_UPDATE_LOG_DOCUMENT)
                .detail("documentId", documentId)
                .detail("updates", updates)
                .log();

        try {
            // Use direct document ID update instead of search-based update
            return updateDocumentById(documentId, "main-logs", updates);

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_UPDATE_LOG_DOCUMENT)
                    .detail("documentId", documentId)
                    .detail("updates", updates)
                    .log();
            return false;
        }
    }

    /**
     * Checks if a document exists by its OpenSearch document ID (direct access).
     * This is more efficient than searching by log ID.
     *
     * @param documentId the OpenSearch document _id
     * @param index      the index name
     * @return true if document exists, false otherwise
     */
    public boolean documentExistsById(String documentId, String index) {
        logger.trace().message(AppConstants.LOG_CHECKING_IF_DOCUMENT_EXISTS_BY_ID)
                .detail("documentId", documentId)
                .detail("index", index)
                .log();

        try {
            String url = openSearchProperties.getUrl() + "/" + index + "/_doc/" + documentId;
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.HEAD, entity, String.class);

            boolean exists = response.getStatusCode() == HttpStatus.OK;

            logger.debug().message(AppConstants.LOG_DOCUMENT_EXISTENCE_CHECK_COMPLETED)
                    .detail("documentId", documentId)
                    .detail("index", index)
                    .detail("exists", exists)
                    .log();

            return exists;

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_EXCEPTION_WHILE_CHECKING_DOCUMENT_EXISTENCE_BY_ID)
                    .detail("documentId", documentId)
                    .detail("index", index)
                    .log();
            return false;
        }
    }

    /**
     * Searches documents in a specified index with a custom query.
     * This is a general OpenSearch operation.
     *
     * @param index       the index name to search in
     * @param searchQuery the search query to execute
     * @return JsonNode containing search results
     */
    public JsonNode searchDocuments(String index, Map<String, Object> searchQuery) {
        logger.trace().message(AppConstants.LOG_SEARCHING_DOCUMENTS)
                .detail("index", index)
                .log();

        try {
            String url = openSearchProperties.getUrl() + "/" + index + "/_search";
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(searchQuery, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readTree(response.getBody());
            } else {
                logger.error().message(AppConstants.LOG_FAILED_TO_SEARCH_DOCUMENTS)
                        .detail("index", index)
                        .detail("statusCode", response.getStatusCode())
                        .log();
                return null;
            }
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_EXCEPTION_WHILE_SEARCHING_DOCUMENTS)
                    .detail("index", index)
                    .log();
            return null;
        }
    }
}
