package com.aidentx.applicationpermissions.roles_management.service;

import com.aidentx.applicationpermissions.roles_management.enums.HttpMethod;
import com.aidentx.applicationpermissions.roles_management.model.dto.HierarchicalPermissionsResponse;
import com.aidentx.applicationpermissions.roles_management.model.entity.Action;
import com.aidentx.applicationpermissions.roles_management.model.entity.Module;
import com.aidentx.applicationpermissions.roles_management.model.entity.Page;

import java.util.UUID;

/**
 * Service interface for managing granular permissions components.
 * 
 * <p>This service provides operations for creating system-wide permission entities
 * (modules, pages, and actions) with tenantId = null. These entities serve as
 * base templates available to all tenants in the system.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public interface GranularPermissionService {

    /**
     * Creates a system-wide module with tenantId = null.
     * 
     * <p>System modules are available to all tenants and serve as base
     * organizational units for grouping related pages and actions.</p>
     * 
     * @param moduleName the name of the module to create
     * @return the created module entity
     * @throws IllegalArgumentException if moduleName is null or empty
     */
    Module createSystemModule(String moduleName);
    
    /**
     * Creates a system-wide page with tenantId = null.
     * 
     * <p>System pages are available to all tenants and represent
     * UI components or sections within the application.</p>
     * 
     * @param pageName the name of the page to create
     * @param moduleId the ID of the module this page belongs to (optional)
     * @return the created page entity
     * @throws IllegalArgumentException if pageName is null or empty
     */
    Page createSystemPage(String pageName, UUID moduleId);
    
    /**
     * Creates a system-wide page with tenantId = null and page path.
     * 
     * <p>System pages are available to all tenants and represent
     * UI components or sections within the application.</p>
     * 
     * @param pageName the name of the page to create
     * @param pagePath the URL path or route for this page (optional)
     * @param moduleId the ID of the module this page belongs to (optional)
     * @return the created page entity
     * @throws IllegalArgumentException if pageName is null or empty
     */
    Page createSystemPage(String pageName, String pagePath, UUID moduleId);
    
    /**
     * Creates a system-wide action with tenantId = null.
     * 
     * <p>System actions are available to all tenants and represent
     * specific operations or permissions that can be granted.</p>
     * 
     * @param actionName the name of the action to create
     * @param endpoint the API endpoint associated with this action (optional)
     * @param method the HTTP method for this action (optional)
     * @param pageId the ID of the page this action belongs to (optional)
     * @return the created action entity
     * @throws IllegalArgumentException if actionName is null or empty
     */
    Action createSystemAction(String actionName, String endpoint, HttpMethod method, UUID pageId);
    
    /**
     * Gets the hierarchical permissions structure.
     * 
     * @return the complete hierarchical permissions response
     */
    HierarchicalPermissionsResponse getHierarchicalPermissions();
    
    /**
     * Gets the hierarchical permissions structure filtered by module name.
     * 
     * @param moduleName the name of the module to filter by
     * @return the filtered hierarchical permissions response
     */
    HierarchicalPermissionsResponse getHierarchicalPermissions(String moduleName);
    
    /**
     * Gets the hierarchical permissions structure filtered by module name and role permissions.
     * 
     * @param moduleName the name of the module to filter by
     * @param roleId the role ID to check permissions for (adds hasRole field to actions)
     * @return the filtered hierarchical permissions response with role permission status
     */
    HierarchicalPermissionsResponse getHierarchicalPermissions(String moduleName, String roleId);
}