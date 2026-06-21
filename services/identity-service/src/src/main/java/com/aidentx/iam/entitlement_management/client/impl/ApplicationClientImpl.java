package com.aidentx.iam.entitlement_management.client.impl;

import com.aidentx.iam.entitlement_management.client.ApplicationClient;
import com.aidentx.iam.entitlement_management.config.constants.ApplicationClientConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link ApplicationClient} interface for fetching application information.
 *
 * <p>This client communicates with the application service through the API gateway to retrieve
 * application names associated with resource identifiers. It handles the REST API calls,
 * authentication propagation, and error handling with graceful fallbacks.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Bulk fetching of application names by resource IDs</li>
 *   <li>Automatic propagation of authentication tokens from incoming requests</li>
 *   <li>Graceful degradation with empty map returns on failures</li>
 *   <li>Comprehensive logging for debugging and monitoring</li>
 *   <li>Configurable base URL via environment variables</li>
 * </ul>
 *
 * <p>The client uses the API gateway's bulk endpoint to efficiently resolve multiple
 * application names in a single request, reducing network overhead and improving performance.</p>
 *
 * @author aidentx Development Team
 * @since 1.0
 * @see ApplicationClient
 * @see RestTemplate
 * @see ApplicationClientConstants
 */
@Component
public class ApplicationClientImpl implements ApplicationClient {

    private final RestTemplate restTemplate;
    private final IUnifiedLogger logger;
    private final String baseUrl;

    /**
     * Constructs a new ApplicationClientImpl with the required dependencies.
     *
     * <p>Initializes the client with a configured RestTemplate for HTTP communication,
     * a unified logger for structured logging, and a base URL for the API gateway.
     * The base URL can be overridden through environment variables or application properties.</p>
     *
     * @param restTemplate the configured RestTemplate for making HTTP requests
     * @param loggerFactory the factory for creating structured loggers
     * @param baseUrl the base URL of the API gateway, defaults to http://localhost:8081
     *                if not specified in properties or environment variables
     */
    public ApplicationClientImpl(RestTemplate restTemplate,
                                 UnifiedLoggerFactory loggerFactory,
                                 @Value("${" + ApplicationClientConstants.CONFIG_API_GATEWAY_URL +
                                       ":" + ApplicationClientConstants.DEFAULT_GATEWAY_URL + "}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.logger = loggerFactory.getLogger("ApplicationClient", "entitlement-management");
        this.baseUrl = baseUrl;
    }

    /**
     * Retrieves application names for the specified resource identifiers.
     *
     * <p>This method makes a bulk POST request to the application service through the API gateway
     * to fetch application names for multiple resource IDs in a single call. It automatically
     * propagates the authentication token from the current request context to ensure proper
     * authorization through the API gateway.</p>
     *
     * <p>Error handling:</p>
     * <ul>
     *   <li>If the resourceIds list is null or empty, returns an empty map immediately</li>
     *   <li>If any exception occurs during the API call, logs the error and returns an empty map</li>
     *   <li>This ensures the entitlement service continues functioning even if the application
     *       service is temporarily unavailable</li>
     * </ul>
     *
     * <p>Logging is implemented at multiple levels:</p>
     * <ul>
     *   <li>DEBUG: Request details including resource count and authentication token presence</li>
     *   <li>INFO: Successful resolution with count of resolved applications</li>
     *   <li>ERROR: Any failures during the API call</li>
     * </ul>
     *
     * @param tenantId the tenant identifier, used for multi-tenancy isolation
     * @param resourceIds the list of resource (application) UUIDs to look up
     * @return a map where keys are resource UUIDs and values are corresponding application names,
     *         or an empty map if no matches found or an error occurred
     */
    @Override
    public Map<UUID, String> getApplicationNames(String tenantId, List<UUID> resourceIds) {

        if (resourceIds == null || resourceIds.isEmpty()) {
            logger.debug().message(ApplicationClientConstants.LOG_EMPTY_RESOURCE_IDS).log();
            return Collections.emptyMap();
        }

        try {
            logger.debug().message(ApplicationClientConstants.LOG_FETCHING_APPLICATION_NAMES)
                    .detail(ApplicationClientConstants.LOG_DETAIL_COUNT, resourceIds.size())
                    .log();

            // Build the complete URL by appending service path to base gateway URL
            String url = baseUrl + ApplicationClientConstants.APPLICATION_BASE_PATH +
                    ApplicationClientConstants.BULK_ENDPOINT;

            // Prepare the request body with the list of IDs
            Map<String, Object> request = Map.of(ApplicationClientConstants.REQUEST_IDS_FIELD, resourceIds);

            // Set up HTTP headers with tenant information and authentication token
            var headers = new org.springframework.http.HttpHeaders();
            headers.set(ApplicationClientConstants.TENANT_HEADER, tenantId);

            // Propagate authorization token from current request to maintain security context
            String authToken = getCurrentAuthToken();
            if (authToken != null) {
                headers.set(ApplicationClientConstants.AUTHORIZATION_HEADER, authToken);
                logger.debug().message(ApplicationClientConstants.LOG_AUTH_TOKEN_ADDED).log();
            } else {
                logger.warn().message(ApplicationClientConstants.LOG_AUTH_TOKEN_MISSING).log();
            }

            var entity = new org.springframework.http.HttpEntity<>(request, headers);

            // Execute the POST request to the bulk endpoint
            var response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, List<Map<String, Object>>>>() {
                    }
            );

            // Extract the applications list from the response body
            List<Map<String, Object>> apps =
                    (List<Map<String, Object>>) response.getBody().get(ApplicationClientConstants.RESPONSE_APPLICATIONS_FIELD);

            // Transform the response into a map of UUID to application name
            Map<UUID, String> result = apps.stream()
                    .collect(Collectors.toMap(
                            app -> UUID.fromString((String) app.get(ApplicationClientConstants.APPLICATION_ID_FIELD)),
                            app -> (String) app.get(ApplicationClientConstants.APPLICATION_NAME_FIELD)
                    ));

            logger.info().message(ApplicationClientConstants.LOG_APPLICATION_NAMES_RESOLVED)
                    .detail(ApplicationClientConstants.LOG_DETAIL_RESOLVED_COUNT, result.size())
                    .detail(ApplicationClientConstants.LOG_DETAIL_REQUESTED_COUNT, resourceIds.size())
                    .log();

            return result;

        } catch (Exception e) {
            // Log the error and return empty map for graceful degradation
            logger.error(e).message(ApplicationClientConstants.LOG_FAILED_TO_FETCH)
                    .detail(ApplicationClientConstants.LOG_DETAIL_TENANT_ID, tenantId)
                    .detail(ApplicationClientConstants.LOG_DETAIL_RESOURCE_COUNT, resourceIds.size())
                    .log();
            return Collections.emptyMap(); // safe fallback to maintain service availability
        }
    }

    /**
     * Retrieves the authentication token from the current HTTP request context.
     *
     * <p>This helper method extracts the Authorization header from the current request
     * to propagate authentication credentials to downstream service calls. This ensures
     * that the API gateway can properly authenticate and authorize the request.</p>
     *
     * <p>The method safely handles cases where no request context is available (e.g., when
     * called from background threads or scheduled tasks) by returning null.</p>
     *
     * @return the value of the Authorization header from the current request, or null if
     *         no request context is available or no Authorization header is present
     */
    private String getCurrentAuthToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader(ApplicationClientConstants.AUTHORIZATION_HEADER);
            if (authHeader != null && !authHeader.isEmpty()) {
                return authHeader;
            }
        }
        return null;
    }
}