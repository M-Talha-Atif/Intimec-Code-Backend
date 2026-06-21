package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

/**
 * Response DTO for page access information.
 * Contains page details and user access permission status.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageAccessResponse {
    
    private UUID id;
    private String name;
    private String path;
    private boolean canAccess;
    private boolean requiresPermission;
    private String moduleName;
    private UUID moduleId;

    // Constructors
    public PageAccessResponse() {}

    public PageAccessResponse(UUID id, String name, String path, boolean canAccess, boolean requiresPermission, String moduleName, UUID moduleId) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.canAccess = canAccess;
        this.requiresPermission = requiresPermission;
        this.moduleName = moduleName;
        this.moduleId = moduleId;
    }
    
    public PageAccessResponse(UUID id, String name, String path, boolean canAccess, boolean requiresPermission) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.canAccess = canAccess;
        this.requiresPermission = requiresPermission;
        this.moduleName = null; // Exclude from response
        this.moduleId = null;   // Exclude from response
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isCanAccess() {
        return canAccess;
    }

    public void setCanAccess(boolean canAccess) {
        this.canAccess = canAccess;
    }

    public boolean isRequiresPermission() {
        return requiresPermission;
    }

    public void setRequiresPermission(boolean requiresPermission) {
        this.requiresPermission = requiresPermission;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public void setModuleId(UUID moduleId) {
        this.moduleId = moduleId;
    }
}