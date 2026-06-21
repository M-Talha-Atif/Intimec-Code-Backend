package com.aidentx.applicationpermissions.roles_management.controller;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.model.dto.CreateActionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.CreateModuleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.CreatePageRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.HierarchicalPermissionsResponse;
import com.aidentx.applicationpermissions.roles_management.model.entity.Action;
import com.aidentx.applicationpermissions.roles_management.model.entity.Module;
import com.aidentx.applicationpermissions.roles_management.model.entity.Page;
import com.aidentx.applicationpermissions.roles_management.service.GranularPermissionService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing granular permission components.
 * 
 * <p>This controller provides endpoints for creating system-wide permission entities
 * (modules, pages, and actions) that serve as base templates available to all tenants.</p>
 * 
 * <p>All endpoints in this controller create entities with tenantId = null, making them
 * system-wide resources that can be used across all tenant environments.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/granular-permissions")
public class GranularPermissionController {

    private final GranularPermissionService granularPermissionService;
    private final IUnifiedLogger logger;

    /**
     * Constructor for GranularPermissionController.
     * 
     * @param granularPermissionService service for handling granular permission operations
     * @param loggerFactory factory for creating unified loggers
     */
    @Autowired
    public GranularPermissionController(GranularPermissionService granularPermissionService,
                                      UnifiedLoggerFactory loggerFactory) {
        this.granularPermissionService = granularPermissionService;
        this.logger = loggerFactory.getLogger(AppConstants.GRANULAR_PERMISSION_CONTROLLER_LOGGER_NAME, 
                                            AppConstants.LOG_CATEGORY_GRANULAR_PERMISSION_MANAGEMENT);
    }

    /**
     * Creates a system-wide module.
     * 
     * <p>The created module will have tenantId = null, making it available
     * as a base template for all tenants in the system.</p>
     * 
     * @param request the module creation request containing module name
     * @return ResponseEntity containing the created Module entity
     * @throws IllegalArgumentException if request validation fails
     */
    @PostMapping("/system/module")
    public ResponseEntity<?> createSystemModule(@Valid @RequestBody CreateModuleRequest request) {
        try {
            logger.info()
                .message(AppConstants.LOG_MODULE_CREATE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_MODULE_NAME, request.getModuleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE_REQUEST)
                .log();
            
            Module module = granularPermissionService.createSystemModule(request.getModuleName());
            
            logger.info()
                .message(AppConstants.LOG_MODULE_CREATED)
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, module.getId().toString())
                .detail(AppConstants.DETAIL_KEY_MODULE_NAME, module.getModuleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE)
                .log();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(module);
            
        } catch (IllegalArgumentException e) {
            logger.warn()
                .message(AppConstants.LOG_MODULE_VALIDATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_MODULE_NAME, request.getModuleName())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE_REQUEST)
                .log();
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_MODULE_CREATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_MODULE_NAME, request.getModuleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE_REQUEST)
                .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AppConstants.MODULE_CREATION_FAILED + ": " + e.getMessage());
        }
    }

    /**
     * Creates a system-wide page under a specific module.
     * 
     * <p>The created page will have tenantId = null, making it available
     * as a base template for all tenants in the system.</p>
     * 
     * @param moduleId the ID of the module this page belongs to
     * @param request the page creation request containing page name
     * @return ResponseEntity containing the created Page entity
     * @throws IllegalArgumentException if request validation fails
     */
    @PostMapping("/system/module/{moduleId}/page")
    public ResponseEntity<?> createSystemPage(@PathVariable UUID moduleId, 
                                            @Valid @RequestBody CreatePageRequest request) {
        try {
            logger.info()
                .message(AppConstants.LOG_PAGE_CREATE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, request.getPageName())
                .detail("pagePath", request.getPagePath())
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                .log();
            
            Page page = granularPermissionService.createSystemPage(request.getPageName(), request.getPagePath(), moduleId);
            
            logger.info()
                .message(AppConstants.LOG_PAGE_CREATED)
                .detail(AppConstants.DETAIL_KEY_PAGE_ID, page.getId().toString())
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, page.getPageName())
                .detail("pagePath", page.getPagePath())
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE)
                .log();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(page);
            
        } catch (IllegalArgumentException e) {
            logger.warn()
                .message(AppConstants.LOG_PAGE_VALIDATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, request.getPageName())
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                .log();
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_PAGE_CREATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, request.getPageName())
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AppConstants.PAGE_CREATION_FAILED + ": " + e.getMessage());
        }
    }

    /**
     * Creates a system-wide action under a specific page.
     * 
     * <p>The created action will have tenantId = null, making it available
     * as a base template for all tenants in the system.</p>
     * 
     * @param pageId the ID of the page this action belongs to
     * @param request the action creation request containing action name and endpoint
     * @return ResponseEntity containing the created Action entity
     * @throws IllegalArgumentException if request validation fails
     */
    @PostMapping("/system/page/{pageId}/action")
    public ResponseEntity<?> createSystemAction(@PathVariable UUID pageId, 
                                              @Valid @RequestBody CreateActionRequest request) {
        try {
            logger.info()
                .message(AppConstants.LOG_ACTION_CREATE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_ACTION_NAME, request.getActionName())
                .detail(AppConstants.DETAIL_KEY_ENDPOINT, request.getEndpoint())
                .detail("method", request.getMethod() != null ? request.getMethod().toString() : null)
                .detail(AppConstants.DETAIL_KEY_PAGE_ID, pageId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION_REQUEST)
                .log();
            
            Action action = granularPermissionService.createSystemAction(
                request.getActionName(), request.getEndpoint(), request.getMethod(), pageId);
            
            logger.info()
                .message(AppConstants.LOG_ACTION_CREATED)
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, action.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_NAME, action.getActionName())
                .detail(AppConstants.DETAIL_KEY_ENDPOINT, action.getEndpoint())
                .detail("method", action.getMethod() != null ? action.getMethod().toString() : null)
                .detail(AppConstants.DETAIL_KEY_PAGE_ID, pageId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION)
                .log();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(action);
            
        } catch (IllegalArgumentException e) {
            logger.warn()
                .message(AppConstants.LOG_ACTION_VALIDATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_ACTION_NAME, request.getActionName())
                .detail(AppConstants.DETAIL_KEY_PAGE_ID, pageId.toString())
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION_REQUEST)
                .log();
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ACTION_CREATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_ACTION_NAME, request.getActionName())
                .detail(AppConstants.DETAIL_KEY_PAGE_ID, pageId.toString())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION_REQUEST)
                .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AppConstants.ACTION_CREATION_FAILED + ": " + e.getMessage());
        }
    }

    /**
     * Retrieves the list of available roles with user counts, excluding SYSTEM roles.
     * 
     * <p>Returns paginated list of roles that can have permissions assigned to them.</p>
     * 
     * @param page page number (default: 0)
     * @param size page size (default: 5)
     * @param search optional search term to filter roles by name
     * @return ResponseEntity containing paginated list of available roles
     */
    @GetMapping("/available-roles")
    public ResponseEntity<?> getAvailableRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search) {
        try {
            logger.trace()
                .message(AppConstants.LOG_TRACE_ENTERING_GET_AVAILABLE_ROLES)
                .detail("page", page)
                .detail("size", size)
                .detail("search", search)
                .objectType(AppConstants.AVAILABLE_ROLES_REQUEST)
                .log();
            
            logger.info()
                .message(AppConstants.RETRIEVING_AVAILABLE_ROLES)
                .objectType(AppConstants.AVAILABLE_ROLES_REQUEST)
                .log();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_CALLING_SERVICE_GET_HIERARCHY)
                .objectType(AppConstants.AVAILABLE_ROLES_REQUEST)
                .log();
            
            HierarchicalPermissionsResponse response = granularPermissionService.getHierarchicalPermissions(null, null);
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_HIERARCHY_RETRIEVED_BY_SERVICE)
                .objectType(AppConstants.AVAILABLE_ROLES_REQUEST)
                .log();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXTRACTING_AVAILABLE_ROLES)
                .detail(AppConstants.DETAIL_KEY_AVAILABLE_ROLE_COUNT, response.getAvailableRoles() != null ? response.getAvailableRoles().size() : 0)
                .objectType(AppConstants.AVAILABLE_ROLES_RESPONSE)
                .log();
            
            // Get all available roles
            var availableRoles = response.getAvailableRoles();
            if (availableRoles == null) {
                availableRoles = new java.util.ArrayList<>();
            }
            
            // Filter out SYSTEM roles
            availableRoles = availableRoles.stream()
                .filter(role -> !AppConstants.ROLE_TYPE_SYSTEM.equals(role.getRole()))
                .collect(java.util.stream.Collectors.toList());
            
            logger.debug()
                .message("Total available roles after filtering (SYSTEM roles excluded)")
                .detail("totalRoles", availableRoles.size())
                .detail("roleNames", availableRoles.stream().map(r -> r.getRoleName()).collect(java.util.stream.Collectors.toList()))
                .log();
            
            // Filter by search term if provided
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase().trim();
                availableRoles = availableRoles.stream()
                    .filter(role -> role.getRoleName() != null && 
                                   role.getRoleName().toLowerCase().contains(searchLower))
                    .collect(java.util.stream.Collectors.toList());
                
                logger.debug()
                    .message("Roles filtered by search term")
                    .detail("search", search)
                    .detail("filteredCount", availableRoles.size())
                    .log();
            }
            
            // Calculate pagination
            int totalElements = availableRoles.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalElements);
            
            // Get page content
            var pageContent = availableRoles.subList(
                Math.min(fromIndex, totalElements),
                Math.min(toIndex, totalElements)
            );
            
            // Build paginated response
            Map<String, Object> paginatedResponse = new java.util.HashMap<>();
            paginatedResponse.put("content", pageContent);
            paginatedResponse.put("page", page);
            paginatedResponse.put("size", size);
            paginatedResponse.put("totalElements", totalElements);
            paginatedResponse.put("totalPages", totalPages);
            paginatedResponse.put("hasNext", page < totalPages - 1);
            paginatedResponse.put("hasPrevious", page > 0);
            
            logger.info()
                .message(AppConstants.AVAILABLE_ROLES_RETRIEVED_SUCCESSFULLY)
                .detail("totalRoles", totalElements)
                .detail("page", page)
                .detail("pageSize", pageContent.size())
                .objectType(AppConstants.AVAILABLE_ROLES_RESPONSE)
                .log();
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_GET_AVAILABLE_ROLES)
                .detail("totalRoles", totalElements)
                .objectType(AppConstants.AVAILABLE_ROLES_RESPONSE)
                .log();
            
            return ResponseEntity.ok(paginatedResponse);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.FAILED_TO_RETRIEVE_AVAILABLE_ROLES)
                .objectType(AppConstants.AVAILABLE_ROLES_REQUEST)
                .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AppConstants.FAILED_TO_RETRIEVE_AVAILABLE_ROLES + ": " + e.getMessage());
        }
    }

    /**
     * Retrieves the complete hierarchical structure of system permissions.
     * 
     * <p>Returns modules with their nested pages and actions, plus a separate
     * list of modules for filtering purposes.</p>
     * 
     * @param moduleFilter optional module name to filter by
     * @return ResponseEntity containing the hierarchical permissions response
     */
    @GetMapping("/hierarchy")
    public ResponseEntity<?> getHierarchicalPermissions(
            @RequestParam(required = false) String moduleFilter,
            @RequestParam(required = false) String roleFilter) {
        try {
            logger.info()
                .message(AppConstants.LOG_HIERARCHICAL_PERMISSIONS_REQUEST)
                .detail("moduleFilter", moduleFilter)
                .detail("roleFilter", roleFilter)
                .objectType(AppConstants.LOG_OBJECT_TYPE_HIERARCHICAL_PERMISSIONS_REQUEST)
                .log();
            
            HierarchicalPermissionsResponse response = granularPermissionService.getHierarchicalPermissions(moduleFilter, roleFilter);
            
            logger.info()
                .message(AppConstants.LOG_HIERARCHICAL_PERMISSIONS_SUCCESS)
                .detail("moduleCount", response.getModules().size())
                .detail("availableModuleCount", response.getAvailableModules().size())
                .detail("moduleFilter", moduleFilter)
                .detail("roleFilter", roleFilter)
                .objectType(AppConstants.LOG_OBJECT_TYPE_HIERARCHICAL_PERMISSIONS_RESPONSE)
                .log();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_HIERARCHICAL_PERMISSIONS_FAILED)
                .detail("moduleFilter", moduleFilter)
                .detail("roleFilter", roleFilter)
                .objectType(AppConstants.LOG_OBJECT_TYPE_HIERARCHICAL_PERMISSIONS_REQUEST)
                .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AppConstants.LOG_HIERARCHICAL_PERMISSIONS_FAILED + ": " + e.getMessage());
        }
    }

       /**
     * Retrieves the list of available modules for filtering.
     * 
     * @return ResponseEntity containing array of module names
     */
    @GetMapping("/available-modules")
    public ResponseEntity<?> getAvailableModules() {
        
        logger.trace()
            .message(AppConstants.LOG_TRACE_ENTERING_GET_AVAILABLE_MODULES)
            .detail(AppConstants.DETAIL_KEY_METHOD_NAME, "getAvailableModules")
            .objectType(AppConstants.AVAILABLE_MODULES_REQUEST)
            .log();
        
        try {
            logger.info()
                .message(AppConstants.LOG_GET_AVAILABLE_MODULES_ENDPOINT_CALLED)
                .detail(AppConstants.DETAIL_KEY_ENDPOINT, "GET /api/v1/granular-permissions/available-modules")
                .objectType(AppConstants.AVAILABLE_MODULES_REQUEST)
                .log();
            
            logger.info()
                .message(AppConstants.RETRIEVING_AVAILABLE_MODULES)
                .objectType(AppConstants.AVAILABLE_MODULES_REQUEST)
                .log();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_CALLING_SERVICE_GET_HIERARCHY)
                .objectType(AppConstants.AVAILABLE_MODULES_REQUEST)
                .log();
            
            HierarchicalPermissionsResponse response = granularPermissionService.getHierarchicalPermissions(null, null);
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_HIERARCHY_RETRIEVED_BY_SERVICE)
                .objectType(AppConstants.AVAILABLE_MODULES_REQUEST)
                .log();
            
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXTRACTING_AVAILABLE_MODULES)
                .detail(AppConstants.DETAIL_KEY_AVAILABLE_MODULE_COUNT, response.getAvailableModules().size())
                .objectType(AppConstants.AVAILABLE_MODULES_RESPONSE)
                .log();
            
            logger.info()
                .message(AppConstants.LOG_AVAILABLE_MODULES_RETRIEVED_SUCCESSFULLY)
                .detail(AppConstants.DETAIL_KEY_AVAILABLE_MODULE_COUNT, response.getAvailableModules().size())
                .detail(AppConstants.DETAIL_KEY_HTTP_STATUS, HttpStatus.OK.value())
                .objectType(AppConstants.AVAILABLE_MODULES_RESPONSE)
                .log();
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_GET_AVAILABLE_MODULES)
                .detail(AppConstants.DETAIL_KEY_SUCCESS, true)
                .objectType(AppConstants.AVAILABLE_MODULES_RESPONSE)
                .log();
            
            return ResponseEntity.ok(response.getAvailableModules());
            
        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.LOG_DEBUG_EXCEPTION_CAUGHT)
                .detail(AppConstants.DETAIL_KEY_EXCEPTION_TYPE, e.getClass().getSimpleName())
                .objectType(AppConstants.AVAILABLE_MODULES_REQUEST)
                .log();
            
            logger.error(e)
                .message(AppConstants.LOG_AVAILABLE_MODULES_RETRIEVAL_FAILED)
                .objectType(AppConstants.AVAILABLE_MODULES_REQUEST)
                .log();
            
            logger.trace()
                .message(AppConstants.LOG_TRACE_EXITING_GET_AVAILABLE_MODULES)
                .detail(AppConstants.DETAIL_KEY_SUCCESS, false)
                .objectType(AppConstants.AVAILABLE_MODULES_REQUEST)
                .log();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AppConstants.MSG_AVAILABLE_MODULES_RETRIEVAL_FAILED + ": " + e.getMessage());
        }
    }

}