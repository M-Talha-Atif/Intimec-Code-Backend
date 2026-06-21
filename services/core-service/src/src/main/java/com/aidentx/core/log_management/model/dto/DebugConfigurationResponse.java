package com.aidentx.core.log_management.model.dto;

import java.util.List;

/**
 * DTO representing the overall debug configuration response.
 */
public class DebugConfigurationResponse {
    
    private int totalComponents;
    private List<ComponentDebugInfo> components;

    public DebugConfigurationResponse() {}

    public int getTotalComponents() {
        return totalComponents;
    }

    public void setTotalComponents(int totalComponents) {
        this.totalComponents = totalComponents;
    }

    public List<ComponentDebugInfo> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentDebugInfo> components) {
        this.components = components;
    }
}
