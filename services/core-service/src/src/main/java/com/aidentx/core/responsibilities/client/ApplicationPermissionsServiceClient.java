package com.aidentx.core.responsibilities.client;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * REST client for communicating with application-permissions-service.
 * Fetches role and permission data for the Responsibilities page.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
public class ApplicationPermissionsServiceClient {
    
    private final RestTemplate restTemplate;
    private final IUnifiedLogger logger;
    
    @Value("${application-permissions-service.url:http://localhost:8095}")
    private String applicationPermissionsServiceUrl;
    
    public ApplicationPermissionsServiceClient(RestTemplate restTemplate, UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.logger = loggerFactory.getLogger(ApplicationPermissionsServiceClient.class, "ApplicationPermissionsServiceClient");
    }
    
    /**
     * Get roles owned by the current user.
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param authToken JWT Authorization token
     * @return OwnedRolesResponse containing roles owned by user
     */
    public Object getOwnedRoles(UUID userId, String tenantId, String authToken) {
        try {
            logger.info()
                .message("Fetching owned roles from application-permissions-service")
                .detail("userId", userId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            String url = applicationPermissionsServiceUrl + "/api/v1/roles/owned-by-me";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-ID", userId.toString());
            headers.set("X-Tenant-ID", tenantId);
            if (authToken != null && !authToken.isEmpty()) {
                headers.set("Authorization", authToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Object.class
            );
            
            Object ownedRoles = response.getBody();
            
            logger.info()
                .message("Successfully fetched owned roles")
                .detail("userId", userId.toString())
                .log();
            
            return ownedRoles;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch owned roles from application-permissions-service")
                .detail("userId", userId.toString())
                .detail("error", e.getMessage())
                .log();
            throw new RuntimeException("Failed to fetch owned roles: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get roles assigned to a specific user.
     * 
     * @param userId User ID to fetch roles for
     * @param tenantId Tenant ID
     * @param authToken JWT Authorization token
     * @return List of roles assigned to the user
     */
    public java.util.List<java.util.Map<String, Object>> getUserRoles(UUID userId, String tenantId, String authToken) {
        try {
            logger.info()
                .message("Fetching user roles from application-permissions-service")
                .detail("userId", userId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            String url = applicationPermissionsServiceUrl + "/api/v1/user-roles/user/" + userId;
            
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
            if (responseBody != null && responseBody.containsKey("data")) {
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String, Object>> roles = 
                    (java.util.List<java.util.Map<String, Object>>) responseBody.get("data");
                
                logger.info()
                    .message("Successfully fetched user roles")
                    .detail("userId", userId.toString())
                    .detail("rolesCount", String.valueOf(roles != null ? roles.size() : 0))
                    .log();
                
                return roles != null ? roles : java.util.Collections.emptyList();
            }
            
            return java.util.Collections.emptyList();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch user roles from application-permissions-service")
                .detail("userId", userId.toString())
                .detail("error", e.getMessage())
                .log();
            
            // Return empty list on error instead of throwing
            return java.util.Collections.emptyList();
        }
    }
}
