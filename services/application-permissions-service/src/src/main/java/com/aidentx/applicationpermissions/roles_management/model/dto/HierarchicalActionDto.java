package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing an action in the hierarchical structure.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class HierarchicalActionDto {
    
    private String id;
    private String name;
    private String apiEndpoint;
    private String method;
    private Boolean hasRole;

    // Constructors
    public HierarchicalActionDto() {}

    public HierarchicalActionDto(String id, String name, String apiEndpoint, String method) {
        this.id = id;
        this.name = name;
        this.apiEndpoint = apiEndpoint;
        this.method = method;
    }

    public HierarchicalActionDto(String id, String name, String apiEndpoint, String method, Boolean hasRole) {
        this.id = id;
        this.name = name;
        this.apiEndpoint = apiEndpoint;
        this.method = method;
        this.hasRole = hasRole;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Boolean getHasRole() {
        return hasRole;
    }

    public void setHasRole(Boolean hasRole) {
        this.hasRole = hasRole;
    }
}