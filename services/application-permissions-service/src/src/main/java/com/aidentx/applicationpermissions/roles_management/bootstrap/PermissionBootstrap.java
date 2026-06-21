package com.aidentx.applicationpermissions.roles_management.bootstrap;

import com.aidentx.applicationpermissions.roles_management.enums.AssignmentType;
import com.aidentx.applicationpermissions.roles_management.enums.HttpMethod;
import com.aidentx.applicationpermissions.roles_management.enums.RiskLevel;
import com.aidentx.applicationpermissions.roles_management.enums.RoleType;
import com.aidentx.applicationpermissions.roles_management.model.entity.Action;
import com.aidentx.applicationpermissions.roles_management.model.entity.Module;
import com.aidentx.applicationpermissions.roles_management.model.entity.Page;
import com.aidentx.applicationpermissions.roles_management.model.entity.Permission;
import com.aidentx.applicationpermissions.roles_management.model.entity.Role;
import com.aidentx.applicationpermissions.roles_management.model.entity.RolePermission;
import com.aidentx.applicationpermissions.roles_management.model.entity.UserRole;
import com.aidentx.applicationpermissions.roles_management.repository.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Bootstrap component for initializing system-wide permission structure.
 * 
 * <p>This component runs on application startup and seeds the database with:</p>
 * <ul>
 *   <li>System modules (organizational containers)</li>
 *   <li>System pages (frontend routes)</li>
 *   <li>Standard actions (View, Create, Edit, Delete)</li>
 *   <li>Permissions for all actions</li>
 *   <li>Initial permission assignments to ADMINISTRATOR roles</li>
 * </ul>
 * 
 * <p>All entities created have tenantId=null, making them system-wide and
 * available to all tenants.</p>
 * 
 * <p>The bootstrap process is idempotent - it only runs if the module table
 * is empty, preventing duplicate entries on subsequent startups.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class PermissionBootstrap {
    
    @Value("${security.permissions.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;
    
    private final ModuleRepository moduleRepository;
    private final PageRepository pageRepository;
    private final ActionRepository actionRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final IUnifiedLogger logger;
    
    @Autowired
    public PermissionBootstrap(
            ModuleRepository moduleRepository,
            PageRepository pageRepository,
            ActionRepository actionRepository,
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRoleRepository userRoleRepository,
            JdbcTemplate jdbcTemplate,
            UnifiedLoggerFactory loggerFactory) {
        this.moduleRepository = moduleRepository;
        this.pageRepository = pageRepository;
        this.actionRepository = actionRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.logger = loggerFactory.getLogger("PermissionBootstrap", "BOOTSTRAP");
    }
    
    /**
     * Initialize system permissions on application startup.
     * Only runs if modules table is empty (first startup).
     */
    @PostConstruct
    @Transactional
    public void initializeSystemPermissions() {
        if (!bootstrapEnabled) {
            logger.info()
                .message("Permission bootstrap is disabled via configuration")
                .log();
            return;
        }
        
        // Check if already initialized - check multiple tables to prevent duplicate runs
        long moduleCount = moduleRepository.count();
        long pageCount = pageRepository.count();
        long actionCount = actionRepository.count();
        
        if (moduleCount > 0 || pageCount > 0 || actionCount > 0) {
            // Validate data integrity: check if pages have module_id set
            long orphanedPages = pageRepository.findByTenantIdIsNull().stream()
                .filter(p -> p.getModuleId() == null)
                .count();
            
            if (orphanedPages > 0) {
                logger.warn()
                    .message("Found orphaned pages without module_id, fixing data integrity issue")
                    .detail("orphanedPageCount", String.valueOf(orphanedPages))
                    .log();
                
                // Fix orphaned pages by deleting them - they'll be recreated
                List<Page> orphaned = pageRepository.findByTenantIdIsNull().stream()
                    .filter(p -> p.getModuleId() == null)
                    .collect(Collectors.toList());
                
                // Delete associated actions first (foreign key constraint)
                for (Page page : orphaned) {
                    List<Action> pageActions = actionRepository.findByPageId(page.getId());
                    actionRepository.deleteAll(pageActions);
                    logger.debug()
                        .message("Deleted actions for orphaned page: " + page.getPageName())
                        .detail("actionCount", pageActions.size())
                        .log();
                }
                
                // Delete orphaned pages
                pageRepository.deleteAll(orphaned);
                logger.info()
                    .message("Deleted orphaned pages to allow recreation")
                    .detail("deletedPageCount", String.valueOf(orphaned.size()))
                    .log();
                
                // Recalculate counts
                pageCount = pageRepository.count();
                actionCount = actionRepository.count();
            }
            
            // Check for pages referencing deleted modules
            List<Module> existingModules = moduleRepository.findByTenantIdIsNull();
            Set<UUID> validModuleIds = existingModules.stream()
                .map(Module::getId)
                .collect(Collectors.toSet());
            
            List<Page> pagesWithInvalidModules = pageRepository.findByTenantIdIsNull().stream()
                .filter(p -> p.getModuleId() != null && !validModuleIds.contains(p.getModuleId()))
                .collect(Collectors.toList());
            
            if (!pagesWithInvalidModules.isEmpty()) {
                logger.warn()
                    .message("Found pages referencing deleted modules, fixing integrity")
                    .detail("affectedPageCount", String.valueOf(pagesWithInvalidModules.size()))
                    .log();
                
                // Delete actions for these pages
                for (Page page : pagesWithInvalidModules) {
                    List<Action> pageActions = actionRepository.findByPageId(page.getId());
                    actionRepository.deleteAll(pageActions);
                }
                
                // Delete pages with invalid module references
                pageRepository.deleteAll(pagesWithInvalidModules);
                logger.info()
                    .message("Deleted pages with invalid module references")
                    .detail("deletedPageCount", String.valueOf(pagesWithInvalidModules.size()))
                    .log();
                
                // Recalculate counts
                pageCount = pageRepository.count();
                actionCount = actionRepository.count();
            }
            
            // Note: We continue with bootstrap even if data exists
            // This allows adding new actions/permissions without deleting existing ones
            // The create methods are idempotent and will reuse existing entities
            logger.info()
                .message("Existing permission structure found, will update if needed")
                .detail("moduleCount", String.valueOf(moduleCount))
                .detail("pageCount", String.valueOf(pageCount))
                .detail("actionCount", String.valueOf(actionCount))
                .log();
        }
        
        logger.info()
            .message("========================================")
            .log();
        logger.info()
            .message("INITIALIZING SYSTEM PERMISSIONS")
            .log();
        logger.info()
            .message("========================================")
            .log();
        
        try {
            // Step 0: Create standard system roles
            Map<String, Role> standardRoles = createStandardRoles();
            logger.info()
                .message("✓ Ensured " + standardRoles.size() + " standard roles exist")
                .log();
            
            // Step 1: Create modules
            Map<String, Module> modules = createModules();
            logger.info()
                .message("✓ Created " + modules.size() + " system modules")
                .log();
            
            // Step 2: Create pages
            List<Page> pages = createPages(modules);
            logger.info()
                .message("✓ Created " + pages.size() + " system pages")
                .log();
            
            // Step 3: Create actions for all pages
            List<Action> actions = createActions(pages);
            logger.info()
                .message("✓ Created " + actions.size() + " standard actions")
                .log();
            
            // Step 4: Create permissions for all actions
            List<Permission> permissions = createPermissions(actions);
            logger.info()
                .message("✓ Created " + permissions.size() + " permissions")
                .log();
            
            // Step 5: Assign all permissions to ADMINISTRATOR roles
            int assignedCount = assignPermissionsToAdministrators(permissions);
            logger.info()
                .message("✓ Assigned " + assignedCount + " permissions to ADMINISTRATOR roles")
                .log();
            
            // Step 6: Migrate users from users_role to user_role
            int migratedCount = migrateUsersFromAuthService(standardRoles);
            logger.info()
                .message("✓ Migrated/verified " + migratedCount + " user-role assignments")
                .log();
            
            logger.info()
                .message("========================================")
                .log();
            logger.info()
                .message("PERMISSION BOOTSTRAP COMPLETED SUCCESSFULLY")
                .log();
            logger.info()
                .message("Modules: " + modules.size() + 
                        " | Pages: " + pages.size() + 
                        " | Actions: " + actions.size() + 
                        " | Permissions: " + permissions.size())
                .log();
            logger.info()
                .message("========================================")
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .message("FAILED TO BOOTSTRAP PERMISSIONS")
                .detail("error", e.getMessage())
                .log();
            throw new RuntimeException("Permission bootstrap failed", e);
        }
    }
    
    /**
     * Creates system-wide modules (organizational containers).
     * Based on production structure from 52.143.130.207
     * Checks if modules already exist to prevent duplicates.
     */
    private Map<String, Module> createModules() {
        Map<String, Module> modules = new LinkedHashMap<>();
        
        String[] moduleNames = {
            "Core Management",
            "Monitoring & Analytics",
            "Security & Access",
            "System"
        };
        
        // Get existing system modules
        List<Module> existingModules = moduleRepository.findByTenantIdIsNull();
        Map<String, Module> existingByName = existingModules.stream()
            .collect(Collectors.toMap(Module::getModuleName, m -> m));
        
        for (String moduleName : moduleNames) {
            Module module;
            
            if (existingByName.containsKey(moduleName)) {
                // Use existing module
                module = existingByName.get(moduleName);
                logger.debug()
                    .message("Module already exists: " + moduleName)
                    .detail("moduleId", module.getId().toString())
                    .log();
            } else {
                // Create new module
                module = new Module(moduleName, null); // null = system-wide
                module = moduleRepository.save(module);
                logger.debug()
                    .message("Created module: " + moduleName)
                    .detail("moduleId", module.getId().toString())
                    .log();
            }
            
            modules.put(moduleName, module);
        }
        
        return modules;
    }
    
    /**
     * Creates system-wide pages based on production structure.
     * Matches exact structure from production database @ 52.143.130.207
     */
    private List<Page> createPages(Map<String, Module> modules) {
        List<Page> pages = new ArrayList<>();
        
        // Core Management Module (5 pages)
        pages.addAll(createPagesForModule("Core Management", modules, new String[][]{
            {"Access Catalog", "/access-catalog"},
            {"Identities", "/identities"},
            {"Organization", "/organization"},
            {"applications", "/applications"},
            {"dashboard", "/dashboard"}
        }));
        
        // Monitoring & Analytics Module (6 pages)
        pages.addAll(createPagesForModule("Monitoring & Analytics", modules, new String[][]{
            {"Access Analysis", "/access-analysis"},
            {"Audit logs", "/auditlogs"},
            {"Debug Logs", "/debugging"},  // Renamed from "Debugging" to avoid duplicate with System module
            {"Monitoring", "/monitoring"},
            {"Reports", "/reports"},
            {"Task center", "/task-center"}
        }));
        
        // Security & Access Module (4 pages)
        pages.addAll(createPagesForModule("Security & Access", modules, new String[][]{
            {"Access Portal", "/access-portal"},
            {"Access Review", "/access-review"},
            {"SoD Policies", "/sod-policies"},
            {"Workflows", "/workflows"}
        }));
        
        // System Module (19 pages - mostly settings)
        pages.addAll(createPagesForModule("System", modules, new String[][]{
            {"Settings", "/settings"},
            {"AI Management", "/settings/ai-management"},
            {"Access Management", "/settings/access"},
            {"Access Reviews", "/settings/reviews"},
            {"Application Permissions", "/settings/application-permissions"},
            {"Application UI", "/settings/application-ui"},
            {"Compliance & Audit", "/settings/compliance"},
            {"Debugging Settings", "/settings/debugging"},  // Renamed from "Debugging" to avoid duplicate
            {"Identity Attribute Mappings", "/settings/identity-mappings"},
            {"Identity Governance", "/settings/governance"},
            {"Lifecycle Management", "/settings/lifecycle"},
            {"Login Security", "/settings/login-security"},
            {"Notification Management", "/settings/notification-management"},
            {"Reports Management", "/settings/reports-management"},
            {"Risk Analytics", "/settings/risk"},
            {"Schema Management", "/settings/schema-management"},
            {"Search Management", "/settings/search-management"},
            {"Single Sign-on connections", "/settings/sso"},
            {"System Integrations", "/settings/integrations"}
        }));
        
        return pages;
    }
    
    /**
     * Helper to create pages for a specific module.
     */
    private List<Page> createPagesForModule(String moduleName, Map<String, Module> modules, String[][] pageData) {
        List<Page> pages = new ArrayList<>();
        Module module = modules.get(moduleName);
        
        if (module == null) {
            logger.warn()
                .message("Module not found: " + moduleName)
                .log();
            return pages;
        }
        
        // Get existing pages for this module
        List<Page> existingPages = pageRepository.findByModuleId(module.getId());
        Map<String, Page> existingByPath = existingPages.stream()
            .collect(Collectors.toMap(Page::getPagePath, p -> p, (p1, p2) -> p1));
        
        for (String[] data : pageData) {
            String pageName = data[0];
            String pagePath = data[1];
            
            Page page;
            if (existingByPath.containsKey(pagePath)) {
                // Reuse existing page
                page = existingByPath.get(pagePath);
                // Update name if it changed
                if (!page.getPageName().equals(pageName)) {
                    page.setPageName(pageName);
                    page = pageRepository.save(page);
                    logger.debug()
                        .message("Updated page name: " + pagePath + " -> " + pageName)
                        .detail("moduleId", module.getId().toString())
                        .log();
                } else {
                    logger.debug()
                        .message("Page already exists: " + pageName + " (" + pagePath + ")")
                        .detail("moduleId", module.getId().toString())
                        .log();
                }
            } else {
                // Create new page
                page = new Page(pageName, pagePath, null); // null = system-wide
                page.setModuleId(module.getId());
                page = pageRepository.save(page);
                logger.debug()
                    .message("Created page: " + pageName + " (" + pagePath + ")")
                    .detail("moduleId", module.getId().toString())
                    .log();
            }
            
            pages.add(page);
        }
        
        return pages;
    }
    
    /**
     * Creates basic CRUD actions + Export/Import for all pages.
     * Simplified to standard operations only.
     */
    private List<Action> createActions(List<Page> pages) {
        List<Action> actions = new ArrayList<>();
        
        for (Page page : pages) {
            String apiBase = determineApiEndpoint(page.getPageName(), page.getPagePath());
            
            if (apiBase != null) {
                // Standard CRUD + Export/Import
                // View covers both base endpoint (/api/v1/workflows) AND sub-paths (/api/v1/workflows/123)
                actions.add(createAction("View", page, HttpMethod.GET, createViewPattern(apiBase)));
                // Create also needs base pattern (POST /api/v1/workflows without ID) - same as View
                actions.add(createAction("Create", page, HttpMethod.POST, createViewPattern(apiBase)));
                actions.add(createAction("Update", page, HttpMethod.PUT, appendToAllPatterns(apiBase, "/*")));
                actions.add(createAction("Delete", page, HttpMethod.DELETE, appendToAllPatterns(apiBase, "/*")));
                actions.add(createAction("Export", page, HttpMethod.GET, appendToAllPatterns(apiBase, "/export")));
                actions.add(createAction("Import", page, HttpMethod.POST, appendToAllPatterns(apiBase, "/import")));
            } else {
                // Settings/config pages - View only
                actions.add(createAction("View", page, null, null));
            }
        }
        
        return actions;
    }
    
    /**
     * Appends a suffix to each pattern in a comma-separated list.
     * Example: "/api/v1/workflows,/api/v1/executions" + "/*" -> "/api/v1/workflows/*,/api/v1/executions/*"
     */
    private String appendToAllPatterns(String patterns, String suffix) {
        if (patterns.contains(",")) {
            String[] patternArray = patterns.split(",");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < patternArray.length; i++) {
                if (i > 0) result.append(",");
                result.append(patternArray[i].trim()).append(suffix);
            }
            return result.toString();
        }
        return patterns + suffix;
    }
    
    /**
     * Creates View endpoint pattern covering both base and wildcard paths.
     * Example: "/api/v1/workflows,/api/v1/executions" -> "/api/v1/workflows,/api/v1/workflows/*,/api/v1/executions,/api/v1/executions/*"
     */
    private String createViewPattern(String patterns) {
        if (patterns.contains(",")) {
            String[] patternArray = patterns.split(",");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < patternArray.length; i++) {
                if (i > 0) result.append(",");
                String pattern = patternArray[i].trim();
                result.append(pattern).append(",").append(pattern).append("/*");
            }
            return result.toString();
        }
        return patterns + "," + patterns + "/*";
    }
    
    /**
     * Determine API endpoint base from page name/path.
     * Can return multiple patterns separated by comma to cover related endpoints.
     * Verified against actual @RequestMapping in controllers for production readiness.
     */
    private String determineApiEndpoint(String pageName, String pagePath) {
        switch (pageName) {
            // ========== Core Management Module ==========
            case "Identities": 
                // identity-service: IdentityController, GovernancePolicyController, ExtendedAttributeDefinitionController, ShadowController
                // NOTE: /api/v1/roles removed - gateway routes it to app-permissions-service, not identity-service
                return "/api/v1/identity,/api/v1/identity/schema,/api/v1/shadows";
                
            case "Organization": 
                // application-permissions-service: OrganizationManagementController
                return "/api/v1/organizations";
                
            case "applications": 
                // core-service: ApplicationsOnboardingController, ResourceDiscoveryController
                return "/api/v1/applications,/api/v1/connectors";
                
            case "Access Catalog": 
                // identity-service: EntitlementController
                return "/api/v1/entitlements";
                
            case "dashboard":
                return null; // Frontend only page, no backend APIs
            
            // ========== Security & Access Module ==========
            case "Access Portal": 
                return null; // No controller exists
                
            case "SoD Policies": 
                return null; // No controller exists
                
            case "Workflows": 
                // workflows-service: WorkflowController, WorkflowExecutionController, WorkflowApprovalController, WorkflowTemplateController, ActionTemplateController
                return "/api/v1/workflows,/api/v1/executions,/api/v1/approvals,/api/v1/templates,/api/v1/action-templates";
                
            case "Access Review": 
                return null; // No controller exists
            
            // ========== Monitoring & Analytics Module ==========
            case "Access Analysis": 
                return null; // No controller exists
                
            case "Audit logs": 
                // core-service: AuditLogController
                return "/api/v1/audit-logs";
                
            case "Debug Logs":
                // core-service: LogLevelController, LogsFilteringController, LogsPinningController
                // auth-service: LogLevelController
                return "/api/v1/logs";
                
            case "Monitoring": 
                return null; // No controller exists
                
            case "Task center": 
                // task-management-service: TaskLogController, TaskController
                return "/api/v1/tasks";
                
            case "Reports": 
                return null; // No controller exists
            
            // ========== System Module - Settings Pages ==========
            case "Settings":
                return null; // Parent settings page, no backend APIs
                
            case "AI Management": 
                // llm-service: LlmController
                // configuration-service: ArtifactController
                return "/api/v1/llm,/api/v1/artifacts";
                
            case "Access Management": 
                return null; // Configuration page, no backend APIs
                
            case "Application Permissions": 
                // application-permissions-service: ALL controllers (different tabs in UI)
                // RoleController (Application Permission Roles - NOT Business Roles), UserRoleController, UserController
                // DynamicRuleController, DynamicRuleSchedulerController, GranularPermissionController, PermissionCheckController
                // NOTE: /api/v1/roles here is APPLICATION-PERMISSIONS-SERVICE, different from identity-service business roles
                return "/api/v1/roles,/api/v1/user-roles,/api/v1/users,/api/v1/dynamic-rules,/api/v1/dynamic-rules/scheduler,/api/v1/granular-permissions,/api/v1/permissions";
                
            case "Access Reviews": 
                return null; // No controller exists
                
            case "Application UI": 
                // configuration-service: ConfigurationController
                return "/api/v1/configurations";
                
            case "Compliance & Audit": 
                return null; // Configuration page, no backend APIs
                
            case "Debugging Settings": 
                // core-service: LogLevelController, LogsFilteringController, LogsPinningController
                return "/api/v1/logs";
                
            case "Identity Attribute Mappings": 
                // identity-service: ExtendedAttributeDefinitionController
                return "/api/v1/identity/schema";
                
            case "Identity Governance": 
                // identity-service: IdentityController, GovernancePolicyController
                return "/api/v1/identity";
                
            case "Lifecycle Management": 
                return null; // Configuration page, no backend APIs
                
            case "Login Security": 
                // auth-service: AccountLockPolicyController, PasswordPolicyController, PasswordPolicyExpirationController, SessionControlController
                return "/api/v1/account-lock-policy,/api/v1/password-policy,/api/v1/admin/password-policy-expiration,/api/v1/session-control";
                
            case "Notification Management": 
                // core-service: NotificationSseController, EmailConfigurationController, TemplateManagerController, UserNotificationController
                return "/api/v1/notifications,/api/v1/notifications/email-configurations,/api/v1/notifications/template-managers,/api/v1/user-notifications";
                
            case "Reports Management": 
                return null; // No controller exists
                
            case "Risk Analytics": 
                return null; // Configuration page, no backend APIs
                
            case "Schema Management": 
                // identity-service: ExtendedAttributeDefinitionController
                return "/api/v1/identity/schema";
                
            case "Search Management": 
                return null; // Configuration page, no backend APIs
                
            case "Single Sign-on connections": 
                // configuration-service: SsoConfigController
                return "/api/v1/admin/sso";
                
            case "System Integrations": 
                // core-service: ResourceDiscoveryController
                return "/api/v1/connectors";
            
            default: 
                return null; // Unknown page
        }
    }

    
    /**
     * Helper to create a single action.
     * Matches production pattern where most actions have NULL method/endpoint.
     */
    private Action createAction(String actionName, Page page, HttpMethod method, String endpoint) {
        // Check if action already exists for this page with same name and endpoint
        List<Action> existingActions = actionRepository.findByPageId(page.getId());
        
        String methodStr = method != null ? method.toString() : null;
        String endpointStr = endpoint != null ? endpoint : null;
        
        for (Action existing : existingActions) {
            boolean nameMatches = existing.getActionName().equals(actionName);
            boolean methodMatches = (existing.getMethod() == null && method == null) || 
                                   (existing.getMethod() != null && existing.getMethod().equals(method));
            boolean endpointMatches = (existing.getEndpoint() == null && endpoint == null) ||
                                     (existing.getEndpoint() != null && existing.getEndpoint().equals(endpoint));
            
            if (nameMatches && methodMatches && endpointMatches) {
                logger.debug()
                    .message("Action already exists: " + actionName + " " + 
                            (methodStr != null ? methodStr : "NULL") + " " + 
                            (endpointStr != null ? endpointStr : "NULL"))
                    .detail("pageId", page.getId().toString())
                    .detail("actionId", existing.getId().toString())
                    .log();
                return existing;
            }
        }
        
        // Create new action
        Action action = new Action();
        action.setActionName(actionName);
        action.setMethod(method); // Can be null for page-level access
        action.setEndpoint(endpoint); // Can be null for page-level access
        action.setPageId(page.getId());
        action.setTenantId(null); // system-wide
        action = actionRepository.save(action);
        
        logger.debug()
            .message("Created action: " + actionName + " " + 
                    (methodStr != null ? methodStr : "NULL") + " " + 
                    (endpointStr != null ? endpointStr : "NULL"))
            .detail("pageId", page.getId().toString())
            .detail("actionId", action.getId().toString())
            .log();
        
        return action;
    }
    
    /**
     * Creates permissions for all actions.
     * Idempotent: Reuses existing permissions, only creates if missing.
     */
    private List<Permission> createPermissions(List<Action> actions) {
        List<Permission> permissions = new ArrayList<>();
        
        for (Action action : actions) {
            // Check if permission already exists for this action
            List<Permission> existingPermissions = permissionRepository.findByActionId(action.getId());
            
            if (!existingPermissions.isEmpty()) {
                // Reuse existing permission (take first one if multiple exist)
                Permission existingPermission = existingPermissions.get(0);
                permissions.add(existingPermission);
                logger.debug()
                    .message("Reusing existing permission for action: " + action.getActionName())
                    .detail("actionId", action.getId().toString())
                    .detail("permissionId", existingPermission.getId().toString())
                    .log();
            } else {
                // Create new permission
                Permission permission = new Permission();
                permission.setActionId(action.getId());
                permission = permissionRepository.save(permission);
                permissions.add(permission);
                
                logger.debug()
                    .message("Created new permission for action: " + action.getActionName())
                    .detail("actionId", action.getId().toString())
                    .detail("permissionId", permission.getId().toString())
                    .log();
            }
        }
        
        return permissions;
    }
    
    /**
     * Assigns all permissions to existing ADMINISTRATOR and SYSTEM roles.
     * This ensures existing admin users maintain full access after bootstrap.
     */
    private int assignPermissionsToAdministrators(List<Permission> permissions) {
        // Find all ADMINISTRATOR and SYSTEM roles
        List<Role> adminRoles = roleRepository.findByRoleType(RoleType.ADMINISTRATOR);
        List<Role> systemRoles = roleRepository.findByRoleType(RoleType.SYSTEM);
        
        // Combine both lists
        List<Role> allAdminRoles = new ArrayList<>();
        allAdminRoles.addAll(adminRoles);
        allAdminRoles.addAll(systemRoles);
        
        if (allAdminRoles.isEmpty()) {
            logger.warn()
                .message("No ADMINISTRATOR or SYSTEM roles found - no initial permissions assigned")
                .log();
            return 0;
        }
        
        logger.info()
            .message("Found " + adminRoles.size() + " ADMINISTRATOR roles and " + systemRoles.size() + " SYSTEM roles")
            .log();
        
        int assignedCount = 0;
        
        for (Role adminRole : allAdminRoles) {
            logger.info()
                .message("Assigning permissions to role: " + adminRole.getRoleName())
                .detail("roleType", adminRole.getRoleType().name())
                .detail("roleId", adminRole.getId().toString())
                .detail("tenantId", adminRole.getTenantId())
                .log();
            
            for (Permission permission : permissions) {
                // Check if already assigned (in case of partial bootstrap)
                boolean exists = rolePermissionRepository.existsByRoleIdAndPermissionId(
                    adminRole.getId(), permission.getId()
                );
                
                if (!exists) {
                    RolePermission rolePermission = new RolePermission(
                        adminRole.getId(), 
                        permission.getId()
                    );
                    rolePermissionRepository.save(rolePermission);
                    assignedCount++;
                }
            }
            
            logger.info()
                .message("Assigned " + permissions.size() + " permissions to role: " + adminRole.getRoleName())
                .log();
        }
        
        return assignedCount;
    }
    
    /**
     * Creates standard system roles for each tenant if they don't exist.
     * Standard roles: Administrator, Reviewer, Operator, Viewer.
     * 
     * @return map of role names to Role entities
     */
    private Map<String, Role> createStandardRoles() {
        Map<String, Role> standardRoles = new HashMap<>();
        
        // Get all unique tenants from the database
        List<String> tenants = getTenants();
        
        if (tenants.isEmpty()) {
            tenants.add("default"); // Ensure at least default tenant
        }
        
        // Standard roles configuration: [name, roleType]
        Object[][] roleConfigs = {
            {"Administrator", RoleType.ADMINISTRATOR},
            {"Reviewer", RoleType.REVIEWER},
            {"Operator", RoleType.OPERATOR},
            {"Viewer", RoleType.VIEWER}
        };
        
        for (String tenantId : tenants) {
            logger.info()
                .message("Checking standard roles for tenant: " + tenantId)
                .log();
            
            for (Object[] config : roleConfigs) {
                String roleName = (String) config[0];
                RoleType roleType = (RoleType) config[1];
                
                // Check if role already exists
                Optional<Role> existingRole = roleRepository
                    .findByRoleNameAndTenantId(roleName, tenantId);
                
                if (existingRole.isEmpty()) {
                    // Use proper constructor that sets audit fields automatically
                    UUID systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");
                    
                    Role role = new Role(
                        tenantId,                                      // tenantId
                        roleType,                                      // roleType
                        roleName,                                      // roleName
                        "System-generated " + roleName + " role",     // description
                        "Bootstrap initialization",                    // businessJustification
                        false,                                         // requireOwnerApproval
                        null,                                          // tags
                        RiskLevel.Low,                                 // riskLevel
                        true,                                          // isActive
                        "System",                                      // createdByName
                        systemUserId                                   // createdById
                    );
                    
                    Role savedRole = roleRepository.save(role);
                    standardRoles.put(roleName + "_" + tenantId, savedRole);
                    
                    logger.info()
                        .message("Created standard role: " + roleName)
                        .detail("roleId", savedRole.getId().toString())
                        .detail("tenantId", tenantId)
                        .detail("roleType", roleType.name())
                        .log();
                } else {
                    standardRoles.put(roleName + "_" + tenantId, existingRole.get());
                    logger.info()
                        .message("Standard role already exists: " + roleName)
                        .detail("tenantId", tenantId)
                        .log();
                }
            }
        }
        
        return standardRoles;
    }
    
    /**
     * Migrates users from users_role (auth-service) to user_role (permissions-service).
     * Maps SUPER_ADMIN → Administrator, USER → Viewer.
     * 
     * @param standardRoles map of standard roles created
     * @return count of migrated/verified user-role assignments
     */
    private int migrateUsersFromAuthService(Map<String, Role> standardRoles) {
        int migratedCount = 0;
        
        try {
            // Query users_role table from auth-service schema
            String query = "SELECT ur.user_id, ur.role, u.tenant_id " +
                          "FROM users_role ur " +
                          "JOIN users u ON ur.user_id = u.id";
            
            List<Map<String, Object>> usersWithRoles = jdbcTemplate.queryForList(query);
            
            logger.info()
                .message("Found " + usersWithRoles.size() + " users in users_role table")
                .log();
            
            for (Map<String, Object> userRole : usersWithRoles) {
                UUID userId = UUID.fromString(userRole.get("user_id").toString());
                String authRole = userRole.get("role").toString(); // SUPER_ADMIN or USER
                String tenantId = userRole.get("tenant_id").toString();
                
                // Map auth-service role to permissions-service role
                String targetRoleName = "SUPER_ADMIN".equals(authRole) ? "Administrator" : "Viewer";
                String roleKey = targetRoleName + "_" + tenantId;
                
                Role targetRole = standardRoles.get(roleKey);
                
                if (targetRole == null) {
                    // Try to find the role if it wasn't in standard roles
                    Optional<Role> foundRole = roleRepository
                        .findByRoleNameAndTenantId(targetRoleName, tenantId);
                    
                    if (foundRole.isEmpty()) {
                        logger.warn()
                            .message("Target role not found for migration")
                            .detail("userId", userId.toString())
                            .detail("authRole", authRole)
                            .detail("targetRoleName", targetRoleName)
                            .detail("tenantId", tenantId)
                            .log();
                        continue;
                    }
                    
                    targetRole = foundRole.get();
                }
                
                // Check if user-role assignment already exists
                boolean exists = userRoleRepository.existsByUserIdAndRoleIdAndTenantId(
                    userId, targetRole.getId(), tenantId
                );
                
                if (!exists) {
                    // Create user-role assignment
                    UserRole newUserRole = new UserRole(
                        userId,
                        targetRole.getId(),
                        tenantId,
                        AssignmentType.MANUAL
                    );
                    
                    userRoleRepository.save(newUserRole);
                    migratedCount++;
                    
                    logger.info()
                        .message("Migrated user from users_role to user_role")
                        .detail("userId", userId.toString())
                        .detail("authRole", authRole)
                        .detail("newRole", targetRole.getRoleName())
                        .detail("roleType", targetRole.getRoleType().name())
                        .detail("tenantId", tenantId)
                        .log();
                } else {
                    logger.info()
                        .message("User-role assignment already exists")
                        .detail("userId", userId.toString())
                        .detail("roleId", targetRole.getId().toString())
                        .detail("tenantId", tenantId)
                        .log();
                }
            }
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to migrate users from users_role table")
                .detail("error", e.getMessage())
                .log();
            // Don't fail the entire bootstrap if migration fails
        }
        
        return migratedCount;
    }
    
    /**
     * Gets list of unique tenant IDs from the users table.
     * 
     * @return list of tenant IDs
     */
    private List<String> getTenants() {
        try {
            String query = "SELECT DISTINCT tenant_id FROM users WHERE tenant_id IS NOT NULL";
            return jdbcTemplate.queryForList(query, String.class);
        } catch (Exception e) {
            logger.warn()
                .message("Could not fetch tenants from users table, using default")
                .detail("error", e.getMessage())
                .log();
            return new ArrayList<>();
        }
    }
}
