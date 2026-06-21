package com.aidentx.applicationpermissions.roles_management.controller;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.model.dto.PageAccessResponse;
import com.aidentx.applicationpermissions.roles_management.model.entity.Action;
import com.aidentx.applicationpermissions.roles_management.model.entity.Module;
import com.aidentx.applicationpermissions.roles_management.model.entity.Page;
import com.aidentx.applicationpermissions.roles_management.model.entity.Permission;
import com.aidentx.applicationpermissions.roles_management.repository.ActionRepository;
import com.aidentx.applicationpermissions.roles_management.repository.ModuleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.PageRepository;
import com.aidentx.applicationpermissions.roles_management.repository.PermissionRepository;
import com.aidentx.applicationpermissions.roles_management.service.UserPermissionService;
import com.aidentx.applicationpermissions.shared.security.TenantContextService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for checking user permissions.
 * This will be converted to gRPC later, but for now provides REST endpoint for testing.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionCheckController {
    
    private final UserPermissionService userPermissionService;
    private final PageRepository pageRepository;
    private final ModuleRepository moduleRepository;
    private final ActionRepository actionRepository;
    private final PermissionRepository permissionRepository;
    private final TenantContextService tenantContextService;
    private final IUnifiedLogger logger;
    
    @Autowired
    public PermissionCheckController(UserPermissionService userPermissionService,
                                   PageRepository pageRepository,
                                   ModuleRepository moduleRepository,
                                   ActionRepository actionRepository,
                                   PermissionRepository permissionRepository,
                                   TenantContextService tenantContextService,
                                   UnifiedLoggerFactory loggerFactory) {
        this.userPermissionService = userPermissionService;
        this.pageRepository = pageRepository;
        this.moduleRepository = moduleRepository;
        this.actionRepository = actionRepository;
        this.permissionRepository = permissionRepository;
        this.tenantContextService = tenantContextService;
        this.logger = loggerFactory.getLogger("PermissionCheckController", "API_CONTROLLER");
    }
    
    /**
     * Check if a user has permission for a specific endpoint and method.
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @param apiEndpoint the API endpoint
     * @param httpMethod the HTTP method
     * @return permission check result
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkPermission(
            @RequestParam String userId,
            @RequestParam String tenantId,
            @RequestParam String apiEndpoint,
            @RequestParam String httpMethod) {
        
        logger.trace()
            .message(AppConstants.LOG_PERMISSION_CHECK_REQUEST_STARTED)
            .detail(AppConstants.DETAIL_USER_ID, userId)
            .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
            .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
            .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
            .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
            .log();
        
        logger.debug()
            .message(AppConstants.LOG_PERMISSION_CHECK_PARAMETERS_RECEIVED)
            .detail(AppConstants.DETAIL_USER_ID, userId)
            .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
            .detail(AppConstants.DETAIL_API_ENDPOINT, apiEndpoint)
            .detail(AppConstants.DETAIL_HTTP_METHOD, httpMethod)
            .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
            .log();
        
        logger.trace()
            .message(AppConstants.LOG_PERMISSION_CHECK_SERVICE_CALL)
            .detail(AppConstants.DETAIL_USER_ID, userId)
            .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
            .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
            .log();
        
        boolean hasPermission = userPermissionService.hasPermissionForEndpoint(
            userId, tenantId, apiEndpoint, httpMethod);
        
        logger.debug()
            .message(AppConstants.LOG_PERMISSION_CHECK_SERVICE_RESPONSE)
            .detail(AppConstants.DETAIL_USER_ID, userId)
            .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
            .detail(AppConstants.DETAIL_HAS_PERMISSION, String.valueOf(hasPermission))
            .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
            .log();
        
        logger.trace()
            .message(AppConstants.LOG_PERMISSION_CHECK_RESPONSE_BUILDING)
            .detail(AppConstants.DETAIL_HAS_PERMISSION, String.valueOf(hasPermission))
            .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
            .log();
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasPermission", hasPermission);
        response.put("userId", userId);
        response.put("tenantId", tenantId);
        response.put("apiEndpoint", apiEndpoint);
        response.put("httpMethod", httpMethod);
        
        logger.debug()
            .message(AppConstants.LOG_PERMISSION_CHECK_RESPONSE_BUILT)
            .detail(AppConstants.DETAIL_RESPONSE_SIZE, String.valueOf(response.size()))
            .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
            .log();
        
        logger.trace()
            .message(AppConstants.LOG_PERMISSION_CHECK_REQUEST_COMPLETED)
            .detail(AppConstants.DETAIL_USER_ID, userId)
            .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
            .detail(AppConstants.DETAIL_HAS_PERMISSION, String.valueOf(hasPermission))
            .objectType(AppConstants.OBJECT_TYPE_PERMISSION_CHECK)
            .log();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check-endpoint")
    public ResponseEntity<Boolean> checkEndpointExists(
            @RequestParam String endpoint,
            @RequestParam String method) {
        
        logger.trace()
            .message(AppConstants.LOG_ENDPOINT_CHECK_REQUEST_STARTED)
            .detail(AppConstants.DETAIL_ENDPOINT, endpoint)
            .detail(AppConstants.DETAIL_METHOD, method)
            .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_CHECK)
            .log();
        
        logger.debug()
            .message(AppConstants.LOG_ENDPOINT_CHECK_PARAMETERS_RECEIVED)
            .detail(AppConstants.DETAIL_ENDPOINT, endpoint)
            .detail(AppConstants.DETAIL_METHOD, method)
            .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_CHECK)
            .log();
        
        logger.info()
            .message(AppConstants.LOG_CHECKING_IF_ENDPOINT_EXISTS)
            .detail(AppConstants.DETAIL_METHOD, method)
            .detail(AppConstants.DETAIL_ENDPOINT, endpoint)
            .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_CHECK)
            .log();
        
        try {
            logger.trace()
                .message(AppConstants.LOG_ENDPOINT_CHECK_SERVICE_CALL)
                .detail(AppConstants.DETAIL_ENDPOINT, endpoint)
                .detail(AppConstants.DETAIL_METHOD, method)
                .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_CHECK)
                .log();
            
            boolean exists = userPermissionService.isEndpointManaged(endpoint, method);
            
            logger.debug()
                .message(AppConstants.LOG_ENDPOINT_CHECK_SERVICE_RESPONSE)
                .detail(AppConstants.DETAIL_METHOD, method)
                .detail(AppConstants.DETAIL_ENDPOINT, endpoint)
                .detail(AppConstants.DETAIL_IS_MANAGED, String.valueOf(exists))
                .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_CHECK)
                .log();
            
            logger.trace()
                .message(AppConstants.LOG_ENDPOINT_CHECK_REQUEST_COMPLETED)
                .detail(AppConstants.DETAIL_ENDPOINT, endpoint)
                .detail(AppConstants.DETAIL_METHOD, method)
                .detail(AppConstants.DETAIL_IS_MANAGED, String.valueOf(exists))
                .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_CHECK)
                .log();
                
            return ResponseEntity.ok(exists);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ERROR_ENDPOINT_CHECK_FAILED)
                .detail(AppConstants.DETAIL_METHOD, method)
                .detail(AppConstants.DETAIL_ENDPOINT, endpoint)
                .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_CHECK)
                .log();
            
            logger.trace()
                .message(AppConstants.LOG_ENDPOINT_CHECK_RETURNING_SAFE_VALUE)
                .detail(AppConstants.DETAIL_ENDPOINT, endpoint)
                .detail(AppConstants.DETAIL_METHOD, method)
                .objectType(AppConstants.OBJECT_TYPE_ENDPOINT_CHECK)
                .log();
            
            return ResponseEntity.ok(false); // Return false for safety
        }
    }
    
    /**
     * Get all system pages with user access status.
     * 
     * <p>Retrieves all system pages and checks if the authenticated user has "View" permission
     * for each page. Returns page details including ID, name, path, and access status.</p>
     * 
     * @return ResponseEntity containing list of page access information
     */
    @GetMapping("/pages")
    public ResponseEntity<Map<String, Object>> getUserPages() {
        try {
            logger.info()
                .message(AppConstants.LOG_PROCESSING_USER_PAGES_ACCESS_REQUEST)
                .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_REQUEST)
                .log();
            
            // Get user ID from JWT token via TenantContextService
            String userId = tenantContextService.getCurrentUserId();
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.debug()
                .message(AppConstants.LOG_EXTRACTED_USER_CONTEXT_FROM_JWT)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_REQUEST)
                .log();
            
            // Get page access information using the service
            List<PageAccessResponse> pageAccessList = userPermissionService.getUserPageAccess(userId, tenantId);
            
            logger.info()
                .message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_USER_PAGE_ACCESS)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId)
                .detail(AppConstants.DETAIL_KEY_TOTAL_PAGES, String.valueOf(pageAccessList.size()))
                .detail(AppConstants.DETAIL_KEY_ACCESSIBLE_PAGES, String.valueOf((int) pageAccessList.stream().filter(PageAccessResponse::isCanAccess).count()))
                .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_REQUEST)
                .log();
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("pages", pageAccessList);
            response.put("totalPages", pageAccessList.size());
            response.put("accessiblePages", pageAccessList.stream().filter(PageAccessResponse::isCanAccess).count());
            response.put("userId", userId);
            response.put("tenantId", tenantId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_USER_PAGE_ACCESS)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_REQUEST)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", AppConstants.ERROR_FAILED_TO_RETRIEVE_PAGE_ACCESS);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Check if user has access to a specific page by path.
     * 
     * <p>Checks if the authenticated user has "View" permission for the specified page path.
     * Returns access status and permission requirement information.</p>
     * 
     * @param path the page path to check (e.g., "/dashboard", "/settings/sso")
     * @return ResponseEntity containing page access information
     */
    @GetMapping("/pages/check")
    public ResponseEntity<Map<String, Object>> checkPageAccess(@RequestParam String path) {
        try {
            logger.info()
                .message(AppConstants.LOG_PROCESSING_PAGE_ACCESS_CHECK_REQUEST)
                .detail(AppConstants.DETAIL_KEY_PATH, path)
                .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                .log();
            
            // Get user ID from JWT token via TenantContextService
            String userId = tenantContextService.getCurrentUserId();
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.debug()
                .message(AppConstants.LOG_EXTRACTED_USER_CONTEXT_FOR_PAGE_ACCESS_CHECK)
                .detail(AppConstants.DETAIL_KEY_USER_ID, userId)
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_PATH, path)
                .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                .log();
            
            // Find the page by path
            Optional<Page> pageOptional = pageRepository.findByPagePath(path);
            
            // If page not found, try parent paths (for sub-routes like /applications/add-application)
            if (pageOptional.isEmpty()) {
                logger.debug()
                    .message(AppConstants.LOG_PAGE_NOT_FOUND_FOR_PATH)
                    .detail(AppConstants.DETAIL_KEY_PATH, path)
                    .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                    .log();
                
                // Try to find parent page by removing last segment
                String[] pathSegments = path.split("/");
                for (int i = pathSegments.length - 1; i > 0; i--) {
                    StringBuilder parentPathBuilder = new StringBuilder();
                    for (int j = 0; j < i; j++) {
                        if (!pathSegments[j].isEmpty()) {
                            parentPathBuilder.append("/").append(pathSegments[j]);
                        }
                    }
                    String parentPath = parentPathBuilder.toString();
                    if (parentPath.isEmpty()) {
                        parentPath = "/";
                    }
                    
                    logger.debug()
                        .message(AppConstants.LOG_TRYING_PARENT_PATH_FOR_PAGE_ACCESS)
                        .detail(AppConstants.DETAIL_KEY_PATH, parentPath)
                        .detail(AppConstants.DETAIL_KEY_ORIGINAL_PATH, path)
                        .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                        .log();
                    
                    pageOptional = pageRepository.findByPagePath(parentPath);
                    if (pageOptional.isPresent()) {
                        logger.info()
                            .message(AppConstants.LOG_FOUND_PARENT_PAGE_FOR_SUB_ROUTE)
                            .detail(AppConstants.DETAIL_KEY_ORIGINAL_PATH, path)
                            .detail(AppConstants.DETAIL_KEY_PARENT_PATH, parentPath)
                            .detail(AppConstants.DETAIL_KEY_PAGE_NAME, pageOptional.get().getPageName())
                            .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                            .log();
                        break;
                    }
                }
                
                // If still not found after checking parents, allow access (open access for unmanaged routes)
                if (pageOptional.isEmpty()) {
                    logger.info()
                        .message(AppConstants.LOG_NO_PAGE_OR_PARENT_FOUND_ALLOWING_ACCESS)
                        .detail(AppConstants.DETAIL_KEY_PATH, path)
                        .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                        .log();
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("canAccess", true);
                    response.put("requiresPermission", false);
                    response.put("path", path);
                    response.put("message", AppConstants.MESSAGE_UNMANAGED_ROUTE_OPEN_ACCESS);
                    
                    return ResponseEntity.ok(response);
                }
            }
            
            Page page = pageOptional.get();
            
            // Check if there's a "View" action for this page
            Optional<Action> viewActionOpt = actionRepository
                .findByActionNameAndPageId("View", page.getId());
            
            boolean requiresPermission = false;
            boolean canAccess = true; // Default to true (open access)
            
            if (viewActionOpt.isPresent()) {
                // Check if this action has any permissions assigned to ACTIVE roles
                List<Permission> permissions = permissionRepository.findByActionId(viewActionOpt.get().getId());
                
                // Check if any of these permissions belong to ACTIVE roles
                boolean hasActiveRolePermissions = false;
                if (!permissions.isEmpty()) {
                    for (Permission permission : permissions) {
                        Long activeRoleCount = permissionRepository.countActiveRolesByPermissionId(permission.getId());
                        if (activeRoleCount > 0) {
                            hasActiveRolePermissions = true;
                            break;
                        }
                    }
                }
                
                if (hasActiveRolePermissions) {
                    // Permissions exist for ACTIVE roles - requires permission check
                    requiresPermission = true;
                    canAccess = userPermissionService.checkActionPermission(userId, tenantId, viewActionOpt.get().getId());
                    
                    logger.debug()
                        .message(AppConstants.LOG_PAGE_REQUIRES_PERMISSION_CHECKED_USER_ACCESS)
                        .detail(AppConstants.DETAIL_KEY_USER_ID, userId)
                        .detail(AppConstants.DETAIL_KEY_PAGE_ID, page.getId().toString())
                        .detail(AppConstants.DETAIL_KEY_PAGE_NAME, page.getPageName())
                        .detail(AppConstants.DETAIL_KEY_CAN_ACCESS, String.valueOf(canAccess))
                        .detail(AppConstants.DETAIL_KEY_REQUIRES_PERMISSION, String.valueOf(requiresPermission))
                        .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                        .log();
                } else {
                    // Action exists but no permissions assigned - open access
                    logger.debug()
                        .message(AppConstants.LOG_PAGE_HAS_VIEW_ACTION_NO_PERMISSIONS)
                        .detail(AppConstants.DETAIL_KEY_PATH, path)
                        .detail(AppConstants.DETAIL_KEY_PAGE_NAME, page.getPageName())
                        .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                        .log();
                }
            } else {
                // No View action exists - open access
                logger.debug()
                    .message(AppConstants.LOG_PAGE_HAS_NO_VIEW_ACTION)
                    .detail(AppConstants.DETAIL_KEY_PATH, path)
                    .detail(AppConstants.DETAIL_KEY_PAGE_NAME, page.getPageName())
                    .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                    .log();
            }
            
            logger.info()
                .message(AppConstants.LOG_PAGE_ACCESS_CHECK_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_PATH, path)
                .detail(AppConstants.DETAIL_KEY_CAN_ACCESS, String.valueOf(canAccess))
                .detail(AppConstants.DETAIL_KEY_REQUIRES_PERMISSION, String.valueOf(requiresPermission))
                .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                .log();
            
            Map<String, Object> response = new HashMap<>();
            response.put("canAccess", canAccess);
            response.put("requiresPermission", requiresPermission);
            response.put("path", path);
            response.put("pageId", page.getId());
            response.put("pageName", page.getPageName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_CHECK_PAGE_ACCESS)
                .detail(AppConstants.DETAIL_KEY_PATH, path)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.OBJECT_TYPE_PAGE_ACCESS_CHECK)
                .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("canAccess", false);
            errorResponse.put("error", AppConstants.ERROR_FAILED_TO_CHECK_PAGE_ACCESS);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("path", path);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}