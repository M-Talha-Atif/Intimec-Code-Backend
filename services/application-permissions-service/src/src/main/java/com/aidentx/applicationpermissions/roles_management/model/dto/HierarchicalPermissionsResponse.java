package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.util.List;

/**
 * DTO for the complete hierarchical permissions response.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class HierarchicalPermissionsResponse {
    
    private List<HierarchicalModuleDto> modules;
    private List<String> availableModules;
    private List<AvailableRoleDto> availableRoles;

    // Constructors
    public HierarchicalPermissionsResponse() {}

    public HierarchicalPermissionsResponse(List<HierarchicalModuleDto> modules, List<String> availableModules) {
        this.modules = modules;
        this.availableModules = availableModules;
    }

    public HierarchicalPermissionsResponse(List<HierarchicalModuleDto> modules, List<String> availableModules, List<AvailableRoleDto> availableRoles) {
        this.modules = modules;
        this.availableModules = availableModules;
        this.availableRoles = availableRoles;
    }

    // Getters and Setters
    public List<HierarchicalModuleDto> getModules() {
        return modules;
    }

    public void setModules(List<HierarchicalModuleDto> modules) {
        this.modules = modules;
    }

    public List<String> getAvailableModules() {
        return availableModules;
    }

    public void setAvailableModules(List<String> availableModules) {
        this.availableModules = availableModules;
    }

    public List<AvailableRoleDto> getAvailableRoles() {
        return availableRoles;
    }

    public void setAvailableRoles(List<AvailableRoleDto> availableRoles) {
        this.availableRoles = availableRoles;
    }
}