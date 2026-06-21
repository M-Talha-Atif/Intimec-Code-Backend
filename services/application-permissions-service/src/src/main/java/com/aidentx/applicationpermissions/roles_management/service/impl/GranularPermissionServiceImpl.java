package com.aidentx.applicationpermissions.roles_management.service.impl;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.HttpMethod;
import com.aidentx.applicationpermissions.roles_management.model.dto.*;
import com.aidentx.applicationpermissions.roles_management.model.entity.Action;
import com.aidentx.applicationpermissions.roles_management.model.entity.Module;
import com.aidentx.applicationpermissions.roles_management.model.entity.Page;
import com.aidentx.applicationpermissions.roles_management.model.entity.Permission;
import com.aidentx.applicationpermissions.roles_management.model.entity.RolePermission;
import com.aidentx.applicationpermissions.roles_management.repository.ActionRepository;
import com.aidentx.applicationpermissions.roles_management.repository.ModuleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.PageRepository;
import com.aidentx.applicationpermissions.roles_management.repository.PermissionRepository;
import com.aidentx.applicationpermissions.roles_management.repository.RolePermissionRepository;
import com.aidentx.applicationpermissions.roles_management.service.GranularPermissionService;
import com.aidentx.applicationpermissions.roles_management.service.RoleService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link GranularPermissionService} for managing granular permissions.
 * 
 * <p>This service implementation handles the creation of system-wide permission entities
 * by setting their tenantId to null, making them available as base templates for all tenants.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class GranularPermissionServiceImpl implements GranularPermissionService {

    private final ModuleRepository moduleRepository;
    private final PageRepository pageRepository;
    private final ActionRepository actionRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleService roleService;
    private final IUnifiedLogger logger;

    /**
     * Constructor for GranularPermissionServiceImpl.
     * 
     * @param moduleRepository repository for module entities
     * @param pageRepository repository for page entities
     * @param actionRepository repository for action entities
     * @param permissionRepository repository for permission entities
     * @param rolePermissionRepository repository for role-permission associations
     * @param roleService service for role operations
     * @param loggerFactory factory for creating unified loggers
     */
    @Autowired
    public GranularPermissionServiceImpl(ModuleRepository moduleRepository,
                                       PageRepository pageRepository,
                                       ActionRepository actionRepository,
                                       PermissionRepository permissionRepository,
                                       RolePermissionRepository rolePermissionRepository,
                                       RoleService roleService,
                                       UnifiedLoggerFactory loggerFactory) {
        this.moduleRepository = moduleRepository;
        this.pageRepository = pageRepository;
        this.actionRepository = actionRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleService = roleService;
        this.logger = loggerFactory.getLogger(AppConstants.GRANULAR_PERMISSION_SERVICE_LOGGER_NAME, 
                                            AppConstants.LOG_CATEGORY_GRANULAR_PERMISSION_MANAGEMENT);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Creates a new Module entity with the specified name and sets tenantId to null
     * to make it available system-wide.</p>
     */
    @Override
    public Module createSystemModule(String moduleName) {
        try {
            logger.info()
                .message(AppConstants.LOG_MODULE_CREATE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_MODULE_NAME, moduleName)
                .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE_REQUEST)
                .log();
            
            if (!StringUtils.hasText(moduleName)) {
                throw new IllegalArgumentException(AppConstants.MODULE_NAME_CANNOT_BE_NULL_OR_EMPTY);
            }
            
            // Check if module with same name already exists
            if (moduleRepository.findByModuleName(moduleName.trim()).isPresent()) {
                logger.warn()
                    .message(AppConstants.LOG_MODULE_VALIDATION_FAILED)
                    .detail(AppConstants.DETAIL_KEY_MODULE_NAME, moduleName)
                    .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, AppConstants.MODULE_ALREADY_EXISTS)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE_REQUEST)
                    .log();
                throw new IllegalArgumentException(AppConstants.MODULE_ALREADY_EXISTS);
            }
            
            Module module = new Module();
            module.setModuleName(moduleName.trim());
            module.setTenantId(null);
            
            Module savedModule = moduleRepository.save(module);
            
            logger.info()
                .message(AppConstants.LOG_MODULE_CREATED)
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, savedModule.getId().toString())
                .detail(AppConstants.DETAIL_KEY_MODULE_NAME, savedModule.getModuleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE)
                .log();
            
            return savedModule;
            
        } catch (IllegalArgumentException e) {
            logger.warn()
                .message(AppConstants.LOG_MODULE_VALIDATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_MODULE_NAME, moduleName)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE_REQUEST)
                .log();
            throw e;
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_MODULE_CREATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_MODULE_NAME, moduleName)
                .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.MODULE_CREATION_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Creates a new Page entity with the specified name and sets tenantId to null
     * to make it available system-wide.</p>
     */
    @Override
    public Page createSystemPage(String pageName, UUID moduleId) {
        try {
            logger.info()
                .message(AppConstants.LOG_PAGE_CREATE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, pageName)
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId != null ? moduleId.toString() : null)
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                .log();
            
            if (!StringUtils.hasText(pageName)) {
                throw new IllegalArgumentException(AppConstants.PAGE_NAME_CANNOT_BE_NULL_OR_EMPTY);
            }
            
            // Validate module exists if moduleId is provided
            if (moduleId != null && !moduleRepository.existsById(moduleId)) {
                logger.warn()
                    .message(AppConstants.LOG_PAGE_VALIDATION_FAILED)
                    .detail(AppConstants.DETAIL_KEY_PAGE_NAME, pageName)
                    .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId.toString())
                    .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, AppConstants.MODULE_NOT_FOUND)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                    .log();
                throw new IllegalArgumentException(AppConstants.MODULE_NOT_FOUND);
            }
            
            // Check if page with same name already exists
            if (pageRepository.findByPageName(pageName.trim()).isPresent()) {
                logger.warn()
                    .message(AppConstants.LOG_PAGE_VALIDATION_FAILED)
                    .detail(AppConstants.DETAIL_KEY_PAGE_NAME, pageName)
                    .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, AppConstants.PAGE_ALREADY_EXISTS)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                    .log();
                throw new IllegalArgumentException(AppConstants.PAGE_ALREADY_EXISTS);
            }
            
            Page page = new Page();
            page.setPageName(pageName.trim());
            page.setModuleId(moduleId);
            page.setTenantId(null);
            
            Page savedPage = pageRepository.save(page);
            
            logger.info()
                .message(AppConstants.LOG_PAGE_CREATED)
                .detail(AppConstants.DETAIL_KEY_PAGE_ID, savedPage.getId().toString())
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, savedPage.getPageName())
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId != null ? moduleId.toString() : null)
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE)
                .log();
            
            return savedPage;
            
        } catch (IllegalArgumentException e) {
            logger.warn()
                .message(AppConstants.LOG_PAGE_VALIDATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, pageName)
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId != null ? moduleId.toString() : null)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                .log();
            throw e;
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_PAGE_CREATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, pageName)
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId != null ? moduleId.toString() : null)
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.PAGE_CREATION_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Creates a new Page entity with the specified name, path, and module ID, and sets tenantId to null
     * to make it available system-wide.</p>
     */
    @Override
    public Page createSystemPage(String pageName, String pagePath, UUID moduleId) {
        try {
            logger.info()
                .message(AppConstants.LOG_PAGE_CREATE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, pageName)
                .detail("pagePath", pagePath)
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId != null ? moduleId.toString() : null)
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                .log();
            
            if (!StringUtils.hasText(pageName)) {
                throw new IllegalArgumentException(AppConstants.PAGE_NAME_CANNOT_BE_NULL_OR_EMPTY);
            }
            
            // Validate module exists if moduleId is provided
            if (moduleId != null && !moduleRepository.existsById(moduleId)) {
                logger.warn()
                    .message(AppConstants.LOG_PAGE_VALIDATION_FAILED)
                    .detail(AppConstants.DETAIL_KEY_PAGE_NAME, pageName)
                    .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId.toString())
                    .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, AppConstants.MODULE_NOT_FOUND)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                    .log();
                throw new IllegalArgumentException(AppConstants.MODULE_NOT_FOUND);
            }
            
            // Check if page with same name already exists
            if (pageRepository.findByPageName(pageName.trim()).isPresent()) {
                logger.warn()
                    .message(AppConstants.LOG_PAGE_VALIDATION_FAILED)
                    .detail(AppConstants.DETAIL_KEY_PAGE_NAME, pageName)
                    .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, AppConstants.PAGE_ALREADY_EXISTS)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                    .log();
                throw new IllegalArgumentException(AppConstants.PAGE_ALREADY_EXISTS);
            }
            
            Page page = new Page();
            page.setPageName(pageName.trim());
            page.setPagePath(StringUtils.hasText(pagePath) ? pagePath.trim() : null);
            page.setModuleId(moduleId);
            page.setTenantId(null);
            
            Page savedPage = pageRepository.save(page);
            
            logger.info()
                .message(AppConstants.LOG_PAGE_CREATED)
                .detail(AppConstants.DETAIL_KEY_PAGE_ID, savedPage.getId().toString())
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, savedPage.getPageName())
                .detail("pagePath", savedPage.getPagePath())
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId != null ? moduleId.toString() : null)
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE)
                .log();
            
            return savedPage;
            
        } catch (IllegalArgumentException e) {
            logger.warn()
                .message(AppConstants.LOG_PAGE_VALIDATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, pageName)
                .detail("pagePath", pagePath)
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId != null ? moduleId.toString() : null)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                .log();
            throw e;
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_PAGE_CREATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_PAGE_NAME, pageName)
                .detail("pagePath", pagePath)
                .detail(AppConstants.DETAIL_KEY_MODULE_ID, moduleId != null ? moduleId.toString() : null)
                .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.PAGE_CREATION_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Creates a new Action entity with the specified name and endpoint, and sets tenantId to null
     * to make it available system-wide.</p>
     */
    @Override
    public Action createSystemAction(String actionName, String endpoint, HttpMethod method, UUID pageId) {
        try {
            logger.info()
                .message(AppConstants.LOG_ACTION_CREATE_REQUEST)
                .detail(AppConstants.DETAIL_KEY_ACTION_NAME, actionName)
                .detail(AppConstants.DETAIL_KEY_ENDPOINT, endpoint)
                .detail(AppConstants.DETAIL_KEY_METHOD, method != null ? method.toString() : null)
                .detail(AppConstants.DETAIL_KEY_PAGE_ID, pageId != null ? pageId.toString() : null)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION_REQUEST)
                .log();
            
            if (!StringUtils.hasText(actionName)) {
                throw new IllegalArgumentException(AppConstants.ACTION_NAME_CANNOT_BE_NULL_OR_EMPTY);
            }
            
            // Validate page exists if pageId is provided
            if (pageId != null && !pageRepository.existsById(pageId)) {
                logger.warn()
                    .message(AppConstants.LOG_ACTION_VALIDATION_FAILED)
                    .detail(AppConstants.DETAIL_KEY_ACTION_NAME, actionName)
                    .detail(AppConstants.DETAIL_KEY_PAGE_ID, pageId.toString())
                    .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, AppConstants.PAGE_NOT_FOUND)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION_REQUEST)
                    .log();
                throw new IllegalArgumentException(AppConstants.PAGE_NOT_FOUND);
            }
            
            // Check if action with same name already exists within the same page
            if (actionRepository.findByActionNameAndPageId(actionName.trim(), pageId).isPresent()) {
                logger.warn()
                    .message(AppConstants.LOG_ACTION_VALIDATION_FAILED)
                    .detail(AppConstants.DETAIL_KEY_ACTION_NAME, actionName)
                    .detail(AppConstants.DETAIL_KEY_PAGE_ID, pageId.toString())
                    .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, AppConstants.EXCEPTION_ACTION_NAME_EXISTS)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION_REQUEST)
                    .log();
                throw new IllegalArgumentException(AppConstants.EXCEPTION_ACTION_NAME_EXISTS);
            }
            
            Action action = new Action();
            action.setActionName(actionName.trim());
            action.setEndpoint(StringUtils.hasText(endpoint) ? endpoint.trim() : null);
            action.setMethod(method);
            action.setPageId(pageId);
            action.setTenantId(null);
            
            Action savedAction = actionRepository.save(action);
            
            logger.info()
                .message(AppConstants.LOG_ACTION_CREATED)
                .detail(AppConstants.DETAIL_KEY_ACTION_ID, savedAction.getId().toString())
                .detail(AppConstants.DETAIL_KEY_ACTION_NAME, savedAction.getActionName())
                .detail(AppConstants.DETAIL_KEY_ENDPOINT, savedAction.getEndpoint())
                .detail(AppConstants.DETAIL_KEY_METHOD, savedAction.getMethod() != null ? savedAction.getMethod().toString() : null)
                .detail(AppConstants.DETAIL_KEY_PAGE_ID, pageId != null ? pageId.toString() : null)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION)
                .log();
            
            return savedAction;
            
        } catch (IllegalArgumentException e) {
            logger.warn()
                .message(AppConstants.LOG_ACTION_VALIDATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_ACTION_NAME, actionName)
                .detail(AppConstants.DETAIL_KEY_PAGE_ID, pageId != null ? pageId.toString() : null)
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION_REQUEST)
                .log();
            throw e;
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_ACTION_CREATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_ACTION_NAME, actionName)
                .detail(AppConstants.DETAIL_KEY_PAGE_ID, pageId != null ? pageId.toString() : null)
                .objectType(AppConstants.LOG_OBJECT_TYPE_ACTION_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.ACTION_CREATION_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HierarchicalPermissionsResponse getHierarchicalPermissions() {
        return getHierarchicalPermissions(null);
    }

    @Override
    public HierarchicalPermissionsResponse getHierarchicalPermissions(String moduleNameFilter) {
        return getHierarchicalPermissions(moduleNameFilter, null);
    }

    @Override
    public HierarchicalPermissionsResponse getHierarchicalPermissions(String moduleNameFilter, String roleFilter) {
        try {
            logger.info()
                .message(AppConstants.LOG_RETRIEVING_HIERARCHICAL_PERMISSIONS)
                .detail(AppConstants.DETAIL_KEY_MODULE_FILTER, moduleNameFilter)
                .detail(AppConstants.DETAIL_KEY_ROLE_FILTER, roleFilter)
                .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE_REQUEST)
                .log();

            // Get role permissions if roleFilter is provided
            Set<UUID> roleActionIds = new HashSet<>();
            if (StringUtils.hasText(roleFilter)) {
                try {
                    UUID roleId = UUID.fromString(roleFilter);
                    List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId);
                    
                    for (RolePermission rolePermission : rolePermissions) {
                        // Get the permission entity to access the actionId
                        Optional<Permission> permissionOpt = permissionRepository.findById(rolePermission.getPermissionId());
                        if (permissionOpt.isPresent()) {
                            Permission permission = permissionOpt.get();
                            roleActionIds.add(permission.getActionId());
                        }
                    }
                    
                    logger.debug()
                        .message(AppConstants.LOG_RETRIEVED_ROLE_PERMISSIONS)
                        .detail("roleId", roleFilter)
                        .detail(AppConstants.DETAIL_KEY_ROLE_PERMISSIONS_COUNT, rolePermissions.size())
                        .detail(AppConstants.DETAIL_KEY_ROLE_ACTION_IDS, roleActionIds.size())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                        .log();
                        
                } catch (IllegalArgumentException e) {
                    logger.warn()
                        .message(AppConstants.LOG_INVALID_ROLE_FILTER_PROVIDED)
                        .detail("roleFilter", roleFilter)
                        .detail("error", e.getMessage())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                        .log();
                }
            }

            // Get all modules (for the modules filter array)
            List<Module> allModules = moduleRepository.findByTenantIdIsNull();
            List<String> moduleNames = allModules.stream()
                .map(Module::getModuleName)
                .collect(Collectors.toList());

            // Filter modules if needed
            List<Module> filteredModules = allModules;
            if (StringUtils.hasText(moduleNameFilter)) {
                logger.debug()
                    .message(AppConstants.LOG_FILTERING_MODULES_BY_NAME)
                    .detail("moduleNameFilter", moduleNameFilter)
                    .detail(AppConstants.DETAIL_KEY_AVAILABLE_MODULES, moduleNames.toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE_REQUEST)
                    .log();
                    
                filteredModules = allModules.stream()
                    .filter(module -> {
                        String trimmedFilter = moduleNameFilter.trim();
                        // Support both exact match and partial match (contains)
                        boolean exactMatch = module.getModuleName().equalsIgnoreCase(trimmedFilter);
                        boolean partialMatch = module.getModuleName().toLowerCase().contains(trimmedFilter.toLowerCase());
                        boolean matches = exactMatch || partialMatch;
                        
                        logger.debug()
                            .message(AppConstants.LOG_MODULE_FILTER_CHECK)
                            .detail("moduleName", module.getModuleName())
                            .detail(AppConstants.DETAIL_KEY_TRIMMED_FILTER, trimmedFilter)
                            .detail(AppConstants.DETAIL_KEY_EXACT_MATCH, exactMatch)
                            .detail(AppConstants.DETAIL_KEY_PARTIAL_MATCH, partialMatch)
                            .detail(AppConstants.DETAIL_KEY_MATCHES, matches)
                            .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE)
                            .log();
                        return matches;
                    })
                    .collect(Collectors.toList());
                    
                logger.debug()
                    .message(AppConstants.LOG_FILTERING_RESULT)
                    .detail(AppConstants.DETAIL_KEY_ORIGINAL_MODULE_COUNT, allModules.size())
                    .detail(AppConstants.DETAIL_KEY_FILTERED_MODULE_COUNT, filteredModules.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE_REQUEST)
                    .log();
            }

            // Build hierarchical structure
            List<HierarchicalModuleDto> modules = new ArrayList<>();
            
            for (Module module : filteredModules) {
                // Get pages for this module
                List<Page> pages = pageRepository.findByModuleId(module.getId());
                
                logger.debug()
                    .message(AppConstants.LOG_PROCESSING_MODULE)
                    .detail(AppConstants.DETAIL_KEY_MODULE_ID, module.getId().toString())
                    .detail(AppConstants.DETAIL_KEY_MODULE_NAME, module.getModuleName())
                    .detail(AppConstants.DETAIL_KEY_PAGES_FOUND, pages.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE)
                    .log();
                
                List<HierarchicalPageDto> pageDtos = new ArrayList<>();
                
                for (Page page : pages) {
                    // Get actions for this page
                    List<Action> actions = actionRepository.findByPageId(page.getId());
                    
                    logger.debug()
                        .message(AppConstants.LOG_PROCESSING_PAGE)
                        .detail(AppConstants.DETAIL_KEY_PAGE_ID, page.getId().toString())
                        .detail(AppConstants.DETAIL_KEY_PAGE_NAME, page.getPageName())
                        .detail(AppConstants.DETAIL_KEY_ACTIONS_FOUND, actions.size())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_PAGE)
                        .log();
                    
                    List<HierarchicalActionDto> actionDtos = actions.stream()
                        .map(action -> {
                            Boolean hasRole = StringUtils.hasText(roleFilter) ? roleActionIds.contains(action.getId()) : null;
                            return new HierarchicalActionDto(
                                action.getId().toString(),
                                action.getActionName(),
                                action.getEndpoint(),
                                action.getMethod() != null ? action.getMethod().toString() : null,
                                hasRole
                            );
                        })
                        .collect(Collectors.toList());
                    
                    pageDtos.add(new HierarchicalPageDto(
                        page.getId().toString(),
                        page.getPageName(),
                        actionDtos
                    ));
                }
                
                modules.add(new HierarchicalModuleDto(
                    module.getId().toString(),
                    module.getModuleName(),
                    pageDtos
                ));
            }

            // Get available roles with user count for current tenant
            List<AvailableRoleDto> availableRoles = new ArrayList<>();
            try {
                List<RoleResponse> roles = roleService.getAllRoles();
                
                logger.debug()
                    .message(AppConstants.LOG_FILTERING_ACTIVE_ROLES_ONLY)
                    .detail("totalRolesRetrieved", roles.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                // Filter to include only active roles
                List<RoleResponse> activeRoles = roles.stream()
                    .filter(role -> role.getIsActive() != null && role.getIsActive())
                    .collect(Collectors.toList());
                
                int inactiveRolesFiltered = roles.size() - activeRoles.size();
                
                logger.debug()
                    .message(AppConstants.LOG_FILTERED_INACTIVE_ROLES)
                    .detail(AppConstants.DETAIL_KEY_ACTIVE_ROLES_COUNT, activeRoles.size())
                    .detail(AppConstants.DETAIL_KEY_INACTIVE_ROLES_FILTERED, inactiveRolesFiltered)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                
                availableRoles = activeRoles.stream()
                    .map(role -> new AvailableRoleDto(
                        role.getId(),
                        role.getRoleName(),
                        role.getRoleType().name(),
                        role.getUserCount()
                    ))
                    .collect(Collectors.toList());
                    
                logger.debug()
                    .message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_AVAILABLE_ROLES)
                    .detail(AppConstants.DETAIL_KEY_AVAILABLE_ROLES_COUNT, availableRoles.size())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE)
                    .log();
                    
            } catch (Exception e) {
                logger.warn()
                    .message(AppConstants.LOG_FAILED_TO_RETRIEVE_AVAILABLE_ROLES)
                    .detail("error", e.getMessage())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_ROLE_REQUEST)
                    .log();
                // Continue with empty roles list - don't fail the entire request
            }

            logger.info()
                .message(AppConstants.LOG_SUCCESSFULLY_RETRIEVED_HIERARCHICAL_PERMISSIONS)
                .detail(AppConstants.DETAIL_KEY_TOTAL_AVAILABLE_MODULES, moduleNames.size())
                .detail(AppConstants.DETAIL_KEY_FILTERED_MODULES, modules.size())
                .detail(AppConstants.DETAIL_KEY_AVAILABLE_ROLES_COUNT, availableRoles.size())
                .detail(AppConstants.DETAIL_KEY_MODULE_FILTER, moduleNameFilter)
                .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE)
                .log();

            return new HierarchicalPermissionsResponse(modules, moduleNames, availableRoles);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_RETRIEVE_HIERARCHICAL_PERMISSIONS)
                .detail(AppConstants.DETAIL_KEY_MODULE_FILTER, moduleNameFilter)
                .objectType(AppConstants.LOG_OBJECT_TYPE_MODULE_REQUEST)
                .log();
            throw new RuntimeException(AppConstants.FAILED_TO_RETRIEVE_HIERARCHICAL_PERMISSIONS + ": " + e.getMessage(), e);
        }
    }
}