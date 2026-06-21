package com.aidentx.core.log_management.model.dto;

/**
 * DTO representing a microservice component with its debug configuration.
 */
public class ComponentDebugInfo {
    
    private String name;
    private String description;
    private String level;
    private String serviceUrl;
    private boolean active;

    public ComponentDebugInfo() {}

    public ComponentDebugInfo(String name, String description, String level, String serviceUrl, boolean active) {
        this.name = name;
        this.description = description;
        this.level = level;
        this.serviceUrl = serviceUrl;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
