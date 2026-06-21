package com.aidentx.core.responsibilities.client;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * REST client for communicating with user-service.
 * Fetches user data including user_id by identity_id mapping.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
public class UserServiceRestClient {
    
    private final RestTemplate restTemplate;
    private final IUnifiedLogger logger;
    
    @Value("${gateway.auth.service.url:http://localhost:8090}")
    private String userServiceUrl;
    
    public UserServiceRestClient(RestTemplate restTemplate, UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.logger = loggerFactory.getLogger(UserServiceRestClient.class, "UserServiceRestClient");
    }
    
    /**
     * Get user_id from identity_id.
     * 
     * @param identityId Identity ID from identity-service
     * @param tenantId Tenant ID
     * @param authToken JWT Authorization token
     * @return User ID (UUID) or null if not found
     */
    public UUID getUserIdByIdentityId(UUID identityId, String tenantId, String authToken) {
        try {
            logger.info()
                .message("Fetching user_id by identity_id from user-service")
                .detail("identityId", identityId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            String url = userServiceUrl + "/api/v1/users/by-identity/" + identityId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-ID", tenantId);
            if (authToken != null && !authToken.isEmpty()) {
                headers.set("Authorization", authToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<java.util.Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                java.util.Map.class
            );
            
            java.util.Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && "OK".equals(responseBody.get("status")) && responseBody.containsKey("userId")) {
                String userId = responseBody.get("userId").toString();
                
                logger.info()
                    .message("Successfully fetched user_id by identity_id")
                    .detail("identityId", identityId.toString())
                    .detail("userId", userId)
                    .log();
                
                return UUID.fromString(userId);
            }
            
            logger.warn()
                .message("No user found for identity_id")
                .detail("identityId", identityId.toString())
                .log();
            
            return null;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch user_id by identity_id from user-service")
                .detail("identityId", identityId.toString())
                .detail("error", e.getMessage())
                .log();
            
            // Return null on error - will use identity_id as fallback
            return null;
        }
    }
    
    /**
     * Get complete user data by identity_id.
     * 
     * @param identityId Identity ID from identity-service
     * @param tenantId Tenant ID
     * @param authToken JWT Authorization token
     * @return Map containing complete user data or null if not found
     */
    public java.util.Map<String, Object> getUserByIdentityId(UUID identityId, String tenantId, String authToken) {
        try {
            logger.info()
                .message("Fetching user data by identity_id from user-service")
                .detail("identityId", identityId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            String url = userServiceUrl + "/api/v1/users/by-identity/" + identityId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-ID", tenantId);
            if (authToken != null && !authToken.isEmpty()) {
                headers.set("Authorization", authToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<java.util.Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                java.util.Map.class
            );
            
            java.util.Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && "OK".equals(responseBody.get("status"))) {
                logger.info()
                    .message("Successfully fetched user data by identity_id")
                    .detail("identityId", identityId.toString())
                    .log();
                
                return responseBody;
            }
            
            logger.warn()
                .message("No user found for identity_id")
                .detail("identityId", identityId.toString())
                .log();
            
            return null;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch user data by identity_id from user-service")
                .detail("identityId", identityId.toString())
                .detail("error", e.getMessage())
                .log();
            
            return null;
        }
    }
}
