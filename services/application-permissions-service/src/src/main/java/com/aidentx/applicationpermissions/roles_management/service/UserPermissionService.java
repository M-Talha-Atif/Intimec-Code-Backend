package com.aidentx.applicationpermissions.roles_management.service;

import com.aidentx.applicationpermissions.roles_management.model.dto.PageAccessResponse;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for checking user permissions via gRPC.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public interface UserPermissionService {
    
    /**
     * Checks if a user has permission to access a specific endpoint with given HTTP method.
     * 
     * @param userId the user ID to check permissions for
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param apiEndpoint the API endpoint (e.g., "/api/v1/audit-logs")
     * @param httpMethod the HTTP method (e.g., "GET", "POST")
     * @return true if user has permission, false otherwise
     */
    boolean hasPermissionForEndpoint(String userId, String tenantId, String apiEndpoint, String httpMethod);
    
    /**
     * Checks if an endpoint is managed by the permission system.
     * 
     * @param apiEndpoint the API endpoint (e.g., "/api/v1/audit-logs")
     * @param httpMethod the HTTP method (e.g., "GET", "POST") 
     * @return true if endpoint is managed by permission system, false otherwise
     */
    boolean isEndpointManaged(String apiEndpoint, String httpMethod);
    
    /**
     * Gets page access information for a user across all system pages.
     * 
     * @param userId the user ID to check permissions for
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return list of page access responses containing page details and access status
     */
    List<PageAccessResponse> getUserPageAccess(String userId, String tenantId);
    
    /**
     * Checks if a user has permission for a specific action.
     * 
     * @param userId the user ID to check permissions for
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param actionId the action ID to check
     * @return true if user has permission for the action, false otherwise
     */
    boolean checkActionPermission(String userId, String tenantId, UUID actionId);
}