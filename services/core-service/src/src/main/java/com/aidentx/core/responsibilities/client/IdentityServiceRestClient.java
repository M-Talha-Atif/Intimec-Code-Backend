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
 * REST client for communicating with identity-service.
 * Fetches employee and identity data for team member queries.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
public class IdentityServiceRestClient {
    
    private final RestTemplate restTemplate;
    private final IUnifiedLogger logger;
    
    @Value("${identity-service.url:http://identity-service:8094}")
    private String identityServiceUrl;
    
    public IdentityServiceRestClient(RestTemplate restTemplate, UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.logger = loggerFactory.getLogger(IdentityServiceRestClient.class, "IdentityServiceRestClient");
    }
    
    /**
     * Get employees (subordinates) managed by a specific manager.
     * 
     * @param managerId Manager's identity ID
     * @param tenantId Tenant ID
     * @param authToken JWT Authorization token
     * @return List of employee identity objects
     */
    public List<Map<String, Object>> getEmployeesByManager(UUID managerId, String tenantId, String authToken) {
        try {
            logger.info()
                .message("Fetching employees by manager from identity-service")
                .detail("managerId", managerId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            String url = identityServiceUrl + "/api/v1/identity/employees/by-manager/" + managerId.toString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-ID", tenantId);
            if (authToken != null && !authToken.isEmpty()) {
                headers.set("Authorization", authToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                List.class
            );
            
            List<Map<String, Object>> employees = response.getBody();
            
            logger.info()
                .message("Successfully fetched employees by manager")
                .detail("managerId", managerId.toString())
                .detail("count", employees != null ? String.valueOf(employees.size()) : "0")
                .log();
            
            return employees;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch employees from identity-service")
                .detail("managerId", managerId.toString())
                .detail("error", e.getMessage())
                .log();
            
            // Return empty list on error (service might be unavailable)
            return List.of();
        }
    }
    
    /**
     * Get identity details by identity ID.
     * 
     * @param identityId Identity ID
     * @param tenantId Tenant ID
     * @param authToken JWT Authorization token
     * @return Identity details
     */
    public Map<String, Object> getIdentityById(UUID identityId, String tenantId, String authToken) {
        try {
            logger.info()
                .message("Fetching identity by ID from identity-service")
                .detail("identityId", identityId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            String url = identityServiceUrl + "/api/v1/identities/" + identityId.toString();
            
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
            
            Map<String, Object> identity = response.getBody();
            
            logger.info()
                .message("Successfully fetched identity")
                .detail("identityId", identityId.toString())
                .log();
            
            return identity;
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch identity from identity-service")
                .detail("identityId", identityId.toString())
                .detail("error", e.getMessage())
                .log();
            
            return Map.of();
        }
    }
}
