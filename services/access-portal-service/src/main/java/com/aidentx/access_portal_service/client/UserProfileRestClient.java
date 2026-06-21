package com.aidentx.access_portal_service.client;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * REST client for user profile service.
 * Fetches user information from the authentication service.
 */
@Service
public class UserProfileRestClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    @Value("${auth.service.rest.url:http://localhost:8081}")
    private String authServiceUrl;

    @Autowired
    public UserProfileRestClient(RestTemplate restTemplate, UnifiedLoggerFactory loggerFactory, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(
                getClass(),
                AccessPortalConstants.LOG_CATEGORY_EXTERNAL_SERVICE
        );
    }


    /**
     * Fetches the display name for the current user from the auth service.
     * Returns null if the user cannot be found or if an error occurs.
     *
     * @param jwtToken JWT token for authentication
     * @return display name or null
     */
    public String getCurrentUserDisplayName(String jwtToken) {
        if (jwtToken == null || jwtToken.isBlank()) {
            logger.warn()
                    .message("Cannot fetch user display name - JWT token is null or empty")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_PROFILE_FETCH_ERROR)
                    .log();
            return null;
        }

        try {
            String url = authServiceUrl + "/api/v1/auth/current";

            logger.debug()
                    .message("Fetching current user profile from auth service")
                    .detail("url", url)
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_PROFILE_FETCH_START)
                    .log();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String displayName = jsonNode.path("displayName").asText(null);

                logger.info()
                        .message("Successfully fetched user display name")
                        .detail("displayName", displayName != null ? displayName : "null")
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_PROFILE_FETCH_SUCCESS)
                        .log();

                return displayName;
            } else {
                logger.warn()
                        .message("Failed to fetch user profile - unexpected response status")
                        .detail("statusCode", String.valueOf(response.getStatusCode().value()))
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_PROFILE_FETCH_ERROR)
                        .log();

                return null;
            }

        } catch (Exception e) {
            logger.error()
                    .message("Error fetching user display name from auth service")
                    .detail("error", e.getMessage())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_PROFILE_FETCH_ERROR)
                    .log();

            return null;
        }
    }
}