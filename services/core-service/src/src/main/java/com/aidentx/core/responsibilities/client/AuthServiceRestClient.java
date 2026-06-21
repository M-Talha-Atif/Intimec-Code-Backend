package com.aidentx.core.responsibilities.client;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for communicating with auth-service.
 * Fetches user authentication and profile data.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
public class AuthServiceRestClient {
    
    private final RestTemplate restTemplate;
    private final IUnifiedLogger logger;
    
    @Value("${auth-service.url:http://auth-service:8090}")
    private String authServiceUrl;
    
    public AuthServiceRestClient(RestTemplate restTemplate, UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.logger = loggerFactory.getLogger(AuthServiceRestClient.class, "AuthServiceRestClient");
    }
    
    /**
     * Get user details including identityId.
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param authToken JWT Authorization token
     * @return User details map
     */
    public Map<String, Object> getUserById(UUID userId, String tenantId, String authToken) {
        try {
            logger.info()
                .message("Fetching user by ID from auth-service")
                .detail("userId", userId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            String url = authServiceUrl + "/api/v1/users/" + userId.toString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-ID", tenantId);
            if (authToken != null && !authToken.isEmpty()) {
                headers.set("Authorization", authToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            Map<String, Object> user = response.getBody();
            
            logger.info()
                .message("Successfully fetched user")
                .detail("userId", userId.toString())
                .log();
            
            return user;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch user from auth-service")
                .detail("userId", userId.toString())
                .detail("error", e.getMessage())
                .log();
            
            return Map.of();
        }
    }
    
    /**
     * Get users by their identity IDs (batch operation).
     * 
     * @param identityIds List of identity IDs
     * @param tenantId Tenant ID
     * @param authToken JWT Authorization token
     * @return List of user details
     */
    public List<Map<String, Object>> getUsersByIdentityIds(List<UUID> identityIds, String tenantId, String authToken) {
        try {
            logger.info()
                .message("Fetching users by identity IDs from auth-service")
                .detail("count", String.valueOf(identityIds.size()))
                .detail("tenantId", tenantId)
                .log();
            
            String url = authServiceUrl + "/api/v1/users/by-identity-ids";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-ID", tenantId);
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authToken != null && !authToken.isEmpty()) {
                headers.set("Authorization", authToken);
            }
            
            HttpEntity<List<UUID>> entity = new HttpEntity<>(identityIds, headers);
            
            ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                List.class
            );
            
            List<Map<String, Object>> users = response.getBody();
            
            logger.info()
                .message("Successfully fetched users by identity IDs")
                .detail("requestCount", String.valueOf(identityIds.size()))
                .detail("resultCount", users != null ? String.valueOf(users.size()) : "0")
                .log();
            
            return users;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch users from auth-service")
                .detail("error", e.getMessage())
                .log();
            
            return List.of();
        }
    }
}
